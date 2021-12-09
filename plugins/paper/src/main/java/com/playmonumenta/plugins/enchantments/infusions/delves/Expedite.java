package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

public class Expedite implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Expedite";
	private static final int DURATION = 5 * 20;
	private static final double PERCENT_SPEED_PER_LEVEL = 0.0125;
	private static final String PERCENT_SPEED_EFFECT_NAME = "ExpeditePercentSpeedEffect";
	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "ExpediteTick";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onAbility(Plugin plugin, Player player, int level, LivingEntity enemy, CustomDamageEvent customDamageEvent) {
		if (customDamageEvent.getMagicType() == MagicType.ENCHANTMENT) {
			return;
		}
		if (MetadataUtils.checkOnceThisTick(plugin, player, CHECK_ONCE_THIS_TICK_METAKEY)) {
			NavigableSet<Effect> speedEffects = plugin.mEffectManager.getEffects(player, PERCENT_SPEED_EFFECT_NAME);
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.1f, 1.0f);
			if (speedEffects != null) {
				for (Effect effect : new TreeSet<>(speedEffects)) {
					double mag = effect.getMagnitude() / (PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level));
					if (effect.getMagnitude() == PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level) * Math.min(5, mag + 1)) {
						effect.setDuration(DURATION);
					} else {
						effect.setDuration(1);
						plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level) * Math.min(5, mag + 1), PERCENT_SPEED_EFFECT_NAME));
					}
				}
			} else {
				plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED_PER_LEVEL * DelveInfusionUtils.getModifiedLevel(plugin, player, level), PERCENT_SPEED_EFFECT_NAME));
			}
		}
	}
}
