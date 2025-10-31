package com.playmonumenta.plugins.plots;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.plots.PlotManager.PlotInfo;
import com.playmonumenta.plugins.plots.PlotManager.PlotInfo.OtherAccessToOwnerPlotRecord;
import com.playmonumenta.plugins.plots.PlotManager.PlotInfo.OwnerAccessToOtherPlotsRecord;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

public class PlotAccessGui extends Gui {
	private static final ArrayList<Integer> GUI_LOCATIONS = new ArrayList<>(Arrays.asList(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43));
	private AccessInfoGuiMode mAccessInfoGuiMode = AccessInfoGuiMode.INACTIVE;
	private int mTotalPages;
	private int mCurrentPage = 1;
	private @Nullable PlotEntry mSelectedPlotEntry = null;

	private final MainGuiMode mMainGuiMode;
	private final Player mViewer;
	private final boolean mIsSelf;
	private final String mOwnerName;
	private final UUID mOwnerUuid;
	private final PlotInfo mOwnerPlotInfo;
	private final ArrayList<PlotEntry> mOtherAccessToOwnerPlotList = new ArrayList<>();
	private final ArrayList<PlotEntry> mOwnerAccessToOtherPlotsList = new ArrayList<>();

	public enum MainGuiMode {
		ACCESS_INFO_MODE,
		TELEPORT_MODE
	}

	private enum AccessInfoGuiMode {
		INACTIVE,
		OTHER_ACCESS_TO_OWNER_PLOT,
		OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL,
		OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL_CONFIRMATION,
		OWNER_ACCESS_TO_OTHER_PLOTS,
		OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL,
		OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL_CONFIRMATION
	}

	private static class PlotEntry {
		boolean mSelf;
		@Nullable OtherAccessToOwnerPlotRecord mOtherAccessToOwnerPlotEntry;
		@Nullable OwnerAccessToOtherPlotsRecord mOwnerAccessToOtherPlotsEntry;

		// Refers to the owner's plot
		private PlotEntry() {
			mSelf = true;
			mOtherAccessToOwnerPlotEntry = null;
			mOwnerAccessToOtherPlotsEntry = null;
		}

		// Access that other players have to owner's plot
		private PlotEntry(@Nullable OtherAccessToOwnerPlotRecord record) {
			mSelf = false;
			mOtherAccessToOwnerPlotEntry = record;
			mOwnerAccessToOtherPlotsEntry = null;
		}

		// Access the owner has to other plots
		private PlotEntry(@Nullable OwnerAccessToOtherPlotsRecord record) {
			mSelf = false;
			mOtherAccessToOwnerPlotEntry = null;
			mOwnerAccessToOtherPlotsEntry = record;
		}
	}

	public PlotAccessGui(Player viewer, String ownerUsername, UUID ownerUuid, PlotInfo ownerPlotInfo, MainGuiMode mainGuiMode, Component title) {
		super(viewer, 6 * 9, title);
		mMainGuiMode = mainGuiMode;
		mViewer = viewer;
		mIsSelf = mViewer.getUniqueId().equals(ownerUuid);
		mOwnerName = ownerUsername;
		mOwnerUuid = ownerUuid;
		mOwnerPlotInfo = ownerPlotInfo;

		// Only parse the player's own plot for TELEPORT_MODE and not ACCESS_INFO_MODE
		if (mOwnerPlotInfo.mOwnedPlotId > 0 && mMainGuiMode == MainGuiMode.TELEPORT_MODE) {
			mOwnerAccessToOtherPlotsList.add(new PlotEntry());
		}

		populateOtherAccessToOwnerPlotList();
		populateOwnerAccessToOtherPlotsList();
	}

	private void refreshData() {
		PlotManager.getPlotInfo(mOwnerUuid).thenCompose(PlotInfo::populateNamesAndHeads).whenComplete((info, ex) -> {
			if (ex != null) {
				MMLog.severe("Caught exception while refreshing plot access for " + mOwnerName + ": " + ex.getMessage());
				mViewer.sendMessage(Component.text("An error occurred while refreshing the plot access information. Please report this: " + ex.getMessage(), NamedTextColor.RED));
				MessagingUtils.sendStackTrace(mViewer, ex);
				close();
				return;
			}

			mOwnerPlotInfo.mOtherAccessToOwnerPlot.keySet().retainAll(info.mOtherAccessToOwnerPlot.keySet());
			mOwnerPlotInfo.mOtherAccessToOwnerPlot.putAll(info.mOtherAccessToOwnerPlot);

			mOwnerPlotInfo.mOwnerAccessToOtherPlots.keySet().retainAll(info.mOwnerAccessToOtherPlots.keySet());
			mOwnerPlotInfo.mOwnerAccessToOtherPlots.putAll(info.mOwnerAccessToOtherPlots);

			mOtherAccessToOwnerPlotList.clear();
			populateOtherAccessToOwnerPlotList();

			mOwnerAccessToOtherPlotsList.clear();
			populateOwnerAccessToOtherPlotsList();

			update();
		});
	}

	private void populateOtherAccessToOwnerPlotList() {
		mOwnerPlotInfo.mOtherAccessToOwnerPlot.values().stream()
			.sorted(Comparator.comparing((OtherAccessToOwnerPlotRecord access) -> access.mName == null ? "" : access.mName))
			.forEach(access -> mOtherAccessToOwnerPlotList.add(new PlotEntry(access)));
	}

	private void populateOwnerAccessToOtherPlotsList() {
		mOwnerPlotInfo.mOwnerAccessToOtherPlots.values().stream()
			.sorted(Comparator.comparing((OwnerAccessToOtherPlotsRecord access) -> access.mName == null ? "" : access.mName)
				.thenComparingInt(access -> access.mPlotId))
			.forEach(access -> mOwnerAccessToOtherPlotsList.add(new PlotEntry(access)));
	}

	@Override
	protected void setup() {
		if (mMainGuiMode == MainGuiMode.ACCESS_INFO_MODE) {
			switch (mAccessInfoGuiMode) {
				case INACTIVE -> setLayoutForAccessInfo();
				case OTHER_ACCESS_TO_OWNER_PLOT ->
					setLayout(mOtherAccessToOwnerPlotList, "No one has access to " + (mIsSelf ? "your " : mOwnerName + "'s ") + "plot!", null, false, false);
				case OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL ->
					setLayout(mOtherAccessToOwnerPlotList, "No one has access to " + (mIsSelf ? "your " : mOwnerName + "'s ") + "plot!", AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL_CONFIRMATION, true, false);
				case OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL_CONFIRMATION -> {
					if (mSelectedPlotEntry == null) {
						MMLog.severe("mSelectedPlotEntry was still null by the time it was called by OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL_CONFIRMATION in PlotAccessGui.java");
						mViewer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
						close();
						return;
					}

					setLayoutForOtherAccessToOwnerPlotRemovalConfirmation(mSelectedPlotEntry);
				}

				case OWNER_ACCESS_TO_OTHER_PLOTS ->
					setLayout(mOwnerAccessToOtherPlotsList, (mIsSelf ? "You don't " : mOwnerName + " doesn't ") + "have access to any plots!", null, false, false);
				case OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL ->
					setLayout(mOwnerAccessToOtherPlotsList, (mIsSelf ? "You don't " : mOwnerName + " doesn't ") + "have access to any plots!", AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL_CONFIRMATION, true, false);
				case OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL_CONFIRMATION -> {
					if (mSelectedPlotEntry == null) {
						MMLog.severe("mSelectedPlotEntry was still null by the time it was called by OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL_CONFIRMATION in PlotAccessGui.java");
						mViewer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
						close();
						return;
					}

					setLayoutForOwnerAccessToOtherPlotsRemovalConfirmation(mSelectedPlotEntry);
				}

				default -> {
					MMLog.severe("Couldn't find a matching switch case to open '/plot access info' for player in PlotAccessGui.java");
					mViewer.sendMessage(Component.text("An error occurred while setting the layout of the GUI. Please report this as a bug.", NamedTextColor.RED));
					close();
				}
			}
		} else if (mMainGuiMode == MainGuiMode.TELEPORT_MODE) {
			setLayout(mOwnerAccessToOtherPlotsList, "", null, false, true);
		}
	}

	private void setLayoutForAccessInfo() {
		createInfoHead();

		setItem(30, new GuiItem(GUIUtils.createBasicItem(Material.GRASS_BLOCK, "Access to " + (mIsSelf ? "Your " : mOwnerName + "'s ") + "Plot", NamedTextColor.WHITE, false, "Click here to see who has access to " + (mIsSelf ? "your " : mOwnerName + "'s ") + "plot!", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				mAccessInfoGuiMode = AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT;
				setTitle(Component.text("Access to " + (mIsSelf ? "Your " : mOwnerName + "'s ") + "Plot"));
				update();
			});

		setItem(32, new GuiItem(GUIUtils.createBasicItem(Material.ENDER_PEARL, "Access to Other Plots", NamedTextColor.WHITE, false, "Click here to see which plots " + (mIsSelf ? "you have " : mOwnerName + " has ") + "access to!", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				mAccessInfoGuiMode = AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS;
				setTitle(Component.text("Access to Other Plots"));
				update();
			});
	}

	private void setLayout(List<PlotEntry> plotList, String emptyMessage, @Nullable AccessInfoGuiMode removalConfirmationMode, boolean isRemovalMode, boolean isTeleportMode) {
		mTotalPages = (int) Math.ceil((double) plotList.size() / (double) GUI_LOCATIONS.size());
		int pageOffset = (mCurrentPage - 1) * GUI_LOCATIONS.size();

		if (isTeleportMode) {
			setItem(4, new GuiItem(GUIUtils.createBasicItem(Material.ENDER_PEARL, "Plot Selection", NamedTextColor.WHITE, false, "Click the head of the plot you would like to teleport to.", NamedTextColor.LIGHT_PURPLE)));
		} else {
			createInfoHead();
		}
		createControlButtons();

		if (plotList.isEmpty()) {
			setItem(31, new GuiItem(GUIUtils.createBasicItem(Material.BARRIER, emptyMessage, NamedTextColor.RED)));
		} else {
			for (int i = 0; i < GUI_LOCATIONS.size(); i++) {
				if (i + pageOffset < plotList.size()) {
					PlotEntry plotEntry = plotList.get(i + pageOffset);
					GuiItem guiItem = new GuiItem(createHead(plotEntry));

					if (isRemovalMode) {
						if (removalConfirmationMode == null) {
							MMLog.severe("Argument removalConfirmationMode was still null despite isRemovalMode being true");
							mViewer.sendMessage(Component.text("An error occurred while setting the layout of the GUI. Please report this as a bug.", NamedTextColor.RED));
							close();
							return;
						}

						guiItem.onClick(event -> {
							mAccessInfoGuiMode = removalConfirmationMode;
							mSelectedPlotEntry = plotEntry;
							update();
						});
					} else if (isTeleportMode) {
						guiItem.onClick(event -> {
							if (plotEntry.mSelf) {
								ScoreboardUtils.setScoreboardValue(mViewer, Constants.Objectives.CURRENT_PLOT, mOwnerPlotInfo.mOwnedPlotId);
							} else if (plotEntry.mOwnerAccessToOtherPlotsEntry != null) {
								ScoreboardUtils.setScoreboardValue(mViewer, Constants.Objectives.CURRENT_PLOT, plotEntry.mOwnerAccessToOtherPlotsEntry.mPlotId);
							}

							PlotManager.sendPlayerToPlot(mViewer);
							close();
						});
					}

					setItem(GUI_LOCATIONS.get(i), guiItem);
				}
			}
		}
	}

	private void setLayoutForOtherAccessToOwnerPlotRemovalConfirmation(PlotEntry record) {
		// This condition should never be reached because of existing null checks in setup()
		if (record.mOtherAccessToOwnerPlotEntry == null || record.mOtherAccessToOwnerPlotEntry.mName == null) {
			MMLog.severe("mSelectedPlotEntry was still null by the time it was called by setLayoutForOtherAccessToOwnerPlotRemovalConfirmation()");
			mViewer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
			close();
			return;
		}

		String recordName = record.mOtherAccessToOwnerPlotEntry.mName;
		UUID recordUuid = record.mOtherAccessToOwnerPlotEntry.mUUID;

		setItem(4, new GuiItem(createHead(record)));

		setItem(30, new GuiItem(GUIUtils.createBasicItem(Material.RED_CONCRETE, "Cancel Removal", NamedTextColor.WHITE, false, "Click here to return to the access management screen.", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				mAccessInfoGuiMode = AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL;
				mSelectedPlotEntry = null;
				update();
			});

		setItem(32, new GuiItem(GUIUtils.createBasicItem(Material.GREEN_CONCRETE, "Confirm Removal", NamedTextColor.WHITE, false, "Click here to revoke access from " + recordName + ".", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				if (!mIsSelf) {
					AuditListener.log("[Plot Manager] " + mViewer.getName() + " removed " + recordName + "'s access to " + mOwnerName + "'s plot.");
				}

				PlotManager.plotAccessRemove(mViewer, mOwnerUuid, recordUuid);
				refreshData();

				mAccessInfoGuiMode = AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL;
				mSelectedPlotEntry = null;
				update();
			});
	}

	private void setLayoutForOwnerAccessToOtherPlotsRemovalConfirmation(PlotEntry record) {
		// This condition should never be reached because of existing null checks in setup()
		if (record.mOwnerAccessToOtherPlotsEntry == null || record.mOwnerAccessToOtherPlotsEntry.mName == null) {
			MMLog.severe("mSelectedPlotEntry was still null by the time it was called by setLayoutForOwnerAccessToOtherPlotsRemovalConfirmation()");
			mViewer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
			close();
			return;
		}

		String recordName = record.mOwnerAccessToOtherPlotsEntry.mName;
		UUID recordUuid = MonumentaRedisSyncIntegration.cachedNameToUuid(record.mOwnerAccessToOtherPlotsEntry.mName);

		// This condition should never be reached because of existing null checks in setup()
		if (recordUuid == null) {
			MMLog.severe("recordUuid was somehow null despite mSelectedPlotEntry NOT being null when it was called by setLayoutForOwnerAccessToOtherPlotsRemovalConfirmation()");
			mViewer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
			close();
			return;
		}

		setItem(4, new GuiItem(createHead(record)));

		setItem(30, new GuiItem(GUIUtils.createBasicItem(Material.RED_CONCRETE, "Cancel Removal", NamedTextColor.WHITE, false, "Click here to return to the access management screen.", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				mAccessInfoGuiMode = AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL;
				mSelectedPlotEntry = null;
				update();
			});

		setItem(32, new GuiItem(GUIUtils.createBasicItem(Material.GREEN_CONCRETE, "Confirm Removal", NamedTextColor.WHITE, false, "Click here to remove " + (mIsSelf ? "your " : mOwnerName + "'s ") + "access to " + recordName + "'s plot.", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				AuditListener.log("[Plot Manager] " + mViewer.getName() + " removed " + mOwnerName + "'s access to " + recordName + "'s plot.");
				PlotManager.plotAccessRemove(mViewer, recordUuid, mOwnerUuid);
				refreshData();

				mAccessInfoGuiMode = AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL;
				mSelectedPlotEntry = null;
				update();
			});
	}

	private void createInfoHead() {
		ItemStack ownerSkull = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) ownerSkull.getItemMeta();
		List<Component> lore = new ArrayList<>();

		meta.setPlayerProfile(Bukkit.createProfile(mOwnerUuid, mOwnerName));
		meta.displayName(Component.text(mOwnerName + "'s Plot", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text((mIsSelf ? "Your " : "This player's ") + "plot number is: ", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false).append(Component.text("#" + mOwnerPlotInfo.mOwnedPlotId, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
		lore.add(Component.text((mIsSelf ? "Your " : "This player's ") + "selected plot is: ", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false).append(Component.text("#" + mOwnerPlotInfo.mCurrentPlotId, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
		meta.lore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		ownerSkull.setItemMeta(meta);

		setItem(4, new GuiItem(ownerSkull));
	}

	private void createControlButtons() {
		if (mCurrentPage > 1) {
			setItem(0, new GuiItem(GUIUtils.createBasicItem(Material.ARROW, "Back", NamedTextColor.WHITE, false, "Click to go to page " + (mCurrentPage - 1) + ".", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mCurrentPage -= 1;
					update();
				});
		}

		if (mCurrentPage < mTotalPages) {
			setItem(8, new GuiItem(GUIUtils.createBasicItem(Material.ARROW, "Next", NamedTextColor.WHITE, false, "Click to go to page " + (mCurrentPage + 1) + ".", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mCurrentPage += 1;
					update();
				});
		}

		if (mAccessInfoGuiMode != AccessInfoGuiMode.INACTIVE) {
			setItem(45, new GuiItem(GUIUtils.createBasicItem(Material.RED_CONCRETE, "Main Menu", NamedTextColor.WHITE, false, "Click to return to the main menu.", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mCurrentPage = 1;
					mAccessInfoGuiMode = AccessInfoGuiMode.INACTIVE;
					setTitle(Component.text("Plot Access Information"));
					update();
				});
		}

		if (mAccessInfoGuiMode == AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT) {
			setItem(6, new GuiItem(GUIUtils.createBasicItem(Material.FLINT_AND_STEEL, "Enter Revoke Access Mode", NamedTextColor.WHITE, false, "Click here to enter revoke access mode where you can remove access from players on this screen.", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mAccessInfoGuiMode = AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL;
					update();
				});
		}

		// TODO: redo permission handling
		if (mAccessInfoGuiMode == AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS && mViewer.hasPermission("monumenta.command.plot.remove.others")) {
			setItem(6, new GuiItem(GUIUtils.createBasicItem(Material.FLINT_AND_STEEL, "Enter Revoke Access Mode", NamedTextColor.WHITE, false, "Click here to enter revoke access mode where you can remove " + (mIsSelf ? "your " : mOwnerName + "'s ") + "access to other players' plots on this screen.", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mAccessInfoGuiMode = AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL;
					update();
				});
		}

		if (mAccessInfoGuiMode == AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL) {
			setItem(6, new GuiItem(GUIUtils.createBasicItem(Material.CAMPFIRE, "Exit Revoke Access Mode", NamedTextColor.WHITE, false, "Click here to exit revoke access mode.", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mAccessInfoGuiMode = AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT;
					update();
				});
		}

		if (mAccessInfoGuiMode == AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL) {
			setItem(6, new GuiItem(GUIUtils.createBasicItem(Material.CAMPFIRE, "Exit Revoke Access Mode", NamedTextColor.WHITE, false, "Click here to exit revoke access mode.", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mAccessInfoGuiMode = AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS;
					update();
				});
		}
	}

	private ItemStack createHead(PlotEntry record) {
		if (record.mSelf) {
			ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
			SkullMeta meta = (SkullMeta) head.getItemMeta();

			meta.setOwningPlayer(mViewer);
			meta.displayName(Component.text("Your Plot", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			head.setItemMeta(meta);

			return head;
		} else if (record.mOtherAccessToOwnerPlotEntry != null && record.mOtherAccessToOwnerPlotEntry.mHead != null) {
			return record.mOtherAccessToOwnerPlotEntry.mHead;
		} else if (record.mOwnerAccessToOtherPlotsEntry != null && record.mOwnerAccessToOtherPlotsEntry.mHead != null) {
			return record.mOwnerAccessToOtherPlotsEntry.mHead;
		}

		return new ItemStack(Material.PLAYER_HEAD);
	}
}
