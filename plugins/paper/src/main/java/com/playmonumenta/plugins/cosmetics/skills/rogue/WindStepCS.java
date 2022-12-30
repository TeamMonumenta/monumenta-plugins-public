package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.DepthsCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class WindStepCS extends AdvancingShadowsCS implements DepthsCS {
	// Windy advancing shadow. Depth set: wind

	public static final String NAME = "Wind Step";
	private static final Color TRAIL_COLOR_BRIGHT = Color.fromRGB(227, 255, 234);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"Let a single step move you",
			"as swiftly as a violent vortex.");
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.ADVANCING_SHADOWS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FEATHER;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public String getToken() {
		return TALISMAN_WIND;
	}

	@Override
	public void tpParticle(Player mPlayer) {
		Location loc = mPlayer.getLocation().add(0, 1, 0);
		loc.setPitch(loc.getPitch() + 90);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 40, 0.3f,
			false, 0, -1.25, Particle.CLOUD);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation().add(0, 1.1, 0), 25, 0.35, 0.5, 0.35, 0.1)
			.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1.1, 0), 7, 0.3, 0.5, 0.3, 0.025)
			.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void tpTrail(Player mPlayer, Location loc, int i) {
		if (i % 2 == 0) {
			Vector dir = VectorUtils.rotateTargetDirection(loc.getDirection(), 90, FastUtils.randomDoubleInRange(-225, 45));
			Location l = loc.clone().add(0, 1.2, 0).add(dir.multiply(FastUtils.randomDoubleInRange(0.9, 1.25)));
			ParticleUtils.drawParticleLineSlash(l, l.getDirection(), 0, 1.75, 0.1, FastUtils.RANDOM.nextInt(5, 8),
				(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
					new PartialParticle(Particle.ELECTRIC_SPARK, lineLoc, 1, 0, 0, 0, 0.05)
						.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
			});
		}

		for (int j = 0; j < 3; j++) {
			double radian = FastMath.toRadians((i * 8) + (j * 120));
			Vector vec = new Vector(FastUtils.cos(radian) * 1.1, 0, FastUtils.sin(radian) * 1.1);
			vec = VectorUtils.rotateXAxis(vec, loc.getPitch() + 90);
			vec = VectorUtils.rotateYAxis(vec, loc.getYaw());
			Location l = loc.clone().add(0, 1.15, 0).add(vec);
			new PartialParticle(Particle.REDSTONE, l, 5, 0.125f, 0.125f, 0.125f, 0,
				new Particle.DustOptions(TRAIL_COLOR_BRIGHT, 1.7f))
				.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		}

		new PartialParticle(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 1, 0), 1, 0.25, 0.5, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void tpSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1.25f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.25f, 1.35f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 1.25f, 1);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.25f, 1.45f);
	}

	@Override
	public void tpSoundFail(World world, Player mPlayer) {
		tpSound(world, mPlayer);
	}
}
