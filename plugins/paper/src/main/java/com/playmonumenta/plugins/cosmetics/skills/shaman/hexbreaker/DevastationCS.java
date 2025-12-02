package com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
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
		for (Particle particle : List.of(Particle.SOUL_FIRE_FLAME, Particle.SOUL)) {
			ParticleUtils.explodingRingEffect(plugin, targetLoc.clone().add(0, 0.1, 0), radius, 1.2, 7, 0.3, loc -> new PartialParticle(particle, loc, 1, 0, 0, 0, 0.05).spawnAsPlayerActive(player));
		}
		for (double r = 0.5; r <= radius; r += 0.5) {
			double height = Math.sqrt(radius * radius - r * r);
			new PPCircle(Particle.SCULK_SOUL, targetLoc.clone().add(0, height, 0), r).extra(0.03).countPerMeter(0.3).spawnAsPlayerActive(player);
			new PPCircle(Particle.SOUL_FIRE_FLAME, targetLoc.clone().add(0, height + 0.15, 0), r).extra(0.03).countPerMeter(0.3).spawnAsPlayerActive(player);
		}
		new PartialParticle(Particle.EXPLOSION_LARGE, targetLoc.clone().add(0, 0.3, 0), 1).spawnAsPlayerActive(player);

		World world = targetLoc.getWorld();
		world.playSound(targetLoc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(targetLoc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.8f, 0.7f);
		world.playSound(targetLoc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 0.8f, 0.7f);
		world.playSound(targetLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 0.8f, 0.7f);
		world.playSound(targetLoc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1.0f, 2f);
		world.playSound(targetLoc, Sound.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(targetLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(targetLoc, Sound.BLOCK_TRIAL_SPAWNER_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
	}
}
