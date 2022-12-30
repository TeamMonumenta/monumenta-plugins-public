package com.playmonumenta.plugins.plots;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import com.playmonumenta.structures.StructuresAPI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class PlotBorderCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

	/*
	 * Pages explanation: Page x0 of the x0-x9 set is the default landing for the gui.
	 * Pages 0-9: Plots
	 * Pages 10-19: Region 1
	 * Pages 20-29: Region 2
	 * Pages 50-on: Common pages
	 */
	private enum PermType {
		SCOREBOARD, COSMETIC_ITEM
	}

	public static class TeleportEntry {
		int mPage;
		int mSlot;
		String mName;
		String mLore;
		PermType mPermType;
		@Nullable String mCosmeticString;
		@Nullable String mScoreboard;
		int mScoreRequired;
		Material mType;
		String mLeftClick;

		public TeleportEntry(int page, int slot, String name, String lore, Material type, @Nullable String scoreboard, int scoreRequired, String leftClick) {
			mPage = page;
			mSlot = slot;
			mName = name;
			mLore = lore;
			mType = type;
			mPermType = PermType.SCOREBOARD;
			mScoreboard = scoreboard;
			mScoreRequired = scoreRequired;
			mLeftClick = leftClick;
		}

		public TeleportEntry(int page, int slot, String name, String lore, Material type, String cosmetic, String leftClick) {
			mPage = page;
			mSlot = slot;
			mName = name;
			mLore = lore;
			mType = type;
			mPermType = PermType.COSMETIC_ITEM;
			mCosmeticString = cosmetic;
			mLeftClick = leftClick;
		}
	}
	/* Page Info
	 * Page 0: Common for 1-9
	 * Page 1: Region 1
	 * Page 2: Region 2
	 * Page 3: Plots
	 * Page 10: Common for 11-19
	 * Page 11: Region Instance Choice
	 */


	private static final ArrayList<TeleportEntry> BORDER_ITEMS = new ArrayList<>();

	static {
		BORDER_ITEMS.add(new TeleportEntry(0, 47, "Base Choices", "Click to view the plot borders with no requirements.", Material.GRASS_BLOCK, null, 0, "page 1"));
		BORDER_ITEMS.add(new TeleportEntry(0, 49, "Unlockable Choices", "Click to view plot borders locked behind completion of content.", Material.IRON_INGOT, null, 0, "page 2"));
		BORDER_ITEMS.add(new TeleportEntry(0, 51, "Patreon Choices", "Click to view options only available to Patrons.", Material.GOLD_INGOT, Constants.Objectives.PATREON_DOLLARS, Constants.PATREON_TIER_2, "page 3"));


		BORDER_ITEMS.add(new TeleportEntry(1, 20, "Narsen Village", "A small town of Narsen citizens, bearing some resemblance to the old plots world.", Material.LIGHT_BLUE_CONCRETE, null, 0, "narsen_village"));
		BORDER_ITEMS.add(new TeleportEntry(1, 21, "King's Valley Jungle", "A plot nestled in the jungles of the King's Valley.", Material.GREEN_CONCRETE, null, 0, "kings_valley_jungle"));
		BORDER_ITEMS.add(new TeleportEntry(1, 23, "Flat Grass Field", "A flat and boring field of grass.", Material.GRASS_BLOCK, null, 0, "flatgrass"));
		BORDER_ITEMS.add(new TeleportEntry(1, 24, "Void", "Nothing to see here.", Material.BLACK_STAINED_GLASS, null, 0, "void"));


		BORDER_ITEMS.add(new TeleportEntry(2, 20, "Celsian Isles: Chillwind", "Located in the frosty forests of Chillwind.", Material.SNOW_BLOCK, "Quest101", 13, "celsian_isles_chillwind"));
		BORDER_ITEMS.add(new TeleportEntry(2, 21, "Celsian Isles: Ishnir", "Located in the desert of Ishnir.", Material.SANDSTONE, "Quest101", 13, "celsian_isles_ishnir"));
		BORDER_ITEMS.add(new TeleportEntry(2, 22, "Kaul's Arena", "Located in the Kaul arena.", Material.JUNGLE_LEAVES, "KaulWins", 1, "kaul_arena"));
		BORDER_ITEMS.add(new TeleportEntry(2, 23, "Eldrask's Arena", "Located in the Eldrask arena.", Material.PACKED_ICE, "FGWins", 1, "eldrask_arena"));
		BORDER_ITEMS.add(new TeleportEntry(2, 24, "Hekawt's Arena", "Located in the Hekawt arena.", Material.RED_SANDSTONE, "LichWins", 1, "hekawt_arena"));
		BORDER_ITEMS.add(new TeleportEntry(2, 29, "Verdant Remnants", "A plot located right in the middle of Verdant Remnants.", Material.JUNGLE_SAPLING, "Verdant Remnants", "dungeons/verdant_remnants"));
		BORDER_ITEMS.add(new TeleportEntry(2, 30, "Corsair's Claw", "Located under a looming claw on the beach.", Material.SAND, "Corsair's Claw", "pois/corsairs_claw"));
		BORDER_ITEMS.add(new TeleportEntry(2, 32, "Monumenta Spawn Box", "Pulled directly from the new spawn location, with its scenic views and stylized signage.", Material.OAK_SIGN, "Monumenta Spawn Box", "pass/spawnbox"));
		BORDER_ITEMS.add(new TeleportEntry(2, 33, "Sierhaven", "Located right in the town square of Sierhaven.", Material.BLUE_CONCRETE, "Sierhaven", "pass/sierhaven"));

		BORDER_ITEMS.add(new TeleportEntry(3, 18, "Halls of Wind and Blood", "A plot right in the middle of the main room.", Material.WHITE_WOOL, "White", 1, "dungeons/white"));
		BORDER_ITEMS.add(new TeleportEntry(3, 19, "Arcane Rivalry", "Located in the lake looking in towards the first castle.", Material.LIGHT_BLUE_WOOL, "LightBlue", 1, "dungeons/lightblue"));
		BORDER_ITEMS.add(new TeleportEntry(3, 20, "Malevolent Reverie", "The best destination for corrupted nightmares of Sierhaven!", Material.NETHER_WART_BLOCK, "Corrupted", 1, "dungeons/reverie"));
		BORDER_ITEMS.add(new TeleportEntry(3, 21, "Celsian Isles Ocean", "A plot drowned beneath the waters of the Celsian Isles.", Material.BUBBLE_CORAL, "Quest101", 13, "celsian_isles_ocean"));
		BORDER_ITEMS.add(new TeleportEntry(3, 22, "Snow Wool Spirit's Arena", "A festive snowglobe.", Material.WHITE_STAINED_GLASS, "Quest58", 8, "snow_spirit_arena"));
		BORDER_ITEMS.add(new TeleportEntry(3, 23, "Salazar's Folly", "Located right in the middle of the Viridian City.", Material.LIME_WOOL, "Lime", 1, "dungeons/lime"));
		BORDER_ITEMS.add(new TeleportEntry(3, 24, "Valley of Forgotten Pharaohs", "A plot in the hostile town within the gray dungeon.", Material.GRAY_WOOL, "Gray", 1, "dungeons/gray"));
		BORDER_ITEMS.add(new TeleportEntry(3, 25, "Grasp of Avarice", "A plot found at the end of the orange branch of the dungeon.", Material.PURPLE_WOOL, "Purple", 1, "dungeons/purple"));
		BORDER_ITEMS.add(new TeleportEntry(3, 26, "Echoes of Oblivion", "Warp to another time with this plot located in Era 3!", Material.CYAN_CONCRETE_POWDER, "Teal", 1, "dungeons/teal"));
		BORDER_ITEMS.add(new TeleportEntry(3, 30, "Christmas Night", "Not a creature was stirring, not even a mouse...", Material.SPRUCE_SAPLING, null, 0, "christmas_night"));
		BORDER_ITEMS.add(new TeleportEntry(3, 32, "Gyrhaeddant's Lair", "A plot directly below the tentacles of the depths boss", Material.CRIMSON_HYPHAE, null, 0, "pois/gyrhaeddant"));
	}

	private int mCurrentPage;
	private final Boolean mOverridePermissions;

	public PlotBorderCustomInventory(Player player, boolean fullAccess) {
		super(player, 54, "Border Choices");
		mCurrentPage = 1;
		mOverridePermissions = fullAccess;
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
			for (TeleportEntry item : BORDER_ITEMS) {
				if (item.mSlot == chosenSlot && item.mPage == mCurrentPage) {
					if (event.isLeftClick()) {
						completeCommand(player, item.mName, item.mLeftClick);
					}
				}
				if (item.mSlot == chosenSlot && item.mPage == commonPage) {
					if (event.isLeftClick()) {
						completeCommand(player, item.mName, item.mLeftClick);
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
		} else if (cmd.equals("back")) {
			mCurrentPage = 1;
			setLayout(player);
		}
	}

	public void completeCommand(Player player, String name, String cmd) {
		if (cmd.isEmpty()) {
			return;
		}
		if (isInternalCommand(cmd)) {
			runInternalCommand(player, cmd);
		} else {
			long timeLeft = COOLDOWNS.getOrDefault(player.getUniqueId(), 0L) - Instant.now().getEpochSecond();

			if (timeLeft > 0 && !player.isOp()) {
				player.sendMessage("Too fast! You can only change the border once every 120s (" + timeLeft + "s remaining)");
			} else if (!player.getWorld().getName().contains("plot")) {
				player.sendMessage("Can only load plot borders if the world's name contains 'plot', got '" + player.getWorld().getName() + "'");
			} else {
				player.sendMessage("Started loading plot border: " + name);
				COOLDOWNS.put(player.getUniqueId(), Instant.now().getEpochSecond() + 120);
				Location loc = player.getLocation();
				loc.setX(-1392);
				loc.setY(0);
				loc.setZ(-1392);
				StructuresAPI.loadAndPasteStructure("plots/borders/" + cmd, loc, false).whenComplete((unused, ex) -> {
					if (ex != null) {
						player.sendMessage("Plot border completed with error: " + ex.getMessage());
						ex.printStackTrace();
					} else {
						player.sendMessage("Plot border loading complete");
					}
				});
			}
			player.closeInventory();
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
		ItemUtils.setPlainName(newItem, location.mName);
		return newItem;
	}

	public void setLayout(Player player) {
		mInventory.clear();
		CosmeticsManager cosmetics = CosmeticsManager.getInstance();
		int commonPage = (int) Math.floor(mCurrentPage / 10.0) * 10;
		for (TeleportEntry item : BORDER_ITEMS) {
			if (item.mPage == commonPage) {
				if (item.mPermType == PermType.COSMETIC_ITEM &&
					    (mOverridePermissions || cosmetics.playerHasCosmetic(player, CosmeticType.PLOT_BORDER, item.mCosmeticString))) {
					mInventory.setItem(item.mSlot, createCustomItem(item));
				} else if (item.mPermType == PermType.SCOREBOARD &&
					           (item.mScoreboard == null || mOverridePermissions ||
						            ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired)) {
					mInventory.setItem(item.mSlot, createCustomItem(item));
				}
			}

			//intentionally not else, so overrides can happen
			if (item.mPage == mCurrentPage) {
				if (item.mPermType == PermType.COSMETIC_ITEM &&
					    cosmetics.playerHasCosmetic(player, CosmeticType.PLOT_BORDER, item.mCosmeticString)) {
					mInventory.setItem(item.mSlot, createCustomItem(item));
				} else if (item.mPermType == PermType.SCOREBOARD &&
					           (item.mScoreboard == null || mOverridePermissions ||
						            ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired)) {
					mInventory.setItem(item.mSlot, createCustomItem(item));
				}
			}
		}

		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
	}

	public static String[] getCosmeticNames() {
		return BORDER_ITEMS.stream().map(item -> item.mCosmeticString).filter(Objects::nonNull).toArray(String[]::new);
	}

}
