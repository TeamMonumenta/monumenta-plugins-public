package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ProwlersRoarCS extends MelancholicLamentCS {
	private static final Particle.DustOptions OFF_WHITE = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The jungle stands still as a",
			"predator makes itself known."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.BONE;
	}

	@Override
	public @Nullable String getName() {
		return "Prowler's Roar";
	}

	@Override
	public void onCast(Player player, World world, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(loc, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(loc, Sound.ENTITY_STRIDER_DEATH, SoundCategory.PLAYERS, 0.8f, 0.6f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.2f);
		world.playSound(loc, Sound.ENTITY_RAVAGER_HURT, SoundCategory.PLAYERS, 0.6f, 0.5f);
		world.playSound(loc, Sound.ENTITY_SQUID_DEATH, SoundCategory.PLAYERS, 1.0f, 0.8f);

		Location eyeLoc = player.getEyeLocation();
		eyeLoc.subtract(eyeLoc.getDirection().multiply(0.15));
		ParticleUtils.drawParticleCircleExplosion(player, eyeLoc, 0, 0.85, 0, -90,
			20, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			ParticleUtils.drawParticleCircleExplosion(player, eyeLoc, 0, 0.85, 0, -90,
				20, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		}, 2);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			ParticleUtils.drawParticleCircleExplosion(player, eyeLoc, 0, 0.85, 0, -90,
				20, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		}, 4);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			ParticleUtils.drawParticleCircleExplosion(player, eyeLoc, 0, 0.85, 0, -90,
				20, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		}, 6);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			ParticleUtils.drawParticleCircleExplosion(player, eyeLoc, 0, 0.85, 0, -90,
				20, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		}, 8);

		new PartialParticle(Particle.END_ROD, loc, 120, 8, 0.06).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 120, 8, 0.06).spawnAsPlayerActive(player);


		new BukkitRunnable() {
			int mTicks = 0;
			final Location mLoc = loc.clone().add(0, 0.1, 0);
			double mRadius = radius * 0.85;

			@Override
			public void run() {
				new PPCircle(Particle.REDSTONE, mLoc, mRadius).data(OFF_WHITE)
					.countPerMeter(4).spawnAsPlayerActive(player);

				if (mRadius >= radius || mTicks >= 5) {
					ParticleUtils.drawParticleCircleExplosion(player, mLoc, 0, radius, -mLoc.getYaw(), -mLoc.getPitch(),
						150, 0.4f, false, 0, 0.01, Particle.EXPLOSION_NORMAL);
					this.cancel();
				}
				mRadius += radius * 0.075;
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void onWeakenApply(Player player, LivingEntity mob) {
		Vector front = LocationUtils.getDirectionTo(player.getEyeLocation(), mob.getEyeLocation());
		Vector up = new Vector(0, 1, 0);
		Vector right = VectorUtils.crossProd(up, front);

		Location origin = mob.getEyeLocation();

		new PPCircle(Particle.WAX_OFF, origin, 0.5).count(8).axes(up, right)
			.directionalMode(true).rotateDelta(true).delta(0, 1, 0.3).extra(3).spawnAsPlayerActive(player);
	}

	@Override
	public void enhancementTick(Player otherPlayer, Player user, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Location loc = otherPlayer.getLocation();

		double offset = (user.getTicksLived() % 180) * 3.5;
		Location loc1 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(0.75, 0.5, 0), offset));
		Location loc2 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(0.75, 0.5, 0), offset + 120));
		Location loc3 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(0.75, 0.5, 0), offset + 240));
		new PartialParticle(Particle.FIREWORKS_SPARK, loc1, 1).spawnAsPlayerActive(user);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc2, 1).spawnAsPlayerActive(user);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc3, 1).spawnAsPlayerActive(user);
	}

	@Override
	public void onCleanse(Player otherPlayer, Player user) {
		Location loc = otherPlayer.getLocation();
		otherPlayer.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1f, 1.5f);

		ParticleUtils.drawParticleCircleExplosion(user, loc.clone().add(0, 0.5, 0), 0, 0.01, -loc.getYaw(), -loc.getPitch(), 20, 0.2f, true, 0, 0, Particle.END_ROD);
	}

	@Override
	public void onSilenceHit(Player player, LivingEntity mob, double radius) {
		World world = player.getWorld();
		Location loc = mob.getLocation();

		world.playSound(loc, Sound.ENTITY_RAVAGER_ATTACK, SoundCategory.PLAYERS, 1.0f, 1.45f);
		world.playSound(loc, Sound.ENTITY_RAVAGER_STEP, SoundCategory.PLAYERS, 1.0f, 1.45f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1.0f, 1.35f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 1.0f, 1.25f);
		world.playSound(loc, Sound.ENTITY_SQUID_DEATH, SoundCategory.PLAYERS, 1.0f, 0.75f);

		ParticleUtils.drawParticleCircleExplosion(player, loc.clone().add(0, 0.1, 0), 0, radius / 2, -loc.getYaw(), -loc.getPitch(),
			50, 0.65f, true, 0, 0, Particle.EXPLOSION_NORMAL);
		ParticleUtils.drawParticleCircleExplosion(player, loc.clone().add(0, 0.1, 0), 0, radius / 2, -loc.getYaw(), -loc.getPitch(),
			30, 0.5f, true, 0, 0, Particle.EXPLOSION_NORMAL);
	}
}
