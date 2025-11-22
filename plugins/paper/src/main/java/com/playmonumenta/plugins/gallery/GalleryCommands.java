package com.playmonumenta.plugins.gallery;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.gallery.effects.GalleryEffectType;
import com.playmonumenta.plugins.gallery.interactables.BaseInteractable;
import com.playmonumenta.plugins.utils.MetadataUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public class GalleryCommands {
	private static final String COMMAND = "gallery";
	private static final String PERMISSION = "monumenta.r3.gallery";

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("init"),
				new EntitySelectorArgument.ManyPlayers("players"),
				new MultiLiteralArgument("map", Arrays.stream(GalleryMap.values()).map(Enum::name).toList().toArray(new String[0])))
			.executes((sender, args) -> {
				GalleryMap map = GalleryMap.fromName(args.getUnchecked("map"));
				List<Player> players = new ArrayList<>(args.getUnchecked("players"));
				if (!players.isEmpty() && map != null) {
					GalleryGame game = new GalleryGame(players.get(0).getWorld().getUID(), map, players);
					GalleryManager.addGame(game);
					return 1;
				}
				GalleryUtils.printDebugMessage("UNABLE TO START GALLERY GAME " + map + " MAP NULL OR TARGET PLAYERS EMPTY? THIS IS A BUG!");
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("player"),
				new LiteralArgument("info"),
				new EntitySelectorArgument.OnePlayer("players"))
			.executes((sender, args) -> {
				Player target = args.getUnchecked("players");
				GalleryGame game = GalleryManager.GAMES.get(target.getWorld().getUID());

				if (!MetadataUtils.checkOnceThisTick(Plugin.getInstance(), target, "GalleryCommandOneTick")) {
					return 1;
				}

				if (game != null) {
					game.printPlayerInfo(target);
				} else {
					target.sendMessage(Component.text("The game has not started yet!", NamedTextColor.GRAY));
				}
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("player"),
				new LiteralArgument("leave"),
				new EntitySelectorArgument.OnePlayer("players"))
			.executes((sender, args) -> {
				Player target = args.getUnchecked("players");
				GalleryGame game = GalleryManager.GAMES.get(target.getWorld().getUID());
				if (game != null) {
					game.playerLeave(target);
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("player"),
				new EntitySelectorArgument.ManyPlayers("players"),
				new LiteralArgument("set"),
				new LiteralArgument("alive"),
				new BooleanArgument("true/false"))
			.executes((sender, args) -> {
				boolean alive = args.getUnchecked("true/false");
				GalleryGame game = null;
				for (Player player : (Collection<Player>) args.get("players")) {
					if (game == null) {
						game = getGameFromEntity(player);
					}

					if (game != null) {
						game.setPlayerAlive(player, alive);
					}
				}
			}).register();


		registerUtils();
	}

	private static void registerUtils() {
		Argument<?> util = new LiteralArgument("utils");

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("moderation"),
				new LiteralArgument("get"),
				new LiteralArgument("info"))
			.executesPlayer((moderator, args) -> {
				GalleryGame game = getGameFromSender(moderator);
				if (game != null) {
					game.printModerationInfo(moderator, null);
				}
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("moderation"),
				new LiteralArgument("get"),
				new LiteralArgument("info"),
				new EntitySelectorArgument.OnePlayer("target"))
			.executesPlayer((moderator, args) -> {
				GalleryGame game = getGameFromSender(moderator);
				if (game != null) {
					game.printModerationInfo(moderator, args.getUnchecked("target"));
				}
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("give"),
				new LiteralArgument("coin"),
				new IntegerArgument("coins"))
			.executes((sender, args) -> {
				int coins = args.getUnchecked("coins");
				GalleryGame game = getGameFromSender(sender);

				if (game != null) {
					game.givePlayersCoins(coins);
				}

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("give"),
				new LiteralArgument("effect"),
				new EntitySelectorArgument.ManyPlayers("players"),
				new MultiLiteralArgument("type", Arrays.stream(GalleryEffectType.values()).map(Enum::name).toList().toArray(new String[0])))
			.executes((sender, args) -> {
				GalleryEffectType type = GalleryEffectType.fromName(args.getUnchecked("type"));
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				if (type == null) {
					throw CommandAPI.failWithString("Invalid gallery effect type");
				}
				for (Player entity : (Collection<Player>) args.get("players")) {
					Objects.requireNonNull(game.getGalleryPlayer(entity.getUniqueId())).giveEffect(type.newEffect());
				}

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("get"),
				new LiteralArgument("round"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				return game.getCurrentRound();
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("get"),
				new LiteralArgument("coin"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				return game.getPlayersCoins();
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("box"),
				new LiteralArgument("get"),
				new LiteralArgument("locations"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				List<Location> locations = game.getBoxLocations();

				sender.sendMessage("Found " + locations.size() + " possible box location");
				sender.sendMessage("----------------------------------------------");
				for (Location location : locations) {
					sender.sendMessage("loc " + location.toVector());
				}
				sender.sendMessage("----------------------------------------------");
				sender.sendMessage("Active box at: " + Objects.requireNonNull(game.getBoxLocation()).toVector());

			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("box"),
				new LiteralArgument("move"),
				new LiteralArgument("random"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				game.moveBoxAtRandomLocation();

			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("get"),
				new LiteralArgument("interactable"),
				new LiteralArgument("position"),
				new StringArgument("name"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				String name = args.getUnchecked("name");
				BaseInteractable interactable = game.getInteractable(name);
				if (interactable == null) {
					throw CommandAPI.failWithString("Interactable null!!! no match for name: " + name);
				}
				sender.sendMessage("Interactable " + interactable.getName() + " at Pos: " + interactable.getLocation());
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("spawner"),
				new LiteralArgument("activate"),
				new StringArgument("spawnerName"),
				new BooleanArgument("active"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				boolean active = args.getUnchecked("active");
				game.setActiveSpawner(args.getUnchecked("spawnerName"), active);
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("interactable"),
				new StringArgument("name"),
				new LiteralArgument("set"),
				new LiteralArgument("interactable"),
				new BooleanArgument("isInteractableOrNot"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				game.setInteractable(args.getUnchecked("name"), args.getUnchecked("isInteractableOrNot"));
				return 1;
			}).register();

		// According to nicknon this command is never used. As written it didn't make sense, so I modified it to what I think it should be, but I honestly have no idea
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("interactable"),
				new StringArgument("name"),
				new LiteralArgument("set"),
				new LiteralArgument("interactable"),
				new GreedyStringArgument("showingText"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				String text = args.getUnchecked("showingText");
				if (text != null && text.isEmpty()) {
					text = null;
				}
				game.setInteractableText(args.getUnchecked("name"), text);
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("despawn"),
				new LiteralArgument("mobs"),
				new EntitySelectorArgument.ManyEntities("mobs"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				for (Entity entity : (Collection<Entity>) args.get("mobs")) {
					if (entity instanceof LivingEntity livingEntity) {
						game.despawnMob(livingEntity);
					}
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("interactable"),
				new StringArgument("name"),
				new LiteralArgument("remove"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				game.removeInteractable(args.getUnchecked("name"));
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("set"),
				new LiteralArgument("mobs"),
				new LiteralArgument("spawning"),
				new BooleanArgument("True/False"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				game.setCanMobsSpawn(args.getUnchecked("True/False"));
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("reload"),
				new LiteralArgument("all"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				game.reloadAll();
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("load"),
				new EntitySelectorArgument.ManyEntities("targets"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				for (Entity entity : (Collection<Entity>) args.get("targets")) {
					game.load(entity);
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("set"),
				new LiteralArgument("round"),
				new IntegerArgument("round", 0))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				game.setRound(args.getUnchecked("round"));
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new LiteralArgument("set"),
				new LiteralArgument("spawnlocation"),
				new LocationArgument("location", LocationType.BLOCK_POSITION))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game == null) {
					throw CommandAPI.failWithString("Could not detect game");
				}
				game.setSpawnLocation(args.getUnchecked("location"));
				return 1;
			}).register();
	}

	private static @Nullable GalleryGame getGameFromSender(CommandSender sender) {
		if (sender instanceof Entity entity) {
			return GalleryManager.GAMES.get(entity.getWorld().getUID());
		} else if (sender instanceof NativeProxyCommandSender proxyCommandSender) {
			return GalleryManager.GAMES.get(proxyCommandSender.getWorld().getUID());
		}
		return null;
	}

	private static @Nullable GalleryGame getGameFromEntity(Entity entity) {
		return GalleryManager.GAMES.get(entity.getWorld().getUID());
	}
}
