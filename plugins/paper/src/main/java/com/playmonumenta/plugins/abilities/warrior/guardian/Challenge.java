package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.ChallengeCS;
import com.playmonumenta.plugins.effects.ChallengeMobEffect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_MINUTE;

public class Challenge extends Ability {
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "ChallengePercentDamageDealtEffect";
	private static final String SPEED_EFFECT_NAME = "ChallengePercentSpeedEffect";
	private static final String AFFECTED_MOB_EFFECT_NAME = "ChallengeMobEffect";
	private static final int DURATION = Constants.TICKS_PER_SECOND * 10;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_1 = 0.2;
	private static final double PERCENT_DAMAGE_DEALT_PER_1 = 0.05;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_2 = 0.3;
	private static final double PERCENT_DAMAGE_DEALT_PER_2 = 0.075;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = DamageType.getAllMeleeTypes();

	private static final int ABSORPTION_PER_MOB_1 = 1;
	private static final int ABSORPTION_PER_MOB_2 = 2;
	private static final int MAX_ABSORPTION_1 = 4;
	private static final int MAX_ABSORPTION_2 = 8;
	private static final int CHALLENGE_RANGE = 14;
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND * 20;
	private static final int KILLED_MOBS_CAP = 6;
	private static final double SPEED_PER = 0.04;
	private static final int CDR_PER = 10;

	public static final String CHARM_DURATION = "Challenge Duration";
	public static final String CHARM_DAMAGE_PER = "Challenge Melee Damage Amplifier Per Mob";
	public static final String CHARM_DAMAGE_MAX = "Challenge Max Melee Damage Amplifier";
	public static final String CHARM_ABSORPTION_PER = "Challenge Absorption Health Per Mob";
	public static final String CHARM_ABSORPTION_MAX = "Challenge Max Absorption Health";
	public static final String CHARM_SPEED_PER = "Challenge Speed Per Mob";
	public static final String CHARM_CDR_PER = "Challenge Cooldown Reduction Per Mob";
	public static final String CHARM_RANGE = "Challenge Range";
	public static final String CHARM_COOLDOWN = "Challenge Cooldown";
	public static final String CHARM_MAX_MOBS = "Challenge Max Mobs";

	public static final AbilityInfo<Challenge> INFO =
		new AbilityInfo<>(Challenge.class, "Challenge", Challenge::new)
			.linkedSpell(ClassAbility.CHALLENGE)
			.scoreboardId("Challenge")
			.shorthandName("Ch")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Taunt nearby enemies and gain absorption and melee damage.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Challenge::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)))
			.displayItem(Material.IRON_AXE);

	private final double mRadius;
	private final double mPercentDamageDealtPerMob;
	private final double mPercentDamageDealtEffect;
	private final double mAbsorptionPerMob;
	private final double mMaxAbsorption;
	private final int mKilledMobsCap;
	private final double mSpeedPerMob;
	private final int mCDRPerMob;
	private final int mDuration;
	private @Nullable CounterStrike mCounterStrike;

	private List<LivingEntity> mAffectedEntities = new ArrayList<>();
	private int mKillCount = 0;

	private final ChallengeCS mCosmetic;

	public Challenge(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RANGE, CHALLENGE_RANGE);
		mPercentDamageDealtPerMob = (isLevelOne() ? PERCENT_DAMAGE_DEALT_PER_1 : PERCENT_DAMAGE_DEALT_PER_2)
			+ CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_PER);
		mPercentDamageDealtEffect = (isLevelOne() ? PERCENT_DAMAGE_DEALT_EFFECT_1 : PERCENT_DAMAGE_DEALT_EFFECT_2)
			+ CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_MAX);
		mAbsorptionPerMob = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_PER,
			isLevelOne() ? ABSORPTION_PER_MOB_1 : ABSORPTION_PER_MOB_2);
		mMaxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_MAX,
			isLevelOne() ? MAX_ABSORPTION_1 : MAX_ABSORPTION_2);
		mKilledMobsCap = KILLED_MOBS_CAP + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_MOBS);
		mSpeedPerMob = SPEED_PER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED_PER);
		mCDRPerMob = CharmManager.getDuration(mPlayer, CHARM_CDR_PER, CDR_PER);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new ChallengeCS());
		Bukkit.getScheduler().runTask(mPlugin, () ->
			mCounterStrike = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, CounterStrike.class));
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		final Location playerLoc = mPlayer.getLocation();
		final List<LivingEntity> mobs = new Hitbox.SphereHitbox(playerLoc, mRadius).getHitMobs();
		mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
		if (mobs.isEmpty()) {
			return false;
		}

		AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionPerMob * mobs.size(), mMaxAbsorption, mDuration);
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME,
			new PercentDamageDealt(mDuration, Math.min(mobs.size() * mPercentDamageDealtPerMob, mPercentDamageDealtEffect))
				.damageTypes(AFFECTED_DAMAGE_TYPES).deleteOnAbilityUpdate(true));

		mobs.stream().filter(mob -> mob instanceof Mob).forEach(mob -> {
			EntityUtils.applyTaunt(mob, mPlayer);
			if (mCounterStrike != null) {
				mCounterStrike.onTaunt(mob);
			}
		});
		if (isLevelTwo()) {
			clearAffectedEntities();
			mAffectedEntities = mobs;
			mobs.forEach(mob -> {
				mPlugin.mEffectManager.addEffect(mob, AFFECTED_MOB_EFFECT_NAME,
					new ChallengeMobEffect(TICKS_PER_MINUTE, this));
				mCosmetic.onCastEffect(mPlayer, mPlayer.getWorld(), mob.getLocation());
			});
		}

		mCosmetic.onCast(mPlayer, mPlayer.getWorld(), playerLoc);
		putOnCooldown();
		return true;
	}

	public void incrementKills(final LivingEntity mob) {
		mKillCount++;
		mCosmetic.killMob(mPlayer, mPlayer.getWorld(), mob.getLocation());

		if (mKillCount >= mKilledMobsCap || mKillCount > mAffectedEntities.size()) {
			mCosmetic.maxMobs(mPlayer, mPlayer.getWorld(), mPlayer.getLocation());
			clearAffectedEntities();
			return;
		}

		mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME,
			new PercentSpeed(mDuration, mKillCount * mSpeedPerMob, SPEED_EFFECT_NAME).deleteOnAbilityUpdate(true));
		EnumSet.of(ClassAbility.CHALLENGE, ClassAbility.BODYGUARD, ClassAbility.SHIELD_WALL)
			.forEach(ca -> mPlugin.mTimers.updateCooldown(mPlayer, ca, mCDRPerMob));

		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, (float) Math.pow(2, ((mKillCount - 12) / 12.0)));
	}

	public void clearAffectedEntities() {
		mAffectedEntities.stream().filter(mob -> mob.isValid() &&
			!mob.isDead()).forEach(mob -> mPlugin.mEffectManager.clearEffects(mob, AFFECTED_MOB_EFFECT_NAME));
		mAffectedEntities = new ArrayList<>();
		mKillCount = 0;
	}

	private static Description<Challenge> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to make all mobs within ")
			.add(a -> a.mRadius, CHALLENGE_RANGE)
			.add(" blocks target you. You gain ")
			.add(a -> a.mAbsorptionPerMob, ABSORPTION_PER_MOB_1, false, Ability::isLevelOne)
			.add(" absorption and ")
			.addPercent(a -> a.mPercentDamageDealtPerMob, PERCENT_DAMAGE_DEALT_PER_1, false, Ability::isLevelOne)
			.add(" melee damage per affected mob (up to ")
			.add(a -> a.mMaxAbsorption, MAX_ABSORPTION_1, false, Ability::isLevelOne)
			.add(" absorption and ")
			.addPercent(a -> a.mPercentDamageDealtEffect, PERCENT_DAMAGE_DEALT_EFFECT_1, false, Ability::isLevelOne)
			.add(" melee damage) for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

	private static Description<Challenge> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Gain ")
			.add(a -> a.mAbsorptionPerMob, ABSORPTION_PER_MOB_2, false, Ability::isLevelTwo)
			.add(" absorption and ")
			.addPercent(a -> a.mPercentDamageDealtPerMob, PERCENT_DAMAGE_DEALT_PER_2, false, Ability::isLevelTwo)
			.add(" melee damage per affected mob (up to ")
			.add(a -> a.mMaxAbsorption, MAX_ABSORPTION_2, false, Ability::isLevelTwo)
			.add(" absorption and ")
			.addPercent(a -> a.mPercentDamageDealtEffect, PERCENT_DAMAGE_DEALT_EFFECT_2, false, Ability::isLevelTwo)
			.add(" melee damage) instead. For each taunted mob killed (up to ")
			.add(a -> a.mKilledMobsCap, KILLED_MOBS_CAP)
			.add("), gain a stackable ")
			.addPercent(a -> a.mSpeedPerMob, SPEED_PER)
			.add(" speed for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds and reduce the cooldown of Guardian skills by ")
			.addDuration(a -> a.mCDRPerMob, CDR_PER)
			.add(" seconds.");
	}
}
