package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.BossPhasesList;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.commands.BossTagCommand.TypeAndDesc;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.Tooltip;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public abstract class BossParameters {

	public static <T extends BossParameters> T getParameters(Entity boss, String identityTag, T parameters) {
		String modTag = identityTag + "[";

		for (String tag : boss.getScoreboardTags()) {
			if (tag.startsWith(modTag)) {
				StringReader reader = new StringReader(tag);
				reader.advance(identityTag);

				Location mobLoc = boss.getLocation();
				Component bossDescription = Objects.requireNonNullElseGet(boss.customName(), () -> Component.text("Unnamed " + boss.getType(), NamedTextColor.WHITE))
					                            .append(Component.text(" (at x: " + mobLoc.getBlockX() + " y: " + mobLoc.getBlockY() + " z: " + mobLoc.getBlockZ() + ")", NamedTextColor.GRAY));
				// Parse as many parameters as possible... but ignore the result, and just return the populated parameters
				parseParametersWithWarnings(bossDescription, reader, parameters, Plugin.IS_PLAY_SERVER ? Collections.emptyList() : boss.getWorld().getPlayers());
			}
		}
		return parameters;
	}

	public static <T extends BossParameters> ParseResult<T> parseParametersWithWarnings(Component bossDescription, StringReader reader, T parameters, List<Player> receivingPlayers) {
		ParseResult<T> res = parseParameters(reader, parameters, true);
		if (res.getResult() == null) {
			String readSoFar = reader.readSoFar();
			String remaining = reader.remaining();
			List<String> expected = Objects.requireNonNull(res.getTooltip()).stream().filter(Objects::nonNull).map(Tooltip::getSuggestion).map(s -> s.startsWith(readSoFar) ? s.substring(readSoFar.length()) : s).toList();
			Component message = Component.text("[BossParameters] ", NamedTextColor.GOLD)
				                    .append(Component.text("problems during parsing tag for ", NamedTextColor.RED))
				                    .append(bossDescription)
				                    .append(Component.text(" | on tag: ", NamedTextColor.RED))
				                    .append(Component.text(reader.getString(), NamedTextColor.WHITE));
			if (expected.isEmpty()) {
				message = message.append(Component.text(" | unspecified error", NamedTextColor.RED));
			} else {
				message = message.append(Component.text(" | expected one of [", NamedTextColor.RED));
				message = message.append(expected.stream().limit(8).map(e -> e.isEmpty() ? Component.text("<end>", NamedTextColor.RED) : Component.text(e, NamedTextColor.AQUA))
					                         .reduce(Component.empty(), (c1, c2) -> c1.equals(Component.empty()) ? c2 : c1.append(Component.text(", ", NamedTextColor.RED)).append(c2)));
				if (expected.size() > 8) {
					message = message.append(Component.text(", and " + (expected.size() - 8) + " more possibilities", NamedTextColor.RED));
				}
				message = message.append(Component.text("]", NamedTextColor.RED));
			}
			message = message.append(Component.text(" at position " + readSoFar.length() + " (", NamedTextColor.RED))
				          .append(Component.text(readSoFar.length() > 10 ? "..." + readSoFar.substring(readSoFar.length() - 10) : readSoFar, NamedTextColor.WHITE))
				          .append(Component.text("<here>", NamedTextColor.RED))
				          .append(Component.text(remaining.length() > 10 ? remaining.substring(0, 10) + "..." : remaining, NamedTextColor.WHITE))
				          .append(Component.text(")", NamedTextColor.RED));
			MMLog.warning(MessagingUtils.plainText(message));
			for (Player player : receivingPlayers) {
				player.sendMessage(message);
			}
		}
		return res;
	}

	public static <T extends BossParameters> ParseResult<T> parseParameters(StringReader reader, T parameters, boolean fullSuggestions) {
		String bossDescription = "undefined";
		BossParam annotations = parameters.getClass().getAnnotation(BossParam.class);
		if (annotations != null) {
			bossDescription = annotations.help();
		}

		// Advance beyond [ character
		if (!reader.advance("[")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "[", bossDescription)));
		}

		// Parse all the valid fields into a map of name : Entry(class, description)
		Field[] fields = parameters.getClass().getFields();
		Map<String, TypeAndDesc> validParams = new LinkedHashMap<>();
		for (Field field : fields) {
			String description = "undefined";
			BossParam paramAnnotations = field.getAnnotation(BossParam.class);
			boolean deprecated = false;
			if (paramAnnotations != null) {
				description = paramAnnotations.help();
				deprecated = paramAnnotations.deprecated();
			}
			validParams.put(BossUtils.translateFieldNameToTag(field.getName()), new TypeAndDesc(field, description, deprecated));
		}

		Set<String> usedParams = new LinkedHashSet<>(fields.length);
		boolean atLeastOneIter = false;
		Set<String> deprecatedParameters = null;
		while (!reader.advance("]")) {
			// Require a comma to separate arguments the 2nd time through
			if (atLeastOneIter
				    && !reader.advance(",")) {
				return ParseResult.of(Tooltip.arrayOf(
					Tooltip.ofString(reader.readSoFar() + ",", ""),
					Tooltip.ofString(reader.readSoFar() + "]", "")
				));
			}

			atLeastOneIter = true;

			try {
				String validKey = reader.readOneOf(validParams.keySet());
				if (validKey == null) {
					// Could not read a valid key. Either we haven't entered anything yet or possibly we match some but not others
					String remaining = reader.remaining();
					String soFar = reader.readSoFar();
					List<Tooltip<String>> suggArgs = new ArrayList<>(fields.length);
					for (Map.Entry<String, TypeAndDesc> valid : validParams.entrySet()) {
						if ((valid.getKey().startsWith(remaining) || fullSuggestions) && !usedParams.contains(valid.getKey()) && !valid.getValue().getDeprecated()) {
							Object def = valid.getValue().getField().get(parameters);
							final String defStr;
							if (def instanceof PotionEffectType) {
								defStr = ((PotionEffectType) def).getName();
							} else if (def instanceof Sound) {
								defStr = ((Sound) def).name();
							} else if (def instanceof Color color) {
								if (StringReader.COLOR_NAME_MAP.containsKey(color)) {
									defStr = StringReader.COLOR_NAME_MAP.get(color);
								} else {
									defStr = "#" + Integer.toString(color.asRGB(), 16);
								}
							} else {
								defStr = def.toString();
							}
							suggArgs.add(Tooltip.ofString(soFar + valid.getKey() + "=" + defStr, valid.getValue().getDesc()));
						}
					}
					return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
				} else {
					// This hashmap get must succeed or there is a bug in the Reader
					TypeAndDesc validType = Objects.requireNonNull(validParams.get(validKey));
					if (!reader.advance("=")) {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "=", validType.getDesc())));
					}

					if (validType.getDeprecated()) {
						if (deprecatedParameters == null) {
							deprecatedParameters = new HashSet<>();
						}
						deprecatedParameters.add(validKey);
					}

					Class<?> validTypeClass = validType.getField().getType();
					if (validTypeClass == int.class || validTypeClass == long.class) {
						Long val = reader.readLong();
						if (val == null) {
							// No valid value here - offer default as completion
							final long def;
							if (validTypeClass == int.class) {
								def = validType.getField().getInt(parameters);
							} else {
								def = validType.getField().getLong(parameters);
							}
							return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + def, validType.getDesc())));
						}
						if (validTypeClass == int.class) {
							validType.getField().setInt(parameters, val.intValue());
						} else {
							validType.getField().setLong(parameters, val);
						}
					} else if (validTypeClass == float.class || validTypeClass == double.class) {
						Double val = reader.readDouble();
						if (val == null) {
							// No valid value here - offer default as completion
							final double def;
							if (validTypeClass == float.class) {
								def = validType.getField().getFloat(parameters);
							} else {
								def = validType.getField().getDouble(parameters);
							}
							return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + def, validType.getDesc())));
						}
						if (validTypeClass == float.class) {
							validType.getField().setFloat(parameters, val.floatValue());
						} else {
							validType.getField().setDouble(parameters, val);
						}
					} else if (validTypeClass == boolean.class) {
						Boolean val = reader.readBoolean();
						if (val == null) {
							// No valid value here - offer true and false as options
							return ParseResult.of(Tooltip.arrayOf(
								Tooltip.ofString(reader.readSoFar() + "true", validType.getDesc()),
								Tooltip.ofString(reader.readSoFar() + "false", validType.getDesc())
							));
						}
						validType.getField().setBoolean(parameters, val);
					} else if (validTypeClass == String.class) {
						String val = reader.readString();
						if (val == null) {
							// No valid string here
							return ParseResult.of(Tooltip.arrayOf(
								Tooltip.ofString(reader.readSoFar() + "\"quoted,)string\"", "String with quotes can store any data"),
								Tooltip.ofString(reader.readSoFar() + "simple string", "String without quotes can't contain , or )")
							));
						}
						validType.getField().set(parameters, val);
					} else if (validTypeClass == PotionEffectType.class) {
						PotionEffectType val = reader.readPotionEffectType();
						if (val == null) {
							// Entry not valid, offer all entries as completions
							List<Tooltip<String>> suggArgs = new ArrayList<>(PotionEffectType.values().length);
							String soFar = reader.readSoFar();
							for (PotionEffectType valid : PotionEffectType.values()) {
								suggArgs.add(Tooltip.ofString(soFar + valid.getName(), validType.getDesc()));
							}
							return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
						}
						validType.getField().set(parameters, val);
					} else if (validTypeClass == Sound.class) {
						Sound val = reader.readSound();
						if (val == null) {
							// Entry not valid, offer all entries as completions
							List<Tooltip<String>> suggArgs = new ArrayList<>(Sound.values().length);
							String soFar = reader.readSoFar();
							for (Sound valid : Sound.values()) {
								suggArgs.add(Tooltip.ofString(soFar + valid.name(), validType.getDesc()));
							}
							return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
						}
						validType.getField().set(parameters, val);
					} else if (validTypeClass == Color.class) {
						Color val = reader.readColor();
						if (val == null) {
							// Color not valid - need to offer all colors as a completion option, plus #FFFFFF
							List<Tooltip<String>> suggArgs = new ArrayList<>(1 + StringReader.COLOR_MAP.size());
							String soFar = reader.readSoFar();
							for (String valid : StringReader.COLOR_MAP.keySet()) {
								suggArgs.add(Tooltip.ofString(soFar + valid, validType.getDesc()));
							}
							suggArgs.add(Tooltip.ofString("#FFFFFF", validType.getDesc()));
							return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
						}
						validType.getField().set(parameters, val);
					} else if (validTypeClass == Material.class) {
						Material mat = reader.readMaterial();
						if (mat == null) {
							// Entry not valid, offer all entries as completions
							List<Tooltip<String>> suggArgs = new ArrayList<>(Material.values().length);
							String soFar = reader.readSoFar();
							for (Material valid : Material.values()) {
								suggArgs.add(Tooltip.ofString(soFar + valid.name(), validType.getDesc()));
							}
							return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
						}
						validType.getField().set(parameters, mat);
					} else if (validTypeClass == SoundsList.class) {
						ParseResult<SoundsList> result = SoundsList.fromReader(reader, validType.getDesc());
						if (result.getTooltip() != null) {
							return ParseResult.of(result.getTooltip());
						}
						validType.getField().set(parameters, result.getResult());
					} else if (validTypeClass == EffectsList.class) {
						ParseResult<EffectsList> result = EffectsList.fromReader(reader, validType.getDesc());
						if (result.getTooltip() != null) {
							return ParseResult.of(result.getTooltip());
						}
						validType.getField().set(parameters, result.getResult());
					} else if (validTypeClass == ParticlesList.class) {
						ParseResult<ParticlesList> result = ParticlesList.fromReader(reader, validType.getDesc());
						if (result.getTooltip() != null) {
							return ParseResult.of(result.getTooltip());
						}
						validType.getField().set(parameters, result.getResult());
					} else if (validTypeClass == LoSPool.class) {
						ParseResult<LoSPool> result = LoSPool.fromReader(reader, validType.getDesc());
						if (result.getTooltip() != null) {
							return ParseResult.of(result.getTooltip());
						}
						validType.getField().set(parameters, result.getResult());
					} else if (validTypeClass == EntityTargets.class) {
						ParseResult<EntityTargets> result = EntityTargets.fromReader(reader, validType.getDesc());
						if (result.getTooltip() != null) {
							return ParseResult.of(result.getTooltip());
						}
						validType.getField().set(parameters, result.getResult());
					} else if (validTypeClass == BossPhasesList.class) {
						ParseResult<BossPhasesList> result = BossPhasesList.fromReader(reader);
						if (result.getTooltip() != null) {
							return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "USE: /bosstag phase add ---", "DUMP")));
						}
						((BossPhasesList) validType.getField().get(parameters)).addBossPhases(Objects.requireNonNull(result.getResult()));
					} else if (Enum.class.isAssignableFrom(validTypeClass)) {
						Object val = reader.readEnum(((Class<? extends Enum>) validTypeClass).getEnumConstants());
						if (val == null) {
							// Entry not valid, offer all entries as completions
							List<Tooltip<String>> suggArgs = new ArrayList<>();
							String soFar = reader.readSoFar();
							for (Enum<?> valid : ((Class<? extends Enum>) validTypeClass).getEnumConstants()) {
								suggArgs.add(Tooltip.ofString(soFar + valid.name(), validType.getDesc()));
							}
							return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
						}
						validType.getField().set(parameters, val);
					} else {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "Not supported yet: " + validTypeClass.toString(), "")));
					}

					// Still more in the string, so this isn't the end
					usedParams.add(validKey);
				}
			} catch (IllegalAccessException e) {
				MMLog.severe("[BossParameters] Somehow got IllegalAccessException parsing boss tag: " + e.getMessage());
				e.printStackTrace();
				return ParseResult.of(Tooltip.arrayOf());
			} catch (Exception e) {
				MMLog.severe("[BossParameters] Somehow got " + e.getClass().getSimpleName() + " parsing boss tag: " + e.getMessage());
				e.printStackTrace();
				return ParseResult.of(Tooltip.arrayOf());
			}
		}

		ParseResult<T> res = ParseResult.of(parameters);
		res.mDeprecatedParameters = deprecatedParameters;
		return res;
	}
}
