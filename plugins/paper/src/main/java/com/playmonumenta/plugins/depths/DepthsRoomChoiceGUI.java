package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DepthsRoomChoiceGUI extends CustomInventory {
	private static final Material NO_CHOICE = Material.BLACK_STAINED_GLASS_PANE;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final ArrayList<RoomChoice> ROOM_LOCATIONS = new ArrayList<>();

	public static final int LEVELSIX = 0x703663;

	static class RoomChoice {
		ItemStack mItem;
		Integer mLocation;
		DepthsRoomType mType;

		RoomChoice(Integer loc, DepthsRoomType t, ItemStack i) {
			mLocation = loc;
			mType = t;
			mItem = i;
		}
	}

	static {
		ROOM_LOCATIONS.add(new RoomChoice(0, DepthsRoomType.ABILITY,
			GUIUtils.createBasicItem(Material.BOOK, "Normal Room with Ability Reward",
				NamedTextColor.LIGHT_PURPLE,
				"Grants an ability upon clearing the room.")));
		ROOM_LOCATIONS.add(new RoomChoice(18, DepthsRoomType.ABILITY_ELITE,
			GUIUtils.createBasicItem(Material.ENCHANTED_BOOK, "Elite Room with Ability Reward",
				NamedTextColor.LIGHT_PURPLE,
				"Grants a powerful ability upon clearing the room.")));
		ROOM_LOCATIONS.add(new RoomChoice(22, DepthsRoomType.BOSS,
			GUIUtils.createBasicItem(Material.WITHER_ROSE, "Boss Challenge",
				NamedTextColor.LIGHT_PURPLE,
				"")));
		ROOM_LOCATIONS.add(new RoomChoice(8, DepthsRoomType.UPGRADE,
			GUIUtils.createBasicItem(Material.DAMAGED_ANVIL, "Normal Room with Upgrade Reward",
				NamedTextColor.LIGHT_PURPLE,
				"Grants an upgrade upon clearing the room.")));
		ROOM_LOCATIONS.add(new RoomChoice(26, DepthsRoomType.UPGRADE_ELITE,
			GUIUtils.createBasicItem(Material.ANVIL, "Elite Room with Upgrade Reward",
				NamedTextColor.LIGHT_PURPLE,
				"Grants a powerful upgrade upon clearing the room.")));
		ROOM_LOCATIONS.add(new RoomChoice(11, DepthsRoomType.TREASURE,
			GUIUtils.createBasicItem(Material.GOLD_NUGGET, "Normal Room with Treasure Reward",
				NamedTextColor.LIGHT_PURPLE,
				"Grants 3 treasure score for room completion.")));
		ROOM_LOCATIONS.add(new RoomChoice(15, DepthsRoomType.TREASURE_ELITE,
			GUIUtils.createBasicItem(Material.GOLD_INGOT, "Elite Room with Treasure Reward",
				NamedTextColor.LIGHT_PURPLE,
				"Grants 5 treasure score for room completion.")));
		ROOM_LOCATIONS.add(new RoomChoice(4, DepthsRoomType.UTILITY,
			GUIUtils.createBasicItem(Material.ENDER_CHEST, "Utility Room",
				NamedTextColor.LIGHT_PURPLE,
				"A non-combat room with a random other benefit.")));
		ROOM_LOCATIONS.add(new RoomChoice(13, DepthsRoomType.TWISTED,
			GUIUtils.createBasicItem(Material.BLACK_CONCRETE, 1,
				Component.text("XXXXXX", TextColor.color(LEVELSIX), TextDecoration.OBFUSCATED),
				"", NamedTextColor.GRAY, 30, true)));
	}


	public DepthsRoomChoiceGUI(Player player) {
		super(player, 27, "Select the Next Room Type");

		EnumSet<DepthsRoomType> roomChoices = DepthsManager.getInstance().generateRoomOptions(player);
		if (roomChoices == null) {
			return;
		}

		for (RoomChoice item : ROOM_LOCATIONS) {
			if (roomChoices.contains(item.mType)) {
				mInventory.setItem(item.mLocation, item.mItem);
			} else {
				mInventory.setItem(item.mLocation, new ItemStack(NO_CHOICE));
			}
		}
		fillEmpty();
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory ||
			    clickedItem == null ||
			    clickedItem.getType() == FILLER ||
			    clickedItem.getType() == NO_CHOICE ||
			    event.isShiftClick()) {
			return;
		}

		Player p = (Player) event.getWhoClicked();
		for (RoomChoice item : ROOM_LOCATIONS) {
			if (item.mLocation == event.getSlot()) {
				DepthsManager.getInstance().playerSelectedRoom(item.mType, p);
			}
		}

		event.getWhoClicked().closeInventory();
	}

	public void fillEmpty() {
		for (int i = 0; i < 27; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
	}
}
