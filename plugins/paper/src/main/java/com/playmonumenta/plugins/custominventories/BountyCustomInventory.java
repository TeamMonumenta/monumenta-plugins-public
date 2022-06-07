package com.playmonumenta.plugins.custominventories;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
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
	private static final List<String> BOUNTY_SCOREBOARDS = new ArrayList<>(Arrays.asList("DailyQuest", "Daily2Quest", "Daily3Quest"));
	private static final List<String> BOUNTY_REWARD_BOARDS = new ArrayList<>(Arrays.asList("DailyReward", "Daily2Reward", "Daily3Reward"));
	private static final List<String> LORE_SCOREBOARDS = new ArrayList<>(Arrays.asList("DailyLoreReq", "Daily2LoreReq", "Daily3LoreReq"));

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
	private final int mLevel;
	private final int mRegion;

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
			pickNewBounties(player);
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
			for (int i = 0; i < BOUNTY_L1_LOCATIONS.size(); i++) {
				if (event.getSlot() == BOUNTY_L1_LOCATIONS.get(i)) {
					setBounty((Player) event.getWhoClicked(), mBountyChoices.get(i));
					event.getWhoClicked().closeInventory();
					return;
				}
			}
		}
	}

	private void pickNewBounties(Player player) {
		if (mLevel == 0) {
			Collections.shuffle(mBounties);
			int usedLocations = 0;
			for (BountyData bounty : mBounties) {
				if (bounty.mScoreboardReq == null ||
					(ScoreboardUtils.getScoreboardValue(player, bounty.mScoreboardReq).orElse(0) >= bounty.mReqMin)) {
					_inventory.setItem(BOUNTY_L1_LOCATIONS.get(usedLocations++), createBasicItem(
						bounty.mMaterial, bounty.mName, NamedTextColor.AQUA,
						false, (bounty.mLevel != 0) ? "Tier " + bounty.mLevel : "", ChatColor.WHITE));
					tagThemAll(player, "R" + mRegion + "Bounties" + usedLocations, bounty.mID * 100);
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
			for (int i = 1; i <= 3; i++) {
				int currentValue = ScoreboardUtils.getScoreboardValue(player, "R" + mRegion + "Bounties" + i).orElse(0);
				savedBounties.add(currentValue / 100);
			}
			for (BountyData bounty : mBounties) {
				if (savedBounties.contains(bounty.mID)) {
					mBountyChoices.add(bounty);
				}
			}
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

	private void setBounty(Player player, BountyData bounty) {
		List<Player> nearbyPlayers = PlayerUtils.playersInRange(player.getLocation(), RANGE, true);
		for (Player target : nearbyPlayers) {
			if (ScoreboardUtils.getScoreboardValue(target, BOUNTY_SCOREBOARDS.get(mRegion - 1)).orElse(0) == 0 &&
				ScoreboardUtils.getScoreboardValue(target, BOUNTY_REWARD_BOARDS.get(mRegion - 1)).orElse(0) == 0 &&
				confirmMatchingBounties(player, target)) {

				ScoreboardUtils.setScoreboardValue(target, BOUNTY_SCOREBOARDS.get(mRegion - 1), bounty.mID);
				ScoreboardUtils.setScoreboardValue(target, LORE_SCOREBOARDS.get(mRegion - 1), 1);
				ScoreboardUtils.setScoreboardValue(target, "R" + mRegion + "Bounties" + 1, 0);
				ScoreboardUtils.setScoreboardValue(target, "R" + mRegion + "Bounties" + 2, 0);
				ScoreboardUtils.setScoreboardValue(target, "R" + mRegion + "Bounties" + 3, 0);
				target.sendMessage("Your bounty for today is " + ChatColor.AQUA + bounty.mName + ChatColor.WHITE + "!");
			}
		}
	}

	public ItemStack createBasicItem(Material mat, String name, NamedTextColor nameColor, boolean nameBold, String desc, ChatColor loreColor) {
		ItemStack item = new ItemStack(mat, 1);
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

	public void fillEmpty() {
		for (int i = 0; i < 36; i++) {
			if (_inventory.getItem(i) == null) {
				_inventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
	}
}
