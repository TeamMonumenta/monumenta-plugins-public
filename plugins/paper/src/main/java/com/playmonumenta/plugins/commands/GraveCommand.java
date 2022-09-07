package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.graves.Grave;
import com.playmonumenta.plugins.graves.GraveManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.UUIDArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class GraveCommand {
	private static final String SUMMON_LIST_TAG = "SummonGraveSelect";
	private static final HashMap<UUID, BukkitRunnable> SUMMON_LIST_RUNNABLES = new HashMap<>();

	public static void register() {
		new CommandAPICommand("grave")
			.withPermission("monumenta.command.grave")
			.withSubcommand(new CommandAPICommand("list")
				.withPermission("monumenta.command.grave.list")
				.executesPlayer((PlayerCommandExecutor) (player, args) -> listGravesSelf(player, 1))
			)
			.withSubcommand(new CommandAPICommand("list")
				.withPermission("monumenta.command.grave.list")
				.withArguments(new IntegerArgument("page"))
				.executesPlayer((PlayerCommandExecutor) (player, args) -> listGravesSelf(player, (int) args[0]))
			)
			.withSubcommand(new CommandAPICommand("list")
				.withPermission("monumenta.command.grave.list")
				.withArguments(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER).withPermission("monumenta.command.grave.list.other"))
				.executesPlayer((PlayerCommandExecutor) (player, args) -> listGravesOther(player, (Player) args[0], 1))
			)
			.withSubcommand(new CommandAPICommand("list")
				.withPermission("monumenta.command.grave.list")
				.withArguments(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER).withPermission("monumenta.command.grave.list.other"))
				.withArguments(new IntegerArgument("page"))
				.executesPlayer((PlayerCommandExecutor) (player, args) -> listGravesOther(player, (Player) args[0], (int) args[1]))
			)
			.withSubcommand(new CommandAPICommand("summon")
				.withSubcommand(new CommandAPICommand("list")
					.withArguments(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER).withPermission("monumenta.command.grave.summon.list"))
					.withArguments(new LocationArgument("location"))
					.executes((CommandExecutor) (sender, args) -> summonGraveList((Player) args[0], (Location) args[1]))
				)
				.withSubcommand(new CommandAPICommand("list")
					.withSubcommand(new CommandAPICommand("page")
						.withRequirement(sender -> ((Player) sender).getScoreboardTags().contains(SUMMON_LIST_TAG))
						.withArguments(new LocationArgument("location"))
						.withArguments(new IntegerArgument("page"))
						.executesPlayer((PlayerCommandExecutor) (player, args) -> summonGraveListPage(player, (Location) args[0], (int) args[1]))
					)
					.withSubcommand(new CommandAPICommand("select")
						.withRequirement(sender -> ((Player) sender).getScoreboardTags().contains(SUMMON_LIST_TAG))
						.withArguments(new LocationArgument("location"))
						.withArguments(new UUIDArgument("grave"))
						.executesPlayer((PlayerCommandExecutor) (player, args) -> summonGraveListSelect(player, (Location) args[0], (UUID) args[1]))
					)
					.withSubcommand(new CommandAPICommand("confirm")
						.withRequirement(sender -> ((Player) sender).getScoreboardTags().contains(SUMMON_LIST_TAG))
						.withArguments(new LocationArgument("location"))
						.withArguments(new UUIDArgument("grave"))
						.executesPlayer((PlayerCommandExecutor) (player, args) -> summonGraveListConfirm(player, (Location) args[0], (UUID) args[1]))
					)
				)
			)
			.withSubcommand(new CommandAPICommand("summon")
				.withArguments(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER).withPermission("monumenta.command.grave.summon.other"))
				.withArguments(new LocationArgument("location"))
				.withArguments(new UUIDArgument("grave"))
				.executesPlayer((PlayerCommandExecutor) (player, args) -> summonGraveOther(player, (Player) args[0], (Location) args[1], (UUID) args[2]))
			)
			.withSubcommand(new CommandAPICommand("summon")
				.withArguments(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER).withPermission("monumenta.command.grave.summon.other"))
				.withArguments(new UUIDArgument("grave"))
				.executesPlayer((PlayerCommandExecutor) (player, args) -> summonGraveOther(player, (Player) args[0], player.getLocation(), (UUID) args[1]))
			)
			.withSubcommand(new CommandAPICommand("delete")
				.withPermission("monumenta.command.grave.delete")
				.withArguments(new UUIDArgument("grave"))
				.executesPlayer((PlayerCommandExecutor) (player, args) -> delete(player, (UUID) args[0])))
			.withSubcommand(new CommandAPICommand("delete")
				.withPermission("monumenta.command.grave.delete")
				.withArguments(new LiteralArgument("cancel"))
				.executesPlayer((PlayerCommandExecutor) (player, args) -> delete(player, null)))
			.register();
	}

	private static void listGravesSelf(Player player, int page) throws WrapperCommandSyntaxException {
		GraveManager manager = GraveManager.getInstance(player);
		if (manager != null && manager.getGravesCount() > 0) {
			int pages = manager.getGravesPageCount();
			if (page > pages || page < 1) {
				page = pages;
			}
			ArrayList<Component> entries = manager.getGravesList(page);
			Component message = Component.text("List of ", NamedTextColor.AQUA)
				.append(player.displayName().hoverEvent(player))
				.append(Component.text(player.displayName().toString().endsWith("s") ? "' Graves" : "'s Graves"));
			for (Component entry : entries) {
				message = message.append(Component.newline().append(entry));
			}
			message = message.append(Component.newline()
				.append(Component.text("<<<")
					.color(page == 1 ? NamedTextColor.GRAY : NamedTextColor.GOLD)
					.hoverEvent(HoverEvent.showText(Component.text("First Page")))
					.clickEvent(page == 1 ? null : ClickEvent.runCommand("/grave list 1")))
				.append(Component.text(" << ")
					.color(page == 1 ? NamedTextColor.GRAY : NamedTextColor.GOLD)
					.hoverEvent(HoverEvent.showText(Component.text("Previous Page")))
					.clickEvent(page == 1 ? null : ClickEvent.runCommand("/grave list " + (page - 1))))
				.append(Component.text("Page " + page + "/" + pages))
				.append(Component.text(" >> ")
					.color(page == pages ? NamedTextColor.GRAY : NamedTextColor.GOLD)
					.hoverEvent(HoverEvent.showText(Component.text("Next Page")))
					.clickEvent(page == pages ? null : ClickEvent.runCommand("/grave list " + (page + 1))))
				.append(Component.text(">>>")
					.color(page == pages ? NamedTextColor.GRAY : NamedTextColor.GOLD)
					.hoverEvent(HoverEvent.showText(Component.text("Last Page")))
					.clickEvent(page == pages ? null : ClickEvent.runCommand("/grave list " + pages)))
			);
			player.sendMessage(message);
		} else {
			CommandAPI.fail("You don't have any graves");
		}
	}

	private static void listGravesOther(Player sender, Player player, int page) throws WrapperCommandSyntaxException {
		GraveManager manager = GraveManager.getInstance(player);
		String name = player.getName();
		if (manager != null && manager.getGravesCount() > 0) {
			int pages = manager.getGravesPageCount();
			if (page > pages || page < 1) {
				page = pages;
			}
			ArrayList<Component> entries = manager.getGravesList(page);
			Component message = Component.text("List of ", NamedTextColor.AQUA)
				.append(player.displayName().hoverEvent(player))
				.append(Component.text(player.displayName().toString().endsWith("s") ? "' Graves" : "'s Graves"));
			for (Component entry : entries) {
				message = message.append(Component.newline().append(entry));
			}
			message = message.append(Component.newline()
				.append(Component.text("<<<")
					.color(page == 1 ? NamedTextColor.GRAY : NamedTextColor.GOLD)
					.hoverEvent(HoverEvent.showText(Component.text("First Page")))
					.clickEvent(page == 1 ? null : ClickEvent.runCommand("/grave list " + name + " 1")))
				.append(Component.text(" << ")
					.color(page == 1 ? NamedTextColor.GRAY : NamedTextColor.GOLD)
					.hoverEvent(HoverEvent.showText(Component.text("Previous Page")))
					.clickEvent(page == 1 ? null : ClickEvent.runCommand("/grave list " + name + " " + (page - 1))))
				.append(Component.text("Page " + page + "/" + pages))
				.append(Component.text(" >> ")
					.color(page == pages ? NamedTextColor.GRAY : NamedTextColor.GOLD)
					.hoverEvent(HoverEvent.showText(Component.text("Next Page")))
					.clickEvent(page == pages ? null : ClickEvent.runCommand("/grave list " + name + " " + (page + 1))))
				.append(Component.text(">>>")
					.color(page == pages ? NamedTextColor.GRAY : NamedTextColor.GOLD)
					.hoverEvent(HoverEvent.showText(Component.text("Last Page")))
					.clickEvent(page == pages ? null : ClickEvent.runCommand("/grave list " + name + " " + pages)))
			);
			sender.sendMessage(message);
		} else {
			CommandAPI.fail(player.getName() + " doesn't have any graves");
		}
	}

	private static void summonGraveList(Player player, Location location) throws WrapperCommandSyntaxException {
		addSummonListTag(player);
		summonGraveListPage(player, location, 1);
	}

	private static void summonGraveListPage(Player player, Location location, int page) throws WrapperCommandSyntaxException {
		GraveManager manager = GraveManager.getInstance(player);
		if (removeSummonListTag(player)) {
			if (manager != null && manager.getGravesCount() > 0) {
				int pages = manager.getGravesPageCount();
				if (page > pages || page < 1) {
					page = pages;
				}
				String coordinates = String.format("%s %s %s", location.getX(), location.getY(), location.getZ());
				ArrayList<Component> entries = manager.getGravesList(page, grave -> Component.text(" ")
					.append(Component.text("[S]", NamedTextColor.GOLD))
					.hoverEvent(HoverEvent.showText(Component.text("Click to summon")))
					.clickEvent(ClickEvent.runCommand("/grave summon list select " + coordinates + " " + grave.mUuid)));
				Component message = Component.text("List of ", NamedTextColor.AQUA)
					.append(player.displayName().hoverEvent(player))
					.append(Component.text(player.displayName().toString().endsWith("s") ? "' Graves" : "'s Graves"));
				for (Component entry : entries) {
					message = message.append(Component.newline().append(entry));
				}
				message = message.append(Component.newline()
					.append(Component.text("<<<")
						.color(page == 1 ? NamedTextColor.GRAY : NamedTextColor.GOLD)
						.hoverEvent(HoverEvent.showText(Component.text("First Page")))
						.clickEvent(page == 1 ? null : ClickEvent.runCommand("/grave summon list page " + coordinates + " 1")))
					.append(Component.text(" << ")
						.color(page == 1 ? NamedTextColor.GRAY : NamedTextColor.GOLD)
						.hoverEvent(HoverEvent.showText(Component.text("Previous Page")))
						.clickEvent(page == 1 ? null : ClickEvent.runCommand("/grave summon list page " + coordinates + " " + (page - 1))))
					.append(Component.text("Page " + page + "/" + pages))
					.append(Component.text(" >> ")
						.color(page == pages ? NamedTextColor.GRAY : NamedTextColor.GOLD)
						.hoverEvent(HoverEvent.showText(Component.text("Next Page")))
						.clickEvent(page == pages ? null : ClickEvent.runCommand("/grave summon list page " + coordinates + " " + (page + 1))))
					.append(Component.text(">>>")
						.color(page == pages ? NamedTextColor.GRAY : NamedTextColor.GOLD)
						.hoverEvent(HoverEvent.showText(Component.text("Next Page")))
						.clickEvent(page == pages ? null : ClickEvent.runCommand("/grave summon list page " + coordinates + " " + pages)))
				);
				player.sendMessage(message);
				addSummonListTag(player);
			} else {
				CommandAPI.fail("You don't have any graves");
			}
		} else {
			CommandAPI.fail("Timed out; please talk to the NPC again.");
		}
	}

	private static void summonGraveListSelect(Player player, Location location, UUID uuid) throws WrapperCommandSyntaxException {
		if (removeSummonListTag(player)) {
			GraveManager manager = GraveManager.getInstance(player);
			if (manager != null) {
				Grave grave = manager.getGrave(uuid);
				if (grave != null) {
					Component graveInfo = manager.getGraveInfo(manager.getGrave(uuid));
					String coordinates = String.format("%s %s %s", location.getBlockX(), location.getBlockY(), location.getBlockZ());
					player.sendMessage(Component.text("Are you sure you wanted to summon this grave?", NamedTextColor.AQUA)
						.append(Component.newline())
						.append(graveInfo)
						.append(Component.newline())
						.append(Component.text("[", NamedTextColor.GRAY)
							.append(Component.text("CONFIRM", NamedTextColor.GREEN))
							.append(Component.text("]", NamedTextColor.GRAY))
							.clickEvent(ClickEvent.runCommand("/grave summon list confirm " + coordinates + " " + uuid))
						)
					);
					addSummonListTag(player);
				} else {
					CommandAPI.fail("This grave does not exist.");
				}
			} else {
				CommandAPI.fail("You do not have any graves.");
			}
		} else {
			CommandAPI.fail("Timed out; please talk to the NPC again.");
		}
	}

	private static void summonGraveListConfirm(Player player, Location location, UUID uuid) {
		GraveManager manager = GraveManager.getInstance(player);
		if (removeSummonListTag(player)) {
			if (manager != null) {
				Grave grave = manager.getGrave(uuid);
				if (grave != null) {
					grave.summon(location);
					player.sendMessage(Component.text("Grave successfully summoned to ", NamedTextColor.AQUA)
						.append(Component.text(location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ()))
					);
				} else {
					player.sendMessage(Component.text("This grave does not exist.", NamedTextColor.RED));
				}
			} else {
				player.sendMessage(Component.text("You do not have any graves", NamedTextColor.RED));
			}
		} else {
			player.sendMessage(Component.text("Timed out; please talk to the NPC again.", NamedTextColor.RED));
		}
	}

	private static void summonGraveOther(Player sender, Player player, Location location, UUID uuid) {
		GraveManager manager = GraveManager.getInstance(player);
		if (manager != null) {
			Grave grave = manager.getGrave(uuid);
			if (grave != null) {
				grave.summon(location);
				sender.sendMessage(Component.text("Grave successfully summoned to ", NamedTextColor.AQUA)
					.append(Component.text(location.getX() + "," + location.getY() + "," + location.getZ()))
				);
			} else {
				player.sendMessage(Component.text("This grave does not exist.", NamedTextColor.RED));
			}
		} else {
			sender.sendMessage(Component.text("You do not have any graves", NamedTextColor.RED));
		}
	}

	private static void addSummonListTag(Player player) {
		// Add the tag that allows the player to run the next command
		if (SUMMON_LIST_RUNNABLES.containsKey(player.getUniqueId())) {
			BukkitRunnable runnable = SUMMON_LIST_RUNNABLES.remove(player.getUniqueId());
			runnable.cancel();
		}
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				removeSummonListTag(player);
			}
		};
		SUMMON_LIST_RUNNABLES.put(player.getUniqueId(), runnable);
		runnable.runTaskLater(Plugin.getInstance(), 30 * 20);
		player.addScoreboardTag(SUMMON_LIST_TAG);
		CommandAPI.updateRequirements(player);
	}

	public static boolean removeSummonListTag(Player player) {
		if (player != null) {
			if (SUMMON_LIST_RUNNABLES.containsKey(player.getUniqueId())) {
				BukkitRunnable runnable = SUMMON_LIST_RUNNABLES.remove(player.getUniqueId());
				runnable.cancel();
			}
			if (player.getScoreboardTags().contains(SUMMON_LIST_TAG)) {
				player.removeScoreboardTag(SUMMON_LIST_TAG);
				CommandAPI.updateRequirements(player);
				return true;
			}
		}
		return false;
	}

	private static void delete(Player player, UUID uuid) {
		GraveManager manager = GraveManager.getInstance(player);
		if (manager == null) {
			player.sendMessage(Component.text("You do not have any graves.", NamedTextColor.RED));
			return;
		}
		if (uuid == null) {
			if (manager.cancelDeletion()) {
				player.sendMessage(Component.text("Grave deletion cancelled.", NamedTextColor.RED));
			}
			return;
		}
		Optional<Grave> optionalGrave = manager.getGraves().stream().filter(grave -> grave.mUuid.equals(uuid)).findFirst();
		if (optionalGrave.isEmpty()) {
			player.sendMessage(Component.text("This grave does not exist. It may have already been collected or deleted.", NamedTextColor.RED));
			return;
		}
		Grave grave = optionalGrave.get();
		if (manager.isDeleteConfirmation(uuid, player.getTicksLived())) {
			grave.delete();
			manager.cancelDeletion();
			player.sendMessage(Component.text("Grave has been deleted.", NamedTextColor.GREEN));
		} else {
			player.sendMessage(Component.text("Are you sure you want to fully delete this grave? This cannot be undone!", NamedTextColor.RED, TextDecoration.BOLD));
			player.sendMessage(Component.text("Item" + (grave.getItems().size() > 1 ? "s" : "") + " in the grave: ", NamedTextColor.AQUA)
				.append(grave.getItemList(true)));
			player.sendMessage(Component.text()
				.append(Component.text("[DELETE]", NamedTextColor.RED)
					.hoverEvent(HoverEvent.showText(Component.text("Delete the grave. Cannot be undone!", NamedTextColor.RED)))
					.clickEvent(ClickEvent.runCommand("/grave delete " + uuid)))
				.append(Component.text("   "))
				.append(Component.text("[CANCEL]", NamedTextColor.WHITE)
					.hoverEvent(HoverEvent.showText(Component.text("Cancel deletion of the grave", NamedTextColor.WHITE)))
					.clickEvent(ClickEvent.runCommand("/grave delete cancel"))));
		}
	}
}
