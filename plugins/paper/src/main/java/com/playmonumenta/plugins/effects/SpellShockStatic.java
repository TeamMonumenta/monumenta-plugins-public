package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.cosmetics.skills.mage.SpellshockCS;
import org.bukkit.entity.Entity;

public class SpellShockStatic extends Effect {
	public static final String effectID = "SpellShockStatic";
	private final SpellshockCS mCosmetic;
	private boolean mTriggered = false;

	public SpellShockStatic(int duration, SpellshockCS cosmetic) {
		super(duration, effectID);
		mCosmetic = cosmetic;
	}

	public boolean isTriggered() {
		return mTriggered;
	}

	public void trigger() {
		mTriggered = true;
		setDuration(0);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		mCosmetic.tickEffect(entity);
	}

	@Override
	public String toString() {
		return String.format("SpellShockStatic duration=%d", this.getDuration());
	}
}
