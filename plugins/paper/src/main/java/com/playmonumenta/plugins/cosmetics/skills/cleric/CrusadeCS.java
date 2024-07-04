package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;


public class CrusadeCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CRUSADE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ZOMBIE_HEAD;
	}

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(252, 211, 3), 1.0f);

	public void crusadeTag(Entity enemy) {
		Location loc = enemy.getLocation().add(0, enemy.getHeight() + 0.6, 0);
		new PartialParticle(Particle.REDSTONE, loc, 20, 0.01, 0.35, 0.01, COLOR).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, loc.add(0, 0.2, 0), 20, 0.175, 0.01, 0.01, COLOR).spawnAsEnemyBuff();
	}

	public void crusadeEnhancement(Player player, long numMobs) {

	}
}

