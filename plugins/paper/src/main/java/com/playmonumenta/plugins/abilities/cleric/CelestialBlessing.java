package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.CelestialBlessingCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.CelestialBlessingDamageBuff;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class CelestialBlessing extends Ability {
	private static final int CELESTIAL_COOLDOWN = TICKS_PER_SECOND * 35;
	private static final int CELESTIAL_DURATION = TICKS_PER_SECOND * 15;
	private static final double CELESTIAL_RADIUS = 12;
	private static final double CELESTIAL_1_EXTRA_DAMAGE = 0.20;
	private static final double CELESTIAL_2_EXTRA_DAMAGE = 0.30;
	private static final double CELESTIAL_EXTRA_SPEED = 0.20;
	private static final String ATTR_NAME = "CelestialBlessingExtraSpeedAttr";
	public static final int CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED = TICKS_PER_SECOND * 5;

	public static final String DAMAGE_EFFECT_NAME = "CelestialBlessingExtraDamage";
	public static final String SPEED_EFFECT_NAME = "CelestialBlessingExtraSpeed";
	public static final String PARTICLE_EFFECT_NAME = "CelestialBlessingParticles";
	public static final String CHARM_DAMAGE = "Celestial Blessing Damage Modifier";
	public static final String CHARM_COOLDOWN = "Celestial Blessing Cooldown";
	public static final String CHARM_RADIUS = "Celestial Blessing Radius";
	public static final String CHARM_SPEED = "Celestial Blessing Speed Amplifier";
	public static final String CHARM_DURATION = "Celestial Blessing Duration";

	public static final AbilityInfo<CelestialBlessing> INFO =
		new AbilityInfo<>(CelestialBlessing.class, "Celestial Blessing", CelestialBlessing::new)
			.linkedSpell(ClassAbility.CELESTIAL_BLESSING)
			.scoreboardId("Celestial")
			.shorthandName("CB")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Grant yourself and nearby players speed and increased damage.")
			.cooldown(CELESTIAL_COOLDOWN, CELESTIAL_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CelestialBlessing::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.SUGAR);

	private final int mDuration;
	private final double mExtraDamage;
	private final double mSpeedPotency;
	private final double mRadius;
	private final CelestialBlessingCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public CelestialBlessing(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, CELESTIAL_DURATION);
		mExtraDamage = (isLevelTwo() ? CELESTIAL_2_EXTRA_DAMAGE : CELESTIAL_1_EXTRA_DAMAGE) +
			CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mSpeedPotency = CELESTIAL_EXTRA_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, CELESTIAL_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new CelestialBlessingCS());

		Bukkit.getScheduler().runTask(mPlugin, () ->
			mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Crusade.class));
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		final List<Player> affectedPlayers = PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true);

		// Don't buff players that have their class disabled
		affectedPlayers.removeIf(p -> p.getScoreboardTags().contains("disable_class"));

		for (final Player p : affectedPlayers) {
			mPlugin.mEffectManager.addEffect(p, DAMAGE_EFFECT_NAME,
				new CelestialBlessingDamageBuff(mDuration, mExtraDamage, isEnhanced(), mCosmetic, p, null)
					.deleteOnAbilityUpdate(true));
			mPlugin.mEffectManager.addEffect(p, SPEED_EFFECT_NAME, new PercentSpeed(mDuration, mSpeedPotency, ATTR_NAME)
				.deleteOnAbilityUpdate(true));
			mPlugin.mEffectManager.addEffect(p, PARTICLE_EFFECT_NAME, new Aesthetics(mDuration,
				(entity, fourHertz, twoHertz, oneHertz) ->
					mCosmetic.tickEffect(mPlayer, p, fourHertz, twoHertz, oneHertz),
				(entity) -> mCosmetic.loseEffect(mPlayer, p))
				.deleteOnAbilityUpdate(true)
			);
			mCosmetic.startEffectTargets(p);
		}
		mCosmetic.startEffectCaster(mPlayer, mRadius);

		putOnCooldown();
		return true;
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (mPlugin.mEffectManager.hasEffect(mPlayer, DAMAGE_EFFECT_NAME)) {
			Crusade.addCrusadeTag(enemy, mCrusade);
		}
		return false;
	}

	private static Description<CelestialBlessing> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to grant all players within ")
			.add(a -> a.mRadius, CELESTIAL_RADIUS)
			.add(" blocks ")
			.addPercent(a -> a.mExtraDamage, CELESTIAL_1_EXTRA_DAMAGE, false, Ability::isLevelOne)
			.add(" damage and ")
			.addPercent(a -> a.mSpeedPotency, CELESTIAL_EXTRA_SPEED)
			.add(" speed for ")
			.addDuration(a -> a.mDuration, CELESTIAL_DURATION)
			.add(" seconds. Dealing damage affected by this effect triggers Crusade's target marking.")
			.addCooldown(CELESTIAL_COOLDOWN);
	}

	private static Description<CelestialBlessing> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage buff is increased to ")
			.addPercent(a -> a.mExtraDamage, CELESTIAL_2_EXTRA_DAMAGE, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<CelestialBlessing> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Celestial Blessing can be extended by ")
			.addDuration(CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED)
			.add(" seconds by performing a fully-charged melee attack, critical projectile attack, and ability hit on any enemy during its duration.");
	}
}
