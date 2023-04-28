package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class GalacticCloakCS extends SmokescreenCS {
	private static final int SPIRAL_POINTS = 210;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Heaps of interstellar dust form the",
			"foundations of entire solar systems."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.WITHER_ROSE;
	}

	@Override
	public @Nullable String getName() {
		return "Galactic Cloak";
	}

	@Override
	public void smokescreenEffects(Player player, World world, Location loc) {
		ArrayList<Vector> spiralPoints = new ArrayList<>();
		for (double d = 0; d < SPIRAL_POINTS; d++) {
			spiralPoints.add(new Vector(loc.getX() + d / 35.0 * FastUtils.cos(d * Math.PI / 21.0), loc.getY() + 0.2, loc.getZ() + d / 35.0 * FastUtils.sin(d * Math.PI / 21.0)));
		}

		for (double d = 0; d < 2 * Math.PI; d += Math.PI / 20) {
			drawParticle(new Location(world, loc.getX() + 6 * FastUtils.cos(d), loc.getY(), loc.getZ() + 6 * FastUtils.sin(d)), player, rollColor());
		}

		new BukkitRunnable() {
			int mTicks = 0;
			int mPointsDrawn = 0;

			@Override
			public void run() {
				// Draw 20 - mTicks spiral points
				for (int i = 0; i < 20 - mTicks; i++) {
					drawParticle(spiralPoints.get(i + mPointsDrawn).toLocation(world), player, rollColor());
					if (i % 2 == 0) {
						drawCrit(spiralPoints.get(i + mPointsDrawn).toLocation(world), player);
					}
				}
				mPointsDrawn += 20 - mTicks;
				mTicks++;
				if (mTicks >= 20) {
					for (double d = 0; d < 2 * Math.PI; d += Math.PI / 20) {
						drawParticle(new Location(world, loc.getX() + 6 * FastUtils.cos(d), loc.getY(), loc.getZ() + 6 * FastUtils.sin(d)), player, rollColor());
					}
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1f, 0.5f);
	}

	@Override
	public void residualEnhanceEffects(Player player, World world, Location loc) {
		for (double d = 0; d < 2 * Math.PI; d += Math.PI / 20) {
			drawParticle(new Location(world, loc.getX() + 4 * FastUtils.cos(d), loc.getY(), loc.getZ() + 4 * FastUtils.sin(d)), player, rollColor());
		}
		new PartialParticle(Particle.CRIT_MAGIC, loc, 65, 3.5, 0.2, 3.5, 0.05).spawnAsPlayerActive(player);
		AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 0.3f, 1.5f);
	}

	private Color rollColor() {
		return Color.fromRGB(80 + FastUtils.randomIntInRange(0, 160), 80, 200);
	}

	private void drawParticle(Location location, Player player, Color color) {
		new PartialParticle(Particle.REDSTONE, location, 5, 0.03, 0, 0.03, 0, new Particle.DustOptions(color, 2.0f)).minimumCount(0)
			.spawnAsPlayerActive(player);
	}

	private void drawCrit(Location location, Player player) {
		new PartialParticle(Particle.CRIT_MAGIC, location, 5, 0.3, 0.05, 0.3, 0.075).minimumCount(0)
			.spawnAsPlayerActive(player);
	}
}
