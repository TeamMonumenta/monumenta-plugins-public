package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collections;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class UnyieldingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_unyielding";
	public static final int detectionRange = 50;

	public static class Parameters extends BossParameters {
		public double DAMAGE_INCREASE = 0.3;
		public double SPEED_INCREASE = 0.1;
	}

	final Parameters mParam;
	private boolean mTriggered = false;


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
		if (!mTriggered && mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2.0 >= mBoss.getHealth() - event.getDamage()) {
			event.setDamage(mBoss.getHealth() - mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2.0);
			PotionUtils.clearNegatives(mBoss);
			EntityUtils.setWeakenTicks(com.playmonumenta.plugins.Plugin.getInstance(), mBoss, 0);
			EntityUtils.setSlowTicks(com.playmonumenta.plugins.Plugin.getInstance(), mBoss, 0);

			if (mBoss.getFireTicks() > 1) {
				mBoss.setFireTicks(1);
			}
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, "UnyieldingSpeedEffect", new PercentDamageDealt(999999999, mParam.SPEED_INCREASE));
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, "UnyieldingDamageEffect", new PercentDamageDealt(999999999, mParam.DAMAGE_INCREASE));
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, "UnyieldingResistanceEffect", new PercentDamageReceived(40, -1));
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					if (mTicks == 0) {
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ITEM_BREAK, 2f, 1f);
					}
					if (mTicks % 6 == 0) {
						mBoss.getWorld().spawnParticle(Particle.DRAGON_BREATH, mBoss.getLocation().add(0, mBoss.getBoundingBox().getHeight()/2, 0), 30, mBoss.getBoundingBox().getWidthX()/2.0, mBoss.getBoundingBox().getHeight()/2, mBoss.getBoundingBox().getWidthZ()/2.0, 0);
					}
					if (mTicks == 40) {
						this.cancel();
					}
					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
			mTriggered = true;
		}
	}
}


