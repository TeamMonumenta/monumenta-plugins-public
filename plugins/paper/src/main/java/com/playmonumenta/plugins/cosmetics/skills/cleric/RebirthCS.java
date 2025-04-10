package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RebirthCS extends CelestialBlessingCS {

	public static final String NAME = "Rebirth";
	private static final Color BRIGHT_AQUA = Color.AQUA.mixColors(Color.WHITE);
	private final Set<Player> mBuffedPlayers = new HashSet<>();
	private final List<Integer> mPosNeg = List.of(1, -1);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The call of the veil is deafening,",
			"but the ancient shrine's clamor is much louder.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.LIGHT_BLUE_CANDLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void tickEffect(Player player, Player target, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Location loc = target.getLocation().add(0, 0.5, 0);
		if (twoHertz) {
			new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 3)
				.delta(0.08)
				.spawnAsPlayerBuff(target);
		}
		new PartialParticle(Particle.REDSTONE, loc, 6)
			.delta(0.4)
			.data(new Particle.DustOptions(Color.AQUA.mixColors(Color.WHITE), 0.8f))
			.spawnAsPlayerBuff(target);

	}

	@Override
	public void loseEffect(Player player, Player target) {
		Location loc = target.getLocation();
		loc.setPitch(0);
		target.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1f, 0.65f);
		target.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.8f, 0.25f);

		new PPSpiral(Particle.SOUL_FIRE_FLAME, loc.clone().add(new Vector(0, 0.1, 0)), 3)
			.ticks(10)
			.countPerBlockPerCurve(8)
			.spawnAsPlayerBuff(target);

		ParticleUtils.drawParticleCircleExplosion(target, loc, 0, 0.5, 0, 0, 25, 3, false, 0, 0.15, Particle.CRIT_MAGIC);

		if (target != player) {
			createOrb(new Vector(0, 0.6, 0), loc, player, player);
		}
	}

	@Override
	public void startEffectCaster(Player player, double radius) {
		Location loc = player.getLocation();

		loc.setPitch(0);
		new PPLightning(Particle.REDSTONE, loc)
			.maxWidth(2).hopXZ(1.5).hopY(0).height(8)
			.data(new Particle.DustOptions(BRIGHT_AQUA, 1.2f))
			.duration(4)
			.count(8)
			.spawnAsPlayerActive(player);

		int randomNegative = FastUtils.getRandomElement(mPosNeg);
		for (int i = 1; i <= 3; i++) {
			Location mCenter = loc.clone().add(new Vector(0, 0.9, 0));
			new PPCircle(Particle.REDSTONE, mCenter, radius * 0.125 * i)
				.countPerMeter(6)
				.axes(VectorUtils.rotateZAxis(new Vector(1, 0, 0), FastUtils.randomDoubleInRange(0, 10) * i * randomNegative),
					VectorUtils.rotateXAxis(new Vector(0, 0, 1), FastUtils.randomDoubleInRange(0, -10) * i * randomNegative)
				)
				.data(new Particle.DustOptions(BRIGHT_AQUA, 1.2f))
				.spawnAsPlayerActive(player);
			randomNegative *= -1;
		}

		new PPSpiral(Particle.DUST_COLOR_TRANSITION, loc.clone().add(0, 0.1, 0), radius)
			.countPerBlockPerCurve(12)
			.ticks(20)
			.distanceFalloff(radius)
			.data(new Particle.DustTransition(BRIGHT_AQUA, Color.PURPLE, 1.67f))
			.spawnAsPlayerActive(player);

		for (int i = 0; i < 360; i += 90) {
			int degrees = i + (int) loc.getYaw();

			new BukkitRunnable() {
				final Vector mVec = VectorUtils.rotateYAxis(new Vector(0.5, 0, 0), degrees).add(new Vector(0, 0.2, 0));

				int mTicks = 0;

				@Override
				public void run() {
					for (int j = 0; j < 8; j++) {
						Vector vecRotated = VectorUtils.rotateYAxis(mVec, 50).normalize().multiply(radius / 4 * Math.log(mTicks + 1) / Math.log(37.0));
						new PartialParticle(Particle.END_ROD, loc.clone().add(mVec), 1, vecRotated.getX(), mTicks / 40.0, vecRotated.getZ(), 0.1, null, true, 0.01)
							.spawnAsPlayerActive(player);
						mVec.subtract(new Vector(0, 0.015, 0));
						mVec.rotateAroundY(0.2);
						mVec.multiply(1.01);
						mTicks++;
					}
					if (mTicks > 6 * 8) {
						this.cancel();
					}
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);

			ParticleUtils.drawCleaveArc(loc.clone().add(new Vector(0, 1, 0)), 2, 90, 180, 360, 2, i + 45, 0, 0.1, 15,
				(location, rings, angleProgress) -> new PartialParticle(Particle.SOUL_FIRE_FLAME, location).spawnAsPlayerActive(player));

		}
		ParticleUtils.drawParticleCircleExplosion(player, loc, 0, 0.5, 0, 0, 40, 3, false, 0, 0.15, Particle.ENCHANTMENT_TABLE);

		World world = player.getWorld();
		world.playSound(player, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.8f, 1.5f);
		world.playSound(player, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.9f, 1f);
		world.playSound(player, Sound.ENTITY_BREEZE_DEATH, 0.6f, 0.75f);
		world.playSound(player, Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.25f);
		world.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.55f, 1f);
		world.playSound(player, Sound.ENTITY_BREEZE_JUMP, 1.2f, 1f);

		//because startEffectTargets is called before this :p

		mBuffedPlayers.forEach(affectedPlayer -> {
			if (affectedPlayer != player) {
				createOrb(new Vector(0, 0.6, 0), player.getLocation(), affectedPlayer, player);
			}
		});

		mBuffedPlayers.clear();
	}

	@Override
	public void startEffectTargets(Player player) {
		mBuffedPlayers.add(player);
	}

	private void createOrb(Vector startingDirection, Location startingLocation, Entity targetEntity, Player mPlayer) {
		new BukkitRunnable() {
			final Location mL = startingLocation.clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = startingDirection.clone();

			@Override
			public void run() {
				mT++;

				if (!targetEntity.getWorld().equals(mL.getWorld())) {
					cancel();
					return;
				}

				for (int i = 0; i < 4; i++) {
					if (mT <= 2) {
						mD = startingDirection.clone();
					} else {
						mArcCurve += 0.15;
						mD = startingDirection.clone().add(LocationUtils.getDirectionTo(targetEntity.getLocation(), mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.2) {
						mD.normalize().multiply(0.2);
					}

					mL.add(mD);
					new PartialParticle(Particle.SOUL_FIRE_FLAME, mL, 1, 0.05, 0.05, 0.05, 0.035)
						.spawnAsPlayerActive(mPlayer);
					if (mT > 5 && mL.distance(targetEntity.getLocation()) < 0.35) {
						new PartialParticle(Particle.SHRIEK, mL, 1)
							.data(0)
							.spawnAsPlayerActive(mPlayer);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
