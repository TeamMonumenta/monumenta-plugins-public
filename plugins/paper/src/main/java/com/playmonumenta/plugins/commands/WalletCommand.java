package com.playmonumenta.plugins.commands;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.playmonumenta.plugins.guis.WalletGui;
import com.playmonumenta.plugins.inventories.BaseWallet;
import com.playmonumenta.plugins.inventories.SharedVaultManager;
import com.playmonumenta.plugins.inventories.Wallet;
import com.playmonumenta.plugins.inventories.WalletBlock;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
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
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class WalletCommand {

	public static void register() {
		new CommandAPICommand("wallet")
			.withSubcommand(
				new CommandAPICommand("openvault")
					.withPermission("monumenta.command.openwallet")
					.withArguments(new LocationArgument("vault location", LocationType.BLOCK_POSITION))
					.executes((sender, args) -> {
						Player viewer = CommandUtils.getPlayerFromSender(sender);
						Location loc = Objects.requireNonNull(args.getUnchecked("vault location"));
						BlockState blockState = loc.getBlock().getState();
						WalletBlock walletBlock = SharedVaultManager.getOrRegisterWallet(blockState);
						if (walletBlock == null) {
							viewer.sendMessage(Component.text("Could not find a vault at that location", NamedTextColor.RED));
							return;
						}
						new WalletGui(
							viewer,
							walletBlock,
							WalletManager.MAX_SETTINGS,
							Component.text("Wallet of ", NamedTextColor.GOLD).append(Component.text(SharedVaultManager.SHARED_VAULT_NAME, NamedTextColor.DARK_GREEN)),
							true
						).open();
					})
			)
			.withSubcommand(
				new CommandAPICommand("open")
					.withPermission("monumenta.command.openwallet")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player viewer = CommandUtils.getPlayerFromSender(sender);
						Player viewee = Objects.requireNonNull(args.getUnchecked("player"));
						new WalletGui(
							viewer,
							WalletManager.getWallet(viewee),
							WalletManager.MAX_SETTINGS,
							Component.text("Wallet of ", NamedTextColor.GOLD).append(viewee.displayName()),
							true
						).open();
					})
			).withSubcommand(
				new CommandAPICommand("withdraw")
					.withArguments(new GreedyStringArgument("items")
						.replaceSuggestions((info, builder) -> {
							AtomicReference<SuggestionsBuilder> builderRef = new AtomicReference<>(builder);
							if (info.sender() instanceof Player player) {
								parseWalletCommandItems(player, info.currentArg(), info.currentInput().length() - info.currentArg().length(), builderRef);
							}
							return Objects.requireNonNull(builderRef.get()).buildFuture();
						}))
					.executes((sender, args) -> {
						Player player = CommandUtils.getPlayerFromSender(sender);
						List<WalletCommandItem> items = parseWalletCommandItems(player, Objects.requireNonNull(args.getUnchecked("items")), 0, null);
						if (items == null) {
							throw CommandAPI.failWithString("Invalid items list, check format and spelling");
						}
						withdrawFromWallet(player, items);
					})
			)
			.register();
	}

	public static class WalletCommandItem {
		String mName;
		long mCount;

		public WalletCommandItem(String name, long count) {
			this.mName = name;
			this.mCount = count;
		}
	}

	private static @Nullable List<WalletCommandItem> parseWalletCommandItems(Player viewee, String arg, int offset, @Nullable AtomicReference<SuggestionsBuilder> suggestionsBuilder) {
		List<WalletCommandItem> result = new ArrayList<>();

		Wallet wallet = WalletManager.getWallet(viewee);
		List<String> walletItems = new ArrayList<>(wallet.mItems.stream().map(item -> ItemUtils.getPlainNameIfExists(item.mItem)).distinct().sorted().toList());
		for (WalletManager.CompressionInfo compressionInfo : WalletManager.COMPRESSIBLE_CURRENCIES) {
			if (wallet.mItems.stream().anyMatch(item -> item.mItem.isSimilar(compressionInfo.mBase))) {
				walletItems.add(ItemUtils.getPlainNameIfExists(compressionInfo.mCompressed));
			}
		}

		BiFunction<Integer, String, @Nullable List<WalletCommandItem>> fail = (index, suggestion) -> {
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
			if (walletItems.stream().noneMatch(item -> item.equalsIgnoreCase(name))) {
				if (suggestionsBuilder != null) {
					SuggestionsBuilder builder = Objects.requireNonNull(suggestionsBuilder.get()).createOffset(offset + i);
					String nameLowercase = name.toLowerCase(Locale.ROOT);
					for (String item : walletItems) {
						if (item.toLowerCase(Locale.ROOT).startsWith(nameLowercase)) {
							builder.suggest(item);
						}
					}
					suggestionsBuilder.set(builder);
				}
				return null;
			}
			result.add(new WalletCommandItem(name, count));
			i = end + 1; // skip comma
			while (i < arg.length() && arg.charAt(i) == ' ') {
				i++;
			}
		}
		return result;
	}

	public static void withdrawFromWallet(Player player, List<WalletCommandItem> items) throws WrapperCommandSyntaxException {
		ItemStack walletItemStack = WalletManager.playerGetWalletItem(player);
		if (walletItemStack == null) {
			throw CommandAPI.failWithString("You're not carrying a wallet.");
		}
		String walletName = ItemUtils.getPlainNameOrDefault(walletItemStack);
		boolean retrievedAll = true;
		Wallet wallet = WalletManager.getWallet(player);
		for (WalletCommandItem item : items) {
			ItemStack removed = null;
			Optional<WalletManager.CompressionInfo> compressionInfo = WalletManager.COMPRESSIBLE_CURRENCIES.stream().filter(ci -> ItemUtils.getPlainNameOrDefault(ci.mCompressed).equalsIgnoreCase(item.mName)).findFirst();
			if (compressionInfo.isPresent()) {
				long baseItemCount = wallet.count(compressionInfo.get().mBase);
				if (baseItemCount >= compressionInfo.get().mAmount) {
					removed = ItemUtils.clone(compressionInfo.get().mCompressed);
					removed.setAmount((int) Math.min(baseItemCount / compressionInfo.get().mAmount, item.mCount));
				}
			} else {
				Optional<BaseWallet.WalletItem> walletItemOpt = wallet.mItems.stream().filter(wi -> ItemUtils.getPlainNameOrDefault(wi.mItem).equalsIgnoreCase(item.mName)).findFirst();
				if (walletItemOpt.isPresent()) {
					BaseWallet.WalletItem walletItem = walletItemOpt.get();
					removed = ItemUtils.clone(walletItem.mItem);
					removed.setAmount((int) Math.min(walletItem.mAmount, item.mCount));
				}
			}
			if (removed != null && removed.getAmount() > 0) {
				if (removed.getAmount() < item.mCount) {
					player.sendMessage(Component.text("Not enough '" + item.mName + "' in your " + walletName + ", will only retrieve " + removed.getAmount(), TextColor.color(255, 128, 0)));
					retrievedAll = false;
				}

				wallet.remove(player, ItemUtils.clone(removed));
				long remaining = wallet.count(removed);
				int withdrawnAmount = removed.getAmount(); // Capture the correct amount of items being withdrawn to display to the player; the line below somehow modifies it and shows a wrong number
				InventoryUtils.giveItem(player, removed);

				player.sendMessage(Component.text("Withdrew ", NamedTextColor.GREEN).append(
					Component.text(withdrawnAmount + " " + ItemUtils.getPlainNameOrDefault(removed), NamedTextColor.WHITE).append(
						Component.text(" from your wallet. ", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)).append(
						Component.text("(Remaining in wallet: " + remaining + ")", NamedTextColor.GRAY)
					)));
			} else {
				player.sendMessage(Component.text("You do not have any '" + item.mName + "' in your " + walletName + "!", NamedTextColor.RED));
				retrievedAll = false;
			}
		}
		if (!retrievedAll && items.size() > 1) {
			player.sendMessage(Component.text("Could not retrieve all desired items!", NamedTextColor.RED)
				.append(Component.text(" You may have to scroll up for more information.", NamedTextColor.WHITE)));
		}
	}

}
