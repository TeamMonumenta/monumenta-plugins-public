package com.playmonumenta.plugins.inventories;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CharmBag {

	public final UUID mOwner;
	public final ArrayList<CharmBagItem> mItems = new ArrayList<>();

	CharmBag(UUID owner) {
		mOwner = owner;
	}

	public static class CharmBagItem {
		public final ItemStack mItem;
		public long mAmount;

		public CharmBagItem(ItemStack item, long amount) {
			mItem = item;
			mAmount = amount;
		}

		JsonObject serialize() {
			JsonObject json = new JsonObject();
			json.addProperty("item", ItemUtils.serializeItemStack(mItem));
			json.addProperty("amount", mAmount);
			return json;
		}

		static CharmBagItem deserialize(JsonObject json) {
			return new CharmBagItem(ItemUtils.parseItemStack(json.get("item").getAsString()), json.get("amount").getAsLong());
		}

	}

	private @Nullable CharmBagItem find(ItemStack charm) {
		for (CharmBagItem charmBagItem : mItems) {
			if (charmBagItem.mItem.isSimilar(charm)) {
				return charmBagItem;
			}
		}
		return null;
	}

	/**
	 * "Add" function for Charm Bags
	 */
	public void add(Player player, ItemStack charm) {
		if (ItemUtils.isNullOrAir(charm)) {
			throw new IllegalArgumentException("Tried to add air to charm bag!");
		}

		if (!player.getUniqueId().equals(mOwner)) {
			AuditListener.log("+AddItemToCharmBag: " + player.getName() + " added to charm bag of "
				+ MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(charm));
		}

		for (CharmBagItem charmBagItem : mItems) {
			if (charmBagItem.mItem.isSimilar(charm)) {
				charmBagItem.mAmount += charm.getAmount();
				charm.setAmount(0);
				return;
			}
		}

		ItemStack clone = ItemUtils.clone(charm);
		clone.setAmount(1);
		mItems.add(new CharmBagItem(clone, charm.getAmount()));
		charm.setAmount(0);
	}

	/**
	 * Counts the number of items of the given type contained in the Charm Bag.
	 */
	public long count(ItemStack charm) {
		//
		CharmBagItem charmBagItem = find(charm);
		return charmBagItem != null ? charmBagItem.mAmount : 0;
	}

	/**
	 * Removes a quantity of a certain charm type from this charm bag. Does not check if it is a valid amount.
	 */
	public void remove(Player player, ItemStack charm) {
		if (ItemUtils.isNullOrAir(charm)) {
			return;
		}

		// Unlike the Wallet, we have no compressions to worry about - only normal item cases!
		for (Iterator<CharmBagItem> iterator = mItems.iterator(); iterator.hasNext(); ) {
			CharmBagItem charmBagItem = iterator.next();
			if (charmBagItem.mItem.isSimilar(charm)) {
				if (charm.getAmount() >= charmBagItem.mAmount) {
					charm.setAmount((int) charmBagItem.mAmount);
					iterator.remove();
				} else {
					charmBagItem.mAmount -= charm.getAmount();
				}
				if (charm.getAmount() > 0 && !player.getUniqueId().equals(mOwner)) {
					AuditListener.log("-RemoveItemFromCharmBag: " + player.getName() + " removed from charm bag of "
						+ MonumentaRedisSyncIntegration.cachedUuidToName(mOwner) + ": " + AuditListener.getItemLogString(charm));
				}
				return;
			}
		}

		charm.setAmount(0);
	}

	/**
	 * Create a deep clone of this charm bag. The returned charm bag can be modified without affecting the original.
	 * Care should be taken to perform operations with the correct player, as audit messages can be logged otherwise still.
	 */
	public CharmBag deepClone() {
		CharmBag clone = new CharmBag(mOwner);
		mItems.stream()
			.map(item -> new CharmBagItem(ItemUtils.clone(item.mItem), item.mAmount))
			.forEach(clone.mItems::add);
		return clone;
	}

	JsonObject serialize() {
		JsonObject json = new JsonObject();
		JsonArray itemsArray = new JsonArray();
		for (CharmBagItem item : mItems) {
			itemsArray.add(item.serialize());
		}
		json.add("items", itemsArray);
		return json;
	}

	static CharmBag deserialize(Player player, JsonObject json) {
		CharmBag charmBag = new CharmBag(player.getUniqueId());
		for (JsonElement item : json.getAsJsonArray("items")) {
			// combine duplicate charm bag items together
			CharmBagItem charmBagItem = CharmBagItem.deserialize(item.getAsJsonObject());
			if (ItemUtils.isNullOrAir(charmBagItem.mItem) || charmBagItem.mAmount <= 0) {
				continue;
			}
			// perform basic deduplication
			for (Iterator<CharmBagItem> it = charmBag.mItems.iterator(); it.hasNext(); ) {
				CharmBagItem otherCharmBagItem = it.next();
				if (otherCharmBagItem.mItem.isSimilar(charmBagItem.mItem)) {
					charmBagItem.mAmount += otherCharmBagItem.mAmount;
					it.remove();
				}
			}
			charmBag.mItems.add(charmBagItem);
		}
		return charmBag;
	}

}
