package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.EnumSet;

public class Freerunner implements Enchantment {

	public static final double NEARBY_ENEMY_RANGE = 18;

	public static final double SPEED_BONUS = 0.025;

	private static final int EFFECT_DURATION = 10;
	private static final String PERCENT_SPEED_EFFECT_NAME = "FreerunnerPercentSpeedEffect";

	@Override
	public String getName() { return "Freerunner"; }

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FREERUNNER;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.OFFHAND, Slot.MAINHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET);
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHertz, boolean oneHertz) {
		if (shouldActivate(player)) {
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(EFFECT_DURATION, level * SPEED_BONUS, PERCENT_SPEED_EFFECT_NAME).displaysTime(false));
		}
		else {
			plugin.mEffectManager.clearEffects(player, PERCENT_SPEED_EFFECT_NAME);
		}
	}

	public boolean shouldActivate(Player player) {
		Collection<LivingEntity> nearbyEntities = player.getLocation().getNearbyLivingEntities(NEARBY_ENEMY_RANGE);
		for (LivingEntity entity : nearbyEntities) {
			if ((!(entity instanceof Player) && entity.getScoreboardTags().contains("Boss"))
				|| EntityUtils.isHostileMob(entity)) {
				return false;
			}
		}
		return true;
	}
}
