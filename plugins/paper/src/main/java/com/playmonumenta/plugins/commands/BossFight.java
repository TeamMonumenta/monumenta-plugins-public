package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BossFight {
	static final String COMMAND = "bossfight";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.bossfight");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OneEntity("entity"));
		arguments.add(new StringArgument("boss_tag").replaceSuggestions(ArgumentSuggestions.strings(BossManager.listBosses())));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.withOptionalArguments(new LocationArgument("redstone_pos"))
			.executes(BossFight::execute)
			.register();
	}

	private static void execute(CommandSender sender, CommandArguments args) {
		Entity entity = args.getUnchecked("entity");
		if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
			String tag = args.getUnchecked("boss_tag");
			Location loc = args.getUnchecked("redstone_pos");
			try {
				if (loc != null) {
					BossManager.createBoss(sender, le, tag, loc);
				} else {
					BossManager.createBoss(sender, le, tag);
				}
			} catch (Exception ex) {
				MessagingUtils.sendStackTrace(sender, ex);
			}
		} else {
			sender.sendMessage(Component.text("This command must be on a LivingEntity!", NamedTextColor.RED));
		}
	}
}
