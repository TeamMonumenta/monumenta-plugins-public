package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.utils.MessagingUtils.ESCAPED_TEXT_COLOR_SUGGESTIONS;

public class GuildChatColorCommand {
	private static final Argument<String> COLOR_ARG = new TextArgument("color").replaceSuggestions(ESCAPED_TEXT_COLOR_SUGGESTIONS);
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.guild.chatcolor");
	private static final CommandPermission PERMISSION_MOD = CommandPermission.fromString("monumenta.command.guild.mod.chatcolor");

	public static CommandAPICommand attach(Plugin plugin, CommandAPICommand root) {
		return root
			.withArguments(
				COLOR_ARG
			)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				CommandSender callee = sender;
				if (GuildCommand.senderCannotRunCommand(callee, false)) {
					throw CommandAPI.failWithString("You cannot run this command on 'build'");
				}

				if (callee instanceof ProxiedCommandSender proxied) {
					callee = proxied.getCallee();
				}
				if (!(callee instanceof Player player)) {
					throw CommandAPI.failWithString("This command may only be run by a player");
				}

				TextColor color = MessagingUtils.colorFromString(args.getByArgument(COLOR_ARG));
				if (color == null) {
					throw CommandAPI.failWithString("Color should either be the color's name or in the hex format '#RRGGBB'");
				}

				runChangeGuildChatColorPlayer(plugin, player, color);
			});
	}

	public static CommandAPICommand attachModOnly(Plugin plugin, CommandAPICommand root) {
		return root
			.withArguments(
				GuildCommand.GUILD_NAME_ARG,
				COLOR_ARG
			)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION_MOD);
				if (GuildCommand.senderCannotRunCommand(sender, true)) {
					return;
				}

				String guildName = args.getByArgument(GuildCommand.GUILD_NAME_ARG);
				TextColor color = MessagingUtils.colorFromString(args.getByArgument(COLOR_ARG));
				if (color == null) {
					throw CommandAPI.failWithString("Color should either be the color's name or in the hex format '#RRGGBB'");
				}

				runChangeGuildChatColor(plugin, sender, guildName, color);
			});
	}

	public static void runChangeGuildChatColorPlayer(Plugin plugin, Player sender, TextColor color) {
		Group guild = LuckPermsIntegration.getGuild(sender);
		if (guild == null) {
			sender.sendMessage(Component.text("You are not in a guild", NamedTextColor.RED));
			return;
		}
		if (GuildAccessLevel.MANAGER.ordinal() < GuildAccessLevel.byGroup(guild).ordinal()) {
			sender.sendMessage(Component.text("You must be a manager or founder to manage guild chat color", NamedTextColor.RED));
			return;
		}

		Group root = LuckPermsIntegration.getGuildRoot(guild);
		if (root == null) {
			sender.sendMessage(Component.text("Could not obtain your guild's root group\nThis guild may be malformed.", NamedTextColor.RED));
			return;
		}

		runChangeGuildChatColor(plugin, sender, root, color);
	}

	public static void runChangeGuildChatColor(Plugin plugin, CommandSender sender, Group guild, TextColor color) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			TextComponent res = changeGuildChatColor(plugin, guild, color, sender).join();
			Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(res));
		});
	}

	public static void runChangeGuildChatColor(Plugin plugin, CommandSender sender, String guildName, TextColor color) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			String rootGuildId = GuildArguments.getIdFromName(guildName);
			if (rootGuildId == null) {
				Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text("Could not identify guild by name '"
					+ guildName + "'.", NamedTextColor.RED)));
				return;
			}

			Group rootGuild = LuckPermsIntegration.GM.loadGroup(rootGuildId).join().orElse(null);
			if (rootGuild == null) {
				Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text("Cannot change chat color for '"
					+ rootGuildId + "' as this guild does not exist.", NamedTextColor.RED)));
				return;
			}

			TextComponent res = changeGuildChatColor(plugin, rootGuild, color, sender).join();
			Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(res));
		});
	}

	public static CompletableFuture<TextComponent> changeGuildChatColor(Plugin plugin, Group rootGuild, TextColor color, CommandSender sender) {
		CompletableFuture<TextComponent> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			String rootGuildId = rootGuild.getName();
			String plainTag = LuckPermsIntegration.getGuildPlainTag(rootGuild);
			if (plainTag == null) {
				future.complete(Component.text("Cannot change chat color for '" + rootGuildId + "' as the tag could not be obtained.", NamedTextColor.RED));
				return;
			}

			try {
				MonumentaNetworkChatIntegration.changeGuildChannelColor(sender, plainTag, color);
			} catch (WrapperCommandSyntaxException e) {
				future.complete(Component.text("Failed to change chat color for '" + rootGuildId + "': " + e.getMessage(), NamedTextColor.RED));
				return;
			}

			AuditListener.log("<+> Edited guild chat color for guild '" + rootGuildId + "' (" + plainTag + ")" +
				"\nTask executed by " + sender.getName());
			future.complete(Component.text("Successfully changed guild chat color for '" +
					rootGuildId + "' to ", NamedTextColor.GOLD)
				.append(Component.text(color.asHexString(), color)));
		});
		return future;
	}
}
