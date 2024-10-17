package com.playmonumenta.plugins.mail;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.redissync.RedisAPI;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * A cache of mail sent and received by a recipient; this is loaded asynchronously on demand, loading all relevant mailboxes
 * and their items from Redis. Note that the cache is only used to preview items in mailboxes.
 * In the event of a desync, the Mailbox class will prevent the duplication or destruction of items.
 */
public class MailCache {
	private final Recipient mRecipient;
	private final CompletableFuture<Void> mInitializationFuture = new CompletableFuture<>();
	private ConcurrentSkipListSet<Recipient> mRecipientAllowList;
	private ConcurrentSkipListSet<Recipient> mRecipientBlockList;
	private ConcurrentSkipListSet<Mailbox> mRelevantMailboxes;
	private ConcurrentSkipListMap<Recipient, Mailbox> mSentMailboxes;
	private ConcurrentSkipListMap<Recipient, Mailbox> mReceivedMailboxes;

	public MailCache(Recipient recipient) {
		mRecipient = recipient;
		mRecipientAllowList = new ConcurrentSkipListSet<>(Recipient::mailboxCompareTo);
		mRecipientBlockList = new ConcurrentSkipListSet<>(Recipient::mailboxCompareTo);
		mRelevantMailboxes = new ConcurrentSkipListSet<>(Mailbox::mailboxCompareTo);
		mSentMailboxes = new ConcurrentSkipListMap<>(Recipient::mailboxCompareTo);
		mReceivedMailboxes = new ConcurrentSkipListMap<>(Recipient::mailboxCompareTo);
		MMLog.fine(() -> "[Mailbox] Created mail cache for " + recipient.friendlyStr(MailDirection.DEFAULT));

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			RedisCommands<String, String> redis = RedisAPI.getInstance().sync();

			ConcurrentSkipListSet<Recipient> allowList = new ConcurrentSkipListSet<>(Recipient::mailboxCompareTo);
			ConcurrentSkipListSet<Recipient> blockList = new ConcurrentSkipListSet<>(Recipient::mailboxCompareTo);
			ConcurrentSkipListSet<Recipient> allowedAndBlocked = new ConcurrentSkipListSet<>();

			for (String senderKey : redis.smembers(mRecipient.allowListRedisKey())) {
				Recipient testSender = Recipient.of(senderKey).join();
				if (testSender == null) {
					continue;
				}
				allowList.add(testSender);
			}

			for (String senderKey : redis.smembers(mRecipient.blockListRedisKey())) {
				Recipient testSender = Recipient.of(senderKey).join();
				if (testSender == null) {
					continue;
				}
				blockList.add(testSender);
				if (allowList.remove(testSender)) {
					allowedAndBlocked.add(testSender);
				}
			}

			ConcurrentSkipListSet<Mailbox> relevantMailboxes
				= new ConcurrentSkipListSet<>(Mailbox::mailboxCompareTo);
			ConcurrentSkipListMap<Recipient, Mailbox> sentMailboxes
				= new ConcurrentSkipListMap<>(Recipient::mailboxCompareTo);
			ConcurrentSkipListMap<Recipient, Mailbox> receivedMailboxes
				= new ConcurrentSkipListMap<>(Recipient::mailboxCompareTo);

			List<Mailbox> testMailboxes = Mailbox.getRecipientMailboxes(mRecipient).join();

			for (Mailbox mailbox : testMailboxes) {
				// Ensure this mailbox is relevant to this sender
				if (!mailbox.participants().contains(recipient)) {
					continue;
				}

				// Cache mail contents
				try {
					mailbox.refreshContents().join();
				} catch (Exception ex) {
					MMLog.warning("[Mailbox] Failed to get mail for mailbox " + MessagingUtils.plainText(mailbox.friendlyName()));
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
					continue;
				}

				// Skip if empty
				if (mailbox.isEmpty()) {
					continue;
				}

				relevantMailboxes.add(mailbox);
				if (recipient.equals(mailbox.sender())) {
					sentMailboxes.put(mailbox.receiver(), mailbox);
				}
				if (recipient.equals(mailbox.receiver())) {
					receivedMailboxes.put(mailbox.sender(), mailbox);
				}
			}

			mRecipientAllowList = allowList;
			mRecipientBlockList = blockList;
			mRelevantMailboxes = relevantMailboxes;
			mSentMailboxes = sentMailboxes;
			mReceivedMailboxes = receivedMailboxes;

			for (Recipient erroneousRecipient : allowedAndBlocked) {
				allowListRemove(erroneousRecipient, false).join();
			}

			if (totalMailboxesReceived() > 0) {
				showGotMailMessage();
			}

			MMLog.fine(() -> "[Mailbox] Finished loading mail cache for " + recipient.friendlyStr(MailDirection.DEFAULT));
			mInitializationFuture.complete(null);
		});
	}

	public Recipient recipient() {
		return mRecipient;
	}

	public boolean isInitialized() {
		return mInitializationFuture.isDone();
	}

	public CompletableFuture<Void> awaitInitialization() {
		CompletableFuture<Void> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			if (mInitializationFuture.isDone()) {
				future.complete(null);
				return;
			}
			mInitializationFuture.join();
			future.complete(null);
		});

		return future;
	}

	public ConcurrentSkipListSet<Recipient> recipientBlockAllowList(BlockAllowListType listType) {
		switch (listType) {
			case ALLOW -> {
				return recipientAllowList();
			}
			case BLOCK -> {
				return recipientBlockList();
			}
			default -> {
				return new ConcurrentSkipListSet<>();
			}
		}
	}

	public ConcurrentSkipListSet<Recipient> recipientAllowList() {
		return new ConcurrentSkipListSet<>(mRecipientAllowList);
	}

	public ConcurrentSkipListSet<Recipient> recipientBlockList() {
		return new ConcurrentSkipListSet<>(mRecipientBlockList);
	}

	public CompletableFuture<Void> blockAllowListAdd(BlockAllowListType listType, Recipient recipient, boolean isLocal) {
		switch (listType) {
			case ALLOW -> {
				return allowListAdd(recipient, isLocal);
			}
			case BLOCK -> {
				return blockListAdd(recipient, isLocal);
			}
			default -> {
				CompletableFuture<Void> future = new CompletableFuture<>();
				future.complete(null);
				return future;
			}
		}
	}

	public CompletableFuture<Void> allowListAdd(Recipient recipient, boolean isLocal) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (mRecipientAllowList.contains(recipient)) {
			future.complete(null);
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			RedisCommands<String, String> redis = RedisAPI.getInstance().sync();
			redis.srem(mRecipient.blockListRedisKey(), recipient.redisKey(MailDirection.DEFAULT));
			mRecipientBlockList.remove(recipient);
			redis.sadd(mRecipient.allowListRedisKey(), recipient.redisKey(MailDirection.DEFAULT));
			mRecipientAllowList.add(recipient);

			if (isLocal) {
				MailMan.broadcastBlockAllowListChange(mRecipient, BlockAllowListType.ALLOW, recipient, true);
			}

			future.complete(null);
		});

		return future;
	}

	public CompletableFuture<Void> blockListAdd(Recipient recipient, boolean isLocal) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (mRecipientBlockList.contains(recipient)) {
			future.complete(null);
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			RedisCommands<String, String> redis = RedisAPI.getInstance().sync();
			redis.srem(mRecipient.allowListRedisKey(), recipient.redisKey(MailDirection.DEFAULT));
			mRecipientAllowList.remove(recipient);
			redis.sadd(mRecipient.blockListRedisKey(), recipient.redisKey(MailDirection.DEFAULT));
			mRecipientBlockList.add(recipient);

			if (isLocal) {
				MailMan.broadcastBlockAllowListChange(mRecipient, BlockAllowListType.BLOCK, recipient, true);
			}

			future.complete(null);
		});

		return future;
	}

	public CompletableFuture<Void> blockAllowListRemove(BlockAllowListType listType, Recipient recipient, boolean isLocal) {
		switch (listType) {
			case ALLOW -> {
				return allowListRemove(recipient, isLocal);
			}
			case BLOCK -> {
				return blockListRemove(recipient, isLocal);
			}
			default -> {
				CompletableFuture<Void> future = new CompletableFuture<>();
				future.complete(null);
				return future;
			}
		}
	}

	public CompletableFuture<Void> allowListRemove(Recipient recipient, boolean isLocal) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (!mRecipientAllowList.contains(recipient)) {
			future.complete(null);
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			RedisCommands<String, String> redis = RedisAPI.getInstance().sync();
			redis.srem(mRecipient.allowListRedisKey(), recipient.redisKey(MailDirection.DEFAULT));
			mRecipientAllowList.remove(recipient);

			if (isLocal) {
				MailMan.broadcastBlockAllowListChange(mRecipient, BlockAllowListType.ALLOW, recipient, false);
			}

			future.complete(null);
		});

		return future;
	}

	public CompletableFuture<Void> blockListRemove(Recipient recipient, boolean isLocal) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (!mRecipientBlockList.contains(recipient)) {
			future.complete(null);
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			RedisCommands<String, String> redis = RedisAPI.getInstance().sync();
			redis.srem(mRecipient.blockListRedisKey(), recipient.redisKey(MailDirection.DEFAULT));
			mRecipientBlockList.remove(recipient);

			if (isLocal) {
				MailMan.broadcastBlockAllowListChange(mRecipient, BlockAllowListType.BLOCK, recipient, false);
			}

			future.complete(null);
		});

		return future;
	}

	public boolean failsBlockAllowLists(Recipient sender) {
		return failsBlockAllowLists(mRecipient, sender, mRecipientAllowList, mRecipientBlockList);
	}

	public static boolean failsBlockAllowLists(
		Recipient receiver,
		Recipient sender,
		ConcurrentSkipListSet<Recipient> allowList,
		ConcurrentSkipListSet<Recipient> blockList
	) {
		if (sender.equals(receiver)) {
			return false;
		}

		if (blockList.contains(sender)) {
			return true;
		}

		if (
			!allowList.isEmpty()
		) {
			return !allowList.contains(sender);
		}

		return false;
	}

	/**
	 * Registers a mailbox, returning the already registered mailbox instead if it exists
	 * @param mailbox A mailbox to register
	 * @return The stored mailbox, whether new or old
	 */
	public Mailbox registerMailbox(Mailbox mailbox) {
		if (!mailbox.participants().contains(mRecipient)) {
			// Not relevant to this cache's recipient - return as-is without updating cache
			return mailbox;
		}

		// Only adds the mailbox if it's not already present
		if (!mRelevantMailboxes.add(mailbox)) {
			// Gets the actually stored mailbox instead; sets have no .get(k) method
			Mailbox storedMailbox = mRelevantMailboxes.floor(mailbox);
			// Double check it wasn't changed out from under us before returning:
			if (mailbox.equals(storedMailbox)) {
				return storedMailbox;
			}
			// Try again?
			return registerMailbox(mailbox);
		}

		// Add the mailbox in whichever places it belongs
		if (mRecipient.equals(mailbox.sender())) {
			mSentMailboxes.put(mailbox.receiver(), mailbox);
		}
		if (mRecipient.equals(mailbox.receiver())) {
			if (totalMailboxesReceived() == 0 && !mailbox.isEmpty()) {
				showGotMailMessage();
			}
			mReceivedMailboxes.put(mailbox.sender(), mailbox);
		}
		return mailbox;
	}

	/**
	 * Get mail sent by this player
	 * @return a sorted list of sent mailboxes
	 */
	public List<Mailbox> sentMailboxes() {
		return new ArrayList<>(mSentMailboxes.values());
	}

	public int totalMailboxesSent() {
		int total = 0;
		for (Mailbox mailbox : sentMailboxes()) {
			if (!mailbox.isEmpty()) {
				total++;
			}
		}
		return total;
	}

	/**
	 * Get mail received by this player
	 * @return a sorted list of received mailboxes
	 */
	public List<Mailbox> receivedMailboxes() {
		List<Mailbox> result = new ArrayList<>();
		for (Map.Entry<Recipient, Mailbox> entry : mReceivedMailboxes.entrySet()) {
			Recipient sender = entry.getKey();
			if (failsBlockAllowLists(sender)) {
				continue;
			}
			result.add(entry.getValue());
		}
		return result;
	}

	public int totalMailboxesReceived() {
		int total = 0;
		for (Mailbox mailbox : receivedMailboxes()) {
			if (!mailbox.isEmpty()) {
				total++;
			}
		}
		return total;
	}

	public List<Mailbox> allMailboxes() {
		List<Mailbox> result = receivedMailboxes();
		// Remove duplicate
		result.removeIf(mailbox -> mailbox.sender().equals(mailbox.receiver()));
		result.addAll(sentMailboxes());
		return result;
	}

	public List<Mailbox> mailboxes(MailDirection mailDirection) {
		return switch (mailDirection) {
			case TO -> receivedMailboxes();
			case FROM -> sentMailboxes();
			default -> allMailboxes();
		};
	}

	public List<Mailbox> mailboxes(MailGuiSettings mailGuiSettings) {
		List<Mailbox> result = new ArrayList<>();

		for (Mailbox mailbox : mailboxes(mailGuiSettings.mDirection)) {
			Set<Recipient> participants = mailbox.participants();
			participants.remove(mRecipient);

			// You can send mail to yourself, in which case the set is empty
			Recipient other = mRecipient;
			for (Recipient recipient : participants) {
				other = recipient;
				break;
			}

			if (mailGuiSettings.mRecipientTypes.contains(other.recipientType())) {
				result.add(mailbox);
			}
		}

		return result;
	}

	public void showGotMailMessage() {
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			String mailboxCommand = mRecipient.recipientType().mailboxCommand();
			if (mailboxCommand != null) {
				mRecipient.audience().forEachAudience(audience -> {
					if (!(audience instanceof Player player)) {
						return;
					}
					if (!MetadataUtils.checkOnceInRecentTicks(
						Plugin.getInstance(),
						player,
						"GotMail:" + mRecipient.redisKey(MailDirection.DEFAULT),
						1200
					)) {
						return;
					}
					player.sendMessage(Component.text("[", NamedTextColor.LIGHT_PURPLE)
						.decoration(TextDecoration.ITALIC, false)
						.clickEvent(ClickEvent.runCommand(mailboxCommand))
						.append(mRecipient.friendlyComponent(MailDirection.DEFAULT))
						.append(Component.text(" got mail]")));
				});
			}
		});
	}
}
