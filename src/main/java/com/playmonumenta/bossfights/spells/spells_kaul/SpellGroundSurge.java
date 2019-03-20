package com.playmonumenta.bossfights.spells.spells_kaul;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.utils.Utils;

public class SpellGroundSurge extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRange;
	private final Random random = new Random();
	public SpellGroundSurge(Plugin plugin, LivingEntity boss, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mBoss.removePotionEffect(PotionEffectType.SLOW);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int)(20 * 2.75), 1));
		Player target = (Player)((Mob) mBoss).getTarget();
		List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);
		players.removeIf(p -> p.getLocation().getY() >= 61);
		new BukkitRunnable() {
			int t = 0;
			float pitch = 0;
			@Override
			public void run() {
				t++;
				Location loc = mBoss.getLocation();
				pitch += 0.025;
				if (t % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, 3, pitch);
				}
				world.spawnParticle(Particle.BLOCK_DUST, loc, 8, 0.4, 0.1, 0.4, 0.25, Material.COARSE_DIRT.createBlockData());
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.25, 0.1, 0.25, 0.25);

				if (t >= 20 * 2.5) {
					this.cancel();
					final int targets;
					if (players.size() <= 1) {
						targets = 1;
					} else if (players.size() == 2) {
						targets = 2;
					} else if (players.size() >= 3 && players.size() <= 6) {
						targets = 3;
					} else if (players.size() >= 7 && players.size() <= 15) {
						targets = 4;
					} else {
						targets = 5;
					}

					List<Player> toHit = new ArrayList<Player>();
					while (toHit.size() < targets) {
						Player player = players.get(random.nextInt(players.size()));
						if (!toHit.contains(player)) {
							toHit.add(player);
						}
					}

					for (Player target : toHit) {
						new BukkitRunnable() {
							int i = 0;
							Location nloc = mBoss.getLocation().add(0, 0.5, 0);
							BoundingBox box = BoundingBox.of(nloc, 0.65, 0.65, 0.65);
							Vector dir = Utils.getDirectionTo(target.getLocation(), nloc).setY(0).normalize();
							@Override
							public void run() {
								i++;
								box.shift(dir.clone().multiply(1.1));
								Location bLoc = box.getCenter().toLocation(world);
								if (bLoc.getBlock().getType().isSolid()) {
									bLoc.add(0, 1, 0);
									if (bLoc.getBlock().getType().isSolid()) {
										this.cancel();
										bLoc.subtract(0, 1, 0);
									}
								}

								if (!bLoc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
									bLoc.subtract(0, 1, 0);
									if (!bLoc.getBlock().getType().isSolid()) {
										bLoc.subtract(0, 1, 0);
										if (!bLoc.getBlock().getType().isSolid()) {
											this.cancel();
										}
									}
								}
								bLoc.add(0, 0.5, 0);

								if (i >= 45) {
									this.cancel();
								}

								world.playSound(bLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.75f, 1);
								world.spawnParticle(Particle.BLOCK_DUST, bLoc, 20, 0.5, 0.5, 0.5, 0.25, Material.COARSE_DIRT.createBlockData());
								world.spawnParticle(Particle.FLAME, bLoc, 15, 0.5, 0.5, 0.5, 0.075);
								world.spawnParticle(Particle.LAVA, bLoc, 2, 0.5, 0.5, 0.5, 0.25);
								for (Player player : players) {
									if (player.getBoundingBox().overlaps(box)) {
										this.cancel();
										player.damage(20, mBoss);
										player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 20, 2));
										Utils.KnockAway(mBoss.getLocation(), player, 0.3f, 1.25f);
										world.spawnParticle(Particle.SMOKE_LARGE, bLoc, 20, 0, 0, 0, 0.2);
										world.spawnParticle(Particle.FLAME, bLoc, 75, 0, 0, 0, 0.25);
										world.playSound(bLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
										// Send surges to all other players now.
										Player rPlayer = players.get(random.nextInt(players.size()));
										while (rPlayer.getUniqueId().equals(target.getUniqueId())) {
											rPlayer = players.get(random.nextInt(players.size()));
										}
										Player tPlayer = rPlayer;
										new BukkitRunnable() {
											Player _tPlayer = tPlayer;
											BoundingBox box = BoundingBox.of(bLoc, 0.4, 0.4, 0.4);
											int j = 0;
											int hits = 0;
											List<UUID> hit = new ArrayList<UUID>();
											@Override
											public void run() {
												j++;
												Location _bLoc = box.getCenter().toLocation(world);
												Vector dir = Utils.getDirectionTo(_tPlayer.getLocation(), _bLoc).setY(0).normalize();
												box.shift(dir.clone().multiply(0.7));
												if (_bLoc.getBlock().getType().isSolid()) {
													_bLoc.add(0, 1, 0);
													if (_bLoc.getBlock().getType().isSolid()) {
														this.cancel();
														_bLoc.subtract(0, 1, 0);
													}
												}
												if (!_bLoc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
													_bLoc.subtract(0, 1, 0);
													if (!_bLoc.getBlock().getType().isSolid()) {
														_bLoc.subtract(0, 1, 0);
														if (!_bLoc.getBlock().getType().isSolid()) {
															this.cancel();
														}
													}
												}
												_bLoc.add(0, 1, 0);
												world.playSound(_bLoc, Sound.BLOCK_STONE_BREAK, 0f, 1);
												//Have particles with collision show only for the player who's targetted.
												//This is to prevent lag from the numerous other surges that have these same
												//Particles
												player.spawnParticle(Particle.BLOCK_DUST, _bLoc, 8, 0.2, 0.2, 0.2, 0.25, Material.COARSE_DIRT.createBlockData());
												world.spawnParticle(Particle.FLAME, _bLoc, 6, 0.2, 0.2, 0.2, 0.075);
												player.spawnParticle(Particle.LAVA, _bLoc, 1, 0.2, 0.2, 0.2, 0.25);
												for (Player _player : players) {
													if (_player.getBoundingBox().overlaps(box)
															&& !_player.getUniqueId().equals(player.getUniqueId())
															&& !hit.contains(_player.getUniqueId())) {
														hit.add(_player.getUniqueId());
														_player.damage(18, mBoss);
														_player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 20, 2));
														Utils.KnockAway(mBoss.getLocation(), _player, 0.175f, 0.85f);
														world.spawnParticle(Particle.SMOKE_LARGE, _bLoc, 10, 0, 0, 0, 0.2);
														world.spawnParticle(Particle.FLAME, _bLoc, 50, 0, 0, 0, 0.25);
														world.playSound(_bLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1.25f);
														hits++;
														if (hits < players.size() && hits <= 2) {
															int attempts = 0;
															_tPlayer = players.get(random.nextInt(players.size()));
															while (hit.contains(_tPlayer.getUniqueId())) {
																//A rare case can occur where the loop has gone through all of the possible
																//players, but they have been hit. Add an attempt integer to make sure that
																//it does not cause an infinite loop.
																if (attempts < 5) {
																	_tPlayer = players.get(random.nextInt(players.size()));
																	attempts++;
																} else {
																	this.cancel();
																	break;
																}
															}
														} else {
															this.cancel();
														}
													}
												}
												if (j >= 20 * 1.25) {
													this.cancel();
												}
											}

										}.runTaskTimer(mPlugin, 0, 1);
										break;
									}
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);
					}
				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean canRun() {
		return ((Mob) mBoss).getTarget() != null && ((Mob) mBoss).getTarget() instanceof Player;
	}

	@Override
	public int duration() {
		return 20 * 10;
	}

}
