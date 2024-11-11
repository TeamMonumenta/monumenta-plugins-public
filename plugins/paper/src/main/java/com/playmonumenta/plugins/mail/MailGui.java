package com.playmonumenta.plugins.mail;

import com.playmonumenta.plugins.Constants.Keybind;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.RecipientType;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import dev.jorel.commandapi.CommandPermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MailGui extends Gui implements Comparable<MailGui> {
	public static final CommandPermission MAIL_PERM = CommandPermission.fromString("monumenta.command.mail");
	public static final CommandPermission MAIL_MOD_PERM = CommandPermission.fromString("monumenta.command.mail.mod");
	public static final ItemStack HEADER_FILLER = GUIUtils.createFiller(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
	public static final ItemStack BACKGROUND_FILLER = GUIUtils.createFiller(Material.WHITE_STAINED_GLASS_PANE);
	// Click types not in this list have not been proven to be handled correctly, and are disabled.
	// If proven to work correctly, they can be added to this list.
	public static final EnumSet<ClickType> ACCEPTED_CLICK_TYPES = EnumSet.of(
		ClickType.LEFT,
		ClickType.RIGHT,
		ClickType.WINDOW_BORDER_LEFT,
		ClickType.WINDOW_BORDER_RIGHT,
		ClickType.MIDDLE,
		ClickType.NUMBER_KEY,
		ClickType.DROP,
		ClickType.CONTROL_DROP,
		ClickType.CREATIVE,
		ClickType.SWAP_OFFHAND
	);
	public static final Set<String> DISALLOWED_SHARDS = Set.of(
		"tutorial"
	);

	protected final boolean mOpenedAsModerator;
	// Only used to implement Comparable and related methods; not a meaningful value
	private final UUID mDummyIdentity = UUID.randomUUID();

	public MailGui(Player player, int size, Component title, boolean openedAsModerator) {
		super(player, size, title);
		mOpenedAsModerator = openedAsModerator;
	}

	@Override
	public void open() {
		try {
			noAccessGuiCheck(null);
		} catch (NoMailAccessException ex) {
			mPlayer.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
			return;
		}
		openBypassAccessCheck();
	}

	// Useful for opening GUIs that conditionally show mail
	public void openBypassAccessCheck() {
		super.open();
		MailMan.registerMailGui(this);
	}

	@Override
	protected void onClose(InventoryCloseEvent event) {
		super.onClose(event);
		MailMan.unregisterMailGui(this);
	}

	public static List<Component> guiSettingsPrompt(MailGuiSettings mailGuiSettings) {
		List<Component> result = new ArrayList<>();

		result.add(Component.text("Selected mailbox (hotbar keys to change):", NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false));
		int i = 0;

		MailDirection currentDirection = mailGuiSettings.mDirection;
		for (MailDirection mailDirection : MailDirection.values()) {
			if (i >= Keybind.values().length) {
				break;
			}

			boolean isDirection = mailDirection.equals(currentDirection);
			TextColor color = isDirection ? NamedTextColor.YELLOW : NamedTextColor.GRAY;
			String showOrShowing = isDirection ? ": Showing " : ": Show ";
			result.add(Component.text("", color)
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, isDirection)
				.append(Component.keybind(Keybind.hotbar(i)))
				.append(Component.text(showOrShowing + mailDirection.argument()))
			);

			i++;
		}

		for (RecipientType recipientType : RecipientType.values()) {
			if (i >= Keybind.values().length) {
				break;
			}
			if (RecipientType.UNKNOWN.equals(recipientType)) {
				continue;
			}

			boolean isEnabled = mailGuiSettings.mRecipientTypes.contains(recipientType);
			TextColor color = isEnabled ? NamedTextColor.GREEN : NamedTextColor.RED;
			String includingOrNot = isEnabled ? ": Including " : ": Not including ";
			result.add(Component.text("", color)
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, isEnabled)
				.append(Component.keybind(Keybind.hotbar(i)))
				.append(Component.text(includingOrNot + recipientType.id()))
			);

			i++;
		}

		return result;
	}

	public static boolean guiSettingsClick(InventoryClickEvent event, MailGuiSettings mailGuiSettings) {
		if (!event.getClick().equals(ClickType.NUMBER_KEY)) {
			return false;
		}

		int hotbarButton = event.getHotbarButton();
		if (hotbarButton < 0) {
			return false;
		}
		int i = 0;

		for (MailDirection mailDirection : MailDirection.values()) {
			if (i == hotbarButton) {
				mailGuiSettings.mDirection = mailDirection;
				return true;
			}

			i++;
		}

		for (RecipientType recipientType : RecipientType.values()) {
			if (RecipientType.UNKNOWN.equals(recipientType)) {
				continue;
			}

			if (i == hotbarButton) {
				if (mailGuiSettings.mRecipientTypes.contains(recipientType)) {
					mailGuiSettings.mRecipientTypes.remove(recipientType);
				} else {
					mailGuiSettings.mRecipientTypes.add(recipientType);
				}
				return true;
			}

			i++;
		}

		return false;
	}

	/**
	 * Checks if the GUI's viewer has access to the GUI and an optional mailbox
	 * @param mailbox The mailbox to check access for, if any
	 * @throws NoMailAccessException with the reason the player does not have access to the mailbox,
	 * as well as if the GUI should be closed.
	 */
	public void noAccessGuiCheck(@Nullable Mailbox mailbox) throws NoMailAccessException {
		String shard = ServerProperties.getShardName();
		if (DISALLOWED_SHARDS.contains(shard)) {
			throw new NoMailAccessException("You may not check your mail on " + shard);
		}

		if (!mPlayer.hasPermission(MAIL_PERM.toString())) {
			throw new NoMailAccessException("Your do not have permission to access mail.");
		}

		if (mOpenedAsModerator && !mPlayer.hasPermission(MAIL_MOD_PERM.toString())) {
			throw new NoMailAccessException("Your do not have permission to access mail as a moderator.");
		}

		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_VIRTUAL_INVENTORIES)) {
			throw new NoMailAccessException("You may not access this GUI here; virtual inventories disabled.");
		}

		if (!mOpenedAsModerator) {
			if (getOwnerCache().recipient().nonMemberCheck(mPlayer)) {
				throw new NoMailAccessException("You are not a participant of this mailbox");
			}

			if (mailbox != null) {
				mailbox.noAccessCheck(mPlayer);
			}
		}
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		if (!ACCEPTED_CLICK_TYPES.contains(event.getClick())) {
			mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.sendActionBar(Component.text("That click type is disabled in mailboxes.", NamedTextColor.RED));
			return;
		}

		event.setCancelled(false);
	}

	public void setMailboxSlot(int row, int column, Mailbox mailbox, int mailSlot) {
		ItemStack mailItem = mailbox.getLastKnownSlotContents(mailSlot);
		if (mailItem == null) {
			mailItem = new ItemStack(Material.AIR);
		}
		setItem(row, column, new GuiItem(mailItem, false))
			.onClick((InventoryClickEvent event) -> onMailClick(event, mailbox, mailSlot));
	}

	private void onMailClick(InventoryClickEvent event, Mailbox mailbox, int mailSlot) {
		try {
			noAccessGuiCheck(mailbox);
		} catch (NoMailAccessException ex) {
			mPlayer.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
			if (ex.closeGui()) {
				close();
			}
			return;
		}

		if (!ACCEPTED_CLICK_TYPES.contains(event.getClick())) {
			mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.sendActionBar(Component.text("That click type is disabled in mailboxes.", NamedTextColor.RED));
			return;
		}

		ItemStack existingMailItem = event.getCurrentItem();
		ItemStack newMailItem = event.getCursor();

		if (ItemUtils.isNullOrAir(existingMailItem) && ItemUtils.isNullOrAir(newMailItem)) {
			return;
		}

		mPlayer.playSound(mPlayer, Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1.0f, 1.0f);
		mPlayer.sendActionBar(Component.text("Please wait...", NamedTextColor.GREEN));
		// Take the player's item - we have a local copy
		event.getView().setCursor(null);

		String redisLockKey = mailbox.lockSlotMapRedisKey(mailSlot);
		MailMan.interactionChange(mPlayer, redisLockKey, true);
		CompletableFuture<List<ItemStack>> itemSwapFuture = mailbox.swapItemInSlot(mPlayer, mailSlot, newMailItem);
		// Mark the slot as in use to prevent additional swaps
		update();
		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {
			List<ItemStack> returnedItems;
			try {
				returnedItems = itemSwapFuture.join();
			} catch (CompletionException completionException) {
				Throwable cause = completionException.getCause();
				if (cause instanceof UnacceptedItemException unacceptedItemException) {
					Bukkit.getScheduler().runTask(mPlugin, () -> {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.sendActionBar(Component.text(unacceptedItemException.getMessage(), NamedTextColor.YELLOW));
						InventoryUtils.giveItem(mPlayer, newMailItem);
						refresh();
					});
				} else if (cause instanceof LockException lockException) {
					Bukkit.getScheduler().runTask(mPlugin, () -> {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.sendMessage(Component.text(lockException.getMessage(), NamedTextColor.YELLOW));
						InventoryUtils.giveItem(mPlayer, newMailItem);
						refresh();
					});
				} else if (cause instanceof NullPointerException nullPointerException) {
					Bukkit.getScheduler().runTask(mPlugin, () -> {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.sendMessage(Component.text(nullPointerException.getMessage(), NamedTextColor.YELLOW));
						MessagingUtils.sendStackTrace(mPlayer, nullPointerException);
						InventoryUtils.giveItem(mPlayer, newMailItem);
						refresh();
					});
				} else if (cause != null) {
					Bukkit.getScheduler().runTask(mPlugin, () -> {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.sendMessage(Component.text(cause.getMessage(), NamedTextColor.YELLOW));
						MessagingUtils.sendStackTrace(mPlayer, cause);
						InventoryUtils.giveItem(mPlayer, newMailItem);
						refresh();
					});
				} else {
					Bukkit.getScheduler().runTask(mPlugin, () -> {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.sendMessage(Component.text(completionException.getMessage(), NamedTextColor.YELLOW));
						MessagingUtils.sendStackTrace(mPlayer, completionException);
						InventoryUtils.giveItem(mPlayer, newMailItem);
						refresh();
					});
				}
				MailMan.interactionChange(mPlayer, redisLockKey, false);
				return;
			}

			Bukkit.getScheduler().runTask(mPlugin, () -> {
				if (!mPlayer.isOnline() || MonumentaRedisSyncIntegration.isPlayerTransferring(mPlayer)) {
					Location loc = mPlayer.getLocation();
					for (ItemStack item : returnedItems) {
						loc.getWorld().dropItem(loc, item);
					}
					MailMan.interactionChange(mPlayer, redisLockKey, false);
					return;
				}

				List<Component> returnedItemComponents = new ArrayList<>();
				for (ItemStack item : returnedItems) {
					returnedItemComponents.add(
						Component.text("[", NamedTextColor.GREEN)
							.append(ItemUtils.getDisplayName(item))
							.append(Component.text("]"))
							.hoverEvent(item)
					);
					InventoryUtils.giveItem(mPlayer, item);
				}
				MailMan.interactionChange(mPlayer, redisLockKey, false);

				JoinConfiguration joinConfiguration;
				if (returnedItems.size() == 2) {
					joinConfiguration = JoinConfiguration.separator(Component.text(" and "));
				} else {
					joinConfiguration = JoinConfiguration.separators(
						Component.text(", "),
						Component.text(", and ")
					);
				}

				TextColor confirmationColor = TextColor.color(0x7f, 0xbf, 0xff);
				Component confirmationMessage;
				if (returnedItems.isEmpty()) {
					confirmationMessage = Component.text("Sent ", confirmationColor)
						.append(Component.text("[", NamedTextColor.RED)
							.append(ItemUtils.getDisplayName(newMailItem))
							.append(Component.text("]"))
							.hoverEvent(newMailItem))
						.append(Component.text("!"));
				} else if (ItemUtils.isNullOrAir(newMailItem)) {
					confirmationMessage = Component.text("Received ", confirmationColor)
						.append(Component.join(joinConfiguration, returnedItemComponents))
						.append(Component.text("!"));
				} else {
					Component itemDisplay = ItemUtils.getDisplayName(newMailItem);
					confirmationMessage = Component.text("Swapped ", confirmationColor)
						.append(Component.text("[", NamedTextColor.RED)
							.append(itemDisplay)
							.append(Component.text("]"))
							.hoverEvent(newMailItem))
						.append(Component.text(" with "))
						.append(Component.join(joinConfiguration, returnedItemComponents))
						.append(Component.text("!"));
				}
				mPlayer.sendMessage(confirmationMessage);

				mPlayer.playSound(mPlayer, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);

				refresh();
			});
		});
	}

	public abstract void refresh();

	public abstract void refreshMailbox(Mailbox mailbox);

	public abstract MailCache getOwnerCache();

	public abstract Collection<MailCache> getRecipientCaches();

	@Override
	public int compareTo(@NotNull MailGui o) {
		return mDummyIdentity.compareTo(o.mDummyIdentity);
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof MailGui o)) {
			return false;
		}
		return mDummyIdentity.equals(o.mDummyIdentity);
	}

	@Override
	public int hashCode() {
		return mDummyIdentity.hashCode();
	}
}
