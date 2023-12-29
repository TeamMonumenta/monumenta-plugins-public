package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.delves.DelvesModifier;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class WeeklyMission {
	//Which week of the pass the mission is active
	public int mWeek;
	//XP granted for completing the mission
	public int mMP;
	//Is bonus MP
	public boolean mIsBonus = false;
	//Type of mission - content, kills, etc
	public @Nullable WeeklyMissionType mType;
	//Times needed to do the thing (content clears, kills, room number, etc.)
	//This is what is tracked on the scoreboard
	public int mAmount;

	//Description shown in GUI
	public @Nullable String mDescription;

	//Unique fields for specific missions
	//Which piece of content to clear (matters for CONTENT or DISTANCE types)
	public @Nullable List<MonumentaContent> mContent;
	//Delve mission fields
	public @Nullable List<DelvesModifier> mDelveModifiers;
	public int mRotatingModifiersAmount;
	public int mModifierRank;
	public int mDelvePoints;
	//Region of content to clear (includes dungeon, strike, boss)
	public int mRegion;

	public WeeklyMission(CommandSender sender,
	                     String startDateStr,
						 String passName,
	                     JsonElement missionElement,
	                     boolean showWarnings) {
		JsonObject toParse = missionElement.getAsJsonObject();

		// Required fields
		String missionTypeStr = toParse.get("type").getAsString();
		mType = WeeklyMissionType.getMissionTypeSelection(missionTypeStr);
		if (mType == null && showWarnings) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ passName + ": No such mission type " + missionTypeStr, NamedTextColor.RED)
				.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
		}
		mWeek = toParse.get("week").getAsInt();
		if (mWeek <= 0 && showWarnings) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ passName + ": Mission week is <= 0: " + mWeek, NamedTextColor.RED)
				.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
		}
		mMP = toParse.get("mp").getAsInt();
		if (mMP <= 0 && showWarnings) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ passName + ": Mission MP is <= 0: " + mMP, NamedTextColor.RED)
				.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
		}
		if (toParse.get("is_bonus") instanceof JsonPrimitive bonusPrimitive
			&& bonusPrimitive.isBoolean()
			&& bonusPrimitive.getAsBoolean()) {
			mIsBonus = true;
		}
		mAmount = toParse.get("amount").getAsInt();
		if (mAmount <= 0 && showWarnings) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ passName + ": Mission Amount is <= 0: " + mAmount, NamedTextColor.RED)
				.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
		}
		mDescription = toParse.get("description").getAsString();

		// Optional fields

		if (toParse.get("content") != null) {
			JsonArray content = toParse.get("content").getAsJsonArray();
			List<MonumentaContent> contentList = new ArrayList<>();
			for (JsonElement con : content) {
				String contentStr = con.getAsString();
				MonumentaContent monumentaContent = MonumentaContent.getContentSelection(contentStr);
				if (monumentaContent == null) {
					if (showWarnings) {
						sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
								+ " " + passName + ": No such content " + contentStr, NamedTextColor.RED)
							.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
					}
					continue;
				}
				contentList.add(monumentaContent);
			}
			mContent = contentList;
		}
		if (toParse.get("region") != null) {
			mRegion = toParse.get("region").getAsInt();
			if (showWarnings && !MonumentaContent.ALL_CONTENT_REGION_INDEXES.contains(mRegion)) {
				sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
							+ " " + passName + ": Region not used in Monumenta content: " + mRegion,
						NamedTextColor.RED)
					.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
			}
		}
		if (toParse.get("delvepoints") != null) {
			mDelvePoints = toParse.get("delvepoints").getAsInt();
		}
		if (toParse.get("modifierrank") != null) {
			mModifierRank = toParse.get("modifierrank").getAsInt();
		}
		if (toParse.get("rotatingamount") != null) {
			mRotatingModifiersAmount = toParse.get("rotatingamount").getAsInt();
		}
		if (toParse.get("delvemodifier") != null) {
			// This compares using integers - in json need to list the number of the modifier, not the name!
			if (toParse.get("delvemodifier") instanceof JsonArray mods) {
				List<DelvesModifier> modList = new ArrayList<>();
				for (JsonElement mod : mods) {
					JsonPrimitive modPrimitive = mod.getAsJsonPrimitive();
					if (modPrimitive.isString()) {
						String modName = modPrimitive.getAsString();
						DelvesModifier modifier = DelvesModifier.fromName(modName);
						if (modifier != null) {
							modList.add(modifier);
						} else if (showWarnings) {
							sender.sendMessage(Component.text("[SeasonPass] loadMissions for "
										+ startDateStr + " " + passName + ": No such modifier " + modName,
									NamedTextColor.RED)
								.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
						}
					} else if (showWarnings) {
						sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
								+ " " + passName + ": Modifier ID is not string " + mod, NamedTextColor.RED)
							.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
					}
				}
				mDelveModifiers = modList;
			} else {
				JsonPrimitive modPrimitive = toParse.get("delvemodifier").getAsJsonPrimitive();
				if (modPrimitive.isString()) {
					String modName = modPrimitive.getAsString();
					DelvesModifier modifier = DelvesModifier.fromName(modName);
					if (modifier != null) {
						List<DelvesModifier> modList = new ArrayList<>();
						modList.add(modifier);
						mDelveModifiers = modList;
					} else if (showWarnings) {
						sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
								+ " " + passName + ": No such modifier " + modName, NamedTextColor.RED)
							.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
					}
				} else if (showWarnings) {
					sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
							+ " " + passName + ": Modifier ID is not string " + modPrimitive, NamedTextColor.RED)
						.hoverEvent(Component.text(missionElement.toString(), NamedTextColor.RED)));
				}
			}
		}
	}

}
