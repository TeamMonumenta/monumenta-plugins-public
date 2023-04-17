package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
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

public class CosmicCleaveCS extends DeadlyRondeCS {
	private int mMode = 0;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Three slashes.",
			"Tear the skies asunder."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.AMETHYST_CLUSTER;
	}

	@Override
	public @Nullable String getName() {
		return "Cosmic Cleave";
	}

	@Override
	public void rondeHitEffect(World world, Player player, double radius, double rondeBaseRadius, boolean lv2) {
		Location loc = player.getLocation();
		Vector viewDirection = loc.getDirection();
		switch (mMode) {
			case 0 -> {
				drawArc(player, viewDirection, Math.PI / 6);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.8f, 1.7f);
				mMode = 1;
			}
			case 1 -> {
				drawArc(player, viewDirection, -Math.PI / 6);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.8f, 1.7f);
				mMode = 2;
			}
			default -> {
				drawArc(player, viewDirection, Math.PI / 6);
				drawArc(player, viewDirection, -Math.PI / 6);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 0.8f, 2.0f);
				mMode = 0;
			}
		}
	}

	@Override
	public void rondeTickEffect(Player player, int charges, int mTicks) {
		// This is intentionally empty :)
	}

	@Override
	public void rondeGainStackEffect(Player player) {
		World world = player.getWorld();
		Location loc = player.getLocation();
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.8f, 2.0f);
		new PartialParticle(Particle.REDSTONE, loc.add(0, 1, 0), 50, 0.35, 0.5, 0.35, 0, new Particle.DustOptions(rollCosmicColor(), 0.75f)).spawnAsPlayerActive(player);
	}

	private void drawArc(Player player, Vector viewDirection, double tilt) {
		Vector viewNormal = viewDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();

		new BukkitRunnable() {
			double mAngle = -Math.PI / 5;
			@Override
			public void run() {
				if (mAngle > Math.PI / 5) {
					this.cancel();
				}

				for (int i = 0; i < 4; i++) {
					Vector offsetX = viewDirection.clone().multiply(2.3 * FastUtils.cos(Math.signum(tilt) * mAngle));
					Vector offsetZ = viewNormal.clone().multiply(2.3 * FastUtils.sin(Math.signum(tilt) * mAngle));
					drawCosmic(player, player.getEyeLocation().add(offsetX.add(offsetZ).rotateAroundAxis(viewDirection, tilt)));
					mAngle += Math.PI / 30;
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void drawCosmic(Player player, Location loc) {
		if (FastUtils.randomIntInRange(0, 3) == 0) {
			new PartialParticle(Particle.CRIT_MAGIC, loc, 4, 0.1, 0.1, 0.1, 0).minimumCount(0).spawnAsPlayerActive(player);
		} else {
			new PartialParticle(Particle.REDSTONE, loc, 6, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(rollCosmicColor(), 0.6f)).minimumCount(0).spawnAsPlayerActive(player);
		}
	}

	private Color rollCosmicColor() {
		return Color.fromRGB(80 + FastUtils.randomIntInRange(0, 160), 80, 200);
	}
}
