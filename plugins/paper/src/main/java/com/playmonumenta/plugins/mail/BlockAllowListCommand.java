package com.playmonumenta.plugins.mail;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.mail.recipient.RecipientCmdArgs;
import com.playmonumenta.plugins.mail.recipient.RecipientCmdArgs.ArgTarget;
import com.playmonumenta.plugins.mail.recipient.RecipientType;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentSkipListSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BlockAllowListCommand {
	public static void attach(CommandAPICommand root, CommandAPICommand mod) {
		for (BlockAllowListType listType : BlockAllowListType.values()) {
			// Player commands
			for (RecipientCmdArgs senderArgs : Recipient.argumentVariants(
				ArgTarget.CALLEE, "", "")
			) {
				CommandAPICommand actionCmdRoot = new CommandAPICommand(listType.argument());

				attachAddSubcommand(actionCmdRoot, listType, senderArgs);
				attachRemoveSubcommand(actionCmdRoot, listType, senderArgs);
				attachListSubcommand(actionCmdRoot, listType, senderArgs);

				root.withSubcommand(actionCmdRoot);
			}

			// Moderator commands
			for (RecipientCmdArgs senderArgs : Recipient.argumentVariants(
				ArgTarget.ARG, "", "")
			) {
				CommandAPICommand actionCmdRoot = new CommandAPICommand(listType.argument());

				attachListSubcommand(actionCmdRoot, listType, senderArgs);

				mod.withSubcommand(actionCmdRoot);
			}
		}
	}

	private static void attachListSubcommand(
		CommandAPICommand actionCmdRoot,
		BlockAllowListType listType,
		RecipientCmdArgs listOwnerArgs
	) {
		CommandAPICommand listAction;

		listAction = new CommandAPICommand("as")
			.withArguments(listOwnerArgs.recipientArgs())
			.withArguments(new LiteralArgument("list"))
			.executesPlayer((viewer, args) -> {
				runListSubcommand(viewer, args, listType, listOwnerArgs, null);
			});
		actionCmdRoot
			.withSubcommand(listAction);

		for (RecipientType recipientType : RecipientType.validOptions()) {
			listAction = new CommandAPICommand("as")
				.withArguments(listOwnerArgs.recipientArgs())
				.withArguments(new LiteralArgument("list"))
				.withArguments(new LiteralArgument(recipientType.id()))
				.executesPlayer((viewer, args) -> {
					runListSubcommand(viewer, args, listType, listOwnerArgs, recipientType);
				});
			actionCmdRoot
				.withSubcommand(listAction);
		}
	}

	public static void runListSubcommand(
		CommandSender viewer,
		CommandArguments args,
		BlockAllowListType listType,
		RecipientCmdArgs listOwnerArgs,
		@Nullable RecipientType recipientTypeFilter
	) {
		if (
			listOwnerArgs.mTarget.equals(ArgTarget.ARG)
			&& !viewer.hasPermission(MailGui.MAIL_MOD_PERM.toString())
		) {
			viewer.sendMessage(Component.text("You do not have permission for this command.", NamedTextColor.RED));
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			MailCache mailCache;
			try {
				mailCache = listOwnerArgs.getRecipientCache(viewer, args).join();
			} catch (CompletionException wrappedEx) {
				Throwable cause = wrappedEx.getCause();
				if (cause != null) {
					viewer.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
				}
				return;
			}

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				ConcurrentSkipListSet<Recipient> blockAllowSet
					= mailCache.recipientBlockAllowList(listType);
				if (recipientTypeFilter != null) {
					blockAllowSet.removeIf(k -> !k.recipientType().equals(recipientTypeFilter));
				}

				viewer.sendMessage(
					Component.text(listType.argument() + " for ", NamedTextColor.GREEN, TextDecoration.BOLD)
						.append(mailCache.recipient().friendlyComponent(MailDirection.DEFAULT))
						.append(Component.text(":"))
				);
				if (blockAllowSet.isEmpty()) {
					viewer.sendMessage(Component.text("Empty", NamedTextColor.AQUA));
					return;
				}
				for (Recipient blockedOrAllowed : blockAllowSet) {
					viewer.sendMessage(Component.text("- ", NamedTextColor.AQUA)
						.append(blockedOrAllowed.friendlyComponent(MailDirection.DEFAULT)));
				}
			});
		});
	}

	private static void attachAddSubcommand(
		CommandAPICommand actionCmdRoot,
		BlockAllowListType listType,
		RecipientCmdArgs listOwnerArgs
	) {
		CommandAPICommand addAction;

		for (RecipientCmdArgs targetArgs : Recipient.argumentVariants(
			ArgTarget.ARG, "", " to added to " + listType)
		) {
			addAction = new CommandAPICommand("as")
				.withArguments(listOwnerArgs.recipientArgs())
				.withArguments(new LiteralArgument("add"))
				.withArguments(targetArgs.recipientArgs())
				.executesPlayer((viewer, args) -> {
					runAddSubcommand(viewer, args, listType, listOwnerArgs, targetArgs);
				});
			actionCmdRoot
				.withSubcommand(addAction);
		}
	}

	public static void runAddSubcommand(
		Player viewer,
		CommandArguments args,
		BlockAllowListType listType,
		RecipientCmdArgs listOwnerArgs,
		RecipientCmdArgs targetArgs
	) {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			MailCache mailCache;
			try {
				mailCache = listOwnerArgs.getRecipientCache(viewer, args).join();
			} catch (CompletionException wrappedEx) {
				Throwable cause = wrappedEx.getCause();
				if (cause != null) {
					viewer.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
				}
				return;
			}

			Recipient recipient = mailCache.recipient();
			if (recipient.nonMemberCheck(viewer)) {
				viewer.sendMessage(Component.text("You may not modify this list.", NamedTextColor.RED));
				return;
			}

			try {
				recipient.lockedCheck(viewer);
			} catch (NoMailAccessException ex) {
				viewer.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
				if (!viewer.hasPermission(MailGui.MAIL_MOD_PERM.toString())) {
					return;
				}
			}

			Recipient target;
			try {
				target = targetArgs.getRecipient(viewer, args).join();
				mailCache.blockAllowListAdd(listType, target, true).join();
			} catch (CompletionException wrappedEx) {
				Throwable cause = wrappedEx.getCause();
				if (cause != null) {
					viewer.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
				}
				return;
			}

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> viewer.sendMessage(
				Component.text("Added ", NamedTextColor.GREEN)
					.append(target.friendlyComponent(MailDirection.DEFAULT))
					.append(Component.text(" to " + listType.argument()))
			));
		});
	}

	private static void attachRemoveSubcommand(
		CommandAPICommand actionCmdRoot,
		BlockAllowListType listType,
		RecipientCmdArgs listOwnerArgs
	) {
		CommandAPICommand removeAction;

		for (RecipientCmdArgs targetArgs : Recipient.argumentVariants(
			ArgTarget.ARG, "", " to removed from " + listType)
		) {
			removeAction = new CommandAPICommand("as")
				.withArguments(listOwnerArgs.recipientArgs())
				.withArguments(new LiteralArgument("remove"))
				.withArguments(targetArgs.recipientArgs())
				.executesPlayer((viewer, args) -> {
					runRemoveSubcommand(viewer, args, listType, listOwnerArgs, targetArgs);
				});
			actionCmdRoot
				.withSubcommand(removeAction);
		}
	}

	public static void runRemoveSubcommand(
		Player viewer,
		CommandArguments args,
		BlockAllowListType listType,
		RecipientCmdArgs listOwnerArgs,
		RecipientCmdArgs targetArgs
	) {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			MailCache mailCache;
			try {
				mailCache = listOwnerArgs.getRecipientCache(viewer, args).join();
			} catch (CompletionException wrappedEx) {
				Throwable cause = wrappedEx.getCause();
				if (cause != null) {
					viewer.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
				}
				return;
			}

			Recipient recipient = mailCache.recipient();
			if (recipient.nonMemberCheck(viewer)) {
				viewer.sendMessage(Component.text("You may not modify this list.", NamedTextColor.RED));
				return;
			}

			try {
				recipient.lockedCheck(viewer);
			} catch (NoMailAccessException ex) {
				viewer.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
				if (!viewer.hasPermission(MailGui.MAIL_MOD_PERM.toString())) {
					return;
				}
			}

			Recipient target;
			try {
				target = targetArgs.getRecipient(viewer, args).join();
				mailCache.blockAllowListRemove(listType, target, true).join();
			} catch (CompletionException wrappedEx) {
				Throwable cause = wrappedEx.getCause();
				if (cause != null) {
					viewer.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
				}
				return;
			}

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> viewer.sendMessage(
				Component.text("Remove ", NamedTextColor.GREEN)
					.append(target.friendlyComponent(MailDirection.DEFAULT))
					.append(Component.text(" from " + listType.argument()))
			));
		});
	}
}
