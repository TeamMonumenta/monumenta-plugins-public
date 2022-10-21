package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ClockworkAssassination extends Spell {
	private static final String ABILITY_NAME = "Clockwork Assassination";
	private static final int COOLDOWN = 20 * 4;
	private static final int CAST_TIME = 20 * 2;
	private static final int EXECUTION_TIME = 20 * 5;
	private static final int RANGE = 50;
	private static final int GROWTH_TICKS = 30;
	private static final double MAX_RADIUS = 6;
	private static final double DAMAGE = 90;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final ChargeUpManager mChargeUp;

	public ClockworkAssassination(Plugin mPlugin, LivingEntity mBoss) {
		this.mPlugin = mPlugin;
		this.mBoss = mBoss;
		this.mChargeUp = new ChargeUpManager(mBoss, CAST_TIME, ChatColor.GOLD + "Channeling " + ChatColor.YELLOW + ABILITY_NAME,
			BarColor.YELLOW, BarStyle.SOLID, RANGE);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mChargeUp.setTime(0);
		mChargeUp.setColor(BarColor.YELLOW);
		mChargeUp.setTitle(ChatColor.GOLD + "Channeling " + ChatColor.YELLOW + ABILITY_NAME);
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE, true);
		Collections.shuffle(players);

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					mChargeUp.setTitle(ChatColor.GOLD + "Executing " + ChatColor.RED + ABILITY_NAME);
					mChargeUp.setColor(BarColor.RED);
					players.forEach(p -> world.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 2.0f));

					BukkitRunnable runnable = new BukkitRunnable() {
						int mT = 0;
						int mIndex = 0;
						@Override
						public void run() {
							double progress = 1 - ((double) mT / (double) EXECUTION_TIME);
							mChargeUp.setProgress(progress);
							if (progress == 0.8) {
								executeAssassination(players.get(mIndex));
								if (mIndex < players.size() - 1) {
									mIndex += 1;
								}
							}
							if (progress == 0.6) {
								executeAssassination(players.get(mIndex));
								if (mIndex < players.size() - 1) {
									mIndex += 1;
								}
							}
							if (progress == 0.4) {
								executeAssassination(players.get(mIndex));
								if (mIndex < players.size() - 1) {
									mIndex += 1;
								}
							}
							if (progress == 0.24) {
								executeAssassination(players.get(mIndex));
								if (mIndex < players.size() - 1) {
									mIndex += 1;
								}
							}
							if (mT > EXECUTION_TIME) {
								this.cancel();
								mActiveRunnables.remove(this);
							}
							mT += 1;
						}
					};
					runnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runnable);
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	public void executeAssassination(Player player) {
		Location location = player.getLocation();
		World world = location.getWorld();
		location.setY(location.getBlockY());
		world.playSound(location, Sound.BLOCK_CAMPFIRE_CRACKLE, 2.0f, 0.3f);
		world.playSound(location, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.7f);
		PPCircle indicator = new PPCircle(Particle.REDSTONE, location, 0).ringMode(true).count(25).delta(0.1, 0.05, 0.1).data(new Particle.DustOptions(Color.fromRGB(214, 58, 166), 1.65f));
		PPCircle indicator2 = new PPCircle(Particle.DRAGON_BREATH, location, 0).ringMode(true).count(15).delta(0.25, 0.1, 0.25).extra(0.03);

		Location tpLoc = player.getLocation();
		tpLoc.setY(tpLoc.getY() + 0.1f);
		Vector shift = tpLoc.getDirection();
		shift.setY(0).normalize().multiply(-3);
		tpLoc.add(shift);
		mBoss.teleport(tpLoc);
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				double radius;

				if (mTicks < GROWTH_TICKS) {
					radius = mTicks * MAX_RADIUS / GROWTH_TICKS;
				} else {
					for (Player p : PlayerUtils.playersInRange(location, MAX_RADIUS, true)) {
						DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false, "Clockwork Assassination");
					}
					world.playSound(location, Sound.BLOCK_SHROOMLIGHT_BREAK, 2.0f, 0.7f);
					radius = MAX_RADIUS;
					this.cancel();
				}

				indicator.radius(radius).spawnAsBoss();
				indicator2.radius(radius).spawnAsBoss();

				mTicks += 5;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 5);
	}

	@Override
	public int cooldownTicks() {
		return EXECUTION_TIME + CAST_TIME + COOLDOWN;
	}
}
