package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.VanguardChallengeBoss;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellVanguardChallenge extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final VanguardChallengeBoss.Parameters mParams;

	public SpellVanguardChallenge(Plugin plugin, LivingEntity boss, VanguardChallengeBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mParams = parameters;
	}

	@Override
	public void run() {
		mParams.SOUND.play(mBoss.getLocation());
		mParams.EFFECT_BOSS.apply(mBoss, mBoss);
		mActiveTasks.add(new BukkitRunnable() {
			private final List<? extends LivingEntity> mTargetsList = mParams.TARGETS.getTargetsList(mBoss);

			int mTicks = 0;

			@Override
			public void run() {
				if (mTargetsList.isEmpty() || mTicks >= mParams.DURATION) {
					this.cancel();
				}
				mTargetsList.forEach(target -> {
					if (target instanceof Player) {
						mParams.EFFECT_TARGET.apply(target, mBoss);
					}
					double[] rotations = VectorUtils.vectorToRotation(LocationUtils.getDirectionTo(mBoss.getLocation(), target.getLocation()));
					target.setRotation((float) rotations[0], (float) rotations[1]);
				});
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, mParams.INTERVAL));
	}

	@Override
	public int cooldownTicks() {
		return mParams.COOLDOWN;
	}
}
