package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.delves.DelvesModifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public abstract class Mission {
	// Dummy internal value to uniquely identify this mission
	private final UUID mFingerprint = UUID.randomUUID();

	// XP granted for completing the mission
	public int mMP;
	// Is bonus MP
	public boolean mIsBonus = false;
	// Type of mission - content, kills, etc
	public @Nullable MissionType mType;
	// Times needed to do the thing (content clears, kills, room number, etc.)
	// This is what is tracked on the scoreboard
	public int mAmount;

	// Description shown in GUI
	public @Nullable String mDescription;

	// Unique fields for specific missions
	// Which piece of content to clear (matters for CONTENT or DISTANCE types)
	public @Nullable List<MonumentaContent> mContent;
	// Delve mission fields
	public @Nullable List<DelvesModifier> mDelveModifiers;
	public int mRotatingModifiersAmount;
	public int mModifierRank;
	public int mDelvePoints;
	public int mAscension;
	// Region of content to clear (includes dungeon, strike, boss)
	public int mRegion;

	public static Mission loadMission(CommandSender sender,
	                                  String startDateStr,
	                                  String passName,
	                                  JsonObject missionObject,
	                                  boolean showWarnings) throws NullPointerException {
		if (missionObject.has("week")) {
			return new WeeklyMission(sender, startDateStr, passName, missionObject, showWarnings);
		} else {
			return new LongMission(sender, startDateStr, passName, missionObject, showWarnings);
		}
	}

	protected Mission(CommandSender sender,
	               String startDateStr,
	               String passName,
	               JsonObject missionObject,
	               boolean showWarnings) {
		// Required fields
		JsonElement missionTypeJson = missionObject.get("type");
		if (missionTypeJson == null) {
			mType = null;
			if (showWarnings) {
				sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
						+ passName + ": Mission type not set", NamedTextColor.RED)
					.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
			}
		} else {
			String missionTypeStr = missionTypeJson.getAsString();
			mType = MissionType.getMissionTypeSelection(missionTypeStr);
			if (mType == null && showWarnings) {
				sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
						+ passName + ": No such mission type " + missionTypeStr, NamedTextColor.RED)
					.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
			}
		}
		mMP = missionObject.get("mp").getAsInt();
		if (mMP <= 0 && showWarnings) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ passName + ": Mission MP is <= 0: " + mMP, NamedTextColor.RED)
				.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
		}
		if (missionObject.get("is_bonus") instanceof JsonPrimitive bonusPrimitive
			&& bonusPrimitive.isBoolean()
			&& bonusPrimitive.getAsBoolean()) {
			mIsBonus = true;
		}
		mAmount = missionObject.get("amount").getAsInt();
		if (mAmount <= 0 && showWarnings) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ passName + ": Mission Amount is <= 0: " + mAmount, NamedTextColor.RED)
				.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
		}
		mDescription = missionObject.get("description").getAsString();

		// Optional fields

		if (missionObject.get("content") != null) {
			JsonArray content = missionObject.get("content").getAsJsonArray();
			List<MonumentaContent> contentList = new ArrayList<>();
			for (JsonElement con : content) {
				String contentStr = con.getAsString();
				MonumentaContent monumentaContent = MonumentaContent.getContentSelection(contentStr);
				if (monumentaContent == null) {
					if (showWarnings) {
						sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
								+ " " + passName + ": No such content " + contentStr, NamedTextColor.RED)
							.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
					}
					continue;
				}
				contentList.add(monumentaContent);
			}
			mContent = contentList;
		}
		if (missionObject.get("region") != null) {
			mRegion = missionObject.get("region").getAsInt();
			if (showWarnings && !MonumentaContent.ALL_CONTENT_REGION_INDEXES.contains(mRegion)) {
				sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
							+ " " + passName + ": Region not used in Monumenta content: " + mRegion,
						NamedTextColor.RED)
					.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
			}
		}
		if (missionObject.get("delvepoints") != null) {
			mDelvePoints = missionObject.get("delvepoints").getAsInt();
		}
		if (missionObject.get("modifierrank") != null) {
			mModifierRank = missionObject.get("modifierrank").getAsInt();
		}
		if (missionObject.get("rotatingamount") != null) {
			mRotatingModifiersAmount = missionObject.get("rotatingamount").getAsInt();
		}
		if (missionObject.get("delvemodifier") != null) {
			// This compares using integers - in json need to list the number of the modifier, not the name!
			if (missionObject.get("delvemodifier") instanceof JsonArray mods) {
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
								.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
						}
					} else if (showWarnings) {
						sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
								+ " " + passName + ": Modifier ID is not string " + mod, NamedTextColor.RED)
							.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
					}
				}
				mDelveModifiers = modList;
			} else {
				JsonPrimitive modPrimitive = missionObject.get("delvemodifier").getAsJsonPrimitive();
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
							.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
					}
				} else if (showWarnings) {
					sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr
							+ " " + passName + ": Modifier ID is not string " + modPrimitive, NamedTextColor.RED)
						.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
				}
			}
		}
		JsonElement ascensionJson = missionObject.get("ascension");
		if (ascensionJson instanceof JsonPrimitive ascensionPrimitive && ascensionPrimitive.isNumber()) {
			mAscension = ascensionPrimitive.getAsInt();
		}
	}

	public abstract int firstWeek();

	public abstract int lastWeek();

	public boolean isActive(int week) {
		return firstWeek() <= week && week <= lastWeek();
	}

	@Override
	public int hashCode() {
		return mFingerprint.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Mission other)) {
			return false;
		}

		return mFingerprint.equals(other.mFingerprint);
	}
}
