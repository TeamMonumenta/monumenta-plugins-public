package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.GalleryCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
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

public class BloodyRetaliationCS extends SanctifiedArmorCS implements GalleryCS {
	// Gallery set: blood

	public static final String NAME = "Bloody Retaliation";

	private static final Particle.DustOptions BLOODY_COLOR1 = new Particle.DustOptions(Color.fromRGB(149, 39, 33), 1.0f);
	private static final Particle.DustOptions BLOODY_COLOR2 = new Particle.DustOptions(Color.fromRGB(201, 46, 36), 1.2f);
	private static final Particle.DustOptions INJURY_COLOR = new Particle.DustOptions(Color.fromRGB(79, 24, 17), 1.2f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"The light cannot protect you forever.",
			"It was never meant to be the color of blood.",
			"For now this sanguine light protects you too."
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.SANCTIFIED_ARMOR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.REDSTONE_TORCH;
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
	public void sanctOnTrigger1(World world, Player mPlayer, Location loc, LivingEntity source) {
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.5f, 1.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.25f, 1.75f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 5f, 0.5f);
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 6f, 2f);

		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.65f, 1.5f);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.45f, 1.75f);
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 6f, 0.75f);
				world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 4f, 2f);
			}
		}.runTaskLater(Plugin.getInstance(), 3);

		Location mCenter = loc.clone().add(0, source.getHeight() / 2, 0);
		Vector mFront = new Vector(loc.getX() - mPlayer.getLocation().getX(), 0, loc.getZ() - mPlayer.getLocation().getZ()).normalize();
		new PartialParticle(Particle.DAMAGE_INDICATOR, mCenter, 15, 0.2, 0.2, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mCenter, 20, 0.3, 0.5, 0.3, 0.1, INJURY_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mCenter, 30, 0.3, 0.5, 0.3, 0.1, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);

		final double offsetR1 = FastUtils.RANDOM.nextDouble() - 0.5;
		final double offsetU1 = FastUtils.RANDOM.nextDouble() - 0.5;
		ParticleUtils.drawCurve(mCenter, -10, 6, mFront,
			t -> 0.25 * t,
			t -> offsetR1 * t / 5,
			t -> offsetU1 * t / 5,
			(l, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, l, (10 - t) / 2, 0.05, 0.05, 0.05, 0.01, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), 1 + (t + 10) / 5)
		);

		ParticleUtils.drawCurve(mCenter, 0, 8, mFront,
			t -> FastUtils.sin(t * 3.1416 / 16) - 0.75,
			t -> 0,
			t -> 2.5 - 0.5 * t,
			(l, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, l, (10 - t) / 2, 0.05, 0.05, 0.05, 0.01, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SOUL_FIRE_FLAME, l, 1, 0, 0, 0, 0.005).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), 4 + t / 2)
		);
	}

	@Override
	public void sanctOnTrigger2(World world, Player mPlayer, Location loc, LivingEntity source) {
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 2f, 1.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.65f, 1.75f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 7f, 0.5f);
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 7f, 2f);

		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.5f, 1.5f);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.25f, 1.75f);
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 5f, 0.5f);
				world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 6f, 2f);
			}
		}.runTaskLater(Plugin.getInstance(), 3);

		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.65f, 1.5f);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.45f, 1.75f);
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 6f, 0.75f);
				world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 4f, 2f);
			}
		}.runTaskLater(Plugin.getInstance(), 5);

		Location mCenter = loc.clone().add(0, source.getHeight() / 2, 0);
		Vector mFront = new Vector(loc.getX() - mPlayer.getLocation().getX(), 0, loc.getZ() - mPlayer.getLocation().getZ()).normalize();
		new PartialParticle(Particle.DAMAGE_INDICATOR, mCenter, 25, 0.3, 0.5, 0.3, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mCenter, 30, 0.3, 0.5, 0.3, 0.1, INJURY_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mCenter, 35, 0.3, 0.5, 0.3, 0.1, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);

		final double offsetR1 = 1.33 * (FastUtils.RANDOM.nextDouble() - 0.5);
		final double offsetU1 = 1.33 * (FastUtils.RANDOM.nextDouble() - 0.5);
		ParticleUtils.drawCurve(mCenter, -10, 6, mFront,
			t -> 0.25 * t,
			t -> offsetR1 * t / 5,
			t -> offsetU1 * t / 5,
			(l, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, l, (10 - t) / 2, 0.05, 0.05, 0.05, 0.01, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), 1 + (t + 10) / 5)
		);

		final double offsetR2 = 2 * (FastUtils.RANDOM.nextDouble() - 0.5);
		final double offsetU2 = 2 * (FastUtils.RANDOM.nextDouble() - 0.5);
		ParticleUtils.drawCurve(mCenter, -12, 8, mFront,
			t -> 0.25 * t,
			t -> offsetR2 * t / 5,
			t -> offsetU2 * t / 5,
			(l, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, l, (14 - t) / 3, 0.05, 0.05, 0.05, 0.01, BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), 4 + (t + 12) / 5)
		);

		ParticleUtils.drawCurve(mCenter, 0, 10, mFront,
			t -> FastUtils.sin(t * 3.1416 / 16) - 0.75,
			t -> 0,
			t -> 2.5 - 0.4 * t,
			(l, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, l, (14 - t) / 2, 0.05, 0.05, 0.05, 0.01, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SOUL_FIRE_FLAME, l, 2, 0, 0, 0, 0.005).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), 4 + t / 2)
		);
	}

	@Override
	public void sanctOnHeal(Player mPlayer, LivingEntity enemy) {
		Location location = mPlayer.getLocation();
		mPlayer.playSound(location, Sound.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 2f, 0.8f);
		mPlayer.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.5f, 0.65f);
		mPlayer.playSound(location, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1.25f, 2f);

		Location mCenter = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
		Vector mFront = new Vector(location.getX() - enemy.getLocation().getX(), 0, location.getZ() - enemy.getLocation().getZ());
		double l = mFront.length();
		int units = (int) Math.ceil(l * 3.5);

		ParticleUtils.drawCurve(mCenter, 0, units, mFront.normalize(),
			t -> l * t / units,
			t -> 0.5 * FastUtils.sin(t * 3.1416 / units) * FastUtils.sin(t * 3.1416 / 6),
			t -> 0.5 * FastUtils.sin(t * 3.1416 / units) * FastUtils.cos(t * 3.1416 / 6),
			(loc, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), t / 2)
		);
		ParticleUtils.drawCurve(mCenter, 0, units, mFront.normalize(),
			t -> l * t / units,
			t -> -0.5 * FastUtils.sin(t * 3.1416 / units) * FastUtils.sin(t * 3.1416 / 6),
			t -> -0.5 * FastUtils.sin(t * 3.1416 / units) * FastUtils.cos(t * 3.1416 / 6),
			(loc, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), t / 2)
		);

	}
}
