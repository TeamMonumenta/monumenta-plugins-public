package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.ScoreHolderArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;

public class DelvesCommands {

	private static final String COMMAND = "delves";

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		String perms = "monumenta.command.delves";

		String[] delveModNames = new String[DelvesModifier.values().length];
		int i = 0;
		for (DelvesModifier mod : DelvesModifier.values()) {
			delveModNames[i++] = mod.name();
		}

		Argument<String> dungeonArg = new StringArgument("dungeon").includeSuggestions(
			ArgumentSuggestions.strings(info -> DelvesManager.DUNGEONS.toArray(new String[0])));
		Argument<String> dungeonArgOptional = new StringArgument("dungeon").includeSuggestions(
			ArgumentSuggestions.strings(info -> DelvesManager.DUNGEONS.toArray(new String[0])));
		Argument<?> delveModArg = new MultiLiteralArgument("mod", delveModNames);

		//this command is the old used to open Delve GUI
		new CommandAPICommand("opendmsgui")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				new DelveCustomInventory(player, args.getUnchecked("dungeon"), true).openInventory(player, plugin);
			}).register();

		new CommandAPICommand("openmoderatordmsgui")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument.OnePlayer("moderator"),
				new EntitySelectorArgument.OnePlayer("playerToDebug"),
				dungeonArg)
			.executes((sender, args) -> {
				new DelveCustomInventory(args.getUnchecked("playerToDebug"), args.getUnchecked("dungeon"), true).openInventory(args.getUnchecked("moderator"), plugin);
			}).register();

		new CommandAPICommand("opendpsgui")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg)
			.executes((sender, args) -> {
				new DelvePresetSelectionGui(args.getUnchecked("player"), args.getUnchecked("dungeon")).open();
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("show"),
				new LiteralArgument("mods"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				new DelveCustomInventory(player, args.getUnchecked("dungeon"), false).openInventory(player, plugin);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("utils"),
				new LiteralArgument("hasallpoints"),
				new EntitySelectorArgument.OnePlayer("player")
			).executes((commandSender, args) -> {
				Player player = args.getUnchecked("player");
				int currentPoint = DelvesUtils.getPlayerTotalDelvePoint(null, player, DelvesUtils.getDungeonName(player));
				return currentPoint >= DelvesUtils.MAX_DEPTH_POINTS ? 1 : -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("get"),
				new LiteralArgument("mod"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg,
				delveModArg
			).executes((commandSender, args) -> {
				return DelvesUtils.stampDelveInfo(commandSender, args.getUnchecked("player"), args.getUnchecked("dungeon"), DelvesModifier.fromName(args.getUnchecked("mod")));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("get"),
				new LiteralArgument("total"),
				new LiteralArgument("points"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg
			).executes((commandSender, args) -> {
				return DelvesUtils.getPlayerTotalDelvePoint(commandSender, args.getUnchecked("player"), args.getUnchecked("dungeon"));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("get"),
				new LiteralArgument("total"),
				new LiteralArgument("points"),
				new LiteralArgument("range")
			).executesPlayer((player, args) -> {
				return DelvesUtils.getTotalDelvePointInRange(player, player.getLocation());
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("set"),
				new LiteralArgument("mod"),
				new EntitySelectorArgument.ManyPlayers("entity"),
				dungeonArg,
				delveModArg,
				new IntegerArgument("rank", 0, 10)
			).executes((commandSender, args) -> {
				int rank = args.getUnchecked("rank");
				String dungeon = args.getUnchecked("dungeon");
				DelvesModifier mod = DelvesModifier.fromName(args.getUnchecked("mod"));
				for (Player target : (Collection<Player>) args.get("entity")) {
					DelvesUtils.setDelvePoint(commandSender, target, dungeon, mod, rank);
				}
				return rank;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("set"),
				new LiteralArgument("mod"),
				new EntitySelectorArgument.ManyPlayers("player"),
				dungeonArg,
				delveModArg,
				new LiteralArgument("score"),
				new ScoreHolderArgument.Single("score holder"),
				new ObjectiveArgument("objective")
			).executes((commandSender, args) -> {
				String dungeon = args.getUnchecked("dungeon");
				DelvesModifier mod = DelvesModifier.fromName(args.getUnchecked("mod"));
				String scoreHolder = args.getUnchecked("score holder");
				Objective objective = args.getUnchecked("objective");
				int rank = ScoreboardUtils.getScoreboardValue(scoreHolder, objective).orElse(0);
				rank = DelvesUtils.getMaxPointAssignable(mod, rank);
				for (Player target : ((Collection<Player>) args.get("player"))) {
					DelvesUtils.setDelvePoint(commandSender, target, dungeon, mod, rank);
				}
				return rank;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("random"),
				new LiteralArgument("mods"),
				new EntitySelectorArgument.ManyPlayers("player"),
				dungeonArg,
				new IntegerArgument("pointsToAssign", 0)
			).executes((commandSender, args) -> {
				List<Player> otherPlayers = new ArrayList<>(args.getUnchecked("player"));
				Player firstPlayer = otherPlayers.remove(0);
				String dungeon = args.getUnchecked("dungeon");
				DelvesUtils.assignRandomDelvePoints(firstPlayer, dungeon, args.getUnchecked("pointsToAssign"));
				for (Player target : otherPlayers) {
					DelvesUtils.copyDelvePoint(commandSender, firstPlayer, target, dungeon);
				}
				return DelvesUtils.getPlayerTotalDelvePoint(commandSender, firstPlayer, dungeon);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("clear"),
				new LiteralArgument("mods"),
				new EntitySelectorArgument.ManyPlayers("player")
			)
			.withOptionalArguments(dungeonArgOptional)
			.executes((commandSender, args) -> {
				int count = 0;
				for (Player target : ((Collection<Player>) args.get("player"))) {
					count++;
					DelvesUtils.clearDelvePlayerByShard(commandSender, target, args.getOrDefaultUnchecked("dungeon", DelvesUtils.getDungeonName(target)));
				}
				return count;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("update"),
				new LiteralArgument("scoreboard"),
				new EntitySelectorArgument.ManyEntities("player")
			).executes((commandSender, args) -> {
				for (Player target : (Collection<Player>) args.get("player")) {
					DelvesUtils.updateDelveScoreBoard(target);
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("copy"),
				new LiteralArgument("mods"),
				new EntitySelectorArgument.OnePlayer("copy player"),
				dungeonArg,
				new EntitySelectorArgument.ManyPlayers("players to copy")
			).executes((commandSender, args) -> {
				Player copyPlayer = args.getUnchecked("copy player");
				String dungeon = args.getUnchecked("dungeon");
				for (Player target : (Collection<Player>) args.get("players to copy")) {
					DelvesUtils.copyDelvePoint(commandSender, copyPlayer, target, dungeon);
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("copy"),
				new LiteralArgument("mods"),
				new EntitySelectorArgument.OnePlayer("copy player"),
				new EntitySelectorArgument.ManyPlayers("players to copy")
			).executes((commandSender, args) -> {
				Player copyFrom = args.getUnchecked("copy player");
				String dungeonName = DelvesUtils.getDungeonName(copyFrom);
				for (Player target : (Collection<Player>) args.get("players to copy")) {
					DelvesUtils.copyDelvePoint(commandSender, copyFrom, target, dungeonName);
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("validate"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg
			).executes((commandSender, args) -> {
				Player player = args.getUnchecked("player");
				String dungeon = args.getUnchecked("dungeon");
				return DelvesManager.validateDelvePreset(player, dungeon) ? 1 : 0;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("set"),
				new LiteralArgument("preset"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg,
				new GreedyStringArgument("preset").replaceSuggestions(ArgumentSuggestions.strings(Arrays.stream(DelvePreset.values()).map(p -> p.mName).toArray(String[]::new)))
			).executes((commandSender, args) -> {
				Player player = args.getUnchecked("player");
				String dungeon = args.getUnchecked("dungeon");
				String presetString = args.getUnchecked("preset");
				DelvePreset preset = DelvePreset.getDelvePreset(presetString);
				if (preset == null) {
					throw CommandAPI.failWithString("Unknown preset '" + presetString + "'");
				}
				DelvesManager.savePlayerData(player, dungeon, preset.mModifiers, preset.mId);
				if (commandSender != null && !(commandSender instanceof ProxiedCommandSender)) {
					commandSender.sendMessage(Component.text("Applied preset '" + preset.mName + "' to " + player.getName() + " for shard " + dungeon, NamedTextColor.GOLD));
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("spawnercheck"),
				new EntitySelectorArgument.OnePlayer("player"),
				new FloatArgument("percentage", 0, 100)
			).executes((commandSender, args) -> {
				Player player = args.getUnchecked("player");
				float percentage = args.getUnchecked("percentage");
				World world = player.getWorld();
				int broken = DelvesManager.getSpawnersBroken(world);
				int total = DelvesManager.getSpawnersTotal(world);
				if (broken < 0 || total < 0) {
					return 0;
				} else if (broken >= (percentage / 100.0) * total) {
					return 1;
				} else {
					return 0;
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("setbrokenspawners"),
				new EntitySelectorArgument.OnePlayer("player"),
				new IntegerArgument("broken", 0)
			).executes((commandSender, args) -> {
				Player player = args.getUnchecked("player");
				int broken = args.getUnchecked("broken");
				DelvesManager.setSpawnersBroken(player.getWorld(), broken);
				commandSender.sendMessage(Component.text("Set broken spawner count to " + broken, NamedTextColor.GOLD));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("nextrotation"),
				new IntegerArgument("week", 0)
			).executes((commandSender, args) -> {
				for (DelvesModifier mod : DelvesUtils.getWeeklyRotatingModifier(args.getUnchecked("week"))) {
					commandSender.sendMessage(Component.text(mod.name(), NamedTextColor.RED));
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("debugchallenge"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg
			).executes((commandSender, args) -> {
				Player player = args.getUnchecked("player");
				String dungeon = args.getUnchecked("dungeon");
				World world = player.getWorld();
				int broken = DelvesManager.getSpawnersBroken(world);
				int total = DelvesManager.getSpawnersTotal(world);
				boolean isChallenge = DelvesManager.validateDelvePreset(player, dungeon);
				if (!isChallenge) {
					commandSender.sendMessage(Component.text("Instance is not a challenge delve", NamedTextColor.RED));
					return;
				}
				commandSender.sendMessage(Component.text("Instance is a challenge delve", NamedTextColor.GREEN));
				commandSender.sendMessage(Component.text(String.format("Spawners broken: %d / %d (%d %%)", broken, total, 100 * broken / total), NamedTextColor.GOLD));
			}).register();
	}

}
