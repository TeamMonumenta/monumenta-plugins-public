package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PrestigiousStarfallCS extends StarfallCS implements PrestigeCS {

	public static final String NAME = "Prestigious Starfall";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.25f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);
	private static final Particle.DustOptions BURN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 180, 0), 1.25f);
	private static double CAST_EFFECT_RADIUS = 3.5;

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"\"Nobody survived under catastrophe\"",
			"Now, we are the catastrophe."
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.STARFALL;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player mPlayer) {
		return mPlayer != null;
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
	public void starfallCastEffect(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 1f, 0.65f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.5f, 0.6f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2f, 0.8f);
		new PartialParticle(Particle.FLAME, mPlayer.getLocation(), 45, 0.35f, 0.1f, 0.35f, 0.2f).spawnAsPlayerActive(mPlayer);

		// Draw 火
		Location mCenter = mPlayer.getLocation().add(0, 0.25, 0);
		Vector mFront = mPlayer.getLocation().getDirection().setY(0).normalize().multiply(CAST_EFFECT_RADIUS);
		ParticleUtils.drawRing(mCenter, (int) Math.ceil(12.8 * CAST_EFFECT_RADIUS), new Vector(0, 1, 0), CAST_EFFECT_RADIUS,
			(l, t) -> {
				new PartialParticle(Particle.REDSTONE, l, 3, 0.1, 0.1, 0.1, 0, BURN_COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, l, 2, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
			}
		);
		ParticleUtils.drawRing(mCenter, (int) Math.ceil(6.4 * CAST_EFFECT_RADIUS), new Vector(0, 1, 0), 0.6 * CAST_EFFECT_RADIUS,
			(l, t) -> {
				new PartialParticle(Particle.REDSTONE, l, 3, 0.1, 0.1, 0.1, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
			}
		);
		final int units1 = (int) Math.ceil(8 * CAST_EFFECT_RADIUS);
		final int units2 = (int) Math.ceil(2.4 * CAST_EFFECT_RADIUS);
		final double thresh = 0.65;
		ParticleUtils.drawCurve(mCenter, 0, units1, mFront,
			t -> FastUtils.cosDeg(140.0 * t / units1),
			t -> t < thresh * units1 ? 0 : FastUtils.sinDeg(40.0 * (t - thresh * units1) / ((1 - thresh) * units1)) * (1.0 * t * t / units1 / units1),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units1, mFront,
			t -> FastUtils.cosDeg(140.0 * t / units1),
			t -> t < thresh * units1 ? 0 : -FastUtils.sinDeg(40.0 * (t - thresh * units1) / ((1 - thresh) * units1)) * (1.0 * t * t / units1 / units1),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units2, mFront,
			t -> 0.6 * t / units2,
			t -> 0.6 - 0.25 * t / units2,
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units2, mFront,
			t -> 0.6 * t / units2,
			t -> -(0.6 - 0.25 * t / units2),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
	}

	@Override
	public void starfallCastTrail(Location loc, Player mPlayer) {
		new PartialParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 6, 0.1, 0.1, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void starfallFallEffect(World world, Player mPlayer, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 1);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 0.8f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.2f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.2f, 0.6f);
		new PartialParticle(Particle.FLAME, loc, 15, 0.25, 0.25, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.TOTEM, loc, 10, 0.25, 0.5, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 20, 0.4, 0.4, 0.4, 0.1, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.BLOCK_CRACK, loc.clone().add(0, 0.5, 0), 15, 0.2, 0.2, 0.2, 0.1, Bukkit.createBlockData(Material.GOLD_BLOCK)).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void starfallLandEffect(World world, Player mPlayer, Location loc) {
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.7f, 0.6f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 0.65f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.4f, 0.7f);
		new PartialParticle(Particle.FLAME, loc, 150, 0, 0, 0, 0.235).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 75, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 350, 2.5, 1.75, 2.5, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 300, 2.5, 1.75, 2.5, 0, BURN_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 150, 2.5, 1.75, 2.5, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
	}

}
