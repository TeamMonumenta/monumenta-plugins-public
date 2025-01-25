package com.playmonumenta.plugins.plots;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.plots.PlotManager.PlotInfo;
import com.playmonumenta.plugins.plots.PlotManager.PlotInfo.OtherAccessToOwnerPlotRecord;
import com.playmonumenta.plugins.plots.PlotManager.PlotInfo.OwnerAccessToOtherPlotsRecord;
import com.playmonumenta.plugins.utils.GUIUtils;
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

import static com.playmonumenta.plugins.plots.PlotManager.getPlotInfo;
import static com.playmonumenta.plugins.plots.PlotManager.plotAccessRemove;

public class PlotAccessGui extends Gui {
	private static final Component PLOT_ACCESS_INFO_TITLE = Component.text("Plot Access Information");
	private static final Component OTHER_ACCESS_TO_OWNER_PLOT_TITLE = Component.text("Access to Your Plot");
	private static final Component OWNER_ACCESS_TO_OTHER_PLOTS_TITLE = Component.text("Access to Other Plots");
	private static final ArrayList<Integer> GUI_LOCATIONS = new ArrayList<>(Arrays.asList(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43));

	private final ArrayList<PlotEntry> mOtherAccessToOwnerPlotList = new ArrayList<>();
	private final ArrayList<PlotEntry> mOwnerAccessToOtherPlotsList = new ArrayList<>();
	private final String mOwnerUsername;
	private final UUID mOwnerUuid;
	private final PlotInfo mPlotInfo;
	private final MainGuiMode mMainGuiMode;
	private AccessInfoGuiMode mAccessInfoGuiMode = AccessInfoGuiMode.INACTIVE;
	private @Nullable PlotEntry mSelectedPlotEntry = null;
	private int mNumPages;
	private int mCurrentPage = 1;

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

	private void populateOtherAccessToOwnerPlotList() {
		mPlotInfo.mOtherAccessToOwnerPlot.values().stream()
			.sorted(Comparator.comparing((OtherAccessToOwnerPlotRecord access) -> access.mName == null ? "" : access.mName))
			.forEach(access -> mOtherAccessToOwnerPlotList.add(new PlotEntry(access)));
	}

	private void populateOwnerAccessToOtherPlotsList() {
		mPlotInfo.mOwnerAccessToOtherPlots.values().stream()
			.sorted(Comparator.comparing((OwnerAccessToOtherPlotsRecord access) -> access.mName == null ? "" : access.mName)
				.thenComparingInt(access -> access.mPlotId))
			.forEach(access -> mOwnerAccessToOtherPlotsList.add(new PlotEntry(access)));
	}

	private void refreshData() {
		getPlotInfo(mOwnerUuid).thenCompose(PlotInfo::populateNamesAndHeads).whenComplete((info, ex) -> {
			if (ex != null) {
				Plugin.getInstance().getLogger().severe("Caught exception while refreshing plot access for " + mOwnerUsername + ": " + ex.getMessage());
				mPlayer.sendMessage(Component.text("An error occurred while refreshing the plot access information. Please report this: " + ex.getMessage(), NamedTextColor.RED));
				MessagingUtils.sendStackTrace(mPlayer, ex);
				close();
			} else {
				mPlotInfo.mOtherAccessToOwnerPlot.keySet().retainAll(info.mOtherAccessToOwnerPlot.keySet());
				mPlotInfo.mOtherAccessToOwnerPlot.putAll(info.mOtherAccessToOwnerPlot);

				mPlotInfo.mOwnerAccessToOtherPlots.keySet().retainAll(info.mOwnerAccessToOtherPlots.keySet());
				mPlotInfo.mOwnerAccessToOtherPlots.putAll(info.mOwnerAccessToOtherPlots);

				mOtherAccessToOwnerPlotList.clear();
				populateOtherAccessToOwnerPlotList();

				mOwnerAccessToOtherPlotsList.clear();
				populateOwnerAccessToOtherPlotsList();

				update();
			}
		});
	}

	public PlotAccessGui(Player player, String ownerUsername, UUID ownerUuid, PlotInfo plotInfo, MainGuiMode mainGuiMode, Component title) {
		super(player, 6 * 9, title);
		mOwnerUsername = ownerUsername;
		mOwnerUuid = ownerUuid;
		mPlotInfo = plotInfo;
		mMainGuiMode = mainGuiMode;

		// Only parse the player's own plot for TELEPORT_MODE and not ACCESS_INFO_MODE
		if (mPlotInfo.mOwnedPlotId > 0 && mMainGuiMode == MainGuiMode.TELEPORT_MODE) {
			mOwnerAccessToOtherPlotsList.add(new PlotEntry());
		}

		populateOtherAccessToOwnerPlotList();
		populateOwnerAccessToOtherPlotsList();
	}

	@Override
	protected void setup() {
		if (mMainGuiMode == MainGuiMode.ACCESS_INFO_MODE) {
			switch (mAccessInfoGuiMode) {
				case INACTIVE -> setLayoutForAccessInfo();
				case OTHER_ACCESS_TO_OWNER_PLOT -> setLayoutForOtherAccessToOwnerPlot();
				case OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL -> setLayoutForOtherAccessToOwnerPlotRemoval();
				case OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL_CONFIRMATION -> {
					if (mSelectedPlotEntry != null) {
						setLayoutForOtherAccessToOwnerPlotRemovalConfirmation(mSelectedPlotEntry);
					} else {
						Plugin.getInstance().getLogger().severe("mSelectedPlotEntry was still null by the time it was called by OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL_CONFIRMATION");
						mPlayer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
						close();
					}
				}

				case OWNER_ACCESS_TO_OTHER_PLOTS -> setLayoutForOwnerAccessToOtherPlots();
				case OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL -> setLayoutForOwnerAccessToOtherPlotsRemoval();
				case OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL_CONFIRMATION -> {
					if (mSelectedPlotEntry != null) {
						setLayoutForOwnerAccessToOtherPlotsRemovalConfirmation(mSelectedPlotEntry);
					} else {
						Plugin.getInstance().getLogger().severe("mSelectedPlotEntry was still null by the time it was called by OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL_CONFIRMATION");
						mPlayer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
						close();
					}
				}

				default -> {
					Plugin.getInstance().getLogger().severe("Couldn't find a matching switch case to open '/plot access info' for player");
					mPlayer.sendMessage(Component.text("An error occurred while setting the layout of the GUI. Please report this as a bug.", NamedTextColor.RED));
					close();
				}
			}
		} else if (mMainGuiMode == MainGuiMode.TELEPORT_MODE) {
			setLayoutForTeleport();
		}
	}

	private void setLayoutForAccessInfo() {
		createInfoHead();

		setItem(30, new GuiItem(GUIUtils.createBasicItem(Material.GRASS_BLOCK, "Access to Your Plot", NamedTextColor.WHITE, false, "Click here to see who has access to your plot!", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				mAccessInfoGuiMode = AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT;
				setTitle(OTHER_ACCESS_TO_OWNER_PLOT_TITLE);
				update();
			});

		setItem(32, new GuiItem(GUIUtils.createBasicItem(Material.ENDER_PEARL, "Access to Other Plots", NamedTextColor.WHITE, false, "Click here to see which plots you have access to!", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				mAccessInfoGuiMode = AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS;
				setTitle(OWNER_ACCESS_TO_OTHER_PLOTS_TITLE);
				update();
			});
	}

	private void setLayoutForTeleport() {
		setLayout(mOwnerAccessToOtherPlotsList, "", null, false, true);
	}

	private void setLayoutForOtherAccessToOwnerPlot() {
		setLayout(mOtherAccessToOwnerPlotList, "No one has access to your plot!", null, false, false);
	}

	private void setLayoutForOwnerAccessToOtherPlots() {
		setLayout(mOwnerAccessToOtherPlotsList, "You don't have access to any plots!", null, false, false);
	}

	private void setLayoutForOtherAccessToOwnerPlotRemoval() {
		setLayout(mOtherAccessToOwnerPlotList, "No one has access to your plot!", AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL_CONFIRMATION, true, false);
	}

	private void setLayoutForOwnerAccessToOtherPlotsRemoval() {
		setLayout(mOwnerAccessToOtherPlotsList, "You don't have access to any plots!", AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL_CONFIRMATION, true, false);
	}

	private void setLayoutForOtherAccessToOwnerPlotRemovalConfirmation(PlotEntry record) {
		if (record.mOtherAccessToOwnerPlotEntry != null && record.mOtherAccessToOwnerPlotEntry.mName != null) {
			String recordName = record.mOtherAccessToOwnerPlotEntry.mName;
			@Nullable UUID recordUuid = MonumentaRedisSyncIntegration.cachedNameToUuid(record.mOtherAccessToOwnerPlotEntry.mName);

			if (recordUuid != null) {
				setItem(4, new GuiItem(createHead(record)));

				setItem(30, new GuiItem(GUIUtils.createBasicItem(Material.RED_CONCRETE, "Cancel Removal", NamedTextColor.WHITE, false, "Click here to return to the access management screen.", NamedTextColor.LIGHT_PURPLE)))
					.onClick(event -> {
						mAccessInfoGuiMode = AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL;
						mSelectedPlotEntry = null;
						update();
					});

				setItem(32, new GuiItem(GUIUtils.createBasicItem(Material.GREEN_CONCRETE, "Confirm Removal", NamedTextColor.WHITE, false, "Click here to revoke access from " + recordName + ".", NamedTextColor.LIGHT_PURPLE)))
					.onClick(event -> {
						if (mPlayer != Bukkit.getPlayer(mOwnerUuid)) {
							AuditListener.log("[Plot Manager] " + mPlayer.getName() + " removed " + recordName + " from " + mOwnerUsername + "'s plot");
						}

						plotAccessRemove(mPlayer, mOwnerUuid, recordUuid);
						refreshData();

						mAccessInfoGuiMode = AccessInfoGuiMode.OTHER_ACCESS_TO_OWNER_PLOT_REMOVAL;
						mSelectedPlotEntry = null;
						update();
					});
			} else {
				// This condition should never be reached because of existing null checks in setup()
				Plugin.getInstance().getLogger().severe("recordUuid was somehow null despite mSelectedPlotEntry NOT being null when it was called by setLayoutForOtherAccessToOwnerPlotRemovalConfirmation()");
				mPlayer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
				close();
			}
		} else {
			// This condition should never be reached because of existing null checks in setup()
			Plugin.getInstance().getLogger().severe("mSelectedPlotEntry was still null by the time it was called by setLayoutForOtherAccessToOwnerPlotRemovalConfirmation()");
			mPlayer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
			close();
		}
	}

	private void setLayoutForOwnerAccessToOtherPlotsRemovalConfirmation(PlotEntry record) {
		if (record.mOwnerAccessToOtherPlotsEntry != null && record.mOwnerAccessToOtherPlotsEntry.mName != null) {
			String recordName = record.mOwnerAccessToOtherPlotsEntry.mName;
			@Nullable UUID recordUuid = MonumentaRedisSyncIntegration.cachedNameToUuid(record.mOwnerAccessToOtherPlotsEntry.mName);

			if (recordUuid != null) {
				setItem(4, new GuiItem(createHead(record)));

				setItem(30, new GuiItem(GUIUtils.createBasicItem(Material.RED_CONCRETE, "Cancel Removal", NamedTextColor.WHITE, false, "Click here to return to the access management screen.", NamedTextColor.LIGHT_PURPLE)))
					.onClick(event -> {
						mAccessInfoGuiMode = AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL;
						mSelectedPlotEntry = null;
						update();
					});

				setItem(32, new GuiItem(GUIUtils.createBasicItem(Material.GREEN_CONCRETE, "Confirm Removal", NamedTextColor.WHITE, false, "Click here to remove your access to " + recordName + "'s plot.", NamedTextColor.LIGHT_PURPLE)))
					.onClick(event -> {
						AuditListener.log("[Plot Manager] " + mPlayer.getName() + " removed " + mOwnerUsername + " from " + recordName + "'s plot");
						plotAccessRemove(mPlayer, recordUuid, mOwnerUuid);
						refreshData();

						mAccessInfoGuiMode = AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS_REMOVAL;
						mSelectedPlotEntry = null;
						update();
					});
			} else {
				// This condition should never be reached because of existing null checks in setup()
				Plugin.getInstance().getLogger().severe("recordUuid was somehow null despite mSelectedPlotEntry NOT being null when it was called by setLayoutForOwnerAccessToOtherPlotsRemovalConfirmation()");
				mPlayer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
				close();
			}
		} else {
			// This condition should never be reached because of existing null checks in setup()
			Plugin.getInstance().getLogger().severe("mSelectedPlotEntry was still null by the time it was called by setLayoutForOwnerAccessToOtherPlotsRemovalConfirmation()");
			mPlayer.sendMessage(Component.text("An error occurred while preparing a player for plot access removal because their associated entry couldn't be found. Please report this as a bug.", NamedTextColor.RED));
			close();
		}
	}

	private void setLayout(List<PlotEntry> plotList, String emptyMessage, @Nullable AccessInfoGuiMode removalConfirmationMode, boolean isRemovalMode, boolean isTeleportMode) {
		mNumPages = (int) Math.ceil((double) plotList.size() / (double) GUI_LOCATIONS.size());
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
						if (removalConfirmationMode != null) {
							guiItem.onClick(event -> {
								mAccessInfoGuiMode = removalConfirmationMode;
								mSelectedPlotEntry = plotEntry;
								update();
							});
						} else {
							Plugin.getInstance().getLogger().severe("Argument removalConfirmationMode was still null despite isRemovalMode being true");
							mPlayer.sendMessage(Component.text("An error occurred while setting the layout of the GUI. Please report this as a bug.", NamedTextColor.RED));
							close();
						}
					} else if (isTeleportMode) {
						guiItem.onClick(event -> {
							if (plotEntry.mSelf) {
								ScoreboardUtils.setScoreboardValue(mPlayer, Constants.Objectives.CURRENT_PLOT, mPlotInfo.mOwnedPlotId);
							} else if (plotEntry.mOwnerAccessToOtherPlotsEntry != null) {
								ScoreboardUtils.setScoreboardValue(mPlayer, Constants.Objectives.CURRENT_PLOT, plotEntry.mOwnerAccessToOtherPlotsEntry.mPlotId);
							}

							PlotManager.sendPlayerToPlot(mPlayer);
							close();
						});
					}

					setItem(GUI_LOCATIONS.get(i), guiItem);
				}
			}
		}
	}

	private void createControlButtons() {
		if (mCurrentPage > 1) {
			setItem(0, new GuiItem(GUIUtils.createBasicItem(Material.ARROW, "Back", NamedTextColor.WHITE, false, "Click to go to page " + (mCurrentPage - 1) + ".", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mCurrentPage -= 1;
					update();
				});
		}

		if (mCurrentPage < mNumPages) {
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
					setTitle(PLOT_ACCESS_INFO_TITLE);
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

		if (mAccessInfoGuiMode == AccessInfoGuiMode.OWNER_ACCESS_TO_OTHER_PLOTS && mPlayer.hasPermission("monumenta.command.plot.remove.others")) {
			setItem(6, new GuiItem(GUIUtils.createBasicItem(Material.FLINT_AND_STEEL, "Enter Revoke Access Mode", NamedTextColor.WHITE, false, "Click here to enter revoke access mode where you can remove your access to other players' plots on this screen.", NamedTextColor.LIGHT_PURPLE)))
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

	private void createInfoHead() {
		ItemStack ownerSkull = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) ownerSkull.getItemMeta();
		List<Component> lore = new ArrayList<>();

		meta.setPlayerProfile(Bukkit.createProfile(mOwnerUuid, mOwnerUsername));
		meta.displayName(Component.text(mOwnerUsername + "'s Plot", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("Your plot number is: ", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false).append(Component.text("#" + mPlotInfo.mOwnedPlotId, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
		lore.add(Component.text("Your selected plot is: ", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false).append(Component.text("#" + mPlotInfo.mCurrentPlotId, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
		meta.lore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		ownerSkull.setItemMeta(meta);

		setItem(4, new GuiItem(ownerSkull));
	}

	private ItemStack createHead(PlotEntry record) {
		if (record.mSelf) {
			ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
			SkullMeta meta = (SkullMeta) head.getItemMeta();

			meta.setOwningPlayer(mPlayer);
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
