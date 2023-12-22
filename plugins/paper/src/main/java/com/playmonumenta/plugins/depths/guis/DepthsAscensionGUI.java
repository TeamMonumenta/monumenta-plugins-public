package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.loot.ZenithLoot;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DepthsAscensionGUI extends Gui {
	private static final Component MAIN_PAGE_TITLE = Component.text("Ascension Modifiers");

	private static final int DELVE_POINTS_PER_LEVEL = 10;

	private static class AscensionLevelDetails {
		int mLevel;
		@Nullable String mAscensionDescription;
		int mAscensionDelvePoints;
		Material mItemMat;
		int mSlot;

		AscensionLevelDetails(int level, @Nullable String description, int delvePoints, Material mat, int slot) {
			mLevel = level;
			mAscensionDescription = description;
			mAscensionDelvePoints = delvePoints;
			mItemMat = mat;
			mSlot = slot;
		}
	}

	private static final List<AscensionLevelDetails> mLevelData = new ArrayList<>(List.of(
		new AscensionLevelDetails(1, null,
			DELVE_POINTS_PER_LEVEL, Material.BLACKSTONE, 10),
		new AscensionLevelDetails(2, "Utility rooms are half as common.",
			DELVE_POINTS_PER_LEVEL, Material.BLACK_CONCRETE_POWDER, 11),
		new AscensionLevelDetails(3, null,
			DELVE_POINTS_PER_LEVEL * 2, Material.BLACK_CONCRETE_POWDER, 12),
		new AscensionLevelDetails(4, "Bosses have new abilities.",
			DELVE_POINTS_PER_LEVEL * 2, Material.BLACK_CONCRETE_POWDER, 13),
		new AscensionLevelDetails(5, null,
			DELVE_POINTS_PER_LEVEL * 3, Material.BLACK_CONCRETE_POWDER, 14),
		new AscensionLevelDetails(6, "You are more likely to get lower rarity abilities.",
			DELVE_POINTS_PER_LEVEL * 3, Material.BLACK_CONCRETE, 15),
		new AscensionLevelDetails(7, null,
			DELVE_POINTS_PER_LEVEL * 4, Material.BLACK_CONCRETE, 16),
		new AscensionLevelDetails(8, "Bosses cast attacks more frequently and have new patterns.",
			DELVE_POINTS_PER_LEVEL * 4, Material.BLACK_CONCRETE, 19),
		new AscensionLevelDetails(9, null,
			DELVE_POINTS_PER_LEVEL * 5, Material.CRYING_OBSIDIAN, 20),
		new AscensionLevelDetails(10, "You must remove an ability before each boss fight.",
			DELVE_POINTS_PER_LEVEL * 5, Material.CRYING_OBSIDIAN, 21),
		new AscensionLevelDetails(11, null,
			DELVE_POINTS_PER_LEVEL * 6, Material.CRYING_OBSIDIAN, 22),
		new AscensionLevelDetails(12, "The time to revive is reduced.",
			DELVE_POINTS_PER_LEVEL * 6, Material.CRYING_OBSIDIAN, 23),
		new AscensionLevelDetails(13, "Your party is assigned max Twisted delve points.",
			DELVE_POINTS_PER_LEVEL * 7, Material.CRYING_OBSIDIAN, 24),
		new AscensionLevelDetails(14, "Ability and upgrade options are reduced.",
			DELVE_POINTS_PER_LEVEL * 7, Material.CRYING_OBSIDIAN, 25),
		new AscensionLevelDetails(15, "All bosses are now stronger.",
			DELVE_POINTS_PER_LEVEL * 7, Material.CRYING_OBSIDIAN, 40)
	));

	public DepthsAscensionGUI(Player p) {
		super(p, 54, MAIN_PAGE_TITLE);
	}

	@Override
	protected void setup() {
		int ascensionScore = ScoreboardUtils.getScoreboardValue(mPlayer, DepthsParty.ASCENSION_LEADERBOARD).orElse(0);
		for (AscensionLevelDetails details : mLevelData) {
			generateItem(details, ascensionScore);
		}
		//ascension 1
		ItemStack summaryItem = GUIUtils.createBasicItem(Material.BLACKSTONE, 1, "Celestial Zenith Ascension Choices", NamedTextColor.DARK_PURPLE, true,
			List.of(Component.empty()), true);
		GUIUtils.splitLoreLine(summaryItem, "Choose an ascension level of the Celestial Zenith Dungeon, " +
			"its difficulty now enhanced using custom modifiers.", NamedTextColor.GRAY, 30, true);
		setItem(4, summaryItem);
	}

	private static void startInstance(Player mPlayer, int ascension) {
		//probably doing something wrong here so someone who knows how to do this properly please fix.
		// TODO change to a bounding box centered on the bot and check for players that collide.
		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), 5, true)) {
			ScoreboardUtils.setScoreboardValue(p, "CurrentAscension", ascension);
		}
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute as " + mPlayer.getName() + " at @s run function monumenta:lobbies/dcz/new"), 0);
	}

	private void generateItem(AscensionLevelDetails details, int playerScore) {
		if (playerScore >= details.mLevel - 1) {
			//build full item
			ItemStack clickableItem = GUIUtils.createBasicItem(details.mItemMat,
				"Celestial Zenith - Ascension " + details.mLevel,
				NamedTextColor.DARK_PURPLE, true);
			GUIUtils.splitLoreLine(clickableItem, "Ascension modifiers:",
				NamedTextColor.GRAY, 30, true);
			//Bonuses
			GUIUtils.splitLoreLine(clickableItem, "+ Increases currency drops by "
				+ StringUtils.multiplierToPercentage(ZenithLoot.CURRENCY_PER_ASC_LEVEL * details.mLevel)
				+ "% and dungeon material drops by "
				+ StringUtils.multiplierToPercentage(ZenithLoot.DUNGEON_PER_ASC_LEVEL * details.mLevel) + "%.",
				NamedTextColor.GREEN, 30, false);
			GUIUtils.splitLoreLine(clickableItem, "+ Zenith Charms drop at Level " + details.mLevel + " rates",
				NamedTextColor.GREEN, 30, false);

			GUIUtils.splitLoreLine(clickableItem, "- Your party is randomly assigned "
					+ details.mAscensionDelvePoints + " delve points, split between F1 and F2.",
				NamedTextColor.RED, 30, false);
			//Difficulties
			for (int i = 0; i < details.mLevel; i++) {
				String detailString = mLevelData.get(i).mAscensionDescription;
				if (detailString != null) {
					GUIUtils.splitLoreLine(clickableItem, "- " + detailString, NamedTextColor.RED, 30, false);
				}
			}

			setItem(details.mSlot, clickableItem).onLeftClick(() -> {
				startInstance(mPlayer, details.mLevel);
			});
		} else {
			ItemStack lockedItem = GUIUtils.createBasicItem(Material.RED_STAINED_GLASS_PANE, 1,
				"Celestial Zenith - Ascension " + details.mLevel, NamedTextColor.DARK_PURPLE, true, List.of(Component.empty()), true);
			GUIUtils.splitLoreLine(lockedItem, "Defeat Celestial Zenith on ascension "
				+ (details.mLevel - 1) + " or greater to unlock this difficulty.", NamedTextColor.RED, 30, true);
			setItem(details.mSlot, lockedItem);
		}
	}

}
