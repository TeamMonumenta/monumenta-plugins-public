package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class TwoHanded implements BaseEnchantment{
	private static String PROPERTY_NAME = ChatColor.RED + "Two Handed";
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
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public void applyProperty(Plugin plugin, Player player, int level) {
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.WEAKNESS, 1000000, 0, true, false));
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW, 1000000, 1, true, false));
	}
	@Override
	public void removeProperty(Plugin plugin, Player player) {
		plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.WEAKNESS);
		plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.SLOW);
	}

	@Override
	public int getLevelFromItem(ItemStack item, Player player) {
		PlayerInventory inventory = player.getInventory();
		if (inventory.getItemInOffHand().getType() != Material.AIR && inventory.getItemInMainHand().getType() != Material.AIR) {
			return getLevelFromItem(item);
		} else {
			return 0;
		}
	}
}
