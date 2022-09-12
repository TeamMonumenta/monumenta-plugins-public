package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemCooldown extends Effect {
	public static final String effectID = "ItemCooldown";

	private final String mItemName;
	private final Material mMaterial; // mMaterial is the "cooldown" item we select.
	private final Plugin mPlugin;

	public ItemCooldown(int duration, ItemStack item, Plugin plugin) {
		this(duration, ItemUtils.getPlainName(item), plugin);
	}

	public ItemCooldown(int duration, String itemName, Plugin plugin) {
		this(duration, itemName, null, plugin);
	}

	public ItemCooldown(int duration, ItemStack item, Material material, Plugin plugin) {
		this(duration, ItemUtils.getPlainName(item), material, plugin);
	}

	public ItemCooldown(int duration, String itemName, @Nullable Material material, Plugin plugin) {
		super(duration, effectID);
		mItemName = itemName;
		mMaterial = material;
		mPlugin = plugin;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (mMaterial != null && entity instanceof Player player) {
			player.setCooldown(mMaterial, mDuration);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		// Once the effect ends, we want to update the player's inventory slightly to force a packet send.
		if (entity instanceof Player player) {
			player.sendActionBar(Component.text(mItemName + " is now off cooldown!", NamedTextColor.YELLOW));

			new BukkitRunnable() {
				@Override public void run() {
					player.updateInventory();
				}
			}.runTaskLater(mPlugin, 1);
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("itemName", mItemName);

		if (mMaterial != null) {
			object.addProperty("material", mMaterial.name());
		}

		return object;
	}

	public static ItemCooldown deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		String itemName = object.get("itemName").getAsString();

		if (object.has("material")) {
			String itemMaterial = object.get("material").getAsString();
			Material material = Material.valueOf(itemMaterial);
			return new ItemCooldown(duration, itemName, material, plugin);
		}

		return new ItemCooldown(duration, itemName, plugin);
	}

	/**
	 * Provide the source string to compare based on the enchantmentType.
	 *
	 * @param enchantmentType input
	 * @return "CD" + enchantmentName
	 */
	public static String toSource(ItemStatUtils.EnchantmentType enchantmentType) {
		return "CD" + enchantmentType.getName();
	}

	@Override
	public String toString() {
		return String.format("ItemCooldown duration:%d plainName:%s", this.getDuration(), mItemName);
	}
}
