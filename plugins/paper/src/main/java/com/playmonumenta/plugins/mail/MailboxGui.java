package com.playmonumenta.plugins.mail;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.PlayerRecipient;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.mail.recipient.RecipientCmdArgs;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MailboxGui extends MailGui {
	public static void attach(CommandAPICommand root, CommandAPICommand mod) {
		PlayerRecipient.PlayerRecipientCmdArgs defaultRecipientArgs
			= new PlayerRecipient.PlayerRecipientCmdArgs(RecipientCmdArgs.ArgTarget.CALLEE, "", "");

		root
			.executesPlayer((viewer, args) -> {
				CommandUtils.checkPerm(viewer, MAIL_PERM);

				loadGui(
					viewer,
					defaultRecipientArgs.getRecipientCache(viewer, args),
					false
				);
			});

		root
			.withSubcommand(new CommandAPICommand(MailDirection.DEFAULT.argument())
				.executesPlayer((viewer, args) -> {
					CommandUtils.checkPerm(viewer, MAIL_PERM);

					loadGui(
						viewer,
						defaultRecipientArgs.getRecipientCache(viewer, args),
						false
					);
				})
			);

		for (RecipientCmdArgs recipientArgs : Recipient.argumentVariants(RecipientCmdArgs.ArgTarget.CALLEE, "", "")) {
			root
				.withSubcommand(new CommandAPICommand(MailDirection.DEFAULT.argument())
					.withArguments(recipientArgs.recipientArgs())
					.executesPlayer((viewer, args) -> {
						CommandUtils.checkPerm(viewer, MAIL_PERM);

						loadGui(
							viewer,
							recipientArgs.getRecipientCache(viewer, args),
							false
						);
					})
				);
		}

		for (RecipientCmdArgs recipientArgs : Recipient.argumentVariants(RecipientCmdArgs.ArgTarget.ARG, "", "")) {
			mod
				.withSubcommand(new CommandAPICommand("as")
					.withArguments(recipientArgs.recipientArgs())
					.executesPlayer((viewer, args) -> {
						CommandUtils.checkPerm(viewer, MAIL_MOD_PERM);
						loadGui(
							viewer,
							recipientArgs.getRecipientCache(viewer, args),
							true
						);
					})
				);
		}
	}

	private static final int PAGE_HEIGHT = 5;

	private final MailCache mMailCache;
	private final MailGuiSettings mSettings;
	private int mPage = 0;

	public static void loadGui(
		Player viewer,
		CompletableFuture<MailCache> cacheFuture,
		boolean openedAsModerator
	) {
		viewer.sendMessage(Component.text("Please wait, your mail is loading...", NamedTextColor.YELLOW));
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			MailCache mailCache;
			try {
				mailCache = cacheFuture.join();
			} catch (CompletionException wrappedEx) {
				Throwable cause = wrappedEx.getCause();
				if (cause != null) {
					viewer.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
				}
				return;
			}

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				viewer.sendMessage(Component.text("Your mail is now ready!", NamedTextColor.GREEN));
				new MailboxGui(viewer, mailCache, openedAsModerator).open();
			});
		});
	}

	private MailboxGui(
		Player viewer,
		MailCache mailCache,
		boolean openedAsModerator
	) {
		super(
			viewer,
			54,
			mailCache
				.recipient()
				.mailboxComponent(MailDirection.DEFAULT)
				.color(NamedTextColor.DARK_GRAY),
			openedAsModerator
		);
		mMailCache = mailCache;
		mSettings = new MailGuiSettings();
		mFiller = BACKGROUND_FILLER;
		refresh();
	}

	@Override
	protected void setup() {
		setTitle(mMailCache
			.recipient()
			.mailboxComponent(mSettings.mDirection)
			.color(NamedTextColor.DARK_GRAY)
		);

		if (!mPlayer.hasPermission(MAIL_PERM.toString())) {
			// In case the GUI permission is revoked
			mPlayer.sendMessage(Component.text("Your access to this GUI has been revoked.", NamedTextColor.RED));
			close();
			return;
		}

		for (int x = 0; x <= 8; x++) {
			setItem(0, x, HEADER_FILLER);
		}
		setItem(0, 4, mMailCache.recipient().mailboxIcon(mSettings))
			.onClick(event -> {
				if (guiSettingsClick(event, mSettings)) {
					refresh();
				}
			});
		int numMailboxes = mMailCache.mailboxes(mSettings).size();
		setPageArrows(numMailboxes);

		if (mMailCache.mailboxes(mSettings).isEmpty()) {
			setItem(3, 4, GUIUtils.createBasicItem(Material.BARRIER, "No new mail", NamedTextColor.GREEN));
		}

		for (int row = 0; row < PAGE_HEIGHT; row++) {
			int index = PAGE_HEIGHT * mPage + row;
			if (index >= mMailCache.mailboxes(mSettings).size()) {
				continue;
			}

			Mailbox mailbox = mMailCache.mailboxes(mSettings).get(index);
			setItem(row + 1, 0, mailbox.senderIcon());
			setItem(row + 1, 1, mailbox.receiverIcon());

			for (int mailSlot = 0; mailSlot < Mailbox.MAX_SLOTS; mailSlot++) {
				int column = 2 + mailSlot;

				setMailboxSlot(row + 1, column, mailbox, mailSlot);
			}
		}
	}

	@Override
	public void refresh() {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				List<Mailbox> mailboxes = mMailCache.mailboxes(mSettings);
				for (int row = 0; row < PAGE_HEIGHT; row++) {
					int index = PAGE_HEIGHT * mPage + row;
					if (index >= mailboxes.size()) {
						break;
					}
					Mailbox mailbox = mailboxes.get(index);
					mailbox.refreshContents().join();
				}
				Bukkit.getScheduler().runTask(Plugin.getInstance(), this::update);
			} catch (Exception ex) {
				mPlayer.sendMessage(Component.text("Something went wrong getting your latest mail."));
				MMLog.warning("An error occurred trying to get latest mail "
					+ mMailCache.recipient().friendlyStr(mSettings.mDirection));
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			}
		});
	}

	@Override
	public void refreshMailbox(Mailbox mailbox) {
		if (!mMailCache.mailboxes(mSettings).contains(mailbox)) {
			MMLog.fine(() -> "[Mailbox] refreshMailboxSlot: MailboxGui detected no matching mailbox; ignoring");
			return;
		}

		MMLog.fine(() -> "[Mailbox] refreshMailboxSlot: MailboxGui (" + mPlayer.getName() + ") refreshing");
		refresh();
	}

	@Override
	public MailCache getOwnerCache() {
		return mMailCache;
	}

	@Override
	public Collection<MailCache> getRecipientCaches() {
		return List.of(mMailCache);
	}

	protected void setPageArrows(int totalRows) {
		int maxPage = Math.floorDiv(Math.max(0, totalRows - 1), PAGE_HEIGHT);
		int oldPage = mPage;
		mPage = Math.max(0, Math.min(mPage, maxPage));
		if (oldPage != mPage) {
			refresh();
		}

		ItemStack item;
		ItemMeta meta;

		// Prev/Next page buttons
		if (mPage > 0) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Previous Page", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			setItem(0, 0, item).onClick((InventoryClickEvent event) -> clickPrev());
		}

		if (mPage < maxPage) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Next Page", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			setItem(0, 8, item).onClick((InventoryClickEvent event) -> clickNext());
		}
	}

	private void clickPrev() {
		mPage--;
		mPlayer.playSound(mPlayer, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
		update();
	}

	private void clickNext() {
		mPage++;
		mPlayer.playSound(mPlayer, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
		update();
	}
}
