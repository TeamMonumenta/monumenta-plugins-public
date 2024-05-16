package com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class VerdantApparitionCS extends HauntingShadesCS {
	private static final Particle.DustOptions GREEN_SMALL = new Particle.DustOptions(Color.fromRGB(120, 170, 0), 0.8f);
	private static final Particle.DustOptions NEON_SMALL = new Particle.DustOptions(Color.fromRGB(235, 255, 0), 0.8f);
	private static final String AS_NAME = "VerdantApparition";

	@Override
	public String getAsName() {
		return AS_NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The nightmare you conquered returns,",
			"ready to be unleashed by your own hands."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.JUNGLE_LOG;
	}

	@Override
	public @Nullable String getName() {
		return "Verdant Apparition";
	}

	@Override
	public void shadesStartSound(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_SKELETON_HORSE_AMBIENT, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(loc, Sound.ENTITY_SKELETON_HORSE_HURT, SoundCategory.PLAYERS, 0.7f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 0.7f, 1.2f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_DEATH, SoundCategory.PLAYERS, 0.7f, 0.5f);
	}

	@Override
	public void shadesTickEffect(Plugin plugin, World world, Player player, Location bLoc, double mAoeRadius, int mT) {
		Location loc = bLoc.clone().add(0, 0.25, 0);

		new PartialParticle(Particle.FALLING_SPORE_BLOSSOM, loc.clone().add(0, 1, 0), 2, mAoeRadius / 2, 1, mAoeRadius / 2, 0).spawnAsPlayerActive(player);

		Color color = FastUtils.RANDOM.nextBoolean() ? GREEN_SMALL.getColor() : NEON_SMALL.getColor();
		double red = color.getRed() / 255d;
		double green = color.getGreen() / 255d;
		double blue = color.getBlue() / 255d;
		new PPCircle(Particle.SPELL_MOB_AMBIENT, loc, mAoeRadius)
			.ringMode(false).delta(red, green, blue).extra(1).directionalMode(true).count(4).spawnAsPlayerActive(player);

		if (mT % 20 == 0) {
			int trees = FastUtils.randomIntInRange(2, 4);
			double length = FastUtils.randomDoubleInRange(1.1, 1.3) * (mAoeRadius / 6) + (trees == 4 ? 0.3 : 0);
			double angle = FastUtils.randomDoubleInRange(20, 40);
			double offset = FastUtils.randomDoubleInRange(0, 360);
			for (int i = 0; i < trees; i++) {
				Vector direction = VectorUtils.rotateYAxis(new Vector(1, 0, 0), 360d / trees * i + offset);
				drawTreeRecursively(player, 1, loc, direction, length, angle, loc, mAoeRadius);
			}

			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_RAVAGER_STEP, 0.35f, 0.7f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1f, 0.6f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1f, 1.2f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.BLOCK_GRASS_STEP, 1f, 0.7f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.BLOCK_SOUL_SAND_STEP, 1f, 0.7f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, 0.25f, 0.5f);
		}
	}

	@Override
	public void shadesEndEffect(World world, Player player, Location bLoc, double radius) {
		new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 40, 0.3, 0.6, 0.3, 0.15).spawnAsPlayerActive(player);

		Color color = NEON_SMALL.getColor();
		double red = color.getRed() / 255d;
		double green = color.getGreen() / 255d;
		double blue = color.getBlue() / 255d;
		new PPCircle(Particle.SPELL_MOB, bLoc.clone().add(0, 0.25, 0), 0.5)
			.ringMode(false).delta(red, green, blue).extra(1).directionalMode(true).count(16).spawnAsPlayerActive(player);

		world.playSound(bLoc, Sound.ENTITY_SKELETON_HORSE_AMBIENT, SoundCategory.PLAYERS, 1f, 1.2f);
		world.playSound(bLoc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 0.7f, 1.6f);
		world.playSound(bLoc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1f, 0.75f);
	}

	public void drawTreeRecursively(Player player, int iteration, Location loc, Vector direction, double length, double angle, Location origin, double maxDistance) {
		if (iteration > 8 || loc.distance(origin) > maxDistance) {
			return;
		}

		double density = 12 - iteration * 1.5;
		Particle.DustOptions color = new Particle.DustOptions(
			ParticleUtils.getTransition(GREEN_SMALL.getColor(), NEON_SMALL.getColor(), loc.distance(origin) / maxDistance), 0.8f);
		new PPLine(Particle.REDSTONE, loc, direction, length).data(color).countPerMeter(density).groupingDistance(0.05).spawnAsPlayerActive(player);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Location end = loc.clone().add(direction.clone().multiply(length));
			drawTreeRecursively(player, iteration + 1, end, VectorUtils.rotateYAxis(direction, angle), length * 1.05, angle, origin, maxDistance);
			drawTreeRecursively(player, iteration + 1, end, VectorUtils.rotateYAxis(direction, -angle), length * 1.05, angle, origin, maxDistance);
		}, 1);
	}
}
