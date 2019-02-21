package com.playmonumenta.bossfights.spells;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;

/**
 * This is the base spell for a bolt spell.
 * Odd bug: When the projectile is fired, the mob is knocked back
 * @author FirelordWeaponry
 *
 */
public class SpellBaseBolt extends Spell {
	@FunctionalInterface
	public interface TickAction {
		/**
		 * User function called once every two ticks while bolt is charging
		 * @param entity  The entity charging the bolt
		 * @param tick    Number of ticks since start of attack
		 *      NOTE - Only even numbers are returned here!
		 */
		void run(Entity entity, int tick);
	}

	@FunctionalInterface
	public interface CastAction {
		/**
		 * User function called once the bolt is fired
		 * @param entity  The entity firing the bolt
		 */
		void run(Entity entity);
	}

	@FunctionalInterface
	public interface ParticleAction {
		/**
		 * User function called many times per tick with the location where
		 * a bolt particle should be spawned
		 * @param loc Location to spawn a particle
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface IntersectAction {
		/**
		 * User function called when the bolt hits/intersects with a player
		 * @param player  Player being targeted
		 * @param loc     Location where the laser ends (either at player or occluding block)
		 * @param blocked Whether the laser is obstructed (true) or hits the player (false)
		 */
		void run(Player player, Location loc, boolean blocked);
	}

	private Plugin mPlugin;
	private LivingEntity mCaster;
	private int mDelay;
	private int mDuration;
	private double mVelocity;
	private double mDetect_range;
	private double mHitbox_radius;
	private final boolean mSingleTarget;
	private final TickAction mTickAction;
	private final CastAction mCastAction;
	private final ParticleAction mParticleAction;
	private final IntersectAction mIntersectAction;
	private final Random mRandom = new Random();

	/**
	 *
	 * @param plugin The main plugin
	 * @param caster The mob casting the spell
	 * @param delay The chargeup timer;the time before the bolt is casted (in ticks)
	 * @param duration The duration of the bolt;how long it lasts (in ticks)
	 * @param velocity The velocity of the bolt
	 * @param detect_range The range in which a player has to be in in order for the spell to be charged and used
	 * @param hitbox_radius The radius of the hitbox
	 * @param singleTarget Whether to target a single player (Default is its current target, otherwise select at random)
	 * @param tickAction The action to perform while charging the bolt
	 * @param castAction The action to perform when the bolt is casted
	 * @param particleAction The action the bolt performs while it travels
	 * @param intersectAction The action the bolt performs when it intersects a block or player
	 */
	public SpellBaseBolt(Plugin plugin, LivingEntity caster, int delay, int duration, double velocity, double detect_range, double hitbox_radius, boolean singleTarget,
	                     TickAction tickAction, CastAction castAction, ParticleAction particleAction, IntersectAction intersectAction) {
		mPlugin = plugin;
		mCaster = caster;
		mDelay = delay;
		mDuration = duration;
		mVelocity = velocity;
		mDetect_range = detect_range;
		mHitbox_radius = hitbox_radius;
		mSingleTarget = singleTarget;
		mTickAction = tickAction;
		mCastAction = castAction;
		mParticleAction = particleAction;
		mIntersectAction = intersectAction;
	}

	@Override
	public void run() {
		if (Utils.playersInRange(mCaster.getLocation(), mDetect_range).size() > 0) {
			new BukkitRunnable() {
				int t = 0;
				@Override
				public void run() {
					t++;
					mTickAction.run(mCaster, t);

					if (mCaster.isDead() || mCaster == null) {
						this.cancel();
						return;
					}

					if (t >= mDelay) {
						this.cancel();
						mCastAction.run(mCaster);
						List<Player> players = Utils.playersInRange(mCaster.getLocation(), mDetect_range);
						if (players.size() > 0) {
							if (mSingleTarget) {
								if (mCaster instanceof Mob) {
									Mob mob = (Mob) mCaster;
									if (mob.getTarget() != null && mob.getTarget() instanceof Player) {
										launchBolt(mob.getTarget());
									}
								} else {
									Player player = players.get(mRandom.nextInt(players.size()));
									launchBolt(player);
								}
							} else {
								for (Player player : players) {
									launchBolt(player);
								}
							}
						}
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void launchBolt(LivingEntity targetEntity) {
		new BukkitRunnable() {
			BoundingBox box = BoundingBox.of(mCaster.getEyeLocation(), mHitbox_radius, mHitbox_radius, mHitbox_radius);
			Vector dir = Utils.getDirectionTo(targetEntity.getLocation().add(0, 1, 0), mCaster.getEyeLocation());
			Location detLoc = mCaster.getLocation();
			List<Player> players = Utils.playersInRange(detLoc, 75);
			int i = 0;
			@Override
			public void run() {
				box.shift(dir.clone().multiply(mVelocity));
				Location loc = box.getCenter().toLocation(mCaster.getWorld());
				i++;
				mParticleAction.run(loc);
				for (Player player : players) {
					if (player.getBoundingBox().overlaps(box)) {
						mIntersectAction.run(player, loc, false);
					}
				}

				if (loc.getBlock().getType().isSolid()) {
					this.cancel();
					mIntersectAction.run(null, loc, true);
				}

				if (i >= mDuration || mCaster.isDead() || mCaster == null) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	/* If there are players in range of the attack, put it on cooldown. Otherwise, skip and move on*/
	@Override
	public int duration() {
		if (Utils.playersInRange(mCaster.getLocation(), mDetect_range).size() > 0) {
			return mDelay + mDuration;
		} else {
			return 1;
		}
	}

}
