package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Nameable;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.loot.Lootable;

public class ScanChests {

	private static final String COMMAND = "scanchests";

	public static void register() {
		// scan
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.command.scanchests")
			.withArguments(new IntegerArgument("chunk range"))
			.executesPlayer((PlayerCommandExecutor) (sender, args) -> execute(sender, (int) args[0], false))
			.register();
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.command.scanchests")
			.withArguments(new IntegerArgument("chunk range"), new BooleanArgument("include empty non-chests"))
			.executesPlayer((PlayerCommandExecutor) (sender, args) -> execute(sender, (int) args[0], (boolean) args[1]))
			.register();

		// clear seeds
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.command.scanchests")
			.withArguments(new LiteralArgument("clearseeds"), new IntegerArgument("chunk range"))
			.executesPlayer((PlayerCommandExecutor) (sender, args) -> clearSeeds(sender, (int) args[0]))
			.register();

		// clear names
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.command.scanchests")
			.withArguments(new LiteralArgument("clearnames"), new IntegerArgument("chunk range"))
			.executesPlayer((PlayerCommandExecutor) (sender, args) -> clearNames(sender, (int) args[0], null))
			.register();
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.command.scanchests")
			.withArguments(new LiteralArgument("clearnames"), new IntegerArgument("chunk range"), new GreedyStringArgument("name match"))
			.executesPlayer((PlayerCommandExecutor) (sender, args) -> clearNames(sender, (int) args[0], (String) args[1]))
			.register();
	}

	private static void execute(Player player, int range, boolean includeEmptyNonChests) {
		if (range > 20) { // limit to 20 chunks range, or 1681 chunks tested at most
			range = 20;
			player.sendMessage(Component.text("Range has been limited to " + range));
		}
		int startX = player.getLocation().getChunk().getX() - range;
		int endX = startX + 2 * range;
		int startZ = player.getLocation().getChunk().getZ() - range;
		int endZ = startZ + 2 * range;
		int skippedChunks = 0;

		int lootables = 0;
		int emptyNonChests = 0;
		Comparator<Location> locationComparator = Comparator.comparing(Location::getBlockX).thenComparing(Location::getBlockZ).thenComparing(Location::getBlockY);
		Set<Location> missingLootTables = new TreeSet<>(locationComparator);
		Set<Location> lootTableSeedSet = new TreeSet<>(locationComparator);
		for (int x = startX; x <= endX; x++) {
			for (int z = startZ; z <= endZ; z++) {
				if (!player.getWorld().isChunkLoaded(x, z)) {
					skippedChunks++;
					continue;
				}
				Chunk chunk = player.getWorld().getChunkAt(x, z);
				for (BlockState tileEntity : chunk.getTileEntities(true)) {
					if (tileEntity instanceof Lootable lootable) {
						lootables++;
						if (!lootable.hasLootTable()) {
							if (tileEntity instanceof InventoryHolder inventoryHolder
								    && !inventoryHolder.getInventory().isEmpty()) {
								continue;
							}
							if (includeEmptyNonChests || tileEntity instanceof Chest) {
								missingLootTables.add(tileEntity.getLocation());
							} else {
								emptyNonChests++;
							}
						} else if (lootable.getSeed() != 0) {
							lootTableSeedSet.add(tileEntity.getLocation());
						}
					}
				}
			}
		}
		player.sendMessage(Component.text("Found ", NamedTextColor.GOLD).append(Component.text(lootables, NamedTextColor.AQUA))
			                   .append(Component.text(" lootable blocks within " + range + " chunks", NamedTextColor.GOLD)));
		if (skippedChunks > 0) {
			player.sendMessage(Component.text(skippedChunks + " chunks have been skipped because they are not loaded", NamedTextColor.RED));
		}
		if (!missingLootTables.isEmpty()) {
			player.sendMessage(Component.text("Found ", NamedTextColor.YELLOW).append(Component.text(missingLootTables.size(), NamedTextColor.AQUA))
				                   .append(Component.text(" empty " + (includeEmptyNonChests ? "lootable blocks" : "chests") + " without loot tables:", NamedTextColor.YELLOW)));
			sendLocations(player, missingLootTables);
		}
		if (emptyNonChests > 0) {
			player.sendMessage(Component.text("Found ", NamedTextColor.GRAY).append(Component.text(emptyNonChests, NamedTextColor.AQUA))
				                   .append(Component.text(" empty non-chest lootable blocks without loot tables. ", NamedTextColor.GRAY))
				                   .append(Component.text("Click to include these too").decorate(TextDecoration.UNDERLINED)
					                           .clickEvent(ClickEvent.runCommand("/" + COMMAND + " " + range + " true"))));
		}
		if (!lootTableSeedSet.isEmpty()) {
			player.sendMessage(Component.text("Found ", NamedTextColor.YELLOW).append(Component.text(lootTableSeedSet.size(), NamedTextColor.AQUA))
				                   .append(Component.text(" lootable blocks with a loot table seed set:", NamedTextColor.YELLOW)));
			sendLocations(player, lootTableSeedSet);
			player.sendMessage(Component.text("Click to clear seeds").decorate(TextDecoration.UNDERLINED)
				                   .clickEvent(ClickEvent.runCommand("/" + COMMAND + " clearseeds " + range)));
		}
	}

	private static void sendLocations(Player player, Collection<Location> locations) {
		int i = 0;
		Component line = null;
		for (Location l : locations) {
			Component c = Component.text(l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ())
				              .hoverEvent(HoverEvent.showText(Component.text("Click to teleport")))
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

	private static void clearSeeds(Player player, int range) {
		if (range > 20) { // limit to 20 chunks range, or 1681 chunks tested at most
			range = 20;
			player.sendMessage(Component.text("Range has been limited to " + range));
		}
		int startX = player.getLocation().getChunk().getX() - range;
		int endX = startX + 2 * range;
		int startZ = player.getLocation().getChunk().getZ() - range;
		int endZ = startZ + 2 * range;
		int skippedChunks = 0;
		int fixed = 0;
		for (int x = startX; x <= endX; x++) {
			for (int z = startZ; z <= endZ; z++) {
				if (!player.getWorld().isChunkLoaded(x, z)) {
					skippedChunks++;
					continue;
				}
				Chunk chunk = player.getWorld().getChunkAt(x, z);
				for (BlockState tileEntity : chunk.getTileEntities(true)) {
					if (tileEntity instanceof Lootable lootable) {
						if (lootable.getSeed() != 0) {
							lootable.setSeed(0);
							tileEntity.update();
							fixed++;
						}
					}
				}
			}
		}
		player.sendMessage(Component.text("Cleared loot table seeds in ", NamedTextColor.GOLD).append(Component.text(fixed, NamedTextColor.AQUA))
			                   .append(Component.text(" lootable blocks", NamedTextColor.GOLD)));
		if (skippedChunks > 0) {
			player.sendMessage(Component.text(skippedChunks + " chunks have been skipped because they are not loaded", NamedTextColor.RED));
		}
	}

	private static void clearNames(Player player, int range, String matchingName) {
		if (range > 20) { // limit to 20 chunks range, or 1681 chunks tested at most
			range = 20;
			player.sendMessage(Component.text("Range has been limited to " + range));
		}
		int startX = player.getLocation().getChunk().getX() - range;
		int endX = startX + 2 * range;
		int startZ = player.getLocation().getChunk().getZ() - range;
		int endZ = startZ + 2 * range;
		int skippedChunks = 0;
		int fixed = 0;
		for (int x = startX; x <= endX; x++) {
			for (int z = startZ; z <= endZ; z++) {
				if (!player.getWorld().isChunkLoaded(x, z)) {
					skippedChunks++;
					continue;
				}
				Chunk chunk = player.getWorld().getChunkAt(x, z);
				for (BlockState tileEntity : chunk.getTileEntities(true)) {
					if (tileEntity instanceof Lootable lootable
						    && tileEntity instanceof Nameable nameable
						    && nameable.customName() != null
						    && (matchingName == null || matchingName.equals(MessagingUtils.plainText(nameable.customName())))) {
						nameable.customName(null);
						tileEntity.update();
						fixed++;
					}
				}
			}
		}
		player.sendMessage(Component.text("Cleared names of ", NamedTextColor.GOLD).append(Component.text(fixed, NamedTextColor.AQUA))
			                   .append(Component.text(" lootable blocks", NamedTextColor.GOLD)));
		if (skippedChunks > 0) {
			player.sendMessage(Component.text(skippedChunks + " chunks have been skipped because they are not loaded", NamedTextColor.RED));
		}
	}


}
