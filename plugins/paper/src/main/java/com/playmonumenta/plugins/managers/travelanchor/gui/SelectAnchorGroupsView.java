package com.playmonumenta.plugins.managers.travelanchor.gui;

import com.playmonumenta.plugins.managers.travelanchor.AnchorGroup;
import com.playmonumenta.plugins.utils.SignUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;

public class SelectAnchorGroupsView extends AnchorGroupView {
	protected static final int PAGE_START_X = 0;
	protected static final int PAGE_START_Y = 1;
	protected static final int PAGE_WIDTH = 9;

	public SelectAnchorGroupsView(AnchorGroupGui gui) {
		super(gui);
	}

	@Override
	public void setup() {
		List<AnchorGroup> anchorGroups = mGui.mWorldAnchorGroups.anchorGroups();
		int totalRows = Math.floorDiv((anchorGroups.size() + PAGE_WIDTH - 1), PAGE_WIDTH);
		setPageArrows(totalRows);

		mGui.setTitle(Component.text("Select Anchor Groups", NamedTextColor.DARK_GRAY));

		mGui.setAnchorIcon();

		int index = 0;
		for (int y = 0; y < rowsPerPage(); y++) {
			if (index >= anchorGroups.size()) {
				break;
			}

			for (int x = 0; x < PAGE_WIDTH; x++) {
				index = (mPage * rowsPerPage() + y) * PAGE_WIDTH + x;
				if (index >= anchorGroups.size()) {
					break;
				}

				mGui.setAnchorGroupIcon(
					PAGE_START_Y + y,
					PAGE_START_X + x,
					anchorGroups.get(index)
				);
			}
		}
	}

	@Override
	protected void handleShiftClickedFromInventory(ItemStack item) {
		mGui.close();
		SignUtils.newMenu(List.of("", "", "~~~~~~~~~~~", "Group Name"))
			.reopenIfFail(false)
			.response((player, text) -> {
				String groupName = (
					StringUtils.substring(text[0], 0, 24).trim()
						+ " "
						+ StringUtils.substring(text[1], 0, 24).trim()
				).trim();

				if (groupName.isBlank()) {
					player.sendMessage(Component.text("Group names cannot be blank", NamedTextColor.RED));
					player.playSound(player, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					mGui.open();
					return true;
				}

				AnchorGroup anchorGroup = new AnchorGroup(groupName, item.getType());
				if (!mGui.mWorldAnchorGroups.addGroup(anchorGroup)) {
					player.sendMessage(Component.text("Group name already exists; try editing the existing group instead?", NamedTextColor.RED));
					player.playSound(player, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				} else {
					player.playSound(player, Sound.ENTITY_SHULKER_CLOSE, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
				mGui.open();
				return true;
			})
			.open(mGui.mPlayer);
	}
}
