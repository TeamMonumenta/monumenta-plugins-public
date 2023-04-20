package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ArcaneTransmutationCS extends TransmutationRingCS {

	public static final String NAME = "Arcane Transmutation Ring";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Some Alchemists claim to have performed",
			"the Magnum Opus. While their sigils are impressive",
			"and can use souls of unfortunate creatures to enhance",
			"souls of others, the effects are only temporary.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private double mRotation = 0;
	private double mIncreasedRotation = 0;
	private final List<ArcanePotionsCS.Symbol> mSideSymbols = new ArrayList<>(ArcanePotionsCS.SMALL_SYMBOLS);

	@Override
	public void startEffect(Player player, Location center, double radius) {
		mRotation = 0;
		mIncreasedRotation = 0;
		Collections.shuffle(mSideSymbols, FastUtils.RANDOM);

		center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.PLAYERS, 1, 0.5f);
	}

	@Override
	public void periodicEffect(Player player, Location center, double radius, int tick, int maxTicks, int maximumPotentialTicks) {

		if (tick % 40 == 0) {
			center.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.5f, 0.75f);
		}

		double maxRotationDelta = 20;
		double rotationDelta = -maxRotationDelta * (maxTicks - tick) / maximumPotentialTicks;
		mRotation += rotationDelta;
		mIncreasedRotation += (rotationDelta + maxRotationDelta) / 2;

		Location raisedLoc = center.clone().add(0, 0.25, 0);

		float initialRotation = center.getYaw() + 180;

		double centerSymbolSize = radius * 0.45;
		double smallRadius = 0.2 * radius;
		double arcCut = Math.toDegrees(2 * Math.asin(smallRadius / radius / 2));

		// big circle on the ground
		new PPCircle(Particle.ENCHANTMENT_TABLE, raisedLoc.clone().add(0, 0.5, 0), radius)
			.ringMode(true)
			.arcDegree(mIncreasedRotation, mIncreasedRotation + 360)
			.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER / 6)
			.directionalMode(true).delta(0, -0.5, 0).extra(1)
			.anglePredicate(angle -> Math.abs(((((angle - initialRotation - 90 + 60) % 120) + 120) % 120) - 60) >= arcCut)
			.spawnAsPlayerActive(player);

		// raised ring effect (big circle, but in the air)
		new PPCircle(Particle.ENCHANTMENT_TABLE, raisedLoc.clone().add(0, 2, 0), radius)
			.ringMode(true)
			.arcDegree(mIncreasedRotation, mIncreasedRotation + 360)
			.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER / 8)
			.directionalMode(true).delta(0, -2, 0).extra(1)
			.spawnAsPlayerActive(player);

		// This loop is mostly copied from ArcaneScorchedEarthCS but changed too much to make using a common method viable
		boolean drawStaticSymbols = tick % 10 < 5;
		for (int i = 0; i < 3; i++) {
			double currentAngle = initialRotation + 90 + i * 120;
			Vector dir = VectorUtils.rotateYAxis(new Vector(1, 0, 0), currentAngle);
			Location lineStartLoc = raisedLoc.clone().add(dir.clone().multiply(radius));

			// triangle
			Location nextLineStart = raisedLoc.clone().add(VectorUtils.rotateYAxis(new Vector(radius, 0, 0), currentAngle + 120));
			Vector dirToNext = nextLineStart.clone().toVector().subtract(lineStartLoc.toVector()).normalize();
			new PPLine(Particle.ENCHANTMENT_TABLE,
				lineStartLoc.clone().add(0, 0.5, 0).add(dirToNext.clone().multiply(smallRadius)),
				nextLineStart.clone().add(0, 0.5, 0).add(dirToNext.clone().multiply(-smallRadius)))
				.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER / 4)
				.directionalMode(true).delta(0, -0.5, 0).extra(1)
				.offset((tick % 20) / 20.0)
				.spawnAsPlayerActive(player);

			// smaller circles, rotating at increased speed
			new PPCircle(Particle.ENCHANTMENT_TABLE, lineStartLoc.clone().add(0, 0.5, 0), smallRadius)
				.ringMode(true).countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER / 4)
				.arcDegree(mIncreasedRotation, mIncreasedRotation + 360)
				.directionalMode(true).delta(0, -0.5, 0).extra(1)
				.spawnAsPlayerActive(player);

			if (drawStaticSymbols) {
				// accent symbol in small circles
				ArcanePotionsCS.WATER.draw(new ArcanePotionsCS.Transform(lineStartLoc, smallRadius * 0.8, initialRotation), Particle.SCRAPE, player);

				// small side symbols
				Location symbolLoc = raisedLoc.clone().add(dir.clone().multiply(-0.75 * radius));
				double sideSymbolSize = radius * 0.15;
				mSideSymbols.get(i).draw(new ArcanePotionsCS.Transform(symbolLoc, sideSymbolSize, initialRotation).skipBelowMultiplier(0.325), Particle.SCRAPE, player);
			}
		}

		// center symbol
		if (drawStaticSymbols) {
			// move slightly off-center to fit better
			Location symbolLoc = raisedLoc.clone().add(VectorUtils.rotateYAxis(new Vector(0, 0, 0.05 * radius), initialRotation));
			ArcanePotionsCS.ANTIMONY.draw(new ArcanePotionsCS.Transform(symbolLoc, centerSymbolSize, initialRotation), Particle.SCRAPE, player);
		}

		// rays
		double centerSymbolSizeRingRadius = centerSymbolSize * 1.05;
		new PPCircle(Particle.END_ROD, raisedLoc, centerSymbolSizeRingRadius)
			.ringMode(true)
			.count(8).minimumCount(3).maximumMultiplier(1) // 3 to 8 rays
			.arcDegree(mRotation, mRotation + 360)
			.directionalMode(true)
			.delta(1, 0, 0)
			.extra((radius - centerSymbolSizeRingRadius) / 11)
			.rotateDelta(true)
			.spawnAsPlayerActive(player);

	}

	@Override
	public void effectOnKill(Player player, Location loc) {

	}

}
