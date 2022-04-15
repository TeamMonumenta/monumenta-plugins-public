package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;


/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class StatTrackItem extends GenericCommand {


	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.stattrackhelditem");

		HashMap<String, InfusionType> options = new HashMap<>();
		options.put("kills", InfusionType.STAT_TRACK_KILLS);
		options.put("damage", InfusionType.STAT_TRACK_DAMAGE);
		options.put("melee", InfusionType.STAT_TRACK_MELEE);
		options.put("projectile", InfusionType.STAT_TRACK_PROJECTILE);
		options.put("magic", InfusionType.STAT_TRACK_MAGIC);
		options.put("boss", InfusionType.STAT_TRACK_BOSS);
		options.put("spawners", InfusionType.STAT_TRACK_SPAWNER);
		options.put("consumed", InfusionType.STAT_TRACK_CONSUMED);
		options.put("blocks", InfusionType.STAT_TRACK_BLOCKS);

		Argument selectionArg = new MultiLiteralArgument(options.keySet().toArray(new String[options.size()]));

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(selectionArg);
		new CommandAPICommand("stattrackhelditem")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				InfusionType selection = options.get((String) args[1]);
				if (selection == null) {
					CommandAPI.fail("Invalid stat selection; how did we get here?");
					return;
				}
				run((Player) args[0], selection);
			})
			.register();

		perms = CommandPermission.fromString("monumenta.command.modstattrackhelditem");

		arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new IntegerArgument("number"));
		new CommandAPICommand("modstattrackhelditem")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				runMod((Player) args[0], (int) args[1]);
			})
			.register();

		perms = CommandPermission.fromString("monumenta.command.removestattrackhelditem");

		arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("removestattrackhelditem")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				runRemove((Player) args[0]);
			})
			.register();
	}

	/**
	 * The typical infusion logic that players will run themselves from an npc
	 * @param player the player who is stat tracking their gear
	 * @param option the stat track enchant option to infuse with
	 */
	private static void run(Player player, InfusionType option) throws WrapperCommandSyntaxException {

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
		animate(player);
	}

	/**
	 * Command to be run by moderators to manually set the stat on an item
	 * @param player the mod who ran the command (get their item in hand)
	 * @param stat the numerical value the stat should have
	 */
	private static void runMod(Player player, int stat) throws WrapperCommandSyntaxException {
		//Check to see if the item in hand is already infused
		ItemStack is = player.getInventory().getItemInMainHand();

		if (ItemStatUtils.getInfusionLevel(is, ItemStatUtils.InfusionType.STAT_TRACK) <= 0) {
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
			animate(player);
		}
	}

	/**
	 * Removes the stat track infusion from the item in hand
	 * @param player player to get item from
	 */
	private static void runRemove(Player player) {
		//Check to see if the item in hand is already infused
		ItemStack is = player.getInventory().getItemInMainHand();
		InfusionType type = StatTrackManager.getTrackingType(is);
		if (type == null) {
			player.sendMessage("Could not find stat track infusion type!");
		} else if (ItemStatUtils.getInfusionLevel(is, ItemStatUtils.InfusionType.STAT_TRACK) <= 0) {
			player.sendMessage("This item is not infused with stat tracking!");
		} else if (StatTrackManager.isPlayersItem(is, player)) {
			ItemStatUtils.removeInfusion(is, ItemStatUtils.InfusionType.STAT_TRACK, false);
			for (InfusionType stat : InfusionType.STAT_TRACK_OPTIONS) {
				ItemStatUtils.removeInfusion(is, stat, false);
			}
			ItemStatUtils.generateItemStats(is);
			player.sendMessage("Removed Stat Tracking from your item!");
			animate(player);

		} else {
			player.sendMessage("You cannot remove stat track from an item not tracked by you!");
		}
	}

	//Firework effect for stat infusion
	private static void animate(Player player) {
		Location loc = player.getLocation();
		Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK);
		FireworkMeta fwm = fw.getFireworkMeta();
		FireworkEffect.Builder fwBuilder = FireworkEffect.builder();
		fwBuilder.withColor(Color.RED, Color.GREEN, Color.BLUE);
		fwBuilder.with(FireworkEffect.Type.BURST);
		FireworkEffect fwEffect = fwBuilder.build();
		fwm.addEffect(fwEffect);
		fw.setFireworkMeta(fwm);

		new BukkitRunnable() {
			@Override
			public void run() {
				fw.detonate();
			}
		}.runTaskLater(Plugin.getInstance(), 5);
	}
}
