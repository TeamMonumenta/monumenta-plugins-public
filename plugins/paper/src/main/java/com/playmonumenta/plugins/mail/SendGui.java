package com.playmonumenta.plugins.mail;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.PlayerRecipient.PlayerRecipientCmdArgs;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.mail.recipient.RecipientCmdArgs;
import com.playmonumenta.plugins.mail.recipient.RecipientCmdArgs.ArgTarget;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MMLog;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SendGui extends MailGui {
	public static void attach(CommandAPICommand root) {
		for (RecipientCmdArgs receiverArgs : Recipient.argumentVariants(ArgTarget.ARG, "To ", "")) {
			PlayerRecipientCmdArgs defaultRecipientArgs
				= new PlayerRecipientCmdArgs(ArgTarget.CALLEE, "", "");

			root
				.withSubcommand(new CommandAPICommand("send")
					.withArguments(receiverArgs.recipientArgs())
					.executesPlayer((viewer, args) -> {
						CommandUtils.checkPerm(viewer, MAIL_PERM);

						loadGui(
							viewer,
							defaultRecipientArgs.getRecipientCache(viewer, args),
							receiverArgs.getRecipientCache(viewer, args)
						);
					})
				);

			for (RecipientCmdArgs senderArgs : Recipient.argumentVariants(ArgTarget.CALLEE, "", "")) {
				root
					.withSubcommand(new CommandAPICommand("send")
						.withArguments(receiverArgs.recipientArgs())
						.withArguments(new LiteralArgument("from"))
						.withArguments(senderArgs.recipientArgs())
						.executesPlayer((viewer, args) -> {
							CommandUtils.checkPerm(viewer, MAIL_PERM);

							loadGui(
								viewer,
								senderArgs.getRecipientCache(viewer, args),
								receiverArgs.getRecipientCache(viewer, args)
							);
						})
					);
			}
		}
	}

	private final MailCache mSenderCache;
	private final MailCache mReceiverCache;
	private final Mailbox mMailbox;

	public static void loadGui(
		Player viewer,
		CompletableFuture<MailCache> senderCacheFuture,
		CompletableFuture<MailCache> receiverCacheFuture
	) {
		viewer.sendMessage(Component.text("Please wait, loading mail...", NamedTextColor.YELLOW));
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			MailCache senderCache;
			try {
				senderCache = senderCacheFuture.join();
			} catch (CompletionException wrappedEx) {
				Throwable cause = wrappedEx.getCause();
				if (cause != null) {
					viewer.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
				}
				return;
			}

			MailCache receiverCache;
			try {
				receiverCache = receiverCacheFuture.join();
			} catch (CompletionException wrappedEx) {
				Throwable cause = wrappedEx.getCause();
				if (cause != null) {
					viewer.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
				}
				return;
			}

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				viewer.sendMessage(Component.text("Your mail is now ready!", NamedTextColor.GREEN));
				new SendGui(viewer, senderCache, receiverCache).open();
			});
		});
	}

	private SendGui(
		Player viewer,
		MailCache senderCache,
		MailCache receiverCache
	) {
		super(
			viewer,
			36,
			Component.text("Mail ")
				.color(NamedTextColor.DARK_GRAY)
				.append(receiverCache.recipient().friendlyComponent(MailDirection.TO)),
			false
		);
		Recipient sender = senderCache.recipient();
		Recipient receiver = receiverCache.recipient();
		mSenderCache = senderCache;
		mReceiverCache = receiverCache;
		mMailbox = MailMan.getOrRegister(new Mailbox(sender, receiver));
		mFiller = BACKGROUND_FILLER;

		refresh();
	}

	@Override
	public void open() {
		if (mReceiverCache.failsBlockAllowLists(mSenderCache.recipient())) {
			mPlayer.sendMessage(Component.text("", NamedTextColor.RED)
				.append(mReceiverCache.recipient().friendlyComponent(MailDirection.DEFAULT))
				.append(Component.text(" is not accepting mail from you.")));
			return;
		}

		super.open();
	}

	@Override
	protected void setup() {
		if (!mPlayer.hasPermission(MAIL_PERM.toString())) {
			// In case the GUI permission is revoked
			close();
			return;
		}

		for (int x = 0; x <= 8; x++) {
			setItem(0, x, HEADER_FILLER);
		}
		setItem(0, 3, mMailbox.senderIcon());
		setItem(0, 4, mMailbox.mailboxIcon());
		setItem(0, 5, mMailbox.receiverIcon());

		for (int mailSlot = 0; mailSlot < Mailbox.MAX_SLOTS; mailSlot++) {
			int row = 2;
			int column = 2 + mailSlot;

			setMailboxSlot(row, column, mMailbox, mailSlot);
		}
	}

	@Override
	public void refresh() {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				mMailbox.refreshContents().join();
				Bukkit.getScheduler().runTask(Plugin.getInstance(), this::update);
			} catch (Exception ex) {
				mPlayer.sendMessage(Component.text("Something went wrong getting mailbox contents"));
			}
		});
	}

	@Override
	public void refreshMailbox(Mailbox mailbox) {
		if (mMailbox.equals(mailbox)) {
			MMLog.fine(() -> "[Mailbox] refreshMailboxSlot: SendGui (" + mPlayer.getName()
				+ " " + mailbox.receiver().friendlyStr(MailDirection.TO) + ") refreshing");
			refresh();
		}
	}

	@Override
	public MailCache getOwnerCache() {
		return mSenderCache;
	}

	@Override
	public Collection<MailCache> getRecipientCaches() {
		Set<MailCache> result = new HashSet<>();
		result.add(mSenderCache);
		result.add(mReceiverCache);
		return result;
	}
}
