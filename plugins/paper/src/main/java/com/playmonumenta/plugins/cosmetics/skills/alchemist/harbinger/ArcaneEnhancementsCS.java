package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ArcaneEnhancementsCS extends EsotericEnhancementsCS {

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public @Nullable String getName() {
		return "Arcane Enhancements";
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Ripping out a piece of a being's soul and",
			"making it fight against its original owner",
			"is often very effective, if somewhat cruel.");
	}

	@Override
	public String getAberrationName() {
		return "Arcane Aberration";
	}

	@Override
	public void esotericSummonEffect(Player player, Creeper aberration) {
		aberration.getWorld().playSound(aberration.getLocation(), Sound.ENTITY_ALLAY_DEATH, 1.5f, 0.8f);

		// only few particles, spawning an aberration requires 2 potions or an amalgam, so there's lots of particles already
		new PartialParticle(Particle.SOUL, aberration.getEyeLocation().add(0, -0.2, 0), 10)
			.delta(aberration.getWidth() / 3)
			.spawnAsPlayerActive(player);
	}

	@Override
	public void periodicEffects(Player player, Creeper aberration, int ticks) {
		if (ticks % 16 == 0) {
			ArcanePotionsCS.drawSingleCircle(player, aberration.getEyeLocation().add(0, -0.4, 0), 0.5, null, Particle.SCRAPE);
		}
		if (ticks % 4 == 0) {
			new PPPeriodic(Particle.SOUL, aberration.getEyeLocation())
				.manualTimeOverride(ticks / 4)
				.delta(aberration.getWidth() / 3)
				.spawnAsPlayerActive(player);
		}
	}

	@Override
	public void explosionEffects(Player player, LivingEntity aberration, double radius) {
		aberration.getWorld().playSound(aberration.getLocation(), Sound.ENTITY_ALLAY_DEATH, 1.5f, 1.3f);

		Location loc = aberration.getLocation().add(0, 0.25, 0);
		ArcanePotionsCS.drawSingleCircle(player, loc, radius, null, Particle.SCRAPE);
		new PPCircle(Particle.SOUL, loc, radius)
			.countPerMeter(3)
			.delta(0, 0.5, 0)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.SOUL, loc, radius - 0.1)
			.count((int) (radius * radius * 10))
			.ringMode(false)
			.delta(0, 1, 0)
			.spawnAsPlayerActive(player);
	}

	@Override
	public void expireEffects(Player player, LivingEntity aberration) {
		aberration.getWorld().playSound(aberration.getLocation(), Sound.ENTITY_ALLAY_DEATH, 1, 0.5f);
	}

}
