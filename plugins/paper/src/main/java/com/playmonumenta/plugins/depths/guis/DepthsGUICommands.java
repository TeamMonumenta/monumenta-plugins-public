package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DepthsGUICommands {
	public static void register(Plugin plugin) {
		final String command = "opendepthsgui";

		new CommandAPICommand(command)
			.withPermission(CommandPermission.fromString("monumenta.command.opendepthsgui"))
			.withSubcommand(new CommandAPICommand("summary")
				.withArguments(new EntitySelectorArgument.OnePlayer("player"))
				.executes((sender, args) -> {
					Player player = (Player) args[0];
					summary(plugin, player);
				}))
			.withSubcommand(new CommandAPICommand("roomchoice")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player player = (Player) args[0];
						roomChoice(plugin, player);
					}))
			.withSubcommand(new CommandAPICommand("upgrade")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player player = (Player) args[0];
						upgrade(plugin, player, false);
					}))
			.withSubcommand(new CommandAPICommand("ability")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player player = (Player) args[0];
						ability(plugin, player, false);
					}))
			.withSubcommand(new CommandAPICommand("weaponaspect")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player player = (Player) args[0];
						//If the player is not in the system or they already have selected a weapon aspect, return
						DepthsPlayer depthsPlayer = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
						if (depthsPlayer == null || depthsPlayer.mHasWeaponAspect) {
							return;
						}

						List<DepthsAbilityInfo<? extends WeaponAspectDepthsAbility>> weapons = depthsPlayer.mWeaponOfferings;

						if (weapons == null || weapons.size() == 0) {
							return;
						}

						new DepthsWeaponAspectGUI(player).openInventory(player, plugin);
					}))
			.withSubcommand(new CommandAPICommand("removeability")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player player = (Player) args[0];

						DepthsPlayer depthsPlayer = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
						if (depthsPlayer == null || depthsPlayer.mUsedAbilityDeletion) {
							MessagingUtils.sendActionBarMessage(player, "You've already removed your ability for this floor!");
							return;
						}
						DepthsParty party = DepthsManager.getInstance().getPartyFromId(depthsPlayer);
						if (party == null) {
							return;
						}

						new DepthsRemoveAbilityGUI(player, player.getLocation().getX() < party.mNoPassiveRemoveRoomStartX).openInventory(player, plugin);
					}))
			.withSubcommand(new CommandAPICommand("mutateability")
				.withArguments(new EntitySelectorArgument.OnePlayer("player"))
				.executes((sender, args) -> {
					Player player = (Player) args[0];

					DepthsPlayer depthsPlayer = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
					if (depthsPlayer == null || depthsPlayer.mUsedAbilityMutation) {
						MessagingUtils.sendActionBarMessage(player, "You've already mutated an ability on this floor!");
						return;
					}

					new DepthsMutateAbilityGUI(player).openInventory(player, plugin);
				}))
			.register();
	}

	public static void roomChoice(Plugin plugin, Player player) {
		EnumSet<DepthsRoomType> roomChoices = DepthsManager.getInstance().generateRoomOptions(player);

		if (roomChoices == null) {
			MessagingUtils.sendActionBarMessage(player, "No room choices are available.");
			player.closeInventory();
			return;
		}
		new DepthsRoomChoiceGUI(player).openInventory(player, plugin);
	}

	public static void upgrade(Plugin plugin, Player player, boolean fromSummaryGUI) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUpgradeOptions(player);

		if (items == null || items.size() == 0) {
			MessagingUtils.sendActionBarMessage(player, "No ability upgrade options to show.");
			DepthsPlayer depthsPlayer = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
			if (depthsPlayer != null) {
				depthsPlayer.mEarnedRewards.poll();
			}
			return;
		}
		new DepthsUpgradeGUI(player, fromSummaryGUI).openInventory(player, plugin);
	}

	public static void ability(Plugin plugin, Player player, boolean fromSummaryGUI) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUnlocks(player);

		if (items == null || items.size() == 0) {
			MessagingUtils.sendActionBarMessage(player, "No abilities to choose from.");
			DepthsPlayer depthsPlayer = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
			if (depthsPlayer != null) {
				depthsPlayer.mEarnedRewards.poll();
			}
			return;
		}
		new DepthsAbilitiesGUI(player, fromSummaryGUI).openInventory(player, plugin);
	}

	public static void generosity(Plugin plugin, Player player, boolean fromSummaryGUI) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null || dp.mGenerosityGifts.isEmpty()) {
			MessagingUtils.sendActionBarMessage(player, "No abilities to choose from.");
			if (dp != null) {
				dp.mEarnedRewards.poll();
			}
			return;
		}
		new DepthsGenerosityGUI(player, fromSummaryGUI).openInventory(player, plugin);
	}

	public static void summary(Plugin plugin, Player player) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(player);

		if (items == null || items.size() == 0) {
			MessagingUtils.sendActionBarMessage(player, "No abilities to summarize.");
			player.closeInventory();
			return;
		}
		new DepthsSummaryGUI(player).openInventory(player, plugin);
	}
}
