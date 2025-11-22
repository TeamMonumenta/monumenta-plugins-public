package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class SpellBerserk extends Spell {
	private static final String DAMAGE_SRC = "SpellBerserkDamage";
	private final LivingEntity mLauncher;

	public SpellBerserk(final LivingEntity boss) {
		mLauncher = boss;
	}

	@Override
	public void run() {
		if (mLauncher.isValid() && !mLauncher.isDead() && mLauncher.getHealth() <= EntityUtils.getMaxHealth(mLauncher) / 2.0) {
			Plugin.getInstance().mEffectManager.addEffect(mLauncher, DAMAGE_SRC,
				new PercentDamageDealt(TICKS_PER_SECOND, 0.1).damageTypes(DamageEvent.DamageType.getAllMeleeTypes()));
		}
	}

	@Override
	public int cooldownTicks() {
		return TICKS_PER_SECOND;
	}
}
