package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.networkchat.commands.ChatCommand;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.CommandUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.UUIDArgument;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;

public class TransferLPPermissions {
	private static final int MAX_REMOVAL_TIME = Constants.FIVE_MINUTES * 50; //5 minutes in milliseconds.

	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.transferpermissions");
	private static final Argument<?> FROM_ARG = new StringArgument("from")
		.replaceSuggestions(ChatCommand.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS);
	private static final Argument<?> TARGET_ARG = new EntitySelectorArgument.OnePlayer("target");

	private static final ConcurrentMap<UUID, TransferPermissionContext> awaitingConfirmations = new ConcurrentHashMap<>();

	@MonotonicNonNull
	private static BukkitTask clearConfirmationsRunnable = null;

	public static void register(Plugin plugin) {
		CommandAPICommand root = new CommandAPICommand("transferpermissions");
		//playerName <playerName|UUID|Player Selector>

		root
			.withArguments(FROM_ARG, TARGET_ARG)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				if (GuildCommand.senderCannotRunCommand(sender, true)) {
					return;
				}

				String fromUser = args.getUnchecked("from");
				Player targetPlayer = args.getUnchecked("target");

				UUID fromUUID = MonumentaRedisSyncIntegration.cachedNameToUuid(fromUser);
				if (fromUUID == null) {
					throw CommandAPI.failWithString("Given target(from) for transfer does not exist in Redis.");
				}

				runTransferPermissions(plugin, sender, fromUUID, targetPlayer);
			}).register();

		new CommandAPICommand("confirmpermissiontransfer")
			.withSubcommands(
				new CommandAPICommand("confirm")
					.withArguments(new UUIDArgument("uuid"))
					.executes((sender, args) -> {
						CommandUtils.checkPerm(sender, PERMISSION);
						if (GuildCommand.senderCannotRunCommand(sender, true)) {
							return;
						}

						UUID confirmationUUID = args.getUnchecked("uuid");

						if (!awaitingConfirmations.containsKey(confirmationUUID)) {
							throw CommandAPI.failWithString("Could not get context for given UUID");
						}

						runConfirmTransfer(plugin, sender, confirmationUUID);
					}),
				new CommandAPICommand("cancel")
					.withArguments(new UUIDArgument("uuid"))
					.executes((sender, args) -> {
						CommandUtils.checkPerm(sender, PERMISSION);
						if (GuildCommand.senderCannotRunCommand(sender, true)) {
							return;
						}

						UUID confirmationUUID = args.getUnchecked("uuid");

						if (!awaitingConfirmations.containsKey(confirmationUUID)) {
							throw CommandAPI.failWithString("Could not get context for given UUID");
						}

						awaitingConfirmations.remove(confirmationUUID);
						sender.sendMessage(Component.text("Cancelled request", NamedTextColor.RED));
					})
			).register();

		if (clearConfirmationsRunnable == null) {
			//set up auto-clean.
			clearConfirmationsRunnable = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
				awaitingConfirmations.entrySet().removeIf(entry -> entry.getValue().mCreatedAt + MAX_REMOVAL_TIME <= System.currentTimeMillis());
			}, 20, Constants.TICKS_PER_MINUTE);
		}
	}

	private static void runTransferPermissions(Plugin plugin, CommandSender sender, UUID fromUUID, Player toPlayer) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			User fromUser = LuckPermsIntegration.loadUser(fromUUID).join();
			if (fromUser == null) {
				Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text("Could not get old user from LuckPerms", NamedTextColor.RED)));
				return;
			}
			User toUser = LuckPermsIntegration.getUser(toPlayer);

			TransferPermissionContext context = new TransferPermissionContext(fromUser, toUser);
			UUID key = UUID.randomUUID();
			awaitingConfirmations.put(key, context);

			Component confirmMessage = getConfirmMessage(fromUser, toUser, key);
			Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(confirmMessage));
		});
	}

	private static void runConfirmTransfer(Plugin plugin, CommandSender sender, UUID contextKey) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			TransferPermissionContext context = awaitingConfirmations.get(contextKey);
			if (context == null) {
				return;
			}

			Component res = transferPermissions(sender, context.mFromUser, context.mToUser);
			awaitingConfirmations.remove(contextKey);
			Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(res));
		});
	}

	private static Component transferPermissions(CommandSender sender, User fromUser, User toUser) {
		NodeMap fromMap = fromUser.data();
		NodeMap toMap = toUser.data();

		toMap.clear();
		for (Node fromNode : fromMap.toCollection()) {
			toMap.add(fromNode);
		}

		LuckPermsIntegration.UM.saveUser(toUser);
		LuckPermsIntegration.pushUserUpdate(toUser);

		AuditListener.log("<+> Transferred all permission data from user: " + fromUser.getUsername() + " to user: " + toUser.getUsername() + "\nTask executed by '" + sender.getName() + "'");
		return Component.text("Successfully transferred all permission data from '" + fromUser.getUsername() + "' to '" + toUser.getUsername() + "'.", NamedTextColor.GOLD);
	}

	@NotNull
	private static Component getConfirmMessage(User fromUser, User toUser, UUID key) {
		Component confirmMessage = Component.text("Are you sure, you want to transfer permission data from '" + fromUser.getUsername() + "' to '" + toUser.getUsername() + "?", NamedTextColor.RED);
		confirmMessage = confirmMessage.append(Component.text("\n[CONFIRM]          ", NamedTextColor.GREEN).decorate(TextDecoration.BOLD).clickEvent(ClickEvent.runCommand("/confirmpermissiontransfer confirm " + key)));
		confirmMessage = confirmMessage.append(Component.text("[CANCEL]", NamedTextColor.RED).decorate(TextDecoration.BOLD).clickEvent(ClickEvent.runCommand("/confirmpermissiontransfer cancel " + key)));
		return confirmMessage;
	}

	private static class TransferPermissionContext {
		public final User mFromUser;
		public final User mToUser;

		private final long mCreatedAt;

		private TransferPermissionContext(User fromUser, User toUser) {
			mFromUser = fromUser;
			mToUser = toUser;

			mCreatedAt = System.currentTimeMillis();
		}
	}
}
