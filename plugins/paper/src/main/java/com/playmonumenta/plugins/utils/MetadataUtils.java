package com.playmonumenta.plugins.utils;

import java.util.function.Supplier;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

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
		if (happenedThisTick(entity, metakey)) {
			return false;
		}
		entity.setMetadata(metakey, new FixedMetadataValue(plugin, entity.getTicksLived()));
		return true;
	}

	/**
	 * This is just another way to check if a certain metakey has been called.
	 * <p>
	 * Comes with the ability to offset the tick amount being checked if ever needed (used for BukkitRunnables)
	 *
	 * @param entity     The entity being checked
	 * @param metakey    A unique key that will be checked
	 * @param tickOffset Offsets the tick amount checked
	 * @return A true/false. If true, this has been called already. If false, it has not been called.
	 */
	public static boolean happenedThisTick(Entity entity, String metakey, int tickOffset) {
		return entity.hasMetadata(metakey)
			       && entity.getMetadata(metakey).get(0).asInt() == entity.getTicksLived() + tickOffset;
	}

	public static boolean happenedThisTick(Entity entity, String metakey) {
		return happenedThisTick(entity, metakey, 0);
	}

	public static void removeAllMetadata(Plugin plugin) {
		NmsUtils.getVersionAdapter().removeAllMetadata(plugin);
	}

	public static <T> T setMetadata(Metadatable metadatable, String key, T value) {
		metadatable.setMetadata(key, new FixedMetadataValue(com.playmonumenta.plugins.Plugin.getInstance(), value));
		return value;
	}

	public static MetadataValue getMetadataValue(Metadatable metadatable, String key) {
		for (MetadataValue value : metadatable.getMetadata(key)) {
			if (value.getOwningPlugin() instanceof com.playmonumenta.plugins.Plugin) {
				return value;
			}
		}
		return null;
	}

	public static <T> T getMetadata(Metadatable metadatable, String key, T defaultValue) {
		MetadataValue metadata = getMetadataValue(metadatable, key);
		if (metadata != null) {
			return (T) metadata.value();
		}
		return defaultValue;
	}

	public static <T> T getOrSetMetadata(Metadatable metadatable, String key, T value) {
		return getOrSetMetadata(metadatable, key, () -> value);
	}

	public static <T> T getOrSetMetadata(Metadatable metadatable, String key, Supplier<T> valueSupplier) {
		MetadataValue metadata = getMetadataValue(metadatable, key);
		if (metadata != null) {
			return (T) metadata.value();
		}
		T value = valueSupplier.get();
		metadatable.setMetadata(key, new FixedMetadataValue(com.playmonumenta.plugins.Plugin.getInstance(), value));
		return value;
	}

}
