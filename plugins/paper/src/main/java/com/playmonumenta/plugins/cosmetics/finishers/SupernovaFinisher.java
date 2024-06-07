package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SupernovaFinisher implements EliteFinisher {

	public static final String NAME = "Supernova";
	private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(5, 0, 5), 3.0f);

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		Location eyeLocation = p.getEyeLocation();
		Vector direction = eyeLocation.getDirection().setY(0).normalize();
		Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		Location centered = loc.add(0, 8, 0).add(direction).add(direction);

		// lots of calculations for the red/white line
		Location lineTop = centered.clone().add(direction.getX() * 1.75, 3.25, direction.getZ() * 1.75)
			.add(right.getX() * 2, 0, right.getZ() * 2);
		Location lineBot = centered.clone().add(-direction.getX() * 1.75, -3.25, -direction.getZ() * 1.75)
			.add(-right.getX() * 2, 0, -right.getZ() * 2);
		double radius = 0.1;
		Vector vectorTop = lineTop.toVector().subtract(centered.toVector());
		Vector vectorBot = lineBot.toVector().subtract(centered.toVector());
		vectorTop.normalize();
		vectorBot.normalize();
		vectorTop.multiply(radius);
		vectorBot.multiply(radius);
		Location lineTopMid = centered.clone().add(vectorTop);
		Location lineBotMid = centered.clone().add(vectorBot);

		Location[][] curve = new Location[4][9];
		curve[0] = new Location[] {
			centered.clone().add(-0.7, 0, 1.067),
			centered.clone().add(-1, 0, 0.97),
			centered.clone().add(-1.3, 0, 0.8),
			centered.clone().add(-1.62, 0, 0.5),
			centered.clone().add(-1.92, 0, 0),
			centered.clone().add(-2.1, 0, -0.75),
			centered.clone().add(-1.95, 0, -1.7),
			centered.clone().add(-1.2, 0, -2.9),
			centered.clone().add(0, 0, -3.69)
		};

		curve[1] = new Location[] {
			centered.clone().add(0.7, 0, -1.067),
			centered.clone().add(1, 0, -0.97),
			centered.clone().add(1.3, 0, -0.8),
			centered.clone().add(1.62, 0, -0.5),
			centered.clone().add(1.92, 0, 0),
			centered.clone().add(2.1, 0, 0.75),
			centered.clone().add(1.95, 0, 1.7),
			centered.clone().add(1.2, 0, 2.9),
			centered.clone().add(0, 0, 3.69)
		};

		curve[2] = new Location[] {
			centered.clone().add(1.067, 0, 0.7),
			centered.clone().add(0.97, 0, 1),
			centered.clone().add(0.8, 0, 1.3),
			centered.clone().add(0.5, 0, 1.62),
			centered.clone().add(0, 0, 1.92),
			centered.clone().add(-0.75, 0, 2.1),
			centered.clone().add(-1.7, 0, 1.95),
			centered.clone().add(-2.9, 0, 1.2),
			centered.clone().add(-3.69, 0, 0)
		};

		curve[3] = new Location[] {
			centered.clone().add(-1.067, 0, -0.7),
			centered.clone().add(-0.97, 0, -1),
			centered.clone().add(-0.8, 0, -1.3),
			centered.clone().add(-0.5, 0, -1.62),
			centered.clone().add(0, 0, -1.92),
			centered.clone().add(0.75, 0, -2.1),
			centered.clone().add(1.7, 0, -1.95),
			centered.clone().add(2.9, 0, -1.2),
			centered.clone().add(3.69, 0, 0)
		};

		Location[] suck = new Location[30];
		Vector[] suckLength = new Vector[30];
		for (int i = 0; i < 30; i++) {
			suck[i] = centered.clone();
			suck[i].add(FastUtils.randomFloatInRange(-4, 4), FastUtils.randomFloatInRange(-4, 4), FastUtils.randomFloatInRange(-4, 4));
			suckLength[i] = new Vector(0, 0, 0);
			suckLength[i].setX((centered.getX() - suck[i].getX()) / 10);
			suckLength[i].setY((centered.getY() - suck[i].getY()) / 10);
			suckLength[i].setZ((centered.getZ() - suck[i].getZ()) / 10);
		}

		Location[] shootStart = new Location[2];
		Location[] shootEnd = new Location[2];
		Location[] current = new Location[2];
		Location[] realCurrent = new Location[2];
		Vector[] directionSTAR = new Vector[2];
		Vector[] increment = new Vector[2];
		int[] redLine = new int[2];
		int[] greenLine = new int[2];
		int[] blueLine = new int[2];
		shootStart[0] = lineTop;
		shootEnd[0] = lineTopMid;
		shootStart[1] = lineBot;
		shootEnd[1] = lineBotMid;

		for (int i = 0; i < 2; i++) {
			int units = 10;
			current[i] = shootStart[i].clone();
			directionSTAR[i] = shootEnd[i].clone().toVector().subtract(shootStart[i].toVector());
			increment[i] = directionSTAR[i].divide(new Vector(units, units, units));
			redLine[i] = 230;
			greenLine[i] = 48;
			blueLine[i] = 34;
		}
		realCurrent[0] = current[0].clone();
		realCurrent[1] = current[1].clone();

			new BukkitRunnable() {
			int mTicks = 0;
			final int mRStep = (255 - 230) / 10;
			final int mGStep = (255 - 48) / 10;
			final int mBStep = (255 - 34) / 10;
			float mLineSize = 0.5f;

			@Override
			public void run() {

				if (mTicks == 0) {
					loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.75f, 0.05f);
				}
				if (mTicks < 10) {
					mLineSize = 1.0f;
					if (mTicks % 5 == 0) {
						ParticleUtils.drawSphere(centered, 15, 1.2,
							(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1).directionalMode(true).data(BLACK)
								.extra(0.15).spawnAsPlayerActive(p)
						);
					}

					for (int i = 0; i < 7; i++) {
						Particle.DustOptions mSTAR = new Particle.DustOptions(Color.fromRGB(redLine[0], greenLine[0], blueLine[0]), mLineSize);
						new PartialParticle(Particle.REDSTONE, current[0], 1, 0, 0, 0, 0).data(mSTAR).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.REDSTONE, current[1], 1, 0, 0, 0, 0).data(mSTAR).minimumCount(1).spawnAsPlayerActive(p);

						redLine[0] += mRStep;
						greenLine[0] += mGStep;
						blueLine[0] += mBStep;
						current[0].add(increment[0]);
						current[1].add(increment[1]);
						mLineSize += 0.3f;

						if (i < 4 && mTicks < 9) {
							int redCurve = 255;
							int greenCurve = 255;
							int blueCurve = 255;

							switch (mTicks) {
								case 2 -> {
									redCurve = 253;
									greenCurve = 226;
									blueCurve = 95;
								}
								case 3 -> {
									redCurve = 208;
									greenCurve = 105;
									blueCurve = 57;
								}
								case 4 -> {
									redCurve = 164;
									greenCurve = 30;
									blueCurve = 13;
								}
								case 5 -> {
									redCurve = 81;
									greenCurve = 17;
									blueCurve = 33;
								}
								case 6 -> {
									redCurve = 58;
									greenCurve = 83;
									blueCurve = 113;
								}
								case 7 -> {
									redCurve = 57;
									greenCurve = 70;
									blueCurve = 134;
								}
								case 8 -> {
									redCurve = 70;
									greenCurve = 51;
									blueCurve = 84;
								}
								default -> {
								}
							}
							Particle.DustOptions mCURVECOL = new Particle.DustOptions(Color.fromRGB(redCurve, greenCurve, blueCurve), 4);
							new PartialParticle(Particle.REDSTONE, curve[i][mTicks], 1, 0, 0, 0, 0).data(mCURVECOL).minimumCount(1).spawnAsPlayerActive(p);
						}
					}

					redLine[0] = 230;
					greenLine[0] = 48;
					blueLine[0] = 34;
					current[0] = realCurrent[0].clone();
					current[1] = realCurrent[1].clone();

				} else if (mTicks == 35) {
					p.stopSound(Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS);
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 2.0f);
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 2.0f);
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 2.0f);
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 2.0f);
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 2.0f);
					loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1.0f, 0.25f);
					loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 2.0f, 0.15f);
					loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 3f, 0.75f);

					new PartialParticle(Particle.FLASH, centered, 1).minimumCount(1).spawnAsPlayerActive(p);
					new PartialParticle(Particle.EXPLOSION_LARGE, centered, 1).minimumCount(1).spawnAsPlayerActive(p);
					ParticleUtils.drawSphere(centered, 30, 1.2,
						(l, t) -> {
							Vector vel = centered.clone().subtract(l).toVector().normalize();
							new PartialParticle(Particle.FLAME, l, 1).directionalMode(true)
								.delta(vel.getX(), vel.getY(), vel.getZ()).extra(0.5).spawnAsPlayerActive(p);
						}
					);
					ParticleUtils.drawSphere(centered, 7, 0.1,
						(l, t) -> {
							Vector vel = centered.clone().subtract(l).toVector().normalize().multiply(1.7);
							new PartialParticle(Particle.END_ROD, l, 1).directionalMode(true)
								.delta(vel.getX(), vel.getY(), vel.getZ()).extra(0.15).spawnAsPlayerActive(p);
						}
					);
				}
				if (mTicks < 10) {
					for (int i = 0; i < 10; i++) {
						new PartialParticle(Particle.PORTAL, suck[i], 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
						suck[i].add(suckLength[i]);
					}
				} else if (mTicks < 20) {
					for (int i = 10; i < 20; i++) {
						new PartialParticle(Particle.PORTAL, suck[i], 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
						suck[i].add(suckLength[i]);
					}
				} else if (mTicks < 30) {
					for (int i = 20; i < 30; i++) {
						new PartialParticle(Particle.PORTAL, suck[i], 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
						suck[i].add(suckLength[i]);
					}
				}
				if (mTicks >= 37) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}
}
