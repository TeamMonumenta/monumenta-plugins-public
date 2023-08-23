package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.SmokescreenCS;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Smokescreen extends Ability implements AbilityWithDuration {

	private static final int SMOKESCREEN_RANGE = 6;
	private static final int SMOKESCREEN_DURATION = 8 * 20;
	private static final double SMOKESCREEN_SLOWNESS_AMPLIFIER = 0.2;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.4;
	private static final int SMOKESCREEN_COOLDOWN = 20 * 20;
	private static final int ENHANCEMENT_SMOKECLOUD_DURATION = 8 * 20;
	private static final int ENHANCEMENT_SMOKECLOUD_EFFECT_DURATION = 2 * 20;
	private static final int ENHANCEMENT_SMOKECLOUD_RADIUS = 4;

	public static final String CHARM_SLOW = "Smokescreen Slowness Amplifier";
	public static final String CHARM_WEAKEN = "Smokescreen Weakness Amplifier";
	public static final String CHARM_COOLDOWN = "Smokescreen Cooldown";
	public static final String CHARM_RANGE = "Smokescreen Range";

	private final SmokescreenCS mCosmetic;

	private int mCurrDuration = -1;

	public static final AbilityInfo<Smokescreen> INFO =
			new AbilityInfo<>(Smokescreen.class, "Smokescreen", Smokescreen::new)
					.linkedSpell(ClassAbility.SMOKESCREEN)
					.scoreboardId("SmokeScreen")
					.shorthandName("Smk")
					.descriptions(
							String.format("When holding two swords, right-click while sneaking and looking down to release a cloud of smoke, " +
											"afflicting all enemies in a %s block radius with %ss of %s%% Weaken and %s%% Slowness. Cooldown: %ss.",
									SMOKESCREEN_RANGE,
									SMOKESCREEN_DURATION / 20,
									(int) (WEAKEN_EFFECT_1 * 100),
									(int) (SMOKESCREEN_SLOWNESS_AMPLIFIER * 100),
									SMOKESCREEN_COOLDOWN / 20),
							String.format("The Weaken debuff is increased to %s%%.",
									(int) (WEAKEN_EFFECT_2 * 100)),
							String.format("Leave a %s block radius persistent cloud on the ground for %s seconds after activating. " +
											"Enemies in the cloud gain the same debuffs for %s seconds, pulsing every second.",
									ENHANCEMENT_SMOKECLOUD_RADIUS,
									ENHANCEMENT_SMOKECLOUD_DURATION / 20,
									ENHANCEMENT_SMOKECLOUD_EFFECT_DURATION / 20))
					.simpleDescription("Weaken and slow nearby mobs.")
					.cooldown(SMOKESCREEN_COOLDOWN, CHARM_COOLDOWN)
					.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Smokescreen::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).lookDirections(AbilityTrigger.LookDirection.DOWN),
							AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
					.displayItem(Material.DEAD_TUBE_CORAL);

	private final double mWeakenEffect;

	public Smokescreen(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SmokescreenCS());
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKEN) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();

		mCosmetic.smokescreenEffects(mPlayer, world, loc);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, CharmManager.getRadius(mPlayer, CHARM_RANGE, SMOKESCREEN_RANGE), mPlayer)) {
			EntityUtils.applySlow(mPlugin, SMOKESCREEN_DURATION, SMOKESCREEN_SLOWNESS_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW), mob);
			EntityUtils.applyWeaken(mPlugin, SMOKESCREEN_DURATION, mWeakenEffect, mob);
		}
		putOnCooldown();

		if (isEnhanced()) {
			new BukkitRunnable() {
				final Location mCloudLocation = loc.clone();
				int mT = 0;

				@Override
				public void run() {
					if (mT > ENHANCEMENT_SMOKECLOUD_DURATION) {
						this.cancel();
						return;
					} else {
						if (mT > 0) {
							mCosmetic.residualEnhanceEffects(mPlayer, world, mCloudLocation);

							for (LivingEntity mob : EntityUtils.getNearbyMobs(mCloudLocation, ENHANCEMENT_SMOKECLOUD_RADIUS, mPlayer)) {
								EntityUtils.applySlow(mPlugin, ENHANCEMENT_SMOKECLOUD_EFFECT_DURATION, SMOKESCREEN_SLOWNESS_AMPLIFIER, mob);
								EntityUtils.applyWeaken(mPlugin, ENHANCEMENT_SMOKECLOUD_EFFECT_DURATION, mWeakenEffect, mob);
							}
						}
						mT += 20;
						mCurrDuration += 20;
					}
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					mCurrDuration = -1;
					ClientModHandler.updateAbility(mPlayer, Smokescreen.this);
				}
			}.runTaskTimer(mPlugin, 0, 20);
		}

		ClientModHandler.updateAbility(mPlayer, this);
	}

	@Override
	public int getInitialAbilityDuration() {
		return isEnhanced() ? ENHANCEMENT_SMOKECLOUD_DURATION : 0;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 && isEnhanced() ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}
}
