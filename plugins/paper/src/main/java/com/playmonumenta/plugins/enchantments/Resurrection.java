package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Resurrection - Makes the armor piece act like a totem (saves your life and then breaks)
 * Effects are the same as a normal totem
 */
public class Resurrection implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Resurrection";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onFatalHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		event.setDamage(0.001);
		player.setHealth(1);

		for (PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 40, 0, true, true));
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.REGENERATION, 20 * 45, 1, true, true));
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.ABSORPTION, 20 * 5, 1, true, true));

		player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1);
		player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 100, 0, 0, 0, 1);

		PlayerInventory inventory = player.getInventory();
		if (InventoryUtils.getCustomEnchantLevel(inventory.getHelmet(), PROPERTY_NAME, false) == 1) {
			inventory.setHelmet(new ItemStack(Material.AIR));
		} else if (InventoryUtils.getCustomEnchantLevel(inventory.getChestplate(), PROPERTY_NAME, false) == 1) {
			inventory.setChestplate(new ItemStack(Material.AIR));
		} else if (InventoryUtils.getCustomEnchantLevel(inventory.getLeggings(), PROPERTY_NAME, false) == 1) {
			inventory.setLeggings(new ItemStack(Material.AIR));
		} else if (InventoryUtils.getCustomEnchantLevel(inventory.getBoots(), PROPERTY_NAME, false) == 1) {
			inventory.setBoots(new ItemStack(Material.AIR));
		} else if (InventoryUtils.getCustomEnchantLevel(inventory.getItemInOffHand(), PROPERTY_NAME, false) == 1) {
			inventory.setItemInOffHand(new ItemStack(Material.AIR));
		} else {
			inventory.setItemInMainHand(new ItemStack(Material.AIR));
		}

		InventoryUtils.scheduleDelayedEquipmentCheck(plugin, player, null);
	}

}
