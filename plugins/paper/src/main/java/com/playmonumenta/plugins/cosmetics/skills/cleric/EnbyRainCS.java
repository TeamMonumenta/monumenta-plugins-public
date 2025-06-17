package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.particle.PPCircle;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class EnbyRainCS extends CleansingRainCS {
	public static final String NAME = "Enby Rain";

	public static final Color NB_YELLOW = Color.fromRGB(0xFFF433);
	public static final Color NB_WHITE = Color.WHITE;
	public static final Color NB_PURPLE = Color.fromRGB(0x9B59D0);
	public static final Color NB_BLACK = Color.fromRGB(0x2D2D2D);

	public static final List<Color> NB_COLORS = List.of(NB_YELLOW, NB_WHITE, NB_PURPLE, NB_BLACK);
	public static final List<BlockData> NB_FALLING_DUST_COLORS = List.of(
		Material.YELLOW_WOOL.createBlockData(),
		Material.WHITE_WOOL.createBlockData(),
		Material.PURPLE_WOOL.createBlockData(),
		Material.BLACK_WOOL.createBlockData()
	);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Under the rain, a binding that confined, dissipated",
			"what value lies in conformity,",
			"when you could be free.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.SUNFLOWER;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private int mTicks = 0;

	@Override
	public void rainCast(Player player, double mRadius) {
		World world = player.getWorld();
		Location location = player.getLocation();
		world.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 2.0f, 0.1f);
		world.playSound(location, Sound.BLOCK_BELL_RESONATE, 2.0f, 1.5f);
		world.playSound(location, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.7f, 0.7f);
		world.playSound(location, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.7f, 0.6f);
		world.playSound(location, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 2.0f, 0.5f);
	}

	@Override
	public void rainCloud(Player player, double ratio, double mRadius) {
		mTicks++;

		if (mTicks % 4 == 0) {
			new PPCircle(Particle.FALLING_DUST, player.getLocation().add(0, 2, 0), mRadius)
				.data(NB_FALLING_DUST_COLORS.get((mTicks / 4) % NB_FALLING_DUST_COLORS.size()))
				.count(20)
				.innerRadiusFactor(0.2)
				.delta(0.5, 1, 0.5)
				.spawnAsPlayerActive(player);
			new PPCircle(Particle.REDSTONE, player.getLocation().add(0, 4, 0), mRadius)
				.data(new Particle.DustOptions(NB_COLORS.get((mTicks / 4) % NB_COLORS.size()), 2.9f))
				.count(14)
				.delta(0.25)
				.ringMode(false)
				.spawnAsPlayerActive(player);
		}
	}

	@Override
	public void rainEnhancement(Player player, double smallRatio, double mRadius) {
		if (mTicks % 4 == 0) {
			new PPCircle(Particle.FALLING_DUST, player.getLocation().add(0, 2, 0), mRadius)
				.data(NB_FALLING_DUST_COLORS.get((mTicks / 4) % NB_FALLING_DUST_COLORS.size()))
				.count(10)
				.innerRadiusFactor(0.2)
				.delta(0.5, 1, 0.5)
				.spawnAsPlayerActive(player);
			new PPCircle(Particle.REDSTONE, player.getLocation().add(0, 4, 0), mRadius)
				.data(new Particle.DustOptions(NB_COLORS.get((mTicks / 4) % NB_COLORS.size()), 2.6f))
				.delta(0.25)
				.count(7)
				.ringMode(false)
				.spawnAsPlayerActive(player);
		}
	}
}
