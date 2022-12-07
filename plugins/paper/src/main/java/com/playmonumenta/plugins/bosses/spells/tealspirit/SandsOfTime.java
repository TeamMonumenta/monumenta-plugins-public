package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.TealSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
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
	private static final int BLUE_ROOT = 1 * 20;
	private static final double DIST = 25;
	private static final int SPREAD = 4;
	private static final int BLUE_DELAY = 4 * 20;
	private static final String ROOT_EFFECT = "SandsOfTimePercentSpeedEffect";
	private static final String RED_TEAM = "SandsOfTimeRed";
	private static final String BLUE_TEAM = "SandsOfTimeBlue";

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final int mCooldownTicks;
	private final double mDamage;
	private final int mBellTime;
	private final ChargeUpManager mChargeUp;

	private final Team mNormalTeam;
	private final Team mRedTeam;
	private final Team mBlueTeam;

	public SandsOfTime(LivingEntity boss, Location center, Team team, int cooldownTicks, int damage, int bellTime) {
		mBoss = boss;
		mCenter = center;
		mCooldownTicks = cooldownTicks;
		mDamage = damage;
		mNormalTeam = team;
		mRedTeam = ScoreboardUtils.getExistingTeamOrCreate(RED_TEAM, NamedTextColor.DARK_RED);
		mBlueTeam = ScoreboardUtils.getExistingTeamOrCreate(BLUE_TEAM, NamedTextColor.BLUE);
		mBellTime = bellTime;
		mChargeUp = new ChargeUpManager(mCenter, mBoss, 4 * mBellTime, ChatColor.BLUE + "Channeling Sands of Time...", BarColor.BLUE, BarStyle.SOLID, TealSpirit.detectionRange);
	}

	@Override
	public void run() {
		Plugin plugin = Plugin.getInstance();
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

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				int time = mChargeUp.getTime();
				if (time % mBellTime == 0 && time < 4 * mBellTime) {
					int i = time / mBellTime;
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
						Location soundLoc = playerLoc.clone().add(LocationUtils.getDirectionTo(loc, playerLoc).normalize().multiply(3));
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
							activate(plugin, loc, Color.RED, false, tallCenter);
						} else {
							Bukkit.getScheduler().runTaskLater(plugin, () -> {
								activate(plugin, loc, Color.BLUE, true, tallCenter);
							}, BLUE_DELAY);
						}
					}

					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						mBoss.setAI(true);
						mBoss.setGravity(true);
					}, BLUE_DELAY);

					mChargeUp.reset();
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mBoss.setAI(true);
				mBoss.setGravity(true);
			}
		};
		runnable.runTaskTimer(plugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void activate(Plugin plugin, Location loc, Color color, boolean doRoot, Location tallCenter) {
		World world = loc.getWorld();
		Vector dir = LocationUtils.getDirectionTo(loc, mCenter);
		Vector horizontal = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
		for (int i = 0; i < DIST; i += SPREAD) {
			for (int j = -i; j <= i; j += SPREAD) {
				Vector offset = dir.clone().multiply(i).add(horizontal.clone().multiply(j));
				Location target = mCenter.clone().add(offset);
				if (target.distance(mCenter) > DIST) {
					continue;
				}
				target.add(FastUtils.randomDoubleInRange(-0.75, 0.75), 0, FastUtils.randomDoubleInRange(-0.75, 0.75));
				target = LocationUtils.fallToGround(target, mCenter.getY());
				Location current = mCenter.clone().add(0, 6, 0).add(offset.clone().multiply(1.0 / 3));
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
							new PartialParticle(Particle.REDSTONE, current.clone().add(move.clone().normalize().multiply(r)), 1, new Particle.DustOptions(color, 2.5f))
								.minimumMultiplier(false)
								.spawnAsEntityActive(mBoss);
						}
						current.add(move);

						if (mT >= 10) {
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

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			List<Location> soundLocs = getRandomLocationsNear(tallCenter, 2);
			for (Location soundLoc : soundLocs) {
				world.playSound(soundLoc, Sound.ITEM_CROSSBOW_LOADING_START, 2.0f, 0.75f);
			}
		}, 2);

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			List<Location> soundLocs = getRandomLocationsNear(tallCenter, 3);
			for (Location soundLoc : soundLocs) {
				world.playSound(soundLoc, Sound.ITEM_TRIDENT_THROW, 1.5f, 1.25f);
			}
		}, 5);

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (Player player : PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true)) {
				Location playerLoc = player.getLocation();
				// Within 2 blocks or 45 degrees in either direction
				// 0.7071 = sqrt(2) / 2
				if (playerLoc.distanceSquared(mCenter) < 2 * 2 || dir.clone().setY(0).normalize().dot(LocationUtils.getDirectionTo(playerLoc, mCenter).setY(0).normalize()) >= 0.7071) {
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, mDamage, null, false, false, "Sands of Time");
					if (doRoot) {
						plugin.mEffectManager.addEffect(player, ROOT_EFFECT, new PercentSpeed(BLUE_ROOT, -1, ROOT_EFFECT));
					}
				}
			}

			List<Location> soundLocs = getRandomLocationsNear(loc, 3);
			for (Location soundLoc : soundLocs) {
				world.playSound(soundLoc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.2f, 1.25f);
			}
			soundLocs = getRandomLocationsNear(loc, 3);
			for (Location soundLoc : soundLocs) {
				world.playSound(soundLoc, Sound.ENTITY_ARROW_HIT, 1.2f, 0.8f);
			}
		}, 10);
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
}
