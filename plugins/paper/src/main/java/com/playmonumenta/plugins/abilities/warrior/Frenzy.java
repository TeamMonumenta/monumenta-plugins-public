package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentSpeed;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import javax.annotation.Nullable;



public class Frenzy extends Ability {

	private static final String PERCENT_ATTACK_SPEED_EFFECT_NAME = "FrenzyPercentAttackSpeedEffect";
	private static final int DURATION = 5 * 20;
	private static final double PERCENT_ATTACK_SPEED_EFFECT_1 = 0.3;
	private static final double PERCENT_ATTACK_SPEED_EFFECT_2 = 0.4;
	private static final String PERCENT_SPEED_EFFECT_NAME = "FrenzyPercentSpeedEffect";
	private static final double PERCENT_SPEED = 0.2;

	private final double mPercentAttackSpeedEffect;

	public Frenzy(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Frenzy");
		mInfo.mScoreboardId = "Frenzy";
		mInfo.mShorthandName = "Fnz";
		mInfo.mDescriptions.add("Gain +30% Attack Speed for 5 seconds after killing a mob.");
		mInfo.mDescriptions.add("Gain +40% Attack Speed and +20% Speed for 5 seconds after killing a mob.");
		mDisplayItem = new ItemStack(Material.FEATHER, 1);

		mPercentAttackSpeedEffect = isLevelOne() ? PERCENT_ATTACK_SPEED_EFFECT_1 : PERCENT_ATTACK_SPEED_EFFECT_2;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mPlayer == null) {
			return;
		}
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_ATTACK_SPEED_EFFECT_NAME,
				new PercentAttackSpeed(DURATION, mPercentAttackSpeedEffect, PERCENT_ATTACK_SPEED_EFFECT_NAME));

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME,
					new PercentSpeed(DURATION, PERCENT_SPEED, PERCENT_SPEED_EFFECT_NAME));
		}
	}

}
