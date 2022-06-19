package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Entity;
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
		mLauncher = launcher;
		mAdaptToBoundingBox = adaptboundingbox;
		mFootLevelBreak = false;
		mNoBreak = Arrays.asList(Material.AIR);
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

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.CAVE_AIR,
		Material.VOID_AIR,
		Material.STRUCTURE_VOID,
		Material.STRUCTURE_BLOCK,
		Material.JIGSAW,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER
	);

	@Override
	public void run() {
		Location loc = mLauncher.getLocation();
		/* Get a list of all blocks that impede the boss's movement */
		List<Block> badBlockList = new ArrayList<Block>();
		Location testloc = new Location(loc.getWorld(), 0, 0, 0);
		int xRad = (int) (mAdaptToBoundingBox ? mLauncher.getBoundingBox().getWidthX() : mXRad);
		int yRad = (int) (mAdaptToBoundingBox ? mLauncher.getBoundingBox().getHeight() + 1 : mYRad);
		int zRad = (int) (mAdaptToBoundingBox ? mLauncher.getBoundingBox().getWidthZ() : mZRad);
		for (int x = -xRad; x <= xRad; x++) {
			testloc.setX(loc.getX() + x);
			for (int y = 0; y <= yRad; y++) {
				testloc.setY(loc.getY() + y);
				for (int z = -zRad; z <= zRad; z++) {
					testloc.setZ(loc.getZ() + z);
					Block block = testloc.getBlock();
					Material material = block.getType();

					if (material.equals(Material.COBWEB) || material.equals(Material.HONEY_BLOCK) || block.getBlockData() instanceof TrapDoor || block.getBlockData() instanceof Fence || block.getBlockData() instanceof Gate || block.getBlockData() instanceof Wall || block.getBlockData() instanceof Rail) {
						/* Break cobwebs immediately, don't add them to the bad block list */
						List<Block> list = new ArrayList<>(1);
						list.add(block);
						EntityExplodeEvent event = new EntityExplodeEvent(mLauncher, mLauncher.getLocation(), list, 0f);
						Bukkit.getServer().getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							/* Only allow bosses to break blocks in areas where explosions are allowed */
							for (Block b : event.blockList()) {
								b.setType(Material.AIR);
								b.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 0.9f);
								b.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 6, 1, 1, 1, 0.03);
							}
						}
					} else if ((y > 0 || (mFootLevelBreak && y >= 0)) &&
					           !mIgnoredMats.contains(material) && !mNoBreak.contains(material) &&
					           (material.isSolid() || ItemUtils.carpet.contains(material) || material.equals(Material.PLAYER_HEAD) || material.equals(Material.PLAYER_WALL_HEAD)) &&
					           (!(block.getState() instanceof Lootable)
								|| (!((Lootable)block.getState()).hasLootTable()
								    && !block.getLocation().subtract(0, 1, 0).getBlock().getType().equals(Material.BEDROCK)))) {
						badBlockList.add(block);
					}
				}
			}
		}

		/* If more than two blocks, destroy all blocking blocks */
		if (badBlockList.size() > 2) {
			/* Call an event with these exploding blocks to give plugins a chance to modify it */
			EntityExplodeEvent event = new EntityExplodeEvent(mLauncher, mLauncher.getLocation(), badBlockList, 0f);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}

			if (badBlockList.size() > 0) {
				/* Remove any remaining blocks, which might have been modified by the event */
				for (Block block : badBlockList) {
					switch (block.getType()) {
						case SHULKER_BOX, BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, CYAN_SHULKER_BOX,
							     GREEN_SHULKER_BOX, LIME_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX,
							     MAGENTA_SHULKER_BOX, ORANGE_SHULKER_BOX, PINK_SHULKER_BOX, PURPLE_SHULKER_BOX,
							     RED_SHULKER_BOX, WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX, GRAY_SHULKER_BOX,
							     CHEST, TRAPPED_CHEST,
							     IRON_ORE, IRON_BLOCK, GOLD_ORE, GOLD_BLOCK, NETHER_GOLD_ORE, GILDED_BLACKSTONE, LAPIS_ORE, LAPIS_BLOCK,
							     ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> block.breakNaturally(new ItemStack(Material.IRON_PICKAXE));
						default -> block.setType(Material.AIR);
					}
				}

				loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 0.9f);
				loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 6, 1, 1, 1, 0.03);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
