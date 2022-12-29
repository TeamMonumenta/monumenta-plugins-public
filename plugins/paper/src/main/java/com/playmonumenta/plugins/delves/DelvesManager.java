package com.playmonumenta.plugins.delves;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.delves.abilities.Astral;
import com.playmonumenta.plugins.delves.abilities.Chivalrous;
import com.playmonumenta.plugins.delves.abilities.Chronology;
import com.playmonumenta.plugins.delves.abilities.Colossal;
import com.playmonumenta.plugins.delves.abilities.Fragile;
import com.playmonumenta.plugins.delves.abilities.Haunted;
import com.playmonumenta.plugins.delves.abilities.Infernal;
import com.playmonumenta.plugins.delves.abilities.Riftborn;
import com.playmonumenta.plugins.delves.abilities.StatMultiplier;
import com.playmonumenta.plugins.delves.abilities.Twisted;
import com.playmonumenta.plugins.delves.mobabilities.TwistedMiniBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;


public class DelvesManager implements Listener {
	public static final String KEY_DELVES_PLUGIN_DATA = "MonumentaDelves";
	protected static final double DELVES_MAX_PARTY_DISTANCE = 32.0;


	/**
	 * This structure contains All the delves mods picked by online players (in this shard)
	 *
	 * it's structured in this way:
	 * <PlayerID, <DungeonID, <ModifierID, points>>>
	 */
	public static final Map<UUID, Map<String, DungeonDelveInfo>> PLAYER_DELVE_DUNGEON_MOD_MAP = new HashMap<>();
	public static final String PHANTOM_NAME = "LoomingConsequence";
	static final String SPAWNER_BREAKS_SCORE_NAME = "SpawnerBreaks";
	static final String SPAWNER_TOTAL_SCORE_NAME = "SpawnersTotal";
	static final int CHUNK_SCAN_RADIUS = 32;
	public static final Set<String> DUNGEONS = new HashSet<>();

	//List of all the shard where we can use delves
	static {
		DUNGEONS.add("white");
		DUNGEONS.add("orange");
		DUNGEONS.add("magenta");
		DUNGEONS.add("lightblue");
		DUNGEONS.add("yellow");
		DUNGEONS.add("willows");
		DUNGEONS.add("reverie");
		DUNGEONS.add("lime");
		DUNGEONS.add("pink");
		DUNGEONS.add("gray");
		DUNGEONS.add("lightgray");
		DUNGEONS.add("cyan");
		DUNGEONS.add("purple");
		DUNGEONS.add("teal");
		DUNGEONS.add("forum");
		DUNGEONS.add("shiftingcity");
		DUNGEONS.add("dev1");
		DUNGEONS.add("dev2");
		DUNGEONS.add("dev3");
		DUNGEONS.add("mobs");
		DUNGEONS.add("depths");
		DUNGEONS.add("corridors");
		DUNGEONS.add("ring");
		DUNGEONS.add("ruin");
		DUNGEONS.add("portal");
		DUNGEONS.add("blue");
		DUNGEONS.add("brown");
		DUNGEONS.add("futurama");
	}

	private static final String HAS_DELVE_MODIFIER_TAG = "DelveModifiersApplied";
	public static final String AVOID_MODIFIERS = "boss_delveimmune";

	public static int getRank(Player player, DelvesModifier modifier) {
		if (player == null || modifier == null) {
			return 0;
		}

		if (PLAYER_DELVE_DUNGEON_MOD_MAP.containsKey(player.getUniqueId())) {
			Map<String, DungeonDelveInfo> playerDDinfo = PLAYER_DELVE_DUNGEON_MOD_MAP.getOrDefault(player.getUniqueId(), new HashMap<>());
			if (playerDDinfo.containsKey(ServerProperties.getShardName())) {
				DungeonDelveInfo info = playerDDinfo.get(ServerProperties.getShardName());

				return info.get(modifier);
			}
		}
		return 0;
	}

	//return all players running this dungeon or in MAX_PARTY_DISTANCE radius
	protected static List<Player> getParty(Location loc) {
		List<Player> players = DelvesUtils.playerInRangeForDelves(loc);

		if (players.isEmpty()) {
			//somehow a mobs delve mobs spawned with a distance longer then MAX_PARTY_DISTANCE from the party
			//if this happens in R3 we need to increase MAX_PARTY_DISTANCE - make a log
			List<Player> playerInWorld = new ArrayList<>(loc.getWorld().getPlayers());
			if (playerInWorld.isEmpty()) {
				MMLog.warning("[DelveManager] No players in world on spawn of a (potential) delve mob");
				return playerInWorld;
			}
			playerInWorld.sort((player1, player2) -> (int) (loc.distance(player1.getLocation()) - loc.distance(player2.getLocation())));
			Player player = playerInWorld.get(0);
			MMLog.warning("[DelveManager] Party Empty when spawned a delveMob - Real Distance: " + loc.distance(player.getLocation()));
			return List.of(player);
		}

		return players;

	}

	private static void loadPlayerData(Player player) {
		JsonObject obj;
		try {
			obj = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_DELVES_PLUGIN_DATA);
		} catch (Exception e) {
			MMLog.warning("[DelveManager] error while loading player info. Reason: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		if (obj == null) {
			return;
		}

		JsonArray dungeonArr = obj.getAsJsonArray("dungeons");
		if (dungeonArr == null || dungeonArr.size() == 0) {
			return;
		}

		PLAYER_DELVE_DUNGEON_MOD_MAP.putIfAbsent(player.getUniqueId(), new HashMap<>());

		for (int i = 0; i < dungeonArr.size(); i++) {
			JsonObject dungeonObj = dungeonArr.get(i).getAsJsonObject();
			try {
				String name = dungeonObj.get("dungeonName").getAsString();

				DungeonDelveInfo info = new DungeonDelveInfo();

				JsonArray delveModArr = dungeonObj.getAsJsonArray("delveMods");
				if (delveModArr == null) {
					continue;
				}
				for (int delveIterator = 0; delveIterator < delveModArr.size(); delveIterator++) {
					JsonObject delveObj = delveModArr.get(delveIterator).getAsJsonObject();
					DelvesModifier delveMod = DelvesModifier.fromName(delveObj.getAsJsonPrimitive("delveModName").getAsString());
					if (delveMod == null) {
						continue;
					}

					int lvl = delveObj.getAsJsonPrimitive("delveModLvl").getAsInt();

					if (lvl > 0) {
						info.put(delveMod, lvl);
					}


				}

				PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId()).putIfAbsent(name, info);
				DelvesUtils.updateDelveScoreBoard(player);
			} catch (Exception e) {
				MMLog.warning("[DelveManager] error while loading player info. Reason: " + e.getMessage());
				e.printStackTrace();
				player.sendMessage(Component.text("Some of your delve data have not loaded correctly, try reloading!", NamedTextColor.RED).hoverEvent(HoverEvent.showText(Component.text(e.toString()))));
			}
		}
	}

	public static void savePlayerData(Player player, String dungeon, Map<DelvesModifier, Integer> mods) {
		PLAYER_DELVE_DUNGEON_MOD_MAP.putIfAbsent(player.getUniqueId(), new HashMap<>());

		Map<String, DungeonDelveInfo> playerDungeonInfo = PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId());

		DungeonDelveInfo ddinfo = playerDungeonInfo.getOrDefault(dungeon, new DungeonDelveInfo());
		ddinfo.mModifierPoint.clear();
		for (DelvesModifier mod : mods.keySet()) {
			ddinfo.mModifierPoint.put(mod, mods.get(mod));
		}
		ddinfo.recalculateTotalPoint();
		playerDungeonInfo.put(dungeon, ddinfo);
		PLAYER_DELVE_DUNGEON_MOD_MAP.put(player.getUniqueId(), playerDungeonInfo);
	}

	public static boolean validateDelvePreset(Player player, String dungeon) {
		Map<String, DungeonDelveInfo> playerDungeonInfo = PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId());

		DungeonDelveInfo ddinfo = playerDungeonInfo.getOrDefault(dungeon, new DungeonDelveInfo());

		int preset = ScoreboardUtils.getScoreboardValue(player, DelvePreset.PRESET_SCOREBOARD).orElse(0);
		if (preset == 0) {
			return true;
		}
		if (DelvePreset.validatePresetModifiers(ddinfo, DelvePreset.getDelvePreset(preset))) {
			return true;
		}
		return false;
	}

	protected static JsonObject convertPlayerData(Player player) {
		if (PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId()) == null) {
			return null;
		}

		JsonObject obj = new JsonObject();
		JsonArray dungeons = new JsonArray();
		obj.add("dungeons", dungeons);

		for (Map.Entry<String, DungeonDelveInfo> playerDungeonInfo : PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId()).entrySet()) {
			JsonObject dungeonObj = new JsonObject();
			dungeonObj.addProperty("dungeonName", playerDungeonInfo.getKey());

			JsonArray delveModArr = new JsonArray();
			dungeonObj.add("delveMods", delveModArr);
			for (Map.Entry<DelvesModifier, Integer> modLevelEntry : playerDungeonInfo.getValue().mModifierPoint.entrySet()) {
				JsonObject dungeonMod = new JsonObject();
				dungeonMod.addProperty("delveModName", modLevelEntry.getKey().name());
				dungeonMod.addProperty("delveModLvl", modLevelEntry.getValue());
				delveModArr.add(dungeonMod);
			}
			dungeons.add(dungeonObj);

		}

		return obj;
	}

	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {
		JsonObject data = convertPlayerData(event.getPlayer());
		if (data != null) {
			event.setPluginData(KEY_DELVES_PLUGIN_DATA, data);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entitySpawnEvent(EntitySpawnEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())) {
			return;
		}
		Entity entity = event.getEntity();

		/*
		 * Since this intercepts the CreatureSpawnEvent and SpawnerSpawnEvent,
		 * it seems that some modifiers get applied twice, which is annoying.
		 *
		 * Making sure that this only runs once per entity just future proofs
		 * it in case more events get added along the line that extend the
		 * EntitySpawnEvent.
		 */
		Set<String> tags = entity.getScoreboardTags();
		if (tags.contains(HAS_DELVE_MODIFIER_TAG) || tags.contains(AVOID_MODIFIERS)) {
			return;
		}

		if (entity instanceof LivingEntity livingEntity) {
			PotionEffect resistance = livingEntity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			if (resistance != null && (resistance.getAmplifier() > 4 || resistance.getDuration() > 20 * 7)) {
				return;
			}
		}

		if (EntityUtils.isHostileMob(entity) && entity instanceof LivingEntity livingEntity) {
			List<Player> playerParty = getParty(entity.getLocation());

			if (!playerParty.isEmpty()) {
				Map<DelvesModifier, Integer> delvesApplied = DelvesUtils.getPartyDelvePointsMap(playerParty);
				//check if this mob is summoned by command or by spawners
				if (event instanceof SpawnerSpawnEvent) {
					//normal spawn - handle all the mods
					Riftborn.applyModifiers(((SpawnerSpawnEvent) event).getSpawner().getBlock(), delvesApplied.getOrDefault(DelvesModifier.RIFTBORN, 0));
					Chronology.applyModifiers(((SpawnerSpawnEvent) event).getSpawner(), delvesApplied.getOrDefault(DelvesModifier.CHRONOLOGY, 0));

					delvesApplied.forEach((mod, level) -> mod.applyDelve(livingEntity, level));
				} else {
					//this mob is spawned by something that is not a spawner (plugin - command - egg)
					//give only death trigger abilities

					for (DelvesModifier mod : DelvesModifier.deathTriggerDelvesModifier()) {
						mod.applyDelve(livingEntity, delvesApplied.getOrDefault(mod, 0));
					}
					// Apply unyielding on all elites (i.e. gray evokers)
					DelvesModifier.UNYIELDING.applyDelve(livingEntity, delvesApplied.getOrDefault(DelvesModifier.UNYIELDING, 0));
				}
				//Giving tag so this function doesn't run twice on the same mob
				livingEntity.addScoreboardTag(HAS_DELVE_MODIFIER_TAG);

				//Mob stats should ALWAYS work on any mobs even if spawned by command plugin or spawners
				StatMultiplier.applyModifiers(livingEntity, DelvesUtils.getTotalPoints(delvesApplied));
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())) {
			return;
		}
		event.blockList().removeIf(b -> (b.getType() == Material.CHEST && b.getState() instanceof Chest chest && chest.isLocked()) || b.hasMetadata("Unbreakable"));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		//load the delves
		loadPlayerData(player);
		if (getRank(player, DelvesModifier.HAUNTED) > 0 && !player.isDead()) {
			Haunted.applyModifiers(player);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerWalk(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (getRank(player, DelvesModifier.ASTRAL) == 0
			|| player.getGameMode() == GameMode.SPECTATOR
			|| player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		PotionEffect resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
		double resistanceLevel = (resistance == null ? 0 : resistance.getAmplifier() + 1);
		if (resistanceLevel < 5) {
			List<Chunk> chunkList = LocationUtils.getSurroundingChunks(event.getTo().getBlock(), 32);
			for (Chunk chunk : chunkList) {
				for (BlockState interestingBlock : chunk.getTileEntities()) {
					if (ChestUtils.isAstrableChest(interestingBlock.getBlock()) && LocationUtils.blocksAreWithinRadius(event.getTo().getBlock(), interestingBlock.getBlock(), 32) && PlayerUtils.hasLineOfSight(player, interestingBlock.getBlock())) {
						Astral.applyModifiers(interestingBlock.getBlock(), getRank(player, DelvesModifier.ASTRAL));
					}
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		new BukkitRunnable() {

			@Override
			public void run() {
				Player p = event.getPlayer();
				if (!p.isOnline()) {
					PLAYER_DELVE_DUNGEON_MOD_MAP.remove(p.getUniqueId());
				}
			}

		}.runTaskLater(Plugin.getInstance(), 100);
	}

	@EventHandler(ignoreCancelled = true)
	public void onExpChange(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();
		for (DelvesModifier mod : DelvesModifier.rotatingDelveModifiers()) {
			if (getRank(player, mod) > 0) {
				double expBuffPct = .25;
				event.setAmount((int)(event.getAmount() * (1.0 + expBuffPct)));
				return;
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerHurt(DamageEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())) {
			return;
		}

		if (event.getDamagee() instanceof Player player) {
			Infernal.applyDamageModifiers(event, DelvesUtils.getModifierLevel(player, DelvesModifier.INFERNAL));
		}

		//hard coded since magma cubes from Chivalrous should not do damage to player
		if (event.getDamager() != null && Chivalrous.MOUNT_NAMES[1].equals(event.getDamager().getName())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockBreakEventEarly(BlockBreakEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())) {
			return;
		}
		if ((event.getBlock().getState() instanceof Chest chest && chest.isLocked()) || event.getBlock().hasMetadata("Unbreakable")) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreakEventLate(BlockBreakEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())
			    || event.getBlock().getType() != Material.SPAWNER) {
			return;
		}

		Location loc = event.getBlock().getLocation();
		Colossal.applyModifiers(loc, DelvesUtils.getModifierLevel(loc, DelvesModifier.COLOSSAL));

		//region Spawner Breaks Handling
		if (false) { // this is causing massive lag and is not currently used
			ScoreboardManager manager = Bukkit.getScoreboardManager();
			final Scoreboard board = manager.getNewScoreboard();
			if (board.getObjective(SPAWNER_TOTAL_SCORE_NAME) != null) {
				board.registerNewObjective(SPAWNER_TOTAL_SCORE_NAME, "dummy");
			}
			if (board.getObjective(SPAWNER_BREAKS_SCORE_NAME) != null) {
				board.registerNewObjective(SPAWNER_BREAKS_SCORE_NAME, "dummy");
			}

			Location armorStandLoc = event.getPlayer().getWorld().getSpawnLocation(); // get the spawn location
			boolean foundStand = false;
			ArmorStand armorStand = null;
			for (Entity entity : armorStandLoc.getNearbyEntities(2, 2, 2)) { // get the entities at the spawn location
				if (entity.getType().equals(EntityType.ARMOR_STAND) && entity.getCustomName() != null && entity.getCustomName().equals("SpawnerBreaksArmorStand")) { //if it's our marker armorstand
					foundStand = true;
					armorStand = (ArmorStand) entity;
				}
			}
			if (!foundStand) { // create new armor stand
				armorStand = (ArmorStand) event.getPlayer().getWorld().spawnEntity(armorStandLoc, EntityType.ARMOR_STAND);
				armorStand.setVisible(false);
				armorStand.setGravity(false);
				armorStand.setMarker(true);
				armorStand.setCustomName("SpawnerBreaksArmorStand");
			}
			// add one to breaks
			int breaks = ScoreboardUtils.getScoreboardValue(armorStand, SPAWNER_BREAKS_SCORE_NAME).orElse(0) + 1;
			ScoreboardUtils.setScoreboardValue(armorStand, SPAWNER_BREAKS_SCORE_NAME, breaks);
			// if we haven't initialized spawners
			int numSpawnersTotal = ScoreboardUtils.getScoreboardValue(armorStand, SPAWNER_TOTAL_SCORE_NAME).orElse(-2);
			if (numSpawnersTotal < -1) {
				// Haven't been initialized yet - load all the chunks and count the spawners

				// Set the score to -1 to start so that this doesn't get called more than once before it finishes
				ScoreboardUtils.setScoreboardValue(armorStand, SPAWNER_TOTAL_SCORE_NAME, -1);

				// Get starting coords (divide by 16, basically)
				int spawnChunkX = armorStandLoc.getBlockX() >> 4;
				int spawnChunkZ = armorStandLoc.getBlockZ() >> 4;

				World world = armorStandLoc.getWorld();
				AtomicInteger numSpawners = new AtomicInteger(0);
				AtomicInteger numChunksToLoad = new AtomicInteger(64 * 64);

				ArmorStand finalArmorStand = armorStand;
				// Load each chunk async, when they load the callback will be called
				for (int cx = spawnChunkX - CHUNK_SCAN_RADIUS; cx <= spawnChunkX + CHUNK_SCAN_RADIUS; cx++) {
					for (int cz = spawnChunkZ - CHUNK_SCAN_RADIUS; cz <= spawnChunkZ + CHUNK_SCAN_RADIUS; cz++) {
						world.getChunkAtAsync(cx, cz, false /* don't create new chunks */, (Consumer<Chunk>) (chunk) -> {
							if (chunk != null && chunk.isLoaded()) {
								// This gets called once per chunk
								for (BlockState tile : chunk.getTileEntities()) {
									if (tile.getType().equals(Material.SPAWNER)) {
										// Found another spawner
										if (tile.getLocation().subtract(new Vector(0, 1, 0)).getBlock().getType().equals(Material.BEDROCK)) {
											continue;
										}
										numSpawners.incrementAndGet();
									}
								}

							}
							// Decrement the number of chunks left until we get to 0
							int numLeft = numChunksToLoad.decrementAndGet();
							if (numLeft == 0) {
								// This is the last chunk - so we can now store the value to the armor stand
								// Need to run this on the main thread, since this function is labeled async

								ScoreboardUtils.setScoreboardValue(finalArmorStand, SPAWNER_TOTAL_SCORE_NAME, numSpawners.intValue());
							}
						});
					}
				}
			}
		}
		//endregion
	}

	@EventHandler(ignoreCancelled = true)
	public void onChunkUnloadEvent(ChunkUnloadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity.getScoreboardTags().contains(Twisted.TWISTED_MINIBOSS_TAG)) {
				Twisted.despawnTwistedMiniBoss((LivingEntity) entity);
				entity.remove();
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (getRank(player, DelvesModifier.HAUNTED) > 0) {
			Haunted.applyModifiers(player);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (getRank(player, DelvesModifier.HAUNTED) > 0) {
			for (Entity entity : player.getNearbyEntities(100, 100, 100)) {
				if (entity instanceof ArmorStand && entity.getScoreboardTags().contains(PHANTOM_NAME + player.getUniqueId())) {
					entity.remove();
				}
			}
		}

		EntityUtils.removeAttributesContaining(player, Attribute.GENERIC_MAX_HEALTH, "Whispers");

		if (!event.getKeepInventory()) {
			Fragile.applyModifiers(player, DelvesUtils.getModifierLevel(player, DelvesModifier.FRAGILE));
		}

		for (LivingEntity mob : player.getLocation().getNearbyLivingEntities(25)) {
			TwistedMiniBoss boss = BossManager.getInstance().getBoss(mob, TwistedMiniBoss.class);
			if (boss != null) {
				boss.playerDeath(player);
			}
		}
	}

	//utility class to store date and modifier lvl on each player
	public static class DungeonDelveInfo {
		private final Map<DelvesModifier, Integer> mModifierPoint = new HashMap<>();

		public int mTotalPoint = 0;

		@Override public String toString() {
			return "DungeonDelveInfo{" +
					   "TotalPoint=" + mTotalPoint +
				       ",ModifierPoint=" + mModifierPoint +
				       '}';
		}

		protected DungeonDelveInfo cloneDelveInfo() {
			DungeonDelveInfo info = new DungeonDelveInfo();
			info.mModifierPoint.putAll(this.mModifierPoint);
			info.mTotalPoint = this.mTotalPoint;
			return info;
		}

		public HashMap<DelvesModifier, Integer> getMap() {
			return new HashMap<>(mModifierPoint);
		}

		public void put(DelvesModifier mod, int level) {
			mModifierPoint.put(mod, level);
			recalculateTotalPoint();
		}

		public int get(DelvesModifier mod) {
			return mModifierPoint.getOrDefault(mod, 0);
		}

		public void recalculateTotalPoint() {
			mTotalPoint = 0;
			for (Map.Entry<DelvesModifier, Integer> entry : mModifierPoint.entrySet()) {
				mTotalPoint += entry.getValue() * entry.getKey().getPointsPerLevel();
			}
			mTotalPoint = Math.min(mTotalPoint, DelvesUtils.MAX_DEPTH_POINTS);
		}
	}

}
