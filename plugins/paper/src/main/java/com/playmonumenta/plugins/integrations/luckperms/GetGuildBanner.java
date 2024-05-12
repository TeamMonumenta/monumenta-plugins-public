package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GetGuildBanner {
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.guild.getbanner");
	private static final CommandPermission MOD_PERMISSION = CommandPermission.fromString("monumenta.command.guild.mod.getbanner");

	public static void register(Plugin plugin) {
		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("getbanner"))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				run(sender);
			})
			.register();

		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("mod"))
			.withArguments(new LiteralArgument("getbanner"))
			.withArguments(GuildCommand.GUILD_NAME_ARG)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, MOD_PERMISSION);
				String guildName = args.getUnchecked("guild name");
				run(plugin, sender, guildName);
			})
			.register();
	}

	public static void run(CommandSender sender) throws WrapperCommandSyntaxException {
		CommandSender callee = CommandUtils.getCallee(sender);
		if (!(callee instanceof Player player)) {
			throw CommandAPI.failWithString("This command must be run as a player");
		}

		ItemStack banner = LuckPermsIntegration.getGuildBanner(player);
		if (banner == null) {
			throw CommandAPI.failWithString("You are not in a guild");
		}
		InventoryUtils.giveItem(player, banner);
	}

	public static void run(Plugin plugin, CommandSender sender, String guildName) throws WrapperCommandSyntaxException {
		if (ServerProperties.getShardName().contains("build")) {
			throw CommandAPI.failWithString("This command cannot be run on the build shard.");
		}

		String guildId = GuildArguments.getIdFromName(guildName);
		if (guildId == null) {
			throw CommandAPI.failWithString("Could not identify guild by name: " + guildName);
		}

		CommandSender callee = CommandUtils.getCallee(sender);
		if (!(callee instanceof Player player)) {
			throw CommandAPI.failWithString("This command must be run as a player");
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Group guildRoot = LuckPermsIntegration
				.loadGroup(guildId)
				.join()
				.orElse(null);

			if (guildRoot == null) {
				callee.sendMessage(Component.text("Could not load guild " + guildName, NamedTextColor.RED));
				return;
			}

			Bukkit.getScheduler().runTask(plugin, () -> {
				ItemStack banner = LuckPermsIntegration.getGuildBanner(guildRoot);
				InventoryUtils.giveItem(player, banner);
			});
		});
	}
}
