package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
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
import org.jetbrains.annotations.Nullable;

public class ViciousCombosCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.VICIOUS_COMBOS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ZOMBIE_HEAD;
	}

	public void comboOnKill(World world, Location loc, Player mPlayer, double range, LivingEntity target) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1, 0.5f);
		new PartialParticle(Particle.CRIT, loc, 50, range, range, range, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 50, range, range, range, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 30, range, range, range, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_MOB, loc, 30, range, range, range, 0.001).spawnAsPlayerActive(mPlayer);
	}

	public void comboOnElite(World world, Location loc, Player mPlayer, double range, LivingEntity target) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 2, 0.5f);
		new PartialParticle(Particle.CRIT, loc, 500, range, range, range, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 500, range, range, range, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 350, range, range, range, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_MOB, loc, 350, range, range, range, 0.001).spawnAsPlayerActive(mPlayer);
	}
}
