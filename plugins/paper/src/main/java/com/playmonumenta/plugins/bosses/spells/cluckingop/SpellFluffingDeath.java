package com.playmonumenta.plugins.bosses.spells.cluckingop;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellFluffingDeath extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mRange;
	private Location mStartLoc;

	public SpellFluffingDeath(Plugin plugin, LivingEntity boss, int range, Location startLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mStartLoc = startLoc;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		teleport(mStartLoc);
		List<Player> players = PlayerUtils.playersInRange(mStartLoc, mRange);
		mBoss.setAI(false);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 12, 10));
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks += 2;
				float fTick = mTicks;
				float ft = fTick / 25;
				world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 3, 0.35, 0, 0.35, 0.1);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 0.5f + ft);
				if (mTicks >= 20 * 2) {
					this.cancel();
					new BukkitRunnable() {

						int mInnerTicks = 0;
						@Override
						public void run() {
							mInnerTicks++;

							for (int j = 0; j < 3; j++) {
								rainMeteor(mStartLoc.clone().add(FastUtils.randomDoubleInRange(-mRange, mRange), -1.25, FastUtils.randomDoubleInRange(-mRange, mRange)), 30);
							}

							//Target one random player. Have a meteor rain nearby them.
							if (players.size() > 1) {
								Player rPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
								Location loc = rPlayer.getLocation();
								rainMeteor(loc.add(FastUtils.randomDoubleInRange(-5, 5), 0, FastUtils.randomDoubleInRange(-5, 5)), 30);
							} else if (players.size() == 1) {
								Player rPlayer = players.get(0);
								Location loc = rPlayer.getLocation();
								rainMeteor(loc.add(FastUtils.randomDoubleInRange(-5, 5), 0, FastUtils.randomDoubleInRange(-5, 5)), 30);
							}

							if (mInnerTicks >= 20) {
								this.cancel();
								mBoss.setAI(true);
							}
						}

					}.runTaskTimer(mPlugin, 0, 10);
				}
			}

		}.runTaskTimer(mPlugin, 0, 2);
	}

	private void rainMeteor(Location loc, double spawnY) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			double mY = spawnY;
			@Override
			public void run() {
				mY -= 1;
				Location particle = loc.clone().add(0, mY, 0);
				world.spawnParticle(Particle.EXPLOSION_NORMAL, particle, 1, 0.2f, 0.2f, 0.2f, 0.05, null, true);
				world.spawnParticle(Particle.CLOUD, particle, 1, 0, 0, 0, 0, null, true);
				world.playSound(particle, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				if (mY <= 0) {
					this.cancel();
					world.spawnParticle(Particle.FLAME, loc, 25, 0, 0, 0, 0.175, null, true);
					world.spawnParticle(Particle.CLOUD, loc, 75, 0, 0, 0, 0.25, null, true);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.9f);
					for (Player player : PlayerUtils.playersInRange(loc, 4)) {
						BossUtils.bossDamage(mBoss, player, 1);
						MovementUtils.knockAway(loc, player, 0.5f, 0.65f);
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0f);
		world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0f);
		world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
	}

	@Override
	public int duration() {
		return 20 * 15;
	}

}
