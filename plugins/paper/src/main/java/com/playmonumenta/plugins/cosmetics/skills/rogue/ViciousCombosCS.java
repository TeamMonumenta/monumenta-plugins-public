package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ViciousCombosCS implements CosmeticSkill {

	public static final ImmutableMap<String, ViciousCombosCS> SKIN_LIST = ImmutableMap.<String, ViciousCombosCS>builder()
		.put(TranscCombosCS.NAME, new TranscCombosCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.VICIOUS_COMBOS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ZOMBIE_HEAD;
	}

	public void comboOnKill(World world, Location loc, Player mPlayer, double range, LivingEntity target) {
		final double VICIOUS_COMBOS_RANGE = range;
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5f);
		new PartialParticle(Particle.CRIT, loc, 50, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 50, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 30, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_MOB, loc, 30, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001).spawnAsPlayerActive(mPlayer);
	}

	public void comboOnElite(World world, Location loc, Player mPlayer, double range, LivingEntity target) {
		final double VICIOUS_COMBOS_RANGE = range;
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2, 0.5f);
		new PartialParticle(Particle.CRIT, loc, 500, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 500, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 350, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_MOB, loc, 350, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001).spawnAsPlayerActive(mPlayer);
	}
}
