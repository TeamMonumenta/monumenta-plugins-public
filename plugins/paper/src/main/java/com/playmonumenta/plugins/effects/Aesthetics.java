package com.playmonumenta.plugins.effects;

import org.bukkit.entity.Entity;

public class Aesthetics extends Effect {

	@FunctionalInterface
	public interface TickEffectAction {
		/**
		 * @param entity    the entity with the effect
		 * @param fourHertz true every 5 ticks
		 * @param twoHertz  true every 10 ticks
		 * @param oneHertz  true every 20 ticks
		 */
		void run(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz);
	}

	@FunctionalInterface
	public interface LoseEffectAction {
		/**
		 * @param entity the entity losing the effect
		 */
		void run(Entity entity);
	}

	private final TickEffectAction mTickEffectAction;
	private final LoseEffectAction mLoseEffectAction;

	public Aesthetics(int duration, TickEffectAction tickEffectAction, LoseEffectAction loseEffectAction) {
		super(duration);
		mTickEffectAction = tickEffectAction;
		mLoseEffectAction = loseEffectAction;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (mTickEffectAction != null) {
			mTickEffectAction.run(entity, fourHertz, twoHertz, oneHertz);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (mLoseEffectAction != null) {
			mLoseEffectAction.run(entity);
		}
	}

}
