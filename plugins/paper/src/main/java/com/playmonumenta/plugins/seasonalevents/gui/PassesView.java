package com.playmonumenta.plugins.seasonalevents.gui;

import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.seasonalevents.SeasonalPass;
import com.playmonumenta.plugins.utils.DateUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PassesView extends View {
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy");
	private static final int PAGE_DELTA = 3;
	private static final int COLUMNS_SHOWN = 6;

	int mStartColumn;

	public PassesView(PassGui gui) {
		super(gui);
		// Start with the active pass all the way on the right (bounds checked later)
		mStartColumn = getMaxColumn();
	}

	@Override
	public void setup(Player displayedPlayer) {
		ItemStack item;
		ItemMeta meta;

		// Page bounds check
		int maxColumn = getMaxColumn();
		mStartColumn = Math.max(0, Math.min(mStartColumn, maxColumn));

		if (mStartColumn > 0) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Previous Passes", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(0, 1, item).onClick((InventoryClickEvent event) -> prevPage());
		}

		if (mStartColumn < maxColumn) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Next Passes", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(0, 1, item).onClick((InventoryClickEvent event) -> nextPage());
		}

		Iterator<SeasonalPass> passIter = SeasonalEventManager.mAllPasses.values().iterator();
		for (int column = -mStartColumn; column < COLUMNS_SHOWN; column++) {
			int x = column + 2;
			for (int row = 0; row < 4; row++) {
				if (!passIter.hasNext()) {
					return;
				}

				SeasonalPass pass = passIter.next();
				int weekOfPass = pass.getWeekOfPass();
				if (weekOfPass <= 0) {
					// future pass, done
					return;
				}

				item = new ItemStack(pass.mDisplayItem);
				meta = item.getItemMeta();
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
				meta.displayName(Component.text(pass.mName, pass.mNameColor, TextDecoration.BOLD)
					.decoration(TextDecoration.ITALIC, false));

				if (pass.isActive()) {
					meta.lore(List.of(Component.text("Active pass since "
							+ pass.mPassStart.format(DATE_FORMAT), NamedTextColor.WHITE)
						.decoration(TextDecoration.ITALIC, false)));
				} else {
					LocalDateTime passEnd = pass.mPassStart.plusWeeks(pass.mNumberOfWeeks).minusDays(1);

					meta.lore(List.of(Component.text("Ran from "
							+ pass.mPassStart.format(DATE_FORMAT)
							+ " to "
							+ passEnd.format(DATE_FORMAT), NamedTextColor.WHITE)
						.decoration(TextDecoration.ITALIC, false)));
				}

				item.setItemMeta(meta);
				int y = row + 1;
				mGui.setItem(y, x, item)
					.onLeftClick(() -> {
						mGui.mPass = pass;
						if (pass.isActive()) {
							mGui.mDisplayedEpochWeek = mGui.mOpenedEpochWeek;
						} else {
							mGui.mDisplayedEpochWeek = DateUtils.getWeeklyVersion(pass.mPassStart);
						}
						mGui.mView = new WeekView(mGui);
						mGui.updateWithPageSound();
					});
			}
		}
	}

	private int getMaxColumn() {
		return (SeasonalEventManager.mAllPasses.size() / 4) - (COLUMNS_SHOWN - 1);
	}

	public void prevPage() {
		mStartColumn -= PAGE_DELTA;
		mGui.updateWithPageSound();
	}

	public void nextPage() {
		mStartColumn += PAGE_DELTA;
		mGui.updateWithPageSound();
	}
}
