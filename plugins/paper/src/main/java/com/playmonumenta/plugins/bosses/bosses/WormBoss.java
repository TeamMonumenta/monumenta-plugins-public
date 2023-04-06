package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.listeners.MobListener;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class WormBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_worm";

	public static class Parameters extends BossParameters {
		public int DETECTION = 64;

		@BossParam(help = "If true, parts move along the exact same path that the head did. If false, they follow the segment in front of them, and non-flying segments get gravity if the head is on the ground.")
		public boolean SNAKE_MOVEMENT = false;

		@BossParam(help = "Number of segments of this worm, including the head and tail")
		public int LENGTH = 5;

		@BossParam(help = "Distance between segments, as a fraction of the segments' horizontal hitbox sizes.")
		public double SEGMENT_DISTANCE = 0.8;

		@BossParam(help = "Pool from which to take body segments. If not set, will use a copy of the main mob without passengers. These should probably have the delve immune tag.")
		public LoSPool BODY_POOL = LoSPool.EMPTY;

		@BossParam(help = "Pool from which to take the tail segment. If not set, will use a copy of the main mob without passengers. These should probably have the delve immune tag.")
		public LoSPool TAIL_POOL = LoSPool.EMPTY;

		@BossParam(help = "Pool from which to take body passengers.")
		public LoSPool BODY_PASSENGER_POOL = LoSPool.EMPTY;

		@BossParam(help = "How often to place a body passenger (e.g. 3 = every third segment). The first is placed after this many body segments.")
		public int BODY_PASSENGER_INTERVAL = 1;

		@BossParam(help = "Pool from which to take the tail passenger.")
		public LoSPool TAIL_PASSENGER_POOL = LoSPool.EMPTY;

		@BossParam(help = "Size of the body entities (for slimes etc.). -1 to use the head's size. Does not affect body segments if a mob pool is used for body segments.")
		public int BODY_SIZE = -1;
		@BossParam(help = "Size of the tail entity (for slimes etc.). -1 to use the head's size. Does not affect the tail if a mob pool is used for the tail.")
		public int TAIL_SIZE = -1;

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WormBoss(plugin, boss);
	}

	private final List<LivingEntity> mParts = new ArrayList<>();
	private final Parameters mParams;
	private final WormMovement mMovement;

	public WormBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());

		for (int i = 1; i < mParams.LENGTH - 1; i++) {
			summonPart(mParams, i, false);
		}
		summonPart(mParams, mParams.LENGTH - 1, true);

		mMovement = mParams.SNAKE_MOVEMENT ? new SnakeMovement(boss.getLocation().toVector()) : new FollowMovement();

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mBoss.isValid()) {
					if (!mBoss.isDead()) {
						for (LivingEntity part : mParts) {
							part.remove();
						}
					}
					cancel();
					return;
				}

				// Simple damage sharing: make all parts have the lowest health of any of them every tick
				double lowestHealth = mBoss.getHealth();
				for (LivingEntity part : mParts) {
					if (part.isDead()) {
						mBoss.damage(0.001, part.getKiller()); // Make sure that the head gets the correct killer set
						mBoss.setHealth(0);
						return;
					}
					lowestHealth = Math.min(lowestHealth, part.getHealth());
				}
				mBoss.setHealth(lowestHealth);
				for (LivingEntity part : mParts) {
					part.setHealth(lowestHealth);
				}

				mMovement.move();
			}
		}.runTaskTimer(plugin, 0, 1);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.DETECTION, null);
	}

	private void summonPart(Parameters params, int index, boolean tail) {

		double logIndex = Math.log(1 + index / 6.0);
		Location spawnLocation = mBoss.getLocation().add(VectorUtils.rotateYAxis(new Vector(mBoss.getWidth() * (0.8 + logIndex), 0, 0), 500 * logIndex));
		LivingEntity part = null;
		LoSPool pool = tail ? params.TAIL_POOL : params.BODY_POOL;
		if (pool != null) {
			part = (LivingEntity) pool.spawn(spawnLocation);
		}
		if (part == null) {
			part = EntityUtils.copyMob(mBoss, spawnLocation);
			part.addScoreboardTag(DelvesManager.AVOID_MODIFIERS);
			int size = tail ? params.TAIL_SIZE : params.BODY_SIZE;
			if (size >= 0) {
				EntityUtils.setSize(part, size);
			}
			Component customName = mBoss.customName();
			if (customName != null) {
				part.customName(customName.append(Component.text(tail ? " Tail" : " Body")));
			}
		}
		mParts.add(part);

		if (tail || index % params.BODY_PASSENGER_INTERVAL == 0) {
			LoSPool passengerPool = tail ? params.TAIL_PASSENGER_POOL : params.BODY_PASSENGER_POOL;
			Entity passenger = passengerPool.spawn(part.getLocation());
			if (passenger != null) {
				part.addPassenger(passenger);
			}
		}

		EntityUtils.setRemoveEntityOnUnload(part);
		part.setAI(false);
		mBoss.getCollidableExemptions().add(part.getUniqueId());
		EntityUtils.setAttributeBase(part, Attribute.GENERIC_MAX_HEALTH, EntityUtils.getMaxHealth(mBoss));
		part.setHealth(mBoss.getHealth());

		// prevent dropping XP (and items)
		MetadataUtils.setMetadata(part, Constants.SPAWNER_COUNT_METAKEY, MobListener.SPAWNER_DROP_THRESHOLD + 1);

	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (LivingEntity part : mParts) {
			part.setInvulnerable(false);
			part.setHealth(0); // TODO check if this drops xp
		}
	}

	@Override
	public void unload() {
		if (!mBoss.isDead()) {
			for (LivingEntity part : mParts) {
				part.remove();
			}
		}
	}

	private interface WormMovement {
		void move();
	}

	private class FollowMovement implements WormMovement {

		private final double[] mFallSpeeds;

		private FollowMovement() {
			mFallSpeeds = new double[mParts.size()];
		}

		@Override
		public void move() {
			LivingEntity previousPart = mBoss;
			for (int i = 0; i < mParts.size(); i++) {
				LivingEntity part = mParts.get(i);
				// Disabling AI also disables gravity, but that makes worms look weird, so manually apply gravity
				if (mBoss.isOnGround() && !EntityUtils.isFlyingMob(part)) {
					double fallSpeed = mFallSpeeds[i]; // Using velocity to store this information is not possible, as it is reset to 0 every tick
					fallSpeed = fallSpeed * 0.98 - 0.08;
					NmsUtils.getVersionAdapter().moveEntity(part, new Vector(0, fallSpeed, 0));
					mFallSpeeds[i] = fallSpeed;
				} else {
					mFallSpeeds[i] = 0;
				}
				double desiredDistance = (previousPart.getBoundingBox().getWidthX() + part.getBoundingBox().getWidthX()) / 2 * mParams.SEGMENT_DISTANCE;
				Vector relativePosition = part.getLocation().toVector().subtract(previousPart.getLocation().toVector());
				double distance = relativePosition.length();
				if (distance > 0.001) {
					relativePosition.multiply(1 / distance);
					Location newLocation;
					if (distance > desiredDistance) {
						newLocation = previousPart.getLocation().add(relativePosition.clone().multiply(desiredDistance));
					} else {
						newLocation = part.getLocation();
					}
					// look at next segment
					newLocation.setDirection(relativePosition.multiply(-1));
					// Use NMS utils to move the segment even if it has a passenger
					NmsUtils.getVersionAdapter().setEntityLocation(part, newLocation.toVector(), newLocation.getYaw(), newLocation.getPitch());
				}
				previousPart = part;
			}
		}
	}

	private class SnakeMovement implements WormMovement {

		private static class SnakePosition {

			final Vector mPos;
			@Nullable SnakePosition mPrevious;
			final double mDistanceToPrevious;

			private SnakePosition(Vector pos, @Nullable SnakePosition last, double distanceToLast) {
				mPos = pos;
				mPrevious = last;
				mDistanceToPrevious = distanceToLast;
			}

		}

		private SnakePosition mFirst;

		private SnakeMovement(Vector pos) {
			SnakePosition previousPos = null;
			for (int i = mParts.size() - 1; i >= 0; i--) {
				LivingEntity part = mParts.get(i);
				previousPos = new SnakePosition(part.getLocation().toVector(), previousPos, previousPos == null ? Double.POSITIVE_INFINITY : previousPos.mPos.distance(part.getLocation().toVector()));
			}
			mFirst = new SnakePosition(pos, previousPos, previousPos == null ? Double.POSITIVE_INFINITY : previousPos.mPos.distance(pos));
		}

		@Override
		public void move() {

			add(mBoss.getLocation().toVector());

			double distance = 0;
			SnakePosition pos = mFirst;
			LivingEntity previousPart = mBoss;
			for (LivingEntity part : mParts) {
				double desiredDistance = (previousPart.getBoundingBox().getWidthX() + part.getBoundingBox().getWidthX()) / 2 * mParams.SEGMENT_DISTANCE;
				distance += desiredDistance;
				while (pos.mPrevious != null && distance > pos.mDistanceToPrevious) {
					distance -= pos.mDistanceToPrevious;
					pos = pos.mPrevious;
				}

				Vector newPos = pos.mPos.clone();
				if (pos.mPrevious != null) {
					newPos.add(pos.mPrevious.mPos.clone().subtract(pos.mPos).multiply(distance / pos.mDistanceToPrevious));
				}
				Location newLocation = newPos.toLocation(part.getWorld());
				// look at next segment
				newLocation.setDirection(previousPart.getLocation().toVector().subtract(newPos));
				// Use NMS utils to move the segment even if it has a passenger
				NmsUtils.getVersionAdapter().setEntityLocation(part, newLocation.toVector(), newLocation.getYaw(), newLocation.getPitch());
				previousPart = part;
			}
			if (pos.mPrevious != null) {
				pos.mPrevious.mPrevious = null;
			}

		}

		void add(Vector pos) {
			double distance = mFirst.mPos.distance(pos);
			mFirst = new SnakePosition(pos, mFirst, distance);
		}

	}

}
