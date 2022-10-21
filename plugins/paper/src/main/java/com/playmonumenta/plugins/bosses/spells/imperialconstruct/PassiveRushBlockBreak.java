package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.spells.Spell;
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
import org.bukkit.loot.Lootable;

public class PassiveRushBlockBreak extends Spell {

	public boolean mIsActive = false;

	private Entity mLauncher;
	private List<Material> mNoBreak;

	private int mXRad;
	private int mYRad;
	private int mZRad;

	//When true, mob breaks blocks at foot level
	private boolean mFootLevelBreak;

	public PassiveRushBlockBreak(Entity launcher) {
		this(launcher, 1, 3, 1);
	}

	public PassiveRushBlockBreak(Entity launcher, Material... noBreak) {
		this(launcher, 1, 3, 1, false, noBreak);
	}

	public PassiveRushBlockBreak(Entity launcher, int xRad, int yRad, int zRad) {
		this(launcher, xRad, yRad, zRad, false, Material.AIR);
	}

	public PassiveRushBlockBreak(Entity launcher, int xRad, int yRad, int zRad, boolean footLevelBreak, Material... noBreak) {
		mLauncher = launcher;
		mXRad = xRad;
		mYRad = yRad;
		mZRad = zRad;
		mFootLevelBreak = footLevelBreak;
		mNoBreak = Arrays.asList(noBreak);
	}

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER
	);

	@Override
	public void run() {
		if (!mIsActive) {
			return;
		}

		Location loc = mLauncher.getLocation();
		/* Get a list of all blocks that impede the boss's movement */
		List<Block> badBlockList = new ArrayList<Block>();
		Location testloc = new Location(loc.getWorld(), 0, 0, 0);
		for (int x = -mXRad; x <= mXRad; x++) {
			testloc.setX(loc.getX() + x);
			for (int y = 0; y <= mYRad; y++) {
				testloc.setY(loc.getY() + y);
				for (int z = -mZRad; z <= mZRad; z++) {
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

			/* Remove any remaining blocks, which might have been modified by the event */
			for (Block block : badBlockList) {
				block.setType(Material.AIR);
			}
			if (badBlockList.size() > 0) {
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
