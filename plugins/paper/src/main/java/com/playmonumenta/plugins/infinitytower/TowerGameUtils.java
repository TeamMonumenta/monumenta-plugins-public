package com.playmonumenta.plugins.infinitytower;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobClass;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobRarity;
import com.playmonumenta.plugins.infinitytower.mobs.abilities.GenericTowerMob;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TowerGameUtils {

	//-------------------------Sell/Buy Utils---------------------------------------

	public static void sellMob(TowerGame game, TowerMob mob) {
		game.removeMob(mob);

		int unitCost = mob.mInfo.mMobStats.mCost;
		int refund = (unitCost / 2 > 0 ? unitCost / 2 : 1);

		Player player = game.mPlayer.mPlayer;

		int coin = ScoreboardUtils.getScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME).orElse(0);
		ScoreboardUtils.setScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME, coin + refund);
	}

	public static boolean canBuy(TowerMobInfo info, Player player) {
		int cost = info.mMobStats.mCost;
		return canBuy(player, cost);
	}

	public static boolean canBuy(Player player, int cost) {
		int coin = ScoreboardUtils.getScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME).orElse(0);
		return coin >= cost;
	}

	public static void pay(TowerMobInfo info, Player player) {
		int cost = info.mMobStats.mCost;
		pay(player, cost);
	}

	public static void pay(Player player, int cost) {
		int coin = ScoreboardUtils.getScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME).orElse(0);
		coin = coin - cost;
		ScoreboardUtils.setScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME, coin);
	}

	public static boolean canBuyXP(TowerGame game, Player player) {
		if (game.mPlayerLevel < 10) {
			int cost = TowerConstants.LEVEL_COST[game.mPlayerLevel - 1];
			return canBuy(player, cost);
		}
		return false;
	}

	public static void upgradeLvl(TowerGame game, Player player) {
		if (game.mPlayerLevel < 10) {
			int cost = TowerConstants.LEVEL_COST[game.mPlayerLevel - 1];
			int coin = ScoreboardUtils.getScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME).orElse(0);
			coin = coin - cost;
			ScoreboardUtils.setScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME, coin);
			game.mPlayerLevel++;
			//game.mRoll++; no free roll when upgrade the level
		}
	}

	public static void addGold(Player player, int gold) {
		int coin = ScoreboardUtils.getScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME).orElse(0);
		ScoreboardUtils.setScoreboardValue(player, TowerConstants.COIN_SCOREBOARD_NAME, coin + gold);
	}

	public static void moveMob(TowerGame game, TowerMob mob) {
		Location playerLoc = game.mPlayer.mPlayer.getLocation();
		Location floorLoc = new Location(playerLoc.getWorld(), game.mFloor.mVector.getX(), game.mFloor.mVector.getY(), game.mFloor.mVector.getZ());
		double x = (playerLoc.getX() - floorLoc.getX());
		double y = (playerLoc.getY() - floorLoc.getY()) + 0.2;
		double z = (playerLoc.getZ() - floorLoc.getZ());
		z = Math.max(0.5, Math.min(z, game.mFloor.mZSize - 0.5));
		x = Math.max(0.5, Math.min(x, game.mFloor.mXSize - 0.5));
		mob.setLocation(x, y, z);
	}

	public static int getNextLevelCost(TowerMob towerMob) {
		int lvl = towerMob.mMobLevel;
		return (int) (Math.pow(2, towerMob.mInfo.mMobRarity.getIndex()) * lvl);
	}


	//----------------------Component for lore-----------------------------------

	private static final int MAX_LORE_LENGHT = 40;

	public static List<Component> getGenericLoreComponent(@Nullable String lore) {
		if (lore == null || lore.isEmpty()) {
			return List.of(Component.empty());
		}
		String[] splittedLore = lore.split(" ");

		List<Component> list = new ArrayList<>();
		Component text = Component.empty();
		int i = 0;
		for (String s : splittedLore) {
			if (i >= MAX_LORE_LENGHT) {
				i = 0;
				list.add(text);
				text = Component.empty();
			}
			text = text.append(Component.text(s + " ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			i += s.length() + 1;
		}

		if (i > 0) {
			list.add(text);
		}

		return list;
	}

	public static Component getRarityComponent(TowerMobRarity rarity) {
		return Component.text("Rarity :", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false).append(Component.text(" " + rarity.getName(), NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, false));
	}


	public static Component getClassComponent(TowerMobClass mMobClass) {
		return Component.text("Class :", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false).append(Component.text(" " + mMobClass.getName(), NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false));
	}

	public static Component getWeightComponent(int weight) {
		return Component.text("Weight :", NamedTextColor.DARK_GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false).append(Component.text(" " + weight, NamedTextColor.DARK_GREEN).decoration(TextDecoration.BOLD, false));
	}

	public static Component getAtkComponent(@Nullable Double atk) {
		return Component.text("Atk :", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false).append(Component.text(" " + atk + " ⚔", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, false));
	}

	public static Component getHpComponent(@Nullable Double hp) {
		return Component.text("HP :", NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false).append(Component.text(" " + hp + " ♥", NamedTextColor.BLUE).decoration(TextDecoration.BOLD, false));
	}

	public static Component getPriceComponent(int price) {
		return Component.text("Price :", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false).append(Component.text(" " + price + " c", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false));
	}

	public static Component getLimitComponent(int price) {
		return Component.text("Limit :", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false).append(Component.text(" " + price + " p", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false));
	}

	public static List<Component> getAbilityComponent(@Nullable List<String> abilities) {
		if (abilities == null || abilities.isEmpty()) {
			return List.of(Component.empty());
		}
		List<Component> list = new ArrayList<>();
		list.add(Component.text("Abilities: ", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		for (String ability : abilities) {
			list.add(Component.text(" - " + ability, NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
		}

		return list;
	}

	public static Component getLevelComponent(int level) {
		return Component.text("Level :", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false).append(Component.text(" " + level, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false));
	}

	public static Component getMobNameComponent(TowerMob towerMob, boolean playerMob) {
		return getMobNameComponent(towerMob.mInfo.mDisplayName, playerMob);
	}

	public static Component getMobNameComponent(String mobName, boolean playerMob) {
		if (playerMob) {
			return Component.empty().append(
				Component.text(mobName, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
		} else {
			return Component.empty().append(
				Component.text(mobName, NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
		}


	}

	//--------------------------------GUI ITEMS-----------------------------------------------------


	public static ItemStack getSignLvlItem(TowerGame game) {
		ItemStack item = new ItemStack(Material.OAK_SIGN);
		ItemMeta meta = item.getItemMeta();

		int playerLvl = game.mPlayerLevel;

		meta.displayName(Component.text("You are lvl: " + playerLvl, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();

		lore.add(Component.text("Mobs rarity percentage:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		for (TowerMobRarity rarity : TowerMobRarity.values()) {
			lore.add(Component.text(" " + rarity.getName() + ": " + (rarity.getWeight(playerLvl - 1) * 100) + "%", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		}

		meta.lore(lore);

		item.setItemMeta(meta);
		return item;
	}

	public static ItemStack getCoinItem(TowerGame game) {
		ItemStack item = new ItemStack(Material.GOLD_NUGGET);
		ItemMeta meta = item.getItemMeta();

		int coin = ScoreboardUtils.getScoreboardValue(game.mPlayer.mPlayer, "ITCoin").orElse(0);

		meta.displayName(Component.text("Coin: " + coin, NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

		item.setItemMeta(meta);
		return item;
	}

	public static ItemStack getXPItem(TowerGame game) {
		ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
		ItemMeta meta = item.getItemMeta();

		int currentlvl = game.mPlayerLevel;

		meta.displayName(Component.text("Lvl: " + currentlvl, NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();

		if (currentlvl < TowerConstants.PLAYER_MAX_LEVEL) {
			lore.add(Component.text("Pay " + TowerConstants.LEVEL_COST[currentlvl - 1] + " Coin to level up", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		}

		meta.lore(lore);

		item.setItemMeta(meta);
		return item;
	}

	public static ItemStack getWeightItem(TowerGame game) {
		ItemStack item = new ItemStack(Material.ANVIL);
		ItemMeta meta = item.getItemMeta();

		int currentWeight = game.mPlayer.mTeam.mCurrentSize;
		int maxWeight = game.mPlayerLevel * 3;

		meta.displayName(Component.text("Weight " + currentWeight + "/" + maxWeight, NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();

		if (game.mPlayerLevel < 10) {
			lore.addAll(getGenericLoreComponent("You can level up to obtain more space in your team"));
		}

		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}

	public static ItemStack getSellMobItem(TowerMob mob) {
		ItemStack item = new ItemStack(Material.FEATHER);
		ItemMeta meta = item.getItemMeta();

		meta.displayName(Component.text("Sell this unit!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));

		List<Component> lore = new ArrayList<>();

		int unitCost = mob.mInfo.mMobStats.mCost;
		int refund = (unitCost / 2 > 0 ? unitCost / 2 : 1);

		lore.add(Component.text("You will get " + refund + " Coin back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));

		meta.lore(lore);

		item.setItemMeta(meta);
		return item;
	}



	//---------------------------Game utils - giveloot and msg-------------------------------------

	public static void giveLoot(TowerGame game) {
		int floor = game.mCurrentFloor;

		int maxFloor = Math.max(floor + 1, ScoreboardUtils.getScoreboardValue(game.mPlayer.mPlayer, TowerConstants.SCOREBOARD_RESULT).orElse(0));
		ScoreboardUtils.setScoreboardValue(game.mPlayer.mPlayer, TowerConstants.SCOREBOARD_RESULT, maxFloor);
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "leaderboard update " + game.mPlayer.mPlayer.getName() + " Blitz");

		if (floor < 10) {
			return;
		}

		if (floor >= 50) {
			MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a [{\"text\":\"" + game.mPlayer.mPlayer.getName() + "\",\"color\":\"gold\",\"bold\":true,\"italic\":false},{\"text\":\" has reached the " + (floor + 1) + "th round in Plunderer's Blitz\",\"color\":\"white\",\"bold\":false}]");
		}

		if (floor > 50) {
			floor = 50;
		}
		int loot = (int) Math.pow(2, (floor / 10)) - 1;

		ItemStack stack = InventoryUtils.getItemFromLootTable(game.mPlayer.mPlayer, TowerConstants.COIN_LOOT_TABLE_KEY);
		if (stack == null) {
			game.mPlayer.mPlayer.sendMessage(Component.text("ERROR! no lootable for blitz_doubloon, contact a mod! " + loot + " missing", NamedTextColor.RED));
			return;
		}
		stack.setAmount(loot);
		game.mPlayer.mPlayer.getInventory().addItem(stack);

	}

	public static void sendMessage(Player player, String string) {
		player.sendMessage(Component.text("[Plunderer's Blitz] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(
			Component.text(string, NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)
		));
	}

	public static void setMainHandItem(Player sender, TowerMobInfo newMob) {
		sender.getInventory().setItemInMainHand(newMob.getBuyableItem());
	}


	//------------------------------------Damage utils----------------------------------------------------

	public static double getFinalDamage(DamageEvent event, LivingEntity damager, LivingEntity damagee, double startingDamage) {
		double finalDamage = startingDamage;
		Set<String> damagerTags = damager.getScoreboardTags();
		Set<String> damgaeeTags = damagee.getScoreboardTags();


		if ((damagerTags.contains(TowerConstants.MOB_TAG_CASTER) && damgaeeTags.contains(TowerConstants.MOB_TAG_DEFENDER)) || //casters do X% more damage to defenders
			(damagerTags.contains(TowerConstants.MOB_TAG_DEFENDER) && damgaeeTags.contains(TowerConstants.MOB_TAG_FIGHTER)) || //defenders do X% more damage to TowerFighter
			(damagerTags.contains(TowerConstants.MOB_TAG_FIGHTER) && damgaeeTags.contains(TowerConstants.MOB_TAG_CASTER))) { //TowerFighter do X% more damage to casters
			finalDamage *= TowerConstants.DAMAGE_MLT_CLASS;
		}

		double damageMult = 0.0;
		int lvl = 0;
		for (TowerMobRarity rarity : TowerMobRarity.values()) {
			for (String tag : damagerTags) {
				if (tag.startsWith(rarity.getTag() + "_")) {
					lvl = Integer.parseInt(tag.substring((rarity.getTag() + "_").length()));
					damageMult += rarity.getDamageMult() * (lvl - 1);
				}
			}
			for (String tag : damgaeeTags) {
				if (tag.startsWith(rarity.getTag() + "_")) {
					lvl = Integer.parseInt(tag.substring((rarity.getTag() + "_").length()));
					damageMult -= rarity.getDamageMult() * (lvl - 1);
				}
			}
		}

		finalDamage = finalDamage + (finalDamage * damageMult);

		event.setDamage(finalDamage);

		return finalDamage;

	}

	public static void startMob(LivingEntity mobSpawned, TowerMob mob, TowerGame game, boolean playerSummon) throws Exception {
		if (BossManager.getInstance() != null) {
			BossManager.getInstance().createBossInternal(mobSpawned, new GenericTowerMob(TowerManager.mPlugin, mobSpawned.getName(), (Mob) mobSpawned, game, mob, playerSummon));
		}
	}

}
