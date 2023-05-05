package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.particle.PPCircle;
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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SolarChainsCS extends SkirmisherCS {
	@Override
	public Material getDisplayItem() {
		return Material.CHAIN;
	}

	@Override
	public @Nullable String getName() {
		return "Solar Chains";
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Bring molten wrath to your enemies,",
			"strike with chains, face the fury."
		);
	}

	@Override
	public void aesthetics(Player mPlayer, Location loc, World world, LivingEntity enemy) {
		double halfHeight = enemy.getHeight() / 2;
		double halfWidth = enemy.getWidth() / 2;
		world.playSound(loc, Sound.BLOCK_IRON_DOOR_CLOSE, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 0.6f, 1.5f);
		world.playSound(loc, Sound.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.6f, 0.8f);

		if (FastUtils.randomIntInRange(1, 5) <= 4) {
			new PPCircle(Particle.CRIT, loc, halfWidth * 2).delta(0.02).count(40).spawnAsPlayerActive(mPlayer);
		} else {
			double starSize = halfWidth * 4;
			int starVertices = FastUtils.randomIntInRange(3, 7);

			ArrayList<Vector> starFull = StarCosmeticsFunctions.interpolatePolygon(StarCosmeticsFunctions.generateStarVertices(starVertices, starSize, 0.35, true, true), 2);

			for (Vector v : starFull) {
				new PartialParticle(Particle.CRIT, loc.clone().add(v), 3).delta(0.02).spawnAsPlayerActive(mPlayer);
			}
		}
		loc.add(0, halfHeight, 0);
		new PartialParticle(Particle.ELECTRIC_SPARK, loc, 6, halfWidth, halfHeight, halfWidth, 0.05).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 20, halfWidth, halfHeight, halfWidth, 0, new Particle.DustOptions(Color.fromRGB(255, 200, 70), 0.6f)).spawnAsPlayerActive(mPlayer);
	}
}
