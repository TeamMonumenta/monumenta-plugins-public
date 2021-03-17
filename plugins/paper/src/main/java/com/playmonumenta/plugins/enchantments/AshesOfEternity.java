package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

/*
 * Ashes of Eternity - When held, acts as a normal totem but does not shatter or break when life is saved.
 * Instead, remove the lore text from the item which will be returned with the weekly update item replacements.
 * Effects are the same as a normal totem + Void Tether
 */
public class AshesOfEternity extends VoidTether {
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
	protected boolean runCheck(Plugin plugin, Player player) {
		return InventoryUtils.testForItemWithLore(player.getInventory().getItemInMainHand(), PROPERTY_NAME);
	}

	@Override
	public void onFatalHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		// Run the void tether logic. If in the void, will cancel event and teleport the player up, then kill them again
		super.onFatalHurt(plugin, player, level, event);

		if (!event.isCancelled()) {
			// Void tether didn't cancel the event - so this player would die
			// They definitely aren't in the void at this point

			// Remove Lore Text
			ItemStack item = player.getInventory().getItemInMainHand();
			ItemMeta meta = item.getItemMeta();
			List<String> lore = meta.getLore();
			lore.removeIf((String loreEntry) -> loreEntry.contains(PROPERTY_NAME));
			meta.setLore(lore);
			item.setItemMeta(meta);
			item = ItemUtils.setPlainLore(item);

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
