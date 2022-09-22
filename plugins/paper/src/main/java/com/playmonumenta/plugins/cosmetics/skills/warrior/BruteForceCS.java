package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BruteForceCS implements CosmeticSkill {

	public static final ImmutableMap<String, BruteForceCS> SKIN_LIST = ImmutableMap.<String, BruteForceCS>builder()
		.put(ColossalBruteCS.NAME, new ColossalBruteCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BRUTE_FORCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STONE_AXE;
	}

	public void bruteOnDamage(Player mPlayer, Location loc, int combo) {
		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.135).spawnAsPlayerActive(mPlayer);
	}

	public void bruteOnSpread(Player mPlayer, LivingEntity mob) {
		//Nope!
	}
}
