package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ScorchedEarthCS implements CosmeticSkill {

	public static final Color SCORCHED_EARTH_COLOR_LIGHT = Color.fromRGB(230, 134, 0);
	public static final Color SCORCHED_EARTH_COLOR_DARK = Color.fromRGB(140, 63, 0);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SCORCHED_EARTH;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public ScorchedEarthCS copyForActiveInstance() {
		return this;
	}

	public void landEffects(Player player, Location loc, double radius, int duration) {
		loc = loc.clone().add(0, 0.25, 0);
		World world = player.getWorld();
		double delta = (radius - 1) / 2;
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 50, delta, 0.5, delta, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 15, delta, 0.5, delta, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 20, delta, 0.5, delta, new Particle.DustOptions(SCORCHED_EARTH_COLOR_DARK, 2.0f)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, 30, delta, 0.5, delta, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.LAVA, loc, 25, delta, 0.5, delta, 0).spawnAsPlayerActive(player);
		new PPCircle(Particle.FLAME, loc, radius).ringMode(true).count(20).spawnAsPlayerActive(player);

		world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.5f, 1.5f);
	}

	public void activeEffects(Player player, Location loc, double radius, int timeRemaining, int duration) {
		double delta = (radius - 1) / 2;
		new PartialParticle(Particle.SMOKE_LARGE, loc, 3, delta, 0.3, delta, 0).minimumCount(0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, 3, delta, 0.1, delta, 0.1f).minimumCount(0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 5, delta, 0.3, delta, new Particle.DustOptions(SCORCHED_EARTH_COLOR_LIGHT, 1.5f))
			.minimumCount(0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 5, delta, 0.3, delta, new Particle.DustOptions(SCORCHED_EARTH_COLOR_DARK, 1.5f))
			.minimumCount(0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.LAVA, loc, 1, delta, 0.1, delta, 0).minimumCount(0).spawnAsPlayerActive(player);
		new PPCircle(Particle.FLAME, loc, radius).ringMode(true).count(5).spawnAsPlayerActive(player);

		new PartialParticle(Particle.REDSTONE,
			loc.clone().add(5 * FastUtils.sin((timeRemaining % 40 / 20.0 - 1) * Math.PI), 0, 5 * FastUtils.cos((timeRemaining % 40 / 20.0 - 1) * Math.PI)))
			.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.25f))
			.minimumCount(0).spawnAsPlayerActive(player);

		if (60 <= timeRemaining % 120 && timeRemaining % 120 < 65 && timeRemaining < duration) {
			loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 1f, 0.5f);
		}
	}

	public void damageEffect(LivingEntity entity, Player alchemist) {
		Location loc = entity.getLocation().clone().add(0, 1, 0);
		new PartialParticle(Particle.FLAME, loc, 5, 0.25, 0.5, 0.25, 0.05).spawnAsPlayerActive(alchemist);
		new PartialParticle(Particle.REDSTONE, loc, 15, 0.35, 0.5, 0.35,
				new Particle.DustOptions(ScorchedEarthCS.SCORCHED_EARTH_COLOR_DARK, 1.0f)).spawnAsPlayerActive(alchemist);
		new PartialParticle(Particle.LAVA, loc, 3, 0.25, 0.5, 0.25, 0).spawnAsPlayerActive(alchemist);
	}

}
