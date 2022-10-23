package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
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

public class OrinCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final int[] INSTANCE_LOCATIONS = {20, 22, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42, 47, 48, 49, 50, 51};

	public static class TeleportEntry {
		int mPage;
		int mSlot;
		String mName;
		@Nullable String mScoreboard;
		String mLore;
		int mScoreRequired;
		Material mType;
		String mLeftClick;
		String mRightClick;

		public TeleportEntry(int p, int s, String n, String l, Material t, @Nullable String sc, int sr, String left, String right) {
			mPage = p;
			mSlot = s;
			mName = n;
			mLore = l;
			mType = t;
			mScoreboard = sc;
			mScoreRequired = sr;
			mLeftClick = left;
			mRightClick = right;
		}

		public TeleportEntry(int p, int s, String n, String l, Material t, @Nullable String sc, int sr, String left) {
			mPage = p;
			mSlot = s;
			mName = n;
			mLore = l;
			mType = t;
			mScoreboard = sc;
			mScoreRequired = sr;
			mLeftClick = left;
			mRightClick = "";
		}
	}
	/* Page Info
	 * Page 0: Common for 1-9
	 * Page 1: Region 1
	 * Page 2: Region 2
	 * Page 3: Region 3
	 * Page 4: Plots
	 * Page 5: Default Page (Playerplots, dungeon shards, etc)
	 * Page 10: Common for 11-19
	 * Page 11: Region 1 Instance Bot
	 * Page 12: Region 2 Instance Bot
	 * Page 13: Region 3 Instance Bot
	 * Page 20: Common for 21-29
	 * Page 21: Dungeon Instances
	 */

	private static final ArrayList<TeleportEntry> ORIN_ITEMS = new ArrayList<>();
	private final ArrayList<TeleportEntry> INSTANCE_ITEMS = new ArrayList<>();

	static {
		//R1 Page
		ORIN_ITEMS.add(new TeleportEntry(1, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/sierhaven_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(1, 27, "Player Plot", "Click to teleport!", Material.PLAYER_HEAD, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(1, 12, "Sierhaven", "Left Click to be sorted to a shard, right click to choose the shard.", Material.GREEN_CONCRETE, null, 0, "tp @S -765.5 106.0625 70.5 180 0", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(1, 15, "Mistport", "Left Click to be sorted to a shard, right click to choose the shard.", Material.SAND, "Quest101", 13, "execute as @S run function monumenta:mechanisms/teleporters/tp/sierhaven_to_isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(1, 39, "Galengarde", "Left click to be sorted to a shard, right click to choose a shard.", Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "execute as @S run function monumenta:mechanisms/teleporters/goto/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(1, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));
		ORIN_ITEMS.add(new TeleportEntry(1, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		//R2 Page
		ORIN_ITEMS.add(new TeleportEntry(2, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/mistport_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(2, 27, "Player Plot", "Click to teleport!", Material.PLAYER_HEAD, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(2, 12, "Sierhaven", "Left Click to be sorted to a shard, right click to choose the shard.", Material.GREEN_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/mistport_to_valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(2, 15, "Mistport", "Left Click to be sorted to a shard, right click to choose the shard.", Material.SAND, "Quest101", 13, "tp @S -762.5 70.1 1344.5 180 0", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(2, 39, "Galengarde", "Left click to be sorted to a shard, right click to choose a shard.", Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "execute as @S run function monumenta:mechanisms/teleporters/tp/mistport_to_ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(2, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));
		ORIN_ITEMS.add(new TeleportEntry(2, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		//R3 Page
		ORIN_ITEMS.add(new TeleportEntry(3, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/galengarde_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(3, 27, "Player Plot", "Click to teleport!", Material.PLAYER_HEAD, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(3, 12, "Sierhaven", "Left Click to be sorted to a shard, right click to choose the shard.", Material.GREEN_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/galengarde_to_valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(3, 15, "Mistport", "Left Click to be sorted to a shard, right click to choose the shard.", Material.SAND, "Quest101", 13, "execute as @S run function monumenta:mechanisms/teleporters/tp/galengarde_to_isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(3, 39, "Galengarde", "Left click to be sorted to a shard, right click to choose a shard.", Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "tp @S -303.5 83 -654.5 90 0", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(3, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));
		ORIN_ITEMS.add(new TeleportEntry(3, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		//Plots Page
		ORIN_ITEMS.add(new TeleportEntry(4, 0, "Docks", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "tp @S -2456.0 56.5 1104.0 90 0"));
		ORIN_ITEMS.add(new TeleportEntry(4, 18, "Market", "Click to teleport!", Material.BARREL, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/enter_new_market"));
		ORIN_ITEMS.add(new TeleportEntry(4, 27, "Player Plot", "Click to teleport!", Material.GRASS_BLOCK, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(4, 36, "Guild Plot", "Click to teleport!", Material.YELLOW_BANNER, null, 0, "teleportguild @S"));
		ORIN_ITEMS.add(new TeleportEntry(4, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));
		ORIN_ITEMS.add(new TeleportEntry(4, 12, "Sierhaven", "Left Click to be sorted to a shard, right click to choose the shard.", Material.GREEN_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/plots_to_valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(4, 15, "Mistport", "Left Click to be sorted to a shard, right click to choose the shard.", Material.SAND, "Quest101", 13, "execute as @S run function monumenta:mechanisms/teleporters/tp/plots_to_isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(4, 39, "Galengarde", "Left click to be sorted to a shard, right click to choose a shard.", Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "execute as @S run function monumenta:mechanisms/teleporters/tp/plots_to_ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(4, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));


		ORIN_ITEMS.add(new TeleportEntry(5, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "transferserver plots"));
		ORIN_ITEMS.add(new TeleportEntry(5, 27, "Player Plot", "Click to teleport!", Material.GRASS_BLOCK, "CurrentPlot", 1, "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(5, 12, "Sierhaven", "Left Click to be sorted to a shard, right click to choose the shard.", Material.GREEN_CONCRETE, null, 0, "transferserver valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(5, 15, "Mistport", "Left Click to be sorted to a shard, right click to choose the shard.", Material.SAND, "Quest101", 13, "transferserver isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(5, 39, "Galengarde", "Left click to be sorted to a shard, right click to choose a shard.", Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "transferserver ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(5, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));
		ORIN_ITEMS.add(new TeleportEntry(5, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		//Common 10-19: Instance Bot Choices
		ORIN_ITEMS.add(new TeleportEntry(10, 0, "Back", "Return to the main page.", Material.ARROW, null, 0, "back"));
		ORIN_ITEMS.add(new TeleportEntry(10, 4, "Available Shards", "Choose your shard below.", Material.SCUTE, null, 0, ""));

		//Common 20-29: Dungeon Instances
		ORIN_ITEMS.add(new TeleportEntry(20, 0, "Back", "Return to the main page.", Material.ARROW, null, 0, "back"));
		ORIN_ITEMS.add(new TeleportEntry(20, 9, "Region 1 Dungeons", "Dungeons located with the King's Valley.", Material.GREEN_CONCRETE, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 10, "Region 1 Dungeons", "Dungeons located with the King's Valley.", Material.GREEN_CONCRETE, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 11, "Region 1 Dungeons", "Dungeons located with the King's Valley.", Material.GREEN_CONCRETE, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 12, "Region 2 Dungeons", "Dungeons located with the Celsian Isles.", Material.SAND, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 13, "Region 2 Dungeons", "Dungeons located with the Celsian Isles.", Material.SAND, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 14, "Region 2 Dungeons", "Dungeons located with the Celsian Isles.", Material.SAND, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 15, "Region 3 Dungeons", "Dungeons located with the Architect's Ring.", Material.RED_MUSHROOM_BLOCK, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 16, "Region 3 Dungeons", "Dungeons located with the Architect's Ring.", Material.RED_MUSHROOM_BLOCK, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 17, "Region 3 Dungeons", "Dungeons located with the Architect's Ring.", Material.RED_MUSHROOM_BLOCK, null, 0, "", ""));

		//21: Dungeon Instances
		//Group: R1 Dungeons
		ORIN_ITEMS.add(new TeleportEntry(21, 18, "Labs", "Click to teleport!", Material.GLASS_BOTTLE, "D0Access", 1, "execute as @S run function monumenta:lobbies/send_one/d0"));
		ORIN_ITEMS.add(new TeleportEntry(21, 19, "White", "Click to teleport!", Material.WHITE_WOOL, "D1Access", 1, "execute as @S run function monumenta:lobbies/send_one/d1"));
		ORIN_ITEMS.add(new TeleportEntry(21, 20, "Orange", "Click to teleport!", Material.ORANGE_WOOL, "D2Access", 1, "execute as @S run function monumenta:lobbies/send_one/d2"));
		ORIN_ITEMS.add(new TeleportEntry(21, 27, "Magenta", "Click to teleport!", Material.MAGENTA_WOOL, "D3Access", 1, "execute as @S run function monumenta:lobbies/send_one/d3"));
		ORIN_ITEMS.add(new TeleportEntry(21, 28, "Light Blue", "Click to teleport!", Material.LIGHT_BLUE_WOOL, "D4Access", 1, "execute as @S run function monumenta:lobbies/send_one/d4"));
		ORIN_ITEMS.add(new TeleportEntry(21, 29, "Yellow", "Click to teleport!", Material.YELLOW_WOOL, "D5Access", 1, "execute as @S run function monumenta:lobbies/send_one/d5"));
		ORIN_ITEMS.add(new TeleportEntry(21, 36, "Willows", "Click to teleport!", Material.JUNGLE_LEAVES, "DB1Access", 1, "execute as @S run function monumenta:lobbies/send_one/db1"));
		ORIN_ITEMS.add(new TeleportEntry(21, 37, "Reverie", "Click to teleport!", Material.FIRE_CORAL, "DCAccess", 1, "execute as @S run function monumenta:lobbies/send_one/dc"));
		ORIN_ITEMS.add(new TeleportEntry(21, 38, "Sanctum", "Click to teleport!", Material.GRASS_BLOCK, "DS1Access", 1, "execute as @S run function monumenta:lobbies/send_one/ds1"));

		//Group: R2 Dungeons
		ORIN_ITEMS.add(new TeleportEntry(21, 21, "Lime", "Click to teleport!", Material.LIME_WOOL, "D6Access", 1, "execute as @S run function monumenta:lobbies/send_one/d6"));
		ORIN_ITEMS.add(new TeleportEntry(21, 22, "Pink", "Click to teleport!", Material.PINK_WOOL, "D7Access", 1, "execute as @S run function monumenta:lobbies/send_one/d7"));
		ORIN_ITEMS.add(new TeleportEntry(21, 23, "Gray", "Click to teleport!", Material.GRAY_WOOL, "D8Access", 1, "execute as @S run function monumenta:lobbies/send_one/d8"));
		ORIN_ITEMS.add(new TeleportEntry(21, 30, "Light Gray", "Click to teleport!", Material.LIGHT_GRAY_WOOL, "D9Access", 1, "execute as @S run function monumenta:lobbies/send_one/d9"));
		ORIN_ITEMS.add(new TeleportEntry(21, 31, "Cyan", "Click to teleport!", Material.CYAN_WOOL, "D10Access", 1, "execute as @S run function monumenta:lobbies/send_one/d10"));
		ORIN_ITEMS.add(new TeleportEntry(21, 32, "Purple", "Click to teleport!", Material.PURPLE_WOOL, "D11Access", 1, "execute as @S run function monumenta:lobbies/send_one/d11"));
		ORIN_ITEMS.add(new TeleportEntry(21, 39, "Teal", "Click to teleport!", Material.CYAN_CONCRETE_POWDER, "DTLAccess", 1, "execute as @S run function monumenta:lobbies/send_one/dtl"));
		ORIN_ITEMS.add(new TeleportEntry(21, 40, "Shifting City", "Click to teleport!", Material.PRISMARINE_BRICKS, "DRL2Access", 1, "execute as @S run function monumenta:lobbies/send_one/drl2"));
		ORIN_ITEMS.add(new TeleportEntry(21, 41, "The Fallen Forum", "Click to teleport!", Material.BOOKSHELF, "DFFAccess", 1, "execute as @S run function monumenta:lobbies/send_one/dff", ""));

		//Group: R3 Dungeons
		ORIN_ITEMS.add(new TeleportEntry(21, 24, "Silver Knight's Tomb", "Click to teleport!", Material.DEEPSLATE, "DSKTAccess", 1, "execute as @S run function monumenta:lobbies/send_one/dskt"));
		ORIN_ITEMS.add(new TeleportEntry(21, 25, "Blue", "Click to teleport!", Material.BLUE_WOOL, "D12Access", 1, "execute as @S run function monumenta:lobbies/send_one/d12"));
	}

	private int mCurrentPage;
	private final String mCurrentShard = ServerProperties.getShardName();

	public OrinCustomInventory(Player player, int page) {
		super(player, 54, "Teleportation Choices");
		if (page == -1) {
			if (mCurrentShard.contains("valley")) {
				mCurrentPage = 1;
			} else if (mCurrentShard.contains("isles")) {
				mCurrentPage = 2;
			} else if (mCurrentShard.contains("ring")) {
				mCurrentPage = 3;
			} else if (mCurrentShard.equals("plots")) {
				mCurrentPage = 4;
			} else {
				mCurrentPage = 5;
			}
		} else {
			mCurrentPage = page;
		}

		setLayout(player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		Player player;
		if (event.getWhoClicked() instanceof Player) {
			player = (Player) event.getWhoClicked();
		} else {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory) {
			return;
		}

		int commonPage = (int) Math.floor(mCurrentPage / 10.0) * 10;
		if (clickedItem != null && clickedItem.getType() != FILLER && !event.isShiftClick()) {
			int chosenSlot = event.getSlot();
			for (TeleportEntry item : ORIN_ITEMS) {
				if (item.mSlot == chosenSlot && item.mPage == mCurrentPage) {
					if (event.isLeftClick()) {
						completeCommand(player, item.mLeftClick);
					} else {
						completeCommand(player, item.mRightClick);
					}
				}
				if (item.mSlot == chosenSlot && item.mPage == commonPage) {
					if (event.isLeftClick()) {
						completeCommand(player, item.mLeftClick);
					} else {
						completeCommand(player, item.mRightClick);
					}
				}
			}
			if (mCurrentPage == 11 || mCurrentPage == 12 || mCurrentPage == 13) {
				for (TeleportEntry item : INSTANCE_ITEMS) {
					if (item.mSlot == chosenSlot && item.mPage == mCurrentPage) {
						if (event.isLeftClick()) {
							completeCommand(player, item.mLeftClick);
						} else {
							completeCommand(player, item.mRightClick);
						}
					}
				}
			}
		}
	}

	public Boolean isInternalCommand(String command) {
		return command.equals("exit") || command.startsWith("page") || command.startsWith("instancebot") || command.equals("back");
	}

	public void runInternalCommand(Player player, String cmd) {
		if (cmd.startsWith("page")) {
			mCurrentPage = Integer.parseInt(cmd.split(" ")[1]);
			setLayout(player);
		} else if (cmd.startsWith("exit")) {
			player.closeInventory();
		} else if (cmd.startsWith("instancebot")) {
			String searchTerm = cmd.split(" ")[1];
			if (searchTerm.startsWith("valley")) {
				mCurrentPage = 11;
			} else if (searchTerm.startsWith("isles")) {
				mCurrentPage = 12;
			} else {
				mCurrentPage = 13;
			}
			setLayout(player);
		} else if (cmd.equals("back")) {
			if (mCurrentShard.contains("valley")) {
				mCurrentPage = 1;
			} else if (mCurrentShard.contains("isles")) {
				mCurrentPage = 2;
			} else if (mCurrentShard.contains("ring")) {
				mCurrentPage = 3;
			} else if (mCurrentShard.equals("plots")) {
				mCurrentPage = 4;
			} else {
				mCurrentPage = 5;
			}
			setLayout(player);
		}
	}

	public void completeCommand(Player player, String cmd) {
		if (cmd.isEmpty()) {
			return;
		}
		if (isInternalCommand(cmd)) {
			runInternalCommand(player, cmd);
		} else {
			if (cmd.startsWith("transferserver")) {
				//input format should be "transferserver <shard_name>"
				String[] splitCommand = cmd.split(" ");
				String targetShard = splitCommand[1];

				try {
					/* Note that this API accepts null returnLoc, returnYaw, returnPitch as default current player location */
					MonumentaRedisSyncAPI.sendPlayer(player, targetShard);
				} catch (Exception e) {
					MessagingUtils.sendStackTrace(player, e);
				}
			} else {
				String finalCommand = cmd.replace("@S", player.getName());
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
				player.closeInventory();
			}
		}
	}

	public ItemStack createCustomItem(TeleportEntry location) {
		ItemStack newItem = new ItemStack(location.mType, 1);
		ItemMeta meta = newItem.getItemMeta();
		meta.displayName(Component.text(location.mName, NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false));
		if (!location.mLore.isEmpty()) {
			GUIUtils.splitLoreLine(meta, location.mLore, 30, ChatColor.DARK_PURPLE, true);
		}
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		newItem.setItemMeta(meta);
		return newItem;
	}

	public void setLayout(Player player) {
		mInventory.clear();
		int commonPage = (int) Math.floor(mCurrentPage / 10.0) * 10;
		for (TeleportEntry item : ORIN_ITEMS) {
			if (item.mPage == commonPage) {
				if (item.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired) {
					mInventory.setItem(item.mSlot, createCustomItem(item));
				}
			} //intentionally not else, so overrides can happen
			if (item.mPage == mCurrentPage) {
				if (item.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired) {
					mInventory.setItem(item.mSlot, createCustomItem(item));
				}
			}
		}

		GUIUtils.fillWithFiller(mInventory, FILLER);

		if (mCurrentPage == 11) {
			showInstances(player, "valley");
		} else if (mCurrentPage == 12) {
			showInstances(player, "isles");
		} else if (mCurrentPage == 13) {
			showInstances(player, "ring");
		}
	}

	public void showInstances(Player player, String searchTerm) {
		Set<String> results = null;
		INSTANCE_ITEMS.clear();
		try {
			results = NetworkRelayAPI.getOnlineShardNames();
		} catch (Exception e) {
			e.printStackTrace();
			player.closeInventory();
		}
		int index = 0;
		if (results == null) {
			return;
		}
		results.removeIf(item -> !item.startsWith(searchTerm));
		int page;
		Material itemType;

		if (searchTerm.startsWith("valley")) {
			page = 11;
			itemType = Material.GRASS;
		} else if (searchTerm.startsWith("isles")) {
			page = 12;
			itemType = Material.PUFFERFISH;
		} else {
			itemType = Material.DARK_OAK_SAPLING;
			page = 13;
		}
		ArrayList<String> resultList = new ArrayList<>(results);
		Collections.sort(resultList);
		for (String shard : resultList) {
			String shardName = shard.substring(0, 1).toUpperCase() + shard.substring(1);
			if (index <= INSTANCE_LOCATIONS.length) {
				INSTANCE_ITEMS.add(new TeleportEntry(page, INSTANCE_LOCATIONS[index++], shardName, "Click to teleport!", itemType, null, 0, "transferserver " + shard));
			}
		}

		for (TeleportEntry item : INSTANCE_ITEMS) {
			mInventory.setItem(item.mSlot, createCustomItem(item));
		}
	}
}
