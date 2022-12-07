package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * This is the base spell for a bolt spell.
 * Odd bug: When the projectile is fired, the mob is knocked back
 *
 * @author FirelordWeaponry
 */
public class SpellBaseBolt extends Spell {
	@FunctionalInterface
	public interface TickAction {
		/**
		 * User function called once every two ticks while bolt is charging
		 *
		 * @param entity The entity charging the bolt
		 * @param tick   Number of ticks since start of attack
		 *               NOTE - Only even numbers are returned here!
		 */
		void run(Entity entity, int tick);
	}

	@FunctionalInterface
	public interface CastAction {
		/**
		 * User function called once the bolt is fired
		 *
		 * @param entity The entity firing the bolt
		 */
		void run(Entity entity);
	}

	@FunctionalInterface
	public interface ParticleAction {
		/**
		 * User function called many times per tick with the location where
		 * a bolt particle should be spawned
		 *
		 * @param loc Location to spawn a particle
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface IntersectAction {
		/**
		 * User function called when the bolt hits/intersects with a player
		 *
		 * @param player  Player being targeted
		 * @param loc     Location where the laser ends (either at player or occluding block)
		 * @param blocked Whether the laser is obstructed (true) or hits the player (false)
		 */
		void run(@Nullable Player player, Location loc, boolean blocked);
	}

	private final Plugin mPlugin;
	private final LivingEntity mCaster;
	private final int mDelay;
	private final int mDuration;
	private final double mVelocity;
	private final double mDetectRange;
	private final double mHitboxRadius;
	private final boolean mSingleTarget;
	private final boolean mStopOnFirstHit;
	private final int mShots;
	private final int mRate;
	private final TickAction mTickAction;
	private final CastAction mCastAction;
	private final ParticleAction mParticleAction;
	private final IntersectAction mIntersectAction;
	private final @Nullable Predicate<Player> mPlayerFilter;

	/**
	 * @param plugin          The main plugin
	 * @param caster          The mob casting the spell
	 * @param delay           The chargeup timer;the time before the bolt is casted (in ticks)
	 * @param duration        The duration of the bolt;how long it lasts (in ticks)
	 * @param velocity        The velocity of the bolt
	 * @param detectRange     The range in which a player has to be in in order for the spell to be charged and used
	 * @param hitboxRadius    The radius of the hitbox
	 * @param stopOnFirstHit  Whether to target a single player (Default is its current target, otherwise select at random)
	 * @param shots           The amount of shots
	 * @param rate            The rate of fire for shots
	 * @param tickAction      The action to perform while charging the bolt
	 * @param castAction      The action to perform when the bolt is casted
	 * @param particleAction  The action the bolt performs while it travels
	 * @param intersectAction The action the bolt performs when it intersects a block or player
	 * @param playerFilter    A function to evaluate whether a player is a valid target (true) or not (false)
	 */
	public SpellBaseBolt(Plugin plugin, LivingEntity caster, int delay, int duration, double velocity,
	                     double detectRange, double hitboxRadius, boolean singleTarget, boolean stopOnFirstHit, int shots, int rate,
	                     TickAction tickAction, CastAction castAction, ParticleAction particleAction, IntersectAction intersectAction, @Nullable Predicate<Player> playerFilter) {
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
		mPlayerFilter = playerFilter;
	}

	@Override
	public void run() {
		if (PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, false).size() > 0) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					mTickAction.run(mCaster, mTicks);

					if (mCaster == null || mCaster.isDead() || EntityUtils.isStunned(mCaster) || EntityUtils.isSilenced(mCaster)) {
						this.cancel();
						return;
					}

					if (mTicks >= mDelay) {
						this.cancel();

						List<Player> players = PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, false);
						if (mPlayerFilter != null) {
							players.removeIf(mPlayerFilter.negate());
						}
						if (players.size() > 0) {
							if (mSingleTarget) {
								if (mCaster instanceof Mob) {
									Mob mob = (Mob) mCaster;
									LivingEntity target = mob.getTarget();
									if (target instanceof Player) {
										launchBolt((Player) target);
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
				Vector dir = LocationUtils.getDirectionTo(player.getLocation().add(0, 1, 0), mCaster.getEyeLocation());
				Location detLoc = mCaster.getLocation();
				List<Player> players = PlayerUtils.playersInRange(detLoc, 75, false);
				if (mPlayerFilter != null) {
					players.removeIf(mPlayerFilter.negate());
				}

				new BukkitRunnable() {
					BoundingBox mBox = BoundingBox.of(mCaster.getEyeLocation(), mHitboxRadius, mHitboxRadius, mHitboxRadius);
					int mInnerTicks = 0;

					@Override
					public void run() {
						// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
						for (int j = 0; j < 2; j++) {
							mBox.shift(dir.clone().multiply(mVelocity * 0.5));
							Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
							for (Player player : players) {
								if (player.getBoundingBox().overlaps(mBox)) {
									mIntersectAction.run(player, loc, false);
									if (mStopOnFirstHit) {
										this.cancel();
										return;
									}
								}
							}

							if (loc.getBlock().getType().isSolid()) {
								this.cancel();
								mIntersectAction.run(null, loc, true);
							}
						}
						Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
						mInnerTicks++;
						mParticleAction.run(loc);

						if (mInnerTicks >= mDuration || mCaster == null || mCaster.isDead()) {
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
	public int cooldownTicks() {
		if (PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, true).size() > 0) {
			return mDelay + (20 * 5);
		} else {
			return 1;
		}
	}

}
