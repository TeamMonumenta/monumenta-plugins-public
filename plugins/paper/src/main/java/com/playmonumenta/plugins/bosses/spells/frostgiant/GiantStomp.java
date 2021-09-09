package com.playmonumenta.plugins.bosses.spells.frostgiant;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class GiantStomp extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mTimer;
	private int mCooldown;

	private static final Particle.DustOptions BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 128), 1.0f);


	//direction is 0 or 180

	public GiantStomp(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mTimer = 5 * 20;
	}

	@Override
	public void run() {
		if (FrostGiant.castStomp) {
			mCooldown -= 5;
			if (mCooldown <= 0) {
				mCooldown = mTimer;
				World world = mBoss.getWorld();

				Vector dir = mBoss.getLocation().getDirection();

				BukkitRunnable runnable = new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						Location loc = mBoss.getLocation();
						loc.setDirection(dir);

						int radius = 5;
						Vector vec;

						world.playSound(loc, Sound.ENTITY_RAVAGER_STEP, SoundCategory.HOSTILE, 3, 0.6f);

						for (double degree = 0; degree < 360; degree += 15) {
							double radian1 = Math.toRadians(degree);
							for (int i = 1; i <= radius; i++) {
								vec = new Vector(FastUtils.cos(radian1) * i, 0, FastUtils.sin(radian1) * i);
								vec = VectorUtils.rotateYAxis(vec, 5);
								Location l = loc.clone().add(vec);
								world.spawnParticle(Particle.REDSTONE, l, 5, 0.1, 1, 0.1, 0.1, BLUE_COLOR);
							}
						}

						if (mTicks >= 20) {
							for (Player player : PlayerUtils.playersInRange(loc, radius, true)) {
								BossUtils.bossDamage(mBoss, player, 40, null, "Giant Stomp");
								MovementUtils.knockAway(mBoss.getLocation(), player, 0.5f, 0.1f, true);
							}
							this.cancel();
						}
						mTicks += 4;
					}
				};
				runnable.runTaskTimer(mPlugin, 0, 4);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 7 * 20;
	}

}
