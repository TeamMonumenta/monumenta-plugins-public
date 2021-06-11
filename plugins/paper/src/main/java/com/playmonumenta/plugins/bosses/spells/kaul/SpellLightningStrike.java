package com.playmonumenta.plugins.bosses.spells.kaul;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Lightning Strike (Always Active, does not use minecraft lighting).
 * Creates a patch of electrically charged ground (particle effects)
 * below 1/3 (Based on player count) random players. 2 seconds later,
 * lighting strikes over the charged ground. This delay gives players
 * the chance to dodge the lighting strike if they are fast enough.
 * ( 22s cd )
 */
public class SpellLightningStrike extends Spell {

	private int mCooldown = 0;
	private LivingEntity mBoss;
	private Plugin mPlugin;
	private double mRange;
	private int mTimer;
	private int mDivisor;
	private Location mLoc;
	private static final double HITBOX_HEIGHT = 10;
	private static final double HITBOX_RADIUS = 3;
	private static final double HITBOX_MAX_DIST = Math.sqrt(HITBOX_HEIGHT * HITBOX_HEIGHT + HITBOX_RADIUS * HITBOX_RADIUS);
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);

	public SpellLightningStrike(Plugin plugin, LivingEntity boss, Location loc, double range, int timer, int divisor) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mTimer = timer;
		mDivisor = divisor;
		mLoc = loc;

		if (mTimer == 20 * 18) {
			mCooldown = mTimer / 5;
		}
	}

	@Override
	public void run() {
		if (SpellPutridPlague.getPlagueActive()) {
			return;
		}

		mCooldown--;
		if (mCooldown <= 0) {
			mCooldown = (mTimer / 5);
			List<Player> players = PlayerUtils.playersInRange(mLoc, mRange, true);
			if (players.size() > 2) {
				List<Player> toHit = new ArrayList<Player>();
				int cap = players.size() / mDivisor;
				for (int i = 0; i < cap; i++) {
					Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
					if (!toHit.contains(player)) {
						toHit.add(player);
					} else {
						cap++;
					}
				}

				for (Player player : toHit) {
					lightning(player);
				}
			} else {
				for (Player player : players) {
					lightning(player);
				}
			}
		}
	}

	public void lightning(Player player) {
		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1, 1.25f);
		Location loc = player.getLocation();

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				world.spawnParticle(Particle.REDSTONE, loc, 12, 1.5, 0.1, 1.5, YELLOW_1_COLOR);
				world.spawnParticle(Particle.REDSTONE, loc, 12, 1.5, 0.1, 1.5, YELLOW_2_COLOR);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 1, 0), 3, 1.5, 0.5, 1.5, 0.05);
				Location prestrike = loc.clone().add(0, 10, 0);
				for (int i = 0; i < 10; i++) {
					prestrike.subtract(0, 1, 0);
					world.spawnParticle(Particle.FLAME, prestrike, 1, 0, 0, 0, 0.05);
				}
				if (mTicks >= 20 * 1.25) {
					this.cancel();
					Location strike = loc.clone().add(0, 10, 0);
					for (int i = 0; i < 10; i++) {
						strike.subtract(0, 1, 0);
						world.spawnParticle(Particle.REDSTONE, strike, 10, 0.3, 0.3, 0.3, YELLOW_1_COLOR);
						world.spawnParticle(Particle.REDSTONE, strike, 10, 0.3, 0.3, 0.3, YELLOW_2_COLOR);
					}
					world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 10, 0, 0, 0, 0.25);
					world.spawnParticle(Particle.FLAME, loc, 30, 0, 0, 0, 0.175);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 10, 0, 0, 0, 0.25);
					world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.9f);
					List<Player> potentialHits = PlayerUtils.playersInRange(loc, HITBOX_MAX_DIST, true);
					for (Player p : potentialHits) {
						Location pLoc = player.getLocation();
						if (!(pLoc.getY() > loc.getY() + HITBOX_HEIGHT || pLoc.getY() < loc.getY() - HITBOX_HEIGHT)) {
							Location flattenedLoc = new Location(world, pLoc.getX(), loc.getY(), pLoc.getZ());
							if (flattenedLoc.distance(loc) < HITBOX_RADIUS) {
								multiHit(p);
							}
						}
					}
					lingeringDamage(world, loc);
				}
			}

		}.runTaskTimer(mPlugin, 10, 1);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public void multiHit(Player p) {
		new BukkitRunnable() {
			int mNDT = p.getNoDamageTicks();
			int mInc = 0;
			@Override
			public void run() {
				World world = mBoss.getWorld();
				mInc++;
				if (mInc < 22 && mInc % 2 == 0) {
					p.setNoDamageTicks(0);
					world.spawnParticle(Particle.CRIT_MAGIC, p.getLocation(), 30, 0.1, 0.1, 0.1, 0.75);
					BossUtils.bossDamagePercent(mBoss, p, 0.05, (Location)null);
					// Doesn't matter if the player is blocking, there are 18 hits and only one can be blocked
				}
				if (mInc >= 20) {
					p.setNoDamageTicks(mNDT);
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void lingeringDamage(World world, Location loc) {
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks += 2;

				for (double deg = 0; deg < 360; deg += 15) {
					world.spawnParticle(Particle.FLAME, loc.clone().add(FastUtils.cos(deg) * 3, 0, FastUtils.sin(deg) * 3), 1, 0.15, 0.15, 0.15, 0.05);
				}
				world.spawnParticle(Particle.REDSTONE, loc, 10, 2, 0.3, 2, YELLOW_1_COLOR);
				world.spawnParticle(Particle.REDSTONE, loc, 10, 2, 0.3, 2, YELLOW_2_COLOR);
				if (mTicks % 10 == 0) {
					for (Player player : PlayerUtils.playersInRange(loc, 3)) {
						if (loc.distance(player.getLocation()) < Kaul.detectionRange) {
							BossUtils.bossDamagePercent(mBoss, player, 0.1, (Location)null);
							player.setFireTicks(20 * 3);
						}
					}
				}

				if (mTicks >= 20 * 5) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 5, 2);
		this.cancel();
	}
}
