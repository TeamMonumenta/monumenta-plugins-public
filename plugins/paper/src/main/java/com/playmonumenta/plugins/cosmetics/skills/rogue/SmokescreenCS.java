package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SmokescreenCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SMOKESCREEN;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DEAD_TUBE_CORAL;
	}

	public void smokescreenEffects(Player mPlayer, World world, Location loc) {
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 750, 4.5, 0.8, 4.5, 0.05).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 1500, 4.5, 0.2, 4.5, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.35f);
	}

	public void residualEnhanceEffects(Player mPlayer, World world, Location mCloudLocation) {
		// Visuals are based off of Hekawt's UndeadRogue Smokescreen Spell
		new PartialParticle(Particle.SMOKE_NORMAL, mCloudLocation, 3, 0.3, 0.05, 0.3, 0.075).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, mCloudLocation, 75, 3.5, 0.2, 4.5, 0.05).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, mCloudLocation, 2, 0.3, 0.05, 0.3, 0.075).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, mCloudLocation, 30, 3.5, 0.8, 4.5, 0.025).spawnAsPlayerActive(mPlayer);

		AbilityUtils.playPassiveAbilitySound(mCloudLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1, 0.7f);
	}
}
