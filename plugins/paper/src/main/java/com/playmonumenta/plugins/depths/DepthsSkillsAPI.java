package com.playmonumenta.plugins.depths;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class DepthsSkillsAPI {
	public static JsonObject dumpFullJson() {
		JsonArray trees = new JsonArray();
		for (DepthsTree tree : DepthsTree.values()) {
			trees.add(treeToJson(tree));
		}
		trees.add(treeToJson(null)); // aspects

		JsonObject obj = new JsonObject();
		obj.add("trees", trees);
		return obj;
	}

	private static JsonObject treeToJson(@Nullable DepthsTree tree) {
		boolean isInDepths = tree != DepthsTree.PRISMATIC && tree != DepthsTree.CURSE && tree != DepthsTree.GIFT;

		JsonArray abilities = new JsonArray();
		for (DepthsAbilityInfo<?> ability : DepthsManager.getAbilitiesOfTree(tree)) {
			if (ability != null) {
				abilities.add(abilityToJson(ability, isInDepths));
			}
		}

		JsonObject info = new JsonObject();
		info.addProperty("tree", tree == null ? "Aspect" : tree.getDisplayName());
		if (isInDepths && tree != null) {
			info.addProperty("description", tree.getDescription());
		}
		info.add("skills", abilities);
		return info;
	}

	private static JsonObject abilityToJson(DepthsAbilityInfo<?> ability, boolean isInDepths) {
		JsonObject info = new JsonObject();
		info.addProperty("name", ability.getDisplayName());
		info.addProperty("display_item", Objects.requireNonNull(ability.getDisplayItem()).name().toLowerCase(Locale.ROOT));
		if (ability.getDepthsTrigger() != null) {
			info.addProperty("trigger", ability.getDepthsTrigger().mName);
		}
		// why is the trigger private but cooldowns is public? idk just roll with it
		if (ability.mCooldowns != null) {
			JsonArray cooldowns = new JsonArray();
			for (Integer cooldown : ability.mCooldowns) {
				cooldowns.add(cooldown);
			}

			// ok so this sucks. so for skills that only have one cooldown that doesnt change with rarity,
			// some skills use .cooldown(COOLDOWN) and some use .cooldown(CHARM_COOLDOWN, COOLDOWN)
			// and some use .cooldown(COOLDOWN, CHARM_COOLDOWN), which ends up with a situation where mCooldowns
			// can be either 1, 4, or 6 elements long. this is not good for standardization
			// this just repeats the last cooldown until it's up to 6 entries
			while (cooldowns.size() < 6) {
				cooldowns.add(ability.mCooldowns.getLast());
			}

			info.add("cooldowns", cooldowns);
		}

		// Rebirth is the only ability with a description that varies "majorly" between rarities.
		// The last sentence doesn't exist at common rarity, and the last word is "ability" at uncommon
		// and "abilities" at rare and above. Rather than overcomplicating collectDescriptions() for the sake
		// of one ability, this special case is handled separately.
		if (ability.getLinkedSpell() == ClassAbility.REBIRTH) {
			String desc = MessagingUtils.plainText(ability.getDescriptions().get(DepthsAbility.MAX_RARITY - 1)).replace("6", "#{0|1|2|3|4|6}");
			info.addProperty("zenith_description", desc);
			return info;
		}

		List<Component> descriptionList;
		if (isInDepths) {
			// add depths descriptions if it's not a zenith-only tree (prismatic, curse, gift)
			DepthsUtils.setDepthsContentOverride(DepthsContent.DARKEST_DEPTHS);
			descriptionList = ability.rebuildDescriptions().getDescriptions();
			info.addProperty("depths_description", collectDescriptions(descriptionList));
		}

		// rerun with zenith context
		DepthsUtils.setDepthsContentOverride(DepthsContent.CELESTIAL_ZENITH);
		descriptionList = ability.rebuildDescriptions().getDescriptions();
		info.addProperty("zenith_description", collectDescriptions(descriptionList));
		return info;
	}

	private static String collectDescriptions(List<Component> components) {
		List<String> descriptions = components.stream().map(MessagingUtils::plainText).toList();
		if (descriptions.size() == 1) {
			return descriptions.getFirst().replaceAll("XXXXXX", "Twisted");
		}
		List<List<String>> splitDescriptions = descriptions.stream().map(s -> List.of(s.split(" "))).toList();
		StringBuilder builder = new StringBuilder();
		int wordsInCommonDescription = splitDescriptions.getFirst().size();
		if (splitDescriptions.stream().anyMatch(d -> d.size() != wordsInCommonDescription)) {
			MMLog.warning("[Depths Skills API] Skill descriptions have different word counts, defaulting to:");
			MMLog.warning(descriptions.getLast());
			return descriptions.getLast();
		}
		for (int i = 0; i < wordsInCommonDescription; i++) {
			boolean foundDifference = false;
			for (int j = 0; j < descriptions.size(); j++) {
				if (!splitDescriptions.get(j).get(i).equals(splitDescriptions.getFirst().get(i))) {
					foundDifference = true;
				}
			}
			if (foundDifference) {
				// found something different between descriptions
				List<String> words = new ArrayList<>();
				for (int j = 0; j < descriptions.size(); j++) {
					words.add(splitDescriptions.get(j).get(i));
				}

				// attempt to "improve" the condensed description by moving some characters
				// outside of the braces, or detecting plural forms of words
				String suffix = "";

				// move period outside of braces: #{0.|1.} -> #{0|1}.
				if (words.stream().allMatch(word -> word.charAt(word.length() - 1) == '.')) {
					suffix = ".";
					words.replaceAll(s -> s.substring(0, s.length() - 1));
				}

				// if all end with 's', move the s outside (cooldown in seconds)
				if (words.stream().allMatch(word -> word.charAt(word.length() - 1) == 's')) {
					suffix = "s" + suffix;
					words.replaceAll(s -> s.substring(0, s.length() - 1));
				}

				// replace singular/plural words differing between rarities with plural form
				// i.e. #{1|2} #{reroll|rerolls} -> #{1|2} rerolls
				int shortestLength = words.stream().mapToInt(String::length).min().orElse(0);
				String shortest = words.stream().filter(word -> word.length() == shortestLength).findAny().orElse("");
				if (words.stream().allMatch(
					word -> (word.length() == shortestLength && word.equals(shortest))
					|| (word.length() == shortestLength + 1 && word.equals(shortest + "s"))
				)) {
					suffix = shortest + "s" + suffix;
					words.clear();
				}

				words.removeIf(String::isEmpty);

				if (!words.isEmpty()) {
					StringBuilder miniBuilder = new StringBuilder("#{");
					words.forEach(sd -> miniBuilder.append("|").append(sd));
					miniBuilder.append("}");
					miniBuilder.deleteCharAt(2); // extra |
					builder.append(miniBuilder);
				}
				builder.append(suffix).append(" ");
			} else {
				// didn't find anything different, just append the word itself
				builder.append(splitDescriptions.getFirst().get(i)).append(" ");
			}

		}
		builder.deleteCharAt(builder.length() - 1); // extra space
		return builder.toString().replaceAll("XXXXXX", "Twisted");
	}
}
