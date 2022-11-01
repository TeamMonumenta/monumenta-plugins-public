package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
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
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public class RitualRingCS extends TransmRingCS implements GalleryCS {
	//Gallery map1: blood theme

	public static final String NAME = "Ritual Ring";

	private static final Particle.DustOptions BLOODY_COLOR1 = new Particle.DustOptions(Color.fromRGB(129, 34, 31), 1.0f);
	private static final Particle.DustOptions BLOODY_COLOR2 = new Particle.DustOptions(Color.fromRGB(203, 12, 7), 1.0f);
	private static final BlockData BLOOD_BLOCK = Bukkit.createBlockData(Material.REDSTONE_BLOCK);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, RitualRingCS.NAME, false, this.getAbilityName(),
			"An eldritch ritual twisted space. You",
			"gazed upon unholy sacrifice and felt a",
			"link with the circle's unholy symbol."
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.TRANSMUTATION_RING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_WART;
	}

	@Override
	public String getName() {
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
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public void ringSoundStart(World world, Location mCenter) {
		world.playSound(mCenter, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundCategory.PLAYERS, 3f, 1.75f);
	}

	@Override
	public PPCircle ringPPCircle(Location mCenter, double mRadius) {
		return new PPCircle(Particle.REDSTONE, mCenter, mRadius)
			.data(BLOODY_COLOR1)
			.ringMode(true);
	}

	@Override
	public void ringEffect(Player mPlayer, Location mCenter, PPCircle particles, double mRadius, double ringRaidus, int tick) {
		if (tick % 40 == 0) {
			mPlayer.getWorld().playSound(mCenter, Sound.BLOCK_LAVA_AMBIENT, SoundCategory.PLAYERS, 1.2f, 1.2f);
		}
		new PPCircle(Particle.ENCHANTMENT_TABLE, mCenter, ringRaidus * 0.75).delta(0.1, 0.25, 0.1).count((int) Math.round(ringRaidus * 20)).ringMode(false).spawnAsPlayerActive(mPlayer);

		particles.count((int) Math.floor(90 * mRadius / ringRaidus)).location(mCenter).spawnAsPlayerActive(mPlayer);
		particles.count((int) Math.floor(6 * mRadius / ringRaidus)).location(mCenter.clone().add(0, 1, 0)).spawnAsPlayerActive(mPlayer);
		particles.count((int) Math.floor(30 * mRadius / ringRaidus)).location(mCenter.clone().add(0, 1.75, 0)).spawnAsPlayerActive(mPlayer);

		particles.data(BLOODY_COLOR2);
		particles.count((int) Math.floor(12 * mRadius / ringRaidus)).location(mCenter.clone().add(0, 0.5, 0)).spawnAsPlayerActive(mPlayer);
		particles.count((int) Math.floor(12 * mRadius / ringRaidus)).location(mCenter.clone().add(0, 1.25, 0)).spawnAsPlayerActive(mPlayer);

		particles.radius(mRadius/2.5);
		particles.count((int) Math.floor(6 * mRadius / ringRaidus)).location(mCenter).spawnAsPlayerActive(mPlayer);
		particles.data(BLOODY_COLOR1);
		particles.count((int) Math.floor(4 * mRadius / ringRaidus)).location(mCenter.clone().add(0, 1, 0)).spawnAsPlayerActive(mPlayer);

		if (tick % 10 == 0) {
			particles.count((int) Math.floor(6 * mRadius / ringRaidus)).location(mCenter.clone().add(0, 1.5, 0)).spawnAsPlayerActive(mPlayer);

			new PPLine(Particle.BLOCK_CRACK,
				mCenter.clone().add(0, 0, mRadius),
				mCenter.clone().add(mRadius * FastUtils.sinDeg(120), 0, mRadius * FastUtils.cosDeg(120)))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.BLOCK_CRACK,
				mCenter.clone().add(mRadius * FastUtils.sinDeg(120), 0, mRadius * FastUtils.cosDeg(120)),
				mCenter.clone().add(mRadius * FastUtils.sinDeg(240), 0, mRadius * FastUtils.cosDeg(240)))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.BLOCK_CRACK,
				mCenter.clone().add(mRadius * FastUtils.sinDeg(240), 0, mRadius * FastUtils.cosDeg(240)),
				mCenter.clone().add(0, 0, mRadius))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.REDSTONE,
				mCenter.clone().add(0, 0.5, -mRadius),
				mCenter.clone().add(mRadius * FastUtils.sinDeg(60), 0.5, mRadius * FastUtils.cosDeg(60)))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.REDSTONE,
				mCenter.clone().add(mRadius * FastUtils.sinDeg(60), 0.5, mRadius * FastUtils.cosDeg(60)),
				mCenter.clone().add(mRadius * FastUtils.sinDeg(300), 0.5, mRadius * FastUtils.cosDeg(300)))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.REDSTONE,
				mCenter.clone().add(mRadius * FastUtils.sinDeg(300), 0.5, mRadius * FastUtils.cosDeg(300)),
				mCenter.clone().add(0, 0.5, -mRadius))
				.countPerMeter(1).delta(0.03).data(BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
		} else {
			new PPLine(Particle.REDSTONE,
				mCenter.clone().add(0, 0.5, mRadius),
				mCenter.clone().add(mRadius * FastUtils.sinDeg(120), 0.5, mRadius * FastUtils.cosDeg(120)))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.REDSTONE,
				mCenter.clone().add(mRadius * FastUtils.sinDeg(120), 0.5, mRadius * FastUtils.cosDeg(120)),
				mCenter.clone().add(mRadius * FastUtils.sinDeg(240), 0.5, mRadius * FastUtils.cosDeg(240)))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.REDSTONE,
				mCenter.clone().add(mRadius * FastUtils.sinDeg(240), 0.5, mRadius * FastUtils.cosDeg(240)),
				mCenter.clone().add(0, 0.5, mRadius))
				.countPerMeter(1).delta(0.01).data(BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.BLOCK_CRACK,
				mCenter.clone().add(0, 0, -mRadius),
				mCenter.clone().add(mRadius * FastUtils.sinDeg(60), 0, mRadius * FastUtils.cosDeg(60)))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.BLOCK_CRACK,
				mCenter.clone().add(mRadius * FastUtils.sinDeg(60), 0, mRadius * FastUtils.cosDeg(60)),
				mCenter.clone().add(mRadius * FastUtils.sinDeg(300), 0, mRadius * FastUtils.cosDeg(300)))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(mPlayer);
			new PPLine(Particle.BLOCK_CRACK,
				mCenter.clone().add(mRadius * FastUtils.sinDeg(300), 0, mRadius * FastUtils.cosDeg(300)),
				mCenter.clone().add(0, 0, -mRadius))
				.countPerMeter(2).delta(0.03).data(BLOOD_BLOCK).spawnAsPlayerActive(mPlayer);
		}

		particles.radius(mRadius);
	}

	@Override
	public void ringEffectOnKill(Player mPlayer, Location loc) {
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.6f, 0.7f);
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.75f, 0.75f);
		new PartialParticle(Particle.CRIMSON_SPORE, loc.clone().add(0, 0.8, 0), 20, 0, 0.5, 0, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SOUL, loc.clone().add(0, 0.8, 0), 8, 0.2, 0.5, 0.2, 0.001).spawnAsPlayerActive(mPlayer);
	}
}
