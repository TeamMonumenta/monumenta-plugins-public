package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealtSingle;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;



public class Frenzy extends Ability {

	private static final int DURATION = 5 * 20;
	private static final double PERCENT_ATTACK_SPEED_EFFECT_1 = 0.3;
	private static final double PERCENT_ATTACK_SPEED_EFFECT_2 = 0.4;
	private static final double PERCENT_SPEED = 0.2;
	private static final double DAMAGE_BONUS = 0.2;
	private static final String PERCENT_ATTACK_SPEED_EFFECT_NAME = "FrenzyPercentAttackSpeedEffect";
	private static final String PERCENT_SPEED_EFFECT_NAME = "FrenzyPercentSpeedEffect";
	private static final String PERCENT_DAMAGE_SINGLE_EFFECT_NAME = "FrenzyPercentDamageDealtSingleEffect";

	public static final String CHARM_DURATION = "Frenzy Duration";
	public static final String CHARM_ATTACK_SPEED = "Frenzy Attack Speed";
	public static final String CHARM_SPEED = "Frenzy Speed";
	public static final String CHARM_BONUS_DAMAGE = "Frenzy Bonus Damage";

	public static final AbilityInfo<Frenzy> INFO =
		new AbilityInfo<>(Frenzy.class, "Frenzy", Frenzy::new)
			.scoreboardId("Frenzy")
			.shorthandName("Fnz")
			.descriptions(
				"Gain +30% Attack Speed for 5 seconds after killing a mob.",
				"Gain +40% Attack Speed and +20% Speed for 5 seconds after killing a mob.",
				"Additionally, your next melee damage within 5 seconds after getting a kill deals 20% extra damage.")
			.displayItem(new ItemStack(Material.FEATHER, 1));

	private final double mPercentAttackSpeedEffect;
	private final int mDuration;

	public Frenzy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentAttackSpeedEffect = (isLevelOne() ? PERCENT_ATTACK_SPEED_EFFECT_1 : PERCENT_ATTACK_SPEED_EFFECT_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ATTACK_SPEED);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_ATTACK_SPEED_EFFECT_NAME,
			new PercentAttackSpeed(mDuration, mPercentAttackSpeedEffect, PERCENT_ATTACK_SPEED_EFFECT_NAME));

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME,
					new PercentSpeed(mDuration, PERCENT_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED), PERCENT_SPEED_EFFECT_NAME));
		}

		if (isEnhanced()) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_SINGLE_EFFECT_NAME,
				new PercentDamageDealtSingle(mDuration, DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS_DAMAGE), EnumSet.of(DamageEvent.DamageType.MELEE)));
		}
	}
}
