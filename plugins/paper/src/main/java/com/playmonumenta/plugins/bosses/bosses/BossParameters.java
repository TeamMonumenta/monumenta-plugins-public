package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.commands.BossTagCommand.TypeAndDesc;
import com.playmonumenta.plugins.utils.BossUtils;
import dev.jorel.commandapi.Tooltip;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BossParameters {

	public static <T extends BossParameters> T getParameters(Entity boss, String identityTag, T parameters) {
		String modTag = identityTag + "[";

		for (String tag : boss.getScoreboardTags()) {
			if (tag.startsWith(modTag)) {
				StringReader reader = new StringReader(tag);
				reader.advance(identityTag);

				// Parse as many parameters as possible... but ignore the result, and just return the populated parameters
				ParseResult<T> res = parseParameters(reader, parameters);
				if (res.getResult() == null) {
					Plugin.getInstance().getLogger().warning("[BossParameters] problems during parsing tag for entity: " + boss.getName() + " on tag: " + tag);
				}
			}
		}
		return parameters;
	}

	public static <T extends BossParameters> ParseResult<T> parseParameters(StringReader reader, T parameters) {
		String bossDescription = "undefined";
		BossParam annotations = parameters.getClass().getAnnotation(BossParam.class);
		if (annotations != null) {
			bossDescription = annotations.help();
		}

		// Advance beyond [ character
		if (!reader.advance("[")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "[", bossDescription)));
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
		boolean containsDeprecatedField = false;
		while (true) {
			// Require a comma to separate arguments the 2nd time through
			if (atLeastOneIter) {
				if (!reader.advance(",")) {
					if (reader.advance("]")) {
						ParseResult<T> res = ParseResult.of(parameters);
						res.mContainsDeprecated = containsDeprecatedField;
						return res;
					} else {
						return ParseResult.of(Tooltip.arrayOf(
							Tooltip.of(reader.readSoFar() + ",", ""),
							Tooltip.of(reader.readSoFar() + "]", "")
						));
					}
				}
			} else {
				if (reader.advance("]")) {
					ParseResult<T> res = ParseResult.of(parameters);
					res.mContainsDeprecated = containsDeprecatedField;
					return res;
				}
			}

			atLeastOneIter = true;

			try {
				if (reader.advance("]")) {
					// End!
					ParseResult<T> res = ParseResult.of(parameters);
					res.mContainsDeprecated = containsDeprecatedField;
					return res;
				}

				String validKey = reader.readOneOf(validParams.keySet());
				if (validKey == null) {
					// Could not read a valid key. Either we haven't entered anything yet or possibly we match some but not others
					String remaining = reader.remaining();
					String soFar = reader.readSoFar();
					List<Tooltip<String>> suggArgs = new ArrayList<>(fields.length);
					for (Map.Entry<String, TypeAndDesc> valid : validParams.entrySet()) {
						if (valid.getKey().startsWith(remaining) && !usedParams.contains(valid.getKey())) {
							Object def = valid.getValue().getField().get(parameters);
							final String defStr;
							if (def instanceof PotionEffectType) {
								defStr = ((PotionEffectType) def).getName();
							} else if (def instanceof Sound) {
								defStr = ((Sound) def).name();
							} else {
								defStr = def.toString();
							}
							suggArgs.add(Tooltip.of(soFar + valid.getKey() + "=" + defStr, valid.getValue().getDesc()));
						}
					}
					return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
				} else {
					// This hashmap get must succeed or there is a bug in the Reader
					TypeAndDesc validType = validParams.get(validKey);
					if (!reader.advance("=")) {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "=", validType.getDesc())));
					}

					if (validType.getDeprecated()) {
						containsDeprecatedField = true;
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
							return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + def, validType.getDesc())));
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
							return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + def, validType.getDesc())));
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
								Tooltip.of(reader.readSoFar() + "true", validType.getDesc()),
								Tooltip.of(reader.readSoFar() + "false", validType.getDesc())
							));
						}
						validType.getField().setBoolean(parameters, val);
					} else if (validTypeClass == String.class) {
						String val = reader.readString();
						if (val == null) {
							// No valid string here
							return ParseResult.of(Tooltip.arrayOf(
								Tooltip.of(reader.readSoFar() + "\"quoted,)string\"", "String with quotes can store any data"),
								Tooltip.of(reader.readSoFar() + "simple string", "String without quotes can't contain , or )")
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
								suggArgs.add(Tooltip.of(soFar + valid.getName(), validType.getDesc()));
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
								suggArgs.add(Tooltip.of(soFar + valid.name(), validType.getDesc()));
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
								suggArgs.add(Tooltip.of(soFar + valid, validType.getDesc()));
							}
							suggArgs.add(Tooltip.of("#FFFFFF", validType.getDesc()));
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
								suggArgs.add(Tooltip.of(soFar + valid.name(), validType.getDesc()));
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
					} else {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "Not supported yet: " + validTypeClass.toString(), "")));
					}

					// Still more in the string, so this isn't the end
					usedParams.add(validKey);
				}
			} catch (IllegalAccessException e) {
				Plugin.getInstance().getLogger().severe("Somehow got ILlegalAccessException parsing boss tag: " + e.getMessage());
				e.printStackTrace();
				return ParseResult.of(Tooltip.arrayOf());
			}
		}
	}
}
