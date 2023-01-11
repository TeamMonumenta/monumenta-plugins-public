package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class LightningFinisher implements EliteFinisher {

	public static final String NAME = "Lightning";

	public static final Particle.DustOptions DUST_GRAY_LARGE = new Particle.DustOptions(Color.fromRGB(51, 51, 51), 5);
	public static final Particle.DustOptions DUST_YELLOW_LARGE = new Particle.DustOptions(Color.fromRGB(255, 255, 64), 1.25f);
	public static final Particle.DustOptions DUST_YELLOW_SMALL = new Particle.DustOptions(DUST_YELLOW_LARGE.getColor(), 0.75f);
	public static final Particle.DustOptions DUST_LIGHT_YELLOW_SMALL = new Particle.DustOptions(Color.fromRGB(255, 255, 128), 0.75f);
	private static final int SHOCK_VERTICAL_RANGE = 10;
	private static final int SHOCK_DELAY_TICKS = Constants.TICKS_PER_SECOND;

	// Yeah, this is straight ripped and tweaked from Kaul's LightningStrike
	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		Location strikeLocation = killedMob.getLocation();
		strikeLocation.setY(strikeLocation.getY());

		// P: Danger, tall markers
		new PPPillar(Particle.REDSTONE, strikeLocation, SHOCK_VERTICAL_RANGE).count(2 * SHOCK_VERTICAL_RANGE).data(DUST_YELLOW_SMALL).spawnAsPlayerActive(p);
		new PPPillar(Particle.FIREWORKS_SPARK, strikeLocation.clone().subtract(0, SHOCK_VERTICAL_RANGE, 0), SHOCK_VERTICAL_RANGE)
			.count(2 * SHOCK_VERTICAL_RANGE).spawnAsPlayerActive(p);


		// S: Thunder & distant sparks
		world.playSound(
			strikeLocation,
			Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
			SoundCategory.HOSTILE,
			1,
			1.25f
		);
		world.playSound(
			strikeLocation,
			Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
			SoundCategory.HOSTILE,
			0.75f,
			1.5f
		);
		world.playSound(
			strikeLocation,
			Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
			SoundCategory.HOSTILE,
			1,
			1.75f
		);
		world.playSound(
			strikeLocation,
			Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
			SoundCategory.HOSTILE,
			0.75f,
			1.75f
		);

		// /particle dust 0.2 0.2 0.2 5 ~ ~10 ~ 1.5 0.25 1.5 0 5
		PartialParticle stormClouds = new PartialParticle(
			Particle.REDSTONE,
			strikeLocation.clone().add(0, SHOCK_VERTICAL_RANGE, 0),
			5,
			1.5,
			0.25,
			1.5,
			0,
			DUST_GRAY_LARGE
		);

		BukkitRunnable lightningRunnable = new BukkitRunnable() {
			int mCountdownTicks = SHOCK_DELAY_TICKS;

			@Nullable BukkitRunnable mInternalParticleRunnable;

			@Override
			public void run() {
				// P: Dark storm clouds gather
				stormClouds.spawnAsBoss();

				// Count of the tick this run, last being 1
				if (mCountdownTicks == PPLightning.ANIMATION_TICKS) {
					// P: Lightning starts
					PPLightning lightning = new PPLightning(Particle.END_ROD, strikeLocation).init(SHOCK_VERTICAL_RANGE, 2.5, 0.3, 0.15);
					lightning.spawnAsPlayerActive(p);
					mInternalParticleRunnable = lightning.runnable();

					// S: Electricity courses
					world.playSound(
						strikeLocation,
						Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
						SoundCategory.PLAYERS,
						1,
						1.25f
					);
					world.playSound(
						strikeLocation,
						Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
						SoundCategory.PLAYERS,
						1,
						1.5f
					);
				}

				// Count of the tick next run.
				// If next run would be tick #0
				if (--mCountdownTicks < 1) {
					cancel();

					// P: Lightning hits & sparks
					// /particle firework ~ ~ ~ 0.9 1.8 0.9 0.3 0
					PartialParticle sparks = new PartialParticle(
						Particle.FIREWORKS_SPARK,
						strikeLocation,
						20,
						0.9,
						1.8,
						0.9,
						0.25,
						null,
						true,
						0.05
					);
					sparks.deltaVariance(true, false, true);
					sparks.mVaryPositiveY = true;
					sparks.spawnAsBoss();

					// /particle dust 1 1 0.5 0.75 ~ ~ ~ 1.5 1.5 1.5 0 100
					new PartialParticle(
						Particle.REDSTONE,
						strikeLocation,
						50,
						1.5,
						1.5,
						1.5,
						DUST_LIGHT_YELLOW_SMALL
					).spawnAsBoss();

					// S: Booms & fire ignites
					world.playSound(
						strikeLocation,
						Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
						SoundCategory.HOSTILE,
						0.75f,
						0.5f
					);
					world.playSound(
						strikeLocation,
						Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
						SoundCategory.HOSTILE,
						0.75f,
						0.75f
					);
					world.playSound(
						strikeLocation,
						Sound.ENTITY_BLAZE_SHOOT,
						SoundCategory.HOSTILE,
						1,
						0.75f
					);
					world.playSound(
						strikeLocation,
						Sound.ENTITY_BLAZE_SHOOT,
						SoundCategory.HOSTILE,
						1,
						1
					);
				}
			}
		};
		lightningRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_STAR;
	}

}
