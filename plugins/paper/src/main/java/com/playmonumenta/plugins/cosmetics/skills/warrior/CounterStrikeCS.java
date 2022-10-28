package com.playmonumenta.plugins.cosmetics.skills.warrior;

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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CounterStrikeCS implements CosmeticSkill {

	public static final ImmutableMap<String, CounterStrikeCS> SKIN_LIST = ImmutableMap.<String, CounterStrikeCS>builder()
		.put(BrambleShellCS.NAME, new BrambleShellCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.COUNTER_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CACTUS;
	}

	@Override
	public String getName() {
		return null;
	}

	public void counterOnHurt(Player mPlayer, Location loc, LivingEntity source) {
		mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.6f, 0.7f);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 6, 0.75, 0.5, 0.75, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 8, 0.75, 0.5, 0.75, 0.1).spawnAsPlayerActive(mPlayer);
	}
}
