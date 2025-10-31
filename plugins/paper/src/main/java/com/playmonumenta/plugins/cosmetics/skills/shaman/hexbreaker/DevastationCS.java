package com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class DevastationCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DEVASTATION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.COAL_BLOCK;
	}

	public void devastationCast(Plugin plugin, Location targetLoc, Player player, double radius) {
		for (Particle particle : List.of(Particle.FLAME, Particle.LAVA)) {
			ParticleUtils.explodingRingEffect(plugin, targetLoc.clone().add(0, 0.1, 0), radius, 1.2, 5, 0.2, loc -> new PartialParticle(particle, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player));
		}
		targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_END_PORTAL_SPAWN,
			SoundCategory.PLAYERS, 0.3f, 2.0f);
		targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_ENDER_CHEST_OPEN,
			SoundCategory.PLAYERS, 0.6f, 0.6f);
		targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT,
			SoundCategory.PLAYERS, 1.0f, 1.0f);
	}
}
