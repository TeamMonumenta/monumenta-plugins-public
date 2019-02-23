package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class InstantDrink implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Instant Drink";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event, int level) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.POTION) {
				PotionMeta meta = (PotionMeta) item.getItemMeta();
				if (meta.hasCustomEffects()) {
					for (PotionEffect effect : meta.getCustomEffects()) {
						plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
					}
				}
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1, 1);
				item.setType(Material.AIR);
			}
		}
	}
}
