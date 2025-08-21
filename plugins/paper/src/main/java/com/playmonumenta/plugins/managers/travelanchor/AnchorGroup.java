package com.playmonumenta.plugins.managers.travelanchor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnchorGroup implements Comparable<AnchorGroup> {
	private final UUID mId;
	private String mName;
	private Material mItemMat;
	// Reserved for special groups
	private final boolean mDeletable;

	public AnchorGroup(String name, Material itemMat) {
		this(UUID.randomUUID(), name, itemMat, true);
	}

	protected AnchorGroup(UUID uuid, String name, Material itemMat, boolean deletable) {
		mId = uuid;
		mName = name;
		mItemMat = itemMat;
		mDeletable = deletable;
	}

	public AnchorGroup(JsonElement data) throws Exception {
		this(null, data, true);
	}

	protected AnchorGroup(@Nullable UUID uuid, JsonElement data, boolean deletable) throws Exception {
		if (!(data instanceof JsonObject jsonObject)) {
			throw new Exception("Expected AnchorGroup as a JsonObject");
		}

		if (uuid != null) {
			mId = uuid;
		} else {
			if (!(jsonObject.get("mId") instanceof JsonPrimitive idPrimitive && idPrimitive.isString())) {
				throw new Exception("Expected AnchorGroup id to be a UUID string");
			} else {
				try {
					mId = UUID.fromString(idPrimitive.getAsString());
				} catch (Exception ex) {
					throw new Exception("Expected AnchorGroup id to be a UUID string", ex);
				}
			}
		}

		if (!(jsonObject.get("mName") instanceof JsonPrimitive namePrimitive && namePrimitive.isString())) {
			throw new Exception("Expected AnchorGroup name to be a string");
		} else {
			mName = namePrimitive.getAsString();
		}

		if (!(jsonObject.get("mItemMat") instanceof JsonPrimitive itemMatPrimitive && itemMatPrimitive.isString())) {
			throw new Exception("Expected AnchorGroup itemMat to be a string");
		} else {
			String itemMatString = itemMatPrimitive.getAsString();
			// Include legacy names for update purposes
			Material itemMat = Material.matchMaterial(itemMatString, false);
			if (itemMat == null) {
				itemMat = Material.matchMaterial(itemMatString, true);
			}
			// Failsafe if no match is found
			mItemMat = Objects.requireNonNullElse(itemMat, Material.STONE);
		}

		mDeletable = deletable;
	}

	public JsonObject toJson() {
		JsonObject data = new JsonObject();

		data.addProperty("mId", mId.toString());
		data.addProperty("mName", mName);
		data.addProperty("mItemMat", mItemMat.key().toString());

		return data;
	}

	public boolean deletable() {
		return mDeletable;
	}

	public UUID id() {
		return mId;
	}

	public String name() {
		return mName;
	}

	// Edit this from WorldAnchorGroups instead
	protected void name(String name) {
		mName = name;
	}

	public Material itemMat() {
		return mItemMat;
	}

	public ItemStack item(TextColor textColor) {
		return GUIUtils.createBasicItem(mItemMat, Component.text(mName, textColor)
			.decoration(TextDecoration.ITALIC, false));
	}

	public void item(ItemStack item) {
		mItemMat = item.getType();
	}

	@Override
	public int compareTo(@NotNull AnchorGroup o) {
		int value = mName.compareTo(o.mName);
		if (value != 0) {
			return value;
		}

		return mId.compareTo(o.mId);
	}
}
