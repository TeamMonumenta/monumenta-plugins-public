package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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

public class LiferootTonicCS extends EnergizingElixirCS {

	public static final String NAME = "Liferoot Tonic";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The sap of the ancient Liferoot trees make",
			"for an extremely potent elixir indeed.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.WATER_BUCKET;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Color BLUE = Color.fromRGB(0, 68, 255);
	private static final Particle.DustTransition BLUE_TRANSITION = new Particle.DustTransition(Color.fromRGB(0, 68, 255), Color.fromRGB(2, 30, 191), 1.2f);

	@Override
	public void activate(Player player, int newStacks, boolean manualCast) {
		if (manualCast) {
			World world = player.getWorld();
			Location loc = player.getLocation();
			Vector front = player.getEyeLocation().getDirection().setY(0).normalize();
			Vector right = VectorUtils.rotateTargetDirection(front.clone(), 90, 15);
			Vector left = VectorUtils.rotateTargetDirection(front.clone(), -90, 15);
			double r = BLUE.getRed() / 255D;
			double g = BLUE.getGreen() / 255D;
			double b = BLUE.getBlue() / 255D;

			world.playSound(loc, Sound.ENTITY_WITCH_DRINK, SoundCategory.PLAYERS, 0.9f, 1.4f);
			world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.5f, 0.5f);
			world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 0.7f, 0.5f);
			world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.7f, 1.7f);
			world.playSound(loc, Sound.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.PLAYERS, 2f, 0.5f);

			new PPCircle(Particle.DUST_COLOR_TRANSITION, loc.clone().add(0, 1, 0), 1.5f)
				.data(BLUE_TRANSITION)
				.countPerMeter(6).axes(front, right).spawnAsPlayerActive(player);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () ->
				new PPCircle(Particle.DUST_COLOR_TRANSITION, loc.clone().add(0, 1, 0), 1.5f)
					.data(BLUE_TRANSITION)
					.countPerMeter(6).axes(front, left).spawnAsPlayerActive(player), 2);

			for (int i = 0; i < newStacks + 1; i++) {
				new BukkitRunnable() {
					@Override
					public void run() {
						world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.7f, (float) (1 + 0.25 * newStacks));
						new PPCircle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 1f)
							.count(18).directionalMode(true).rotateDelta(true)
							.delta(1, 0.1, 0).extra(0.2).spawnAsPlayerActive(player);
					}
				}.runTaskLater(Plugin.getInstance(), 3L*i);
			}

			new PPCircle(Particle.SCRAPE, loc, 1f)
				.count(16).directionalMode(true).rotateDelta(true)
				.delta(0, 5, 2).extra(2).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SPELL_MOB, loc, 24, r, g, b, 1).directionalMode(true)
				.spawnAsPlayerActive(player);
			new PartialParticle(Particle.WARPED_SPORE, loc, 16, 0, 1).spawnAsPlayerActive(player);
			new BukkitRunnable() {
				int mTicks = 0;
				final double mYaw = player.getEyeLocation().getYaw();

				@Override
				public void run() {
					if (mTicks >= 12) {
						this.cancel();
					}

					Location point1 = player.getLocation().clone().add(2 * FastUtils.cosDeg(mTicks * 30 + mYaw), 0.25 * mTicks, 2 * FastUtils.sinDeg(mTicks * 30 + mYaw));
					Location point2 = player.getLocation().clone().add(2 * FastUtils.cosDeg(mTicks * 30 + mYaw + 180), 0.25 * mTicks, 2 * FastUtils.sinDeg(mTicks * 30 + mYaw + 180));

					new PartialParticle(Particle.FALLING_DRIPSTONE_WATER, point1).spawnAsPlayerActive(player);
					new PartialParticle(Particle.FALLING_DRIPSTONE_WATER, point2).spawnAsPlayerActive(player);
					new PartialParticle(Particle.BUBBLE_POP, point1).spawnAsPlayerActive(player);
					new PartialParticle(Particle.BUBBLE_POP, point2).spawnAsPlayerActive(player);

					mTicks++;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	@Override
	public void stackDecayEffect(Player player, int newStacks) {
		Location loc = player.getLocation();
		player.getWorld().playSound(loc, Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.PLAYERS, 1.4f, 0.5f);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 8, 1, 1, 1, 0)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.SCRAPE, loc.clone().add(0, 2, 0), 1f)
			.count(16).directionalMode(true).rotateDelta(true)
			.delta(0, -5, -2).extra(2).spawnAsPlayerActive(player);
	}
}
