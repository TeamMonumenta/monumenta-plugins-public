package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;

public class SpellBlockBreak extends Spell {
	public static final int DEFAULT_REQUIRED_SCORE = 6;
	public boolean mIsActive = true;

	private static final int HEIGHT_BELOW_WORLD = 100;
	private final Entity mLauncher;
	private final List<Material> mNoBreak;
	private final int mXRad;
	private final int mYRad;
	private final int mZRad;
	private final int mYOffset;
	private final int mArenaFloorY;
	private final boolean mBreakBossArena;
	private final boolean mFlattenArena;
	private final boolean mBreakOverheadBlocks;
	private final boolean mBreakFootLevel;
	private final boolean mAdaptToBoundingBox;
	private final boolean mOnlyForcecast;

	public SpellBlockBreak(final Entity launcher) {
		this(launcher, 1, 3, 1);
	}

	public SpellBlockBreak(final Entity launcher, final int xRad, final int yRad, final int zRad) {
		this(launcher, xRad, yRad, zRad, launcher.getWorld().getMinHeight() - HEIGHT_BELOW_WORLD,
			false, true, false);
	}

	public SpellBlockBreak(final Entity launcher, final boolean adaptToBoundingBox, final boolean breakFootLevel) {
		this(launcher, 1, 3, 1, launcher.getWorld().getMinHeight() - HEIGHT_BELOW_WORLD,
			adaptToBoundingBox, true, breakFootLevel);
	}

	public SpellBlockBreak(final Entity launcher, final int xRad, final int yRad, final int zRad, final int arenaFloorY,
	                       final boolean adaptToBoundingBox, final boolean breakBossArena, final boolean breakFootLevel) {
		this(launcher, xRad, yRad, zRad, 0, arenaFloorY, adaptToBoundingBox, breakBossArena, breakFootLevel);
	}

	public SpellBlockBreak(final Entity launcher, final int xRad, final int yRad, final int zRad, final int yOffset, final int arenaFloorY,
	                       final boolean adaptToBoundingBox, final boolean breakBossArena, final boolean breakFootLevel) {
		this(launcher, xRad, yRad, zRad, yOffset, arenaFloorY, adaptToBoundingBox, breakBossArena, false,
			breakFootLevel, true, false, Material.AIR);
	}


	public SpellBlockBreak(final Entity launcher, final boolean adaptToBoundingBox, final boolean breakFootLevel,
	                       final boolean onlyForcecast) {
		this(launcher, 1, 3, 1, 0, launcher.getWorld().getMinHeight() - HEIGHT_BELOW_WORLD,
			adaptToBoundingBox, true, false, breakFootLevel, true,
			onlyForcecast, Material.AIR);
	}

	/**
	 * Spell for all bosses that break blocks in the world.
	 *
	 * @param launcher            The entity using the spell
	 * @param xRad                X radius to check for blocks
	 * @param yRad                Y radius to check for blocks
	 * @param zRad                Z radius to check for blocks
	 * @param yOffset             Vertical offset for block break volume
	 * @param arenaFloorY         For bosses with a dedicated arena. Only used if breakBossArena is set to false
	 * @param adaptToBoundingBox  Whether the launcher should check for blocks to break depending on the radii or its bounding box
	 * @param breakBossArena      If the launcher should be able to break the arena floor
	 * @param flattenArena        If the launcher should consistently break blocks above {@code arenaFloorY}
	 * @param breakFootLevel      If the launcher should break blocks at "foot" level. If set to true, the launcher won't attempt to "stair" from low to high elevation
	 * @param breakOverheadBlocks Only used with Eldrask for now. Defaults to true
	 * @param onlyForcecast       Whether to run this spell manually. Used during the launcher's pathfinding calculations. Defaults to false
	 * @param noBreak             List of block materials the launcher should not break
	 */
	public SpellBlockBreak(final Entity launcher, final int xRad, final int yRad, final int zRad, final int yOffset, final int arenaFloorY,
	                       final boolean adaptToBoundingBox, final boolean breakBossArena, final boolean flattenArena,
	                       final boolean breakFootLevel, final boolean breakOverheadBlocks, final boolean onlyForcecast,
	                       final Material... noBreak) {
		mLauncher = launcher;
		mAdaptToBoundingBox = adaptToBoundingBox;
		mXRad = xRad;
		mYRad = yRad;
		mZRad = zRad;
		mYOffset = yOffset;
		mArenaFloorY = arenaFloorY;
		mBreakBossArena = breakBossArena;
		mFlattenArena = flattenArena;
		mBreakFootLevel = breakFootLevel;
		mBreakOverheadBlocks = breakOverheadBlocks;
		mOnlyForcecast = onlyForcecast;
		mNoBreak = Arrays.asList(noBreak);
	}

	@Override
	public void run() {
		if (!canRun()) {
			// The canRun() method doesn't normally do anything for passives but might as well just use it here if bosses want to add some check
			return;
		}
		tryToBreakBlocks(DEFAULT_REQUIRED_SCORE);
	}

	/**
	 * Attempt to break blocks in the launcher's world. Uses location of the launcher
	 *
	 * @param initialRequiredScore Threshold score that must be met before block breaking happens. Defaults to 6
	 * @return True if blocks were broken
	 */
	public boolean tryToBreakBlocks(final int initialRequiredScore) {
		return tryToBreakBlocks(mLauncher.getLocation(), initialRequiredScore);
	}

	/**
	 * Attempt to break blocks in the launcher's world.<br>
	 * Warning: Do not move this code outside of this method! Attempting to put this in run() causes race conditions!
	 *
	 * @param loc                  Location where block breaking should happen. I had to add this param to this dang spell to get it to work with the LaserBoss code
	 * @param initialRequiredScore Threshold score that must be met before block breaking happens. Defaults to 6
	 * @return True if blocks were broken
	 */
	public boolean tryToBreakBlocks(final Location loc, final int initialRequiredScore) {
		if (!mIsActive || ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)) {
			return false;
		}

		/* Calculate radii every time the spell is run because of size changing slimes with block break */
		final int xRad = (int) (mAdaptToBoundingBox ? Math.round(mLauncher.getBoundingBox().getWidthX()) : mXRad);
		final int yRad = (int) (mAdaptToBoundingBox ? Math.ceil(mLauncher.getBoundingBox().getHeight()) : mYRad);
		final int zRad = (int) (mAdaptToBoundingBox ? Math.round(mLauncher.getBoundingBox().getWidthZ()) : mZRad);
		final double testLocX = loc.getX();
		double testLocY = loc.getY() - 1.0 + mYOffset;
		final double testLocZ = loc.getZ();
		final Location testLoc = new Location(loc.getWorld(), 0, 0, 0);
		Block testBlock;
		Material testMat;

		int badScore = 0;
		int requiredScore = initialRequiredScore;
		final List<Block> breakBlockList = new ArrayList<>();

		/* Allow for roughly half of the launcher's hitbox perimeter to be surrounded by foot-level blocks before
		 * attempting to break them unless the launcher should bulldoze the arena flat (e.g. Eldrask) */
		if (mBreakFootLevel && !mFlattenArena) {
			requiredScore = 2 * (xRad + yRad + 2);
		}

		// If the launcher is a mob with a valid player target with a GEQ 2 block height difference, reduce threshold
		if (mLauncher instanceof final Mob mob && mob.getTarget() instanceof final Player target
			&& target.getLocation().getY() >= loc.getY() + 2 && PlayerUtils.isOnGround(target)) {
			requiredScore /= 2;
		}

		/* Special case for Slime Blocks, which are full blocks that aren't caught by subsequent checks */
		for (double x = testLocX - xRad; x <= testLocX + xRad; x++) {
			for (double z = testLocZ - zRad; z <= testLocZ + zRad; z++) {
				testLoc.set(x, testLocY, z);
				testBlock = testLoc.getBlock();
				testMat = testBlock.getType();

				if (testMat == Material.SLIME_BLOCK) {
					requiredScore = 0;
					breakBlockList.add(testBlock);
				}
			}
		}

		/* Get a list of all blocks that impede the launcher's movement */
		if (!mFlattenArena) {
			testLocY++;
		}
		for (double x = testLocX - xRad; x <= testLocX + xRad; x++) {
			for (double y = testLocY; y <= testLocY + yRad; y++) {
				for (double z = testLocZ - zRad; z <= testLocZ + zRad; z++) {
					testLoc.set(x, y, z);
					testBlock = testLoc.getBlock();
					testMat = testBlock.getType();
					/* Mob pathfinding frequently doesn't navigate over or around open trapdoors */
					final boolean evilTrapdoor = testBlock.getBlockData() instanceof final TrapDoor trapdoor
						&& trapdoor.isOpen();

					if (BlockUtils.isEnvHazardForMobs(testMat) || BlockUtils.mobCannotPathfindOver(testBlock.getBlockData()) || evilTrapdoor) {
						requiredScore = 0;
						breakBlockList.add(testBlock);
						/* If the block is not a mech block and is not in mNoBreak
						 * and the block has collision with entities
						 * and (the block is not a Lootable or (is a Lootable without a loot table and the block below it is not bedrock)) */
					} else if (!BlockUtils.isMechanicalBlock(testMat) && !mNoBreak.contains(testMat)
						&& blockMaterialHasCollision(testMat)
						&& (!(testBlock.getState() instanceof Lootable) || (!((Lootable) testBlock.getState()).hasLootTable()
						&& !testBlock.getLocation().subtract(0, 1, 0).getBlock().getType().equals(Material.BEDROCK)))) {
						// Threshold for badScore is usually 6 (A "fully bad block" is 2)
						// Example:
						// 3 Bad blocks Top level (3 * 2) = 6 (Break)
						// 4 Foot Level blocks (4 * 1) + 1 Top level block (1 * 2) = 6 (Break)
						// 3 Foot Level blocks (2 * 1) + 1 Top level block (1 * 2) = 5 (Don't Break)
						if (y > testLocY || (mBreakFootLevel && shouldBreakSlab(testBlock)) || (mFlattenArena && y > mArenaFloorY)) {
							breakBlockList.add(testBlock);
							badScore += 2;
						} else {
							badScore += 1;
						}
					}
				}
			}
		}

		/* If the threshold has been met, attempt to break blocks */
		if (badScore >= requiredScore) {
			final int finalBadScore = badScore;
			MMLog.finest(() -> "[SpellBlockBreak] Launcher " + mLauncher.getName() + " has achieved " + finalBadScore +
				" badScore and is attempting to break blocks");
			return breakBlocks(loc, breakBlockList);
		}
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

	@Override
	public boolean onlyForceCasted() {
		return mOnlyForcecast;
	}

	/**
	 * Helper method to handle destruction of blocks in world
	 *
	 * @param blockList List of blocks to break
	 * @return True if blocks were destroyed
	 */
	public boolean breakBlocks(final Location loc, final List<Block> blockList) {
		/* Call an event with these exploding blocks to give plugins a chance to modify it */
		final EntityExplodeEvent event = new EntityExplodeEvent(mLauncher, loc, blockList, 0f);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled() || blockList.isEmpty()) {
			MMLog.finest(() -> "[SpellBlockBreak] Launcher " + mLauncher.getName() +
				"'s EntityExplodeEvent was cancelled or the blockList is empty. Returning false");
			return false;
		}

		/* Remove any remaining blocks which might have been modified by the event */
		final Material particleMat = blockList.get(0).getType();
		boolean atLeastOneBlockChanged = false;
		for (final Block block : blockList) {
			/* Special case for big boy Eldrask who can crunch the blocks over his head when doing Giant Stomp.
			 * If (don't break overhead blocks) and (difference between current block and arena floor >= 15), skip */
			if ((!mBreakOverheadBlocks && block.getLocation().getY() - mArenaFloorY >= mYRad)
				/* If (don't break boss arena) and (current block's Y coord is less than arena floor's Y), skip */
				|| (!mBreakBossArena && block.getLocation().getY() < mArenaFloorY)) {
				continue;
			}

			if (BlockUtils.isValuableBlock(block.getType()) || BlockUtils.isNonEmptyContainer(block)) {
				atLeastOneBlockChanged = true;
				block.breakNaturally(new ItemStack(Material.IRON_PICKAXE));
			} else {
				atLeastOneBlockChanged = true;
				block.setType(Material.AIR);
			}
		}

		if (atLeastOneBlockChanged) {
			loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS, 0.25f, FastUtils.randomFloatInRange(0.7f, 1.0f));
			new PartialParticle(Particle.BLOCK_CRACK, loc, 15, mXRad / 2.0, mYRad / 4.0, mZRad / 2.0,
				0.05, particleMat.createBlockData()).spawnAsEntityActive(mLauncher);
			MMLog.finest(() -> "[SpellBlockBreak] Launcher " + mLauncher.getName() + " successfully broke blocks. Returning true");
			return true;
		}

		return false;
	}

	/**
	 * Helper method to check for block material collision with entities
	 *
	 * @param material Material of the block to be tested
	 * @return True if an entity's hitbox can collide with the block material
	 */
	private boolean blockMaterialHasCollision(final Material material) {
		return (material.isSolid()
			|| ItemUtils.HEADS.contains(material)
			|| ItemUtils.CARPETS.contains(material)
			|| ItemUtils.CANDLES.contains(material)
			|| ItemUtils.FLOWER_POTS.contains(material));
	}

	private boolean shouldBreakSlab(final Block block) {
		if (block instanceof final Slab s) {
			// if block is slab, don't break if it's a bottom slab
			return s.getType() != Slab.Type.BOTTOM;
		}
		// if block is not a slab, don't break
		return true;
	}
}
