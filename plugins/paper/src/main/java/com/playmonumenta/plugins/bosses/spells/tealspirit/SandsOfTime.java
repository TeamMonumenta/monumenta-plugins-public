package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.TealSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public class SandsOfTime extends Spell {
	private static final double RADIUS = 21;
	private static final double HEIGHT = 4;
	private static final double RED_DAMAGE = 180;
	private static final double RED_DIST = 25;
	private static final int RED_SPREAD = 4;
	private static final double RED_RADIUS = 2.5;
	private static final double BLUE_DAMAGE = 180;
	private static final int BLUE_ROOT = 4 * 20;
	private static final String ROOT_EFFECT = "SandsOfTimePercentSpeedEffect";
	private static final int BLUE_DELAY = 2 * 20;
	private static final double BLUE_MIN_DIST = 5;
	private static final double BLUE_MAX_DIST = 20;
	private static final double BLUE_ANGLE = 30;
	private static final double BLUE_RADIUS = 7;
	private static final String RED_TEAM = "SandsOfTimeRed";
	private static final String BLUE_TEAM = "SandsOfTimeBlue";

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final int mCooldownTicks;
	private final TealSpirit mTealSpirit;
	private final ChargeUpManager mChargeUp;

	private final Team mNormalTeam;
	private final Team mRedTeam;
	private final Team mBlueTeam;

	public SandsOfTime(LivingEntity boss, Location center, Team team, int cooldownTicks, TealSpirit tealSpirit) {
		mBoss = boss;
		mCenter = center;
		mCooldownTicks = cooldownTicks;
		mTealSpirit = tealSpirit;

		mNormalTeam = team;
		mRedTeam = ScoreboardUtils.getExistingTeamOrCreate(RED_TEAM, NamedTextColor.DARK_RED);
		mBlueTeam = ScoreboardUtils.getExistingTeamOrCreate(BLUE_TEAM, NamedTextColor.BLUE);

		mChargeUp = new ChargeUpManager(mCenter, mBoss, 4 * 20, ChatColor.BLUE + "Channeling Sands of Time...", BarColor.BLUE, BarStyle.SOLID, TealSpirit.detectionRange);
	}

	@Override
	public void run() {
		mTealSpirit.setInterspellCooldown(5 * 20);

		World world = mCenter.getWorld();
		Plugin plugin = Plugin.getInstance();
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		mBoss.setGravity(false);

		PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "now the very sands of time will be unleashed!"));

		List<Location> locs = new ArrayList<>();
		locs.add(mCenter.clone().add(RADIUS, HEIGHT, 0));
		locs.add(mCenter.clone().add(-RADIUS, HEIGHT, 0));
		locs.add(mCenter.clone().add(0, HEIGHT, RADIUS));
		locs.add(mCenter.clone().add(0, HEIGHT, -RADIUS));
		Collections.shuffle(locs);

		// Red is true
		// Blue is false
		List<Boolean> bools = new ArrayList<>();
		bools.add(true);
		bools.add(true);
		bools.add(false);
		bools.add(false);
		Collections.shuffle(bools);

		new BukkitRunnable() {
			@Override
			public void run() {
				int time = mChargeUp.getTime();
				if (time % 20 == 0 && time < 80) {
					int i = time / 20;
					Location loc = locs.get(i);
					mBoss.teleport(loc);
					float pitch;
					if (bools.get(i)) {
						mChargeUp.setColor(BarColor.RED);
						mChargeUp.setTitle(ChatColor.RED + "Channeling Sands of Time...");
						mRedTeam.addEntity(mBoss);
						pitch = 0.5f;
					} else {
						mChargeUp.setColor(BarColor.BLUE);
						mChargeUp.setTitle(ChatColor.BLUE + "Channeling Sands of Time...");
						mBlueTeam.addEntity(mBoss);
						pitch = 0.354f;
					}

					for (Player player : PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true)) {
						Location playerLoc = player.getLocation();
						Location soundLoc = playerLoc.clone().add(LocationUtils.getDirectionTo(loc, playerLoc).multiply(3));
						player.playSound(soundLoc, Sound.BLOCK_BELL_USE, 1, pitch);
						player.playSound(soundLoc, Sound.BLOCK_BELL_USE, 0.75f, pitch * 0.334f);
					}
				}

				if (mChargeUp.nextTick()) {
					Location tallCenter = mCenter.clone().add(0, HEIGHT, 0);
					mBoss.teleport(tallCenter);
					mNormalTeam.addEntity(mBoss);

					for (int i = 0; i < locs.size(); i++) {
						Location loc = locs.get(i);
						if (bools.get(i)) {
							activateRed(plugin, loc);
						} else {
							Bukkit.getScheduler().runTaskLater(plugin, () -> {
								activateBlue(plugin, loc);
							}, BLUE_DELAY);
						}
					}

					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						List<Location> soundLocs = getRandomLocationsNear(tallCenter, 3);
						for (Location loc : soundLocs) {
							world.playSound(loc, Sound.ITEM_CROSSBOW_LOADING_START, 2.0f, 0.75f);
						}
					}, 2);

					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						List<Location> soundLocs = getRandomLocationsNear(tallCenter, 5);
						for (Location loc : soundLocs) {
							world.playSound(loc, Sound.ITEM_TRIDENT_THROW, 1.5f, 1.25f);
						}
					}, 5);

					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						for (int i = 0; i < locs.size(); i++) {
							Location loc = locs.get(i);
							if (bools.get(i)) {
								List<Location> soundLocs = getRandomLocationsNear(loc, 3);
								for (Location soundLoc : soundLocs) {
									world.playSound(soundLoc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.2f, 1.25f);
								}
								soundLocs = getRandomLocationsNear(loc, 3);
								for (Location soundLoc : soundLocs) {
									world.playSound(soundLoc, Sound.ENTITY_ARROW_HIT, 1.2f, 0.8f);
								}
							}
						}
					}, 10);

					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						mBoss.setInvulnerable(false);
						mBoss.setAI(true);
						mBoss.setGravity(true);
					}, BLUE_DELAY);

					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						for (int i = 0; i < locs.size(); i++) {
							Location loc = locs.get(i);
							if (!bools.get(i)) {
								world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
								world.playSound(loc, Sound.BLOCK_SHROOMLIGHT_BREAK, 1.0f, 0.5f);
							}
						}
					}, BLUE_DELAY + 10);

					mChargeUp.reset();
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private void activateRed(Plugin plugin, Location loc) {
		World world = loc.getWorld();
		Vector dir = LocationUtils.getDirectionTo(loc, mCenter);
		Vector horizontal = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
		for (int i = 0; i < RED_DIST; i += RED_SPREAD) {
			for (int j = -i; j <= i; j += RED_SPREAD) {
				Vector offset = dir.clone().multiply(i).add(horizontal.clone().multiply(j));
				Location target = mCenter.clone().add(offset);
				if (target.distance(mCenter) > RED_DIST) {
					continue;
				}
				target.add(FastUtils.randomDoubleInRange(-0.75, 0.75), 0, FastUtils.randomDoubleInRange(-0.75, 0.75));
				target = LocationUtils.fallToGround(target, mCenter.getY());
				Location current = mCenter.clone().add(0, 6, 0).add(offset.clone().multiply(1.0/3));
				Vector path = LocationUtils.getDirectionTo(target, current);
				double pullback = -0.5;
				double shoot = (current.distance(target) - pullback * 3) / 5;

				BukkitRunnable runnable = new BukkitRunnable() {
					int mT = 1;
					@Override
					public void run() {
						Vector move;
						if (mT <= 2) {
							move = new Vector();
						} else if (mT <= 5) {
							move = path.clone().multiply(pullback);
						} else {
							move = path.clone().multiply(shoot);
						}

						double length = move.length();
						for (double r = 0; r <= length; r += 0.3 + 0.2 * length) {
							world.spawnParticle(Particle.REDSTONE, current.clone().add(move.clone().normalize().multiply(r)), 1, new Particle.DustOptions(Color.fromRGB(255, 0, 0), 2));
						}
						current.add(move);

						if (mT >= 10) {
							for (Player player : PlayerUtils.playersInRange(current, RED_RADIUS, true)) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, RED_DAMAGE, null, false, false, "Sands of Time");
							}
							this.cancel();
							return;
						}

						mT++;
					}
				};
				mActiveRunnables.add(runnable);
				runnable.runTaskTimer(plugin, 0, 1);
			}
		}
	}

	private void activateBlue(Plugin plugin, Location loc) {
		World world = loc.getWorld();
		Vector dir = LocationUtils.getDirectionTo(loc, mCenter);
		double dist = FastUtils.randomDoubleInRange(BLUE_MIN_DIST, BLUE_MAX_DIST);
		double deg = FastUtils.randomDoubleInRange(-BLUE_ANGLE, BLUE_ANGLE);
		Vector dirR = VectorUtils.rotateYAxis(dir, deg);
		Location target = mCenter.clone().add(dirR.clone().multiply(dist));
		Location current = mCenter.clone().add(0, 6, 0);
		Vector path = LocationUtils.getDirectionTo(target, current).multiply(current.distance(target) / 5);

		List<Vector> points = new ArrayList<>();
		for (int phi = 0; phi < 360; phi += 15) {
			double y = (BLUE_RADIUS - 1) * FastUtils.cosDeg(phi);
			double sin = (BLUE_RADIUS - 1) * FastUtils.sinDeg(phi);
			for (int theta = 0; theta < 360; theta += 15) {
				double x = sin * FastUtils.cosDeg(theta);
				double z = sin * FastUtils.sinDeg(theta);
				points.add(new Vector(x, y, z));
			}
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				current.add(path);

				for (Vector point : points) {
					world.spawnParticle(Particle.REDSTONE, current.clone().add(point), 1, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(Color.fromRGB(20, 70, 150), 2));
				}
				double halfRadius = BLUE_RADIUS / 2;
				world.spawnParticle(Particle.SOUL_FIRE_FLAME, current, 20, halfRadius, halfRadius, halfRadius, 0);
				world.spawnParticle(Particle.END_ROD, current, 30, halfRadius, halfRadius, halfRadius, 0);

				if (mT >= 10) {
					for (Player player : PlayerUtils.playersInRange(current, BLUE_RADIUS, true)) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, BLUE_DAMAGE, null, false, false, "Sands of Time");
						plugin.mEffectManager.addEffect(player, ROOT_EFFECT, new PercentSpeed(BLUE_ROOT, -1, ROOT_EFFECT));
					}
					this.cancel();
					return;
				}

				mT += 2;
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(plugin, 0, 2);
	}

	private List<Location> getRandomLocationsNear(Location center, int num) {
		List<Location> locs = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			locs.add(center.clone().add(FastUtils.randomDoubleInRange(-3, 3), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-3, 3)));
		}
		return locs;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	@Override
	public boolean canRun() {
		return !mTealSpirit.isInterspellCooldown();
	}
}
