package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.scout.Quickdraw;
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

		onOversizedShoot(player, false);
	}

	// called by this and ThrowingKnife
	public static void onOversizedShoot(Player player, boolean fromThrowingKnife) {
		ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
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
			// ThrowRate already calls this, but others don't
			onAnyShoot(player, cooldown, fromThrowingKnife, !fromThrowingKnife);
		}

		if (player.getCooldown(Material.SNOWBALL) < cooldown) {
			player.setCooldown(Material.SNOWBALL, cooldown);
		}
		// only set tridents on cooldown if the player has a non-riptide trident in hotbar
		// to prevent riptide tridents from being disabled
		for (int i = 0; i < 9; i++) {
			ItemStack item = player.getInventory().getItem(i);
			if (item != null && item.getType() == Material.TRIDENT
				&& !ItemStatUtils.hasEnchantment(item, EnchantmentType.RIPTIDE)
				&& player.getCooldown(Material.TRIDENT) < cooldown) {
				player.setCooldown(Material.TRIDENT, cooldown);
				break;
			}
		}
	}

	// Called by this and ThrowRate
	public static void onAnyShoot(Player player, int cooldown, boolean disableOversized, boolean disableThrowingKnife) {
		for (int i = 0; i < 9; i++) {
			ItemStack item = player.getInventory().getItem(i);
			if (item != null && ((disableOversized && ItemStatUtils.hasEnchantment(item, EnchantmentType.OVERSIZED))
				|| (disableThrowingKnife && ItemStatUtils.hasEnchantment(item, EnchantmentType.THROWING_KNIFE)))) {
				if (player.getCooldown(item.getType()) < cooldown) {
					player.setCooldown(item.getType(), cooldown);
				}
			}
		}
	}

}
