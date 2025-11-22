package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.EmpoweringOdorCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageDealtSingle;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class EmpoweringOdor extends Ability implements PotionAbility {
	public static final int POTION_RECHARGE_TIME_REDUCTION_2 = 10;

	private static final int EFFECT_DURATION = TICKS_PER_SECOND * 8;
	private static final double SPEED_POTENCY = 0.1;
	private static final double DAMAGE_POTENCY = 0.1;
	private static final double ENHANCE_DAMAGE_POTENCY = 0.1;
	private static final String SPEED_SRC = "EmpoweringOdorSpeedEffect";
	private static final String DAMAGE_SRC = "EmpoweringOdorDamageEffect";
	private static final String ENHANCE_DAMAGE_SRC = "EmpoweringOdorEnhancementDamageEffect";

	public static final String CHARM_DURATION = "Empowering Odor Duration";
	public static final String CHARM_SPEED = "Empowering Odor Speed Modifier";
	public static final String CHARM_DAMAGE = "Empowering Odor Damage Bonus";
	public static final String CHARM_SINGLE_HIT_DAMAGE = "Empowering Odor Single Hit Damage";

	public static final AbilityInfo<EmpoweringOdor> INFO =
		new AbilityInfo<>(EmpoweringOdor.class, "Empowering Odor", EmpoweringOdor::new)
			.linkedSpell(ClassAbility.EMPOWERING_ODOR)
			.scoreboardId("EmpoweringOdor")
			.shorthandName("EO")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Buff allies splashed by your potions.")
			.displayItem(Material.GLOWSTONE_DUST);

	private final double mDamagePotency;
	private final double mSpeedPotency;
	private final int mEffectDuration;
	private final double mEnhanceDamagePotency;
	private final EmpoweringOdorCS mCosmetic;

	public EmpoweringOdor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamagePotency = DAMAGE_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mSpeedPotency = SPEED_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mEffectDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, EFFECT_DURATION);
		mEnhanceDamagePotency = ENHANCE_DAMAGE_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SINGLE_HIT_DAMAGE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new EmpoweringOdorCS());
	}

	@Override
	public void applyToPlayer(final Player player, final ThrownPotion potion, final boolean isGruesome) {
		mPlugin.mEffectManager.addEffect(player, SPEED_SRC,
			new PercentSpeed(mEffectDuration, mSpeedPotency, SPEED_SRC).deleteOnAbilityUpdate(true));
		mPlugin.mEffectManager.addEffect(player, DAMAGE_SRC,
			new PercentDamageDealt(mEffectDuration, mDamagePotency).deleteOnAbilityUpdate(true));
		if (isEnhanced()) {
			mPlugin.mEffectManager.addEffect(player, ENHANCE_DAMAGE_SRC,
				new PercentDamageDealtSingle(mEffectDuration, mEnhanceDamagePotency).deleteOnAbilityUpdate(true));
		}
		mCosmetic.applyEffects(mPlayer, player, mEffectDuration);
	}

	private static Description<EmpoweringOdor> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Other players hit by your Alchemist's Potions are given ")
			.addPercent(a -> a.mSpeedPotency, SPEED_POTENCY)
			.add(" speed and ")
			.addPercent(a -> a.mDamagePotency, DAMAGE_POTENCY)
			.add(" damage for ")
			.addDuration(a -> a.mEffectDuration, EFFECT_DURATION)
			.add(" seconds.");
	}

	private static Description<EmpoweringOdor> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your potion recharge delay is decreased by ")
			.addDuration(POTION_RECHARGE_TIME_REDUCTION_2)
			.add(" seconds.");
	}

	private static Description<EmpoweringOdor> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The first hit a player would deal to an enemy after they gain this bonus is increased by ")
			.addPercent(a -> a.mEnhanceDamagePotency, ENHANCE_DAMAGE_POTENCY)
			.add(", refreshing on each application.");
	}
}
