package com.playmonumenta.plugins.enchantments;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

/*
 * Ashes of Eternity - When held, acts as a normal totem but does not shatter or break when life is saved.
 * Instead, remove the lore text from the item which will be returned with the weekly update item replacements.
 * Effects are the same as a normal totem
 */
public class AshesOfEternity implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Ashes of Eternity";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onFatalHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		ItemStack item = player.getInventory().getItemInMainHand();
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		List<String> newLore = new ArrayList<>();
		for (String loreEntry : lore) {
			if (!loreEntry.contains(ChatColor.GRAY + "Ashes of Eternity")) {
				newLore.add(loreEntry);
				} else {
					//Act like a normal totem
					event.setDamage(0.001);
					player.setHealth(1);

					for (PotionEffect effect : player.getActivePotionEffects()) {
						player.removePotionEffect(effect.getType());
					}
					plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 40, 0, true, true));
					plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.REGENERATION, 20 * 45, 1, true, true));
					plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.ABSORPTION, 20 * 5, 1, true, true));

					player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.3f, 1);
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_DEATH, 2, 2);
					player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 100, 0, 0, 0, 1);
				}
		}
		//Remove Lore Text
		meta.setLore(newLore);
		item.setItemMeta(meta);
	}
}
