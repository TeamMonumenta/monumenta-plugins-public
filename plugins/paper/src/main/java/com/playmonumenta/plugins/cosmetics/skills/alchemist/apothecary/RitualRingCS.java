package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.playmonumenta.plugins.cosmetics.skills.GalleryCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class RitualRingCS extends TransmutationRingCS implements GalleryCS {
	//Gallery map1: blood theme

	public static final String NAME = "Ritual Ring";

	private static final Particle.DustOptions BLOODY_COLOR1 = new Particle.DustOptions(Color.fromRGB(129, 34, 31), 1.0f);
	private static final Particle.DustOptions BLOODY_COLOR2 = new Particle.DustOptions(Color.fromRGB(203, 12, 7), 1.0f);
	private static final BlockData BLOOD_BLOCK = Bukkit.createBlockData(Material.REDSTONE_BLOCK);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"An eldritch ritual twisted space. You",
			"gazed upon unholy sacrifice and felt a",
			"link with the circle's unholy symbol."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_WART;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public GalleryMap getMap() {
		return GalleryMap.SANGUINE;
	}

	@Override
	public boolean isUnlocked(Player mPlayer) {
		return ScoreboardUtils.getScoreboardValue(mPlayer, GALLERY_COMPLETE_SCB).orElse(0) >= 1
			       || mPlayer.getGameMode() == GameMode.CREATIVE;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("Complete Sanguine Halls to unlock!").toArray(new String[0]);
	}

	@Override
	public void startEffect(Player player, Location center, double radius) {
		center.getWorld().playSound(center, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundCategory.PLAYERS, 3f, 1.75f);
	}

	@Override
	public void periodicEffect(Player player, Location center, double radius, int tick, int maxTicks, int maximumPotentialTicks) {
		if (tick % 40 == 0) {
			player.getWorld().playSound(center, Sound.BLOCK_LAVA_AMBIENT, SoundCategory.PLAYERS, 1.2f, 1.2f);
		}
		new PPCircle(Particle.ENCHANTMENT_TABLE, center, 5 * 0.75).delta(0.1, 0.25, 0.1).count(100).ringMode(false).spawnAsPlayerActive(player);

		PPCircle particles = new PPCircle(Particle.REDSTONE, center, radius)
			                     .data(BLOODY_COLOR1);
		particles.count((int) Math.floor(90 * radius / 5)).location(center).spawnAsPlayerActive(player);
		particles.count((int) Math.floor(6 * radius / 5)).location(center.clone().add(0, 1, 0)).spawnAsPlayerActive(player);
		particles.count((int) Math.floor(30 * radius / 5)).location(center.clone().add(0, 1.75, 0)).spawnAsPlayerActive(player);

		particles.data(BLOODY_COLOR2);
		particles.count((int) Math.floor(12 * radius / 5)).location(center.clone().add(0, 0.5, 0)).spawnAsPlayerActive(player);
		particles.count((int) Math.floor(12 * radius / 5)).location(center.clone().add(0, 1.25, 0)).spawnAsPlayerActive(player);

		particles.radius(radius / 2.5);
		particles.count((int) Math.floor(6 * radius / 5)).location(center).spawnAsPlayerActive(player);
		particles.data(BLOODY_COLOR1);
		particles.count((int) Math.floor(4 * radius / 5)).location(center.clone().add(0, 1, 0)).spawnAsPlayerActive(player);

		if (tick % 10 == 0) {
			particles.count((int) Math.floor(6 * radius / 5)).location(center.clone().add(0, 1.5, 0)).spawnAsPlayerActive(player);

			new PPLine(Particle.BLOCK_CRACK,
				center.clone().add(0, 0, radius),
				center.clone().add(radius * FastUtils.sinDeg(120), 0, radius * FastUtils.cosDeg(120)))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(player);
			new PPLine(Particle.BLOCK_CRACK,
				center.clone().add(radius * FastUtils.sinDeg(120), 0, radius * FastUtils.cosDeg(120)),
				center.clone().add(radius * FastUtils.sinDeg(240), 0, radius * FastUtils.cosDeg(240)))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(player);
			new PPLine(Particle.BLOCK_CRACK,
				center.clone().add(radius * FastUtils.sinDeg(240), 0, radius * FastUtils.cosDeg(240)),
				center.clone().add(0, 0, radius))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE,
				center.clone().add(0, 0.5, -radius),
				center.clone().add(radius * FastUtils.sinDeg(60), 0.5, radius * FastUtils.cosDeg(60)))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE,
				center.clone().add(radius * FastUtils.sinDeg(60), 0.5, radius * FastUtils.cosDeg(60)),
				center.clone().add(radius * FastUtils.sinDeg(300), 0.5, radius * FastUtils.cosDeg(300)))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE,
				center.clone().add(radius * FastUtils.sinDeg(300), 0.5, radius * FastUtils.cosDeg(300)),
				center.clone().add(0, 0.5, -radius))
				.countPerMeter(1).delta(0.03).data(BLOODY_COLOR2).spawnAsPlayerActive(player);
		} else {
			new PPLine(Particle.REDSTONE,
				center.clone().add(0, 0.5, radius),
				center.clone().add(radius * FastUtils.sinDeg(120), 0.5, radius * FastUtils.cosDeg(120)))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE,
				center.clone().add(radius * FastUtils.sinDeg(120), 0.5, radius * FastUtils.cosDeg(120)),
				center.clone().add(radius * FastUtils.sinDeg(240), 0.5, radius * FastUtils.cosDeg(240)))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE,
				center.clone().add(radius * FastUtils.sinDeg(240), 0.5, radius * FastUtils.cosDeg(240)),
				center.clone().add(0, 0.5, radius))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(player);
			new PPLine(Particle.BLOCK_CRACK,
				center.clone().add(0, 0, -radius),
				center.clone().add(radius * FastUtils.sinDeg(60), 0, radius * FastUtils.cosDeg(60)))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(player);
			new PPLine(Particle.BLOCK_CRACK,
				center.clone().add(radius * FastUtils.sinDeg(60), 0, radius * FastUtils.cosDeg(60)),
				center.clone().add(radius * FastUtils.sinDeg(300), 0, radius * FastUtils.cosDeg(300)))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(player);
			new PPLine(Particle.BLOCK_CRACK,
				center.clone().add(radius * FastUtils.sinDeg(300), 0, radius * FastUtils.cosDeg(300)),
				center.clone().add(0, 0, -radius))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(player);
		}

		particles.radius(radius);
	}

	@Override
	public void effectOnKill(Player mPlayer, Location loc) {
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.6f, 0.7f);
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.75f, 0.75f);
		new PartialParticle(Particle.CRIMSON_SPORE, loc.clone().add(0, 0.8, 0), 20, 0, 0.5, 0, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SOUL, loc.clone().add(0, 0.8, 0), 8, 0.2, 0.5, 0.2, 0.001).spawnAsPlayerActive(mPlayer);
	}
}
