package com.playmonumenta.plugins.depths;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.depths.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.depths.abilities.aspects.AxeAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.RandomAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.ScytheAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.SwordAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.WandAspect;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.BottledSunlight;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.DepthsRejuvenation;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Enlightenment;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.LightningBottle;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.RadiantBlessing;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.SoothingCombos;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Sundrops;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.TotemOfSalvation;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.WardOfLight;
import com.playmonumenta.plugins.depths.abilities.earthbound.BrambleShell;
import com.playmonumenta.plugins.depths.abilities.earthbound.Bulwark;
import com.playmonumenta.plugins.depths.abilities.earthbound.CrushingEarth;
import com.playmonumenta.plugins.depths.abilities.earthbound.DepthsToughness;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenCombos;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenWrath;
import com.playmonumenta.plugins.depths.abilities.earthbound.Earthquake;
import com.playmonumenta.plugins.depths.abilities.earthbound.Entrench;
import com.playmonumenta.plugins.depths.abilities.earthbound.StoneSkin;
import com.playmonumenta.plugins.depths.abilities.earthbound.Taunt;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Apocalypse;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Detonation;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Fireball;
import com.playmonumenta.plugins.depths.abilities.flamecaller.FlameSpirit;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Flamestrike;
import com.playmonumenta.plugins.depths.abilities.flamecaller.PrimordialMastery;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Pyroblast;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Pyromania;
import com.playmonumenta.plugins.depths.abilities.flamecaller.RingOfFlames;
import com.playmonumenta.plugins.depths.abilities.flamecaller.VolcanicCombos;
import com.playmonumenta.plugins.depths.abilities.flamecaller.VolcanicMeteor;
import com.playmonumenta.plugins.depths.abilities.frostborn.Avalanche;
import com.playmonumenta.plugins.depths.abilities.frostborn.Cryobox;
import com.playmonumenta.plugins.depths.abilities.frostborn.DepthsFrostNova;
import com.playmonumenta.plugins.depths.abilities.frostborn.FrigidCombos;
import com.playmonumenta.plugins.depths.abilities.frostborn.FrozenDomain;
import com.playmonumenta.plugins.depths.abilities.frostborn.IceBarrier;
import com.playmonumenta.plugins.depths.abilities.frostborn.IceLance;
import com.playmonumenta.plugins.depths.abilities.frostborn.Icebreaker;
import com.playmonumenta.plugins.depths.abilities.frostborn.Permafrost;
import com.playmonumenta.plugins.depths.abilities.frostborn.PiercingCold;
import com.playmonumenta.plugins.depths.abilities.shadow.BladeFlurry;
import com.playmonumenta.plugins.depths.abilities.shadow.Brutalize;
import com.playmonumenta.plugins.depths.abilities.shadow.ChaosDagger;
import com.playmonumenta.plugins.depths.abilities.shadow.CloakOfShadows;
import com.playmonumenta.plugins.depths.abilities.shadow.DarkCombos;
import com.playmonumenta.plugins.depths.abilities.shadow.DeadlyStrike;
import com.playmonumenta.plugins.depths.abilities.shadow.DepthsAdvancingShadows;
import com.playmonumenta.plugins.depths.abilities.shadow.DepthsDethroner;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.depths.abilities.shadow.ShadowSlam;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsSharpshooter;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsSplitArrow;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsVolley;
import com.playmonumenta.plugins.depths.abilities.steelsage.FireworkBlast;
import com.playmonumenta.plugins.depths.abilities.steelsage.FocusedCombos;
import com.playmonumenta.plugins.depths.abilities.steelsage.Metalmancy;
import com.playmonumenta.plugins.depths.abilities.steelsage.ProjectileMastery;
import com.playmonumenta.plugins.depths.abilities.steelsage.RapidFire;
import com.playmonumenta.plugins.depths.abilities.steelsage.Scrapshot;
import com.playmonumenta.plugins.depths.abilities.steelsage.Sidearm;
import com.playmonumenta.plugins.depths.abilities.steelsage.SteelStallion;
import com.playmonumenta.plugins.depths.abilities.windwalker.Aeromancy;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsDodging;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsWindWalk;
import com.playmonumenta.plugins.depths.abilities.windwalker.GuardingBolt;
import com.playmonumenta.plugins.depths.abilities.windwalker.HowlingWinds;
import com.playmonumenta.plugins.depths.abilities.windwalker.LastBreath;
import com.playmonumenta.plugins.depths.abilities.windwalker.OneWithTheWind;
import com.playmonumenta.plugins.depths.abilities.windwalker.RestoringDraft;
import com.playmonumenta.plugins.depths.abilities.windwalker.Skyhook;
import com.playmonumenta.plugins.depths.abilities.windwalker.Slipstream;
import com.playmonumenta.plugins.depths.abilities.windwalker.Whirlwind;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * This is the main brain of the depths plugin, responsible for handling all interactions from
 * depths commands, events, player and party interaction.
 *
 * Anything from the playable world to the plugin goes through here and is then delegated to
 * other classes in the main package.
 *
 * @author ShadowVisions
 */
public class DepthsManager {

	//The max num of trees the player can have in a run
	public static final int NUM_TREES_PER_RUN = 4;

	//How often players on glass take damage
	public static final int GLASS_DAMAGE_TICK_INTERVAL = 20;

	//The percentage of spawners that must be broken in a room in order to give the reward
	public static final double ROOM_SPAWNER_PERCENT = 0.85;

	//Tag to check for endless mode
	public static final String ENDLESS_MODE_STRING = "DepthsEndlessMode";

	//Tag to check for rigged talisman (in endless mode)
	public static final String RIGGED_STRING = "DepthsRigged";

	//Treasure score amount for defeating rooms
	public static final int TREASURE_PER_NORMAL = 3;
	public static final int TREASURE_PER_ELITE = 5;
	//Will be multiplied by the current floor number the party is on, ex. f2 is 5 * 2
	public static final int TREASURE_PER_FLOOR = 8;
	//How often the depths data is saved normally
	public static final int SAVE_INTERVAL = 60 * 5 * 20; //5 min

	//Boss soul names
	public static final String HEDERA_LOS = "HederaVenomoftheWaves";
	public static final String DAVEY_LOS = "LieutenantDaveyVoidHerald";
	public static final String NUCLEUS_LOS = "Gyrhaeddant";

	public static final String PLAYER_SPAWN_STAND_NAME = "PlayerSpawn";

	public static final String FLOOR_LOBBY_LOAD_STAND_NAME = "LobbyLoadPoint";

	public static final String LOOT_ROOM_STAND_NAME = "DepthsLootRoom";

	public static final String PAID_SCOREBOARD_TAG = "DepthsWeaponAspectUpgradeBought";

	public static final EnumSet<DepthsRoomType> mRoomOptionsWithoutBoss = EnumSet.of(DepthsRoomType.ABILITY,
	                                                                                 DepthsRoomType.ABILITY_ELITE, DepthsRoomType.TREASURE, DepthsRoomType.TREASURE_ELITE,
	                                                                                 DepthsRoomType.UPGRADE, DepthsRoomType.UPGRADE_ELITE, DepthsRoomType.UTILITY);

	// Singleton implementation
	private static @Nullable DepthsManager mInstance;
	// Map of players to their depths information- to persist through logouts
	public Map<UUID, DepthsPlayer> mPlayers = new HashMap<>();
	private static @Nullable Plugin mPlugin;
	// List of all items with random rarities
	public ArrayList<DepthsAbilityItem> mItems = new ArrayList<>();
	// List of ability offers for each player
	public Map<UUID, List<DepthsAbilityItem>> mAbilityOfferings = new HashMap<>();
	// List of upgrade offers for each player
	public Map<UUID, List<DepthsAbilityItem>> mUpgradeOfferings = new HashMap<>();
	public Random mRandom = new Random();
	// Parties currently active in the system
	public ArrayList<DepthsParty> mParties = new ArrayList<>();
	// Repository for storing and spawning depths rooms
	public @Nullable DepthsRoomRepository mRoomRepository;
	// Runnable to manage for dealing damage to players under certain conditions
	public @Nullable DepthsDamageRunnable mDamageRunnable;
	// Config path for saving and loading depths data from the file system
	private @Nullable String mConfigPath;

	private DepthsManager() {
		//Start the runnable for damaging players on bad glass
		mDamageRunnable = new DepthsDamageRunnable();
		mDamageRunnable.runTaskTimer(Plugin.getInstance(), 0, GLASS_DAMAGE_TICK_INTERVAL);
		mPlugin = Plugin.getInstance();
	}

	public DepthsManager(Plugin p, Logger logger, String configPath) {

		//Try to load the manager from file, if it exists
		mPlugin = p;
		if (!load(logger, configPath)) {
			//Otherwise create a new instance
			//Start the runnable for damaging players on bad glass
			mDamageRunnable = new DepthsDamageRunnable();
			mDamageRunnable.runTaskTimer(p, 0, GLASS_DAMAGE_TICK_INTERVAL);
			mConfigPath = configPath;
			new BukkitRunnable() {

				@Override
				public void run() {
					DepthsManager.getInstance().save(p.getDataFolder() + File.separator + "depths");
				}

			}.runTaskTimer(p, SAVE_INTERVAL, SAVE_INTERVAL);
			mInstance = this;
		}
	}

	//Singleton pattern for manager
	public static DepthsManager getInstance() {
		if (mInstance == null) {
			mInstance = new DepthsManager();
		}
		return mInstance;
	}

	/**
	 * Loads a data file for previous plugin's instance on that shard, and generates the depths manager object with it if applicable
	 * @param logger logs info
	 * @param configPath path to look for the file data
	 * @return whether a file with depths data exists (and was successfully loaded)
	 */
	public boolean load(Logger logger, String configPath) {
		mConfigPath = configPath;
		//Attempt to load save data from the file
		try {
			String playerContent = FileUtils.readFile(mConfigPath + "players.json");
			String partyContent = FileUtils.readFile(mConfigPath + "parties.json");
			if (playerContent == null || playerContent.isEmpty()) {
				logger.warning("Depths" + mConfigPath + "' is empty - defaulting to new depths manager");
			} else {
				Gson gson = new Gson();
				Type playerType = new TypeToken<Map<UUID, DepthsPlayer>>() {}.getType();

				mPlayers = gson.fromJson(playerContent, playerType);

				Type partyType = new TypeToken<ArrayList<DepthsParty>>() {}.getType();
				mParties = gson.fromJson(partyContent, partyType);

				//Stitch the players and parties together

				if (mParties != null && mPlayers != null) {
					for (DepthsPlayer dp : mPlayers.values()) {
						for (DepthsParty party : mParties) {
							if (dp.mPartyNum == party.mPartyNum) {
								if (party.mPlayersInParty == null) {
									party.mPlayersInParty = new ArrayList<>();
								}
								party.mPlayersInParty.add(dp);
							}
						}
					}
				} else {
					//No valid players and parties were found
					return false;
				}

				mDamageRunnable = new DepthsDamageRunnable();
				mDamageRunnable.runTaskTimer(Plugin.getInstance(), 0, GLASS_DAMAGE_TICK_INTERVAL);
				new BukkitRunnable() {

					@Override
					public void run() {
						DepthsManager.getInstance().save(Plugin.getInstance().getDataFolder() + File.separator + "depths");
					}

				}.runTaskTimer(mPlugin, SAVE_INTERVAL, SAVE_INTERVAL);
				mInstance = this;
				return true;
			}

		} catch (FileNotFoundException e) {
			logger.warning("Depths access file '" + mConfigPath + "' does not exist - defaulting to new depths manager");
		} catch (Exception e) {
			logger.severe("Caught depths exception: " + e);
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Saves the current status of depths players and parties into a json to load on shard restart
	 * @param path the file path to save the data
	 */
	public void save(String path) {

		//Save two separate files, one for players and one for parties
		String tempFilePath = ".tmp";

		try {
			Gson gson = new Gson();
			FileUtils.writeFile(path + "players.json" + tempFilePath, gson.toJson(mPlayers));
			FileUtils.writeFile(path + "parties.json" + tempFilePath, gson.toJson(mParties));

		} catch (Exception e) {
			Plugin.getInstance().getLogger().severe("Caught exception saving file '" + tempFilePath + "': " + e);
			e.printStackTrace();
		}
		try {
			FileUtils.moveFile(path + "players.json" + tempFilePath, path + "players.json");
			FileUtils.moveFile(path + "parties.json" + tempFilePath, path + "parties.json");
		} catch (Exception e) {
			Plugin.getInstance().getLogger().severe("Caught exception renaming file '" + tempFilePath + "' to '" + path + "': " + e);
			e.printStackTrace();
		}
	}

	/**
	 * Returns the rarity level the player has in the specified ability.
	 *
	 * @param name the exact name of the ability's ABILITY_NAME param
	 * @param p    player to check for
	 * @return the level in the ability
	 */
	public int getPlayerLevelInAbility(@Nullable String name, @Nullable Player p) {

		if (p == null || name == null) {
			return 0;
		}

		DepthsPlayer dp = getDepthsPlayer(p);

		if (dp != null) {
			Integer i = dp.mAbilities.get(name);
			if (i != null) {
				return i;
			}
		}
		return 0;
	}

	/**
	 * Helper method to get the party object from a participating player
	 *
	 * @param dp depths player instance to check
	 * @return party object of associated player
	 */
	public @Nullable DepthsParty getPartyFromId(@Nullable DepthsPlayer dp) {
		if (dp == null) {
			return null;
		}
		for (DepthsParty party : mParties) {
			if (party.mPartyNum == dp.mPartyNum) {
				return party;
			}
		}
		return null;
	}

	/**
	 * This function is run to load a player into the system for the first time and add them to a depths party.
	 * @param player the player to add
	 */
	public void init(Player player) {

		// Get nearby players and put them in the same party
		List<Player> nearbyPlayers = EntityUtils.getNearestPlayers(player.getLocation(), 50.0);
		List<DepthsPlayer> depthsPlayers = new ArrayList<>();
		DepthsParty partyToAdd = null;
		for (Player p : nearbyPlayers) {
			if (!isInSystem(p)) {
				DepthsPlayer dp = new DepthsPlayer(p);
				mPlayers.put(p.getUniqueId(), dp);
			}

			// Add the players to the new party if they don't already have one
			if (getDepthsParty(p) == null) {
				depthsPlayers.add(mPlayers.get(p.getUniqueId()));
			} else {
				//Add the new players to the current party
				partyToAdd = getDepthsParty(p);
			}
		}
		// If the players need a new party, create one
		// The constructor will also assign the players to this party
		if (depthsPlayers.size() > 0) {
			if (partyToAdd == null) {
				mParties.add(new DepthsParty(depthsPlayers, player.getLocation()));
			} else {
				//Join the new players to the party that already exists
				for (DepthsPlayer dp : depthsPlayers) {
					partyToAdd.addPlayerToParty(dp);
				}
			}
		}
	}

	/**
	 * Sets the player level in a specific ability
	 * @param name the name of the ability's ABILITY_NAME attribute. Needs to be exact!
	 * @param p the player to give it to
	 * @param level the rarity level of the ability
	 */
	public void setPlayerLevelInAbility(String name, Player p, int level) {
		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp != null) {
			dp.mAbilities.put(name, level);
		}
		DepthsParty party = getPartyFromId(dp);
		if (party == null) {
			return;
		}
		//Tell their party that someone has selected an ability and is eligible for upgrade rooms
		if (!DepthsUtils.isWeaponAspectAbility(name)) {
			party.mHasAtLeastOneAbility = true;
			try {
				for (DepthsPlayer otherPlayer : party.mPlayersInParty) {
					Player newPlayer = Bukkit.getPlayer(otherPlayer.mPlayerId);
					if (newPlayer != null && !newPlayer.equals(p)) {
						if (level > 0 && level < 6) {
							newPlayer.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + p.getDisplayName() + " now has ability: " + name + " at " + DepthsUtils.getRarityText(level) + " level!");
						} else if (level == 6) {
							newPlayer.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + p.getDisplayName() + " now has ability: " + name + " at " + ChatColor.MAGIC + DepthsUtils.getRarityText(level) + ChatColor.RESET + ChatColor.LIGHT_PURPLE + " level!");
						} else if (level == 0) {
							newPlayer.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + p.getDisplayName() + " has lost ability: " + name + "!");
						}
					}
				}
			} catch (Exception e) {
				Plugin.getInstance().getLogger().info("Error while attempting to set player depths ability");
				e.printStackTrace();
			}
		} else {
			for (DepthsPlayer otherPlayer : party.mPlayersInParty) {
				Player newPlayer = Bukkit.getPlayer(otherPlayer.mPlayerId);
				if (newPlayer != null && !newPlayer.equals(p)) {
					if (level == 1) {
						newPlayer.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + p.getDisplayName() + " has selected " + name + " as their aspect!");
					} else if (level == 2) {
						// Aspect is being transformed from mystery box
						newPlayer.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + p.getDisplayName() + " has had their mystery box transform into " + name + "!");
					}
				}
			}
		}
		AbilityManager.getManager().updatePlayerAbilities(p, false);
	}

	/**
	 * The list of all depths abilities, to be added into the general ability manager so players can trigger their events on the depths shard
	 *
	 * @return list of depths abilities
	 */
	public static Collection<DepthsAbilityInfo<?>> getAbilities() {

		return List.of(

			//Weapon aspects
			AxeAspect.INFO,
			RandomAspect.INFO,
			ScytheAspect.INFO,
			SwordAspect.INFO,
			WandAspect.INFO,
			BowAspect.INFO,

			//Steelsage abilities
			FireworkBlast.INFO,
			FocusedCombos.INFO,
			Metalmancy.INFO,
			ProjectileMastery.INFO,
			RapidFire.INFO,
			Scrapshot.INFO,
			Sidearm.INFO,
			SteelStallion.INFO,
			DepthsSharpshooter.INFO,
			DepthsSplitArrow.INFO,
			DepthsVolley.INFO,

			//Windwalker abilities
			Aeromancy.INFO,
			DepthsDodging.INFO,
			GuardingBolt.INFO,
			HowlingWinds.INFO,
			LastBreath.INFO,
			OneWithTheWind.INFO,
			RestoringDraft.INFO,
			Skyhook.INFO,
			Slipstream.INFO,
			DepthsWindWalk.INFO,
			Whirlwind.INFO,

			//Shadow abilities
			DepthsAdvancingShadows.INFO,
			BladeFlurry.INFO,
			Brutalize.INFO,
			ChaosDagger.INFO,
			DarkCombos.INFO,
			DeadlyStrike.INFO,
			DepthsDethroner.INFO,
			DummyDecoy.INFO,
			CloakOfShadows.INFO,
			ShadowSlam.INFO,

			//Dawnbringer abilities
			BottledSunlight.INFO,
			Enlightenment.INFO,
			LightningBottle.INFO,
			RadiantBlessing.INFO,
			DepthsRejuvenation.INFO,
			SoothingCombos.INFO,
			Sundrops.INFO,
			TotemOfSalvation.INFO,
			WardOfLight.INFO,

			//Flamecaller abilities
			Apocalypse.INFO,
			Detonation.INFO,
			Fireball.INFO,
			FlameSpirit.INFO,
			Flamestrike.INFO,
			PrimordialMastery.INFO,
			Pyroblast.INFO,
			Pyromania.INFO,
			RingOfFlames.INFO,
			VolcanicCombos.INFO,
			VolcanicMeteor.INFO,

			// Frostborn abilities
			Avalanche.INFO,
			Cryobox.INFO,
			FrigidCombos.INFO,
			DepthsFrostNova.INFO,
			FrozenDomain.INFO,
			IceBarrier.INFO,
			Icebreaker.INFO,
			IceLance.INFO,
			Permafrost.INFO,
			PiercingCold.INFO,

			// Earthbound abilities
			BrambleShell.INFO,
			Bulwark.INFO,
			CrushingEarth.INFO,
			DepthsToughness.INFO,
			EarthenCombos.INFO,
			EarthenWrath.INFO,
			Earthquake.INFO,
			Entrench.INFO,
			StoneSkin.INFO,
			Taunt.INFO);
	}

	/**
	 * Returns a list of depth abilities filtered by certain trees
	 *
	 * @param filter the valid trees to filter by
	 * @return list of filtered abilities
	 */
	public static List<DepthsAbilityInfo<?>> getFilteredAbilities(List<DepthsTree> filter) {
		List<DepthsAbilityInfo<?>> filteredList = new ArrayList<>();
		for (DepthsAbilityInfo<?> da : getAbilities()) {
			if (filter.contains(da.getDepthsTree())) {
				filteredList.add(da);
			}
		}
		return filteredList;
	}

	public static List<DepthsAbilityInfo<?>> getMutatedAbilities(List<DepthsTree> filter, DepthsTrigger trigger) {
		List<DepthsAbilityInfo<?>> filteredList = new ArrayList<>();
		for (DepthsAbilityInfo<?> da : getAbilities()) {
			if (filter.contains(da.getDepthsTree()) && da.getDepthsTrigger() == trigger) {
				filteredList.add(da);
			}
		}
		return filteredList;
	}

	public static List<DepthsAbilityInfo<? extends WeaponAspectDepthsAbility>> getWeaponAspects() {
		return Arrays.asList(
			AxeAspect.INFO,
			RandomAspect.INFO,
			ScytheAspect.INFO,
			SwordAspect.INFO,
			WandAspect.INFO,
			BowAspect.INFO);
	}

	/**
	 * This method generates ability items for the players with random rarities.
	 * In the future, this might be more applicable to work on a per player basis,
	 * with active skills they have influencing rarities or available abilities.
	 */
	private void initItems(List<DepthsTree> filter, boolean isElite, Player p) {
		// Replace this with a dedicated place later
		mItems.clear();
		for (DepthsAbilityInfo<?> da : getFilteredAbilities(filter)) {
			// Get a number 1 to 100
			int roll = mRandom.nextInt(100) + 1;
			DepthsAbilityItem item = null;

			//Add enlightenment level to roll if applicable
			int enlightenmentLevel = getPlayerLevelInAbility(Enlightenment.ABILITY_NAME, p);
			if (enlightenmentLevel > 0) {
				roll += Enlightenment.RARITY_INCREASE[enlightenmentLevel - 1];
			}

			if (isElite) {
				if (roll < 46) {
					//UNCOMMON RARITY- 45%
					item = da.getAbilityItem(2);
				} else if (roll < 76) {
					//RARE RARITY- 30%
					item = da.getAbilityItem(3);
				} else if (roll < 91) {
					//EPIC RARITY- 15%
					item = da.getAbilityItem(4);
				} else {
					//LEGENDARY RARITY- 10%
					item = da.getAbilityItem(5);
				}
			} else {
				if (roll < 41) {
					//COMMON RARITY- 40%
					item = da.getAbilityItem(1);
				} else if (roll < 71) {
					//UNCOMMON RARITY- 30%
					item = da.getAbilityItem(2);
				} else if (roll < 91) {
					//RARE RARITY- 20%
					item = da.getAbilityItem(3);
				} else {
					//EPIC RARITY- 10%
					item = da.getAbilityItem(4);
				}
			}

			if (item != null) {
				mItems.add(item);
			}
		}

	}

	/**
	 * This method generates the available ability offerings for the given player.
	 *
	 * @param p player to generate items for
	 * @return the ability items the player can select from
	 */
	public @Nullable List<DepthsAbilityItem> getAbilityUnlocks(Player p) {

		DepthsPlayer dp = getDepthsPlayer(p);

		if (dp == null) {
			return null;
		}

		List<DepthsAbilityItem> offeredItems = mAbilityOfferings.get(p.getUniqueId());
		if (offeredItems == null || offeredItems.size() == 0) {
			//Filter the item offerings by the player's eligible trees for that run
			if (dp.mEarnedRewards.peek() == DepthsRewardType.ABILITY_ELITE) {
				initItems(dp.mEligibleTrees, true, p);
			} else {
				initItems(dp.mEligibleTrees, false, p);
			}
		} else {
			return offeredItems;
		}

		// Return 3 choices of items
		List<String> allowedAbilities = new ArrayList<>();
		for (DepthsAbilityInfo<?> da : getAbilities()) {
			if (da.canBeOffered(p)) {
				allowedAbilities.add(da.getDisplayName());
			}
		}
		offeredItems = new ArrayList<>();
		Collections.shuffle(mItems);
		// Add the first 3 items the player is eligible for to the thing
		for (DepthsAbilityItem item : mItems) {
			if (offeredItems.size() >= 3) {
				break;
			}
			if (allowedAbilities.contains(item.mAbility)) {
				offeredItems.add(item);
			}
		}

		mAbilityOfferings.put(p.getUniqueId(), offeredItems);

		return offeredItems;
	}

	/**
	 * This method is called when the player selects which ability to upgrade from the gui options
	 * @param p the player to give the ability to
	 * @param slot the index to give from their offerings array
	 */
	public void playerChoseItem(Player p, int slot) {
		List<DepthsAbilityItem> itemChoices = mAbilityOfferings.get(p.getUniqueId());
		if (itemChoices == null) {
			return;
		}
		DepthsAbilityItem choice = itemChoices.get(slot);
		if (choice == null || choice.mAbility == null) {
			return;
		}
		setPlayerLevelInAbility(choice.mAbility, p, choice.mRarity);
		mAbilityOfferings.remove(p.getUniqueId());
		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp != null) {
			dp.mEarnedRewards.poll();
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
		}
	}

	/**
	 * Player chose the weapon aspect at the start of the run
	 * @param p player
	 * @param slot which element in the array they picked
	 */
	public void playerChoseWeaponAspect(Player p, int slot) {

		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp != null) {
			dp.mHasWeaponAspect = true;
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
		} else {
			return;
		}

		List<DepthsAbilityInfo<? extends WeaponAspectDepthsAbility>> options = dp.mWeaponOfferings;
		if (options == null) {
			return;
		}
		DepthsAbilityInfo<? extends WeaponAspectDepthsAbility> choice = options.get(slot);
		if (choice == null) {
			return;
		}

		if (choice == RandomAspect.INFO) {
			//Roll random ability if they selected mystery box
			int[] chances = {40, 40, 20, 0, 0};
			getRandomAbility(p, dp, chances);
		}
		setPlayerLevelInAbility(choice.getDisplayName(), p, 1);
	}

	/**
	 * Removes the player from the depths system
	 * @param p the player to remove
	 */
	public void deletePlayer(Player p) {
		UUID uuid = p.getUniqueId();

		// Remove from party
		DepthsPlayer dp = getDepthsPlayer(uuid);

		if (dp != null) {
			DepthsParty party = getPartyFromId(dp);
			if (party != null) {
				party.mPlayersInParty.remove(dp);

				//Delete the party if no players are left
				if (party.mPlayersInParty.isEmpty()) {
					mParties.remove(party);
				}
			}
		}

		// If the player is in the system
		if (mPlayers.remove(uuid) != null) {
			mAbilityOfferings.remove(uuid);
			mUpgradeOfferings.remove(uuid);

			AbilityManager.getManager().updatePlayerAbilities(p, true);
			//Reset delve player info
			DelvesUtils.clearDelvePlayerByShard(null, p, ServerProperties.getShardName());
		}
	}

	/**
	 * Returns the ability item previews of all abilities the player currently has
	 *
	 * @param p the player to look up
	 * @return ability items for all their active abilities (rarity > 0)
	 */
	public @Nullable List<DepthsAbilityItem> getPlayerAbilitySummary(Player p) {

		DepthsPlayer dp = getDepthsPlayer(p);

		// Check if they're in the system
		if (dp == null) {
			return null;
		}

		List<DepthsAbilityItem> abilities = new ArrayList<>();

		// For each ability they have a score for, return the item that says what that ability does

		for (DepthsAbilityInfo<?> da : getAbilities()) {
			Integer rarity = dp.mAbilities.getOrDefault(da.getDisplayName(), 0);
			if (rarity > 0) {
				abilities.add(da.getAbilityItem(rarity));
			}
		}

		return abilities;
	}

	/**
	 * Returns the abilities the player currently has
	 *
	 * @param p the player to look up
	 * @return all their active abilities (rarity > 0)
	 */
	public @Nullable List<DepthsAbilityInfo<?>> getPlayerAbilities(Player p) {

		DepthsPlayer dp = getDepthsPlayer(p);

		// Check if they're in the system
		if (dp == null) {
			return null;
		}

		List<DepthsAbilityInfo<?>> abilities = new ArrayList<>();

		// For each ability they have a score for, return the item that says what that ability does

		for (DepthsAbilityInfo<?> da : getAbilities()) {
			Integer rarity = dp.mAbilities.getOrDefault(da.getDisplayName(), 0);
			if (rarity > 0) {
				abilities.add(da);
			}
		}

		return abilities;
	}

	//Simple boolean check to see if the player is already in the depths system
	public boolean isInSystem(Player p) {
		return getDepthsPlayer(p) != null;
	}

	// Tells us when a player in the system broke a spawner so we can track their progress
	public void playerBrokeSpawner(Player p, Location l) {
		DepthsParty party = getDepthsParty(p);
		if (party != null && l.getX() >= party.getRoomX()) {
			party.partyBrokeSpawner(l);
		}
	}

	/**
	 * Generates the next available room options for the party
	 *
	 * @param party the depths party to generate for
	 * @return available room options for the party's designated player to select from the gui
	 */
	public @Nullable EnumSet<DepthsRoomType> generateRoomOptions(DepthsParty party) {

		// Check if they're currently in a bossfight and cancel the call
		if (!party.mBeatBoss && party.mRoomNumber % 10 == 0 && party.mRoomNumber > 0) {
			return null;
		}

		// Check and see if they already have had options generated
		if (party.mNextRoomChoices != null && party.mNextRoomChoices.size() > 0) {
			return party.mNextRoomChoices;
		}

		// If party just started, they get the weapon aspect room
		if (party.mRoomNumber == 0) {
			party.mNextRoomChoices = EnumSet.of(DepthsRoomType.ABILITY);
			return EnumSet.of(DepthsRoomType.ABILITY);
		}
		// If party is on a boss interval, they get a boss
		if (party.mRoomNumber % 10 == 9) {
			party.mNextRoomChoices = EnumSet.of(DepthsRoomType.BOSS);
			return EnumSet.of(DepthsRoomType.BOSS);
		}
		// Roll for how many options the party gets
		//Will roll between 2 and 4 times
		int choices = mRandom.nextInt(3) + 2;
		ArrayList<DepthsRoomType> values = new ArrayList<>(Arrays.asList(DepthsRoomType.values()));
		values.remove(DepthsRoomType.BOSS);
		values.remove(DepthsRoomType.TWISTED);
		//if the party has not selected any abilities yet, do not let them get an upgrade reward!
		if (!party.mHasAtLeastOneAbility) {
			values.remove(DepthsRoomType.UPGRADE);
			values.remove(DepthsRoomType.UPGRADE_ELITE);
		}

		//If the party is in endless mode and on floor 4+, reduce the chance of utility room
		//50% chance to remove utility room from the pool if so
		if (party.mRoomNumber >= 30 && mRandom.nextInt(2) > 0) {
			values.remove(DepthsRoomType.UTILITY);
		}

		//Roll chance for twisted room - 1.5% per room selection
		boolean twisted = mRandom.nextInt(100) < 2 && !party.mTwistedThisFloor;

		//Pull 4 room types at random. They may or may not be the same, so it is skewed towards fewer options atm.
		DepthsRoomType e1 = values.get(mRandom.nextInt(values.size()));
		DepthsRoomType e2 = values.get(mRandom.nextInt(values.size()));
		DepthsRoomType e3 = values.get(mRandom.nextInt(values.size()));
		DepthsRoomType e4 = values.get(mRandom.nextInt(values.size()));

		if (choices == 4) {
			if (twisted) {
				party.mNextRoomChoices = EnumSet.of(e1, e2, e3, DepthsRoomType.TWISTED);
				return EnumSet.of(e1, e2, e3, DepthsRoomType.TWISTED);
			}
			party.mNextRoomChoices = EnumSet.of(e1, e2, e3, e4);
			return EnumSet.of(e1, e2, e3, e4);
		} else if (choices == 3) {
			if (twisted) {
				party.mNextRoomChoices = EnumSet.of(e1, e2, DepthsRoomType.TWISTED);
				return EnumSet.of(e1, e2, DepthsRoomType.TWISTED);
			}
			party.mNextRoomChoices = EnumSet.of(e1, e2, e3);
			return EnumSet.of(e1, e2, e3);
		} else if (choices == 2) {
			if (twisted) {
				party.mNextRoomChoices = EnumSet.of(e1, DepthsRoomType.TWISTED);
				return EnumSet.of(e1, DepthsRoomType.TWISTED);
			}
			party.mNextRoomChoices = EnumSet.of(e1, e2);
			return EnumSet.of(e1, e2);
		} else {
			if (twisted) {
				party.mNextRoomChoices = EnumSet.of(e1, DepthsRoomType.TWISTED);
				return EnumSet.of(e1, DepthsRoomType.TWISTED);
			}
			party.mNextRoomChoices = EnumSet.of(e1);
			return EnumSet.of(e1);
		}
	}

	/**
	 * This method is input when the player needs to generate a new room
	 * It looks up the next available room from the party
	 *
	 * @param p player to generate for
	 * @return available room choices for the party
	 */
	public @Nullable EnumSet<DepthsRoomType> generateRoomOptions(Player p) {
		DepthsParty party = getDepthsParty(p);
		if (party != null) {
			return generateRoomOptions(party);
		} else {
			return null;
		}
	}

	/**
	 * This method is run when the player selects which room they want next from the GUI
	 * It calls the room repository to get an eligible room to spawn, and spawns that room
	 * @param roomType the type of room the party selected
	 * @param player the player that selected the room
	 */
	public void playerSelectedRoom(DepthsRoomType roomType, Player player) {
		DepthsParty party = getDepthsParty(player);
		if (party == null || party.mNextRoomChoices == null || party.mNextRoomChoices.isEmpty() || party.mRoomSpawnerLocation == null) {
			return;
		}
		party.mNextRoomChoices.clear();

		// Summon the boss room if they are on a boss room interval, regardless of whatever
		// the players tried to pull with the gui
		if (party.mRoomNumber % 10 == 9) {
			roomType = DepthsRoomType.BOSS;
		}

		World world = player.getWorld();
		Vector roomSpawnerLocation = party.mRoomSpawnerLocation;
		removeNearbyButton(roomSpawnerLocation, world);
		//Remove the button later in case of structure bug
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			removeNearbyButton(roomSpawnerLocation, world);
		}, 10);

		// Generate the room
		if (mRoomRepository == null) {
			mRoomRepository = new DepthsRoomRepository();
		}

		// Summon the new room and give it to the party
		Location l = new Location(world, roomSpawnerLocation.getX(), roomSpawnerLocation.getY(), roomSpawnerLocation.getZ());
		party.setNewRoom(mRoomRepository.summonRoom(l, roomType, party));
	}

	// Utility method to get rid of the button players use to select the next room
	private void removeNearbyButton(Vector v, World w) {

		for (int x = -5; x < 5; x++) {
			for (int y = -5; y < 5; y++) {
				Location loc = new Location(w, v.getX() + x, v.getY() + y, v.getZ());
				if (w.getBlockAt(loc).getType() == Material.STONE_BUTTON) {
					w.getBlockAt(loc).setType(Material.AIR);
				}
			}
		}
	}

	/**
	 * Returns the party's summary for the given player
	 * @param p player to get party summary for
	 * @return a string containing the party summary
	 */
	public String getPartySummary(Player p) {
		DepthsParty party = getDepthsParty(p);
		if (party != null) {
			return party.getSummaryString();
		} else {
			return "You are not currently in a depths party!";
		}
	}

	/**
	 * This method is called by the command block at the end of each room.
	 * If the room is clear, it opens the next room selection gui
	 * @param p player to generate rooms for
	 * @param l the location to spawn the next room at, in order to line up with the door
	 */
	public void gotRoomEndpoint(Player p, Location l) {
		DepthsPlayer dp = getDepthsPlayer(p);

		if (dp == null || getPartyFromId(dp) == null) {
			//Will be the main way players init their party, spawning the first room
			init(p);
		}

		DepthsParty party = getPartyFromId(dp);
		if (dp != null && party != null) {

			// Check that spawner count is zero
			if (party.mSpawnersToBreak > 0) {
				if (party.mSpawnersToBreak == 1) {
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "There is still " + party.mSpawnersToBreak + " spawner left to break!");
				} else {
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "There are still " + party.mSpawnersToBreak + " spawners left to break!");
				}
				return;
			}

			// Store the location to spawn the next room from
			party.mRoomSpawnerLocation = l.toVector();
			//Let the player select the room
			DepthsGUICommands.roomChoice(Plugin.getInstance(), p);
		}
	}

	/**
	 * Increases treasure score for the player's party by the specified amount
	 * @param l location to play sound
	 * @param p player whose party to increase
	 * @param score amount to increase treasure score
	 */
	public void incrementTreasure(@Nullable Location l, Player p, int score) {
		DepthsParty party = getDepthsParty(p);
		if (party != null) {
			party.giveTreasureReward(l, score);
		}
	}

	/**
	 * This method is called when a player opens a chest
	 * If the room is clear and they haven't picked a reward yet,
	 * they will get the gui to select their reward
	 *
	 * @param p player who opened the chest
	 * @return the appropriate GUI to open for their current room reward
	 */
	public Boolean getRoomReward(Player p, @Nullable Location l) {
		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp == null) {
			return false;
		}

		DepthsParty party = getPartyFromId(dp);
		if (party == null) {
			return false;
		}

		//First- check if the player has any rewards to open
		if (!dp.mEarnedRewards.isEmpty()) {
			DepthsRewardType reward = dp.mEarnedRewards.peek();
			if (reward == DepthsRewardType.ABILITY || reward == DepthsRewardType.ABILITY_ELITE) {
				DepthsGUICommands.ability(Plugin.getInstance(), p);
				return true;
			} else if (reward == DepthsRewardType.UPGRADE || reward == DepthsRewardType.UPGRADE_ELITE || reward == DepthsRewardType.TWISTED) {
				DepthsGUICommands.upgrade(Plugin.getInstance(), p);
				return true;
			}
		}

		if (party.mSpawnersToBreak > 0 || !party.mCanGetTreasureReward) {
			return false;
		}

		DepthsRoomType room = party.mCurrentRoomType;

		//Give treasure if their reward queue is empty and it's a treasure room the party has cleared
		if (room == DepthsRoomType.TREASURE) {
			party.giveTreasureReward(l, TREASURE_PER_NORMAL);
			return false;
		} else if (room == DepthsRoomType.TREASURE_ELITE) {
			party.giveTreasureReward(l, TREASURE_PER_ELITE);
			return false;
		}

		return false;
	}

	/**
	 * Called when the player selects an ability to upgrade. Upgrades the respective skill associated with the item
	 * @param p player to upgrade for
	 * @param slot the index of which item they selected in their offering array
	 */
	public void playerUpgradedItem(Player p, int slot) {
		UUID uuid = p.getUniqueId();
		List<DepthsAbilityItem> itemChoices = mUpgradeOfferings.get(uuid);
		if (itemChoices == null) {
			return;
		}
		if (slot > itemChoices.size() + 1) {
			return;
		}
		DepthsAbilityItem choice = itemChoices.get(slot);
		setPlayerLevelInAbility(choice.mAbility, p, choice.mRarity);
		mUpgradeOfferings.remove(uuid);
		DepthsPlayer dp = getDepthsPlayer(uuid);
		if (dp != null) {
			dp.mEarnedRewards.poll();
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
		}
	}

	/**
	 * Logic to get available upgrades for the player when they have an upgrade reward
	 *
	 * @param p player to generate for
	 * @return the items for the available upgrades
	 */
	public @Nullable List<DepthsAbilityItem> getAbilityUpgradeOptions(Player p) {
		UUID uuid = p.getUniqueId();
		List<DepthsAbilityItem> offeredItems = mUpgradeOfferings.get(uuid);
		if (offeredItems != null && !offeredItems.isEmpty()) {
			return offeredItems;
		}

		DepthsPlayer dp = getDepthsPlayer(uuid);
		if (dp == null) {
			return null;
		}
		// Return up to 3 random choices of items that are one level above the current level
		ArrayList<DepthsAbilityInfo<?>> abilities = new ArrayList<>(getAbilities());
		Collections.shuffle(abilities);
		offeredItems = new ArrayList<>();

		// Loop through all possible abilities and show random ones they have at a higher rarity
		for (DepthsAbilityInfo<?> da : abilities) {
			if (offeredItems.size() >= 3) {
				break;
			}
			int level = getPlayerLevelInAbility(da.getDisplayName(), p);
			if (level == 0 || (level >= 5 && !(dp.mEarnedRewards.peek() == DepthsRewardType.TWISTED)) || level >= 6 || WeaponAspectDepthsAbility.class.isAssignableFrom(da.getAbilityClass())) {
				continue;
			} else {
				DepthsAbilityItem item;
				//If they're in an elite room, their reward is +2 levels instead
				if (dp.mEarnedRewards.peek() == DepthsRewardType.UPGRADE_ELITE) {
					item = da.getAbilityItem(Math.min(5, level + 2));
				} else if (dp.mEarnedRewards.peek() == DepthsRewardType.TWISTED) {
					item = da.getAbilityItem(6);
				} else {
					item = da.getAbilityItem(Math.min(5, level + 1));
				}

				offeredItems.add(item);
			}
		}

		mUpgradeOfferings.put(uuid, offeredItems);

		return offeredItems;
	}

	/**
	 * This method takes a player and, if they are in the system, removes one of their abilities at random
	 * and rolls them two random new abilities at the replaced ability's rarity
	 * @param p Player to roll for
	 */
	public void chaos(Player p) {

		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp == null) {
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Player not in depths system!");
			return;
		}
		if (dp.mUsedChaosThisFloor) {
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You have already used the chaos system on this floor!");
			return;
		}

		//Remove random ability
		Object[] playerAbilities = dp.mAbilities.keySet().toArray();
		List<String> abilityList = new ArrayList<>();
		for (Object ability : playerAbilities) {
			abilityList.add((String) ability);
		}
		Collections.shuffle(abilityList);
		String removedAbility = null;
		int index = 0;
		int removedLevel = 1;
		boolean isMutated = false;
		while (removedAbility == null) {
			if (index >= abilityList.size()) {
				return;
			}
			String test = abilityList.get(index);
			//Make sure the player has the ability AND it's not a weapon aspect
			int testLevel = getPlayerLevelInAbility(test, p);
			if (testLevel > 0 && !DepthsUtils.isWeaponAspectAbility(test)) {
				removedAbility = test;
				removedLevel = testLevel;
			}
			index++;
		}
		for (DepthsAbilityInfo<?> da : getAbilities()) {
			if (da.getDisplayName().equals(removedAbility)) {
				if (!dp.mEligibleTrees.contains(da.getDepthsTree())) {
					isMutated = true;
				}
			}
		}
		setPlayerLevelInAbility(removedAbility, p, 0);
		p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Removed ability: " + removedAbility);
		dp.mUsedChaosThisFloor = true;

		//Give 2 random abilities that aren't the one we just removed
		for (int i = 0; i < 2; i++) {
			List<DepthsAbilityInfo<?>> abilities = getFilteredAbilities(dp.mEligibleTrees);
			if (isMutated) {
				List<DepthsTree> validTrees = new ArrayList<>();
				for (DepthsTree tree : DepthsTree.values()) {
					if (!dp.mEligibleTrees.contains(tree)) {
						validTrees.add(tree);
					}
				}
				abilities = getFilteredAbilities(validTrees);
			}

			//Do not give any abilities that have the same trigger as abilities that are currently offered in an ability reward
			//This is needed because players can open up an ability reward, not choose anything, then take mystery box or chaos and end up with two abilities on a trigger
			List<DepthsAbilityItem> abilityOffering = mAbilityOfferings.get(p.getUniqueId());
			if (abilityOffering != null) {
				List<DepthsTrigger> blockedTriggers = new ArrayList<>();
				for (DepthsAbilityItem offeredAbility : abilityOffering) {
					DepthsTrigger trigger = offeredAbility.mTrigger;
					if (trigger != DepthsTrigger.PASSIVE) {
						blockedTriggers.add(trigger);
					}
				}

				abilities.removeIf(ability -> blockedTriggers.contains(ability.getDepthsTrigger()));
			}

			Collections.shuffle(abilities);
			for (DepthsAbilityInfo<?> da : abilities) {
				if (da.canBeOffered(p) && !da.getDisplayName().equals(removedAbility)) {
					setPlayerLevelInAbility(da.getDisplayName(), p, removedLevel);
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Gained ability: " + da.getDisplayName());

					break;
				}
			}
		}
	}

	/**
	 * Sends the party to the next floor (boss death for each will call this)
	 * @param p player - get their party and send them to next floor
	 */
	public void goToNextFloor(Player p) {
		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp == null || getPartyFromId(dp) == null) {
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Player not in depths system!");
			return;
		}
		DepthsParty party = getPartyFromId(dp);
		int partyFloor = party.getFloor();
		int treasureScoreIncrease = TREASURE_PER_FLOOR * partyFloor + 2;
		party.mTreasureScore += treasureScoreIncrease;

		//Check to see if they've finished the run (normal mode) and send to loot rooms
		if (partyFloor == 3) {
			if (!party.mEndlessMode) {
				List<DepthsPlayer> playersToLoop = new ArrayList<>(party.mPlayersInParty);
				for (DepthsPlayer playerInParty : playersToLoop) {
					if (playerInParty.hasDied()) {
						//The player died before the rest of the party won, and has not yet respawned
						continue;
					}
					playerInParty.setDeathRoom(30);
					Player player = Bukkit.getPlayer(playerInParty.mPlayerId);
					if (player != null) {
						DepthsUtils.storetoFile(dp, Plugin.getInstance().getDataFolder() + File.separator + "DepthsStats"); //Save the player's stats
						dp.mFinalTreasureScore = party.mTreasureScore;
						player.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Congratulations! Your final treasure score is " + dp.mFinalTreasureScore + "!");
						getPartyFromId(dp).populateLootRoom(player, false);
						int depthsWins = ScoreboardUtils.getScoreboardValue(player, "Depths").orElse(0);
						if (depthsWins == 0) {
							MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a [\"\",{\"text\":\"" + player.getDisplayName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Darkest Depths for the first time!\",\"color\":\"white\",\"italic\":true,\"bold\":false}]");
						} else {
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a [\"\",{\"text\":\"" + player.getDisplayName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Darkest Depths!\",\"color\":\"yellow\",\"italic\":true,\"bold\":false}]");
						}
						//Set score
						ScoreboardUtils.setScoreboardValue(player, "Depths", depthsWins + 1);
						Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + player.getName() + " Depths");
					}
				}
				return;
			} else {
				for (DepthsPlayer playerInParty : party.mPlayersInParty) {
					Player player = Bukkit.getPlayer(playerInParty.mPlayerId);
					if (player == null || !player.isOnline()) {
						continue;
					}
					//Transform mystery box if applicable
					if (getPlayerLevelInAbility(RandomAspect.ABILITY_NAME, player) > 0) {
						transformMysteryBox(player);
					}
					//Set score
					ScoreboardUtils.setScoreboardValue(player, "Depths", ScoreboardUtils.getScoreboardValue(player, "Depths").orElse(0) + 1);
					Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + player.getName() + " Depths");
				}
			}
		}

		//Check to see if we're in endless mode and need to assign delve points to players
		if (party.mEndlessMode && partyFloor >= 3 && partyFloor <= 14) {
			int delvePoints = DepthsEndlessDifficulty.DELVE_POINTS_PER_FLOOR[partyFloor - 1];
			DepthsEndlessDifficulty.applyDelvePointsToParty(party.mPlayersInParty, delvePoints, party.mDelveModifiers, false);
		} else if (partyFloor > 12 && partyFloor % 3 == 0) {
			List<DepthsPlayer> playersToLoop = new ArrayList<>(party.mPlayersInParty);
			for (DepthsPlayer playerInParty : playersToLoop) {
				Player player = Bukkit.getPlayer(playerInParty.mPlayerId);
				if (player != null) {
					player.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You will now take +" + 10 * (partyFloor - 12) / 3 + "% damage from all sources!");
				}
			}
		}

		//Remove all mobs in the player's region
		List<Entity> mobs = p.getWorld().getEntities();
		for (Entity e : mobs) {
			if (EntityUtils.isHostileMob(e)) {
				e.remove();
			}
		}

		if (mRoomRepository == null) {
			mRoomRepository = new DepthsRoomRepository();
		}
		mRoomRepository.goToNextFloor(party, treasureScoreIncrease);
		party.mBeatBoss = true;
	}

	/**
	 * Transforms the mystery box aspect into a random other aspect after defeating floor 3
	 * The level 2 denotes that it is being transformed rather than selected (the aspects are not leveled)
	 * @param player the player to transform the ability of
	 */
	private void transformMysteryBox(Player player) {
		setPlayerLevelInAbility(RandomAspect.ABILITY_NAME, player, 0);
		List<DepthsAbilityInfo<? extends WeaponAspectDepthsAbility>> aspects = new ArrayList<>(getWeaponAspects());
		aspects.remove(RandomAspect.INFO);
		Collections.shuffle(aspects);
		setPlayerLevelInAbility(aspects.get(0).getDisplayName(), player, 2);
	}

	/**
	 * This method starts a bossfight for the given player's party depending on their current floor
	 * The bosses themselves will handle reading armor stands for cleaning up the arena
	 * @param p player to get party of
	 * @param l location to summon boss
	 */
	public void startBossFight(Player p, Location l) {
		//Check the player is in the system
		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp == null || getPartyFromId(dp) == null) {
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Player not in depths system!");
			return;
		}
		DepthsParty depthsParty = getPartyFromId(dp);
		//Teleport all players in party to the player activating the fight
		for (DepthsPlayer dpInParty : depthsParty.mPlayersInParty) {
			try {
				if (dpInParty != dp) {
					Player playerToTp = Bukkit.getPlayer(dpInParty.mPlayerId);
					if (playerToTp == null) {
						dpInParty.offlineTeleport(p.getLocation());
					} else if (playerToTp.getLocation().distance(l) > 20) {
						playerToTp.teleport(p);
					}
				}
			} catch (Exception e) {
				//Don't crash if we can't find a player
				Plugin.getInstance().getLogger().info("Missing depths player at bossfight");
			}

		}
		//Spawn boss depending on which floor we're on
		final String losName;
		final String bossTag;
		if (depthsParty.getFloor() % 3 == 1) {
			losName = HEDERA_LOS;
			bossTag = "boss_hedera";
		} else if (depthsParty.getFloor() % 3 == 2) {
			losName = DAVEY_LOS;
			bossTag = "boss_davey";
		} else {
			losName = NUCLEUS_LOS;
			bossTag = "boss_nucleus";
		}

		try {
			Entity entity = LibraryOfSoulsIntegration.summon(l, losName);
			if (entity instanceof LivingEntity boss) {
				BossManager.createBoss(null, boss, bossTag, l.clone().add(0, -2, 0));
			} else {
				Plugin.getInstance().getLogger().severe("Failed to summon depths boss " + bossTag);
			}
		} catch (Exception e) {
			Plugin.getInstance().getLogger().severe("Failed to set up depths boss '" + bossTag + "': " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Helper method for adding up odds for random rolls
	 */
	private int addUpChances(int rarity, int[] chances) {
		int chance = 1;
		for (int i = 0; i <= rarity; i++) {
			chance += chances[i];
		}
		return chance;
	}

	/**
	 * Gives the player a random ability at low rarity, for run start or mystery box
	 * @param p player
	 * @param dp depths player
	 * @param chances the array of odds for each rarity, length 5
	 */
	public void getRandomAbility(Player p, DepthsPlayer dp, int[] chances) {
		//Give random ability
		List<DepthsAbilityInfo<?>> abilities = getFilteredAbilities(dp.mEligibleTrees);

		//Do not give any abilities that have the same trigger as abilities that are currently offered in an ability reward
		//This is needed because players can open up an ability reward, not choose anything, then take mystery box or chaos and end up with two abilities on a trigger
		List<DepthsAbilityItem> abilityOffering = mAbilityOfferings.get(p.getUniqueId());
		if (abilityOffering != null) {
			List<DepthsTrigger> blockedTriggers = new ArrayList<>();
			for (DepthsAbilityItem offeredAbility : abilityOffering) {
				DepthsTrigger trigger = offeredAbility.mTrigger;
				if (trigger != DepthsTrigger.PASSIVE) {
					blockedTriggers.add(trigger);
				}
			}

			abilities.removeIf(ability -> blockedTriggers.contains(ability.getDepthsTrigger()));
		}

		Collections.shuffle(abilities);
		for (DepthsAbilityInfo<?> da : abilities) {
			if (da.canBeOffered(p)) {
				int roll = mRandom.nextInt(100) + 1;
				for (int i = 0; i < 5; i++) {
					if (roll < addUpChances(i, chances)) {
						setPlayerLevelInAbility(da.getDisplayName(), p, i + 1);
						p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You gained ability " + da.getDisplayName() + " at " + DepthsUtils.getRarityText(i + 1) + " level!");
						break;
					}
				}
				break;
			}
		}
	}

	public void getMutatedAbility(Player p, DepthsPlayer dp, DepthsTrigger trigger, String currentAbility) {
		//Give random ability
		List<DepthsTree> validTrees = new ArrayList<>();
		for (DepthsTree tree : DepthsTree.values()) {
			if (!dp.mEligibleTrees.contains(tree)) {
				validTrees.add(tree);
			}
		}
		List<DepthsAbilityInfo<?>> abilities = getMutatedAbilities(validTrees, trigger);

		//Clear any upgrades the player may have for the ability they are mutating
		List<DepthsAbilityItem> upgradeOffering = mUpgradeOfferings.get(p.getUniqueId());
		if (upgradeOffering != null) {
			for (DepthsAbilityItem offeredUpgrade : upgradeOffering) {
				DepthsTrigger currentTrigger = offeredUpgrade.mTrigger;
				if (trigger == currentTrigger) {
					mUpgradeOfferings.remove(p.getUniqueId());
				}
			}
		}

		Collections.shuffle(abilities);
		if (currentAbility.equals(abilities.get(0).getDisplayName()) && abilities.size() > 1) {
			setPlayerLevelInAbility(abilities.get(1).getDisplayName(), p, 1);
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You gained ability " + abilities.get(1).getDisplayName() + " at " + DepthsUtils.getRarityText(1) + " level!");
		} else if (!abilities.isEmpty()) {
			setPlayerLevelInAbility(abilities.get(0).getDisplayName(), p, 1);
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You gained ability " + abilities.get(0).getDisplayName() + " at " + DepthsUtils.getRarityText(1) + " level!");
		}

	}

	/**
	 * Debug method to set party room number manually
	 * @param player player to get party from
	 * @param number room number to set
	 */
	public void setRoomDebug(Player player, int number) {
		DepthsParty depthsParty = getDepthsParty(player);
		if (depthsParty == null) {
			player.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Player not in depths system!");
			return;
		}
		depthsParty.mRoomNumber = number;
	}

	public @Nullable DepthsPlayer getDepthsPlayer(UUID uuid) {
		return mPlayers.get(uuid);
	}

	public @Nullable DepthsPlayer getDepthsPlayer(Player player) {
		return getDepthsPlayer(player.getUniqueId());
	}

	public @Nullable DepthsParty getDepthsParty(Player player) {
		DepthsPlayer dp = getDepthsPlayer(player);
		if (dp != null) {
			return getPartyFromId(dp);
		}
		return null;
	}
}
