package com.playmonumenta.plugins.bosses.spells;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

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
	private double mDetectRange;
	private double mHitboxRadius;
	private final boolean mSingleTarget;
	private final boolean mStopOnFirstHit;
	private final int mShots;
	private final int mRate;
	private final TickAction mTickAction;
	private final CastAction mCastAction;
	private final ParticleAction mParticleAction;
	private final IntersectAction mIntersectAction;

	public SpellBaseBolt(Plugin plugin, LivingEntity caster, int delay, int duration, double velocity,
	                     double detectRange, double hitboxRadius, boolean singleTarget, boolean stopOnFirstHit,
						 TickAction tickAction, CastAction castAction, ParticleAction particleAction, IntersectAction intersectAction) {
		this(plugin, caster, delay, duration, velocity, detectRange, hitboxRadius, singleTarget, stopOnFirstHit, 1, 1, tickAction, castAction, particleAction, intersectAction);
	}

	/**
	 *
	 * @param plugin The main plugin
	 * @param caster The mob casting the spell
	 * @param delay The chargeup timer;the time before the bolt is casted (in ticks)
	 * @param duration The duration of the bolt;how long it lasts (in ticks)
	 * @param velocity The velocity of the bolt
	 * @param detectRange The range in which a player has to be in in order for the spell to be charged and used
	 * @param hitboxRadius The radius of the hitbox
	 * @param stopOnFirstHit Whether to target a single player (Default is its current target, otherwise select at random)
	 * @param shots The amount of shots
	 * @param rate The rate of fire for shots
	 * @param tickAction The action to perform while charging the bolt
	 * @param castAction The action to perform when the bolt is casted
	 * @param particleAction The action the bolt performs while it travels
	 * @param intersectAction The action the bolt performs when it intersects a block or player
	 */
	public SpellBaseBolt(Plugin plugin, LivingEntity caster, int delay, int duration, double velocity,
	                     double detectRange, double hitboxRadius, boolean singleTarget, boolean stopOnFirstHit, int shots, int rate,
						 TickAction tickAction, CastAction castAction, ParticleAction particleAction, IntersectAction intersectAction) {
		mPlugin = plugin;
		mCaster = caster;
		mDelay = delay;
		mDuration = duration;
		mVelocity = velocity;
		mDetectRange = detectRange;
		mHitboxRadius = hitboxRadius;
		mSingleTarget = singleTarget;
		mStopOnFirstHit = stopOnFirstHit;
		mShots = shots;
		mRate = rate;
		mTickAction = tickAction;
		mCastAction = castAction;
		mParticleAction = particleAction;
		mIntersectAction = intersectAction;
	}

	@Override
	public void run() {
		if (PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange).size() > 0) {
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					mTicks++;
					mTickAction.run(mCaster, mTicks);

					if (mCaster == null || mCaster.isDead()) {
						this.cancel();
						return;
					}

					if (mTicks >= mDelay) {
						this.cancel();

						List<Player> players = PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange);
						if (players.size() > 0) {
							if (mSingleTarget) {
								if (mCaster instanceof Mob) {
									Mob mob = (Mob) mCaster;
									if (mob.getTarget() != null && mob.getTarget() instanceof Player) {
										launchBolt((Player)mob.getTarget());
									} else {
										Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
										launchBolt(player);
									}
								} else {
									Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
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

	private void launchBolt(Player player) {
		mCastAction.run(mCaster);
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks++;
				new BukkitRunnable() {
					BoundingBox box = BoundingBox.of(mCaster.getEyeLocation(), mHitboxRadius, mHitboxRadius, mHitboxRadius);
					Vector dir = LocationUtils.getDirectionTo(player.getLocation().add(0, 1, 0), mCaster.getEyeLocation());
					Location detLoc = mCaster.getLocation();
					List<Player> players = PlayerUtils.playersInRange(detLoc, 75);
					int i = 0;
					@Override
					public void run() {
						// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
						for (int j = 0; j < 2; j++) {
							box.shift(dir.clone().multiply(mVelocity * 0.5));
							Location loc = box.getCenter().toLocation(mCaster.getWorld());
							for (Player player : players) {
								if (player.getBoundingBox().overlaps(box)) {
									mIntersectAction.run(player, loc, false);
									if (mStopOnFirstHit) {
										this.cancel();
									}
								}
							}

							if (loc.getBlock().getType().isSolid()) {
								this.cancel();
								mIntersectAction.run(null, loc, true);
							}
						}
						Location loc = box.getCenter().toLocation(mCaster.getWorld());
						i++;
						mParticleAction.run(loc);

						if (i >= mDuration || mCaster == null || mCaster.isDead()) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);

				if (mTicks >= mShots) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, mRate);
	}

	/* If there are players in range of the attack, put it on cooldown. Otherwise, skip and move on*/
	@Override
	public int duration() {
		if (PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange).size() > 0) {
			return mDelay + (20 * 5);
		} else {
			return 1;
		}
	}

}
