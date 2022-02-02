package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;

public class SpellAutoAttack extends Spell {

	private Plugin mPlugin;
	private Lich mLich;
	private LivingEntity mBoss;
	private Location mCenter;
	private int mTicks;
	private double mRange;
	private double mINC = 0;
	private int mPhase;
	private int mCeiling;
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);
	private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.0f);
	private PartialParticle mPSmoke1;
	private PartialParticle mPRed;
	private PartialParticle mPWitch;
	private PartialParticle mPCrit1;
	private PartialParticle mPCrit2;
	private PartialParticle mPSmoke2;
	private PartialParticle mPYellow;
	private PartialParticle mPSoul;
	private PartialParticle mPWitch2;
	private PartialParticle mPYellow2;

	public SpellAutoAttack(Plugin plugin, Lich lich, LivingEntity boss, Location loc, int ticks, double range, int ceil, int phase) {
		mPlugin = plugin;
		mLich = lich;
		mBoss = boss;
		mCenter = loc;
		mTicks = ticks;
		mRange = range;
		mCeiling = ceil;
		mPhase = phase;
		mPSmoke1 = new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 1, 0.35, 0, 0.35, 0.05);

		mPRed = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 2, 0.1, 0.1, 0.1, 0.1, RED);
		mPWitch = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.1);
		mPCrit1 = new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 2, 0.1, 0.1, 0.1, 0.1);
		mPCrit2 = new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 20, 0.25, 0.25, 0.25, 0.25);

		mPSmoke2 = new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 15, 0, 0, 0, 0.25);
		mPYellow = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 15, 0.2, 0.2, 0.2, 0.25, YELLOW);
		mPSoul = new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 2, 0.35, 0.35, 0.35, 0.025);
		mPWitch2 = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 4, 0.2, 0.2, 0.2, 0.125);
		mPYellow2 = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 2, 0.2, 0.2, 0.2, 0.25, YELLOW);
	}

	@Override
	public void run() {
		mINC += 5;
		double cd = mTicks;
		if (mPhase >= 4) {
			cd = mTicks;
		}
		if (mINC >= cd) {
			mINC -= cd;
			if (!mLich.hasRunningSpellOfType(SpellDiesIrae.class)) {
				bolt();
			}
		}
	}

	private void bolt() {
		World world = mBoss.getWorld();
		List<Player> tooClose = Lich.playersInRange(mBoss.getLocation(), 6, true);

		if (tooClose.size() > 0 && mBoss.getLocation().getY() < mCenter.getY() + 3) {
			Collections.shuffle(tooClose);
			Player target = tooClose.get(FastUtils.RANDOM.nextInt(tooClose.size()));
			attack(target, world);
		} else {
			if (Lich.playersInRange(mBoss.getLocation(), mRange, true).size() > 0) {
				BukkitRunnable runA = new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						mTicks++;
						if (mTicks == 1) {
							world.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 1.5f, 0.75f);
							world.playSound(mBoss.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 0);
						}
						mPSmoke1.location(mBoss.getLocation()).spawnAsBoss();
						mBoss.removePotionEffect(PotionEffectType.SLOW);
						mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 1));

						if (mBoss == null || mBoss.isDead()) {
							this.cancel();
							return;
						}

						if (mTicks >= 30) {
							List<Player> players = Lich.playersInRange(mCenter, mRange, true);
							players.removeIf(p -> SpellDimensionDoor.getShadowed().contains(p) || p.getLocation().getY() >= mCenter.getY() + mCeiling);
							if (players.size() > 0) {
								if (mBoss.getLocation().getY() >= mCenter.getY() + 3) {
									Collections.shuffle(players);
									List<Player> targets = players.subList(0, (int) Math.min(players.size(), Math.max(2, Math.ceil(players.size() / 5))));
									for (Player p : targets) {
										launchBolt(p);
									}
								} else if (mBoss instanceof Mob) {
									Mob mob = (Mob) mBoss;
									if (mob.getTarget() != null && mob.getTarget() instanceof Player) {
										launchBolt((Player)mob.getTarget());
									} else {
										Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
										world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 1.5f, 0.75f);
										launchBolt(player);
									}
								} else {
									Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
									world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 1.5f, 0.75f);
									launchBolt(player);
								}
							}
							this.cancel();
						}
					}

				};
				runA.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(runA);
			}
		}
	}

	private void attack(Player target, World world) {
		world.playSound(mBoss.getLocation(), Sound.ENTITY_CAT_HISS, 3.0f, 0.5f);

		Vector dir = LocationUtils.getDirectionTo(target.getLocation().add(0, 1, 0), mBoss.getLocation());
		Location tloc = mBoss.getLocation().setDirection(dir);
		BukkitRunnable runB = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				Vector v;
				for (double r = 0; r <= 5; r += 0.75) {
					for (double degree = -40; degree < 40; degree += 10) {
						double radian1 = Math.toRadians(degree);
						v = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
						v = VectorUtils.rotateXAxis(v, 0);
						v = VectorUtils.rotateYAxis(v, tloc.getYaw() + 90);

						Location loc = mBoss.getLocation().clone().add(v);
						mPRed.location(loc).spawnAsBoss();
					}
				}
				if (mT >= 10) {
					Vector vec;
					for (double r1 = 0; r1 <= 5; r1 += 0.75) {
						for (double degree1 = -40; degree1 < 40; degree1 += 10) {
							double radian2 = Math.toRadians(degree1);
							vec = new Vector(Math.cos(radian2) * r1, 0, Math.sin(radian2) * r1);
							vec = VectorUtils.rotateXAxis(vec, 0);
							vec = VectorUtils.rotateYAxis(vec, tloc.getYaw() + 90);

							Location l = mBoss.getLocation().clone().add(vec);
							mPWitch.location(l).spawnAsBoss();
							mPCrit1.location(l).spawnAsBoss();
							BoundingBox box = BoundingBox.of(l, 0.4, 10, 0.4);

							for (Player player : Lich.playersInRange(mBoss.getLocation(), 10, true)) {
								if (player.getBoundingBox().overlaps(box)) {
									MovementUtils.knockAway(mBoss.getLocation(), player, 1.5f, 0.5f);
									mPCrit2.location(player.getLocation()).spawnAsBoss();
									damage(player, true);
								}
							}
						}
					}
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3.0f, 1.0f);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 3.0f, 0.5f);
					this.cancel();
				}
			}
		};
		runB.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runB);
	}

	private void launchBolt(Player player) {
		BukkitRunnable runC = new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(mBoss.getEyeLocation(), 0.3, 0.3, 0.3);
			int mInnerTicks = 0;

			@Override
			public void run() {
				World w = mBoss.getWorld();
				Vector dir = LocationUtils.getDirectionTo(player.getLocation().add(0, 1, 0), mBoss.getEyeLocation());
				Location detLoc = mBoss.getLocation();
				List<Player> players = Lich.playersInRange(detLoc, 75, true);
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				for (int j = 0; j < 2; j++) {
					mBox.shift(dir.clone().multiply(0.9 * 0.5));
					Location loc = mBox.getCenter().toLocation(mBoss.getWorld());
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(mBox)) {
							damage(player, false);
							mPSmoke2.location(loc).spawnAsBoss();
							mPYellow.location(loc).spawnAsBoss();
							w.playSound(loc, Sound.ENTITY_WITHER_HURT, 1, 0.75f);
							this.cancel();
						}
					}

					if (loc.getBlock().getType().isSolid() && (mPhase != 4 || loc.getY() <= mCenter.getY())) {
						mPSmoke2.location(loc).spawnAsBoss();
						mPYellow.location(loc).spawnAsBoss();
						w.playSound(loc, Sound.ENTITY_WITHER_HURT, 1, 0.75f);
						this.cancel();
					}
				}
				Location loc = mBox.getCenter().toLocation(mBoss.getWorld());
				mPSoul.location(loc).spawnAsBoss();
				mPWitch2.location(loc).spawnAsBoss();
				mPYellow2.location(loc).spawnAsBoss();

				mInnerTicks++;
				if (mInnerTicks >= 20 * 5 || mBoss == null || !mBoss.isValid()) {
					this.cancel();
				}
			}
		};
		runC.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runC);
	}

	private void damage(Player player, boolean melee) {
		//TODO replace melee with this after dual type damage is fixed
		/*
		if (mPhase == 1) {
				BossUtils.dualTypeBlockableDamage(mBoss, player, DamageType.MAGIC, DamageType.MELEE, 20, 0.75, "Death Sweep", mBoss.getLocation());
			} else if (mPhase == 2) {
				BossUtils.dualTypeBlockableDamage(mBoss, player, DamageType.MAGIC, DamageType.MELEE, 23, 0.75, "Death Sweep", mBoss.getLocation());
				AbilityUtils.increaseDamageRecievedPlayer(player, 20 * 5, 0.2, "Lich");
			} else if (mPhase == 3) {
				BossUtils.dualTypeBlockableDamage(mBoss, player, DamageType.MAGIC, DamageType.MELEE, 26, 0.75, "Death Sweep", mBoss.getLocation());
				AbilityUtils.increaseDamageRecievedPlayer(player, 20 * 5, 0.2, "Lich");
				AbilityUtils.increaseDamageDealtPlayer(player, 20 * 5, -0.2, "Lich");
			} else {
				BossUtils.dualTypeBlockableDamage(mBoss, player, DamageType.MAGIC, DamageType.MELEE, 29, 0.75, "Death Sweep", mBoss.getLocation());
				AbilityUtils.increaseDamageRecievedPlayer(player, 20 * 5, 0.2, "Lich");
				AbilityUtils.increaseDamageDealtPlayer(player, 20 * 5, -0.2, "Lich");
			}
		 */
		String cause = "Death Bolt";
		if (melee) {
			cause = "Death Sweep";
		}
		if (mPhase == 1) {
			BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, 21, cause, mBoss.getLocation());
		} else if (mPhase == 2) {
			BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, 24, cause, mBoss.getLocation());
			AbilityUtils.increaseDamageRecievedPlayer(player, 20 * 5, 0.2, "Lich");
		} else if (mPhase == 3) {
			BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, 27, cause, mBoss.getLocation());
			AbilityUtils.increaseDamageRecievedPlayer(player, 20 * 5, 0.2, "Lich");
			AbilityUtils.increaseDamageDealtPlayer(player, 20 * 5, -0.2, "Lich");
		} else {
			BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, 30, cause, mBoss.getLocation());
			AbilityUtils.increaseDamageRecievedPlayer(player, 20 * 5, 0.2, "Lich");
			AbilityUtils.increaseDamageDealtPlayer(player, 20 * 5, -0.2, "Lich");
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
