package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

/*
 * Resurrection - Makes the armor piece act like a totem (saves your life and then breaks)
 * Effects are the same as a normal totem
 */
public class Resurrection implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Resurrection";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		if (player.getHealth() - event.getFinalDamage() <= 0) {
			player.setHealth(2);
			event.setDamage(1);
			for (PotionEffect effect : player.getActivePotionEffects()) {
				player.removePotionEffect(effect.getType());
			}
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 40, 0, true, true));
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.REGENERATION, 20 * 45, 1, true, true));
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.ABSORPTION, 20 * 5, 1, true, true));
			player.getInventory().setHelmet(new ItemStack(Material.AIR));

			player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1);
			player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 100, 0, 0, 0, 1);
		}
	}

}
