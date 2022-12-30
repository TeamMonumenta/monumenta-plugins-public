package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.scriptedquests.Plugin;
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

public class PrestigiousSlamCS extends MeteorSlamCS implements PrestigeCS {

	public static final String NAME = "Prestigious Slam";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(199, 175, 31), 1.25f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.25f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"SLAM_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.METEOR_SLAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.RAW_GOLD;
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
	public void slamCastEffect(World world, Location location, Player mPlayer) {
		world.playSound(location, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.8f, 1.6f);
		world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.5f, 0.75f);
		world.playSound(location, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 2f, 0.65f);
		new PartialParticle(Particle.FLAME, location, 40, 2, 0.5, 2, 0.25).spawnAsPlayerActive(mPlayer);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (++mTicks > 20) {
					this.cancel();
					return;
				}
				double rad1 = mPlayer.getLocation().getYaw() * 3.1416 / 180 + mTicks * 0.08 * 3.1416;
				double rad2 = rad1 + 3.1416;
				double rad3 = rad1 + 0.04 * 3.1416;
				double rad4 = rad2 + 0.04 * 3.1416;
				double dist = 1.6 - 0.04 * mTicks;
				Location loc = mPlayer.getLocation();
				new PartialParticle(Particle.REDSTONE, loc.clone().add(dist * FastUtils.cos(rad1), mTicks * 0.055, dist * FastUtils.sin(rad1)),
					2, 0.15, 0.15, 0.15, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, loc.clone().add(dist * FastUtils.cos(rad2), mTicks * 0.055, dist * FastUtils.sin(rad2)),
					1, 0.15, 0.15, 0.15, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, loc.clone().add(dist * FastUtils.cos(rad3), (mTicks + 0.5) * 0.055, dist * FastUtils.sin(rad3)),
					1, 0.15, 0.15, 0.15, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, loc.clone().add(dist * FastUtils.cos(rad4), (mTicks + 0.5) * 0.055, dist * FastUtils.sin(rad4)),
					2, 0.15, 0.15, 0.15, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void slamAttackEffect(World world, Location location, Player mPlayer, double radius, double fallDistance) {
		float volumeScale = (float) Math.min(Math.log(fallDistance), 1);
		world.playSound(location, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, volumeScale * 1.1f, 0.65f);
		world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, volumeScale * 0.8f, 1.6f);
		world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, volumeScale * 1.2f, 0.55f);
		world.playSound(location, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, volumeScale * 0.65f, 1.9f);

		new PartialParticle(Particle.FLAME, location, 60, 0, 0, 0, 0.15).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, location, 20, 0, 0, 0, 0.4).spawnAsPlayerActive(mPlayer);
		Location mCenter = location.clone().add(0, 0.125, 0);
		Vector mFront = mPlayer.getLocation().getDirection().clone().setY(0).normalize().multiply(radius);
		int units = (int) Math.ceil(radius * 1.8);
		ParticleUtils.drawCurve(mCenter, 0, units * 6, mFront,
			t -> FastUtils.cos(3.1416 * t / (units * 6)),
			t -> 0.025 + FastUtils.sin(3.1416 * t / (units * 6)),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.02, 0, 0.02, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 3, mFront,
			t -> 0.5 + 0.5 * FastUtils.cos(3.1416 * t / (units * 3)),
			t -> 0.025 - 0.5 * FastUtils.sin(3.1416 * t / (units * 3)),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 3, mFront,
			t -> -0.5 + 0.5 * FastUtils.cos(3.1416 * t / (units * 3)),
			t -> 0.025 + 0.5 * FastUtils.sin(3.1416 * t / (units * 3)),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 2, mFront,
			t -> -0.5 + 0.167 * FastUtils.cos(3.1416 * 2 * t / (units * 2)),
			t -> 0.167 * FastUtils.sin(3.1416 * 2 * t / (units * 2)),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 6, mFront,
			t -> FastUtils.cos(3.1416 * t / (units * 6)),
			t -> -0.025 - FastUtils.sin(3.1416 * t / (units * 6)),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.02, 0, 0.02, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 3, mFront,
			t -> 0.5 + 0.5 * FastUtils.cos(3.1416 * t / (units * 3)),
			t -> -0.025 - 0.5 * FastUtils.sin(3.1416 * t / (units * 3)),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 3, mFront,
			t -> -0.5 + 0.5 * FastUtils.cos(3.1416 * t / (units * 3)),
			t -> -0.025 + 0.5 * FastUtils.sin(3.1416 * t / (units * 3)),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, units * 2, mFront,
			t -> 0.5 + 0.167 * FastUtils.cos(3.1416 * 2 * t / (units * 2)),
			t -> 0.167 * FastUtils.sin(3.1416 * 2 * t / (units * 2)),
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		);
	}
}
