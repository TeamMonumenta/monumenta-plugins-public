package com.playmonumenta.plugins.bosses.spells.kaul;

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
import org.bukkit.block.Container;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.playmonumenta.plugins.bosses.spells.Spell;

public class SpellKaulBlockBreak extends Spell {
	private final LivingEntity mBoss;
	private double mArenaY;
	private int mINC = 0;

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
			Material.AIR,
			Material.COMMAND_BLOCK,
			Material.CHAIN_COMMAND_BLOCK,
			Material.REPEATING_COMMAND_BLOCK,
			Material.BEDROCK,
			Material.BARRIER,
			Material.SPAWNER,
			Material.WATER
		);

	public SpellKaulBlockBreak(LivingEntity launcher, double y) {
		mBoss = launcher;
		mArenaY = y;
	}

	@Override
	public void run() {
		LivingEntity target = null;
		Location loc = mBoss.getLocation();
		if (mBoss instanceof Creature) {
			Creature c = (Creature) mBoss;
			target = c.getTarget();
		}
		if (target == null) {
			this.cancel();
			return;
		}

		Location l = mBoss.getLocation();

		/* Get a list of all blocks that impede the boss's movement */
		List<Block> badBlockList = new ArrayList<Block>();
		Location testloc = new Location(l.getWorld(), 0, 0, 0);

		if (target.getLocation().getY() > mArenaY + 0.7) {
			mINC++;
			if (target.getLocation().getY() > mArenaY + 0.7 && mBoss.getLocation().distance(target.getLocation()) < 5 &&
			    mINC > 4) {
				l = target.getLocation();
				l.setY(target.getLocation().getY() - 3);
				mINC = 0;
			}
		} else {
			mINC = 0;
		}

		for (int x = -1; x <= 1; x++) {
			testloc.setX(l.getX() + x);
			for (int y = 0; y <= 4; y++) {
				testloc.setY(l.getY() + y);
				if (l.getY() < 8) {
					testloc.setY(l.getY() + y + 1);
				}
				for (int z = -1; z <= 1; z++) {
					testloc.setZ(l.getZ());
					Block block = testloc.getBlock();
					Material mat = block.getType();
					if (mat == Material.COBWEB || block.getBlockData() instanceof TrapDoor) {
						/* Break cobwebs immediately, don't add them to the bad block list */
						EntityExplodeEvent event = new EntityExplodeEvent(mBoss, mBoss.getLocation(), Arrays.asList(block), 0f);
						Bukkit.getServer().getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							/* Only allow bosses to break blocks in areas where explosions are allowed */
							testloc.getBlock().setType(Material.AIR);
							loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 0.9f);
							loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 6, 1, 1, 1, 0.03);
						}
					} else if (!mIgnoredMats.contains(block.getType()) && y != 0) {
						badBlockList.add(block);
					}
				}
			}
		}

		/* If there are any blocks, destroy all blocking blocks */
		if (badBlockList.size() > 0) {
			/* Call an event with these exploding blocks to give plugins a chance to modify it */
			EntityExplodeEvent event = new EntityExplodeEvent(mBoss, l, badBlockList, 0f);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}

			/* Remove any remaining blocks, which might have been modified by the event */
			for (Block block : badBlockList) {
				if (block.getState() instanceof Container) {
					block.breakNaturally();
				} else {
					block.setType(Material.AIR);
				}
			}
			loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 0.9f);
			loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 6, 1, 1, 1, 0.03);
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
