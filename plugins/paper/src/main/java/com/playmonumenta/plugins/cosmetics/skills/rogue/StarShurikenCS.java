package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
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

public class StarShurikenCS extends DaggerThrowCS {
	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Concealed blades are one threat.",
			"Molten concealed blades are another."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.QUARTZ;
	}

	@Override
	public @Nullable String getName() {
		return "Star Shuriken";
	}

	@Override
	public void daggerCastSound(World world, Location loc) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.5f, 1.8f);
				if (mTicks == 2) {
					world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.5f, 1.5f);
				} else if (mTicks > 2) {
					world.playSound(loc, Sound.ITEM_AXE_WAX_OFF, SoundCategory.PLAYERS, 0.8f, 2f);
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void daggerParticle(Location startLoc, Location endLoc, Player player) {
		Vector direction = LocationUtils.getDirectionTo(endLoc, startLoc);
		double angle = Math.atan2(direction.getX(), direction.getZ()) + Math.PI / 2;
		for (int i = 0; i <= startLoc.distance(endLoc); i++) {
			Location starCentre = startLoc.clone().add(direction.clone().multiply(i));
			double angle2 = (-Math.PI / 3) * starCentre.clone().distance(player.getLocation());
			ArrayList<Vector> shuriken = StarCosmeticsFunctions.interpolatePolygon(StarCosmeticsFunctions.generateStarVertices(4, 0.25, 0.2, false, false), 1);

			for (Vector v : shuriken) {
				drawParticle(v.rotateAroundZ(angle2).rotateAroundY(angle).add(starCentre.toVector()).toLocation(player.getWorld()), player);
			}
		}
	}

	@Override
	public void daggerHitEffect(World world, Location loc, Location bLoc, Player mPlayer) {
		new PartialParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1).spawnAsPlayerActive(mPlayer).minimumCount(0);
		new PartialParticle(Particle.FLAME, bLoc, 3, 0.3, 0.3, 0.3, 0.1).spawnAsPlayerActive(mPlayer).minimumCount(0);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.8f, 1.6f);
	}

	private void drawParticle(Location location, Player player) {
		new PartialParticle(Particle.REDSTONE, location, 2, 0.01, 0.01, 0.01, 0, new Particle.DustOptions(Color.fromRGB(255, 200, 50), 0.4f)).minimumCount(0)
			.spawnAsPlayerActive(player);
	}
}
