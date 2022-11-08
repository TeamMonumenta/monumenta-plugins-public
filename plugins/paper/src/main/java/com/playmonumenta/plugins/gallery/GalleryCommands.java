package com.playmonumenta.plugins.gallery;

import com.playmonumenta.plugins.gallery.effects.GalleryEffectType;
import com.playmonumenta.plugins.gallery.interactables.BaseInteractable;
import com.playmonumenta.plugins.utils.MetadataUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class GalleryCommands {
	private static final String COMMAND = "gallery";
	private static final String PERMISSION = "monumenta.r3.gallery";

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new MultiLiteralArgument("init"),
				new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
				new MultiLiteralArgument(Arrays.stream(GalleryMap.values()).map(Enum::name).toList().toArray(new String[0])))
			.executes((sender, args) -> {
				GalleryMap map = GalleryMap.fromName((String) args[2]);
				List<Player> players = new ArrayList<>((Collection<Player>) args[1]);
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
				new MultiLiteralArgument("player"),
				new MultiLiteralArgument("info"),
				new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player target = (Player) args[2];
				GalleryGame game = GalleryManager.GAMES.get(target.getWorld().getUID());

				if (!MetadataUtils.checkOnceThisTick(GalleryManager.mPlugin, target, "GalleryCommandOneTick")) {
					return 1;
				}

				if (game != null) {
					game.printPlayerInfo(target);
				} else {
					target.sendMessage(ChatColor.GRAY + "the game should start in a bit");
				}
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new MultiLiteralArgument("player"),
				new MultiLiteralArgument("leave"),
				new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player target = (Player) args[2];
				GalleryGame game = GalleryManager.GAMES.get(target.getWorld().getUID());
				if (game != null) {
					game.playerLeave(target);
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new MultiLiteralArgument("player"),
				new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("alive"),
				new BooleanArgument("true/false"))
			.executes((sender, args) -> {
				boolean alive = (boolean) args[4];
				GalleryGame game = null;
				for (Player player : new ArrayList<>((Collection<Player>) args[1])) {
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
		Argument util = new MultiLiteralArgument("utils");


		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("moderation"),
				new MultiLiteralArgument("get"),
				new MultiLiteralArgument("info"))
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
				new MultiLiteralArgument("moderation"),
				new MultiLiteralArgument("get"),
				new MultiLiteralArgument("info"),
				new EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
			.executesPlayer((moderator, args) -> {
				GalleryGame game = getGameFromSender(moderator);
				if (game != null) {
					game.printModerationInfo(moderator, (Player) args[4]);
				}
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("give"),
				new MultiLiteralArgument("coin"),
				new IntegerArgument("coins"))
			.executes((sender, args) -> {
				int coins = (int) args[3];
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
				new MultiLiteralArgument("give"),
				new MultiLiteralArgument("effect"),
				new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
				new MultiLiteralArgument(Arrays.stream(GalleryEffectType.values()).map(Enum::name).toList().toArray(new String[0])))
			.executes((sender, args) -> {
				GalleryEffectType type = GalleryEffectType.fromName((String) args[4]);
				GalleryGame game = getGameFromSender(sender);
				for (Player entity : (Collection<Player>) args[3]) {
					if (game == null) {
						game = getGameFromEntity(entity);
					}
					game.getGalleryPlayer(entity.getUniqueId()).giveEffect(type.newEffect());
				}

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("get"),
				new MultiLiteralArgument("round"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game != null) {
					return game.getCurrentRound();
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("box"),
				new MultiLiteralArgument("get"),
				new MultiLiteralArgument("locations"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				List<Location> locs = game.getBoxLocations();

				sender.sendMessage("Found " + locs.size() + " possible box location");
				sender.sendMessage("----------------------------------------------");
				for (Location location : locs) {
					sender.sendMessage("loc " + location.toVector());
				}
				sender.sendMessage("----------------------------------------------");
				sender.sendMessage("Active box at: " + game.getBoxLocation().toVector());

			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("box"),
				new MultiLiteralArgument("move"),
				new MultiLiteralArgument("random"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				game.moveBoxAtRandomLocation();

			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("get"),
				new MultiLiteralArgument("interactable"),
				new MultiLiteralArgument("position"),
				new StringArgument("name"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game != null) {
					BaseInteractable interactable = game.getInteractable((String) args[4]);
					if (interactable == null) {
						sender.sendMessage("Interactable null!!! no match for name: " + args[4]);
						return -1;
					}
					sender.sendMessage("Interactable " + interactable.getName() + " at Pos: " + interactable.getLocation());
					return 1;
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("spawner"),
				new MultiLiteralArgument("activate"),
				new StringArgument("spawnerName"),
				new BooleanArgument("active"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				boolean active = (boolean) args[4];
				if (game != null) {
					game.setActiveSpawner((String) args[3], active);
					return 1;
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("interactable"),
				new StringArgument("name"),
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("interactable"),
				new BooleanArgument("isInteractableOrNot"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game != null) {
					game.setInteractable((String) args[2], (Boolean) args[5]);
					return 1;
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("interactable"),
				new StringArgument("name"),
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("interactable"),
				new MultiLiteralArgument("showingText"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game != null) {
					game.setInteractableText((String) args[2], ((String) args[5]).isEmpty() ? null : (String) args[5]);
					return 1;
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("despawn"),
				new MultiLiteralArgument("mobs"),
				new EntitySelectorArgument("mobs", EntitySelectorArgument.EntitySelector.MANY_ENTITIES))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				for (Entity entity : (Collection<Entity>) args[3]) {
					if (game == null) {
						game = getGameFromEntity(entity);
					}
					if (game != null && entity instanceof LivingEntity livingEntity) {
						game.despawnMob(livingEntity);
					}
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("interactable"),
				new StringArgument("name"),
				new MultiLiteralArgument("remove"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game != null) {
					game.removeInteractable((String) args[2]);
					return 1;
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("mobs"),
				new MultiLiteralArgument("spawning"),
				new BooleanArgument("True/False"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game != null) {
					game.setCanMobsSpawn((Boolean)args[4]);
					return 1;
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("reload"),
				new MultiLiteralArgument("all"))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game != null) {
					game.reloadAll();
					return 1;
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("load"),
				new EntitySelectorArgument("targets", EntitySelectorArgument.EntitySelector.MANY_ENTITIES))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				for (Entity entity : (Collection<Entity>) args[2]) {
					if (game == null) {
						game = getGameFromEntity(entity);
					}
					if (game != null) {
						game.load(entity);
					}
				}
				return -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				util,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("round"),
				new IntegerArgument("round", 0))
			.executes((sender, args) -> {
				GalleryGame game = getGameFromSender(sender);
				if (game != null) {
					game.setRound((int) args[3]);
					return 1;
				}
				return -1;
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
