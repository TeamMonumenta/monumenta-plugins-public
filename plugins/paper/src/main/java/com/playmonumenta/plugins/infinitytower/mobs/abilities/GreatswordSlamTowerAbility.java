package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Creature;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class GreatswordSlamTowerAbility extends TowerAbility {

	private static final Material ICE_TYPE = Material.FROSTED_ICE;

	private static final double DEG = 50;
	private static final int DAMAGE = 8;
	private static final int DURATION = 8 * 20;
	private static final int COOLDOWN = 10 * 20;
	private static final Set<Material> BLACKLIST_MATERIALS = new HashSet<>();

	static {
		for (Material mat : Material.values()) {
			if (mat.name().toLowerCase(Locale.getDefault()).contains("carpet")) {
				BLACKLIST_MATERIALS.add(mat);
			}
		}

		BLACKLIST_MATERIALS.add(Material.BEDROCK);
		BLACKLIST_MATERIALS.add(Material.BARRIER);
	}

	private static final Particle.DustOptions BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(66, 185, 245), 1.0f);

	public GreatswordSlamTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);


		Spell spell = new Spell() {

			@Override
			public void run() {
				World world = mBoss.getWorld();
				Creature c = (Creature) mBoss;
				Pathfinder pathfinder = c.getPathfinder();

				pathfinder.stopPathfinding();
				Vector bossDir = mBoss.getLocation().getDirection();
				Location loc = mBoss.getLocation();

				BukkitRunnable runnable1 = new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {

						mT += 10;
						if (mT > 20 * 3.5) {
							this.cancel();
						}

						for (int r = 0; r < 30; r += 2) {
							for (double degree = 90 - DEG/2; degree <= 90 + DEG/2; degree += 5) {
								double radian1 = Math.toRadians(degree);
								Vector vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

								Location l = loc.clone().add(vec);
								while (l.getBlock().getType() != Material.AIR && l.getBlockY() <= loc.getBlockY() + 3) {
									l.add(0, 1, 0);
								}
								new PartialParticle(Particle.SPELL_WITCH, l, 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.END_ROD, l, 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
							}
						}

						if (mT <= 70) {
							mBoss.teleport(mBoss.getLocation().setDirection(bossDir));
						}
					}
				};
				runnable1.runTaskTimer(mPlugin, 0, 10);
				mActiveRunnables.add(runnable1);


				BukkitRunnable runnable2 = new BukkitRunnable() {
					int mT = 0;
					final List<LivingEntity> mHitPlayers = new ArrayList<>();
					@Override
					public void run() {
						mT += 2;

						if (mT <= 30 && mT >= 20) {
							//Initiates the jump upwards
							mBoss.setVelocity(new Vector(0, 1.5, 0));
						} else if (mT >= 30) {
							if (!mBoss.isOnGround()) {
								//Initiates the slam down
								mBoss.setVelocity(new Vector(0, -1.5, 0));
							} else {
								//Creates the giant 30 degree cone rift of damage
								world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1, 0);
								BukkitRunnable runnable = new BukkitRunnable() {
									int mRadius = 0;
									@Override
									public void run() {

										mBoss.setVelocity(new Vector(0, 0, 0));
										pathfinder.stopPathfinding();

										if (mRadius >= 30) {
											this.cancel();
										}

										//In the current radius, makes a cone of frostsed ice and various other particles
										//If player is in trajectory (in bounding box), damage them and knock back
										Vector vec;
										List<BoundingBox> boxes = new ArrayList<>();
										for (double degree = 90 - DEG/2; degree <= 90 + DEG/2; degree += 5) {

											double radian1 = Math.toRadians(degree);
											vec = new Vector(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
											vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

											//Also have to clone location because of use in HashMap, can not optimize
											Location l = loc.clone().add(vec).add(0, -1, 0);
											//Move down one block to not overshoot, sometimes boss can stand on a single block, affects location
											if (l.getBlock().getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
												l.add(0, -1, 0);
											}
											//Once it leaves the arena, stop iterating
											if (l.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
												continue;
											}

											if (!mGame.isInArena(l)) {
												continue;
											}

											//If on bedrock or barriers, move up one to not replace that
											if (BLACKLIST_MATERIALS.contains(l.getBlock().getType())) {
												l.add(0, 1, 0);
											}

											//Put less frosted ice than the entire cone
											if (degree % 10 == 0) {
												Block block = l.getBlock();
												if (TemporaryBlockChangeManager.INSTANCE.changeBlock(block, ICE_TYPE, DURATION - mRadius + FastUtils.randomIntInRange(0, 10))) {
													Ageable age = (Ageable) block.getBlockData();
													age.setAge(1 + FastUtils.RANDOM.nextInt(3));
													block.setBlockData(age);
												}
											}

											//15 -> 3.65 lol
											BoundingBox box = BoundingBox.of(l, 1, 3.65, 1);
											boxes.add(box);

											FallingBlock fallBlock = world.spawnFallingBlock(l.add(0, 0.4, 0), Bukkit.createBlockData(Material.BLUE_ICE));
											fallBlock.setDropItem(false);
											EntityUtils.disableBlockPlacement(fallBlock);
											fallBlock.setVelocity(new Vector(0, 0.4, 0));
											fallBlock.setHurtEntities(false);

											new BukkitRunnable() {
												@Override
												public void run() {
													if (fallBlock.isValid()) {
														fallBlock.remove();
													}
												}
											}.runTaskLater(mPlugin, 15);

											new PartialParticle(Particle.CLOUD, l, 2, 0.15, 0.15, 0.15, 0.125).spawnAsEntityActive(mBoss);
											new PartialParticle(Particle.CRIT, l, 8, 0.15, 0.15, 0.15, 0.7).spawnAsEntityActive(mBoss);
											new PartialParticle(Particle.REDSTONE, l, 8, 0.15, 0.15, 0.15, BLUE_COLOR).spawnAsEntityActive(mBoss);
											if (degree > 85 && degree < 95 && mRadius % 5 == 0) {
												world.playSound(l, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0);
											}
										}
										for (LivingEntity target : mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs()) {

											for (BoundingBox box : boxes) {
												if (target.getBoundingBox().overlaps(box) && !mHitPlayers.contains(target)) {
													DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false);
													MovementUtils.knockAway(loc, target, 0f, 1.5f, false);
													mHitPlayers.add(target);
													break;
												}
											}
										}
										mRadius++;
									}
								};
								runnable.runTaskTimer(mPlugin, 0, 1);
								mActiveRunnables.add(runnable);

								this.cancel();
							}
						} else {
							mBoss.setVelocity(new Vector(0, 0, 0));
							pathfinder.stopPathfinding();
						}
					}
				};
				runnable2.runTaskTimer(mPlugin, 0, 2);
				mActiveRunnables.add(runnable2);

				mActiveTasks.add(new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {

						//Stop running after duration seconds
						if (mT >= DURATION || mBoss.isDead() || !mBoss.isValid() || mGame.isTurnEnded() || mGame.isGameEnded()) {
							this.cancel();
						}
						for (LivingEntity target : new ArrayList<>(mIsPlayerMob ? mGame.mFloorMobs : mGame.mPlayerMobs)) {
							if ((target.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR || target.getLocation().getBlock().getType() != Material.AIR)
								    && (target.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == ICE_TYPE || target.getLocation().getBlock().getType() == ICE_TYPE)) {

								plugin.mEffectManager.addEffect(target, "ITPercentSpeedModifyGreatsword",
									new PercentSpeed(20, -0.3, "ITPercentSpeedModifyGreatsword"));

							}
						}
						mT += 10;
					}
				}.runTaskTimer(mPlugin, 0, 10)); //Every 0.5 seconds, check if player is on cone area damage

			}

			@Override
			public int cooldownTicks() {
				return COOLDOWN;
			}
		};

		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, (int) (FastUtils.RANDOM.nextDouble() * 100) + 20);
	}
}
