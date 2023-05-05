package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
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

public class StarstruckCS extends ByMyBladeCS {
	@Override
	public Material getDisplayItem() {
		return Material.NETHER_STAR;
	}

	@Override
	public @Nullable String getName() {
		return "Starstruck";
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A single blast from the heavens",
			"can tear the world into two."
		);
	}

	@Override
	public void bmbDamage(World world, Player player, LivingEntity enemy, int level) {
		Vector starCentre = enemy.getLocation().clone().add(0, enemy.getHeight() + 0.8, 0).toVector();

		ArrayList<Vector> starFull = StarCosmeticsFunctions.interpolatePolygon(StarCosmeticsFunctions.generateStarVertices(4, 0.5, 0.3, false, false), 3);

		Vector direction = player.getLocation().clone().subtract(starCentre.clone()).getDirection();
		double angle = Math.atan2(direction.getX(), direction.getZ());
		for (Vector v : starFull) {
			drawStar(v.clone().rotateAroundY(angle).add(starCentre.clone()).toLocation(world), player);
		}

		Location playerLocation = player.getLocation();
		world.playSound(playerLocation, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(playerLocation, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 0.6f, 2f);
		world.playSound(playerLocation, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1f, 1.3f);
		world.playSound(playerLocation, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.18f, 1.6f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				drawYellow(starCentre.toLocation(player.getWorld()), player);
				for (int i = 0; i < 8 - mTicks; i++) {
					drawGold(starCentre.clone().subtract(new Vector(0, i - (1f / 2f * mTicks * (mTicks - 17)), 0).multiply((enemy.getHeight() + 0.8) / 36f)).toLocation(player.getWorld()), player);
				}

				mTicks++;
				if (mTicks >= 8) {
					world.playSound(playerLocation, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.22f, 2f);
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void bmbDamageLv2(Player player, LivingEntity enemy) {
		drawYellow(enemy.getEyeLocation(), player);
	}

	private void drawStar(Location location, Player player) {
		new PartialParticle(Particle.END_ROD, location, 1, 0, 0, 0, 0)
			.spawnAsPlayerActive(player);
	}

	private void drawYellow(Location location, Player player) {
		new PartialParticle(Particle.REDSTONE, location, 3, 0.3, 0.3, 0.3, 1f, new Particle.DustOptions(Color.fromRGB(255, 210, 30), 1f))
			.spawnAsPlayerActive(player);
	}

	private void drawGold(Location location, Player player) {
		new PartialParticle(Particle.REDSTONE, location, 3, 0.03, 0, 0.03, 1f, new Particle.DustOptions(Color.fromRGB(255, 180, 22), 1f))
			.spawnAsPlayerActive(player);
	}
}
