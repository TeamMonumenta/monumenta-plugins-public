package com.playmonumenta.plugins.bosses.spells.shura;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellShuraJump extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRange;
	private boolean mTrigger = true;
	private double mVelocityMultiplier = 0.7;
	private int mCooldown = 8 * 20;
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);

	public SpellShuraJump(Plugin plugin, LivingEntity boss, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
	}

	@Override
	public void run() {

		BukkitRunnable a = new BukkitRunnable() {
			int mT = 0;
			List<Player> mTargeted = new ArrayList<Player>();

			@Override
			public void run() {
				//List is farthest players in the beginning, and nearest players at the end
				List<Player> players = EntityUtils.getNearestPlayers(mBoss.getLocation(), mRange);
				players.removeAll(mTargeted);
				if (mTrigger && players.size() > 0) {
					mTrigger = false;
					jump(players.get(0));
					mTargeted.add(players.get(0));
					mT++;
				}
				if (players.size() == 0) {
					this.cancel();
				}
			}
		};
		a.runTaskTimer(mPlugin, 0, 10);
		mActiveRunnables.add(a);
	}

	private void jump(Player p) {
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();
		Location locTarget = p.getLocation();
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


		Vector velocity = locTarget.subtract(moveTo).toVector().normalize().multiply(mVelocityMultiplier);
		velocity.setY(1.1);

		final Player finalTargetPlayer = p;
		final Vector finalVelocity = velocity;

		BukkitRunnable leap = new BukkitRunnable() {
			Location mLeapLocation = moveTo;
			Vector mDirection = finalVelocity;
			boolean mLeaping = false;
			boolean mHasBeenOneTick = false;

			@Override
			public void run() {
				if (!mLeaping) {
					// start leaping
					if (mBoss.getLocation().distance(mLeapLocation) < 1) {
						world.playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 15, 1, 0f, 1, 0).spawnAsEntityActive(mBoss);
						((Mob) mBoss).getPathfinder().stopPathfinding();
						mBoss.setVelocity(finalVelocity);
						mLeaping = true;
					}
				} else {
					new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f)).spawnAsEntityActive(mBoss);
					mBoss.setFallDistance(0);
					if (mBoss.isOnGround() && mHasBeenOneTick) {
						land(mDirection, world);
						this.cancel();
						return;
					}

					BoundingBox hitbox = mBoss.getBoundingBox();
					for (Player player : mBoss.getWorld().getPlayers()) {
						if (player.getBoundingBox().overlaps(hitbox) && mHasBeenOneTick) {
							((Mob) mBoss).setTarget(player);
							land(mDirection, world);
							this.cancel();
							return;
						}
					}

					// Give the caller a chance to run extra effects or manipulate the boss's leap velocity
					if (finalTargetPlayer.isOnline()) {
						Vector towardsPlayer = finalTargetPlayer.getLocation().subtract(mBoss.getLocation()).toVector().setY(0).normalize();
						Vector originalVelocity = mBoss.getVelocity();
						double scale = 0.5;
						Vector newVelocity = new Vector();
						newVelocity.setX((originalVelocity.getX() * 20 + towardsPlayer.getX() * scale) / 20);
						// Use the original mob's vertical velocity, so it doesn't somehow fall faster than gravity
						newVelocity.setY(originalVelocity.getY());
						newVelocity.setZ((originalVelocity.getZ() * 20 + towardsPlayer.getZ() * scale) / 20);
						mBoss.setVelocity(newVelocity);
					}

					// At least one tick has passed to avoid insta smacking a nearby player
					mHasBeenOneTick = true;
				}
			}
		};

		leap.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(leap);
	}

	private void land(Vector v, World world) {
		ParticleUtils.explodingRingEffect(mPlugin, mBoss.getLocation(), 4, 1, 4,
			Arrays.asList(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.5, (Location location) -> {
					new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
				})
			));
		world.playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_HURT, 1f, 0.5f);
		Location tloc = mBoss.getLocation().setDirection(v);
		BukkitRunnable runB = new BukkitRunnable() {
			int mT = 0;
			PartialParticle mPRed = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 2, 0.1, 0.1, 0.1, 0.1, RED);

			@Override
			public void run() {
				mT++;
				Vector v;
				for (double r = 0; r <= 4; r += 0.75) {
					for (double degree = -90; degree < 90; degree += 10) {
						double radian1 = Math.toRadians(degree);
						v = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
						v = VectorUtils.rotateXAxis(v, 0);
						v = VectorUtils.rotateYAxis(v, tloc.getYaw() + 90);

						Location loc = mBoss.getLocation().clone().add(v);
						mPRed.location(loc).spawnAsBoss();
					}
				}
				if (mT >= 5) {
					mTrigger = true;
					Vector vec;
					for (double r1 = 0; r1 <= 4; r1 += 0.75) {
						for (double degree1 = -90; degree1 < 90; degree1 += 10) {
							double radian2 = Math.toRadians(degree1);
							vec = new Vector(Math.cos(radian2) * r1, 0, Math.sin(radian2) * r1);
							vec = VectorUtils.rotateXAxis(vec, 0);
							vec = VectorUtils.rotateYAxis(vec, tloc.getYaw() + 90);

							Location l = mBoss.getLocation().clone().add(vec);
							new PartialParticle(Particle.SWEEP_ATTACK, l, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
							BoundingBox box = BoundingBox.of(l, 0.4, 10, 0.4);

							for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 7, true)) {
								if (player.getBoundingBox().overlaps(box)) {
									MovementUtils.knockAway(mBoss.getLocation(), player, 0.75f, 0.5f);
									BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, 25, "Corrupted Strike", mBoss.getLocation());
								}
							}
						}
					}
					world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1f);
					this.cancel();
				}
			}
		};
		runB.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runB);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
