package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * This is the base spell for a bolt spell.
 * Odd bug: When the projectile is fired, the mob is knocked back
 *
 * @author FirelordWeaponry
 */
public abstract class SpellBaseBolt extends Spell {
	protected final Plugin mPlugin;
	protected final LivingEntity mCaster;
	protected final int mDelay;
	protected final int mDuration;
	protected final double mVelocity;
	protected final double mDetectRange;
	protected final double mHitboxRadius;
	protected final boolean mSingleTarget;
	protected final boolean mStopOnFirstHit;
	protected final int mShots;
	protected final int mRate;
	private final @Nullable Predicate<Player> mPlayerFilter;

	/**
	 * @param plugin         The main plugin
	 * @param caster         The mob casting the spell
	 * @param delay          The chargeup timer;the time before the bolt is casted (in ticks)
	 * @param duration       The duration of the bolt;how long it lasts (in ticks)
	 * @param velocity       The velocity of the bolt
	 * @param detectRange    The range in which a player has to be in in order for the spell to be charged and used
	 * @param hitboxRadius   The radius of the hitbox
	 * @param stopOnFirstHit Whether to target a single player (Default is its current target, otherwise select at random)
	 * @param shots          The amount of shots
	 * @param rate           The rate of fire for shots
	 * @param playerFilter   A function to evaluate whether a player is a valid target (true) or not (false)
	 */
	public SpellBaseBolt(Plugin plugin, LivingEntity caster, int delay, int duration, double velocity,
	                     double detectRange, double hitboxRadius, boolean singleTarget, boolean stopOnFirstHit, int shots, int rate,
	                     @Nullable Predicate<Player> playerFilter) {
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
		mPlayerFilter = playerFilter;
	}

	// The action to perform while charging the bolt
	protected abstract void tickAction(Entity entity, int tick);

	// The action to perform when the bolt is casted
	protected abstract void castAction(Entity entity);

	// The action the bolt performs while it travels
	protected abstract void particleAction(Location loc);

	// The action the bolt performs when it intersects a block or player
	protected abstract void intersectAction(@Nullable Player player, Location loc, boolean blocked, @Nullable Location prevLoc);


	@Override
	public void run() {
		if (!PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, false).isEmpty()) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					tickAction(mCaster, mTicks);

					if (mCaster.isDead() || EntityUtils.isStunned(mCaster) || EntityUtils.isSilenced(mCaster)) {
						this.cancel();
						return;
					}

					if (mTicks >= mDelay) {
						this.cancel();

						List<Player> players = PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, false);
						if (mPlayerFilter != null) {
							players.removeIf(mPlayerFilter.negate());
						}
						if (!players.isEmpty()) {
							if (mSingleTarget) {
								Player player;
								if (mCaster instanceof Mob mob && mob.getTarget() instanceof Player p) {
									player = p;
								} else {
									player = players.get(FastUtils.RANDOM.nextInt(players.size()));
								}
								launchBolt(player);
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
		castAction(mCaster);
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
					final BoundingBox mBox = BoundingBox.of(mCaster.getEyeLocation(), mHitboxRadius, mHitboxRadius, mHitboxRadius);
					int mInnerTicks = 0;

					@Override
					public void run() {
						// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
						for (int j = 0; j < 2; j++) {
							Location prevLoc = mBox.getCenter().toLocation(mCaster.getWorld());
							mBox.shift(dir.clone().multiply(mVelocity * 0.5));
							Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
							for (Player player : players) {
								if (player.getBoundingBox().overlaps(mBox)) {
									intersectAction(player, loc, false, prevLoc);
									if (mStopOnFirstHit) {
										this.cancel();
										return;
									}
								}
							}

							if (loc.getBlock().getType().isSolid()) {
								this.cancel();
								intersectAction(null, loc, true, prevLoc);
							}
						}
						Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
						mInnerTicks++;
						particleAction(loc);

						if (mInnerTicks >= mDuration || mCaster.isDead()) {
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
		if (!PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, true).isEmpty()) {
			return mDelay + (20 * 5);
		} else {
			return 1;
		}
	}

}
