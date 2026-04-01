package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrestigiousSlamCS extends MeteorSlamCS implements PrestigeCS {

	public static final String NAME = "Prestigious Slam";

	@Override
	public Material getDisplayItem() {
		return Material.GOLD_ORE;
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
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Only the best can balance",
			"both power and grace.");
	}

	@Override
	public int getPrice() {
		return 1;
	}

	public static final @NotNull Color COLOR_GOLD = Color.fromRGB(199, 175, 31);
	public static final @NotNull Color COLOR_LIGHT = Color.fromRGB(255, 247, 207);
	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(COLOR_GOLD, 1.25f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(COLOR_LIGHT, 1.25f);

	@Override
	public void onUpwardSlash(World world, Location location, Player player, double radius, int coneAngle) {
		world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(location, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1.2f, 1.5f);
		world.playSound(location, "minecraft:entity.breeze.deflect", SoundCategory.PLAYERS, 1.0f, 0.4f);
		world.playSound(location, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 0.7f);
		world.playSound(location, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.2f, 2f);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.3f, 0.9f);

		Location pLoc = player.getEyeLocation();

		ParticleUtils.drawHalfArc(pLoc, radius - 2.5, 40, 30, 110 + coneAngle / 2.0, 15, 0.2,
			(loc, rings, angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, loc)
					.data(new Particle.DustOptions(
						ParticleUtils.getTransition(
							COLOR_LIGHT,
							COLOR_GOLD,
							rings / 15.0),
						(float) (1 + (angleProgress + rings / 20.0) * 0.2)))
					.spawnAsPlayerActive(player);
			});

		ParticleUtils.drawHalfArc(pLoc, radius - 2.5, 140, 30, 110 + coneAngle / 2.0, 15, 0.2,
			(loc, rings, angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, loc)
					.data(new Particle.DustOptions(
						ParticleUtils.getTransition(
							COLOR_LIGHT,
							COLOR_GOLD,
							rings / 15.0),
						(float) (1 + (angleProgress + rings / 20.0) * 0.2)))
					.spawnAsPlayerActive(player);
			});

		location.setPitch(0);

		new BukkitRunnable() {
			private int mTicks = 0;
			private final Location mLocation = location.clone();

			@Override
			public void run() {
				if (++mTicks >= 40 || (mTicks > 5 && PlayerUtils.isOnGround(player))) {
					this.cancel();
					return;
				}
				double rad1 = player.getLocation().getYaw() * 3.1416 / 180 + mTicks * 0.08 * 3.1416;
				double rad2 = rad1 + 3.1416;
				double rad3 = rad1 + 0.04 * 3.1416;
				double rad4 = rad2 + 0.04 * 3.1416;
				double dist = 1.6 - 0.04 * mTicks;
				Location pLoc = player.getLocation();
				new PartialParticle(Particle.REDSTONE, pLoc.clone().add(dist * FastUtils.cos(rad1), mTicks * 0.055, dist * FastUtils.sin(rad1)),
					2, 0.15, 0.15, 0.15, 0, GOLD_COLOR).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, pLoc.clone().add(dist * FastUtils.cos(rad2), mTicks * 0.055, dist * FastUtils.sin(rad2)),
					1, 0.15, 0.15, 0.15, 0, LIGHT_COLOR).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, pLoc.clone().add(dist * FastUtils.cos(rad3), (mTicks + 0.5) * 0.055, dist * FastUtils.sin(rad3)),
					1, 0.15, 0.15, 0.15, 0, GOLD_COLOR).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, pLoc.clone().add(dist * FastUtils.cos(rad4), (mTicks + 0.5) * 0.055, dist * FastUtils.sin(rad4)),
					2, 0.15, 0.15, 0.15, 0, LIGHT_COLOR).spawnAsPlayerActive(player);

				if (mTicks <= 5) {
					mLocation.setYaw(mLocation.getYaw() + 90);
					mLocation.add(0, 1, 0);

					Vector offset = mLocation.getDirection();
					Location center = mLocation.clone().add(offset);

					ParticleUtils.drawHalfArc(center, radius - 1.75, 20, -30, 115, 6, 0.2,
						(loc, rings, angleProgress) -> {
							new PartialParticle(Particle.REDSTONE, loc)
								.data(new Particle.DustOptions(
									ParticleUtils.getTransition(
										COLOR_LIGHT,
										COLOR_GOLD,
										rings / 10.0),
									1.1f))
								.spawnAsPlayerActive(player);
							if (rings == 1) {
								Vector vec = LocationUtils.getDirectionTo(loc, center);
								new PartialParticle(Particle.CRIT, loc)
									.delta(vec.getX(), vec.getY(), vec.getZ())
									.directionalMode(true)
									.extra(1.6 + angleProgress * 0.6)
									.spawnAsPlayerActive(player);
							}
						});
					new PartialParticle(Particle.CRIT, center)
						.count(20)
						.extra(1.0)
						.spawnAsPlayerActive(player);

					new PPPillar(Particle.REDSTONE, center.clone().subtract(0, 1, 0).add(offset), 2)
						.count(20)
						.data(LIGHT_COLOR)
						.spawnAsPlayerActive(player);

					world.playSound(location, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.5f, 0.9f);
					world.playSound(location, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.7f, 0.5f + mTicks * 0.25f);
					world.playSound(location, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.5f, 1.0f + mTicks * 0.1f);
				}

			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void onSlam(World world, Location location, Player player, double radius, double fallDistance) {
		float volumeScale = (float) Math.min(0.1 + fallDistance / 8 * 0.9, 1);

		world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, volumeScale, 0.4f);
		world.playSound(location, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, volumeScale * 0.3f, 1.8f);
		world.playSound(location, "minecraft:entity.breeze.deflect", SoundCategory.PLAYERS, volumeScale, 0.4f);
		world.playSound(location, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, volumeScale, 0.7f);
		world.playSound(location, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, volumeScale * 0.7f, 0.4f);
		world.playSound(location, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, volumeScale * 1.4f, 0.7f);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, volumeScale * 0.3f, 0.7f);
		world.playSound(location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, volumeScale * 0.4f, 1.7f);
		world.playSound(location, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, volumeScale * 1.4f, 1f);

		slam(location, player, radius, false);
	}

	@Override
	public void onSlamCritical(Plugin plugin, World world, Location location, Player player) {
		world.playSound(location, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.55f, 0.7f);
		world.playSound(location, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.35f, 2.0f);

		new PartialParticle(Particle.BLOCK_CRACK, location.clone().add(0, 0.5, 0))
			.data(Material.GOLD_BLOCK.createBlockData())
			.delta(0.3, 0.5, 0.3)
			.count(18)
			.extra(1)
			.spawnAsPlayerActive(player);

		Location pLoc = player.getLocation();
		pLoc.setDirection(LocationUtils.getDirectionTo(location, pLoc));

		ParticleUtils.drawHalfArc(pLoc, pLoc.distance(location) - 1.75, 270, 40, 120, 15, 0.2,
			(loc, rings, angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, loc)
					.data(new Particle.DustOptions(
						ParticleUtils.getTransition(
							COLOR_LIGHT,
							COLOR_GOLD,
							rings / 15.0),
						(float) (1 + (angleProgress + rings / 20.0) * 0.4)))
					.spawnAsPlayerActive(player);
				if (rings == 1) {
					Vector vec = LocationUtils.getDirectionTo(loc, pLoc);
					new PartialParticle(Particle.END_ROD, loc)
						.delta(vec.getX(), vec.getY(), vec.getZ())
						.directionalMode(true)
						.extra(0.2)
						.spawnAsPlayerActive(player);
				}
			});
	}

	private void slam(Location location, Player player, double radius, boolean isGroundPound) {
		Location mCenter = location.clone().add(0, 0.125, 0);
		Vector mFront = player.getLocation().getDirection().clone().setY(0).normalize().multiply(radius);
		int units = (int) Math.ceil(radius * 1.8);
		ParticleUtils.drawCurve(mCenter, 0, units * 6, mFront,
			t -> FastUtils.cos(3.1416 * t / (units * 6)),
			t -> 0, t -> 0.025 + FastUtils.sin(3.1416 * t / (units * 6)),
			(l, t) -> retrieveParticle(isGroundPound, GOLD_COLOR, l).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 3, mFront,
			t -> 0.5 + 0.5 * FastUtils.cos(3.1416 * t / (units * 3)),
			t -> 0, t -> 0.025 - 0.5 * FastUtils.sin(3.1416 * t / (units * 3)),
			(l, t) -> retrieveParticle(isGroundPound, GOLD_COLOR, l).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 3, mFront,
			t -> -0.5 + 0.5 * FastUtils.cos(3.1416 * t / (units * 3)),
			t -> 0, t -> 0.025 + 0.5 * FastUtils.sin(3.1416 * t / (units * 3)),
			(l, t) -> retrieveParticle(isGroundPound, GOLD_COLOR, l).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 2, mFront,
			t -> -0.5 + 0.167 * FastUtils.cos(3.1416 * 2 * t / (units * 2)),
			t -> 0, t -> 0.167 * FastUtils.sin(3.1416 * 2 * t / (units * 2)),
			(l, t) -> retrieveParticle(isGroundPound, GOLD_COLOR, l).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 6, mFront,
			t -> FastUtils.cos(3.1416 * t / (units * 6)),
			t -> 0, t -> -0.025 - FastUtils.sin(3.1416 * t / (units * 6)),
			(l, t) -> retrieveParticle(isGroundPound, LIGHT_COLOR, l).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 3, mFront,
			t -> 0.5 + 0.5 * FastUtils.cos(3.1416 * t / (units * 3)),
			t -> 0, t -> -0.025 - 0.5 * FastUtils.sin(3.1416 * t / (units * 3)),
			(l, t) -> retrieveParticle(isGroundPound, LIGHT_COLOR, l).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 3, mFront,
			t -> -0.5 + 0.5 * FastUtils.cos(3.1416 * t / (units * 3)),
			t -> 0, t -> -0.025 + 0.5 * FastUtils.sin(3.1416 * t / (units * 3)),
			(l, t) -> retrieveParticle(isGroundPound, LIGHT_COLOR, l).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 2, mFront,
			t -> 0.5 + 0.167 * FastUtils.cos(3.1416 * 2 * t / (units * 2)),
			t -> 0, t -> 0.167 * FastUtils.sin(3.1416 * 2 * t / (units * 2)),
			(l, t) -> retrieveParticle(isGroundPound, LIGHT_COLOR, l).spawnAsPlayerActive(player)
		);
	}

	private PartialParticle retrieveParticle(boolean isGroundPound, Particle.DustOptions color, Location l) {
		return isGroundPound ? new PartialParticle(Particle.END_ROD, l, 1, 0, 0.75, 0, 1).directionalMode(true)
			: new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0).data(color);
	}

	@Override
	public void onGroundPoundCast(Plugin plugin, World world, Location location, Player player) {
		world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(location, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1.2f, 1.0f);
		world.playSound(location, "minecraft:entity.breeze.deflect", SoundCategory.PLAYERS, 1.0f, 0.4f);
		world.playSound(location, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(location, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.2f, 1.5f);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.3f, 0.5f);
	}

	@Override
	public void onGroundPoundTick(Plugin plugin, World world, Location location, Player player) {
		new PartialParticle(Particle.GUST, location, 1, 0F, 0F, 0F, 0.1F).spawnAsPlayerActive(player);

		new PPCircle(Particle.END_ROD, location, 0.5)
			.count(15)
			.delta(0.07, 0, 0)
			.directionalMode(true).rotateDelta(true)
			.extra(1.5).extraVariance(0.5)
			.spawnAsPlayerActive(player);

		new PPCircle(Particle.REDSTONE, location, 1.5)
			.count(16)
			.ringMode(true)
			.data(LIGHT_COLOR)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, location, 1.5)
			.count(16)
			.ringMode(true)
			.data(GOLD_COLOR)
			.spawnAsPlayerActive(player);
	}

	@Override
	public void onGroundPoundSlam(Plugin plugin, World world, Location location, Player player, double radius) {
		world.playSound(location, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.2f, 0.75f);
		slam(location, player, radius, true);
	}
}
