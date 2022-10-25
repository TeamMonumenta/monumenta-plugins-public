package com.playmonumenta.plugins.gallery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.interactables.BaseInteractable;
import com.playmonumenta.plugins.gallery.interactables.BasePricedInteractable;
import com.playmonumenta.plugins.gallery.interactables.EffectInteractable;
import com.playmonumenta.plugins.gallery.interactables.MysteryBoxInteractable;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;


/**
 * This is the main class for each game of gallery, each map has a UNIQUE key as the UUID of the world is playing on.
 */
public class GalleryGame {

	//statics constant that we may need to move inside GalleryGame if each map has his own constant
	private static final int MOBS_ON_MAP = 40;
	private static final float SPAWNER_STARTING_SPEED = 0.5f;
	private static final float SPAWNER_SPEED_PER_ROUND = 0.05f;
	private static final float SPAWNER_MAX_SPEED = 2.5f;
	private static final int SPECTERS_MOBS_COUNTDOWN = 20 * 60;
	private static final int SPECTERS_MOBS_COUNT = 10;
	private static final int ELITE_STARTING_ROUND = 11;

	//UUID of the world and also this game key for the GalleryManager
	protected final UUID mUUIDGame;

	//instance of this game
	private final GalleryGame GAME_INSTANCE;

	//ENUM to know which map is loaded
	private final GalleryMap mMap;

	//ENUM to know which map is loaded
	protected final Set<GalleryGrave> mGraves = new HashSet<>();

	//map of ALL the interactable object loaded in the game, each object has a unique name used has map key (updated on request by reloadAll)
	private final Map<String, BaseInteractable> mInteractableMap = new HashMap<>();

	//map of ALL the spawners object loaded in the game, each object has a unique name used has map key (updated on request by reloadAll)
	private final Map<String, Spawner> mSpawnerMap = new HashMap<>();

	//contains a reference of all the spawners that have been activated (updated when ever a spawner is activated)
	private final Set<Spawner> mActivatedSpawnerSet = new HashSet<>();

	//contains a reference of all the spawners that have been activated and are near the players (updated two time in a sec)
	private final List<Spawner> mSpawningSpawnersList = new ArrayList<>();

	//contains a reference of all the spawners that have been activated and are NOT near the players (updated two time in a sec)
	private final List<Spawner> mSleepySpawnersList = new ArrayList<>();

	//map of ALL the players that are playing this map, as map keys their UUID
	private final Map<UUID, GalleryPlayer> mPlayersMap = new HashMap<>();


	//variable that contains the coins the team has
	private int mPlayersCoins = 0;

	//location of the spawn
	protected @NotNull Vector mSpawningLoc;

	//location of the death box, where send player that are dead but the game is not yet closed
	protected @NotNull Vector mDeathBoxLoc;

	//contains ALL the mobs that the game need to spawn this round
	protected int mMobsToSpawnThisRound = 0;
	//contains the number of mobs spawned this round
	protected int mMobsSpawnedThisRound = 0;
	//contains the counter of mobs spawned this round
	protected int mMobsKilledThisRound = 0;

	//utils to block mobs spawning
	private boolean mCanMobsSpawn = true;

	//contains the chance of spawning an elite at the start of the round
	private double mEliteChance = 0.0d;

	//variable that indicate the current round
	private int mCurrentRound = 0;

	//variable used to count last time a mob is killed to spawn specters
	private int mLastMobKilledTick;

	//if the game is ended or not
	private boolean mIsGameEnded = false;

	//if the game should reload all the interactable and spawners
	private boolean mShouldReload = false;

	//used when the new round should start and
	private BukkitRunnable mRoundStartRunnable = null;

	/**
	 * @param gameUUID uuid of the world the game is playing in.
	 * @param map which current map is playing
	 * @param players list of the players
	 *
	 *                Main constructor of the game, used when run by command /gallery init @a[...] {GalleryMapName}
	 */
	public GalleryGame(UUID gameUUID, GalleryMap map, List<Player> players) {
		GAME_INSTANCE = this;
		mUUIDGame = gameUUID;
		mMap = map;
		mLastMobKilledTick = GalleryManager.ticks;

		for (Player player : players) {
			mPlayersMap.put(player.getUniqueId(), new GalleryPlayer(player.getUniqueId(), gameUUID));
		}

		Collection<ArmorStand> armorStands = players.get(0).getLocation().getNearbyEntitiesByType(ArmorStand.class, 500);

		List<ArmorStand> utilsArmorStands = armorStands.stream().filter(armorStand -> armorStand.getScoreboardTags().contains(GalleryManager.TAG_UTIL_LOCATION)).toList();
		for (LivingEntity livingEntity : utilsArmorStands) {
			if (livingEntity.getScoreboardTags().contains(GalleryManager.TAG_SPAWNING_LOC)) {
				mSpawningLoc = livingEntity.getLocation().toVector();
			}
			if (livingEntity.getScoreboardTags().contains(GalleryManager.TAG_DEAD_BOX_LOC)) {
				mDeathBoxLoc = livingEntity.getLocation().toVector();
			}
		}

		reloadAll();
	}

	//this constructor is only used for when creating a game from the Json file
	private GalleryGame(UUID gameUUID, GalleryMap map) {
		GAME_INSTANCE = this;
		mUUIDGame = gameUUID;
		mMap = map;
	}

	private boolean mShouldGameTick = true;
	private int mLastTimeTicked = 0;

	/**
	 *           This is the hearth of the game, performed each tick.
	 *           Handle the spawning of new mobs/elite/specters, the advancement of new rounds,
	 *           showing text and player ticking
	 *           if this function somehow fails, GalleryManager will unload the game and save the game state as a json
	 *           inside /monumenta/gallery/crashed/. with file name as mUUIDGame.toString() + ".json"
	 */
	protected void tick(boolean oneSecond, boolean twoHertz, int ticks) {
		//two time in a sec will check if some players are online and if the game should tick
		if (twoHertz) {
			mShouldGameTick = false;
			for (GalleryPlayer gPlayer : mPlayersMap.values()) {
				if (gPlayer.isOnline()) {
					mShouldGameTick = true;
					mLastTimeTicked = ticks;
					break;
				}
			}
		}

		//avoid useless ticking if the game is not running or ended
		if (!mShouldGameTick || mIsGameEnded) {
			if (ticks - mLastTimeTicked >= 20 * 3) {
				mInteractableMap.clear();
				mSpawnerMap.clear();
				mActivatedSpawnerSet.clear();
				mSpawningSpawnersList.clear();
				mSleepySpawnersList.clear();
				mShouldReload = true;
			}
			return;
		}

		//reloading all the interactable and spawners
		if (mShouldReload) {
			mLastMobKilledTick = ticks - 20 * 5;
			reloadAll();
		}

		//one time in a sec check to update showing names
		if (oneSecond) {
			boolean shouldShowMessage;
			for (BaseInteractable interactable : mInteractableMap.values()) {
				shouldShowMessage = false;
				for (GalleryPlayer gPlayer : mPlayersMap.values().stream().filter(player -> player.isOnline() && !player.isDead()).toList()) {
					if (interactable.shouldShowMessage(gPlayer)) {
						interactable.showMessage();
						shouldShowMessage = true;
						break;
					}
				}

				if (!shouldShowMessage) {
					interactable.removeMessage();
				}
			}
		}

		//2 time in a sec update all the spawner activated near the players
		if (twoHertz) {
			mSpawningSpawnersList.clear();
			mSleepySpawnersList.clear();
			mSleepySpawnersList.addAll(mActivatedSpawnerSet);
			for (GalleryPlayer gPlayer : mPlayersMap.values().stream().filter(player -> player.isOnline() && !player.isDead()).toList()) {
				Player player = gPlayer.getPlayer();
				if (player == null) {
					continue;
				}
				Iterator<Spawner> iterator = mSleepySpawnersList.iterator();
				while (iterator.hasNext()) {
					Spawner spawner = iterator.next();
					if (player.getLocation().distance(spawner.getLocation()) <= 30) {
						mSpawningSpawnersList.add(spawner);
						iterator.remove();
					}
				}
			}
		}

		//2 time inn a sec check if we can spawn new mobs up to map limit - Also check if we can spawn an elite
		if (twoHertz && mCanMobsSpawn && !mSpawningSpawnersList.isEmpty() && mRoundStartRunnable == null) {
			int mobsToSpawn = mMobsToSpawnThisRound - mMobsSpawnedThisRound;
			int remainingMobsOnMap = mMobsSpawnedThisRound - mMobsKilledThisRound;
			if (remainingMobsOnMap <= MOBS_ON_MAP && mobsToSpawn > 0) {
				//we can spawn some new mobs
				Collections.shuffle(mSpawningSpawnersList);
				int spawningMobs = Math.min(Math.min(mobsToSpawn, MOBS_ON_MAP), (MOBS_ON_MAP - remainingMobsOnMap));
				float spawnerSpeed = Math.min(SPAWNER_STARTING_SPEED + SPAWNER_SPEED_PER_ROUND * (mCurrentRound - 1), SPAWNER_MAX_SPEED);
				for (int i = 0; i < spawningMobs; i++) {
					LivingEntity mob = mSpawningSpawnersList.get(0).spawn(mMap.getLosPool(), spawnerSpeed, true);
					if (mob != null) {
						if (!GalleryUtils.ignoreScaling(mob)) {
							GalleryUtils.scaleMobPerPlayerCount(mob, mPlayersMap.size());
							GalleryUtils.scaleMobPerLevel(mob, mCurrentRound);
						}
						mMobsSpawnedThisRound++;
						Collections.shuffle(mSpawningSpawnersList);
					}
				}
			}

			if (mEliteChance > 0 && mEliteChance >= FastUtils.RANDOM.nextDouble()) {
				Collections.shuffle(mSpawningSpawnersList);
				float spawnerSpeed = Math.min(SPAWNER_STARTING_SPEED + SPAWNER_SPEED_PER_ROUND * (mCurrentRound - 1), SPAWNER_MAX_SPEED);
				LivingEntity elite1 = mSpawningSpawnersList.get(0).spawn(mMap.getElitePool(), spawnerSpeed, false);
				if (elite1 != null && !GalleryUtils.ignoreScaling(elite1)) {
					GalleryUtils.scaleMobPerPlayerCount(elite1, mPlayersMap.size());
					GalleryUtils.scaleMobPerLevel(elite1, mCurrentRound);
				}
				double extraEliteChange = mEliteChance - 1;
				while (extraEliteChange > 0) {
					if (extraEliteChange >= FastUtils.RANDOM.nextDouble()) {
						Collections.shuffle(mSpawningSpawnersList);
						LivingEntity mob = mSpawningSpawnersList.get(0).spawn(mMap.getElitePool(), spawnerSpeed, false);
						if (mob != null && !GalleryUtils.ignoreScaling(mob)) {
							GalleryUtils.scaleMobPerPlayerCount(mob, mPlayersMap.size());
							GalleryUtils.scaleMobPerLevel(mob, mCurrentRound);
						}
					}
					extraEliteChange--;
				}
			}
			mEliteChance = 0;
		}

		//if all the mob of this round are killed and if that then start a new round
		if (mMobsToSpawnThisRound == mMobsSpawnedThisRound && mMobsSpawnedThisRound == mMobsKilledThisRound && mRoundStartRunnable == null) {
			startNewRound();
		}

		//tick of all the players
		for (GalleryPlayer gPlayer : mPlayersMap.values()) {
			gPlayer.tick(oneSecond, twoHertz, ticks);
		}

		//2 time in a sec if mob can spawn and no mob has been killed in <SPECTERS_MOBS_COUNTDOWN>, start spawning specters
		if (mCanMobsSpawn && twoHertz && ticks - mLastMobKilledTick >= SPECTERS_MOBS_COUNTDOWN && !mSpawningSpawnersList.isEmpty()) {
			//no mobs have been killed in the last 60s -> spawn specters
			mLastMobKilledTick = ticks;

			float spawnerSpeed = Math.min(SPAWNER_STARTING_SPEED + SPAWNER_SPEED_PER_ROUND * (mCurrentRound - 1), SPAWNER_MAX_SPEED);
			for (int i = 0; i < SPECTERS_MOBS_COUNT; i++) {
				LivingEntity mob = mSpawningSpawnersList.get(0).spawn(mMap.getSpectersPool(), spawnerSpeed, false);
				if (mob != null) {
					if (!GalleryUtils.ignoreScaling(mob)) {
						GalleryUtils.scaleMobPerPlayerCount(mob, mPlayersMap.size());
						GalleryUtils.scaleMobPerLevel(mob, mCurrentRound);
					}
					Collections.shuffle(mSpawningSpawnersList);
				}
			}
			for (GalleryPlayer player : mPlayersMap.values()) {
				player.sendMessage("The nightmare has noticed you, an infestation emerges from the canvas.");
				player.playSound(Sound.ENTITY_ENDERMAN_SCREAM, 0.2f, 1.0f);
			}
		}

	}

	//Utils function to reload all the spawners and interactable inside the map
	protected void reloadAll() {
		World world = Bukkit.getWorld(mUUIDGame);
		Collection<ArmorStand> armorStands = new Location(world, mSpawningLoc.getX(), mSpawningLoc.getY(), mSpawningLoc.getZ()).getNearbyEntitiesByType(ArmorStand.class, 500);

		mInteractableMap.clear();
		mSpawnerMap.clear();
		mActivatedSpawnerSet.clear();

		List<ArmorStand> interactable = armorStands.stream().filter(armorStand -> armorStand.getScoreboardTags().contains(BaseInteractable.TAG_STRING) || armorStand.getScoreboardTags().contains(BasePricedInteractable.TAG_STRING) || armorStand.getScoreboardTags().contains(MysteryBoxInteractable.TAG_STRING) || armorStand.getScoreboardTags().contains(EffectInteractable.TAG_STRING)).toList();
		List<MysteryBoxInteractable> mysteryBox = new ArrayList<>();
		boolean mysteryBoxPlaced = false;
		for (LivingEntity entity : interactable) {
			try {
				BaseInteractable interact = BaseInteractable.fromEntity(entity);
				BaseInteractable otherInteractable = mInteractableMap.get(interact.getName());
				if (otherInteractable != null) {
					//we have more than one interactable with the same name. this is a problem since only one will be saved
					GalleryUtils.printDebugMessage("DUPLICATE INTERACTABLE NAME! " + interact.getName() + " Pos1. " + interact.getLocation().toVector() + " Pos2. " + otherInteractable.getLocation().toVector());
				}
				mInteractableMap.put(interact.getName(), interact);
				if (interact instanceof MysteryBoxInteractable mBox) {
					mysteryBox.add(mBox);
					if (!mysteryBoxPlaced && mBox.getValid()) {
						mysteryBoxPlaced = true;
					}
				}

			} catch (Exception e) {
				GalleryUtils.printDebugMessage("catch exception while converting interactable - reason: " + e.getMessage());
				e.printStackTrace();
			}
		}

		List<ArmorStand> spawnersEntities = armorStands.stream().filter(armorStand -> armorStand.getScoreboardTags().contains(Spawner.TAG_STRING)).toList();
		for (LivingEntity entity : spawnersEntities) {
			try {
				Spawner spawner = Spawner.fromEntity(entity);
				Spawner otherSpawner = mSpawnerMap.get(spawner.getName());
				if (otherSpawner != null) {
					//we have more than one interactable with the same name. this is a problem since only one will be saved
					GalleryUtils.printDebugMessage("DUPLICATE SPAWNER NAME! " + spawner.getName() + " Pos1. " + spawner.getLocation().toVector() + " Pos2. " + otherSpawner.getLocation().toVector());
				}
				mSpawnerMap.put(spawner.getName(), spawner);
				if (spawner.isActive()) {
					mActivatedSpawnerSet.add(spawner);
				}

			} catch (Exception e) {
				GalleryUtils.printDebugMessage("catch exception while converting spawner - reason: " + e.getMessage());
				e.printStackTrace();
			}
		}

		if (!mysteryBoxPlaced && !mysteryBox.isEmpty()) {
			Collections.shuffle(mysteryBox);
			mysteryBox.get(0).setValidBox(true);
			mysteryBox.get(0).runCommandPlace();
		}
		mShouldReload = false;
	}

	//start a new round and give gold to the player for the current one
	private void startNewRound() {
		mCurrentRound++;
		mMobsKilledThisRound = 0;
		mMobsSpawnedThisRound = 0;
		mMobsToSpawnThisRound = GalleryUtils.getMobsCountForRound(mCurrentRound, mPlayersMap.values().size());
		if (mCurrentRound >= ELITE_STARTING_ROUND) {
			mEliteChance = mCurrentRound == ELITE_STARTING_ROUND ? 1.0 : (0.02 * (mCurrentRound - ELITE_STARTING_ROUND));
		}
		mPlayersCoins += GalleryUtils.getGold(mCurrentRound, mPlayersMap.size());
		for (GalleryGrave grave : new HashSet<>(mGraves)) {
			grave.removeGrave();
		}

		for (GalleryPlayer player : mPlayersMap.values()) {
			if (player.isDead()) {
				player.setAlive(true);
				if (player.isOnline()) {
					//player.getPlayer() will always not null at this point
					player.getPlayer().teleport(new Location(player.getPlayer().getWorld(), mSpawningLoc.getX(), mSpawningLoc.getY(), mSpawningLoc.getZ()));
				} else {
					//player is not online, add new tag to teleport him when reenter the world
					player.setShouldTeleportWhenJoining(true);
				}
			}

			if (player.isOnline()) {
				player.onRoundStart(this);
			}
			player.sendMessage("Starting round: " + mCurrentRound);
		}

		mRoundStartRunnable = new BukkitRunnable() {
			int mTimer = 0;
			@Override public void run() {
				mTimer++;
				if (mTimer >= 20 * 3) {
					cancel();
				}
			}

			@Override public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				mRoundStartRunnable = null;
			}
		};
		mRoundStartRunnable.runTaskTimer(GalleryManager.mPlugin, 0, 1);
	}


	//----------------------functions called by commands or utils-------------------------------------------------

	public void setInteractable(String interactableName, Boolean isInteractable) {
		BaseInteractable interactable = mInteractableMap.get(interactableName);
		if (interactable != null) {
			interactable.setInteractable(isInteractable);
		}
	}

	public void setInteractableText(String interactableName, String text) {
		BaseInteractable interactable = mInteractableMap.get(interactableName);
		if (interactable != null) {
			interactable.setShowingText(text);
		}
	}

	public void setActiveSpawner(String name, boolean active) {
		if (!mSpawnerMap.containsKey(name)) {
			return;
		}
		Spawner spawner = mSpawnerMap.get(name);
		spawner.setActive(active);
		if (active) {
			mActivatedSpawnerSet.add(spawner);
		} else {
			mActivatedSpawnerSet.remove(spawner);
		}
	}

	public void despawnMob(LivingEntity mob) {
		if (mob.getScoreboardTags().contains(GalleryManager.MOB_TAG_FROM_SPAWNER)) {
			mMobsSpawnedThisRound--;
		}

		if (GalleryUtils.isGalleryElite(mob)) {
			mEliteChance += 1;
		}

		mob.remove();
	}

	public void scaleMob(LivingEntity livingEntity) {
		GalleryUtils.scaleMobPerLevel(livingEntity, mCurrentRound);
		GalleryUtils.scaleMobPerPlayerCount(livingEntity, mPlayersMap.size());
	}


	public void removeInteractable(String name) {
		BaseInteractable interactable = mInteractableMap.get(name);
		if (interactable != null) {
			interactable.remove();
			mInteractableMap.remove(name);
		}
	}

	public void load(Entity entity) {
		if (entity.getScoreboardTags().contains(BaseInteractable.TAG_STRING) ||
			    entity.getScoreboardTags().contains(BasePricedInteractable.TAG_STRING) ||
			    entity.getScoreboardTags().contains(MysteryBoxInteractable.TAG_STRING) ||
			    entity.getScoreboardTags().contains(EffectInteractable.TAG_STRING)) {
			try {
				BaseInteractable interact = BaseInteractable.fromEntity(entity);
				mInteractableMap.put(interact.getName(), interact);

			} catch (Exception e) {
				GalleryUtils.printDebugMessage("catch exception while converting interactable - reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
		if (entity.getScoreboardTags().contains(Spawner.TAG_STRING)) {
			try {
				Spawner spawner = Spawner.fromEntity(entity);
				mSpawnerMap.put(spawner.getName(), spawner);
				if (spawner.isActive()) {
					mActivatedSpawnerSet.add(spawner);
				}

			} catch (Exception e) {
				GalleryUtils.printDebugMessage("catch exception while converting spawner - reason: " + e.getMessage());
				e.printStackTrace();
			}
		}

	}

	public void setCanMobsSpawn(boolean bool) {
		mCanMobsSpawn = bool;
		mLastMobKilledTick = GalleryManager.ticks;
	}

	public void setRound(int round) {
		mCurrentRound = round - 1;
		startNewRound();
	}

	public int getCurrentRound() {
		return mCurrentRound;
	}

	public int getPlayersCoins() {
		return mPlayersCoins;
	}

	public void setPlayersCoins(int newCoinsValue) {
		mPlayersCoins = newCoinsValue;
	}

	public void givePlayersCoins(int coins) {
		mPlayersCoins += coins;
	}

	protected GalleryPlayer getGalleryPlayer(UUID player) {
		return mPlayersMap.get(player);
	}

	public void playerLeave(Player target) {
		mPlayersMap.remove(target.getUniqueId());
		if (mPlayersMap.isEmpty()) {
			GalleryManager.removeGame(GAME_INSTANCE);
		}
	}

	public void setPlayerAlive(Player player, boolean alive) {
		GalleryPlayer gPlayer = mPlayersMap.get(player.getUniqueId());
		if (gPlayer != null) {
			gPlayer.setAlive(alive);
			if (!alive) {
				ensureGameClosure();
			}
		}
	}

	public void printPlayerInfo(Player target) {
		GalleryPlayer galleryPlayer = mPlayersMap.get(target.getUniqueId());
		if (galleryPlayer != null && galleryPlayer.isOnline()) {
			galleryPlayer.printPlayerInfo();
		}
	}

	public void sendMessageToPlayers(String msg) {
		for (GalleryPlayer player : mPlayersMap.values()) {
			player.sendMessage(msg);
		}
	}

	public void moveBoxAtRandomLocation() {
		List<MysteryBoxInteractable> boxes = new ArrayList<>();
		mInteractableMap.values().stream().filter(baseInteractable -> baseInteractable instanceof MysteryBoxInteractable).forEach(baseInteractable -> boxes.add((MysteryBoxInteractable) baseInteractable));
		for (MysteryBoxInteractable box : boxes) {
			if (box.getValid()) {
				box.setValidBox(false);
				box.runCommandRemove();
				break;
			}
		}

		Collections.shuffle(boxes);
		if (!boxes.isEmpty()) {
			boxes.get(0).runCommandPlace();
			boxes.get(0).setValidBox(true);
		}
	}

	public List<Location> getBoxLocations() {
		List<Location> locations = new ArrayList<>();
		for (BaseInteractable interactable : mInteractableMap.values()) {
			if (interactable instanceof MysteryBoxInteractable mBox) {
				locations.add(mBox.getLocation());
			}
		}
		return locations;
	}

	public Location getBoxLocation() {
		for (BaseInteractable interactable : mInteractableMap.values()) {
			if (interactable instanceof MysteryBoxInteractable box) {
				if (box.getValid()) {
					return box.getLocation();
				}
			}
		}
		return null;
	}

	public Location getSpawnLocation() {
		for (GalleryPlayer player : mPlayersMap.values()) {
			if (player.isOnline()) {
				return new Location(player.getPlayer().getWorld(), mSpawningLoc.getX(), mSpawningLoc.getY(), mSpawningLoc.getZ());
			}
		}
		return null;
	}

	public BaseInteractable getInteractable(String name) {
		return mInteractableMap.get(name);
	}

	//this function is used to make sure that the game should or not close and send players to the loot rooms
	private void ensureGameClosure() {
		boolean shouldCloseTheGame = true;

		for (GalleryPlayer player : mPlayersMap.values()) {
			if (!player.isDead()) {
				shouldCloseTheGame = false;
				break;
			}
		}

		if (shouldCloseTheGame) {
			mIsGameEnded = true;
			for (GalleryPlayer player : mPlayersMap.values()) {
				if (player.isOnline()) {
					player.sendMessage(ChatColor.DARK_RED + "The Nightmare has taken you, a fresh canvas begging to be painted.");
					GalleryUtils.runCommandAsEntity(player.getPlayer(), "function monumenta:dungeons/gallery/enter_lootroom");
				}
			}
		}
	}

	//---------------function to save the game and its current state-----------
	public void saveObjects() {
		for (BaseInteractable interactable : mInteractableMap.values()) {
			interactable.save();
		}

		for (Spawner spawner : mSpawnerMap.values()) {
			spawner.save();
		}
	}

	public JsonObject toJson() {
		JsonObject object = new JsonObject();

		object.addProperty("GalleryMapName", mMap.name());
		object.addProperty("GalleryMapID", mMap.getIndex());
		object.addProperty("UUID", mUUIDGame.toString());
		object.addProperty("round", mCurrentRound);
		object.addProperty("MobsToSpawnThisRound", mMobsToSpawnThisRound);
		object.addProperty("MobsSpawnedThisRound", mMobsSpawnedThisRound);
		object.addProperty("MobsKilledThisRound", mMobsKilledThisRound);
		object.addProperty("Ended", mIsGameEnded);
		object.addProperty("coins", mPlayersCoins);
		object.addProperty("SpawningLocX", mSpawningLoc.getBlockX());
		object.addProperty("SpawningLocY", mSpawningLoc.getBlockY());
		object.addProperty("SpawningLocZ", mSpawningLoc.getBlockZ());
		object.addProperty("DeathBoxLocX", mDeathBoxLoc.getBlockX());
		object.addProperty("DeathBoxLocY", mDeathBoxLoc.getBlockY());
		object.addProperty("DeathBoxLocZ", mDeathBoxLoc.getBlockZ());
		JsonArray arr = new JsonArray();
		for (GalleryPlayer player : mPlayersMap.values()) {
			arr.add(player.toJson());
		}
		object.add("players", arr);

		return object;
	}

	public static GalleryGame fromJson(JsonObject object) throws Exception {
		GalleryMap map = GalleryMap.fromID(object.getAsJsonPrimitive("GalleryMapID").getAsInt());
		if (map == null) {
			throw new Exception("MAP ID == null | this is a serious bug! ID: " + object.getAsJsonPrimitive("GalleryMapID").getAsInt());
		}

		UUID uuid = UUID.fromString(object.getAsJsonPrimitive("UUID").getAsString());

		Vector spawningLoc = new Vector(object.getAsJsonPrimitive("SpawningLocX").getAsInt(), object.getAsJsonPrimitive("SpawningLocY").getAsInt(), object.getAsJsonPrimitive("SpawningLocZ").getAsInt());
		Vector deathBoxLoc = new Vector(object.getAsJsonPrimitive("DeathBoxLocX").getAsInt(), object.getAsJsonPrimitive("DeathBoxLocY").getAsInt(), object.getAsJsonPrimitive("DeathBoxLocZ").getAsInt());

		GalleryGame game = new GalleryGame(uuid, map);

		for (JsonElement element : object.getAsJsonArray("players")) {
			GalleryPlayer gPlayer = GalleryPlayer.fromJson(element.getAsJsonObject(), uuid);
			game.mPlayersMap.put(gPlayer.getPlayerUUID(), gPlayer);
		}
		game.mPlayersCoins = object.getAsJsonPrimitive("coins").getAsInt();
		game.mSpawningLoc = spawningLoc;
		game.mDeathBoxLoc = deathBoxLoc;
		game.mIsGameEnded = object.getAsJsonPrimitive("Ended").getAsBoolean();
		game.mShouldReload = true;
		game.mCurrentRound = object.getAsJsonPrimitive("round").getAsInt();
		game.mMobsToSpawnThisRound = object.getAsJsonPrimitive("MobsToSpawnThisRound").getAsInt();
		game.mMobsSpawnedThisRound = object.getAsJsonPrimitive("MobsSpawnedThisRound").getAsInt();
		game.mMobsKilledThisRound = object.getAsJsonPrimitive("MobsKilledThisRound").getAsInt();

		return game;
	}

	//---------------functions to handle events-------------------------------

	public void mobDeathEvent(EntityDeathEvent event, LivingEntity entity, int ticks) {
		if (entity.getScoreboardTags().contains(GalleryManager.MOB_TAG_FROM_SPAWNER)) {
			mMobsKilledThisRound++;
		}

		mLastMobKilledTick = ticks;
	}

	public void onMobExplodeEvent(EntityExplodeEvent event, Entity entity, int ticks) {
		if (entity.getScoreboardTags().contains(GalleryManager.MOB_TAG_FROM_SPAWNER)) {
			mMobsKilledThisRound++;
		}
		mLastMobKilledTick = ticks;
	}

	public void playerInteractWithObject(Player player) {
		GalleryPlayer gPlayer = mPlayersMap.get(player.getUniqueId());
		if (gPlayer != null) {
			BaseInteractable lastInteract = null;
			for (BaseInteractable interactable : mInteractableMap.values()) {
				if (interactable.getLocation().distance(player.getLocation()) < 20) {
					if (interactable.canInteractWithObject(GAME_INSTANCE, gPlayer)) {
						if (interactable.interactWithObject(GAME_INSTANCE, gPlayer)) {
							lastInteract = interactable;
							break;
						}
					}
				}
			}

			if (lastInteract instanceof MysteryBoxInteractable mysteryBoxInteractable) {
				//run odds and loop throw if we need to make a new one
				if (FastUtils.RANDOM.nextDouble() <= 0.1) {
					//delayed so the animation ends
					mysteryBoxInteractable.setValidBox(false);
					Bukkit.getScheduler().runTaskLater(GalleryManager.mPlugin, () -> {
						List<BaseInteractable> list = new ArrayList<>(mInteractableMap.values().stream().filter(bInteract -> bInteract instanceof MysteryBoxInteractable mmBox && mmBox != mysteryBoxInteractable).toList());
						if (!list.isEmpty()) {
							mysteryBoxInteractable.runCommandRemove();
							MysteryBoxInteractable mmBoxInteractable = (MysteryBoxInteractable) list.get(0);
							mmBoxInteractable.runCommandPlace();
							mmBoxInteractable.setValidBox(true);
						}
					}, 20 * 6);
				}
			}
		}
	}

	public void playerDeathEvent(PlayerDeathEvent event, LivingEntity player, int ticks) {
		GalleryPlayer realPlayer = mPlayersMap.get(player.getUniqueId());
		if (realPlayer == null) {
			GalleryUtils.printDebugMessage("Player == null while playerDeathEvent for game " + mUUIDGame);
			return;
		}

		for (GalleryPlayer gPlayer : mPlayersMap.values()) {
			Player gpp = gPlayer.getPlayer();
			if (gpp != null && gpp.isOnline()) {
				if (!gpp.equals(player)) {
					gPlayer.otherPlayerDeathEvent(event, player, ticks);
				}
			}
		}

		if (!event.isCancelled()) {
			realPlayer.playerDeathEvent(event, player, ticks);
		}

		if (!event.isCancelled()) {
			event.setCancelled(true);
			realPlayer.setAlive(false);
			realPlayer.clearEffects();
			player.setHealth(EntityUtils.getMaxHealth(player)); //player has to be with health > 0 when teleported

			//make sure the game should not close
			ensureGameClosure();

			if (mIsGameEnded) {
				return;
			}

			Location deadLoc = player.getLocation();

			player.teleport(new Location(player.getWorld(), mDeathBoxLoc.getX(), mDeathBoxLoc.getY(), mDeathBoxLoc.getZ()));
			Title.Times times = Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1));
			Component mainTitle = Component.text("⎧", NamedTextColor.BLACK).append(Component.text("YOU DIED", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true)).append(Component.text("⎫", NamedTextColor.BLACK).decoration(TextDecoration.BOLD, false));
			Component subTitle = Component.text("⎩", NamedTextColor.BLACK).decoration(TextDecoration.BOLD, true).append(Component.text("ᴿᴵᴾ", NamedTextColor.BLACK)).append(Component.text("⎭", NamedTextColor.BLACK));
			Title title = Title.title(mainTitle, subTitle, times);
			player.showTitle(title);

			for (GalleryPlayer gPlayer : mPlayersMap.values()) {
				if (gPlayer != realPlayer) {
					gPlayer.sendMessage(player.getName() + " has fallen, rush to their grave");
					gPlayer.sendMessage("hold crouch to revive them");
				}
			}
			GalleryGrave.createGrave(realPlayer, deadLoc, this);
		}
	}

	public void onPlayerHurtEvent(DamageEvent event, Player player, Entity damager, LivingEntity source) {
		if (mCurrentRound > GalleryUtils.STARTING_ROUND_FOR_SCALING && (source == null || !GalleryUtils.ignoreScaling(source))) {
			int dif = mCurrentRound - GalleryUtils.STARTING_ROUND_FOR_SCALING;
			double multiply = Math.min(1 + dif * 0.1, 5.0);
			event.setDamage(event.getDamage() * multiply);
		}

		GalleryPlayer gPlayer = mPlayersMap.get(player.getUniqueId());
		if (gPlayer != null) {
			gPlayer.onPlayerHurtEvent(event, source);
		}

		if (player.getHealth() + player.getAbsorptionAmount() <= event.getFinalDamage(true) && gPlayer != null) {
			gPlayer.onPlayerFatalHurtEvent(event, source);
		}
	}

	public void onPlayerDamageEvent(DamageEvent event, Player player, LivingEntity damagee) {
		GalleryPlayer gPlayer = mPlayersMap.get(player.getUniqueId());
		if (gPlayer != null) {
			gPlayer.onPlayerDamageEvent(event, damagee);
		}
	}

	public void playerJoinEvent(Player player) {
		GalleryPlayer gPlayer = mPlayersMap.get(player.getUniqueId());
		if (gPlayer != null) {
			if (mIsGameEnded) {
				gPlayer.sendMessage(ChatColor.DARK_RED + "The Nightmare has taken you, a fresh canvas begging to be painted.");
				GalleryUtils.runCommandAsEntity(player, "function monumenta:dungeons/gallery/enter_lootroom");
			}
			gPlayer.onPlayerJoinEvent();
		}
	}

	public void playerQuitEvent(Player player) {
		GalleryPlayer gPlayer = mPlayersMap.get(player.getUniqueId());
		boolean otherPlayersOnline = false;

		for (GalleryPlayer player1 : mPlayersMap.values()) {
			if (player1.isOnline() && !player1.isDead() && player1 != gPlayer) {
				otherPlayersOnline = true;
			}
		}

		if (!mGraves.isEmpty() && !otherPlayersOnline) {
			for (GalleryGrave grave : new HashSet<>(mGraves)) {
				grave.removeGrave();
				mGraves.remove(grave);
			}
		}
	}

}
