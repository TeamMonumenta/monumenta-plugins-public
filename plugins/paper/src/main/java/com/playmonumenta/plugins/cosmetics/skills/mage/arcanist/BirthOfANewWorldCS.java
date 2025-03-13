package com.playmonumenta.plugins.cosmetics.skills.mage.arcanist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BirthOfANewWorldCS extends SagesInsightCS {
	public static final String NAME = "Birth of a New World";
	public static final Color ROSE_COLOR = Color.fromRGB(140, 6, 46);
	private static final Color DARK_RED = Color.fromRGB(48, 2, 22);
	public static final Color LILAC_TIP = Color.fromRGB(139, 123, 219);
	public static final Color LILAC_BASE = Color.fromRGB(220, 204, 255);
	private static final Color ROSE_TIP = Color.fromRGB(237, 17, 82);
	private static final Color ROSE_BASE = Color.fromRGB(41, 7, 17);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"When the howling maelstrom finally subsided,",
			"a new world had coalesced from the dregs of",
			"another. Fractured. Isolated. Screaming for",
			"deliverance."
		);
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SAGES_INSIGHT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLACK_GLAZED_TERRACOTTA;
	}

	@Override
	public void insightTrigger(Plugin plugin, Player player, int resetSize) {
		World world = player.getWorld();
		Location loc = player.getLocation();
		loc.setDirection(loc.getDirection().setY(0).normalize());
		world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1, 1);
		world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 1, 0.5f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1, 0.5f);
		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1, 0.5f);

		new PartialParticle(Particle.WAX_OFF, loc.clone().add(0, 1, 0), 50, 0.05f, 0.05f, 0.05f, 25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 75, 0.05f, 0.05f, 0.05f, 0.5).spawnAsPlayerActive(player);

		ParticleUtils.drawCleaveArc(loc.clone().add(0, 1, 0), 3.5, 165, -80, 260, 6, 0, 0, 0.2, 60,
			(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
				new Particle.DustOptions(
					ParticleUtils.getTransition(LILAC_BASE, LILAC_TIP, ring / 6D),
					0.6f + (ring * 0.1f)
				)).spawnAsPlayerActive(player));

		ParticleUtils.drawCleaveArc(loc.clone().add(0, 1, 0), 3.5, -165, 100, 440, 6, 0, 0, 0.2, 60,
			(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
				new Particle.DustOptions(
					ParticleUtils.getTransition(ROSE_BASE, ROSE_TIP, ring / 6D),
					0.6f + (ring * 0.1f)
				)).spawnAsPlayerActive(player));

		RayTraceResult result = player.getWorld().rayTraceBlocks(player.getLocation(), new Vector(0, -1, 0), 5,
			FluidCollisionMode.SOURCE_ONLY, true);
		Location cLoc;
		if (result != null) {
			cLoc = result.getHitPosition().toLocation(player.getWorld()).add(0, 0.15, 0);
		} else {
			cLoc = player.getLocation().add(0, -5, 0);
		}
		cLoc.setPitch(0);
		ParticleUtils.drawParticleCircleExplosion(player, cLoc, 0, 1, 0, 0, 100, 0.7f,
			true, 0, 0, Particle.END_ROD);
		ParticleUtils.drawParticleCircleExplosion(player, cLoc.clone().add(0, 0.2, 0), 0, 1, 0, 0, 80, 0.7f,
			true, 0, 0, Particle.SQUID_INK);
		double rotation = 0;
		for (double speed = 0; speed < 0.7; speed += 0.02) {
			rotation += 3.5;
			ParticleUtils.drawParticleCircleExplosion(player, cLoc, 0, 1, 0, 0, 2 * resetSize, (float) speed,
				true, rotation, 0, Particle.END_ROD);
			ParticleUtils.drawParticleCircleExplosion(player, cLoc, 0, 1, 0, 0, 2 * resetSize, (float) speed,
				true, -rotation, 0, Particle.END_ROD);

			ParticleUtils.drawParticleCircleExplosion(player, cLoc.clone().add(0, 0.2, 0), 0, 1, 0, 0, 2 * resetSize, (float) speed,
				true, rotation, 0, Particle.SQUID_INK);
			ParticleUtils.drawParticleCircleExplosion(player, cLoc.clone().add(0, 0.2, 0), 0, 1, 0, 0, 2 * resetSize, (float) speed,
				true, -rotation, 0, Particle.SQUID_INK);

		}
	}

	@Override
	public void insightStackGain(Player player, DamageEvent event) {
		Location locD = event.getDamagee().getLocation().add(0, 1, 0);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-1, 1.5),
			FastUtils.randomDoubleInRange(0.7, 0.9),
			FastUtils.randomDoubleInRange(-1.5, 1.5)), player.getLocation().clone().add(0, 1, 0), player, locD, null);
	}

	private void createOrb(Vector dir, Location loc, Player player, Location targetLoc, @Nullable Location optLoc) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = targetLoc;
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = optLoc != null ? optLoc : LocationUtils.getHalfHeightLocation(player);

				for (int j = 0; j < 2; j++) {
					new PartialParticle(Particle.SMOKE_NORMAL,
						mL.clone().add(FastUtils.randomDoubleInRange(-0.05, 0.05),
							FastUtils.randomDoubleInRange(-0.05, 0.05),
							FastUtils.randomDoubleInRange(-0.05, 0.05)),
						1, 0, 0, 0, 0)
						.directionalMode(false).spawnAsPlayerActive(player);
				}

				for (int i = 0; i < 4; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.08;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.2) {
						mD.normalize().multiply(0.2);
					}

					mL.add(mD);

					Color c = FastUtils.RANDOM.nextBoolean() ? DARK_RED : ROSE_COLOR;
					new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0,
						new Particle.DustOptions(c, 1f))
						.spawnAsPlayerActive(player);

					if (mT > 5 && mL.distance(to) < 0.35) {
						world.playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.9f, 1f);
						new PartialParticle(Particle.SQUID_INK, mL, 10, 0f, 0f, 0f, 0.1F)
							.spawnAsPlayerActive(player);
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
