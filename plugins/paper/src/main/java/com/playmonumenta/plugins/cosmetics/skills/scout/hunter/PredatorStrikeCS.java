package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PredatorStrikeCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PREDATOR_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SPECTRAL_ARROW;
	}

	public void strikeTick(Player player, int tick) {
		new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 0.75, 0), 1, 0.25, 0, 0.25, 0).spawnAsPlayerActive(player);
	}

	public void strikeParticleLine(Player player, Location startLoc, Location endLoc) {
		new PPLine(Particle.SMOKE_NORMAL, startLoc, endLoc).countPerMeter(20).delta(0.15).extra(0.075).spawnAsPlayerActive(player);
		new PPLine(Particle.FLAME, startLoc, endLoc).countPerMeter(4).delta(0.2).extra(0.1).spawnAsPlayerActive(player);
	}

	public void strikeSoundReady(World world, Player player) {
		world.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, SoundCategory.PLAYERS, 1, 1.0f);
	}

	public void strikeLaunch(World world, Player player) {
		world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1, 0.8f);
	}

	public void strikeExplode(World world, Player player, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.7f);
		world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.7f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 45, radius, radius, radius, 0.125).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, 12, radius, radius, radius, 0.1).spawnAsPlayerActive(player);
	}

	public void strikeImpact(Runnable runnable, Location l, Player player) {
		runnable.run();
	}
}
