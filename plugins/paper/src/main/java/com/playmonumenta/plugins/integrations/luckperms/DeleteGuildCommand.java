package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.Optional;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;

public class DeleteGuildCommand {
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.guild.mod.delete");

	public static CommandAPICommand attach(Plugin plugin, CommandAPICommand rootCommand) {
		return rootCommand
			.withArguments(
				GuildCommand.GUILD_NAME_ARG
			)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				if (GuildCommand.senderCannotRunCommand(sender, true)) {
					return;
				}

				String guildName = args.getByArgument(GuildCommand.GUILD_NAME_ARG);

				deleteGuild(plugin, sender, guildName);
			});
	}

	public static void deleteGuild(Plugin plugin, Audience audience, String guildName) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				String guildId = GuildArguments.getIdFromName(guildName);
				if (guildId == null) {
					audience.sendMessage(Component.text("Could identify group by name " + guildName, NamedTextColor.RED));
					return;
				}
				Optional<Group> optGuild = LuckPermsIntegration.loadGroup(guildId).join();
				if (optGuild.isEmpty()) {
					audience.sendMessage(Component.text("Could not find group " + guildId, NamedTextColor.RED));
					return;
				}
				Group guild = optGuild.get();
				if (!LuckPermsIntegration.isValidGuild(guild)) {
					audience.sendMessage(Component.text("Invalid guild " + guildName + " may cannot be deleted automatically", NamedTextColor.RED));
					audience.sendMessage(Component.text("The below clickables suggest commands rather than run them:", NamedTextColor.RED));
					audience.sendMessage(Component.text("[Delete guild chat (insert guild tag into command)]", NamedTextColor.LIGHT_PURPLE)
						.clickEvent(ClickEvent.suggestCommand("/chat channel delete ")));
					audience.sendMessage(Component.text("[Then delete the guild group]", NamedTextColor.LIGHT_PURPLE)
						.clickEvent(ClickEvent.suggestCommand("/lpgroup delete " + guildId + " recursive")));
					return;
				}
				String plainTag = LuckPermsIntegration.getGuildPlainTag(guild);
				if (plainTag != null) {
					MonumentaNetworkChatIntegration.deleteGuildChannel(audience, plainTag);
				}
				LuckPermsIntegration.deleteGroup(audience, guildId, true).join();
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					audience.sendMessage(Component.text("An exception occurred deleting this guild:", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(audience, ex);
				});
			}
		});
	}
}
