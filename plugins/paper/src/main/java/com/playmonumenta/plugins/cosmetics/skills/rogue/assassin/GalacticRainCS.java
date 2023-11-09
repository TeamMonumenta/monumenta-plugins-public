package com.playmonumenta.plugins.cosmetics.skills.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.rogue.StarCosmeticsFunctions;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class GalacticRainCS extends CoupDeGraceCS {
	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Some will bring the skies down to",
			"exact their plans and rain of terror."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.BEACON;
	}

	@Override
	public @Nullable String getName() {
		return "Galactic Rain";
	}

	@Override
	public void execution(Player player, LivingEntity le) {
		Location loc = le.getLocation();
		World world = player.getWorld();

		Vector[] starVertices = StarCosmeticsFunctions.generateStarVertices(8, 0.35, 0.5, false, false);
		ArrayList<Vector> star = StarCosmeticsFunctions.interpolatePolygon(starVertices, 1);

		Vector direction = player.getLocation().clone().subtract(loc.clone()).getDirection();
		double angle = Math.atan2(direction.getX(), direction.getZ());
		for (Vector v : star) {
			v.rotateAroundY(angle);
		}

		double approachAngle = FastUtils.randomDoubleInRange(0, Math.PI * 2);
		Vector approachVector = new Vector(FastUtils.cos(approachAngle), 0, FastUtils.sin(approachAngle));

		Vector[] pentagramStarVertices = StarCosmeticsFunctions.generateStarVertices(5, 3, 0.5, true, true);
		ArrayList<Vector> pentagramPentagon = StarCosmeticsFunctions.interpolatePolygon(new Vector[] { pentagramStarVertices[1], pentagramStarVertices[3], pentagramStarVertices[5], pentagramStarVertices[7], pentagramStarVertices[9] }, 3);
		ArrayList<Vector> pentagramStar = StarCosmeticsFunctions.interpolatePolygon(pentagramStarVertices, 4);
		pentagramStar.addAll(pentagramPentagon);

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1.3f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1.35f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1.4f);

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				if (mTicks < 7) {
					for (Vector v : star) {
						drawStar(player, le.getLocation().add(0, (7 - mTicks) * 15.0 / 7.0, 0)
							.add(approachVector.clone().multiply((7 - mTicks) * 8.0 / 7.0))
							.add(v));
					}
				} else if (mTicks == 7) {
					for (Vector v : star) {
						drawStar(player, le.getLocation().add(v));
					}
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 30, 0.5, 0.1, 0.5, 0).spawnAsPlayerActive(player);
					new PartialParticle(Particle.EXPLOSION_LARGE, loc, 15, 0.5, 0.1, 0.5, 0).spawnAsPlayerActive(player);
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 2f);
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 2f);
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 2f);
				} else if (mTicks < 14) {
					for (Vector v : pentagramStar) {
						drawCosmic(player, le.getLocation().add(v));
					}
				} else if (mTicks == 14) {
					for (Vector v : pentagramStar) {
						drawSolar(player, le.getLocation().add(v));
					}
					world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.8f, 2f);
				} else if (mTicks <= 17) {
					for (Vector v : pentagramStar) {
						drawSolar(player, le.getLocation().add(v));
					}
				} else {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void executionLv2(Player mPlayer, LivingEntity le) {
		// Empty. All effects are in Lv1 because I am boring... and adding anything more would get overwhelming as heck for something that triggers this frequently.
	}

	private void drawCosmic(Player player, Location loc) {
		switch (FastUtils.randomIntInRange(0, 5)) {
			case 0 -> new PartialParticle(Particle.CRIT_MAGIC, loc, 3, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			case 1 -> new PartialParticle(Particle.SPELL_WITCH, loc, 3, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			case 2 ->
				new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 3, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			default ->
				new PartialParticle(Particle.REDSTONE, loc, 2, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(rollCosmicColor(), 1.5f)).spawnAsPlayerActive(player);
		}
	}

	private void drawSolar(Player player, Location loc) {
		switch (FastUtils.randomIntInRange(0, 5)) {
			case 0 -> new PartialParticle(Particle.CRIT, loc, 3, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			case 1 ->
				new PartialParticle(Particle.ELECTRIC_SPARK, loc, 3, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			case 2 -> new PartialParticle(Particle.SMALL_FLAME, loc, 3, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			default ->
				new PartialParticle(Particle.REDSTONE, loc, 2, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(rollSolarColor(), 1.5f)).spawnAsPlayerActive(player);
		}
	}

	private void drawStar(Player player, Location location) {
		new PartialParticle(Particle.END_ROD, location, 1, 0, 0, 0, 0)
			.spawnAsPlayerActive(player);
	}

	private Color rollCosmicColor() {
		return Color.fromRGB(80 + FastUtils.randomIntInRange(0, 160), 80, 200);
	}

	private Color rollSolarColor() {
		int randColorGen = FastUtils.randomIntInRange(0, 160);
		return Color.fromRGB(240, randColorGen <= 120 ? 80 + randColorGen : 80, randColorGen > 120 ? randColorGen - 40 : 80);
	}
}
