package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.commands.CharmsCommand;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmsGUI;
import com.playmonumenta.plugins.itemstats.gui.PlayerItemStatsGUI;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class PlayerDisplayCustomInventory extends CustomInventory {
	private static final int INV_LOC = 10;
	private static final int CLASS_LOC = 12;
	private static final int PS_LOC = 14;
	private static final int CHARMS_LOC = 16;
	private static final MonumentaClasses mClasses = new MonumentaClasses();
	private static final String mZenithKey = "monumenta:dungeons/zenith/find";
	private final Player mRequestingPlayer;
	private final Player mTargetPlayer;

	public PlayerDisplayCustomInventory(Player requestingPlayer, Player clickedPlayer) {
		super(requestingPlayer, 27, clickedPlayer.getName() + "'s Details");
		mTargetPlayer = clickedPlayer;
		mRequestingPlayer = requestingPlayer;
		setLayout(clickedPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		switch (event.getSlot()) {
			case INV_LOC -> {
				mInventory.close();
				new PlayerInventoryCustomInventory(mRequestingPlayer, mTargetPlayer, true).openInventory(mRequestingPlayer, Plugin.getInstance());
			}
			case CLASS_LOC -> {
				if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.BARRIER) {
					mInventory.close();
					new ClassDisplayCustomInventory(mRequestingPlayer, mTargetPlayer, true).open();
				}
			}
			case PS_LOC -> {
				mInventory.close();
				new PlayerItemStatsGUI(mRequestingPlayer, mTargetPlayer, true).openInventory(mRequestingPlayer, Plugin.getInstance());
			}
			case CHARMS_LOC -> {
				if (CharmsCommand.checkZone(mRequestingPlayer)) {
					return;
				}

				boolean zenithAdvancement = AdvancementUtils.checkAdvancement(mTargetPlayer, mZenithKey);
				int charmPower = ScoreboardUtils.getScoreboardValue(mTargetPlayer, AbilityUtils.CHARM_POWER).orElse(0);
				if (event.isLeftClick()
					&& (charmPower > 0 || (CharmManager.getInstance().mEnabledCharmType == CharmManager.CharmType.ZENITH && zenithAdvancement))) {
					mInventory.close();
					new CharmsGUI(mRequestingPlayer, mTargetPlayer, false, true).open();
				} else if (event.isRightClick() && zenithAdvancement) {
					mInventory.close();
					new CharmsGUI(mRequestingPlayer, mTargetPlayer, false, CharmManager.CharmType.ZENITH, true).open();
				} else {
					mRequestingPlayer.sendMessage("No valid charms UI to show.");
					if (!zenithAdvancement) {
						mRequestingPlayer.sendMessage("Zenith advancement progress is not done");
					}
					mRequestingPlayer.sendMessage("Charm power: " + charmPower);

				}
			}
			default -> {
			}
		}
	}

	public void setLayout(Player clickedPlayer) {
		mInventory.clear();
		int currentClass = AbilityUtils.getClassNum(clickedPlayer);
		PlayerClass playerClass = null;
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (currentClass == oneClass.mClass) {
				playerClass = oneClass;
				break;
			}
		}

		mInventory.setItem(INV_LOC, GUIUtils.createBasicItem(Material.CHEST, "Inventory",
			NamedTextColor.AQUA, true, "View " + clickedPlayer.getName() + "'s inventory", NamedTextColor.GRAY));
		if (playerClass != null) {
			mInventory.setItem(CLASS_LOC, GUIUtils.createBasicItem(playerClass.mDisplayItem, "Class Details",
				NamedTextColor.AQUA, true, "View " + clickedPlayer.getName() + "'s class information", NamedTextColor.GRAY));
		} else {
			mInventory.setItem(CLASS_LOC, GUIUtils.createBasicItem(Material.BARRIER, clickedPlayer.getName() + " has no selected class.",
				NamedTextColor.RED, true, "", NamedTextColor.GRAY));
		}
		mInventory.setItem(PS_LOC, GUIUtils.createBasicItem(Material.BOOK, "Item Statistics",
			NamedTextColor.AQUA, true, "View " + clickedPlayer.getName() + "'s gear statistics.", NamedTextColor.GRAY));

		mInventory.setItem(CHARMS_LOC, GUIUtils.createBasicItem(Material.NETHER_STAR, "Charms",
			NamedTextColor.AQUA, true, "Left click to view " + clickedPlayer.getName() + "'s charms, "
				+ "right click to view their Zenith charms", NamedTextColor.GRAY));
	}
}
