package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/*
 *
 * Shatter - All players within a 70 degree cone in front of the giant after
a 1 second charge up take 24 damage and are knocked back X blocks. If they
collide with a wall they take 10 additional damage and are stunned (Slowness 7,
Negative Jump Boost, weakness 10, maybe putting bows on cooldown, you get the
idea) for 2 seconds.
 */
public class Shatter extends Spell {
	private static final double DAMAGE = 25;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	public float mKnockback;

	private Map<Location, BlockState> mOldBlocks = new HashMap<>();

	private Location mStartLoc;

	public Shatter(Plugin plugin, LivingEntity boss, float knock, Location startLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mKnockback = knock;
		mStartLoc = startLoc;
	}

	@Override
	public void run() {
		FrostGiant.freezeGolems(mBoss);

		mBoss.setAI(false);
		World world = mBoss.getWorld();
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 50, true);
		players.removeIf(p -> p.getGameMode() != GameMode.SURVIVAL);
		Player target = null;

		//Choose random target
		if (players.size() == 1) {
			target = players.get(0);
		} else if (players.size() > 1) {
			target = players.get(FastUtils.RANDOM.nextInt(players.size()));
		}

		Player tar = target;
		if (tar != null) {
			Vector dir = LocationUtils.getDirectionTo(tar.getLocation(), mBoss.getLocation()).setY(0).normalize();
			mBoss.teleport(mBoss.getLocation().setDirection(dir));
		}

		Location loc = mBoss.getLocation();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			float mPitch = 0;

			@Override
			public void run() {
				mT += 2;
				mPitch += 0.025f;

				//Play shatter sound
				if (mT % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 3, mPitch);
				}

				//Every half-second, do visuals
				if (mT % 10 == 0) {
					//Creates 4 cones in 4 different directions
					for (int dir = 0; dir <= 270; dir += 90) {
						Vector vec;
						//The degree range is 60 degrees for 30 blocks radius
						for (double degree = 60; degree < 120; degree += 5) {
							for (double r = 0; r < 30; r++) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + dir);

								//Spawns particles
								Location l = loc.clone().add(vec);

								l.subtract(0, 1, 0);
								//Spawns crimson hyphae as a warning at a 1/3 rate, will try to climb 1 block up or down if needed
								if (l.getBlock().getType() != Material.CRIMSON_HYPHAE) {
									if (FastUtils.RANDOM.nextInt(3) == 0 || mT == 20 * 2) {
										while (l.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR && l.getBlockY() <= mStartLoc.getBlockY() + 3) {
											l.add(0, 1, 0);
										}
										if (l.getBlock().getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
											l.subtract(0, 1, 0);
										}
										//Once it leaves the arena, stop iterating
										if ((l.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)
											|| l.distance(mStartLoc) > FrostGiant.fighterRange) {
											continue;
										}
										//Move up one block if on barrier or bedrock level
										if (l.getBlock().getType() == Material.BEDROCK || l.getBlock().getType() == Material.BARRIER) {
											l.add(0, 1, 0);
										}
										if (l.getBlock().getType() != Material.CRIMSON_HYPHAE) {
											mOldBlocks.put(l, l.getBlock().getState());
										}
										l.getBlock().setType(Material.CRIMSON_HYPHAE);
									}
								}
							}
						}
					}
				}

				//End shatter, deal damage, show visuals
				if (mT >= 20 * 2.5) {
					mBoss.setAI(true);
					Mob mob = (Mob) mBoss;
					List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), FrostGiant.detectionRange, true);
					players.removeIf(p -> p.getGameMode() != GameMode.SURVIVAL);
					if (players.size() > 1) {
						Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
						if (mob.getTarget() != null) {
							while (player.getUniqueId().equals(mob.getTarget().getUniqueId())) {
								player = players.get(FastUtils.RANDOM.nextInt(players.size()));
							}
							mob.setTarget(player);
						}
					}
					this.cancel();
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 0.5f);
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 3, 0.5f);
					Vector vec;
					List<BoundingBox> boxes = new ArrayList<BoundingBox>();

					for (double r = 0; r < 30; r++) {
						for (int dir = 0; dir < 360; dir += 90) {
							for (double degree = 60; degree < 120; degree += 5) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + dir);

								Location l = loc.clone().add(vec);
								//1.5 -> 15
								BoundingBox box = BoundingBox.of(l, 0.65, 15, 0.65);
								boxes.add(box);
							}
						}
					}

					//Damage player by 35 in cone after warning is over (2 seconds) and knock player away
					for (Player player : PlayerUtils.playersInRange(loc, 40, true)) {
						if (player.getLocation().distance(mStartLoc) > FrostGiant.fighterRange) {
							continue;
						}

						List<Player> hitPlayers = new ArrayList<>();
						for (BoundingBox box : boxes) {
							if (player.getBoundingBox().overlaps(box) && !hitPlayers.contains(player)) {
								DamageUtils.damage(mBoss, player, DamageType.BLAST, DAMAGE, null, true, true, "Shatter");
								MovementUtils.knockAway(loc, player, mKnockback, 0.5f, false);
								AbilityUtils.silencePlayer(player, 20 * 5);
								hitPlayers.add(player);

								// If Eldrask is in the 4th phase (checked if material of weapon is Iron Hoe)
								// Add FGShatterLP tag (For Shattered Yet Standing advancement)
								if (EntityUtils.getMaxHealth(mBoss) * 0.15 > mBoss.getHealth()) {
									player.addScoreboardTag("FGShatterLP");
								}
							}
						}
					}

					FrostGiant.unfreezeGolems(mBoss);

					if (!mOldBlocks.isEmpty()) {
						BukkitRunnable runnable = new BukkitRunnable() {
							int mTicks = 0;

							@Override
							public void run() {
								mTicks++;

								if (mTicks >= 20 * 2 || mOldBlocks.isEmpty()) {
									//Restore everything that is currently hyphae to original state, and clear map
									for (Map.Entry<Location, BlockState> e : mOldBlocks.entrySet()) {
										if (e.getKey().getBlock().getType() == Material.CRIMSON_HYPHAE) {
											e.getValue().update(true, false);
										}
									}
									mOldBlocks.clear();

									this.cancel();
								} else {
									//Remove 100 blocks per tick
									Iterator<Map.Entry<Location, BlockState>> blockIter = mOldBlocks.entrySet().iterator();
									for (int i = 0; i < 100 && blockIter.hasNext(); i++) {
										Map.Entry<Location, BlockState> e = blockIter.next();
										if (e.getKey().getBlock().getType() == Material.CRIMSON_HYPHAE) {
											e.getValue().update(true, false);
										}
										blockIter.remove();
									}
								}
							}
						};
						runnable.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runnable);
					}
				}
			}

		};

		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public void cancel() {
		super.cancel();

		//Restore everything that is currently hyphae to original state, and clear map
		for (Map.Entry<Location, BlockState> e : mOldBlocks.entrySet()) {
			if (e.getKey().getBlock().getType() == Material.CRIMSON_HYPHAE) {
				e.getValue().update(true, false);
			}
		}
		mOldBlocks.clear();
	}

	@Override
	public int cooldownTicks() {
		return 7 * 20;
	}
}
