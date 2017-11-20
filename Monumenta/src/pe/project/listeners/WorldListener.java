package pe.project.listeners;

import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.world.ChunkLoadEvent;

import pe.project.Plugin;

public class WorldListener implements Listener {
	Plugin mPlugin;
	World mWorld;
	
	public WorldListener(Plugin plugin, World world) {
		mPlugin = plugin;
		mWorld = world;
	}
	
	//	A Chunk Loaded.
	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkLoadEvent(ChunkLoadEvent event) {
		Entity[] entities = event.getChunk().getEntities();
		
		for (Entity entity : entities) {
			if (entity instanceof Monster) {
				Monster mob = (Monster)entity;

				int timer = mPlugin.mCombatLoggingTimers.getTimer(entity.getUniqueId());
				if (timer >= 0) {
					Set<String> tags = mob.getScoreboardTags();
					if (!tags.contains("Elite") && !tags.contains("Boss")) {
						mob.setRemoveWhenFarAway(false);
					}
					
					mPlugin.mCombatLoggingTimers.removeTimer(entity.getUniqueId());
				}
			}
		}
	}
}
