package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.graves.GraveManager;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class Grave {
	private static final String SUMMON_LIST_TAG = "SummonGraveSelect";
	private static final HashMap<UUID, BukkitRunnable> SUMMON_LIST_RUNNABLES = new HashMap<>();

	public static void register() {
		new CommandAPICommand("grave")
			.withPermission("monumenta.command.grave")
			.withSubcommand(new CommandAPICommand("permission")
				.withPermission("monumenta.command.grave.permission")
				.withSubcommand(new CommandAPICommand("grant")
					.withArguments(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
					.executesPlayer((PlayerCommandExecutor) (player, args) -> permissionGrantPlayer(player, (Player) args[0]))
				)
				.withSubcommand(new CommandAPICommand("revoke")
					.withArguments(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
					.executesPlayer((PlayerCommandExecutor) (player, args) -> permissionRevokePlayer(player, (Player) args[0]))
				)
			)
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
						.withRequirement((sender -> ((Player) sender).getScoreboardTags().contains(SUMMON_LIST_TAG)))
						.withArguments(new LocationArgument("location"))
						.withArguments(new IntegerArgument("page"))
						.executesPlayer((PlayerCommandExecutor) (player, args) -> summonGraveListPage(player, (Location) args[0], (int) args[1]))
					)
					.withSubcommand(new CommandAPICommand("select")
						.withRequirement((sender -> ((Player) sender).getScoreboardTags().contains(SUMMON_LIST_TAG)))
						.withArguments(new LocationArgument("location"))
						.withArguments(new IntegerArgument("grave"))
						.executesPlayer((PlayerCommandExecutor) (player, args) -> summonGraveListSelect(player, (Location) args[0], (int) args[1]))
					)
					.withSubcommand(new CommandAPICommand("confirm")
						.withRequirement((sender -> ((Player) sender).getScoreboardTags().contains(SUMMON_LIST_TAG)))
						.withArguments(new LocationArgument("location"))
						.withArguments(new IntegerArgument("grave"))
						.executesPlayer((PlayerCommandExecutor) (player, args) -> summonGraveListConfirm(player, (Location) args[0], (int) args[1]))
					)
				)
			)
			.withSubcommand(new CommandAPICommand("summon")
				.withArguments(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER).withPermission("monumenta.command.grave.summon.other"))
				.withArguments(new LocationArgument("location"))
				.withArguments(new IntegerArgument("grave"))
				.executesPlayer((PlayerCommandExecutor) (player, args) -> summonGraveOther(player, (Player) args[0], (Location) args[1], (int) args[2]))
			)
			.register();
	}

	private static void permissionGrantPlayer(Player player, Player target) {
		GraveManager manager = GraveManager.getInstance(player);
		if (manager != null && manager.grantPermission(target)) {
			player.sendMessage(Component.text()
				.color(NamedTextColor.AQUA)
				.append(target.displayName()
					.hoverEvent(target))
				.append(Component.text(" has been granted permission to collect your graves."))
				.append(Component.newline())
				.append(Component.text("This permission will last until you log out or revoke it with /grave permission revoke <player>."))
			);
			target.sendMessage(Component.text()
				.color(NamedTextColor.AQUA)
				.append(player.displayName()
					.hoverEvent(player))
				.append(Component.text(" has granted you permission to collect their graves."))
				.append(Component.newline())
				.append(Component.text("This permission will last until they log out or revoke it."))
			);
		} else {
			player.sendMessage(Component.text()
				.color(NamedTextColor.AQUA)
				.append(target.displayName()
					.hoverEvent(target))
				.append(Component.text(" already has permission to collect your graves."))
				.append(Component.newline())
				.append(Component.text("This permission will last until you log out or revoke it with /grave permission revoke <player>."))
			);
		}
	}

	private static void permissionRevokePlayer(Player player, Player target) {
		GraveManager manager = GraveManager.getInstance(player);
		if (manager != null && manager.revokePermission(target)) {
			player.sendMessage(Component.text()
				.color(NamedTextColor.AQUA)
				.append(target.displayName()
					.hoverEvent(target))
				.append(Component.text(" has had their permission to access your graves revoked."))
			);
			target.sendMessage(Component.text()
				.color(NamedTextColor.AQUA)
				.append(player.displayName()
					.hoverEvent(player))
				.append(Component.text(" has revoked your permission to access their graves."))
			);
		} else {
			player.sendMessage(Component.text()
				.color(NamedTextColor.AQUA)
				.append(target.displayName()
					.hoverEvent(target))
				.append(Component.text(" does not have permission to collect your graves."))
				.append(Component.newline())
				.append(Component.text("You can grant them permission with /grave permission grant <player>."))
			);
		}
	}

	private static void listGravesSelf(Player player, int page) throws WrapperCommandSyntaxException {
		GraveManager manager = GraveManager.getInstance(player);
		if (manager != null && manager.getGravesCount() > 0) {
			int pages = manager.getGravesPageCount();
			if (page > pages || page < 1) {
				page = pages;
			}
			int index = (page - 1) * 5;
			ArrayList<Component> entries = manager.getGravesList(page);
			Component message = Component.text("List of ", NamedTextColor.AQUA)
				.append(player.displayName().hoverEvent(player))
				.append(Component.text(player.displayName().toString().endsWith("s") ? "' Graves" : "'s Graves"));
			for (Component entry : entries) {
				message = message.append(Component.newline()
					.append(Component.text("[", NamedTextColor.GRAY))
					.append(Component.text(index, NamedTextColor.WHITE))
					.append(Component.text("] ", NamedTextColor.GRAY))
					.append(entry)
				);
				index++;
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
			int index = (page - 1) * 5;
			ArrayList<Component> entries = manager.getGravesList(page);
			Component message = Component.text("List of ", NamedTextColor.AQUA)
				.append(player.displayName().hoverEvent(player))
				.append(Component.text(player.displayName().toString().endsWith("s") ? "' Graves" : "'s Graves"));
			for (Component entry : entries) {
				message = message.append(Component.newline()
					.append(Component.text("[", NamedTextColor.GRAY))
					.append(Component.text(index, NamedTextColor.WHITE))
					.append(Component.text("] ", NamedTextColor.GRAY))
					.append(entry)
				);
				index++;
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
				int index = (page - 1) * 5;
				String coordinates = String.format("%s %s %s", location.getX(), location.getY(), location.getZ());
				ArrayList<Component> entries = manager.getGravesList(page);
				Component message = Component.text("List of ", NamedTextColor.AQUA)
					.append(player.displayName().hoverEvent(player))
					.append(Component.text(player.displayName().toString().endsWith("s") ? "' Graves" : "'s Graves"))
					.append(Component.newline())
					.append(Component.text("Click the number on the left to select", NamedTextColor.GOLD));
				for (Component entry : entries) {
					message = message.append(Component.newline()
						.append(Component.text("[", NamedTextColor.GRAY)
							.append(Component.text(index, NamedTextColor.GOLD))
							.append(Component.text("] ", NamedTextColor.GRAY))
							.hoverEvent(HoverEvent.showText(Component.text("Click to summon")))
							.clickEvent(ClickEvent.runCommand("/grave summon list select " + coordinates + " " + index)))
						.append(entry)
					);
					index++;
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

	private static void summonGraveListSelect(Player player, Location location, int index) throws WrapperCommandSyntaxException {
		if (removeSummonListTag(player)) {
			GraveManager manager = GraveManager.getInstance(player);
			if (manager != null) {
				Component grave = manager.getGraveInfo(index);
				if (grave != null) {
					String coordinates = String.format("%s %s %s", location.getBlockX(), location.getBlockY(), location.getBlockZ());
					player.sendMessage(Component.text("Are you sure you wanted to summon this grave?", NamedTextColor.AQUA)
						.append(Component.newline())
						.append(grave)
						.append(Component.newline())
						.append(Component.text("[", NamedTextColor.GRAY)
							.append(Component.text("CONFIRM", NamedTextColor.GREEN))
							.append(Component.text("]", NamedTextColor.GRAY))
							.clickEvent(ClickEvent.runCommand("/grave summon list confirm " + coordinates + " " + index))
						)
					);
					addSummonListTag(player);
				} else {
					CommandAPI.fail("Grave " + index + " does not exist. Maximum is " + (manager.getGravesCount() - 1));
				}
			} else {
				CommandAPI.fail("You do not have any graves.");
			}
		} else {
			CommandAPI.fail("Timed out; please talk to the NPC again.");
		}
	}

	private static void summonGraveListConfirm(Player player, Location location, int index) {
		GraveManager manager = GraveManager.getInstance(player);
		if (removeSummonListTag(player)) {
			if (manager != null) {
				if (manager.getGravesCount() > index) {
					if (manager.summonGrave(index, location)) {
						player.sendMessage(Component.text("Grave successfully summoned to ", NamedTextColor.AQUA)
							.append(Component.text(location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ()))
						);
					} else {
						player.sendMessage(Component.text("Something went wrong getting your grave; please try again", NamedTextColor.RED));
					}
				} else {
					player.sendMessage(Component.text("Grave " + index + " does not exist. Maximum is " + (manager.getGravesCount() - 1), NamedTextColor.RED));
				}
			} else {
				player.sendMessage(Component.text("You do not have any graves", NamedTextColor.RED));
			}
		} else {
			player.sendMessage(Component.text("Timed out; please talk to the NPC again.", NamedTextColor.RED));
		}
	}

	private static void summonGraveOther(Player sender, Player player, Location location, int index) {
		GraveManager manager = GraveManager.getInstance(player);
		if (manager != null) {
			if (manager.getGravesCount() > index) {
				if (manager.summonGrave(index, location)) {
					sender.sendMessage(Component.text("Grave successfully summoned to ", NamedTextColor.AQUA)
						.append(Component.text(location.getX() + "," + location.getY() + "," + location.getZ()))
					);
				} else {
					sender.sendMessage(Component.text("Something went wrong getting the grave; please try again", NamedTextColor.RED));
				}
			} else {
				sender.sendMessage(Component.text("Grave " + index + " does not exist. Maximum is " + (manager.getGravesCount() - 1), NamedTextColor.RED));
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
}
