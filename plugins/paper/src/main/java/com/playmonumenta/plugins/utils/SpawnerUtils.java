package com.playmonumenta.plugins.utils;

import com.goncalomb.bukkit.nbteditor.nbt.EntityNBT;
import com.goncalomb.bukkit.nbteditor.nbt.SpawnerNBTWrapper;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.spawners.SpawnerActionManager;
import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTTileEntity;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Marker;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

/* How Spawner Break Actions are stored in NBT:
BreakActions [
	{
		identifier: String
		parameters: {
			key1: value1
			key2: value2
			...
		}
	},
	{
		identifier: String
		parameters: {
			key1: value1
			key2: value2
			...
		}
	},
	...
]
*/

public class SpawnerUtils {
	// Backwards compatibility: spawners have been placed with this tag in use.
	// Only remove this tag, and add the one below it.
	public static final String SHIELDED_SPAWNER_MARKER_TAG = "spawner_shield_display_marker";
	public static final String EFFECTS_SPAWNER_MARKER_TAG = "spawner_effects_display_marker";
	public static final String SHIELDS_ATTRIBUTE = "Shields";
	public static final String LOS_POOL_ATTRIBUTE = "LoSPool";
	public static final String BREAK_ACTIONS_ATTRIBUTE = "BreakActions";

	private static final NamespacedKey ORIGINAL_MAX_DELAY = NamespacedKeyUtils.fromString("original_max_delay");
	private static final NamespacedKey ORIGINAL_MIN_DELAY = NamespacedKeyUtils.fromString("original_min_delay");
	private static final NamespacedKey LAST_MULTIPLIER = NamespacedKeyUtils.fromString("last_multiplier");
	private static final NamespacedKey TORCH_LOCATIONS = NamespacedKeyUtils.fromString("spawner_torches");
	private static final NamespacedKey X = NamespacedKeyUtils.fromString("x");
	private static final NamespacedKey Y = NamespacedKeyUtils.fromString("y");
	private static final NamespacedKey Z = NamespacedKeyUtils.fromString("z");
	private static final int MAX_TORCH_TAXICAB_DISTANCE = 8;
	private static final double SPAWNER_DELAY_MULTIPLIER_CAP = 10;

	/**
	 * Returns true if the damage is enough to break the spawner.
	 * Otherwise, removes shields equal to the damage done.
	 * @param block the spawner block.
	 * @param damage the damage dealt by the pickaxe.
	 */
	public static boolean tryBreakSpawner(Block block, int damage) {
		if (!hasShieldsAttribute(block)) {
			return true;
		}

		int shields = getShields(block);

		if (shields < damage) {
			setShields(block, 0);
			return true;
		}

		setShields(block, shields - damage);
		return false;
	}

	public static void startSpawnerEffectsDisplay(Marker marker) {
		if (!marker.getScoreboardTags().contains(EFFECTS_SPAWNER_MARKER_TAG) && !marker.getScoreboardTags().contains(SHIELDED_SPAWNER_MARKER_TAG)) {
			return;
		}

		new BukkitRunnable() {
			final Marker mMarker = marker;
			final PPCircle mHealthyShield = new PPCircle(Particle.SOUL_FIRE_FLAME, mMarker.getLocation(), 1)
				.countPerMeter(2).distanceFalloff(20).ringMode(true);
			final boolean mHasLosPool = getLosPool(marker.getLocation().getBlock()) != null;

			@Override
			public void run() {
				// If the marker despawned or got removed, cancel the runnable.
				if (!mMarker.isValid()) {
					cancel();
					return;
				}

				Block spawnerBlock = marker.getLocation().getBlock();
				if (!SpawnerUtils.isSpawner(spawnerBlock)) {
					// The spawner was destroyed and the marker somehow is still lingering around.
					cancel();
					return;
				}

				// Spawn the particles
				int shields = SpawnerUtils.getShields(spawnerBlock);
				if (shields > 0) {
					mHealthyShield.spawnFull();
				}

				if (mHasLosPool) {
					Location centerLoc = BlockUtils.getCenterBlockLocation(spawnerBlock);
					// Place colorful particles at the corners of the spawner.
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(0.5, 0.5, 0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(0.5, -0.5, 0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(-0.5, 0.5, 0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(-0.5, -0.5, 0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(0.5, 0.5, -0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(0.5, -0.5, -0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(-0.5, 0.5, -0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();
					new PartialParticle(Particle.REDSTONE, centerLoc.clone().add(-0.5, -0.5, -0.5), 1).data(ParticleUtils.getRandomColorOptions(150, 1)).spawnFull();

					// Explicit null check for Review Dog
					String losPool = getLosPool(spawnerBlock);
					if (losPool != null) {
						List<Soul> souls = LibraryOfSoulsIntegration.getPool(losPool).keySet().stream().toList();
						if (souls.isEmpty()) {
							return;
						}
						Soul soul = souls.get(0);
						SpawnerNBTWrapper wrapper = new SpawnerNBTWrapper(spawnerBlock);
						wrapper.clearEntities();
						wrapper.addEntity(new SpawnerNBTWrapper.SpawnerEntity(EntityNBT.fromEntityData(soul.getNBT()), 1));
						wrapper.save();
					}
				}

				List<String> breakActionIdentifiers = SpawnerUtils.getBreakActionIdentifiers(spawnerBlock);
				breakActionIdentifiers.forEach(id -> {
					SpawnerBreakAction action = SpawnerActionManager.getAction(id);
					if (action != null) {
						action.periodicAesthetics(spawnerBlock);
					}
				});
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 20);
	}

	public static List<String> getBreakActionIdentifiers(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return Collections.emptyList();
		}

		return new NBTItem(spawnerItem).getCompoundList(BREAK_ACTIONS_ATTRIBUTE)
			.stream().map(compound -> compound.getString("identifier")).toList();
	}

	public static List<String> getBreakActionIdentifiers(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return Collections.emptyList();
		}

		return new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE)
			.stream().map(compound -> compound.getString("identifier")).toList();
	}

	public static void transferBreakActionList(ItemStack spawnerItem, Block spawnerBlock) {
		if (!isSpawner(spawnerItem) || !isSpawner(spawnerBlock)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList itemBreakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		NBTCompoundList tileBreakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		itemBreakActions.forEach(tileBreakActions::addCompound);
	}

	public static void addBreakAction(ItemStack spawnerItem, String actionIdentifier) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTCompoundList breakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() == 0) {
			// Create a new container for the action, add the identifier,
			// and add the compound containing the parameters and their default values.
			NBTContainer container = new NBTContainer();
			container.setString("identifier", actionIdentifier);
			NBTCompound parameters = container.addCompound("parameters");
			addParametersToCompound(SpawnerActionManager.getActionParameters(actionIdentifier), parameters);
			breakActions.addCompound(container);
			spawnerItem.setItemMeta(item.getItem().getItemMeta());
		}
	}

	public static void addBreakAction(Block spawnerBlock, String actionIdentifier) {
		if (!isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList breakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() == 0) {
			// Create a new container for the action, add the identifier,
			// and add the compound containing the parameters and their default values.
			NBTContainer container = new NBTContainer();
			container.setString("identifier", actionIdentifier);
			NBTCompound parameters = container.addCompound("parameters");
			addParametersToCompound(SpawnerActionManager.getActionParameters(actionIdentifier), parameters);
			breakActions.addCompound(container);
		}
	}

	public static void addBreakActions(ItemStack spawnerItem, List<String> actionIdentifiers) {
		actionIdentifiers.forEach(actionIdentifier -> addBreakAction(spawnerItem, actionIdentifier));
	}

	public static void addBreakActions(Block spawnerBlock, List<String> actionIdentifiers) {
		actionIdentifiers.forEach(actionIdentifier -> addBreakAction(spawnerBlock, actionIdentifier));
	}

	public static void addParametersToCompound(Map<String, Object> parameters, NBTCompound compound) {
		parameters.forEach(compound::setObject);
	}

	public static void removeBreakAction(ItemStack spawnerItem, String actionIdentifier) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTCompoundList breakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Remove the action
			breakActions.remove(wantedAction.get(0));
			spawnerItem.setItemMeta(item.getItem().getItemMeta());
		}
	}

	public static void setParameterValue(ItemStack spawnerItem, String actionIdentifier, String parameterName, Object value) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTCompoundList breakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		// Try to find the requested action
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Set the requested parameter
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			parameters.setString(parameterName, MessagingUtils.GSON.toJson(value, value.getClass()));
			spawnerItem.setItemMeta(item.getItem().getItemMeta());
		}
	}

	public static void setParameterValue(Block spawnerBlock, String actionIdentifier, String parameterName, Object value) {
		if (!isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList breakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		// Try to find the requested action
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Set the requested parameter
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			parameters.setString(parameterName, MessagingUtils.GSON.toJson(value, value.getClass()));
		}
	}

	public static @Nullable Object getParameterValue(ItemStack spawnerItem, String actionIdentifier, String parameterName) {
		if (!isSpawner(spawnerItem)) {
			return null;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTCompoundList breakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		// Try to find the requested action
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Try to find the requested parameter
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			if (parameters.hasTag(parameterName)) {
				String json = parameters.getOrNull(parameterName, String.class);
				if (json == null) {
					return null;
				}
				Object parameter = SpawnerActionManager.getActionParameters(actionIdentifier).get(parameterName);
				if (parameter == null) {
					return null;
				}
				return MessagingUtils.GSON.fromJson(json, parameter.getClass());
			}
		}

		return null;
	}

	public static @Nullable Object getParameterValue(Block spawnerBlock, String actionIdentifier, String parameterName) {
		if (!isSpawner(spawnerBlock)) {
			return null;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList breakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		// Try to find the requested action
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Try to find the requested parameter
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			if (parameters.hasTag(parameterName)) {
				String json = parameters.getOrNull(parameterName, String.class);
				if (json == null) {
					return null;
				}
				Object parameter = SpawnerActionManager.getActionParameters(actionIdentifier).get(parameterName);
				if (parameter == null) {
					return null;
				}
				return MessagingUtils.GSON.fromJson(json, parameter.getClass());
			}
		}

		return null;
	}

	public static HashMap<String, Object> getStoredParameters(Block spawnerBlock, String actionIdentifier) {
		if (!isSpawner(spawnerBlock)) {
			return new HashMap<>();
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		NBTCompoundList breakActions = tileEntity.getPersistentDataContainer().getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		List<ReadWriteNBT> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			HashMap<String, Object> parameterMap = new HashMap<>(SpawnerActionManager.getActionParameters(actionIdentifier));
			ReadWriteNBT actionCompound = wantedAction.get(0);
			ReadWriteNBT parameters = actionCompound.getOrCreateCompound("parameters");
			// Start from base parameters map and replace the values with the ones stored on the block.
			parameterMap.forEach((key, value) -> {
				String json = parameters.getOrNull(key, String.class);
				if (json == null) {
					return;
				}
				parameterMap.replace(key, MessagingUtils.GSON.fromJson(json, value.getClass()));
			});

			return parameterMap;
		}

		return new HashMap<>();
	}

	public static @Nullable String getLosPool(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return null;
		}

		String losPool = new NBTItem(spawnerItem).getString(LOS_POOL_ATTRIBUTE);

		return losPool.equals("") ? null : losPool;
	}

	public static @Nullable String getLosPool(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return null;
		}

		String losPool = new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer().getString(LOS_POOL_ATTRIBUTE);

		return losPool.equals("") ? null : losPool;
	}

	public static void setLosPool(ItemStack spawnerItem, String losPool) {
		if (!isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		item.setString(LOS_POOL_ATTRIBUTE, losPool);
		spawnerItem.setItemMeta(item.getItem().getItemMeta());
	}

	public static void setLosPool(Block spawnerBlock, String losPool) {
		if (!isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		tileEntity.getPersistentDataContainer().setString(LOS_POOL_ATTRIBUTE, losPool);
	}

	public static boolean hasShieldsAttribute(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return false;
		}

		return new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer().getKeys().contains(SHIELDS_ATTRIBUTE);
	}

	public static int getShields(ItemStack spawnerItem) {
		if (!isSpawner(spawnerItem)) {
			return 0;
		}

		NBTItem item = new NBTItem(spawnerItem);

		if (!item.hasTag(SHIELDS_ATTRIBUTE)) {
			return 0;
		}

		return item.getInteger(SHIELDS_ATTRIBUTE);
	}

	public static int getShields(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return 0;
		}

		NBTCompound dataContainer = new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer();

		if (!dataContainer.hasTag(SHIELDS_ATTRIBUTE)) {
			return 0;
		}

		return dataContainer.getInteger(SHIELDS_ATTRIBUTE);
	}

	public static void setShields(ItemStack spawnerItem, int shields) {
		if (shields < 0 || !isSpawner(spawnerItem)) {
			return;
		}

		NBTItem item = new NBTItem(spawnerItem);
		item.setInteger(SHIELDS_ATTRIBUTE, shields);
		spawnerItem.setItemMeta(item.getItem().getItemMeta());
	}

	public static void setShields(Block spawnerBlock, int shields) {
		if (shields < 0 || !isSpawner(spawnerBlock)) {
			return;
		}

		NBTTileEntity tileEntity = new NBTTileEntity(spawnerBlock.getState());
		tileEntity.getPersistentDataContainer().setInteger(SHIELDS_ATTRIBUTE, shields);
	}

	public static boolean isSpawner(Block block) {
		return block.getType().equals(Material.SPAWNER);
	}

	public static boolean isSpawner(ItemStack item) {
		return item.getType().equals(Material.SPAWNER);
	}

	public static void addEffectsDisplayMarker(Block block) {
		Entity entity = block.getWorld().spawnEntity(BlockUtils.getCenterBlockLocation(block), EntityType.MARKER);
		if (entity instanceof Marker marker) {
			marker.addScoreboardTag(EFFECTS_SPAWNER_MARKER_TAG);
			startSpawnerEffectsDisplay(marker);
		}
	}

	public static void removeEffectsDisplayMarker(Block block) {
		BlockUtils.getCenterBlockLocation(block).getNearbyEntities(0.1, 0.1, 0.1).forEach(e -> {
			if (e instanceof Marker marker &&
				(marker.getScoreboardTags().contains(SHIELDED_SPAWNER_MARKER_TAG) || marker.getScoreboardTags().contains(EFFECTS_SPAWNER_MARKER_TAG))) {
				marker.remove();
			}
		});
	}

	public static boolean hasEffectsDisplayMarker(Block block) {
		return BlockUtils.getCenterBlockLocation(block).getNearbyEntities(0.1, 0.1, 0.1)
			.stream().anyMatch(e -> (e instanceof Marker marker &&
				(marker.getScoreboardTags().contains(SHIELDED_SPAWNER_MARKER_TAG) || marker.getScoreboardTags().contains(EFFECTS_SPAWNER_MARKER_TAG)))
			);
	}

	public static void addTorch(Block block) {
		List<CreatureSpawner> spawners = getNearbySpawners(block);
		for (CreatureSpawner spawner : spawners) {
			if (addTorchToSpawner(spawner, block)) {
				updateSpawnDelay(spawner);
			}
		}
	}

	public static void removeTorch(Block block) {
		List<CreatureSpawner> spawners = getNearbySpawners(block);
		for (CreatureSpawner spawner : spawners) {
			if (filterTorchesFromSpawner(spawner)) {
				updateSpawnDelay(spawner);
			}
		}
	}

	private static List<CreatureSpawner> getNearbySpawners(Block block) {
		List<CreatureSpawner> spawners = new ArrayList<>();
		for (int x = -16; x <= 16; x += 16) {
			for (int z = -16; z <= 16; z += 16) {
				Chunk chunk = block.getLocation().clone().add(x, 0, z).getChunk();
				if (!chunk.isLoaded()) {
					continue;
				}
				chunk.getTileEntities();
				spawners.addAll(chunk.getTileEntities(b -> BlockUtils.taxiCabDistance(block, b) <= MAX_TORCH_TAXICAB_DISTANCE, true).stream().filter(b -> b instanceof CreatureSpawner).map(b -> (CreatureSpawner) b).toList());
			}
		}
		return spawners;
	}

	private static boolean addTorchToSpawner(CreatureSpawner spawner, Block torch) {
		if (BlockUtils.findNonOccludingTaxicabDistance(torch, spawner.getBlock(), MAX_TORCH_TAXICAB_DISTANCE) >= 0) {
			List<Block> torches = getTorches(spawner);
			torches.add(torch);
			setTorches(spawner, torches);
			return true;
		}
		return false;
	}

	private static boolean filterTorchesFromSpawner(CreatureSpawner spawner) {
		List<Block> torches = getTorches(spawner);
		int count = torches.size();
		torches.removeIf(b -> !BlockUtils.isTorch(b));
		if (torches.size() < count) {
			setTorches(spawner, torches);
			return true;
		}
		return false;
	}

	private static List<Block> getTorches(CreatureSpawner spawner) {
		World world = spawner.getWorld();
		PersistentDataContainer[] containers = getTorchContainers(spawner);
		return new ArrayList<>(Arrays.stream(containers).map(c -> getBlockFromContainer(c, world)).toList());
	}

	private static PersistentDataContainer[] getTorchContainers(CreatureSpawner spawner) {
		return spawner.getPersistentDataContainer().getOrDefault(TORCH_LOCATIONS, PersistentDataType.TAG_CONTAINER_ARRAY, new PersistentDataContainer[]{});
	}

	private static void setTorches(CreatureSpawner spawner, List<Block> torches) {
		PersistentDataContainer persistentDataContainer = spawner.getPersistentDataContainer();
		PersistentDataAdapterContext context = persistentDataContainer.getAdapterContext();
		persistentDataContainer.set(TORCH_LOCATIONS, PersistentDataType.TAG_CONTAINER_ARRAY,
			torches.stream().map(b -> getContainerFromBlock(b, context)).toArray(PersistentDataContainer[]::new));
	}

	private static Block getBlockFromContainer(PersistentDataContainer c, World world) {
		return new Location(world, Objects.requireNonNull(c.get(X, PersistentDataType.INTEGER)), Objects.requireNonNull(c.get(Y, PersistentDataType.INTEGER)), Objects.requireNonNull(c.get(Z, PersistentDataType.INTEGER))).getBlock();
	}

	private static PersistentDataContainer getContainerFromBlock(Block b, PersistentDataAdapterContext context) {
		PersistentDataContainer container = context.newPersistentDataContainer();
		container.set(X, PersistentDataType.INTEGER, b.getX());
		container.set(Y, PersistentDataType.INTEGER, b.getY());
		container.set(Z, PersistentDataType.INTEGER, b.getZ());
		return container;
	}

	private static void updateSpawnDelay(CreatureSpawner spawner) {
		PersistentDataContainer persistentDataContainer = spawner.getPersistentDataContainer();
		// Initialize the default values of the max and min delay
		int originalMaxDelay;
		if (!persistentDataContainer.has(ORIGINAL_MAX_DELAY)) {
			originalMaxDelay = spawner.getMaxSpawnDelay();
			persistentDataContainer.set(ORIGINAL_MAX_DELAY, PersistentDataType.INTEGER, originalMaxDelay);
		} else {
			originalMaxDelay = Objects.requireNonNull(persistentDataContainer.get(ORIGINAL_MAX_DELAY, PersistentDataType.INTEGER));
		}

		int originalMinDelay;
		if (!persistentDataContainer.has(ORIGINAL_MIN_DELAY)) {
			originalMinDelay = spawner.getMinSpawnDelay();
			persistentDataContainer.set(ORIGINAL_MIN_DELAY, PersistentDataType.INTEGER, originalMinDelay);
		} else {
			originalMinDelay = Objects.requireNonNull(persistentDataContainer.get(ORIGINAL_MIN_DELAY, PersistentDataType.INTEGER));
		}

		double lastMultiplier = !persistentDataContainer.has(LAST_MULTIPLIER) ? 1 : Objects.requireNonNull(persistentDataContainer.get(LAST_MULTIPLIER, PersistentDataType.DOUBLE));

		Block spawnerBlock = spawner.getBlock();
		double multiplier = 1;
		List<Block> torches = getTorches(spawner);
		for (Block torch : torches) {
			int distance = BlockUtils.findNonOccludingTaxicabDistance(torch, spawnerBlock, MAX_TORCH_TAXICAB_DISTANCE);
			if (distance <= 0) {
				continue;
			}
			multiplier = Math.min(SPAWNER_DELAY_MULTIPLIER_CAP, (multiplier * (distance + 1)) / distance);
		}

		double multiplierRatio = multiplier / lastMultiplier;
		persistentDataContainer.set(LAST_MULTIPLIER, PersistentDataType.DOUBLE, multiplier);

		// Need to do them in the right order or else it sometimes throws an IllegalArgumentException
		if (multiplierRatio > 1) {
			spawner.setMaxSpawnDelay((int) (originalMaxDelay * multiplier));
			spawner.setMinSpawnDelay((int) (originalMinDelay * multiplier));
		} else {
			spawner.setMinSpawnDelay((int) (originalMinDelay * multiplier));
			spawner.setMaxSpawnDelay((int) (originalMaxDelay * multiplier));
		}
		spawner.setDelay((int) (spawner.getDelay() * multiplierRatio));
		spawner.update(false, false);
	}

	public static void addSpawnerEffectMarkers(Location corner1, Location corner2) {
		int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
		int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
		int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
		int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
		int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
		int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Location blockLoc = corner1.clone().set(x, y, z);
					Block block = blockLoc.getBlock();

					if (isSpawner(block) && !hasEffectsDisplayMarker(block)) {
						addEffectsDisplayMarker(block);
					}
				}
			}
		}
	}
}
