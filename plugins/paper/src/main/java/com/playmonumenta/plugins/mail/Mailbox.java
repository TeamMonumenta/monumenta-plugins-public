package com.playmonumenta.plugins.mail;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.RedisAPI;
import io.lettuce.core.SetArgs;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Mailbox is a container of mail sent from a player to a recipient, whether that be a player or guild.
 * It is backed by Redis in order to provide cross-shard access without risk of duplication or deletion of items.
 * Because of this, there is no harm in having multiple Mailbox objects between the same sender-recipient pair.
 */
public class Mailbox implements Comparable<Mailbox> {
	// These limits can be adjusted if needed; only MAX_SLOTS requires a GUI update
	public static final int MAX_SLOTS = 6;
	private static final int MAX_LOG_DESCRIPTION_LENGTH = 900;
	// This is the worst case time allowed to finish working with Redis data; if exceeded, the lock will expire
	private static final long LOCK_TIMEOUT_MS = 10_000L;

	// Special return codes for redis pexiretime command
	private static final long PEXPIRETIME_EXISTS_NO_EXPIRATION = -1L;
	private static final long PEXPIRETIME_DOES_NOT_EXIST = -2L;

	private final Recipient mSender;
	private final Recipient mReceiver;
	// This is a cache for display purposes ONLY! Do not allow players to grab these items.
	private Map<Integer, ItemStack> mMailItems = new HashMap<>();
	// This is a local lock indicator, used to skip attempting to check
	// the real lock in Redis if we already know the result
	private final ConcurrentSkipListMap<Integer, LocalDateTime> mLockedSlots = new ConcurrentSkipListMap<>();

	public Mailbox(Recipient sender, Recipient receiver) {
		mSender = sender;
		mReceiver = receiver;
	}

	/**
	 * Returns all mailboxes sent or received by a given receiver, without caching their contents.
	 * @param recipient The recipient whose mail is being fetched
	 * @return A sorted list of relevant mailboxes
	 */
	public static CompletableFuture<List<Mailbox>> getRecipientMailboxes(Recipient recipient) {
		CompletableFuture<List<Mailbox>> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			ConcurrentSkipListSet<Mailbox> result = new ConcurrentSkipListSet<>();
			for (Map.Entry<String, String> entry : RedisAPI.getInstance().async().hgetall(recipient.redisKey(MailDirection.TO)).toCompletableFuture().join().entrySet()) {
				MMLog.fine(() -> "[Mailbox] Caching "
					+ recipient.friendlyStr(MailDirection.TO)
					+ ": "
					+ entry.getKey()
					+ ": "
					+ entry.getValue());

				long slotCount;
				try {
					slotCount = Long.parseLong(entry.getValue());
				} catch (NumberFormatException ex) {
					continue;
				}
				if (slotCount <= 0) {
					continue;
				}

				Recipient testSender = Recipient.of(entry.getKey()).join();
				if (testSender == null) {
					continue;
				}

				Mailbox redisMailbox = new Mailbox(testSender, recipient);
				result.add(MailMan.getOrRegister(redisMailbox));
			}

			for (Map.Entry<String, String> entry : RedisAPI.getInstance().async().hgetall(recipient.redisKey(MailDirection.FROM)).toCompletableFuture().join().entrySet()) {
				MMLog.fine(() -> "[Mailbox] Caching "
					+ recipient.friendlyStr(MailDirection.FROM)
					+ ": "
					+ entry.getKey()
					+ ": "
					+ entry.getValue());

				long slotCount;
				try {
					slotCount = Long.parseLong(entry.getValue());
				} catch (NumberFormatException ex) {
					continue;
				}
				if (slotCount <= 0) {
					continue;
				}

				Recipient testRecipient = Recipient.of(entry.getKey()).join();
				if (testRecipient == null) {
					continue;
				}

				Mailbox redisMailbox = new Mailbox(recipient, testRecipient);
				result.add(MailMan.getOrRegister(redisMailbox));
			}

			future.complete(new ArrayList<>(result));
		});

		return future;
	}

	public static CompletableFuture<Mailbox> fromJson(JsonObject mailboxJson) {
		CompletableFuture<Mailbox> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Recipient sender = Recipient.fromJson(mailboxJson.getAsJsonObject("sender")).join();
				if (sender == null) {
					throw new Exception("Could not get sender type");
				}

				Recipient receiver = Recipient.fromJson(mailboxJson.getAsJsonObject("receiver")).join();
				if (receiver == null) {
					throw new Exception("Could not get receiver type");
				}

				future.complete(new Mailbox(sender, receiver));
			} catch (Throwable throwable) {
				future.completeExceptionally(throwable);
			}
		});

		return future;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.add("sender", mSender.toJson());
		result.add("receiver", mReceiver.toJson());
		return result;
	}

	public Recipient sender() {
		return mSender;
	}

	public ItemStack senderIcon() {
		return mSender.icon(MailDirection.FROM);
	}

	public Recipient receiver() {
		return mReceiver;
	}

	public ItemStack receiverIcon() {
		return mReceiver.icon(MailDirection.TO);
	}

	public Set<Recipient> participants() {
		// Set.of() doesn't allow two of the same object; this does
		Set<Recipient> result = new HashSet<>();
		result.add(mSender);
		result.add(mReceiver);
		return result;
	}

	public Component friendlyName() {
		return Component.text("Mailbox ", NamedTextColor.DARK_GRAY)
			.decoration(TextDecoration.ITALIC, false)
			.append(sender().friendlyComponent(MailDirection.FROM))
			.append(Component.space())
			.append(receiver().friendlyComponent(MailDirection.TO));
	}

	public ItemStack mailboxIcon() {
		return GUIUtils.createBasicItem(Material.ENDER_CHEST, friendlyName());
	}

	/**
	 * Refreshes the mailbox content cache. Remember to only get or add items using swapItemInSlot.
	 * @return A future that will be completed when the contents are updated.
	 */
	public CompletableFuture<Void> refreshContents() {
		CompletableFuture<Void> future = new CompletableFuture<>();

		String itemSlotRedisKey = itemSlotMapRedisKey();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			Map<Integer, ItemStack> result = new TreeMap<>();

			Map<String, String> rawMailboxItems;
			try {
				rawMailboxItems = RedisAPI.getInstance().async().hgetall(itemSlotRedisKey).toCompletableFuture().join();
				if (rawMailboxItems == null) {
					throw new Exception("Unable to load mail");
				}
			} catch (Exception ex) {
				future.completeExceptionally(ex);
				return;
			}

			for (Map.Entry<String, String> entry : rawMailboxItems.entrySet()) {
				int slot;
				try {
					slot = Integer.parseInt(entry.getKey());
				} catch (Exception ex) {
					MMLog.warning("[Mailbox] Non-numeric slot ID in " + itemSlotRedisKey + ": " + entry.getKey() + ": "
						+ ex.getMessage());
					continue;
				}

				JsonObject slotJson;
				try {
					slotJson = new Gson().fromJson(entry.getValue(), JsonObject.class);
				} catch (Exception ex) {
					MMLog.warning("[Mailbox] Error parsing mail json (" + itemSlotRedisKey + ", " + entry.getKey() + "): "
						+ ex.getMessage());
					MMLog.warning(entry.getValue());
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
					continue;
				}

				MailboxSlot mailboxSlot;
				try {
					mailboxSlot = new MailboxSlot(slotJson);
				} catch (NullPointerException ex) {
					MMLog.warning("[Mailbox] NPE processing mail slot json: (" + itemSlotRedisKey + ", " + entry.getKey() + "): "
						+ ex.getMessage());
					MMLog.warning(entry.getValue());
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
					continue;
				}

				result.put(slot, mailboxSlot.getItem());
			}

			mMailItems = result;
			future.complete(null);
		});

		return future;
	}

	public CompletableFuture<Void> refreshSlot(int slot) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		String itemSlotRedisKey = itemSlotMapRedisKey();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				String itemSlotJson = RedisAPI.getInstance().async().hget(itemSlotRedisKey, Long.toString(slot)).toCompletableFuture().join();
				JsonObject slotJson;
				slotJson = itemSlotJson == null ? null : new Gson().fromJson(itemSlotJson, JsonObject.class);

				MailboxSlot mailboxSlot;
				mailboxSlot = slotJson == null ? null : new MailboxSlot(slotJson);

				if (mailboxSlot == null) {
					mMailItems.remove(slot);
				} else {
					mMailItems.put(slot, mailboxSlot.getItem());
				}

				loadLockFromRedis(slot).join();

				future.complete(null);
			} catch (Throwable throwable) {
				MMLog.warning("[Mailbox] Failed to refresh " + friendlyName() + " slot: " + slot);
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), throwable);
				future.completeExceptionally(throwable);
			}
		});

		return future;
	}

	/**
	 * Gets the item fetched by refreshContents(). Remember this cache is for display purposes only.
	 * Use swapItemInSlot to get/set items instead.
	 * @param slot The slot of the mailbox to be returned
	 * @return The cached item for display purposes only
	 */
	public @Nullable ItemStack getLastKnownSlotContents(int slot) {
		if (isLocked(slot)) {
			return GUIUtils.createBasicItem(Material.BARRIER, "Please wait...!", NamedTextColor.YELLOW);
		}
		return mMailItems.get(slot);
	}

	public boolean isEmpty() {
		return mMailItems.isEmpty();
	}

	public void noAccessCheck(Player viewer) throws NoMailAccessException {
		if (sender().nonMemberCheck(viewer) && receiver().nonMemberCheck(viewer)) {
			throw new NoMailAccessException("You are not a participant of this mailbox");
		}

		sender().lockedCheck(viewer);
		receiver().lockedCheck(viewer);
	}

	/**
	 * Swaps a held item with an item already in a mailbox slot
	 * Future can throw: UnacceptedItemException Could not accept newItem; return the item with a message
	 * Future can throw: NullPointerException The oldItem already in the mailbox could not be processed and was left in place; return newItem
	 *
	 * @param slot    Which slot of the mailbox to swap with
	 * @param newItem Which item to put in that slot
	 * @return A future list of items now local to this shard. Returns all items that should be given to the player
	 */
	public CompletableFuture<List<ItemStack>> swapItemInSlot(Player player, int slot, @Nullable ItemStack newItem) {
		CompletableFuture<List<ItemStack>> future = new CompletableFuture<>();
		if (!MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, "Mailbox")) {
			future.completeExceptionally(new UnacceptedItemException("You're clicking too fast!"));
			return future;
		}

		String timeUntilOffCooldown = MailMan.addInteractionCooldown(player);
		if (timeUntilOffCooldown != null) {
			future.completeExceptionally(
				new LockException("You may only make "
					+ MailMan.INTERACTIONS_PER_TIME_LIMIT
					+ " transactions every "
					+ MailMan.INTERACTION_TIME_LIMIT_MINUTES
					+ " minutes. Please wait "
					+ timeUntilOffCooldown
				));
			return future;
		}

		String newItemJsonStr;
		if (newItem == null || newItem.getType().equals(Material.AIR) || newItem.getAmount() == 0) {
			newItemJsonStr = null;
		} else {
			try {
				MailboxSlot newItemSlot = new MailboxSlot(player, newItem);
				newItemJsonStr = newItemSlot.toJson().toString();
			} catch (UnacceptedItemException ex) {
				future.completeExceptionally(ex);
				return future;
			}
		}

		String newItemMovementDescription = MailboxSlot.describeNestedItem(newItem);
		if (newItemMovementDescription.length() > MAX_LOG_DESCRIPTION_LENGTH) {
			future.completeExceptionally(new UnacceptedItemException("Too many nested item types to mail this"));
			return future;
		}

		if (isLocked(slot)) {
			future.completeExceptionally(new LockException("That slot is in use, try again in a few seconds."));
			return future;
		}

		mLockedSlots.put(slot, DateUtils.trueUtcDateTime().plus(LOCK_TIMEOUT_MS, ChronoUnit.MILLIS));

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			if (!ItemUtils.isNullOrAir(newItem)) {
				MailCache senderCache = MailMan.recipientMailCache(sender());
				senderCache.awaitInitialization().join();

				MailCache receiverCache = MailMan.recipientMailCache(receiver());
				receiverCache.awaitInitialization().join();

				if (senderCache.failsBlockAllowLists(receiver())) {
					future.completeExceptionally(new UnacceptedItemException(
						sender().friendlyStr(MailDirection.DEFAULT)
							+ " is not accepting mail from "
							+ receiver().friendlyStr(MailDirection.DEFAULT)
					));
					return;
				}

				if (receiverCache.failsBlockAllowLists(sender())) {
					future.completeExceptionally(new UnacceptedItemException(
						receiver().friendlyStr(MailDirection.DEFAULT)
							+ " is not accepting mail from "
							+ sender().friendlyStr(MailDirection.DEFAULT)
					));
					return;
				}

				int totalMailboxesSent = senderCache.totalMailboxesSent();
				if (isEmpty()) {
					totalMailboxesSent++;
				}
				int sentLimit = sender().sentMailboxLimit();
				if (totalMailboxesSent > sentLimit) {
					future.completeExceptionally(new UnacceptedItemException(
						sender().friendlyStr(MailDirection.DEFAULT)
							+ " cannot send mail to more than "
							+ sentLimit + " recipients!"
					));
					return;
				}

				int totalMailboxesReceived = receiverCache.totalMailboxesReceived();
				if (isEmpty()) {
					totalMailboxesReceived++;
				}
				int receivedLimit = mReceiver.receivedMailboxLimit();
				if (totalMailboxesReceived > receivedLimit) {
					future.completeExceptionally(new UnacceptedItemException(
						receiver().friendlyStr(MailDirection.DEFAULT)
							+ " has too much unread mail!"
					));
					return;
				}
			}

			String itemSlotRedisKey = itemSlotMapRedisKey();
			String slotStr = "" + slot;
			long deltaItemSlots = 0L;

			String lockId;
			try {
				lockId = claimLock(slot).join();
			} catch (LockException ex) {
				future.completeExceptionally(ex);
				return;
			}

			String oldItemJsonStr;
			try {
				oldItemJsonStr = RedisAPI.getInstance().async().hget(itemSlotRedisKey, slotStr).toCompletableFuture().join();
			} catch (Throwable throwable) {
				try {
					freeLock(slot, lockId, throwable).join();
					future.completeExceptionally(throwable);
				} catch (LockException ex2) {
					MMLog.warning("[Mailbox] Caught LockException while freeing a lock due to another exception:");
					future.completeExceptionally(ex2);
				}
				return;
			}

			JsonObject oldItemJson;
			if (oldItemJsonStr == null) {
				oldItemJson = null;
			} else {
				try {
					oldItemJson = new Gson().fromJson(oldItemJsonStr, JsonObject.class);
				} catch (Exception ex) {
					MMLog.warning("[Mailbox] Error parsing mail json ("
						+ itemSlotRedisKey + ", "
						+ slotStr + "): "
						+ ex.getMessage());
					MMLog.warning(oldItemJsonStr);
					future.completeExceptionally(ex);
					return;
				}
			}

			ItemStack oldItemStack;
			if (oldItemJson == null) {
				oldItemStack = null;
			} else {
				try {
					oldItemStack = new MailboxSlot(oldItemJson).getItem();
				} catch (NullPointerException ex) {
					MMLog.warning("[Mailbox] NPE processing mail slot json: (" + itemSlotRedisKey + ", " + slotStr + "): "
						+ ex.getMessage());
					MMLog.warning(oldItemJsonStr);
					try {
						freeLock(slot, lockId, ex).join();
					} catch (LockException ex2) {
						MMLog.warning("[Mailbox] Caught LockException while freeing a lock due to another exception:");
						future.completeExceptionally(ex2);
						return;
					}
					future.completeExceptionally(ex);
					return;
				}
			}

			if (oldItemStack != null) {
				try {
					long deletedCount = RedisAPI.getInstance().async().hdel(itemSlotRedisKey, slotStr).toCompletableFuture().join();
					if (deletedCount != 1) {
						throw new Exception("Deleted " + deletedCount + " items instead of 1 item");
					}
					deltaItemSlots--;

					String oldItemMovementDescription = MailboxSlot.describeNestedItem(oldItemStack);
					if (!oldItemMovementDescription.isBlank()) {
						AuditListener.logMail(String.format("[Mailbox %s %s] %s removed item %s",
							sender().friendlyStr(MailDirection.FROM),
							receiver().friendlyStr(MailDirection.TO),
							player.getName(),
							oldItemMovementDescription
						));
					}
				} catch (Exception ex) {
					MMLog.warning("[Mailbox] Error deleting existing item at (" + itemSlotRedisKey + ", " + slotStr + "); aborting item swap: "
						+ ex.getMessage());
					updateMailSlotCounts(deltaItemSlots).join();
					try {
						freeLock(slot, lockId, ex).join();
					} catch (LockException ex2) {
						MMLog.warning("[Mailbox] Caught LockException while freeing a lock due to another exception:");
						future.completeExceptionally(ex2);
						return;
					}
					future.completeExceptionally(ex);
					return;
				}
			}

			if (newItemJsonStr != null) {
				try {
					if (!RedisAPI.getInstance().async().hset(itemSlotRedisKey, slotStr, newItemJsonStr).toCompletableFuture().join()) {
						throw new Exception("Failed to set value; an existing value may be present");
					}
					deltaItemSlots++;

					if (!newItemMovementDescription.isBlank()) {
						AuditListener.logMail(String.format("[Mailbox %s %s] %s added item %s",
							sender().friendlyStr(MailDirection.FROM),
							receiver().friendlyStr(MailDirection.TO),
							player.getName(),
							newItemMovementDescription
						));
					}
				} catch (Exception ex) {
					MMLog.warning("[Mailbox] Error setting replacement item at (" + itemSlotRedisKey + ", " + slotStr + "): "
						+ ex.getMessage());
					updateMailSlotCounts(deltaItemSlots).join();

					try {
						freeLock(slot, lockId, ex).join();
					} catch (LockException ex2) {
						MMLog.warning("[Mailbox] Caught LockException while freeing a lock due to another exception:");
						MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex2);
					}

					// Redis contains neither item - need to return both
					List<ItemStack> result = new ArrayList<>();
					if (oldItemStack != null) {
						result.add(oldItemStack);
					}
					result.add(newItem);
					future.complete(result);
					return;
				}
			}

			try {
				freeLock(slot, lockId).join();
			} catch (LockException ex) {
				MMLog.warning("[Mailbox] Caught LockException while freeing a lock, but the lock is the only issue:");
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			}

			List<ItemStack> result = new ArrayList<>();
			if (oldItemStack != null) {
				result.add(oldItemStack);
			}

			updateMailSlotCounts(deltaItemSlots).join();
			MailMan.broadcastMailSlotChange(this, slot);

			future.complete(result);
		});

		return future;
	}

	/**
	 * Updates the number of item slots filled for a given mailbox
	 * @param deltaItemSlots +1 if an item was added, -1 if an item was removed, 0 if the count is the same
	 */
	public CompletableFuture<Void> updateMailSlotCounts(long deltaItemSlots) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (deltaItemSlots == 0) {
			future.complete(null);
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			RedisAPI.getInstance().async().multi();
			RedisAPI.getInstance().async().hincrby(receiver().redisKey(MailDirection.TO), sender().redisKey(MailDirection.DEFAULT), deltaItemSlots);
			RedisAPI.getInstance().async().hincrby(sender().redisKey(MailDirection.FROM), receiver().redisKey(MailDirection.DEFAULT), deltaItemSlots);
			RedisAPI.getInstance().async().exec();

			future.complete(null);
		});

		return future;
	}

	public static String redisKeyPrefix(String subKey) {
		return ConfigAPI.getServerDomain() + ":mailbox:" + subKey + ":";
	}

	public String itemSlotMapRedisKey() {
		return redisKeyPrefix("slots")
			+ receiver().redisKey(MailDirection.DEFAULT) + ":"
			+ sender().redisKey(MailDirection.DEFAULT);
	}

	protected String lockSlotMapRedisKey(int slot) {
		return redisKeyPrefix("lock")
			+ receiver().redisKey(MailDirection.DEFAULT) + ":"
			+ sender().redisKey(MailDirection.DEFAULT) + ":"
			+ slot;
	}

	public int mailboxCompareTo(@NotNull Mailbox o) {
		int result;

		result = mSender.mailboxCompareTo(o.mSender);
		if (result != 0) {
			return result;
		}

		return mReceiver.mailboxCompareTo(o.mReceiver);
	}

	private CompletableFuture<Void> loadLockFromRedis(int slot) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		String lockSlotRedisKey = lockSlotMapRedisKey(slot);

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				long lockExpirationMs = RedisAPI.getInstance().async().pexpiretime(lockSlotRedisKey).toCompletableFuture().join();

				if (lockExpirationMs == PEXPIRETIME_EXISTS_NO_EXPIRATION) {
					lockExpirationMs = DateUtils.TRUE_EPOCH.until(DateUtils.trueUtcDateTime(), ChronoUnit.MILLIS)
						+ LOCK_TIMEOUT_MS;
				}

				if (lockExpirationMs == PEXPIRETIME_DOES_NOT_EXIST) {
					mLockedSlots.remove(slot);
				} else {
					mLockedSlots.put(slot, DateUtils.TRUE_EPOCH.plus(lockExpirationMs, ChronoUnit.MILLIS));
				}
				future.complete(null);
			} catch (Throwable throwable) {
				MMLog.warning("[Mailbox] Failed to load lock " + friendlyName() + " slot: " + slot);
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), throwable);
				future.completeExceptionally(throwable);
			}
		});

		return future;
	}

	/**
	 * Checks if a given mailbox slot is currently known to be being updated.
	 * This can still return false if it is being updated, but the local cache is not aware of this.
	 * It is always safe to use swapItemInSlot to attempt to update a slot, though if this returns true,
	 * then the attempt will most likely be denied.
	 * @param slot The slot to check
	 * @return true if the slot is known to be locked, otherwise false
	 */
	public boolean isLocked(int slot) {
		LocalDateTime expiryTime = mLockedSlots.get(slot);
		if (expiryTime == null) {
			return false;
		}

		if (expiryTime.isAfter(DateUtils.trueUtcDateTime())) {
			return true;
		}

		mLockedSlots.remove(slot);
		MMLog.severe("[Mailbox] Mailbox lock expired without being properly handled!");
		return false;
	}

	/**
	 * Attempts to claim a lock on a given mailbox's slot.
	 * This claim will expire automatically should the attempt to modify be interrupted.
	 * Based on <a href="https://redis.io/docs/latest/develop/use/patterns/distributed-locks/#correct-implementation-with-a-single-instance">the official Redis implementation</a>
	 * @param slot The slot to be claimed.
	 * @return A CompletableFuture with the lock ID string required to later release the lock
	 * @throws LockException in the event the lock was not claimed successfully, including a reason for the issue.
	 */
	private CompletableFuture<String> claimLock(int slot) throws LockException {
		CompletableFuture<String> future = new CompletableFuture<>();

		String redisKey = lockSlotMapRedisKey(slot);
		String shardPrefix = ServerProperties.getShardName().toLowerCase(Locale.ROOT) + ":";
		String lockId = shardPrefix + UUID.randomUUID();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			mLockedSlots.put(slot, DateUtils.trueUtcDateTime().plus(LOCK_TIMEOUT_MS, ChronoUnit.MILLIS));
			String response = RedisAPI.getInstance().async()
				.set(redisKey, lockId, new SetArgs().nx().px(LOCK_TIMEOUT_MS)).toCompletableFuture().join();
			if ("OK".equals(response)) {
				MailMan.broadcastMailSlotChange(this, slot);
				future.complete(lockId);
			} else {
				mLockedSlots.remove(slot);
				MMLog.warning("[Mailbox] Failed to get lock; Redis response to set command: " + response);
				future.completeExceptionally(new LockException("There's a lock on that slot! Try again."));
			}
		});

		return future;
	}

	private CompletableFuture<Void> freeLock(int slot, String lockId) throws LockException {
		return freeLock(slot, lockId, null);
	}

	private CompletableFuture<Void> freeLock(int slot, String lockId, @Nullable Throwable previousException) throws LockException {
		CompletableFuture<Void> future = new CompletableFuture<>();

		String redisKey = lockSlotMapRedisKey(slot);

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			String response = RedisAPI.getInstance().async().get(redisKey).toCompletableFuture().join();
			if (response == null) {
				mLockedSlots.remove(slot);
				future.completeExceptionally(
					LockException.withOptCause(
						"The lock expired before it was freed!",
						previousException
					));
				return;
			}
			if (!lockId.equals(response)) {
				mLockedSlots.remove(slot);
				future.completeExceptionally(
					LockException.withOptCause(
						"The lock claim was replaced before it was expected!",
						previousException
					));
				return;
			}

			long deletedCount = RedisAPI.getInstance().async().del(redisKey).toCompletableFuture().join();
			if (deletedCount != 1L) {
				mLockedSlots.remove(slot);
				MailMan.broadcastMailSlotChange(this, slot);
				future.completeExceptionally(
					LockException.withOptCause(
						"Failed to remove lock claim! Maybe it expired just as we were done with it?",
						previousException
					)
				);
				return;
			}
			mLockedSlots.remove(slot);
			MailMan.broadcastMailSlotChange(this, slot);
			future.complete(null);
		});

		return future;
	}

	@Override
	public int compareTo(@NotNull Mailbox o) {
		int result;

		result = mSender.compareTo(o.mSender);
		if (result != 0) {
			return result;
		}

		return mReceiver.compareTo(o.mReceiver);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Mailbox other)) {
			return false;
		}
		return mSender.equals(other.mSender)
			&& mReceiver.equals(other.mReceiver);
	}

	@Override
	public int hashCode() {
		int result = mSender.hashCode();
		result = 31 * result + mReceiver.hashCode();
		return result;
	}
}
