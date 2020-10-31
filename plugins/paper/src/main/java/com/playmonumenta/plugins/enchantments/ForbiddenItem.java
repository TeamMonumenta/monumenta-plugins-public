package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class ForbiddenItem implements BaseEnchantment {
	public static final PotionEffect FORBIDDEN_ITEM_WEAKNESS_EFFECT = new PotionEffect(PotionEffectType.WEAKNESS, 60, 50, true, false);
	public static final PotionEffect FORBIDDEN_ITEM_SLOWNESS_EFFECT = new PotionEffect(PotionEffectType.SLOW, 60, 3, true, false);
	public static final PotionEffect FORBIDDEN_ITEM_BLINDNESS_EFFECT = new PotionEffect(PotionEffectType.BLINDNESS, 78, 0, true, false);

	/* This is dynamically set based on config */
	private final String mPropertyName;

	public ForbiddenItem(String propertyName) {
		mPropertyName = propertyName;
	}

	@Override
	public String getProperty() {
		return mPropertyName;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
	}

	@Override
	public void applyProperty(Plugin plugin, Player player, int level) {
		// Forbidden items are different from the others - it applies effects only for a short duration
		// and doesn't remove them when you switch off
		if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.RESIST_5)) {
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, FORBIDDEN_ITEM_SLOWNESS_EFFECT);
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, FORBIDDEN_ITEM_WEAKNESS_EFFECT);
			if (!player.getGameMode().equals(GameMode.CREATIVE)) {
				plugin.mPotionManager.addPotion(player, PotionID.ITEM, FORBIDDEN_ITEM_BLINDNESS_EFFECT);
			}
			MessagingUtils.sendActionBarMessage(plugin, player, ChatColor.stripColor(mPropertyName).replace(" :", "") + " items can not be used here!");
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		applyProperty(plugin, player, level);
	}
}
