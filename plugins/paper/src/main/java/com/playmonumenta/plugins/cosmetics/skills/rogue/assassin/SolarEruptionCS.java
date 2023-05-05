package com.playmonumenta.plugins.cosmetics.skills.rogue.assassin;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SolarEruptionCS extends BodkinBlitzCS {
	private double mAngle = 0;
	private boolean mBuffActive = false;
	private final Vector[] mNormals = new Vector[2];

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Imbue the fury of the sun into",
			"your spirit and dash forth."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIREWORK_ROCKET;
	}

	@Override
	public @Nullable String getName() {
		return "Solar Eruption";
	}

	@Override
	public void blitzStartSound(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1, 1.7f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1, 2f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 1, 1.3f);
	}

	@Override
	public void blitzTrailEffect(Player player, Location loc, Vector dir) {
		if (mNormals[0] == null) {
			mNormals[0] = dir.getCrossProduct(new Vector(0, 1, 0)).normalize();
			if (!Double.isFinite(mNormals[0].getX())) {
				mNormals[0] = dir.getCrossProduct(new Vector(1, 0, 0)).normalize();
			}
			mNormals[1] = mNormals[0].getCrossProduct(dir);
		}

		for (int i = 0; i < 5; i++) {
			drawSolar(player, loc.clone().add(mNormals[0].clone().multiply(FastUtils.cos(mAngle + 2 * i * Math.PI / 5.0)))
				.add(mNormals[1].clone().multiply(FastUtils.sin(mAngle + 2 * i * Math.PI / 5.0))));
		}
		mAngle += Math.PI / 30;
	}

	@Override
	public void blitzEndEffect(World world, Player player, Location tpLoc) {
		mBuffActive = true;
		mNormals[0] = null;
		mNormals[1] = null;

		new PartialParticle(Particle.SMOKE_LARGE, tpLoc.clone().add(0, 1, 0), 200, 2.5, 0.8, 2.5, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_NORMAL, tpLoc, 400, 2.5, 0.2, 2.5, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMALL_FLAME, tpLoc, 80, 2.5, 0.2, 2.5, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, tpLoc, 30, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.ELECTRIC_SPARK, tpLoc, 30, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, tpLoc, 30, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(rollSolarColor(), 1f)).spawnAsPlayerActive(player);

		world.playSound(tpLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 1.6f);
		world.playSound(tpLoc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1, 0.5f);
		world.playSound(tpLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 0.5f);
	}

	@Override
	public void blitzBuffEffect(Player player, int mTicks) {
		if (mBuffActive) {
			Location loc = player.getLocation().add(0, 0.3, 0);
			new PartialParticle(Particle.CRIT, loc, 6, 0.3, 0.1, 0.3, 0.1).spawnAsPlayerActive(player);
			new PartialParticle(Particle.ELECTRIC_SPARK, loc, 6, 0.3, 0.05, 0.3, 0.1).spawnAsPlayerActive(player);
			new PartialParticle(Particle.REDSTONE, loc, 10, 0.3, 0.1, 0.3, 0, new Particle.DustOptions(rollSolarColor(), 0.75f)).spawnAsPlayerActive(player);
		}
	}

	@Override
	public void blitzOnDamage(World world, Player player, Location entityLoc) {
		new PartialParticle(Particle.SMOKE_LARGE, entityLoc.clone().add(0, -1, 0), 100, 2.5, 0.8, 2.5, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, entityLoc.clone().add(0, 10, 0), 100, 2.5, 0.8, 2.5, 0.05).spawnAsPlayerActive(player);
		double angleVariance = FastUtils.randomDoubleInRange(0, 2 * Math.PI / 5.0);
		for (double height = -1; height < 10; height += 0.1) {
			for (double angle = 0; angle < Math.PI * 2; angle += 2 * Math.PI / 5.0) {
				drawSolar(player, entityLoc.clone().add(new Vector(1.2 * FastUtils.cos(angle + angleVariance), height, 1.2 * FastUtils.sin(FastUtils.sin(angle + angleVariance)))));
			}
			angleVariance += Math.PI / 40;
		}

		player.getWorld().playSound(entityLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.8f);
	}

	@Override
	public void applyStealthCosmetic(Player player) {
		// Should be empty, put stuff in blitzEndEffect().
	}

	@Override
	public void removeStealthCosmetic(Player player) {
		mBuffActive = false;
		Location loc = player.getLocation();
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 200, 2.5, 0.8, 2.5, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 400, 2.5, 0.2, 2.5, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMALL_FLAME, loc, 80, 2.5, 0.2, 2.5, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, loc, 30, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.ELECTRIC_SPARK, loc, 30, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 30, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(rollSolarColor(), 1f)).spawnAsPlayerActive(player);

		player.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.8f, 0.5f);
		player.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.2f);
	}

	private void drawSolar(Player player, Location loc) {
		switch (FastUtils.randomIntInRange(0, 5)) {
			case 0 -> new PartialParticle(Particle.CRIT, loc, 5, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			case 1 ->
				new PartialParticle(Particle.ELECTRIC_SPARK, loc, 5, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			case 2 -> new PartialParticle(Particle.SMALL_FLAME, loc, 5, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			default ->
				new PartialParticle(Particle.REDSTONE, loc, 3, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(rollSolarColor(), 1.5f)).spawnAsPlayerActive(player);
		}
	}

	private Color rollSolarColor() {
		int randColorGen = FastUtils.randomIntInRange(0, 160);
		return Color.fromRGB(240, randColorGen <= 120 ? 80 + randColorGen : 80, randColorGen > 120 ? randColorGen - 40 : 80);
	}
}
