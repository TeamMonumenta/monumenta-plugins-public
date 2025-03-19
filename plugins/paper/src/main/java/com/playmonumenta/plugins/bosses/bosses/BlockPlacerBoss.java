package com.playmonumenta.plugins.bosses.bosses;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

/**
 * Places and destroys blocks to reach a target player.<br>
 * Warning: Do not apply SpellBlockBreak or any of its implementations to mobs using this spell!
 */
public final class BlockPlacerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blockplacer";
	public static final String PREVENT_BLOCK_PLACEMENT = "disable_boss_blockplacer";

	private static final double PREVENT_PLACING_RADIUS = 2.5;
	private static final int IDLE_TIME = 6 * TICKS_PER_SECOND;
	private static final int NEW_TARGET_RANGE = 25;
	private static final int BEELINE_DISTANCE = 8;
	/* Materials that have collision but should never be replaced when a launcher bridges. Left as a set in case
	 * Mojank does something silly in the future */
	private static final EnumSet<Material> CANNOT_REPLACE_MATERIALS = EnumSet.of(
		Material.END_PORTAL
	);
	private static final Material BRIDGE_MAT = Material.POLISHED_BLACKSTONE_BRICKS;

	private final Mob mMob = (Mob) mBoss;
	private Location mLastLocation = mMob.getLocation();
	private int mNoTargetTicks = 0;
	private int mLowMovementTicks = 0;

	public BlockPlacerBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);

		final SpellBlockBreak blockBreakToTarget = new SpellBlockBreak(mBoss, true, false, true);

		final List<Spell> spells = List.of(
			blockBreakToTarget,
			new Spell() {
				@Override
				public void run() {
					if (mBoss.hasMetadata(PREVENT_BLOCK_PLACEMENT)
						|| ZoneUtils.hasZoneProperty(mBoss, ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)) {
						return;
					}

					final LivingEntity target = mMob.getTarget();
					final List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), NEW_TARGET_RANGE, false);
					if (target instanceof final Player player && !players.isEmpty()) {
						if (!player.getWorld().equals(mMob.getWorld())) {
							mMob.setTarget(null);
							return;
						}
						mNoTargetTicks = 0;
						pathfindToTarget(player);
					} else {
						mNoTargetTicks += 5;
						if (mNoTargetTicks > IDLE_TIME && !players.isEmpty()) {
							mMob.setTarget(players.get(FastUtils.RANDOM.nextInt(players.size())));
						}
					}
				}

				@Override
				public int cooldownTicks() {
					return 1;
				}

				private void pathfindToTarget(final Player target) {
					MMLog.finest(() -> "[BlockPlacerBoss] Launcher " + mMob + " is attempting to pathfind to " + target);
					final Location targetLoc = target.getLocation();
					final Location bossLoc = mMob.getLocation();
					final Pathfinder pathfinder = mMob.getPathfinder();
					final Pathfinder.PathResult path = pathfinder.getCurrentPath();

					if (bossLoc.distance(mLastLocation) < 0.25) {
						mLowMovementTicks += 5;
					} else {
						mLowMovementTicks = 0;
					}

					mLastLocation = bossLoc;
					if (mLowMovementTicks >= TICKS_PER_SECOND) {
						/* Launcher likely can't navigate to the target's loc */
						blockBreakToTarget.tryToBreakBlocks(SpellBlockBreak.DEFAULT_REQUIRED_SCORE / 2);
					}
					if (mLowMovementTicks >= TICKS_PER_SECOND * 4) {
						/* Launcher likely boxed itself inside a cage and needs to be rescued */
						final List<Block> nearbyBridgeBlocks = BlockUtils.getBlocksInCube(bossLoc, 3);
						nearbyBridgeBlocks.removeIf(block -> block.getType() != BRIDGE_MAT);
						blockBreakToTarget.breakBlocks(bossLoc, nearbyBridgeBlocks);
						mLowMovementTicks = 0;
						return;
					}

					if (path == null) {
						if (!blockBreakToTarget.tryToBreakBlocks(SpellBlockBreak.DEFAULT_REQUIRED_SCORE)) {
							MMLog.finest(() -> "[BlockPlacerBoss] Launcher " + mMob.getName() + " is attempting to " +
								"bridge to " + mMob.getTarget() + " because its path is null and there are no " +
								"available blocks to break");
							bridge(pathfinder, bossLoc, targetLoc);
						}
					} else {
						final double distanceToTargetFlat = bossLoc.clone().subtract(targetLoc).toVector().setY(0).length();
						if (mBoss.hasLineOfSight(target) && distanceToTargetFlat < BEELINE_DISTANCE / 2.0
							    && targetLoc.getY() - bossLoc.getY() < BEELINE_DISTANCE / 2.0) {
							MMLog.finest(() -> "[BlockPlacerBoss] Launcher " + mMob.getName() + " is attempting to " +
								"bridge to " + mMob.getTarget() + " because its path is not null and the target is " +
								"within beeline distance");
							bridge(pathfinder, bossLoc, targetLoc);
						}

						final double distanceToTarget = bossLoc.distance(targetLoc);
						final Location nextPoint = path.getNextPoint();
						final Location finalPoint = path.getFinalPoint();
						if ((distanceToTarget > 2 && (finalPoint == null || finalPoint.distance(bossLoc) < 1))
							|| nextPoint == null
							|| (distanceToTarget < Objects.requireNonNull(path.getNextPoint()).distance(targetLoc)
							    && (distanceToTarget < BEELINE_DISTANCE
									|| path.getFinalPoint().distance(targetLoc) > BEELINE_DISTANCE))) {
							if (!blockBreakToTarget.tryToBreakBlocks(SpellBlockBreak.DEFAULT_REQUIRED_SCORE)) {
								MMLog.finest(() -> "[BlockPlacerBoss] Launcher " + mMob.getName() + " is attempting " +
									"to bridge to " + mMob.getTarget() + " because there are no available blocks to " +
									"break and the distance to target seems appropriate for bridging");
								bridge(pathfinder, bossLoc, targetLoc);
							}
						}
					}
				}

				private void bridge(final Pathfinder pathfinder, final Location bossLoc, final Location targetLoc) {
					if (!PlayerUtils.playersInRange(bossLoc, PREVENT_PLACING_RADIUS, true).isEmpty()) {
						MMLog.finest(() -> "[BlockPlacerBoss] Launcher " + mMob.getName() + " attempted to place " +
								"a block but was too close to one or more players");
						return;
					}

					final Vector euclideanVec = targetLoc.clone().subtract(bossLoc).toVector().normalize();
					final Location posXDir = bossLoc.clone().add(1, -1, 0);
					final Location negXDir = bossLoc.clone().add(-1, -1, 0);
					final Location posZDir = bossLoc.clone().add(0, -1, 1);
					final Location negZDir = bossLoc.clone().add(0, -1, -1);
					final Vector yVec = new Vector(0, 1, 0);
					/* Allow for the target to jump without the launcher placing blocks to go up to reduce block spam */
					final boolean targetIsElevated = bossLoc.getY() + 1.3 < targetLoc.getY();
					final Location resultDir;
					final List<Location> bridgeLocs = new ArrayList<>();

					if (Math.abs(euclideanVec.getX()) > Math.abs(euclideanVec.getZ())) {
						resultDir = euclideanVec.getX() > 0 ? posXDir : negXDir;
					} else {
						resultDir = euclideanVec.getZ() > 0 ? posZDir : negZDir;
					}
					bridgeLocs.add(resultDir);

					if (targetIsElevated) {
						resultDir.add(yVec);
						bridgeLocs.add(resultDir);
					}

					for (final Location loc : bridgeLocs) {
						final Block block = loc.getBlock();
						final Material material = block.getType();
						// If block is in cannot replace mats or is a solid or is water or the location is in adventure
						// mode or the location intersects an entity's box, don't replace that block
						if (CANNOT_REPLACE_MATERIALS.contains(material) || block.isSolid() || BlockUtils.containsWater(block)
							|| ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.ADVENTURE_MODE)
							|| LocationUtils.blocksIntersectEntity(loc.getWorld(), List.of(loc),
								hitbox -> hitbox.getHitEntities(e -> e instanceof LivingEntity && !e.isInvulnerable()))) {
							MMLog.finest(() -> "[BlockPlacerBoss] Launcher " + mMob.getName() + " attempted to place " +
								"a block but failed the final placement check");
							continue;
						}

						block.setType(BRIDGE_MAT);
						loc.getWorld().playSound(loc, Sound.BLOCK_NETHER_BRICKS_PLACE, SoundCategory.BLOCKS, 1f,
							FastUtils.randomFloatInRange(0.5f, 0.7f));
						pathfinder.moveTo(loc.clone().add(0, 1, 0));
						Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () ->
							pathfinder.moveTo(loc.clone().add(0, 1, 0)), 5);
					}
				}
			}
		);

		super.constructBoss(SpellManager.EMPTY, spells, NEW_TARGET_RANGE * 2, null);
	}
}
