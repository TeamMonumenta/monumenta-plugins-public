package com.playmonumenta.plugins.plots;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.plots.PlotManager.PlotInfo;
import com.playmonumenta.plugins.plots.PlotManager.PlotInfo.OtherAccessRecord;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

public class PlotAccessGui extends Gui {
	private static final ArrayList<Integer> GUI_LOCATIONS = new ArrayList<>(Arrays.asList(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43));

	private final ArrayList<PlotEntry> mAccessList = new ArrayList<>();
	private final PlotInfo mPlotInfo;
	private final GuiMode mGuiMode;
	private final int mNumPages;
	private int mCurrentPage = 1;

	public enum GuiMode {
		TELEPORT_MODE
	}

	private static class PlotEntry {
		boolean mSelf;
		@Nullable OtherAccessRecord mEntry;

		public PlotEntry(@Nullable OtherAccessRecord record) {
			mSelf = false;
			mEntry = record;
		}

		public PlotEntry() {
			mSelf = true;
			mEntry = null;
		}
	}

	public PlotAccessGui(Player player, PlotInfo plotInfo, GuiMode guiMode) {
		super(player, 6 * 9, Component.text("Available Plots"));
		mPlotInfo = plotInfo;
		mGuiMode = guiMode;

		if (mPlotInfo.mOwnedPlotId > 0) {
			mAccessList.add(new PlotEntry());
		}

		plotInfo.mOwnerAccessToOtherPlots.values().stream()
			.sorted(Comparator.comparing((OtherAccessRecord access) -> access.mName == null ? "" : access.mName)
				.thenComparingInt(access -> access.mPlotId))
			.forEach(access -> mAccessList.add(new PlotEntry(access)));

		mNumPages = (int) Math.ceil((double) mAccessList.size() / (double) GUI_LOCATIONS.size());
	}

	@Override
	protected void setup() {
		if (mGuiMode == GuiMode.TELEPORT_MODE) {
			setLayoutForTeleport();
		}
	}

	private void setLayoutForTeleport() {
		int pageOffset = (mCurrentPage - 1) * GUI_LOCATIONS.size();

		for (int i = 0; i < GUI_LOCATIONS.size(); i++) {
			if (i + pageOffset < mAccessList.size()) {
				PlotEntry destination = mAccessList.get(i + pageOffset);

				setItem(GUI_LOCATIONS.get(i), new GuiItem(createHead(destination)))
					.onClick(event -> {
						if (destination.mSelf) {
							ScoreboardUtils.setScoreboardValue(mPlayer, Constants.Objectives.CURRENT_PLOT, mPlotInfo.mOwnedPlotId);
						} else if (destination.mEntry != null) {
							ScoreboardUtils.setScoreboardValue(mPlayer, Constants.Objectives.CURRENT_PLOT, destination.mEntry.mPlotId);
						}

						PlotManager.sendPlayerToPlot(mPlayer);
						close();
					});
			}
		}

		setItem(4, new GuiItem(GUIUtils.createBasicItem(Material.SCUTE, "Plot Selection", NamedTextColor.AQUA, false, "Click the head of the plot you would like to visit.", NamedTextColor.GOLD)));
		createControlButtons();
	}

	private void createControlButtons() {
		if (mCurrentPage > 1) {
			setItem(0, new GuiItem(GUIUtils.createBasicItem(Material.ARROW, "Back", NamedTextColor.GRAY, false, "Click to go to page " + (mCurrentPage - 1), NamedTextColor.GRAY)))
				.onClick(event -> {
					mCurrentPage -= 1;
					update();
				});
		}

		if (mCurrentPage < mNumPages) {
			setItem(8, new GuiItem(GUIUtils.createBasicItem(Material.ARROW, "Next", NamedTextColor.GRAY, false, "Click to go to page " + (mCurrentPage + 1), NamedTextColor.GRAY)))
				.onClick(event -> {
					mCurrentPage += 1;
					update();
				});
		}
	}

	private ItemStack createHead(PlotEntry record) {
		if (record.mSelf) {
			return createYourHead();
		} else {
			if (record.mEntry != null && record.mEntry.mHead != null) {
				return record.mEntry.mHead;
			}

			return new ItemStack(Material.PLAYER_HEAD);
		}
	}

	private ItemStack createYourHead() {
		ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) head.getItemMeta();

		meta.setOwningPlayer(mPlayer);
		meta.displayName(Component.text("Your Plot", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		head.setItemMeta(meta);

		return head;
	}
}
