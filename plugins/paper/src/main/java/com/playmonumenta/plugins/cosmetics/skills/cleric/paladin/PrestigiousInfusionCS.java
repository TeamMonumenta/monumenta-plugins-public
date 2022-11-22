package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PrestigiousInfusionCS extends LuminousInfusionCS implements PrestigeCS {

	public static final String NAME = "Prestigious Infusion";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.15f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
	private static final Particle.DustOptions BURN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 168, 16), 1.3f);
	private static final Particle.DustOptions[] DATA = {
		LIGHT_COLOR,
		LIGHT_COLOR,
		GOLD_COLOR,
		GOLD_COLOR,
		BURN_COLOR
	};

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"INFUSION_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.LUMINOUS_INFUSION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE_DUST;
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
	public void infusionStartEffect(World world, Player mPlayer) {
		MessagingUtils.sendActionBarMessage(mPlayer, "Holy energy radiates from prestige...");
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.6f, 1.2f);
		world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.95f, 1.4f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.25f, 0.75f);
		new PartialParticle(Particle.SPELL, mPlayer.getLocation(), 50, 0.5, 0.125, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
		final float mTheta = loc.getYaw() + FastUtils.RANDOM.nextFloat(72);
		for (int i = 0; i < DATA.length; i++) {
			final double mRadius = (DATA.length - i) * 0.8;
			final int iter = i;
			new BukkitRunnable() {
				@Override
				public void run() {
					new PPLine(Particle.REDSTONE,
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta), 0.25, mRadius * FastUtils.sinDeg(mTheta)),
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 72), 0.25, mRadius * FastUtils.sinDeg(mTheta + 72)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(mPlayer);
					new PPLine(Particle.REDSTONE,
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 72), 0.25, mRadius * FastUtils.sinDeg(mTheta + 72)),
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 144), 0.25, mRadius * FastUtils.sinDeg(mTheta + 144)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(mPlayer);
					new PPLine(Particle.REDSTONE,
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 144), 0.25, mRadius * FastUtils.sinDeg(mTheta + 144)),
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 216), 0.25, mRadius * FastUtils.sinDeg(mTheta + 216)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(mPlayer);
					new PPLine(Particle.REDSTONE,
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 216), 0.25, mRadius * FastUtils.sinDeg(mTheta + 216)),
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 288), 0.25, mRadius * FastUtils.sinDeg(mTheta + 288)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(mPlayer);
					new PPLine(Particle.REDSTONE,
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta + 288), 0.25, mRadius * FastUtils.sinDeg(mTheta + 288)),
						loc.clone().add(mRadius * FastUtils.cosDeg(mTheta), 0.25, mRadius * FastUtils.sinDeg(mTheta)))
						.data(DATA[iter]).countPerMeter(16).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), i);
		}
	}

	@Override
	public void infusionExpireMsg(Player mPlayer) {
		MessagingUtils.sendActionBarMessage(mPlayer, "The light fades in glory...");
	}

	@Override
	public void infusionTickEffect(Player mPlayer, int tick) {
		Location rightHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.45).subtract(0, .8, 0);
		Location leftHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.45).subtract(0, .8, 0);
		new PartialParticle(Particle.REDSTONE, leftHand, 1, 0.05, 0.05, 0.05, 0, LIGHT_COLOR).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, rightHand, 1, 0.05, 0.05, 0.05, 0, LIGHT_COLOR).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		if (FastUtils.RANDOM.nextDouble() < 0.75) {
			new PartialParticle(Particle.REDSTONE, leftHand, 1, 0.1, 0.1, 0.1, 0, GOLD_COLOR).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		}
		if (FastUtils.RANDOM.nextDouble() < 0.75) {
			new PartialParticle(Particle.REDSTONE, rightHand, 1, 0.1, 0.1, 0.1, 0, GOLD_COLOR).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		}
	}

	@Override
	public void infusionHitEffect(World world, Player mPlayer, LivingEntity damagee) {
		Location mCenter = damagee.getLocation();
		world.playSound(mCenter, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(mCenter, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 3f, 1.5f);
		world.playSound(mCenter, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.25f, 1.25f);
		new PartialParticle(Particle.REDSTONE, mCenter, 125, 2.5f, 0.25f, 2.5f, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mCenter, 75, 2f, 0.15f, 2f, 0, BURN_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, mCenter, 100, 0.1f, 0.025f, 0.1f, 0.25).spawnAsPlayerActive(mPlayer);

		Vector mFront = mPlayer.getLocation().getDirection().clone().setY(0).normalize();
		ParticleUtils.drawCurve(mCenter, 1, 31, mFront,
			t -> 0,
			t -> 0,
			t -> 0.26 * t,
			(l, t) -> {
				if (t <= 6) {
					int units = (int) Math.ceil(t * 0.18 * 3.2);
					ParticleUtils.drawCurve(l, -units, units, mFront,
						u -> 0,
						u -> 0.17 * t * u / units,
						u -> 0,
						(l2, u) -> new PartialParticle(Particle.REDSTONE, l2, 1, 0.15, 0.15, 0.15, 0, BURN_COLOR).spawnAsPlayerActive(mPlayer)
					);
				} else if (t <= 20) {
					new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0.05, 0.05, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
					ParticleUtils.drawCurve(l, -6, 6, mFront,
						u -> 0,
						u -> 0.17 * u,
						u -> 0,
						(l2, u) -> new PartialParticle(Particle.REDSTONE, l2, 1, 0.15, 0.15, 0.15, 0, BURN_COLOR).spawnAsPlayerActive(mPlayer)
					);
				} else {
					new PartialParticle(Particle.REDSTONE, l, 4, 0.15, 0.15, 0.15, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
				}
			}
		);
		ParticleUtils.drawCurve(mCenter, -9, 9, mFront,
			t -> 0,
			t -> 0.21 * t,
			t -> 5.2,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 3, 0.15, 0.15, 0.15, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
	}

	@Override
	public void infusionSpreadEffect(World world, Player mPlayer, LivingEntity damagee, LivingEntity e, float volume) {
		Location eLoc = e.getLocation();
		ParticleUtils.drawRing(eLoc.clone().add(0, 1.6, 0), 24, new Vector(0, 1, 0), 1.6,
			(l, t) -> new PartialParticle(Particle.FALLING_DUST, l, 1, 0, 0, 0, 0,
				Bukkit.createBlockData(Material.YELLOW_CONCRETE_POWDER))
				.spawnAsPlayerActive(mPlayer)
		);
	}
}
