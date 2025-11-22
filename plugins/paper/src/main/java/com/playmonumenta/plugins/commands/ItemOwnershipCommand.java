package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.integrations.luckperms.GuildPermission;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemOwnershipCommand {
	public static void register() {
		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		new CommandAPICommand("itemownership")
			.withPermission("monumenta.command.itemownership")
			.withSubcommand(
				new CommandAPICommand("add")
					.withSubcommand(
						new CommandAPICommand("player")
							.withArguments(playerArg)
							.executes((sender, args) -> {
								Player player = args.getByArgument(playerArg);
								UUID uuid = player.getUniqueId();
								ItemStack item = player.getInventory().getItemInMainHand();
								if (!ItemStatUtils.isOwnable(item)) {
									player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
									player.sendMessage(Component.text("This item is not a valid item for infusing!", NamedTextColor.RED));
								} else if (!ItemStatUtils.hasInfusion(item, InfusionType.OWNED)) {
									int currentExp = ExperienceUtils.getTotalExperience(player);
									if (currentExp >= ExperienceUtils.LEVEL_30) {
										ExperienceUtils.setTotalExperience(player, currentExp - ExperienceUtils.LEVEL_30);
										player.playSound(player, Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
										EntityUtils.fireworkAnimation(player);
										ItemStatUtils.addInfusion(item, InfusionType.OWNED, 1, uuid);
										player.sendMessage(Component.text("You have added your Owned infusion on this item.", NamedTextColor.GREEN));
									} else {
										player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
										player.sendMessage(Component.text("You do not have enough experience points!", NamedTextColor.RED));
									}
								} else {
									player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
									player.sendMessage(Component.text("This item is already infused with Owned!", NamedTextColor.RED));
								}
							})
					)
					.withSubcommand(
						new CommandAPICommand("guild")
							.withArguments(new TextArgument("guild name").replaceSuggestions(GuildArguments.NAME_SUGGESTIONS))
							.executes((sender, args) -> {
								Player player = CommandUtils.getPlayerFromSender(sender);
								ItemStack item = player.getInventory().getItemInMainHand();
								String guildName = args.getUnchecked("guild name");
								String guildId = GuildArguments.getIdFromName(guildName);
								Group guild = LuckPermsIntegration.getGroup(guildId);
								if (!ItemStatUtils.isOwnable(item) || guild == null) {
									player.sendMessage(Component.text("The guild or the item held are not valid.", NamedTextColor.RED));
								} else { // forceadd infusion based on specified guild#+guild plot id
									Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guild);
									if (ItemStatUtils.hasInfusion(item, InfusionType.OWNED)) {
										player.sendMessage(Component.text("Removed prior guild's Owned infusion.", NamedTextColor.YELLOW));
										ItemStatUtils.removeInfusion(item, InfusionType.OWNED);
									}
									ItemStatUtils.addInfusion(item, InfusionType.OWNED, 1, "guild#" + guildPlotId, true);
									player.sendMessage(Component.text("You have added the specified guild's Owned infusion on this item.", NamedTextColor.GREEN));
								}
							}))
					.withSubcommand(
						new CommandAPICommand("playerguild")
							.withArguments(playerArg)
							.executes((sender, args) -> {
								Player player = args.getByArgument(playerArg);
								ItemStack item = player.getInventory().getItemInMainHand();
								Group playerGuild = LuckPermsIntegration.getGuild(player);
								if (!ItemStatUtils.isOwnable(item)) {
									player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
									player.sendMessage(Component.text("This item is not a valid item for infusing!", NamedTextColor.RED));
								} else if (playerGuild == null) { // player is guildless
									player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
									player.sendMessage(Component.text("You do not have a guild!", NamedTextColor.RED));
								} else if (!GuildPermission.GUILD_OWNED_INFUSION.hasAccess(playerGuild, player)) { // permission check to add Owned
									player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
									player.sendMessage(Component.text("You do not have permission to add Owned for your guild!", NamedTextColor.RED));
								} else { // add infusion based on guild#+guild plot id
									Long guildPlotId = LuckPermsIntegration.getGuildPlotId(playerGuild);
									if (!ItemStatUtils.hasInfusion(item, InfusionType.OWNED)) {
										int currentExp = ExperienceUtils.getTotalExperience(player);
										if (currentExp >= ExperienceUtils.LEVEL_30) {
											ExperienceUtils.setTotalExperience(player, currentExp - ExperienceUtils.LEVEL_30);
											player.playSound(player, Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
											EntityUtils.fireworkAnimation(player);
											ItemStatUtils.addInfusion(item, InfusionType.OWNED, 1, "guild#" + guildPlotId, true);
											player.sendMessage(Component.text("You have added your guild's Owned infusion on this item.", NamedTextColor.GREEN));
										} else {
											player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
											player.sendMessage(Component.text("You do not have enough experience points!", NamedTextColor.RED));
										}
									} else {
										player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
										player.sendMessage(Component.text("This item is already infused with Owned!", NamedTextColor.RED));
									}
								}
							})))
			.withSubcommand(
				new CommandAPICommand("remove")
					.withArguments(playerArg)
					.executes((sender, args) -> {
						Player player = args.getByArgument(playerArg);
						ItemStack item = player.getInventory().getItemInMainHand();
						if (!ItemStatUtils.isOwnable(item)) {
							player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
							player.sendMessage(Component.text("This item is not a valid item for uninfusing!", NamedTextColor.RED));
						} else if (!ItemStatUtils.hasInfusion(item, InfusionType.OWNED)) {
							player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
							player.sendMessage(Component.text("This item is not infused with Owned!", NamedTextColor.RED));
						} else {
							String infuserGuild = ItemStatUtils.getInfuserNpc(item, InfusionType.OWNED);
							UUID infuserPlayer = ItemStatUtils.getInfuser(item, InfusionType.OWNED);
							if (infuserGuild != null && infuserGuild.contains("guild#")) { // guild case, check permissions
								Long guildPlotId = Long.parseLong(infuserGuild.substring(6));
								Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotId);
								if (guild == null || GuildPermission.GUILD_OWNED_INFUSION.hasAccess(guild, player)) { // success - guild case
									player.playSound(player, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 1.0f, 1.0f);
									EntityUtils.fireworkAnimation(player);
									ItemStatUtils.removeInfusion(item, InfusionType.OWNED);
									player.sendMessage(Component.text("You have removed your guild's Owned infusion from this item.", NamedTextColor.GREEN));
								} else { // fail
									player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
									player.sendMessage(Component.text("You do not have permission to remove Owned from this item!", NamedTextColor.RED));
								}
							} else if (infuserPlayer != null && infuserPlayer.equals(player.getUniqueId())) { // success - player case
								player.playSound(player, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 1.0f, 1.0f);
								EntityUtils.fireworkAnimation(player);
								ItemStatUtils.removeInfusion(item, InfusionType.OWNED);
								player.sendMessage(Component.text("You have removed your Owned infusion from this item.", NamedTextColor.GREEN));
							} else { // failed
								player.playSound(player, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
								player.sendMessage(Component.text("You do not have permission to remove Owned from this item!", NamedTextColor.RED));
							}
						}
					})
			)
			.register();
	}
}
