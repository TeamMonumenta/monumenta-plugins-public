package com.playmonumenta.plugins.commands;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class StatTrackItem extends GenericCommand {

	public static final ImmutableMap<String, InfusionType> OPTIONS = ImmutableMap.<String, InfusionType>builder()
		.put("kills", InfusionType.STAT_TRACK_KILLS)
		.put("damage", InfusionType.STAT_TRACK_DAMAGE)
		.put("melee", InfusionType.STAT_TRACK_MELEE)
		.put("projectile", InfusionType.STAT_TRACK_PROJECTILE)
		.put("magic", InfusionType.STAT_TRACK_MAGIC)
		.put("boss", InfusionType.STAT_TRACK_BOSS)
		.put("spawners", InfusionType.STAT_TRACK_SPAWNER)
		.put("consumed", InfusionType.STAT_TRACK_CONSUMED)
		.put("blocks", InfusionType.STAT_TRACK_BLOCKS)
		.put("blocksbroken", InfusionType.STAT_TRACK_BLOCKS_BROKEN)
		.put("deaths", InfusionType.STAT_TRACK_DEATH)
		.put("riptide", InfusionType.STAT_TRACK_RIPTIDE)
		.put("shield", InfusionType.STAT_TRACK_SHIELD_BLOCKED)
		.put("repair", InfusionType.STAT_TRACK_REPAIR)
		.put("convert", InfusionType.STAT_TRACK_CONVERT)
		.put("dmgtaken", InfusionType.STAT_TRACK_DAMAGE_TAKEN)
		.put("healdone", InfusionType.STAT_TRACK_HEALING_DONE)
		.put("fish", InfusionType.STAT_TRACK_FISH_CAUGHT)
		.build();

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.stattrackhelditem");

		Argument<?> selectionArg = new MultiLiteralArgument("selection", OPTIONS.keySet().toArray(String[]::new));

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(selectionArg);
		new CommandAPICommand("stattrackhelditem")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				InfusionType selection = OPTIONS.get(args.getUnchecked("selection"));
				if (selection == null) {
					throw CommandAPI.failWithString("Invalid stat selection; how did we get here?");
				}
				run(args.getUnchecked("player"), selection);
			})
			.register();

		perms = CommandPermission.fromString("monumenta.command.modstattrackhelditem");

		arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("number"));
		new CommandAPICommand("modstattrackhelditem")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				runMod(args.getUnchecked("player"), args.getUnchecked("number"));
			})
			.register();

		perms = CommandPermission.fromString("monumenta.command.removestattrackhelditem");

		arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("removestattrackhelditem")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				runRemove(args.getUnchecked("player"));
			})
			.register();
	}

	/**
	 * The typical infusion logic that players will run themselves from an npc
	 *
	 * @param player the player who is stat tracking their gear
	 * @param option the stat track enchant option to infuse with
	 */
	private static void run(Player player, InfusionType option) {

		//Check to see if the player is a patron
		if (
			PlayerData.getPatreonDollars(player) < StatTrackManager.PATRON_TIER
		) {
			player.sendMessage("You must be an active $" + StatTrackManager.PATRON_TIER + " patron or higher to infuse items with stat tracking!");
			return;
		}
		//Check to see if the item in hand is already infused
		ItemStack is = player.getInventory().getItemInMainHand();
		if (ItemStatUtils.getInfusionLevel(is, InfusionType.STAT_TRACK) > 0) {
			player.sendMessage("This item is already infused with stat tracking!");
			return;
		}
		//Add the chosen stat tracking enchant to the item
		ItemStatUtils.addInfusion(is, InfusionType.STAT_TRACK, 1, player.getUniqueId(), false);
		ItemStatUtils.addInfusion(is, option, 1, player.getUniqueId());
		EntityUtils.fireworkAnimation(player);
	}

	/**
	 * Command to be run by moderators to manually set the stat on an item
	 *
	 * @param player the mod who ran the command (get their item in hand)
	 * @param stat   the numerical value the stat should have
	 */
	private static void runMod(Player player, int stat) {
		//Check to see if the item in hand is already infused
		ItemStack is = player.getInventory().getItemInMainHand();

		if (ItemStatUtils.getInfusionLevel(is, InfusionType.STAT_TRACK) <= 0) {
			player.sendMessage("This item is not infused with stat tracking!");
			return;
		}
		//Update the counter of the item
		InfusionType type = StatTrackManager.getTrackingType(is);
		if (type == null) {
			player.sendMessage("Could not find stat track infusion type!");
			return;
		} else {
			StatTrackManager.incrementStat(is, player, type, stat);
			player.sendMessage("Updated the stat on your item to desired value!");
			EntityUtils.fireworkAnimation(player);
		}
	}

	/**
	 * Removes the stat track infusion from the item in hand
	 *
	 * @param player player to get item from
	 */
	private static void runRemove(Player player) {
		//Check to see if the item in hand is already infused
		ItemStack is = player.getInventory().getItemInMainHand();
		InfusionType type = StatTrackManager.getTrackingType(is);
		if (type == null) {
			player.sendMessage("Could not find stat track infusion type!");
		} else if (ItemStatUtils.getInfusionLevel(is, InfusionType.STAT_TRACK) <= 0) {
			player.sendMessage("This item is not infused with stat tracking!");
		} else if (StatTrackManager.isPlayersItem(is, player)) {
			ItemStatUtils.removeInfusion(is, InfusionType.STAT_TRACK, false);
			for (InfusionType stat : InfusionType.STAT_TRACK_OPTIONS) {
				ItemStatUtils.removeInfusion(is, stat, false);
			}
			ItemUpdateHelper.generateItemStats(is);
			player.sendMessage("Removed Stat Tracking from your item!");
			EntityUtils.fireworkAnimation(player);

		} else {
			player.sendMessage("You cannot remove stat track from an item not tracked by you!");
		}
	}
}
