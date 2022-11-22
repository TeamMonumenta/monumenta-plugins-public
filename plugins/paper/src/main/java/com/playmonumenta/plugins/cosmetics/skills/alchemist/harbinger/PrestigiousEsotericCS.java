package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PrestigiousEsotericCS extends EsotericEnhancementsCS implements PrestigeCS {

	public static final String NAME = "Prestigious Esoteric";

	private static final String ABERRATION_LOS = "PrestigiousAberration";
	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
	private static final Particle.DustOptions BURN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 180, 0), 1.0f);
	private static final Particle.DustOptions[] DATA = {
		LIGHT_COLOR,
		GOLD_COLOR,
		BURN_COLOR
	};

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"ESOTERIC_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.ESOTERIC_ENHANCEMENTS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.HONEY_BOTTLE;
	}

	@Override
	public String getName() {
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
	public String getLos() {
		return ABERRATION_LOS;
	}

	@Override
	public void esotericSummonEffect(World world, Player mPlayer, Location mLoc) {
		final double radius = 3.6;
		final double theta = FastUtils.RANDOM.nextDouble(120);
		final double dRadius = -1.2;
		final double dTheta = 30;
		final int interval = 3;

		for (int i = 0; i < DATA.length; i++) {
			final double mRadius = radius + i * dRadius;
			final double mTheta = theta + i * dTheta;
			final int iter = i;
			new BukkitRunnable() {
				@Override
				public void run() {
					new PPLine(Particle.REDSTONE,
						mLoc.clone().add(mRadius * FastUtils.cosDeg(mTheta), 0.25, mRadius * FastUtils.sinDeg(mTheta)),
						mLoc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 120), 0.25, mRadius * FastUtils.sinDeg(mTheta + 120)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(mPlayer);
					new PPLine(Particle.REDSTONE,
						mLoc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 120), 0.25, mRadius * FastUtils.sinDeg(mTheta + 120)),
						mLoc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 240), 0.25, mRadius * FastUtils.sinDeg(mTheta + 240)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(mPlayer);
					new PPLine(Particle.REDSTONE,
						mLoc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 240), 0.25, mRadius * FastUtils.sinDeg(mTheta + 240)),
						mLoc.clone().add(mRadius * FastUtils.cosDeg(mTheta), 0.25, mRadius * FastUtils.sinDeg(mTheta)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), interval * i);
		}
	}
}
