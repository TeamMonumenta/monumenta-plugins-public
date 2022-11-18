package com.playmonumenta.plugins.poi;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class POICommands {
	public static void register(Plugin plugin) {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.weeklypoi");
		String[] pois = Arrays.stream(POI.values()).map(POI::getName).toArray(String[]::new);

		// CONQUER POI COMMAND
		new CommandAPICommand("weeklypoi")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("conquer"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				new MultiLiteralArgument(pois))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				String poiName = (String) args[1];
				boolean added = Plugin.getInstance().mPOIManager.completePOI(player, poiName);
				if (added && player != null) {
					player.sendMessage(Component.text("You've conquered " + POI.getPOI(poiName).getCleanName() + " for the first time this week, earning you bonus loot!", NamedTextColor.GOLD));
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
				List<POICompletion> poiList = Plugin.getInstance().mPOIManager.mPlayerPOI.get(player.getUniqueId());
				if (poiList != null && player != null) {
					player.sendMessage(Component.text("Uncleared Points of Interest are displayed below in red. Your first clear of each this week will earn you bonus loot!", NamedTextColor.GOLD));
					for (POICompletion p : poiList) {
						if (p.getPOI().getName().equals("none")) {
							continue;
						}
						if (p.isCompleted()) {
							player.sendMessage(Component.text(" - " + p.getPOI().getCleanName(), NamedTextColor.GREEN));
						} else {
							player.sendMessage(Component.text(" - " + p.getPOI().getCleanName(), NamedTextColor.RED));
						}
					}
				}
			})
			.register();
	}
}
