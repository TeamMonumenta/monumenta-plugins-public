package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.warrior.DefensiveLineCS;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPSpiral;
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

public class BiLineCS extends DefensiveLineCS {

	public static final String NAME = "Bi Line";

	public static final Color BI_RED = Color.fromRGB(0xD70270);
	public static final Color BI_PURPLE = Color.fromRGB(0x734F96);
	public static final Color BI_BLUE = Color.fromRGB(0x0038A8);

	public static final List<Color> BI_COLORS = List.of(BI_RED, BI_RED, BI_PURPLE, BI_BLUE, BI_BLUE);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Those who look both ways",
			"will not be caught off guard.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGENTA_BANNER;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void onCast(Plugin plugin, Player player, World world, Location loc, List<Player> affectedPlayers) {
		ParticleUtils.drawFlag(player, loc.clone().add(0, 3, 0), BI_COLORS, 1.6f);
		world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.4f, 0.4f);
		world.playSound(loc, Sound.ENTITY_WITCH_HURT, SoundCategory.PLAYERS, 2.0f, 1.5f);
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.4f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 0.5f, 1.6f);
		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 2.0f, 0.1f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				int sides = mTicks + 4;
				double radius = 2.5 - mTicks * 0.5;

				double degStep = 360.0 / sides;
				for (double deg = 0; deg < 360; deg += degStep) {
					new PPLine(Particle.REDSTONE,
						loc.clone().add(radius * FastUtils.cosDeg(deg), 0, radius * FastUtils.sinDeg(deg)),
						loc.clone().add(radius * FastUtils.cosDeg(deg + degStep), 0, radius * FastUtils.sinDeg(deg + degStep)))
						.data(new Particle.DustOptions(BI_COLORS.get(mTicks % BI_COLORS.size()), 1.4f))
						.countPerMeter(4)
						.delta(0.5)
						.spawnAsPlayerActive(player);
				}
				mTicks++;
				if (radius <= 0.5) {
					this.cancel();
					new PPSpiral(Particle.DUST_COLOR_TRANSITION, loc.clone().add(0, 0.15, 0), 4)
						.ticks(10)
						.data(new Particle.DustTransition(BI_RED, BI_BLUE, 1.66f))
						.count(180)
						.curveAngle(650)
						.curves(6)
						.delta(0.05)
						.spawnAsPlayerActive(player);
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		new PPExplosion(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 0.15, 0))
			.flat(true)
			.speed(1)
			.count(30)
			.extraRange(0.15, 0.5)
			.spawnAsPlayerActive(player);
	}

}
