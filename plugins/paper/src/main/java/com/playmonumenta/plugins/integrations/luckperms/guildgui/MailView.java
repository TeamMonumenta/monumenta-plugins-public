package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.mail.MailCache;
import com.playmonumenta.plugins.mail.MailGui;
import com.playmonumenta.plugins.mail.Mailbox;
import com.playmonumenta.plugins.mail.recipient.GuildRecipient;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class MailView extends View {
	protected static final int PAGE_HEIGHT = 5;

	public MailView(GuildGui gui) {
		super(gui);
	}

	@Override
	public void setup() {
		@Nullable View backupView = null;
		MailCache mailCache = mGui.mMailCache;

		if (!mGui.mPlayer.hasPermission(MailGui.MAIL_PERM.toString())) {
			// In case the GUI permission is revoked
			mGui.mPlayer.sendMessage(Component.text("Your access to mail GUIs has been revoked.", NamedTextColor.RED));
			backupView = new AllGuildsView(mGui, GuildOrder.DEFAULT);
		} else if (!(mailCache.recipient() instanceof GuildRecipient guildRecipient)) {
			mGui.mPlayer.sendMessage(Component.text(
				"How did you open the guild mail to something other than guild mail?",
				NamedTextColor.RED));
			backupView = new AllGuildsView(mGui, GuildOrder.DEFAULT);
		} else {
			if (guildRecipient.getGuildId() == GuildRecipient.DUMMY_ID_NOT_LOADED) {
				mGui.mPlayer.sendMessage(Component.text(
					"Guild information not loaded yet, try again. You may not be able to open this directly.",
					NamedTextColor.RED));
				backupView = new AllGuildsView(mGui, GuildOrder.DEFAULT);
			}

			if (guildRecipient.getGuildId() == GuildRecipient.DUMMY_ID_NO_GUILD) {
				mGui.mPlayer.sendMessage(Component.text(
					"You are not in a guild.",
					NamedTextColor.RED));
				backupView = new AllGuildsView(mGui, GuildOrder.DEFAULT);
			}

			if (guildRecipient.getGuildId() <= GuildRecipient.DUMMY_ID_NO_GUILD_NUMBER) {
				mGui.mPlayer.sendMessage(Component.text(
					"Your guild has no plot number assigned to it, and so its mail is inaccessible.",
					NamedTextColor.RED));
				backupView = new GuildMembersView(mGui);
			}

			if (!mailCache.isInitialized()) {
				mGui.mPlayer.sendActionBar(Component.text(
					"Your guild's mail is still loading. Please wait...",
					NamedTextColor.RED));

				Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
					mailCache.awaitInitialization().join();
					Bukkit.getScheduler().runTask(Plugin.getInstance(),
						() -> {
							mGui.mPlayer.sendActionBar(Component.text("Your guild's mail is now done loading.",
								NamedTextColor.GREEN));
							if (mGui.mView.equals(this)) {
								refresh();
							}
						});
				});
			}
		}

		if (backupView != null) {
			mGui.setView(backupView);
			return;
		}

		mGui.setTitle(mailCache
			.recipient()
			.mailboxComponent(mGui.mMailSettings.mDirection)
			.color(NamedTextColor.DARK_GRAY));

		setPageArrows(mailCache.mailboxes(mGui.mMailSettings).size());

		for (int row = 0; row < PAGE_HEIGHT; row++) {
			int index = PAGE_HEIGHT * mPage + row;
			if (index >= mailCache.mailboxes(mGui.mMailSettings).size()) {
				for (int column = 0; column < 9; column++) {
					mGui.setItem(row + 1, column, MailGui.BACKGROUND_FILLER);
				}
				continue;
			}

			Mailbox mailbox = mailCache.mailboxes(mGui.mMailSettings).get(index);
			mGui.setItem(row + 1, 0, mailbox.senderIcon());
			mGui.setItem(row + 1, 1, mailbox.receiverIcon());

			for (int mailSlot = 0; mailSlot < Mailbox.MAX_SLOTS; mailSlot++) {
				int col = 2 + mailSlot;

				mGui.setMailboxSlot(row + 1, col, mailbox, mailSlot);
			}
		}
		if (mailCache.mailboxes(mGui.mMailSettings).isEmpty()) {
			mGui.setItem(3, 4, GUIUtils.createBasicItem(Material.BARRIER, "No new mail", NamedTextColor.GREEN));
		}
	}


	@Override
	public void refresh() {
		super.refresh();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				List<Mailbox> mailboxes = mGui.mMailCache.mailboxes(mGui.mMailSettings);
				for (int column = 0; column < PAGE_HEIGHT; column++) {
					int index = PAGE_HEIGHT * mPage + column;
					if (index >= mailboxes.size()) {
						break;
					}
					Mailbox mailbox = mailboxes.get(index);
					mailbox.refreshContents().join();
				}
				Bukkit.getScheduler().runTask(Plugin.getInstance(), mGui::update);
			} catch (Exception ex) {
				mGui.mPlayer.sendMessage(Component.text("Something went wrong getting your latest mail."));
				MMLog.warning("An error occurred trying to get latest mail "
					+ mGui.mMailCache.recipient().friendlyStr(mGui.mMailSettings.mDirection));
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			}
		});
	}

	@Override
	protected int entriesPerPage() {
		return PAGE_HEIGHT;
	}

	public void refreshMailboxSlot(Mailbox mailbox) {
		MailCache mailCache = mGui.mMailCache;
		if (mailCache == null) {
			return;
		}

		if (!mailCache.mailboxes(mGui.mMailSettings).contains(mailbox)) {
			return;
		}

		mGui.refresh();
	}
}
