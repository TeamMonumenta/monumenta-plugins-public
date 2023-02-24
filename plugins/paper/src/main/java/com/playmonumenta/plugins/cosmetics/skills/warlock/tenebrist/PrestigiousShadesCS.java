package com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
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
import org.jetbrains.annotations.Nullable;

public class PrestigiousShadesCS extends HauntingShadesCS implements PrestigeCS {

	public static final String NAME = "Prestigious Shades";

	private static final String AS_NAME = "PrestigiousShade";
	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"Golden church bells haunt",
			"the hero's requiem."
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HAUNTING_SHADES;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TOTEM_OF_UNDYING;
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
	public String getAsName() {
		return AS_NAME;
	}

	@Override
	public void shadesStartSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.PLAYERS, 0.9f, 0.8f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_POLAR_BEAR_AMBIENT, SoundCategory.PLAYERS, 1.25f, 0.6f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2.0f, 0.75f);
	}

	@Override
	public void shadesTrailParticle(Player mPlayer, Location bLoc, Vector dir, double distance) {
		double radius = distance / 6.4;
		int units = (int) Math.ceil(distance * 2.4);
		ParticleUtils.drawCurve(bLoc, 1, units, dir,
			t -> 0,
				t -> radius * FastUtils.sin(3.1416 * 2 * t / units), t -> radius * FastUtils.cos(3.1416 * 2 * t / units),
				(loc, t) -> {
				if (t % 2 == 0) {
					new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
				} else {
					new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
				}
			}
		);
	}

	@Override
	public void shadesTickEffect(Plugin mPlugin, World world, Player mPlayer, Location bLoc, double mAoeRadius, int mT) {
		if (mT % 10 == 0) {
			new BukkitRunnable() {
				double mRadius = 0;
				final Location mLoc = bLoc.clone().add(0, 0.2, 0);

				@Override
				public void run() {
					mRadius += 1.05;
					new PPCircle(Particle.REDSTONE, mLoc, mRadius).ringMode(true).count(40).delta(0.2).extra(0.1).data(LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.CLOUD, mLoc, mRadius + 0.15).ringMode(true).count(10).delta(0.05).extra(0.01).spawnAsPlayerActive(mPlayer);
					if (mRadius >= mAoeRadius + 1) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);

			int units = 4 * (int) Math.ceil(mAoeRadius * 3.2);
			ParticleUtils.drawCurve(bLoc.clone().add(0, 0.125, 0), 1, units,
				new Vector(mAoeRadius + 0.5, 0, 0),
				new Vector(0, 0, mAoeRadius + 0.5),
				new Vector(0, 1, 0),
				t -> Math.pow(FastUtils.cos(3.1416 * 2 * t / units), 3),
				t -> Math.pow(FastUtils.sin(3.1416 * 2 * t / units), 3),
				t -> 0,
				(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0.05, 0.05, 0.05, 0.1, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
			);
		}

		if (mT % 20 == 0) {
			world.playSound(bLoc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2.0f, 0.5f);
			world.playSound(bLoc, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 0.6f);
		}
	}

	@Override
	public void shadesEndEffect(World world, Player mPlayer, Location bLoc, double radius) {
		for (int i = 0; i < 3; i++) {
			radius *= 0.6;
			final int frame = i + 1;
			final int units = 4 * (int) Math.ceil(radius * 3.2);
			ParticleUtils.drawCurve(bLoc.clone().add(0, 0.125, 0), 1, units,
				new Vector(radius + 0.5, 0, 0),
				new Vector(0, 0, radius + 0.5),
				new Vector(0, 1, 0),
				t -> Math.pow(FastUtils.cos(3.1416 * 2 * t / units), 3),
				t -> Math.pow(FastUtils.sin(3.1416 * 2 * t / units), 3),
				t -> 0,
				(loc, t) -> new BukkitRunnable() {
					@Override
					public void run() {
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.05, 0.05, 0.05, 0.1, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
					}
				}.runTaskLater(Plugin.getInstance(), 2 * frame)
			);
		}

		world.playSound(bLoc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, .5f, 0.65f);
		world.playSound(bLoc, Sound.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.PLAYERS, 2.0f, 0.6f);
	}
}
