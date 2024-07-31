package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class PrestigiousEsotericCS extends EsotericEnhancementsCS implements PrestigeCS {

	public static final String NAME = "Prestigious Enhancements";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
	private static final Particle.DustOptions BURN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 180, 0), 1.0f);
	private static final Particle.DustOptions[] DATA = {
		LIGHT_COLOR,
		GOLD_COLOR,
		BURN_COLOR
	};

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The perfect permutation",
			"forms a divine geometry."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.HONEY_BOTTLE;
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
	public String getAberrationName() {
		return "Prestigious Aberration";
	}

	@Override
	public void esotericSummonEffect(Player player, Creeper aberration) {
		final double radius = 3.6;
		final double theta = FastUtils.RANDOM.nextDouble(120);
		final double dRadius = -1.2;
		final double dTheta = 30;
		final long interval = 3;
		final Location loc = aberration.getLocation();

		for (int i = 0; i < DATA.length; i++) {
			final double mRadius = radius + i * dRadius;
			final double mTheta = theta + i * dTheta;
			final int iter = i;
			new BukkitRunnable() {
				@Override
				public void run() {
					new PPLine(Particle.REDSTONE,
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta), 0.25, mRadius * FastUtils.sinDeg(mTheta)),
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 120), 0.25, mRadius * FastUtils.sinDeg(mTheta + 120)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE,
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 120), 0.25, mRadius * FastUtils.sinDeg(mTheta + 120)),
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 240), 0.25, mRadius * FastUtils.sinDeg(mTheta + 240)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE,
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 240), 0.25, mRadius * FastUtils.sinDeg(mTheta + 240)),
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta), 0.25, mRadius * FastUtils.sinDeg(mTheta)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(player);
				}
			}.runTaskLater(Plugin.getInstance(), interval * i);
		}
	}

	@Override
	public void periodicEffects(Player player, Creeper aberration, int ticks) {
		if (ticks % 2 == 0) {
			new PartialParticle(Particle.REDSTONE, LocationUtils.getEntityCenter(aberration), 2)
				.delta(0.4, 0.7, 0.4)
				.data(new Particle.DustOptions(Color.WHITE, 1.2f))
				.spawnAsPlayerActive(player);
			new PartialParticle(Particle.REDSTONE, LocationUtils.getEntityCenter(aberration), 1)
				.delta(0.4, 0.7, 0.4)
				.data(new Particle.DustOptions(Color.YELLOW, 1.5f))
				.spawnAsPlayerActive(player);
		}
	}

}
