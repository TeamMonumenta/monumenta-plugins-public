package com.playmonumenta.plugins.depths;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.depths.abilities.aspects.AxeAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.RandomAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.ScytheAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.SwordAspect;
import com.playmonumenta.plugins.depths.abilities.aspects.WandAspect;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfAnchoring;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfDeath;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfDependency;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfEnvy;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfGluttony;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfGreed;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfImpatience;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfLust;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfObscurity;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfPessimism;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfRedundancy;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfRuin;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfSloth;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfSobriety;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.BottledSunlight;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.DepthsRejuvenation;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.DivineBeam;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Enlightenment;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.EternalSavior;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.LightningBottle;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.RadiantBlessing;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.SoothingCombos;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.SparkOfInspiration;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Sundrops;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.WardOfLight;
import com.playmonumenta.plugins.depths.abilities.earthbound.BrambleShell;
import com.playmonumenta.plugins.depths.abilities.earthbound.Bulwark;
import com.playmonumenta.plugins.depths.abilities.earthbound.CrushingEarth;
import com.playmonumenta.plugins.depths.abilities.earthbound.DepthsToughness;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenCombos;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenWrath;
import com.playmonumenta.plugins.depths.abilities.earthbound.Earthquake;
import com.playmonumenta.plugins.depths.abilities.earthbound.Entrench;
import com.playmonumenta.plugins.depths.abilities.earthbound.IronGrip;
import com.playmonumenta.plugins.depths.abilities.earthbound.Taunt;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Apocalypse;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Detonation;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Fireball;
import com.playmonumenta.plugins.depths.abilities.flamecaller.FlameSpirit;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Flamestrike;
import com.playmonumenta.plugins.depths.abilities.flamecaller.IgneousRune;
import com.playmonumenta.plugins.depths.abilities.flamecaller.PrimordialMastery;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Pyroblast;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Pyromania;
import com.playmonumenta.plugins.depths.abilities.flamecaller.VolcanicCombos;
import com.playmonumenta.plugins.depths.abilities.flamecaller.VolcanicMeteor;
import com.playmonumenta.plugins.depths.abilities.frostborn.Avalanche;
import com.playmonumenta.plugins.depths.abilities.frostborn.Cryobox;
import com.playmonumenta.plugins.depths.abilities.frostborn.FrigidCombos;
import com.playmonumenta.plugins.depths.abilities.frostborn.FrozenDomain;
import com.playmonumenta.plugins.depths.abilities.frostborn.IceBarrier;
import com.playmonumenta.plugins.depths.abilities.frostborn.IceLance;
import com.playmonumenta.plugins.depths.abilities.frostborn.Icebreaker;
import com.playmonumenta.plugins.depths.abilities.frostborn.Permafrost;
import com.playmonumenta.plugins.depths.abilities.frostborn.PiercingCold;
import com.playmonumenta.plugins.depths.abilities.frostborn.Snowstorm;
import com.playmonumenta.plugins.depths.abilities.prismatic.Abnormality;
import com.playmonumenta.plugins.depths.abilities.prismatic.Charity;
import com.playmonumenta.plugins.depths.abilities.prismatic.ChromaBlade;
import com.playmonumenta.plugins.depths.abilities.prismatic.ColorSplash;
import com.playmonumenta.plugins.depths.abilities.prismatic.Convergence;
import com.playmonumenta.plugins.depths.abilities.prismatic.DiscoBall;
import com.playmonumenta.plugins.depths.abilities.prismatic.Diversity;
import com.playmonumenta.plugins.depths.abilities.prismatic.Encore;
import com.playmonumenta.plugins.depths.abilities.prismatic.Flexibility;
import com.playmonumenta.plugins.depths.abilities.prismatic.Generosity;
import com.playmonumenta.plugins.depths.abilities.prismatic.Multiplicity;
import com.playmonumenta.plugins.depths.abilities.prismatic.Opportunity;
import com.playmonumenta.plugins.depths.abilities.prismatic.Prosperity;
import com.playmonumenta.plugins.depths.abilities.prismatic.Rebirth;
import com.playmonumenta.plugins.depths.abilities.prismatic.Refraction;
import com.playmonumenta.plugins.depths.abilities.prismatic.SolarRay;
import com.playmonumenta.plugins.depths.abilities.shadow.BladeFlurry;
import com.playmonumenta.plugins.depths.abilities.shadow.Brutalize;
import com.playmonumenta.plugins.depths.abilities.shadow.ChaosDagger;
import com.playmonumenta.plugins.depths.abilities.shadow.CloakOfShadows;
import com.playmonumenta.plugins.depths.abilities.shadow.DarkCombos;
import com.playmonumenta.plugins.depths.abilities.shadow.DeadlyStrike;
import com.playmonumenta.plugins.depths.abilities.shadow.DepthsAdvancingShadows;
import com.playmonumenta.plugins.depths.abilities.shadow.DepthsDethroner;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.depths.abilities.shadow.EscapeArtist;
import com.playmonumenta.plugins.depths.abilities.shadow.PhantomForce;
import com.playmonumenta.plugins.depths.abilities.shadow.ShadowSlam;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsSharpshooter;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsSplitArrow;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsVolley;
import com.playmonumenta.plugins.depths.abilities.steelsage.FireworkBlast;
import com.playmonumenta.plugins.depths.abilities.steelsage.FocusedCombos;
import com.playmonumenta.plugins.depths.abilities.steelsage.GravityBomb;
import com.playmonumenta.plugins.depths.abilities.steelsage.PrecisionStrike;
import com.playmonumenta.plugins.depths.abilities.steelsage.RapidFire;
import com.playmonumenta.plugins.depths.abilities.steelsage.Scrapshot;
import com.playmonumenta.plugins.depths.abilities.steelsage.Sidearm;
import com.playmonumenta.plugins.depths.abilities.steelsage.SteelStallion;
import com.playmonumenta.plugins.depths.abilities.windwalker.Aeroblast;
import com.playmonumenta.plugins.depths.abilities.windwalker.Aeromancy;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsDodging;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsWindWalk;
import com.playmonumenta.plugins.depths.abilities.windwalker.GuardingBolt;
import com.playmonumenta.plugins.depths.abilities.windwalker.LastBreath;
import com.playmonumenta.plugins.depths.abilities.windwalker.OneWithTheWind;
import com.playmonumenta.plugins.depths.abilities.windwalker.RestoringDraft;
import com.playmonumenta.plugins.depths.abilities.windwalker.Skyhook;
import com.playmonumenta.plugins.depths.abilities.windwalker.ThundercloudForm;
import com.playmonumenta.plugins.depths.abilities.windwalker.Whirlwind;
import com.playmonumenta.plugins.depths.abilities.windwalker.WindsweptCombos;
import com.playmonumenta.plugins.depths.guis.DepthsGUICommands;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.depths.rooms.RoomRepository;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MMLog;
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
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
 * <p>
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
	//The amount of mobs that should be killed in order to open the next room (anti speedrun)
	public static final double MOBS_PER_SPAWNER = 1.75;

	//Tag to check for endless mode
	public static final String ENDLESS_MODE_STRING = "DepthsEndlessMode";

	//Access scoreboard for depths
	public static final String DEPTHS_ACCESS = "DDAccess";

	//Access scoreboard for zenith
	public static final String ZENITH_ACCESS = "DCZAccess";

	//Tag to check for rigged talisman (in endless mode)
	public static final String RIGGED_STRING = "DepthsRigged";

	//Treasure score amount for defeating rooms
	public static final int TREASURE_PER_NORMAL = 3;
	public static final int TREASURE_PER_ELITE = 5;
	//Will be multiplied by the current floor number the party is on, ex. f2 is 5 * 2
	public static final int TREASURE_PER_FLOOR = 8;
	//How often the depths data is saved normally
	public static final int SAVE_INTERVAL = 60 * 5 * 20; //5 min

	public static final String PLAYER_SPAWN_STAND_NAME = "PlayerSpawn";

	public static final String FLOOR_LOBBY_LOAD_STAND_NAME = "LobbyLoadPoint";

	public static final String LOOT_ROOM_STAND_NAME = "DepthsLootRoom";

	public static final String DEATH_WAITING_ROOM_STAND_NAME = "DepthsDeathWaitingRoom";

	public static final String PAID_SCOREBOARD_TAG = "DepthsWeaponAspectUpgradeBought";

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
	public @Nullable RoomRepository mRoomRepository;
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
	 *
	 * @param logger     logs info
	 * @param configPath path to look for the file data
	 * @return whether a file with depths data exists (and was successfully loaded)
	 */
	public boolean load(Logger logger, String configPath) {
		mConfigPath = configPath;
		//Attempt to load save data from the file
		try {
			String playerContent = FileUtils.readFile(mConfigPath + "players.json");
			String partyContent = FileUtils.readFile(mConfigPath + "parties.json");
			if (playerContent.isEmpty()) {
				logger.warning("Depths" + mConfigPath + "' is empty - defaulting to new depths manager");
			} else {
				Gson gson = new Gson();
				Type playerType = new TypeToken<Map<UUID, DepthsPlayer>>() {
				}.getType();

				mPlayers = gson.fromJson(playerContent, playerType);

				Type partyType = new TypeToken<ArrayList<DepthsParty>>() {
				}.getType();
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
	 *
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
			MMLog.severe("Caught exception saving file '" + tempFilePath + "': " + e);
			e.printStackTrace();
		}
		try {
			FileUtils.moveFile(path + "players.json" + tempFilePath, path + "players.json");
			FileUtils.moveFile(path + "parties.json" + tempFilePath, path + "parties.json");
		} catch (Exception e) {
			MMLog.severe("Caught exception renaming file '" + tempFilePath + "' to '" + path + "': " + e);
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
	 *
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

	public void setPlayerLevelInAbility(@Nullable String name, Player p, int level) {
		setPlayerLevelInAbility(name, p, level, true);
	}

	public void setPlayerLevelInAbility(@Nullable String name, Player p, int level, boolean announceToTeam) {
		setPlayerLevelInAbility(name, p, level, announceToTeam, false);
	}

	/**
	 * Sets the player level in a specific ability
	 *
	 * @param name  the name of the ability's ABILITY_NAME attribute. Needs to be exact!
	 * @param p     the player to give it to
	 * @param level the rarity level of the ability
	 */
	public void setPlayerLevelInAbility(@Nullable String name, Player p, int level, boolean announceToTeam, boolean announceToSelf) {
		if (name == null) {
			return;
		}
		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp != null) {
			int previousLevel = dp.mAbilities.getOrDefault(name, 0);
			int displayLevel = level == 0 ? previousLevel : level;
			if (level > 0) {
				dp.mAbilities.put(name, level);
			} else {
				dp.mAbilities.remove(name);
			}
			AbilityManager.getManager().updatePlayerAbilities(p, false);

			DepthsAbilityInfo<?> info = getAbility(name);
			if (info == null) {
				return;
			}
			if (previousLevel == 0 && level > 0) {
				info.onGain(p);
			}

			//Adjust wand aspect active logic
			DepthsTree tree = info.getDepthsTree();
			if (dp.mWandAspectCharges > 0 && previousLevel == 0 && Arrays.asList(DepthsTree.OWNABLE_TREES).contains(tree) && info.getDepthsTrigger().isActive()) {
				dp.mWandAspectCharges--;
			}

			DepthsParty party = getPartyFromId(dp);
			if (party == null) {
				return;
			}
			party.mHasAtLeastOneAbility = true;

			if (announceToTeam || announceToSelf) {
				//Tell their party that someone has selected an ability and is eligible for upgrade rooms
				Component teamMessage = null;
				Component playerMessage = null;
				Component playerName = p.displayName();
				Component abilityName = colorAbilityWithHover(name, displayLevel, p);
				if (tree != null) {
					if (level == 0) {
						Component message = Component.text("lost ability: ").append(abilityName).append(Component.text("!"));
						teamMessage = playerName.append(Component.text(" has ")).append(message);
						playerMessage = Component.text("You ").append(message);
					} else if (previousLevel > 0) {
						String direction = previousLevel <= level ? "upgraded" : "downgraded";
						Component message = Component.text(" " + direction + " ability: ").append(abilityName).append(Component.text(" to ")).append(DepthsUtils.getRarityComponent(level)).append(Component.text(" level!"));
						teamMessage = playerName.append(message);
						playerMessage = Component.text("You ").append(message);
					} else {
						Component message = Component.text("ability: ").append(abilityName);
						if (info.getHasLevels()) {
							message = message.append(Component.text(" at ")).append(DepthsUtils.getRarityComponent(level)).append(Component.text(" level!"));
						} else {
							message = message.append(Component.text("!"));
						}
						teamMessage = playerName.append(Component.text(" now has ")).append(message);
						playerMessage = Component.text("You now have ").append(message);
					}
				} else {
					if (level == 1) {
						Component message = Component.text("selected ").append(abilityName).append(Component.text(" as their aspect!"));
						teamMessage = playerName.append(Component.text(" has ")).append(message);
						playerMessage = Component.text("You have ").append(message);
					} else if (level == 2) {
						teamMessage = playerName.append(Component.text(" has had their Mystery Box transform into ").append(abilityName).append(Component.text("!")));
						playerMessage = Component.text("Your Mystery Box has transformed into ").append(abilityName).append(Component.text("!"));
					}
				}

				if (announceToTeam && teamMessage != null) {
					party.sendMessage(teamMessage, o -> o != dp);
				}
				if (announceToSelf && playerMessage != null) {
					dp.sendMessage(playerMessage);
				}
			}
		}
	}

	public Component colorAbilityWithHover(String name, int rarity, Player p) {
		DepthsAbilityInfo<?> info = getAbility(name);
		if (info == null) {
			return Component.text(name);
		}
		return info.getNameWithHover(rarity, p);
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
			GravityBomb.INFO,
			PrecisionStrike.INFO,
			RapidFire.INFO,
			Scrapshot.INFO,
			Sidearm.INFO,
			SteelStallion.INFO,
			DepthsSharpshooter.INFO,
			DepthsSplitArrow.INFO,
			DepthsVolley.INFO,

			//Windwalker abilities
			Aeroblast.INFO,
			Aeromancy.INFO,
			DepthsDodging.INFO,
			GuardingBolt.INFO,
			ThundercloudForm.INFO,
			LastBreath.INFO,
			OneWithTheWind.INFO,
			RestoringDraft.INFO,
			Skyhook.INFO,
			DepthsWindWalk.INFO,
			Whirlwind.INFO,
			WindsweptCombos.INFO,

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
			EscapeArtist.INFO,
			PhantomForce.INFO,

			//Dawnbringer abilities
			BottledSunlight.INFO,
			Enlightenment.INFO,
			LightningBottle.INFO,
			RadiantBlessing.INFO,
			DepthsRejuvenation.INFO,
			SoothingCombos.INFO,
			Sundrops.INFO,
			WardOfLight.INFO,
			DivineBeam.INFO,
			EternalSavior.INFO,
			SparkOfInspiration.INFO,

			//Flamecaller abilities
			Apocalypse.INFO,
			Detonation.INFO,
			Fireball.INFO,
			FlameSpirit.INFO,
			Flamestrike.INFO,
			PrimordialMastery.INFO,
			Pyroblast.INFO,
			Pyromania.INFO,
			VolcanicCombos.INFO,
			VolcanicMeteor.INFO,
			IgneousRune.INFO,

			// Frostborn abilities
			Avalanche.INFO,
			Cryobox.INFO,
			FrigidCombos.INFO,
			Snowstorm.INFO,
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
			IronGrip.INFO,
			Taunt.INFO,

			// Prismatic abilities
			Abnormality.INFO,
			Encore.INFO,
			DiscoBall.INFO,
			Generosity.INFO,
			Charity.INFO,
			SolarRay.INFO,
			Convergence.INFO,
			Flexibility.INFO,
			Multiplicity.INFO,
			Prosperity.INFO,
			Refraction.INFO,
			Rebirth.INFO,
			ColorSplash.INFO,
			Diversity.INFO,
			ChromaBlade.INFO,
			Opportunity.INFO,

			// Curses
			CurseOfAnchoring.INFO,
			CurseOfDeath.INFO,
			CurseOfDependency.INFO,
			CurseOfEnvy.INFO,
			CurseOfGluttony.INFO,
			CurseOfGreed.INFO,
			CurseOfImpatience.INFO,
			CurseOfLust.INFO,
			CurseOfObscurity.INFO,
			CurseOfPessimism.INFO,
			CurseOfRedundancy.INFO,
			CurseOfRuin.INFO,
			CurseOfSloth.INFO,
			CurseOfSobriety.INFO
		);
	}

	public static List<DepthsAbilityInfo<?>> getFilteredAbilities(List<DepthsTree> filter) {
		return getFilteredAbilities(filter, List.of());
	}

	/**
	 * Returns a list of depth abilities filtered by certain trees
	 *
	 * @param filter the valid trees to filter by
	 * @return list of filtered abilities
	 */
	public static List<DepthsAbilityInfo<?>> getFilteredAbilities(List<DepthsTree> filter, List<DepthsTree> noActiveTrees) {
		List<DepthsAbilityInfo<?>> filteredList = new ArrayList<>();
		for (DepthsAbilityInfo<?> da : getAbilities()) {
			DepthsTree tree = da.getDepthsTree();
			if (filter.contains(tree) && !(da.getDepthsTrigger() != DepthsTrigger.PASSIVE && noActiveTrees.contains(tree))) {
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

	public static List<DepthsAbilityInfo<?>> getAbilitiesOfTree(DepthsTree tree) {
		return getAbilities().stream().filter(info -> info.getDepthsTree() == tree).toList();
	}

	public static List<DepthsAbilityInfo<?>> getPrismaticAbilities() {
		return getAbilitiesOfTree(DepthsTree.PRISMATIC);
	}

	public static List<DepthsAbilityInfo<?>> getCurseAbilities() {
		return getAbilitiesOfTree(DepthsTree.CURSE);
	}

	private void initItems(List<DepthsTree> filter, Player p, DepthsPlayer dp) {
		initItems(filter, false, p, dp, List.of());
	}

	/**
	 * This method generates ability items for the players with random rarities.
	 */
	private void initItems(List<DepthsTree> filter, boolean isElite, Player p, DepthsPlayer dp, List<DepthsTree> noActiveTrees) {
		// Replace this with a dedicated place later
		mItems.clear();

		int forceLevel = filter.contains(DepthsTree.PRISMATIC) ? dp.mAbnormalityLevel : 0;

		for (DepthsAbilityInfo<?> da : getFilteredAbilities(filter, noActiveTrees)) {
			int rarity;
			if (forceLevel > 0) {
				rarity = forceLevel;
				if (da == Abnormality.INFO) {
					continue;
				}
			} else {
				// Get a number 1 to 100
				int roll = mRandom.nextInt(100) + 1;

				//Add enlightenment level to roll if applicable
				Enlightenment enlightenment = Plugin.getInstance().mAbilityManager.getPlayerAbilityIgnoringSilence(p, Enlightenment.class);
				if (enlightenment != null) {
					roll += (int) (enlightenment.getRarityIncrease() * 100);
				}
				// Add Diversity's rarity increase to roll if applicable
				Diversity diversity = Plugin.getInstance().mAbilityManager.getPlayerAbilityIgnoringSilence(p, Diversity.class);
				if (diversity != null) {
					roll += (int) (diversity.getRarityIncrease() * 100);
				}
				DepthsParty party = getPartyFromId(dp);
				if (party != null && party.getAscension() >= DepthsEndlessDifficulty.ASCENSION_STARTING_RARITY) {
					roll -= DepthsEndlessDifficulty.ASCENSION_STARTING_RARITY_AMOUNT;
				}

				if (isElite) {
					if (roll < 46) {
						//UNCOMMON RARITY- 45%
						rarity = 2;
					} else if (roll < 76) {
						//RARE RARITY- 30%
						rarity = 3;
					} else if (roll < 91) {
						//EPIC RARITY- 15%
						rarity = 4;
					} else {
						//LEGENDARY RARITY- 10%
						rarity = 5;
					}
				} else {
					if (roll < 41) {
						//COMMON RARITY- 40%
						rarity = 1;
					} else if (roll < 71) {
						//UNCOMMON RARITY- 30%
						rarity = 2;
					} else if (roll < 91) {
						//RARE RARITY- 20%
						rarity = 3;
					} else {
						//EPIC RARITY- 10%
						rarity = 4;
					}
				}
			}

			DepthsAbilityItem item = da.getAbilityItem(rarity);
			if (item != null) {
				mItems.add(item);
			}
		}

		if (forceLevel > 0) {
			// We've used this abnormality prismatic selection
			dp.mAbnormalityLevel = 0;
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
		DepthsParty party = getPartyFromId(dp);
		if (dp == null || party == null) {
			return null;
		}

		List<DepthsAbilityItem> offeredItems = mAbilityOfferings.get(p.getUniqueId());
		if (offeredItems == null || offeredItems.size() == 0) {
			//Filter the item offerings by the player's eligible trees for that run
			DepthsRewardType reward = dp.mEarnedRewards.peek();
			if (reward == DepthsRewardType.PRISMATIC) {
				List<DepthsTree> prismaticFilter = new ArrayList<>();
				prismaticFilter.add(DepthsTree.PRISMATIC);
				initItems(prismaticFilter, p, dp);
			} else if (reward == DepthsRewardType.CURSE) {
				List<DepthsTree> curseFilter = new ArrayList<>();
				curseFilter.add(DepthsTree.CURSE);
				initItems(curseFilter, p, dp);
			} else {
				List<DepthsTree> cappedTrees = new ArrayList<>();
				if (party.getAscension() >= DepthsEndlessDifficulty.ASCENSION_ACTIVE_TREE_CAP) {
					List<DepthsTree> trees = getActiveAbilityTrees(dp);
					cappedTrees = Arrays.stream(DepthsTree.values()).filter(tree -> trees.stream().filter(t -> t == tree).count() >= 4).toList();
				}
				initItems(dp.mEligibleTrees, reward == DepthsRewardType.ABILITY_ELITE, p, dp, cappedTrees);
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
		int options = 3;
		Prosperity prosperity = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(p, Prosperity.class);
		if (prosperity != null) {
			options += prosperity.getExtraChoices();
		}

		for (DepthsAbilityItem item : mItems) {
			if (offeredItems.size() >= options) {
				break;
			}
			if (allowedAbilities.contains(item.mAbility)) {
				offeredItems.add(item);
			}
		}

		// Prevent getting no ability options due to wand aspect
		if (offeredItems.isEmpty() && dp.mWandAspectCharges > 0) {
			dp.mWandAspectCharges = 0;
			// Recalculate options. Will never call itself more than once
			return getAbilityUnlocks(p);
		}

		mAbilityOfferings.put(p.getUniqueId(), offeredItems);

		return offeredItems;
	}

	/**
	 * This method is called when the player selects which ability to upgrade from the gui options
	 *
	 * @param p    the player to give the ability to
	 * @param slot the index to give from their offerings array
	 */
	public void playerChoseItem(Player p, int slot, boolean sendMessage) {
		List<DepthsAbilityItem> itemChoices = mAbilityOfferings.get(p.getUniqueId());
		if (itemChoices == null || slot >= itemChoices.size()) {
			return;
		}
		DepthsAbilityItem choice = itemChoices.get(slot);
		if (choice == null || choice.mAbility == null) {
			return;
		}
		setPlayerLevelInAbility(choice.mAbility, p, choice.mRarity, true, sendMessage);
		mAbilityOfferings.remove(p.getUniqueId());
		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp != null) {
			dp.mEarnedRewards.poll();
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		}
	}

	/**
	 * Player chose the weapon aspect at the start of the run
	 *
	 * @param p    player
	 * @param slot which element in the array they picked
	 */
	public void playerChoseWeaponAspect(Player p, int slot) {

		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp != null) {
			dp.mHasWeaponAspect = true;
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
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
		String displayName = choice.getDisplayName();
		if (displayName == null) {
			return;
		}

		if (choice == RandomAspect.INFO) {
			//Roll random ability if they selected mystery box
			int[] chances = {40, 40, 20, 0, 0};
			boolean twisted = DepthsUtils.getDepthsContent() == DepthsContent.CELESTIAL_ZENITH;
			getRandomAbility(p, dp, chances, null, twisted);
		}
		setPlayerLevelInAbility(displayName, p, 1);
	}

	/**
	 * Removes the player from the depths system
	 *
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

				//Delete the party if no players are left-
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

		// Called when the player logs out even if they are still alive - check that they are online
		if (p.isOnline()) {
			InventoryUtils.removeSpecialItems(p, false, true, false);
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
			int rarity = dp.mAbilities.getOrDefault(da.getDisplayName(), 0);
			if (rarity > 0) {
				abilities.add(da.getAbilityItem(rarity, p));
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
	public List<DepthsAbilityInfo<?>> getPlayerAbilities(@Nullable Player p) {
		if (p == null) {
			return new ArrayList<>();
		}
		return getPlayerAbilities(getDepthsPlayer(p));
	}

	public List<DepthsAbilityInfo<?>> getPlayerAbilities(@Nullable DepthsPlayer dp) {
		if (dp == null) {
			return new ArrayList<>();
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

	public boolean isAlive(Player p) {
		DepthsPlayer dp = getDepthsPlayer(p);
		return dp != null && !dp.mDead;
	}

	// Tells us when a player in the system broke a spawner, so we can track their progress
	public void playerBrokeSpawner(Player p, Location l) {
		DepthsParty party = getDepthsParty(p);
		if (party != null && l.getX() >= party.getRoomX()) {
			party.partyBrokeSpawner(l);
		}
	}

	// Anti-speedrun: count mob kills
	public void playerKilledMob(Player p) {
		DepthsParty party = getDepthsParty(p);
		if (party != null) {
			party.playerKilledMob();
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
		if (DepthsUtils.getDepthsContent() == DepthsContent.CELESTIAL_ZENITH) {
			// Likely lower options by 1 since no treasure rooms
			if (mRandom.nextInt(3) > 0) {
				choices--;
			}
		}
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
		//Also remove half of utility rooms with ascension 2 of depths 2
		if ((party.mRoomNumber >= 30 || party.getAscension() >= DepthsEndlessDifficulty.ASCENSION_UTILITY_ROOMS) && mRandom.nextInt(2) > 0) {
			values.remove(DepthsRoomType.UTILITY);
		}
		boolean twisted = false;
		// In Depths 2, remove treasure and twisted options
		if (DepthsUtils.getDepthsContent() == DepthsContent.CELESTIAL_ZENITH) {
			values.remove(DepthsRoomType.TREASURE);
			values.remove(DepthsRoomType.TREASURE_ELITE);
		} else {
			//Roll chance for twisted room - 1.5% per room selection
			twisted = mRandom.nextInt(100) < 2 && !party.mTwistedThisFloor;
			//Remove wildcard option
			values.remove(DepthsRoomType.WILDCARD);
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
	 *
	 * @param roomType the type of room the party selected
	 * @param player   the player that selected the room
	 */
	public void playerSelectedRoom(DepthsRoomType roomType, Player player) {
		DepthsParty party = getDepthsParty(player);
		if (player == null || party == null || party.mNextRoomChoices == null || party.mNextRoomChoices.isEmpty() || party.mRoomSpawnerLocation == null || party.mIsLoadingRoom) {
			return;
		}
		if (party.mSpawnedForcedCleansingRoom && !party.isAscensionPurgeMet()) {
			party.sendMessage("Each player must remove an ability before moving on!");
			return;
		}
		party.mNextRoomChoices.clear();

		// Summon the boss room if they are on a boss room interval, regardless of whatever
		// the players tried to pull with the gui
		if (party.mRoomNumber % 10 == 9) {
			roomType = DepthsRoomType.BOSS;
		}

		boolean wildcard = roomType == DepthsRoomType.WILDCARD;
		if (wildcard) {
			incrementTreasure(null, player, 1);
			roomType = getWildcardRoomType();
		}

		World world = player.getWorld();
		Vector roomSpawnerLocation = party.mRoomSpawnerLocation;
		removeNearbyButton(roomSpawnerLocation, world);
		//Remove the button later in case of structure bug
		Bukkit.getScheduler().runTaskLater(mPlugin, () ->
			removeNearbyButton(roomSpawnerLocation, world), 10);

		// Generate the room
		if (mRoomRepository == null) {
			mRoomRepository = DepthsUtils.getDepthsContent().getRoomRepository();
		}

		// Summon the new room and give it to the party
		Location l = new Location(world, roomSpawnerLocation.getX(), roomSpawnerLocation.getY(), roomSpawnerLocation.getZ());
		party.setNewRoom(mRoomRepository.summonRoom(l, roomType, party), wildcard);
	}

	private DepthsRoomType getWildcardRoomType() {
		List<DepthsRoomType> roomOptions = new ArrayList<>(Arrays.asList(DepthsRoomType.values()));
		roomOptions.remove(DepthsRoomType.BOSS);
		roomOptions.remove(DepthsRoomType.TREASURE);
		roomOptions.remove(DepthsRoomType.TREASURE_ELITE);
		roomOptions.remove(DepthsRoomType.TWISTED);
		roomOptions.remove(DepthsRoomType.WILDCARD);

		return roomOptions.get(mRandom.nextInt(roomOptions.size()));
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
	 *
	 * @param p player to get party summary for
	 * @return a string containing the party summary
	 */
	public Component getPartySummary(Player p) {
		DepthsParty party = getDepthsParty(p);
		if (party != null) {
			return party.getSummaryComponent();
		} else {
			return Component.text("You are not currently in a depths party!");
		}
	}

	public boolean getIsEndless(Player p) {
		DepthsParty party = getDepthsParty(p);
		if (party != null) {
			return party.mEndlessMode;
		} else {
			return false;
		}
	}

	/**
	 * This method is called by the command block at the end of each room.
	 * If the room is clear, it opens the next room selection gui
	 *
	 * @param p player to generate rooms for
	 * @param l the location to spawn the next room at, in order to line up with the door
	 */
	public void gotRoomEndpoint(Player p, Location l) {
		DepthsPlayer dp = getDepthsPlayer(p);
		DepthsParty party = getPartyFromId(dp);

		if (party == null) {
			//Will be the main way players init their party, spawning the first room
			init(p);
			party = getPartyFromId(dp);
		}

		if (dp != null && party != null) {
			// Check that spawner count is zero
			if (party.mSpawnersToBreak > 0) {
				dp.sendMessage("There " + (party.mSpawnersToBreak > 1 ? "are" : "is") + " still " + party.mSpawnersToBreak + " spawner" + (party.mSpawnersToBreak > 1 ? "s" : "") + " left to break!");
				return;
			}

			if (party.mMobsToKill > 0 && p.hasPermission("monumenta.depths.mobanticheese")) {
				Location hitboxLoc = l.clone();
				hitboxLoc.setDirection(new Vector(-1, 0, 0)); // rooms have the room at the +X axis end of the room, so we face -X to face into the room
				double anticheeseRadius = 16;
				Hitbox hitbox = Hitbox.approximateCylinderSegment(l.clone().add(0, -anticheeseRadius, 0), anticheeseRadius, anticheeseRadius, Math.PI / 2);
				List<LivingEntity> mobs = hitbox.getHitMobs();
				mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
				if (!mobs.isEmpty()) {
					mobs.forEach(mob -> GlowingManager.startGlowing(mob, NamedTextColor.WHITE, 100, 0));
					dp.sendMessage("There are enemies blocking you from opening the next room!");
					return;
				}
			}

			// Store the location to spawn the next room from
			party.mRoomSpawnerLocation = l.toVector();
			//Let the player select the room
			DepthsGUICommands.roomChoice(p);
		}
	}

	/**
	 * Increases treasure score for the player's party by the specified amount
	 *
	 * @param l     location to play sound
	 * @param p     player whose party to increase
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
	 * If the room is clear, and they haven't picked a reward yet,
	 * they will get the gui to select their reward
	 *
	 * @param p player who opened the chest
	 * @return the appropriate GUI to open for their current room reward
	 */
	public boolean getRoomReward(Player p, @Nullable Location l, boolean fromSummaryGUI) {
		DepthsPlayer dp = getDepthsPlayer(p);
		DepthsParty party = getPartyFromId(dp);
		if (dp == null || party == null) {
			return false;
		}

		//First - check if the player has any rewards to open
		if (!dp.mEarnedRewards.isEmpty()) {
			DepthsRewardType reward = dp.mEarnedRewards.peek();
			if (reward == DepthsRewardType.ABILITY || reward == DepthsRewardType.ABILITY_ELITE || reward == DepthsRewardType.PRISMATIC || reward == DepthsRewardType.CURSE) {
				DepthsGUICommands.ability(p, fromSummaryGUI);
				return true;
			} else if (reward == DepthsRewardType.UPGRADE || reward == DepthsRewardType.UPGRADE_ELITE || reward == DepthsRewardType.TWISTED) {
				DepthsGUICommands.upgrade(p, fromSummaryGUI);
				return true;
			} else if (reward == DepthsRewardType.GENEROSITY) {
				DepthsGUICommands.generosity(p, fromSummaryGUI);
				return true;
			}
		}

		if (party.mSpawnersToBreak > 0 || !party.mCanGetTreasureReward) {
			return false;
		}

		DepthsRoomType room = party.mCurrentRoomType;

		//Give treasure if their reward queue is empty, and it's a treasure room the party has cleared
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
	 *
	 * @param p    player to upgrade for
	 * @param slot the index of which item they selected in their offering array
	 */
	public void playerUpgradedItem(Player p, int slot, boolean sendMessage) {
		UUID uuid = p.getUniqueId();
		List<DepthsAbilityItem> itemChoices = mUpgradeOfferings.get(uuid);
		if (itemChoices == null) {
			return;
		}
		if (slot >= itemChoices.size()) {
			return;
		}
		DepthsAbilityItem choice = itemChoices.get(slot);
		setPlayerLevelInAbility(choice.mAbility, p, choice.mRarity, true, sendMessage);
		mUpgradeOfferings.remove(uuid);
		DepthsPlayer dp = getDepthsPlayer(uuid);
		if (dp != null) {
			dp.mEarnedRewards.poll();
			p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
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
		DepthsParty party = getPartyFromId(dp);
		if (dp == null || party == null) {
			return null;
		}
		// Return up to 3 random choices of items that are one level above the current level
		ArrayList<DepthsAbilityInfo<?>> abilities = new ArrayList<>(getAbilities());
		abilities.removeIf(info -> !info.getHasLevels());
		Collections.shuffle(abilities);
		offeredItems = new ArrayList<>();

		//Give an extra upgrade level if running depths 2
		int depths2UpgradeBonus = (dp.getContent() == DepthsContent.CELESTIAL_ZENITH) ? 1 : 0;

		// Loop through all possible abilities and show random ones they have at a higher rarity
		int options = 3;
		Prosperity prosperity = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(p, Prosperity.class);
		if (prosperity != null) {
			options += prosperity.getExtraChoices();
		}

		for (DepthsAbilityInfo<?> da : abilities) {
			if (offeredItems.size() >= options) {
				break;
			}
			int level = getPlayerLevelInAbility(da.getDisplayName(), p);
			if (level == 0 || (level >= 5 && !(dp.mEarnedRewards.peek() == DepthsRewardType.TWISTED)) || level >= 6 || WeaponAspectDepthsAbility.class.isAssignableFrom(da.getAbilityClass())) {
				continue;
			} else {
				int newRarity;
				//If they're in an elite room, their reward is +2 levels instead
				if (dp.mEarnedRewards.peek() == DepthsRewardType.UPGRADE_ELITE) {
					newRarity = Math.min(5, level + 2 + depths2UpgradeBonus);
				} else if (dp.mEarnedRewards.peek() == DepthsRewardType.TWISTED) {
					newRarity = 6;
				} else {
					newRarity = Math.min(5, level + 1 + depths2UpgradeBonus);
				}
				int oldRarity = getPlayerLevelInAbility(da.getDisplayName(), p);
				DepthsAbilityItem item = da.getAbilityItem(newRarity, oldRarity);

				offeredItems.add(item);
			}
		}

		mUpgradeOfferings.put(uuid, offeredItems);

		return offeredItems;
	}

	/**
	 * This method takes a player and, if they are in the system, removes one of their abilities at random
	 * and rolls them two random new abilities at the replaced ability's rarity
	 *
	 * @param p Player to roll for
	 */
	public void chaos(Player p) {

		DepthsPlayer dp = getDepthsPlayer(p);
		if (dp == null) {
			DepthsUtils.sendFormattedMessage(p, DepthsContent.DARKEST_DEPTHS, "Player not in depths system");
			return;
		}
		if (dp.mUsedChaosThisFloor) {
			dp.sendMessage("You have already used the chaos system on this floor!");
			return;
		}

		//Remove random ability
		List<String> abilityList = new ArrayList<>(dp.mAbilities.keySet());
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
			if (testLevel > 0 && !DepthsUtils.isWeaponAspectAbility(test) && !DepthsUtils.isPrismaticAbility(test) && !DepthsUtils.isCurseAbility(test)) {
				removedAbility = test;
				removedLevel = testLevel;
			}
			index++;
		}
		DepthsAbilityInfo<?> info = getAbility(removedAbility);
		boolean isMutated = info != null && !dp.mEligibleTrees.contains(info.getDepthsTree());

		setPlayerLevelInAbility(removedAbility, p, 0, true, true);
		dp.mUsedChaosThisFloor = true;

		//Give 2 random abilities that aren't the one we just removed
		int abilityCount = 2;
		if (mRandom.nextInt(3) == 0) {
			abilityCount = 3;
		}
		for (int i = 0; i < abilityCount; i++) {
			List<DepthsAbilityInfo<?>> abilities = getFilteredAbilities(dp.mEligibleTrees);
			if (isMutated) {
				List<DepthsTree> validTrees = new ArrayList<>();
				for (DepthsTree tree : DepthsTree.OWNABLE_TREES) {
					if (!dp.mEligibleTrees.contains(tree)) {
						validTrees.add(tree);
					}
				}
				abilities = getFilteredAbilities(validTrees);
			}

			Collections.shuffle(abilities);
			for (DepthsAbilityInfo<?> da : abilities) {
				if (da.canBeOffered(p) && !Objects.equals(da.getDisplayName(), removedAbility)) {
					int newLevel;
					if (removedLevel < 5 && mRandom.nextInt(3) == 0) {
						newLevel = removedLevel + 1;
					} else {
						newLevel = removedLevel;
					}
					setPlayerLevelInAbility(da.getDisplayName(), p, newLevel, true, true);
					break;
				}
			}
		}

		validateOfferings(dp, removedAbility);
	}

	public void bossDefeated(Location loc, int range) {
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Player nearestPlayer = EntityUtils.getNearestPlayer(loc, range);
			if (nearestPlayer != null) {
				DepthsManager.getInstance().goToNextFloor(nearestPlayer);
			}
		}, 20);
	}

	/**
	 * Sends the party to the next floor (boss death for each will call this)
	 *
	 * @param p player - get their party and send them to next floor
	 */
	public void goToNextFloor(Player p) {
		DepthsPlayer dp = getDepthsPlayer(p);
		DepthsParty party = getPartyFromId(dp);
		if (dp == null || party == null) {
			DepthsUtils.sendFormattedMessage(p, DepthsContent.DARKEST_DEPTHS, "Player not in depths system!");
			return;
		}
		party.mSpawnedForcedCleansingRoom = false; // reset this.
		if (dp.mGraveRunnable != null) {
			dp.mDead = false;
			dp.mGraveRunnable.cancel();
		}
		dp.mNumDeaths = Math.max(0, dp.mNumDeaths - 1);
		MMLog.finer(p.getName() + " went to next floor. mNumDeaths = " + dp.mNumDeaths);
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
						DepthsUtils.storeRunStatsToFile(playerInParty, Plugin.getInstance().getDataFolder() + File.separator + "DepthsStats", true); //Save the player's stats
						playerInParty.mFinalTreasureScore = party.mTreasureScore;
						playerInParty.sendMessage("Congratulations! Your final treasure score is " + playerInParty.mFinalTreasureScore + "!");
						party.populateLootRoom(player, true);
						if (party.getContent() == DepthsContent.DARKEST_DEPTHS) {
							int depthsWins = ScoreboardUtils.getScoreboardValue(player, "Depths").orElse(0);
							if (depthsWins == 0) {
								MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a[all_worlds=true] [\"\",{\"text\":\"" + player.getName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Darkest Depths for the first time!\",\"color\":\"white\",\"italic\":true,\"bold\":false}]");
							} else {
								Bukkit.getServer().sendMessage(Component.empty()
									.append(Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.ITALIC))
									.append(Component.text(" defeated the Darkest Depths!", NamedTextColor.YELLOW, TextDecoration.ITALIC)));
							}
							//Set score
							ScoreboardUtils.setScoreboardValue(player, "Depths", depthsWins + 1);
							Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + player.getName() + " Depths");
						} else if (party.getContent() == DepthsContent.CELESTIAL_ZENITH) {
							int depthsWins = ScoreboardUtils.getScoreboardValue(player, "Zenith").orElse(0);
							if (party.getAscension() == 0) {
								if (depthsWins == 0) {
									MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a[all_worlds=true] [\"\",{\"text\":\"" + player.getName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Celestial Zenith for the first time!\",\"color\":\"white\",\"italic\":true,\"bold\":false}]");
								} else {
									Bukkit.getServer().sendMessage(Component.empty()
										.append(Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.ITALIC))
										.append(Component.text(" defeated the Celestial Zenith!", NamedTextColor.YELLOW, TextDecoration.ITALIC)));
								}
							}
							//Set score
							ScoreboardUtils.setScoreboardValue(player, "Zenith", depthsWins + 1);
							Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + player.getName() + " Zenith");
						}

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
					ScoreboardUtils.setScoreboardValue(player, "Depths",
						ScoreboardUtils.getScoreboardValue(player, "Depths").orElse(0) + 1);
					Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
						"leaderboard update " + player.getName() + " Depths");
				}
			}
		} else if (party.mContent == DepthsContent.CELESTIAL_ZENITH) {
			if (party.getAscension() >= DepthsEndlessDifficulty.ASCENSION_CURSE_FLOOR) {
				party.mPlayersInParty.forEach(dp2 -> dp2.mEarnedRewards.add(DepthsRewardType.CURSE));
				party.sendMessage("You have been laden with an additional curse after clearing the floor!");
			}
			//Give twisted reward for beating boss in depths 2
			party.mPlayersInParty.forEach(dp2 -> dp2.mEarnedRewards.add(DepthsRewardType.TWISTED));
			party.sendMessage("You received a celestial gift for clearing the floor! Check your trinket to see the upgrade.");

			// Give delve points for ascension level
			if (partyFloor == 1 && party.getAscension() > 0) {
				int totalPoints = 0;
				for (int x : DepthsEndlessDifficulty.ASCENSION_DELVE_POINTS) {
					if (x <= party.getAscension()) {
						totalPoints += DepthsEndlessDifficulty.ASCENSION_DELVE_POINTS_AMOUNT;
					}
				}
				DepthsEndlessDifficulty.applyDelvePointsToParty(party, totalPoints / 2, party.mDelveModifiers, false);
			}
		}

		//Check to see if we're in endless mode and need to assign delve points to players
		if (party.mEndlessMode && partyFloor >= 3 && partyFloor <= 14) {
			int delvePoints = DepthsEndlessDifficulty.DELVE_POINTS_PER_FLOOR[partyFloor - 1];
			DepthsEndlessDifficulty.applyDelvePointsToParty(party, delvePoints, party.mDelveModifiers, false);
		} else if (partyFloor > 12 && partyFloor % 3 == 0) {
			party.sendMessage("You will now take +" + 10 * (partyFloor - 12) / 3 + "% damage from all sources!");
		}

		//Remove all mobs in the player's region
		List<Entity> mobs = p.getWorld().getEntities();
		for (Entity e : mobs) {
			if (EntityUtils.isHostileMob(e)) {
				e.remove();
			}
		}

		if (mRoomRepository == null) {
			mRoomRepository = DepthsUtils.getDepthsContent().getRoomRepository();
		}
		mRoomRepository.goToNextFloor(party, treasureScoreIncrease);
		party.mBeatBoss = true;
	}

	/**
	 * Transforms the mystery box aspect into a random other aspect after defeating floor 3
	 * The level 2 denotes that it is being transformed rather than selected (the aspects are not leveled)
	 *
	 * @param player the player to transform the ability of
	 */
	private void transformMysteryBox(Player player) {
		setPlayerLevelInAbility(RandomAspect.ABILITY_NAME, player, 0, false);
		List<DepthsAbilityInfo<? extends WeaponAspectDepthsAbility>> aspects = new ArrayList<>(getWeaponAspects());
		aspects.remove(RandomAspect.INFO);
		DepthsAbilityInfo<?> info = FastUtils.getRandomElement(aspects);
		setPlayerLevelInAbility(info.getDisplayName(), player, 2, true, true);
	}

	/**
	 * This method starts a bossfight for the given player's party depending on their current floor
	 * The bosses themselves will handle reading armor stands for cleaning up the arena
	 *
	 * @param p player to get party of
	 * @param l location to summon boss
	 */
	public void startBossFight(Player p, Location l) {
		//Check the player is in the system
		DepthsPlayer dp = getDepthsPlayer(p);
		DepthsParty depthsParty = getPartyFromId(dp);
		if (depthsParty == null) {
			DepthsUtils.sendFormattedMessage(p, DepthsContent.DARKEST_DEPTHS, "Player not in depths system!");
			return;
		}
		//Teleport all players in party to the player activating the fight
		for (DepthsPlayer dpInParty : depthsParty.mPlayersInParty) {
			if (dpInParty != dp) {
				Player playerToTp = Bukkit.getPlayer(dpInParty.mPlayerId);
				if (playerToTp == null || dpInParty.mDead) {
					dpInParty.offlineTeleport(p.getLocation());
				} else if (playerToTp.getLocation().distance(l) > 20) {
					playerToTp.teleport(p);
				}
			}
		}

		DepthsBoss boss = depthsParty.getContent().getBoss(depthsParty.getFloor());
		boss.summon(l);
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

	public void getRandomAbility(Player p, DepthsPlayer dp, int[] chances, @Nullable DepthsTree filterTree, boolean isTwisted) {
		getRandomAbility(p, dp, chances, filterTree, isTwisted, true);
	}

	/**
	 * Gives the player a random ability at low rarity, for run start or mystery box
	 *
	 * @param p       player
	 * @param dp      depths player
	 * @param chances the array of odds for each rarity, length 5
	 */
	public void getRandomAbility(Player p, DepthsPlayer dp, int[] chances, @Nullable DepthsTree filterTree, boolean isTwisted, boolean sendMessage) {
		//Give random ability

		List<DepthsAbilityInfo<?>> abilities;
		if (filterTree != null) {
			List<DepthsTree> filterList = new ArrayList<>();
			filterList.add(filterTree);
			abilities = getFilteredAbilities(filterList);
		} else {
			abilities = getFilteredAbilities(dp.mEligibleTrees);
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
			if (da.canBeOffered(p)) {

				int rarity = 1;
				if (isTwisted) {
					rarity = 6;
				} else if (da.getHasLevels()) {
					int roll = mRandom.nextInt(100) + 1;
					for (int i = 0; i < 5; i++) {
						if (roll < addUpChances(i, chances)) {
							rarity = i + 1;
							break;
						}
					}
				}

				setPlayerLevelInAbility(da.getDisplayName(), p, rarity, true, sendMessage);
				break;
			}
		}
	}

	public void getMutatedAbility(Player p, DepthsPlayer dp, DepthsAbilityInfo<?> currentAbility) {
		//Give random ability
		List<DepthsTree> validTrees = new ArrayList<>();
		for (DepthsTree tree : DepthsTree.OWNABLE_TREES) {
			if (!dp.mEligibleTrees.contains(tree)) {
				validTrees.add(tree);
			}
		}
		List<DepthsAbilityInfo<?>> abilities = getMutatedAbilities(validTrees, currentAbility.getDepthsTrigger());

		if (abilities.size() > 1) {
			abilities.remove(currentAbility);
		}
		if (abilities.isEmpty()) {
			return;
		}
		Collections.shuffle(abilities);
		DepthsAbilityInfo<?> newAbility = abilities.get(0);
		setPlayerLevelInAbility(Objects.requireNonNull(newAbility.getDisplayName()), p, 1, true, true);
	}

	/**
	 * Debug method to set party room number manually
	 *
	 * @param player player to get party from
	 * @param number room number to set
	 */
	public void setRoomDebug(Player player, int number) {
		DepthsParty depthsParty = getDepthsParty(player);
		if (depthsParty == null) {
			DepthsUtils.sendFormattedMessage(player, DepthsContent.DARKEST_DEPTHS, "Player not in depths system!");
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
		return getPartyFromId(dp);
	}

	public boolean hasTreeUnlocked(Player player, DepthsTree tree) {
		DepthsPlayer dp = getDepthsPlayer(player);
		if (dp == null) {
			return false;
		}
		return dp.mEligibleTrees.contains(tree);
	}

	/**
	 * Utility room wheel method to give random effect to player
	 * Randomness is handled in the mech, not plugin code
	 * Note that 1 will be replaced by gain a random prismatic ability
	 * 1 Gain 2 random abilities
	 * 2 Lose all abilities from a random tree
	 * 3 Gain two levels on every ability
	 * 4 Upgrade a random ability to twisted
	 * 5 Gain access to a random tree
	 * 6 Lose one level on every ability
	 *
	 * @param player Selected player who spun the wheel (maybe)
	 * @param arg    the wheel number landed on
	 */
	public void wheel(@Nullable Player player, int arg) {
		if (player == null) {
			return;
		}
		DepthsPlayer dp = getDepthsPlayer(player);
		if (dp == null) {
			DepthsUtils.sendFormattedMessage(player, DepthsContent.DARKEST_DEPTHS, "Player not in depths system!");
			return;
		}
		if (dp.mUsedWheelThisFloor) {
			dp.sendMessage("You have already spun the wheel on this floor!");
			return;
		}
		UUID uuid = player.getUniqueId();
		DepthsParty party = getPartyFromId(dp);
		String playerName = player.getName();
		if (party == null) {
			dp.sendMessage("Error finding depths party!");
			return;
		}

		switch (arg) {
			case 1:
				int[] chances = {70, 25, 4, 1, 0};
				getRandomAbility(player, dp, chances, DepthsTree.PRISMATIC, false);
				validateOfferings(dp);
				break;
			case 2:
				DepthsTree chosenTree = dp.mEligibleTrees.get(mRandom.nextInt(dp.mEligibleTrees.size()));
				boolean removed = false;
				for (DepthsAbilityInfo<?> da : getAbilities()) {
					String name = da.getDisplayName();
					DepthsTree tree = da.getDepthsTree();
					if (name == null || tree == null) {
						continue;
					}
					if (tree.equals(chosenTree) && getPlayerLevelInAbility(name, player) > 0) {
						setPlayerLevelInAbility(name, player, 0, true, true);
						validateOfferings(dp, name);
						removed = true;
					}
				}
				if (!removed) {
					dp.sendMessage(Component.text("You would have lost all of your ").append(chosenTree.getNameComponent().hoverEvent(chosenTree.createItem().asHoverEvent())).append(Component.text(" abilities, but you had none.")));
				}

				break;
			case 3:
				dp.mAbilities.forEach((ability, level) -> {
					if (level < 5 && level > 0) {
						DepthsAbilityInfo<?> info = getAbility(ability);
						if (info != null && info.getHasLevels()) {
							setPlayerLevelInAbility(ability, player, Math.min(5, level + 2), false);
						}
					}

				});
				dp.sendMessage("You upgraded all your abilities by two levels!");
				party.sendMessage(playerName + " has upgraded all their abilities by two levels!", o -> o != dp);
				validateOfferings(dp);
				break;
			case 4:
				if (party.getFloor() == 1) {
					dp.mRerolls += 2;
					dp.sendMessage("You gained 2 rerolls!");
					party.sendMessage(playerName + " gained 2 rerolls!", o -> o != dp);
				} else if (party.getFloor() == 2) {
					dp.mRerolls += 3;
					dp.sendMessage("You gained 3 rerolls!");
					party.sendMessage(playerName + " gained 3 rerolls!", o -> o != dp);
				} else {
					//Upgrade random ability to twisted
					List<String> abilityList = new ArrayList<>(dp.mAbilities.keySet());
					Collections.shuffle(abilityList);
					String abilityToUpgrade = null;
					int index = 0;
					while (abilityToUpgrade == null) {
						if (index >= abilityList.size()) {
							return;
						}
						String test = abilityList.get(index);
						//Make sure the player has the ability AND it's not a weapon aspect
						int testLevel = getPlayerLevelInAbility(test, player);
						if (testLevel > 0 && testLevel < 6) {
							DepthsAbilityInfo<?> info = getAbility(test);
							if (info != null && info.getHasLevels()) {
								abilityToUpgrade = test;
							}
						}
						index++;
					}

					setPlayerLevelInAbility(abilityToUpgrade, player, 6, true, true);
					validateOfferings(dp, abilityToUpgrade);
				}
				break;
			case 5:
				DepthsTree randomTree = null;

				if (dp.mEligibleTrees.size() == DepthsTree.OWNABLE_TREES.length) {
					dp.sendMessage("You already have all the trees unlocked. Spin again!");
					return;
				}
				while (randomTree == null) {
					DepthsTree test = DepthsTree.OWNABLE_TREES[mRandom.nextInt(DepthsTree.OWNABLE_TREES.length)];
					if (!dp.mEligibleTrees.contains(test)) {
						randomTree = test;
					}
				}
				dp.mEligibleTrees.add(randomTree);
				Component tree = randomTree.getNameComponent().hoverEvent(randomTree.createItem().asHoverEvent());
				dp.sendMessage(Component.text("You unlocked the ").append(tree).append(Component.text(" tree!")));
				party.sendMessage(Component.text(playerName + " unlocked the ").append(tree).append(Component.text(" tree!")), o -> !uuid.equals(o.mPlayerId));

				break;
			case 6:
				dp.mAbilities.forEach((ability, level) -> {
					if (level > 1 && level < 6) {
						DepthsAbilityInfo<?> info = getAbility(ability);
						if (info != null && info.getHasLevels()) {
							setPlayerLevelInAbility(ability, player, Math.max(1, level - 1), false);
						}
					}
				});
				dp.sendMessage("Unlucky! You downgraded all your abilities by a level.");
				party.sendMessage(playerName + " downgraded all their abilities by a level!", o -> o != dp);
				validateOfferings(dp);

				break;
			default:
				break;
		}

		dp.mUsedWheelThisFloor = true;
	}

	public @Nullable DepthsAbilityInfo<?> getAbility(@Nullable String abilityName) {
		if (abilityName == null) {
			return null;
		}
		for (DepthsAbilityInfo<?> info : getAbilities()) {
			if (Objects.equals(info.getDisplayName(), abilityName)) {
				return info;
			}
		}
		return null;
	}

	public void validateOfferings(DepthsPlayer dp) {
		validateOfferings(dp, null);
	}

	public void validateOfferings(DepthsPlayer dp, @Nullable String removedAbility) {
		List<DepthsAbilityItem> upgradeOfferings = mUpgradeOfferings.get(dp.mPlayerId);
		if (upgradeOfferings != null) {
			if (removedAbility != null) {
				upgradeOfferings.removeIf(dai -> dai.mAbility.equals(removedAbility));
			}
			ListIterator<DepthsAbilityItem> iter = upgradeOfferings.listIterator();
			while (iter.hasNext()) {
				DepthsAbilityItem dai = iter.next();
				int actualRarity = dp.mAbilities.getOrDefault(dai.mAbility, 0);
				if (dai.mRarity <= actualRarity) {
					iter.remove();
					continue;
				}
				if (dai.mPreviousRarity != actualRarity) {
					DepthsAbilityInfo<?> info = getAbility(dai.mAbility);
					if (info != null) {
						DepthsAbilityItem newItem = info.getAbilityItem(dai.mRarity, actualRarity);
						if (newItem != null) {
							iter.set(newItem);
						}
					}
				}
			}
			if (upgradeOfferings.isEmpty()) {
				mUpgradeOfferings.remove(dp.mPlayerId);
			}
		}

		List<DepthsAbilityItem> abilityOfferings = mAbilityOfferings.get(dp.mPlayerId);
		if (abilityOfferings != null) {
			abilityOfferings.removeIf(dai -> dp.mAbilities.keySet().stream().map(this::getAbility).filter(Objects::nonNull).anyMatch(info -> dai.mAbility.equals(info.getDisplayName()) || (dai.mTrigger != DepthsTrigger.PASSIVE && info.getDepthsTrigger() == dai.mTrigger)));
			DepthsParty party = getPartyFromId(dp);
			if (party != null && party.getAscension() >= DepthsEndlessDifficulty.ASCENSION_ACTIVE_TREE_CAP) {
				List<DepthsTree> trees = getActiveAbilityTrees(dp);
				abilityOfferings.removeIf(dai -> dai.mTrigger != DepthsTrigger.PASSIVE && trees.stream().filter(t -> t == dai.mTree).count() >= 4);
			}
			if (abilityOfferings.isEmpty()) {
				mAbilityOfferings.remove(dp.mPlayerId);
			}
		}
	}

	private List<DepthsTree> getActiveAbilityTrees(DepthsPlayer dp) {
		return dp.mAbilities.keySet().stream()
			.map(this::getAbility).filter(Objects::nonNull)
			.filter(info -> info.getDepthsTrigger().isActive())
			.map(DepthsAbilityInfo::getDepthsTree).filter(Objects::nonNull)
			.toList();
	}

	public boolean hasActiveAbility(DepthsPlayer dp) {
		return getPlayerAbilities(dp).stream().anyMatch(info -> info.getDepthsTrigger().isActive());
	}

	public @Nullable DepthsParty getParty(World world) {
		UUID uuid = world.getUID();
		for (DepthsParty party : mParties) {
			if (uuid.equals(party.mWorldUUID)) {
				return party;
			}
		}
		return null;
	}
}
