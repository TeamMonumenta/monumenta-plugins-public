package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
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
		public final Map<EffectPriority, Map<String, NavigableSet<Effect>>> mPriorityMap = new EnumMap<>(EffectPriority.class);
		public final Entity mEntity;

		public Effects(Entity entity) {
			mEntity = entity;
			// NB: concurrent maps and sets are used as the tab list reads effects from a different thread
			// mPriorityMap is not concurrent, as it is never updated after this constructor
			// (and we rely on its iteration order being the order of enum constants)
			mPriorityMap.put(EffectPriority.EARLY, new ConcurrentHashMap<>());
			mPriorityMap.put(EffectPriority.NORMAL, new ConcurrentHashMap<>());
			mPriorityMap.put(EffectPriority.LATE, new ConcurrentHashMap<>());
		}

		public void addEffect(String source, Effect effect) {
			Map<String, NavigableSet<Effect>> priorityEffects = mPriorityMap.get(effect.getPriority());

			NavigableSet<Effect> effectGroup = priorityEffects.computeIfAbsent(source, k -> new ConcurrentSkipListSet<>());

			if (!effectGroup.isEmpty()) {
				Effect currentEffect = effectGroup.last();

				// Iterate through effectGroup to check if there is already an effect with existing magnitude and less duration.
				boolean foundEffect = false;
				for (Effect effectIter : effectGroup) {
					if (effectIter.compareTo(effect) == 0 && effectIter.getDuration() < effect.getDuration()) {
						effectIter.entityLoseEffect(mEntity);
						effectIter.entityGainEffect(mEntity);
						effectIter.setDuration(effect.getDuration());
						foundEffect = true;
						break;
					}
				}

				// If effect with same magnitude is not found, simply add the effect
				if (!foundEffect) {
					effectGroup.add(effect);
				}

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

		@SuppressWarnings("unchecked")
		public <T extends Effect> NavigableSet<T> getEffects(Class<T> cls) {
			NavigableSet<T> effectSet = new TreeSet<>();
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				for (NavigableSet<Effect> effects : priorityEffects.values()) {
					for (Effect effect : effects) {
						if (cls.isInstance(effect)) {
							effectSet.add((T) effect);
						}
					}
				}
			}
			return effectSet;
		}

		// Gets ONLY active effects, effects with the largest magnitude.
		public List<Effect> getEffects() {
			List<Effect> effects = new ArrayList<>();
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				for (NavigableSet<Effect> eff : priorityEffects.values()) {
					effects.add(eff.last());
				}
			}
			return effects;
		}

		// Gets ALL effects, including non-active, hidden ones.
		public List<Effect> getAllEffects() {
			List<Effect> effects = new ArrayList<>();
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				for (NavigableSet<Effect> eff : priorityEffects.values()) {
					effects.addAll(eff);
				}
			}
			return effects;
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

		public @Nullable String getSource(Effect effect) {
			for (EffectPriority priority : mPriorityMap.keySet()) {
				Map<String, NavigableSet<Effect>> sourceMap = mPriorityMap.get(priority);
				for (String source : sourceMap.keySet()) {
					if (sourceMap.get(source).contains(effect)) {
						return source;
					}
				}
			}
			return null;
		}

		/**
		 * Gets all effects as a json object
		 */
		public JsonObject getAsJsonObject() {
			JsonObject ret = new JsonObject();
			for (Map.Entry<EffectPriority, Map<String, NavigableSet<Effect>>> priorityEntries : mPriorityMap.entrySet()) {
				JsonObject mid = new JsonObject();
				JsonArray source = new JsonArray();
				for (Map.Entry<String, NavigableSet<Effect>> effects : priorityEntries.getValue().entrySet()) {
					JsonArray inner = new JsonArray();
					for (Effect effect : effects.getValue()) {
						inner.add(effect.serialize());
					}
					mid.add(effects.getKey(), inner);
					source.add(effects.getKey());
				}
				mid.add("source", source);
				ret.add(priorityEntries.getKey().name(), mid);
			}
			return ret;
		}
	}

	@FunctionalInterface
	public interface EffectDeserializer {
		Effect deserialize(JsonObject object, Plugin plugin) throws Exception;
	}

	private static final Map<String, EffectDeserializer> mEffectDeserializer;

	static {
		mEffectDeserializer = new HashMap<>();
		mEffectDeserializer.put(AbilityCooldownDecrease.effectID, AbilityCooldownDecrease::deserialize);
		mEffectDeserializer.put(AbilityCooldownIncrease.effectID, AbilityCooldownIncrease::deserialize);
		mEffectDeserializer.put(AbilitySilence.effectID, AbilitySilence::deserialize);
		mEffectDeserializer.put(AbsorptionSickness.effectID, AbsorptionSickness::deserialize);
		mEffectDeserializer.put(Aesthetics.effectID, Aesthetics::deserialize);
		mEffectDeserializer.put(ArrowSaving.effectID, ArrowSaving::deserialize);
		mEffectDeserializer.put(AstralOmenBonusDamage.effectID, AstralOmenBonusDamage::deserialize);
		mEffectDeserializer.put(AstralOmenArcaneStacks.effectID, AstralOmenArcaneStacks::deserialize);
		mEffectDeserializer.put(AstralOmenFireStacks.effectID, AstralOmenFireStacks::deserialize);
		mEffectDeserializer.put(AstralOmenIceStacks.effectID, AstralOmenIceStacks::deserialize);
		mEffectDeserializer.put(AstralOmenThunderStacks.effectID, AstralOmenThunderStacks::deserialize);
		mEffectDeserializer.put(Bleed.effectID, Bleed::deserialize);
		mEffectDeserializer.put(BonusSoulThreads.effectID, BonusSoulThreads::deserialize);
		mEffectDeserializer.put(BoonOfKnightlyPrayer.effectID, BoonOfKnightlyPrayer::deserialize);
		mEffectDeserializer.put(BoonOfThePit.effectID, BoonOfThePit::deserialize);
		mEffectDeserializer.put(CourageEffect.effectID, CourageEffect::deserialize);
		mEffectDeserializer.put(CrystalineBlessing.effectID, CrystalineBlessing::deserialize);
		mEffectDeserializer.put(CustomAbsorption.effectID, CustomAbsorption::deserialize);
		mEffectDeserializer.put(CustomDamageOverTime.effectID, CustomDamageOverTime::deserialize);
		mEffectDeserializer.put(CustomRegeneration.effectID, CustomRegeneration::deserialize);
		mEffectDeserializer.put(DeepGodsEndowment.effectID, DeepGodsEndowment::deserialize);
		mEffectDeserializer.put(DurabilitySaving.effectID, DurabilitySaving::deserialize);
		mEffectDeserializer.put(EnchantedPrayerAoE.effectID, EnchantedPrayerAoE::deserialize);
		mEffectDeserializer.put(FirstStrikeCooldown.effectID, FirstStrikeCooldown::deserialize);
		mEffectDeserializer.put(FlatDamageDealt.effectID, FlatDamageDealt::deserialize);
		mEffectDeserializer.put(HealingSickness.effectID, HealingSickness::deserialize);
		mEffectDeserializer.put(HealPlayerOnDeath.effectID, HealPlayerOnDeath::deserialize);
		mEffectDeserializer.put(InfernoDamage.effectID, InfernoDamage::deserialize);
		mEffectDeserializer.put(ItemCooldown.effectID, ItemCooldown::deserialize);
		mEffectDeserializer.put(JudgementChainMobEffect.effectID, JudgementChainMobEffect::deserialize);
		mEffectDeserializer.put(NegateDamage.effectID, NegateDamage::deserialize);
		mEffectDeserializer.put(OnHitTimerEffect.effectID, OnHitTimerEffect::deserialize);
		mEffectDeserializer.put(Paralyze.effectID, Paralyze::deserialize);
		mEffectDeserializer.put(PercentAbilityDamageReceived.effectID, PercentAbilityDamageReceived::deserialize);
		mEffectDeserializer.put(PercentAttackSpeed.effectID, PercentAttackSpeed::deserialize);
		mEffectDeserializer.put(PercentDamageDealt.effectID, PercentDamageDealt::deserialize);
		mEffectDeserializer.put(PercentDamageDealtSingle.effectID, PercentDamageDealtSingle::deserialize);
		mEffectDeserializer.put(PercentDamageReceived.effectID, PercentDamageReceived::deserialize);
		mEffectDeserializer.put(PercentExperience.effectID, PercentExperience::deserialize);
		mEffectDeserializer.put(PercentHeal.effectID, PercentHeal::deserialize);
		mEffectDeserializer.put(PercentHealthBoost.effectID, PercentHealthBoost::deserialize);
		mEffectDeserializer.put(PercentKnockbackResist.effectID, PercentKnockbackResist::deserialize);
		mEffectDeserializer.put(PercentSpeed.effectID, PercentSpeed::deserialize);
		mEffectDeserializer.put(RecoilDisable.effectID, RecoilDisable::deserialize);
		mEffectDeserializer.put(RiptideDisable.effectID, RiptideDisable::deserialize);
		mEffectDeserializer.put(SanctifiedArmorHeal.effectID, SanctifiedArmorHeal::deserialize);
		mEffectDeserializer.put(SanguineHarvestBlight.effectID, SanguineHarvestBlight::deserialize);
		mEffectDeserializer.put(SanguineMark.effectID, SanguineMark::deserialize);
		mEffectDeserializer.put(ScorchedEarthDamage.effectID, ScorchedEarthDamage::deserialize);
		mEffectDeserializer.put(SilverPrayer.effectID, SilverPrayer::deserialize);
		mEffectDeserializer.put(SpellShockExplosion.effectID, SpellShockExplosion::deserialize);
		mEffectDeserializer.put(SpellShockStatic.effectID, SpellShockStatic::deserialize);
		mEffectDeserializer.put(SplitArrowIframesEffect.effectID, SplitArrowIframesEffect::deserialize);
		mEffectDeserializer.put(SpreadEffectOnDeath.effectID, SpreadEffectOnDeath::deserialize);
		mEffectDeserializer.put(StarCommunion.effectID, StarCommunion::deserialize);
		mEffectDeserializer.put(Stasis.effectID, Stasis::deserialize);
		mEffectDeserializer.put(ThuribleBonusHealing.effectID, ThuribleBonusHealing::deserialize);
		mEffectDeserializer.put(TuathanBlessing.effectID, TuathanBlessing::deserialize);
		mEffectDeserializer.put(VengefulTag.effectID, VengefulTag::deserialize);
		mEffectDeserializer.put(VoodooBondsOtherPlayer.effectID, VoodooBondsOtherPlayer::deserialize);
		mEffectDeserializer.put(VoodooBondsReaper.effectID, VoodooBondsReaper::deserialize);
		mEffectDeserializer.put(WarmthEffect.effectID, WarmthEffect::deserialize);
		mEffectDeserializer.put(WindBombAirTag.effectID, WindBombAirTag::deserialize);
	}

	private static final int PERIOD = 5;

	private final HashMap<Entity, Effects> mEntities = new HashMap<>();
	private final BukkitRunnable mTimer;
	private static @Nullable
	EffectManager INSTANCE = null;

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
					for (Map.Entry<Entity, Effects> entry : ((HashMap<Entity, Effects>) mEntities.clone()).entrySet()) {
						for (Map<String, NavigableSet<Effect>> priorityEffects : entry.getValue().mPriorityMap.values()) {
							// Have to make a copy of the effects to prevent concurrent modification exceptions in case ticking changes the effects :(
							for (NavigableSet<Effect> effectGroup : new ArrayList<>(priorityEffects.values())) {
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

						for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
							Iterator<NavigableSet<Effect>> priorityEffectsIter = priorityEffects.values().iterator();
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

	public static class SortEffectsByDuration implements Comparator<Effect> {
		@Override
		public int compare(Effect effect1, Effect effect2) {
			return effect2.getDuration() - effect1.getDuration();
		}
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
		// The event may be cancelled, and all 3 parameters may be modified
		CustomEffectApplyEvent event = new CustomEffectApplyEvent(entity, effect, source);
		if (event.callEvent()) {
			entity = event.getEntity();
			source = event.getSource();
			effect = event.getEffect();

			Effects effects = mEntities.get(entity);
			if (effects == null) {
				effects = new Effects(entity);
				mEntities.put(entity, effects);
			}

			effects.addEffect(source, effect);
		}
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
	public <T extends Effect> NavigableSet<T> getEffects(Entity entity, Class<T> type) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.getEffects(type);
		}
		return Collections.emptyNavigableSet();
	}

	public @Nullable List<Effect> getEffects(Entity entity) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.getEffects();
		} else {
			return null;
		}
	}

	// Gets ALL Effects of entity, including hidden non-active ones.
	// I.E. The effects with lesser magnitude.
	public @Nullable List<Effect> getAllEffects(Entity entity) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.getAllEffects();
		} else {
			return null;
		}
	}

	public @Nullable Effect getActiveEffect(Entity entity, String source) {
		NavigableSet<Effect> effects = getEffects(entity, source);
		if (effects != null) {
			return effects.last();
		}

		return null;
	}

	/**
	 * Returns if an entity has an effect source or not.
	 *
	 * @param entity the entity being checked
	 * @param source the source of effects to be checked
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
	 * @param entity the entity being checked
	 * @param type   the class of effect
	 * @return whether the entity has the effect or not
	 */
	public boolean hasEffect(Entity entity, Class<? extends Effect> type) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.hasEffect(type);
		}
		return false;
	}

	public HashMap<String, Effect> getPriorityEffects(Entity entity) {
		EffectManager.Effects effects = mEntities.get(entity);
		HashMap<String, Effect> output = new HashMap<>();
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (Map.Entry<String, NavigableSet<Effect>> entry : priorityEffects.entrySet()) {
					Effect effect = entry.getValue().pollLast();
					if (effect != null) {
						output.put(entry.getKey(), effect);
					}
				}
			}
		}
		return output;
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

	public @Nullable String getSource(Entity entity, Effect effect) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.getSource(effect);
		}
		return null;
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

	public static @Nullable Effect getEffectFromJson(JsonObject object, Plugin plugin) throws Exception {
		String effectID = object.get("effectID").getAsString();
		return mEffectDeserializer.get(effectID).deserialize(object, plugin);
	}

	public void loadFromJsonObject(Player player, JsonObject object, Plugin plugin) throws Exception {
		clearEffects(player);

		String[] keys = {EffectPriority.EARLY.name(), EffectPriority.NORMAL.name(), EffectPriority.LATE.name()};

		for (String priority : keys) {
			if (object.get(priority) == null) {
				continue;
			}
			JsonObject priorityEffect = object.get(priority).getAsJsonObject();

			JsonArray sourceList = priorityEffect.get("source").getAsJsonArray();
			for (JsonElement sourceElement : sourceList) {
				String source = sourceElement.getAsString();

				// Skip re-application if effect is from the Patron Shrine
				if (source.startsWith("PatronShrine")) {
					continue;
				}

				JsonArray innerArray = priorityEffect.get(source).getAsJsonArray();
				for (JsonElement effectJson : innerArray) {
					JsonObject effectObject = effectJson.getAsJsonObject();
					Effect effect = getEffectFromJson(effectObject, plugin);
					if (effect != null) {
						addEffect(player, source, effect);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void entityRegainHealthEvent(EntityRegainHealthEvent event) {
		Effects effects = mEntities.get(event.getEntity());
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					if (!effectGroup.last().entityRegainHealthEvent(event)) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		LivingEntity killed = event.getEntity();
		Effects killedEffects = mEntities.get(killed);
		if (killedEffects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : killedEffects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					effectGroup.last().onDeath(event);
				}
			}
		}

		Player killer = killed.getKiller();
		if (killer != null) {
			Effects killerEffects = mEntities.get(killer);
			if (killerEffects != null) {
				for (Map<String, NavigableSet<Effect>> priorityEffects : killerEffects.mPriorityMap.values()) {
					for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
						effectGroup.last().onKill(event);
					}
				}
			}
		}
	}

	//Called in DamageListener
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

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void expChangeEvent(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();
		Effects effects = mEntities.get(player);
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					effectGroup.last().onExpChange(player, event);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		if (event.getEntity() instanceof AbstractArrow arrow && arrow.getShooter() instanceof Player player) {
			Effects effects = mEntities.get(player);
			if (effects != null) {
				for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
					for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
						effectGroup.last().onProjectileLaunch(player, arrow);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		Player player = event.getPlayer();
		Effects effects = mEntities.get(player);
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					effectGroup.last().onDurabilityDamage(player, event);
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
