package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;

public class SpellBlockBreak extends Spell {
	private final Entity mLauncher;
	private final List<Material> mNoBreak;
	private final int mXRad;
	private final int mYRad;
	private final int mZRad;

	//When true, mob breaks blocks at foot level
	private boolean mFootLevelBreak;

	//When true, use launcher bounding box as radius
	private boolean mAdaptToBoundingBox = false;

	public SpellBlockBreak(Entity launcher, boolean adaptboundingbox) {
		this(launcher, adaptboundingbox, false);
	}

	public SpellBlockBreak(Entity launcher, boolean adaptboundingbox, boolean footLevelBreak) {
		this(launcher);
		mAdaptToBoundingBox = adaptboundingbox;
		mFootLevelBreak = footLevelBreak;
	}

	public SpellBlockBreak(Entity launcher) {
		this(launcher, 1, 3, 1);
	}

	public SpellBlockBreak(Entity launcher, Material... noBreak) {
		this(launcher, 1, 3, 1, false, noBreak);
	}

	public SpellBlockBreak(Entity launcher, int xRad, int yRad, int zRad) {
		this(launcher, xRad, yRad, zRad, false, Material.AIR);
	}

	public SpellBlockBreak(Entity launcher, int xRad, int yRad, int zRad, boolean footLevelBreak, Material... noBreak) {
		mLauncher = launcher;
		mXRad = xRad;
		mYRad = yRad;
		mZRad = zRad;
		mFootLevelBreak = footLevelBreak;
		mNoBreak = Arrays.asList(noBreak);
	}

	@Override
	public void run() {
		final Location loc = mLauncher.getLocation();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)) {
			return;
		}

		List<Block> breakBlockList = new ArrayList<>();
		int badScore = 0;
		int requiredScore = 6;
		Location testloc = new Location(loc.getWorld(), 0, 0, 0);
		int xRad = (int) (mAdaptToBoundingBox ? Math.round(mLauncher.getBoundingBox().getWidthX()) : mXRad);
		int yRad = (int) (mAdaptToBoundingBox ? Math.ceil(mLauncher.getBoundingBox().getHeight()) : mYRad);
		int zRad = (int) (mAdaptToBoundingBox ? Math.round(mLauncher.getBoundingBox().getWidthZ()) : mZRad);

		/* Allow for roughly half of the launcher's hitbox perimeter to be surrounded by foot-level blocks before
		 * attempting to break them */
		if (mFootLevelBreak) {
			requiredScore = 2 * (xRad + yRad + 2);
		}

		if (mLauncher instanceof Mob mob && mob.getTarget() instanceof Player target && target.getLocation().getY() >= loc.getY() + 2 && PlayerUtils.isOnGround(target)) {
			// The player the mob is targeting is probably trying to pillar
			requiredScore = 3;
		}

		/* Special case for Slime Blocks, which are full blocks that aren't caught by subsequent checks */
		testloc.setY(loc.getY() - 1);
		for (int x = -xRad; x <= xRad; x++) {
			testloc.setX(loc.getX() + x);
			for (int z = -zRad; z <= zRad; z++) {
				testloc.setZ(loc.getZ() + z);
				Block block = testloc.getBlock();
				Material material = block.getType();

				if (material == Material.SLIME_BLOCK) {
					/* Break immediately, don't add them to the bad block list */
					List<Block> list = new ArrayList<>(1);
					list.add(block);
					breakBlocks(list);
				}
			}
		}

		/* Get a list of all blocks that impede the launcher's movement */
		for (int x = -xRad; x <= xRad; x++) {
			testloc.setX(loc.getX() + x);
			for (int y = 0; y <= yRad; y++) {
				testloc.setY(loc.getY() + y);
				for (int z = -zRad; z <= zRad; z++) {
					testloc.setZ(loc.getZ() + z);
					Block block = testloc.getBlock();
					Material material = block.getType();

					if (BlockUtils.isEnvHazardForMobs(material)) {
						/* Break immediately, don't add them to the bad block list */
						List<Block> list = new ArrayList<>(1);
						list.add(block);
						breakBlocks(list);
					/* If the block is not a mech block and is not in mNoBreak
					 * and (the block has collision with entities)
					 * and (the block is not a Lootable or (is a Lootable without a loot table and the block below it is not bedrock)) */
					} else if (!BlockUtils.isMechanicalBlock(material) && !mNoBreak.contains(material)
						&& (material.isSolid() || ItemUtils.HEADS.contains(material) || ItemUtils.CARPETS.contains(material) || ItemUtils.CANDLES.contains(material) || ItemUtils.FLOWER_POTS.contains(material))
						&& (!(block.getState() instanceof Lootable) || (!((Lootable) block.getState()).hasLootTable()
						&& !block.getLocation().subtract(0, 1, 0).getBlock().getType().equals(Material.BEDROCK)))) {
						// Threshold for BadScore is usually 6 (A "fully bad block" is 2)
						// Example:
						// 3 Bad blocks Top level (3 * 2) = 6 (Break)
						// 4 Foot Level blocks (4 * 1) + 1 Top level block (1 * 2) = 6 (Break)
						// 3 Foot Level blocks (2 * 1) + 1 Top level block (1 * 2) = 5 (Don't Break)
						if (y != 0 || (mFootLevelBreak && shouldBreakSlab(block))) {
							// If we plan to break this block, add to list and count them towards the threshold as full block.
							breakBlockList.add(block);
							badScore += 2;
						} else {
							badScore += 1;
						}
					}
				}
			}
		}

		/* If more than two blocks surrounding mob, and some are breakable, destroy all breakable blocking blocks */
		if (badScore >= requiredScore) {
			breakBlocks(breakBlockList);
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

	/**
	 * Helper method to handle destruction of blocks in world
	 * @param blockList List of blocks to break
	 */
	private void breakBlocks(List<Block> blockList) {
		final Location launcherLocation = mLauncher.getLocation();

		/* Call an event with these exploding blocks to give plugins a chance to modify it */
		EntityExplodeEvent event = new EntityExplodeEvent(mLauncher, launcherLocation, blockList, 0f);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled() || blockList.isEmpty()) {
			return;
		}

		/* Remove any remaining blocks which might have been modified by the event */
		for (Block block : blockList) {
			if (BlockUtils.isValuableBlock(block.getType()) || BlockUtils.isNonEmptyContainer(block)) {
				block.breakNaturally(new ItemStack(Material.IRON_PICKAXE));
			} else {
				block.setType(Material.AIR);
			}
		}

		launcherLocation.getWorld().playSound(launcherLocation, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS, 0.3f, 0.9f);
		new PartialParticle(Particle.EXPLOSION_NORMAL, launcherLocation, 6, 1, 1, 1, 0.03).spawnAsEntityActive(mLauncher);
	}

	private boolean shouldBreakSlab(Block block) {
		if (block instanceof Slab s) {
			// if block is slab, don't break if it's a bottom slab
			return s.getType() != Slab.Type.BOTTOM;
		}
		// if block is not a slab, don't break
		return true;
	}
}
