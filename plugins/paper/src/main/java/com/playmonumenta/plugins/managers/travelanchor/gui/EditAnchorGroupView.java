package com.playmonumenta.plugins.managers.travelanchor.gui;

import com.playmonumenta.plugins.managers.travelanchor.AnchorGroup;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;

public class EditAnchorGroupView extends AnchorGroupView {
	private final AnchorGroup mAnchorGroup;

	public EditAnchorGroupView(AnchorGroupGui gui, AnchorGroup anchorGroup) {
		super(gui);
		mAnchorGroup = anchorGroup;
	}

	@Override
	public void setup() {
		mGui.setTitle(Component.text("Editing ", NamedTextColor.DARK_GRAY)
			.append(Component.text(mAnchorGroup.name(), NamedTextColor.BLACK)));

		mGui.setItem(0, 0, GUIUtils.createBasicItem(Material.ARROW, "Back", NamedTextColor.GRAY))
			.onClick(event -> mGui.setView(new SelectAnchorGroupsView(mGui)));

		mGui.setAnchorGroupIcon(0, 4, mAnchorGroup);

		mGui.setItem(3, 3, GUIUtils.createBasicItem(Material.BAMBOO_SIGN, "Rename Group", NamedTextColor.GREEN))
			.onClick(event -> {
				mGui.close();
				String oldName = mAnchorGroup.name();
				int spaceIndex = getSpaceIndex(oldName);

				SignUtils.newMenu(List.of(
						oldName.substring(0, spaceIndex).trim(),
						oldName.substring(spaceIndex).trim(),
						"~~~~~~~~~~~",
						"Group Name"
					))
					.reopenIfFail(false)
					.response((player, text) -> {
						String groupName = (
							StringUtils.substring(text[0], 0, 24).trim()
								+ " "
								+ StringUtils.substring(text[1], 0, 24).trim()
						).trim();

						if (groupName.isBlank()) {
							player.sendMessage(Component.text("Group names cannot be blank; keeping old name", NamedTextColor.RED));
							player.playSound(player, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
							mGui.open();
							return true;
						}

						if (!mGui.mWorldAnchorGroups.renameGroup(mAnchorGroup, groupName)) {
							player.sendMessage(Component.text("Group name already exists; try editing the existing group instead?", NamedTextColor.RED));
							player.playSound(player, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else {
							player.playSound(player, Sound.ENTITY_SHULKER_CLOSE, SoundCategory.PLAYERS, 1.0f, 1.0f);
						}
						mGui.open();
						return true;
					})
					.open(mGui.mPlayer);
			});

		if (mAnchorGroup.deletable()) {
			mGui.setItem(3, 5, GUIUtils.createBasicItem(Material.FLINT_AND_STEEL, "Delete Group", NamedTextColor.RED))
				.onClick(event -> {
					mGui.mPlayer.playSound(mGui.mPlayer, Sound.ENTITY_SHULKER_DEATH, SoundCategory.PLAYERS, 1.0F, 1.0F);
					mGui.mWorldAnchorGroups.deleteGroup(mAnchorGroup);
					mGui.setView(new SelectAnchorGroupsView(mGui));
				});
		} else {
			mGui.setItem(3, 5, GUIUtils.createBasicItem(Material.FLINT_AND_STEEL, "Cannot Delete This Group", NamedTextColor.RED))
				.onClick(event -> mGui.mPlayer.playSound(mGui.mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f));
		}
	}

	@Override
	protected void handleShiftClickedFromInventory(ItemStack item) {
		mAnchorGroup.item(item);
		mGui.mPlayer.playSound(mGui.mPlayer, Sound.ENTITY_SHULKER_CLOSE, SoundCategory.PLAYERS, 1.0F, 1.0F);
		mGui.update();
	}

	private static int getSpaceIndex(String existingName) {
		int spaceIndex1 = existingName.indexOf(' ', existingName.length() / 2);
		int spaceIndex2 = existingName.lastIndexOf(' ', existingName.length() / 2);
		int spaceIndex = spaceIndex2 < 0 || (spaceIndex1 >= 0 && Math.abs(spaceIndex1 - existingName.length() / 2) < Math.abs(spaceIndex2 - existingName.length() / 2))
			? spaceIndex1 : spaceIndex2;
		if (spaceIndex < 0) {
			spaceIndex = existingName.length();
		}
		return spaceIndex;
	}
}
