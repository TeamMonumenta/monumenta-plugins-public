package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.spawners.SpawnerActionManager;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import de.tr7zw.nbtapi.NBTTileEntity;
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

	public static final String SHIELDED_SPAWNER_MARKER_TAG = "spawner_shield_display_marker";
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

	public static void startShieldedSpawnerDisplay(Marker marker) {
		if (!marker.getScoreboardTags().contains(SHIELDED_SPAWNER_MARKER_TAG)) {
			return;
		}

		new BukkitRunnable() {
			final Marker mMarker = marker;
			final PPCircle mHealthyShield = new PPCircle(Particle.SOUL_FIRE_FLAME, mMarker.getLocation(), 1)
				.countPerMeter(2).distanceFalloff(20).ringMode(true);
			final PPCircle mMediumShield = new PPCircle(Particle.FLAME, mMarker.getLocation(), 1)
				.countPerMeter(2).distanceFalloff(20).ringMode(true);
			final PPCircle mLowShield = new PPCircle(Particle.SMALL_FLAME, mMarker.getLocation(), 1)
				.countPerMeter(2).distanceFalloff(20).ringMode(true);

			@Override
			public void run() {
				// If the marker despawned or got removed, cancel the runnable.
				if (!mMarker.isValid()) {
					cancel();
					return;
				}

				// Spawn the particles
				int shields = SpawnerUtils.getShields(marker.getLocation().getBlock());
				switch (shields) {
					case 1 -> mLowShield.spawnFull();
					case 2 -> mMediumShield.spawnFull();
					default -> mHealthyShield.spawnFull();
				}
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
		List<NBTListCompound> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
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
		List<NBTListCompound> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
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
		List<NBTListCompound> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
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
		List<NBTListCompound> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Set the requested parameter
			NBTCompound actionCompound = wantedAction.get(0);
			NBTCompound parameters = actionCompound.getCompound("parameters");
			parameters.setObject(parameterName, value);
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
		List<NBTListCompound> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Set the requested parameter
			NBTCompound actionCompound = wantedAction.get(0);
			NBTCompound parameters = actionCompound.getCompound("parameters");
			parameters.setObject(parameterName, value);
		}
	}

	public static @Nullable Object getParameterValue(ItemStack spawnerItem, String actionIdentifier, String parameterName) {
		if (!isSpawner(spawnerItem)) {
			return null;
		}

		NBTItem item = new NBTItem(spawnerItem);
		NBTCompoundList breakActions = item.getCompoundList(BREAK_ACTIONS_ATTRIBUTE);
		// Try to find the requested action
		List<NBTListCompound> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Try to find the requested parameter
			NBTCompound actionCompound = wantedAction.get(0);
			NBTCompound parameters = actionCompound.getCompound("parameters");
			if (parameters.hasKey(parameterName)) {
				return parameters.getObject(parameterName, parameters.getType(parameterName).getDeclaringClass());
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
		List<NBTListCompound> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			// Try to find the requested parameter
			NBTCompound actionCompound = wantedAction.get(0);
			NBTCompound parameters = actionCompound.getCompound("parameters");
			if (parameters.hasKey(parameterName)) {
				return parameters.getObject(parameterName, parameters.getType(parameterName).getDeclaringClass());
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
		List<NBTListCompound> wantedAction = breakActions.stream().filter(action -> action.getString("identifier").equals(actionIdentifier)).toList();
		if (wantedAction.size() > 0) {
			HashMap<String, Object> parameterMap = new HashMap<>(SpawnerActionManager.getActionParameters(actionIdentifier));
			NBTCompound actionCompound = wantedAction.get(0);
			NBTCompound parameters = actionCompound.getCompound("parameters");
			// Start from base parameters map and replace the values with the ones stored on the block.
			parameterMap.forEach((key, value) -> {
				Object currParam = parameters.getObject(key, value.getClass());
				parameterMap.replace(key, currParam);
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

		if (!item.hasKey(SHIELDS_ATTRIBUTE)) {
			return 0;
		}

		return item.getInteger(SHIELDS_ATTRIBUTE);
	}

	public static int getShields(Block spawnerBlock) {
		if (!isSpawner(spawnerBlock)) {
			return 0;
		}

		NBTCompound dataContainer = new NBTTileEntity(spawnerBlock.getState()).getPersistentDataContainer();

		if (!dataContainer.hasKey(SHIELDS_ATTRIBUTE)) {
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

	public static void addShieldDisplayMarker(Block block) {
		Entity entity = block.getWorld().spawnEntity(BlockUtils.getCenterBlockLocation(block), EntityType.MARKER);
		if (entity instanceof Marker marker) {
			marker.addScoreboardTag(SHIELDED_SPAWNER_MARKER_TAG);
			startShieldedSpawnerDisplay(marker);
		}
	}

	public static void removeShieldDisplayMarker(Block block) {
		BlockUtils.getCenterBlockLocation(block).getNearbyEntities(0.1, 0.1, 0.1).forEach(e -> {
			if (e instanceof Marker marker && marker.getScoreboardTags().contains(SHIELDED_SPAWNER_MARKER_TAG)) {
				marker.remove();
			}
		});
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

		spawner.setMaxSpawnDelay((int) (originalMaxDelay * multiplier));
		spawner.setMinSpawnDelay((int) (originalMinDelay * multiplier));
		spawner.setDelay((int) (spawner.getDelay() * multiplierRatio));
		spawner.update(false, false);
	}
}
