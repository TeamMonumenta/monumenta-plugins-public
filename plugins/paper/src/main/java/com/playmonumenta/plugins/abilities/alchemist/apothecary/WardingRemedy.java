package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.WardingRemedyCS;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WardingRemedy extends Ability implements AbilityWithDuration {

	private static final int WARDING_REMEDY_1_COOLDOWN = 20 * 30;
	private static final int WARDING_REMEDY_2_COOLDOWN = 20 * 25;
	private static final int WARDING_REMEDY_PULSES = 8;
	private static final int WARDING_REMEDY_PULSE_DELAY = 10;
	private static final int WARDING_REMEDY_ABSORPTION = 1;
	private static final int WARDING_REMEDY_MAX_ABSORPTION = 6;
	private static final int WARDING_REMEDY_ABSORPTION_DURATION = 20 * 30;
	private static final int WARDING_REMEDY_RANGE = 12;
	private static final double WARDING_REMEDY_HEAL_MULTIPLIER = 0.1;
	private static final double WARDING_REMEDY_ACTIVE_RADIUS = 6;

	public static final String CHARM_COOLDOWN = "Warding Remedy Cooldown";
	public static final String CHARM_PULSES = "Warding Remedy Pulses";
	public static final String CHARM_DELAY = "Warding Remedy Pulse Delay";
	public static final String CHARM_ABSORPTION = "Warding Remedy Absorption Health";
	public static final String CHARM_MAX_ABSORPTION = "Warding Remedy Max Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Warding Remedy Absorption Duration";
	public static final String CHARM_RADIUS = "Warding Remedy Radius";
	public static final String CHARM_HEALING = "Warding Remedy Healing Bonus";

	public static final AbilityInfo<WardingRemedy> INFO =
		new AbilityInfo<>(WardingRemedy.class, "Warding Remedy", WardingRemedy::new)
			.linkedSpell(ClassAbility.WARDING_REMEDY)
			.scoreboardId("WardingRemedy")
			.shorthandName("WR")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Periodically grant absorption to you and nearby allies, for a short period of time.")
			.cooldown(WARDING_REMEDY_1_COOLDOWN, WARDING_REMEDY_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WardingRemedy::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(Material.GOLDEN_CARROT);

	private final int mDelay;
	private final int mTotalPulses;
	private final double mActiveRadius;
	private final double mAbsorption;
	private final double mMaxAbsorption;
	private final int mAbsorptionDuration;
	private final double mHealing;
	private final double mRange;
	private final WardingRemedyCS mCosmetic;

	public WardingRemedy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDelay = CharmManager.getDuration(mPlayer, CHARM_DELAY, WARDING_REMEDY_PULSE_DELAY);
		mTotalPulses = WARDING_REMEDY_PULSES + (int) CharmManager.getLevel(mPlayer, CHARM_PULSES);
		mActiveRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, WARDING_REMEDY_ACTIVE_RADIUS);
		mAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, WARDING_REMEDY_ABSORPTION);
		mMaxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MAX_ABSORPTION, WARDING_REMEDY_MAX_ABSORPTION);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, WARDING_REMEDY_ABSORPTION_DURATION);
		mHealing = WARDING_REMEDY_HEAL_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALING);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RADIUS, WARDING_REMEDY_RANGE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new WardingRemedyCS());
	}

	private int mCurrDuration = -1;

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		mCosmetic.remedyStartEffect(world, loc, mPlayer, mActiveRadius);

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);

		cancelOnDeath(new BukkitRunnable() {
			int mPulses = 0;
			int mTick = mDelay;

			@Override
			public void run() {
				Location playerLoc = mPlayer.getLocation();

				mCosmetic.remedyPeriodicEffect(playerLoc.clone().add(0, 0.5, 0), mPlayer, mTick + mPulses * mDelay);

				if (mTick >= mDelay) {
					mCosmetic.remedyPulseEffect(world, playerLoc, mPlayer, mPulses, mTotalPulses, mActiveRadius);

					for (Player p : PlayerUtils.playersInRange(playerLoc, mActiveRadius, true)) {
						AbsorptionUtils.addAbsorption(p, mAbsorption, mMaxAbsorption, mAbsorptionDuration);
						mCosmetic.remedyApplyEffect(mPlayer, p);
					}
					mTick = 0;
					mPulses++;
					if (mPulses >= mTotalPulses) {
						this.cancel();
					}
				}

				mTick++;
				if (mCurrDuration >= 0) {
					mCurrDuration++;
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, WardingRemedy.this);
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		//Triggers four times a second

		if (isLevelOne()) {
			return;
		}

		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), mRange, true)) {
			if (AbsorptionUtils.getAbsorption(p) > 0) {
				mPlugin.mEffectManager.addEffect(p, "WardingRemedyBonusHealing", new PercentHeal(20, mHealing)
					.displaysTime(false).deleteOnAbilityUpdate(true));
				mCosmetic.remedyHealBuffEffect(mPlayer, p);
			}
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return mTotalPulses * mDelay;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	private static Description<WardingRemedy> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to give players (including yourself) within ")
			.add(a -> a.mActiveRadius, WARDING_REMEDY_ACTIVE_RADIUS)
			.add(" blocks ")
			.add(a -> a.mAbsorption, WARDING_REMEDY_ABSORPTION)
			.add(" absorption health every ")
			.addDuration(a -> a.mDelay, WARDING_REMEDY_PULSE_DELAY)
			.add(" seconds for ")
			.addDuration(WardingRemedy::getInitialAbilityDuration, WARDING_REMEDY_PULSES * WARDING_REMEDY_PULSE_DELAY)
			.add(" seconds, lasting ")
			.addDuration(a -> a.mAbsorptionDuration, WARDING_REMEDY_ABSORPTION_DURATION)
			.add(" seconds, up to ")
			.add(a -> a.mMaxAbsorption, WARDING_REMEDY_MAX_ABSORPTION)
			.add(" absorption health.")
			.addCooldown(WARDING_REMEDY_1_COOLDOWN, Ability::isLevelOne);
	}

	private static Description<WardingRemedy> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("You and allies within ")
			.add(a -> a.mRange, WARDING_REMEDY_RANGE)
			.add(" blocks passively gain ")
			.addPercent(a -> a.mHealing, WARDING_REMEDY_HEAL_MULTIPLIER)
			.add(" increased healing while having absorption health.")
			.addCooldown(WARDING_REMEDY_2_COOLDOWN, Ability::isLevelTwo);
	}
}
