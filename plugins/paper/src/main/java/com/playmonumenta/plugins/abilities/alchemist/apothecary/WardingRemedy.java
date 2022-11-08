package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.WardingRemedyCS;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class WardingRemedy extends Ability {

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
	public static final String CHARM_FREQUENCY = "Warding Remedy Pulse Frequency";
	public static final String CHARM_ABSORPTION = "Warding Remedy Absorption Health";
	public static final String CHARM_MAX_ABSORPTION = "Warding Remedy Max Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Warding Remedy Absorption Duration";
	public static final String CHARM_RADIUS = "Warding Remedy Radius";
	public static final String CHARM_HEALING = "Warding Remedy Healing Bonus";

	private final WardingRemedyCS mCosmetic;

	public WardingRemedy(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Warding Remedy");
		mInfo.mScoreboardId = "WardingRemedy";
		mInfo.mLinkedSpell = ClassAbility.WARDING_REMEDY;
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, isLevelOne() ? WARDING_REMEDY_1_COOLDOWN : WARDING_REMEDY_2_COOLDOWN);
		mInfo.mIgnoreCooldown = true;
		mInfo.mShorthandName = "WR";
		mInfo.mDescriptions.add("Swap hands while sneaking and holding an Alchemist's Bag to give players (including yourself) within a 6 block radius 1 absorption health per 0.5 seconds for 4 seconds, lasting 30 seconds, up to 6 absorption health. Cooldown: 30s.");
		mInfo.mDescriptions.add("You and allies in a 12 block radius passively gain 10% increased healing while having absorption health, and cooldown decreased to 25s.");
		mDisplayItem = new ItemStack(Material.GOLDEN_CARROT, 1);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new WardingRemedyCS(), WardingRemedyCS.SKIN_LIST);
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer == null || !mPlayer.isSneaking() || isTimerActive() || !ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand())) {
			return;
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		int delay = WARDING_REMEDY_PULSE_DELAY - CharmManager.getExtraDuration(mPlayer, CHARM_FREQUENCY);
		int pulses = WARDING_REMEDY_PULSES + (int) CharmManager.getLevel(mPlayer, CHARM_PULSES);
		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, WARDING_REMEDY_ACTIVE_RADIUS);
		double absorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, WARDING_REMEDY_ABSORPTION);
		double maxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MAX_ABSORPTION, WARDING_REMEDY_MAX_ABSORPTION);
		int absorptionDuration = WARDING_REMEDY_ABSORPTION_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_ABSORPTION_DURATION);

		mCosmetic.remedyStartEffect(world, loc, mPlayer, radius);

		new BukkitRunnable() {
			int mPulses = 0;
			int mTick = 10;
			final ImmutableList<PPPeriodic> mParticles = mCosmetic.remedyPeriodicEffect(mPlayer.getLocation());

			@Override
			public void run() {
				if (mPlayer == null) {
					this.cancel();
					return;
				}

				Location playerLoc = mPlayer.getLocation();

				for (PPPeriodic ppp : mParticles) {
					ppp.location(playerLoc.clone().add(0, 0.5, 0)).spawnAsPlayerActive(mPlayer);
				}

				if (mTick >= delay) {
					mCosmetic.remedyPulseEffect(world, playerLoc, mPlayer, mPulses, pulses, radius);

					for (Player p : PlayerUtils.playersInRange(playerLoc, radius, true)) {
						AbsorptionUtils.addAbsorption(p, absorption, maxAbsorption, absorptionDuration);
						mCosmetic.remedyApplyEffect(mPlayer, p);
					}
					mTick = 0;
					mPulses++;
					if (mPulses >= pulses) {
						this.cancel();
					}
				}

				mTick++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		//Triggers four times a second

		if (mPlayer == null || isLevelOne()) {
			return;
		}

		double healing = WARDING_REMEDY_HEAL_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALING);
		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, WARDING_REMEDY_RANGE), true).stream().filter(player -> AbsorptionUtils.getAbsorption(player) > 0).toList()) {
			mPlugin.mEffectManager.addEffect(p, "WardingRemedyBonusHealing", new PercentHeal(20, healing).displaysTime(false));
			mCosmetic.remedyHealBuffEffect(mPlayer, p);
		}
	}

}
