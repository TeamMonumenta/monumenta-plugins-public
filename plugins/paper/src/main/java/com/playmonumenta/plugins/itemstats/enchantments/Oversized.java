package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.scout.Quickdraw;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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

	public static void onAnyShoot(Player player, Projectile projectile) {
		Quickdraw quickdraw = AbilityManager.getManager().getPlayerAbility(player, Quickdraw.class);
		boolean isQuickdraw = quickdraw != null && quickdraw.isQuickDraw(projectile);

		if (isQuickdraw) {
			return;
		}

		ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
		boolean disableAll = playerItemStats.getItemStats().get(EnchantmentType.OVERSIZED) > 0 || playerItemStats.getItemStats().get(EnchantmentType.THROWING_KNIFE) > 0;
		int cooldown = 0;
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item.getType() == Material.TRIDENT || item.getType() == Material.SNOWBALL || playerItemStats.getItemStats().get(EnchantmentType.THROWING_KNIFE) > 0) {
			double throwRate = playerItemStats.getItemStats().get(AttributeType.THROW_RATE);
			if (throwRate > 0) {
				cooldown = (int) (20 / throwRate);
			}
		} else {
			if (item.getType() == Material.CROSSBOW) {
				cooldown = Math.max(0, 25 - 5 * (int) playerItemStats.getItemStats().get(EnchantmentType.QUICK_CHARGE));
			} else if (item.getType() == Material.BOW) {
				cooldown = 10;
			}
		}
		for (int i = 0; i < 9; i++) {
			ItemStack testItem = player.getInventory().getItem(i);
			if (ItemUtils.isProjectileWeapon(testItem) && player.getCooldown(testItem.getType()) < cooldown &&
				(disableAll || (ItemStatUtils.hasEnchantment(testItem, EnchantmentType.OVERSIZED) || ItemStatUtils.hasEnchantment(testItem, EnchantmentType.THROWING_KNIFE)))) {
				player.setCooldown(testItem.getType(), cooldown);
			}
		}
	}
}
