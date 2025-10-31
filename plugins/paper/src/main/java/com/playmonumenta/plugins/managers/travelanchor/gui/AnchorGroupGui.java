package com.playmonumenta.plugins.managers.travelanchor.gui;

import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.managers.travelanchor.AnchorGroup;
import com.playmonumenta.plugins.managers.travelanchor.EntityTravelAnchor;
import com.playmonumenta.plugins.managers.travelanchor.TravelAnchorManager;
import com.playmonumenta.plugins.managers.travelanchor.WorldAnchorGroups;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AnchorGroupGui extends Gui {
	protected EntityTravelAnchor mTravelAnchorEntity;
	protected WorldAnchorGroups mWorldAnchorGroups;
	protected AnchorGroupView mView;

	public AnchorGroupGui(Player player, EntityTravelAnchor entityTravelAnchor) {
		super(player, 6 * 9, Component.text("Anchor Groups"));
		mTravelAnchorEntity = entityTravelAnchor;
		mWorldAnchorGroups = TravelAnchorManager.getInstance()
			.anchorsInWorld(player.getWorld())
			.getAnchorGroups();
		mView = new SelectAnchorGroupsView(this);
	}

	@Override
	protected void setup() {
		mView.setup();
	}

	protected void setView(AnchorGroupView view) {
		mView = view;
		mPlayer.playSound(mPlayer, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
		update();
	}

	protected void setAnchorIcon() {
		TextColor textColor = mTravelAnchorEntity.color();
		DyeColor dyeColor = DyeColor.getByColor(Color.fromRGB(textColor.red(), textColor.green(), textColor.blue()));
		Material shulkerBoxMat = ItemUtils.shulkerBoxOfDyeColor(dyeColor);

		ItemStack item = GUIUtils.createBasicItem(
			shulkerBoxMat,
			mTravelAnchorEntity.label(),
			textColor,
			"Add or remove this travel to/from a group by left clicking the group.\nEdit a group by right clicking it.\nCreate a new group by shift clicking an item into this GUI."
		);

		setItem(0, 4, item);
	}

	protected void setAnchorGroupIcon(int row, int column, AnchorGroup anchorGroup) {
		boolean isInGroup = mTravelAnchorEntity.inGroup(anchorGroup);

		ItemStack item = anchorGroup.item(isInGroup ? NamedTextColor.GREEN : NamedTextColor.RED);
		ItemMeta meta = item.getItemMeta();
		List<Component> lore = meta.lore();
		if (lore == null) {
			lore = new ArrayList<>();
		}

		if (mView instanceof SelectAnchorGroupsView) {
			if (isInGroup) {
				lore.add(Component.text("Left click to remove from group", NamedTextColor.RED));
			} else {
				lore.add(Component.text("Left click to add to group", NamedTextColor.GREEN));
			}
			lore.add(Component.text("Right click to edit group", NamedTextColor.YELLOW));
		} else if (mView instanceof EditAnchorGroupView) {
			lore.add(Component.text("Shift click an item from your inventory to change the group icon", NamedTextColor.YELLOW));
		}

		meta.lore(lore);
		item.setItemMeta(meta);

		setItem(row, column, item)
			.onLeftClick(() -> {
				if (isInGroup) {
					if (mTravelAnchorEntity.removeFromGroup(anchorGroup)) {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_CLOSE, SoundCategory.PLAYERS, 1.0f, 1.0f);
					} else {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.sendMessage(Component.text("Travel Anchors must be in at least one group; how about the default group? Or a new one?", NamedTextColor.RED));
					}
				} else {
					mTravelAnchorEntity.addToGroup(anchorGroup);
					mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
				update();
			})
			.onRightClick(() -> setView(new EditAnchorGroupView(this, anchorGroup)));
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (event.getClick() == ClickType.SHIFT_LEFT && item != null && !item.getType().isAir()) {
			mView.handleShiftClickedFromInventory(item);
		}
	}
}
