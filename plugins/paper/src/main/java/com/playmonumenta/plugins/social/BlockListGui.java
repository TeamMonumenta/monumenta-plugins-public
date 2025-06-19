package com.playmonumenta.plugins.social;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.social.PlayerSocialDisplayInfo.BlockedPlayer;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockListGui extends Gui {
	private static final List<Integer> GUI_LOCATIONS = List.of(
		19, 20, 21, 22, 23, 24, 25,
		28, 29, 30, 31, 32, 33, 34,
		37, 38, 39, 40, 41, 42, 43
	);

	private final Player mViewer;
	private final boolean mIsSelf;
	private final String mPlayerName;
	private final UUID mPlayerUuid;
	private final PlayerSocialDisplayInfo mPlayerSocialDisplayInfo;
	private final List<BlockedPlayer> mBlockList = new ArrayList<>();

	private GuiMode mGuiMode = GuiMode.LIST_MODE;
	private int mTotalPages;
	private int mCurrentPage = 1;
	private @Nullable BlockedPlayer mSelectedBlockedPlayer = null;

	private enum GuiMode {
		LIST_MODE,
		REMOVAL_MODE,
		REMOVAL_CONFIRMATION_MODE
	}

	public BlockListGui(Player viewer, String playerName, UUID playerUuid, PlayerSocialDisplayInfo playerSocialDisplayInfo) {
		super(viewer, 6 * 9, "Block List");

		mViewer = viewer;
		mIsSelf = viewer.getUniqueId().equals(playerUuid);
		mPlayerName = playerName;
		mPlayerUuid = playerUuid;
		mPlayerSocialDisplayInfo = playerSocialDisplayInfo;

		populateBlockList();
	}

	private void populateBlockList() {
		mBlockList.clear();

		mPlayerSocialDisplayInfo.mBlockedMap.values().stream()
			.sorted(Comparator.comparing((BlockedPlayer blockedPlayer) -> blockedPlayer.mBlockedName == null ? "" : blockedPlayer.mBlockedName, String.CASE_INSENSITIVE_ORDER))
			.forEach(mBlockList::add);
	}

	private CompletableFuture<PlayerSocialDisplayInfo> refreshData() {
		return SocialManager.getSocialDisplayInfo(mPlayerUuid).whenCompleteAsync((playerSocialDisplayInfo, throwable) -> {
			if (throwable != null) {
				MMLog.severe("Caught exception trying to list blocked players for player " + mPlayerName + " : " + throwable.getMessage());
				mViewer.sendMessage(Component.text("An error occurred while trying to list blocked players. Please report this: " + throwable.getMessage(), NamedTextColor.RED));
				MessagingUtils.sendStackTrace(mViewer, throwable);
				close();
				return;
			}

			mPlayerSocialDisplayInfo.mBlockedMap.keySet().retainAll(playerSocialDisplayInfo.mBlockedMap.keySet());
			mPlayerSocialDisplayInfo.mBlockedMap.putAll(playerSocialDisplayInfo.mBlockedMap);
			populateBlockList();

			update();
		}, runnable -> Bukkit.getScheduler().runTask(Plugin.getInstance(), runnable));
	}

	@Override
	protected void setup() {
		switch (mGuiMode) {
			case LIST_MODE -> setLayout(GuiMode.LIST_MODE);
			case REMOVAL_MODE -> setLayout(GuiMode.REMOVAL_MODE);
			case REMOVAL_CONFIRMATION_MODE -> {
				if (mSelectedBlockedPlayer == null) {
					MMLog.severe("mSelectedBlockedPlayer was still null by the time it was called by REMOVAL_CONFIRMATION_MODE in BlockListGui.java");
					mViewer.sendMessage(Component.text("An error occurred while preparing a blocked player for removal because their associated information couldn't be found. Please report this as a bug.", NamedTextColor.RED));
					close();
					return;
				}

				setLayoutForRemovalConfirmation(mSelectedBlockedPlayer);
			}
			default -> {
				MMLog.severe("Couldn't find a matching switch case to open '/block list' for player in BlockListGui.java");
				mViewer.sendMessage(Component.text("An error occurred while setting the layout of the GUI. Please report this as a bug.", NamedTextColor.RED));
				close();
			}
		}
	}

	private void setLayout(GuiMode guiMode) {
		mTotalPages = (int) Math.ceil((double) mBlockList.size() / (double) GUI_LOCATIONS.size());
		int pageOffset = (mCurrentPage - 1) * GUI_LOCATIONS.size();

		createInfoHead();
		createControlButtons();

		if (mBlockList.isEmpty()) {
			setItem(31, new GuiItem(GUIUtils.createBasicItem(Material.BARRIER, (mIsSelf ? "You have " : mPlayerName + " has ") + "no one blocked!", NamedTextColor.RED)));
			return;
		}

		for (int i = 0; i < GUI_LOCATIONS.size(); i++) {
			if (i + pageOffset < mBlockList.size()) {
				BlockedPlayer blockedPlayer = mBlockList.get(i + pageOffset);
				GuiItem blockedPlayerSkull = new GuiItem(createHead(blockedPlayer));

				if (guiMode == GuiMode.REMOVAL_MODE) {
					blockedPlayerSkull.onClick(event -> {
						mGuiMode = GuiMode.REMOVAL_CONFIRMATION_MODE;
						mSelectedBlockedPlayer = blockedPlayer;
						update();
					});
				}

				setItem(GUI_LOCATIONS.get(i), blockedPlayerSkull);
			}
		}
	}

	private void setLayoutForRemovalConfirmation(@NotNull BlockedPlayer selectedBlockedPlayer) {
		String selectedBlockedPlayerName = selectedBlockedPlayer.mBlockedName;
		UUID selectedBlockedPlayerUuid = selectedBlockedPlayer.mBlockedUuid;

		setItem(4, new GuiItem(createHead(selectedBlockedPlayer)));

		setItem(30, new GuiItem(GUIUtils.createBasicItem(Material.RED_CONCRETE, "Cancel Removal", NamedTextColor.WHITE, false, "Click here to return to the blocked players management screen.", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				mGuiMode = GuiMode.REMOVAL_MODE;
				mSelectedBlockedPlayer = null;
				update();
			});

		setItem(32, new GuiItem(GUIUtils.createBasicItem(Material.GREEN_CONCRETE, "Confirm Removal", NamedTextColor.WHITE, false, "Click here to remove " + selectedBlockedPlayerName + " as one of your blocked players.", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event ->
				SocialManager.unblockPlayer(mPlayerUuid, selectedBlockedPlayerUuid)
					.thenCompose(v -> refreshData())
					.whenCompleteAsync((result, throwable) -> {
						if (throwable != null) {
							MMLog.severe("Caught exception trying to remove " + selectedBlockedPlayerName + " from " + mPlayerName + "'s block list : " + throwable.getMessage());
							mViewer.sendMessage(Component.text("An error occurred while trying to unblock this player. Please report this: " + throwable.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(mViewer, throwable);
							close();
							return;
						}

						mGuiMode = GuiMode.REMOVAL_MODE;
						mSelectedBlockedPlayer = null;
						update();
					}, runnable -> Bukkit.getScheduler().runTask(Plugin.getInstance(), runnable))
			);
	}

	private void createInfoHead() {
		ItemStack infoHead = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta skullMeta = (SkullMeta) infoHead.getItemMeta();
		List<Component> loreBuilder = new ArrayList<>();

		skullMeta.setPlayerProfile(Bukkit.createProfile(mPlayerUuid));
		skullMeta.displayName(Component.text((mIsSelf ? "Your " : mPlayerName + "'s ") + "List of Blocked Players", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		loreBuilder.add(Component.text((mIsSelf ? "You have " : mPlayerName + " has ") + mBlockList.size() + " player" + (mBlockList.size() == 1 ? "" : "s") + " blocked.", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
		skullMeta.lore(loreBuilder);
		skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		infoHead.setItemMeta(skullMeta);

		setItem(4, new GuiItem(infoHead));
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

		if (mGuiMode == GuiMode.LIST_MODE && mIsSelf) {
			setItem(6, new GuiItem(GUIUtils.createBasicItem(Material.FLINT_AND_STEEL, "Enter Removal Mode", NamedTextColor.WHITE, false, "Click here to enter removal mode where you can unblock players on this screen.", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mGuiMode = GuiMode.REMOVAL_MODE;
					update();
				});
		}

		if (mGuiMode == GuiMode.REMOVAL_MODE && mIsSelf) {
			setItem(6, new GuiItem(GUIUtils.createBasicItem(Material.CAMPFIRE, "Exit Removal Mode", NamedTextColor.WHITE, false, "Click here to exit removal mode.", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mGuiMode = GuiMode.LIST_MODE;
					update();
				});
		}
	}

	private ItemStack createHead(BlockedPlayer blockedPlayer) {
		ItemStack head = ItemUtils.createPlayerHead(blockedPlayer.mBlockedUuid, blockedPlayer.mBlockedName);
		SkullMeta meta = (SkullMeta) head.getItemMeta();

		meta.displayName(Component.text(blockedPlayer.mBlockedName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		meta.lore(List.of(
			Component.text("Blocked since:", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false),
			Component.text(SocialManager.formatTimestamp(blockedPlayer.mBlockedTimestamp), NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
		));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		head.setItemMeta(meta);

		return head;
	}
}
