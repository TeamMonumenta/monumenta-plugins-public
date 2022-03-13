package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class AdvancingShadowTowerAbility extends TowerAbility {

	private LivingEntity mTarget = null;

	public AdvancingShadowTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		Spell spell = new Spell() {

			private LivingEntity getTarget() {
				List<LivingEntity> list = (mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs());
				list.removeIf(le -> le.getScoreboardTags().contains(TowerConstants.MOB_TAG_UNTARGETABLE));
				list.sort((a, b) -> {
					return (int) (a.getHealth() - b.getHealth());
				});
				return list.size() > 0 ? list.get(0) : null;
			}


			@Override
			public void run() {
				mTarget = getTarget();

				if (mTarget != null && mTarget.isValid() && !mTarget.isDead()) {

					BukkitRunnable runnable = new BukkitRunnable() {
						Boolean mHasTP = false;

						@Override
						public void run() {

							if (mTarget == null || !mTarget.isValid() || mTarget.isDead()) {
								mTarget = getTarget();
							}

							if (mTarget == null || mTarget.isDead() || !mTarget.isValid()) {
								mTarget = null;
								cancel();
								return;
							}

							Location loc = mTarget.getLocation();
							World world = mTarget.getWorld();
							loc.setY(loc.getY() + 0.1f);
							Vector shift = loc.getDirection();
							shift.setY(0).normalize().multiply(-0.5);

							// Check from farthest horizontally to closest, lowest vertically to highest
							for (int horizontalShift = 8; horizontalShift > 0; horizontalShift--) {
								for (int verticalShift = 0; verticalShift <= 3; verticalShift++) {
									Location locTest = loc.clone().add(shift.clone().multiply(horizontalShift));
									locTest.setY(locTest.getY() + verticalShift);
									if (canTeleport(locTest) && mGame.isInArena(locTest)) {
										loc.add(0, mBoss.getHeight() / 2, 0);
										world.spawnParticle(Particle.SPELL_WITCH, loc, 30, 0.25, 0.45, 0.25, 1);
										world.spawnParticle(Particle.SMOKE_LARGE, loc, 12, 0, 0.45, 0, 0.125);

										mBoss.teleport(locTest);
										mHasTP = true;
										if (mBoss instanceof Mob mob) {
											GenericTowerMob towerMob = BossManager.getInstance().getBoss(mob, GenericTowerMob.class);
											if (towerMob != null) {
												//this should always be true.
												towerMob.mLastTarget = mBoss;
											}
											mob.setTarget(mTarget);
										}

										locTest.add(0, mBoss.getHeight() / 2, 0);
										world.spawnParticle(Particle.SPELL_WITCH, locTest, 30, 0.25, 0.45, 0.25, 1);
										world.spawnParticle(Particle.SMOKE_LARGE, locTest, 12, 0, 0.45, 0, 0.125);
										world.playSound(locTest, Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.7f);


										// Janky way to break out of nested loop
										horizontalShift = 0;
										break;
									}
								}
							}

							if (!mHasTP) {
								mTarget = null;
							}

						}
					};

					runnable.runTaskLater(mPlugin, 10);
				}
			}

			@Override
			public boolean canRun() {
				return (mTarget == null || mTarget.isDead() || !mTarget.isValid()) && !mGame.isTurnEnded() && !mGame.isGameEnded();
			}

			@Override
			public int cooldownTicks() {
				return 10;
			}
		};

		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, (int) (FastUtils.RANDOM.nextDouble() * 40) + 10);
	}


	private boolean canTeleport(Location loc) {
		World world = loc.getWorld();
		// Move a bounding box to the target location
		BoundingBox box = mBoss.getBoundingBox();
		box.shift(loc.clone().subtract(mBoss.getLocation()));

		// Check the 8 corners of the bounding box and the location itself for blocks
		return isNotObstructed(loc, box)
			&& isNotObstructed(new Location(world, box.getMinX(), box.getMinY(), box.getMinZ()), box)
			&& isNotObstructed(new Location(world, box.getMinX(), box.getMinY(), box.getMaxZ()), box)
			&& isNotObstructed(new Location(world, box.getMinX(), box.getMaxY(), box.getMinZ()), box)
			&& isNotObstructed(new Location(world, box.getMinX(), box.getMaxY(), box.getMaxZ()), box)
			&& isNotObstructed(new Location(world, box.getMaxX(), box.getMinY(), box.getMinZ()), box)
			&& isNotObstructed(new Location(world, box.getMaxX(), box.getMinY(), box.getMaxZ()), box)
			&& isNotObstructed(new Location(world, box.getMaxX(), box.getMaxY(), box.getMinZ()), box)
			&& isNotObstructed(new Location(world, box.getMaxX(), box.getMaxY(), box.getMaxZ()), box);
	}

	private boolean isNotObstructed(Location loc, BoundingBox box) {
		Block block = loc.getBlock();
		return !block.getBoundingBox().overlaps(box) || block.isLiquid();
	}
}
