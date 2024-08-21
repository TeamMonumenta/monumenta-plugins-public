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
import org.bukkit.SoundCategory;
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

	/**
	 * Plays a sound from the aberration's location while making sure the caster always hears it (as long as they can see the creeper)
	 */
	private static void playSound(Player player, LivingEntity aberration, float volume, float pitch) {
		if (player.getWorld() == aberration.getWorld() && player.getLocation().distanceSquared(aberration.getLocation()) > 5 * 5) {
			// if far from the caster, play normal sound for other players and a closer one for the caster so that they can always hear it
			for (Player p : aberration.getTrackedPlayers()) {
				if (p != player) {
					// Normal sound for other players
					p.playSound(aberration.getLocation(), Sound.ENTITY_ALLAY_DEATH, SoundCategory.PLAYERS, volume, pitch);
				} else {
					// Play sound at a point 5 meters from the caster in the direction of the aberration
					Location soundLoc = player.getLocation().add(aberration.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(5));
					player.playSound(soundLoc, Sound.ENTITY_ALLAY_DEATH, SoundCategory.PLAYERS, volume, pitch);
				}
			}
		} else {
			// If close just play the sound normally
			aberration.getWorld().playSound(aberration.getLocation(), Sound.ENTITY_ALLAY_DEATH, SoundCategory.PLAYERS, volume, pitch);
		}
	}

	@Override
	public void esotericSummonEffect(Player player, Creeper aberration) {
		playSound(player, aberration, 0.75f, 0.8f);

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
		playSound(player, aberration, 0.9f, 1.3f);

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
		playSound(player, aberration, 0.75f, 0.5f);
	}

}
