package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;

public class KillTriggeredAbilityTracker {

	public interface KillTriggeredAbility {
		void triggerOnKill(LivingEntity mob);
	}

	private static final int DAMAGE_DEALT_TO_R1_BOSSES_PER_KILL = 100;
	private static final int DAMAGE_DEALT_TO_R2_BOSSES_PER_KILL = 200;

	private final KillTriggeredAbility mLinkedAbility;
	private final int mDamageDealtToBossesPerKill;

	private double mDamageDealtToBosses = 0;

	public KillTriggeredAbilityTracker(KillTriggeredAbility linkedAbility) {
		mLinkedAbility = linkedAbility;
		mDamageDealtToBossesPerKill = ServerProperties.getClassSpecializationsEnabled() ? DAMAGE_DEALT_TO_R2_BOSSES_PER_KILL : DAMAGE_DEALT_TO_R1_BOSSES_PER_KILL;
	}

	public void updateDamageDealtToBosses(DamageEvent event) {
		updateDamageDealtToBosses(event.getFinalDamage(false), event.getDamagee());
	}

	public void updateDamageDealtToBosses(double damage, LivingEntity mob) {
		if (EntityUtils.isBoss(mob)) {
			mDamageDealtToBosses += damage;
			if (mDamageDealtToBosses >= mDamageDealtToBossesPerKill) {
				mDamageDealtToBosses -= mDamageDealtToBossesPerKill;
				mLinkedAbility.triggerOnKill(mob);
			}
		}
	}

}
