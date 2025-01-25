package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.classes.ClassAbility;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DaggerThrowCS implements CosmeticSkill {

	private static final Particle.DustOptions DAGGER_THROW_COLOR = new Particle.DustOptions(Color.fromRGB(64, 64, 64), 1);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DAGGER_THROW;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WOODEN_SWORD;
	}

	public void daggerThrowEffect(World world, Location loc, Player player) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.25f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.0f);
	}

	public void daggerParticle(Location startLoc, Location endLoc, Player player) {
		new PPLine(Particle.REDSTONE, startLoc, endLoc).countPerMeter(10).delta(0.1).data(DAGGER_THROW_COLOR).spawnAsPlayerActive(player);
	}

	public void daggerHitEffect(World world, Location loc, LivingEntity target, Player player) {
		new PartialParticle(Particle.SWEEP_ATTACK, target.getLocation(), 3, 0.3, 0.3, 0.3, 0.1).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.4f, 2.5f);
	}

	public void daggerHitBlockEffect(Location bLoc, Player player) {
		new PartialParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1).spawnAsPlayerActive(player);
	}
}
