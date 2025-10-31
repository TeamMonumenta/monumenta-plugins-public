package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.listeners.MinigameManager;
import com.playmonumenta.plugins.minigames.Minigame;
import com.playmonumenta.plugins.minigames.SpiritArcheryMinigame;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SpiritArcheryGUI extends Gui {
	public static final Component playerInMinigame = Component.text("The spirits must be faced alone...").color(NamedTextColor.GRAY);
	public static final Component playerTooPoor = Component.text("Your experience is insufficient to face down the spirits...").color(NamedTextColor.GRAY);
	private static final NamespacedKey CXP_KEY = NamespacedKeyUtils.fromString("epic:r1/items/currency/concentrated_experience");
	private static final int[] CXP_COSTS = {8, 16, 24, 32};
	private static final ItemStack INFO_ITEM = GUIUtils.createBasicItem(Material.SPECTRAL_ARROW, 1,
		"Information", com.playmonumenta.plugins.itemstats.enums.Location.WINDWALKER.getColor(), true,
		Component.text("Shoot down ", NamedTextColor.GRAY)
			.append(Component.text("green", NamedTextColor.GREEN))
			.append(Component.text(" spirits to gain score, while avoiding "))
			.append(Component.text("red", NamedTextColor.RED))
			.append(Component.text(" spirits which take away score.")),
		30, true);
	private static final ItemStack WARNING_ITEM = GUIUtils.createBasicItem(Material.MAGMA_BLOCK, 1,
		"Warning", com.playmonumenta.plugins.itemstats.enums.Location.RED.getColor(), true,
		Component.text("Make sure you have at least one arrow!", NamedTextColor.GRAY)
			.appendNewline()
			.append(Component.text("The Spiritsinger sells arrows, should you need them.", NamedTextColor.DARK_GRAY)),
		30, true);
	private static final ItemStack[] GUIItems = {
		GUIUtils.createBasicItem(Material.LIME_STAINED_GLASS_PANE, 1, "Easy", NamedTextColor.GREEN, false,
			Component.text("Shoot down normal targets to receive score.")
				.appendNewline()
				.append(Component.text("It will cost you " + CXP_COSTS[0] + " CXP to attempt this fight.", NamedTextColor.GRAY))
				.appendNewline()
				.append(Component.text("The Spiritsinger will award you one ticket per " + SpiritArcheryMinigame.scorePerTicketByDifficulty[0] + " score attained.", NamedTextColor.DARK_GRAY))
			, 30, true),
		GUIUtils.createBasicItem(Material.YELLOW_STAINED_GLASS_PANE, 1, "Medium", NamedTextColor.GOLD, false,
			Component.text("Avoid penalty targets that reduce score.")
				.appendNewline()
				.append(Component.text("It will cost you " + CXP_COSTS[1] + " CXP to attempt this fight.", NamedTextColor.GRAY))
				.appendNewline()
				.append(Component.text("The Spiritsinger will award you one ticket per " + SpiritArcheryMinigame.scorePerTicketByDifficulty[1] + " score attained.", NamedTextColor.DARK_GRAY))
			, 30, true),
		GUIUtils.createBasicItem(Material.RED_STAINED_GLASS_PANE, 1, "Hard", NamedTextColor.RED, false,
			Component.text("Prioritise darker targets that increase score by more.")
				.appendNewline()
				.append(Component.text("It will cost you " + CXP_COSTS[2] + " CXP to attempt this fight.", NamedTextColor.GRAY))
				.appendNewline()
				.append(Component.text("The Spiritsinger will award you one ticket per " + SpiritArcheryMinigame.scorePerTicketByDifficulty[2] + " score attained.", NamedTextColor.DARK_GRAY))
			, 30, true),
		GUIUtils.createBasicItem(Material.BLACK_STAINED_GLASS_PANE, 1, "Expert", NamedTextColor.DARK_RED, false,
			Component.text("Avoid darker red targets, or you'll lose more score.")
				.appendNewline()
				.append(Component.text("It will cost you " + CXP_COSTS[3] + " CXP to attempt this fight.", NamedTextColor.GRAY))
				.appendNewline()
				.append(Component.text("The Spiritsinger will award you one ticket per " + SpiritArcheryMinigame.scorePerTicketByDifficulty[3] + " score attained.", NamedTextColor.DARK_GRAY))
			, 30, true),
	};
	public final Location cornerLoc1 = new Location(mPlayer.getWorld(), 264, 135, -136);
	private final String[] conditions = {
		"OldLab",
		"White",
		"Magenta",
		"Yellow"
	};

	public SpiritArcheryGUI(Player player) {
		super(player, 3 * 9, Component.text("Spirit Archery"));
	}

	private static List<ItemStack> getCurrencyCost(int level, Player player) {
		List<ItemStack> costs = new ArrayList<>();

		ItemStack cxpCost = InventoryUtils.getItemFromLootTable(player, CXP_KEY);
		if (cxpCost != null) {
			cxpCost.setAmount(CXP_COSTS[level]);
			costs.add(cxpCost);
		}

		return costs;
	}

	@Override
	protected void setup() {
		setItem(0, 4, INFO_ITEM);
		setItem(0, 8, WARNING_ITEM);

		for (int iter = 0; iter < 4; iter++) {
			int i = iter; // Without this line, it complains about effectively non-final expressions in lambdas...
			if (ScoreboardUtils.getScoreboardValue(mPlayer, conditions[i]).orElse(0) > 0
				&& ScoreboardUtils.getScoreboardValue(mPlayer, SpiritArcheryMinigame.scoreboard).orElse(0) >= i) {
				setItem(1, 2 * i + 1, GUIItems[i])
					.onClick((clickEvent) -> {
						MinigameManager instance = MinigameManager.getInstance();
						if (instance.checkActiveMinigame(SpiritArcheryMinigame.ID)) {
							mPlayer.sendMessage(playerInMinigame);
							open();
						} else {
							List<ItemStack> mats = getCurrencyCost(i, mPlayer);
							if (WalletUtils.tryToPayFromInventoryAndWallet(mPlayer, mats)) {
								instance.start(
									SpiritArcheryMinigame.ID,
									SpiritArcheryMinigame.ID,
									cornerLoc1,
									mPlayer,
									Minigame.Arguments.of(Map.of("difficulty", (double) i)));
							} else {
								mPlayer.sendMessage(playerTooPoor);
							}
							close();
						}
					});
			} else {
				setItem(1, 2 * i + 1, GUIUtils.createBasicItem(Material.BARRIER, 1,
					"Not Unlocked", NamedTextColor.RED, true,
					Component.text("You are not yet ready to calm the song of these spirits.")
						.appendNewline()
						.append(Component.text("Defeat the " + (i != 0 ? conditions[i] : "Alchemy Labs") + " dungeon "
							+ ((iter > 0) ? "and master the previous Spirit Archery difficulty " : "")
							+ "to prove yourself worthy.")),
					30, true));
			}
		}
	}
}
