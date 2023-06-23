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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Marker;
import org.bukkit.inventory.ItemStack;
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
}
