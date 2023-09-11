package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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
import org.jetbrains.annotations.Nullable;

public class PrestigiousStarfallCS extends StarfallCS implements PrestigeCS {

	public static final String NAME = "Prestigious Starfall";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.25f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);
	private static final Particle.DustOptions BURN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 180, 0), 1.25f);
	private static double CAST_EFFECT_RADIUS = 3.5;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The weight of fate is oft",
			"heavier than any stone."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE;
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
	public void starfallCastEffect(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 1f, 0.65f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.5f, 0.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2f, 0.8f);
		new PartialParticle(Particle.FLAME, loc, 45, 0.35f, 0.1f, 0.35f, 0.2f).spawnAsPlayerActive(player);

		// Draw 火
		Location center = loc.clone().add(0, 0.25, 0);
		Vector front = loc.getDirection().setY(0).normalize().multiply(CAST_EFFECT_RADIUS);
		Vector right = VectorUtils.rotateYAxis(front, 90);
		new PPCircle(Particle.REDSTONE, center, CAST_EFFECT_RADIUS).data(BURN_COLOR).countPerMeter(12).delta(0.1).spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, center, CAST_EFFECT_RADIUS).data(GOLD_COLOR).countPerMeter(8).spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, center, 0.6 * CAST_EFFECT_RADIUS).data(LIGHT_COLOR).countPerMeter(10).delta(0.1).spawnAsPlayerActive(player);

		final int count1 = (int) Math.ceil(16 * CAST_EFFECT_RADIUS);
		final int count2 = (int) Math.ceil(4.8 * CAST_EFFECT_RADIUS);
		final double thresh = 0.65;

		new PPParametric(Particle.REDSTONE, center,
			(t, builder) -> {
				double frontOffset = FastUtils.cosDeg(140 * t);
				double rightOffset = t < thresh ? 0 : FastUtils.sinDeg(40 * (t - thresh) / (1 - thresh)) * (t * t);
				Location l = center.clone().add(front.clone().multiply(frontOffset)).add(right.clone().multiply(rightOffset));
				builder.location(l);
			}).data(GOLD_COLOR).count(count1).spawnAsPlayerActive(player);

		new PPParametric(Particle.REDSTONE, center,
			(t, builder) -> {
				double frontOffset = FastUtils.cosDeg(140 * t);
				double rightOffset = t < thresh ? 0 : -FastUtils.sinDeg(40 * (t - thresh) / (1 - thresh)) * (t * t);
				Location l = center.clone().add(front.clone().multiply(frontOffset)).add(right.clone().multiply(rightOffset));
				builder.location(l);
			}).data(GOLD_COLOR).count(count1).spawnAsPlayerActive(player);

		new PPLine(Particle.REDSTONE,
			center.clone().add(right.clone().multiply(0.6)),
			center.clone().add(right.clone().multiply(0.35)).add(front.clone().multiply(0.6)))
			.data(GOLD_COLOR).count(count2).spawnAsPlayerActive(player);

		new PPLine(Particle.REDSTONE,
			center.clone().add(right.clone().multiply(-0.6)),
			center.clone().add(right.clone().multiply(-0.35)).add(front.clone().multiply(0.6)))
			.data(GOLD_COLOR).count(count2).spawnAsPlayerActive(player);
	}

	@Override
	public void starfallCastTrail(Location loc, Player player) {
		new PartialParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 6, 0.1, 0.1, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(player);
	}

	@Override
	public void starfallFallEffect(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 1);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 0.8f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.2f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.2f, 0.6f);
		new PartialParticle(Particle.FLAME, loc, 15, 0.25, 0.25, 0.25, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.TOTEM, loc, 10, 0.25, 0.5, 0.25, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 20, 0.4, 0.4, 0.4, 0.1, LIGHT_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, loc.clone().add(0, 0.5, 0), 15, 0.2, 0.2, 0.2, 0.1, Bukkit.createBlockData(Material.GOLD_BLOCK)).spawnAsPlayerActive(player);
	}

	@Override
	public void starfallLandEffect(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.7f, 0.6f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 0.65f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.4f, 0.7f);
		new PartialParticle(Particle.FLAME, loc, 150, 0, 0, 0, 0.235).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, loc, 75, 0, 0, 0, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 350, 2.5, 1.75, 2.5, 0, GOLD_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 300, 2.5, 1.75, 2.5, 0, BURN_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 150, 2.5, 1.75, 2.5, 0, LIGHT_COLOR).spawnAsPlayerActive(player);
	}

}
