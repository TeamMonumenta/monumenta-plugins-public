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
import com.playmonumenta.plugins.effects.PercentDamageReceived;
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

	private static final int COOLDOWN_1 = 20 * 30;
	private static final int COOLDOWN_2 = 20 * 25;
	private static final int PULSES = 8;
	private static final int PULSE_DELAY = 10;
	private static final int ABSORPTION = 1;
	private static final int MAX_ABSORPTION = 6;
	private static final int ABSORPTION_DURATION = 20 * 30;
	private static final int RANGE = 12;
	private static final double RESISTANCE_AMP = 0.1;

	public static final String CHARM_COOLDOWN = "Warding Remedy Cooldown";
	public static final String CHARM_PULSES = "Warding Remedy Pulses";
	public static final String CHARM_DELAY = "Warding Remedy Pulse Delay";
	public static final String CHARM_ABSORPTION = "Warding Remedy Absorption Health";
	public static final String CHARM_MAX_ABSORPTION = "Warding Remedy Max Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Warding Remedy Absorption Duration";
	public static final String CHARM_RADIUS = "Warding Remedy Radius";
	public static final String CHARM_RESISTANCE = "Warding Remedy Resistance Amplifier";

	public static final AbilityInfo<WardingRemedy> INFO =
		new AbilityInfo<>(WardingRemedy.class, "Warding Remedy", WardingRemedy::new)
			.linkedSpell(ClassAbility.WARDING_REMEDY)
			.scoreboardId("WardingRemedy")
			.shorthandName("WR")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Periodically grant absorption to you and nearby allies, for a short period of time.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WardingRemedy::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(Material.GOLDEN_CARROT);

	private final int mDelay;
	private final int mTotalPulses;
	private final double mAbsorption;
	private final double mMaxAbsorption;
	private final int mAbsorptionDuration;
	private final double mResistanceAmp;
	private final double mRange;
	private final WardingRemedyCS mCosmetic;

	public WardingRemedy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDelay = CharmManager.getDuration(mPlayer, CHARM_DELAY, PULSE_DELAY);
		mTotalPulses = PULSES + (int) CharmManager.getLevel(mPlayer, CHARM_PULSES);
		mAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, ABSORPTION);
		mMaxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MAX_ABSORPTION, MAX_ABSORPTION);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, ABSORPTION_DURATION);
		mResistanceAmp = RESISTANCE_AMP + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RANGE);
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

		mCosmetic.remedyStartEffect(world, loc, mPlayer, mRange);

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
					mCosmetic.remedyPulseEffect(world, playerLoc, mPlayer, mPulses, mTotalPulses, mRange);

					for (Player p : PlayerUtils.playersInRange(playerLoc, mRange, true)) {
						AbsorptionUtils.addAbsorption(p, mPlayer, mAbsorption, mMaxAbsorption, mAbsorptionDuration);
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
		if (isLevelOne()) {
			return;
		}

		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), mRange, true)) {
			if (AbsorptionUtils.getAbsorption(p) > 0) {
				mPlugin.mEffectManager.addEffect(p, "WardingRemedyResistance", new PercentDamageReceived(20, -mResistanceAmp)
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
			.add(a -> a.mRange, RANGE)
			.add(" blocks ")
			.add(a -> a.mAbsorption, ABSORPTION)
			.add(" absorption health every ")
			.addDuration(a -> a.mDelay, PULSE_DELAY)
			.add(" seconds for ")
			.addDuration(WardingRemedy::getInitialAbilityDuration, PULSES * PULSE_DELAY)
			.add(" seconds, lasting ")
			.addDuration(a -> a.mAbsorptionDuration, ABSORPTION_DURATION)
			.add(" seconds, up to ")
			.add(a -> a.mMaxAbsorption, MAX_ABSORPTION)
			.add(" absorption health.")
			.addCooldown(COOLDOWN_1, Ability::isLevelOne);
	}

	private static Description<WardingRemedy> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("You and allies within ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks passively gain ")
			.addPercent(a -> a.mResistanceAmp, RESISTANCE_AMP)
			.add(" resistance while having absorption health.")
			.addCooldown(COOLDOWN_2, Ability::isLevelTwo);
	}
}
