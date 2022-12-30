package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LootTableArgument;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Nameable;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.jetbrains.annotations.Nullable;

public class ScanChests {

	private static final String COMMAND = "scanchests";

	private static final Comparator<Location> LOCATION_COMPARATOR = Comparator.comparing(Location::getBlockX).thenComparing(Location::getBlockZ).thenComparing(Location::getBlockY);

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.command.scanchests")

			// scan
			.withSubcommand(
				new CommandAPICommand("scan")
					.executesPlayer((PlayerCommandExecutor) (sender, args) -> scan(sender, false, false)))
			.withSubcommand(
				new CommandAPICommand("scan")
					.withArguments(new BooleanArgument("include empty non-chests"),
						new BooleanArgument("include filled non-chests"))
					.executesPlayer((PlayerCommandExecutor) (sender, args) -> scan(sender, (boolean) args[0], (boolean) args[1])))

			// clear seeds
			.withSubcommand(
				new CommandAPICommand("clearseeds")
					.executesPlayer((PlayerCommandExecutor) (sender, args) -> clearSeeds(sender)))


			// clear names
			.withSubcommand(
				new CommandAPICommand("clearnames")
					.executesPlayer((PlayerCommandExecutor) (sender, args) -> clearNames(sender, null)))
			.withSubcommand(
				new CommandAPICommand("clearnames")
					.withArguments(new GreedyStringArgument("name match"))
					.executesPlayer((PlayerCommandExecutor) (sender, args) -> clearNames(sender, (String) args[0])))

			// scan loot tables
			.withSubcommand(
				new CommandAPICommand("scanLootTables")
					.executesPlayer((PlayerCommandExecutor) (sender, args) -> scanLootTables(sender)))
			.withSubcommand(
				new CommandAPICommand("scanLootTables")
					.withArguments(new LootTableArgument("loot table"))
					.executesPlayer((PlayerCommandExecutor) (sender, args) -> scanLootTables(sender, (LootTable) args[0])))

			// replace loot tables
			.withSubcommand(
				new CommandAPICommand("replaceLootTables")
					.withArguments(new LootTableArgument("from"),
						new LootTableArgument("to"))
					.executesPlayer((PlayerCommandExecutor) (sender, args) -> replaceLootTables(sender, (LootTable) args[0], (LootTable) args[1])))

			.register();

	}

	private static void execute(Player player, Consumer<BlockState> action) {
		int range = player.getWorld().getViewDistance();
		int startX = player.getLocation().getChunk().getX() - range;
		int endX = startX + 2 * range;
		int startZ = player.getLocation().getChunk().getZ() - range;
		int endZ = startZ + 2 * range;
		int skippedChunks = 0;
		for (int x = startX; x <= endX; x++) {
			for (int z = startZ; z <= endZ; z++) {
				if (!player.getWorld().isChunkLoaded(x, z)) {
					skippedChunks++;
					continue;
				}
				Chunk chunk = player.getWorld().getChunkAt(x, z);
				for (BlockState tileEntity : chunk.getTileEntities(true)) {
					action.accept(tileEntity);
				}
			}
		}
		if (skippedChunks > 0) {
			player.sendMessage(Component.text(skippedChunks + " chunks have been skipped because they are not loaded", NamedTextColor.RED));
		}
	}

	private static void scan(Player player, boolean includeEmptyNonChests, boolean includeFilledNonChests) {
		AtomicInteger lootables = new AtomicInteger(0);
		AtomicInteger emptyNonChests = new AtomicInteger(0);
		AtomicInteger filledNonChests = new AtomicInteger(0);
		Set<Location> missingLootTables = new TreeSet<>(LOCATION_COMPARATOR);
		Set<Location> lootTableSeedSet = new TreeSet<>(LOCATION_COMPARATOR);
		Set<Location> filledChests = new TreeSet<>(LOCATION_COMPARATOR);
		execute(player, tileEntity -> {
			if (tileEntity instanceof Lootable lootable) {
				lootables.incrementAndGet();
				if (!lootable.hasLootTable()) {
					if (lootable instanceof InventoryHolder inventoryHolder
						    && !inventoryHolder.getInventory().isEmpty()) {
						if (includeFilledNonChests || lootable instanceof Chest) {
							filledChests.add(tileEntity.getLocation());
						} else {
							filledNonChests.incrementAndGet();
						}
						return;
					}
					if (includeEmptyNonChests || lootable instanceof Chest) {
						missingLootTables.add(tileEntity.getLocation());
					} else {
						emptyNonChests.incrementAndGet();
					}
				} else if (lootable.getSeed() != 0) {
					lootTableSeedSet.add(tileEntity.getLocation());
				}
			}
		});

		player.sendMessage(Component.text("Found ", NamedTextColor.GOLD).append(Component.text(lootables.get(), NamedTextColor.AQUA))
			                   .append(Component.text(" lootable blocks within " + player.getWorld().getViewDistance() + " chunks", NamedTextColor.GOLD)));

		if (!missingLootTables.isEmpty()) {
			player.sendMessage(Component.text("Found ", NamedTextColor.YELLOW).append(Component.text(missingLootTables.size(), NamedTextColor.AQUA))
				                   .append(Component.text(" empty " + (includeEmptyNonChests ? "lootable blocks" : "chests") + " without loot tables:", NamedTextColor.YELLOW)));
			sendLocations(player, missingLootTables, loc -> "");
		}
		if (emptyNonChests.get() > 0) {
			player.sendMessage(Component.text("Found ", NamedTextColor.GRAY).append(Component.text(emptyNonChests.get(), NamedTextColor.AQUA))
				                   .append(Component.text(" empty non-chest lootable blocks without loot tables. ", NamedTextColor.GRAY))
				                   .append(Component.text("Click to include these too").decorate(TextDecoration.UNDERLINED)
					                           .clickEvent(ClickEvent.runCommand("/" + COMMAND + " scan true false"))));
		}

		if (!lootTableSeedSet.isEmpty()) {
			player.sendMessage(Component.text("Found ", NamedTextColor.YELLOW).append(Component.text(lootTableSeedSet.size(), NamedTextColor.AQUA))
				                   .append(Component.text(" lootable blocks with a loot table seed set:", NamedTextColor.YELLOW)));
			sendLocations(player, lootTableSeedSet, loc -> "" + ((Lootable) loc.getBlock().getState()).getSeed());
			player.sendMessage(Component.text("Click to clear seeds").decorate(TextDecoration.UNDERLINED)
				                   .clickEvent(ClickEvent.runCommand("/" + COMMAND + " clearseeds")));
		}

		if (!filledChests.isEmpty()) {
			player.sendMessage(Component.text("Found ", NamedTextColor.YELLOW).append(Component.text(filledChests.size(), NamedTextColor.AQUA))
				                   .append(Component.text(" filled " + (includeFilledNonChests ? "lootable blocks" : "chests") + ":", NamedTextColor.YELLOW)));
			sendLocations(player, filledChests, loc -> {
				String result = loc.getBlock().getType() + " with:\n";
				Inventory inventory = ((InventoryHolder) loc.getBlock().getState()).getInventory();
				result += Arrays.stream(inventory.getContents())
					          .filter(Objects::nonNull)
					          .collect(Collectors.groupingBy(i -> {
						          ItemStack clone = i.clone();
						          clone.setAmount(1);
						          return clone;
					          }))
					          .entrySet().stream()
					          .map(e -> e.getValue().stream().mapToInt(ItemStack::getAmount).sum()
						                    + " " + e.getKey().getType() + (e.getKey().getItemMeta() != null && e.getKey().getItemMeta().hasDisplayName()
							                                                    ? "[" + e.getKey().getItemMeta().getDisplayName() + ChatColor.RESET + "]" : ""))
					          .collect(Collectors.joining("\n"));
				return result;
			});
		}
		if (filledNonChests.get() > 0) {
			player.sendMessage(Component.text("Found ", NamedTextColor.GRAY).append(Component.text(filledNonChests.get(), NamedTextColor.AQUA))
				                   .append(Component.text(" filled non-chest lootable blocks. ", NamedTextColor.GRAY))
				                   .append(Component.text("Click to include these too").decorate(TextDecoration.UNDERLINED)
					                           .clickEvent(ClickEvent.runCommand("/" + COMMAND + " scan false true"))));
		}
	}

	private static void sendLocations(Player player, Collection<Location> locations, Function<Location, String> hover) {
		int i = 0;
		Component line = null;
		for (Location l : locations) {
			Component c = Component.text(l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ())
				              .hoverEvent(HoverEvent.showText(Component.text(("Click to teleport\n" + hover.apply(l)).trim())))
				              .clickEvent(ClickEvent.runCommand("/tp @s " + l.getBlockX() + " " + (l.getBlockY() + 1) + " " + l.getBlockZ() + " 0 90"));
			if (line == null) {
				line = Component.text("").append(c);
				i = 1;
			} else {
				line = line.append(Component.text(" | ", NamedTextColor.GRAY)).append(c);
				i++;
				if (i >= 3) {
					player.sendMessage(line);
					line = null;
					i = 0;
				}
			}
		}
		if (line != null) {
			player.sendMessage(line);
		}
	}

	private static void clearSeeds(Player player) {
		AtomicInteger fixed = new AtomicInteger(0);
		execute(player, tileEntity -> {
			if (tileEntity instanceof Lootable lootable && lootable.getSeed() != 0) {
				lootable.setSeed(0);
				tileEntity.update();
				fixed.incrementAndGet();
			}
		});
		player.sendMessage(Component.text("Cleared loot table seeds in ", NamedTextColor.GOLD).append(Component.text(fixed.get(), NamedTextColor.AQUA))
			                   .append(Component.text(" lootable blocks", NamedTextColor.GOLD)));
	}

	private static void clearNames(Player player, @Nullable String matchingName) {
		AtomicInteger fixed = new AtomicInteger(0);
		execute(player, tileEntity -> {
			if (tileEntity instanceof Lootable lootable
				    && tileEntity instanceof Nameable nameable
				    && nameable.customName() != null
				    && (matchingName == null || matchingName.equals(MessagingUtils.plainText(nameable.customName())))) {
				nameable.customName(null);
				tileEntity.update();
				fixed.incrementAndGet();
			}
		});
		player.sendMessage(Component.text("Cleared names of ", NamedTextColor.GOLD).append(Component.text(fixed.get(), NamedTextColor.AQUA))
			                   .append(Component.text(" lootable blocks", NamedTextColor.GOLD)));
	}

	private static void scanLootTables(Player player) {
		Map<NamespacedKey, Integer> lootTables = new HashMap<>();
		execute(player, tileEntity -> {
			if (tileEntity instanceof Lootable lootable
				    && lootable.hasLootTable()) {
				lootTables.compute(lootable.getLootTable().getKey(), (k, v) -> v == null ? 1 : v + 1);
			}
		});
		player.sendMessage(Component.text("Found ", NamedTextColor.GOLD).append(Component.text(lootTables.size(), NamedTextColor.AQUA))
			                   .append(Component.text(" different loot tables within " + player.getWorld().getViewDistance() + " chunks:", NamedTextColor.GOLD)));
		lootTables.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> player.sendMessage(Component.text(" - ", NamedTextColor.WHITE)
				                                     .hoverEvent(HoverEvent.showText(Component.text("Click to list blocks with this loot table")))
				                                     .clickEvent(ClickEvent.runCommand("/" + COMMAND + " scanLootTables " + entry.getKey()))
				                                     .append(Component.text(entry.getValue() + "", NamedTextColor.AQUA))
				                                     .append(Component.text("x " + entry.getKey(), NamedTextColor.WHITE))));
	}

	private static void scanLootTables(Player player, LootTable lootTable) {
		Set<Location> locations = new TreeSet<>(LOCATION_COMPARATOR);
		execute(player, tileEntity -> {
			if (tileEntity instanceof Lootable lootable
				    && lootable.hasLootTable()
				    && lootable.getLootTable().getKey().equals(lootTable.getKey())) {
				locations.add(tileEntity.getLocation());
			}
		});
		player.sendMessage(Component.text("Found ", NamedTextColor.GOLD).append(Component.text(locations.size(), NamedTextColor.AQUA))
			                   .append(Component.text(" lootable blocks within " + player.getWorld().getViewDistance() + " chunks with the loot table ", NamedTextColor.GOLD))
			                   .append(Component.text("" + lootTable.getKey(), NamedTextColor.AQUA))
			                   .append(Component.text(":", NamedTextColor.GOLD)));
		sendLocations(player, locations, loc -> "");
	}

	private static void replaceLootTables(Player player, LootTable from, LootTable to) {
		AtomicInteger replaced = new AtomicInteger(0);
		execute(player, tileEntity -> {
			if (tileEntity instanceof Lootable lootable
				    && lootable.hasLootTable()
				    && lootable.getLootTable().getKey().equals(from.getKey())) {
				lootable.setLootTable(to);
				tileEntity.update();
				replaced.incrementAndGet();
			}
		});
		player.sendMessage(Component.text("Replaced loot tables in ", NamedTextColor.GOLD).append(Component.text(replaced.get(), NamedTextColor.AQUA))
			                   .append(Component.text(" lootable blocks", NamedTextColor.GOLD)));
	}

}
