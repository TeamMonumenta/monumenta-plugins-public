package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPPeriodic;
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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class FracturedSoulCS extends ElementalSpiritCS {
	public static final Color LILAC_COLOR = Color.fromRGB(243, 196, 255);
	public static final Color LILAC_BASE = Color.fromRGB(220, 204, 255);
	private static final Color ROSE_COLOR = Color.fromRGB(237, 17, 82);
	public static final Color BLACK_COLOR = Color.fromRGB(0, 0, 0);
	public static final String NAME = "Fractured Soul";

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ELEMENTAL_SPIRIT_FIRE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.HEARTBREAK_POTTERY_SHERD;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A soul once unified, now separated between realms.",
			"The two fragments yearn for unity, but it is their",
			"longing in of and itself that manifests in a most",
			"magnificent spectacle."
		);
	}

	@Override
	public void fireSpiritActivate(World world, Player player, Location playerLoc, Location loc, Vector dir, double hitbox) {
		world.playSound(playerLoc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.PLAYERS, 1, 1.5f);
		world.playSound(playerLoc, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 1, 1.5f);

		playerLoc.setDirection(dir);
		Location spiritLoc = player.getLocation().clone().add(0, 0.1, 0);
		Location targetLoc = loc.clone().subtract(0, 0.9, 0);

		new BukkitRunnable() {
			int mStage = 0;

			@Override
			public void run() {
				if (mStage > 0) {
					Vector direction = LocationUtils.getDirectionTo(targetLoc, playerLoc.clone().add(0, 0.1, 0));
					spiritLoc.add(direction.clone().multiply(0.125 * playerLoc.distance(targetLoc)));
					world.playSound(spiritLoc, Sound.BLOCK_SCULK_SHRIEKER_BREAK, SoundCategory.PLAYERS, 2.0f, 0.5f);
					world.playSound(spiritLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.5f, 0.5f);
					Vector relX = VectorUtils.rotationToVector(playerLoc.getYaw(), playerLoc.getPitch()).normalize();
					Vector relZ = VectorUtils.rotationToVector(playerLoc.getYaw() + 90, playerLoc.getPitch()).setY(0).normalize();
					Vector spikeDir = VectorUtils.crossProd(relZ.multiply(2 - 0.2 * mStage), relX);

					for (int i = 0; i < 3; i++) {
						Location offsetLoc = LocationUtils.varyInUniform(spiritLoc, hitbox, 0, hitbox).clone().add(spikeDir.multiply(Math.min((0.25 * (mStage * 0.6)) * FastUtils.randomDoubleInRange(0.8, 1.2), 1.25)));
						drawLineSlash(offsetLoc, spikeDir, 0, Math.min(0.75 + 0.1 * mStage * FastUtils.randomDoubleInRange(0.8, 1.2), 1.5), 0.15, 3, (Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
							new PartialParticle(Particle.REDSTONE, lineLoc, 2, 0, 0, 0, 0, new Particle.DustOptions(
								ParticleUtils.getTransition(BLACK_COLOR, ROSE_COLOR, Math.pow(endProgress, 1.5)), Math.max(0.9f, 2.2f - (float) (endProgress * 1.35))))
								.spawnAsPlayerActive(player));
						new PartialParticle(Particle.BLOCK_CRACK, spiritLoc, 5, 0.1, 0, 0.1, 0.1f).data(Material.OBSIDIAN.createBlockData()).spawnAsPlayerActive(player);
					}

					PPCircle sparkfloor = new PPCircle(Particle.REDSTONE, spiritLoc, hitbox * 0.9).data(new Particle.DustOptions(BLACK_COLOR, 1f)).ringMode(false);
					sparkfloor.count((int) (hitbox * hitbox * 3)).location(spiritLoc.clone()).spawnAsPlayerActive(player);
				}


				mStage++;
				if (mStage >= 8) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void fireSpiritTravel(Player player, Location loc, double hitbox) {
		//none
	}

	@Override
	public void iceSpiritPulse(Player player, World world, Location loc, double size) {
		new PPExplosion(Particle.WAX_OFF, loc.clone())
			.extra(25)
			.count(35)
			.spawnAsBoss();

		for (int i = 0; i < 3; i++) {
			Vector dir = VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 2));
			drawLineSlash(loc.clone().add(dir.multiply(0.85)), dir, 0, 1.5, 0.1, 3, (Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0, 0, 0, 0, new Particle.DustOptions(
					ParticleUtils.getTransition(LILAC_BASE, LILAC_COLOR, endProgress), 1.5f - (float) (endProgress * 1.3)))
					.spawnAsPlayerActive(player));
		}
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1f, 1.4f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1f, 1.2f);
	}

	@Override
	public AbstractPartialParticle<?> getFirePeriodicParticle(Player player) {
		return new PPPeriodic(Particle.REDSTONE, player.getLocation()).extra(0.01).data(new Particle.DustOptions(ROSE_COLOR, 1.5f));
	}

	@Override
	public AbstractPartialParticle<?> getIcePeriodicParticle(Player player) {
		return new PPPeriodic(Particle.REDSTONE, player.getLocation()).extra(0.01).data(new Particle.DustOptions(LILAC_COLOR, 1.5f));
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
