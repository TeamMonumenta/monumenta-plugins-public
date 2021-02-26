package com.playmonumenta.plugins.bosses.spells;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.playmonumenta.plugins.utils.ItemUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.loot.Lootable;

public class SpellBlockBreak extends Spell {

	private Entity mLauncher;
	private List<Material> mNoBreak;

	private int mXRad;
	private int mYRad;
	private int mZRad;

	public SpellBlockBreak(Entity launcher) {
		this(launcher, 1, 3, 1);
	}

	public SpellBlockBreak(Entity launcher, Material... noBreak) {
		this(launcher, 1, 3, 1, noBreak);
	}

	public SpellBlockBreak(Entity launcher, int xRad, int yRad, int zRad) {
		this(launcher, xRad, yRad, zRad, Material.AIR);
	}

	public SpellBlockBreak(Entity launcher, int xRad, int yRad, int zRad, Material... noBreak) {
		mLauncher = launcher;
		mXRad = xRad;
		mYRad = yRad;
		mZRad = zRad;
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

					if (material.equals(Material.COBWEB) || block.getBlockData() instanceof TrapDoor) {
						/* Break cobwebs immediately, don't add them to the bad block list */
						EntityExplodeEvent event = new EntityExplodeEvent(mLauncher, mLauncher.getLocation(), Arrays.asList(block), 0f);
						Bukkit.getServer().getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							/* Only allow bosses to break blocks in areas where explosions are allowed */
							testloc.getBlock().setType(Material.AIR);
							loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 0.9f);
							loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 6, 1, 1, 1, 0.03);
						}
					} else if (y > 0 &&
					           (!mIgnoredMats.contains(material)) && !mNoBreak.contains(material) &&
					           (material.isSolid() || ItemUtils.carpet.contains(material)) &&
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
