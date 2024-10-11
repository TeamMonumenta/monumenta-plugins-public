package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.GenericCommand;
import com.playmonumenta.plugins.depths.charmfactory.CharmFactory;
import com.playmonumenta.plugins.depths.guis.DepthsDebugGUI;
import com.playmonumenta.plugins.depths.guis.DepthsGUICommands;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.arguments.UUIDArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DepthsCommand extends GenericCommand {

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.depths");

		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		Argument<String> abilityArg = new TextArgument("Ability Name")
			.replaceSuggestions(ArgumentSuggestions.strings(DepthsManager.getAbilities().stream().map(a -> '"' + a.getDisplayName() + '"').toList()));
		IntegerArgument rarityArg = new IntegerArgument("Rarity", 0, 6);
		LocationArgument locationArg = new LocationArgument("loc", LocationType.BLOCK_POSITION);
		IntegerArgument amountArg = new IntegerArgument("amount");
		IntegerArgument wheelResultArg = new IntegerArgument("result", 1, 6);
		IntegerArgument charmRarityArg = new IntegerArgument("rarity", 1, 5);
		IntegerArgument treeArg = new IntegerArgument("tree");

		List<Argument<?>> arguments = new ArrayList<>();

		//INIT COMMAND - loads nearby players into one party, or joins new player to an existing one
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("init"))
			.withArguments(playerArg)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);

				DepthsManager.getInstance().init(player);
			})
			.register();

		//ABILITY COMMAND - debug to set a player's level in an ability directly
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("ability"))
			.withArguments(playerArg)
			.withArguments(abilityArg)
			.withOptionalArguments(rarityArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);

				DepthsManager.getInstance().setPlayerLevelInAbility(args.getByArgument(abilityArg), player, args.getByArgumentOrDefault(rarityArg, 1));
			}).register();

		//DELETE COMMAND - removes a player from the depths system and clears their data
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("delete"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				DepthsManager.getInstance().deletePlayer(player);
				player.sendMessage("Removed you from depths system");
			}).register();

		//PARTY COMMAND - prints a summary string of party information to the player
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("party"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);

				player.sendMessage(DepthsManager.getInstance().getPartySummary(player));
			}).register();

		//SPAWN COMMAND - ran at the end of each room to load the next one dynamically
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("spawn"))
			.withArguments(playerArg)
			.withOptionalArguments(locationArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				DepthsManager.getInstance().gotRoomEndpoint(player, args.getByArgumentOrDefault(locationArg, player.getLocation()));
			}).register();

		//WEAPON COMMAND - run once by each player to select their weapon aspect
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("weapon"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				DepthsGUICommands.weaponAspect(player);
			}).register();

		//TREASURE COMMAND - run to increase the party's treasure score by given amount (used in select rooms)
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("treasure"))
			.withArguments(playerArg)
			.withArguments(amountArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);

				DepthsManager.getInstance().incrementTreasure(null, player, args.getByArgument(amountArg));
			}).register();

		//CHAOS COMMAND - run in select utility room to replace one player's ability with two others
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("chaos"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);

				DepthsManager.getInstance().chaos(player);
			}).register();

		//WHEEL COMMAND - run in select utility room to trigger a curious effect depending on the result
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("wheel"))
			.withArguments(playerArg)
			.withArguments(wheelResultArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);

				DepthsManager.getInstance().wheel(player, args.getByArgument(wheelResultArg));
			}).register();

		//FLOOR COMMAND - debug command to manually send a party to the next floor
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("floor"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);

				DepthsManager.getInstance().goToNextFloor(player);
			}).register();

		//BOSSFIGHT COMMAND - run by boss rooms to spawn the boss and initiate combat
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("bossfight"))
			.withArguments(playerArg)
			.withOptionalArguments(locationArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				DepthsManager.getInstance().startBossFight(player, args.getByArgumentOrDefault(locationArg, player.getLocation()));
			}).register();

		//ROOM COMMAND - debug command to manually set the party's room number to specified value
		arguments.clear();
		arguments.add(new LiteralArgument("room"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("amount"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("room"))
			.withArguments(playerArg)
			.withArguments(amountArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);

				DepthsManager.getInstance().setRoomDebug(player, args.getByArgument(amountArg));
			}).register();

		//DEBUG COMMAND - usable by moderators to see a GUI with players' information
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("debug"))
			.withArguments(playerArg)
			.executesPlayer((sender, args) -> {
				Player targetPlayer = args.getUnchecked("target player");

				new DepthsDebugGUI(sender, targetPlayer, plugin).openInventory(sender, plugin);
			}).register();

		//CHARMGEN COMMAND - debug command to gen charmfactory charm
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("charmfactory"))
			.withArguments(new LiteralArgument("create"))
			.withArguments(playerArg)
			.withArguments(charmRarityArg)
			.withArguments(treeArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				Location loc = player.getLocation();
				int power = new Random().nextInt(5) + 1;
				ItemStack charm = CharmFactory.generateCharm(args.getByArgument(charmRarityArg), power, 0, null, null, null, null, null, args.getByArgument(treeArg), false);
				player.sendMessage("DEBUG SEED " + Objects.requireNonNull(ItemStatUtils.getPlayerModified(new NBTItem(charm))).getLong(CharmFactory.CHARM_UUID_KEY));
				loc.getWorld().dropItem(loc, charm);
			}).register();

		//CHARMGEN COMMAND - debug command to update charmfactory charm
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("charmfactory"))
			.withArguments(new LiteralArgument("update"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				ItemStack item = player.getInventory().getItemInMainHand();

				ItemStack newCharm = CharmFactory.updateCharm(item);
				if (newCharm != null) {
					player.getInventory().setItemInMainHand(newCharm);
					item.setAmount(0);
					player.updateInventory();
					player.sendMessage("DEBUG SEED " + Objects.requireNonNull(ItemStatUtils.getPlayerModified(new NBTItem(newCharm))).getLong(CharmFactory.CHARM_UUID_KEY));
				}
			}).register();

		//CHECK ENDLESS COMMAND - mech command that returns 1 if endless, else 0
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("check"))
			.withArguments(new LiteralArgument("endless"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);

				return DepthsManager.getInstance().getIsEndless(player) ? 1 : 0;
			}).register();

		// UNSTUCK COMMAND - moderator command to be used if a party somehow gets stuck in the state of loading a room and can't choose a new one
		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(new LiteralArgument("unstuck"))
			.withArguments(playerArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				DepthsParty party = DepthsManager.getInstance().getDepthsParty(player);
				if (party != null) {
					party.mIsLoadingRoom = false;
				}
			}).register();

		String sacrificeBypass = "monumenta.command.depths.bypassnosacrifice";
		new CommandAPICommand("zenithabandon")
			.withArguments(new UUIDArgument("player uuid"))
			.executes((sender, args) -> {
				UUID player = (UUID) args.get("player uuid");
				DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
				if (dp == null) {
					return;
				}
				if (sender instanceof Player playerSender) {

					// This cast is guaranteed to succeed.
					if (dp.mZenithAbandonedByParty || (!playerSender.hasMetadata(DepthsManager.ZENITH_ABANDONABLE_PLAYERS_METADATA_KEY)
						|| !((List<UUID>) playerSender.getMetadata(DepthsManager.ZENITH_ABANDONABLE_PLAYERS_METADATA_KEY).get(0).value()).contains(player) && !playerSender.hasPermission(sacrificeBypass))) {
						DepthsUtils.sendFormattedMessage(playerSender, dp.getContent(), "You cannot do this right now.");
						return;
					}
					dp.mZenithAbandonedByParty = true;
					dp.mFinalTreasureScore = Objects.requireNonNull(DepthsManager.getInstance().getPartyFromId(dp)).mTreasureScore; // Abandoned players no longer acquire treasure score.
					DepthsUtils.sendFormattedMessage(playerSender, dp.getContent(), "It is done...");
					playerSender.playSound(playerSender.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
					playerSender.removeMetadata(DepthsManager.ZENITH_ABANDONABLE_PLAYERS_METADATA_KEY, Plugin.getInstance());
				}

			}).register();
	}
}
