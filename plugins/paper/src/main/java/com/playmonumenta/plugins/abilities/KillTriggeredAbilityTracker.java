package com.playmonumenta.plugins.abilities;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;

public class KillTriggeredAbilityTracker {

	public interface KillTriggeredAbility {
		void triggerOnKill(Entity mob);
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

	public void updateDamageDealtToBosses(EntityDamageByEntityEvent event) {
		if (EntityUtils.isBoss(event.getEntity())) {
			mDamageDealtToBosses += event.getFinalDamage();
			if (mDamageDealtToBosses >= mDamageDealtToBossesPerKill) {
				mDamageDealtToBosses -= mDamageDealtToBossesPerKill;
				mLinkedAbility.triggerOnKill(event.getEntity());
			}
		}
	}

}
