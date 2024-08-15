package com.playmonumenta.plugins.market;

import com.google.gson.GsonBuilder;
import com.playmonumenta.plugins.market.gui.MarketGui;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

public class MarketCommands {

	public static void register() {


		new CommandAPICommand("openmarketgui")
			.withPermission(CommandPermission.fromString("monumenta.command.market"))
			.executesPlayer((player, args) -> {
				new MarketGui(player).open();
			})
			.register();

		CommandPermission perms = CommandPermission.fromString("monumenta.market.commands");
		new CommandAPICommand("market")
			.withPermission(perms)
			.executesPlayer((player, args) -> {
				MarketGui gui = new MarketGui(player);
				gui.open();
			})
			.register();

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("addlisting"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("getPlayerlistings"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				player.sendMessage(MarketManager.getInstance().getListingsOfPlayer(player).toString());

			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("reloadConfig"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				MarketManager.reloadConfig();
				GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.setPrettyPrinting();
				player.sendMessage(gsonBuilder.create().toJson(MarketManager.getConfig()));
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("linkListingToPlayer"));
		arguments.add(new PlayerArgument("player"));
		arguments.add(new LongArgument("listingID"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				Player targetPlayer = args.getUnchecked("player");
				long listingID = args.getUnchecked("listingID");
				MarketManager.getInstance().linkListingToPlayerData(targetPlayer, listingID);
				MarketAudit.logManualLinking(targetPlayer, listingID);
				player.sendMessage("Listing " + listingID + "linked to player " + targetPlayer.getName());
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("unlinkListingFromPlayer"));
		arguments.add(new PlayerArgument("player"));
		arguments.add(new LongArgument("listingID"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				Player targetPlayer = args.getUnchecked("player");
				long listingID = args.getUnchecked("listingID");
				MarketManager.getInstance().unlinkListingFromPlayerData(targetPlayer, listingID);
				MarketAudit.logManualUnlinking(targetPlayer, listingID);
				player.sendMessage("Listing " + listingID + "unlinked from player " + targetPlayer.getName());
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("resyncOwnership"));
		arguments.add(new PlayerArgument("player"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				Player targetPlayer = args.getUnchecked("player");
				MarketManager.getInstance().resyncOwnership(targetPlayer);
			})
			.register();

		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(List.of(new LiteralArgument("filters"), new LiteralArgument("get"), new PlayerArgument("player")))
			.executesPlayer((player, args) -> {
				Player targetPlayer = args.getUnchecked("player");
				MarketManager.getInstance().getAllFiltersData(targetPlayer);
			})
			.register();

		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(List.of(new LiteralArgument("filters"), new LiteralArgument("reset"), new PlayerArgument("player")))
			.executesPlayer((player, args) -> {
				Player targetPlayer = args.getUnchecked("player");
				MarketManager.getInstance().resetPlayerFilters(targetPlayer);
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("getlisting"));
		arguments.add(new LongArgument("listingID"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				player.sendMessage(MarketRedisManager.getListing(args.getUnchecked("listingID")).toBeautifiedJsonString());

			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("getListingsFromIndex"));
		arguments.add(new StringArgument("indexID"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				MarketListingIndex index = MarketListingIndex.valueOf(args.getUnchecked("indexID"));
				player.sendMessage(index.getListingsFromIndex(true).toString());
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("dumpIndexesContents"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String dump = MarketListingIndex.dumpAllListingsContents();
				player.sendMessage(dump.split("\n"));
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("resyncAllIndexes"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				MarketListingIndex.resyncAllIndexes();
				player.sendMessage("Synced all indexes");
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("lockAllListings"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				MarketManager.lockAllListings(player);
			})
			.register();


		new CommandAPICommand("openmarketgui")
			.withPermission("monumenta.command.openmarketgui")
			.executesPlayer((player, args) -> {
				MarketManager.openNewMarketGUI(player);
			})
			.register();
		new CommandAPICommand("openmarketgui")
			.withPermission("monumenta.command.openmarketgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player viewer = args.getUnchecked("player");
				if (sender instanceof Player playerSender) {
					viewer = playerSender;
				}
				MarketManager.openNewMarketGUI(viewer);
			})
			.register();
	}

}
