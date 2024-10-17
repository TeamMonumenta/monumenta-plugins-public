package com.playmonumenta.plugins.mail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
import com.playmonumenta.plugins.itemstats.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.itemstats.enums.Masterwork;
import com.playmonumenta.plugins.managers.LootboxManager;
import com.playmonumenta.plugins.market.RedisItemDatabase;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beehive;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.JukeboxInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class MailboxSlot {
	public static class NestedItemLog {
		private final List<String> mItemDescriptionsOrder = new ArrayList<>();
		private final Map<String, Long> mItemAmounts = new TreeMap<>();

		public void add(@Nullable ItemStack item) {
			if (item == null) {
				return;
			}

			String description = describeItem(item);
			if (description == null) {
				return;
			}

			long amount = mItemAmounts.getOrDefault(description, 0L);
			if (amount == 0) {
				mItemDescriptionsOrder.add(description);
			}

			AtomicLong virtualAmount = new AtomicLong(1L);

			NBT.modify(item, nbt -> {
				ReadWriteNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
				if (playerModified != null && playerModified.hasTag(CustomContainerItemManager.AMOUNT_KEY)) {
					virtualAmount.set(playerModified.getLong(CustomContainerItemManager.AMOUNT_KEY));
				}
			});

			amount += item.getAmount() * virtualAmount.get();
			mItemAmounts.put(description, amount);
		}

		public String fullDescription() {
			boolean addSeparator = false;

			StringBuilder builder = new StringBuilder();
			for (String description : mItemDescriptionsOrder) {
				if (addSeparator) {
					builder.append(", ");
				}
				addSeparator = true;

				long amount = mItemAmounts.getOrDefault(description, 0L);
				if (amount == 0L) {
					continue;
				}

				builder.append(amount).append("x ").append(description);
			}

			return builder.toString();
		}

		public @Nullable String describeItem(@Nullable ItemStack item) {
			if (ItemUtils.isNullOrAir(item)) {
				return null;
			}

			String itemId = item.getType().key().toString().replaceAll("^minecraft:", "");
			ItemMeta meta = item.getItemMeta();
			Component nameComponent = meta == null ? null : meta.displayName();
			if (nameComponent == null && meta instanceof BookMeta bookMeta) {
				nameComponent = bookMeta.title();
			}
			String name = nameComponent == null ? " (unnamed)" : (" named "
				+ CommandUtils.alwaysQuote(MessagingUtils.plainText(nameComponent)));
			Masterwork masterwork = ItemStatUtils.getMasterwork(item);
			String masterworkDescription = Masterwork.NONE.equals(masterwork) ? "" : (" at Masterwork " + masterwork.getName());

			return itemId + name + masterworkDescription;
		}
	}

	private final long mItemId;
	private final int mAmount;
	private final @Nullable Long mVirtualAmount;
	// For items containing a list of items (mainly shulker boxes)
	private final List<@Nullable MailboxSlot> mVanillaContentArray = new ArrayList<>();
	// For items containing named item slots (most likely developer-created)
	private final Map<String, MailboxSlot> mVanillaContentMap = new TreeMap<>();
	// For items in Monumenta.PlayerModified.Items
	private final List<MailboxSlot> mMonumentaContentArray = new ArrayList<>();

	public MailboxSlot(Player player, ItemStack itemStack) throws UnacceptedItemException {
		unacceptedItemCheck(player, itemStack);
		mAmount = itemStack.getAmount();
		AtomicBoolean atomicVirtualAmountFound = new AtomicBoolean(false);
		AtomicLong atomicVirtualAmount = new AtomicLong(-1L);

		ItemStack reduced = itemStack.asOne();
		ItemMeta meta = reduced.getItemMeta();

		if (meta instanceof CrossbowMeta crossbowMeta) {
			for (ItemStack content : crossbowMeta.getChargedProjectiles()) {
				mVanillaContentArray.add(nullableMailboxSlot(player, content));
			}
			crossbowMeta.setChargedProjectiles(List.of());
		} else if (
			meta instanceof BlockStateMeta blockStateMeta
				&& blockStateMeta.hasBlockState()
				&& blockStateMeta.getBlockState() instanceof BlockInventoryHolder inventoryHolder
		) {
			Inventory inventory = inventoryHolder.getInventory();
			for (ItemStack content : inventory) {
				mVanillaContentArray.add(nullableMailboxSlot(player, content));
			}

			if (inventory instanceof BrewerInventory brewerInventory) {
				optPutSlotMap(player, "fuel", brewerInventory.getFuel());
				optPutSlotMap(player, "ingredient", brewerInventory.getIngredient());
			} else if (inventory instanceof FurnaceInventory furnaceInventory) {
				optPutSlotMap(player, "fuel", furnaceInventory.getFuel());
				optPutSlotMap(player, "smelting", furnaceInventory.getSmelting());
				optPutSlotMap(player, "result", furnaceInventory.getResult());
			} else if (inventory instanceof JukeboxInventory jukeboxInventory) {
				optPutSlotMap(player, "record", jukeboxInventory.getRecord());
			}

			inventory.clear();
		}
		reduced.setItemMeta(meta);

		NBT.modify(reduced, nbt -> {
			// Do not create tags if they do not exist

			if (ItemStatUtils.hasItemList(nbt)) {
				ReadWriteNBTCompoundList itemsList = ItemStatUtils.getItemList(nbt);
				if (itemsList != null && !itemsList.isEmpty()) {
					for (ReadWriteNBT contentNbt : itemsList) {
						ItemStack content = NBT.itemStackFromNBT(contentNbt);
						mMonumentaContentArray.add(new MailboxSlot(player, content));
					}
					itemsList.clear();
				}
			}

			ReadWriteNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
			if (playerModified != null) {
				if (playerModified.hasTag(CustomContainerItemManager.AMOUNT_KEY)) {
					atomicVirtualAmount.set(playerModified.getLong(CustomContainerItemManager.AMOUNT_KEY));
					atomicVirtualAmountFound.set(true);
				}
				playerModified.removeKey(CustomContainerItemManager.AMOUNT_KEY);
				if (playerModified.getKeys().isEmpty()) {
					ItemStatUtils.removePlayerModified(nbt);
				}
			}
		});

		mVirtualAmount = atomicVirtualAmountFound.get() ? atomicVirtualAmount.get() : null;
		mItemId = RedisItemDatabase.getIDFromItemStack(reduced);
	}

	public MailboxSlot(JsonObject json) throws NullPointerException {
		mItemId = json.getAsJsonPrimitive("mItemId").getAsLong();
		mAmount = json.getAsJsonPrimitive("mAmount").getAsInt();
		if (
			json.get("mVirtualAmount") instanceof JsonPrimitive virtualAmountPrimitive
				&& virtualAmountPrimitive.isNumber()
		) {
			mVirtualAmount = virtualAmountPrimitive.getAsLong();
		} else {
			mVirtualAmount = null;
		}

		if (json.get("mVanillaContentArray") instanceof JsonArray contentArray) {
			for (JsonElement content : contentArray) {
				mVanillaContentArray.add(nullableMailboxSlot(content));
			}
		}

		if (json.get("mVanillaContentMap") instanceof JsonObject contentMap) {
			for (Map.Entry<String, JsonElement> contentMapEntry : contentMap.entrySet()) {
				optPutSlotMap(contentMapEntry.getKey(), contentMapEntry.getValue());
			}
		}

		if (json.get("mMonumentaContentArray") instanceof JsonArray contentArray) {
			for (JsonElement contentElement : contentArray) {
				MailboxSlot content = nullableMailboxSlot(contentElement);
				if (content != null) {
					mMonumentaContentArray.add(content);
				}
			}
		}
	}

	public static @Nullable MailboxSlot nullableMailboxSlot(
		Player player,
		@Nullable ItemStack itemStack
	) throws UnacceptedItemException {
		if (itemStack == null) {
			return null;
		}
		return new MailboxSlot(player, itemStack);
	}

	public static @Nullable MailboxSlot nullableMailboxSlot(JsonElement json) throws NullPointerException {
		if (json instanceof JsonObject jsonObject) {
			try {
				return new MailboxSlot(jsonObject);
			} catch (NullPointerException ignored) {
				return null;
			}
		}
		return null;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.addProperty("mItemId", mItemId);
		result.addProperty("mAmount", mAmount);
		if (mVirtualAmount != null) {
			result.addProperty("mVirtualAmount", mVirtualAmount);
		}

		if (!mVanillaContentArray.isEmpty()) {
			JsonArray contents = new JsonArray();
			for (MailboxSlot content : mVanillaContentArray) {
				if (content == null) {
					contents.add(JsonNull.INSTANCE);
				} else {
					contents.add(content.toJson());
				}
			}
			result.add("mVanillaContentArray", contents);
		}

		if (!mVanillaContentMap.isEmpty()) {
			JsonObject contentMap = new JsonObject();
			for (Map.Entry<String, MailboxSlot> contentMapEntry : mVanillaContentMap.entrySet()) {
				contentMap.add(contentMapEntry.getKey(), contentMapEntry.getValue().toJson());
			}
			result.add("mVanillaContentMap", contentMap);
		}

		if (!mMonumentaContentArray.isEmpty()) {
			JsonArray contents = new JsonArray();
			for (MailboxSlot content : mMonumentaContentArray) {
				if (content == null) {
					contents.add(JsonNull.INSTANCE);
				} else {
					contents.add(content.toJson());
				}
			}
			result.add("mMonumentaContentArray", contents);
		}

		return result;
	}

	public ItemStack getItem() {
		ItemStack result = RedisItemDatabase.getItemStackFromID(mItemId).asQuantity(mAmount);

		ItemMeta meta = result.getItemMeta();
		if (!mVanillaContentArray.isEmpty() || !mVanillaContentMap.isEmpty()) {
			if (meta instanceof CrossbowMeta crossbowMeta) {
				for (MailboxSlot projectile : mVanillaContentArray) {
					if (projectile != null) {
						crossbowMeta.addChargedProjectile(projectile.getItem());
					}
				}
			}
			if (
				meta instanceof BlockStateMeta blockStateMeta
					&& blockStateMeta.hasBlockState()
					&& blockStateMeta.getBlockState() instanceof BlockInventoryHolder inventoryHolder
			) {
				Inventory inventory = inventoryHolder.getInventory();

				for (int i = 0; i < mVanillaContentArray.size(); i++) {
					MailboxSlot content = mVanillaContentArray.get(i);
					if (content != null) {
						inventory.setItem(i, content.getItem());
					}
				}

				ItemStack content;
				if (inventory instanceof BrewerInventory brewerInventory) {
					if ((content = contentMapGet("fuel")) != null) {
						brewerInventory.setFuel(content);
					}
					if ((content = contentMapGet("ingredient")) != null) {
						brewerInventory.setIngredient(content);
					}
				} else if (inventory instanceof FurnaceInventory furnaceInventory) {
					if ((content = contentMapGet("fuel")) != null) {
						furnaceInventory.setFuel(content);
					}
					if ((content = contentMapGet("smelting")) != null) {
						furnaceInventory.setSmelting(content);
					}
					if ((content = contentMapGet("result")) != null) {
						furnaceInventory.setResult(content);
					}
				} else if (inventory instanceof JukeboxInventory jukeboxInventory) {
					if ((content = contentMapGet("record")) != null) {
						jukeboxInventory.setRecord(content);
					}
				}
			}
		}
		result.setItemMeta(meta);

		if (!mMonumentaContentArray.isEmpty()) {
			NBT.modify(result, nbt -> {
				ReadWriteNBTCompoundList itemsList = ItemStatUtils.getItemList(nbt);
				for (MailboxSlot content : mMonumentaContentArray) {
					ReadWriteNBT newCompound = itemsList.addCompound();
					ReadWriteNBT addedItem = NBT.itemStackToNBT(content.getItem());
					newCompound.mergeCompound(addedItem);
				}
			});
		}

		if (mVirtualAmount != null) {
			NBT.modify(result, nbt -> {
				ReadWriteNBT playerModified = ItemStatUtils.addPlayerModified(nbt);
				playerModified.setLong(CustomContainerItemManager.AMOUNT_KEY, mVirtualAmount);
			});
		}

		return result;
	}

	public static String describeNestedItem(@Nullable ItemStack itemStack) {
		NestedItemLog itemLog = new NestedItemLog();
		describeNestedItem(itemStack, itemLog);
		return itemLog.fullDescription();
	}

	private static void describeNestedItem(@Nullable ItemStack itemStack, NestedItemLog itemLog) {
		if (itemStack == null || itemStack.getAmount() == 0 || itemStack.getType().equals(Material.AIR)) {
			return;
		}

		itemLog.add(itemStack);

		ItemStack reduced = itemStack.asOne();
		ItemMeta meta = reduced.getItemMeta();

		if (meta instanceof CrossbowMeta crossbowMeta) {
			for (ItemStack content : crossbowMeta.getChargedProjectiles()) {
				describeNestedItem(content, itemLog);
			}
		} else if (
			meta instanceof BlockStateMeta blockStateMeta
				&& blockStateMeta.hasBlockState()
				&& blockStateMeta.getBlockState() instanceof BlockInventoryHolder inventoryHolder
		) {
			Inventory inventory = inventoryHolder.getInventory();
			for (ItemStack content : inventory) {
				describeNestedItem(content, itemLog);
			}

			if (inventory instanceof BrewerInventory brewerInventory) {
				describeNestedItem(brewerInventory.getFuel(), itemLog);
				describeNestedItem(brewerInventory.getIngredient(), itemLog);
			} else if (inventory instanceof FurnaceInventory furnaceInventory) {
				describeNestedItem(furnaceInventory.getFuel(), itemLog);
				describeNestedItem(furnaceInventory.getSmelting(), itemLog);
				describeNestedItem(furnaceInventory.getResult(), itemLog);
			} else if (inventory instanceof JukeboxInventory jukeboxInventory) {
				describeNestedItem(jukeboxInventory.getRecord(), itemLog);
			}

			inventory.clear();
		}
		reduced.setItemMeta(meta);

		NBT.modify(reduced, nbt -> {
			// Do not create the tag if it does not exist
			if (ItemStatUtils.hasItemList(nbt)) {
				ReadWriteNBTCompoundList itemsList = ItemStatUtils.getItemList(nbt);
				if (itemsList != null && !itemsList.isEmpty()) {
					for (ReadWriteNBT contentNbt : itemsList) {
						ItemStack content = NBT.itemStackFromNBT(contentNbt);
						describeNestedItem(content, itemLog);
					}
				}
			}
		});
	}

	private void optPutSlotMap(
		Player player,
		String key,
		@Nullable ItemStack itemStack
	) throws UnacceptedItemException {
		if (itemStack == null) {
			return;
		}
		mVanillaContentMap.put(key, new MailboxSlot(player, itemStack));
	}

	private void optPutSlotMap(String key, JsonElement contentMapElement) {
		if (contentMapElement instanceof JsonObject jsonObject) {
			MailboxSlot contentSlot;
			try {
				contentSlot = new MailboxSlot(jsonObject);
			} catch (NullPointerException ignored) {
				return;
			}
			mVanillaContentMap.put(key, contentSlot);
		}
	}

	private @Nullable ItemStack contentMapGet(String key) {
		MailboxSlot content = mVanillaContentMap.get(key);
		if (content == null) {
			return null;
		}
		return content.getItem();
	}

	private static void unacceptedItemCheck(Player player, ItemStack itemStack) throws UnacceptedItemException {
		if (MonumentaNetworkChatIntegration.hasBadWord(player, itemStack, false)) {
			Location loc = player.getLocation();

			String nameDescription = ItemUtils.getPlainNameIfExists(itemStack);
			if (nameDescription.isBlank()) {
				nameDescription = "without a name";
			} else {
				nameDescription = "named " + nameDescription;
			}

			Masterwork masterwork = ItemStatUtils.getMasterwork(itemStack);
			String masterworkDescription;
			if (Masterwork.NONE.equals(masterwork)) {
				masterworkDescription = "";
			} else {
				masterworkDescription = " at masterwork " + masterwork.getName();
			}

			String message = "Player "
				+ player.getName()
				+ " (`/s " + ServerProperties.getShardName()
				+ "` `/world " + loc.getWorld().getName()
				+ "` `/tp @s " + loc.getBlockX() + " "
				+ loc.getBlockY() + " " + loc.getBlockZ()
				+ "`) tried to mail an item with a bad word: " + itemStack.getType().key()
				+ " " + nameDescription + masterworkDescription;
			MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage(message);
			throw new UnacceptedItemException("You cannot mail items with bad words.");
		}

		if (InventoryUtils.containsSpecialLore(itemStack)) {
			throw new UnacceptedItemException("This item cannot be taken out of the dungeon.");
		}

		if (CurseOfEphemerality.isEphemeral(itemStack)) {
			throw new UnacceptedItemException("You cannot mail items with Curse of Ephemerality.");
		}

		if (LootboxManager.isLootbox(itemStack)) {
			if (LootboxManager.getLootshare(player, itemStack, false) != null) {
				// If these are ported to Njol's ItemList system or something similar to the market, then they can be supported
				throw new UnacceptedItemException("Lootboxes with shares can not be mailed at this time.");
			}
		}

		ItemMeta meta = itemStack.getItemMeta();
		if (meta instanceof Beehive beehive) {
			if (beehive.getEntityCount() > 0) {
				// Bees in hives keep their UUID, preventing beehives from being de-duplicated
				throw new UnacceptedItemException("You cannot mail live bees! Empty hives and bee eggs are fine.");
			}
		}
	}
}
