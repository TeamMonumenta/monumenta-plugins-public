package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.DepthsCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.math3.util.FastMath;
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

public class FireworkStrikeCS extends PredatorStrikeCS implements DepthsCS {
	//Steely predator stirke. Depth set: steel

	public static final String NAME = "Firework Strike";

	private static final float[] YAW_ANGLES = {-75, 75};
	private static final float EXPLODE_VOLUME = 2f;
	private static final Color TRAIL_COLOR = Color.fromRGB(255, 255, 199);


	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"These twin fireworks will make",
			"a spectacle out of your target!");
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.PREDATOR_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIREWORK_ROCKET;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public String getToken() {
		return TALISMAN_STEEL;
	}

	@Override
	public void strikeTick(Player mPlayer, int tick) {
		Location loc = mPlayer.getLocation().add(0, 1, 0);

		for (int i = 0; i < 4; i++) {
			double rotation = FastMath.toRadians((tick * 10) + (i * 90));
			Vector vec = new Vector(FastUtils.cos(rotation) * 0.75, 0,
				FastUtils.sin(rotation) * 0.75);
			Location l = loc.clone().add(vec);
			if (i % 2 == 0) {
				new PartialParticle(Particle.REDSTONE, l, 2, 0, 0, 0, 0,
					new Particle.DustOptions(TRAIL_COLOR, 1))
					.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
			} else {
				new PartialParticle(Particle.CRIT, l.clone().subtract(0, 0.25, 0), 1, 0, 0, 0, 0)
					.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
			}
		}
	}

	@Override
	public void strikeParticleProjectile(Player mPlayer, Location bLoc) {
	}

	@Override
	public void strikeSoundReady(World world, Player mPlayer) {
		Location loc = mPlayer.getLocation().add(0, 0.15, 0);
		loc.setPitch(0);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 45, 0.25f,
			true, 0, Particle.END_ROD);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 55, 1.75f,
			true, 0, 0.35, Particle.CRIT_MAGIC);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, SoundCategory.PLAYERS, 1, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, SoundCategory.PLAYERS, 1, 1.5f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, SoundCategory.PLAYERS, 1, 2f);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 75, 0, 0, 0, 0.75)
			.minimumMultiplier(false)
			.spawnAsPlayerActive(mPlayer);

	}

	@Override
	public void strikeLaunch(World world, Player mPlayer) {
		Location loc = mPlayer.getLocation().add(0, 0.15, 0);
		loc.setPitch(0);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1, 1.4f);
		world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1, 1.75f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 1, 1.5f);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 25, 0, 0, 0, 0.175)
			.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 42, 0.5f,
			true, 0, Particle.EXPLOSION_NORMAL);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 55, 2.25f,
			true, 0, 0.25, Particle.CRIT_MAGIC);
	}

	@Override
	public void strikeExplode(World world, Player mPlayer, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, EXPLODE_VOLUME, 0.85f);
		world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, EXPLODE_VOLUME, 0.85f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, EXPLODE_VOLUME, 1.5f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, EXPLODE_VOLUME, 0.8f);
		}, 10);
		new PartialParticle(Particle.FLAME, loc, 75, 0, 0, 0, 0.2)
			.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.END_ROD, loc, 60, 0, 0, 0, 0.25)
			.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 5, 1, 1, 1, 0)
			.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 40, 0, 0, 0, 0.225)
			.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void strikeImpact(Runnable runnable, Location target, Player mPlayer) {
		Location loc = mPlayer.getLocation().add(0, 1.35, 0);

		AtomicBoolean bool = new AtomicBoolean(false);
		for (float angle : YAW_ANGLES) {
			Vector dir = VectorUtils.rotateTargetDirection(loc.getDirection(), angle, -45).multiply(2.25);
			new BukkitRunnable() {
				final Location mL = loc.clone();
				int mT = 0;
				double mAngle = 0;
				double mArcCurve = 0;
				Vector mD = dir.clone();

				@Override
				public void run() {
					mT++;

					for (int i = 0; i < 7; i++) {
						mAngle += 12;
						mArcCurve += 0.2;
						mD = dir.clone().add(LocationUtils.getDirectionTo(target, mL).multiply(mArcCurve));

						if (mD.length() > 0.5) {
							mD.normalize().multiply(0.5);
						}

						mL.add(mD);

						new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0, new Particle.DustOptions(TRAIL_COLOR, 2.5f))
							.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
						ParticleUtils.drawParticleCircleExplosion(mPlayer, mL, 0, 1, 0, 90, 2, 0.085f,
							true, mAngle, Particle.END_ROD);
						ParticleUtils.drawParticleCircleExplosion(mPlayer, mL, 0, 1, 0, 90, 2, 1f,
							true, mAngle + 90, Particle.CRIT_MAGIC);

						if (mL.distance(target) < 0.5) {
							if (!bool.get()) {
								runnable.run();
								bool.set(true);
							}

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
}
