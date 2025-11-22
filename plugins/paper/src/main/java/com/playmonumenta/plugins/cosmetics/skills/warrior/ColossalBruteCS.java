package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ColossalBruteCS extends BruteForceCS {
	//Delve theme

	public static final String NAME = "Colossal Brute";

	private static final Color TWIST_COLOR_BASE = Color.fromRGB(130, 66, 66);
	private static final Color TWIST_COLOR_TIP = Color.fromRGB(127, 0, 0);
	private static final Color COLO_COLOR_BASE = Color.fromRGB(186, 140, 22);
	private static final Color COLO_COLOR_TIP = Color.fromRGB(252, 217, 129);
	private static final double[] ANGLE = {240, 290, 270};
	private static final float[] PITCHES = {0.85f, 1.15f, 0.55f};
	private static final float[] GOLEM_PITCHES = {0.6f, 0.775f, 0.5f};

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Brutalize your attacks even further",
			"using a twisted, colossal force.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHERITE_AXE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void bruteOnDamage(Player player, World world, Location loc, double radius, int combo) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.75f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.75f, PITCHES[combo]);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1f, 0.65f);
		Location pLoc = player.getLocation().add(0, 1, 0);
		Vector dir = LocationUtils.getDirectionTo(loc, pLoc).multiply(2.15);
		loc.setDirection(dir);
		ParticleUtils.drawHalfArc(loc.clone().subtract(dir), 2.15, ANGLE[combo], -40, 140, 8, 0.2,
			(Location l, int ring, double angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
					new Particle.DustOptions(
						ParticleUtils.getTransition(COLO_COLOR_BASE, COLO_COLOR_TIP, ring / 8D),
						0.6f + (ring * 0.1f)
					))
					.spawnAsPlayerActive(player);
			});
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1f, GOLEM_PITCHES[combo]);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.65f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1f, 0.75f);
		if (combo == 2) {
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1f, 0.55f);
			new PartialParticle(Particle.SMOKE_LARGE, loc, 16, 0, 0, 0, 0.125)
				.spawnAsPlayerActive(player);
			new PartialParticle(Particle.SMOKE_NORMAL, loc, 40, 0, 0, 0, 0.15)
				.spawnAsPlayerActive(player);
		}
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 55, 0, 0, 0, 0.15)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, loc, 50, 0, 0, 0, 0.75)
			.spawnAsPlayerActive(player);
		new BukkitRunnable() {

			double mRadius = 0;
			final Location mL = loc.clone().subtract(0, 0.6, 0);
			final double RADIUS = Math.max(radius * 1.25, 0.5);

			@Override
			public void run() {

				for (int i = 0; i < 2; i++) {
					mRadius += 0.3;
					for (int degree = 0; degree < 360; degree += 5) {
						double radian = FastMath.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(radian) * mRadius, 0,
							FastUtils.sin(radian) * mRadius);
						Location loc = mL.clone().add(vec);
						new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0, 0, 0, 0,
							new Particle.DustTransition(
								ParticleUtils.getTransition(COLO_COLOR_BASE, COLO_COLOR_TIP, mRadius / RADIUS),
								ParticleUtils.getTransition(TWIST_COLOR_BASE, TWIST_COLOR_TIP, mRadius / RADIUS),
								0.8f
							))
							.spawnAsPlayerActive(player);
					}
				}

				if (mRadius >= RADIUS) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
