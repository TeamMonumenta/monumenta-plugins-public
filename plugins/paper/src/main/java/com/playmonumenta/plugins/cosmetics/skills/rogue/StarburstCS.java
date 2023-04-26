package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
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

public class StarburstCS extends ViciousCombosCS {
	@Override
	public Material getDisplayItem() {
		return Material.END_CRYSTAL;
	}

	@Override
	public @Nullable String getName() {
		return "Starburst";
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Sacrifice of the highest calibre",
			"will always please the stars."
		);
	}

	private static final Color[] colors = new Color[] {
		Color.fromRGB(255, 90, 90),
		Color.fromRGB(255, 180, 90),
		Color.fromRGB(255, 225, 90),
		Color.fromRGB(170, 255, 90),
		Color.fromRGB(90, 255, 150),
		Color.fromRGB(90, 255, 255),
		Color.fromRGB(100, 90, 255),
		Color.fromRGB(160, 90, 255),
	};

	@Override
	public void comboOnElite(World world, Location loc, Player player, double range, LivingEntity target) {
		Vector starCentre = target.getEyeLocation().toVector().add(new Vector(0, 0.6, 0));

		Vector direction = player.getLocation().clone().subtract(starCentre.clone()).getDirection();
		double angle = Math.atan2(direction.getX(), direction.getZ());

		ArrayList<Vector> starFull = generateStar(starCentre, 3.6, 0.8, 3, angle);

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				if (mTicks < 16 && mTicks % 2 == 0) {
					for (Vector v : starFull) {
						drawParticle(scaleVector(v, starCentre, (double) mTicks / 16).toLocation(world), player, colors[mTicks / 2]);
					}
					world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1f, 1.4f - (float)mTicks / 20);
					world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1f, 1.4f - (float)mTicks / 20);
					world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.4f, 1.6f - (float)mTicks / 20);
				} else if (mTicks == 16) {
					for (Vector v : starFull) {
						drawStar(v.toLocation(world), player);
					}
					world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1f, 0.8f);
					world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.8f, 1.6f);
				}
				if (mTicks > 16) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void comboOnKill(World world, Location loc, Player player, double range, LivingEntity target) {
		double haloAngle = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
		Vector starCentre = target.getEyeLocation().toVector().add(new Vector(0.8 * FastUtils.cos(haloAngle), 0.25, 0.8 * FastUtils.sin(haloAngle)));

		Vector direction = player.getLocation().clone().subtract(starCentre.clone()).getDirection();
		double angle = Math.atan2(direction.getX(), direction.getZ());

		ArrayList<Vector> starFull = generateStar(starCentre, 0.3, 0.06, 2, angle);

		Color c = colors[FastUtils.randomIntInRange(0, 2)];
		for (Vector v : starFull) {
			drawParticle(v.toLocation(world), player, c);
		}

		world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 0.8f, 1.6f);
	}

	private void drawParticle(Location location, Player player, Color color) {
		new PartialParticle(Particle.REDSTONE, location, 4, 0.02, 0.02, 0.02, 0.4, new Particle.DustOptions(color, 0.5f)).minimumCount(0)
			.spawnAsPlayerActive(player);
	}

	private void drawStar(Location location, Player player) {
		new PartialParticle(Particle.END_ROD, location, 1, 0, 0, 0, 0)
			.spawnAsPlayerActive(player);
	}

	private Vector scaleVector(Vector vector, Vector centre, double scale) {
		return vector.clone().subtract(centre).multiply(scale).add(centre);
	}

	private ArrayList<Vector> generateStar(Vector starCentre, double starSize, double starHeaviness, double particlesPerEdge, double rotation) {
		Vector[][] starPoints = new Vector[][] {
			new Vector[] {
				new Vector(starSize, 0, 0),
				new Vector(starHeaviness, starHeaviness, starHeaviness),
				new Vector(starHeaviness, -starHeaviness, starHeaviness),
				new Vector(starHeaviness, starHeaviness, -starHeaviness),
				new Vector(starHeaviness, -starHeaviness, -starHeaviness)
			},
			new Vector[] {
				new Vector(-starSize, 0, 0),
				new Vector(-starHeaviness, starHeaviness, starHeaviness),
				new Vector(-starHeaviness, -starHeaviness, starHeaviness),
				new Vector(-starHeaviness, starHeaviness, -starHeaviness),
				new Vector(-starHeaviness, -starHeaviness, -starHeaviness)
			},
			new Vector[] {
				new Vector(0, starSize, 0),
				new Vector(starHeaviness, starHeaviness, starHeaviness),
				new Vector(starHeaviness, starHeaviness, -starHeaviness),
				new Vector(-starHeaviness, starHeaviness, starHeaviness),
				new Vector(-starHeaviness, starHeaviness, -starHeaviness),
			},
			new Vector[] {
				new Vector(0, -starSize, 0),
				new Vector(starHeaviness, -starHeaviness, starHeaviness),
				new Vector(starHeaviness, -starHeaviness, -starHeaviness),
				new Vector(-starHeaviness, -starHeaviness, starHeaviness),
				new Vector(-starHeaviness, -starHeaviness, -starHeaviness),
			},
			new Vector[] {
				new Vector(0, 0, starSize),
				new Vector(starHeaviness, starHeaviness, starHeaviness),
				new Vector(starHeaviness, -starHeaviness, starHeaviness),
				new Vector(-starHeaviness, starHeaviness, starHeaviness),
				new Vector(-starHeaviness, -starHeaviness, starHeaviness)
			},
			new Vector[] {
				new Vector(0, 0, -starSize),
				new Vector(starHeaviness, starHeaviness, -starHeaviness),
				new Vector(starHeaviness, -starHeaviness, -starHeaviness),
				new Vector(-starHeaviness, starHeaviness, -starHeaviness),
				new Vector(-starHeaviness, -starHeaviness, -starHeaviness)
			}
		};

		ArrayList<Vector> starFull = new ArrayList<>();
		float step = (float) (1.0f / (particlesPerEdge + 1));

		for (int i = 0; i < 6; i++) {
			starFull.add(starPoints[i][0]);
			for (int j = 0; j < 4; j++) {
				if (i < 2) {
					starFull.add(starPoints[i][j + 1]);
				}
				for (int k = 1; k <= particlesPerEdge; k++) {
					float t = step * k;
					starFull.add(
						starPoints[i][0].clone().subtract(starPoints[i][j + 1]).multiply(t).add(starPoints[i][j + 1])
					);
				}
			}
		}

		for (Vector v : starFull) {
			v.rotateAroundY(rotation).add(starCentre);
		}

		return starFull;
	}
}
