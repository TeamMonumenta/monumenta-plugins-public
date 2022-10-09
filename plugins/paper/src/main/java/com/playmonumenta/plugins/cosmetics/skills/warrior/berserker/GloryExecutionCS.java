package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.GalleryCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
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

public class GloryExecutionCS extends GloriousBattleCS implements GalleryCS {
	// Gallery theme1: blood

	public static final String NAME = "Glory Execution";

	private static final Particle.DustOptions BLOODY_COLOR1 = new Particle.DustOptions(Color.fromRGB(175, 33, 19), 1.0f);
	private static final Particle.DustOptions BLOODY_COLOR2 = new Particle.DustOptions(Color.fromRGB(224, 40, 24), 1.1f);
	private static final int LAND_ANIM_FRAMES = 5;

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"What makes glory? What makes a fool?",
			"Loyalty is the trailhead, but for now",
			"start from the next execution."
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.GLORIOUS_BATTLE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_AXE;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public GalleryCS.GalleryMap getMap() {
		return GalleryCS.GalleryMap.SANGUINE;
	}

	@Override
	public boolean isUnlocked(Player mPlayer) {
		return ScoreboardUtils.getScoreOrDefault(mPlayer, GALLERY_COMPLETE_SCB, 0) >= 1
			|| mPlayer.getGameMode() == GameMode.CREATIVE;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("Complete Sanguine Halls to unlock!").toArray(new String[0]);
	}

	@Override
	public void gloryStart(World world, Player mPlayer, Location location, int duration) {
		world.playSound(location, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 5.5f, 0.85f);
		world.playSound(location, Sound.ENTITY_HORSE_GALLOP, SoundCategory.PLAYERS, 3f, 0.75f);
		world.playSound(location, Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 4f, 0.5f);
		world.playSound(location, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.2f, 2f);
		world.playSound(location, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 0.55f, 0.65f);

		PPCircle ring1 = new PPCircle(Particle.CRIT_MAGIC, location.clone().add(0, 0.5, 0), 0).count(13).ringMode(true);
		PPCircle ring2 = new PPCircle(Particle.REDSTONE, location.clone().add(0, 0.5, 0), 0).count(13).ringMode(true).data(BLOODY_COLOR1);

		new BukkitRunnable() {
			int mTimes = duration / 2;
			@Override
			public void run() {
				if (mTimes-- <= 0) {
					this.cancel();
					return;
				}
				ring1.radius(3.6 * (mTimes - 1) / mTimes).spawnAsPlayerActive(mPlayer);
				ring2.radius(3.2 * (mTimes - 1) / mTimes).spawnAsPlayerActive(mPlayer);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 2);
	}

	@Override
	public void gloryTick(Player mPlayer, int tick) {
		Vector mFront = mPlayer.getLocation().getDirection();
		Location loc = mPlayer.getLocation().add(0, mPlayer.getHeight() / 2, 0);

		ParticleUtils.drawCurve(loc, tick, tick + 2, mFront,
			t -> 0.5 * (t - tick - 1),
			t -> 1.25 * FastUtils.cos(t * 3.1416 / 12),
			t -> 1.25 * FastUtils.sin(t * 3.1416 / 12),
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, 0, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer)
		);

		ParticleUtils.drawCurve(loc, tick, tick + 2, mFront,
			t -> 0.5 * (t - tick - 1),
			t -> 1.25 * FastUtils.cos((t + 8) * 3.1416 / 12),
			t -> 1.25 * FastUtils.sin((t + 8) * 3.1416 / 12),
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, 0, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer)
		);

		ParticleUtils.drawCurve(loc, tick, tick + 2, mFront,
			t -> 0.5 * (t - tick - 1),
			t -> 1.25 * FastUtils.cos((t - 8) * 3.1416 / 12),
			t -> 1.25 * FastUtils.sin((t - 8) * 3.1416 / 12),
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, 0, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer)
		);
	}

	@Override
	public void gloryOnDamage(World world, Player mPlayer, LivingEntity target) {
		Location loc1 = mPlayer.getEyeLocation();
		Location loc2 = target.getLocation();
		Location loc3 = target.getEyeLocation();
		mPlayer.getWorld().playSound(loc2, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.75f);
		mPlayer.getWorld().playSound(loc2, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.75f, 0.8f);
		mPlayer.getWorld().playSound(loc2, Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1f, 0.65f);

		new PartialParticle(Particle.REDSTONE, loc3, 25, 0.75, 0.75, 0.75, 0.1, BLOODY_COLOR2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, loc3, 10, 0.5, 0.5, 0.5, 0.25).spawnAsPlayerActive(mPlayer);

		Vector mFront = loc3.clone().subtract(loc1).toVector();
		if (EntityUtils.isHumanlike(target)) {
			// Humanoid -> skull crash
			Vector dir = mFront.clone().normalize();
			ParticleUtils.drawCurve(loc3, -8, 8, dir,
				t -> 0,
				t -> 0.24 * t,
				t -> 0.16 * t,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.01, 0.01, 0.01, 0, BLOODY_COLOR2).spawnAsPlayerActive(mPlayer)
			);
			ParticleUtils.drawCurve(loc3, -8, 8, dir,
				t -> 0,
				t -> 0.24 * t,
				t -> -0.16 * t,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.01, 0.01, 0.01, 0, BLOODY_COLOR2).spawnAsPlayerActive(mPlayer)
			);
			ParticleUtils.drawCurve(loc3, -8, 4, dir,
				t -> 0.25 * t,
				t -> 0,
				t -> 0,
				(l, t) -> new BukkitRunnable() {
					@Override
					public void run() {
						new PartialParticle(Particle.REDSTONE, l, (6 - t) * 2, (4 - t) * 0.025, (4 - t) * 0.025, (4 - t) * 0.025, 0,
							ParticleUtils.getTransition(BLOODY_COLOR2, BLOODY_COLOR1, (4 - t) / 12.0)).spawnAsPlayerActive(mPlayer);
					}
				}.runTaskLater(Plugin.getInstance(), (t + 8) / 4)
			);

		} else if (EntityUtils.isUndead(target)) {
			// Undead -> heavy smite
			Vector dir = mFront.length() < 2 ? mFront.clone().normalize().multiply(2) : mFront;
			Vector delta = mFront.clone().normalize().multiply(-0.8);
			ParticleUtils.drawCurve(loc1, 4, 24, dir,
				t -> 1.2 * FastUtils.sin(t * 3.1416 / 36),
				t -> 0,
				t -> 1.2 * FastUtils.cos(t * 3.1416 / 36),
				(l, t) -> new BukkitRunnable() {
					@Override
					public void run() {
						new PPLine(Particle.REDSTONE, l, l.clone().add(delta))
							.data(ParticleUtils.getTransition(BLOODY_COLOR2, BLOODY_COLOR1, (12 - t) / 10.0))
							.countPerMeter(16).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SMALL_FLAME, l.clone().add(delta.clone().multiply(0.5)), 1, 0, 0.1, 0, 0.1).spawnAsPlayerActive(mPlayer);
					}
				}.runTaskLater(Plugin.getInstance(), (t - 2) / 3)
			);
		} else if (EntityUtils.isFlyingMob(target)) {
			// Flying -> cloud bomb
			Location l = loc2.clone().add(0, target.getHeight() / 2, 0);
			for (int i = 0; i <= 6; i++) {
				final int tick = i;
				new BukkitRunnable() {
					@Override
					public void run() {
						if (tick != 0) {
							ParticleUtils.drawCurve(l, 0, 15, mFront.clone().normalize(),
								t -> 0,
								t -> tick * 0.3 * FastUtils.cos(t * 3.1416 * 2 / 16),
								t -> tick * 0.3 * FastUtils.sin(t * 3.1416 * 2 / 16),
								(loc, t) -> new PartialParticle(Particle.SPELL_INSTANT, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer)
							);
						} else {
							ParticleUtils.drawCurve(l, -8, 8, mFront.clone().normalize(),
								t -> 0.25 * t,
								t -> 0,
								t -> 0,
								(loc, t) -> new PartialParticle(Particle.CLOUD, loc, 3, 0.01, 0.01, 0.01, 0.2).spawnAsPlayerActive(mPlayer)
							);
						}
					}
				}.runTaskLater(Plugin.getInstance(), 6 - tick);
			}
		} else if (EntityUtils.isWaterMob(target)) {
			// Water -> splash explode
			Location l = loc2.clone().add(0, target.getHeight() / 2, 0);
			for (int i = 5; i <= 10; i++) {
				final int t = i;
				new BukkitRunnable() {
					@Override
					public void run() {
						new PartialParticle(Particle.WATER_SPLASH, l, t * 12, t * 0.125, t * 0.125, t * 0.125, 1).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.WATER_DROP, l, t * 6, t * 0.15, t * 0.15, t * 0.15, 1).spawnAsPlayerActive(mPlayer);
					}
				}.runTaskLater(Plugin.getInstance(), t - 5);
			}
		} else {
			// Default -> X flame
			new PartialParticle(Particle.DRIP_LAVA, loc3, 80, 0.75, 0.75, 0.75, 0.5).spawnAsPlayerActive(mPlayer);
			ParticleUtils.drawCurve(loc3, -12, 12, mFront.clone().normalize(),
				t -> 0,
				t -> t * 0.075,
				t -> t * 0.15,
				(l, t) -> new PartialParticle(Particle.FLAME, l, 2, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer)
			);
			ParticleUtils.drawCurve(loc3, -12, 12, mFront.clone().normalize(),
				t -> 0,
				t -> t * 0.075,
				t -> t * -0.15,
				(l, t) -> new PartialParticle(Particle.FLAME, l, 2, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer)
			);

		}
	}

	@Override
	public void gloryOnLand(World world, Player mPlayer, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.35f, 0.55f);
		world.playSound(loc, Sound.ENTITY_HORSE_GALLOP, SoundCategory.PLAYERS, 4.5F, 0.5F);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0.6f);

		Vector mFront = mPlayer.getLocation().getDirection().setY(0).normalize().multiply(radius);
		final int units = (int) Math.ceil(LAND_ANIM_FRAMES * radius * 3);
		ParticleUtils.drawCurve(loc.clone().add(0, 0.5, 0), 0, units, mFront,
			t -> FastUtils.cos(t * 3.1416 / 13) * t / units,
			t -> FastUtils.sin(t * 3.1416 / 13) * t / units,
			t -> 0,
			(l, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), LAND_ANIM_FRAMES * t / units)
		);
		ParticleUtils.drawCurve(loc.clone().add(0, 0.5, 0), 0, units, mFront,
			t -> FastUtils.cos(t * 3.1416 / 13 - 3.1416 * 1.33) * t / units,
			t -> FastUtils.sin(t * 3.1416 / 13 - 3.1416 * 1.33) * t / units,
			t -> 0,
			(l, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), LAND_ANIM_FRAMES * t / units)
		);
		ParticleUtils.drawCurve(loc.clone().add(0, 0.5, 0), 0, units, mFront,
			t -> FastUtils.cos(t * 3.1416 / 13 + 3.1416 * 1.33) * t / units,
			t -> FastUtils.sin(t * 3.1416 / 13 + 3.1416 * 1.33) * t / units,
			t -> 0,
			(l, t) -> new BukkitRunnable() {
				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
				}
			}.runTaskLater(Plugin.getInstance(), LAND_ANIM_FRAMES * t / units)
		);

		new BukkitRunnable() {
			@Override
			public void run() {
				new PPCircle(Particle.CRIT_MAGIC, loc.clone().add(0, 0.5, 0), 0).count((int) Math.ceil(radius * 11)).ringMode(true).spawnAsPlayerActive(mPlayer);
				new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.5, 0), 0).count((int) Math.ceil(radius * 7)).ringMode(true).data(BLOODY_COLOR1).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SWEEP_ATTACK, loc, (int) Math.ceil(radius * 3), radius / 3, 0, radius / 3, 0).spawnAsPlayerActive(mPlayer);
			}
		}.runTaskLater(Plugin.getInstance(), LAND_ANIM_FRAMES);
	}
}
