package com.playmonumenta.bossfights.spells.spells_kaul;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.utils.Utils;


/*
 * Lightning Strike (Always Active, does not use minecraft lighting).
 * Creates a patch of electrically charged ground (particle effects)
 * below 1/3 (Based on player count) random players. 2 seconds later,
 * lighting strikes over the charged ground. This delay gives players
 * the chance to dodge the lighting strike if they are fast enough.
 * ( 22s cd )
 */
public class SpellLightningStrike extends Spell {
	private int cooldown = 0;
	private LivingEntity mBoss;
	private Plugin mPlugin;
	private double mRange;
	private int mTimer;
	private int mDivisor;
	private Random random = new Random();
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(
	    Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(
	    Color.fromRGB(255, 255, 120), 1.0f);
	public SpellLightningStrike(Plugin plugin, LivingEntity boss, double range, int timer, int divisor) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mTimer = timer;
		mDivisor = divisor;
	}

	@Override
	public void run() {
		cooldown--;
		if (cooldown <= 0) {
			cooldown = (mTimer / 5);
			List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);
			if (players.size() > 2) {
				List<Player> toHit = new ArrayList<Player>();
				int cap = players.size() / mDivisor;
				for (int i = 0; i < cap; i++) {
					Player player = players.get(random.nextInt(players.size()));
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
		new BukkitRunnable() {
			Location loc = player.getLocation();
			int i = 0;
			@Override
			public void run() {
				i++;
				world.spawnParticle(Particle.REDSTONE, loc, 12, 1.5, 0.1, 1.5, YELLOW_1_COLOR);
				world.spawnParticle(Particle.REDSTONE, loc, 12, 1.5, 0.1, 1.5, YELLOW_2_COLOR);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 1, 0), 3, 1.5, 0.5, 1.5, 0.05);
				Location prestrike = loc.clone().add(0, 10, 0);
				for (int i = 0; i < 10; i++) {
					prestrike.subtract(0, 1, 0);
					world.spawnParticle(Particle.FLAME, prestrike, 1, 0, 0, 0, 0.05);
				}
				if (i >= 20 * 1.25) {
					this.cancel();
					Location strike = loc.clone().add(0, 10, 0);
					for (int i = 0; i < 10; i++) {
						strike.subtract(0, 1, 0);
						world.spawnParticle(Particle.REDSTONE, strike, 20, 0.3, 0.3, 0.3, YELLOW_1_COLOR);
						world.spawnParticle(Particle.REDSTONE, strike, 20, 0.3, 0.3, 0.3, YELLOW_2_COLOR);
					}
					world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 40, 0, 0, 0, 0.25);
					world.spawnParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.175);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 25, 0, 0, 0, 0.25);
					world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.9f);
					for (Player p : Utils.playersInRange(loc, 3)) {
						double newHealth = p.getHealth() - (p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.4);

						if (newHealth <= 0 && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
							// Kill the player, but allow totems to trigger
							p.damage(100, mBoss);
						} else if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
							p.setHealth(newHealth);
							p.damage(1, mBoss);
						}
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 0;
	}

}
