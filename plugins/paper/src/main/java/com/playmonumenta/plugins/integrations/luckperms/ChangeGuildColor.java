package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import static com.playmonumenta.plugins.utils.MessagingUtils.ESCAPED_TEXT_COLOR_SUGGESTIONS;

public class ChangeGuildColor {
	private static final Argument<String> COLOR_ARG = new TextArgument("color").replaceSuggestions(ESCAPED_TEXT_COLOR_SUGGESTIONS);
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.guild.mod.color");

	public static CommandAPICommand attach(Plugin plugin, CommandAPICommand root) {
		return root
			.withArguments(
				GuildCommand.GUILD_NAME_ARG,
				COLOR_ARG
			)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				if (GuildCommand.senderCannotRunCommand(sender, true)) {
					return;
				}

				String guildName = (String) args[args.length - 2];
				TextColor color = MessagingUtils.colorFromString((String) args[args.length - 1]);

				if (color == null) {
					throw CommandAPI.failWithString("Color should either be the color's name or in the hex format '#RRGGBB'");
				}

				runChangeGuildColor(plugin, sender, guildName, color);
			});
	}

	public static void runChangeGuildColor(Plugin plugin, CommandSender sender, String guildName, TextColor color) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			TextComponent res = changeGuildColor(plugin, guildName, color, sender).join();
			Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(res));
		});
	}

	private static CompletableFuture<TextComponent> changeGuildColor(Plugin plugin, String guildName, TextColor color, CommandSender sender) {
		CompletableFuture<TextComponent> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			String rootGuildId = GuildArguments.getIdFromName(guildName);
			if (rootGuildId == null) {
				future.complete(Component.text("Could not identify guild by name '"
					+ guildName + "'.", NamedTextColor.RED));
				return;
			}

			Group rootGuild = LuckPermsIntegration.GM.loadGroup(rootGuildId).join().orElse(null);
			if (rootGuild == null) {
				future.complete(Component.text("Cannot change color for '"
					+ rootGuildId + "' as this guild does not exist.", NamedTextColor.RED));
				return;
			}
			Group memberGroup = GuildAccessLevel.MEMBER.loadGroupFromRoot(rootGuildId).join().orElse(null);
			if (memberGroup == null) {
				future.complete(Component.text("Cannot change color for '"
					+ rootGuildId + "'as it is missing its member group", NamedTextColor.RED));
				return;
			}

			NodeMap rootData = rootGuild.data();
			NodeMap memberData = memberGroup.data();
			for (Node node : rootData.toCollection()) {
				if (!(node instanceof MetaNode meta)) {
					continue;
				}

				if (meta.getMetaKey().equals(LuckPermsIntegration.GUILD_ROOT_COLOR_MK)) {
					rootData.remove(node);
				}
			}
			for (Node node : memberData.toCollection()) {
				if (!(node instanceof MetaNode meta)) {
					continue;
				}

				if (meta.getMetaKey().equals(LuckPermsIntegration.GUILD_MEMBER_ROOT_COLOR_MK)) {
					memberData.remove(node);
				}
			}

			rootData.add(MetaNode.builder(LuckPermsIntegration.GUILD_ROOT_COLOR_MK, color.asHexString()).build());
			LuckPermsIntegration.GM.saveGroup(rootGuild).join();

			memberData.add(MetaNode.builder(LuckPermsIntegration.GUILD_MEMBER_ROOT_COLOR_MK, color.asHexString()).build());
			LuckPermsIntegration.GM.saveGroup(memberGroup).join();

			try {
				LuckPermsIntegration.rebuildTag(sender, rootGuild).join();
			} catch (IllegalStateException ex) {
				Bukkit.getScheduler().runTask(plugin,
					() -> MMLog.warning("Could not rebuild guild tag for '" + rootGuildId + "' for: ", ex));
				future.complete(Component.text("Failed to rebuild guild tag", NamedTextColor.RED));
				return;
			}
			// push update
			LuckPermsIntegration.pushUpdate();

			AuditListener.log("<+> Edited tag color for guild '" + rootGuildId
				+ "'\nTask executed by " + sender.getName());
			future.complete(Component.text("Successfully changed guild color for '"
					+ rootGuildId + "' to ", NamedTextColor.GOLD)
				.append(Component.text(color.asHexString(), color)));
		});
		return future;
	}
}
