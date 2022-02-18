package com.playmonumenta.plugins.depths;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.depths.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.depths.abilities.aspects.AxeAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.RandomAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.ScytheAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.SwordAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.WandAspect;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.BottledSunlight;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Enlightenment;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.LightningBottle;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.RadiantBlessing;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Rejuvenation;
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
import com.playmonumenta.plugins.depths.abilities.shadow.Dethroner;
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
import com.playmonumenta.plugins.depths.abilities.windwalker.GuardingBolt;
import com.playmonumenta.plugins.depths.abilities.windwalker.HowlingWinds;
import com.playmonumenta.plugins.depths.abilities.windwalker.LastBreath;
import com.playmonumenta.plugins.depths.abilities.windwalker.OneWithTheWind;
import com.playmonumenta.plugins.depths.abilities.windwalker.RestoringDraft;
import com.playmonumenta.plugins.depths.abilities.windwalker.Skyhook;
import com.playmonumenta.plugins.depths.abilities.windwalker.Slipstream;
import com.playmonumenta.plugins.depths.abilities.windwalker.Updraft;
import com.playmonumenta.plugins.depths.abilities.windwalker.Whirlwind;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;

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

		DepthsPlayer dp = mPlayers.get(p.getUniqueId());

		if (dp != null) {
			Integer i = dp.mAbilities.get(name);
			if (i != null) {
				return i.intValue();
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
	public @Nullable DepthsParty getPartyFromId(DepthsPlayer dp) {
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
			if (p.getGameMode() == GameMode.SPECTATOR) {
				continue;
			}
			if (mPlayers.get(p.getUniqueId()) == null) {
				DepthsPlayer dp = new DepthsPlayer(p);
				mPlayers.put(p.getUniqueId(), dp);
			}

			// Add the players to the new party if they don't already have one
			if (getPartyFromId(mPlayers.get(p.getUniqueId())) == null) {
				depthsPlayers.add(mPlayers.get(p.getUniqueId()));
			} else {
				//Add the new players to the current party
				partyToAdd = getPartyFromId(mPlayers.get(p.getUniqueId()));
			}
		}
		// If the players need a new party, create one
		// The constructor will also assign the players to this party
		if (depthsPlayers.size() > 0 && partyToAdd == null) {
			mParties.add(new DepthsParty(depthsPlayers, player.getLocation()));
		} else if (depthsPlayers.size() > 0) {
			//Join the new players to the party that already exists
			for (DepthsPlayer dp : depthsPlayers) {
				partyToAdd.addPlayerToParty(dp);
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
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
		if (dp != null) {
			dp.mAbilities.put(name, level);
		}
		//Tell their party that someone has selected an ability and is eligible for upgrade rooms
		if (!DepthsUtils.isWeaponAspectAbility(name)) {
			DepthsParty party = getPartyFromId(dp);
			if (party != null) {
				party.mHasAtLeastOneAbility = true;
			}
			try {
				for (DepthsPlayer otherPlayer : getPartyFromId(dp).mPlayersInParty) {
					Player newPlayer = Bukkit.getPlayer(otherPlayer.mPlayerId);
					if (newPlayer != null && !newPlayer.equals(p) && level > 0 && level < 6) {
						newPlayer.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + p.getDisplayName() + " now has ability: " + name + " at " + DepthsUtils.getRarityText(level) + " level!");
					} else if (newPlayer != null && !newPlayer.equals(p) && level == 6) {
						newPlayer.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + p.getDisplayName() + " now has ability: " + name + " at " + ChatColor.MAGIC + DepthsUtils.getRarityText(level) + ChatColor.RESET + ChatColor.LIGHT_PURPLE + " level!");
					} else if (newPlayer != null && !newPlayer.equals(p) && level == 0) {
						newPlayer.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + p.getDisplayName() + " has lost ability: " + name + "!");
					}
				}
			} catch (Exception e) {
				Plugin.getInstance().getLogger().info("Error while attempting to set player depths ability");
				e.printStackTrace();
			}
		}
		AbilityManager.getManager().updatePlayerAbilities(p);
	}

	/**
	 * The list of all depths abilities, to be added into the general ability manager so players can trigger their events on the depths shard
	 * @return list of depths abilities
	 */
	public static Collection<? extends DepthsAbility> getAbilities() {

		Plugin plugin = Plugin.getInstance();

		return Arrays.asList(

			//Weapon aspects
			new AxeAspect(plugin, null),
			new RandomAspect(plugin, null),
			new ScytheAspect(plugin, null),
			new SwordAspect(plugin, null),
			new WandAspect(plugin, null),
			new BowAspect(plugin, null),

			//Steelsage abilities
			new FireworkBlast(plugin, null),
			new FocusedCombos(plugin, null),
			new Metalmancy(plugin, null),
			new ProjectileMastery(plugin, null),
			new RapidFire(plugin, null),
			new Scrapshot(plugin, null),
			new Sidearm(plugin, null),
			new SteelStallion(plugin, null),
			new DepthsSharpshooter(plugin, null),
			new DepthsSplitArrow(plugin, null),
			new DepthsVolley(plugin, null),

			//Windwalker abilities
			new Aeromancy(plugin, null),
			new DepthsDodging(plugin, null),
			new GuardingBolt(plugin, null),
			new HowlingWinds(plugin, null),
			new LastBreath(plugin, null),
			new OneWithTheWind(plugin, null),
			new RestoringDraft(plugin, null),
			new Skyhook(plugin, null),
			new Slipstream(plugin, null),
			new Updraft(plugin, null),
			new Whirlwind(plugin, null),

			//Shadow abilities
			new DepthsAdvancingShadows(plugin, null),
			new BladeFlurry(plugin, null),
			new Brutalize(plugin, null),
			new ChaosDagger(plugin, null),
			new DarkCombos(plugin, null),
			new DeadlyStrike(plugin, null),
			new Dethroner(plugin, null),
			new DummyDecoy(plugin, null),
			new CloakOfShadows(plugin, null),
			new ShadowSlam(plugin, null),

			//Dawnbringer abilities
			new BottledSunlight(plugin, null),
			new Enlightenment(plugin, null),
			new LightningBottle(plugin, null),
			new RadiantBlessing(plugin, null),
			new Rejuvenation(plugin, null),
			new SoothingCombos(plugin, null),
			new Sundrops(plugin, null),
			new TotemOfSalvation(plugin, null),
			new WardOfLight(plugin, null),

			//Flamecaller abilities
			new Apocalypse(plugin, null),
			new Detonation(plugin, null),
			new Fireball(plugin, null),
			new FlameSpirit(plugin, null),
			new Flamestrike(plugin, null),
			new PrimordialMastery(plugin, null),
			new Pyroblast(plugin, null),
			new Pyromania(plugin, null),
			new RingOfFlames(plugin, null),
			new VolcanicCombos(plugin, null),
			new VolcanicMeteor(plugin, null),

			// Frostborn abilities
			new Avalanche(plugin, null),
			new Cryobox(plugin, null),
			new FrigidCombos(plugin, null),
			new DepthsFrostNova(plugin, null),
			new FrozenDomain(plugin, null),
			new IceBarrier(plugin, null),
			new Icebreaker(plugin, null),
			new IceLance(plugin, null),
			new Permafrost(plugin, null),
			new PiercingCold(plugin, null),

			// Earthbound abilities
			new BrambleShell(plugin, null),
			new Bulwark(plugin, null),
			new CrushingEarth(plugin, null),
			new DepthsToughness(plugin, null),
			new EarthenCombos(plugin, null),
			new EarthenWrath(plugin, null),
			new Earthquake(plugin, null),
			new Entrench(plugin, null),
			new StoneSkin(plugin, null),
			new Taunt(plugin, null));
	}

	/**
	 * Returns a list of depth abilities filtered by certain trees
	 * @param filter the valid trees to filter by
	 * @return list of filtered abilities
	 */
	public static List<DepthsAbility> getFilteredAbilities(List<DepthsTree> filter) {
		List<DepthsAbility> filteredList = new ArrayList<>();
		for (DepthsAbility da : getAbilities()) {
			if (filter.contains(da.getDepthsTree())) {
				filteredList.add(da);
			}
		}
		return filteredList;
	}

	public static List<WeaponAspectDepthsAbility> getWeaponAspects() {
		return Arrays.asList(
			new AxeAspect(mPlugin, null),
			new RandomAspect(mPlugin, null),
			new ScytheAspect(mPlugin, null),
			new SwordAspect(mPlugin, null),
			new WandAspect(mPlugin, null),
			new BowAspect(mPlugin, null));
	}

	/**
	 * This method generates ability items for the players with random rarities.
	 * In the future, this might be more applicable to work on a per player basis,
	 * with active skills they have influencing rarities or available abilities.
	 */
	private void initItems(List<DepthsTree> filter, boolean isElite, Player p) {
		// Replace this with a dedicated place later
		mItems.clear();
		for (DepthsAbility da : getFilteredAbilities(filter)) {
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

		DepthsPlayer dp = mPlayers.get(p.getUniqueId());

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
		for (DepthsAbility da : getAbilities()) {
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
		if (choice == null) {
			return;
		}
		setPlayerLevelInAbility(choice.mAbility, p, choice.mRarity);
		mAbilityOfferings.remove(p.getUniqueId());
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
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

		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
		if (dp != null) {
			dp.mHasWeaponAspect = true;
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
		} else {
			return;
		}

		List<WeaponAspectDepthsAbility> options = dp.mWeaponOfferings;
		if (options == null) {
			return;
		}
		WeaponAspectDepthsAbility choice = options.get(slot);
		if (choice == null) {
			return;
		}

		if (choice.getDisplayName().equals(RandomAspect.ABILITY_NAME)) {
			//Roll random ability if they selected mystery box
			int[] chances = {40, 40, 20, 0, 0};
			getRandomAbility(p, dp, chances);
		} else {
			setPlayerLevelInAbility(choice.getDisplayName(), p, 1);
		}
	}

	/**
	 * Removes the player from the depths system
	 * @param p the player to remove
	 */
	public void deletePlayer(Player p) {

		// Remove from party
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());

		if (dp != null) {
			DepthsParty party = getPartyFromId(dp);
			party.mPlayersInParty.remove(dp);

			//Delete the party if no players are left
			if (party.mPlayersInParty.size() == 0) {
				mParties.remove(party);
			}
		}

		// If the player is in the system
		if (mPlayers.get(p.getUniqueId()) != null) {
			mPlayers.remove(p.getUniqueId());
			mAbilityOfferings.remove(p.getUniqueId());
			mUpgradeOfferings.remove(p.getUniqueId());

			AbilityManager.getManager().updatePlayerAbilities(p);
			//Reset delve player info
			DelvesUtils.setDelveScore(p, ServerProperties.getShardName(), 0);
			DelvesUtils.removeDelveInfo(p);
		}
	}

	/**
	 * Returns the ability item previews of all abilities the player currently has
	 *
	 * @param p the player to look up
	 * @return ability items for all their active abilities (rarity > 0)
	 */
	public @Nullable List<DepthsAbilityItem> getPlayerAbilitySummary(Player p) {

		DepthsPlayer dp = mPlayers.get(p.getUniqueId());

		// Check if they're in the system
		if (dp == null) {
			return null;
		}

		List<DepthsAbilityItem> abilities = new ArrayList<>();

		// For each ability they have a score for, return the item that says what that ability does

		for (DepthsAbility da : getAbilities()) {
			Integer rarity = dp.mAbilities.get(da.getDisplayName());
			if (rarity != null && rarity.intValue() > 0) {
				abilities.add(da.getAbilityItem(rarity.intValue()));
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
	public @Nullable List<DepthsAbility> getPlayerAbilities(Player p) {

		DepthsPlayer dp = mPlayers.get(p.getUniqueId());

		// Check if they're in the system
		if (dp == null) {
			return null;
		}

		List<DepthsAbility> abilities = new ArrayList<>();

		// For each ability they have a score for, return the item that says what that ability does

		for (DepthsAbility da : getAbilities()) {
			Integer rarity = dp.mAbilities.get(da.getDisplayName());
			if (rarity != null && rarity.intValue() > 0) {
				abilities.add(da);
			}
		}

		return abilities;
	}

	//Simple boolean check to see if the player is already in the depths system
	public boolean isInSystem(Player p) {
		if (mPlayers.get(p.getUniqueId()) == null) {
			return false;
		} else {
			return true;
		}
	}

	// Tells us when a player in the system broke a spawner so we can track their progress
	public void playerBrokeSpawner(Player p, Location l) {
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
		if (dp != null) {
			DepthsParty party = getPartyFromId(dp);
			if (party != null && l.getX() >= party.getRoomX()) {
				getPartyFromId(dp).partyBrokeSpawner(l);
			}
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
		ArrayList<DepthsRoomType> values = new ArrayList<>();
		values.addAll(Arrays.asList(DepthsRoomType.values()));
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
		boolean twisted = false;
		if (mRandom.nextInt(100) < 2 && !party.mTwistedThisFloor) {
			twisted = true;
		}

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

		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
		if (dp != null && getPartyFromId(dp) != null) {
			return generateRoomOptions(getPartyFromId(dp));
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
		// Get the player
		DepthsPlayer dp = mPlayers.get(player.getUniqueId());
		if (dp == null || getPartyFromId(dp).mNextRoomChoices == null || getPartyFromId(dp).mNextRoomChoices.size() == 0) {
			return;
		}
		getPartyFromId(dp).mNextRoomChoices.clear();

		// Summon the boss room if they are on a boss room interval, regardless of whatever
		// the players tried to pull with the gui
		if (getPartyFromId(dp).mRoomNumber % 10 == 9) {
			roomType = DepthsRoomType.BOSS;
		}

		removeNearbyButton(getPartyFromId(dp).mRoomSpawnerLocation, player.getWorld());
		//Remove the button later in case of structure bug
		new BukkitRunnable() {
			@Override
			public void run() {
				removeNearbyButton(getPartyFromId(dp).mRoomSpawnerLocation, player.getWorld());
			}
		}.runTaskLater(mPlugin, 10);

		// Generate the room
		if (mRoomRepository == null) {
			mRoomRepository = new DepthsRoomRepository();
		}

		// Summon the new room and give it to the party
		Location l = new Location(player.getWorld(), getPartyFromId(dp).mRoomSpawnerLocation.getX(), getPartyFromId(dp).mRoomSpawnerLocation.getY(), getPartyFromId(dp).mRoomSpawnerLocation.getZ());
		getPartyFromId(dp).setNewRoom(mRoomRepository.summonRoom(l, roomType, getPartyFromId(dp)));
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
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
		if (dp != null && getPartyFromId(dp) != null) {
			return getPartyFromId(dp).getSummaryString();
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
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());

		if (dp == null || getPartyFromId(dp) == null) {
			//Will be the main way players init their party, spawning the first room
			init(p);
		}

		if (dp != null && getPartyFromId(dp) != null) {

			// Check that spawner count is zero
			if (getPartyFromId(dp).mSpawnersToBreak > 0) {
				if (getPartyFromId(dp).mSpawnersToBreak == 1) {
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "There is still " + getPartyFromId(dp).mSpawnersToBreak + " spawner left to break!");
				} else {
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "There are still " + getPartyFromId(dp).mSpawnersToBreak + " spawners left to break!");
				}
				return;
			}

			// Store the location to spawn the next room from
			getPartyFromId(dp).mRoomSpawnerLocation = new Vector(l.getX(), l.getY(), l.getZ());
			//Let the player select the room
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "opendepthsgui roomchoice " + p.getName());
		}
	}

	/**
	 * Increases treasure score for the player's party by the specified amount
	 * @param l location to play sound
	 * @param p player whose party to increase
	 * @param score amount to increase treasure score
	 */
	public void incrementTreasure(Location l, Player p, int score) {
		if (p == null || mPlayers.get(p.getUniqueId()) == null) {
			return;
		}
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());

		DepthsParty party = getPartyFromId(dp);

		party.giveTreasureReward(l, score);
	}

	/**
	 * This method is called when a player opens a chest
	 * If the room is clear and they haven't picked a reward yet,
	 * they will get the gui to select their reward
	 * @param p player who opened the chest
	 * @return the appropriate GUI to open for their current room reward
	 */
	public Boolean getRoomReward(Player p, Location l) {
		if (p == null || mPlayers.get(p.getUniqueId()) == null) {
			return false;
		}
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());

		DepthsParty party = getPartyFromId(dp);

		//First- check if the player has any rewards to open
		if (dp.mEarnedRewards.size() > 0) {
			if (dp.mEarnedRewards.peek() == DepthsRewardType.ABILITY || dp.mEarnedRewards.peek() == DepthsRewardType.ABILITY_ELITE) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "opendepthsgui ability " + p.getName());

				return true;
			} else if (dp.mEarnedRewards.peek() == DepthsRewardType.UPGRADE || dp.mEarnedRewards.peek() == DepthsRewardType.UPGRADE_ELITE || dp.mEarnedRewards.peek() == DepthsRewardType.TWISTED) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "opendepthsgui upgrade " + p.getName());

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
		List<DepthsAbilityItem> itemChoices = mUpgradeOfferings.get(p.getUniqueId());
		if (itemChoices == null) {
			return;
		}
		if (slot > itemChoices.size() + 1) {
			return;
		}
		DepthsAbilityItem choice = itemChoices.get(slot);
		setPlayerLevelInAbility(choice.mAbility, p, choice.mRarity);
		mUpgradeOfferings.remove(p.getUniqueId());
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
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
		// Move this later

		List<DepthsAbilityItem> offeredItems = mUpgradeOfferings.get(p.getUniqueId());
		if (offeredItems != null && offeredItems.size() > 0) {
			return offeredItems;
		}

		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
		if (dp == null) {
			return null;
		}
		// Return up to 3 random choices of items that are one level above the current level
		ArrayList<? extends DepthsAbility> abilities = new ArrayList<>(getAbilities());
		Collections.shuffle(abilities);
		offeredItems = new ArrayList<>();

		// Loop through all possible abilities and show random ones they have at a higher rarity
		for (DepthsAbility da : abilities) {
			if (offeredItems.size() >= 3) {
				break;
			}
			int level = getPlayerLevelInAbility(da.getDisplayName(), p);
			if (level == 0 || (level >= 5 && !(dp.mEarnedRewards.peek() == DepthsRewardType.TWISTED)) || level >= 6 || da instanceof WeaponAspectDepthsAbility) {
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

		mUpgradeOfferings.put(p.getUniqueId(), offeredItems);

		return offeredItems;
	}

	/**
	 * This method takes a player and, if they are in the system, removes one of their abilities at random
	 * and rolls them two random new abilities at the replaced ability's rarity
	 * @param p Player to roll for
	 */
	public void chaos(Player p) {

		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
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
		setPlayerLevelInAbility(removedAbility, p, 0);
		p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Removed ability: " + removedAbility);
		dp.mUsedChaosThisFloor = true;

		//Give 2 random abilities that aren't the one we just removed

		for (int i = 0; i < 2; i++) {
			List<DepthsAbility> abilities = getFilteredAbilities(dp.mEligibleTrees);

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

				abilities.removeIf(ability -> blockedTriggers.contains(ability.getTrigger()));
			}

			Collections.shuffle(abilities);
			for (DepthsAbility da : abilities) {
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
	 * @param p player- get their party and send them to next floor
	 */
	public void goToNextFloor(Player p) {
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
		DepthsParty party = getPartyFromId(dp);
		int partyFloor = party.getFloor();
		if (dp == null || getPartyFromId(dp) == null) {
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Player not in depths system!");
			return;
		}
		int treasureScoreIncrease = TREASURE_PER_FLOOR * partyFloor + 2;
		party.mTreasureScore += treasureScoreIncrease;

		//Check to see if they've finished the run (normal mode) and send to loot rooms
		if (partyFloor == 3 && !party.mEndlessMode) {
			List<DepthsPlayer> playersToLoop = new ArrayList<>(party.mPlayersInParty);
			for (DepthsPlayer playerInParty : playersToLoop) {
				Player player = Bukkit.getPlayer(playerInParty.mPlayerId);
				if (player != null) {
					DepthsUtils.storetoFile(dp, Plugin.getInstance().getDataFolder() + File.separator + "DepthsStats"); //Save the player's stats
					dp.mFinalTreasureScore = party.mTreasureScore;
					player.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Congratulations! Your final treasure score is " + dp.mFinalTreasureScore + "!");
					getPartyFromId(dp).populateLootRoom(player, false);
					MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a [\"\",{\"text\":\"" + player.getName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Darkest Depths!\",\"color\":\"white\",\"italic\":true,\"bold\":false}]");
					//Set score
					ScoreboardUtils.setScoreboardValue(player, "Depths", ScoreboardUtils.getScoreboardValue(player, "Depths").orElse(0) + 1);
					Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + player.getName() + " Depths");
				}
			}
			return;
		} else if (partyFloor == 3) {
			for (DepthsPlayer playerInParty : party.mPlayersInParty) {
				Player player = Bukkit.getPlayer(playerInParty.mPlayerId);
				if (player == null || !player.isOnline()) {
					continue;
				}

				//Set score
				ScoreboardUtils.setScoreboardValue(player, "Depths", ScoreboardUtils.getScoreboardValue(player, "Depths").orElse(0) + 1);
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + player.getName() + " Depths");
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
			if (EntityUtils.isHostileMob(e) && (e.getLocation().getBlockX() / 512 == p.getLocation().getBlockX() / 512) && (e.getLocation().getBlockZ() / 512 == p.getLocation().getBlockZ())) {
				e.remove();
			}
		}

		if (mRoomRepository == null) {
			mRoomRepository = new DepthsRoomRepository();
		}
		mRoomRepository.goToNextFloor(getPartyFromId(dp), treasureScoreIncrease);
		getPartyFromId(dp).mBeatBoss = true;
	}

	/**
	 * This method starts a bossfight for the given player's party depending on their current floor
	 * The bosses themselves will handle reading armor stands for cleaning up the arena
	 * @param p player to get party of
	 * @param l location to summon boss
	 */
	public void startBossFight(Player p, Location l) {
		//Check the player is in the system
		DepthsPlayer dp = mPlayers.get(p.getUniqueId());
		if (dp == null || getPartyFromId(dp) == null) {
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Player not in depths system!");
			return;
		}
		//Teleport all players in party to the player activating the fight
		for (DepthsPlayer dpInParty : getPartyFromId(dp).mPlayersInParty) {
			try {
				if (dpInParty != dp) {
					Player playerToTp = Bukkit.getPlayer(dpInParty.mPlayerId);
					if (playerToTp.getLocation().distance(l) > 20) {
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
		if (getPartyFromId(dp).getFloor() % 3 == 1) {
			losName = HEDERA_LOS;
			bossTag = "boss_hedera";
		} else if (getPartyFromId(dp).getFloor() % 3 == 2) {
			losName = DAVEY_LOS;
			bossTag = "boss_davey";
		} else {
			losName = NUCLEUS_LOS;
			bossTag = "boss_nucleus";
		}

		try {
			Entity entity = LibraryOfSoulsIntegration.summon(l, losName);
			if (entity != null && entity instanceof LivingEntity) {
				BossManager.createBoss(null, (LivingEntity)entity, bossTag, l.clone().add(0, -2, 0));
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
		List<DepthsAbility> abilities = getFilteredAbilities(dp.mEligibleTrees);

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

			abilities.removeIf(ability -> blockedTriggers.contains(ability.getTrigger()));
		}

		Collections.shuffle(abilities);
		for (DepthsAbility da : abilities) {
			if (da.canBeOffered(p)) {
				int roll = mRandom.nextInt(100) + 1;
				if (roll < addUpChances(0, chances)) {
					setPlayerLevelInAbility(da.getDisplayName(), p, 1);
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You gained ability " + da.getDisplayName() + " at " + DepthsUtils.getRarityText(1) + " level!");
				} else if (roll < addUpChances(1, chances)) {
					setPlayerLevelInAbility(da.getDisplayName(), p, 2);
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You gained ability " + da.getDisplayName() + " at " + DepthsUtils.getRarityText(2) + " level!");
				} else if (roll < addUpChances(2, chances)) {
					setPlayerLevelInAbility(da.getDisplayName(), p, 3);
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You gained ability " + da.getDisplayName() + " at " + DepthsUtils.getRarityText(3) + " level!");
				} else if (roll < addUpChances(3, chances)) {
					setPlayerLevelInAbility(da.getDisplayName(), p, 4);
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You gained ability " + da.getDisplayName() + " at " + DepthsUtils.getRarityText(4) + " level!");
				} else {
					setPlayerLevelInAbility(da.getDisplayName(), p, 5);
					p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You gained ability " + da.getDisplayName() + " at " + DepthsUtils.getRarityText(5) + " level!");
				}
				break;
			}
		}
	}

	/**
	 * Debug method to set party room number manually
	 * @param player player to get party from
	 * @param number room number to set
	 */
	public void setRoomDebug(Player player, int number) {
		DepthsPlayer dp = mPlayers.get(player.getUniqueId());
		if (dp == null || getPartyFromId(dp) == null) {
			player.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Player not in depths system!");
			return;
		}
		getPartyFromId(dp).mRoomNumber = number;
	}
}
