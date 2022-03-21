package com.playmonumenta.plugins.cosmetics;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.GenericCommand;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

public class CosmeticsCommand extends GenericCommand {

	public static void register(Plugin plugin) {

		// ADD COSMETIC COMMAND
		CommandPermission perms = CommandPermission.fromString("monumenta.command.cosmetics");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("add"));
		arguments.add(new LiteralArgument("title"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new TextArgument("name"));

		new CommandAPICommand("cosmetics")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[0];
			CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.TITLE, (String) args[1]);
		})
		.register();

		// ADD FINISHER COMMAND
		arguments.clear();
		arguments.add(new LiteralArgument("add"));
		arguments.add(new LiteralArgument("finisher"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new TextArgument("name"));

		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.ELITE_FINISHER, (String) args[1]);
			})
			.register();

		// ADD PLOT BORDER COMMAND
		arguments.clear();
		arguments.add(new LiteralArgument("add"));
		arguments.add(new LiteralArgument("plotborder"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new TextArgument("name"));

		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.PLOT_BORDER, (String) args[1]);
			})
			.register();

		// REMOVE COSMETIC COMMAND
		arguments.clear();
		arguments.add(new LiteralArgument("remove"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new TextArgument("name"));

		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				CosmeticsManager.getInstance().removeCosmetic(player, (String) args[1]);
			})
			.register();

		// CLEAR COSMETICS COMMAND
		arguments.clear();
		arguments.add(new LiteralArgument("clear"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				CosmeticsManager.getInstance().clearCosmetics(player);
			}).register();

		// GET COSMETICS COMMAND
		arguments.clear();
		arguments.add(new LiteralArgument("get"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Player senderPlayer = null;
				List<Cosmetic> cosmetics = CosmeticsManager.getInstance().getCosmetics(player);
				if (sender instanceof Player) {
					senderPlayer = (Player) sender;
				}
				if (cosmetics != null && senderPlayer != null) {
					for (Cosmetic c : cosmetics) {
						senderPlayer.sendMessage("" + c.getType().getDisplayName() + " " + c.getName());
					}
				}
			}).register();

		//OPEN GUI COMMAND
		arguments.clear();
		arguments.add(new LiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new CosmeticsGUI(plugin, player).openInventory(player, plugin);
			}).register();
	}
}
