package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;

public class Oversized implements Enchantment {

	@Override
	public String getName() {
		return "Oversized";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.OVERSIZED;
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		Quickdraw quickdraw = AbilityManager.getManager().getPlayerAbility(player, Quickdraw.class);
		boolean isQuickdraw = quickdraw != null && quickdraw.isQuickDraw(projectile);

		if (isQuickdraw) {
			return;
		}

		ItemStatManager.PlayerItemStats playerItemStats = plugin.mItemStatManager.getPlayerItemStats(player);
		double throwRate = playerItemStats.getItemStats().get(AttributeType.THROW_RATE);
		int cooldown;
		if (throwRate > 0) {
			cooldown = (int) (20 / throwRate);
		} else {
			ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
			if (itemInMainHand.getType() == Material.CROSSBOW) {
				cooldown = Math.max(0, 25 - 5 * (int) playerItemStats.getItemStats().get(EnchantmentType.QUICK_CHARGE));
			} else if (itemInMainHand.getType() == Material.BOW) {
				cooldown = 10;
			} else {
				return;
			}
		}
		for (Material mat : new Material[] {Material.TRIDENT, Material.SNOWBALL}) {
			if (player.getCooldown(mat) < cooldown) {
				player.setCooldown(mat, cooldown);
			}
		}
	}

	public static void onThrow(Player player, int cooldown) {
		for (int i = 0; i < 9; i++) {
			ItemStack item = player.getInventory().getItem(i);
			if (item != null && ItemStatUtils.hasEnchantment(item, EnchantmentType.OVERSIZED)) {
				if (player.getCooldown(item.getType()) < cooldown) {
					player.setCooldown(item.getType(), cooldown);
				}
			}
		}
	}

}
