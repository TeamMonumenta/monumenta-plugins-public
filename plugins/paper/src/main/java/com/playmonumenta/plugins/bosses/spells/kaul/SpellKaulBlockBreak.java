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
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.loot.Lootable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class SpellKaulBlockBreak extends Spell {
	private LivingEntity mBoss;
	private List<Material> mNoBreak;

	public SpellKaulBlockBreak(LivingEntity boss) {
		mBoss = boss;
		mNoBreak = new ArrayList<Material>();
	}

	public SpellKaulBlockBreak(LivingEntity launcher, Material... noBreak) {
		mBoss = launcher;
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
		Vector dir = LocationUtils.getDirectionTo(target.getLocation().add(0, 1, 0), mBoss.getLocation());
		Location tloc = mBoss.getLocation().setDirection(dir);

		Vector vec;
		float yaw = tloc.getYaw();
		float yaw1 = yaw + 90;
		vec = new Vector(Math.cos(0) * 1, 0, Math.sin(0) * 1);
		vec = VectorUtils.rotateYAxis(vec, yaw1);
		vec = VectorUtils.rotateXAxis(vec, -tloc.getPitch());

		Location l = tloc.clone().add(vec);

		/* Get a list of all blocks that impede the boss's movement */
		List<Block> badBlockList = new ArrayList<Block>();
		Location testloc = new Location(l.getWorld(), 0, 0, 0);
		for (int x = -1; x <= 1; x++) {
			testloc.setX(l.getX() + x);
			for (int y = 1; y <= 3; y++) {
				testloc.setY(l.getY() + y);
				for (int z = -1; z <= 1; z++) {
					testloc.setZ(l.getZ() + z);
					Block block = testloc.getBlock();
					Material material = block.getType();
					if ((!mIgnoredMats.contains(material)) && !mNoBreak.contains(material) &&
						(material.isSolid() || material.equals(Material.COBWEB)) &&
						(!(block.getState() instanceof Lootable) || !((Lootable)block.getState()).hasLootTable())) {
						badBlockList.add(testloc.getBlock());
					}
				}
			}
		}

		/* If more than one block, destroy all blocking blocks */
		if (badBlockList.size() > 1) {
			/* Call an event with these exploding blocks to give plugins a chance to modify it */
			EntityExplodeEvent event = new EntityExplodeEvent(mBoss, l, badBlockList, 0f);
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
