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
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TowerCommands {

	public static final String COMMAND = "infinitytower";

	public static void register(Plugin plugin) {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.infinitytower");

		Argument mobLosNameArgument = new StringArgument("los name").includeSuggestions(info -> TowerFileUtils.TOWER_MOBS_INFO.stream().map(item -> item.mLosName).toArray(String[]::new));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(new MultiLiteralArgument("reload"))
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
				new MultiLiteralArgument("create"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER)
			).executes((sender, args) -> {
				Player player = (Player) args[1];
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
				new MultiLiteralArgument("buy"),
				new MultiLiteralArgument("mobs"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER)
			).executes((sender, args) -> {
				Player player = (Player) args[2];
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game == null) {
					CommandAPI.fail("No game in progress!");
					throw new RuntimeException();
				}
				game.buyMobs();
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("open"),
				new MultiLiteralArgument("team"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER)
			).executes((sender, args) -> {
				Player player = (Player) args[2];
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game == null) {
					CommandAPI.fail("No game in progress!");
					throw new RuntimeException();
				}
				new TowerGuiTeam(player, game).openInventory(player, plugin);

				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("start"),
				new MultiLiteralArgument("turn"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER)
			).executes((sender, args) -> {
				Player player = (Player) args[2];
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game == null) {
					CommandAPI.fail("No game in progress!");
					throw new RuntimeException();
				}
				game.startTurn();
				return 1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("game"),
				new MultiLiteralArgument("stop"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER)
			).executes((sender, args) -> {
				Player player = (Player) args[2];
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game != null) {
					game.stop();
				} else {
					//someone trapped? remove the tags to be sure
					TowerGame.clearPlayer(player);
				}
				return 1;
			}).register();

		//command for debuging, not used by players!
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("add"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument
			).executesPlayer((player, args) -> {
				TowerMobInfo info = TowerFileUtils.getMobInfo((String) args[2]);
				if (info == null) {
					CommandAPI.fail("Can't find a match for this bos: " + args[2]);
					throw new RuntimeException();
				}
				TowerGame game = TowerManager.GAMES.get(player.getUniqueId());
				if (game == null) {
					CommandAPI.fail("No game in progress!");
					throw new RuntimeException();
				}
				game.addNewMob(info);
				return 1;
			}).register();

		//commands for importing/exporting the InfinityTowerPlayer file from redis, not used by players!
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("redis"),
				new MultiLiteralArgument("importplayers")
			).executesPlayer((player, args) -> {
				JsonObject data = TowerFileUtils.readFile("InfinityTowerPlayer.json");
				if (data == null) {
					CommandAPI.fail("Failed to load InfinityTowerPlayer.json file from plugin folder");
					throw new RuntimeException();
				}
				TowerFileUtils.saveFileRedis(data, "InfinityTowerPlayer.json");
				player.sendMessage("Imported InfinityTowerPlayer.json from plugin folder -> redis");
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("redis"),
				new MultiLiteralArgument("exportplayers")
			).executesPlayer((player, args) -> {
				JsonObject data = TowerFileUtils.readFileRedis("InfinityTowerPlayer.json");
				if (data == null) {
					CommandAPI.fail("Failed to load InfinityTowerPlayer.json file from redis");
					throw new RuntimeException();
				}
				TowerFileUtils.saveFile(data, "InfinityTowerPlayer.json");
				player.sendMessage("Exported InfinityTowerPlayer.json from redis -> plugin folder");
			}).register();
	}

	public static void registerDesign(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.infinitytower");

		Argument mobLosNameArgument = new StringArgument("los name").includeSuggestions(info -> TowerFileUtils.TOWER_MOBS_INFO.stream().map(item -> item.mLosName).toArray(String[]::new));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("stats"),
				new MultiLiteralArgument("round")
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
				new MultiLiteralArgument("save"),
				new MultiLiteralArgument("all")
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
				new MultiLiteralArgument("auto"),
				new MultiLiteralArgument("update")
			).executesPlayer((player, args) -> {
				updateMobs(player);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("show"),
				new MultiLiteralArgument("mobs")
			).executesPlayer((player, args) -> {
				new TowerGuiShowMobs(player).openInventory(player, plugin);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("floor"),
				new IntegerArgument("floor")
			).executesPlayer((player, args) -> {
				new TowerGuiFloorDesign(player, (Integer) args[2] - 1).openInventory(player, plugin);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("new"),
				new MultiLiteralArgument("mob"),
				new StringArgument("los name").includeSuggestions((sug) -> {
					return LibraryOfSoulsIntegration.getSoulNames().toArray(new String[0]);
				})
			).executesPlayer((player, args) -> {
				createNewTowerMob(player, (String)args[3]);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("remove"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument
			).executesPlayer((player, args) -> {

				TowerMobInfo info = TowerFileUtils.getMobInfo((String) args[3]);
				if (info == null) {
					CommandAPI.fail("No mob with: " + ((String) args[3]));
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("purchasable"),
				new BooleanArgument("purchasable")
			).executesPlayer((player, args) -> {
				TowerMobInfo info = TowerFileUtils.getMobInfo((String) args[3]);
				if (info == null) {
					CommandAPI.fail("No mob with: " + ((String) args[3]));
					throw new RuntimeException();
				}
				TowerGameUtils.setMainHandItem(player, info);

				info.mBuyable = (Boolean) args[5];

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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("health"),
				new DoubleArgument("health")
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				double hp = (double)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("atk"),
				new DoubleArgument("atk")
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				double atk = (double)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("ability"),
				new MultiLiteralArgument("add"),
				new GreedyStringArgument("Mob Ability").replaceSuggestions((info) -> {
					String[] abilities = new String[TowerMobAbility.ABILITIES.size()];
					int i = 0;
					for (TowerMobAbility.Tuple tuple : TowerMobAbility.ABILITIES) {
						abilities[i++] = tuple.mName;
					}
					return abilities;
				})
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				String ability = (String)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("ability"),
				new MultiLiteralArgument("remove"),
				new GreedyStringArgument("Mob Ability").replaceSuggestions((info) -> {
					String[] abilities = new String[TowerMobAbility.ABILITIES.size()];
					int i = 0;
					for (TowerMobAbility.Tuple tuple : TowerMobAbility.ABILITIES) {
						abilities[i++] = tuple.mName;
					}
					return abilities;
				})
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				String ability = (String)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("price"),
				new IntegerArgument("price")
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				int cost = (int)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("weight"),
				new IntegerArgument("weight")
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				int weight = (int)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("limit"),
				new IntegerArgument("limit", 0, 10)
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				int limit = (int)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("lore"),
				new GreedyStringArgument("lore")
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				String lore = (String)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("name"),
				new StringArgument("name")
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				String name = (String)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob doesn't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("item")
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				ItemStack stack = player.getInventory().getItemInMainHand();
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob doesn't exist");
					throw new RuntimeException();
				}

				if (stack.getType() == Material.AIR) {
					CommandAPI.fail("You must hold an item!");
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("rarity"),
				new MultiLiteralArgument(
					TowerMobRarity.COMMON.name(),
					TowerMobRarity.RARE.name(),
					TowerMobRarity.EPIC.name(),
					TowerMobRarity.LEGENDARY.name()
				)
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];
				TowerMobRarity rarity = TowerMobRarity.valueOf((String)args[5]);
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob doesn't exist");
					throw new RuntimeException();
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
				new MultiLiteralArgument("modify"),
				new MultiLiteralArgument("mob"),
				mobLosNameArgument,
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("class"),
				new MultiLiteralArgument(
					TowerMobClass.CASTER.getName(),
					TowerMobClass.PROTECTOR.getName(),
					TowerMobClass.FIGHTER.getName(),
					TowerMobClass.SPECIAL.getName()
				)
			).executesPlayer((player, args) -> {
				String mob = (String)args[2];

				TowerMobClass mobClass = TowerMobClass.getFromName((String)args[5]);
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob doesn't exist");
					throw new RuntimeException();
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

			LivingEntity entity = (LivingEntity) soul.summon(player.getLocation().add(0, -100, 0));
			double hp = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double atk = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
			info.mMobStats.mAtk = atk;
			info.mMobStats.mHP = hp;
			TowerFileUtils.updateItem(info);
		}

		TowerGameUtils.sendMessage(player, "All Mobs updated! Total: " + TowerFileUtils.TOWER_MOBS_INFO.size());
	}


	public static void createNewTowerMob(Player sender, String mob) throws WrapperCommandSyntaxException {
		ItemStack item = sender.getInventory().getItemInMainHand();

		if (item.getType() == Material.AIR) {
			CommandAPI.fail("You MUST hold an item");
		}

		if (!TowerFileUtils.TOWER_MOBS_INFO.isEmpty()) {
			for (TowerMobInfo oldItem : TowerFileUtils.TOWER_MOBS_INFO) {
				if (oldItem.mLosName.equals(mob)) {
					sender.getInventory().addItem(oldItem.getBuyableItem());
					CommandAPI.fail("LoS already used: " + mob);
				}
			}
		}

		SoulEntry soul = SoulsDatabase.getInstance().getSoul(mob);

		LivingEntity entity = (LivingEntity) soul.summon(sender.getLocation().add(0, -100, 0));
		String mobName = ((TextComponent) entity.customName()).content();

		if (mobName.startsWith("IT")) {
			mobName = mobName.replace("IT", "");
		}
		double hp = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		double atk = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();

		TowerMobInfo newMob = new TowerMobInfo(mob, new TowerMobStats(atk, hp));
		entity.remove();

		newMob.mDisplayName = mobName;
		newMob.mBaseItem = item.clone();


		TowerGameUtils.sendMessage(sender, "New mob created! " + mobName);
		TowerFileUtils.updateItem(newMob);
		TowerGameUtils.setMainHandItem(sender, newMob);
	}
}
