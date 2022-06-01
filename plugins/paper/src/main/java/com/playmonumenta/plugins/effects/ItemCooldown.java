package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemCooldown extends Effect {

	private final String mItemName;
	private final Plugin mPlugin;

	public ItemCooldown(int duration, ItemStack item, Plugin plugin) {
		super(duration);
		mItemName = ItemUtils.getPlainName(item);
		mPlugin = plugin;
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
