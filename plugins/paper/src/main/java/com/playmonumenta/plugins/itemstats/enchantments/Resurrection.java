package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * Resurrection - Makes the armor piece act like a totem (saves your life and then breaks)
 * Effects are the same as a normal totem
 */
public class Resurrection implements Enchantment {

	@Override
	public String getName() {
		return "Resurrection";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RESURRECTION;
	}

	@Override
	public double getPriorityAmount() {
		return 10000;
	}

	@Override
	public void onHurtFatal(Plugin plugin, Player player, double level, DamageEvent event) {
		plugin.mPotionManager.clearAllPotions(player);

		execute(plugin, player, event, getEnchantmentType());
	}

	public static boolean execute(Plugin plugin, Player player, DamageEvent event, @Nullable EnchantmentType resurrectionEnchantment) {

		// Simulate resurrecting the player
		EntityResurrectEvent resEvent = new EntityResurrectEvent(player);
		Bukkit.getPluginManager().callEvent(resEvent);
		if (resEvent.isCancelled()) {
			return false;
		}

		// Act like a normal totem
		event.setDamage(0.001);
		player.setHealth(1);

		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 40, 0, true, true));
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.REGENERATION, 20 * 45, 1, true, true));
		new BukkitRunnable() {
			@Override
			public void run() {
				plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.ABSORPTION, 20 * 5, 1, true, true));
			}
		}.runTaskLater(plugin, 1);

		player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1);
		player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 100, 0, 0, 0, 1);

		if (resurrectionEnchantment != null) {
			PlayerInventory inventory = player.getInventory();
			for (ItemStack item : new ItemStack[] {inventory.getItemInMainHand(), inventory.getItemInOffHand(), inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots()}) {
				if (ItemStatUtils.getEnchantmentLevel(item, resurrectionEnchantment) >= 1) {
					item.subtract();
					break;
				}
			}

			ItemStatManager.PlayerItemStats playerItemStats = plugin.mItemStatManager.getPlayerItemStats(player);
			if (playerItemStats != null) {
				playerItemStats.updateStats(true);
			}
		}

		return true;
	}

}
