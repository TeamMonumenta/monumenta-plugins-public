package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class DivergentCausalityCS extends ElementalArrowsCS {
	public static final String NAME = "Divergent Causality";
	public static final Color LILAC_TIP = Color.fromRGB(139, 123, 219);
	public static final Color LILAC_BASE = Color.fromRGB(220, 204, 255);
	public static final Color PINK_TIP = Color.fromRGB(252, 159, 203);
	public static final Color PINK_BASE = Color.fromRGB(201, 88, 141);
	private static final Color ROSE_TIP = Color.fromRGB(237, 17, 82);
	private static final Color ROSE_BASE = Color.fromRGB(41, 7, 17);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The seed of your actions takes root... will",
			"harmony embrace, or dissonance consume?"
		);
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ELEMENTAL_ARROWS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CORAL;
	}

	@Override
	public void thunderEffect(Player player, LivingEntity enemy, boolean isLevelTwo, double radius) {
		Location loc = enemy.getLocation();
		World world = player.getWorld();

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				Particle.DustOptions colorTransition = new Particle.DustOptions(Color.fromRGB(240 + 5 * mTicks, 180 - 5 * mTicks, 240 - 20 * mTicks), 1.1f);
				Location loc = enemy.getLocation().add(FastUtils.randomDoubleInRange(-mTicks - 0.9, mTicks + 0.9), 0.5 * enemy.getHeight() + FastUtils.randomDoubleInRange(-mTicks - 0.75, mTicks + 0.75), FastUtils.randomDoubleInRange(-mTicks - 0.9, mTicks + 0.9));
				Vector dir = LocationUtils.getDirectionTo(LocationUtils.getHalfHeightLocation(enemy), loc);
				ParticleUtils.drawParticleLineSlash(loc, dir, 0, 2, 0.1, 3,
					(Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
						new PartialParticle(Particle.REDSTONE, lineLoc, 1, 0.0, 0.0, 0.0, 0.0).data(colorTransition).spawnAsPlayerActive(player));
				ParticleUtils.drawParticleLineSlash(loc, dir, 0, 2, 0.1, 3,
					(Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
						new PartialParticle(Particle.CHERRY_LEAVES, lineLoc, 1, 0.0, 0.0, 0.0, 0.5).spawnAsPlayerActive(player));

				new PartialParticle(Particle.SWEEP_ATTACK, loc, 1, 1.25f, 1.8f, 1.25f).spawnAsPlayerActive(player);
				world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.5f, 0.75f);
				mTicks++;
				if (mTicks > 2) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1f, 0.9f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.8f, 1.15f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.8f, 1.25f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.9f, 0.8f);
		if (isLevelTwo) {
			new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
				double x = 0.8 * radius * FastUtils.cos(14 * Math.PI * param) * FastUtils.cos(4 * Math.PI * param);
				double z = 0.8 * radius * FastUtils.cos(14 * Math.PI * param) * FastUtils.sin(4 * Math.PI * param);
				Vector vec = new Vector(x, 0.25, z);
				Color color = ParticleUtils.getTransition(PINK_BASE, PINK_TIP, vec.length() / radius);

				vec.setY(vec.lengthSquared() / 4);
				builder.location(loc.clone().add(vec));
				builder.data(new Particle.DustOptions(color, 1.1f));
			}).count(350).spawnAsPlayerActive(player);
		}
	}

	@Override
	public void iceEffect(Player player, LivingEntity enemy, boolean isLevelTwo, double radius) {
		Location loc = enemy.getLocation();
		World world = player.getWorld();
		new PPExplosion(Particle.ELECTRIC_SPARK, LocationUtils.getHalfHeightLocation(enemy).clone())
			.extra(0.5)
			.count(20)
			.spawnAsBoss();

		for (int i = 0; i < 3; i++) {
			Vector dir = VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 2));
			drawLineSlash(LocationUtils.getHalfHeightLocation(enemy).clone().add(dir.multiply(0.85)), dir, 0, 1.5, 0.1, 3, (Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0, 0, 0, 0, new Particle.DustOptions(
					ParticleUtils.getTransition(LILAC_BASE, LILAC_TIP, endProgress), 1.5f - (float) (endProgress * 1.3)))
					.spawnAsPlayerActive(player));
		}

		if (isLevelTwo) {
			new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
				double x = radius * FastUtils.cos(14 * Math.PI * param) * FastUtils.cos(4 * Math.PI * param);
				double z = radius * FastUtils.cos(14 * Math.PI * param) * FastUtils.sin(4 * Math.PI * param);
				Vector vec = new Vector(x, 0.2, z);
				Color color = ParticleUtils.getTransition(LILAC_BASE, LILAC_TIP, vec.length() / radius);

				vec.setY(vec.lengthSquared() / 8);
				builder.location(loc.clone().add(vec));
				builder.data(new Particle.DustOptions(color, 1.1f));
			}).count(350).spawnAsPlayerActive(player);
		}

		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.85f, 0.5f);
		world.playSound(loc, Sound.BLOCK_LARGE_AMETHYST_BUD_PLACE, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1f, 0.5f);
	}

	@Override
	public void fireEffect(Player player, LivingEntity enemy, boolean isLevelTwo, double radius) {
		Location loc = enemy.getLocation();
		World world = player.getWorld();

		for (int i = 0; i < 3; i++) {
			Vector dir = VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 1.5));
			spawnTendril(LocationUtils.getHalfHeightLocation(enemy), LocationUtils.getHalfHeightLocation(enemy).clone().add(dir.multiply(2.25)), player, Color.BLACK, ROSE_TIP);
		}

		if (isLevelTwo) {
			new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
				double x = radius * FastUtils.cos(14 * Math.PI * param) * FastUtils.cos(4 * Math.PI * param);
				double z = radius * FastUtils.cos(14 * Math.PI * param) * FastUtils.sin(4 * Math.PI * param);
				Vector vec = new Vector(x, 0.2, z);
				Color color = ParticleUtils.getTransition(ROSE_BASE, ROSE_TIP, vec.length() / radius);

				vec.setY(vec.lengthSquared() / 8);
				builder.location(loc.clone().add(vec));
				builder.data(new Particle.DustOptions(color, 1.1f));
			}).count(350).spawnAsPlayerActive(player);
		}

		world.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_BREAK, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.85f, 2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.9f, 0.5f);
	}

	@Override
	public void thunderProjectile(Player player, Projectile projectile, Plugin plugin) {
		plugin.mProjectileEffectTimers.addEntity(projectile, Particle.CHERRY_LEAVES);
	}

	@Override
	public void iceProjectile(Player player, Projectile projectile, Plugin plugin) {
		plugin.mProjectileEffectTimers.addEntity(projectile, Particle.WAX_OFF);
	}

	@Override
	public void fireProjectile(Player player, Projectile projectile, Plugin plugin) {
		plugin.mProjectileEffectTimers.addEntity(projectile, Particle.SMOKE_LARGE);
	}

	private void spawnTendril(Location loc, Location to, Player mPlayer, Color color1, Color color2) {
		double distance = loc.distance(to);
		Vector dirTo = to.toVector().subtract(loc.toVector());

		new BukkitRunnable() {
			final Location mL = loc.clone();
			final double mXMult = FastUtils.randomDoubleInRange(-0.7, 0.7);
			final double mZMult = FastUtils.randomDoubleInRange(-0.7, 0.7);
			final Vector mVecStep = dirTo.normalize().multiply(0.15);

			@Override
			public void run() {
				for (int i = 0; i < distance * 1.25; i++) {
					float size = 0.7f + (1.3f * (float) (1 - (mL.distance(loc) / distance)));
					double offset = 0.1 * (1f - (mL.distance(loc) / distance));
					double transition = (mL.distance(loc) / distance);
					double pi = (Math.PI * 2) * Math.max((1f - (mL.distance(loc) / distance)), 0);


					Vector vec = new Vector(mXMult * FastUtils.cos(pi), 0,
						mZMult * FastUtils.sin(pi));
					vec = VectorUtils.rotateTargetDirection(vec, loc.getYaw(), loc.getPitch() + 90);
					Location tendrilLoc = mL.clone().add(vec);

					new PartialParticle(Particle.REDSTONE, tendrilLoc, 2, offset, offset, offset, 0, new Particle.DustOptions(
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

	public static void drawLineSlash(Location loc, Vector dir, double angle, double length, double spacing, int duration, ParticleUtils.LineSlashAnimation animation) {
		Location l = loc.clone();
		l.setDirection(dir);

		List<Vector> points = new ArrayList<>();
		Vector vec = new Vector(0, 0, 1);
		vec = VectorUtils.rotateZAxis(vec, angle);
		vec = VectorUtils.rotateXAxis(vec, l.getPitch());
		vec = VectorUtils.rotateYAxis(vec, l.getYaw());
		vec = vec.normalize();

		for (double ln = -length; ln < length; ln += spacing) {
			Vector point = l.toVector().add(vec.clone().multiply(ln));
			points.add(point);
		}

		if (duration <= 0) {
			boolean midReached = false;
			for (int i = 0; i < points.size(); i++) {
				Vector point = points.get(i);
				boolean middle = !midReached && i == points.size() / 2;
				if (middle) {
					midReached = true;
				}
				animation.lineSlashAnimation(point.toLocation(loc.getWorld()),
					1D - (point.distance(l.toVector()) / length), (double) (i + 1) / points.size(), middle);
			}
		} else {
			new BukkitRunnable() {
				final int mPointsPerTick = (int) (points.size() * (1D / duration));
				int mT = 0;
				boolean mMidReached = false;

				@Override
				public void run() {


					for (int i = mPointsPerTick * mT; i < FastMath.min(points.size(), mPointsPerTick * (mT + 1)); i++) {
						Vector point = points.get(i);
						boolean middle = !mMidReached && i == points.size() / 2;
						if (middle) {
							mMidReached = true;
						}
						animation.lineSlashAnimation(point.toLocation(loc.getWorld()),
							1D - (point.distance(l.toVector()) / length), (double) (i + 1) / points.size(), middle);
					}
					mT++;

					if (mT >= duration) {
						this.cancel();
					}
				}

			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}
}
