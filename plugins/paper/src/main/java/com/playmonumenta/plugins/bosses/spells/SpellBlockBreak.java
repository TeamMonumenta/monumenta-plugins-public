package com.playmonumenta.plugins.bosses.spells;

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
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.loot.Lootable;

public class SpellBlockBreak extends Spell {
	private Entity mLauncher;
	private List<Material> mNoBreak;

	public SpellBlockBreak(Entity launcher) {
		mLauncher = launcher;
		mNoBreak = new ArrayList<Material>();
	}

	public SpellBlockBreak(Entity launcher, Material... noBreak) {
		mLauncher = launcher;
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
		for (int x = -1; x <= 1; x++) {
			testloc.setX(loc.getX() + x);
			for (int y = 0; y <= 3; y++) {
				testloc.setY(loc.getY() + y);
				for (int z = -1; z <= 1; z++) {
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
					           material.isSolid() &&
					           (!(block.getState() instanceof Lootable) || !((Lootable)block.getState()).hasLootTable())) {
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
	public int duration() {
		return 1;
	}
}
