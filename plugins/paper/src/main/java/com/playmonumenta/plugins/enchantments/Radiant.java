package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Radiant implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Radiant";
	private static final String NIGHTVISION_DISABLED_TAG = "RadiantDarksightDisabled";


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
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public int getPlayerItemLevel(ItemStack itemStack, Player player, ItemSlot itemSlot) {
		if (!InventoryUtils.isSoulboundToPlayer(itemStack, player)) {
			return 0;
		}

		return BaseEnchantment.super.getPlayerItemLevel(itemStack, player, itemSlot);
	}

	@Override
	public void applyProperty(Plugin plugin, Player player, int level) {
		// Radiant is different from the others - it applies effects only for a short duration
		// and doesn't remove them when you switch off
		plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.GLOWING, 600, 0, true, false));
		if (!player.getScoreboardTags().contains(NIGHTVISION_DISABLED_TAG)) {
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0, true, false));
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		applyProperty(plugin, player, level);
	}
}
