package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.listeners.LootTableManager;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MasterworkUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;

public class WhatTableCommand {


	private static final String COMMAND = "findtable";

	private static final String PERMISSION = "monumenta.commands.whattable";

	public static void register() {


		// FIXME: an O(n) solution like this is way too slow, but it still works. Potentially fixable by augmenting LootTableManager with a b-tree to store namespaces
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withOptionalArguments(new GreedyStringArgument("item name"))
			.executes((sender, args) -> {
				if (!(sender instanceof Player p)) {
					throw new RuntimeException("This command only runs from players");
				}

				args.getOptionalByClass("item name", String.class)
					.ifPresentOrElse(query -> searchForItemByName(query, p.getLocation()).whenCompleteAsync((namespaceKey, error) -> {
						if (null != error) {
							throw new RuntimeException(error);
						}
						namespaceKey.ifPresentOrElse(keys -> {
							p.sendMessage("Your query returned " + keys.size() + " response" + (1 == keys.size() ? ":" : "s:"));
							buildOutputMessage(keys.stream().map(NamespacedKey::asString).toList()).forEach(p::sendMessage);
						}, () -> p.sendMessage(Component.text("(!) Could not find your item in the datapack.", NamedTextColor.RED)));
					}), () -> {
						final ItemStack item = Objects.requireNonNull(p.getInventory().getItemInMainHand());
						if (item.getType() == Material.AIR) {
							throw new RuntimeException("This command requires you to hold an item.");
						}
						if (!ItemUtils.isInteresting(item)) {
							p.sendMessage(Component.text("This item is not interesting enough.", NamedTextColor.RED));
							return;
						}
						if (MasterworkUtils.isMasterwork(item)) {
							p.sendMessage(Component.text("Your ", NamedTextColor.GREEN)
								.append(item.displayName())
								.append(Component.text(" lives in multiple places: ", NamedTextColor.GREEN)));

							buildOutputMessage(MasterworkUtils.getAllMasterworks(item, p).stream().map(MasterworkUtils::getItemPath).toList()).forEach(p::sendMessage);
							return;
						}
						// ItemUtils.getLootTable does not work here
						searchForItemByName(ItemUtils.getPlainName(item), p.getLocation())
							.whenComplete((namespacedKey, throwable) -> namespacedKey.ifPresentOrElse(keys -> {
								p.sendMessage(Component.text("Your ", NamedTextColor.GREEN)
									.append(item.displayName())
									.append(Component.text(" lives in ", NamedTextColor.GREEN))
									.append(Component.text(keys.size() > 1 ? "multiple locations: " : keys.get(0).asString(), NamedTextColor.WHITE)));
								if (keys.size() == 1) { // it shouldn't exist in multiple locations tho. we're searching one item.
									final var key = keys.get(0);
									p.sendMessage(Component.text("Click here to copy the path to your clipboard.", NamedTextColor.WHITE).clickEvent(ClickEvent.copyToClipboard(key.asString())));
									p.sendMessage(Component.text("Click here to run the loottable.", NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/loot give @s loot " + key.asString())));
								} else {
									buildOutputMessage(keys.stream().map(NamespacedKey::asString).toList()).forEach(p::sendMessage);
								}
							}, () -> p.sendMessage(Component.text("(!) Could not find your item in the datapack.", NamedTextColor.RED))));
					});
			})
			.register();
	}

	private static List<TextComponent> buildOutputMessage(List<String> paths) {
		return paths.stream()
			.map(x -> Component.text(x)
				.append(Component.text(" [Copy] ", NamedTextColor.GREEN).clickEvent(ClickEvent.copyToClipboard(x)))
				.append(Component.text("[Give]", NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/loot give @s loot " + x))))
			.toList();
	}

	private static CompletableFuture<Optional<List<NamespacedKey>>> searchForItemByName(String name, Location location) {
		return new CompletableFuture<Optional<List<NamespacedKey>>>().completeAsync(() -> {
			final var ret = Optional.of(LootTableManager.INSTANCE.getTables().keySet().stream().filter(namespacedKey -> Optional.ofNullable(Bukkit.getServer().getLootTable(namespacedKey))
				.map(x -> x.populateLoot(null, new LootContext.Builder(location).build()))
				.map(x ->
					x.stream()
						.filter(ItemStack::hasItemMeta)
						.anyMatch(item -> ItemUtils.getRawDisplayNameAsString(item).toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
				).orElse(false)).toList());
			return !ret.get().isEmpty() ? ret : Optional.empty();
		});
	}
}
