package com.playmonumenta.plugins.social;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.social.PlayerSocialDisplayInfo.Friend;
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

public class FriendListGui extends Gui {
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
	private final List<Friend> mFriendList = new ArrayList<>();

	private GuiMode mGuiMode = GuiMode.LIST_MODE;
	private int mTotalPages;
	private int mCurrentPage = 1;
	private @Nullable Friend mSelectedFriend = null;

	private enum GuiMode {
		LIST_MODE,
		REMOVAL_MODE,
		REMOVAL_CONFIRMATION_MODE
	}

	public FriendListGui(Player viewer, String playerName, UUID playerUuid, PlayerSocialDisplayInfo playerSocialDisplayInfo) {
		super(viewer, 6 * 9, "Friend List");

		mViewer = viewer;
		mIsSelf = viewer.getUniqueId().equals(playerUuid);
		mPlayerName = playerName;
		mPlayerUuid = playerUuid;
		mPlayerSocialDisplayInfo = playerSocialDisplayInfo;

		populateFriendList();
	}

	private void populateFriendList() {
		mFriendList.clear();

		mPlayerSocialDisplayInfo.mFriendMap.values().stream()
			.sorted(Comparator.comparing((Friend friend) -> friend.mFriendName == null ? "" : friend.mFriendName, String.CASE_INSENSITIVE_ORDER))
			.forEach(mFriendList::add);
	}

	private CompletableFuture<PlayerSocialDisplayInfo> refreshData() {
		return SocialManager.getSocialDisplayInfo(mPlayerUuid).whenCompleteAsync((playerSocialDisplayInfo, throwable) -> {
			if (throwable != null) {
				MMLog.severe("Caught exception trying to list friends for player " + mPlayerName + " : " + throwable.getMessage());
				mViewer.sendMessage(Component.text("An error occurred while trying to list friends. Please report this: " + throwable.getMessage(), NamedTextColor.RED));
				MessagingUtils.sendStackTrace(mViewer, throwable);
				close();
				return;
			}

			mPlayerSocialDisplayInfo.mFriendMap.keySet().retainAll(playerSocialDisplayInfo.mFriendMap.keySet());
			mPlayerSocialDisplayInfo.mFriendMap.putAll(playerSocialDisplayInfo.mFriendMap);
			populateFriendList();

			update();
		}, runnable -> Bukkit.getScheduler().runTask(Plugin.getInstance(), runnable));
	}

	@Override
	protected void setup() {
		switch (mGuiMode) {
			case LIST_MODE -> setLayout(GuiMode.LIST_MODE);
			case REMOVAL_MODE -> setLayout(GuiMode.REMOVAL_MODE);
			case REMOVAL_CONFIRMATION_MODE -> {
				if (mSelectedFriend == null) {
					MMLog.severe("mSelectedFriend was still null by the time it was called by REMOVAL_CONFIRMATION_MODE in FriendListGui.java");
					mViewer.sendMessage(Component.text("An error occurred while preparing a friend for removal because their associated information couldn't be found. Please report this as a bug.", NamedTextColor.RED));
					close();
					return;
				}

				setLayoutForRemovalConfirmation(mSelectedFriend);
			}
			default -> {
				MMLog.severe("Couldn't find a matching switch case to open '/friend list' for player in FriendListGui.java");
				mViewer.sendMessage(Component.text("An error occurred while setting the layout of the GUI. Please report this as a bug.", NamedTextColor.RED));
				close();
			}
		}
	}

	private void setLayout(GuiMode guiMode) {
		mTotalPages = (int) Math.ceil((double) mFriendList.size() / (double) GUI_LOCATIONS.size());
		int pageOffset = (mCurrentPage - 1) * GUI_LOCATIONS.size();

		createInfoHead();
		createControlButtons();

		if (mFriendList.isEmpty()) {
			setItem(31, new GuiItem(GUIUtils.createBasicItem(Material.BARRIER, (mIsSelf ? "You have " : mPlayerName + " has ") + "no friends added!", NamedTextColor.RED)));
			return;
		}

		for (int i = 0; i < GUI_LOCATIONS.size(); i++) {
			if (i + pageOffset < mFriendList.size()) {
				Friend friend = mFriendList.get(i + pageOffset);
				GuiItem friendSkull = new GuiItem(createHead(friend));

				if (guiMode == GuiMode.REMOVAL_MODE) {
					friendSkull.onClick(event -> {
						mGuiMode = GuiMode.REMOVAL_CONFIRMATION_MODE;
						mSelectedFriend = friend;
						update();
					});
				}

				setItem(GUI_LOCATIONS.get(i), friendSkull);
			}
		}
	}

	private void setLayoutForRemovalConfirmation(@NotNull Friend selectedFriend) {
		String selectedFriendName = selectedFriend.mFriendName;
		UUID selectedFriendUuid = selectedFriend.mFriendUuid;

		setItem(4, new GuiItem(createHead(selectedFriend)));

		setItem(30, new GuiItem(GUIUtils.createBasicItem(Material.RED_CONCRETE, "Cancel Removal", NamedTextColor.WHITE, false, "Click here to return to the friend management screen.", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event -> {
				mGuiMode = GuiMode.REMOVAL_MODE;
				mSelectedFriend = null;
				update();
			});

		setItem(32, new GuiItem(GUIUtils.createBasicItem(Material.GREEN_CONCRETE, "Confirm Removal", NamedTextColor.WHITE, false, "Click here to remove " + selectedFriendName + " as " + (mIsSelf ? "your " : mPlayerName + "'s ") + "friend.", NamedTextColor.LIGHT_PURPLE)))
			.onClick(event ->
				SocialManager.removeFriend((mIsSelf ? null : mViewer), mPlayerUuid, selectedFriendUuid)
					.thenCompose(v -> refreshData())
					.whenCompleteAsync((result, throwable) -> {
						if (throwable != null) {
							MMLog.severe("Caught exception trying to remove " + selectedFriendName + " as " + mPlayerName + "'s friend : " + throwable.getMessage());
							mViewer.sendMessage(Component.text("An error occurred while trying to remove this player as a friend. Please report this: " + throwable.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(mViewer, throwable);
							close();
							return;
						}

						mGuiMode = GuiMode.REMOVAL_MODE;
						mSelectedFriend = null;
						update();
					}, runnable -> Bukkit.getScheduler().runTask(Plugin.getInstance(), runnable))
			);
	}

	private void createInfoHead() {
		ItemStack infoHead = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta skullMeta = (SkullMeta) infoHead.getItemMeta();
		List<Component> loreBuilder = new ArrayList<>();

		skullMeta.setPlayerProfile(Bukkit.createProfile(mPlayerUuid));
		skullMeta.displayName(Component.text((mIsSelf ? "Your " : mPlayerName + "'s ") + "Friends", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		loreBuilder.add(Component.text((mIsSelf ? "You have " : mPlayerName + " has ") + mFriendList.size() + " friend" + (mFriendList.size() == 1 ? "" : "s") + " added.", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
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

		if (mGuiMode == GuiMode.LIST_MODE) {
			setItem(6, new GuiItem(GUIUtils.createBasicItem(Material.FLINT_AND_STEEL, "Enter Removal Mode", NamedTextColor.WHITE, false, "Click here to enter removal mode where you can remove friends on this screen.", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mGuiMode = GuiMode.REMOVAL_MODE;
					update();
				});
		}

		if (mGuiMode == GuiMode.REMOVAL_MODE) {
			setItem(6, new GuiItem(GUIUtils.createBasicItem(Material.CAMPFIRE, "Exit Removal Mode", NamedTextColor.WHITE, false, "Click here to exit removal mode.", NamedTextColor.LIGHT_PURPLE)))
				.onClick(event -> {
					mGuiMode = GuiMode.LIST_MODE;
					update();
				});
		}
	}

	private ItemStack createHead(Friend friend) {
		ItemStack head = ItemUtils.createPlayerHead(friend.mFriendUuid, friend.mFriendName);
		SkullMeta meta = (SkullMeta) head.getItemMeta();

		meta.displayName(Component.text(friend.mFriendName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		meta.lore(List.of(
			Component.text("Friends since:", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false),
			Component.text(SocialManager.formatTimestamp(friend.mFriendTimestamp), NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
		));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		head.setItemMeta(meta);

		return head;
	}
}
