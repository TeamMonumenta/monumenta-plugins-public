package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.CoreElemental;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellPyroclasticSlam extends Spell implements CoreElemental.CoreElementalBase {
	// Maximum target distance from centre
	private static final int RANGE = 20;
	// Damage when the boss lands before fissure creation
	private static final int SLAM_DAMAGE = 55;
	// Lengths of fissures of different segments
	private static final int[] FISSURE_LENGTH = {5, 6, 9};
	// Maximum number of locations the boss will slam on, before fissure creation
	private static final int NUMBER_OF_TARGET = 3;
	// Number of fissures that will expand from the centre
	private static final int NUMBER_OF_FISSURE = 5;
	// Damage dealt to players when the expanding fissures touch the player, should be exceptionally high because the fissures are quite predictable
	private static final int FISSURE_DAMAGE = 110;
	// Maximum radius of the earthquake
	private static final int EARTHQUAKE_RADIUS = 20;
	// Time needed to expand the earthquake radius to the maximum
	private static final int EARTHQUAKE_DURATION = 35;
	// Damage dealt to players if they didn't jump over the earthquake ring
	private static final int EARTHQUAKE_DAMAGE = 70;
	private static final Particle.DustOptions ORANGE_PARTICLE = new Particle.DustOptions(Color.fromRGB(252, 94, 3), 1);
	private static final Particle.DustOptions PALE_ORANGE_PARTICLE = new Particle.DustOptions(Color.fromRGB(255, 184, 143), 1);
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final CoreElemental mQuarry;
	private final Location mStartLoc;
	private int mPhase = 0;
	private int mTimesJumped = 0;
	private final PassiveFissure mFissure;
	private Location[] mTargetLocations = new Location[NUMBER_OF_TARGET];
	private final List<Player> mAffectedPlayers = new ArrayList<>();

	public SpellPyroclasticSlam(Plugin plugin, LivingEntity boss, CoreElemental quarry, Location startLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = startLoc;
		mQuarry = quarry;
		mFissure = new PassiveFissure(mPlugin, mBoss, mStartLoc, NUMBER_OF_FISSURE, FISSURE_LENGTH.length, FISSURE_LENGTH, FISSURE_MATERIAL, LAVA_MATERIAL, FISSURE_DAMAGE, getSpellName(),
			// On block change
			location -> {
				// Effects
				new PartialParticle(Particle.CLOUD, location.clone().add(0, 1.2, 0))
					.count(3)
					.delta(0.5, 0, 0.5)
					.extra(0)
					.spawnAsBoss();
				new PartialParticle(Particle.BLOCK_CRACK, location.clone().add(0, 1, 0))
					.count(15)
					.delta(0.5, 0, 0.5)
					.data(FISSURE_MATERIAL.createBlockData())
					.spawnAsBoss();
				mBoss.getWorld().playSound(location.clone(), Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 0.1f, 0.4f);
				// Chance to change the block above to air
				if (FastUtils.randomDoubleInRange(0, 1) <= 0.2 && location.getBlock().getType() == FISSURE_MATERIAL) {
					Block up = location.getBlock().getRelative(BlockFace.UP);
					if (up.isEmpty()) {
						if (TemporaryBlockChangeManager.INSTANCE.changeBlock(up, Material.FIRE, 12000)) {
							mQuarry.addChangedBlock(up);
						}
					}
				}
			},
			// On cast
			location -> {
				// Effects
				location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 0.1f);
				location.getWorld().playSound(location, Sound.ENTITY_WITHER_HURT, SoundCategory.HOSTILE, 2f, 0.7f);
				location.getWorld().playSound(location, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 3f, 0.5f);
			},
			quarry);
	}

	@Override
	public void run() {
		mAffectedPlayers.clear();
		mTargetLocations = getTargetLocation();
		mTimesJumped = 0;
		jump(mTargetLocations[mTimesJumped]);
	}

	private void triggerFissure() {
		// Trigger the fissure
		mFissure.trigger(mPhase);
		mPhase++;

		// Earthquake
		BukkitRunnable runnable = new BukkitRunnable() {
			double mRadius = 1;

			@Override
			public void run() {
				new PPParametric(Particle.REDSTONE, mStartLoc,
					(t, builder) -> {
						double r = mRadius - 1;
						builder.location(
							LocationUtils.fallToGround(mStartLoc.clone().add(r * FastUtils.cosDeg(t * 360), 0, r * FastUtils.sinDeg(t * 360)), mStartLoc.getY() - 5).add(0, 0.2, 0));
					}).count(60).data(new Particle.DustOptions(Color.WHITE, 1.4F)).spawnAsEntityActive(mBoss);
				new PPParametric(Particle.CRIT_MAGIC, mStartLoc,
					(t, builder) -> builder.location(
						LocationUtils.fallToGround(mStartLoc.clone().add(mRadius * FastUtils.cosDeg(t * 360), 0, mRadius * FastUtils.sinDeg(t * 360)), mStartLoc.getY() - 5).add(0, 0.2, 0))).count(70).delta(0, 0.2, 0).directionalMode(true).spawnAsEntityActive(mBoss);
				for (Player player : PlayerUtils.playersInRange(mStartLoc, RANGE, true)) {
					if (!mAffectedPlayers.contains(player)
						&& Math.abs(LocationUtils.xzDistance(player.getLocation(), mStartLoc) - mRadius) <= player.getBoundingBox().getWidthZ()
						&& PlayerUtils.isOnGround(player)) {
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, EARTHQUAKE_DAMAGE, getSpellName(), mStartLoc);
						mAffectedPlayers.add(player);
					}
				}
				mRadius += (double) EARTHQUAKE_RADIUS / EARTHQUAKE_DURATION;
				if (mRadius >= EARTHQUAKE_RADIUS) {
					this.cancel();
					mActiveRunnables.remove(this);
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private Location[] getTargetLocation() {
		Location[] result = new Location[NUMBER_OF_TARGET];
		for (int i = 0; i < NUMBER_OF_TARGET; i++) {
			result[i] = LocationUtils.fallToGround(LocationUtils.randomLocationInCircle(mStartLoc, 10), mStartLoc.getBlockY() - 5);
		}
		List<Player> players = EntityUtils.getNearestPlayers(mStartLoc, RANGE);
		Collections.shuffle(players);
		for (int j = 0; j < Math.min(players.size(), NUMBER_OF_TARGET); j++) {
			result[j] = LocationUtils.fallToGround(players.get(j).getLocation(), mStartLoc.getBlockY() - 5);
		}
		return result;
	}

	private void jump(Location locTarget) {
		mAffectedPlayers.clear();
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();
		world.playSound(loc, Sound.ENTITY_PILLAGER_CELEBRATE, SoundCategory.PLAYERS, 1f, 1.1f);
		new PartialParticle(Particle.CLOUD, loc, 15, 1, 0f, 1, 0).spawnAsEntityActive(mBoss);

		Location moveTo = loc.clone();
		int i;
		for (i = 0; i < 3; i++) {
			if (!moveTo.getBlock().isPassable()) {
				moveTo.add(0, 1, 0);
			} else {
				break;
			}
		}

		if (i == 3) {
			// Failed to find a good path
			return;
		}

		((Mob) mBoss).getPathfinder().moveTo(moveTo);

		double mVelocityMultiplier = 0.7;
		Vector velocity = locTarget.clone().subtract(moveTo).toVector().normalize().multiply(mVelocityMultiplier);
		velocity.setY(1.1);

		final Vector finalVelocity = velocity;

		BukkitRunnable leap = new BukkitRunnable() {
			final Location mLeapLocation = moveTo;
			boolean mLeaping = false;
			boolean mHasBeenOneTick = false;

			@Override
			public void run() {
				if (!mLeaping) {
					// Start leaping
					if (mBoss.getLocation().distance(mLeapLocation) < 1) {
						world.playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 15, 1, 0f, 1, 0).spawnAsEntityActive(mBoss);
						((Mob) mBoss).getPathfinder().stopPathfinding();
						mBoss.setVelocity(finalVelocity);
						mLeaping = true;
					}
				} else {

					// Particles
					new PPCircle(Particle.REDSTONE, locTarget, 2)
						.count(60)
						.delta(0.1, 0.2, 0.1)
						.data(PALE_ORANGE_PARTICLE)
						.spawnAsEntityActive(mBoss);

					new PartialParticle(Particle.FLAME, mBoss.getLocation())
						.count(10)
						.delta(0.25, 0.25, 0.25)
						.spawnAsEntityActive(mBoss);

					mBoss.setFallDistance(0);
					if (mBoss.isOnGround() && mHasBeenOneTick) {
						if (mTimesJumped < NUMBER_OF_TARGET) {
							land();
						} else {
							triggerFissure();
						}
						this.cancel();
						mActiveRunnables.remove(this);
						return;
					}

					// Give the caller a chance to run extra effects or manipulate the boss's leap velocity
					Vector towardsPlayer = locTarget.clone().subtract(mBoss.getLocation()).toVector().setY(0);
					Vector originalVelocity = mBoss.getVelocity();
					double scale = 0.2;
					Vector newVelocity = new Vector();
					newVelocity.setX((originalVelocity.getX() * 20 + towardsPlayer.getX() * scale) / 20);
					// Use the original mob's vertical velocity, so it doesn't somehow fall faster than gravity
					newVelocity.setY(originalVelocity.getY());
					newVelocity.setZ((originalVelocity.getZ() * 20 + towardsPlayer.getZ() * scale) / 20);
					mBoss.setVelocity(newVelocity);

					// At least one tick has passed to avoid insta smacking a nearby player
					mHasBeenOneTick = true;
				}
			}
		};

		leap.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(leap);
	}

	private void land() {
		// Damage players
		damageNearby(mBoss.getLocation());
		// Effects
		ParticleUtils.explodingRingEffect(mPlugin, mTargetLocations[mTimesJumped], 5, 1, 10,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.5, (Location location) -> {
					new PartialParticle(Particle.FLAME, location, 1, 0, 0, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.REDSTONE, location, 1).data(ORANGE_PARTICLE).spawnAsEntityActive(mBoss);
				})
			));

		mBoss.getWorld().playSound(mBoss, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 0.6f);

		if (++mTimesJumped < NUMBER_OF_TARGET) {
			jump(mTargetLocations[mTimesJumped]);
		} else {
			mFissure.displayTelegraph(mPhase);
			jump(mStartLoc);
		}
	}

	private void damageNearby(Location location) {
		List<Player> players = PlayerUtils.playersInRange(location, 1.5, true, false);
		for (Player player : players) {
			if (!mAffectedPlayers.contains(player)) {
				BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, SLAM_DAMAGE, getSpellName(), mBoss.getLocation());
				mAffectedPlayers.add(player);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 20 * 10;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

	@Override
	public String getSpellName() {
		return "Pyroclastic Slam";
	}

	@Override
	public String getSpellChargePrefix() {
		return "Preparing";
	}

	@Override
	public int getChargeDuration() {
		return 20 * 6;
	}

	@Override
	public int getSpellDuration() {
		return 20 * 2;
	}
}
