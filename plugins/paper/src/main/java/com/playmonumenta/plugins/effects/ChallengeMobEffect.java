package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import org.bukkit.event.entity.EntityDeathEvent;

public class ChallengeMobEffect extends Effect {
	private static final String effectId = "ChallengeMobEffect";

	private final Challenge mChallenge;

	public ChallengeMobEffect(int duration, Challenge challenge) {
		super(duration, effectId);
		mChallenge = challenge;
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		mChallenge.incrementKills();
	}

	@Override
	public String toString() {
		return String.format("ChallengeMobEffect duration:%d", this.getDuration());
	}
}
