package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DaggerThrowCS implements CosmeticSkill {

	public static final ImmutableMap<String, DaggerThrowCS> SKIN_LIST = ImmutableMap.<String, DaggerThrowCS>builder()
		.put(DaggerOfNothingCS.NAME, new DaggerOfNothingCS())
		.build();

	private static final Particle.DustOptions DAGGER_THROW_COLOR = new Particle.DustOptions(Color.fromRGB(64, 64, 64), 1);

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.DAGGER_THROW;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WOODEN_SWORD;
	}

	@Override
	public String getName() {
		return null;
	}

	public void daggerCastSound(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.25f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.0f);
	}

	public void daggerLineEffect(Location bLoc, Vector newDir, Player mPlayer) {
		new PPLine(Particle.REDSTONE, bLoc, newDir, 0.9).countPerMeter(10).delta(0.1).data(DAGGER_THROW_COLOR).spawnAsPlayerActive(mPlayer);
	}

	public void daggerHitEffect(World world, Location loc, Location bLoc, Player mPlayer) {
		new PartialParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.4f, 2.5f);
	}

	public void daggerHitBlockEffect(Location bLoc, Player mPlayer) {
		new PartialParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1).spawnAsPlayerActive(mPlayer);
	}
}
