package com.playmonumenta.plugins.delves;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.commands.SpawnerCountCommand;
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
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;


public class DelvesManager implements Listener {
	public static final String KEY_DELVES_PLUGIN_DATA = "MonumentaDelves";
	protected static final double DELVES_MAX_PARTY_DISTANCE = 32.0;


	/**
	 * This structure contains All the delves mods picked by online players (in this shard)
	 * <p>
	 * it's structured in this way:
	 * <PlayerID, <DungeonID, <ModifierID, points>>>
	 */
	public static final Map<UUID, Map<String, DungeonDelveInfo>> PLAYER_DELVE_DUNGEON_MOD_MAP = new HashMap<>();
	public static final String PHANTOM_NAME = "LoomingConsequence";
	public static final NamespacedKey SPAWNER_BREAKS_DATA_KEY = NamespacedKeyUtils.fromString("monumenta:spawners_broken");
	public static final NamespacedKey SPAWNER_COUNT_DATA_KEY = NamespacedKeyUtils.fromString("monumenta:spawners_total");

	//List of all the shard where we can use delves
	public static final ImmutableSet<String> DUNGEONS = ImmutableSet.of(
		"white",
		"orange",
		"magenta",
		"lightblue",
		"yellow",
		"willows",
		"reverie",
		"lime",
		"pink",
		"gray",
		"lightgray",
		"cyan",
		"purple",
		"teal",
		"forum",
		"shiftingcity",
		"dev1",
		"dev2",
		"dev3",
		"mobs",
		"depths",
		"corridors",
		"ring",
		"ruin",
		"portal",
		"blue",
		"brown",
		"futurama"
	);

	private static final String HAS_DELVE_MODIFIER_TAG = "DelveModifiersApplied";
	public static final String AVOID_MODIFIERS = "boss_delveimmune";

	public static int getRank(Player player, DelvesModifier modifier) {
		if (player == null || modifier == null) {
			return 0;
		}

		if (PLAYER_DELVE_DUNGEON_MOD_MAP.containsKey(player.getUniqueId())) {
			Map<String, DungeonDelveInfo> playerDDinfo = PLAYER_DELVE_DUNGEON_MOD_MAP.getOrDefault(player.getUniqueId(), new HashMap<>());
			DungeonDelveInfo info = playerDDinfo.get(ServerProperties.getShardName());
			if (info != null) {
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

		for (int i = 0; i < dungeonArr.size(); i++) {
			JsonObject dungeonObj = dungeonArr.get(i).getAsJsonObject();
			try {
				String name = dungeonObj.get("dungeonName").getAsString();

				DungeonDelveInfo info = new DungeonDelveInfo();

				JsonElement presetId = dungeonObj.get("presetId");
				if (presetId != null) {
					info.mPresetId = presetId.getAsInt();
				}

				DelvePreset preset = DelvePreset.getDelvePreset(info.mPresetId);
				if (preset != null) {
					// if a preset is saved, ignore the saved modifiers and update to the preset instead
					info.mModifierPoint.putAll(preset.mModifiers);
					info.recalculateTotalPoint();
				} else {
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
				}

				PLAYER_DELVE_DUNGEON_MOD_MAP.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).putIfAbsent(name, info);
				DelvesUtils.updateDelveScoreBoard(player);
			} catch (Exception e) {
				MMLog.warning("[DelveManager] error while loading player info. Reason: " + e.getMessage());
				e.printStackTrace();
				player.sendMessage(Component.text("Some of your delve data have not loaded correctly, try reloading!", NamedTextColor.RED).hoverEvent(HoverEvent.showText(Component.text(e.toString()))));
			}
		}
	}

	public static void savePlayerData(Player player, String dungeon, Map<DelvesModifier, Integer> mods, int presetId) {
		Map<String, DungeonDelveInfo> playerDungeonInfo = PLAYER_DELVE_DUNGEON_MOD_MAP.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>());
		DungeonDelveInfo ddinfo = playerDungeonInfo.computeIfAbsent(dungeon, key -> new DungeonDelveInfo());
		ddinfo.mModifierPoint.clear();
		for (DelvesModifier mod : mods.keySet()) {
			ddinfo.mModifierPoint.put(mod, mods.get(mod));
		}
		ddinfo.mPresetId = presetId;
		ddinfo.recalculateTotalPoint();
	}

	public static boolean validateDelvePreset(Player player, String dungeon) {
		DelvePreset delvePreset;
		if (dungeon.equals("ring")) {
			int presetId = ScoreboardUtils.getScoreboardValue(player, DelvePreset.PRESET_SCOREBOARD).orElse(0);
			if (presetId == 0) {
				return true;
			}
			delvePreset = DelvePreset.getDelvePreset(presetId);
		} else {
			delvePreset = DelvePreset.getDelvePreset(dungeon);
		}
		if (delvePreset == null) {
			return false;
		}

		Map<String, DungeonDelveInfo> playerDungeonInfo = PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId());
		if (playerDungeonInfo == null) {
			return false;
		}

		DungeonDelveInfo ddinfo = playerDungeonInfo.getOrDefault(dungeon, new DungeonDelveInfo());
		return DelvePreset.validatePresetModifiers(ddinfo, delvePreset, false);
	}

	protected static JsonObject convertPlayerData(Player player) {
		Map<String, DungeonDelveInfo> delveInfoMap = PLAYER_DELVE_DUNGEON_MOD_MAP.get(player.getUniqueId());
		if (delveInfoMap == null) {
			return new JsonObject();
		}

		JsonObject obj = new JsonObject();
		JsonArray dungeons = new JsonArray();
		obj.add("dungeons", dungeons);

		for (Map.Entry<String, DungeonDelveInfo> playerDungeonInfo : delveInfoMap.entrySet()) {
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
			dungeonObj.addProperty("presetId", playerDungeonInfo.getValue().mPresetId);
			dungeons.add(dungeonObj);

		}

		return obj;
	}

	public static int getSpawnersBroken(World world) {
		return world.getPersistentDataContainer().getOrDefault(SPAWNER_BREAKS_DATA_KEY, PersistentDataType.INTEGER, -1);
	}

	public static void setSpawnersBroken(World world, int count) {
		world.getPersistentDataContainer().set(SPAWNER_BREAKS_DATA_KEY, PersistentDataType.INTEGER, count);
	}

	public static int getSpawnersTotal(World world) {
		return world.getPersistentDataContainer().getOrDefault(SPAWNER_COUNT_DATA_KEY, PersistentDataType.INTEGER, -1);
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
				for (BlockState interestingBlock : chunk.getTileEntities(b -> b.getType() == Material.CHEST, false)) {
					if (interestingBlock instanceof Chest chest
						    && LocationUtils.blocksAreWithinRadius(event.getTo().getBlock(), interestingBlock.getBlock(), 32)
						    && ChestUtils.isAstrableChest(chest)
						    && PlayerUtils.hasLineOfSight(player, interestingBlock.getBlock())) {
						Astral.applyModifiers(chest, getRank(player, DelvesModifier.ASTRAL));
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
			|| !Plugin.IS_PLAY_SERVER) {
			return;
		}

		spawnerBreakEventHandler(event.getBlock());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockExplodeEventLate(BlockExplodeEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())
			|| !Plugin.IS_PLAY_SERVER) {
			return;
		}

		for (Block block : event.blockList()) {
			spawnerBreakEventHandler(block);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityExplodeEventLate(EntityExplodeEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())
			|| !Plugin.IS_PLAY_SERVER) {
			return;
		}

		for (Block block : event.blockList()) {
			spawnerBreakEventHandler(block);
		}
	}

	public void spawnerBreakEventHandler(Block block) {
		if (block.getType() != Material.SPAWNER) {
			return;
		}
		Location loc = block.getLocation();
		Colossal.applyModifiers(loc, DelvesUtils.getModifierLevel(loc, DelvesModifier.COLOSSAL));

		World world = loc.getWorld();

		// Shifting generates spawners randomly. Modifying the shifting plugin would be a lot of effort, so just count spawners on play for this case.
		boolean shiftingcity = ServerProperties.getShardName().equals("shiftingcity");
		int spawnersTotal = getSpawnersTotal(world);
		if (spawnersTotal == -1 && shiftingcity) {
			// Mark spawner counting as in progress
			world.getPersistentDataContainer().set(DelvesManager.SPAWNER_COUNT_DATA_KEY, PersistentDataType.INTEGER, -2);
			// Start the counter - it will eventually update the total spawner count
			SpawnerCountCommand.count(null, world, 8, true);
		}

		// add one to spawner breaks
		if (spawnersTotal > 0 || shiftingcity) {
			int spawnersBroken = getSpawnersBroken(world);
			world.getPersistentDataContainer().set(SPAWNER_BREAKS_DATA_KEY, PersistentDataType.INTEGER, spawnersBroken < 0 ? 1 : spawnersBroken + 1);
		}
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

		public int mPresetId = 0;

		@Override
		public String toString() {
			return "DungeonDelveInfo{" +
				       "TotalPoint=" + mTotalPoint +
				       ",ModifierPoint=" + mModifierPoint +
				       ",PresetId=" + mPresetId +
				       '}';
		}

		protected DungeonDelveInfo cloneDelveInfo() {
			DungeonDelveInfo info = new DungeonDelveInfo();
			info.mModifierPoint.putAll(this.mModifierPoint);
			info.mTotalPoint = this.mTotalPoint;
			info.mPresetId = this.mPresetId;
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
