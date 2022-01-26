package com.playmonumenta.plugins.effects;

import org.bukkit.entity.Entity;

public class Aesthetics extends Effect {

	@FunctionalInterface
	public interface TickEffectAction {
		/**
		 * Ticking effects on the entity
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
		 * Run when the entity loses the effect
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

	@Override
	public String toString() {
		String tickClass = "null";
		if (mTickEffectAction != null) {
			tickClass = mTickEffectAction.getClass().getName();
		}
		String loseClass = "null";
		if (mLoseEffectAction != null) {
			loseClass = mLoseEffectAction.getClass().getName();
		}
		return String.format("Aesthetics duration:%d tickClass:%s loseClass:%s", this.getDuration(), tickClass, loseClass);
	}
}
