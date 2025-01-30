package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.RemotePlayerAPI;
import com.playmonumenta.networkrelay.RemotePlayerData;
import com.playmonumenta.plugins.commands.DungeonAccessCommand;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DungeonUtils.DungeonCommandMapping;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ShardHealthUtils.ShardHealth;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.getGuildBanner;

public class OrinCustomInventory extends CustomInventory {
	private static final Material FILLER = GUIUtils.FILLER_MATERIAL;
	private static final InvLocation[] INSTANCE_UPTO3_LOCS = {invLocation(3, 3), invLocation(5, 3), invLocation(7, 3)};
	private static final InvLocation[] INSTANCE_UPTO9_LOCS =
		{
			invLocation(3, 3), invLocation(5, 3), invLocation(7, 3),
			invLocation(3, 4), invLocation(5, 4), invLocation(7, 4),
			invLocation(3, 5), invLocation(5, 5), invLocation(7, 5),
		};
	private static final InvLocation[] INSTANCE_UPTO20_LOCS =
		{
			invLocation(3, 3), invLocation(4, 3), invLocation(5, 3), invLocation(6, 3), invLocation(7, 3),
			invLocation(3, 4), invLocation(4, 4), invLocation(5, 4), invLocation(6, 4), invLocation(7, 4),
			invLocation(3, 5), invLocation(4, 5), invLocation(5, 5), invLocation(6, 5), invLocation(7, 5),
			invLocation(3, 6), invLocation(4, 6), invLocation(5, 6), invLocation(6, 6), invLocation(7, 6),
		};
	private static final InvLocation[] INSTANCE_UPTO28_LOCS =
		{
			invLocation(2, 3), invLocation(3, 3), invLocation(4, 3), invLocation(5, 3), invLocation(6, 3), invLocation(7, 3), invLocation(8, 3),
			invLocation(2, 4), invLocation(3, 4), invLocation(4, 4), invLocation(5, 4), invLocation(6, 4), invLocation(7, 4), invLocation(8, 4),
			invLocation(2, 5), invLocation(3, 5), invLocation(4, 5), invLocation(5, 5), invLocation(6, 5), invLocation(7, 5), invLocation(8, 5),
			invLocation(2, 6), invLocation(3, 6), invLocation(4, 6), invLocation(5, 6), invLocation(6, 6), invLocation(7, 6), invLocation(8, 6),
		};

	public static class TeleportEntry {
		TeleporterPage mPage;
		int mSlot;
		String mName;
		@Nullable String mScoreboard;
		String mLore;
		int mScoreRequired;
		Material mType;
		@Nullable BiConsumer<OrinCustomInventory, Player> mLeftClick;
		@Nullable BiConsumer<OrinCustomInventory, Player> mRightClick;
		int mItemCount;
		@Nullable Function<Player, ItemStack> mCustomItem;

		public TeleportEntry(TeleporterPage page, InvLocation slot, String name, String lore,
							 Material type, @Nullable String scoreboard, int scoreRequired, @Nullable String left) {
			this(page, slot, name, lore, type, scoreboard, scoreRequired, left, null, 1);
		}

		public TeleportEntry(TeleporterPage page, InvLocation slot, String name, String lore,
							 Material type, @Nullable String scoreboard, int scoreRequired,
							 @Nullable String left, @Nullable String right) {
			this(page, slot, name, lore, type, scoreboard, scoreRequired, left, right, 1);
		}

		public TeleportEntry(TeleporterPage page, InvLocation slot, String name, String lore,
							 Material type, @Nullable String scoreboard, int scoreRequired,
							 @Nullable String left, @Nullable String right, int count) {
			this(page, slot, name, lore, type, scoreboard, scoreRequired,
				StringUtils.isBlank(left) ? null : (gui, player) -> gui.completeCommand(player, left),
				StringUtils.isBlank(right) ? null : (gui, player) -> gui.completeCommand(player, right),
				count, null);
		}

		public TeleportEntry(TeleporterPage page, InvLocation slot, String name, String lore,
							 Material type, @Nullable String scoreboard, int scoreRequired,
							 @Nullable String left, @Nullable String right, int count, @Nullable Function<Player, ItemStack> customItem) {
			this(page, slot, name, lore, type, scoreboard, scoreRequired,
				StringUtils.isBlank(left) ? null : (gui, player) -> gui.completeCommand(player, left),
				StringUtils.isBlank(right) ? null : (gui, player) -> gui.completeCommand(player, right),
				count, customItem);
		}

		public TeleportEntry(TeleporterPage page, InvLocation slot, String name, String lore,
							 Material type, @Nullable String scoreboard, int scoreRequired,
							 @Nullable BiConsumer<OrinCustomInventory, Player> left,
							 @Nullable BiConsumer<OrinCustomInventory, Player> right,
							 int count, @Nullable Function<Player, ItemStack> customItem) {
			mPage = page;
			mSlot = slot.getInvLoc();
			mName = name;
			mLore = lore;
			mType = type;
			mScoreboard = scoreboard;
			mScoreRequired = scoreRequired;
			mLeftClick = left;
			mRightClick = right;
			mItemCount = count;
			mCustomItem = customItem;
		}
	}

	public static class InvLocation {
		int mX;
		int mY;

		public InvLocation(int x, int y) {
			this.mX = x;
			this.mY = y;
		}

		public int getInvLoc() {
			return (mY - 1) * 9 + (mX - 1);
		}
	}

	public enum TeleporterPage {
		SELF_DETERMINED(-1),
		COMMON_SHARD_PAGES(0),
		REGION_1(1),
		REGION_2(2),
		REGION_3(3),
		PLOTS(4),
		PLAYER_PLOTS(5),
		DEFAULT_SHARD(6),
		COMMON_INSTANCE_BOT_PAGES(10),
		REGION_1_INSTANCE_BOT(11),
		REGION_2_INSTANCE_BOT(12),
		REGION_3_INSTANCE_BOT(13),
		COMMON_DUNGEON_PAGES(20),
		DUNGEON_INSTANCES(21);

		public final int mPage;

		TeleporterPage(int i) {
			this.mPage = i;
		}
	}

	/* Page Info
	 * Page 0: Common for 1-9
	 * Page 1: Region 1
	 * Page 2: Region 2
	 * Page 3: Region 3
	 * Page 4: Plots
	 * Page 5: Playerplots
	 * Page 6: Default Page (dungeon shards, etc.)
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
		String sortedDesc = "Left Click to be sorted to a shard, right click to choose the shard.";
		Function<Player, ItemStack> getBanner = player -> {
			ItemStack banner = getGuildBanner(player);
			if (banner == null) {
				banner = new ItemStack(Material.YELLOW_BANNER);
			}
			ArrayList<Component> lore = new ArrayList<>();
			lore.add(Component.text("Click to teleport!", NamedTextColor.DARK_PURPLE));
			return GUIUtils.createBasicItem(banner, 1, Component.text("Guild Plot", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false),
				lore, true);
		};

		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_SHARD_PAGES, new InvLocation(1, 6), "Build Server", "Click to teleport!",
			Material.STONE_PICKAXE, null, 0, "transferserver build"));

		//R1 Page
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_1, new InvLocation(1, 2), "Plots", "Click to teleport!",
			Material.LIGHT_BLUE_CONCRETE, null, 0,
			"execute as @S run function monumenta:mechanisms/teleporters/tp/sierhaven_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_1, new InvLocation(1, 4), "Player Plot", "Click to teleport!",
			Material.GRASS_BLOCK, "CurrentPlot", 1,
			"plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_1, new InvLocation(4, 2), "Sierhaven", sortedDesc,
			Material.GREEN_CONCRETE, null, 0,
			"tp @S -765.5 107.0625 70.5 180 0", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_1, new InvLocation(7, 2), "Mistport", sortedDesc,
			Material.SAND, "Quest101", 13,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_1, new InvLocation(4, 5), "Galengarde", sortedDesc,
			Material.RED_MUSHROOM_BLOCK, PlayerUtils.SCOREBOARD_RING_UNLOCK, 1,
			"execute as @S run function monumenta:mechanisms/teleporters/goto/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_1, new InvLocation(7, 5), "Dungeon Instances", "Click to view all open dungeon instances.",
			Material.SPAWNER, null, 0,
			"page 21"));

		//R2 Page
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_2, new InvLocation(1, 2), "Plots", "Click to teleport!",
			Material.LIGHT_BLUE_CONCRETE, null, 0,
			"execute as @S run function monumenta:mechanisms/teleporters/tp/mistport_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_2, new InvLocation(1, 4), "Player Plot", "Click to teleport!",
			Material.GRASS_BLOCK, "CurrentPlot", 1,
			"plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_2, new InvLocation(4, 2), "Sierhaven", sortedDesc,
			Material.GREEN_CONCRETE, null, 0,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_2, new InvLocation(7, 2), "Mistport", sortedDesc,
			Material.SAND, "Quest101", 13,
			"tp @S -762.5 70.1 1344.5 180 0", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_2, new InvLocation(4, 5), "Galengarde", sortedDesc,
			Material.RED_MUSHROOM_BLOCK, PlayerUtils.SCOREBOARD_RING_UNLOCK, 1,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_2, new InvLocation(7, 5), "Dungeon Instances", "Click to view all open dungeon instances.",
			Material.SPAWNER, null, 0,
			"page 21"));

		//R3 Page
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_3, new InvLocation(1, 2), "Plots", "Click to teleport!",
			Material.LIGHT_BLUE_CONCRETE, null, 0,
			"execute as @S run function monumenta:mechanisms/teleporters/tp/galengarde_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_3, new InvLocation(1, 4), "Player Plot", "Click to teleport!",
			Material.GRASS_BLOCK, "CurrentPlot", 1,
			"plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_3, new InvLocation(4, 2), "Sierhaven", sortedDesc,
			Material.GREEN_CONCRETE, null, 0,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_3, new InvLocation(7, 2), "Mistport", sortedDesc,
			Material.SAND, "Quest101", 13,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_3, new InvLocation(4, 5), "Galengarde", sortedDesc,
			Material.RED_MUSHROOM_BLOCK, PlayerUtils.SCOREBOARD_RING_UNLOCK, 1,
			"tp @S -303.5 83 -654.5 90 0", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.REGION_3, new InvLocation(7, 5), "Dungeon Instances", "Click to view all open dungeon instances.",
			Material.SPAWNER, null, 0,
			"page 21"));

		//Plots Page
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLOTS, new InvLocation(1, 1), "Docks", "Click to teleport!",
			Material.LIGHT_BLUE_CONCRETE, null, 0,
			"tp @S -2456.0 56.5 1104.0 90 0"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLOTS, new InvLocation(1, 3), "Market", "Click to teleport!",
			Material.BARREL, null, 0,
			"execute as @S run function monumenta:mechanisms/teleporters/enter_new_market"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLOTS, new InvLocation(1, 4), "Player Plot", "Click to teleport!",
			Material.GRASS_BLOCK, "CurrentPlot", 1,
			"plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLOTS, new InvLocation(1, 5), "Guild Plot", "Click to teleport!",
			Material.YELLOW_BANNER, null, 0,
			"teleportguild @S", "guild teleportgui @S", 1, getBanner));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLOTS, new InvLocation(4, 2), "Sierhaven", sortedDesc,
			Material.GREEN_CONCRETE, null, 0,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLOTS, new InvLocation(7, 2), "Mistport", sortedDesc,
			Material.SAND, "Quest101", 13,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLOTS, new InvLocation(4, 5), "Galengarde", sortedDesc,
			Material.RED_MUSHROOM_BLOCK, PlayerUtils.SCOREBOARD_RING_UNLOCK, 1,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLOTS, new InvLocation(7, 5), "Dungeon Instances", "Click to view all open dungeon instances.",
			Material.SPAWNER, null, 0,
			"page 21"));

		//Playerplots Page
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLAYER_PLOTS, new InvLocation(1, 2), "Plots", "Click to teleport!",
			Material.LIGHT_BLUE_CONCRETE, null, 0,
			"transferserver plots"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLAYER_PLOTS, new InvLocation(1, 4), "Player Plot", "Click to teleport!",
			Material.GRASS_BLOCK, "CurrentPlot", 1,
			"plot gui @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLAYER_PLOTS, new InvLocation(4, 2), "Sierhaven", sortedDesc,
			Material.GREEN_CONCRETE, null, 0,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLAYER_PLOTS, new InvLocation(7, 2), "Mistport", sortedDesc,
			Material.SAND, "Quest101", 13,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLAYER_PLOTS, new InvLocation(4, 5), "Galengarde", sortedDesc,
			Material.RED_MUSHROOM_BLOCK, PlayerUtils.SCOREBOARD_RING_UNLOCK, 1,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.PLAYER_PLOTS, new InvLocation(7, 5), "Dungeon Instances", "Click to view all open dungeon instances.",
			Material.SPAWNER, null, 0,
			"page 21"));

		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DEFAULT_SHARD, new InvLocation(1, 2), "Plots", "Click to teleport!",
			Material.LIGHT_BLUE_CONCRETE, null, 0,
			"transferserver plots"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DEFAULT_SHARD, new InvLocation(1, 4), "Player Plot", "Click to teleport!",
			Material.GRASS_BLOCK, "CurrentPlot", 1,
			"plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DEFAULT_SHARD, new InvLocation(4, 2), "Sierhaven", sortedDesc,
			Material.GREEN_CONCRETE, null, 0,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DEFAULT_SHARD, new InvLocation(7, 2), "Mistport", sortedDesc,
			Material.SAND, "Quest101", 13,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DEFAULT_SHARD, new InvLocation(4, 5), "Galengarde", sortedDesc,
			Material.RED_MUSHROOM_BLOCK, PlayerUtils.SCOREBOARD_RING_UNLOCK, 1,
			"execute as @S at @s run function monumenta:mechanisms/teleporters/shards/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DEFAULT_SHARD, new InvLocation(7, 5), "Dungeon Instances", "Click to view all open dungeon instances.",
			Material.SPAWNER, null, 0,
			"page 21"));

		//Common 10-19: Instance Bot Choices
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_INSTANCE_BOT_PAGES, new InvLocation(1, 1), "Back", "Return to the main page.",
			Material.ARROW, null, 0, "back"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_INSTANCE_BOT_PAGES, new InvLocation(5, 1), "Available Shards", "Choose your shard below.",
			Material.SCUTE, null, 0, ""));

		//Common 20-29: Dungeon Instances
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(1, 1), "Back", "Return to the main page.", Material.ARROW, null, 0, "back"));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(1, 2), "Region 1 Dungeons", "Dungeons located with the King's Valley.",
			Material.GREEN_CONCRETE, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(2, 2), "Region 1 Dungeons", "Dungeons located with the King's Valley.",
			Material.GREEN_CONCRETE, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(3, 2), "Region 1 Dungeons", "Dungeons located with the King's Valley.",
			Material.GREEN_CONCRETE, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(4, 2), "Region 2 Dungeons", "Dungeons located with the Celsian Isles.",
			Material.SAND, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(5, 2), "Region 2 Dungeons", "Dungeons located with the Celsian Isles.",
			Material.SAND, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(6, 2), "Region 2 Dungeons", "Dungeons located with the Celsian Isles.",
			Material.SAND, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(7, 2), "Region 3 Dungeons", "Dungeons located with the Architect's Ring.",
			Material.RED_MUSHROOM_BLOCK, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(8, 2), "Region 3 Dungeons", "Dungeons located with the Architect's Ring.",
			Material.RED_MUSHROOM_BLOCK, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.COMMON_DUNGEON_PAGES, new InvLocation(9, 2), "Region 3 Dungeons", "Dungeons located with the Architect's Ring.",
			Material.RED_MUSHROOM_BLOCK, null, 0, "", ""));

		//21: Dungeon Instances
		//Group: R1 Dungeons
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(1, 3), "Labs", "Click to teleport!",
			Material.GLASS_BOTTLE, "D0Access", 1, sendToDungeonAction(DungeonCommandMapping.LABS), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(2, 3), "White", "Click to teleport!",
			Material.WHITE_WOOL, "D1Access", 1, sendToDungeonAction(DungeonCommandMapping.WHITE), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(3, 3), "Orange", "Click to teleport!",
			Material.ORANGE_WOOL, "D2Access", 1, sendToDungeonAction(DungeonCommandMapping.ORANGE), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(1, 4), "Magenta", "Click to teleport!",
			Material.MAGENTA_WOOL, "D3Access", 1, sendToDungeonAction(DungeonCommandMapping.MAGENTA), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(2, 4), "Light Blue", "Click to teleport!",
			Material.LIGHT_BLUE_WOOL, "D4Access", 1, sendToDungeonAction(DungeonCommandMapping.LIGHTBLUE), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(3, 4), "Yellow", "Click to teleport!",
			Material.YELLOW_WOOL, "D5Access", 1, sendToDungeonAction(DungeonCommandMapping.YELLOW), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(1, 5), "Willows", "Click to teleport!",
			Material.JUNGLE_LEAVES, "DB1Access", 1, sendToDungeonAction(DungeonCommandMapping.WILLOWS), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(2, 5), "Reverie", "Click to teleport!",
			Material.FIRE_CORAL, "DCAccess", 1, sendToDungeonAction(DungeonCommandMapping.REVERIE), null, 1, null));

		//Group: R2 Dungeons
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(4, 3), "Lime", "Click to teleport!",
			Material.LIME_WOOL, "D6Access", 1, sendToDungeonAction(DungeonCommandMapping.LIME), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(5, 3), "Pink", "Click to teleport!",
			Material.PINK_WOOL, "D7Access", 1, sendToDungeonAction(DungeonCommandMapping.PINK), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(6, 3), "Gray", "Click to teleport!",
			Material.GRAY_WOOL, "D8Access", 1, sendToDungeonAction(DungeonCommandMapping.GRAY), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(4, 4), "Light Gray", "Click to teleport!",
			Material.LIGHT_GRAY_WOOL, "D9Access", 1, sendToDungeonAction(DungeonCommandMapping.LIGHTGRAY), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(5, 4), "Cyan", "Click to teleport!",
			Material.CYAN_WOOL, "D10Access", 1, sendToDungeonAction(DungeonCommandMapping.CYAN), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(6, 4), "Purple", "Click to teleport!",
			Material.PURPLE_WOOL, "D11Access", 1, sendToDungeonAction(DungeonCommandMapping.PURPLE), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(4, 5), "Teal", "Click to teleport!",
			Material.CYAN_CONCRETE_POWDER, "DTLAccess", 1, sendToDungeonAction(DungeonCommandMapping.TEAL), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(5, 5), "Shifting City", "Click to teleport!",
			Material.PRISMARINE_BRICKS, "DRL2Access", 1, sendToDungeonAction(DungeonCommandMapping.SHIFTINGCITY), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(6, 5), "The Fallen Forum", "Click to teleport!",
			Material.BOOKSHELF, "DFFAccess", 1, sendToDungeonAction(DungeonCommandMapping.FORUM), null, 1, null));

		//Group: R3 Dungeons
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(7, 3), "Silver Knight's Tomb", "Click to teleport!",
			Material.DEEPSLATE, "DSKTAccess", 1, sendToDungeonAction(DungeonCommandMapping.SKT), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(8, 3), "Blue", "Click to teleport!", Material.BLUE_WOOL,
			"D12Access", 1, sendToDungeonAction(DungeonCommandMapping.BLUE), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(9, 3), "Brown", "Click to teleport!", Material.BROWN_WOOL,
			"D13Access", 1, sendToDungeonAction(DungeonCommandMapping.BROWN), null, 1, null));
		ORIN_ITEMS.add(new TeleportEntry(TeleporterPage.DUNGEON_INSTANCES, new InvLocation(7, 4), "Hexfall", "Click to teleport!", Material.MOSSY_STONE_BRICKS,
			"DHFAccess", 1, sendToDungeonAction(DungeonCommandMapping.HEXFALL), null, 1, null));
	}

	private TeleporterPage mCurrentPage;
	private final String mCurrentShard = ServerProperties.getShardName();
	private final boolean mBackButtonEnabled;

	public OrinCustomInventory(Player player, TeleporterPage page) {
		super(player, 54, "Teleportation Choices");
		if (page == TeleporterPage.SELF_DETERMINED) {
			mBackButtonEnabled = true;
			if (mCurrentShard.contains("valley")) {
				mCurrentPage = TeleporterPage.REGION_1;
			} else if (mCurrentShard.contains("isles")) {
				mCurrentPage = TeleporterPage.REGION_2;
			} else if (mCurrentShard.contains("ring")) {
				mCurrentPage = TeleporterPage.REGION_3;
			} else if (mCurrentShard.equals("plots")) {
				mCurrentPage = TeleporterPage.PLOTS;
			} else if (mCurrentShard.equals("playerplots")) {
				mCurrentPage = TeleporterPage.PLAYER_PLOTS;
			} else {
				mCurrentPage = TeleporterPage.DEFAULT_SHARD;
			}
		} else {
			mBackButtonEnabled = false;
			mCurrentPage = page;
		}

		setLayout(player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory) {
			return;
		}

		int commonPage = (int) Math.floor(mCurrentPage.mPage / 10.0) * 10;
		if (clickedItem != null && clickedItem.getType() != FILLER && !event.isShiftClick()) {
			int chosenSlot = event.getSlot();
			for (TeleportEntry item : ORIN_ITEMS) {
				if (item.mSlot == chosenSlot && item.mPage == mCurrentPage) {
					if (event.isLeftClick()) {
						if (item.mLeftClick != null) {
							item.mLeftClick.accept(this, player);
						}
					} else {
						if (item.mRightClick != null) {
							item.mRightClick.accept(this, player);
						}
					}
				}
				if (item.mSlot == chosenSlot && item.mPage.mPage == commonPage) {
					if (event.isLeftClick()) {
						if (item.mLeftClick != null) {
							item.mLeftClick.accept(this, player);
						}
					} else {
						if (item.mRightClick != null) {
							item.mRightClick.accept(this, player);
						}
					}
				}
			}
			if (mCurrentPage.mPage == 11 || mCurrentPage.mPage == 12 || mCurrentPage.mPage == 13) {
				for (TeleportEntry item : INSTANCE_ITEMS) {
					if (item.mSlot == chosenSlot && item.mPage == mCurrentPage) {
						if (event.isLeftClick()) {
							if (item.mLeftClick != null) {
								item.mLeftClick.accept(this, player);
							}
						} else {
							if (item.mRightClick != null) {
								item.mRightClick.accept(this, player);
							}
						}
					}
				}
			}
		}
	}

	public boolean isInternalCommand(String command) {
		return command.equals("exit") || command.startsWith("page") || command.startsWith("instancebot") || command.equals("back");
	}

	public void runInternalCommand(Player player, String cmd) {
		if (cmd.startsWith("page")) {
			int findPage = Integer.parseInt(cmd.split(" ")[1]);
			mCurrentPage = pageFromInt(findPage);
			setLayout(player);
		} else if (cmd.startsWith("exit")) {
			player.closeInventory();
		} else if (cmd.startsWith("instancebot")) {
			String searchTerm = cmd.split(" ")[1];
			if (searchTerm.startsWith("valley")) {
				mCurrentPage = TeleporterPage.REGION_1_INSTANCE_BOT;
			} else if (searchTerm.startsWith("isles")) {
				mCurrentPage = TeleporterPage.REGION_2_INSTANCE_BOT;
			} else {
				mCurrentPage = TeleporterPage.REGION_3_INSTANCE_BOT;
			}
			setLayout(player);
		} else if (cmd.equals("back")) {
			if (mCurrentShard.contains("valley")) {
				mCurrentPage = TeleporterPage.REGION_1;
			} else if (mCurrentShard.contains("isles")) {
				mCurrentPage = TeleporterPage.REGION_2;
			} else if (mCurrentShard.contains("ring")) {
				mCurrentPage = TeleporterPage.REGION_3;
			} else if (mCurrentShard.equals("plots")) {
				mCurrentPage = TeleporterPage.PLOTS;
			} else if (mCurrentShard.equals("playerplots")) {
				mCurrentPage = TeleporterPage.PLAYER_PLOTS;
			} else {
				mCurrentPage = TeleporterPage.DEFAULT_SHARD;
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
				player.closeInventory();
				String finalCommand = cmd.replace("@S", player.getName());
				NmsUtils.getVersionAdapter().runConsoleCommandSilently(finalCommand);
			}
		}
	}

	private static BiConsumer<OrinCustomInventory, Player> sendToDungeonAction(DungeonCommandMapping dungeon) {
		return (gui, player) -> DungeonAccessCommand.send(player, dungeon, player.getLocation());
	}

	public ItemStack createCustomItem(TeleportEntry location, Player player) {
		if (location.mCustomItem != null) {
			return location.mCustomItem.apply(player);
		}
		return GUIUtils.createBasicItem(location.mType, location.mItemCount, location.mName, NamedTextColor.GOLD, false, location.mLore, NamedTextColor.DARK_PURPLE, 30, true);
	}

	public void setLayout(Player player) {
		mInventory.clear();
		int commonPage = (int) Math.floor(mCurrentPage.mPage / 10.0) * 10;
		TeleporterPage actualPage = pageFromInt(commonPage);
		for (TeleportEntry item : ORIN_ITEMS) {
			if (item.mPage == actualPage) {
				if (item.mSlot == 0 && !mBackButtonEnabled) {
					continue;
				}
				if (item.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired) {
					mInventory.setItem(item.mSlot, createCustomItem(item, player));
				}
			} //intentionally not else, so overrides can happen
			if (item.mPage == mCurrentPage) {
				if (item.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired) {
					mInventory.setItem(item.mSlot, createCustomItem(item, player));
				}
			}
		}

		GUIUtils.fillWithFiller(mInventory);

		if (mCurrentPage == TeleporterPage.REGION_1_INSTANCE_BOT) {
			showInstances(player, "valley");
		} else if (mCurrentPage == TeleporterPage.REGION_2_INSTANCE_BOT) {
			showInstances(player, "isles");
		} else if (mCurrentPage == TeleporterPage.REGION_3_INSTANCE_BOT) {
			showInstances(player, "ring");
		}
	}

	public void showInstances(Player player, String searchTerm) {
		Set<String> results = null;
		INSTANCE_ITEMS.clear();
		try {
			results = NetworkRelayAPI.getOnlineShardNames();
		} catch (Exception e) {
			player.sendMessage(Component.text("Unable to get list of online shards, please report this bug:", NamedTextColor.RED));
			MessagingUtils.sendStackTrace(player, e);
			player.closeInventory();
		}
		int index = 0;
		if (results == null) {
			return;
		}
		results.removeIf(item -> !item.startsWith(searchTerm));

		TeleporterPage page;
		Material itemType;

		if (searchTerm.startsWith("valley")) {
			page = TeleporterPage.REGION_1_INSTANCE_BOT;
			itemType = Material.JUNGLE_SAPLING;
		} else if (searchTerm.startsWith("isles")) {
			page = TeleporterPage.REGION_2_INSTANCE_BOT;
			itemType = Material.KELP;
		} else {
			itemType = Material.DARK_OAK_SAPLING;
			page = TeleporterPage.REGION_3_INSTANCE_BOT;
		}
		InvLocation[] instanceLocations;

		switch (results.size()) {
			case 0, 1, 2, 3 -> instanceLocations = INSTANCE_UPTO3_LOCS;
			case 4, 5, 6, 7, 8, 9 -> instanceLocations = INSTANCE_UPTO9_LOCS;
			case 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 -> instanceLocations = INSTANCE_UPTO20_LOCS;
			default -> instanceLocations = INSTANCE_UPTO28_LOCS;
		}

		ArrayList<Integer> resultSortedList = new ArrayList<>();
		for (String shard : results) {
			if (shard.equalsIgnoreCase(searchTerm)) {
				resultSortedList.add(0);
			} else {
				resultSortedList.add(Integer.parseInt(shard.split("-")[1]));
			}
		}
		Collections.sort(resultSortedList);

		for (Integer shard : resultSortedList) {
			String shardName = searchTerm;
			if (shard != 0) {
				shardName += "-" + shard;
			}

			int playerCount = RemotePlayerAPI.getVisiblePlayersOnServer(shardName).size();
			String playerCountString = playerCount + (playerCount == 1 ? " player" : " players") + " online!";

			List<String> shardLore = new ArrayList<>();

			RemotePlayerData selfRemoteData = MonumentaNetworkRelayIntegration.getRemotePlayer(player.getUniqueId());
			if (selfRemoteData != null) {
				String selfGuild = MonumentaNetworkRelayIntegration.remotePlayerGuild(selfRemoteData);
				if (selfGuild != null) {
					int guildMemberCount = MonumentaNetworkRelayIntegration.guildMembersOnShard(selfGuild, shardName).size();
					String guildMemberCountString = guildMemberCount + (guildMemberCount == 1 ? " guild member" : " guild members") + " online!";

					shardLore.add(guildMemberCountString);
				}
			}

			shardLore.add(playerCountString);

			if (player.hasPermission("group.dev")) {
				ShardHealth shardHealth = MonumentaNetworkRelayIntegration.remoteShardHealth(shardName);
				shardLore.add((int) (100 * shardHealth.healthScore()) + "% Shard Health (lags/crashes at 0, only devs see this)");
			}


			if (index <= instanceLocations.length) {
				INSTANCE_ITEMS.add(new TeleportEntry(page, instanceLocations[index++], shardName, String.join("\n", shardLore),
					itemType, null, 0,
					"transferserver " + shardName, "", shard < 1 ? 1 : shard));
			}
		}

		for (TeleportEntry item : INSTANCE_ITEMS) {
			mInventory.setItem(item.mSlot, createCustomItem(item, player));
		}
	}

	private static InvLocation invLocation(int x, int y) {
		return new InvLocation(x, y);
	}

	public TeleporterPage pageFromInt(int i) {
		for (TeleporterPage page : TeleporterPage.values()) {
			if (page.mPage == i) {
				return page;
			}
		}
		return TeleporterPage.SELF_DETERMINED;
	}
}
