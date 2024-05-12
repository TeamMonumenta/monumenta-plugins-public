package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class BulkGuildBanners {
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.guild.mod.bulkbanners");
	private static final Component CONTAINER_NAME = Component.text(
			"Bulk Guild Banners",
			NamedTextColor.LIGHT_PURPLE,
			TextDecoration.BOLD
		)
		.decoration(TextDecoration.ITALIC, false);

	public static void register(Plugin plugin) {
		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("mod"))
			.withArguments(new LiteralArgument("bulkbanners"))
			.withArguments(new LiteralArgument("get"))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				runGet(plugin, sender);
			})
			.register();

		new CommandAPICommand("guild")
			.withArguments(new LiteralArgument("mod"))
			.withArguments(new LiteralArgument("bulkbanners"))
			.withArguments(new LiteralArgument("load"))
			.withArguments(new LocationArgument("from", LocationType.BLOCK_POSITION))
			.withArguments(new LocationArgument("to", LocationType.BLOCK_POSITION))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				Location from = args.getUnchecked("from");
				Location to = args.getUnchecked("to");
				runLoad(plugin, sender, from, to);
			})
			.register();
	}

	public static void runGet(Plugin plugin, CommandSender sender) throws WrapperCommandSyntaxException {
		if (ServerProperties.getShardName().contains("build")) {
			throw CommandAPI.failWithString("This command cannot be run on the build shard.");
		}

		CommandSender callee = CommandUtils.getCallee(sender);
		if (!(callee instanceof Player player)) {
			throw CommandAPI.failWithString("This command must be run as a player");
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Group> guilds;
			try {
				guilds = LuckPermsIntegration.getGuilds(false, false).join();
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					sender.sendMessage(Component.text("Unable to list guilds:", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
				});
				return;
			}

			List<ItemStack> contents = new ArrayList<>();
			for (Group guild : guilds) {
				ItemStack banner = LuckPermsIntegration.getGuildBanner(guild);

				ItemMeta meta = banner.getItemMeta();
				meta.displayName(Component.text(guild.getName())
					.decoration(TextDecoration.ITALIC, false));
				banner.setItemMeta(meta);
				contents.add(banner);

				if (contents.size() == 27) {
					final List<ItemStack> finalContents = contents;
					contents = new ArrayList<>();
					Bukkit.getScheduler().runTask(plugin, () -> giveContainers(player, finalContents));
				}
			}

			final List<ItemStack> finalContents = contents;
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (!finalContents.isEmpty()) {
					giveContainers(player, finalContents);
				}
				player.sendMessage(Component.text("Done!", NamedTextColor.GREEN));
			});
		});
	}

	public static void giveContainers(Player player, List<ItemStack> contents) {
		ItemStack containerItem = new ItemStack(Material.SHULKER_BOX);
		BlockStateMeta meta = (BlockStateMeta) containerItem.getItemMeta();
		meta.displayName(CONTAINER_NAME);
		ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
		Inventory inventory = shulkerBox.getInventory();
		for (ItemStack item : contents) {
			inventory.addItem(item);
		}
		meta.setBlockState(shulkerBox);
		containerItem.setItemMeta(meta);

		InventoryUtils.giveItem(player, containerItem);
	}

	public static void runLoad(Plugin plugin, CommandSender sender, Location from, Location to) throws WrapperCommandSyntaxException {
		if (ServerProperties.getShardName().contains("build")) {
			throw CommandAPI.failWithString("This command cannot be run on the build shard.");
		}

		Iterator<Location> it = getLocations(from, to).iterator();
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (!it.hasNext()) {
					sender.sendMessage(Component.text("Done!", NamedTextColor.GREEN));
					cancel();
					return;
				}

				Location location = it.next();
				loadBlock(plugin, sender, location);
			}
		};
		runnable.runTaskTimer(plugin, 0L, 1L);
	}

	private static List<Location> getLocations(Location from, Location to) {
		World world = from.getWorld();
		int minX = Integer.min(from.getBlockX(), to.getBlockX());
		int minY = Integer.min(from.getBlockY(), to.getBlockY());
		int minZ = Integer.min(from.getBlockZ(), to.getBlockZ());
		int maxX = Integer.max(from.getBlockX(), to.getBlockX());
		int maxY = Integer.max(from.getBlockY(), to.getBlockY());
		int maxZ = Integer.max(from.getBlockZ(), to.getBlockZ());

		List<Location> locations = new ArrayList<>();
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					Location location = new Location(world, x, y, z);
					locations.add(location);
				}
			}
		}
		return locations;
	}

	public static void loadBlock(Plugin plugin, CommandSender sender, Location location) {
		Block block = location.getBlock();
		if (!(block.getState() instanceof ShulkerBox shulkerBox)) {
			return;
		}
		if (!CONTAINER_NAME.equals(shulkerBox.customName())) {
			return;
		}
		Inventory inventory = shulkerBox.getInventory();

		for (ItemStack itemStack : inventory.getContents()) {
			if (itemStack == null) {
				continue;
			}
			loadBanner(plugin, sender, itemStack);
		}
	}

	public static void loadBanner(Plugin plugin, CommandSender sender, ItemStack banner) {
		if (!ItemUtils.isBanner(banner)) {
			return;
		}
		ItemMeta meta = banner.getItemMeta();
		String guildMemberId = MessagingUtils.plainText(meta.displayName());
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Group memberGroup = LuckPermsIntegration.loadGroup(guildMemberId).join().orElse(null);
				if (memberGroup == null) {
					Bukkit.getScheduler().runTask(plugin, () ->
						sender.sendMessage(Component.text("Could not load " + guildMemberId, NamedTextColor.RED))
					);
					return;
				}
				LuckPermsIntegration.setGuildBanner(sender, memberGroup, plugin, banner, true);
				Bukkit.getScheduler().runTask(plugin, () ->
					sender.sendMessage(Component.text("Updated guild banner for " + guildMemberId, NamedTextColor.GREEN))
				);
			} catch (Exception ex) {
				MessagingUtils.sendStackTrace(sender, ex);
			}
		});
	}
}
