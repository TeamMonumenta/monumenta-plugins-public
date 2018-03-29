package pe.project.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import pe.project.Constants;
import pe.project.Plugin;

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

	public static void clearMetadata(Player player, Plugin plugin){
		for (String meta : Constants.METADATA_KEYS_TO_BE_DESTROYED){
			if (player.hasMetadata(meta)){
				player.removeMetadata(meta, plugin);
			}
		}
	}
}
