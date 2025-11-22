package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.market.MarketPlayerOptions;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TabEditOptions implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Market Options Editor");
	static final int TAB_SIZE = 9;
	MarketPlayerOptions mOptions;

	public TabEditOptions(MarketGui marketGUI) {
		this.mGui = marketGUI;
		mOptions = MarketManager.getMarketPlayerOptions(mGui.mPlayer);
	}

	@Override
	public void setup() {
		mGui.setItem(0, MarketGuiIcons.BACK_TO_MAIN_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MAIN_MENU));

		mGui.setItem(2, buildNotificationShardsIcon()).onClick((clickEvent) -> notificationShardsAction(clickEvent));
	}

	private void notificationShardsAction(InventoryClickEvent clickEvent) {
		List<MarketPlayerOptions.NotificationShard> values = List.of(MarketPlayerOptions.NotificationShard.values());
		int idx = values.indexOf(mOptions.getShardsForNotification());
		int hotbarInt = clickEvent.getHotbarButton();
		if (hotbarInt >= 0 && hotbarInt < values.size()) {
			idx = hotbarInt;
		} else {
			idx = mGui.commonMultiplierSelection(clickEvent, idx + 1, values.size()) - 1;
		}
		mOptions.setmShardsForNotification(values.get(idx));
		mGui.update();
	}

	private GuiItem buildNotificationShardsIcon() {
		List<Component> lore = new ArrayList<>();

		Component name = Component.text("Notification Shards", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);

		lore.add(Component.text("Click to select on which shards", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("should the market notifications", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("be displayed to you.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.empty());

		MarketPlayerOptions.NotificationShard selected = mOptions.getShardsForNotification();
		for (MarketPlayerOptions.NotificationShard value : MarketPlayerOptions.NotificationShard.values()) {
			NamedTextColor color = NamedTextColor.GRAY;
			String header = " ";
			if (value == selected) {
				color = NamedTextColor.GREEN;
				header = "⌲";
			}

			lore.add(Component.text(header + "[" + value.getShortDisplay() + "]", color));
		}

		return new GuiItem(GUIUtils.createBasicItem(Material.BELL, 1, name, lore, false), false);
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
		mOptions = MarketManager.getMarketPlayerOptions(mGui.mPlayer);
	}

	@Override
	public void onLeave() {

	}
}
