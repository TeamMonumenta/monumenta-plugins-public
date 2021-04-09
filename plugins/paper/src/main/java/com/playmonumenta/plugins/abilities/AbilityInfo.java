package com.playmonumenta.plugins.abilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.classes.Spells;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

/**
 * The AbilityInfo class contains the small information bits
 * about an ability. This is to keep the information compact and
 * not have a bunch of getters and setters of data that is menial.
 * @author FirelordWeaponry (Fire)
 *
 */
public class AbilityInfo {
	// Ability name as shown in-game
	public String mDisplayName;
	// Ability name shorthand (for statistic purposes; no use in-game. Should be the fewest characters that identifies this)
	public String mShorthandName;
	// List of descriptions to aid ability selection
	public List<String> mDescriptions = new ArrayList<>();

	// If the ability does not require a scoreboardID and just a classId, leave this as null.
	public String mScoreboardId = null;

	public Spells mLinkedSpell = null;
	public AbilityTrigger mTrigger = null;

	//This is in ticks
	public int mCooldown = 0;

	/*
	 * If this is set to true, methods of the class will be called even when the skill
	 * is still on cooldown. This is needed for some skills that have multiple possible
	 * triggers or need to catch events after the skill has been put on cooldown
	 */
	public boolean mIgnoreCooldown = false;

	/*
	 * If this is set to true, the LivingEntityDamagedByPlayerEvent will be allowed
	 * to trigger multiple times per tick. Be very careful with this, as it can lead
	 * to infinite damage loops if you aren't careful.
	 */
	public boolean mIgnoreTriggerCap = false;

	public ComponentBuilder getFormattedDescription(int skillLevel) throws IndexOutOfBoundsException {
		String strDescription = mDescriptions.get(skillLevel - 1);
		if (strDescription == null) {
			strDescription = "NULL! Set description properly!";
		}

		String skillHeader;
		skillHeader = "[" + mDisplayName.toUpperCase() + " Level " + skillLevel + "] : ";

		return new ComponentBuilder(skillHeader).color(ChatColor.GREEN).bold(true)
		           .append(strDescription).color(ChatColor.GREEN).bold(false);
	}

	public ComponentBuilder getFormattedDescriptions() {
		if (mDescriptions.size() == 0) {
			return new ComponentBuilder("No descriptions found for " + mDisplayName + "!").color(ChatColor.RED);
		}

		ComponentBuilder componentBuilder = new ComponentBuilder(getFormattedDescription(1));
		for (int skillLevel = 2; skillLevel <= mDescriptions.size(); skillLevel++) {
			componentBuilder.append("\n");
			componentBuilder.append(getFormattedDescription(skillLevel).create());
		}
		return componentBuilder;
	}

	/*
	 * Returns null if a hover message could not be created
	 */
	public ComponentBuilder getLevelHover(int skillLevel, boolean useShorthand) {
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
		ComponentBuilder componentBuilder = new ComponentBuilder(hoverableString).color(ChatColor.YELLOW);

		HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, getFormattedDescriptions().create());

		return componentBuilder.event(hover);
	}

	public void sendDescriptions(CommandSender sender) {
		sender.sendMessage(getFormattedDescriptions().create());
	}

	public JsonObject getAsJsonObject() {
		return getAsJsonObject(false);
	}

	public JsonObject getAsJsonObject(boolean fullDetails) {
		JsonObject info = new JsonObject();
		if (mScoreboardId != null) {
			info.addProperty("scoreboardId", mScoreboardId);
		}
		if (mLinkedSpell != null) {
			info.addProperty("name", mLinkedSpell.getName());
		}
		if (fullDetails) {
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
		}
		if (mTrigger != null) {
			info.addProperty("trigger", mTrigger.toString());
		}
		info.addProperty("cooldown", mCooldown);

		return info;
	}
}
