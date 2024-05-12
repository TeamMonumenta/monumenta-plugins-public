package com.playmonumenta.plugins.infinitytower;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.libraryofsouls.SoulEntry;
import com.playmonumenta.libraryofsouls.SoulsDatabase;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiFloorDesign;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiFloorDesignMob;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiShowMobs;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiTeam;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobAbility;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobClass;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobRarity;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobStats;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TowerCommands {

	public static final String COMMAND = "infinitytower";

	public static void register(Plugin plugin) {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.infinitytower");

		Argument<?> mobLosNameArgument = new StringArgument("los name").includeSuggestions(ArgumentSuggestions.strings(
			info -> TowerFileUtils.TOWER_MOBS_INFO.stream().map(item -> item.mLosName).toArray(String[]::new)));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(new LiteralArgument("reload"))
			.executesPlayer((player, args) -> {
				TowerGameUtils.sendMessage(player, "Reloading...");
				TowerConstants.SHOULD_GAME_START = true;
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					TowerFileUtils.loadTowerMobsInfo();
					TowerFileUtils.loadDefaultTeams();
					TowerFileUtils.loadPlayerTeams();
					TowerFileUtils.loadFloors();
					TowerGuiShowMobs.loadGuiItems();
					TowerGuiFloorDesignMob.loadGuiItems();
					TowerGameUtils.sendMessage(player, "Reload Completed!");
				});
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("create"),
				new EntitySelectorArgument.OnePlayer("player")
			).executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				UUID playerUUID = player.getUniqueId();

				if (!TowerConstants.SHOULD_GAME_START) {
					player.sendMessage(Component.text("Sorry there are some problems, try again later.", NamedTextColor.RED));
					return -1;
				}

				if (TowerManager.GAMES.containsKey(playerUUID)) {
					TowerFileUtils.warning("Trying to start a new game with a player that has another one? wtf");
					player.sendMessage(Component.text("Sorry there are some problems. Please contact a mod!", NamedTextColor.RED));
					return -1;
				}

				Integer nextID = TowerFileUtils.getNextID();
				if (nextID == null) {
					TowerFileUtils.warning("======Tried to start a game with full instance=====");
					player.sendMessage(Component.text("Sorry all the instance of this game are full, try again later.", NamedTextColor.RED));
					return -1;
				}

				TowerGame game = new TowerGame(nextID, player);
				TowerManager.GAMES.put(playerUUID, game);
				game.start();
				return 1;
			}).register();


		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("buy"),
				new LiteralArgument("mobs"),
				new EntitySelectorArgument.OnePlayer("player")
			).executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game == null) {
					throw CommandAPI.failWithString("No game in progress!");
				}
				game.buyMobs();
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("open"),
				new LiteralArgument("team"),
				new EntitySelectorArgument.OnePlayer("player")
			).executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game == null) {
					throw CommandAPI.failWithString("No game in progress!");
				}
				new TowerGuiTeam(player, game).openInventory(player, plugin);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("start"),
				new LiteralArgument("turn"),
				new EntitySelectorArgument.OnePlayer("player")
			).executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game == null) {
					throw CommandAPI.failWithString("No game in progress!");
				}
				game.startTurn();
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("game"),
				new LiteralArgument("stop"),
				new EntitySelectorArgument.OnePlayer("player")
			).executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game != null) {
					game.stop();
				} else {
					//someone trapped? remove the tags to be sure
					TowerGame.clearPlayer(player);
				}
				return 1;
			}).register();

		//command for debugging, not used by players!
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("add"),
				new LiteralArgument("mob"),
				mobLosNameArgument
			).executesPlayer((player, args) -> {
				String losName = args.getUnchecked("los name");
				TowerMobInfo info = TowerFileUtils.getMobInfo(losName);
				if (info == null) {
					throw CommandAPI.failWithString("Can't find a match for this bos: " + losName);
				}
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game == null) {
					throw CommandAPI.failWithString("No game in progress!");
				}
				game.addNewMob(info);
				return 1;
			}).register();

		//commands for importing/exporting the InfinityTowerPlayer file from redis, not used by players!
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("redis"),
				new LiteralArgument("importplayers")
			).executesPlayer((player, args) -> {
				JsonObject data = TowerFileUtils.readFile("InfinityTowerPlayer.json");
				if (data == null) {
					throw CommandAPI.failWithString("Failed to load InfinityTowerPlayer.json file from plugin folder");
				}
				TowerFileUtils.saveFileRedis(data, "InfinityTowerPlayer.json");
				player.sendMessage("Imported InfinityTowerPlayer.json from plugin folder -> redis");
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("redis"),
				new LiteralArgument("exportplayers")
			).executesPlayer((player, args) -> {
				JsonObject data = TowerFileUtils.readFileRedis("InfinityTowerPlayer.json");
				if (data == null) {
					throw CommandAPI.failWithString("Failed to load InfinityTowerPlayer.json file from redis");
				}
				TowerFileUtils.saveFile(data, "InfinityTowerPlayer.json");
				player.sendMessage("Exported InfinityTowerPlayer.json from redis -> plugin folder");
			}).register();
	}

	public static void registerDesign(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.infinitytower");

		Argument<?> mobLosNameArgument = new StringArgument("los name").includeSuggestions(ArgumentSuggestions.strings(
			info -> TowerFileUtils.TOWER_MOBS_INFO.stream().map(item -> item.mLosName).toArray(String[]::new)));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("stats"),
				new LiteralArgument("round")
			).executesPlayer((player, args) -> {

				JsonObject obj = new JsonObject();
				JsonArray arr = new JsonArray();
				obj.add("wavesStat", arr);

				int floor = 1;
				int weight = 0;
				int lvl = 0;
				int totalGold = TowerConstants.STARTING_GOLD;
				int totalHP = 0;
				double totalATK = 0;
				int totalMob = 0;
				int totalAbility = 0;
				JsonObject jb;
				for (TowerTeam round : TowerFileUtils.DEFAULT_TEAMS) {
					jb = new JsonObject();
					for (TowerMob mob : round.mMobs) {
						weight += mob.mInfo.mMobStats.mWeight;
						lvl += mob.mMobLevel;
						totalHP = (int) (totalHP + mob.mInfo.mMobStats.mHP);
						totalATK += mob.mInfo.mMobStats.mAtk;
						totalMob++;
						totalAbility += mob.mAbilities.size();
					}
					if (floor > 1) {
						totalGold += TowerConstants.getGoldWin(floor - 1);
					}
					jb.addProperty("Round", floor);
					jb.addProperty("Tw", weight);
					jb.addProperty("Tl", lvl);
					jb.addProperty("Thp", totalHP);
					jb.addProperty("Tatk", (int)totalATK);
					jb.addProperty("Tm", totalMob);
					jb.addProperty("Tab", totalAbility);
					jb.addProperty("PTg", totalGold);
					arr.add(jb);

					floor++;
					weight = 0;
					lvl = 0;
					totalHP = 0;
					totalATK = 0;
					totalMob = 0;
					totalAbility = 0;
				}
				TowerFileUtils.saveFile(obj, "TowerStats.json");


			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("save"),
				new LiteralArgument("all")
			).executesPlayer((player, args) -> {
				Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
					TowerFileUtils.saveDefaultTower();
					TowerFileUtils.saveTowerMobs();
					TowerGameUtils.sendMessage(player, "All file saved!");
				});
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("auto"),
				new LiteralArgument("update")
			).executesPlayer((player, args) -> {
				updateMobs(player);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("show"),
				new LiteralArgument("mobs")
			).executesPlayer((player, args) -> {
				new TowerGuiShowMobs(player).openInventory(player, plugin);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("floor"),
				new IntegerArgument("floor")
			).executesPlayer((player, args) -> {
				new TowerGuiFloorDesign(player, (Integer) args.get("floor") - 1).openInventory(player, plugin);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("new"),
				new LiteralArgument("mob"),
				new StringArgument("los name").includeSuggestions(ArgumentSuggestions.strings(
					(sug) -> LibraryOfSoulsIntegration.getSoulNames().toArray(new String[0])))
			).executesPlayer((player, args) -> {
				createNewTowerMob(player, args.getUnchecked("los name"));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("remove"),
				new LiteralArgument("mob"),
				mobLosNameArgument
			).executesPlayer((player, args) -> {
				String losName = args.getUnchecked("los name");
				TowerMobInfo info = TowerFileUtils.getMobInfo(losName);
				if (info == null) {
					throw CommandAPI.failWithString("No mob with: " + losName);
				}
				TowerGameUtils.setMainHandItem(player, info);

				TowerFileUtils.removeMobInfo(info);

				TowerGameUtils.sendMessage(player, "Mob removed, saving all the files");

				TowerFileUtils.savePlayerTower();
				TowerFileUtils.saveDefaultTower();
				TowerFileUtils.saveTowerMobs();

				TowerGameUtils.sendMessage(player, "Files saved!");
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("purchasable"),
				new BooleanArgument("purchasable")
			).executesPlayer((player, args) -> {
				String losName = args.getUnchecked("los name");
				TowerMobInfo info = TowerFileUtils.getMobInfo(losName);
				if (info == null) {
					throw CommandAPI.failWithString("No mob with: " + losName);
				}
				TowerGameUtils.setMainHandItem(player, info);

				info.mBuyable = args.getUnchecked("purchasable");

				if (info.mBuyable) {
					TowerFileUtils.getMobsByRarity(info.mMobRarity).add(info);
					TowerGameUtils.sendMessage(player, "This mob is now purchasable");
				} else {
					TowerFileUtils.getMobsByRarity(info.mMobRarity).remove(info);
					TowerGameUtils.sendMessage(player, "This mob is NOT now purchasable");
				}

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("health"),
				new DoubleArgument("health")
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				double hp = args.getUnchecked("health");
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob don't exist");
				}

				item.mMobStats.mHP = hp;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.sendMessage(player, "You may also want to change the value inside the los..");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("atk"),
				new DoubleArgument("atk")
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				double atk = args.getUnchecked("atk");
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob don't exist");
				}

				item.mMobStats.mAtk = atk;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.sendMessage(player, "You may also want to change the value inside the los..");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("ability"),
				new LiteralArgument("add"),
				new GreedyStringArgument("Mob Ability").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
					String[] abilities = new String[TowerMobAbility.ABILITIES.size()];
					int i = 0;
					for (TowerMobAbility.Tuple tuple : TowerMobAbility.ABILITIES) {
						abilities[i++] = tuple.mName;
					}
					return abilities;
				}))
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				String ability = args.getUnchecked("Mob Ability");
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob don't exist");
				}

				item.mAbilities.add(ability);

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("ability"),
				new LiteralArgument("remove"),
				new GreedyStringArgument("Mob Ability").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
					String[] abilities = new String[TowerMobAbility.ABILITIES.size()];
					int i = 0;
					for (TowerMobAbility.Tuple tuple : TowerMobAbility.ABILITIES) {
						abilities[i++] = tuple.mName;
					}
					return abilities;
				}))
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				String ability = args.getUnchecked("Mob Ability");
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob don't exist");
				}

				item.mAbilities.remove(ability);

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("price"),
				new IntegerArgument("price")
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				int cost = args.getUnchecked("price");
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob don't exist");
				}
				item.mMobStats.mCost = cost;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("weight"),
				new IntegerArgument("weight")
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				int weight = args.getUnchecked("weight");
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob don't exist");
				}
				item.mMobStats.mWeight = weight;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("limit"),
				new IntegerArgument("limit", 0, 10)
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				int limit = args.getUnchecked("limit");
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob don't exist");
				}
				item.mMobStats.mLimit = limit;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();


		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("lore"),
				new GreedyStringArgument("lore")
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				String lore = args.getUnchecked("lore");
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob don't exist");
				}
				item.mLore = lore;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("name"),
				new StringArgument("name")
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				String name = args.getUnchecked("name");
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob doesn't exist");
				}
				item.mDisplayName = name;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("item")
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				ItemStack stack = player.getInventory().getItemInMainHand();
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob doesn't exist");
				}

				if (stack.getType() == Material.AIR) {
					throw CommandAPI.failWithString("You must hold an item!");
				}
				item.mBaseItem = stack.clone();

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("rarity"),
				new MultiLiteralArgument("rarity",
					TowerMobRarity.COMMON.name(),
					TowerMobRarity.RARE.name(),
					TowerMobRarity.EPIC.name(),
					TowerMobRarity.LEGENDARY.name()
				)
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");
				TowerMobRarity rarity = TowerMobRarity.valueOf(args.getUnchecked("rarity"));
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob doesn't exist");
				}
				TowerFileUtils.getMobsByRarity(item.mMobRarity).remove(item);
				item.mMobRarity = rarity;
				TowerFileUtils.getMobsByRarity(item.mMobRarity).add(item);

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("modify"),
				new LiteralArgument("mob"),
				mobLosNameArgument,
				new LiteralArgument("set"),
				new LiteralArgument("class"),
				new MultiLiteralArgument("class",
					TowerMobClass.CASTER.getName(),
					TowerMobClass.PROTECTOR.getName(),
					TowerMobClass.FIGHTER.getName(),
					TowerMobClass.SPECIAL.getName()
				)
			).executesPlayer((player, args) -> {
				String mob = args.getUnchecked("los name");

				TowerMobClass mobClass = TowerMobClass.getFromName(args.getUnchecked("class"));
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					throw CommandAPI.failWithString("This mob doesn't exist");
				}
				item.mMobClass = mobClass;

				TowerFileUtils.updateItem(item);
				TowerGameUtils.setMainHandItem(player, item);
				return 1;
			}).register();
	}

	private static void updateMobs(Player player) {
		for (TowerMobInfo info : TowerFileUtils.TOWER_MOBS_INFO) {
			String losName = info.mLosName;
			SoulEntry soul = SoulsDatabase.getInstance().getSoul(losName);

			Attributable entity = (Attributable) soul.summon(player.getLocation().add(0, -100, 0));
			info.mMobStats.mHP = EntityUtils.getMaxHealth(entity);
			info.mMobStats.mAtk = EntityUtils.getAttributeOrDefault(entity, Attribute.GENERIC_ATTACK_DAMAGE, 0);
			TowerFileUtils.updateItem(info);
		}

		TowerGameUtils.sendMessage(player, "All Mobs updated! Total: " + TowerFileUtils.TOWER_MOBS_INFO.size());
	}


	public static void createNewTowerMob(Player sender, String mob) throws WrapperCommandSyntaxException {
		ItemStack item = sender.getInventory().getItemInMainHand();

		if (item.getType() == Material.AIR) {
			throw CommandAPI.failWithString("You MUST hold an item");
		}

		if (!TowerFileUtils.TOWER_MOBS_INFO.isEmpty()) {
			for (TowerMobInfo oldItem : TowerFileUtils.TOWER_MOBS_INFO) {
				if (oldItem.mLosName.equals(mob)) {
					sender.getInventory().addItem(oldItem.getBuyableItem());
					throw CommandAPI.failWithString("LoS already used: " + mob);
				}
			}
		}

		SoulEntry soul = SoulsDatabase.getInstance().getSoul(mob);

		LivingEntity entity = (LivingEntity) soul.summon(sender.getLocation().add(0, -100, 0));
		String mobName = ((TextComponent) entity.customName()).content();

		if (mobName.startsWith("IT")) {
			mobName = mobName.replace("IT", "");
		}
		double hp = EntityUtils.getMaxHealth(entity);
		double atk = EntityUtils.getAttributeOrDefault(entity, Attribute.GENERIC_ATTACK_DAMAGE, 0);

		TowerMobInfo newMob = new TowerMobInfo(mob, new TowerMobStats(atk, hp));
		entity.remove();

		newMob.mDisplayName = mobName;
		newMob.mBaseItem = item.clone();


		TowerGameUtils.sendMessage(sender, "New mob created! " + mobName);
		TowerFileUtils.updateItem(newMob);
		TowerGameUtils.setMainHandItem(sender, newMob);
	}
}
