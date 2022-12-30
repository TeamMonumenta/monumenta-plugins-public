package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.GalleryCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.GameMode;
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
import org.jetbrains.annotations.Nullable;

public class SanguineAegisCS extends PrismaticShieldCS implements GalleryCS {
	//Gallery theme1 blood

	public static final String NAME = "Sanguine Aegis";

	private static final Particle.DustOptions BLOOD = new Particle.DustOptions(Color.fromRGB(188, 15, 11), 1.0f);
	private static final Particle.DustOptions DARK_BLOOD = new Particle.DustOptions(Color.fromRGB(134, 12, 7), 1.0f);
	private static final int HEAL_UNIT = 9;

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"Blood and water are much the same in the",
			"dream. With what is left of your strength,",
			"seek hope, as the sanguine rush engulfs."
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.PRISMATIC_SHIELD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MUSIC_DISC_PIGSTEP;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public GalleryCS.GalleryMap getMap() {
		return GalleryCS.GalleryMap.SANGUINE;
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
	public void prismaEffect(World world, Player mPlayer, double radius) {
		Location location = mPlayer.getLocation();
		new PartialParticle(Particle.SOUL, location.clone().add(0, 1.15, 0), 25, 0.35, 0.45, 0.35, 0.5).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, location.clone().add(0, 1.15, 0), 50, 0.35, 0.45, 0.35, 0.5).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIMSON_SPORE, location.clone().add(0, 1.15, 0), 100, 0, 0.2, 0, 0).spawnAsPlayerActive(mPlayer);
		world.playSound(location, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 3.5f, 0.9f);
		world.playSound(location, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 7.5f, 0.5f);
		world.playSound(location, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, SoundCategory.PLAYERS, 5.5f, 0.5f);
		world.playSound(location, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 4f, 2f);
		world.playSound(location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.75f, 2f);

		PPCircle flame = new PPCircle(Particle.SOUL_FIRE_FLAME, location.clone().add(0, 0.125, 0), radius).ringMode(true);
		flame.delta(0.05).extra(0.01);
		flame.count((int) Math.ceil(20 * radius)).spawnAsPlayerActive(mPlayer);
		flame.count((int) Math.ceil(16 * radius)).radius(0.8 * radius).spawnAsPlayerActive(mPlayer);

		Vector front = VectorUtils.rotateYAxis(new Vector(0, 0, radius), 45 + location.clone().getYaw());
		final int units = (int) Math.ceil(radius * 3.1416);

		ParticleUtils.drawCurve(location.clone().add(0, 0.125, 0), -units, units, front,
			t -> 0.96 * t / units,
			t -> 0.32 * FastUtils.cos(t * Math.PI / 2 / units) + 0.04,
			t -> 0,
			(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0.05, 0.05, 0.05, 0, BLOOD).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(location.clone().add(0, 0.125, 0), -units, units, front,
			t -> 0.96 * t / units,
			t -> -0.32 * FastUtils.cos(t * Math.PI / 2 / units) + 0.04,
			t -> 0,
			(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0.05, 0.05, 0.05, 0, BLOOD).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(location.clone().add(0, 0.125, 0), -units, units, front,
			t -> 0.32 * FastUtils.cos(t * Math.PI / 2 / units) + 0.04,
			t -> 0.96 * t / units,
			t -> 0,
			(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0.05, 0.05, 0.05, 0, BLOOD).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(location.clone().add(0, 0.125, 0), -units, units, front,
			t -> -0.32 * FastUtils.cos(t * Math.PI / 2 / units) + 0.04,
			t -> 0.96 * t / units,
			t -> 0,
			(loc, t) -> new PartialParticle(Particle.REDSTONE, loc, 2, 0.05, 0.05, 0.05, 0, BLOOD).spawnAsPlayerActive(mPlayer)
		);

	}

	@Override
	public void prismaOnStun(LivingEntity mob, int stunTime, Player mPlayer) {
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				if (mTicks++ >= stunTime) {
					this.cancel();
				}
				new PartialParticle(Particle.REDSTONE, mob.getEyeLocation(), 5, 0.2, 0.4, 0.2, 0, DARK_BLOOD).spawnAsPlayerActive(mPlayer);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void prismaOnHeal(Player mPlayer) {
		Location location = mPlayer.getLocation();
		mPlayer.getWorld().playSound(location.clone(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, SoundCategory.PLAYERS, 3f, 0.6f);

		Vector front = VectorUtils.rotateYAxis(new Vector(0, 0, 2), location.clone().getYaw());
		ParticleUtils.drawCurve(location.clone().add(0, 0.125, 0), -HEAL_UNIT, HEAL_UNIT, front,
			t -> 0.25 * FastUtils.sin(t * Math.PI / HEAL_UNIT),
			t -> 1.0 * t / HEAL_UNIT,
			t -> 0,
			(loc, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.01, 0.01, 0.01, 0, DARK_BLOOD).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), (HEAL_UNIT - Math.abs(t)) / 2)
		);
		ParticleUtils.drawCurve(location.clone().add(0, 0.125, 0), -HEAL_UNIT, HEAL_UNIT, front,
			t -> -0.25 * FastUtils.sin(t * Math.PI / HEAL_UNIT),
			t -> 1.0 * t / HEAL_UNIT,
			t -> 0,
			(loc, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.01, 0.01, 0.01, 0, DARK_BLOOD).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), (HEAL_UNIT - Math.abs(t)) / 2)
		);

		ParticleUtils.drawCurve(location.clone().add(0, 0.125, 0), -HEAL_UNIT, HEAL_UNIT, front,
			t -> 1.0 * t / HEAL_UNIT,
			t -> 0.25 * FastUtils.sin(t * Math.PI / HEAL_UNIT),
			t -> 0,
			(loc, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.01, 0.01, 0.01, 0, DARK_BLOOD).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), (HEAL_UNIT - Math.abs(t)) / 2)
		);
		ParticleUtils.drawCurve(location.clone().add(0, 0.125, 0), -HEAL_UNIT, HEAL_UNIT, front,
			t -> 1.0 * t / HEAL_UNIT,
			t -> -0.25 * FastUtils.sin(t * Math.PI / HEAL_UNIT),
			t -> 0,
			(loc, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.01, 0.01, 0.01, 0, DARK_BLOOD).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), (HEAL_UNIT - Math.abs(t)) / 2)
		);
	}
}
