package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellSurroundingDeath extends Spell {

	public static final int DAMAGE = 45;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	public int mCooldownTicks;
	private Location mStartLoc;
	public Nucleus mBossInstance;

	public SpellSurroundingDeath(Plugin plugin, LivingEntity boss, Location startLoc, int cooldownTicks, Nucleus bossInstance) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = startLoc;
		mCooldownTicks = cooldownTicks;
		mBossInstance = bossInstance;
	}

	@Override
	public boolean canRun() {
		return mBossInstance.mIsHidden;
	}

	@Override
	public void run() {
		cast(21, 30);

		new BukkitRunnable() {
			@Override
			public void run() {
				cast(16, 21);
			}
		}.runTaskLater(mPlugin, 40);

		new BukkitRunnable() {
			@Override
			public void run() {
				cast(9, 16);
			}
		}.runTaskLater(mPlugin, 80);

		new BukkitRunnable() {
			@Override
			public void run() {
				cast(0, 9);
			}
		}.runTaskLater(mPlugin, 120);
	}

	public void cast(int innerRadius, int outerRadius) {

		World world = mStartLoc.getWorld();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT += 5;

				//Spawn particles
				if (mT == 5 || mT == 15) {
					for (double deg = 0; deg < 360; deg += 3) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = innerRadius; x < outerRadius; x++) {
							new PartialParticle(Particle.SPELL_WITCH, mStartLoc.clone().add(cos * x, 0, sin * x), 1, 0.1, 0.1, 0.1, 0)
								.minimumMultiplier(false).spawnAsEntityActive(mBoss);
						}
					}
				}

				//Play knockup sound
				if (mT % 5 == 0 && mT < 25) {
					world.playSound(mStartLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 10, 0f);
				}

				if (mT == 25) {
					world.playSound(mStartLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 10.5f, 2);
					world.playSound(mStartLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 10.5f, 1);

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

							new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0).minimumMultiplier(false).spawnAsEntityActive(mBoss);
							if (deg % 4 == 0) {
								new PartialParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.SHROOMLIGHT.createBlockData()).minimumMultiplier(false).spawnAsEntityActive(mBoss);
							} else {
								new PartialParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.CRIMSON_HYPHAE.createBlockData()).minimumMultiplier(false).spawnAsEntityActive(mBoss);
							}

							if (deg % 30 == 0) {
								new PartialParticle(Particle.LAVA, loc, 1, 0.15, 0.1, 0.15, 0.25).minimumMultiplier(false).spawnAsEntityActive(mBoss);
							}

						}
					}

					for (Player p : PlayerUtils.playersInRange(mStartLoc, outerRadius, true)) {
						if (!PlayerUtils.playersInRange(mStartLoc, innerRadius, true).contains(p)) {
							DamageUtils.damage(mBoss, p, DamageType.MAGIC, DAMAGE, null, false, true, "Surrounding Death");
							MovementUtils.knockAway(mStartLoc, p, 0, .75f, false);
						}
					}
					this.cancel();
				}

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
