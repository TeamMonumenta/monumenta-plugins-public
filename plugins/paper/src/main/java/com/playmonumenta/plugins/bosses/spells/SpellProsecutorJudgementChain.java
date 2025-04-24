package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.ProsecutorJudgementChainBoss;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellProsecutorJudgementChain extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final ProsecutorJudgementChainBoss.Parameters mParams;

	public SpellProsecutorJudgementChain(Plugin plugin, LivingEntity boss, ProsecutorJudgementChainBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mParams = parameters;
	}

	@Override
	public void run() {
		BukkitRunnable castRunnable = new BukkitRunnable() {
			int mT = 0;

			final List<? extends LivingEntity> mTargets = mParams.TARGETS.getTargetsList(mBoss);
			final Map<Player, Integer> mBreakCounter = new HashMap<>();

			@Override
			public void run() {
				if (mTargets.isEmpty() || mT >= mParams.PULL_TIME || EntityUtils.shouldCancelSpells(mBoss)) {
					this.cancel();
				}

				if (mT % mParams.PULL_FREQUENCY == 0) {
					List.copyOf(mTargets).forEach(livingEntity -> {
						if (livingEntity instanceof Player player) {
							final Location halfHeightLocBoss = LocationUtils.getHalfHeightLocation(mBoss);
							final Location halfHeightLocPlayer = LocationUtils.getHalfHeightLocation(player);

							BoundingBox box = BoundingBox.of(halfHeightLocBoss, 0.6f, 0.6f, 0.6f);
							Vector vec = LocationUtils.getDirectionTo(halfHeightLocPlayer, halfHeightLocBoss);
							double distance = halfHeightLocPlayer.distance(halfHeightLocBoss);

							boolean broken = false;

							for (Player breaker : mBoss.getLocation().getNearbyPlayers(mParams.TARGETS.getRange() + 10)) {
								for (int i = 0; i < 100; i++) {
									box.shift(vec.clone().multiply(0.01 * distance));
									if (player != breaker && box.overlaps(breaker.getBoundingBox())) {
										mParams.SOUND_CHIP.play(player);
										if (mParams.BREAK_REQUIREMENT == 1 || mBreakCounter.getOrDefault(player, 1) >= mParams.BREAK_REQUIREMENT) {
											broken = true;
											mParams.SOUND_BREAK.play(player);
											mTargets.remove(livingEntity);
											break;
										} else {
											mBreakCounter.put(player, mBreakCounter.getOrDefault(player, 1) + 1);
										}
									}
								}
								if (broken) {
									return;
								}
							}

							player.setVelocity(vec.clone().multiply(-mParams.PULL_VELOCITY));
							mParams.SOUND_PULL.play(player);
							mParams.PARTICLE_CHAIN.spawn(mBoss, particle ->
								new PPLine(particle, halfHeightLocBoss, halfHeightLocPlayer)
							);
						}
					});
				}
				mT++;
			}
		};
		castRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(castRunnable);
	}

	@Override
	public int cooldownTicks() {
		return mParams.COOLDOWN;
	}
}
