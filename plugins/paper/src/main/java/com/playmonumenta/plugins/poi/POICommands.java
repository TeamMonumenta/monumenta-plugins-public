package com.playmonumenta.plugins.poi;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Arrays;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class POICommands {
	public static void register() {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.weeklypoi");
		String[] poiNames = Arrays.stream(POI.values()).map(POI::getName).toArray(String[]::new);

		// CONQUER POI COMMAND
		new CommandAPICommand("weeklypoi")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("conquer"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				new MultiLiteralArgument(poiNames))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				String poiName = (String) args[1];
				POI poi = POI.getPOI(poiName);
				if (poi == null || poi == POI.NONE) {
					CommandAPI.fail("Invalid POI name " + poiName);
					throw new RuntimeException();
				}
				boolean added = POIManager.getInstance().completePOI(player, poi);
				if (added) {
					player.sendMessage(Component.text("You've conquered " + poi.getCleanName() + " for the first time this week, earning you bonus loot!", NamedTextColor.GOLD));
				}
			})
			.register();

		// DISPLAY CLEARED POIS COMMAND
		new CommandAPICommand("weeklypoi")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("list"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Set<POI> pois = POIManager.getInstance().mPlayerPOI.get(player.getUniqueId());
				player.sendMessage(Component.text("Uncleared Points of Interest are displayed below in red. Your first clear of each this week will earn you bonus loot!", NamedTextColor.GOLD));
				for (POI poi : POI.values()) {
					if (poi == POI.NONE) {
						continue;
					}
					player.sendMessage(Component.text(" - " + poi.getCleanName(), pois != null && pois.contains(poi) ? NamedTextColor.GREEN : NamedTextColor.RED));
				}
			})
			.register();
	}
}
