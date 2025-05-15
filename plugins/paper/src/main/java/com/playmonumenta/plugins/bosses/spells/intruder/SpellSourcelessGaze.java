package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.intruder.SourcelessGazeBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellSourcelessGaze extends Spell {
	private final Plugin mPlugin;
	private final Mob mBoss;
	private final SourcelessGazeBoss.Parameters mParameters;

	public SpellSourcelessGaze(Plugin plugin, LivingEntity boss, SourcelessGazeBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = (Mob) boss;
		mParameters = parameters;
		run();
	}

	@Override
	public void run() {
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += 2;
				// Try to find player
				LivingEntity target = mBoss.getTarget();

				if (target != null) {
					double[] yawPitch = VectorUtils.vectorToRotation(target.getLocation().toVector().subtract(mBoss.getLocation().toVector()));
					mBoss.setRotation((float) yawPitch[0], (float) yawPitch[1]);
				}

				if (mTicks >= mParameters.GAZE_DURATION) {
					if (target != null) {
						LibraryOfSoulsIntegration.summon(mBoss.getEyeLocation(), mParameters.SUMMON_NAME);
					}
					new PartialParticle(Particle.FLASH, mBoss.getLocation()).minimumCount(1).spawnAsBoss();
					mBoss.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 2));
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
