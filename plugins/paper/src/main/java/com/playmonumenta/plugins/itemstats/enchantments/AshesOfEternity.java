package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class AshesOfEternity implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Ashes of Eternity";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ASHES_OF_ETERNITY;
	}

	@Override
	public void onHurtFatal(@NotNull Plugin plugin, @NotNull Player player, double value, @NotNull DamageEvent event) {
		if (!event.isCancelled()) {
			// Void tether didn't cancel the event - so this player would die
			// They definitely aren't in the void at this point

			// Remove Enchant
			ItemStack item = player.getInventory().getItemInMainHand();
			ItemStatUtils.removeEnchantment(item, EnchantmentType.ASHES_OF_ETERNITY);
			ItemStatUtils.generateItemStats(item);

			plugin.mPotionManager.clearAllPotions(player);

			// Simulate resurrecting the player
			EntityResurrectEvent resEvent = new EntityResurrectEvent(player);
			Bukkit.getPluginManager().callEvent(resEvent);
			if (!resEvent.isCancelled()) {
				// Act like a normal totem
				event.setDamage(0.001);
				player.setHealth(1);

				plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 40, 0, true, true));
				plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.REGENERATION, 20 * 45, 1, true, true));
				plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.ABSORPTION, 20 * 5, 1, true, true));

				player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 1);
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_DEATH, 2, 2);
				player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 100, 0, 0, 0, 1);
			}
		}
	}
}
