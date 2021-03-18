package com.playmonumenta.plugins.bosses.spells.frostgiant;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 Glacial Prison - Traps ⅓ players in ice for 3 seconds, after those 3
 seconds the prison explodes dealing 20 damage and giving mining fatigue
 3 for 10 seconds and weakness 2 for 10 seconds.
 */
public class SpellGlacialPrison extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRange;
	private Location mStartLoc;

	private boolean mCooldown = false;

	public SpellGlacialPrison(Plugin plugin, LivingEntity boss, double range, Location start) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mStartLoc = start;
	}

	@Override
	public void run() {
		FrostGiant.freezeGolems(mBoss);
		FrostGiant.delayHailstormDamage();
		//Glacial Prison can not be cast whithin 60 seconds of the previous cast of it
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 60);

		//Plays warning sound and chooses 1/3 of players to target randomly
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3, 0.5f);
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange);
		List<Player> targets = new ArrayList<Player>();
		if (players.size() >= 2) {
			int cap = (int) Math.ceil(players.size() / 2);
			for (int i = 0; i < cap; i++) {
				Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
				if (!targets.contains(player)) {
					targets.add(player);
				} else {
					cap++;
				}
			}
		} else {
			targets = players;
		}

		for (Player player : targets) {
			new BukkitRunnable() {
				int mT = 0;
				@Override
				public void run() {
					mT++;
					world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 2, 0.4, 0.4, 0.4, 0.05);

					if (mBoss.isDead() || !mBoss.isValid()) {
						this.cancel();
					}

					if (mT >= 40) {
						this.cancel();
						//Blocks
						world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.2);
						world.spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation().add(0, 1, 0), 35, 0.4, 0.4, 0.4, 0.25);
						world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.5f);
						world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.75f);

						//Center the player first
						Vector dir = player.getLocation().getDirection();
						Location l = player.getLocation().getBlock().getLocation().add(0.5, 0.15, 0.5).setDirection(dir);
						while (l.getY() - mStartLoc.getY() >= 3) {
							l.add(0, -1, 0);
						}
						player.teleport(l);

						Location center = player.getLocation();
						Location[] locs = new Location[] {
							//First Layer
							center.clone().add(1, 0, 0),
							center.clone().add(-1, 0, 0),
							center.clone().add(0, 0, 1),
							center.clone().add(0, 0, -1),

							//Second Layer
							center.clone().add(1, 1, 0),
							center.clone().add(-1, 1, 0),
							center.clone().add(0, 1, 1),
							center.clone().add(0, 1, -1),

							//Top & Bottom
							center.clone().add(0, 2, 0),
							center.clone().add(0, -1, 0)
						};

						Material[] mats = new Material[locs.length];
						for (int i = 0; i < locs.length; i++) {
							Location loc = locs[i];
							mats[i] = loc.getBlock().getType();
							loc.getBlock().setType(Material.BLUE_ICE);
						}

						center.clone().add(1, 1, 0).getBlock().setType(Material.ICE);
						center.clone().add(-1, 1, 0).getBlock().setType(Material.ICE);
						center.clone().add(0, 1, 1).getBlock().setType(Material.ICE);
						center.clone().add(0, 1, -1).getBlock().setType(Material.ICE);

						center.clone().add(0, -1, 0).getBlock().setType(Material.SEA_LANTERN);

						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 4, 1));

						//Only lasts 4 seconds, needs to be done more than once
						FrostGiant.delayHailstormDamage();

						new BukkitRunnable() {
							int mTicks = 0;
							float mPitch = 0;
							@Override
							public void run() {
								mTicks++;
								mPitch += 0.02f;

								Location middle = center.clone().add(0, 1, 0);

								if (mBoss.isDead() || !mBoss.isValid()) {
									this.cancel();
									for (int i = 0; i < locs.length; i++) {
										Location loc = locs[i];
										if (mats[i] != Material.FROSTED_ICE) {
											loc.getBlock().setType(mats[i]);
										} else {
											loc.getBlock().setType(Material.AIR);
										}
									}
								}

								world.spawnParticle(Particle.FIREWORKS_SPARK, middle, 3, 1, 1, 1, 0);
								world.spawnParticle(Particle.CLOUD, middle, 2, 1, 1, 1, 0);
								world.spawnParticle(Particle.DAMAGE_INDICATOR, middle, 1, 0.5, -0.25, 0.5, 0.005);

								if (mTicks % 10 == 0) {
									world.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1, mPitch);
								}

								if (mTicks == 20 * 2) {
									FrostGiant.delayHailstormDamage();
								}

								//If player did not escape within 4 seconds, damage by 80% of health and remove the ice prison
								if (mTicks >= 20 * 4) {
									FrostGiant.unfreezeGolems(mBoss);
									this.cancel();
									for (int i = 0; i < locs.length; i++) {
										Location loc = locs[i];
										if (mats[i] != Material.FROSTED_ICE) {
											loc.getBlock().setType(mats[i]);
										} else {
											loc.getBlock().setType(Material.AIR);
										}
									}
									world.spawnParticle(Particle.FIREWORKS_SPARK, middle, 50, 1, 1, 1, 0.35);
									world.spawnParticle(Particle.CLOUD, middle, 75, 1, 1, 1, 0.25);
									world.playSound(center, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.75f);
									world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.75f);
									if (player.getLocation().distance(center) <= 2) {
										BossUtils.bossDamagePercent(mBoss, player, 0.8);
									}
								}
							}
						}.runTaskTimer(mPlugin, 0, 2);
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return 10 * 20;
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

}
