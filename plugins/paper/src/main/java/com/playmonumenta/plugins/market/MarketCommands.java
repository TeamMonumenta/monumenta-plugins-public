package com.playmonumenta.plugins.market;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.ListArgumentBuilder;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

public class MarketCommands {

	public static void register() {


		new CommandAPICommand("openmarketgui")
			.withPermission(CommandPermission.fromString("monumenta.command.market"))
			.executesPlayer((player, args) -> {
				new MarketGUI(player, MarketGUI.MarketGuiTab.MAIN_MENU).open();
			})
			.register();

		CommandPermission perms = CommandPermission.fromString("monumenta.market.commands");
		new CommandAPICommand("market")
			.withPermission(perms)
			.executesPlayer((player, args) -> {
				MarketGUI gui = new MarketGUI(player, MarketGUI.MarketGuiTab.MAIN_MENU);
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
		arguments.add(new LiteralArgument("getlisting"));
		arguments.add(new LongArgument("listingID"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				player.sendMessage(MarketRedisManager.getListing((long)args[0]).toBeautifiedJsonString());

			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("getlistings"));
		Long[] tmpList = new Long[2000];
		for (int i = 0; i < 2000; i++) {
			tmpList[i] = (long)i;
		}
		arguments.add(new ListArgumentBuilder<Long>("listingsID").allowDuplicates(true).withList(tmpList).withStringMapper().buildGreedy());
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				List<MarketListing> listings = MarketRedisManager.getListings(List.of((Long[]) args[0]));
				for (MarketListing listing : listings) {
					player.sendMessage(listing.toPlayerReadableComponent(player));
				}
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("getListingsFromIndex"));
		arguments.add(new StringArgument("indexID"));
		new CommandAPICommand("market")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				MarketListingIndex index = MarketListingIndex.valueOf((String)args[0]);
				player.sendMessage(index.getListingsFromIndex(null, true).toString());
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
				Player player = (Player) args[0];
				Player viewer = player;
				if (sender instanceof Player playerSender) {
					viewer = playerSender;
				}
				MarketManager.openNewMarketGUI(viewer);
			})
			.register();
	}

}