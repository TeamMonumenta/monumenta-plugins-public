package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.List;
import java.util.TreeMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_GUILD_NAME_MK;

public class ListGuilds {
	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.guild.mod.list");

		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("mod"))
			.withArguments(new LiteralArgument("list"))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				run(plugin, sender, false);
			})
			.register();

		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("mod"))
			.withArguments(new LiteralArgument("list"))
			.withArguments(new LiteralArgument("legacy"))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				run(plugin, sender, true);
			})
			.register();
	}

	public static void run(Plugin plugin, CommandSender sender, boolean legacyGuilds) throws WrapperCommandSyntaxException {
		if (ServerProperties.getShardName().contains("build")) {
			throw CommandAPI.failWithString("This command cannot be run on the build shard.");
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Group> guilds;
			try {
				guilds = LuckPermsIntegration.getGuilds(legacyGuilds, legacyGuilds).join();
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					sender.sendMessage(Component.text("Unable to list guilds:", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
				});
				return;
			}
			TreeMap<String, Component> sortedGuildComponents = new TreeMap<>();
			for (Group guild : guilds) {
				for (MetaNode node : guild.getNodes(NodeType.META)) {
					if (node.getMetaKey().equals(GUILD_MEMBER_GUILD_NAME_MK)) {
						String guildName = node.getMetaValue();
						String guildId = guild.getName();
						String guildSortKey = StringUtils.getNaturalSortKey(guildName + "_" + guildId);
						Component guildEntry = Component.text(guildName)
							.insertion(guildId);
						if (legacyGuilds) {
							String upgradeCommand = "/upgradeguild " + CommandUtils.quoteIfNeeded(guildId);
							guildEntry = guildEntry
								.hoverEvent(Component.text(upgradeCommand))
								.insertion(upgradeCommand)
								.clickEvent(ClickEvent.suggestCommand(upgradeCommand));
						} else {
							guildEntry = guildEntry
								.hoverEvent(Component.text("Group ID: " + guildId));
						}
						sortedGuildComponents.put(guildSortKey, guildEntry);
						break;
					}
				}
			}

			Bukkit.getScheduler().runTask(plugin, () -> {
				sender.sendMessage(Component.text("The guilds of Monumenta:",
					NamedTextColor.GOLD,
					TextDecoration.BOLD));
				for (Component guild : sortedGuildComponents.values()) {
					sender.sendMessage(Component.text("- ", NamedTextColor.GOLD)
						.append(guild));
				}
				sender.sendMessage(Component.text(
					"Found " + sortedGuildComponents.size() + " guilds.", NamedTextColor.GOLD));
			});
		});
	}
}
