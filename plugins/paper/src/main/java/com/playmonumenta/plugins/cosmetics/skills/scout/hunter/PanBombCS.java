package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.scout.WindBombCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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

public class PanBombCS extends WindBombCS {
	//These will be puns

	public static final String NAME = "Pan Bomb";

	private static final Color PAN_RED = Color.fromRGB(0xE54289);
	private static final Color PAN_YELLOW = Color.fromRGB(0xF5D84A);
	private static final Color PAN_BLUE = Color.fromRGB(0x62ADF8);
	public static final List<Color> PAN_COLORS = List.of(PAN_RED, PAN_YELLOW, PAN_BLUE);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"All colours may be formed by subtraction of these three.",
			"All paths may be formed by the subtraction of your foes."
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

	private final int[] mAngles = {0, 0, 0};

	@Override
	public void onLand(Player player, World world, Location loc, double radius) {
		Location ringLoc = loc.clone();
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				int sides = mTicks + 4;
				double r = 1 + mTicks * 0.5;

				double degStep = 360.0 / sides;
				for (double deg = 0; deg < 360; deg += degStep) {
					new PPLine(Particle.REDSTONE,
						ringLoc.clone().add(r * FastUtils.cosDeg(deg), 0, r * FastUtils.sinDeg(deg)),
						ringLoc.clone().add(r * FastUtils.cosDeg(deg + degStep), 0, r * FastUtils.sinDeg(deg + degStep)))
						.data(new Particle.DustOptions(PAN_COLORS.get(mTicks % PAN_COLORS.size()), 1.33f))
						.countPerMeter(9)
						.delta(0, 0.1 * mTicks, 0)
						.spawnAsPlayerActive(player);
				}
				mTicks++;
				if (r >= radius) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.2f, 0.9f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.8f, 0.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.9f, 0.2f);
		ParticleUtils.drawFlag(player, loc.clone().add(0, 3, 0), PAN_COLORS, 1.88f);
	}

	@Override
	public void onThrow(World world, Location loc) {
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 2.0f, 0.25f);
		world.playSound(loc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.3f, 0.1f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.8f);

		for (int i = 0; i < mAngles.length; i++) {
			mAngles[i] = FastUtils.randomIntInRange(-60, 60);
		}
	}

	@Override
	public void onVortexSpawn(Player player, World world, Location loc, double enhancePullDuration) {
		new PartialParticle(Particle.END_ROD, loc)
			.count(36)
			.delta(0.5)
			.extra(0.3)
			.spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.7f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(loc, Sound.BLOCK_SLIME_BLOCK_BREAK, SoundCategory.PLAYERS, 0.8f, 0.25f);
		world.playSound(loc, Sound.ENTITY_BREEZE_INHALE, SoundCategory.PLAYERS, 2.0f, 0.1f);
	}

	@Override
	public void onVortexTick(Player player, Location loc, double radius, int tick) {
		loc.getWorld().playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.PLAYERS, 0.4f, 0.7f);
		// in 40 ticks, rotate net 360 degrees
		for (int i = 0; i < mAngles.length; i++) {
			int angle = mAngles[i];
			int angleStep = 9 + 3 * (mAngles.length - i - 1);
			int startingDegrees = i * 60 + tick * angleStep;
			int endingDegrees = startingDegrees + angleStep;

			new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.5 * i - 2, 0), radius * (i + 1) / mAngles.length)
				.data(new Particle.DustOptions(PAN_COLORS.get(i), 1.6f))
				.arcDegree(startingDegrees, endingDegrees)
				.countPerMeter(3)
				.axes(VectorUtils.rotateYAxis(new Vector(1, 0, 0), angle), VectorUtils.rotateXAxis(new Vector(0, 0, 1), -angle / 2.0))
				.delta(0.4)
				.spawnAsPlayerActive(player);
		}
	}
}
