package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsContent;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsRarity;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DepthsRoomChoiceGUI extends Gui {
	private static final ItemStack NO_CHOICE = GUIUtils.createFiller(Material.BLACK_STAINED_GLASS_PANE);
	private static final List<RoomChoice> ROOM_LOCATIONS = new ArrayList<>();

	private record RoomChoice(int mLocation, DepthsRoomType mType, ItemStack mStack) {
		public GuiItem onLeftClick(Runnable runnable) {
			return new GuiItem(mStack).onLeftClick(runnable);
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
		if (DepthsUtils.getDepthsContent() == DepthsContent.DARKEST_DEPTHS) {
			ROOM_LOCATIONS.add(new RoomChoice(13, DepthsRoomType.TWISTED,
				GUIUtils.createBasicItem(Material.BLACK_CONCRETE, 1,
					DepthsRarity.TWISTED.getDisplay(),
					"", NamedTextColor.GRAY, 30, true)));
		} else if (DepthsUtils.getDepthsContent() == DepthsContent.CELESTIAL_ZENITH) {
			ROOM_LOCATIONS.add(new RoomChoice(13, DepthsRoomType.WILDCARD,
				GUIUtils.createBasicItem(Material.GOLD_NUGGET, "Wildcard Room",
					NamedTextColor.LIGHT_PURPLE,
					"Could be anything! Grants 1 treasure score for taking.")));
		}

	}


	public DepthsRoomChoiceGUI(Player player) {
		super(player, 27, "Select the Next Room Type");
	}

	@Override
	public void setup() {
		EnumSet<DepthsRoomType> roomChoices = DepthsManager.getInstance().generateRoomOptions(mPlayer);
		if (roomChoices == null) {
			return;
		}

		for (RoomChoice item : ROOM_LOCATIONS) {
			if (roomChoices.contains(item.mType)) {
				setItem(item.mLocation, item.onLeftClick(() -> {
					DepthsManager.getInstance().playerSelectedRoom(item.mType, mPlayer);
					close();

					DepthsParty party = DepthsManager.getInstance().getDepthsParty(mPlayer);
					if (party != null) {
						for (Player p: party.getPlayers()) {
							if (getOpenGui(p) instanceof DepthsRoomChoiceGUI gui) {
								gui.close();
							}
						}
					}
				}));
			} else {
				setItem(item.mLocation, new GuiItem(NO_CHOICE));
			}
		}
	}
}
