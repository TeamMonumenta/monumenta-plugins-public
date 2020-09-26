package com.playmonumenta.plugins.bosses.spells;

import java.util.ArrayList;
import java.util.Iterator;
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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellShadowThorns extends Spell {

	private static final Particle.DustOptions ATTACK_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 100), 1.0f);

	private static final int RANGE = 10;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final int mCooldown;
	private final int mDamage;

	public SpellShadowThorns(Plugin plugin, LivingEntity boss, int cooldown, int damage) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mCooldown = cooldown;
		mDamage = damage;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		mWorld.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1, 0.5f);
		mWorld.spawnParticle(Particle.REDSTONE, loc, 300, 3, 3, 3, 0, ATTACK_COLOR);
		List<Player> players = PlayerUtils.playersInRange(loc, RANGE * 2);
		Vector direction = loc.getDirection();
		double initialAngle = Math.atan2(direction.getX(), direction.getZ());
		boolean clockwise = FastUtils.RANDOM.nextBoolean();

		for (int i = 0; i < 60; i++) {
			int delay = 20 + (i % 25) * 3;
			double angle = initialAngle + (clockwise ? (i * Math.PI / -25) : (i * Math.PI / 25));

			BukkitRunnable attack = new BukkitRunnable() {
				List<Player> mPlayers = new ArrayList<Player>(players);
				double mAngle = angle;
				int mTicks = 0;

				@Override
				public void run() {
					Location loc = mBoss.getLocation().add(0, 1, 0);
					BoundingBox hitbox = new BoundingBox().shift(loc).expand(0.5);
					Vector shift = new Vector(Math.sin(mAngle), 0, Math.cos(mAngle));
					// I would use "i" instead of "j" here but then checkstyle erroneously complains
					for (int j = 0; j < RANGE - 2 * Math.abs(mTicks - RANGE / 2); j++) {
						loc.add(shift);
						hitbox.shift(shift);
						mWorld.spawnParticle(Particle.SQUID_INK, loc, 1, 0.3, 0.3, 0.3, 0);
						mWorld.spawnParticle(Particle.REDSTONE, loc, 1, 0.6, 0.6, 0.6, 0, ATTACK_COLOR);

						Iterator<Player> iter = mPlayers.iterator();
						while (iter.hasNext()) {
							Player player = iter.next();
							if (player.getBoundingBox().overlaps(hitbox)) {
								BossUtils.bossDamage(mBoss, player, mDamage);
								iter.remove();
							}
						}

						if (!loc.getBlock().isPassable()) {
							this.cancel();
							break;
						}
					}

					mTicks++;
					if (mTicks >= RANGE) {
						this.cancel();
					}
				}
			};

			attack.runTaskTimer(mPlugin, delay, 1);
			mActiveRunnables.add(attack);
		}
	}

	@Override
	public int duration() {
		return mCooldown;
	}

}
