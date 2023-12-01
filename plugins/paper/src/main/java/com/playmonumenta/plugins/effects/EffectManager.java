package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.ArrowConsumeEvent;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.EntityGainAbsorptionEvent;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.infusions.Phylactery;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.MMLog;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
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
import org.jetbrains.annotations.Nullable;

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
			if (effect.mUsed) {
				// Each entity must have their own instance of an effect, they cannot be shared
				MMLog.severe("Attempted to add an effect multiple times or to multiple entities! source="
					             + source + ", effectID=" + effect.mEffectID + ", entity=" + mEntity, new IllegalArgumentException());
				return;
			}
			effect.mUsed = true;

			Map<String, NavigableSet<Effect>> priorityEffects = Objects.requireNonNull(mPriorityMap.get(effect.getPriority()));
			NavigableSet<Effect> effectGroup = priorityEffects.computeIfAbsent(source, k -> new ConcurrentSkipListSet<>());

			if (!effectGroup.isEmpty()) {
				Effect currentEffect = effectGroup.last();

				// Iterate through effectGroup to check if there is already an effect with existing magnitude and less duration.
				boolean foundEffect = false;
				for (Effect effectIter : effectGroup) {
					if (effectIter.compareTo(effect) == 0 && effectIter.getDuration() < effect.getDuration()) {
						if (effectIter == currentEffect) {
							effectIter.entityLoseEffect(mEntity);
							effectIter.entityGainEffect(mEntity);
						}
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
		public List<EffectPair> getAllEffectPairs() {
			List<EffectPair> effects = new ArrayList<>();
			for (Map<String, NavigableSet<Effect>> priorityEffects : mPriorityMap.values()) {
				for (Map.Entry<String, NavigableSet<Effect>> entry : priorityEffects.entrySet()) {
					String source = entry.getKey();
					entry.getValue().forEach(effect -> effects.add(new EffectPair(source, effect)));
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
			for (Map<String, NavigableSet<Effect>> sourceMap : mPriorityMap.values()) {
				for (Map.Entry<String, NavigableSet<Effect>> e : sourceMap.entrySet()) {
					// manual loop instead of contains() to check for reference equality
					for (Effect otherEffect : e.getValue()) {
						if (effect == otherEffect) {
							return e.getKey();
						}
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
						JsonObject serializedEffect = effect.serialize();
						serializedEffect.addProperty("displaysTime", effect.doesDisplayTime());
						serializedEffect.addProperty("displays", effect.doesDisplay());
						serializedEffect.addProperty("deleteOnLogout", effect.shouldDeleteOnLogout());
						serializedEffect.addProperty("heavenlyBoonExtensions", effect.mHeavenlyBoonExtensions);
						if (serializedEffect.has("effectID")) {
							inner.add(serializedEffect);
						} else {
							MMLog.severe("Effect " + effect.getClass().getSimpleName() + " did not serialize its effectID!");
						}
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

	// Used for phylactery
	public static class EffectPair {
		public final String mSource;
		public final Effect mEffect;

		public EffectPair(String source, Effect effect) {
			mSource = source;
			mEffect = effect;
		}
	}

	@FunctionalInterface
	public interface EffectDeserializer {
		@Nullable Effect deserialize(JsonObject object, Plugin plugin) throws Exception;
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
		mEffectDeserializer.put(CrusadeTag.effectID, CrusadeTag::deserialize);
		mEffectDeserializer.put(CrystalineBlessing.effectID, CrystalineBlessing::deserialize);
		mEffectDeserializer.put(CustomDamageOverTime.effectID, CustomDamageOverTime::deserialize);
		mEffectDeserializer.put(CustomRegeneration.effectID, CustomRegeneration::deserialize);
		mEffectDeserializer.put(DeepGodsEndowment.effectID, DeepGodsEndowment::deserialize);
		mEffectDeserializer.put(DurabilitySaving.effectID, DurabilitySaving::deserialize);
		mEffectDeserializer.put(EnchantedPrayerAoE.effectID, EnchantedPrayerAoE::deserialize);
		mEffectDeserializer.put(EnergizingElixirStacks.effectID, EnergizingElixirStacks::deserialize);
		mEffectDeserializer.put(FirstStrikeCooldown.effectID, FirstStrikeCooldown::deserialize);
		mEffectDeserializer.put(FlatDamageDealt.effectID, FlatDamageDealt::deserialize);
		mEffectDeserializer.put(HealingSickness.effectID, HealingSickness::deserialize);
		mEffectDeserializer.put(HealPlayerOnDeath.effectID, HealPlayerOnDeath::deserialize);
		mEffectDeserializer.put(InfernoDamage.effectID, InfernoDamage::deserialize);
		mEffectDeserializer.put(ItemCooldown.effectID, ItemCooldown::deserialize);
		mEffectDeserializer.put(JudgementChainMobEffect.effectID, JudgementChainMobEffect::deserialize);
		mEffectDeserializer.put(JudgementChainPlayerEffect.effectID, JudgementChainPlayerEffect::deserialize);
		mEffectDeserializer.put(LichCurseEffect.effectId, LichCurseEffect::deserialize);
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
		mEffectDeserializer.put(BaseMovementSpeedModifyEffect.effectID, BaseMovementSpeedModifyEffect::deserialize);
		mEffectDeserializer.put(RecoilDisable.effectID, RecoilDisable::deserialize);
		mEffectDeserializer.put(BroomstickSlowFalling.effectID, BroomstickSlowFalling::deserialize);
		mEffectDeserializer.put(SanctifiedArmorHeal.effectID, SanctifiedArmorHeal::deserialize);
		mEffectDeserializer.put(SanguineHarvestBlight.effectID, SanguineHarvestBlight::deserialize);
		mEffectDeserializer.put(SanguineMark.effectID, SanguineMark::deserialize);
		mEffectDeserializer.put(ScorchedEarthDamage.effectID, ScorchedEarthDamage::deserialize);
		mEffectDeserializer.put(ShamanCooldownDecreasePerSecond.effectID, ShamanCooldownDecreasePerSecond::deserialize);
		mEffectDeserializer.put(GearChanged.effectID, GearChanged::deserialize);
		mEffectDeserializer.put(SilverPrayer.effectID, SilverPrayer::deserialize);
		mEffectDeserializer.put(SpellShockStatic.effectID, SpellShockStatic::deserialize);
		mEffectDeserializer.put(SplitArrowIframesEffect.effectID, SplitArrowIframesEffect::deserialize);
		mEffectDeserializer.put(SpreadEffectOnDeath.effectID, SpreadEffectOnDeath::deserialize);
		mEffectDeserializer.put(StarCommunion.effectID, StarCommunion::deserialize);
		mEffectDeserializer.put(Stasis.effectID, Stasis::deserialize);
		mEffectDeserializer.put(RespawnStasis.effectID, RespawnStasis::deserialize);
		mEffectDeserializer.put(ThuribleBonusHealing.effectID, ThuribleBonusHealing::deserialize);
		mEffectDeserializer.put(TuathanBlessing.effectID, TuathanBlessing::deserialize);
		mEffectDeserializer.put(UnstableAmalgamDisable.effectID, UnstableAmalgamDisable::deserialize);
		mEffectDeserializer.put(VengefulTag.effectID, VengefulTag::deserialize);
		mEffectDeserializer.put(VoodooBondsOtherPlayer.effectID, VoodooBondsOtherPlayer::deserialize);
		mEffectDeserializer.put(VoodooBondsReaper.effectID, VoodooBondsReaper::deserialize);
		mEffectDeserializer.put(WarmthEffect.effectID, WarmthEffect::deserialize);
		mEffectDeserializer.put(ColoredGlowingEffect.effectID, ColoredGlowingEffect::deserialize);
		mEffectDeserializer.put(FishQualityIncrease.effectID, FishQualityIncrease::deserialize);
		mEffectDeserializer.put(GiftOfTheStars.effectID, GiftOfTheStars::deserialize);
		mEffectDeserializer.put(BoonOfTheFracturedTree.effectID, BoonOfTheFracturedTree::deserialize);
		mEffectDeserializer.put(SkySeekersTag.effectID, SkySeekersTag::deserialize);
		mEffectDeserializer.put(SkySeekersGrace.effectID, SkySeekersGrace::deserialize);
		mEffectDeserializer.put(FractalVuln.effectID, FractalVuln::deserialize);
		mEffectDeserializer.put(FractalCooldown.effectID, FractalCooldown::deserialize);
		mEffectDeserializer.put(IchorCooldown.effectID, IchorCooldown::deserialize);
		mEffectDeserializer.put(IchorEarthEffect.effectID, IchorEarthEffect::deserialize);
		mEffectDeserializer.put(IchorSteelEffect.effectID, IchorSteelEffect::deserialize);
	}

	private static final int PERIOD = 5;

	private final HashMap<Entity, Effects> mEntities = new HashMap<>();
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
					for (Map.Entry<Entity, Effects> entry : ((HashMap<Entity, Effects>) mEntities.clone()).entrySet()) {
						for (Map<String, NavigableSet<Effect>> priorityEffects : entry.getValue().mPriorityMap.values()) {
							// Have to make a copy of the effects to prevent concurrent modification exceptions in case ticking changes the effects :(
							for (NavigableSet<Effect> effectGroup : new ArrayList<>(priorityEffects.values())) {
								try {
									// Only tick when entity is not dead.
									if (!entry.getKey().isDead()) {
										effectGroup.last().entityTickEffect(entry.getKey(), fourHertz, twoHertz, oneHertz);
									}
								} catch (Exception ex) {
									MMLog.severe("Error in effect manager entityTickEffect: " + ex.getMessage());
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
						if (entity instanceof Player player) {
							// Remove effects from players who are no longer logged in here - those effects will be re-added when they return
							if (!player.isOnline()) {
								entityIter.remove();
								continue;
							}
						} else if (entity.isDead() || !entity.isValid()) {
							entityIter.remove();
							continue;
						}

						for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
							Iterator<Map.Entry<String, NavigableSet<Effect>>> priorityEffectsIter = priorityEffects.entrySet().iterator();
							while (priorityEffectsIter.hasNext()) {
								Map.Entry<String, NavigableSet<Effect>> effectGroup = priorityEffectsIter.next();
								if (effectGroup.getValue().isEmpty()) {
									priorityEffectsIter.remove();
									ClientModHandler.updateEffects(entity);
									continue;
								}

								boolean currentEffectRemoved = false;
								Effect currentEffect = effectGroup.getValue().last();
								Iterator<Effect> effectGroupIter = effectGroup.getValue().descendingIterator();
								while (effectGroupIter.hasNext()) {
									Effect effect = effectGroupIter.next();

									if (currentEffectRemoved) {
										try {
											effect.entityGainEffect(entity);
											ClientModHandler.updateEffect(entity, effect, effectGroup.getKey(), false);
										} catch (Exception ex) {
											MMLog.severe("Error in effect manager entityGainEffect: " + ex.getMessage());
											ex.printStackTrace();
										}
										currentEffectRemoved = false;
									}

									boolean tickResult;
									try {
										tickResult = effect.tick(PERIOD);
									} catch (Exception ex) {
										MMLog.severe("Error in effect manager tick: " + ex.getMessage());
										ex.printStackTrace();
										/* If ticking throws an exception (e.g. NPE) remove it */
										tickResult = false;
									}

									if (tickResult) {
										if (effect == currentEffect) {
											try {
												effect.entityLoseEffect(entity);
												ClientModHandler.updateEffect(entity, effect, effectGroup.getKey(), true);
											} catch (Exception ex) {
												MMLog.severe("Error in effect manager entityLoseEffect: " + ex.getMessage());
												ex.printStackTrace();
											}
											currentEffectRemoved = true;
										}

										effectGroupIter.remove();

										if (!effectGroup.getValue().isEmpty()) {
											currentEffect = effectGroup.getValue().last();
										} else {
											priorityEffectsIter.remove();
										}
									}
								}
							}
						}
					}
				} catch (Exception ex) {
					MMLog.severe("Error in effect manager ticking task that caused many pieces to be skipped: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		};

		mTimer.runTaskTimer(plugin, 0, PERIOD);
	}

	public static EffectManager getInstance() {
		return Objects.requireNonNull(INSTANCE);
	}

	/**
	 * Applies an effect to an entity.
	 * <p>
	 * You MUST assign different "sources" to different effect types. The source should only be used to track level and duration overrides of the same effect.
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

			Effects effects = mEntities.computeIfAbsent(entity, Effects::new);
			effects.addEffect(source, effect);
			ClientModHandler.updateEffect(entity, effect, source, false);
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
	public @Nullable List<EffectPair> getAllEffectPairs(Entity entity) {
		Effects effects = mEntities.get(entity);
		if (effects != null) {
			return effects.getAllEffectPairs();
		} else {
			return null;
		}
	}

	public @Nullable List<Effect> getAllEffects(Entity entity) {
		List<EffectPair> effectPairs = getAllEffectPairs(entity);
		if (effectPairs != null) {
			List<Effect> effects = new ArrayList<>();
			effectPairs.forEach(pair -> effects.add(pair.mEffect));
			return effects;
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

	public <T extends Effect> @Nullable T getActiveEffect(Entity entity, Class<T> cls) {
		NavigableSet<T> effects = getEffects(entity, cls);
		if (effects != null && effects.size() > 0) {
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
					try {
						Effect effect = entry.getValue().last();
						if (effect != null) {
							output.put(entry.getKey(), effect);
						}
					} catch (NoSuchElementException e) {
						// ignore - effect was probably removed in another thread (and this method can be called by the tab list from arbitrary threads)
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
			NavigableSet<Effect> removedEffects = effects.clearEffects(source);
			if (entity instanceof Player player
				&& removedEffects != null) {
				for (Effect effect : removedEffects) {
					ClientModHandler.updateEffect(player, effect, source, true);
				}
			}
			return removedEffects;
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
			if (entity instanceof Player player) {
				ClientModHandler.updateEffects(player);
			}
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
		JsonElement effectIDElement = object.get("effectID");
		if (effectIDElement == null) {
			MMLog.warning("Found null effectID - how? Effect Json: " + object);
			return null;
		}
		String effectID = effectIDElement.getAsString();
		EffectDeserializer deserializer = mEffectDeserializer.get(effectID);
		if (deserializer == null) {
			MMLog.severe("Cannot deserialize effect with ID '" + effectID + "'");
			return null;
		}

		Effect deserializedEffect = deserializer.deserialize(object, plugin);
		if (deserializedEffect != null && object.has("displaysTime")) {
			deserializedEffect.displaysTime(object.get("displaysTime").getAsBoolean());
		}
		if (deserializedEffect != null && object.has("displays")) {
			deserializedEffect.displays(object.get("displays").getAsBoolean());
		}
		if (deserializedEffect != null && object.has("deleteOnLogout")) {
			boolean delete = object.get("deleteOnLogout").getAsBoolean();
			deserializedEffect.deleteOnLogout(delete);
			if (delete) {
				Bukkit.getScheduler().runTaskLater(plugin, deserializedEffect::clearEffect, 5);
			}
		}
		if (deserializedEffect != null && object.has("heavenlyBoonExtensions")) {
			deserializedEffect.mHeavenlyBoonExtensions = object.get("heavenlyBoonExtensions").getAsInt();
		}

		return deserializedEffect;
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
	public void entityGainAbsorptionEvent(EntityGainAbsorptionEvent event) {
		Effects effects = mEntities.get(event.getEntity());
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					effectGroup.last().entityGainAbsorptionEvent(event);
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void customEffectAppliedEvent(CustomEffectApplyEvent event) {
		Effects effects = mEntities.get(event.getEntity());
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					effectGroup.last().customEffectAppliedEvent(event);
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
						effectGroup.last().onKill(event, killer);
					}
				}
			}
		}

		if (killed instanceof Player player) {
			// Default phylactery level is 10 (should already be accounted for).
			double phylactery = Plugin.getInstance().mItemStatManager.getInfusionLevel(player, InfusionType.PHYLACTERY);

			// Set durations of Custom Effects for all player effects (including hidden ones)
			List<EffectManager.EffectPair> effectPairs = getAllEffectPairs(player);

			if (effectPairs != null) {
				for (EffectManager.EffectPair pair : effectPairs) {
					Effect effect = pair.mEffect;
					String source = pair.mSource;

					if (source.startsWith("DeathPersistent")) {
						// Don't alter duration for these effects.
						continue;
					} else if (effect.isBuff() && !source.startsWith("PatronShrine")) {
						// Effect is Buff, set duration based on Phylactery value.
						effect.setDuration((int) (effect.getDuration() * phylactery * Phylactery.DURATION_KEPT));
						effect.entityLoseEffect(player);
						effect.entityUpdateEffect(player);
					} else {
						// Effect is Debuff (or not stated), set duration to 0.
						effect.setDuration(0);
						effect.entityUpdateEffect(player);
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
	public void arrowConsumeEvent(ArrowConsumeEvent event) {
		Effects effects = mEntities.get(event.getPlayer());
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					effectGroup.last().onConsumeArrow(event.getPlayer(), event);
					if (event.isCancelled()) {
						return;
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

	public double getFishQualityIncrease(Player player) {
		double chance = 1;
		Effects effects = mEntities.get(player);
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					chance *= effectGroup.last().getFishQualityIncrease(player);
				}
			}
		}
		return chance;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Effects effects = mEntities.get(player);
		if (effects != null) {
			for (Map<String, NavigableSet<Effect>> priorityEffects : effects.mPriorityMap.values()) {
				for (NavigableSet<Effect> effectGroup : priorityEffects.values()) {
					Effect effect = effectGroup.last();
					if (effect.shouldDeleteOnLogout()) {
						effect.entityLoseEffect(player);
					}
				}
			}
		}

		// 1s after the player leaves, remove them from the map to avoid leaking memory
		// If the player happens to log back in immediately, still remove them - they will get a new entity object which will be tracked again
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			// NOTE: If the Entity map is ever changed to key'd by Entity UUID, this will need a guard to check that the player didn't log back in
			mEntities.remove(player);
		}, 20);
	}

	public void applyEffectsOnRespawn(Plugin plugin, Player player) {
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			List<Effect> activeEffects = getAllEffects(player);
			if (activeEffects != null) {
				for (Effect effect : activeEffects) {
					String source = getSource(player, effect);
					// Recall Effect Gain Function to regain buffs one tick later.
					effect.entityLoseEffect(player);
					ClientModHandler.updateEffect(player, effect, source, true);
					if (source != null) {
						Bukkit.getScheduler().runTaskLater(plugin, () -> {
							NavigableSet<Effect> effectsInSource = getEffects(player, source);
							// Ensure that:
							// Effect is still top priority
							// Effect duration has not run out.
							// Only then do we re-apply the gain effect (things like speed attribute)
							// Really, it's more insurance to ensure we don't have another bug in our hands due to all the delay stuff.
							if (effectsInSource != null && effectsInSource.last() == effect && effectsInSource.last().getDuration() == effect.getDuration() && effect.getDuration() > 0) {
								effect.entityGainEffect(player);
								ClientModHandler.updateEffect(player, effect, source, false);
							}
						}, 1);
					}
				}
			}
		}, 1);
	}
}
