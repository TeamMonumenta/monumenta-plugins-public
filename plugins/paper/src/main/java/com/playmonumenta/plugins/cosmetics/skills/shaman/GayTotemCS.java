package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPSpiral;
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
import org.jetbrains.annotations.Nullable;

public class GayTotemCS extends CleansingTotemCS {

	public static final String NAME = "Gay Totem";

	public static final Color GAY_GREEN = Color.fromRGB(0x078D70);
	public static final Color GAY_TEAL = Color.fromRGB(0x26CEAA);
	public static final Color GAY_LIME = Color.fromRGB(0x98E8C1);
	public static final Color GAY_WHITE = Color.WHITE;
	public static final Color GAY_BLUE = Color.fromRGB(0x7BADE2);
	public static final Color GAY_PURPLE = Color.fromRGB(0x5049CC);
	public static final Color GAY_INDIGO = Color.fromRGB(0x3D1A78);

	public static final List<Color> GAY_COLORS = List.of(GAY_GREEN, GAY_TEAL, GAY_LIME, GAY_WHITE, GAY_BLUE, GAY_PURPLE, GAY_INDIGO);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Within the bounds of this bloom",
			"you feel a sense of calm and restoration",
			"Though it was rejected for its colours",
			"it has found its home with you.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.PITCHER_PLANT;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private int mTicks = 0;

	@Override
	public void cleansingTotemSpawn(World world, Location standLocation) {
		mTicks = 0;
	}

	@Override
	public void cleansingTotemCleanse(Player player, Location standLocation, double radius) {
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				int sides = mT + 3;
				double r = radius * sides / 9;
				double degStep = 360.0 / sides;
				for (double deg = 0; deg < 360; deg += degStep) {
					new PPLine(Particle.REDSTONE,
						standLocation.clone().add(r * FastUtils.cosDeg(deg), 0, r * FastUtils.sinDeg(deg)),
						standLocation.clone().add(r * FastUtils.cosDeg(deg + degStep), 0, r * FastUtils.sinDeg(deg + degStep)))
						.data(new Particle.DustOptions(GAY_COLORS.get(mT), 1.2f))
						.countPerMeter(4)
						.delta(0.1)
						.spawnAsPlayerActive(player);
				}
				mT++;
				if (mT > 6) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 2);

		player.playSound(standLocation, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.8f, 0.4f);
		player.playSound(standLocation, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 0.2f, 1.5f);
	}

	@Override
	public void cleansingTotemExpire(World world, Location standLocation, Player player) {
		new PartialParticle(Particle.END_ROD, standLocation)
			.count(50)
			.extra(0.4)
			.spawnAsPlayerActive(player);

		ParticleUtils.drawFlag(player, standLocation.clone().add(0, 3, 0), GAY_COLORS, 1.6f);
		player.playSound(standLocation, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.2f, 1.3f);
		player.playSound(standLocation, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.6f);
		player.playSound(standLocation, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.4f, 1.4f);
		player.playSound(standLocation, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.4f, 0.1f);
	}

	@Override
	public void cleansingTotemPulse(Player player, Location standLocation, double radius) {
		new PPSpiral(Particle.DUST_COLOR_TRANSITION, standLocation, radius)
			.delta(0.1)
			.data(switch (mTicks % 2) {
				case 0 -> new Particle.DustTransition(GAY_GREEN, GAY_WHITE, 1.3f);
				case 1 -> new Particle.DustTransition(GAY_WHITE, GAY_INDIGO, 1.3f);
				default -> throw new IllegalStateException("Unexpected value: " + mTicks % 2);
			})
			.countPerBlockPerCurve(3)
			.curveAngle(70)
			.curves(10)
			.ticks(10)
			.spawnAsPlayerActive(player);

		new PPSpiral(Particle.DUST_COLOR_TRANSITION, standLocation, radius)
			.delta(0.1)
			.data(switch (mTicks % 2) {
				case 0 -> new Particle.DustTransition(GAY_GREEN, GAY_WHITE, 1.3f);
				case 1 -> new Particle.DustTransition(GAY_WHITE, GAY_INDIGO, 1.3f);
				default -> throw new IllegalStateException("Unexpected value: " + mTicks % 2);
			})
			.countPerBlockPerCurve(3)
			.curveAngle(-70)
			.curves(10)
			.ticks(10)
			.spawnAsPlayerActive(player);

		mTicks++;
		player.playSound(standLocation, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.4f, 1.2f);
		player.playSound(standLocation, Sound.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 0.6f, 2.0f);
		player.playSound(standLocation, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.4f, 0.1f);
		player.playSound(standLocation, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.3f, 0.8f);
		player.playSound(standLocation, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.3f, 1.5f);
	}
}
