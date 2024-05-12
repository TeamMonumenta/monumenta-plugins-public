package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class ChangeGuildBanner {
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.guild.mod.setbanner");

	public static CommandAPICommand attach(Plugin plugin, CommandAPICommand root) {
		return root
			.withArguments(
				GuildCommand.GUILD_NAME_ARG
			)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				if (GuildCommand.senderCannotRunCommand(sender, true)) {
					return;
				}

				String guildName = args.getByArgument(GuildCommand.GUILD_NAME_ARG);
				String guildRootGroupId = GuildArguments.getIdFromName(guildName);
				if (guildRootGroupId == null) {
					sender.sendMessage(Component.text("Could not identify a guild by that name", NamedTextColor.RED));
					return;
				}

				if (!(sender instanceof Player senderPlayer)) {
					sender.sendMessage(Component.text("Guilds must be upgraded by a player", NamedTextColor.RED));
					return;
				}

				ItemStack guildBannerTemplate = senderPlayer.getInventory().getItemInMainHand();
				if (!ItemUtils.isBanner(guildBannerTemplate)) {
					senderPlayer.sendMessage(Component.text(
						"To upgrade a guild, you must hold their banner.",
						NamedTextColor.RED));
					return;
				}
				ItemMeta templateMeta = guildBannerTemplate.getItemMeta();

				final ItemStack guildBanner = new ItemStack(guildBannerTemplate.getType());
				ItemMeta meta = guildBanner.getItemMeta();
				if (templateMeta instanceof BannerMeta templateBannerMeta
					&& meta instanceof BannerMeta bannerMeta) {
					bannerMeta.setPatterns(templateBannerMeta.getPatterns());
				}
				guildBanner.setItemMeta(meta);

				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					try {
						Group guildRootGroup = LuckPermsIntegration.GM.loadGroup(guildRootGroupId).join()
							.orElse(null);
						if (guildRootGroup == null) {
							Bukkit.getScheduler().runTask(plugin, ()
								-> sender.sendMessage(Component.text("Guild not found", NamedTextColor.RED)));
							return;
						}

						LuckPermsIntegration.setGuildBanner(sender, guildRootGroup, plugin, guildBanner, true);
					} catch (Exception ex) {
						Bukkit.getScheduler().runTask(plugin, () -> {
							Bukkit.getScheduler().runTask(plugin, () -> {
								sender.sendMessage(Component.text(
									"An exception occurred changing the guild's banner:",
									NamedTextColor.RED));
								MessagingUtils.sendStackTrace(sender, ex);
							});
						});
					}
				});
			});
	}
}
