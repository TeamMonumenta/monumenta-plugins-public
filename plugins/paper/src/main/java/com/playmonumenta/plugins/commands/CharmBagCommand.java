package com.playmonumenta.plugins.commands;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.playmonumenta.plugins.guis.CharmBagGui;
import com.playmonumenta.plugins.inventories.CharmBag;
import com.playmonumenta.plugins.inventories.CharmBagManager;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CharmBagCommand {

	public static void register() {
		new CommandAPICommand("charmbag")
			.withSubcommand(
				new CommandAPICommand("open")
					.withPermission("monumenta.command.opencharmbag")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player viewer = CommandUtils.getPlayerFromSender(sender);
						Player viewee = Objects.requireNonNull(args.getUnchecked("player"));
						new CharmBagGui(viewer, CharmBagManager.getCharmBag(viewee), CharmBagManager.MAX_SETTINGS,
							Component.text("Charm Bag of ", NamedTextColor.GOLD).append(viewee.displayName())).open();
					})
			).withSubcommand(
				new CommandAPICommand("withdraw")
					.withArguments(new GreedyStringArgument("items")
						.replaceSuggestions((info, builder) -> {
							AtomicReference<SuggestionsBuilder> builderRef = new AtomicReference<>(builder);
							if (info.sender() instanceof Player player) {
								parseCharmBagCommandItems(player, info.currentArg(), info.currentInput().length() - info.currentArg().length(), builderRef);
							}
							return Objects.requireNonNull(builderRef.get()).buildFuture();
						}))
					.executes((sender, args) -> {
						Player player = CommandUtils.getPlayerFromSender(sender);
						List<CharmBagCommandItem> items = parseCharmBagCommandItems(player, Objects.requireNonNull(args.getUnchecked("items")), 0, null);
						if (items == null) {
							throw CommandAPI.failWithString("Invalid items list, check format and spelling");
						}
						withdrawFromCharmBag(player, items);
					})
			)
			.register();
	}

	public static class CharmBagCommandItem {
		String mName;
		long mCount;

		public CharmBagCommandItem(String name, long count) {
			this.mName = name;
			this.mCount = count;
		}
	}

	private static @Nullable List<CharmBagCommandItem> parseCharmBagCommandItems(Player viewee, String arg, int offset, @Nullable AtomicReference<SuggestionsBuilder> suggestionsBuilder) {
		List<CharmBagCommandItem> result = new ArrayList<>();

		CharmBag charmBag = CharmBagManager.getCharmBag(viewee);
		List<String> charmBagItems = new ArrayList<>(charmBag.mItems.stream().map(item -> ItemUtils.getPlainNameIfExists(item.mItem)).distinct().sorted().toList());

		BiFunction<Integer, String, @Nullable List<CharmBagCommandItem>> fail = (index, suggestion) -> {
			if (suggestionsBuilder != null) {
				suggestionsBuilder.set(Objects.requireNonNull(suggestionsBuilder.get()).createOffset(offset + index).suggest(suggestion));
			}
			return null;
		};

		int i = 0;
		while (i < arg.length()) {
			int start = i;
			while (i < arg.length() && Character.isDigit(arg.charAt(i))) {
				i++;
			}
			if (i == start) {
				return fail.apply(start, "1");
			}
			if (i - start > 3) {
				return fail.apply(start, "999");
			}
			int count = Integer.parseInt(arg.substring(start, i));
			if (i >= arg.length() || arg.charAt(i) != ' ') {
				return fail.apply(i, " ");
			}
			i++;
			int end = arg.indexOf(',', i);
			if (end < 0) {
				end = arg.length();
			}
			String name = arg.substring(i, end);
			if (charmBagItems.stream().noneMatch(item -> item.equalsIgnoreCase(name))) {
				if (suggestionsBuilder != null) {
					SuggestionsBuilder builder = Objects.requireNonNull(suggestionsBuilder.get()).createOffset(offset + i);
					String nameLowercase = name.toLowerCase(Locale.ROOT);
					for (String item : charmBagItems) {
						if (item.toLowerCase(Locale.ROOT).startsWith(nameLowercase)) {
							builder.suggest(item);
						}
					}
					suggestionsBuilder.set(builder);
				}
				return null;
			}
			result.add(new CharmBagCommandItem(name, count));
			i = end + 1; // skip comma
			while (i < arg.length() && arg.charAt(i) == ' ') {
				i++;
			}
		}
		return result;
	}

	public static void withdrawFromCharmBag(Player player, List<CharmBagCommandItem> items) throws WrapperCommandSyntaxException {
		ItemStack charmBagItemStack = CharmBagManager.playerGetCharmBagItem(player);
		if (charmBagItemStack == null) {
			throw CommandAPI.failWithString("You're not carrying a charm bag.");
		}
		String charmBagName = ItemUtils.getPlainNameOrDefault(charmBagItemStack);
		boolean retrievedAll = true;
		CharmBag charmBag = CharmBagManager.getCharmBag(player);
		for (CharmBagCommandItem item : items) {
			ItemStack removed = null;
			Optional<CharmBag.CharmBagItem> charmBagItemOpt = charmBag.mItems.stream().filter(wi -> ItemUtils.getPlainNameOrDefault(wi.mItem).equalsIgnoreCase(item.mName)).findFirst();
			if (charmBagItemOpt.isPresent()) {
				CharmBag.CharmBagItem charmBagItem = charmBagItemOpt.get();
				removed = ItemUtils.clone(charmBagItem.mItem);
				removed.setAmount((int) Math.min(charmBagItem.mAmount, item.mCount));
			}
			if (removed != null && removed.getAmount() > 0) {
				if (removed.getAmount() < item.mCount) {
					player.sendMessage(Component.text("Not enough '" + item.mName + "' in your " + charmBagName + ", will only retrieve " + removed.getAmount(), TextColor.color(255, 128, 0)));
					retrievedAll = false;
				}

				charmBag.remove(player, ItemUtils.clone(removed));
				long remaining = charmBag.count(removed);
				InventoryUtils.giveItem(player, removed);

				player.sendMessage(Component.text("Withdrew ", NamedTextColor.GREEN).append(
					Component.text(removed.getAmount() + " " + ItemUtils.getPlainNameOrDefault(removed), NamedTextColor.WHITE).append(
						Component.text(" from your charm bag. ", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)).append(
						Component.text("(Remaining in charm bag: " + remaining + ")", NamedTextColor.GRAY)
					)));
			} else {
				player.sendMessage(Component.text("You do not have any '" + item.mName + "' in your " + charmBagName + "!", NamedTextColor.RED));
				retrievedAll = false;
			}
		}
		if (!retrievedAll && items.size() > 1) {
			player.sendMessage(Component.text("Could not retrieve all desired items!", NamedTextColor.RED)
				.append(Component.text(" You may have to scroll up for more information.", NamedTextColor.WHITE)));
		}
	}


}
