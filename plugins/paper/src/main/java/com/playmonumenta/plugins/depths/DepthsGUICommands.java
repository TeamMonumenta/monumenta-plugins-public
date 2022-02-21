package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.utils.MessagingUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.List;

public class DepthsGUICommands {
	public static void register(Plugin plugin) {
		final String command = "opendepthsgui";

		new CommandAPICommand(command)
			.withPermission(CommandPermission.fromString("monumenta.command.opendepthsgui"))
			.withSubcommand(new CommandAPICommand("summary")
				.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
				.executes((sender, args) -> {
					Player player = (Player)args[0];
					List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(player);

					if (items == null || items.size() == 0) {
						MessagingUtils.sendActionBarMessage(player, "No abilities to summarize.");
						player.closeInventory();
						return;
					}
					new DepthsSummaryGUI(player).openInventory(player, plugin);
				}))
			.withSubcommand(new CommandAPICommand("roomchoice")
					.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
					.executes((sender, args) -> {
						Player player = (Player)args[0];
						EnumSet<DepthsRoomType> roomChoices = DepthsManager.getInstance().generateRoomOptions(player);

						if (roomChoices == null) {
							MessagingUtils.sendActionBarMessage(player, "No room choices are available.");
							player.closeInventory();
							return;
						}
						new DepthsRoomChoiceGUI(player).openInventory(player, plugin);
					}))
			.withSubcommand(new CommandAPICommand("upgrade")
					.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
					.executes((sender, args) -> {
						Player player = (Player)args[0];
						List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUpgradeOptions(player);

						if (items == null || items.size() == 0) {
							MessagingUtils.sendActionBarMessage(player, "No ability upgrade options to show.");
							DepthsManager.getInstance().mPlayers.get(player.getUniqueId()).mEarnedRewards.poll();
							return;
						}
						new DepthsUpgradeGUI(player).openInventory(player, plugin);
					}))
			.withSubcommand(new CommandAPICommand("ability")
					.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
					.executes((sender, args) -> {
						Player player = (Player)args[0];
						List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUnlocks(player);

						if (items == null || items.size() == 0) {
							MessagingUtils.sendActionBarMessage(player, "No abilities to choose from.");
							DepthsManager.getInstance().mPlayers.get(player.getUniqueId()).mEarnedRewards.poll();
							return;
						}
						new DepthsAbilitiesGUI(player).openInventory(player, plugin);
					}))
			.withSubcommand(new CommandAPICommand("weaponaspect")
					.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
					.executes((sender, args) -> {
						Player player = (Player)args[0];
						//If the player is not in the system or they already have selected a weapon aspect, return
						if (!DepthsManager.getInstance().isInSystem(player) || DepthsManager.getInstance().mPlayers.get(player.getUniqueId()).mHasWeaponAspect) {
							return;
						}

						List<WeaponAspectDepthsAbility> weapons = DepthsManager.getInstance().mPlayers.get(player.getUniqueId()).mWeaponOfferings;

						if (weapons == null || weapons.size() == 0) {
							return;
						}

						new DepthsWeaponAspectGUI(player).openInventory(player, plugin);
					}))
			.withSubcommand(new CommandAPICommand("removeability")
					.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
					.executes((sender, args) -> {
						Player player = (Player) args[0];

						if (!DepthsManager.getInstance().isInSystem(player) || DepthsManager.getInstance().mPlayers.get(player.getUniqueId()).mUsedAbilityDeletion) {
							MessagingUtils.sendActionBarMessage(player, "You've already removed your ability for this floor!");
							return;
						}

						new DepthsRemoveAbilityGUI(player).openInventory(player, plugin);
					}))
			.register();
	}
}
