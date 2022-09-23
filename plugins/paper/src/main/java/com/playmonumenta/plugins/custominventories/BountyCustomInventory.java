package com.playmonumenta.plugins.custominventories;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelveCustomInventory;
import com.playmonumenta.plugins.delves.DelvePreset;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class BountyCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final int RANGE = 10;

	private static final List<Integer> BOUNTY_L1_LOCATIONS = new ArrayList<>(Arrays.asList(19, 22, 25));
	private static final List<Integer> PRESET_L1_LOCATIONS = new ArrayList<>(Arrays.asList(19, 21, 23, 25));
	private static final List<String> BOUNTY_SCOREBOARDS = new ArrayList<>(Arrays.asList("DailyQuest", "Daily2Quest", "Daily3Quest"));
	private static final List<String> BOUNTY_REWARD_BOARDS = new ArrayList<>(Arrays.asList("DailyReward", "Daily2Reward", "Daily3Reward"));
	private static final List<String> LORE_SCOREBOARDS = new ArrayList<>(Arrays.asList("DailyLoreReq", "Daily2LoreReq", "Daily3LoreReq"));
	private static final List<String> BOUNTY_NPCS = new ArrayList<>(Arrays.asList("King's Herald", "Seer", "?"));

	//Beyond creating this file, you also need a command, set up within
	//CustomInventoriesCommands.java in this folder.
	static class BountyData {
		String mName;
		int mID;
		int mLevel;
		Material mMaterial;
		String mScoreboardReq = "";
		int mReqMin = 0;

		BountyData(String name, int id, int level, String material, String scoreboard, int minimumVal) {
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
			}
		}
	}

	private final List<BountyData> mBounties = new ArrayList<>();
	private final List<BountyData> mBountyChoices = new ArrayList<>();
	private final List<DelvePreset> mPresetChoices = new ArrayList<>();
	private final int mLevel;
	private final int mRegion;
	private int mPresetLevel = -1;

	public BountyCustomInventory(Player player, int region, int level) throws Exception {
		//super creates the GUI with arguments of player to open for, slots in GUI,
		//and the name of the container (top line in the chest)
		super(player, 36, "Choose your bounty!");

		//Main setup thread, create the first page of the GUI that loads in here.
		for (int i = 0; i < 36; i++) {
			_inventory.setItem(i, new ItemStack(FILLER, 1));
		}
		mLevel = level;
		mRegion = region;
		parseData();
		if (ScoreboardUtils.getScoreboardValue(player, "R" + region + "Bounties" + 1).orElse(0) != 0 ||
			ScoreboardUtils.getScoreboardValue(player, "R" + region + "Bounties" + 2).orElse(0) != 0 ||
			ScoreboardUtils.getScoreboardValue(player, "R" + region + "Bounties" + 3).orElse(0) != 0) {
			loadFromExisting(player);
		} else {
			if (mPresetLevel == -1 && region == 3) {
				pickR3DelveLevel();
			} else {
				pickNewBounties(player);
			}
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		//Always cancel at the start if you want to avoid item removal
		event.setCancelled(true);
		//Check to make sure they clicked the GUI, didn't shift click, and
		//did not click the filler item
		if (event.getClickedInventory() != _inventory
			|| event.getCurrentItem() == null
			|| event.getCurrentItem().getType() == FILLER
			|| event.isShiftClick()) {
			return;
		}

		if (mLevel == 0) {
			for (int i = 0; i < PRESET_L1_LOCATIONS.size(); i++) {
				if (event.getSlot() == PRESET_L1_LOCATIONS.get(i)) {
					if (mPresetLevel == -1) {
						mPresetLevel = i;
						for (int j = 0; j < PRESET_L1_LOCATIONS.size(); j++) {
							_inventory.setItem(PRESET_L1_LOCATIONS.get(j), new ItemStack(FILLER, 1));
						}
						pickNewBounties((Player) event.getWhoClicked());
						return;
					}
				}
			}
			for (int i = 0; i < BOUNTY_L1_LOCATIONS.size(); i++) {
				if (event.getSlot() == BOUNTY_L1_LOCATIONS.get(i)) {
					setBounty((Player) event.getWhoClicked(), mBountyChoices.get(i), mPresetChoices.isEmpty() ? null : mPresetChoices.get(i));
					if (!mPresetChoices.isEmpty()) {
						DelvesManager.savePlayerData((Player) event.getWhoClicked(), "ring", mPresetChoices.get(i).mModifiers);
					}
					event.getWhoClicked().closeInventory();
					return;
				} else if (event.getSlot() == BOUNTY_L1_LOCATIONS.get(i) + 9) {
					this.close();
					new DelveCustomInventory((Player) event.getWhoClicked(), "ring", false, mPresetChoices.get(i)).openInventory((Player) event.getWhoClicked(), getPlugin());
				}
			}
		}
	}

	private void pickNewBounties(Player player) {
		if (mLevel == 0) {
			Collections.shuffle(mBounties);
			int usedLocations = 0;
			List<DelvePreset> presets = new ArrayList<>();
			if (mRegion == 3 && mPresetLevel > 0) {
				presets.addAll(DelvePreset.getRandomPresets(mPresetLevel));
			}
			for (BountyData bounty : mBounties) {
				if (bounty.mScoreboardReq == null ||
					(ScoreboardUtils.getScoreboardValue(player, bounty.mScoreboardReq).orElse(0) >= bounty.mReqMin)) {
					int bountyTag = bounty.mID * 100;
					if (mRegion == 3 && mPresetLevel > 0) {
						DelvePreset preset = presets.get(usedLocations);
						bountyTag += preset.mId;
						mPresetChoices.add(preset);
					}
					usedLocations++;
					tagThemAll(player, "R" + mRegion + "Bounties" + usedLocations, bountyTag);
					mBountyChoices.add(bounty);
				}
				if (usedLocations >= BOUNTY_L1_LOCATIONS.size()) {
					break;
				}
			}
			for (Player target : PlayerUtils.otherPlayersInRange(player, RANGE, true)) {
				if (confirmMatchingBounties(player, target)) {
					target.sendMessage("Your bounty options for today have been rolled by " + player.getName() + "!");
				}
			}
			setLayout();
		}
	}

	private void pickR3DelveLevel() {
		_inventory.setItem(PRESET_L1_LOCATIONS.get(0), createBasicItem(
			Material.LANTERN, 1, "None", NamedTextColor.AQUA,
			false, "Delve presets will not be applied.", ChatColor.WHITE));
		for (int i = 1; i < PRESET_L1_LOCATIONS.size(); i++) {
			_inventory.setItem(PRESET_L1_LOCATIONS.get(i), createBasicItem(
				Material.SOUL_LANTERN, i, "Level " + i, NamedTextColor.AQUA,
				false, "Delve presets will be rolled from level " + i + ".", ChatColor.WHITE));
		}
		createDelveInfoItem();
		fillEmpty();
	}

	private void tagThemAll(Player player, String objective, int score) {
		List<Player> playersInRange = PlayerUtils.playersInRange(player.getLocation(), RANGE, true);
		for (Player target : playersInRange) {
			if (ScoreboardUtils.getScoreboardValue(target, objective).orElse(0) == 0) {
				ScoreboardUtils.setScoreboardValue(target, objective, score);
			}
		}
	}


	private void loadFromExisting(Player player) {
		if (mLevel == 0) {
			List<Integer> savedBounties = new ArrayList<>();
			List<Integer> savedPresets = new ArrayList<>();
			BountyData[] orderedBounties = new BountyData[3];
			DelvePreset[] orderedPresets = new DelvePreset[3];
			for (int i = 1; i <= 3; i++) {
				int currentValue = ScoreboardUtils.getScoreboardValue(player, "R" + mRegion + "Bounties" + i).orElse(0);
				savedBounties.add(currentValue / 100);
				savedPresets.add(currentValue % 100);
			}
			for (BountyData bounty : mBounties) {
				for (int i = 0; i < savedBounties.size(); i++) {
					if (savedBounties.get(i) == bounty.mID) {
						orderedBounties[i] = bounty;
					}
				}
			}
			for (DelvePreset preset : DelvePreset.values()) {
				for (int i = 0; i < savedPresets.size(); i++) {
					if (savedPresets.get(i) == preset.mId) {
						orderedPresets[i] = preset;
					}
				}
			}
			mBountyChoices.addAll(Arrays.asList(orderedBounties));
			mPresetChoices.addAll(Arrays.asList(orderedPresets));
			setLayout();
		}
	}

	private void setLayout() {
		if (mLevel == 0) {
			for (int i = 0; i < BOUNTY_L1_LOCATIONS.size(); i++) {
				BountyData bounty = mBountyChoices.get(i);
				if (bounty != null) {
					_inventory.setItem(BOUNTY_L1_LOCATIONS.get(i), createBasicItem(
						bounty.mMaterial, bounty.mName, NamedTextColor.AQUA,
						false, (bounty.mLevel != 0) ? "Tier " + bounty.mLevel : "", ChatColor.WHITE));
				}
			}
			for (int i = 0; i < mPresetChoices.size(); i++) {
				DelvePreset preset = mPresetChoices.get(i);
				if (preset != null) {
					_inventory.setItem(BOUNTY_L1_LOCATIONS.get(i) + 9, createBasicItem(
						preset.mDisplayItem, preset.mName, NamedTextColor.AQUA,
						false, (preset.mLevel != 0) ? "Tier " + preset.mLevel : "", ChatColor.WHITE));
				}
			}
		}
		createInfoItem();
		fillEmpty();
	}

	private boolean confirmMatchingBounties(Player player, Player target) {
		for (int i = 1; i <= 3; i++) {
			int playerScore = ScoreboardUtils.getScoreboardValue(target, "R" + mRegion + "Bounties" + i).orElse(0);
			int targetScore = ScoreboardUtils.getScoreboardValue(player, "R" + mRegion + "Bounties" + i).orElse(0);
			if (playerScore != targetScore) {
				return false;
			}
		}
		return true;
	}

	private void setBounty(Player player, BountyData bounty, @Nullable DelvePreset preset) {
		List<Player> nearbyPlayers = PlayerUtils.playersInRange(player.getLocation(), RANGE, true);
		for (Player target : nearbyPlayers) {
			if (ScoreboardUtils.getScoreboardValue(target, BOUNTY_SCOREBOARDS.get(mRegion - 1)).orElse(0) == 0 &&
				ScoreboardUtils.getScoreboardValue(target, BOUNTY_REWARD_BOARDS.get(mRegion - 1)).orElse(0) == 0 &&
				confirmMatchingBounties(player, target)) {
				if (preset != null) {
					ScoreboardUtils.setScoreboardValue(target, DelvePreset.PRESET_SCOREBOARD, preset.mId);
				}
				ScoreboardUtils.setScoreboardValue(target, BOUNTY_SCOREBOARDS.get(mRegion - 1), bounty.mID);
				ScoreboardUtils.setScoreboardValue(target, LORE_SCOREBOARDS.get(mRegion - 1), 1);
				ScoreboardUtils.setScoreboardValue(target, "R" + mRegion + "Bounties" + 1, 0);
				ScoreboardUtils.setScoreboardValue(target, "R" + mRegion + "Bounties" + 2, 0);
				ScoreboardUtils.setScoreboardValue(target, "R" + mRegion + "Bounties" + 3, 0);
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
	}

	public ItemStack createBasicItem(Material mat, String name, NamedTextColor nameColor, boolean nameBold, String desc, ChatColor loreColor) {
		return createBasicItem(mat, 1, name, nameColor, nameBold, desc, loreColor);
	}

	public ItemStack createBasicItem(Material mat, int amt, String name, NamedTextColor nameColor, boolean nameBold, String desc, ChatColor loreColor) {
		ItemStack item = new ItemStack(mat, amt);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name, nameColor)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, nameBold));
		GUIUtils.splitLoreLine(meta, desc, 30, loreColor, true);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		return item;
	}

	private void parseData() throws Exception {
		String bountyContent = FileUtils.readFile(Plugin.getInstance().getDataFolder().getPath() + "/bounties/region" + mRegion + ".json");
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
				mBounties.add(new BountyData(bountyName, bountyID,
					bountyLevel, bountyMat, bountyBoard, bountyReqMin));
			} else {
				mBounties.add(new BountyData(bountyName, bountyID,
					bountyLevel, bountyMat, null, 0));
			}
		}
	}

	public void createInfoItem() {
		_inventory.setItem(4, createBasicItem(
			Material.SCUTE, "Choose your bounty!", NamedTextColor.AQUA,
			false, "Click the bounty you'd like to complete today.", ChatColor.WHITE));
	}

	public void createDelveInfoItem() {
		_inventory.setItem(4, createBasicItem(
			Material.SCUTE, "Choose your preset level!", NamedTextColor.AQUA,
			false, "Click the level of the delve preset you want to roll from.", ChatColor.WHITE));
	}

	public void fillEmpty() {
		for (int i = 0; i < 36; i++) {
			if (_inventory.getItem(i) == null) {
				_inventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
	}
}
