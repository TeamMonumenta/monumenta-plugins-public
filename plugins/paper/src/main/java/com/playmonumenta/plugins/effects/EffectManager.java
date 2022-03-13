package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public final class EffectManager implements Listener {

	private static class Effects {

		/*
		 * The first map layer divides the effects into 3 buckets, which define a loose trigger order (i.e. everything
		 * with EffectPriority.EARLY will trigger before everything from EffectPriority.NORMAL, but order within each
		 * bucket is undefined).
		 *
		 * The second map layer uses String keys as "sources" of the effects, and ordered sets of effects as values.
		 * Sources could be specific to an ability, like "PowerInjectionPercentSpeedEffect" (a speed buff given by the
		 * Power Injection ability), or more generic, like "VulnerabilityEffect" (increased damage, given by a variety
		 * of abilities).
		 *
		 * Importantly, only the Effect with the highest "magnitude" from any given source is applied. Thus, all
		 * ability specific buffs will stack with each other, while generic Vulnerability will only have the strongest
		 * application take effect. Effects from the same source should always be the same type, and only ever have
		 * differing durations and magnitudes.
		 *
		 * The ordered sets themselves are sorted by magnitude. While only the top Effect is ever applied, all Effects
		 * are tracked and ticked down by the over-arching runnable, meaning that after a stronger Effect wears off,
		 * longer lasting weaker Effects are still active and will be applied.
		 */
		public final Map<EffectPriority, HashMap<String, NavigableSet<Effect>>> mPriorityMap = new EnumMap<>(EffectPriority.class);
		public final Entity mEntity;

		public Effects(Entity entity) {
			mEntity = entity;
			mPriorityMap.put(EffectPriority.EARLY, new HashMap<String, NavigableSet<Effect>>());
			mPriorityMap.put(EffectPriority.NORMAL, new HashMap<String, NavigableSet<Effect>>());
			mPriorityMap.put(EffectPriority.LATE, new HashMap<String, NavigableSet<Effect>>());
		}

		public void addEffect(String source, Effect effect) {
			Map<String, NavigableSet<Effect>> priorityEffects = mPriorityMap.get(effect.getPriority());

			NavigableSet<Effect> effectGroup = priorityEffects.get(source);
			if (effectGroup == null) {
				effectGroup = new TreeSet<Effect>();
				priorityEffects.put(source, effectGroup);
			}

			if (!effectGroup.isEmpty()) {
				Effect currentEffect = effectGroup.last();
				effectGroup.add(effect);
				if (effectGroup.last() == effect) {
					currentEffect.entityLoseEffect(mEntity);
					effect.entityGainEffect(mEntity);
				}
			} else {
				effectGroup.add(effect);
				effect.entityGainEffect(mEntity);
			}
		}

		public @Nullable NavigableSet<Effect> getEffects(String source) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				NavigableSet<Effect> effectGroup = priorityEffects.get(source);
				if (effectGroup != null) {
					return effectGroup;
				}
			}

			return null;
		}

		public NavigableSet<? extends Effect> getEffects(Class<? extends Effect> cls) {
			NavigableSet<Effect> effectSet = new TreeSet<Effect>();
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				for (NavigableSet<Effect> effects : priorityEffects.values()) {
					for (Effect effect : effects) {
						if (cls.isInstance(effect)) {
							effectSet.add(effect);
						}
					}
				}
			}
			return effectSet;
		}

		public boolean hasEffect(String source) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				NavigableSet<Effect> effectGroup = priorityEffects.get(source);
				if (effectGroup != null && !effectGroup.isEmpty()) {
					return true;
				}
			}

			return false;
		}

		public boolean hasEffect(Class<? extends Effect> cls) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				for (NavigableSet<Effect> effects : priorityEffects.values()) {
					for (Effect effect : effects) {
						if (cls.isInstance(effect)) {
							return true;
						}
					}
				}
			}

			return false;
		}

		public @Nullable NavigableSet<Effect> clearEffects(String source) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				NavigableSet<Effect> removedEffectGroup = priorityEffects.remove(source);
				if (removedEffectGroup != null) {
					removedEffectGroup.last().entityLoseEffect(mEntity);
					return removedEffectGroup;
				}
			}

			return null;
		}

		public void clearEffects() {
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				for (NavigableSet<Effect> removedEffect : priorityEffects.values()) {
					removedEffect.last().entityLoseEffect(mEntity);
				}

				priorityEffects.clear();
			}
		}

		/**
		 * Gets all effects as a json object
		 */
		public JsonObject getAsJsonObject() {
			JsonObject ret = new JsonObject();
			for (Map.Entry<EffectPriority, HashMap<String, NavigableSet<Effect>>> priorityEntries : mPriorityMap.entrySet()) {
				JsonObject mid = new JsonObject();
				for (Map.Entry<String, NavigableSet<Effect>> effects : priorityEntries.getValue().entrySet()) {
					JsonArray inner = new JsonArray();
					for (Effect effect : effects.getValue()) {
						inner.add(effect.toString());
					}
					mid.add(effects.getKey(), inner);
				}
				ret.add(priorityEntries.getKey().name(), mid);
			}
			return ret;
		}
	}

	private static final int PERIOD = 5;

	private final Map<Entity, Effects> mEntities = new HashMap<Entity, Effects>();
	private final BukkitRunnable mTimer;
	private static @Nullable EffectManager INSTANCE = null;

	@SuppressWarnings("unchecked")
	public EffectManager(Plugin plugin) {
		INSTANCE = this;
		/*
		 * This timer also ticks down for offline players. Keeping it like this for now since most custom effects we
		 * want to apply are short (no more than 30 seconds, e.g. class abilities) and already function this way.
		 */
		mTimer = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += PERIOD;
				boolean fourHertz = mTicks % 5 == 0;
				boolean twoHertz = mTicks % 10 == 0;
				boolean oneHertz = mTicks % 20 == 0;

				// Periodic trigger for Effects in case they need stuff like particles
				try {
					for (Map.Entry<Entity, Effects> entry : mEntities.entrySet()) {
						for (HashMap<String, NavigableSet<Effect>> priorityEffects : entry.getValue().mPriorityMap.values()) {
							// Have to make a copy of the map to prevent concurrent modification exceptions in case ticking changes the effects :(
							for (NavigableSet<Effect> effectGroup : ((HashMap<String, NavigableSet<Effect>>)priorityEffects.clone()).values()) {
								try {
									effectGroup.last().entityTickEffect(entry.getKey(), fourHertz, twoHertz, oneHertz);
								} catch (Exception ex) {
									Plugin.getInstance().getLogger().severe("Error in effect manager entityTickEffect: " + ex.getMessage());
									ex.printStackTrace();
								}
							}
						}
					}

					// Count down the durations of all Effects, and remove expired Effects and empty sets
					Iterator<Effects> entityIter = mEntities.values().iterator();
					while (entityIter.hasNext()) {
						Effects effects = entityIter.next();
						Entity entity = effects.mEntity;
						if (entity.isDead() || !entity.isValid()) {
							if (!(entity instanceof Player) || ((Player) entity).isOnline()) {
								entityIter.remove();
								continue;
							}
						}

						Iterator<HashMap<String, NavigableSet<Effect>>> effectsIter = effects.mPriorityMap.values().iterator();
						while (effectsIter.hasNext()) {
							Iterator<NavigableSet<Effect>> priorityEffectsIter = effectsIter.next().values().iterator();
							while (priorityEffectsIter.hasNext()) {
								NavigableSet<Effect> effectGroup = priorityEffectsIter.next();
								if (effectGroup.isEmpty()) {
									priorityEffectsIter.remove();
									continue;
								}

								boolean currentEffectRemoved = false;
								Effect currentEffect = effectGroup.last();
								Iterator<Effect> effectGroupIter = effectGroup.descendingIterator();
								while (effectGroupIter.hasNext()) {
									Effect effect = effectGroupIter.next();

									if (currentEffectRemoved) {
										try {
											effect.entityGainEffect(entity);
										} catch (Exception ex) {
											Plugin.getInstance().getLogger().severe("Error in effect manager entityGainEffect: " + ex.getMessage());
											ex.printStackTrace();
										}
										currentEffectRemoved = false;
									}

									boolean tickResult;
									try {
										tickResult = effect.tick(PERIOD);
									} catch (Exception ex) {
										Plugin.getInstance().getLogger().severe("Error in effect manager tick: " + ex.getMessage());
										ex.printStackTrace();
										/* If ticking throws an exception (i.e. NPE) remove it */
										tickResult = false;
									}
									if (tickResult) {
										if (effect == currentEffect) {
											try {
												effect.entityLoseEffect(entity);
											} catch (Exception ex) {
												Plugin.getInstance().getLogger().severe("Error in effect manager entityLoseEffect: " + ex.getMessage());
												ex.printStackTrace();
											}
											currentEffectRemoved = true;
										}

										effectGroupIter.remove();

										if (!effectGroup.isEmpty()) {
											currentEffect = effectGroup.last();
										} else {
											priorityEffectsIter.remove();
										}
									}
								}
							}
						}
					}
				} catch (Exception ex) {
					Plugin.getInstance().getLogger().severe("SEVERE error in effect manager ticking task that caused many pieces to be skipped: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		};

		mTimer.runTaskTimer(plugin, 0, PERIOD);
	}

	public static EffectManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Applies an effect to an entity.
	 * <p>
	 * You MUST assign difference "sources" to different effect types. The source should only be used to track level and duration overrides of the same effect.
	 * <p>
	 * You MUST create a new Effect object for each effect applied, as durations are tracked by the Effect object.
	 *
	 * @param entity the entity to receive the effect
	 * @param source the source of the effect (only the highest effect from a source applies)
	 * @param effect the effect to be applied
	 */
	public void addEffect(Entity entity, String source, Effect effect) {
		Effects effects = mEntities.get(entity);
		if (effects == null) {
			effects = new Effects(entity);
			mEntities.put(entity, effects);
		}

		effects.addEffect(source, effect);
	}

	/**
	 * Returns effects from a given source from an entity.
	 *
	 * @param entity the entity being checked
	 * @param source the source of effects to be retrieved
	 * @return the set of effects if they exist, null otherwise
	 */
	public @Nullable NavigableSet<Effect> getEffects(Entity entity, String source) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.getEffects(source);
		}

		return null;
	}

	/**
	 * Returns effects of a given type from an entity.
	 *
	 * @param entity the entity being checked
	 * @param type   the class of effect
	 * @return the set of effects if they exist, an empty set otherwise
	 */
	public NavigableSet<? extends Effect> getEffects(Entity entity, Class<? extends Effect> type) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.getEffects(type);
		}
		return Collections.emptyNavigableSet();
	}

	/**
     * Returns if an entity has an effect source or not.
     *
     * @param  entity the entity being checked
     * @param  source the source of effects to be checked
     * @return whether the entity has the effect or not
     */
	public boolean hasEffect(Entity entity, String source) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.hasEffect(source);
		}
		return false;
	}

	/**
     * Returns if an entity has an effect source or not.
     *
     * @param  entity the entity being checked
     * @param  type the class of effect
     * @return whether the entity has the effect or not
     */
	public boolean hasEffect(Entity entity, Class<? extends Effect> type) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.hasEffect(type);
		}
		return false;
	}


	/**
	 * Clears and returns effects from a given source from an entity.
	 *
	 * @param entity the entity to clear effects from
	 * @param source the source of effects to be cleared
	 * @return the set of effects if effects were removed, null otherwise
	 */
	public @Nullable NavigableSet<Effect> clearEffects(Entity entity, String source) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.clearEffects(source);
		}

		return null;
	}

	/**
	 * Clears effects from an entity.
	 *
	 * @param entity the entity to clear effects from
	 */
	public void clearEffects(Entity entity) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			effects.clearEffects();
			mEntities.remove(entity);
		}
	}

	/**
	 * Gets all effects for an entity as a single json object
	 *
	 * @param entity the entity to get effects for
	 */
	public JsonObject getAsJsonObject(Entity entity) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.getAsJsonObject();
		}
		return new JsonObject();
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
		Effects effects = mEntities.get(event.getEntity());
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					if (!effectGroup.last().entityRegainHealthEvent(event)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public boolean entityDeathEvent(EntityDeathEvent event) {
		Effects effects = mEntities.get(event.getEntity());
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					if (!effectGroup.last().entityKilledEvent(event)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	//@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		if (event.isCancelled()) {
			return;
		}
		LivingEntity damagee = event.getDamagee();
		LivingEntity source = event.getSource();

		Effects effects = mEntities.get(damagee);
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					if (event.isCancelled()) {
						return;
					}
					effectGroup.last().onHurt(damagee, event);
					Entity damager = event.getDamager();
					if (damager != null) {
						effectGroup.last().onHurtByEntity(damagee, event, damager);
						if (source != null) {
							effectGroup.last().onHurtByEntityWithSource(damagee, event, damager, source);
						}
					}
				}
			}
		}

		if (source != null) {
			Effects sourceEffects = mEntities.get(source);
			if (sourceEffects != null) {
				for (Map<String, NavigableSet<Effect>> priorityEffects : sourceEffects.mPriorityMap.values()) {
					for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
						if (event.isCancelled()) {
							return;
						}
						effectGroup.last().onDamage(source, event, damagee);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		// 1s after the player leaves, remove them from the map to avoid leaking memory
		// If the player happens to log back in immediately, still remove them - they will get a new entity object which will be tracked again
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			// NOTE: If the Entity map is ever changed to key'd by Entity UUID, this will need a guard to check that the player didn't log back in
			mEntities.remove(event.getPlayer());
		}, 20);

		// TODO: There is nothing that actually stores the player's effects before they log out.
		// The above code is NOT the reason that a player can log out and back in to clear their effects
		// Fixing this will require serializing the Effects to plugindata, then restoring them on player join
	}
}
