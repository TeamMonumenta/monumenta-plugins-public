package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;



public class MeteorSlam extends Ability {
	public static final String NAME = "Meteor Slam";
	public static final Spells SPELL = Spells.METEOR_SLAM;
	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickSlammed";

	public static final int DAMAGE_1 = 3;
	public static final int DAMAGE_2 = 4;
	public static final int REDUCED_DAMAGE_1 = 2;
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

	private final int mLevelDamage;
	private final double mLevelReducedDamage;
	private final int mLevelSize;
	private final int mLevelJumpAmplifier;
	private final BukkitRunnable mSlamAttackRunner;

	private double mFallFromY = -7050;

	public MeteorSlam(Plugin plugin, Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = SPELL;

		mInfo.mScoreboardId = "MeteorSlam";
		mInfo.mShorthandName = "MS";
		mInfo.mDescriptions.add(
			String.format(
				"Falling more than %s blocks generates a slam when you land, dealing %s damage to all enemies in a %s-block cube around you per block fallen, with damage reduced to %s per block after the first %s blocks. Falling more than %s blocks and attacking an enemy also generates a slam at that enemy, and resets your fall damage. The damage does not impact your melee attack. | Pressing the swap key grants you Jump Boost %ss for %ss instead of doing its vanilla function. Jump Boost cooldown: %ss.",
				AUTOMATIC_THRESHOLD,
				DAMAGE_1,
				SIZE_1,
				REDUCED_DAMAGE_1,
				REDUCED_THRESHOLD,
				MANUAL_THRESHOLD,
				JUMP_LEVEL_1,
				DURATION_SECONDS,
				COOLDOWN_SECONDS_1
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Damage is increased from %s to %s per block for the first %s blocks, and from %s to %s per block thereafter. Damage size is increased from %s to %s blocks. | Jump Boost level is increased from %s to %s. Jump Boost cooldown is reduced from %ss to %ss.",
				DAMAGE_1,
				DAMAGE_2,
				REDUCED_THRESHOLD,
				REDUCED_DAMAGE_1,
				REDUCED_DAMAGE_2,
				SIZE_1,
				SIZE_2,
				JUMP_LEVEL_1,
				JUMP_LEVEL_2,
				COOLDOWN_SECONDS_1,
				COOLDOWN_SECONDS_2
			)
		);
		mInfo.mIgnoreCooldown = true;

		boolean isUpgraded = getAbilityScore() == 2;
		mInfo.mCooldown = isUpgraded ? COOLDOWN_TICKS_2 : COOLDOWN_TICKS_1;

		mLevelDamage = isUpgraded ? DAMAGE_2 : DAMAGE_1;
		mLevelReducedDamage = isUpgraded ? REDUCED_DAMAGE_2 : REDUCED_DAMAGE_1;
		mLevelSize = isUpgraded ? SIZE_2 : SIZE_1;
		mLevelJumpAmplifier = isUpgraded ? JUMP_AMPLIFIER_2 : JUMP_AMPLIFIER_1;

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
					if (
						calculateFallDistance() > AUTOMATIC_THRESHOLD
					) {
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
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (
			event.getCause() == DamageCause.ENTITY_ATTACK
			&& calculateFallDistance() > MANUAL_THRESHOLD
			&& MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, SLAM_ONCE_THIS_TICK_METAKEY)
		) {
			doSlamAttack(event.getEntity().getLocation().add(0, 0.15, 0));
			mFallFromY = -7050;
			// Also reset fall damage, mFallFromY can continue updating from there
			mPlayer.setFallDistance(0);
		}
		return true;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (
			!isTimerActive()
			&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
		) {
			putOnCooldown();

			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, DURATION_TICKS, mLevelJumpAmplifier, true, false));

			World world = mPlayer.getWorld();
			Location location = mPlayer.getLocation().add(0, 0.15, 0);
			world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
			world.spawnParticle(Particle.LAVA, location, 15, 1, 0f, 1, 0);
			for (int i = 0; i < 30; i++) {
				world.spawnParticle(
					Particle.FLAME,
					location,
					0, // 0 particle count, deltas act as direction values instead of offsets
					FastUtils.randomDoubleInRange(-3, 3),
					0f,
					FastUtils.randomDoubleInRange(-3, 3),
					FastUtils.randomDoubleInRange(0.1, 0.3)
				);
			}
		}
	}

	@Override
	public void invalidate() {
		if (mSlamAttackRunner != null) {
			mSlamAttackRunner.cancel();
		}
	}

	private void updateFallFrom() {
		double currentY = mPlayer.getLocation().getY();
		double fallDistance = mPlayer.getFallDistance();
		mFallFromY = currentY + fallDistance;
	}

	private double calculateFallDistance() {
		double currentY = mPlayer.getLocation().getY();
		double fallDistance = mFallFromY - currentY;
		return Math.max(fallDistance, 0);
	}

	private void doSlamAttack(Location location) {
		double fallDistance = calculateFallDistance();
		double slamDamage = Math.min(REDUCED_THRESHOLD, fallDistance) * mLevelDamage + Math.max(0, (fallDistance - REDUCED_THRESHOLD)) * mLevelReducedDamage;

		for (LivingEntity enemy : EntityUtils.getNearbyMobs(location, mLevelSize)) {
			EntityUtils.damageEntity(mPlugin, enemy, slamDamage, mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);
		}

		World world = mPlayer.getWorld();
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 0);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 1.25F);
		world.spawnParticle(Particle.FLAME, location, 60, 0F, 0F, 0F, 0.2F);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, location, 20, 0F, 0F, 0F, 0.3F);
		world.spawnParticle(Particle.LAVA, location, 3 * mLevelSize * mLevelSize, mLevelSize, 0.25f, mLevelSize, 0);
	}
}
