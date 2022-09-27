package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.TealSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashMap;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
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
import org.bukkit.util.Vector;

public class Rewind extends Spell {
	private static final double RADIUS = 5;
	private static final int CHARGE_TIME = 8 * 20;
	private static final int REWIND_TIME = 8 * 20;

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final int mCooldownTicks;
	private final TealSpirit mTealSpirit;
	private final ChargeUpManager mWindUp;
	private final ChargeUpManager mWindDown;

	private int mTimer = 5;

	public Rewind(LivingEntity boss, Location center, int cooldownTicks, TealSpirit tealSpirit) {
		mBoss = boss;
		mCenter = center;
		mCooldownTicks = cooldownTicks;
		mTealSpirit = tealSpirit;

		mWindUp = new ChargeUpManager(mBoss, CHARGE_TIME, ChatColor.AQUA + "Winding Up...", BarColor.RED, BarStyle.SOLID, TealSpirit.detectionRange);
		mWindDown = new ChargeUpManager(mBoss, REWIND_TIME, ChatColor.AQUA + "Turning Back Time...", BarColor.RED, BarStyle.SOLID, TealSpirit.detectionRange);
	}

	@Override
	public void run() {
		mTimer += 5;
		if (mTimer >= mCooldownTicks && !mTealSpirit.isInterspellCooldown()) {
			mTimer = 0;
			mTealSpirit.setInterspellCooldown(CHARGE_TIME + REWIND_TIME + 2 * 20);

			World world = mCenter.getWorld();
			PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true).forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.7f));

			PPCircle back = new PPCircle(Particle.TOTEM, mCenter, 1.5).ringMode(true).count(12);
			PPCircle surround = new PPCircle(Particle.REDSTONE, mCenter, RADIUS).data(new Particle.DustOptions(Color.WHITE, 1)).ringMode(true);
			PPCircle inside = new PPCircle(Particle.SPELL_WITCH, mCenter, RADIUS).delta(0, 0.2, 0).extra(0.1);

			Plugin plugin = Plugin.getInstance();
			BukkitRunnable outer = new BukkitRunnable() {
				@Override
				public void run() {
					if (mWindUp.nextTick()) {
						mWindUp.reset();

						List<Player> players = PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true);
						HashMap<Player, Location> origins = new HashMap<>();
						players.forEach(player -> origins.put(player, player.getLocation()));

						players.forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.5f));

						mWindDown.setTime(REWIND_TIME);
						BukkitRunnable inner = new BukkitRunnable() {
							@Override
							public void run() {
								if (mWindDown.previousTick()) {
									mWindDown.reset();

									HashMap<Player, HashMap<LivingEntity, Vector>> relatives = new HashMap<>();
									players.forEach(player -> relatives.put(player, new HashMap<>()));
									for (LivingEntity mob : EntityUtils.getNearbyMobs(mCenter, TealSpirit.detectionRange, mBoss)) {
										Location mobLoc = mob.getLocation();
										Player player = EntityUtils.getNearestPlayer(mobLoc, RADIUS);
										if (player != null) {
											HashMap<LivingEntity, Vector> relative = relatives.get(player);
											if (relative != null) {
												relative.put(mob, mobLoc.toVector().subtract(player.getLocation().toVector()));
											}
										}
									}

									for (Player player : players) {
										Location origin = origins.get(player);
										player.teleport(origin);
										HashMap<LivingEntity, Vector> relative = relatives.get(player);
										for (LivingEntity mob : relative.keySet()) {
											Location destination = origin.clone().add(relative.get(mob));
											mob.teleport(destination);
											world.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, destination.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
										}

										player.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
										world.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, origin.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
									}

									this.cancel();
								}

								int time = mWindDown.getTime();
								if (time % 5 == 0) {
									for (Player player : origins.keySet()) {
										back.location(origins.get(player)).spawnAsBoss();

										Location loc = player.getLocation();
										double ratio = ((double) REWIND_TIME - time) / REWIND_TIME;
										player.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, 1.0f + (float) ratio, 0.8f * (time % 10 == 0 ? 1 : 2));
										surround.location(loc.clone().add(0, 0.05, 0)).count(10 + (int) (70 * ratio)).spawnAsBoss();
										inside.location(loc).count(10 + (int) (20 * ratio)).spawnAsBoss();
									}
								}
							}

							@Override
							public synchronized void cancel() {
								super.cancel();
								mWindDown.reset();
							}
						};
						mActiveRunnables.add(inner);
						inner.runTaskTimer(plugin, 0, 1);

						this.cancel();
					}
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					mWindUp.reset();
				}
			};
			mActiveRunnables.add(outer);
			outer.runTaskTimer(plugin, 0, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
