package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;


public final class MeteorSlam extends Ability {
	public static final String NAME = "Meteor Slam";
	public static final ClassAbility ABILITY = ClassAbility.METEOR_SLAM;
	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickSlammed";

	public static final double DAMAGE_1 = 2.5;
	public static final double DAMAGE_2 = 3.5;
	public static final double REDUCED_DAMAGE_1 = 2;
	public static final double REDUCED_DAMAGE_2 = 2.5;
	public static final int SIZE_1 = 2;
	public static final int SIZE_2 = 3;
	public static final int JUMP_AMPLIFIER_1 = 3;
	public static final int JUMP_LEVEL_1 = JUMP_AMPLIFIER_1 + 1;
	public static final int JUMP_AMPLIFIER_2 = 4;
	public static final int JUMP_LEVEL_2 = JUMP_AMPLIFIER_2 + 1;
	public static final int DURATION_SECONDS = 2;
	public static final int DURATION_TICKS = DURATION_SECONDS * 20;
	public static final int AUTOMATIC_THRESHOLD = 3; // Minimum fall distance for landing to automatically trigger slam attack
	public static final double MANUAL_THRESHOLD = 1.5; // Minimum fall distance for attacks to trigger slam attack
	public static final int REDUCED_THRESHOLD = 8; // Fall distance past which damage transitions from starting to ending damage
	public static final int COOLDOWN_SECONDS_1 = 8;
	public static final int COOLDOWN_TICKS_1 = COOLDOWN_SECONDS_1 * 20;
	public static final int COOLDOWN_SECONDS_2 = 6;
	public static final int COOLDOWN_TICKS_2 = COOLDOWN_SECONDS_2 * 20;

	public static final String CHARM_DAMAGE = "Meteor Slam Damage";
	public static final String CHARM_RADIUS = "Meteor Slam Radius";
	public static final String CHARM_JUMP_BOOST = "Meteor Slam Jump Boost";
	public static final String CHARM_DURATION = "Meteor Slam Duration";
	public static final String CHARM_THRESHOLD = "Meteor Slam Fall Requirement";
	public static final String CHARM_COOLDOWN = "Meteor Slam Cooldown";

	private final double mLevelDamage;
	private final double mLevelReducedDamage;
	private final double mLevelSize;
	private final int mLevelJumpAmplifier;
	private final BukkitRunnable mSlamAttackRunner;
	private boolean mHasGloriousBattle = false;

	private double mFallFromY = -7050;

	public MeteorSlam(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "MeteorSlam";
		mInfo.mShorthandName = "MS";
		mInfo.mDescriptions.add(
				String.format(
						"Pressing the swap key grants you Jump Boost %s for %ss instead of doing its vanilla function. Cooldown: %ss. Falling more than %s blocks passively generates a slam when you land, dealing %s melee damage to all enemies in a %s-block cube around you per block fallen for the first %s blocks, and %s damage per block thereafter. Falling more than %s blocks and attacking an enemy also passively generates a slam at that enemy, and resets your blocks fallen. If any enemies are damaged by a slam, you take no fall damage from that fall.",
						JUMP_LEVEL_1,
						DURATION_SECONDS,
						COOLDOWN_SECONDS_1,
						AUTOMATIC_THRESHOLD,
						DAMAGE_1,
						SIZE_1,
						REDUCED_THRESHOLD,
						REDUCED_DAMAGE_1,
						MANUAL_THRESHOLD
				)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Jump Boost level is increased from %s to %s. Cooldown is reduced from %ss to %ss. Damage is increased from %s to %s per block fallen for the first %s blocks, and from %s to %s per block thereafter. Damage size is increased from %s to %s blocks.",
				JUMP_LEVEL_1,
				JUMP_LEVEL_2,
				COOLDOWN_SECONDS_1,
				COOLDOWN_SECONDS_2,
				DAMAGE_1,
				DAMAGE_2,
				REDUCED_THRESHOLD,
				REDUCED_DAMAGE_1,
				REDUCED_DAMAGE_2,
				SIZE_1,
				SIZE_2
			)
		);
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.FIRE_CHARGE, 1);

		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, isLevelOne() ? COOLDOWN_TICKS_1 : COOLDOWN_TICKS_2);

		mLevelDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mLevelReducedDamage = isLevelOne() ? REDUCED_DAMAGE_1 : REDUCED_DAMAGE_2;
		mLevelSize = CharmManager.getRadius(mPlayer, CHARM_RADIUS, (isLevelOne() ? SIZE_1 : SIZE_2));
		mLevelJumpAmplifier = (isLevelOne() ? JUMP_AMPLIFIER_1 : JUMP_AMPLIFIER_2) + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST);

		Bukkit.getScheduler().runTask(mPlugin, () -> {
			mHasGloriousBattle = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, GloriousBattle.class) != null;
		});

		mSlamAttackRunner = new BukkitRunnable() {
			@Override
			public void run() {
				if (player == null) {
					this.cancel();
					return;
				}
				if (
					AbilityManager.getManager().getPlayerAbility(player, MeteorSlam.class) == null
					|| !player.isValid() // Ensure player is not dead, is still online?
				) {
					// If reached this point but not silenced, then proceed with cancelling
					// If silenced, only return to not run anything, but don't cancel runnable
					if (!AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
						this.cancel();
					}
					return;
				}

				if (!player.isOnGround()) {
					updateFallFrom(); // Vanilla fall distance would be 0 if on ground
				} else {
					// Currently on ground

					// If first tick landing, should still have old mFallFromY to calculate using
					// Therefore can damage if eligible
					if (calculateFallDistance() > (AUTOMATIC_THRESHOLD + CharmManager.getLevel(mPlayer, CHARM_THRESHOLD))) {
						// Only for checking in LivingEntityDamagedByPlayerEvent below,
						// so doesn't slam twice, since this doesn't yet set fall distance to 0
						MetadataUtils.checkOnceThisTick(plugin, player, SLAM_ONCE_THIS_TICK_METAKEY);

						doSlamAttack(player.getLocation().add(0, 0.15, 0));
					}

					// Whether or not did attack, now that on ground, forget mFallFromY
					mFallFromY = -7050;
				}
			}
		};
		mSlamAttackRunner.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer != null && event.getType() == DamageType.MELEE && calculateFallDistance() > MANUAL_THRESHOLD && MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, SLAM_ONCE_THIS_TICK_METAKEY)) {
			doSlamAttack(enemy.getLocation().add(0, 0.15, 0));
			mFallFromY = -7050;
			return true;
		}
		return false;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer != null
			    && !isTimerActive()
			    && !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
			    && (!mHasGloriousBattle || !mPlayer.isSneaking())) {
			putOnCooldown();

			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, DURATION_TICKS + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION), mLevelJumpAmplifier, true, false));

			World world = mPlayer.getWorld();
			Location location = mPlayer.getLocation().add(0, 0.15, 0);
			world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
			new PartialParticle(Particle.LAVA, location, 15, 1, 0f, 1, 0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FLAME, location)
				.count(30)
				.delta(3, 0, 3)
				.deltaVariance(true, false, true)
				.extra(0.2)
				.extraVariance(0.1)
				.directionalMode(true)
				.spawnAsPlayerActive(mPlayer);
		}
	}

	@Override
	public void invalidate() {
		if (mSlamAttackRunner != null) {
			mSlamAttackRunner.cancel();
		}
	}

	private void updateFallFrom() {
		if (mPlayer == null) {
			return;
		}
		// player.getFallDistance() is unreliable (e.g. does not get reset while in a bed, despite player.isOnGround() being true),
		// thus we calculate the fall distance ourselves.
		// We still check if fall distance is 0 to reset our fall distance calculation, as that checks for water, vines, etc.
		if (mPlayer.getFallDistance() <= 0) {
			mFallFromY = -10000;
		} else {
			mFallFromY = Math.max(mFallFromY, mPlayer.getLocation().getY());
		}
	}

	private double calculateFallDistance() {
		if (mPlayer == null) {
			return 0;
		}
		double currentY = mPlayer.getLocation().getY();
		double fallDistance = mFallFromY - currentY;
		return Math.max(fallDistance, 0);
	}

	private void doSlamAttack(Location location) {
		if (mPlayer == null) {
			return;
		}
		double fallDistance = calculateFallDistance();
		double slamDamage = Math.min(REDUCED_THRESHOLD, fallDistance) * mLevelDamage + Math.max(0, (fallDistance - REDUCED_THRESHOLD)) * mLevelReducedDamage;
		slamDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, slamDamage);

		for (LivingEntity enemy : EntityUtils.getNearbyMobs(location, mLevelSize)) {
			DamageUtils.damage(mPlayer, enemy, DamageType.MELEE_SKILL, slamDamage, mInfo.mLinkedSpell, true);
		}

		World world = mPlayer.getWorld();
		float volumeScale = (float) Math.min(0.1 + fallDistance / 16 * 0.9, 1);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volumeScale * 1.3f, 0);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volumeScale * 2, 1.25F);
		new PartialParticle(Particle.FLAME, location, 60, 0F, 0F, 0F, 0.2F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, location, 20, 0F, 0F, 0F, 0.3F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.LAVA, location, (int) (3 * mLevelSize * mLevelSize), mLevelSize, 0.25f, mLevelSize, 0).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// If there is a mob in range, cancel the fall damage
		if (event.getType() == DamageType.FALL && !EntityUtils.getNearbyMobs(mPlayer.getLocation(), mLevelSize).isEmpty()) {
			event.setCancelled(true);
		}
	}
}
