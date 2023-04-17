package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Constants;
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

public class LightspeedCS extends AdvancingShadowsCS {
	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Let the stars illuminate your path,",
			"travel at inconceivable speed."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.LANTERN;
	}

	@Override
	public @Nullable String getName() {
		return "Lightspeed";
	}

	@Override
	public void tpParticle(Player player, LivingEntity target) {
		Vector starCentre = target.getLocation().clone().add(0, target.getEyeHeight(), 0).toVector();
		double starSize = 4 * target.getHeight() / 5;

		ArrayList<Vector> starFull = StarCosmeticsFunctions.interpolatePolygon(StarCosmeticsFunctions.generateStarVertices(5, starSize, 0.5, false, false), 3);

		Vector direction = player.getLocation().clone().subtract(starCentre.clone()).getDirection();
		double angle = Math.atan2(direction.getX(), direction.getZ());
		for (Vector v : starFull) {
			switch (FastUtils.randomIntInRange(0, 3)) {
				case 0 -> drawCrit(v.clone().rotateAroundY(angle).add(starCentre.clone()).toLocation(player.getWorld()), player);
				case 1 -> drawSpark(v.clone().rotateAroundY(angle).add(starCentre.clone()).toLocation(player.getWorld()), player);
				default -> drawGold(v.clone().rotateAroundY(angle).add(starCentre.clone()).toLocation(player.getWorld()), player);
			}
		}

		new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 0, new Particle.DustOptions(Color.fromRGB(255, 220, 50), 0.75f))
			.spawnAsPlayerActive(player);
	}

	@Override
	public void tpTrail(Player player, Location loc, int i) {
		drawSpark(loc.clone().add(new Vector(0, 1.1, 0)), player);
	}

	@Override
	public void tpSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 1.0f, 2.0f);
		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_FALL, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
		}.runTaskLater(Plugin.getInstance(), 2);
	}

	@Override
	public void tpSoundFail(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_FALL, SoundCategory.PLAYERS, 1.0f, 0.7f);
	}

	@Override
	public void tpChain(World world, Player player) {
		world.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.7f, Constants.NotePitches.C6);
		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.5f, Constants.NotePitches.G1);
				world.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.5f, Constants.NotePitches.E10);
			}
		}.runTaskLater(Plugin.getInstance(), 1);
	}

	private void drawCrit(Location location, Player player) {
		new PartialParticle(Particle.CRIT, location, 3, 0, 0, 0, 0).minimumCount(0)
			.spawnAsPlayerActive(player);
	}

	private void drawGold(Location location, Player player) {
		new PartialParticle(Particle.REDSTONE, location, 5, 0.04, 0, 0.04, 0, new Particle.DustOptions(Color.fromRGB(255, 200, 70), 0.9f)).minimumCount(0)
			.spawnAsPlayerActive(player);
	}

	private void drawSpark(Location location, Player player) {
		new PartialParticle(Particle.ELECTRIC_SPARK, location, 4, 0.03, 0, 0.03, 0).minimumCount(0)
			.spawnAsPlayerActive(player);
	}
}
