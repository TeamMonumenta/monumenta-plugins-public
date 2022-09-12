package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class MagmaShieldCS implements CosmeticSkill {

	public static final ImmutableMap<String, MagmaShieldCS> SKIN_LIST = ImmutableMap.<String, MagmaShieldCS>builder()
		.put(VolcanicBurstCS.NAME, new VolcanicBurstCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.MAGMA_SHIELD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_CREAM;
	}

	public void magmaParticle(Player mPlayer, Location l) {
		new PartialParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.15).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.15, 0.15, 0.15, 0.1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
	}

	public void magmaSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
	}
}
