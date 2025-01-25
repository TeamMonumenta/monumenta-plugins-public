package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class GiantStomp extends Spell {
	private static final Particle.DustOptions BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 128), 1.0f);

	private final Plugin mPlugin;
	private final FrostGiant mFrostGiant;
	private final LivingEntity mBoss;
	private final int mTimer;
	private int mCooldown;

	public GiantStomp(final Plugin plugin, final FrostGiant frostGiant) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mBoss = mFrostGiant.mBoss;
		mTimer = Constants.TICKS_PER_SECOND * 5;
	}

	@Override
	public void run() {
		if (mFrostGiant.mCastStomp) {
			mCooldown -= BossAbilityGroup.PASSIVE_RUN_INTERVAL_DEFAULT;
		}

		if (mCooldown > 0 || mFrostGiant.getArenaParticipants().isEmpty()) {
			return;
		}

		mCooldown = mTimer;
		final World world = mBoss.getWorld();
		final Vector dir = mBoss.getLocation().getDirection();
		final int runnablePeriod = 4;

		final BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += runnablePeriod;

				final Location loc = mBoss.getLocation();
				final int radius = 5;
				loc.setDirection(dir);
				Vector vec;

				world.playSound(loc, Sound.ENTITY_RAVAGER_STEP, SoundCategory.HOSTILE, 3, 0.6f);

				for (double degree = 0; degree < 360; degree += 15) {
					for (int i = 1; i <= radius; i++) {
						vec = new Vector(FastUtils.cosDeg(degree) * i, 0, FastUtils.sinDeg(degree) * i);
						vec = VectorUtils.rotateYAxis(vec, 5);
						new PartialParticle(Particle.REDSTONE, loc.clone().add(vec), 5, 0.1, 1, 0.1, 0.1, BLUE_COLOR)
							.distanceFalloff(FrostGiant.ARENA_RADIUS).spawnAsEntityActive(mBoss);
					}
				}

				if (mTicks >= Constants.TICKS_PER_SECOND) {
					PlayerUtils.playersInRange(loc, radius, true).forEach(player -> {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, 35, null, false, false, "Giant Stomp");
						MovementUtils.knockAway(mBoss.getLocation(), player, 0.5f, 0.1f, true);
					});
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, runnablePeriod);
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 7;
	}
}
