package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvePreset;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.DungeonUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.RBoardAPI;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.AngleArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DungeonAccessCommand extends GenericCommand {

	@SuppressWarnings("unchecked")
	public static void register() {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.dungeonaccess");

		String[] dungeons = Arrays.stream(DungeonUtils.DungeonCommandMapping.values())
			.map(m -> m.name().toLowerCase(Locale.ROOT))
			.toArray(String[]::new);

		new CommandAPICommand("dungeonaccess")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("new"),
				new EntitySelectorArgument.OnePlayer("key player"),
				new EntitySelectorArgument.ManyPlayers("other players"),
				new StringArgument("dungeon").replaceSuggestions(ArgumentSuggestions.strings(dungeons)),
				new LocationArgument("return location"),
				new AngleArgument("return yaw"),
				new FloatArgument("return pitch")
			)
			.withOptionalArguments(
				new IntegerArgument("type"),
				new BooleanArgument("useDelvePreset")
			)
			.executes((sender, args) -> {
				startNew(args.getUnchecked("key player"), (Collection<Player>) args.get("other players"), getMapping(args.getUnchecked("dungeon")), args.getUnchecked("return location"), args.getUnchecked("return yaw"), args.getUnchecked("return pitch"), args.getOrDefaultUnchecked("type", 0), args.getOrDefaultUnchecked("useDelvePreset", false));
			})
			.register();

		new CommandAPICommand("dungeonaccess")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("invite"),
				new EntitySelectorArgument.OnePlayer("inviting player"),
				new EntitySelectorArgument.ManyPlayers("other players"),
				new StringArgument("dungeon").replaceSuggestions(ArgumentSuggestions.strings(dungeons)),
				new LocationArgument("return location"),
				new AngleArgument("return yaw"),
				new FloatArgument("return pitch")
			)
			.executes((sender, args) -> {
				invite(args.getUnchecked("inviting player"), (Collection<Player>) args.get("other players"), getMapping(args.getUnchecked("dungeon")), args.getUnchecked("return location"), args.getUnchecked("return yaw"), args.getUnchecked("return pitch"));
			})
			.register();

		new CommandAPICommand("dungeonaccess")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("send"),
				new EntitySelectorArgument.ManyPlayers("players"),
				new StringArgument("dungeon").replaceSuggestions(ArgumentSuggestions.strings(dungeons)),
				new LocationArgument("return location"),
				new AngleArgument("return yaw"),
				new FloatArgument("return pitch")
			)
			.executes((sender, args) -> {
				send((Collection<Player>) args.get("players"), getMapping(args.getUnchecked("dungeon")), args.getUnchecked("return location"), args.getUnchecked("return yaw"), args.getUnchecked("return pitch"));
			})
			.register();

		new CommandAPICommand("dungeonaccess")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("abandon"),
				new EntitySelectorArgument.ManyPlayers("players"),
				new StringArgument("dungeon").replaceSuggestions(ArgumentSuggestions.strings(dungeons))
			)
			.executes((sender, args) -> {
				getMapping(args.getUnchecked("dungeon")).forceAbandon((Collection<Player>) args.get("players"));
			})
			.register();
	}

	public static DungeonUtils.DungeonCommandMapping getMapping(String dungeon) throws WrapperCommandSyntaxException {
		try {
			return DungeonUtils.DungeonCommandMapping.valueOf(dungeon.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw CommandAPI.failWithString("Invalid dungeon '" + dungeon + "'");
		}
	}

	/**
	 * Opens a new instance of a dungeon for the key player (if they don't already have an instance), and then invites the other players into the new instance.
	 */
	public static void startNew(Player keyPlayer, Collection<Player> otherPlayersRaw, DungeonUtils.DungeonCommandMapping mapping, Location returnLocation, float returnYaw, float returnPitch, int type, final boolean useDelvePreset) throws WrapperCommandSyntaxException {

		if (ScoreboardUtils.getScoreboardValue(keyPlayer, mapping.getAccessName()).orElse(0) != 0) {
			throw CommandAPI.failWithString("You already have an open instance!");
		}

		final DelvePreset delvePreset = mapping.getDelvePreset();
		final String shardName = mapping.getShardName();
		if (useDelvePreset && (delvePreset == null || shardName == null)) {
			throw CommandAPI.failWithString("This dungeon doesn't have a delve preset!");
		}

		RBoardAPI.add("$Last", mapping.getAccessName(), 1).thenAccept(accessScore
			-> Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> { // must run on main server thread
			// Neither delvePreset nor shardName are null if useDelvePreset is true, but nullaway won't let me remove this
			if (useDelvePreset && delvePreset != null && shardName != null) {
				DelvesManager.savePlayerData(keyPlayer, shardName, delvePreset.mModifiers, delvePreset.mId);
			}
			if (mapping.getFinishedName() != null) {
				ScoreboardUtils.setScoreboardValue(keyPlayer, mapping.getFinishedName(), 0);
			}
			if (mapping.getTypeName() != null) {
				ScoreboardUtils.setScoreboardValue(keyPlayer, mapping.getTypeName(), type);
			}
			if (mapping.getStartDateName() != null) {
				int startDate = (int) DateUtils.getDaysSinceEpoch();
				ScoreboardUtils.setScoreboardValue(keyPlayer, mapping.getStartDateName(), startDate);
			}
			ScoreboardUtils.setScoreboardValue(keyPlayer, mapping.getAccessName(), (int) (long) accessScore);

			invite(keyPlayer, otherPlayersRaw, mapping, returnLocation, returnYaw, returnPitch);
		})).exceptionally(t -> {
			MMLog.warning("Could not get next free instance number for dungeon " + mapping.name().toLowerCase(Locale.ROOT) + ", player '" + keyPlayer.getName() + "' may need a key refund.", t);
			keyPlayer.sendMessage(Component.text("An error occurred! Please contact a moderator for a potential key refund.", NamedTextColor.RED));
			return null;
		});

	}

	/**
	 * Invites players to the dungeon instance of the inviting player, if they don't already have an instance. Copies delve mods, start date, dungeon type, etc. to the invited players.
	 * Does not perform a date check to see if invites are allowed.
	 */
	private static void invite(Player invitingPlayer, Collection<Player> otherPlayersRaw, DungeonUtils.DungeonCommandMapping mapping, Location returnLocation, float returnYaw, float returnPitch) {

		Set<Player> allPlayers = new HashSet<>(otherPlayersRaw);
		allPlayers.add(invitingPlayer);
		Set<Player> otherPlayers = new HashSet<>(otherPlayersRaw);
		otherPlayers.remove(invitingPlayer);

		int accessScore = ScoreboardUtils.getScoreboardValue(invitingPlayer, mapping.getAccessName()).orElseThrow();
		if (accessScore == 0) {
			// This shouldn't happen, but handle it anyway
			invitingPlayer.sendMessage(Component.text("You don't have an open " + mapping.getDisplayName() + " instance to invite other players to!", NamedTextColor.RED));
			return;
		}

		// Only invite players with no open instance
		for (Iterator<Player> iterator = otherPlayers.iterator(); iterator.hasNext(); ) {
			Player otherPlayer = iterator.next();
			int otherAccessScore = ScoreboardUtils.getScoreboardValue(otherPlayer, mapping.getAccessName()).orElse(0);
			if (otherAccessScore == accessScore) {
				// Same instance: send directly without changing any scores on the player
				iterator.remove();
				send(List.of(otherPlayer), mapping, returnLocation, returnYaw, returnPitch);
			} else if (otherAccessScore != 0) {
				// Other instance: send error message and don't send the player anywhere
				iterator.remove();
				otherPlayer.sendMessage(Component.text("You already have an open " + mapping.getDisplayName() + " instance!", NamedTextColor.RED));
				if (mapping.getStartDateName() != null) {
					int otherStartDate = ScoreboardUtils.getScoreboardValue(otherPlayer, mapping.getStartDateName()).orElse(0);
					if (DateUtils.getWeeklyVersion(otherStartDate) == DateUtils.getWeeklyVersion()) {
						otherPlayer.sendMessage(Component.text("Since you started your instance this week, you won't be able to abandon it to join.", NamedTextColor.GRAY));
					} else {
						otherPlayer.sendMessage(Component.text("Since your instance was started in a past week, you can abandon it to be invited again.", NamedTextColor.GRAY));
					}
				}
			}
		}

		if (!otherPlayers.isEmpty()) {
			int startDate = mapping.getStartDateName() == null ? 0 : ScoreboardUtils.getScoreboardValue(invitingPlayer, mapping.getStartDateName()).orElseThrow();
			int type = mapping.getTypeName() == null ? 0 : ScoreboardUtils.getScoreboardValue(invitingPlayer, mapping.getTypeName()).orElse(0);

			for (Player player : otherPlayers) {
				if (mapping.getFinishedName() != null) {
					ScoreboardUtils.setScoreboardValue(player, mapping.getFinishedName(), 0);
				}
				if (mapping.getStartDateName() != null) {
					ScoreboardUtils.setScoreboardValue(player, mapping.getStartDateName(), startDate);
				}
				if (mapping.getTypeName() != null) {
					ScoreboardUtils.setScoreboardValue(player, mapping.getTypeName(), type);
				}
				ScoreboardUtils.setScoreboardValue(player, mapping.getAccessName(), accessScore);
			}

			String shardName = mapping.getShardName();
			if (shardName != null && DelvesManager.DUNGEONS.contains(shardName)) {
				for (Player otherPlayer : otherPlayers) {
					DelvesUtils.copyDelvePoint(null, invitingPlayer, otherPlayer, shardName);
				}
			}
		}

		send(allPlayers, mapping, returnLocation, returnYaw, returnPitch);
	}

	/**
	 * Sends players to their existing instance for the given dungeon, if they have an instance.
	 */
	private static void send(Collection<Player> players, @Nullable DungeonUtils.DungeonCommandMapping mapping, Location returnLocation, float returnYaw, float returnPitch) {
		if (mapping == null) {
			MMLog.warning("Invalid dungeon mapping");
			return;
		}

		for (Iterator<Player> iterator = players.iterator(); iterator.hasNext(); ) {
			Player player = iterator.next();
			int accessScore = ScoreboardUtils.getScoreboardValue(player, mapping.getAccessName()).orElse(0);
			if (accessScore == 0) {
				iterator.remove();
				continue;
			}
			ScoreboardUtils.setScoreboardValue(player, "DAccess", accessScore);
		}

		String shardName = mapping.getShardName();
		if (shardName != null) {
			int numShards = ServerProperties.getShardCount(shardName);
			for (Player player : players) {
				int shardNum = 1 + (ScoreboardUtils.getScoreboardValue(player, mapping.getAccessName()).orElse(0) % numShards);
				String fullShard = shardName + (shardNum == 1 ? "" : "-" + shardNum);
				try {
					MonumentaRedisSyncAPI.sendPlayer(player, fullShard, returnLocation, returnYaw, returnPitch);
				} catch (Exception e) {
					MMLog.warning("Error while sending player " + player.getName() + " to " + fullShard, e);
				}
			}
		} else {
			returnLocation.setYaw(returnYaw);
			returnLocation.setPitch(returnPitch);
			for (Player player : players) {
				try {
					//send location update packet to players using the packet channel.
					if (ClientModHandler.playerHasClientMod(player)) {
						ClientModHandler.sendLocationPacket(player, mapping.getLocationName());
					}

					player.teleport(returnLocation);
					MonumentaWorldManagementAPI.sortWorld(player);
				} catch (Exception e) {
					MMLog.warning("Error while sorting player " + player.getName() + " into world", e);
				}
			}
		}
	}

	public static void send(Player player, @Nullable DungeonUtils.DungeonCommandMapping mapping, Location returnLocation) {
		send(List.of(player), mapping, returnLocation, returnLocation.getYaw(), returnLocation.getPitch());
	}

	public static void send(Player player, String dungeonName) throws WrapperCommandSyntaxException {
		DungeonUtils.DungeonCommandMapping mapping = DungeonUtils.DungeonCommandMapping.getByShard(dungeonName);
		if (mapping == null) {
			throw CommandAPI.failWithString("No such dungeon " + dungeonName);
		}
		send(player, mapping, player.getLocation());
	}

}
