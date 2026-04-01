package com.playmonumenta.plugins.cosmetics.skills.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PrestigiousBeamCS extends HallowedBeamCS implements PrestigeCS {

	public static final String NAME = "Prestigious Beam";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.2f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.2f);
	private static final Particle.DustOptions BURN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 180, 0), 1.2f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Golden winds follow",
			"the beam's arc."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SPECTRAL_ARROW;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return player != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public int getPrice() {
		return 1;
	}

	@Override
	public void beamCast(Player player, Location startLocation, double range, Location targetLocation) {
		World world = player.getWorld();
		// Launch sound
		world.playSound(startLocation, Sound.BLOCK_TRIPWIRE_DETACH, SoundCategory.PLAYERS, 0.6f, 0.75f);
		world.playSound(startLocation, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.2f, 0.8f);
		world.playSound(startLocation, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.3f, 0.5f);
		world.playSound(startLocation, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, SoundCategory.PLAYERS, 0.75f, 0.75f);
		world.playSound(startLocation, Sound.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 2.5f, 1.75f);

		int rotation = 0;
		Vector dir = LocationUtils.getDirectionTo(targetLocation, startLocation);
		for (double i = 0; i < range; i += 0.75) {
			// Beam effect
			startLocation.add(dir.clone().multiply(0.75));
			for (int j = 0; j < 3; j++) {
				double radian = FastMath.toRadians(rotation + (j * 120));
				Vector vec = new Vector(FastUtils.cos(radian), 0, FastUtils.sin(radian));
				vec = VectorUtils.rotateXAxis(vec, startLocation.getPitch() + 90);
				vec = VectorUtils.rotateYAxis(vec, startLocation.getYaw());
				new PartialParticle(Particle.REDSTONE, startLocation.clone().add(vec), 2, 0.05, 0.05, 0.05, 0.25,
					j == 0 ? GOLD_COLOR : (j == 1 ? LIGHT_COLOR : BURN_COLOR))
					.spawnAsPlayerActive(player);
			}
			if (i % 3 == 1.5) {
				new PartialParticle(Particle.CLOUD, startLocation, 1, 0.05, 0.05, 0.05, 0.02).spawnAsPlayerActive(player);
			}
			rotation += 8;
		}
	}

	@Override
	public void beamSplash(Player player, Location targetLocation, double radius) {
		Color burn = BURN_COLOR.getColor();
		new PartialParticle(Particle.EXPLOSION_LARGE, targetLocation.clone().add(0, 1, 0)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, targetLocation.clone().add(0, 0.6, 0), 30, 0.7, 0.4, 0.7, 0.2).data(LIGHT_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, targetLocation.clone().add(0, 0.6, 0), 30, 0.5 * radius, 0.4 * radius, 0.5 * radius, 0.2).data(GOLD_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, targetLocation.clone().add(0, 0.6, 0), 20, 0, 0, 0, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, targetLocation, 25, 1.0, 0.15, 1.0, 0.1).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL_MOB, targetLocation, radius).extra(1).countPerMeter(2).delta(burn.getRed() / 255D, burn.getGreen() / 255D, burn.getBlue() / 255D).directionalMode(true).spawnAsPlayerActive(player);
		for (int i = 1; i < 6; i++) {
			final double healEffectRadius = i * radius / 5;
			final int healRingUnits = i * 16;
			new BukkitRunnable() {
				@Override
				public void run() {
					ParticleUtils.drawRing(targetLocation.clone().add(0, 0.125, 0), healRingUnits, new Vector(0, 1, 0), healEffectRadius,
						(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0, 0.1, GOLD_COLOR).spawnAsPlayerActive(player)
					);
				}
			}.runTaskLater(Plugin.getInstance(), i / 2);
		}

		targetLocation.getWorld().playSound(targetLocation, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.6f, 1.6f);
		targetLocation.getWorld().playSound(targetLocation, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.4f);
		targetLocation.getWorld().playSound(targetLocation, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 1.2f, 1.5f);
		targetLocation.getWorld().playSound(targetLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.0f, 1.5f);
	}
}
