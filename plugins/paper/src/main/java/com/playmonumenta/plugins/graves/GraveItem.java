package com.playmonumenta.plugins.graves;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.utils.ItemUtils;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GraveItem {

	private static final String KEY_NBT = "nbt";

	final ItemStack mItem;

	private GraveItem(ItemStack item) {
		mItem = item;
	}

	// New GraveItem from ThrownItem being destroyed
	public GraveItem(ThrownItem item) {
		this(item.mItem);
	}

	ItemStack getItem() {
		return mItem;
	}

	void collect(int remaining) {
		mItem.setAmount(remaining);
	}

	static @Nullable GraveItem deserialize(JsonObject data) {
		ItemStack item = null;
		if (data.has(KEY_NBT) && data.get(KEY_NBT).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_NBT).isString()) {
			item = NBTItem.convertNBTtoItem(new NBTContainer(data.getAsJsonPrimitive(KEY_NBT).getAsString()));
			if (ItemUtils.isNullOrAir(item)) { // item replacements deleted this item
				return null;
			}
		}
		if (item == null) {
			return null;
		}
		String oldStatusKey = "status";
		if (data.has(oldStatusKey) && data.get(oldStatusKey).isJsonPrimitive() && data.getAsJsonPrimitive(oldStatusKey).isString()) {
			String oldStatus = data.getAsJsonPrimitive(oldStatusKey).getAsString();
			if ("COLLECTED".equals(oldStatus)) {
				return null;
			}
		}
		return new GraveItem(item);
	}

	JsonObject serialize() {
		JsonObject data = new JsonObject();
		data.addProperty(KEY_NBT, NBTItem.convertItemtoNBT(mItem).toString());
		return data;
	}
}
