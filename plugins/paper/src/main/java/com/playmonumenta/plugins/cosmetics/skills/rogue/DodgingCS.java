package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class DodgingCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DODGING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SHIELD;
	}

	public void dodgeEffect(Player mPlayer, World world, Location loc) {
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 2f);
	}

	public void dodgeEffectLv2(Player mPlayer, World world, Location loc) {
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0.25, 0.45, 0.25, 0.15).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1, 1.35f);
	}

	public void deflectTrailEffect(Player mPlayer, Location particleLocation) {
		new PartialParticle(Particle.VILLAGER_HAPPY, particleLocation, 3, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, particleLocation, 6, 0.05, 0.05, 0.05, 0.05).spawnAsPlayerActive(mPlayer);
	}
}
