package com.playmonumenta.plugins.cosmetics.punches;

import com.playmonumenta.networkchat.RemotePlayerListener;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class TranscendentPunch implements PlayerPunch {
	// This punch uses code from the Transcendent Combos rogue cosmetic

	public static final String NAME = "Transcendent";
	private static final Color TRANSC_LINE_COLOR = Color.fromRGB(217, 242, 255);
	private static final Color TRANSC_LINE_ELITE_COLOR = Color.fromRGB(255, 196, 196);
	private static final Color TRANSC_SLASH_COLOR_TIP = Color.fromRGB(153, 220, 255);
	private static final Color TRANSC_SLASH_COLOR_BASE = Color.fromRGB(199, 236, 255);
	private static final double LINE_LENGTH = 2.25;

	@Override
	public void run(Player bully, Player victim) {
		World world = victim.getWorld();
		Location loc = bully.getLocation().add(0, 1, 0);
		Location eLoc = LocationUtils.getHalfHeightLocation(victim);
		eLoc.setPitch(0);
		eLoc.setYaw(bully.getLocation().getYaw());
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 1.7f, 1.3f);
		drawX(eLoc, bully, LINE_LENGTH + 0.75, TRANSC_LINE_ELITE_COLOR);

		new PartialParticle(Particle.END_ROD, eLoc, 15, 0, 0, 0, 0.15).spawnAsPlayerActive(bully);
		new PartialParticle(Particle.CRIT_MAGIC, eLoc, 50, 0, 0, 0, 1).spawnAsPlayerActive(bully);
		new BukkitRunnable() {

			int mT = 0;
			float mTridentPitch = 0.0f;
			float mPufferPitch = 0.0f;

			@Override
			public void run() {
				mT++;

				world.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 1.7f, 0.75f + mPufferPitch);
				world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.7f, 1.25f);
				world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.7f, 0.75f);

				for (int i = 0; i < 6; i++) {
					createRandomLine(loc, bully);
				}
				if (mT < 5) {
					world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.7f, 0.85f + mTridentPitch);
				} else {
					this.cancel();

					Location newLoc = bully.getLocation().add(0, 1, 0);
					newLoc.setPitch(0);

					world.playSound(newLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.7f, 0.75f);
					world.playSound(newLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1f, 0.75f);
					world.playSound(newLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.7f, 1.25f);
					world.playSound(newLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1f, 1.65f);
					world.playSound(newLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1.7f, 1f);
					world.playSound(newLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.7f, 0.8f);
					ParticleUtils.drawParticleLineSlash(eLoc, VectorUtils.rotateTargetDirection(bully.getLocation().getDirection(), 90, 15),
						0, LINE_LENGTH, 0.05, 4,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
							float size = (float) (0.5f + (0.3f * middleProgress));
							new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
								new Particle.DustOptions(TRANSC_LINE_COLOR, size)).spawnAsPlayerActive(bully);
						});

					ParticleUtils.drawCleaveArc(newLoc, 3.5, 160, -80, 260, 8, 0, 0, 0.2, 60,
						(Location l, int ring, double angleProgress) -> {
							new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
								new Particle.DustOptions(
									ParticleUtils.getTransition(TRANSC_SLASH_COLOR_BASE, TRANSC_SLASH_COLOR_TIP, ring / 8D),
									0.6f + (ring * 0.1f)
								)).spawnAsPlayerActive(bully);
						});

					ParticleUtils.drawCleaveArc(newLoc, 3.5, 20, -80, 260, 8, 0, 0, 0.2, 60,
						(Location l, int ring, double angleProgress) -> {
							new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
								new Particle.DustOptions(
									ParticleUtils.getTransition(TRANSC_SLASH_COLOR_BASE, TRANSC_SLASH_COLOR_TIP, ring / 8D),
									0.6f + (ring * 0.1f)
								)).spawnAsPlayerActive(bully);
						});

					RayTraceResult result = bully.getWorld().rayTraceBlocks(bully.getLocation(), new Vector(0, -1, 0), 5,
						FluidCollisionMode.SOURCE_ONLY, true);

					Location cLoc;
					if (result != null) {
						cLoc = result.getHitPosition().toLocation(bully.getWorld()).add(0, 0.15, 0);
					} else {
						cLoc = bully.getLocation().add(0, -5, 0);
					}

					cLoc.setPitch(0);
					ParticleUtils.drawParticleCircleExplosion(bully, cLoc, 0, 1, 0, 0, 100, 0.7f,
						true, 0, 0, Particle.END_ROD);
					double rotation = 0;
					for (double speed = 0; speed < 0.7; speed += 0.02) {
						rotation += 3.5;
						ParticleUtils.drawParticleCircleExplosion(bully, cLoc, 0, 1, 0, 0, 3, (float) speed,
							true, rotation, 0, Particle.END_ROD);
					}

					Location pLoc = bully.getLocation().add(0, 3.5, 0);
					new PartialParticle(Particle.END_ROD, pLoc, 35, 0, 0, 0, 0.15).spawnAsPlayerActive(bully);
					pLoc.setPitch(0);
					ParticleUtils.drawParticleLineSlash(pLoc, VectorUtils.rotateTargetDirection(pLoc.getDirection(), 90, 15),
						0, 5, 0.05, 5,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
							float size = (float) (0.75f + (0.4f * middleProgress));
							new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
								new Particle.DustOptions(Color.fromRGB(255, 255, 255), size)).spawnAsPlayerActive(bully);
						});

					ParticleUtils.drawParticleLineSlash(pLoc, VectorUtils.rotateTargetDirection(pLoc.getDirection(), 90, 15),
						0, 5, 0.2, 5,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
							new PartialParticle(Particle.END_ROD, lineLoc, 2, 0, 0, 0, 0).spawnAsPlayerActive(bully);
						});
				}

				mTridentPitch += 0.125f;
				mPufferPitch += 0.15f;
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 2);

		victim.setVelocity(new Vector(0, 2.5, 0));
	}

	private void drawX(Location loc, Player player, double length, Color color) {
		World world = loc.getWorld();
		loc.setPitch(0);
		loc.setYaw(player.getLocation().getYaw());
		Vector dir = VectorUtils.rotateTargetDirection(player.getLocation().getDirection(), 90, 90 - 35);
		ParticleUtils.drawParticleLineSlash(loc, dir, 0, length, 0.05, 4,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				float size = (float) (0.5f + (0.3f * middleProgress));
				new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
					new Particle.DustOptions(color, size)).spawnAsPlayerActive(player);
			});

		world.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 1.7f, 0.75f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.7f, 0.85f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.7f, 1.25f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.75f);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Vector d = VectorUtils.rotateTargetDirection(player.getLocation().getDirection(), 90, 90 + 35);
			ParticleUtils.drawParticleLineSlash(loc, d, 0, length, 0.05, 4,
				(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
					float size = (float) (0.5f + (0.3f * middleProgress));
					new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
						new Particle.DustOptions(color, size)).spawnAsPlayerActive(player);
				});

			ParticleUtils.drawParticleCircleExplosion(player, loc, -35, 1, 0, 0, 75, 2.25f,
				true, 0, 0, Particle.CRIT_MAGIC);
			ParticleUtils.drawParticleCircleExplosion(player, loc, 35, 1, 0, 0, 75, 2.25f,
				true, 0, 0, Particle.CRIT_MAGIC);
			ParticleUtils.drawParticleCircleExplosion(player, loc, -35, 1, 0, 0, 75, 2f,
				true, 0, 0, Particle.ELECTRIC_SPARK);
			ParticleUtils.drawParticleCircleExplosion(player, loc, 35, 1, 0, 0, 75, 2f,
				true, 0, 0, Particle.ELECTRIC_SPARK);
			ParticleUtils.drawParticleCircleExplosion(player, loc, -35, 1, 0, 0, 75, 2.25f,
				true, 0, 0, Particle.ELECTRIC_SPARK);
			ParticleUtils.drawParticleCircleExplosion(player, loc, 35, 1, 0, 0, 75, 2.25f,
				true, 0, 0, Particle.ELECTRIC_SPARK);
		}, 2);
	}

	private void createRandomLine(Location loc, Player player) {
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
					new Particle.DustOptions(TRANSC_LINE_COLOR, size)).spawnAsPlayerActive(player);
				if (middle) {

					ParticleUtils.drawParticleCircleExplosion(player, lineLoc.clone().setDirection(dir), 0, 1, 0, 90, 60, 1.75f,
						true, 0, 0, Particle.CRIT_MAGIC);
					new PartialParticle(Particle.END_ROD, lineLoc, 3, 0, 0, 0, 0.15f).spawnAsPlayerActive(player);
				}

			});
	}

	@Override
	public void broadcastPunchMessage(Player bully, Player victim, List<Player> playersInWorld, boolean isRemotePunch) {
		for (Player player : playersInWorld) {
			player.sendMessage(
				RemotePlayerListener.getPlayerComponent(bully.getUniqueId())
					.append(Component.text((isRemotePunch ? " remotely punched " : " punched "), NamedTextColor.GRAY)).hoverEvent(null).clickEvent(null)
					.append(RemotePlayerListener.getPlayerComponent(victim.getUniqueId()))
					.append(Component.text(" into another dimension!", NamedTextColor.GRAY)).hoverEvent(null).clickEvent(null)
			);
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_STAR;
	}
}
