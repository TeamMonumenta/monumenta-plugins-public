package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class InterconnectedHavocCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.INTERCONNECTED_HAVOC;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CHAIN;
	}

	public void havocLine(Player player, Location startPoint, Location endPoint) {
		new PPLine(Particle.ENCHANTMENT_TABLE, startPoint, endPoint).countPerMeter(10).spawnAsPlayerActive(player);
	}

	public void havocDamage(Player player, Location startPoint, Location endPoint) {
		new PPLine(Particle.DAMAGE_INDICATOR, startPoint, endPoint).countPerMeter(4).spawnAsPlayerActive(player);
	}
}
