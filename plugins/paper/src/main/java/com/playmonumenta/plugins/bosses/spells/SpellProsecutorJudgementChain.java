package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.ProsecutorJudgementChainBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellProsecutorJudgementChain extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final ProsecutorJudgementChainBoss.Parameters mParams;

	private BiConsumer<LivingEntity, DamageEvent.DamageType> mAttackedByEntityAction = (entity, type) -> {
	};

	public SpellProsecutorJudgementChain(Plugin plugin, LivingEntity boss, ProsecutorJudgementChainBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mParams = parameters;
	}

	@Override
	public void run() {
		ChainRunnable castRunnable = new ChainRunnable();
		mAttackedByEntityAction = castRunnable::onHurtByEntityWithType;

		castRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(castRunnable);
	}

	@Override
	public int cooldownTicks() {
		return mParams.COOLDOWN;
	}

	private class ChainRunnable extends BukkitRunnable {
		int mT = 0;

		final List<? extends LivingEntity> mTargets = mParams.TARGETS.getTargetsList(mBoss);
		final Map<LivingEntity, Integer> mBreakCounter = new HashMap<>();

		@Override
		public void run() {
			if (mTargets.isEmpty() || mT >= mParams.PULL_TIME || EntityUtils.shouldCancelSpells(mBoss)) {
				this.cancel();
			}

			if (mT % mParams.PULL_FREQUENCY == 0) {
				List.copyOf(mTargets).forEach(livingEntity -> {
					final Location halfHeightLocBoss = LocationUtils.getHalfHeightLocation(mBoss);
					final Location halfHeightLocTarget = LocationUtils.getHalfHeightLocation(livingEntity);

					Vector vec = LocationUtils.getDirectionTo(halfHeightLocTarget, halfHeightLocBoss);
					Hitbox hitbox = Hitbox.approximateCylinder(mBoss.getLocation(), livingEntity.getLocation(), 0.6, false);
					hitbox.getHitPlayers(true).stream()
						.filter(player -> player != livingEntity)
						.forEach(breaker -> {
							if (mBreakCounter.getOrDefault(livingEntity, 1) >= mParams.BREAK_REQUIREMENT) {
								mParams.SOUND_BREAK.play(livingEntity.getLocation());
								mTargets.remove(livingEntity);
							} else {
								mParams.SOUND_CHIP.play(livingEntity.getLocation());
								mBreakCounter.put(livingEntity, mBreakCounter.getOrDefault(livingEntity, 1) + 1);
							}
						});
					livingEntity.setVelocity(vec.clone().multiply(-mParams.PULL_VELOCITY));
					mParams.SOUND_PULL.play(livingEntity.getLocation());
					mParams.PARTICLE_CHAIN.spawn(mBoss, particle ->
						new PPLine(particle, halfHeightLocBoss, halfHeightLocTarget));
				});
			}
			mT++;
		}

		public void onHurtByEntityWithType(LivingEntity entity, DamageEvent.DamageType type) {
			if (type == mParams.DAMAGE_TYPE) {
				mParams.SOUND_BREAK.play(entity.getLocation());
				mTargets.remove(entity);
			}
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		mAttackedByEntityAction.accept(source, event.getType());
	}
}
