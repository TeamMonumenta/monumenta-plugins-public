package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
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

public class TransfiguredSpikeCS extends ManaLanceCS {
	public static final String NAME = "Transfigured Spike";
	private static final Color TWIST_COLOR_LIGHT = Color.fromRGB(130, 66, 66);
	private static final Color TWIST_COLOR_DARK = Color.fromRGB(127, 0, 0);
	private static final Color FLESH_COLOR_LIGHT = Color.fromRGB(168, 89, 113);
	private static final Color FLESH_COLOR_DARK = Color.fromRGB(120, 61, 81);


	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"This veil in-between... it will yield to",
			"me. But I need power... more power.");
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WEEPING_VINES;
	}

	@Override
	public void lanceHitBlock(Player player, Location loc, World world) {
		world.playSound(loc, Sound.ENTITY_SHULKER_DEATH, SoundCategory.PLAYERS, 1, 0.85f);
		world.playSound(loc, Sound.ENTITY_GUARDIAN_DEATH_LAND, SoundCategory.PLAYERS, 0.8f, 0.5f);
		new PartialParticle(Particle.SOUL, loc, 15, 0, 0, 0, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SQUID_INK, loc, 15, 0, 0, 0, 0.25).spawnAsPlayerActive(player);
	}

	@Override
	public void lanceParticle(Player player, Location startLoc, Location endLoc, double size) {
		Vector dir = player.getEyeLocation().getDirection();
		Vector crossXZ = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		Vector crossY = dir.clone().crossProduct(crossXZ).normalize();

		new PPParametric(Particle.CRIT_MAGIC, player.getLocation().clone().add(dir.clone().multiply(0.7)).add(0, 1.5, 0), (parameter, builder) -> {
			double angle = parameter * Math.PI * 2;

			Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(1.2);

			builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
		}).count(40).directionalMode(true).extra(1.5).spawnAsBoss();

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> new PPParametric(Particle.CRIT_MAGIC, player.getLocation().clone().add(dir.clone().multiply(0.8)).add(0, 1.5, 0), (parameter, builder) -> {
			double angle = parameter * Math.PI * 2;

			Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(0.8);

			builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
		}).count(35).directionalMode(true).extra(1.5).spawnAsBoss(), 2);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> new PPParametric(Particle.CRIT_MAGIC, player.getLocation().clone().add(dir.clone().multiply(0.9)).add(0, 1.5, 0), (parameter, builder) -> {
			double angle = parameter * Math.PI * 2;

			Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(0.4);

			builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
		}).count(30).directionalMode(true).extra(1.5).spawnAsBoss(), 3);

		startLoc.add(dir.clone().multiply(0.8));

		new PartialParticle(Particle.SMOKE_NORMAL, startLoc, 15, 0, 0, 0, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, startLoc, 30, 0, 0, 0, Bukkit.createBlockData(Material.NETHER_WART_BLOCK)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIMSON_SPORE, startLoc, 10, 0, 0, 0, 0.25).spawnAsPlayerActive(player);
		spawnTendril(startLoc, endLoc, player, TWIST_COLOR_DARK, TWIST_COLOR_LIGHT);
		spawnTendril(startLoc, endLoc, player, FLESH_COLOR_DARK, FLESH_COLOR_LIGHT);
	}

	@Override
	public void lanceSound(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.8f, 0.5f);
		world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.8f, 1.75f);
		world.playSound(loc, Sound.ENTITY_SLIME_DEATH, SoundCategory.PLAYERS, 0.9f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT_LAND, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.9f, 1.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.9f, 0.85f);
	}

	@Override
	public void lanceHit(Location loc, Player player) {
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 30, 0, 0, 0, 0.15)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 75, 0.1, 0.1, 0.1, Bukkit.createBlockData(Material.NETHER_WART_BLOCK)).spawnAsPlayerActive(player);
		loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1f, 0.5f);
		loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.7f, 0.7f);
	}

	public static void spawnTendril(Location loc, Location to, Player mPlayer, Color color1, Color color2) {
		double distance = loc.distance(to);

		new BukkitRunnable() {
			final Location mL = loc.clone();
			final double mXMult = FastUtils.randomDoubleInRange(-0.7, 0.7);
			final double mZMult = FastUtils.randomDoubleInRange(-0.7, 0.7);
			final Vector mVecStep = loc.getDirection().normalize().multiply(0.2);

			@Override
			public void run() {
				for (int i = 0; i < distance * 1.25; i++) {
					float size = 0.5f + (1.25f * (float) (1 - (mL.distance(loc) / distance)));
					double offset = 0.1 * (1f - (mL.distance(loc) / distance));
					double transition = (mL.distance(loc) / distance);
					double pi = (Math.PI * 2) * Math.max((1f - (mL.distance(loc) / distance)), 0);


					Vector vec = new Vector(mXMult * FastUtils.cos(pi), 0,
						mZMult * FastUtils.sin(pi));
					vec = VectorUtils.rotateTargetDirection(vec, loc.getYaw(), loc.getPitch() + 90);
					Location tendrilLoc = mL.clone().add(vec);

					new PartialParticle(Particle.REDSTONE, tendrilLoc, 3, offset, offset, offset, 0, new Particle.DustOptions(
						ParticleUtils.getTransition(color1, color2, transition), size))

						.spawnAsPlayerActive(mPlayer);
					mL.add(mVecStep);
					if (mL.distance(to) < 0.1) {
						this.cancel();
						return;
					}
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
