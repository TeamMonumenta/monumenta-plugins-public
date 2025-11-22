package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.punches.PlayerPunches;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static com.playmonumenta.plugins.cosmetics.CosmeticsManager.getInstance;

public class RemotePunchCommand {
	private static final String COMMAND = "punch";
	private static final String PERMISSION = "monumenta.commands.remotepunch";
	private static final EntitySelectorArgument.OnePlayer VICTIM_ARGUMENT = new EntitySelectorArgument.OnePlayer("victim");

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(VICTIM_ARGUMENT)
			.executesPlayer((bully, args) -> {
				if (PlayerPunches.isOnWhitelistedShard()) {
					Cosmetic activeCosmetic = getInstance().getRandomActiveCosmetic(bully, CosmeticType.PLAYER_PUNCH);
					if (bully.hasPermission("monumenta.cosmetics.punchoptout")) {
						bully.sendMessage(Component.text("You are currently opted out of Player Punches!", NamedTextColor.RED));
					} else if (activeCosmetic != null) {
						PlayerPunches.activatePunch(bully, args.getByArgument(VICTIM_ARGUMENT), activeCosmetic.getName(), true);
					} else {
						bully.sendMessage(Component.text("You must have a punch cosmetic equipped!", NamedTextColor.RED));
					}
				} else {
					bully.sendMessage(Component.text("You cannot remotely punch players on this shard!", NamedTextColor.RED));
				}
			})

			.register();
	}
}
