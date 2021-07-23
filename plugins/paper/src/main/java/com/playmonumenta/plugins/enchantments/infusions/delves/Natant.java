package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;
import java.util.NavigableSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class Natant implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Natant";
	private static final int DURATION = 10;
	private static final double PERCENT_SPEED_PER_LEVEL = 0.04;
	private static final String PERCENT_SPEED_EFFECT_NAME = "NatantPercentSpeedEffect";

	@Override
	public @NotNull String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void tick(@NotNull Plugin plugin, @NotNull Player player, int level) {
		Material m = player.getLocation().getBlock().getType();
		Material mHead = player.getEyeLocation().getBlock().getType();
	    if (m == Material.WATER || mHead == Material.WATER) {
			NavigableSet<Effect> speedEffects = plugin.mEffectManager.getEffects(player, PERCENT_SPEED_EFFECT_NAME);
			if (speedEffects != null) {
				for (Effect effect : speedEffects) {
					if (effect.getMagnitude() == PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level)) {
						effect.setDuration(DURATION);
					} else {
						effect.setDuration(1);
						plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level), PERCENT_SPEED_EFFECT_NAME));
					}
				}
			} else {
				plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level), PERCENT_SPEED_EFFECT_NAME));
			}
	    }
	}

}
