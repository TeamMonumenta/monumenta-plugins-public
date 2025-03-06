package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Bukkit;
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

public class SaltTheEarthCS extends BlizzardCS {
	public static final String NAME = "Salt the Earth";
	public static final Color COLOR = Color.fromRGB(69, 1, 17);
	private static final Color SOUL_COLOR = Color.fromRGB(130, 15, 52);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BLIZZARD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SOUL_SOIL;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A curse upon the land. May these wretched",
			"monstrosities never rise to torment us again.");
	}

	@Override
	public void onCast(Player player, World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.2f, 2.0f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.75f, 1f);
	}

	@Override
	public void tick(Player player, Location loc, int ticks, double radius) {
		new PartialParticle(Particle.CRIMSON_SPORE, loc, 2).delta(radius / 2).extra(0.05).spawnAsPlayerActive(player);
		if (ticks % 15 == 0 || ticks % 5 == 1) {
			new PartialParticle(Particle.BLOCK_CRACK, loc, 25, radius, 0, radius, 0.2, Bukkit.createBlockData(Material.CRIMSON_NYLIUM)).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SMOKE_LARGE, loc, 10, radius, 0, radius, 0.2).spawnAsPlayerActive(player);
			Location orbLoc = LocationUtils.varyInCircle(loc, radius * 0.9);
			createOrb(new Vector(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-0.5, 0.5),
					FastUtils.randomDoubleInRange(-1, 1)), orbLoc,
				player, orbLoc.clone().add(
					FastUtils.randomDoubleInRange(-2, 2), FastUtils.randomDoubleInRange(5, 7),
					FastUtils.randomDoubleInRange(-2, 2)
				));
		}
		double angle = ticks * 3 % 60;
		new PPCircle(Particle.REDSTONE, loc, Math.min(radius, radius * ticks * 0.2)).data(new Particle.DustOptions(COLOR, 2f)).count(6)
			.arcDegree(angle, angle + 360).spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, loc, Math.min(radius * 0.5, radius * ticks * 0.2)).data(new Particle.DustOptions(COLOR, 1.5f)).count(3)
			.arcDegree(-angle, -angle - 360).spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, loc, Math.min(radius * 0.25, radius * ticks * 0.2)).data(new Particle.DustOptions(COLOR, 1.25f)).count(2)
			.arcDegree(angle, angle + 360).spawnAsPlayerActive(player);
	}

	private void createOrb(Vector dir, Location loc, Player mPlayer, @Nullable Location optLoc) {
		new BukkitRunnable() {
			final Location mL = loc.clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = optLoc != null ? optLoc : loc;
				if (!to.getWorld().equals(mL.getWorld())) {
					cancel();
					return;
				}

				for (int i = 0; i < 3; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.085;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.2) {
						mD.normalize().multiply(0.2);
					}

					mL.add(mD);


					new PartialParticle(Particle.REDSTONE, mL, 2, 0.075, 0.075, 0.075, 0,
						new Particle.DustOptions(SOUL_COLOR, 1.25f))
						.spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, mL, 1, 0.1, 0.1, 0.1, 0.035)
						.spawnAsPlayerActive(mPlayer);


					if (mT > 5 && mL.distance(to) < 0.35) {
						new PartialParticle(Particle.SMOKE_NORMAL, mL, 25, 0, 0, 0, 0.1F)
							.spawnAsPlayerActive(mPlayer);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
