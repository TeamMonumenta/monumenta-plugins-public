package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class SpellBaseTrail extends Spell {

	private class TrailNode {
		public final BoundingBox mHitbox;
		public int mDurationRemaining;

		public TrailNode(Location loc) {
			mHitbox = new BoundingBox().shift(loc).expand(mHitboxLength / 2);
			mDurationRemaining = mTrailDuration;
		}
	}

	@FunctionalInterface
	public interface AestheticAction {
		/**
		 * Location to do aesthetics
		 */
		void run(World world, Location loc);
	}

	@FunctionalInterface
	public interface HitAction {
		/**
		 * Called when a trail node intersects a player
		 *
		 * @param player Player being targeted
		 * @param loc    Location of the trail node
		 */
		void run(World world, Player player, Location loc);
	}

	@FunctionalInterface
	public interface ExpireAction {
		/**
		 * Called when a trail node intersects a player
		 *
		 * @param loc Location of the trail node
		 */
		void run(World world, Location loc);
	}

	private final LivingEntity mBoss;
	private final World mWorld;
	private final int mTickRate;
	private final int mTrailRate;
	private final int mTrailDuration;
	private final boolean mTrailConsumed;
	private final boolean mTrailGroundOnly;
	private final double mHitboxLength;
	private final AestheticAction mTrailAesthetic;
	private final HitAction mHitAction;
	private final ExpireAction mExpireAction;

	private final Map<Location, TrailNode> mTrailNodes = new HashMap<>();

	private int mTicks = 0;

	/**
	 * @param boss            Boss
	 * @param tickRate        How many ticks to wait between updates (trail creation, particles, etc.) - should divide trailRate
	 * @param trailRate       How many ticks to wait between dropping trail nodes (must be multiple of 2)
	 * @param trailDuration   How many ticks before a trail node expires (must be multiple of 2)
	 * @param trailGroundOnly Whether or not trail nodes should only be laid on the ground
	 * @param trailConsumed   Whether or not trail nodes should be removed when they hit a player
	 * @param hitboxLength    Length of trail node hitboxes
	 * @param trailAesthetic  Called every two ticks at each trail node
	 * @param hitAction       Called when a trail node intersects a player
	 */
	public SpellBaseTrail(LivingEntity boss, int tickRate, int trailRate, int trailDuration, boolean trailGroundOnly, boolean trailConsumed,
	                      double hitboxLength, AestheticAction trailAesthetic, HitAction hitAction, ExpireAction expireAction) {
		mBoss = boss;
		mWorld = boss.getWorld();
		mTickRate = tickRate;
		mTrailRate = trailRate;
		mTrailDuration = trailDuration;
		mTrailGroundOnly = trailGroundOnly;
		mTrailConsumed = trailConsumed;
		mHitboxLength = hitboxLength;
		mTrailAesthetic = trailAesthetic;
		mHitAction = hitAction;
		mExpireAction = expireAction;
	}

	@Override
	public void run() {
		if (EntityUtils.isStunned(mBoss) || EntityUtils.isSilenced(mBoss)) {
			return;
		}
		mTicks += mTickRate;
		if (mTicks >= mTrailRate) {
			if (!mTrailGroundOnly || mBoss.isOnGround()) {
				mTicks = 0;
				Location loc = mBoss.getLocation();
				mTrailNodes.put(loc, new TrailNode(loc));
			}
		}

		Iterator<Map.Entry<Location, TrailNode>> iterAesthetic = mTrailNodes.entrySet().iterator();
		while (iterAesthetic.hasNext()) {
			Map.Entry<Location, TrailNode> entry = iterAesthetic.next();
			TrailNode node = entry.getValue();
			mTrailAesthetic.run(mWorld, entry.getKey());
			node.mDurationRemaining -= mTickRate;
			if (node.mDurationRemaining <= 0) {
				mExpireAction.run(mWorld, entry.getKey());
				iterAesthetic.remove();
			}
		}

		for (Player player : mBoss.getWorld().getPlayers()) {
			BoundingBox target = player.getBoundingBox();
			Iterator<Map.Entry<Location, TrailNode>> iterHit = mTrailNodes.entrySet().iterator();
			while (iterHit.hasNext()) {
				Map.Entry<Location, TrailNode> entry = iterHit.next();
				if (entry.getValue().mHitbox.overlaps(target)) {
					mHitAction.run(mWorld, player, entry.getKey());

					if (mTrailConsumed) {
						iterHit.remove();
					}

					break;
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mTickRate;
	}

}
