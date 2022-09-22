package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class TranscCombosCS extends ViciousCombosCS {
	//Twisted theme

	public static final String NAME = "Transcendent Combos";
	private static final Color TRANSC_LINE_COLOR = Color.fromRGB(217, 242, 255);
	private static final Color TRANSC_LINE_ELITE_COLOR = Color.fromRGB(255, 196, 196);
	private static final Color TRANSC_SLASH_COLOR_TIP = Color.fromRGB(153, 220, 255);
	private static final Color TRANSC_SLASH_COLOR_BASE = Color.fromRGB(199, 236, 255);
	private static final double LINE_LENGTH = 2.25;

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"The transcendent one",
			"will be bested by none.");
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.VICIOUS_COMBOS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WITHER_SKELETON_SKULL;
	}

	@Override
	public void comboOnKill(World world, Location loc, Player mPlayer, double range, LivingEntity target) {
		loc = mPlayer.getLocation().add(0, 1, 0);
		Location eLoc = LocationUtils.getHalfHeightLocation(target);
		eLoc.setPitch(0);
		eLoc.setYaw(mPlayer.getLocation().getYaw());

		world.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 2f, 0.75f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 2f, 0.85f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2f, 1.25f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.75f);

		Location finalLoc = loc;
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			world.playSound(finalLoc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 2f, 1.25f);
			world.playSound(finalLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 2f, 1.25f);
			world.playSound(finalLoc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 2f, 1.25f);
			world.playSound(finalLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2f, 1.25f);
			world.playSound(finalLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.75f);
			world.playSound(finalLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1f, 0.75f);
		}, 2);


		new PartialParticle(Particle.CRIT_MAGIC, eLoc, 50, 0, 0, 0, 1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.END_ROD, eLoc, 10, 0, 0, 0, 0.15).spawnAsPlayerActive(mPlayer);

		drawX(eLoc, mPlayer, LINE_LENGTH, TRANSC_LINE_COLOR);
	}

	@Override
	public void comboOnElite(World world, Location loc, Player mPlayer, double range, LivingEntity target) {
		loc = mPlayer.getLocation().add(0, 1, 0);
		Location eLoc = LocationUtils.getHalfHeightLocation(target);
		eLoc.setPitch(0);
		eLoc.setYaw(mPlayer.getLocation().getYaw());
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 2f, 1.3f);
		drawX(eLoc, mPlayer, LINE_LENGTH + 0.75, TRANSC_LINE_ELITE_COLOR);

		new PartialParticle(Particle.END_ROD, eLoc, 15, 0, 0, 0, 0.15).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, eLoc, 50, 0, 0, 0, 1).spawnAsPlayerActive(mPlayer);
		Location finalLoc = loc;
		new BukkitRunnable() {

			int mT = 0;
			float mTridentPitch = 0.0f;
			float mPufferPitch = 0.0f;
			@Override
			public void run() {
				mT++;

				world.playSound(finalLoc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 2f, 0.75f + mPufferPitch);
				world.playSound(finalLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2f, 1.25f);
				world.playSound(finalLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 2f, 0.75f);

				for (int i = 0; i < 6; i++) {
					createRandomLine(finalLoc, mPlayer);
				}
				if (mT < 5) {
					world.playSound(finalLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 2f, 0.85f + mTridentPitch);
				} else {
					this.cancel();

					Location newLoc = mPlayer.getLocation().add(0, 1, 0);
					newLoc.setPitch(0);

					world.playSound(newLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 2f, 0.75f);
					world.playSound(newLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1f, 0.75f);
					world.playSound(newLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2f, 1.25f);
					world.playSound(newLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1f, 1.65f);
					world.playSound(newLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 2f, 1f);
					world.playSound(newLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 2f, 0.8f);
					ParticleUtils.drawParticleLineSlash(eLoc, VectorUtils.rotateTargetDirection(mPlayer.getLocation().getDirection(), 90, 15),
						0, LINE_LENGTH, 0.05, 4,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
							float size = (float) (0.5f + (0.3f * middleProgress));
							new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
								new Particle.DustOptions(TRANSC_LINE_COLOR, size)).spawnAsPlayerActive(mPlayer);
						});

					ParticleUtils.drawCleaveArc(newLoc, 3.5, 160, -80, 260, 8, 0, 0, 0.2, 60,
						(Location l, int ring) -> {
							new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
								new Particle.DustOptions(
									ParticleUtils.getTransition(TRANSC_SLASH_COLOR_BASE, TRANSC_SLASH_COLOR_TIP, ring / 8D),
									0.6f + (ring * 0.1f)
								)).spawnAsPlayerActive(mPlayer);
						});

					ParticleUtils.drawCleaveArc(newLoc, 3.5, 20, -80, 260, 8, 0, 0, 0.2, 60,
						(Location l, int ring) -> {
							new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
								new Particle.DustOptions(
									ParticleUtils.getTransition(TRANSC_SLASH_COLOR_BASE, TRANSC_SLASH_COLOR_TIP, ring / 8D),
									0.6f + (ring * 0.1f)
								)).spawnAsPlayerActive(mPlayer);
						});

					RayTraceResult result = mPlayer.getWorld().rayTraceBlocks(mPlayer.getLocation(), new Vector(0, -1, 0), 5,
						FluidCollisionMode.SOURCE_ONLY, true);

					Location cLoc;
					if (result != null) {
						cLoc = result.getHitPosition().toLocation(mPlayer.getWorld()).add(0, 0.15, 0);
					} else {
						cLoc = mPlayer.getLocation().add(0, -5, 0);
					}

					cLoc.setPitch(0);
					ParticleUtils.drawParticleCircleExplosion(mPlayer, cLoc, 0, 1, 0, 0, 100, 0.7f,
						true, 0, 0, Particle.END_ROD);
					double rotation = 0;
					for (double speed = 0; speed < 0.7; speed += 0.02) {
						rotation += 3.5;
						ParticleUtils.drawParticleCircleExplosion(mPlayer, cLoc, 0, 1, 0, 0, 3, (float) speed,
							true, rotation, 0, Particle.END_ROD);
					}

					Location pLoc = mPlayer.getLocation().add(0, 3.5, 0);
					new PartialParticle(Particle.END_ROD, pLoc, 35, 0, 0, 0, 0.15).spawnAsPlayerActive(mPlayer);
					pLoc.setPitch(0);
					ParticleUtils.drawParticleLineSlash(pLoc, VectorUtils.rotateTargetDirection(pLoc.getDirection(), 90, 15),
						0, 5, 0.05, 5,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
							float size = (float) (0.75f + (0.4f * middleProgress));
							new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
								new Particle.DustOptions(Color.fromRGB(255, 255, 255), size)).spawnAsPlayerActive(mPlayer);
						});

					ParticleUtils.drawParticleLineSlash(pLoc, VectorUtils.rotateTargetDirection(pLoc.getDirection(), 90, 15),
						0, 5, 0.2, 5,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
							new PartialParticle(Particle.END_ROD, lineLoc, 2, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);
						});
				}

				mTridentPitch += 0.125f;
				mPufferPitch += 0.15f;
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 2);
	}

	private void drawX(Location loc, Player mPlayer, double length, Color color) {
		World world = loc.getWorld();
		loc.setPitch(0);
		loc.setYaw(mPlayer.getLocation().getYaw());
		Vector dir = VectorUtils.rotateTargetDirection(mPlayer.getLocation().getDirection(), 90, 90 - 35);
		ParticleUtils.drawParticleLineSlash(loc, dir, 0, length, 0.05, 4,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				float size = (float) (0.5f + (0.3f * middleProgress));
				new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
					new Particle.DustOptions(color, size)).spawnAsPlayerActive(mPlayer);
			});

		world.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 2f, 0.75f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 2f, 0.85f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2f, 1.25f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.75f);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Vector d = VectorUtils.rotateTargetDirection(mPlayer.getLocation().getDirection(), 90, 90 + 35);
			ParticleUtils.drawParticleLineSlash(loc, d, 0, length, 0.05, 4,
				(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
					float size = (float) (0.5f + (0.3f * middleProgress));
					new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
						new Particle.DustOptions(color, size)).spawnAsPlayerActive(mPlayer);
				});

			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, -35, 1, 0, 0, 75, 2.25f,
				true, 0, 0, Particle.CRIT_MAGIC);
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 35, 1, 0, 0, 75, 2.25f,
				true, 0, 0, Particle.CRIT_MAGIC);
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, -35, 1, 0, 0, 75, 2f,
				true, 0, 0, Particle.ELECTRIC_SPARK);
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 35, 1, 0, 0, 75, 2f,
				true, 0, 0, Particle.ELECTRIC_SPARK);
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, -35, 1, 0, 0, 75, 2.25f,
				true, 0, 0, Particle.ELECTRIC_SPARK);
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 35, 1, 0, 0, 75, 2.25f,
				true, 0, 0, Particle.ELECTRIC_SPARK);
		}, 2);
	}

	private void createRandomLine(Location loc, Player mPlayer) {
		loc = loc.clone().add(
			FastUtils.randomDoubleInRange(-4.5, 4.5),
			FastUtils.randomDoubleInRange(0, 4),
			FastUtils.randomDoubleInRange(-4.5, 4.5)
		);

		Vector dir = new Vector(
			FastUtils.randomDoubleInRange(-1, 1),
			FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(-1, 1)
		).normalize();

		loc.setDirection(dir);

		ParticleUtils.drawParticleLineSlash(loc, dir, 0, LINE_LENGTH, 0.05, 5,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				float size = (float) (0.3f + (0.35f * middleProgress));
				new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
					new Particle.DustOptions(TRANSC_LINE_COLOR, size)).spawnAsPlayerActive(mPlayer);
				if (middle) {

					ParticleUtils.drawParticleCircleExplosion(mPlayer, lineLoc.clone().setDirection(dir), 0, 1, 0, 90, 60, 1.75f,
						true, 0, 0, Particle.CRIT_MAGIC);
					new PartialParticle(Particle.END_ROD, lineLoc, 3, 0, 0, 0, 0.15f).spawnAsPlayerActive(mPlayer);
				}

			});
	}

}
