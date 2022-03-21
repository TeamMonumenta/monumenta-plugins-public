package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.classes.ClassAbility;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

/**
 * The AbilityInfo class contains the small information bits
 * about an ability. This is to keep the information compact and
 * not have a bunch of getters and setters of data that is menial.
 * @author FirelordWeaponry (Fire)
 *
 */
public class AbilityInfo {
	// Ability name as shown in-game
	public @Nullable String mDisplayName;
	// Ability name shorthand (for statistic purposes; no use in-game. Should be the fewest characters that identifies this)
	public @Nullable String mShorthandName;
	// List of descriptions to aid ability selection
	public List<String> mDescriptions = new ArrayList<>();

	// If the ability does not require a scoreboardID and just a classId, leave this as null.
	public @Nullable String mScoreboardId = null;

	public @Nullable ClassAbility mLinkedSpell = null;
	public @Nullable AbilityTrigger mTrigger = null;

	//This is in ticks
	public int mCooldown = 0;

	/*
	 * If this is set to true, methods of the class will be called even when the skill
	 * is still on cooldown. This is needed for some skills that have multiple possible
	 * triggers or need to catch events after the skill has been put on cooldown
	 */
	public boolean mIgnoreCooldown = false;

	public Component getFormattedDescription(int skillLevel) throws IndexOutOfBoundsException {
		String strDescription = mDescriptions.get(skillLevel - 1);
		if (strDescription == null) {
			strDescription = "NULL! Set description properly!";
		}

		String skillHeader;
		skillHeader = "[" + mDisplayName.toUpperCase() + " Level " + skillLevel + "] : ";

		return Component.text("")
			.append(Component.text(skillHeader, NamedTextColor.GREEN, TextDecoration.BOLD))
			.append(Component.text(strDescription, NamedTextColor.YELLOW));
	}

	public Component getFormattedDescriptions() {
		if (mDescriptions.size() == 0) {
			return Component.text("No descriptions found for " + mDisplayName + "!", NamedTextColor.RED);
		}

		//TODO edit to include enhancements
		Component component = Component.text("");
		component = component.append(getFormattedDescription(1));
		for (int skillLevel = 2; skillLevel <= mDescriptions.size(); skillLevel++) {
			component = component.append(Component.newline())
				.append(getFormattedDescription(skillLevel));
		}
		return component;
	}

	/*
	 * Returns null if a hover message could not be created
	 */
	public @Nullable Component getLevelHover(int skillLevel, boolean useShorthand) {
		String hoverableString;
		if (useShorthand) {
			if (mShorthandName == null) {
				return null;
			}
			hoverableString = mShorthandName + skillLevel;
		} else {
			if (mDisplayName == null) {
				return null;
			}
			hoverableString = mDisplayName.toUpperCase() + " Level " + skillLevel;
		}
		return Component.text(hoverableString, NamedTextColor.YELLOW)
			.hoverEvent(getFormattedDescriptions());
	}

	public void sendDescriptions(CommandSender sender) {
		sender.sendMessage(getFormattedDescriptions());
	}

	public JsonObject toJson() {
		JsonObject info = new JsonObject();
		if (mScoreboardId != null) {
			info.addProperty("scoreboardId", mScoreboardId);
		}
		if (mLinkedSpell != null) {
			info.addProperty("name", mLinkedSpell.getName());
		}
		if (mDisplayName != null) {
			info.addProperty("displayName", mDisplayName);
		}
		if (mShorthandName != null) {
			info.addProperty("shortName", mShorthandName);
		}
		if (mDescriptions.size() != 0) {
			JsonArray descriptions = new JsonArray();
			for (String strDescription : mDescriptions) {
				descriptions.add(strDescription);
			}
			info.add("descriptions", descriptions);
		}
		if (mTrigger != null) {
			info.addProperty("trigger", mTrigger.toString());
		}
		info.addProperty("cooldown", mCooldown);

		return info;
	}
}
