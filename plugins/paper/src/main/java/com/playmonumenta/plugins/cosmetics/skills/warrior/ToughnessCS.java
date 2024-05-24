package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class ToughnessCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TOUGHNESS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_HELMET;
	}

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(229, 28, 36), 0.8f);

	public void toughnessEnhancement(Player player) {
		Location loc = player.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 0.05).data(COLOR).spawnAsPlayerPassive(player);
	}
}
