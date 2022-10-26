package com.playmonumenta.plugins.bosses.spells.portalboss;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellRisingCircles extends Spell {

	public static final double DAMAGE = 0.6;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	public int mCooldownTicks;
	private Location mStartLoc;

	public SpellRisingCircles(Plugin plugin, LivingEntity boss, Location startLoc, int cooldownTicks) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = startLoc;
		mCooldownTicks = cooldownTicks;
	}

	@Override
	public void run() {
		if (FastUtils.RANDOM.nextBoolean()) {
			cast(0, 10);
		} else {
			cast(19, 27);
		}
	}

	public void cast(int innerRadius, int outerRadius) {

		World world = mStartLoc.getWorld();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {

				//Spawn particles
				if (mT % 20 == 0) {
					for (double deg = 0; deg < 360; deg += 3) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = innerRadius; x < outerRadius; x++) {
							world.spawnParticle(Particle.SPELL_WITCH, mStartLoc.clone().add(cos * x, -1, sin * x), 1, 0.1, 0.1, 0.1, 0);
						}
					}
				}

				//Play knockup sound
				if (mT % 20 == 0 && mT < 100) {
					world.playSound(mStartLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 10, 0f + (mT / 100.0f));
				}

				if (mT == 100) {
					world.playSound(mStartLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 10.5f, 2);
					world.playSound(mStartLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 10.5f, 1);

					//Increments the x counter based on how large the circle is
					int inc = 3;
					if (innerRadius == 0) {
						inc += 9;
					}

					for (double deg = 0; deg < 360; deg += inc * 2) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = innerRadius; x < outerRadius; x += inc) {
							Location loc = mStartLoc.clone().add(cos * x, 0, sin * x);

							world.spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0);
							if (deg % 4 == 0) {
								world.spawnParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.SHROOMLIGHT.createBlockData());
							} else {
								world.spawnParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.CRIMSON_HYPHAE.createBlockData());
							}

							if (deg % 30 == 0) {
								world.spawnParticle(Particle.LAVA, loc, 1, 0.15, 0.1, 0.15, 0.25);
							}

						}
					}

					for (Player p : PlayerUtils.playersInRange(mStartLoc, outerRadius, true)) {
						if (!PlayerUtils.playersInRange(mStartLoc, innerRadius, true).contains(p)) {
							BossUtils.bossDamagePercent(mBoss, p, DAMAGE, mStartLoc, "Sector Expunge");
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, "Sector Expunge", new PercentSpeed(2 * 20, -.99, "Rising Circles"));
							MovementUtils.knockAway(mStartLoc, p, 0, .75f, false);
						}
					}
					this.cancel();
				}
				mT += 5;
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(runnable);
	}


	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
