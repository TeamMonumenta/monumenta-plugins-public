package com.playmonumenta.plugins.bosses.parameters;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.parameters.phases.Action;
import com.playmonumenta.plugins.bosses.parameters.phases.AddAbilityAction;
import com.playmonumenta.plugins.bosses.parameters.phases.BossCastTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.CommandAction;
import com.playmonumenta.plugins.bosses.parameters.phases.CustomTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.CustomTriggerAction;
import com.playmonumenta.plugins.bosses.parameters.phases.DelayAction;
import com.playmonumenta.plugins.bosses.parameters.phases.FlagSetAction;
import com.playmonumenta.plugins.bosses.parameters.phases.FlagTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.ForceCastAction;
import com.playmonumenta.plugins.bosses.parameters.phases.HealthTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.NearbyPlayersTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.OnDamageTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.OnDeathTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.OnHurtTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.OnShootTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.OnSpawnTrigger;
import com.playmonumenta.plugins.bosses.parameters.phases.Phase;
import com.playmonumenta.plugins.bosses.parameters.phases.RandomAction;
import com.playmonumenta.plugins.bosses.parameters.phases.RemoveAbilityAction;
import com.playmonumenta.plugins.bosses.parameters.phases.Trigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.commands.BossTagCommand;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MMLog;
import dev.jorel.commandapi.Tooltip;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Parser {
	@FunctionalInterface
	public interface ParserMethod<T> {
		Result<? extends T> parse(Tokens tokens) throws ParseError;
	}

	// Only for the object constructor!
	@FunctionalInterface
	public interface ParameterParserMethod<T> {
		Result<? extends T> parse(Tokens tokens, ObjectConstructor.ParameterMap parameters) throws ParseError;

		static <T> ParameterParserMethod<T> of(ParserMethod<T> parserMethod) {
			return (tokens, parameters) -> parserMethod.parse(tokens);
		}
	}

	@FunctionalInterface
	public interface ClassPredictor<T> {
		Class<? extends T> predict(Tokens tokens) throws ParseError;
	}

	private static Type[] getTypeArguments(Type fieldGenericType) {
		Preconditions.checkArgument(fieldGenericType instanceof ParameterizedType);
		ParameterizedType parameterizedType = (ParameterizedType) fieldGenericType;
		return parameterizedType.getActualTypeArguments();
	}

	private static final Map<Class<?>, ParserMethod<?>> PARSER_MAP = ImmutableMap.<Class<?>, ParserMethod<?>>builder()
		.put(int.class, Parser::parseInteger)
		.put(long.class, Parser::parseLong)
		.put(float.class, Parser::parseFloat)
		.put(double.class, Parser::parseDouble)
		.put(boolean.class, Parser::parseBoolean)
		.put(PotionEffectType.class, tokens -> parseRegistryKey(tokens, Registry.POTION_EFFECT_TYPE, "Effect Type"))
		.put(Color.class, Parser::parseColor)
		.put(String.class, Parser::parseString)
		.put(LoSPool.class, Parser::parseLosPool)
		.put(ParticlesList.class, tokens -> Result.of(new ParticlesList(parseList(tokens, t -> parseObject(t, ParticlesList.CParticle.class, false)).data)))
		.put(SoundsList.class, tokens -> Result.of(new SoundsList(parseList(tokens, t -> parseObject(t, SoundsList.CSound.class, false)).data)))
		.put(EffectsList.class, tokens -> Result.of(new EffectsList(parseList(tokens, t -> parsePredicatedObject(t, t1 -> {
			String value = t1.peek().getValue();
			if (EffectsList.Effect.EFFECT_RUNNER.containsKey(value)) {
				return EffectsList.CustomEffect.class;
			} else if (EffectsList.Effect.CUSTOM_EFFECT_RUNNER.containsKey(value)) {
				return EffectsList.CustomSingleArgumentEffect.class;
			} else if (getRegistrySuggestions(Registry.POTION_EFFECT_TYPE).contains(value.toLowerCase(Locale.ROOT))) {
				return EffectsList.VanillaEffect.class;
			}
			throw new ParseError(String.format("Invalid Effect Type: %s", value), t1.getIndex() + 1, t1)
				.suggests(EffectsList.Effect.EFFECT_IDENTIFIERS, "Effect Identifiers");
		}, false)).data)))
		.put(EntityTargets.class, tokens -> parseObject(tokens, EntityTargets.class, true))
		.put(BossPhasesList.class, tokens -> Result.of(new BossPhasesList(parseList(tokens, t -> parseObject(t, Phase.class, false)).data)))
		.build();

	@SuppressWarnings("unchecked")
	public static <T, E extends Enum<E>> ParserMethod<T> getParserMethod(Class<T> unknownType) throws IllegalArgumentException {
		if (PARSER_MAP.containsKey(unknownType)) {
			return (ParserMethod<T>) PARSER_MAP.get(unknownType);
		} else if (unknownType.isEnum()) {
			Class<E> enumClass = (Class<E>) unknownType;

			return tokens -> (Result<? extends T>) parseEnum(tokens, enumClass);
		}
		throw new IllegalArgumentException("No parser found!");
	}

	// API
	public static <T> T parseOrDefault(ParserMethod<T> parserMethod, String string, T defaultValue) {
		try {
			return parserMethod.parse(new Tokenizer(string).getTokens()).data;
		} catch (ParseError e) {
			MMLog.severe(String.format("Failed to parse '%s' as %s", string, defaultValue.getClass().getSimpleName()));
			MMLog.severe("Exception while parsing: " + e.getMessage());
			MMLog.severe(e.rawErrorHighlighting());

			Thread.dumpStack();

			return defaultValue;
		}
	}

	// maps a class of constants to a map of constant names (as declared field names) to their corresponding values
	private static final LoadingCache<Class<?>, ImmutableMap<String, ?>> CACHE_CONSTANTS = CacheBuilder.newBuilder()
		.softValues()
		.build(CacheLoader.from(clazz -> Arrays.stream(clazz.getDeclaredFields())
			.filter(field ->
				(field.getModifiers() & Modifier.STATIC) == Modifier.STATIC
					&& (field.getModifiers() & Modifier.FINAL) == Modifier.FINAL
					&& field.canAccess(null))
			.filter(field -> field.getType().equals(clazz))
			.collect(ImmutableMap::<String, Object>builder, (builder, field) -> {
				try {
					builder.put(field.getName(), field.get(null));
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}, (builder, builder2) -> builder.putAll(builder2.build()))
			.build()));

	private static final LoadingCache<Class<? extends Enum<?>>, ImmutableMap<String, Enum<?>>> CACHE_ENUM = CacheBuilder.newBuilder()
		.softValues()
		.build(CacheLoader.from(clazz -> Arrays.stream(clazz.getEnumConstants())
			.collect(ImmutableMap::<String, Enum<?>>builder,
				(builder, anEnum) -> builder.put(anEnum.name(), anEnum),
				(builder, builder2) -> builder.putAll(builder2.build()))
			.build()));

	private static final LoadingCache<Registry<?>, ImmutableList<String>> CACHE_REGISTRY = CacheBuilder.newBuilder()
		.softValues()
		.build(CacheLoader.from(registry -> registry.stream()
			.collect(ImmutableList::<String>builder,
				(builder, k) -> builder.add(k.getKey().getKey()),
				(builder, builder2) -> builder.addAll(builder2.build()))
			.build()));

	private static final ImmutableMap<Class<?>, Class<?>> UNWRAPPED_CLASS = ImmutableMap.<Class<?>, Class<?>>builder()
		.put(Integer.class, int.class)
		.put(Long.class, long.class)
		.put(Short.class, short.class)
		.put(Double.class, double.class)
		.put(Float.class, float.class)
		.put(Character.class, char.class)
		.put(Byte.class, byte.class)
		.put(Boolean.class, boolean.class)
		.build();


	private static Class<?> unwrap(Class<?> c) {
		return Optional.ofNullable(UNWRAPPED_CLASS.get(c)).orElseThrow();
	}

	private static final ImmutableMap<String, ParserMethod<Trigger>> PHASE_TRIGGERS = ImmutableMap.<String, ParserMethod<Trigger>>builder()
		.put(OnSpawnTrigger.IDENTIFIER, tokens -> Result.of(new OnSpawnTrigger()))
		.put(OnDeathTrigger.IDENTIFIER, tokens -> Result.of(new OnDeathTrigger()))
		.put(OnDamageTrigger.IDENTIFIER, tokens -> parseObject(tokens, OnDamageTrigger.class, false))
		.put(OnHurtTrigger.IDENTIFIER, tokens -> parseObject(tokens, OnHurtTrigger.class, false))
		.put(BossCastTrigger.IDENTIFIER, tokens -> parseObject(tokens, BossCastTrigger.class, false))
		.put(NearbyPlayersTrigger.IDENTIFIER, tokens -> parseObject(tokens, NearbyPlayersTrigger.class, false))
		.put(HealthTrigger.IDENTIFIER, tokens -> parseObject(tokens, HealthTrigger.class, false))
		.put(CustomTrigger.IDENTIFIER, tokens -> parseObject(tokens, CustomTrigger.class, false))
		.put(FlagTrigger.IDENTIFIER, tokens -> parseObject(tokens, FlagTrigger.class, false))
		.put(OnShootTrigger.IDENTIFIER, tokens -> Result.of(new OnShootTrigger()))
		.build();


	private static final ImmutableMap<String, ParserMethod<Action>> PHASE_ACTIONS = ImmutableMap.<String, ParserMethod<Action>>builder()
		.put(AddAbilityAction.IDENTIFIER, tokens -> parseObject(tokens, AddAbilityAction.class, false))
		.put(RemoveAbilityAction.IDENTIFIER, tokens -> parseObject(tokens, RemoveAbilityAction.class, false))
		.put(ForceCastAction.IDENTIFIER, tokens -> parseObject(tokens, ForceCastAction.class, false))
		.put(CustomTriggerAction.IDENTIFIER, tokens -> parseObject(tokens, CustomTriggerAction.class, false))
		.put(DelayAction.IDENTIFIER, tokens -> parseObject(tokens, DelayAction.class, false))
		.put(CommandAction.IDENTIFIER, tokens -> parseObject(tokens, CommandAction.class, false))
		.put(RandomAction.IDENTIFIER, tokens -> parseObject(tokens, RandomAction.class, false))
		.put(FlagSetAction.IDENTIFIER, tokens -> parseObject(tokens, FlagSetAction.class, false))
		.build();

	/**
	 * <pre>
	 * Declare object constructor using a builder.
	 * put({@code Class} of Object to be parsed, {@code Supplier} of {@code Constructor Information})
	 * {@code Constructor Information}:
	 * {@code put}: An argument, defined as:
	 *    {@code name}: Name, used at factory part and as a tooltip name
	 *    {@code parser method}: Parser method, given by {@code (Tokens, List<Parameter>)}
	 *    {@code default value}: a default value, for suggestions & if parameter isn't given
	 *    {@code optional}: whether this parameter is optional
	 * {@code build}: A factory, defined as:
	 *    {@code factory}: given a Map of Strings (Name) to Objects (Value),
	 * 	with a helping method of {@code getNonNull()} to {@code Objects.requireNonNull()},
	 * 	return an object of {@code Class}
	 * </pre>
	 */
	// Will not throw because there are NO duplicate keys, yet the pmd will complain about it :p
	@SuppressWarnings({"unchecked", "AlwaysThrows"})
	private static final Map<Class<?>, Supplier<ObjectConstructor<?>>> CONSTRUCTOR_MAP = ImmutableMap.<Class<?>, Supplier<ObjectConstructor<?>>>builder()
		.put(ParticlesList.CParticle.class, () -> ObjectConstructor.builder()
			.put("particle", (tokens, parameters) -> parseEnum(tokens, Particle.class))
			.put("count", ParameterParserMethod.of(Parser::parseInteger), 1, false)
			.put("dX", ParameterParserMethod.of(Parser::parseDouble), 0.0d, false)
			.put("dY", ParameterParserMethod.of(Parser::parseDouble), 0.0d, false)
			.put("dZ", ParameterParserMethod.of(Parser::parseDouble), 0.0d, false)
			.put("extra", ParameterParserMethod.of(Parser::parseDouble), 0.0d, false)
			.put("data", (tokens, parameters) -> {
				Particle particle = (Particle) parameters.nonNullValue("particle");

				return switch (particle) {
					case REDSTONE -> parseDustOptions(tokens);
					case DUST_COLOR_TRANSITION -> parseDustTransition(tokens);
					case BLOCK_CRACK, FALLING_DUST -> parseEnum(tokens, Material.class).map(Material::createBlockData);
					case ITEM_CRACK -> parseEnum(tokens, Material.class).map(ItemStack::new);
					default ->
						throw ParseError.of(String.format("No data for Particle '%s'", particle), tokens.getIndex() + 1, tokens);
				};
			}, null, false)
			.build(parameterMap ->
				new ParticlesList.CParticle(
					(Particle) parameterMap.nonNullValue("particle"),
					(int) parameterMap.nonNullValue("count"),
					(double) parameterMap.nonNullValue("dX"),
					(double) parameterMap.nonNullValue("dY"),
					(double) parameterMap.nonNullValue("dZ"),
					(double) parameterMap.nonNullValue("extra"),
					parameterMap.nullableValue("data")
				)))
		.put(SoundsList.CSound.class, () -> ObjectConstructor.<SoundsList.CSound>builder()
			.put("sound", (tokens, parameters) -> parseEnum(tokens, Sound.class))
			.put("volume", ParameterParserMethod.of(Parser::parseFloat), 1.0f, false)
			.put("pitch", ParameterParserMethod.of(Parser::parseFloat), 1.0f, false)
			.build(parameterMap ->
				new SoundsList.CSound(
					(Sound) parameterMap.nonNullValue("sound"),
					(float) parameterMap.nonNullValue("volume"),
					(float) parameterMap.nonNullValue("pitch")
				)))
		.put(EffectsList.VanillaEffect.class, () -> ObjectConstructor.builder()
			.put("name", ParameterParserMethod.of(getParserMethod(PotionEffectType.class)))
			.put("duration", ParameterParserMethod.of(Parser::parseInteger), 20)
			.put("amplifier", ParameterParserMethod.of(Parser::parseInteger), 0, false)
			.build(parameterMap -> new EffectsList.VanillaEffect(
				(PotionEffectType) parameterMap.nonNullValue("name"),
				(int) parameterMap.nonNullValue("duration"),
				(int) parameterMap.nonNullValue("amplifier")
			)))
		.put(EffectsList.CustomEffect.class, () -> ObjectConstructor.builder()
			.put("name", (tokens, parameters) -> parseAnyOf(tokens, EffectsList.Effect.EFFECT_RUNNER.keySet(), "Custom Effect Name"))
			.put("amplifier", ParameterParserMethod.of(Parser::parseFloat), 20)
			.build(parameterMap -> new EffectsList.CustomEffect(
				(String) parameterMap.nonNullValue("name"),
				(float) parameterMap.nonNullValue("amplifier")
			)))
		.put(EffectsList.CustomSingleArgumentEffect.class, () -> ObjectConstructor.builder()
			.put("name", (tokens, parameters) -> parseAnyOf(tokens, EffectsList.Effect.CUSTOM_EFFECT_RUNNER.keySet(), "Custom Effect Name"))
			.put("duration", ParameterParserMethod.of(Parser::parseInteger), 20)
			.put("amplifier", ParameterParserMethod.of(Parser::parseFloat), 20)
			.put("source", ParameterParserMethod.of(Parser::parseString), null, false)
			.build(parameterMap -> new EffectsList.CustomSingleArgumentEffect(
				(String) parameterMap.nonNullValue("name"),
				(int) parameterMap.nonNullValue("duration"),
				(float) parameterMap.nonNullValue("amplifier"),
				(String) parameterMap.nullableValue("source")
			)))
		.put(EntityTargets.class, () -> ObjectConstructor.builder()
			.put("type", (tokens, parameters) -> parseEnum(tokens, EntityTargets.TARGETS.class))
			.put("range", ParameterParserMethod.of(Parser::parseDouble), 10.0)
			.put("limit", (tokens, parameters) -> parseObject(tokens, EntityTargets.Limit.class, false), EntityTargets.Limit.DEFAULT, false)
			.put("filters", (tokens, parameters) -> {
				EntityTargets.TARGETS targets = (EntityTargets.TARGETS) parameters.nonNullValue("type");
				switch (targets) {
					case PLAYER -> {
						return parseEnumSet(tokens, EntityTargets.PLAYERFILTER.class);
					}
					case MOB -> {
						return parseEnumSet(tokens, EntityTargets.MOBFILTER.class);
					}
					case ENTITY -> {
						return parseEnumSet(tokens, EntityTargets.ENTITYFILTERENUM.class);
					}
					case SELF -> {
						return parseCollection(tokens, t -> {
							throw ParseError.of("TARGETS 'SELF' has no filters!", tokens.getIndex() + 1, tokens).suggests(List.of("]"), "Close Object declaration");
						}, Set.of());
					}
					default -> throw ParseError.of("Invalid TARGETS!", tokens.getIndex(), tokens);
				}
			}, Set.of(), false)
			.put("tags", (tokens, parameters) -> parseCollection(tokens, Parser::parseString, new HashSet<>()), Set.of(), false)
			.build(parameters ->
				new EntityTargets(
					(EntityTargets.TARGETS) parameters.nonNullValue("type"),
					(Double) parameters.nonNullValue("range"),
					(EntityTargets.Limit) parameters.nonNullValue("limit"),
					(Collection<EntityTargets.EntityFilter>) parameters.nonNullValue("filters"),
					new EntityTargets.TagsListFiter((Set<String>) parameters.nonNullValue("tags")))))
		.put(EntityTargets.Limit.class, () -> ObjectConstructor.builder()
			.put("limit", (tokens, parameters) -> {
				if (tokens.peek().getType() == Tokens.TokenType.INTEGER) {
					return parseInteger(tokens);
				} else {
					try {
						return parseEnum(tokens, EntityTargets.Limit.LIMITSENUM.class);
					} catch (ParseError e) {
						e.suggestsMore(List.of(Suggestion.of(e.mTokenIndex - 1, "1", "Limit count")));
						throw e;
					}
				}
			})
			.put("sorting", (tokens, parameters) -> parseEnum(tokens, EntityTargets.Limit.SORTING.class))
			.build(parameterMap -> {
				Object limit = parameterMap.nonNullValue("limit");
				if (limit instanceof EntityTargets.Limit.LIMITSENUM limitsenum) {
					return new EntityTargets.Limit(
						limitsenum,
						(EntityTargets.Limit.SORTING) parameterMap.nonNullValue("sorting")
					);
				} else {
					return new EntityTargets.Limit(
						(int) limit,
						(EntityTargets.Limit.SORTING) parameterMap.nonNullValue("sorting")
					);
				}
			}))
		.put(Phase.class, () -> ObjectConstructor.builder()
			.put("name", ParameterParserMethod.of(Parser::parseString))
			.put("reusable", ParameterParserMethod.of(Parser::parseBoolean))
			.put("phase", ParameterParserMethod.of(Parser::parseTriggerActions))
			.build(parameterMap -> {
				Pair<List<Trigger>, List<Action>> triggersActions = (Pair<List<Trigger>, List<Action>>) parameterMap.nonNullValue("phase");
				return new Phase(
					(String) parameterMap.nonNullValue("name"),
					(boolean) parameterMap.nonNullValue("reusable"),
					triggersActions.getLeft(),
					triggersActions.getRight()
				);
			}))
		.put(OnSpawnTrigger.class, () -> ObjectConstructor.builder()
			.build(parameterMap -> new OnSpawnTrigger()))
		.put(OnDeathTrigger.class, () -> ObjectConstructor.builder()
			.build(parameterMap -> new OnDeathTrigger()))
		.put(OnDamageTrigger.class, () -> ObjectConstructor.builder()
			.put("type", (tokens, parameters) -> parseEnumOrString(tokens, List.of("ALL"), false, "Source of damage (mob spell name or all sources)", DamageEvent.DamageType.class))
			.put("threshold", ParameterParserMethod.of(Parser::parseLong), 10)
			.build(parameterMap -> {
				Object type = parameterMap.nonNullValue("type");
				if (type instanceof String name) {
					return new OnDamageTrigger(name, (long) parameterMap.nonNullValue("threshold"));
				} else {
					return new OnDamageTrigger((DamageEvent.DamageType) type, (long) parameterMap.nonNullValue("threshold"));
				}
			}))
		.put(OnHurtTrigger.class, () -> ObjectConstructor.builder()
			.put("type", (tokens, parameters) -> parseEnumOrString(tokens, List.of("ALL"), true, "All damage dealt", DamageEvent.DamageType.class, ClassAbility.class))
			.put("threshold", ParameterParserMethod.of(Parser::parseDouble), 10.0)
			.build(parameterMap -> {
				Object type = parameterMap.nonNullValue("type");
				if (type instanceof DamageEvent.DamageType damageType) {
					return new OnHurtTrigger(damageType, (double) parameterMap.nonNullValue("threshold"));
				} else if (type instanceof ClassAbility ability) {
					return new OnHurtTrigger(ability, (double) parameterMap.nonNullValue("threshold"));
				} else {
					return new OnHurtTrigger((double) parameterMap.nonNullValue("threshold"));
				}
			}))
		.put(BossCastTrigger.class, () -> ObjectConstructor.builder()
			.put("tag", (tokens, parameters) -> parseAnyOf(tokens, BossManager.getStatelessBossNames(), "Ability Tag"))
			.build(parameterMap -> new BossCastTrigger((String) parameterMap.nonNullValue("tag"))))
		.put(NearbyPlayersTrigger.class, () -> ObjectConstructor.builder()
			.put("threshold", ParameterParserMethod.of(Parser::parseInteger), 1)
			.put("compare", (tokens, parameters) -> parseEnum(tokens, NearbyPlayersTrigger.CompareOperation.class))
			.put("radius", ParameterParserMethod.of(Parser::parseDouble), 8.0)
			.put("lineOfSight", ParameterParserMethod.of(Parser::parseBoolean), false, false)
			.build(parameterMap -> new NearbyPlayersTrigger(
				(double) parameterMap.nonNullValue("radius"),
				(NearbyPlayersTrigger.CompareOperation) parameterMap.nonNullValue("compare"),
				(int) parameterMap.nonNullValue("threshold"),
				(boolean) parameterMap.nonNullValue("lineOfSight")
			)))
		.put(HealthTrigger.class, () -> ObjectConstructor.builder()
			.put("threshold", ParameterParserMethod.of(Parser::parseDouble))
			.build(parameterMap -> new HealthTrigger((double) parameterMap.nonNullValue("threshold"))))
		.put(CustomTrigger.class, () -> ObjectConstructor.builder()
			.put("key", ParameterParserMethod.of(Parser::parseString), "TriggerName")
			.put("oneTime", ParameterParserMethod.of(Parser::parseBoolean))
			.build(parameterMap -> new CustomTrigger(
				(String) parameterMap.nonNullValue("key"),
				(boolean) parameterMap.nonNullValue("oneTime")
			)))
		.put(FlagTrigger.class, () -> ObjectConstructor.builder()
			.put("key", ParameterParserMethod.of(Parser::parseString), "FlagName")
			.put("startingState", ParameterParserMethod.of(Parser::parseBoolean))
			.build(parameterMap -> new FlagTrigger(
				(String) parameterMap.nonNullValue("key"),
				(boolean) parameterMap.nonNullValue("startingState")
			)))
		.put(OnShootTrigger.class, () -> ObjectConstructor.builder()
			.build(parameterMap -> new OnShootTrigger())
		)
		.put(AddAbilityAction.class, () -> ObjectConstructor.builder()
			.put("ability", (tokens, parameters) -> parseAnyOf(tokens, BossManager.getStatelessBossNames(), "Ability Tag"))
			.build(parameterMap -> new AddAbilityAction((String) parameterMap.nonNullValue("ability"))))
		.put(RemoveAbilityAction.class, () -> ObjectConstructor.builder()
			.put("ability", (tokens, parameters) -> parseAnyOf(tokens, BossManager.getStatelessBossNames(), "Ability Tag"))
			.build(parameterMap -> new RemoveAbilityAction((String) parameterMap.nonNullValue("ability"))))
		.put(ForceCastAction.class, () -> ObjectConstructor.builder()
			.put("ability", (tokens, parameters) -> parseAnyOf(tokens, BossManager.getStatelessBossNames(), "Ability Tag"))
			.build(parameterMap -> new ForceCastAction((String) parameterMap.nonNullValue("ability"))))
		.put(CustomTriggerAction.class, () -> ObjectConstructor.builder()
			.put("key", ParameterParserMethod.of(Parser::parseString), "TriggerName")
			.build(parameterMap -> new CustomTriggerAction(
				(String) parameterMap.nonNullValue("key")
			)))
		.put(DelayAction.class, () -> ObjectConstructor.builder()
			.put("delay", ParameterParserMethod.of(Parser::parseInteger), 20)
			.put("action", (tokens, parameters) -> parseAction(tokens))
			.build(parameterMap -> new DelayAction(
				(int) parameterMap.nonNullValue("delay"),
				(Action) parameterMap.nonNullValue("action")
			)))
		.put(CommandAction.class, () -> ObjectConstructor.builder()
			.put("command", (tokens, parameters) -> parseCommand(tokens))
			.build(parameterMap -> new CommandAction((String) parameterMap.nonNullValue("command"))))
		.put(RandomAction.class, () -> ObjectConstructor.builder()
			.put("chance", ParameterParserMethod.of(Parser::parseDouble))
			.put("action1", (tokens, parameters) -> parseAction(tokens))
			.put("action2", (tokens, parameters) -> parseAction(tokens))
			.build(parameterMap -> new RandomAction(
				(double) parameterMap.nonNullValue("chance"),
				(Action) parameterMap.nonNullValue("action1"),
				(Action) parameterMap.nonNullValue("action2")
			)))
		.put(FlagSetAction.class, () -> ObjectConstructor.builder()
			.put("key", ParameterParserMethod.of(Parser::parseString), "FlagName")
			.put("state", ParameterParserMethod.of(Parser::parseBoolean))
			.build(parameterMap -> new FlagSetAction(
				(String) parameterMap.nonNullValue("key"),
				(boolean) parameterMap.nonNullValue("state")
			)))
		.build();

	/**
	 * Parses parameters from tokens
	 *
	 * @param parameters an object extending BossParameters, to have its fields changed in place
	 * @param tokens     a {@code Tokens} class, obtained from {@code Tokenizer.new()} and using the {@code getTokens()} method on it
	 * @return set of deprecated parameters used
	 */
	public static <T extends BossParameters> Set<String> parseParameters(T parameters, Tokens tokens) throws ParseError {
		Field[] fields = parameters.getClass().getFields();
		Map<String, BossTagCommand.TypeAndDesc> validParams = new LinkedHashMap<>();
		for (Field field : fields) {
			String description = "undefined";
			BossParam paramAnnotations = field.getAnnotation(BossParam.class);
			boolean deprecated = false;
			if (paramAnnotations != null) {
				description = paramAnnotations.help();
				deprecated = paramAnnotations.deprecated();
			}
			validParams.put(BossUtils.translateFieldNameToTag(field.getName()), new BossTagCommand.TypeAndDesc(field, description, deprecated));
		}

		tokens.consume(Tokens.TokenType.OPEN_SQUARE);

		// Empty Parameters list (???)
		if (tokens.matchThenConsume(Tokens.TokenType.CLOSE_SQUARE)) {
			return Set.of();
		}

		Set<String> deprecatedParamsUsed = new HashSet<>();

		do {
			// Complex nonsense for basically get parameter name or else give suggestions of parameter names and values starting with the trailing input
			String parameterName = tokens.consume(Tokens.TokenType.PARAMETER_NAME, () -> {
				List<Suggestion> suggestions = new ArrayList<>(validParams.size());

				validParams.forEach((key, value) -> {
					try {
						suggestions.add(Suggestion.of(tokens.getIndex(), key + '=' + Optional.ofNullable(value.getField().get(parameters)).map(Object::toString).orElse("null"), value.getDesc()));
					} catch (IllegalAccessException e) {
						MMLog.severe(String.format("Got IllegalAccessException while reading parameter: %s", key) + Arrays.toString(e.getStackTrace()));
					}
				});

				return suggestions;
			}).getValue();

			if (!validParams.containsKey(parameterName)) {
				throw ParseError.of(String.format("Invalid parameter name!: %s", parameterName), tokens.getIndex(), tokens).suggests(validParams.keySet(), "Parameter names");
			}

			// Yay!
			tokens.consume(Tokens.TokenType.EQUALS, () -> List.of(Suggestion.of(tokens.getIndex(), "=", "Equals")));
			BossTagCommand.TypeAndDesc typeAndDesc = validParams.get(parameterName);
			if (typeAndDesc.getDeprecated()) {
				deprecatedParamsUsed.add(parameterName);
			}

			int startingIndex = tokens.getIndex();

			Field field = typeAndDesc.getField();
			Class<?> unknownType = field.getType();

			Result<?> result;

			if (List.class.isAssignableFrom(unknownType)) {
				Class<?> genericType = (Class<?>) getTypeArguments(unknownType)[0];

				ParserMethod<?> parserMethod = getParserMethod(genericType);
				if (parserMethod == null) {
					throw ParseError.of(String.format("Unknown expected type: %s", genericType.getSimpleName()), startingIndex + 1, tokens);
				}
				result = parseList(tokens, parserMethod);
			} else if (EnumSet.class.isAssignableFrom(unknownType)) {
				Class<?> genericType = (Class<?>) getTypeArguments(unknownType)[0];

				result = parseEnumSet(tokens, genericType);
			} else if (Map.class.isAssignableFrom(unknownType)) {
				Type[] typeArguments = getTypeArguments(unknownType);
				Class<?> genericKeyType = (Class<?>) typeArguments[0];
				Class<?> genericValueType = (Class<?>) typeArguments[1];

				ParserMethod<?> keyParser = getParserMethod(genericKeyType);
				ParserMethod<?> valueParser = getParserMethod(genericValueType);

				if (keyParser == null) {
					throw ParseError.of(String.format("Unknown expected type (key): %s", genericKeyType.getSimpleName()), startingIndex + 1, tokens);
				}
				if (valueParser == null) {
					throw ParseError.of(String.format("Unknown expected type (value): %s", genericValueType.getSimpleName()), startingIndex + 1, tokens);
				}

				result = parseMap(tokens, keyParser, valueParser, null);
			} else {
				try {
					ParserMethod<?> parserMethod = getParserMethod(unknownType);
					try {
						result = parserMethod.parse(tokens);
					} catch (ParseError e) {
						e.suggestsMore(List.of(Suggestion.of(startingIndex, typeAndDesc.getField().get(parameters).toString(), String.format("Default value of %s", parameterName))));

						throw e;
					}
				} catch (IllegalAccessException e) {
					throw ParseError.of(String.format("Unknown expected type: %s", unknownType.getSimpleName()), tokens.getIndex() + 1, tokens);
				}
			}
			Object data = result.data;

			if (unknownType.isAssignableFrom(data.getClass()) || (unknownType.isPrimitive() && unknownType.isAssignableFrom(unwrap(data.getClass())))) {
				// Yay!
				try {
					if (data instanceof BossPhasesList phasesList) {
						BossPhasesList bossPhasesList = (BossPhasesList) field.get(parameters);
						bossPhasesList.addBossPhases(phasesList);
					} else {
						field.set(parameters, data);
					}
					validParams.remove(parameterName);
				} catch (IllegalAccessException e) {
					MMLog.severe(String.format("[BossParameters] Somehow got IllegalAccessException parsing parameter%s: %s", parameterName, Arrays.toString(e.getStackTrace())));
				}
			} else {
				throw ParseError.of(String.format("Got wrong type for parsing %s : got %s", unknownType.getSimpleName(), data.getClass().getSimpleName()), tokens.getIndex(), tokens);
			}

			if (tokens.matchThenConsume(Tokens.TokenType.CLOSE_SQUARE)) {
				return deprecatedParamsUsed;
			}

			tokens.consume(EnumSet.of(Tokens.TokenType.COMMA, Tokens.TokenType.CLOSE_SQUARE), () ->
				Suggestion.combine(result.suggestions, List.of(
					Suggestion.of(tokens.getIndex(), ",", "Declare more parameters"),
					Suggestion.of(tokens.getIndex(), "]", "Finish declaring parameters"))));
		} while (true);
	}

	public static <T> Result<List<T>> parseList(Tokens tokens, ParserMethod<T> parser) throws ParseError {
		return parseCollection(tokens, parser, new ArrayList<>());
	}

	public static <C extends Collection<T>, T> Result<C> parseCollection(Tokens tokens, ParserMethod<T> parser, C collection) throws ParseError {
		tokens.consume(Tokens.TokenType.OPEN_SQUARE);

		if (tokens.matchNext(Tokens.TokenType.CLOSE_SQUARE)) {
			// Finish parsing list
			tokens.advance();
			return Result.of(collection);
		}
		while (true) {
			Result<? extends T> result = parser.parse(tokens);
			collection.add(result.data);
			if (tokens.matchNext(Tokens.TokenType.CLOSE_SQUARE)) {
				// Finish parsing list
				tokens.advance();
				return Result.of(collection);
			}
			tokens.consume(EnumSet.of(Tokens.TokenType.COMMA, Tokens.TokenType.CLOSE_SQUARE), () ->
				Suggestion.combine(result.suggestions, List.of(
					Suggestion.of(tokens.getIndex(), ",", "Declare more parameters"),
					Suggestion.of(tokens.getIndex(), "]", String.format("Finish declaring %s", collection.getClass().getSimpleName())))));
		}
	}

	@SuppressWarnings("unchecked")
	public static <E extends Enum<E>> Result<EnumSet<E>> parseEnumSet(Tokens tokens, Class<?> type) throws ParseError {
		Class<E> clazz = (Class<E>) type;
		EnumSet<E> enumSet = EnumSet.noneOf(clazz);
		List<String> values = new ArrayList<>();
		tokens.consume(Tokens.TokenType.OPEN_SQUARE);

		if (tokens.matchNext(Tokens.TokenType.CLOSE_SQUARE)) {
			// Finish parsing list
			tokens.advance();
			return Result.of(enumSet);
		}
		while (true) {
			Result<? extends E> enumResult = parseEnum(tokens, clazz);

			enumSet.add(enumResult.data);
			values.add(enumResult.data.name());

			if (tokens.matchNext(Tokens.TokenType.CLOSE_SQUARE)) {
				// Finish parsing list
				tokens.advance();
				return Result.of(enumSet);
			}
			tokens.consume(EnumSet.of(Tokens.TokenType.COMMA, Tokens.TokenType.CLOSE_SQUARE), () -> {
				enumResult.suggestions.forEach(suggestion ->
					suggestion.filter(x -> !values.contains(x)));

				return Suggestion.combine(enumResult.suggestions, List.of(
					Suggestion.of(tokens.getIndex(), ",", "Declare more parameters"),
					Suggestion.of(tokens.getIndex(), "]", "Finish declaring enum set")));
			});
		}
	}

	public static <K, V> Result<Pair<? extends K, ? extends V>> parseEntry(Tokens tokens, ParserMethod<K> keyParser, ParserMethod<V> valueParser, @Nullable Supplier<V> defaultValue) throws ParseError {
		Result<? extends K> key = keyParser.parse(tokens);
		if (tokens.matchThenConsume(Tokens.TokenType.COLON)) {
			Result<? extends V> value = valueParser.parse(tokens);
			return key.combine(value, Pair::of);
		}
		if (defaultValue == null) {
			throw ParseError.of("Map requires value!", tokens.getIndex() + 1, tokens);
		}
		return key.map(k -> Pair.of(k, defaultValue.get()));
	}

	// A null defaultValue means a value is mandatory!
	public static <K, V> Result<Map<K, V>> parseMap(Tokens tokens, ParserMethod<K> keyParser, ParserMethod<V> valueParser, @Nullable Supplier<V> defaultValue) throws ParseError {
		Map<K, V> map = new HashMap<>();
		tokens.consume(Tokens.TokenType.OPEN_SQUARE);

		if (tokens.matchNext(Tokens.TokenType.CLOSE_SQUARE)) {
			// Finish parsing list
			tokens.advance();
			return Result.of(map);
		}
		while (true) {
			Result<Pair<? extends K, ? extends V>> result = parseEntry(tokens, keyParser, valueParser, defaultValue);
			map.put(result.data.getKey(), result.data.getValue());
			if (tokens.matchNext(Tokens.TokenType.CLOSE_SQUARE)) {
				// Finish parsing list
				tokens.advance();
				return Result.of(map);
			}
			tokens.consume(EnumSet.of(Tokens.TokenType.COMMA, Tokens.TokenType.CLOSE_SQUARE), () ->
				Suggestion.combine(result.suggestions, List.of(
					Suggestion.of(tokens.getIndex(), ",", "Declare more parameters"),
					Suggestion.of(tokens.getIndex(), "]", "Finish declaring map"))));
		}
	}

	// Inlined in a list; no brackets!
	public static Result<Particle.DustOptions> parseDustOptions(Tokens tokens) throws ParseError {
		Result<Color> colorResult = parseColor(tokens);
		float size;
		if (tokens.matchNext(Tokens.TokenType.COMMA)) {
			tokens.advance();
			size = parseFloat(tokens).data;
		} else {
			size = 1.0f;
		}

		return colorResult.map(color -> new Particle.DustOptions(color, size));
	}

	// Inlined in a list; no brackets!
	public static Result<Particle.DustTransition> parseDustTransition(Tokens tokens) throws ParseError {
		Result<Color> colorResult = parseColor(tokens);
		tokens.consume(Tokens.TokenType.COMMA, () ->
			Suggestion.combine(colorResult.suggestions, Suggestion.of(tokens.getIndex(), ",", "Specify color")));
		Result<Color> colorResult2 = parseColor(tokens);
		float size;
		if (tokens.matchNext(Tokens.TokenType.COMMA)) {
			tokens.advance();
			size = parseFloat(tokens).data;
		} else {
			size = 1.0f;
		}

		return colorResult.combine(colorResult2, (c1, c2) -> new Particle.DustTransition(c1, c2, size));
	}

	public static Result<Boolean> parseBoolean(Tokens tokens) throws ParseError {
		tokens.assertNext(Tokens.TokenType.BOOLEAN, () -> List.of(Suggestion.of(tokens.getIndex(), "true", "Set to True"),
			Suggestion.of(tokens.getIndex(), "false", "Set to False")));
		return Result.of(Boolean.parseBoolean(tokens.advance().getValue()));
	}

	public static Result<Integer> parseInteger(Tokens tokens) throws ParseError {
		return parseLong(tokens).map(value ->
			value < Integer.MIN_VALUE ? Integer.MIN_VALUE :
				value > Integer.MAX_VALUE ? Integer.MAX_VALUE :
					value.intValue()
		);
	}

	public static Result<Long> parseLong(Tokens tokens) throws ParseError {
		tokens.assertNext(Tokens.TokenType.INTEGER, Suggestion.of(tokens.getIndex(), "1", "Integer value"));
		return Result.of(Long.parseLong(tokens.advance().getValue()));
	}

	public static Result<Double> parseDouble(Tokens tokens) throws ParseError {
		tokens.assertNext(EnumSet.of(Tokens.TokenType.FLOATING_POINT, Tokens.TokenType.INTEGER),
			() -> List.of(Suggestion.of(tokens.getIndex(), "1.0", "Decimal (floating-point) value")));
		return Result.of(Double.parseDouble(tokens.advance().getValue()));
	}

	public static Result<Float> parseFloat(Tokens tokens) throws ParseError {
		return parseDouble(tokens).map(Double::floatValue);
	}

	public static Result<String> parseString(Tokens tokens) throws ParseError {
		if (tokens.matchThenConsume(Tokens.TokenType.QUOTE)) {
			// Empty String
			if (tokens.matchThenConsume(Tokens.TokenType.QUOTE)) {
				return Result.of(Strings.EMPTY);
			}
			String value = tokens.advance().getValue();
			tokens.consume(Tokens.TokenType.QUOTE);
			return Result.of(value);
		} else if (!tokens.peek().getType().isValue()) {
			// Is not a quoted string, cannot be parsed as a series of identifiers
			throw new Parser.ParseError("Un-expected token, expected a value!", tokens.getIndex() + 1, tokens)
				.suggests(List.of("\"Quoted String\"", "Unquoted String"), "String types");
		}

		List<String> values = new ArrayList<>(1);
		while (tokens.peek().getType().isValue()) {
			values.add(tokens.advance().getValue());
		}
		return Result.of(String.join(" ", values));
	}

	public static Result<Integer> parseHexadecimalRGB(Tokens tokens) throws ParseError {
		tokens.assertNext(Tokens.TokenType.HEXADECIMAL);
		String value = tokens.advance().getValue();
		if (value.startsWith("#")) {
			value = value.substring(1);
		}
		if (value.length() != 6) {
			throw ParseError.of("Invalid RGB hex!", tokens.getIndex(), tokens);
		}
		return Result.of(Integer.parseInt(value, 16));
	}

	// Use this only when necessary
	public static Result<String> parseAnyOf(Tokens tokens, Collection<String> possibleValues, String valueSetName) throws ParseError {
		if (!tokens.peek().getType().isValue()) {
			throw ParseError.of("Not a value!", tokens.getIndex() + 1, tokens).suggests(possibleValues, valueSetName);
		}
		String value = tokens.advance().getValue();
		for (String possibleValue : possibleValues) {
			if (value.equalsIgnoreCase(possibleValue)) {
				return Result.of(possibleValue, Suggestion.fromList(tokens.getIndex() - 1, possibleValues, valueSetName));
			}
		}
		throw ParseError.of(String.format("Invalid %s!", valueSetName), tokens.getIndex(), tokens)
			.suggests(possibleValues, valueSetName);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> Result<T> parseEnum(Tokens tokens, Class<T> enumType) throws ParseError {
		if (!tokens.peek().getType().isValue()) {
			throw ParseError.of("Not a value!", tokens.getIndex() + 1, tokens)
				.suggests(getEnumSuggestions(enumType), enumType.getSimpleName());
		}
		String value = tokens.advance().getValue().toUpperCase(Locale.ROOT);
		@Nullable
		T anEnum = (T) CACHE_ENUM.getUnchecked(enumType).get(value);
		if (anEnum != null) {
			return Result.of(anEnum, Suggestion.fromList(tokens.getIndex() - 1, getEnumSuggestions(enumType), enumType.getSimpleName()));
		} else {
			throw ParseError.of(String.format("Invalid %s name!", enumType.getSimpleName()), tokens.getIndex(), tokens)
				.suggests(getEnumSuggestions(enumType), enumType.getSimpleName());
		}
	}

	public static <T> Result<T> parseConstant(Tokens tokens, Class<T> type) throws ParseError {
		tokens.assertNext(Tokens.TokenType.CONSTANT, Suggestion.of(tokens.getIndex(), getConstantsSuggestions(type), type.getSimpleName()));
		String value = tokens.advance().getValue();

		@Nullable
		T parsed = getConstantFields(type).get(value);
		if (parsed == null) {
			throw ParseError.of(String.format("Invalid %s name!", type.getSimpleName()), tokens.getIndex(), tokens)
				.suggests(getConstantsSuggestions(type), type.getSimpleName());
		}
		return Result.of(parsed, Suggestion.fromList(tokens.getIndex() - 1, getConstantsSuggestions(type), type.getSimpleName()));
	}

	public static <T extends Keyed> Result<T> parseRegistryKey(Tokens tokens, Registry<T> registry, String displayName) throws ParseError {
		String value = parseString(tokens).data.toLowerCase(Locale.ROOT);
		@Nullable
		T parsed = registry.get(NamespacedKey.minecraft(value));
		if (parsed == null) {
			throw ParseError.of(String.format("Invalid %s name!", displayName), tokens.getIndex(), tokens)
				.suggests(getRegistrySuggestions(registry), displayName);
		}
		return Result.of(parsed, Suggestion.fromList(tokens.getIndex() - 1, getRegistrySuggestions(registry), displayName));
	}

	public static Result<Color> parseColor(Tokens tokens) throws ParseError {
		Tokens.Token peek = tokens.peek();

		switch (peek.getType()) {
			case HEXADECIMAL -> {
				return parseHexadecimalRGB(tokens).map(Color::fromRGB);
			}
			case CONSTANT -> {
				return parseConstant(tokens, Color.class);
			}
			default -> throw ParseError.of("Unexpected token type: " + peek.getType(), tokens.getIndex() + 1, tokens)
				.suggests(getConstantsSuggestions(Color.class), "Color");
		}
	}

	public static Result<LoSPool> parseLosPool(Tokens tokens) throws ParseError {
		Tokens.Token peek = tokens.peek();

		switch (peek.getType()) {
			case STRING -> {
				String value = peek.getValue();
				Set<String> poolNames = LibraryOfSoulsIntegration.getPoolNames();
				Set<String> partyNames = LibraryOfSoulsIntegration.getPartyNames();
				
				if (poolNames.contains(value)) {
					return parseAnyOf(tokens, poolNames, "Los Pool").map(LoSPool.LibraryPool::new);
				} else if (partyNames.contains(value)) {
					return parseAnyOf(tokens, partyNames, "Los Party").map(LoSPool.LibraryPool::new);
				} else {
					// Not in either list, throw error with combined suggestions
					Set<String> allNames = new HashSet<>(poolNames);
					allNames.addAll(partyNames);
					throw ParseError.of("Invalid Los Pool/Party name: '" + value + "'", tokens.getIndex() + 1, tokens)
						.suggests(allNames.stream().sorted().toList(), "Los Pool/Party");
				}
			}
			case OPEN_SQUARE -> {
				// Inline Pool
				return Result.of(new LoSPool.InlinePool(parseMap(tokens, t ->
					parseAnyOf(t, LibraryOfSoulsIntegration.getSoulNames(), "Soul Name"), Parser::parseInteger, () -> 100).data));
			}
			default -> throw ParseError.of("Unexpected value: " + peek.getType(), tokens.getIndex() + 1, tokens);
		}
	}

	public static Result<String> parseCommand(Tokens tokens) throws ParseError {
		int commandTokenIndex = tokens.getIndex() + 1;
		int starting = tokens.peek().getType() == Tokens.TokenType.TERMINATOR ? tokens.getRaw().length() : tokens.peek().getStarting();

		while (tokens.hasRemaining() && !tokens.matchNext(Tokens.TokenType.CLOSE_ROUND)) {
			tokens.advance();
		}

		int ending = tokens.peek().getType() == Tokens.TokenType.TERMINATOR ? tokens.getRaw().length() : tokens.peek().getStarting();

		String commandFull = tokens.getRaw().substring(starting, ending);

		String[] cmd = commandFull.split(" ", Integer.MAX_VALUE);

		ArrayList<Suggestion> suggestions = new ArrayList<>();
		if (cmd.length == 1) {
			suggestions.add(Suggestion.of(commandTokenIndex - 1, getCommandSuggestions(), "Command completions"));
		} else {
			Command commandC = Bukkit.getCommandMap().getCommand(cmd[0]);
			if (commandC == null) {
				throw ParseError.of("Invalid Command", commandTokenIndex, tokens);
			}

			List<String> tabComplete = commandC.tabComplete(Bukkit.getConsoleSender(), cmd[0], Arrays.copyOfRange(cmd, 1, cmd.length), null);
			// This shouldn't be null - but somehow it is sometimes based on observed exceptions on the play server
			if (tabComplete != null) {
				String lastArg = cmd[cmd.length - 1];

				tabComplete.forEach(completion -> suggestions.add(completion.startsWith(lastArg) ?
					Suggestion.of(tokens.getIndex() - 1, completion, "Command completions") : // expected
					Suggestion.of(tokens.getIndex(), completion, "Command completions add"))); // bizzare scenario, /customeffect
			}
		}

		return Result.of(commandFull, suggestions);
	}

	public static Result<? extends Trigger> parseTrigger(Tokens tokens) throws ParseError {
		boolean negated = false;
		if (tokens.peek().getValue().equals("NOT")) {
			tokens.advance();
			negated = true;
		}

		Set<String> possibleValues = new HashSet<>(PHASE_TRIGGERS.keySet());
		possibleValues.add("NOT");

		String type = parseAnyOf(tokens, possibleValues, "Trigger").data;

		@Nullable
		ParserMethod<Trigger> parserMethod = PHASE_TRIGGERS.get(type);

		if (parserMethod == null) {
			throw new IllegalArgumentException("No Trigger parser found!");
		}

		Trigger trigger = parserMethod.parse(tokens).data;
		trigger.setNegated(negated);

		String validString;
		try {
			validString = parseAnyOf(tokens, Trigger.OPERATION_NAMES, "Trigger Operation").data;
		} catch (ParseError e) {
			// Add a space to prevent the AND/OR/XOR operators from being absorbed into ON_SPAWN and ON_DEATH triggers
			ListIterator<Suggestion> suggestions = e.rawSuggestions().listIterator();
			while (suggestions.hasNext()) {
				suggestions.set(suggestions.next().map(s -> " " + s));
			}

			throw e;
		}

		@Nullable
		Trigger.TriggerOperation operation = validString.equals("->") ? null : Trigger.TriggerOperation.valueOf(validString);

		if (operation != null) {
			trigger.setOperation(operation);
		}

		return Result.of(trigger);
	}

	public static Result<? extends Action> parseAction(Tokens tokens) throws ParseError {
		String type = parseAnyOf(tokens, PHASE_ACTIONS.keySet(), "PhaseAction").data;

		@Nullable
		ParserMethod<Action> parserMethod = PHASE_ACTIONS.get(type);

		if (parserMethod == null) {
			throw new IllegalArgumentException("No Action parser found!");
		}
		return parserMethod.parse(tokens);
	}

	public static Result<Pair<List<Trigger>, List<Action>>> parseTriggerActions(Tokens tokens) throws ParseError {
		@Nullable
		Trigger.TriggerOperation operation;
		List<Trigger> triggers = new ArrayList<>(1);
		// Parse triggers until -> is reached, denoting the end of trigger declatation.
		do {
			Trigger trigger = parseTrigger(tokens).data;
			triggers.add(trigger);
			operation = trigger.getOperation();
		} while (operation != null);

		List<Action> actions = new ArrayList<>();
		do {
			Action action = parseAction(tokens).data;
			actions.add(action);
		} while (tokens.matchThenConsume(Tokens.TokenType.COMMA));
		return Result.of(Pair.of(triggers, actions));
	}

	// lockToSuggested to accept ONLY strings in lockToSuggested
	@SafeVarargs
	public static Result<?> parseEnumOrString(Tokens tokens, Collection<String> suggestedStrings, boolean lockToSuggested, String stringValueName, Class<? extends Enum<?>>... enumClasses) throws ParseError {
		ArrayList<Suggestion> suggestions = new ArrayList<>(4);
		if (!lockToSuggested) {
			suggestions.addAll(List.of(Suggestion.of(tokens.getIndex(), "Unquoted String", stringValueName),
				Suggestion.of(tokens.getIndex(), "\"Quoted String\"", stringValueName)));
		}
		int stringIndex = tokens.getIndex() + 1;

		// Assume only 1 enum per name in total
		@Nullable
		Object data = null;

		// prevents bad token retreats
		if (tokens.peek().getType().isValue()) {
			for (Class<?> enumClass : enumClasses) {
				try {
					data = getParserMethod(enumClass).parse(tokens).data;
				} catch (ParseError e) {
					suggestions.addAll(e.rawSuggestions());
				} finally {
					tokens.retreat();
				}
			}
		} else {
			for (Class<? extends Enum<?>> clazz : enumClasses) {
				suggestions.add(Suggestion.of(tokens.getIndex(), getEnumSuggestions(clazz), clazz.getSimpleName()));
			}
		}

		suggestions.add(Suggestion.of(tokens.getIndex(), suggestedStrings, stringValueName));

		if (data != null) {
			tokens.advance();
			return new Result<>(data, suggestions);
		}

		Result<String> stringResult;
		try {
			stringResult = parseString(tokens).appendSuggestion(suggestions);
		} catch (ParseError e) {
			e.suggests(suggestions);
			throw e;
		}

		if (lockToSuggested && !suggestedStrings.contains(stringResult.data)) {
			throw ParseError.of(String.format("Invalid String! Accepts only %s", suggestedStrings), stringIndex, tokens)
				.suggests(suggestions);
		}
		return stringResult;
	}

	public static <T> Result<T> parseObject(Tokens tokens, Class<T> objectClass, boolean squareBrackets) throws ParseError {
		tokens.consume(squareBrackets ? Tokens.TokenType.OPEN_SQUARE : Tokens.TokenType.OPEN_ROUND);
		T value = parseObjectParameters(tokens, objectClass, squareBrackets ? Tokens.TokenType.CLOSE_SQUARE : Tokens.TokenType.CLOSE_ROUND);
		tokens.consume(squareBrackets ? Tokens.TokenType.CLOSE_SQUARE : Tokens.TokenType.CLOSE_ROUND);
		return Result.of(value);
	}

	public static <T> Result<T> parsePredicatedObject(Tokens tokens, ClassPredictor<T> classGetter, boolean squareBrackets) throws ParseError {
		tokens.consume(squareBrackets ? Tokens.TokenType.OPEN_SQUARE : Tokens.TokenType.OPEN_ROUND);
		T value = parseObjectParameters(tokens, classGetter.predict(tokens), squareBrackets ? Tokens.TokenType.CLOSE_SQUARE : Tokens.TokenType.CLOSE_ROUND);
		tokens.consume(squareBrackets ? Tokens.TokenType.CLOSE_SQUARE : Tokens.TokenType.CLOSE_ROUND);
		return Result.of(value);
	}

	/**
	 * Parses Parameters one by one, parameters can be specified early using <name>=<value>
	 *
	 * @param tokens      {@code Tokens} object
	 * @param objectClass Class of object being parsed, to be looked up in {@code CONSTRUCTOR_MAP}
	 * @param terminator  {@code TokenType} to look out for to indicate the ending of parameter declaration
	 * @return An object of the class
	 */
	@SuppressWarnings("unchecked")
	private static <T> T parseObjectParameters(Tokens tokens, Class<T> objectClass, Tokens.TokenType terminator) throws ParseError {
		String terminatorDefaultValue = Optional.ofNullable(terminator.getDefaultValue()).orElse("");

		@Nullable
		Supplier<ObjectConstructor<?>> objectConstructorSupplier = CONSTRUCTOR_MAP.get(objectClass);
		if (objectConstructorSupplier == null) {
			throw ParseError.of(String.format("%s is not supported, please ask a plugin dev to add it to the CONSTRUCTOR_MAP!", objectClass.getSimpleName()), tokens.getIndex(), tokens);
		}

		ObjectConstructor<?> objectConstructor = objectConstructorSupplier.get();
		while (true) {
			AbstractList<Suggestion> suggestions;

			int startingIndex = tokens.getIndex();

			if (tokens.matchNext(Tokens.TokenType.PARAMETER_NAME)) {
				String paramName = tokens.advance().getValue();
				tokens.consume(Tokens.TokenType.EQUALS, Suggestion.of(tokens.getIndex(), List.of("="), "Equals"));

				suggestions = objectConstructor.parseParameter(paramName, tokens).suggestions;
			} else {
				try {
					suggestions = objectConstructor.parseCurrentParameter(tokens).suggestions;
				} catch (ParseError e) {
					if (!objectConstructor.getLastParsedParameter().isRequired()) {
						List<Suggestion> more = new ArrayList<>(3);

						objectConstructor.getParsableParameters().forEach(parameter ->
							more.add(Suggestion.of(startingIndex, String.format("%s=%s", parameter.getName(),
								Optional.ofNullable(parameter.getValue())
									.map(Object::toString)
									.orElse("")), parameter.getName())));

						e.suggestsMore(more);
					}

					throw e;
				}
			}
			if (tokens.matchNext(terminator)) {
				if (objectConstructor.noParametersLeft()) {
					break;
				}

				// Check no other parameters are required
				List<ObjectConstructor.Parameter> otherRequiredParams = objectConstructor.getRemainingRequiredParameters();
				ObjectConstructor.Parameter nextParameter = objectConstructor.getNextParameter();
				if (nextParameter.isStillRequired()) {
					throw ParseError.of(String.format("Parameter %s is required!", nextParameter.getName()), tokens.getIndex() + 1, tokens)
						.suggests(List.of(Optional.ofNullable(nextParameter.getValue()).orElse("").toString()), "Required parameter value");
				} else if (!otherRequiredParams.isEmpty()) {
					ObjectConstructor.Parameter paramNextNeeded = otherRequiredParams.get(0);
					throw ParseError.of(String.format("Parameter(s) %s are required!", otherRequiredParams.stream().map(ObjectConstructor.Parameter::getName).toList()), tokens.getIndex(), tokens)
						.suggests(List.of(String.format("%s=%s", paramNextNeeded.getName(), Optional.ofNullable(paramNextNeeded.getValue()).orElse(""))), "Required parameter value");
				}

				break;
			}
			if (objectConstructor.noParametersLeft()) {
				throw ParseError.of(String.format("No more parameters for %s", objectClass.getSimpleName()), tokens.getIndex() + 1, tokens)
					.suggests(List.of(terminatorDefaultValue), "Finish declaring object")
					.suggestsMore(suggestions);
			}
			tokens.consume(EnumSet.of(Tokens.TokenType.COMMA, terminator), () ->
				Suggestion.combine(suggestions, List.of(
					Suggestion.of(tokens.getIndex(), ",", "Declare more parameters"),
					Suggestion.of(tokens.getIndex(), terminatorDefaultValue, "Finish declaring parameters"))));
		}
		return (T) objectConstructor.construct();
	}

	@SuppressWarnings("unchecked")
	public static <T> Map<String, T> getConstantFields(Class<T> clazz) {
		return (Map<String, T>) CACHE_CONSTANTS.getUnchecked(clazz);
	}

	private static Collection<String> getCommandSuggestions() {
		return Bukkit.getCommandMap().getKnownCommands().keySet();
	}

	public static Collection<String> getEnumSuggestions(Class<? extends Enum<?>> enumType) {
		return CACHE_ENUM.getUnchecked(enumType).keySet();
	}

	public static <T> Collection<String> getConstantsSuggestions(Class<T> constantsType) {
		return getConstantFields(constantsType).keySet();
	}

	public static <T extends Keyed> Collection<String> getRegistrySuggestions(Registry<T> registry) {
		return CACHE_REGISTRY.getUnchecked(registry);
	}

	public abstract static class ObjectConstructor<T> {
		protected final ParameterMap mParameters = new ParameterMap();
		private int mIndex = -1;
		private final List<Parameter> mParameterList;

		private ObjectConstructor(List<Parameter> parameters) {
			parameters.forEach(parameter -> mParameters.put(parameter.getName(), parameter));
			mParameterList = new ArrayList<>(mParameters.values());
		}

		public boolean noParametersLeft() {
			for (Parameter value : mParameters.values()) {
				if (value.isNotParsed()) {
					return false;
				}
			}
			return true;
		}

		public List<Parameter> getRemainingRequiredParameters() {
			List<Parameter> requiredParameters = new ArrayList<>();
			for (Parameter value : mParameters.values()) {
				if (value.isStillRequired()) {
					requiredParameters.add(value);
				}
			}
			return requiredParameters;
		}

		public Parameter getNextParameter() {
			for (mIndex++; mIndex < mParameters.size(); mIndex++) {
				Parameter parameter = mParameterList.get(mIndex);
				if (parameter.isNotParsed()) {
					return parameter;
				}
			}
			throw new IllegalStateException("No more parameters! Please scream at the developer!");
		}

		public Parameter getLastParsedParameter() {
			return mParameterList.get(mIndex);
		}

		public List<Parameter> getAllParameters() {
			return mParameterList;
		}

		public List<Parameter> getParsableParameters() {
			List<Parameter> parsable = new ArrayList<>();
			getAllParameters().forEach(parameter -> {
				if (parameter.isNotParsed()) {
					parsable.add(parameter);
				}
			});
			return parsable;
		}

		public Result<?> parseParameter(String name, Tokens tokens) throws ParseError {
			int startingIndex = tokens.getIndex();
			// Assumes only 1 parameter of the same name!
			@Nullable
			Parameter parameter = mParameters.get(name);
			if (parameter == null || !parameter.isNotParsed()) {
				throw ParseError.of(String.format("Parameter %s does not exist or has been duplicated!", name), startingIndex, tokens);
			}

			try {
				return parameter.parse(tokens, mParameters);
			} catch (ParseError e) {
				ListIterator<Suggestion> suggestions = e.rawSuggestions().listIterator();
				while (suggestions.hasNext()) {
					Suggestion next = suggestions.next();
					suggestions.set(Suggestion.of(next.until, next.body, String.format("%s: %s", name, next.tooltip)));
				}

				e.suggestsMore(List.of(Suggestion.of(startingIndex, Optional.ofNullable(parameter.getValue()).map(Object::toString).orElse(""), name)));
				throw e;
			}
		}

		public Result<?> parseCurrentParameter(Tokens tokens) throws ParseError {
			int startingIndex = tokens.getIndex();
			Parameter parameter = getNextParameter();
			try {
				return parameter.parse(tokens, mParameters);
			} catch (ParseError e) {
				e.suggestsMore(List.of(Suggestion.of(startingIndex, Optional.ofNullable(parameter.getValue()).map(Object::toString).orElse(""), parameter.getName())));
				throw e;
			}
		}

		abstract T construct();

		public static <L> ConstructorBuilder<L> builder() {
			return new ConstructorBuilder<>();
		}

		public static class Parameter {
			private final String mName;
			private final ParameterParserMethod<?> mParserMethod;
			private final boolean mRequired;
			@Nullable
			private Object mValue;
			private boolean mParsed = false;

			private Parameter(String name, ParameterParserMethod<?> parserMethod, @Nullable Object defaultValue, boolean required) {
				mName = name;
				mParserMethod = parserMethod;
				mValue = defaultValue;
				mRequired = required;
			}

			public String getName() {
				return mName;
			}

			@Nullable
			public Object getValue() {
				return mValue;
			}

			private Result<?> parse(Tokens tokens, ParameterMap parameters) throws ParseError {
				mParsed = true;

				Result<?> result = mParserMethod.parse(tokens, parameters);
				mValue = result.data;

				return result;
			}

			public boolean isStillRequired() {
				return mRequired && !mParsed;
			}

			public boolean isNotParsed() {
				return !mParsed;
			}

			public boolean isRequired() {
				return mRequired;
			}
		}

		public static class ParameterMap extends LinkedHashMap<String, Parameter> {
			@Nullable
			public Object nullableValue(Object key) {
				return Objects.requireNonNull(super.get(key)).getValue();
			}

			@NotNull
			public Object nonNullValue(Object key) {
				return Objects.requireNonNull(nullableValue(key));
			}
		}

		public static class ConstructorBuilder<I> {
			List<Parameter> mCurrentParameters = new ArrayList<>();

			public ConstructorBuilder() {

			}

			public ConstructorBuilder<I> put(String name, ParameterParserMethod<?> parserMethod) {
				return put(name, parserMethod, null);
			}

			public ConstructorBuilder<I> put(String name, ParameterParserMethod<?> parserMethod, @Nullable Object value) {
				return put(name, parserMethod, value, true);
			}

			public ConstructorBuilder<I> put(String name, ParameterParserMethod<?> parserMethod, @Nullable Object value, boolean required) {
				mCurrentParameters.add(new Parameter(name, parserMethod, value, required));
				return this;
			}

			public ObjectConstructor<I> build(Function<ParameterMap, I> factory) {
				return new ObjectConstructor<>(mCurrentParameters) {

					@Override
					I construct() {
						return factory.apply(mParameters);
					}
				};
			}
		}
	}

	public static class ParseError extends Exception {
		private final int mTokenIndex;
		private final Tokens mTokens;
		private final ArrayList<Suggestion> mSuggestions = new ArrayList<>();

		// No performance overhead from stacktrace generation!
		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}

		private ParseError(String message, int tokenIndex, Tokens tokens) {
			super(message);
			mTokenIndex = tokenIndex;
			mTokens = tokens;
		}

		public static ParseError of(String message, int tokenIndex, Tokens tokens) {
			return new ParseError(message, tokenIndex, tokens);
		}

		public ParseError suggests(Collection<String> suggestions, String tooltip) {
			mSuggestions.clear();
			mSuggestions.add(Suggestion.of(mTokenIndex - 1, suggestions, tooltip));
			return this;
		}

		public ParseError suggestsMore(Collection<String> suggestions, String tooltip) {
			mSuggestions.add(Suggestion.of(mTokenIndex - 1, suggestions, tooltip));
			return this;
		}

		public ParseError suggestsMore(Collection<Suggestion> suggestions) {
			mSuggestions.addAll(suggestions);
			return this;
		}

		public ParseError suggests(Collection<Suggestion> suggestions) {
			mSuggestions.clear();
			mSuggestions.addAll(suggestions);
			return this;
		}

		public Component getErrorHighlighting() {
			ImmutableList<Tokens.Token> tokens = Objects.requireNonNull(mTokens).getTokens();
			if (mTokenIndex < tokens.size()) {
				tokens.get(mTokenIndex).setError();
				return Objects.requireNonNull(mTokens).syntaxHighlight();
			} else {
				return Objects.requireNonNull(mTokens).syntaxHighlight().append(Component.text("<HERE>", NamedTextColor.RED, TextDecoration.UNDERLINED));
			}
		}

		public String rawErrorHighlighting() {
			StringBuilder log = new StringBuilder();
			Tokens.Token errorToken = mTokens.getTokens().get(mTokenIndex);
			String raw = mTokens.getRaw();

			log.append(raw, 0, errorToken.getStarting());
			log.append("!>>");
			log.append(raw, errorToken.getStarting(), errorToken.getEnding());
			log.append("<<!");
			log.append(raw, errorToken.getEnding(), raw.length());

			return log.toString();
		}

		public Collection<Tooltip<String>> getSuggestions(String prefix) {
			AbstractList<Suggestion> suggestions = rawSuggestions();

			String input = prefix + mTokens.getRaw();

			Set<Suggestion.Single> result = new HashSet<>(20);
			for (Suggestion suggestion : suggestions) {
				for (Suggestion.Single single : suggestion.asSingles(prefix, mTokens)) {
					if (single.body.startsWith(input)) {
						result.add(single);
					}
				}
			}

			Collection<Tooltip<String>> tooltips = new ArrayList<>();
			for (Suggestion.Single single : result) {
				tooltips.add(Tooltip.ofString(single.body, single.tooltip));
			}
			return tooltips;
		}

		public AbstractList<Suggestion> rawSuggestions() {
			return mSuggestions;
		}
	}

	public record Result<T>(T data, AbstractList<Suggestion> suggestions) {
		public static <X> Result<X> of(X data, AbstractList<Suggestion> moreSuggestions) {
			return new Result<>(data, moreSuggestions);
		}

		public static <X> Result<X> of(X data) {
			return new Result<>(data, new ArrayList<>());
		}

		public <X> Result<X> map(Function<T, X> mapper) {
			return of(mapper.apply(data), suggestions);
		}

		public <X, Y> Result<Y> combine(Result<X> otherResult, BiFunction<T, X, Y> mapper) {
			return of(mapper.apply(data, otherResult.data), Suggestion.combine(suggestions, otherResult.suggestions));
		}

		public Result<T> appendSuggestion(List<Suggestion> moreSuggestions) {
			return of(data, Suggestion.combine(suggestions, moreSuggestions));
		}
	}

	/**
	 * Record for performant suggestions
	 *
	 * @param until suggest will contain the raw message until this token (index of the token to be referenced in {@code asString()} *inclusive*
	 * @param body  the content to be suggested after {@code until}
	 */
	public record Suggestion(int until, Collection<String> body, String tooltip) {
		public record Single(String body, String tooltip) {
			@Override
			public boolean equals(Object o) {
				if (!(o instanceof Single single)) {
					return false;
				}
				return Objects.equals(body, single.body);
			}

			@Override
			public int hashCode() {
				return Objects.hashCode(body);
			}
		}

		public static Suggestion of(int until, String extra, String tooltip) {
			ArrayList<String> list = new ArrayList<>();
			list.add(extra);
			return new Suggestion(until, list, tooltip);
		}

		public static Suggestion of(int until, Collection<String> extra, String tooltip) {
			return new Suggestion(until, extra, tooltip);
		}

		public static AbstractList<Suggestion> fromList(int until, Collection<String> suggestions, String tooltip) {
			ArrayList<Suggestion> list = new ArrayList<>(1);
			list.add(new Suggestion(until, suggestions, tooltip));
			return list;
		}

		public static AbstractList<Suggestion> combine(AbstractList<Suggestion> suggestions1, Collection<Suggestion> suggestions2) {
			suggestions1.addAll(suggestions2);
			return suggestions1;
		}

		public static AbstractList<Suggestion> combine(AbstractList<Suggestion> suggestions, Suggestion suggestion) {
			suggestions.add(suggestion);
			return suggestions;
		}

		public Suggestion filter(Function<String, Boolean> predicate) {
			body.removeIf(x -> !predicate.apply(x));
			return this;
		}

		public Suggestion map(Function<String, String> mapper) {
			Collection<String> newStrings = new ArrayList<>(body.size());
			for (String string : body) {
				newStrings.add(mapper.apply(string));
			}
			return new Suggestion(until, newStrings, tooltip);
		}

		public Collection<Single> asSingles(String prefix, Tokens tokens) {
			ArrayList<Single> singleSuggestions = new ArrayList<>(body.size());
			body.forEach(b -> singleSuggestions.add(new Single(prefix + asString(tokens, b), tooltip)));
			return singleSuggestions;
		}

		private String asString(Tokens tokens, String append) {
			return tokens.getRaw().substring(0, Optional.ofNullable(tokens.get(until + 1)).map(Tokens.Token::getStarting).orElse(tokens.getRaw().length())) + append;
		}
	}
}
