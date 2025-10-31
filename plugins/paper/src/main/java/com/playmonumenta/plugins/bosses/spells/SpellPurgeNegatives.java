package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class SpellPurgeNegatives extends Spell {
	private int mCooldown = 0;
	private final LivingEntity mBoss;
	private final int mTimer;

	private static final PotionEffectType[] NEGATIVE_EFFECTS = new PotionEffectType[]{
		PotionEffectType.BLINDNESS,
		PotionEffectType.POISON,
		PotionEffectType.CONFUSION,
		PotionEffectType.SLOW,
		PotionEffectType.SLOW_DIGGING,
		PotionEffectType.WITHER,
		PotionEffectType.WEAKNESS,
		PotionEffectType.HARM,
		PotionEffectType.HUNGER,
		PotionEffectType.LEVITATION,
		PotionEffectType.UNLUCK
	};

	public SpellPurgeNegatives(LivingEntity boss, int timer) {
		mBoss = boss;
		mTimer = timer;
	}

	@Override
	public void run() {
		mCooldown -= 5;
		if (mCooldown <= 0) {
			mCooldown = mTimer;
			for (PotionEffectType type : NEGATIVE_EFFECTS) {
				mBoss.removePotionEffect(type);
			}
			if (EntityUtils.isSlowed(Plugin.getInstance(), mBoss)) {
				EntityUtils.setSlowTicks(Plugin.getInstance(), mBoss, 0);
			}
			if (EntityUtils.isBleeding(Plugin.getInstance(), mBoss)) {
				EntityUtils.setBleedTicks(Plugin.getInstance(), mBoss, 0);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
