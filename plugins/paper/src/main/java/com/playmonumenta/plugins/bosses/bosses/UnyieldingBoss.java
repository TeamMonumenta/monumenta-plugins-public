package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collections;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class UnyieldingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_unyielding";
	public static final int detectionRange = 50;

	public static class Parameters extends BossParameters {
		public double HEALING = 0.02;
		public double DURATION_TICKS = 60;
		public double TICKS_TO_HEAL = 2;
	}

	final Parameters mParam;
	private boolean mTriggered = false;
	private @Nullable BukkitRunnable mHealingRunnable = null;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new UnyieldingBoss(plugin, boss);
	}

	public UnyieldingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new UnyieldingBoss.Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mTriggered) {
			return;
		}
		double maxHealth = EntityUtils.getMaxHealth(mBoss);
		double halfMaxHealth = maxHealth / 2.0;
		if (halfMaxHealth >= mBoss.getHealth() - event.getDamage()) {
			event.setDamage(mBoss.getHealth() - halfMaxHealth);
			PotionUtils.clearNegatives(mBoss);
			EntityUtils.setWeakenTicks(com.playmonumenta.plugins.Plugin.getInstance(), mBoss, 0);
			EntityUtils.setSlowTicks(com.playmonumenta.plugins.Plugin.getInstance(), mBoss, 0);

			if (mBoss.getFireTicks() > 1) {
				mBoss.setFireTicks(1);
			}
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, "UnyieldingResistanceEffect", new PercentDamageReceived(2, -1));

			mHealingRunnable = new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					// Cancel upon knock away, knockup, silence, stun, frozen (hard cc)
					if (mBoss.isDead() || !mBoss.hasAI() || mBoss.hasPotionEffect(PotionEffectType.SLOW_FALLING) || mBoss.hasPotionEffect(PotionEffectType.LEVITATION)) {
						interrupt();
						return;
					}
					if (mTicks == 0) {
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2f, 1f);
					}
					if (mTicks % mParam.TICKS_TO_HEAL == 0 && mBoss.getHealth() > 0) {
						double hp = mBoss.getHealth() + maxHealth * mParam.HEALING;
						mBoss.setHealth(Math.min(hp, maxHealth));
						new PartialParticle(Particle.VILLAGER_HAPPY, mBoss.getLocation().add(0, mBoss.getBoundingBox().getHeight() / 2, 0), 3, mBoss.getBoundingBox().getWidthX() / 2.0, mBoss.getBoundingBox().getHeight() / 2, mBoss.getBoundingBox().getWidthZ() / 2.0, 0).spawnAsEntityActive(mBoss);
					}
					if (mTicks >= mParam.DURATION_TICKS) {
						this.cancel();
					}
					mTicks++;
				}
			};
			mHealingRunnable.runTaskTimer(mPlugin, 0, 1);
			mTriggered = true;
		}
	}

	public void interrupt() {
		if (mHealingRunnable != null) {
			mHealingRunnable.cancel();
			mHealingRunnable = null;
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.HOSTILE, 2f, 1f);
		}
	}

	@Override
	public void bossStunned() {
		interrupt();
	}

	@Override
	public void bossSilenced() {
		interrupt();
	}

	@Override
	public void bossKnockedAway(float speed) {
		interrupt();
	}
}


