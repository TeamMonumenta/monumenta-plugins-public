package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Arrays;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ParticlesCommand {

	public static void register() {

		new CommandAPICommand("particles")
			.withArguments(
				new MultiLiteralArgument(Arrays.stream(ParticleCategory.values())
					                         .filter(cat -> cat.mObjectiveName != null)
					                         .map(cat -> cat.name().toLowerCase(Locale.ROOT))
					                         .toArray(String[]::new)),
				new IntegerArgument("multiplier")
			)
			.executesPlayer((player, args) -> {
				ParticleCategory category;
				try {
					category = ParticleCategory.valueOf(((String) args[0]).toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException e) {
					throw CommandAPI.failWithString("Invalid particle category " + args[0]);
				}
				if (category.mObjectiveName == null) {
					throw CommandAPI.failWithString("Invalid particle category " + args[0]);
				}
				int multiplier = (int) args[1];

				multiplier = Math.max(0, Math.min(multiplier, PlayerData.MAX_PARTIAL_PARTICLE_VALUE));
				ScoreboardUtils.setScoreboardValue(player, category.mObjectiveName, multiplier);
				player.sendMessage(Component.text("Your particle multiplier for ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					                   .append(Component.text(category.mDisplayName, NamedTextColor.WHITE))
					                   .append(Component.text(" is now ", NamedTextColor.GRAY))
					                   .append(Component.text(multiplier, NamedTextColor.WHITE)));

			})
			.register();

	}

}
