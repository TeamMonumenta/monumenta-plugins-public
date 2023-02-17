package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillSummary extends GenericCommand {
	private static final String COMMAND = "skillsummary";

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.skillsummary");

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.executes((sender, args) -> {
				tell(plugin, sender, false);
			})
			.register();

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new BooleanArgument("shorthand"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				tell(plugin, sender, (Boolean) args[0]);
			})
			.register();
	}

	private static void tell(Plugin plugin, CommandSender sender, boolean useShorthand) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		Component component = Component.text("")
		.append(Component.selector(player.getName())
			.color(NamedTextColor.AQUA))
		.append(Component.text("'s Skills:", NamedTextColor.GREEN));

		if (useShorthand) {
			for (Ability ability : plugin.mAbilityManager.getPlayerAbilities(player).getAbilitiesIgnoringSilence()) {
				if (ability == null) {
					continue;
				}
				Component abilityHover = ability.getLevelHover(useShorthand);
				if (abilityHover != null) {
					component = component.append(Component.text(" "))
						.append(abilityHover);
				}
			}
			player.sendMessage(component);
		} else {
			player.sendMessage(component);
			for (Ability ability : plugin.mAbilityManager.getPlayerAbilities(player).getAbilitiesIgnoringSilence()) {
				if (ability == null) {
					continue;
				}
				Component abilityHover = ability.getLevelHover(useShorthand);
				if (abilityHover != null) {
					player.sendMessage(abilityHover);
				}
			}
			if (!ServerProperties.getClassSpecializationsEnabled(player)) {
				for (AbilityInfo<?> disabledAbility : plugin.mAbilityManager.getDisabledSpecAbilities()) {
					if (disabledAbility.testCanUse(player)
						    && disabledAbility.getScoreboard() != null) {
						int score = ScoreboardUtils.getScoreboardValue(player, disabledAbility.getScoreboard()).orElse(0);
						if (score > 0) {
							Component abilityHover = disabledAbility.getLevelHover(player, score, useShorthand, false);
							if (abilityHover != null) {
								player.sendMessage(abilityHover.color(NamedTextColor.GRAY).append(Component.text(" (disabled in this region)", NamedTextColor.GRAY)));
							}
						}
					}
				}
			}
		}
	}
}

