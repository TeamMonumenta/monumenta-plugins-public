package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.GuildPermission;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.PlayerGuildInfo;
import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildOrder;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.inventories.BaseWallet;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.PlayerRecipient;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

public class WalletOwnerGui extends Gui {
	private static final int HEADER_Y = 0;
	private static final int PAGE_START_X = 0;
	private static final int PAGE_START_Y = 1;
	private static final int PAGE_HEIGHT = 5;
	private static final int PAGE_WIDTH = 9;

	private final WalletGui mParentGui;
	private final boolean mOpenedAsModerator;
	private final BaseWallet mWallet;
	private int mPage;
	private List<Recipient> mPossibleOwners = new ArrayList<>();
	private GuildOrder mOrder = GuildOrder.DEFAULT;

	public WalletOwnerGui(
		WalletGui parentGui,
		Player player,
		BaseWallet wallet,
		Component displayName,
		boolean openedAsModerator
	) {
		super(player, 6 * 9, displayName);
		mParentGui = parentGui;
		mWallet = wallet;
		mOpenedAsModerator = openedAsModerator;
		refresh();
	}

	@Override
	protected void setup() {
		if (!mOpenedAsModerator && mWallet.canNotAccess(mPlayer)) {
			mPlayer.sendMessage(Component.text("You no longer have access to this wallet", NamedTextColor.RED));
			mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.closeInventory(), 1L);
			return;
		}

		setHeader();

		int totalRows = Math.floorDiv((mPossibleOwners.size() + PAGE_WIDTH - 1), PAGE_WIDTH);
		setPageArrows(totalRows);

		int index = 0;
		for (int y = 0; y < PAGE_HEIGHT; y++) {
			if (index >= mPossibleOwners.size()) {
				break;
			}

			for (int x = 0; x < PAGE_WIDTH; x++) {
				index = (mPage * PAGE_HEIGHT + y) * PAGE_WIDTH + x;
				if (index >= mPossibleOwners.size()) {
					break;
				}

				Recipient recipient = mPossibleOwners.get(index);
				setOwnerIcon(
					PAGE_START_Y + y,
					PAGE_START_X + x,
					recipient
				);
			}
		}
	}

	public void setHeader() {
		ItemStack item;
		ItemMeta meta;
		List<Component> lore;

		List<Component> guildOrderLore = new ArrayList<>();
		guildOrderLore.add(Component.text("Left click here to cancel", NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false));
		guildOrderLore.add(Component.empty());
		guildOrderLore.add(Component.text("Sort order (hotbar keys to select):", NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false));
		List<GuildOrder> orders = GuildOrder.visibleGuildOrders(mPlayer);
		for (int i = 0; i < 9; i++) {
			if (i >= orders.size()) {
				break;
			}

			guildOrderLore.add(
				Component.text("", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind(Constants.Keybind.hotbar(i)))
					.append(Component.text(": " + orders.get(i).mName)));
		}

		item = new ItemStack(Material.PLAYER_HEAD);
		meta = item.getItemMeta();
		meta.displayName(Component.text("Your Guilds", NamedTextColor.GOLD)
			.decoration(TextDecoration.ITALIC, false));
		lore = new ArrayList<>();
		lore.add(Component.text("Guilds you can change the vault ownership of", NamedTextColor.YELLOW)
			.decoration(TextDecoration.ITALIC, false));
		lore.addAll(guildOrderLore);
		meta.lore(lore);
		if (meta instanceof SkullMeta headMeta) {
			headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(mPlayer.getUniqueId()));
		}
		item.setItemMeta(meta);
		setItem(HEADER_Y, 4, item)
			.onClick((InventoryClickEvent event) -> {
				GuildOrder order;
				switch (event.getClick()) {
					case LEFT -> mParentGui.open();
					case NUMBER_KEY -> {
						int index = event.getHotbarButton();
						if (index < 0 || index >= orders.size()) {
							order = GuildOrder.DEFAULT;
						} else {
							order = orders.get(index);
						}
						changeOrder(order);
					}
					default -> refresh();
				}
			});
	}

	public void changeOrder(GuildOrder order) {
		mOrder = order;
		refresh();
	}

	protected void setPageArrows(int totalRows) {
		int maxPage = Math.floorDiv(Math.max(0, totalRows - 1), PAGE_HEIGHT);
		mPage = Math.max(0, Math.min(mPage, maxPage));

		ItemStack item;
		ItemMeta meta;

		// Prev/Next page buttons
		if (mPage > 0) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Previous Page", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			setItem(HEADER_Y, 0, item).onClick((InventoryClickEvent event) -> clickPrev());
		}

		if (mPage < maxPage) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Next Page", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			setItem(HEADER_Y, 8, item).onClick((InventoryClickEvent event) -> clickNext());
		}
	}

	private void clickPrev() {
		mPage--;
		update();
	}

	private void clickNext() {
		mPage++;
		update();
	}

	public void setOwnerIcon(int row, int column, @Nullable Recipient recipient) {
		if (recipient == null) {
			ItemStack item = GUIUtils.createBasicItem(Material.BARRIER, Component.text("No Owner", NamedTextColor.GREEN)
				.decoration(TextDecoration.ITALIC, false));
			setItem(row, column, item)
				.onClick(event -> onNewOwnerClick(null));
			return;
		}

		ItemStack item = recipient.icon(MailDirection.DEFAULT);
		setItem(row, column, item)
			.onClick(event -> onNewOwnerClick(recipient));
	}

	public void onNewOwnerClick(@Nullable Recipient recipient) {
		if (!mOpenedAsModerator && mWallet.canNotChangeOwner(mPlayer)) {
			mPlayer.sendMessage(Component.text("You no longer have access to this wallet", NamedTextColor.RED));
			mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.closeInventory();
			return;
		}

		mWallet.setOwner(recipient);
		mParentGui.open();
		AuditListener.logPlayer("[Shared Vault] " + mPlayer.getName() + " changed the owner of the wallet for " + mWallet);
	}

	public void refresh() {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			User playerUser = LuckPermsIntegration.getUser(mPlayer);
			List<PlayerGuildInfo> guilds = mOrder.sortGuilds(
				PlayerGuildInfo.ofCollection(
						playerUser,
						LuckPermsIntegration.getRelevantGuilds(playerUser, true, false)
					).join()
					.stream()
					.filter(
						playerGuildInfo
							-> playerGuildInfo.getGuildPermissions().contains(GuildPermission.EDIT_VAULT_OWNERSHIP)
					)
					.collect(Collectors.toList())
			).join();

			Group mainGuild = LuckPermsIntegration.getGuild(playerUser);
			if (mainGuild != null) {
				PlayerGuildInfo mainGuildInfo = PlayerGuildInfo.of(playerUser, mainGuild).join();
				if (mainGuildInfo != null && guilds.contains(mainGuildInfo)) {
					guilds.remove(mainGuildInfo);
					guilds.add(0, mainGuildInfo);
				}
			}

			List<Recipient> result = new ArrayList<>();
			result.add(null);
			result.add(new PlayerRecipient(mPlayer.getUniqueId()));
			for (PlayerGuildInfo guildInfo : guilds) {
				Long guildPlotId = LuckPermsIntegration.getGuildPlotId(LuckPermsIntegration.getGuildRoot(guildInfo.getGuild()));
				if (guildPlotId == null) {
					continue;
				}
				Recipient recipient = GuildArguments.getRecipientFromNumber(guildPlotId);
				if (recipient != null) {
					result.add(recipient);
				}
			}

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				mPossibleOwners = result;
				update();
			});
		});
	}
}
