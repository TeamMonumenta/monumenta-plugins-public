package com.playmonumenta.plugins.custominventories;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelveCustomInventory;
import com.playmonumenta.plugins.delves.DelvePreset;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class BountyGui extends Gui {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final int RANGE = 10;

	private static final List<Integer> BOUNTY_L1_LOCATIONS = new ArrayList<>(Arrays.asList(19, 22, 25));
	private static final List<Integer> PRESET_L1_LOCATIONS = new ArrayList<>(Arrays.asList(19, 21, 23, 25));
	private static final List<String> BOUNTY_SCOREBOARDS = new ArrayList<>(Arrays.asList("DailyQuest", "Daily2Quest", "Daily3Quest"));
	private static final List<String> BOUNTY_REWARD_BOARDS = new ArrayList<>(Arrays.asList("DailyReward", "Daily2Reward", "Daily3Reward"));
	private static final List<String> LORE_SCOREBOARDS = new ArrayList<>(Arrays.asList("DailyLoreReq", "Daily2LoreReq", "Daily3LoreReq"));
	private static final List<String> BOUNTY_NPCS = new ArrayList<>(Arrays.asList("King's Herald", "Seer", "The Seanchaidh"));

	public static class BountyData {
		private final String mName;
		private final int mID;
		private final int mLevel;
		private final Material mMaterial;
		private final @Nullable String mScoreboardReq;
		private final int mReqMin;

		BountyData(String name, int id, int level, String material, @Nullable String scoreboard, int minimumVal) {
			mName = name;
			mID = id;
			mLevel = level;
			Material getMat = Material.getMaterial(material);
			if (getMat == null) {
				getMat = Material.ARMOR_STAND;
			}
			mMaterial = getMat;
			if (scoreboard != null) {
				mScoreboardReq = scoreboard;
				mReqMin = minimumVal;
			} else {
				mScoreboardReq = null;
				mReqMin = 0;
			}
		}

		public String getName() {
			return mName;
		}

		public boolean hasBounty(Player player, int region) {
			return ScoreboardUtils.getScoreboardValue(player, BOUNTY_SCOREBOARDS.get(region - 1)).orElse(0) == mID;
		}
	}

	private final List<BountyData> mBounties;
	private final List<BountyData> mBountyChoices = new ArrayList<>();
	private final List<DelvePreset> mPresetChoices = new ArrayList<>();
	private final int mLevel;
	private final int mRegion;
	private int mPresetLevel = -1;

	public BountyGui(Player player, int region, int level) throws Exception {
		super(player, 36, Component.text("Choose your bounty!"));
		mLevel = level;
		mRegion = region;

		mBounties = parseData(region);
		setFiller(FILLER);
	}

	@Override
	protected void setup() {
		setSize(4 * 9);
		setTitle(Component.text("Choose your bounty!"));
		if (getBountyChoice(mPlayer, mRegion, 0) != 0 ||
			    getBountyChoice(mPlayer, mRegion, 1) != 0 ||
			    getBountyChoice(mPlayer, mRegion, 2) != 0) {
			loadFromExisting(mPlayer);
		} else {
			if (mPresetLevel == -1 && mRegion == 3) {
				pickR3DelveLevel();
			} else {
				pickNewBounties(mPlayer);
			}
		}
	}

	private void pickNewBounties(Player player) {
		if (mLevel == 0) {
			Collections.shuffle(mBounties);
			int usedLocations = 0;
			mBountyChoices.clear();
			mPresetChoices.clear();
			if (mRegion == 3 && mPresetLevel > 0) {
				mPresetChoices.addAll(DelvePreset.getRandomPresets(mPresetLevel));
			}
			for (BountyData bounty : mBounties) {
				if (bounty.mScoreboardReq == null ||
					    (ScoreboardUtils.getScoreboardValue(player, bounty.mScoreboardReq).orElse(0) >= bounty.mReqMin)) {
					mBountyChoices.add(bounty);
					usedLocations++;
					if (usedLocations >= BOUNTY_L1_LOCATIONS.size()) {
						break;
					}
				}
			}
			tagThemAll(player);
			setLayout();
		}
	}

	private void pickR3DelveLevel() {
		setTitle(Component.text("Choose your preset level!"));
		if (mLevel == 0) {
			setItem(PRESET_L1_LOCATIONS.get(0), GUIUtils.createBasicItem(
				Material.LANTERN, 1, "None", NamedTextColor.AQUA,
				false, "Delve presets will not be applied.", NamedTextColor.WHITE, 30, true))
				.onLeftClick(() -> {
					mPresetLevel = 0;
					update();
				});
			for (int i = 1; i < PRESET_L1_LOCATIONS.size(); i++) {
				final int level = i;
				Supplier<IntStream> presetPoints = () -> Arrays.stream(DelvePreset.values())
					                                         .filter(preset -> preset.mLevel == level)
					                                         .mapToInt(preset -> preset.mModifiers.entrySet().stream().mapToInt(e -> e.getKey().getPointsPerLevel() * e.getValue()).sum());
				int minPoints = presetPoints.get().min().orElse(0);
				int maxPoints = presetPoints.get().max().orElse(0);
				setItem(PRESET_L1_LOCATIONS.get(i), GUIUtils.createBasicItem(
					Material.SOUL_LANTERN, i, "Level " + i, NamedTextColor.AQUA,
					false, "Delve presets will be rolled from level " + i + ".\n" +
						       "Presets of this level will assign " + (minPoints == maxPoints ? minPoints : minPoints + " to " + maxPoints) + " delve points.", NamedTextColor.WHITE, 30, true))
					.onLeftClick(() -> {
						mPresetLevel = level;
						update();
					});
			}
		}
		createDelveInfoItem();
	}

	private void tagThemAll(Player player) {
		List<Player> playersInRange = PlayerUtils.playersInRange(player.getLocation(), RANGE, true);
		for (int i = 0; i < mBountyChoices.size(); i++) {
			BountyData bounty = mBountyChoices.get(i);
			int bountyTag = bounty.mID * 100;
			if (i < mPresetChoices.size()) {
				DelvePreset preset = mPresetChoices.get(i);
				bountyTag += preset.mId;
			}
			for (Player target : playersInRange) {
				if (getBountyChoice(target, mRegion, i) == 0
					    && ScoreboardUtils.getScoreboardValue(target, BOUNTY_SCOREBOARDS.get(mRegion - 1)).orElse(0) == 0) {
					ScoreboardUtils.setScoreboardValue(target, getBountyChoiceObjective(mRegion, i), bountyTag);
					if (target != player && i == 0) {
						target.sendMessage("Your bounty options for today have been rolled by " + player.getName() + "!");
					}
				}
			}
		}
	}


	private void loadFromExisting(Player player) {
		if (mLevel == 0) {
			mBountyChoices.clear();
			mPresetChoices.clear();
			for (int i = 0; i < 3; i++) {
				int currentValue = getBountyChoice(player, mRegion, i);
				int bountyID = currentValue / 100;
				int presetID = currentValue % 100;
				mBountyChoices.add(mBounties.stream().filter(b -> b.mID == bountyID).findFirst().orElse(null));
				mPresetChoices.add(DelvePreset.getDelvePreset(presetID));
			}
			setLayout();
		}
	}

	private void setLayout() {
		if (mLevel == 0) {
			for (int i = 0; i < BOUNTY_L1_LOCATIONS.size(); i++) {
				final int finalI = i;
				BountyData bounty = mBountyChoices.get(i);
				if (bounty != null) {
					setItem(BOUNTY_L1_LOCATIONS.get(i), GUIUtils.createBasicItem(
						bounty.mMaterial, bounty.mName, NamedTextColor.AQUA,
						false, ((bounty.mLevel != 0) ? "Tier " + bounty.mLevel : "")
							       + (i < mPresetChoices.size() && mPresetChoices.get(i) != null ? (bounty.mLevel != 0 ? "\n" : "") + "This bounty will be a delve, using the delve preset shown below." : ""), NamedTextColor.WHITE))
						.onLeftClick(() -> {
							setBounty(mPlayer, mBountyChoices.get(finalI), finalI < mPresetChoices.size() ? mPresetChoices.get(finalI) : null);
							close();
						});
				}
			}
			for (int i = 0; i < mPresetChoices.size(); i++) {
				final int finalI = i;
				DelvePreset preset = mPresetChoices.get(i);
				if (preset != null) {
					setSize(5 * 9);
					setItem(BOUNTY_L1_LOCATIONS.get(i) + 9, GUIUtils.createBasicItem(
						preset.mDisplayItem, preset.mName, NamedTextColor.DARK_AQUA,
						false, "Delve preset of level " + preset.mLevel + ".\nClick to view delve modifiers in this preset.", NamedTextColor.WHITE))
						.onLeftClick(() -> {
							close();
							new DelveCustomInventory(mPlayer, "ring", false, mPresetChoices.get(finalI)).openInventory(mPlayer, mPlugin);
						});
				}
			}
		}
		createInfoItem();
	}

	private boolean confirmMatchingBounties(Player player, Player target) {
		for (int i = 0; i < 3; i++) {
			if (getBountyChoice(target, mRegion, i) != getBountyChoice(player, mRegion, i)) {
				return false;
			}
		}
		return true;
	}

	private void setBounty(Player player, BountyData bounty, @Nullable DelvePreset preset) {
		// Grab all players to apply the bounty to: must be nearby, not have an active bounty or outstanding reward, and must have the same bounty choices as the selecting player.
		// The result is stored in a list and *then* iterated, as the iteration changes scoreboard values, which would make the confirmMatchingBounties check unsound.
		List<Player> nearbyPlayers = PlayerUtils.playersInRange(player.getLocation(), RANGE, true).stream()
			                             .filter(target -> ScoreboardUtils.getScoreboardValue(target, BOUNTY_SCOREBOARDS.get(mRegion - 1)).orElse(0) == 0
				                                               && ScoreboardUtils.getScoreboardValue(target, BOUNTY_REWARD_BOARDS.get(mRegion - 1)).orElse(0) == 0
				                                               && confirmMatchingBounties(player, target))
			                             .toList();
		for (Player target : nearbyPlayers) {
			if (preset != null) {
				ScoreboardUtils.setScoreboardValue(target, DelvePreset.PRESET_SCOREBOARD, preset.mId);
				DelvesManager.savePlayerData(target, "ring", preset.mModifiers, preset.mId);
			}
			ScoreboardUtils.setScoreboardValue(target, BOUNTY_SCOREBOARDS.get(mRegion - 1), bounty.mID);
			ScoreboardUtils.setScoreboardValue(target, LORE_SCOREBOARDS.get(mRegion - 1), 1);
			ScoreboardUtils.setScoreboardValue(target, getBountyChoiceObjective(mRegion, 0), 0);
			ScoreboardUtils.setScoreboardValue(target, getBountyChoiceObjective(mRegion, 1), 0);
			ScoreboardUtils.setScoreboardValue(target, getBountyChoiceObjective(mRegion, 2), 0);

			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "interactnpc " + target.getName() + " \"" + BOUNTY_NPCS.get(mRegion - 1) + "\"");
			target.sendMessage(Component.text("Your bounty for today is ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, true)
				                   .append(Component.text(bounty.mName, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, true))
				                   .append(Component.text("!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, true)));
			if (preset != null) {
				target.sendMessage(Component.text("Your delve preset ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, true)
					                   .append(Component.text(preset.mName, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, true))
					                   .append(Component.text(" has been automatically selected!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, true)));
			}
		}
	}

	public static List<BountyData> parseData(int region) throws Exception {
		List<BountyData> bounties = new ArrayList<>();
		String bountyContent = FileUtils.readFile(Plugin.getInstance().getDataFolder().getPath() + "/bounties/region" + region + ".json");
		Gson gson = new Gson();
		JsonObject data = gson.fromJson(bountyContent, JsonObject.class);
		JsonArray bountyParse = data.get("pois").getAsJsonArray();
		for (JsonElement poi : bountyParse) {
			JsonObject toParse = poi.getAsJsonObject();
			String bountyName = toParse.get("name").getAsString();
			int bountyID = toParse.get("id").getAsInt();
			int bountyLevel = toParse.get("level").getAsInt();
			String bountyMat = toParse.get("display_item").getAsString();
			if (toParse.get("requirement") != null) {
				JsonObject reqs = toParse.get("requirement").getAsJsonObject();
				String bountyBoard = reqs.get("scoreboard").getAsString();
				int bountyReqMin = reqs.get("minimum").getAsInt();
				bounties.add(new BountyData(bountyName, bountyID,
					bountyLevel, bountyMat, bountyBoard, bountyReqMin));
			} else {
				bounties.add(new BountyData(bountyName, bountyID,
					bountyLevel, bountyMat, null, 0));
			}
		}
		return bounties;
	}

	public void createInfoItem() {
		setItem(4, GUIUtils.createBasicItem(
			Material.SCUTE, "Choose your bounty!", NamedTextColor.AQUA,
			false, "Click the bounty you'd like to complete today.", NamedTextColor.WHITE));
	}

	public void createDelveInfoItem() {
		setItem(4, GUIUtils.createBasicItem(
			Material.SCUTE, "Choose your preset level!", NamedTextColor.AQUA,
			false, "Click the level of the delve preset you want to roll from.", NamedTextColor.WHITE));
	}

	private static String getBountyChoiceObjective(int region, int bountyNum) {
		return "R" + region + "Bounties" + (bountyNum + 1);
	}

	private static int getBountyChoice(Player player, int region, int bountyNum) {
		return ScoreboardUtils.getScoreboardValue(player, getBountyChoiceObjective(region, bountyNum)).orElse(0);
	}

}
