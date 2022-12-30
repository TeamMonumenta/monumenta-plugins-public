package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPeriodic;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PrestigiousRemedyCS extends WardingRemedyCS implements PrestigeCS {

	public static final String NAME = "Prestigious Remedy";

	private static final Particle.DustOptions GOLD_COLOR1 = new Particle.DustOptions(Color.fromRGB(255, 207, 63), 1.8f);
	private static final Particle.DustOptions GOLD_COLOR2 = new Particle.DustOptions(Color.fromRGB(240, 255, 79), 2.8f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 239, 207), 1.5f);
	private static final BlockData GOLD_DUST = Bukkit.createBlockData(Material.GOLD_BLOCK);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"REMEDY_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.WARDING_REMEDY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_APPLE;
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
	public void remedyStartEffect(World world, Location loc, Player mPlayer, double radius) {
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1f, 1.25f);
		world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.6f, 0.8f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 2f, 0.5f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.75f, 0.55f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.5f, 0.65f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2.4f, 0.6f);

		float delta = (float) (radius / 2.1);
		new PartialParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 75, 0.5, 0.5, 0.5, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 60, 0.25, 0.25, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, (int) Math.ceil(radius * 16), delta, delta, delta, 0.1, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, (int) Math.ceil(radius * 6), delta, delta, delta, 0.1, GOLD_COLOR1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, (int) Math.ceil(radius * 4), delta, delta, delta, 0.1, GOLD_COLOR2).spawnAsPlayerActive(mPlayer);

		Location mCenter = loc.clone().add(0, 2.4, 0);
		Vector mFront = loc.getDirection().clone().setY(0).normalize().multiply(radius);
		int units = (int) Math.ceil(radius * 3.6);
		double para1 = 0.36;
		// Draw 水
		ParticleUtils.drawCurve(mCenter, -units, units, mFront,
			t -> 0.8 * t / units,
			t -> -0.05 * FastUtils.sin(3.1416 * t / units),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.FALLING_DUST, l, 3, 0.1, 0, 0.1, 0, GOLD_DUST).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, -units, units, mFront,
			t -> 0.28 * FastUtils.sin(para1 * 3.1416 * t / units) / FastUtils.cos(para1 * 3.1416 * t / units),
			t -> 0.24 / FastUtils.cos(para1 * 3.1416 * t / units),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.FALLING_DUST, l, 3, 0.1, 0, 0.1, 0, GOLD_DUST).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, -units, units, mFront,
			t -> 0.28 * FastUtils.sin(para1 * 3.1416 * t / units) / FastUtils.cos(para1 * 3.1416 * t / units),
			t -> -0.24 / FastUtils.cos(para1 * 3.1416 * t / units),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.FALLING_DUST, l, 3, 0.1, 0, 0.1, 0, GOLD_DUST).spawnAsPlayerActive(mPlayer)
		);
	}

	@Override
	public ImmutableList<PPPeriodic> remedyPeriodicEffect(Location loc) {
		return new ImmutableList.Builder<PPPeriodic>()
			.add(new PPPeriodic(Particle.END_ROD, loc).count(1).delta(0.35, 0.15, 0.35).extra(0.05))
			.add(new PPPeriodic(Particle.REDSTONE, loc).count(1).delta(0.4, 0.2, 0.4).data(GOLD_COLOR2))
			.build();
	}

	@Override
	public void remedyPulseEffect(World world, Location playerLoc, Player mPlayer, int pulse, int maxPulse, double radius) {
		float pitch = (float) Math.pow(2, 1.0 * pulse / maxPulse - 0.75);
		world.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1f, pitch);
		world.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, SoundCategory.PLAYERS, 0.9f, pitch);
		world.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 0.85f, pitch);

		float delta = (float) (radius / 2.1);
		double radiusShrink = radius * (maxPulse - pulse + 1) / (maxPulse + 1);
		int countShrink = (int) Math.ceil(radiusShrink * radiusShrink * 3.1416 * 1.6);
		new PPCircle(Particle.REDSTONE, playerLoc.clone().add(0, 0.15, 0), radius).ringMode(true).count((int) Math.ceil(radius * 9.6)).data(LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.REDSTONE, playerLoc.clone().add(0, 0.45, 0), 0.85 * radius).ringMode(true).count((int) Math.ceil(radius * 7.2)).data(LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.REDSTONE, playerLoc.clone().add(0, 0.15, 0), radiusShrink).ringMode(false).count(countShrink).data(GOLD_COLOR1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, playerLoc, (int) Math.ceil(radius * 3.6), delta, delta, delta, 0, GOLD_COLOR2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, playerLoc, (int) Math.ceil(radius * 6.4), delta, delta, delta, 0.05).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_INSTANT, playerLoc, (int) Math.ceil(radius * 6.4), delta, delta, delta, 0.1).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void remedyApplyEffect(Player mPlayer, Player p) {
		new PartialParticle(Particle.REDSTONE, p.getLocation().clone().add(0, 0.5, 0), 2, 0.5, 0.2, 0.5, GOLD_COLOR1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, p.getLocation().clone().add(0, 0.5, 0), 2, 0.35, 0.15, 0.35, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FIREWORKS_SPARK, p.getLocation().clone().add(0, 0.5, 0), 3, 0.35, 0.15, 0.35, 0).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void remedyHealBuffEffect(Player mPlayer, Player p) {
		new PartialParticle(Particle.FALLING_DUST, p.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, Bukkit.createBlockData(Material.PINK_CONCRETE_POWDER)).spawnAsPlayerBuff(mPlayer);
		new PartialParticle(Particle.REDSTONE, p.getLocation().add(0, 1, 0), 1, 0.4, 0.5, 0.4, GOLD_COLOR1).spawnAsPlayerBuff(mPlayer);
		new PartialParticle(Particle.SPELL_INSTANT, p.getLocation().add(0, 1, 0), 2, 0.25, 0.5, 0.25).spawnAsPlayerBuff(mPlayer);
	}

}
