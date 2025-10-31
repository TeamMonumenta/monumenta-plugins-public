package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.FrenzyCS;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealtSingleTick;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;


public class Frenzy extends Ability {

	private static final int DURATION = 5 * 20;
	private static final double PERCENT_ATTACK_SPEED_EFFECT_1 = 0.3;
	private static final double PERCENT_ATTACK_SPEED_EFFECT_2 = 0.4;
	private static final double PERCENT_SPEED = 0.2;
	private static final double DAMAGE_BONUS = 0.2;
	private static final String PERCENT_ATTACK_SPEED_EFFECT_NAME = "FrenzyPercentAttackSpeedEffect";
	private static final String PERCENT_SPEED_EFFECT_NAME = "FrenzyPercentSpeedEffect";
	private static final String PERCENT_DAMAGE_SINGLE_EFFECT_NAME = "FrenzyPercentDamageDealtSingleEffect";
	private final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.MELEE_ENCH
	);


	public static final String CHARM_DURATION = "Frenzy Duration";
	public static final String CHARM_ATTACK_SPEED = "Frenzy Attack Speed";
	public static final String CHARM_SPEED = "Frenzy Speed";
	public static final String CHARM_BONUS_DAMAGE = "Frenzy Bonus Damage";

	public static final AbilityInfo<Frenzy> INFO =
		new AbilityInfo<>(Frenzy.class, "Frenzy", Frenzy::new)
			.scoreboardId("Frenzy")
			.shorthandName("Fnz")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Killing mobs increases your speed and attack speed.")
			.quest216Message("-------3-------o-------")
			.displayItem(Material.FEATHER);

	private final double mPercentAttackSpeedEffect;
	private final double mSpeedPotency;
	private final int mDuration;
	private final double mEnhanceDamageMult;
	private final FrenzyCS mCosmetic;

	public Frenzy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentAttackSpeedEffect = (isLevelOne() ? PERCENT_ATTACK_SPEED_EFFECT_1 : PERCENT_ATTACK_SPEED_EFFECT_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ATTACK_SPEED);
		mSpeedPotency = PERCENT_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mEnhanceDamageMult = DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS_DAMAGE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new FrenzyCS());
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_ATTACK_SPEED_EFFECT_NAME,
			new PercentAttackSpeed(mDuration, mPercentAttackSpeedEffect, PERCENT_ATTACK_SPEED_EFFECT_NAME)
				.deleteOnAbilityUpdate(true));

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME,
				new PercentSpeed(mDuration, mSpeedPotency, PERCENT_SPEED_EFFECT_NAME).deleteOnAbilityUpdate(true));
			mCosmetic.frenzyLevelTwo(mPlayer);
		} else {
			mCosmetic.frenzyLevelOne(mPlayer);
		}

		if (isEnhanced()) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_SINGLE_EFFECT_NAME,
				new PercentDamageDealtSingleTick(mDuration, mEnhanceDamageMult)
					.damageTypes(AFFECTED_DAMAGE_TYPES).deleteOnAbilityUpdate(true));
			mCosmetic.frenzyEnhancement(mPlayer);
		}
	}

	private static Description<Frenzy> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Gain ")
			.addPercent(a -> a.mPercentAttackSpeedEffect, PERCENT_ATTACK_SPEED_EFFECT_1, false, Ability::isLevelOne)
			.add(" attack speed for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds after killing a mob.");
	}

	private static Description<Frenzy> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Attack speed is increased to ")
			.addPercent(a -> a.mPercentAttackSpeedEffect, PERCENT_ATTACK_SPEED_EFFECT_2, false, Ability::isLevelTwo)
			.add(" and also gain ")
			.addPercent(a -> a.mSpeedPotency, PERCENT_SPEED)
			.add(" speed for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.");
	}

	private static Description<Frenzy> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Additionally, your next melee swing within ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds after getting a kill deals ")
			.addPercent(a -> a.mEnhanceDamageMult, DAMAGE_BONUS)
			.add(" extra damage.");
	}
}
