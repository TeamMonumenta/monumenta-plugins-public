package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class StarfireSpinCS extends BladeDanceCS {
	private static final Particle.DustOptions FIRE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 150, 20), 1.5f);
	private static final Particle.DustOptions FIRE_COLOR_SMALL = new Particle.DustOptions(Color.fromRGB(255, 150, 20), 1.0f);
	private static final Vector[] TRI_UNIT_VECTORS = new Vector[] {
		new Vector(1, 0, 0),
		new Vector(-0.5, 0, 0.866),
		new Vector(-0.5, 0, -0.866)
	};
	private static final double PARTICLES_PER_LINE = 10.0;

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CHARGE;
	}

	@Override
	public @Nullable String getName() {
		return "Starfire Spin";
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Find the right rhythm and you can",
			"bend even flames to your will."
		);
	}

	@Override
	public void danceStart(Player player) {
		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.7f, 1.9f);
		world.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.7f, 1.2f);
		new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, player.getLocation().clone().add(0, 1, 0), 12, 0.55, 0.5, 0.55, 0, FIRE_COLOR).spawnAsPlayerActive(player);
	}

	@Override
	public void danceTick(Player player, int tick, double radius) {
		Location centre = player.getLocation();

		for (Vector v : TRI_UNIT_VECTORS) {
			for (double i = 1; i <= PARTICLES_PER_LINE; i++) {
				Location location = centre.clone()
					.add(v.clone().rotateAroundY(velocityFunction(tick, i / PARTICLES_PER_LINE))
					.multiply(0.9 * i * radius / PARTICLES_PER_LINE).add(new Vector(0, 0.1, 0)));
				Vector direction = centre.clone()
					.add(v.clone().rotateAroundY(velocityFunction(tick + 1, i / PARTICLES_PER_LINE))
					.multiply(0.9 * i * radius / PARTICLES_PER_LINE).add(new Vector(0, 0.1, 0)))
					.subtract(location).toVector();
				drawFlame(location, player, direction.multiply(4));
				if (tick >= 2 && i % 2 == 0) {
					drawTrail(
						centre.clone()
						.add(v.clone().rotateAroundY(velocityFunction(tick - 2, i / PARTICLES_PER_LINE))
						.multiply(0.9 * i * radius / PARTICLES_PER_LINE).add(new Vector(0, 0.1, 0))),
						player);
				}
			}
		}

		if (tick % 2 == 0) {
			float pitch = 0.7f + 0.1f * tick / 2;
			player.getWorld().playSound(centre, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.8f, pitch + 0.3f);
		}
	}

	@Override
	public void danceEnd(Player player, double radius) {
		Location centre = player.getLocation();

		for (double p = 0; p < Math.PI * 2; p += 2 * Math.PI / 30) {
			drawFlame(centre.clone().add(new Vector(1, 0, 0).rotateAroundY(p).multiply(radius).add(new Vector(0, 0.5, 0))), player, new Vector(0, 0, 0));
		}
		new PartialParticle(Particle.END_ROD, centre.clone().add(0, 0.5, 0), 100, 2, 0.2, 2, 0)
			.spawnAsPlayerActive(player);

		World world = player.getWorld();
		world.playSound(centre, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 1.5f);
		world.playSound(centre, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.6f, 0.8f);
		world.playSound(centre, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 1f, 1.4f);
	}

	@Override
	public void danceHit(Player player, LivingEntity mob) {
		Location mobLoc = mob.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.END_ROD, mobLoc, 5, 0.35, 0.5, 0.35, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, mobLoc, 10, 0.25, 0.5, 0.25, 0.3).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, mobLoc, 15, 0.35, 0.5, 0.35, 0, FIRE_COLOR).spawnAsPlayerActive(player);
	}

	private void drawFlame(Location location, Player player, Vector direction) {
		new PartialParticle(Particle.SMALL_FLAME, location, 1, 0, 0, 0, 0.015, null, true)
			.delta(direction.getX(), direction.getY(), direction.getZ())
			.spawnAsPlayerActive(player);
	}

	private void drawTrail(Location location, Player player) {
		new PartialParticle(Particle.REDSTONE, location, 2, 0.03, 0, 0.03, 0, FIRE_COLOR_SMALL)
			.spawnAsPlayerActive(player);
	}

	private double velocityFunction(int tick, double fracAlongLine) {
		return tick * Math.min(tick, 9) * 0.02 - 0.09 * fracAlongLine * (fracAlongLine - 1) * Math.min(tick, 9);
	}
}
