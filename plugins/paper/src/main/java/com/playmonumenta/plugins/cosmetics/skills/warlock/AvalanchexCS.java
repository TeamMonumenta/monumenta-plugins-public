package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class AvalanchexCS extends AmplifyingHexCS {
	//Icy amplifying hex. Depth set: frost

	public static final String NAME = "Avalanchex";

	private static final Particle.DustOptions ICE_PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.AMPLIFYING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.POWDER_SNOW_BUCKET;
	}

	@Override
	public void amplifyingParticle(Player mPlayer, Location l) {
		new PartialParticle(Particle.REDSTONE, l, 10, 0.05, 0.05, 0.05, 0.1, ICE_PARTICLE_COLOR).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, l, 3, 0.05, 0.05, 0.05, 0.1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void amplifyingSound(World world, Location soundLoc) {
		world.playSound(soundLoc, Sound.BLOCK_GLASS_BREAK, 1, 0.85f);
		world.playSound(soundLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.9f);

		world.playSound(soundLoc, Sound.BLOCK_GLASS_BREAK, 1, 0.7f);
		world.playSound(soundLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.15f);

		world.playSound(soundLoc, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.9f);
	}
}
