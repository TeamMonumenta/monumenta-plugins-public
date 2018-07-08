package pe.project.utils;

import java.lang.reflect.InvocationTargetException;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataStoreBase;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.World;

public class MetadataUtils {
	/* This method can be used to wrap code that should only execute once per tick
	 * for a given metadata object (block, entity, etc.).
	 *
	 * If this function returns true, the program should proceed - this is the
	 * first time it has been invoked on this particular tick
	 *
	 * If this function returns false, then it has been called already this tick
	 * for this entity / metakey pair. When returning false, the code should not
	 * be executed.
	 */
	public static boolean checkOnceThisTick(Plugin plugin, Entity entity, String metakey) {
		if (entity.hasMetadata(metakey)
		    && entity.getMetadata(metakey).get(0).asInt() == entity.getTicksLived()) {
			return false;
		}

		entity.setMetadata(metakey, new FixedMetadataValue(plugin, entity.getTicksLived()));
		return true;
	}

	/**
	 * This is just another way to check if a certain metakey has been called.
	 *
	 * Comes with the ability to offset the tick amount being checked if ever needed (used for BukkitRunnables)
	 *
	 * @param plugin The class extending JavaPlugin
	 * @param entity The entity being checked
	 * @param metakey A unique key that will be checked
	 * @param tickOffset Offsets the tick amount checked
	 * @return A true/false. If true, this has been called already. If false, it has not been called.
	 */
	public static boolean happenedThisTick(Plugin plugin, Entity entity, String metakey, int tickOffset) {
		if (entity.hasMetadata(metakey)
		    && entity.getMetadata(metakey).get(0).asInt() == entity.getTicksLived() + tickOffset) {
			return true;
		}
		return false;
	}

	public static void removeAllMetadata(Plugin plugin) {
		_removeAllMetadataHelper("getEntityMetadata", plugin.getServer(), plugin);
		_removeAllMetadataHelper("getPlayerMetadata", plugin.getServer(), plugin);
		_removeAllMetadataHelper("getWorldMetadata", plugin.getServer(), plugin);
		for (World world : Bukkit.getWorlds()) {
			_removeAllMetadataHelper("getBlockMetadata", world, plugin);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> void _removeAllMetadataHelper(String getMetaMethodName, Object obj, Plugin plugin) {
		MetadataStoreBase<T> metaStore = null;
		Map<String, Map<Plugin, MetadataValue>> metaMap = null;

		try {
			/*
			 * Use reflection to reach into the object and grab the MetadataStoreBase object
			 */
			java.lang.reflect.Method method = obj.getClass().getMethod(getMetaMethodName);
			metaStore = (MetadataStoreBase<T>) method.invoke(obj);

			if (metaStore == null) {
				plugin.getLogger().log(Level.SEVERE,
				                       "While clearing metadata, retrieved null metastore object for '" + getMetaMethodName + "'");
				return;
			}

			/*
			 * Use reflection (again) to reach into the metadata store to grab the underlying (private) map
			 */
			if (metaStore.getClass().getSuperclass() == null) {
				plugin.getLogger().log(Level.SEVERE,
				                       "While clearing metadata, metastore has no superclass for '" + getMetaMethodName + "'");
				return;
			}

			java.lang.reflect.Field field =
			    metaStore.getClass().getSuperclass().getDeclaredField("metadataMap");
			field.setAccessible(true);
			metaMap = (Map<String, Map<Plugin, MetadataValue>>) field.get(metaStore);
		} catch (SecurityException | NoSuchMethodException | NoSuchFieldException | IllegalArgumentException
			         | IllegalAccessException | InvocationTargetException e) {
			plugin.getLogger().log(Level.SEVERE,
			                       "While clearing metadata, failed to retrieve CraftServer metadata map object for '" +
			                       getMetaMethodName + "'");
			e.printStackTrace();
			return;
		}

		if (metaMap == null) {
			plugin.getLogger().log(Level.SEVERE,
			                       "While clearing metadata, retrieved null metadata map contents for '" + getMetaMethodName + "'");
			return;
		}

		/* Clear out the metadata map of any references to this plugin */
		Iterator<Map<Plugin, MetadataValue>> iterator = metaMap.values().iterator();
		while (iterator.hasNext()) {
			Map<Plugin, MetadataValue> values = iterator.next();
			if (values.containsKey(plugin)) {
				values.remove(plugin);
			}
			if (values.isEmpty()) {
				iterator.remove();
			}
		}
	}
}
