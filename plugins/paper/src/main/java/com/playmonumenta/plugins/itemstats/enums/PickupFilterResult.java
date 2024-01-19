package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.listeners.JunkItemListener;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTType;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import java.util.List;
import org.bukkit.inventory.ItemStack;

public enum PickupFilterResult {
	// In order from most to least strict (used to optimize code)
	TIERED("tiered", List.of("OnlyTieredItemsPickup", "OnlyLoredItemsPickup", "NoJunkItemsPickup")),
	LORE_AND_INTERESTING("loreAndInteresting", List.of("OnlyLoredItemsPickup", "NoJunkItemsPickup")),
	LORE("lore", List.of("OnlyLoredItemsPickup")),
	INTERESTING("interesting", List.of("NoJunkItemsPickup")),
	COUNT("count", List.of());

	public static final String TIERED_TAG = "OnlyTieredItemsPickup";
	public static final String LORE_TAG = "OnlyLoredItemsPickup";
	public static final String INTERESTING_TAG = "NoJunkItemsPickup";

	public static final String PICKUP_TAG = "Pickup";
	public static final String PICKUP_FILTER_TAG = "FilterResult";
	public static final String PICKUP_COUNT_TAG = "PickupCount";

	public final String mId;
	public final List<String> mTags;

	PickupFilterResult(String id, List<String> tags) {
		mId = id;
		mTags = tags;
	}

	public static PickupFilterResult getFilterResult(ItemStack item) {
		// Sanity check
		if (ItemUtils.isNullOrAir(item)) {
			return COUNT;
		}

		// Check the NBT tag first if it exists
		PickupFilterResult tagResult = NBT.get(item, nbt -> {
			ReadableNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
			if (playerModified == null) {
				return null;
			}

			ReadableNBT pickupTag = playerModified.getCompound(PICKUP_TAG);
			if (pickupTag == null) {
				return null;
			}

			if (!pickupTag.getType(PICKUP_FILTER_TAG).equals(NBTType.NBTTagString)) {
				return null;
			}

			String tagValue = pickupTag.getString(PICKUP_FILTER_TAG);
			for (PickupFilterResult result : PickupFilterResult.values()) {
				if (result.mId.equals(tagValue)) {
					return result;
				}
			}
			return null;
		});
		if (tagResult != null) {
			return tagResult;
		}

		// Rules for every other item
		Tier tier = ItemStatUtils.getTier(item);
		if (!JunkItemListener.IGNORED_TIERS.contains(tier) || ItemUtils.isQuestItem(item) || InventoryUtils.containsSpecialLore(item)) {
			return PickupFilterResult.TIERED;
		}

		boolean hasLore = ItemUtils.hasLore(item);
		boolean isInteresting = ItemUtils.isInteresting(item);
		if (hasLore && isInteresting) {
			return PickupFilterResult.LORE_AND_INTERESTING;
		} else if (hasLore) {
			return PickupFilterResult.LORE;
		} else if (isInteresting) {
			return PickupFilterResult.INTERESTING;
		} else {
			return PickupFilterResult.COUNT;
		}
	}

	public static void setFilterResult(ItemStack item, PickupFilterResult result) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		NBT.modify(item, nbt -> {
			nbt
				.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY)
				.getOrCreateCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
				.getOrCreateCompound(PICKUP_TAG)
				.setString(PICKUP_FILTER_TAG, result.mId);
		});
	}

	public static void removeFilterResult(ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
			if (monumenta == null) {
				return;
			}

			ReadWriteNBT playerModified = monumenta.getCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
			if (playerModified == null) {
				return;
			}

			ReadWriteNBT pickupTag = playerModified.getCompound(PICKUP_TAG);
			if (pickupTag != null) {
				pickupTag.removeKey(PICKUP_FILTER_TAG);

				if (pickupTag.getKeys().isEmpty()) {
					playerModified.removeKey(PICKUP_TAG);
				}
			}

			if (playerModified.getKeys().isEmpty()) {
				monumenta.removeKey(ItemStatUtils.PLAYER_MODIFIED_KEY);
			}

			if (monumenta.getKeys().isEmpty()) {
				nbt.removeKey(ItemStatUtils.MONUMENTA_KEY);
			}
		});
	}

	public static int getPickupCount(ItemStack item) {
		// Sanity check
		if (ItemUtils.isNullOrAir(item)) {
			return 0;
		}

		// Check the NBT tag first if it exists
		int count = NBT.get(item, nbt -> {
			ReadableNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
			if (playerModified == null) {
				return -1;
			}

			ReadableNBT pickupTag = playerModified.getCompound(PICKUP_TAG);
			if (pickupTag == null) {
				return -1;
			}

			if (!pickupTag.getType(PICKUP_COUNT_TAG).equals(NBTType.NBTTagInt)) {
				return -1;
			}

			return pickupTag.getInteger(PICKUP_COUNT_TAG);
		});
		if (count >= 0) {
			return count;
		}

		return item.getAmount();
	}

	public static void setPickupCount(ItemStack item, int count) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		NBT.modify(item, nbt -> {
			nbt
				.getOrCreateCompound(ItemStatUtils.MONUMENTA_KEY)
				.getOrCreateCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
				.getOrCreateCompound(PICKUP_TAG)
				.setInteger(PICKUP_COUNT_TAG, count);
		});
	}

	public static void removePickupCount(ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		NBT.modify(item, nbt -> {
			ReadWriteNBT monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
			if (monumenta == null) {
				return;
			}

			ReadWriteNBT playerModified = monumenta.getCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
			if (playerModified == null) {
				return;
			}

			ReadWriteNBT pickupTag = playerModified.getCompound(PICKUP_TAG);
			if (pickupTag != null) {
				pickupTag.removeKey(PICKUP_COUNT_TAG);

				if (pickupTag.getKeys().isEmpty()) {
					playerModified.removeKey(PICKUP_TAG);
				}
			}

			if (playerModified.getKeys().isEmpty()) {
				monumenta.removeKey(ItemStatUtils.PLAYER_MODIFIED_KEY);
			}

			if (monumenta.getKeys().isEmpty()) {
				nbt.removeKey(ItemStatUtils.MONUMENTA_KEY);
			}
		});
	}
}
