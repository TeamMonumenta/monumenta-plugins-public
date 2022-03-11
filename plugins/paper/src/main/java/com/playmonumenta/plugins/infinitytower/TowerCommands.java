package com.playmonumenta.plugins.infinitytower;

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
import java.util.ArrayList;
import java.util.List;
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
		List<Argument> arguments = new ArrayList<>();

		Argument mobLosNameArgument = new StringArgument("los name").includeSuggestions(info -> TowerFileUtils.TOWER_MOBS_INFO.stream().map(item -> item.mLosName).toArray(String[]::new));

		arguments.add(new MultiLiteralArgument("reload"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
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

		arguments.clear();
		arguments.add(new MultiLiteralArgument("create"));
		arguments.add(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
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


		arguments.clear();
		arguments.add(new MultiLiteralArgument("buy"));
		arguments.add(new MultiLiteralArgument("mobs"));
		arguments.add(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[2];
				TowerManager.GAMES.get(player.getUniqueId()).buyMobs();
				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("open"));
		arguments.add(new MultiLiteralArgument("team"));
		arguments.add(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[2];
				new TowerGuiTeam(player, TowerManager.GAMES.get(player.getUniqueId())).openInventory(player, plugin);

				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("start"));
		arguments.add(new MultiLiteralArgument("turn"));
		arguments.add(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[2];
				TowerManager.GAMES.get(player.getUniqueId()).startTurn();
				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("game"));
		arguments.add(new MultiLiteralArgument("stop"));
		arguments.add(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[2];
				if (TowerManager.GAMES.get(player.getUniqueId()) != null) {
					TowerManager.GAMES.get(player.getUniqueId()).forceStopGame();
				} else {
					//someone trapped? remove the tags to be sure
					TowerGame.clearPlayer(player);
				}

				return 1;
			}).register();

		//command for debuging, not used by players!
		arguments.clear();
		arguments.add(new MultiLiteralArgument("add"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				TowerMobInfo info = TowerFileUtils.getMobInfo((String)args[2]);
				if (info == null) {
					CommandAPI.fail("Can't find a match for this bos: " + (String)args[2]);
				}
				TowerManager.GAMES.get(player.getUniqueId()).addNewMob(info);
				return 1;
			}).register();
	}

	public static void registerDesign(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.infinitytower");
		List<Argument> arguments = new ArrayList<>();

		Argument mobLosNameArgument = new StringArgument("los name").includeSuggestions(info -> TowerFileUtils.TOWER_MOBS_INFO.stream().map(item -> item.mLosName).toArray(String[]::new));

		arguments.clear();
		arguments.add(new MultiLiteralArgument("auto"));
		arguments.add(new MultiLiteralArgument("update"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				updateMobs(player);
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("show"));
		arguments.add(new MultiLiteralArgument("mobs"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				new TowerGuiShowMobs(player).openInventory(player, plugin);
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("floor"));
		arguments.add(new IntegerArgument("floor"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				new TowerGuiFloorDesign(player, (Integer) args[2] - 1).openInventory(player, plugin);
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("new"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(new StringArgument("los name").includeSuggestions((sug) -> {
			return LibraryOfSoulsIntegration.getSoulNames().toArray(new String[0]);
		}));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				createNewTowerMob(player, (String)args[3]);
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("remove"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {

				TowerMobInfo info = TowerFileUtils.getMobInfo((String) args[3]);
				if (info == null) {
					CommandAPI.fail("No mob with: " + ((String) args[3]));
				}
				TowerGameUtils.setMainHandItem(player, info);

				TowerFileUtils.removeMobInfo(info);

				TowerGameUtils.sendMessage(player, "Mob removed, saving all the files");

				TowerFileUtils.savePlayerTower();
				TowerFileUtils.saveDefaultTower();
				TowerFileUtils.saveTowerMobs();

				TowerGameUtils.sendMessage(player, "Files saved!");
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("purchasable"));
		arguments.add(new BooleanArgument("purchasable"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				TowerMobInfo info = TowerFileUtils.getMobInfo((String) args[3]);
				if (info == null) {
					CommandAPI.fail("No mob with: " + ((String) args[3]));
				}
				TowerGameUtils.setMainHandItem(player, info);

				info.mBuyable = (Boolean) args[5];

				if (info.mBuyable) {
					TowerFileUtils.TOWER_MOBS_RARITY_MAP.get(info.mMobRarity).add(info);
					TowerGameUtils.sendMessage(player, "This mob is now purchasable");
				} else {
					TowerFileUtils.TOWER_MOBS_RARITY_MAP.get(info.mMobRarity).remove(info);
					TowerGameUtils.sendMessage(player, "This mob is NOT now purchasable");
				}

				return 1;
			}).register();


		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("health"));
		arguments.add(new DoubleArgument("health"));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				double hp = (double)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}

				item.mMobStats.mHP = hp;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.sendMessage(player, "You may also want to change the value inside the los..");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("atk"));
		arguments.add(new DoubleArgument("atk"));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				double atk = (double)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}

				item.mMobStats.mAtk = atk;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.sendMessage(player, "You may also want to change the value inside the los..");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();


		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("ability"));
		arguments.add(new MultiLiteralArgument("add"));
		arguments.add(new GreedyStringArgument("Mob Ability").replaceSuggestions((info) -> {
			String[] abilities = new String[TowerMobAbility.ABILITIES.size()];
			int i = 0;
			for (TowerMobAbility.Tuple tuple : TowerMobAbility.ABILITIES) {
				abilities[i++] = tuple.mName;
			}
			return abilities;
		}));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				String ability = (String)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}

				item.mAbilities.add(ability);

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("ability"));
		arguments.add(new MultiLiteralArgument("remove"));
		arguments.add(new GreedyStringArgument("Mob Ability").replaceSuggestions((info) -> {
			String[] abilities = new String[TowerMobAbility.ABILITIES.size()];
			int i = 0;
			for (TowerMobAbility.Tuple tuple : TowerMobAbility.ABILITIES) {
				abilities[i++] = tuple.mName;
			}
			return abilities;
		}));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				String ability = (String)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}

				item.mAbilities.remove(ability);

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("price"));
		arguments.add(new IntegerArgument("price"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				int cost = (int)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}
				item.mMobStats.mCost = cost;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("weight"));
		arguments.add(new IntegerArgument("weight"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				int weight = (int)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}
				item.mMobStats.mWeight = weight;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();


		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("limit"));
		arguments.add(new IntegerArgument("limit", 0, 10));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				int limit = (int)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}
				item.mMobStats.mLimit = limit;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();


		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("lore"));
		arguments.add(new GreedyStringArgument("lore"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				String lore = (String)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}
				item.mLore = lore;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("name"));
		arguments.add(new StringArgument("name"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				String name = (String)args[5];
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}
				item.mDisplayName = name;

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("item"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				ItemStack stack = player.getInventory().getItemInMainHand();
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
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

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("rarity"));
		arguments.add(new MultiLiteralArgument(
			TowerMobRarity.COMMON.name(),
			TowerMobRarity.RARE.name(),
			TowerMobRarity.EPIC.name(),
			TowerMobRarity.LEGENDARY.name()
		));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];
				TowerMobRarity rarity = TowerMobRarity.valueOf((String)args[5]);
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
				}
				TowerFileUtils.TOWER_MOBS_RARITY_MAP.get(item.mMobRarity).remove(item);
				item.mMobRarity = rarity;
				TowerFileUtils.TOWER_MOBS_RARITY_MAP.get(item.mMobRarity).add(item);

				TowerFileUtils.updateItem(item);

				TowerGameUtils.sendMessage(player, "Change made!");
				TowerGameUtils.setMainHandItem(player, item);

				return 1;
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modify"));
		arguments.add(new MultiLiteralArgument("mob"));
		arguments.add(mobLosNameArgument);
		arguments.add(new MultiLiteralArgument("set"));
		arguments.add(new MultiLiteralArgument("class"));
		arguments.add(new MultiLiteralArgument(
			TowerMobClass.CASTER.getName(),
			TowerMobClass.PROTECTOR.getName(),
			TowerMobClass.FIGHTER.getName(),
			TowerMobClass.SPECIAL.getName()
		));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String mob = (String)args[2];

				TowerMobClass mobClass = TowerMobClass.getFromName((String)args[5]);
				TowerMobInfo item = TowerFileUtils.getMobInfo(mob);

				if (item == null) {
					CommandAPI.fail("This mob don't exist");
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
