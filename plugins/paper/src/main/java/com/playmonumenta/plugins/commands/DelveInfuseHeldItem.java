package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.DelveInfusionUtils.DelveInfusionSelection;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.ArrayList;
import java.util.List;

public class DelveInfuseHeldItem extends GenericCommand {

	private static final String COMMAND = "delveinfusehelditem";


/*
* NOTICE!
* This command is only for test and mods use.
* Normal player should use the GUI for theirs infusions
*/
	public static void register() {
		CommandPermission perm = CommandPermission.fromString("monumenta.command.delveinfusehelditem");

		List<Argument> arguments = new ArrayList<>();

		Argument selectionArg = new MultiLiteralArgument(DelveInfusionSelection.PENNATE.getLabel(),
														DelveInfusionSelection.CARAPACE.getLabel(),
														DelveInfusionSelection.AURA.getLabel(),
														DelveInfusionSelection.EXPEDITE.getLabel(),
														DelveInfusionSelection.CHOLER.getLabel(),
														DelveInfusionSelection.USURPER.getLabel(),
														DelveInfusionSelection.EMPOWERED.getLabel(),
														DelveInfusionSelection.NUTRIMENT.getLabel(),
														DelveInfusionSelection.EXECUTION.getLabel(),
														DelveInfusionSelection.REFLECTION.getLabel(),
														DelveInfusionSelection.MITOSIS.getLabel(),
														DelveInfusionSelection.ARDOR.getLabel(),
														DelveInfusionSelection.EPOCH.getLabel(),
														DelveInfusionSelection.NATANT.getLabel(),
														DelveInfusionSelection.UNDERSTANDING.getLabel(),
														DelveInfusionSelection.REFUND.getLabel());
		arguments.add(selectionArg);

		new CommandAPICommand(COMMAND)
			.withPermission(perm)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				DelveInfusionSelection selection = DelveInfusionSelection.getInfusionSelection((String) args[0]);
				if (selection == null) {
					CommandAPI.fail("Invalid delve infusion selection");
				} else if (selection == DelveInfusionSelection.REFUND) {
					DelveInfusionUtils.refundInfusion(player.getInventory().getItemInMainHand(), player);
				} else {
					try {
						DelveInfusionUtils.infuseItem(player, player.getInventory().getItemInMainHand(), selection);
					} catch (Exception e) {
						CommandAPI.fail("Error: " + e.getMessage());
					}
				}
			})
			.register();
	}


}
