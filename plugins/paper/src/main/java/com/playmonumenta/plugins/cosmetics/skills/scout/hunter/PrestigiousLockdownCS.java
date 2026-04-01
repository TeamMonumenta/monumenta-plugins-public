package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
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

public class PrestigiousLockdownCS extends LockdownCS implements PrestigeCS {

	public static final String NAME = "Prestigious Shot";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 0.9f);
	private static final Particle.DustOptions WARN_COLOR = new Particle.DustOptions(Color.fromRGB(240, 64, 0), 0.9f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A halo of light marks",
			"those soon to depart."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_HORSE_ARMOR;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return player != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public int getPrice() {
		return 1;
	}

	@Override
	public void lockdownShoot(Plugin plugin, Player player, Location loc) {
		World world = player.getWorld();

		new PartialParticle(Particle.END_ROD, loc.clone().add(player.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WAX_OFF, loc.clone().add(player.getLocation().getDirection()), 15, 0, 0, 0, 5f).spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, 1.6f);
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.4f, 1.2f);
		world.playSound(loc, Sound.ENTITY_VEX_HURT, SoundCategory.PLAYERS, 2f, 0.4f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.4f, 2f);

		Location circleLoc = player.getLocation().add(0, 0.15, 0);
		circleLoc.setPitch(0);
		ParticleUtils.drawParticleCircleExplosion(player, circleLoc, 0, 2, 1, 0, 45, 0.6f, false, 0, Particle.END_ROD);

		Vector playerDir = player.getLocation().getDirection();
		Location front = player.getEyeLocation().subtract(0, 0.2, 0).add(playerDir.clone().multiply(2));

		ParticleUtils.drawRing(front, 36, playerDir, 1.6,
			(l, t) -> {
				new PartialParticle(Particle.FALLING_DUST, l, 1, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData(Material.YELLOW_CONCRETE)).spawnAsPlayerActive(player);
				new PartialParticle(Particle.FALLING_DUST, l, 1, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData(Material.ORANGE_CONCRETE)).spawnAsPlayerActive(player);
			}
		);
	}

	@Override
	public void lockdownParticleLine(Player player, Location startLoc, Location endLoc, double radius, final double maxDistance) {
		new BukkitRunnable() {
			int mT = 0;
			final Location mLoc = startLoc.clone();
			final Vector mDir = LocationUtils.getDirectionTo(endLoc, startLoc).multiply(0.5);
			double mAngle = 0;

			@Override
			public void run() {
				if (player.isDead() || !player.isOnline() || mT > 50) {
					this.cancel();
					return;
				}

				for (int i = 0; i < 8; i++) {
					if (mLoc.distance(endLoc) < 0.5) {
						this.cancel();
						return;
					}
					new PartialParticle(Particle.REDSTONE, mLoc, 3)
						.data(GOLD_COLOR)
						.delta(0.1)
						.spawnAsPlayerActive(player);

					new PartialParticle(Particle.CRIT_MAGIC, mLoc, 3)
						.delta(0.1)
						.spawnAsPlayerActive(player);

					ParticleUtils.drawParticleCircleExplosion(player, mLoc, 0, 2, 0, 90, 2, 1.3f,
						true, mAngle + 90, Particle.ELECTRIC_SPARK);

					mAngle += 12;
					mLoc.add(mDir);
				}
				mT++;
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		ParticleUtils.drawLine(startLoc, endLoc, 80,
			(l, t) -> new PartialParticle(Particle.ELECTRIC_SPARK, l, 1)
				.spawnAsPlayerActive(player));
	}

	@Override
	public void lockdownCharged(Player player) {
		World world = player.getWorld();
		Location pLoc = player.getLocation();

		new PartialParticle(Particle.END_ROD, LocationUtils.getHalfHeightLocation(player))
			.count(8)
			.delta(0.25, 0.5, 0.25)
			.spawnAsPlayerActive(player);

		world.playSound(pLoc, Sound.ENTITY_BREEZE_INHALE, SoundCategory.PLAYERS, 0.5f, 2f);
		world.playSound(pLoc, Sound.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.PLAYERS, 1f, 1.8f);
		world.playSound(pLoc, Sound.ITEM_SPYGLASS_USE, SoundCategory.PLAYERS, 1.4f, 0.4f);
	}

	@Override
	public void lockdownOrb(Location particleLoc, Player player) {
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, particleLoc)
			.minimumCount(1)
			.distanceFalloff(16)
			.data(new Particle.DustTransition(Color.YELLOW, Color.ORANGE, 1.25f))
			.spawnAsPlayerActive(player);
	}

	@Override
	public void lockdownCharging(Player player, double progress) {
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.35f, (float) (1.5f * progress));
	}

	@Override
	public void lockdownHit(Player player, LivingEntity e) {
		Location eLoc = e.getEyeLocation().subtract(0, 0.25, 0);
		World world = e.getWorld();

		world.playSound(eLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.1f, 0.944f);
		world.playSound(eLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.2f, 1.189f);
		world.playSound(eLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.3f, 1.414f);
		world.playSound(eLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.5f, 1.25f);

		Vector mFront = eLoc.toVector().subtract(player.getLocation().toVector()).normalize(); // .setY(0)
		new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 15, 0, 0, 0, 0.3f).spawnAsPlayerActive(player);
		ParticleUtils.drawRing(eLoc, 36, mFront, 1.6,
			(l, t) -> {
				new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, 0, WARN_COLOR).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(player);
			}
		);
		ParticleUtils.drawCurve(eLoc, -9, 9, mFront,
			t -> 0,
			t -> 0, t -> 0.21 * t,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, WARN_COLOR).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(eLoc, -9, 9, mFront,
			t -> 0,
			t -> 0.21 * t, t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, WARN_COLOR).spawnAsPlayerActive(player)
		);
	}

	@Override
	public void lockdownSuccess(Player player) {
		World world = player.getWorld();
		Location loc = player.getLocation();

		world.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.4f, 2f);
	}
}
