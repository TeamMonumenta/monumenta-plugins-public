package com.playmonumenta.plugins.cosmetics;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.GenericCommand;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CosmeticsCommand extends GenericCommand {

	public static void register(Plugin plugin) {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.cosmetics");
		String[] types = Arrays.stream(CosmeticType.values()).map(CosmeticType::getType).toArray(String[]::new);

		// ADD COSMETIC COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("add"),
				new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER),
				new MultiLiteralArgument(types),
				new GreedyStringArgument("name"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));
				String name = (String) args[2];
				boolean added = CosmeticsManager.getInstance().addCosmetic(player, type, name);
				if (added) {
					sender.sendMessage(Component.text("Added " + type.getDisplayName() + " '" + name + "' to " + player.getName(), NamedTextColor.WHITE));
				} else {
					sender.sendMessage(Component.text(player.getName() + " already has " + type.getDisplayName() + " '" + name + "'", NamedTextColor.RED));
				}
			})
			.register();

		// REMOVE COSMETIC COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("remove"),
				new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER),
				new MultiLiteralArgument(types),
				new GreedyStringArgument("name").replaceSuggestions(
					info -> CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical((Player) info.previousArgs()[0], CosmeticType.valueOf(((String) info.previousArgs()[1]).toUpperCase(Locale.ROOT))).stream()
						.map(Cosmetic::getName)
						.filter(n -> n.startsWith(info.currentArg()))
						.toArray(String[]::new)))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));
				String name = (String) args[2];
				boolean removed = CosmeticsManager.getInstance().removeCosmetic(player, type, name);
				if (removed) {
					sender.sendMessage(Component.text("Removed " + type.getDisplayName() + " '" + name + "' from " + player.getName(), NamedTextColor.WHITE));
				} else {
					sender.sendMessage(Component.text(player.getName() + " does not have " + type.getDisplayName() + " '" + name + "'", NamedTextColor.RED));
				}
			})
			.register();

		// CLEAR COSMETICS COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("clear"),
				new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER),
				new MultiLiteralArgument(types))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));

				CosmeticsManager.getInstance().clearCosmetics(player, type);
				sender.sendMessage(Component.text("Removed all " + type.getDisplayName() + "s from " + player.getName(), NamedTextColor.RED));
			}).register();

		// GET COSMETICS COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("get"),
				new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				for (CosmeticType type : CosmeticType.values()) {
					listCosmetics(player, type, sender);
				}
			}).register();
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("get"),
				new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER),
				new MultiLiteralArgument(types))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));
				listCosmetics(player, type, sender);
			}).register();

		//OPEN GUI COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("gui"),
				new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new CosmeticsGUI(plugin, player).openInventory(player, plugin);
			}).register();
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("gui"))
			.executesPlayer((player, args) -> {
				new CosmeticsGUI(plugin, player).openInventory(player, plugin);
			}).register();
	}

	private static void listCosmetics(Player player, CosmeticType type, CommandSender sender) {
		List<Cosmetic> cosmetics = CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical(player, type);
		sender.sendMessage(Component.text(type.getDisplayName() + ":", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		if (cosmetics.isEmpty()) {
			sender.sendMessage(Component.text(" - none -", NamedTextColor.GRAY));
		} else {
			int lengthLimit = 1000;
			int length = 0;
			Component line = Component.empty();
			for (int i = 0; i < cosmetics.size(); i++) {
				Cosmetic c = cosmetics.get(i);
				String name = c.getName();
				if (length != 0 && name.length() + length > lengthLimit) {
					sender.sendMessage(line);
					length = 0;
					line = Component.empty();
				}
				if (name.length() <= lengthLimit) {
					line = line.append(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
					length += name.length();
				} else {
					line = line.append(Component.text(name.substring(0, lengthLimit), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
						.append(Component.text("...", NamedTextColor.GRAY));
					length += lengthLimit + 3;
				}
				if (i != cosmetics.size() - 1) {
					line = line.append(Component.text(", ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
					length += 3;
				}
			}
			if (length != 0) {
				sender.sendMessage(line);
			}
		}
	}
}
