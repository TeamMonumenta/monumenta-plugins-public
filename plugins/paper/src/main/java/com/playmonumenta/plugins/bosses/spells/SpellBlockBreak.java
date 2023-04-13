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
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;

public class SpellBlockBreak extends Spell {
	private Entity mLauncher;
	private List<Material> mNoBreak;

	private int mXRad;
	private int mYRad;
	private int mZRad;

	//When true, mob breaks blocks at foot level
	private boolean mFootLevelBreak;

	//When true, use launcher bounding box as radius
	private boolean mAdaptToBoundingBox = false;

	public SpellBlockBreak(Entity launcher, boolean adaptboundingbox) {
		this(launcher);
		mAdaptToBoundingBox = adaptboundingbox;
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
		Location loc = mLauncher.getLocation();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)) {
			return;
		}
		/* Get a list of all blocks that impede the boss's movement */
		List<Block> breakBlockList = new ArrayList<>();
		int badScore = 0;
		Location testloc = new Location(loc.getWorld(), 0, 0, 0);
		int xRad = (int) (mAdaptToBoundingBox ? Math.round(mLauncher.getBoundingBox().getWidthX()) : mXRad);
		int yRad = (int) (mAdaptToBoundingBox ? Math.ceil(mLauncher.getBoundingBox().getHeight()) : mYRad);
		int zRad = (int) (mAdaptToBoundingBox ? Math.round(mLauncher.getBoundingBox().getWidthZ()) : mZRad);
		for (int x = -xRad; x <= xRad; x++) {
			testloc.setX(loc.getX() + x);
			for (int y = 0; y <= yRad; y++) {
				testloc.setY(loc.getY() + y);
				for (int z = -zRad; z <= zRad; z++) {
					testloc.setZ(loc.getZ() + z);
					Block block = testloc.getBlock();
					Material material = block.getType();

					BlockData blockData = block.getBlockData();
					if (material == Material.COBWEB || material == Material.HONEY_BLOCK || material == Material.POWDER_SNOW
						    || blockData instanceof TrapDoor || blockData instanceof Fence || blockData instanceof Gate || blockData instanceof Wall || blockData instanceof Rail) {
						/* Break cobwebs immediately, don't add them to the bad block list */
						List<Block> list = new ArrayList<>(1);
						list.add(block);
						EntityExplodeEvent event = new EntityExplodeEvent(mLauncher, loc, list, 0f);
						Bukkit.getServer().getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							/* Only allow bosses to break blocks in areas where explosions are allowed */
							for (Block b : event.blockList()) {
								b.setType(Material.AIR);
								b.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.9f);
								new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 6, 1, 1, 1, 0.03).spawnAsEntityActive(mLauncher);
							}
						}
					} else if (!BlockUtils.isMechanicalBlock(material) && !mNoBreak.contains(material)
						&& (material.isSolid() || ItemUtils.CARPETS.contains(material) || ItemUtils.CANDLES.contains(material) || material.equals(Material.PLAYER_HEAD) || material.equals(Material.PLAYER_WALL_HEAD) || ItemUtils.FLOWER_POTS.contains(material))
						&& (!(block.getState() instanceof Lootable)
						|| (!((Lootable) block.getState()).hasLootTable()
						&& !block.getLocation().subtract(0, 1, 0).getBlock().getType().equals(Material.BEDROCK)))) {
						// Threshold for BadScore is 6 (A "fully bad block" is 2)
						// Example:
						// 3 Bad blocks Top level (3 * 2) = 6 (Break)
						// 4 Foot Level blocks (4 * 1) + 1 Top level block (1 * 2) = 6 (Break)
						// 3 Foot Level blocks (2 * 1) + 1 Top level block (1 * 2) = 5 (Don't Break)
						if (y == 0 && !mFootLevelBreak) {
							// Even if we don't break foot level blocks, count them towards the threshold as half a block.
							badScore += 1;
						} else {
							// If we plan to break this block, add to list and count them towards the threshold as full block.
							breakBlockList.add(block);
							badScore += 2;
						}
					}
				}
			}
		}

		if (breakBlockList.isEmpty()) {
			return;
		}

		int requiredScore = 6;
		if (mLauncher instanceof Mob mob && mob.getTarget() instanceof Player target && target.getLocation().getY() >= loc.getY() + 2 && PlayerUtils.isOnGround(target)) {
			// If changing the score makes a difference, the player the mob is targeting is probably trying to pillar
			requiredScore = 3;
		}

		/* If more than two blocks surrounding mob, and some are breakable, destroy all breakable blocking blocks */
		if (badScore >= requiredScore) {
			/* Call an event with these exploding blocks to give plugins a chance to modify it */
			EntityExplodeEvent event = new EntityExplodeEvent(mLauncher, mLauncher.getLocation(), breakBlockList, 0f);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled() || breakBlockList.isEmpty()) {
				return;
			}

			/* Remove any remaining blocks, which might have been modified by the event */
			for (Block block : breakBlockList) {
				if (BlockUtils.isValuableBlock(block.getType())) {
					block.breakNaturally(new ItemStack(Material.IRON_PICKAXE));
				} else {
					block.setType(Material.AIR);
				}
			}

			loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.9f);
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 6, 1, 1, 1, 0.03).spawnAsEntityActive(mLauncher);
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
