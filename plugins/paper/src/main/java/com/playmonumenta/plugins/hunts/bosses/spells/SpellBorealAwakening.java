package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.AlocAcoc;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellBorealAwakening extends Spell {
	private static final String SUMMON_LOS = "BorealSpirit";

	private static final int SUMMON_TIME = 40;

	private static final double MIN_RANGE = 5.5;
	private static final double MAX_RANGE = 12;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final AlocAcoc mAlocAcoc;

	public SpellBorealAwakening(Plugin plugin, LivingEntity boss, AlocAcoc alocAcoc) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mAlocAcoc = alocAcoc;
	}

	@Override
	public boolean canRun() {
		return mAlocAcoc.canRunSpell(this);
	}

	@Override
	public void run() {
		int total = summonCount();
		for (int i = 0; i < total; i++) {
			for (int a = 0; a < 20; a++) {
				double theta = FastUtils.randomDoubleInRange(i * (Math.PI * 2 / total), (i + 1) * (Math.PI * 2 / total));
				double distance = FastUtils.randomDoubleInRange(MIN_RANGE, MAX_RANGE);

				Location location = LocationUtils.fallToGround(mBoss.getLocation().clone().add(FastUtils.cos(theta) * distance, 5, FastUtils.sin(theta) * distance), 10);
				if (!location.getBlock().isSolid()) {
					summon(location);

					new PPLine(Particle.END_ROD, LocationUtils.getHalfHeightLocation(mBoss), location)
						.countPerMeter(4)
						.delta(0.075)
						.spawnAsBoss();
					break;
				}
			}
		}

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_CAMEL_DASH, SoundCategory.HOSTILE, 3f, 0.65f);
		mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 0.4f, 1.3f);
	}

	private void summon(Location location) {
		Entity entity = LibraryOfSoulsIntegration.summon(location.clone().subtract(0, 2, 0), SUMMON_LOS);
		if (!(entity instanceof LivingEntity summon)) {
			return;
		}

		mAlocAcoc.addSummon(entity);

		summon.setAI(false);
		summon.setGravity(false);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				summon.teleport(summon.getLocation().clone().add(0, 2.0 / SUMMON_TIME, 0));

				new PartialParticle(Particle.BLOCK_CRACK, location)
					.data(Material.ICE.createBlockData())
					.delta(0.25, 0, 0.25)
					.count(2)
					.spawnAsBoss();

				if (mTicks % 8 == 0) {
					mWorld.playSound(summon.getLocation(), Sound.ENTITY_HORSE_STEP, SoundCategory.HOSTILE, 1f, 1 + 0.3f * (float) mTicks / SUMMON_TIME);
				}

				if (mTicks == SUMMON_TIME) {
					mWorld.playSound(summon.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 1f, 0.5f);
				}

				mTicks++;
				if (mTicks > SUMMON_TIME || mBoss.isDead()) {
					summon.setAI(true);
					summon.setGravity(true);

					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				if (mBoss.isDead()) {
					summon.remove();
				}
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private int summonCount() {
		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), AlocAcoc.OUTER_RADIUS, true).size();
		return (int) ((double) playerCount / 2.5 + 1);
	}

	@Override
	public int cooldownTicks() {
		return SUMMON_TIME + 30;
	}
}
