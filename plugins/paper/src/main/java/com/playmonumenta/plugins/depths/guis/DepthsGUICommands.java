package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.depths.abilities.gifts.BottomlessBowl;
import com.playmonumenta.plugins.depths.abilities.gifts.RainbowGeode;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public class DepthsGUICommands {

	private static final String COMMAND = "opendepthsgui";
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.opendepthsgui");
	private static final EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");

	public static void register() {
		registerCommand("summary", DepthsGUICommands::summary);
		registerCommand("roomchoice", DepthsGUICommands::roomChoice);
		registerCommand("upgrade", player -> upgrade(player, false));
		registerCommand("ability", player -> ability(player, false));
		registerCommand("weaponaspect", DepthsGUICommands::weaponAspect);
		registerCommand("removeability", DepthsGUICommands::remove);
		registerCommand("mutateability", DepthsGUICommands::mutate);
	}

	private static void registerCommand(String subcommand, Consumer<Player> action) {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withSubcommand(new CommandAPICommand(subcommand)
				.executesPlayer((player, args) -> {
					action.accept(player);
				}))
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withSubcommand(new CommandAPICommand(subcommand)
				.withArguments(playerArg)
				.executes((sender, args) -> {
					Player player = args.getByArgument(playerArg);
					action.accept(player);
				}))
			.register();
	}

	public static void roomChoice(Player player) {
		EnumSet<DepthsRoomType> roomChoices = DepthsManager.getInstance().generateRoomOptions(player);

		if (roomChoices == null) {
			MessagingUtils.sendActionBarMessage(player, "No room choices are available.");
			player.closeInventory();
			return;
		}
		new DepthsRoomChoiceGUI(player).openInventory(player, Plugin.getInstance());
	}

	public static void upgrade(Player player, boolean fromSummaryGUI) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUpgradeOptions(player);

		if (items == null || items.isEmpty()) {
			MessagingUtils.sendActionBarMessage(player, "No ability upgrade options to show.");
			DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
			if (depthsPlayer != null) {
				depthsPlayer.mEarnedRewards.poll();
			}
			return;
		}
		new DepthsUpgradeGUI(player, fromSummaryGUI).open();
	}

	public static void ability(Player player, boolean fromSummaryGUI) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUnlocks(player);

		if (items == null || items.isEmpty()) {
			MessagingUtils.sendActionBarMessage(player, "No abilities to choose from.");
			DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
			if (dp != null) {
				dp.mEarnedRewards.poll();
				if (dp.hasAbility(BottomlessBowl.ABILITY_NAME) || dp.hasAbility(RainbowGeode.ABILITY_NAME)) {
					dp.mRewardSkips++;
				}
			}
			return;
		}
		new DepthsAbilitiesGUI(player, fromSummaryGUI).open();
	}

	public static void generosity(Player player, boolean fromSummaryGUI) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null || dp.mGenerosityGifts.isEmpty()) {
			MessagingUtils.sendActionBarMessage(player, "No Generosity gift available.");
			if (dp != null) {
				dp.mEarnedRewards.poll();
				if (dp.hasAbility(BottomlessBowl.ABILITY_NAME) || dp.hasAbility(RainbowGeode.ABILITY_NAME)) {
					dp.mRewardSkips++;
				}
			}
			return;
		}
		new DepthsGenerosityGUI(player, fromSummaryGUI).open();
	}

	public static void summary(Player player) {
		List<DepthsAbilityItem> items = DepthsManager.getInstance().getPlayerAbilitySummary(player);

		if (items == null || items.size() == 0) {
			MessagingUtils.sendActionBarMessage(player, "No abilities to summarize.");
			player.closeInventory();
			return;
		}
		new DepthsSummaryGUI(player).open();
	}

	public static void weaponAspect(Player player) {
		//If the player is not in the system or they already have selected a weapon aspect, return
		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
		if (depthsPlayer == null || depthsPlayer.mHasWeaponAspect) {
			return;
		}

		List<DepthsAbilityInfo<? extends WeaponAspectDepthsAbility>> weapons = depthsPlayer.mWeaponOfferings;

		if (weapons == null || weapons.isEmpty()) {
			return;
		}

		new DepthsWeaponAspectGUI(player).openInventory(player, Plugin.getInstance());
	}

	public static void remove(Player player) {
		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
		if (depthsPlayer == null || depthsPlayer.mUsedAbilityDeletion) {
			MessagingUtils.sendActionBarMessage(player, "You've already removed your ability for this floor!");
			return;
		}
		DepthsParty party = DepthsManager.getInstance().getPartyFromId(depthsPlayer);
		if (party == null) {
			return;
		}

		new DepthsRemoveAbilityGUI(player, player.getLocation().getX() < party.mNoPassiveRemoveRoomStartX).openInventory(player, Plugin.getInstance());
	}

	public static void mutate(Player player) {
		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
		if (depthsPlayer == null || depthsPlayer.mUsedAbilityMutation) {
			MessagingUtils.sendActionBarMessage(player, "You've already mutated an ability on this floor!");
			return;
		}

		new DepthsMutateAbilityGUI(player).openInventory(player, Plugin.getInstance());
	}
}
