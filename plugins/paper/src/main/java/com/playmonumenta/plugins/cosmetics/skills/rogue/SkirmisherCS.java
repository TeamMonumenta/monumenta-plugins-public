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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkirmisherCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SKIRMISHER;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BONE;
	}

	public void aesthetics(Player mPlayer, Location loc, World world, LivingEntity enemy) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1.5f);
		world.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 1, 0.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1f, 0.5f);
		loc.add(0, 1, 0);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 10, 0.35, 0.5, 0.35, 0.05).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_MOB, loc, 10, 0.35, 0.5, 0.35, 0.00001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT, loc, 10, 0.25, 0.5, 0.25, 0.55).spawnAsPlayerActive(mPlayer);
	}
}
