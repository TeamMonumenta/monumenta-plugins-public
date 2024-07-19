package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
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
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ArcaneScorchedEarthCS extends ScorchedEarthCS {

	public static final String NAME = "Arcane Scorched Earth";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"When an Alchemist wants to set the world",
			"on fire, that is not just an expression.",
			"The land will burn unnaturally long,",
			"and any attempts to quench the fire",
			"will make it burn ever hotter.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private List<ArcanePotionsCS.Symbol> mSideSymbols = List.of();

	@Override
	public void landEffects(Player player, Location loc, double radius, int duration) {
		World world = player.getWorld();

		// sounds
		world.playSound(loc, "minecraft:block.amethyst_block.resonate", 2.0f, 0.4f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.2f, 0.4f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.8f, 0.4f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.6f, 0.8f);
		world.playSound(loc, Sound.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.PLAYERS, 1.8f, 0.4f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.2f, 0.4f);
		world.playSound(loc, Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 2.0f, 0.4f);
		world.playSound(loc, Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 2.5f, 0.8f);
		world.playSound(loc, Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 3.0f, 0.4f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.5f, 0.4f);
		world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.3f, 0.7f);

		// extra landing particles
		double delta = (radius - 1) / 2;
		double height = 0;
		Location raisedLoc = loc.clone().add(0, height, 0);
		new PartialParticle(Particle.SMOKE_NORMAL, raisedLoc, 50, delta, height, delta, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, raisedLoc, 15, delta, height, delta, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.LAVA, raisedLoc, 25, delta, height, delta, 0).spawnAsPlayerActive(player);
	}

	@Override
	public ArcaneScorchedEarthCS copyForActiveInstance() {
		ArcaneScorchedEarthCS copy = new ArcaneScorchedEarthCS();
		copy.mSideSymbols = new ArrayList<>(ArcanePotionsCS.SMALL_SYMBOLS);
		Collections.shuffle(copy.mSideSymbols, FastUtils.RANDOM);
		return copy;
	}

	@Override
	public void activeEffects(Player player, Location loc, double radius, int timeRemaining, int duration) {
		// sound
		if (timeRemaining > 60 && timeRemaining % 30 < 5) {
			AbilityUtils.playPassiveAbilitySound(loc, Sound.BLOCK_FIRE_AMBIENT, 1, 0.5f);
		}

		int totalTicks = duration - timeRemaining;
		double maxRotPerTick = 1.5;
		double currentRotPerTick = maxRotPerTick * timeRemaining / duration;
		double rotation = maxRotPerTick * duration / 2 - currentRotPerTick * timeRemaining / 2;
		double height = 0.25;

		Location raisedLoc = loc.clone().add(0, height, 0);

		float initialRotation = loc.getYaw() + 180;

		double smallRadius = 0.2 * radius;
		double arcCut = Math.toDegrees(2 * Math.asin(smallRadius / radius / 2));

		// rotating ring
		new PPCircle(Particle.FLAME, raisedLoc, radius)
			.arcDegree(initialRotation + rotation, initialRotation + rotation + 360)
			.countPerMeter(0.5)
			.directionalMode(true)
			.rotateDelta(true)
			.delta(-0.2, 0, 1)
			.extra(Math.toRadians(currentRotPerTick) * radius * 1.4)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.ENCHANTMENT_TABLE, raisedLoc.clone().add(0, 0.5, 0), radius)
			.arcDegree(initialRotation + rotation, initialRotation + rotation + 360)
			.countPerMeter(0.5)
			.directionalMode(true)
			.delta(0, -0.5, 0)
			.extra(1)
			.anglePredicate(angle -> Math.abs(((((angle - initialRotation + 90 + 60) % 120) + 120) % 120) - 60) >= arcCut)
			.spawnAsPlayerActive(player)
			// spawn a second particle slightly behind to get a denser trail
			.arcDegree(initialRotation + rotation - 5 * currentRotPerTick / 2, initialRotation + rotation + 360 - 5 * currentRotPerTick / 2)
			.spawnAsPlayerActive(player);

		// This loop is mostly copied from ArcanePotionsCS, but with enough changes to make refactoring it into one method tricky
		boolean drawStaticSymbols = totalTicks % 10 < 5;
		for (int i = 0; i < 3; i++) {
			double currentAngle = initialRotation - 90 + i * 120;
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
				.offset((timeRemaining % 20) / 20.0)
				.spawnAsPlayerActive(player);

			// smaller circles
			new PPCircle(Particle.ENCHANTMENT_TABLE, lineStartLoc.clone().add(0, 0.5, 0), smallRadius)
				.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER / 4)
				.offset((timeRemaining % 20) / 20.0)
				.directionalMode(true).delta(0, -0.5, 0).extra(1)
				.spawnAsPlayerActive(player);

			if (drawStaticSymbols) {
				// accent symbol in small circles
				ArcanePotionsCS.FIRE.draw(new ArcanePotionsCS.Transform(lineStartLoc, smallRadius * 0.8, initialRotation), Particle.WAX_ON, player);

				// small side symbols
				Location symbolLoc = raisedLoc.clone().add(dir.clone().multiply(-0.75 * radius));
				double sideSymbolSize = radius * 0.15;
				mSideSymbols.get(i).draw(new ArcanePotionsCS.Transform(symbolLoc, sideSymbolSize, initialRotation).skipBelowMultiplier(0.325), Particle.WAX_ON, player);
			}
		}

		// Phlogiston symbol
		if (drawStaticSymbols) {
			// move slightly off-center to fit better
			Location symbolLoc = raisedLoc.clone().add(VectorUtils.rotateYAxis(new Vector(0, 0, -0.05 * radius), initialRotation));
			ArcanePotionsCS.PHLOGISTON.draw(new ArcanePotionsCS.Transform(symbolLoc, radius / 2, initialRotation), Particle.WAX_ON, player);
		}

		// rotating fire within
		new PPCircle(Particle.SMALL_FLAME, loc.clone().add(0, height / 2, 0), radius - 0.1)
			.ringMode(false)
			.count((int) Math.ceil(5 * radius))
			.directionalMode(true)
			.rotateDelta(true)
			.delta(-0.2, 0, 1)
			.extra(Math.toRadians(currentRotPerTick) * radius * 1.4)
			.spawnAsPlayerActive(player);

		// other particles
		double delta = (radius - 1) / 2;
		new PartialParticle(Particle.SMOKE_LARGE, raisedLoc, 3, delta, height / 2, delta, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.LAVA, raisedLoc, 3, delta, height / 2, delta, 0).spawnAsPlayerActive(player);

	}

	@Override
	public void damageEffect(LivingEntity entity, Player alchemist) {
		BoundingBox boundingBox = entity.getBoundingBox();
		Location loc = entity.getLocation().clone().add(0, boundingBox.getHeight() / 2, 0);
		new PartialParticle(Particle.FLAME, loc, 5).delta(boundingBox.getWidthX() / 2, boundingBox.getHeight() / 2, boundingBox.getWidthZ() / 2).extra(0.05).spawnAsPlayerActive(alchemist);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 5).delta(boundingBox.getWidthX() / 2, boundingBox.getHeight() / 2, boundingBox.getWidthZ() / 2).spawnAsPlayerActive(alchemist);
		new PartialParticle(Particle.LAVA, loc, 2).delta(boundingBox.getWidthX() / 2, boundingBox.getHeight() / 2, boundingBox.getWidthZ() / 2).spawnAsPlayerActive(alchemist);
	}

}
