package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;

import java.util.EnumSet;

public class KineticLoading implements Enchantment {
	@Override
	public String getName() {
		return "Kinetic Loading";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.KINETICLOADING;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE && PlayerUtils.isCriticalAttack(player)) {
			ItemStack mainhand = player.getInventory().getItemInMainHand();
			if (mainhand.getItemMeta() instanceof CrossbowMeta crossbowMeta && !crossbowMeta.hasChargedProjectiles()) {
				int numProjectiles = 1 + ItemStatUtils.getEnchantmentLevel(mainhand, EnchantmentType.MULTILOAD);

				// Crossbows refund arrows when being shot instead of not consuming arrows when being loaded
				ItemStack projectileItem = new ItemStack(Material.ARROW);
				int amount = projectileItem.getAmount();
				projectileItem.setAmount(1);
				if (numProjectiles > 1) {
					// multi-loading handles adding charged projectile
					Multiload.loadCrossbow(player, mainhand, projectileItem, numProjectiles, amount);
				} else {
					crossbowMeta.addChargedProjectile(projectileItem);
					if (ItemStatUtils.hasEnchantment(mainhand, EnchantmentType.MULTISHOT)) {
						crossbowMeta.addChargedProjectile(ItemUtils.clone(projectileItem));
						crossbowMeta.addChargedProjectile(ItemUtils.clone(projectileItem));
					}
					mainhand.setItemMeta(crossbowMeta);
				}
				// Sound copied from vanilla (won't play due to cancelled event)
				player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS,
					1.0F, 1.0F / (FastUtils.randomFloatInRange(0, 1) * 0.5F + 1.0F) + 0.2F);
			} else if (mainhand.getType().equals(Material.TRIDENT)
				|| mainhand.getType().equals(Material.SNOWBALL)) {
				player.setCooldown(mainhand.getType(), 0);
			}
		}
	}
}
