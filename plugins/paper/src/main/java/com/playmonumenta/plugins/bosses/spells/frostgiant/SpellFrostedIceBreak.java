package com.playmonumenta.plugins.bosses.spells.frostgiant;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

import com.playmonumenta.plugins.bosses.spells.Spell;

//Frost Giant breaks frosted ice that he walks over
public class SpellFrostedIceBreak extends Spell {

	private LivingEntity mBoss;

	public SpellFrostedIceBreak(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		Location tempLoc = loc.clone();
		for (int x = -3; x <= 3; x++) {
			for (int z = -1; z <= 1; z++) {
				tempLoc.set(loc.getX() + x, loc.getY() - 1, loc.getZ() + z);
				if (tempLoc.getBlock().getType() == Material.FROSTED_ICE) {
					tempLoc.getBlock().setType(Material.CRACKED_STONE_BRICKS);
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
