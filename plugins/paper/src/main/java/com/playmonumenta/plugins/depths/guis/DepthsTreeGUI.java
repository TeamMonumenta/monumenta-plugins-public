package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DepthsTreeGUI extends Gui {

	private final List<DepthsTree> mChoices;
	private final int mTreasureIndex;

	public DepthsTreeGUI(Player player, List<DepthsTree> choices, int treasureIndex) {
		super(player, 3 * 9, "Select a Tree");
		mChoices = choices;
		mTreasureIndex = treasureIndex;
	}

	@Override
	protected void setup() {
		setItem(1, 1, createTreeChoice(0)).onLeftClick(() -> treeSelected(0));
		setItem(1, 4, createTreeChoice(1)).onLeftClick(() -> treeSelected(1));
		setItem(1, 7, createTreeChoice(2)).onLeftClick(() -> treeSelected(2));
	}

	private ItemStack createTreeChoice(int index) {
		ItemStack item = mChoices.get(index).createItem();

		if (index == mTreasureIndex) {
			GUIUtils.splitLoreLine(item, "\nSelecting this tree will increase your treasure score by 15% this run!", NamedTextColor.YELLOW, 40, false);
			GUIUtils.splitLoreLine(item, "(Capped at +10 score.)", NamedTextColor.YELLOW, 40, false);
		}

		return item;
	}

	private void treeSelected(int index) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(mPlayer);
		if (dp == null) {
			return;
		}

		DepthsTree tree = mChoices.get(index);

		dp.mEligibleTrees.add(tree);
		if (index == mTreasureIndex) {
			dp.mBonusTreeSelected = true;
			dp.sendMessage(
				Component.text("You have selected the ")
					.append(tree.getNameComponent())
					.append(Component.text(" tree, and will receive 15% more treasure score at the end of this run!"))
			);
		} else {
			dp.sendMessage(
				Component.text("You have selected the ")
					.append(tree.getNameComponent())
					.append(Component.text(" tree!"))
			);
		}

		// give the player 3 other random trees
		List<DepthsTree> allTrees = new ArrayList<>(List.of(DepthsTree.OWNABLE_TREES));
		allTrees.removeAll(dp.mEligibleTrees);
		Collections.shuffle(allTrees);
		for (int i = 0; i < DepthsManager.NUM_TREES_PER_RUN - 1; i++) {
			dp.mEligibleTrees.add(allTrees.get(i));
		}

		close();
	}
}
