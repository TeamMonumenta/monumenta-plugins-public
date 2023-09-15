package com.playmonumenta.plugins.chunk;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class ChunkManager implements Listener {
	private enum ChunkType {
		BLOCK_CHUNK,
		BLOCK_CHUNK_LOAD_DELAY,
		ENTITY_CHUNK,
		ENTITY_CHUNK_LOAD_DELAY;
	}

	private static final Map<UUID, Map<Long, EnumSet<ChunkType>>> mLoadedChunks = new HashMap<>();
	private static final Map<UUID, Set<Long>> mNeedsSave = new HashMap<>();
	private static int mLoaded = 0;
	private final Plugin mPlugin;

	public ChunkManager(Plugin plugin) {
		mPlugin = plugin;
		for (World world : Bukkit.getServer().getWorlds()) {
			UUID worldId = world.getUID();
			Map<Long, EnumSet<ChunkType>> worldChunks
				= mLoadedChunks.computeIfAbsent(worldId, (UUID unused1) -> new HashMap<>());
			for (Chunk chunk : world.getLoadedChunks()) {
				long chunkKey = chunk.getChunkKey();
				worldChunks.put(chunkKey, EnumSet.allOf(ChunkType.class));
				mLoaded++;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		UUID worldId = event.getWorld().getUID();
		Map<Long, EnumSet<ChunkType>> worldChunks
			= mLoadedChunks.computeIfAbsent(worldId, (UUID unused1) -> new HashMap<>());

		Chunk chunk = event.getChunk();
		long chunkKey = chunk.getChunkKey();
		EnumSet<ChunkType> loadedState
			= worldChunks.computeIfAbsent(chunkKey, (Long unused) -> EnumSet.noneOf(ChunkType.class));

		loadedState.add(ChunkType.BLOCK_CHUNK);
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			if (loadedState.contains(ChunkType.BLOCK_CHUNK)) {
				if (loadedState.add(ChunkType.BLOCK_CHUNK_LOAD_DELAY)) {
					if (loadedState.size() == ChunkType.values().length) {
						mLoaded++;
						MMLog.finest(() -> "[CHUNK] Loaded " + mLoaded + "/" + worldChunks.size() + "/" + mLoadedChunks.size() + "chunk(s); block load");
						Bukkit.getPluginManager().callEvent(new ChunkFullLoadEvent(chunk));
					}
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityLoadEvent(EntitiesLoadEvent event) {
		UUID worldId = event.getWorld().getUID();
		Map<Long, EnumSet<ChunkType>> worldChunks
			= mLoadedChunks.computeIfAbsent(worldId, (UUID unused1) -> new HashMap<>());

		Chunk chunk = event.getChunk();
		long chunkKey = chunk.getChunkKey();
		EnumSet<ChunkType> loadedState
			= worldChunks.computeIfAbsent(chunkKey, (Long unused) -> EnumSet.noneOf(ChunkType.class));

		loadedState.add(ChunkType.ENTITY_CHUNK);
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			if (loadedState.contains(ChunkType.ENTITY_CHUNK)) {
				if (loadedState.add(ChunkType.ENTITY_CHUNK_LOAD_DELAY)) {
					if (loadedState.size() == ChunkType.values().length) {
						mLoaded++;
						MMLog.finest(() -> "[CHUNK] Loaded " + mLoaded + "/" + worldChunks.size() + "/" + mLoadedChunks.size() + "chunk(s); entity load");
						Bukkit.getPluginManager().callEvent(new ChunkFullLoadEvent(chunk));
					}
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		UUID worldId = event.getWorld().getUID();
		Map<Long, EnumSet<ChunkType>> worldChunks
			= mLoadedChunks.computeIfAbsent(worldId, (UUID unused1) -> new HashMap<>());
		Set<Long> worldNeedsSave
			= mNeedsSave.computeIfAbsent(worldId, (UUID unused1) -> new HashSet<>());

		Chunk chunk = event.getChunk();
		long chunkKey = chunk.getChunkKey();
		EnumSet<ChunkType> loadedState
			= worldChunks.computeIfAbsent(chunkKey, (Long unused) -> EnumSet.noneOf(ChunkType.class));

		loadedState.remove(ChunkType.BLOCK_CHUNK);
		if (loadedState.size() == ChunkType.values().length - 1) {
			mLoaded--;
			MMLog.finest(() -> "[CHUNK] Loaded " + mLoaded + "/" + worldChunks.size() + "/" + mLoadedChunks.size() + "chunk(s) block partial unload");
			ChunkPartialUnloadEvent partialEvent = new ChunkPartialUnloadEvent(chunk, event.isSaveChunk(), Arrays.stream(chunk.getEntities()).toList());
			Bukkit.getPluginManager().callEvent(partialEvent);
			if (partialEvent.isNeedsSave()) {
				worldNeedsSave.add(chunkKey);
			}
		}
		loadedState.remove(ChunkType.BLOCK_CHUNK_LOAD_DELAY);
		if (worldNeedsSave.contains(chunkKey)) {
			event.setSaveChunk(true);
		}
		if (loadedState.isEmpty()) {
			worldChunks.remove(chunkKey);
			if (worldChunks.isEmpty()) {
				mLoadedChunks.remove(worldId);
			}
			worldNeedsSave.remove(chunkKey);
			if (worldNeedsSave.isEmpty()) {
				mNeedsSave.remove(worldId);
			}
			MMLog.finest(() -> "[CHUNK] Loaded " + mLoaded + "/" + worldChunks.size() + "/" + mLoadedChunks.size() + "chunk(s) block full unload");
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityUnloadEvent(EntitiesUnloadEvent event) {
		UUID worldId = event.getWorld().getUID();
		Map<Long, EnumSet<ChunkType>> worldChunks
			= mLoadedChunks.computeIfAbsent(worldId, (UUID unused1) -> new HashMap<>());
		Set<Long> worldNeedsSave
			= mNeedsSave.computeIfAbsent(worldId, (UUID unused1) -> new HashSet<>());

		Chunk chunk = event.getChunk();
		long chunkKey = chunk.getChunkKey();
		EnumSet<ChunkType> loadedState
			= worldChunks.computeIfAbsent(chunkKey, (Long unused) -> EnumSet.noneOf(ChunkType.class));

		loadedState.remove(ChunkType.ENTITY_CHUNK);
		if (loadedState.size() == ChunkType.values().length - 1) {
			mLoaded--;
			MMLog.finest(() -> "[CHUNK] Loaded " + mLoaded + "/" + worldChunks.size() + "/" + mLoadedChunks.size() + "chunk(s); entity partial unload");
			ChunkPartialUnloadEvent partialEvent = new ChunkPartialUnloadEvent(chunk, true, event.getEntities());
			Bukkit.getPluginManager().callEvent(partialEvent);
			if (partialEvent.isNeedsSave()) {
				worldNeedsSave.add(chunkKey);
			}
		}
		loadedState.remove(ChunkType.ENTITY_CHUNK_LOAD_DELAY);
		if (loadedState.isEmpty()) {
			worldChunks.remove(chunkKey);
			if (worldChunks.isEmpty()) {
				mLoadedChunks.remove(worldId);
			}
			worldNeedsSave.remove(chunkKey);
			if (worldNeedsSave.isEmpty()) {
				mNeedsSave.remove(worldId);
			}
			MMLog.finest(() -> "[CHUNK] Loaded " + mLoaded + "/" + worldChunks.size() + "/" + mLoadedChunks.size() + "chunk(s); entity full unload");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		World world = event.getWorld();
		UUID worldId = world.getUID();
		Map<Long, EnumSet<ChunkType>> worldChunks
			= mLoadedChunks.computeIfAbsent(worldId, (UUID unused1) -> new HashMap<>());
		mLoadedChunks.remove(worldId);
		mNeedsSave.remove(worldId);

		for (Chunk chunk : world.getLoadedChunks()) {
			ChunkPartialUnloadEvent partialEvent
				= new ChunkPartialUnloadEvent(chunk, true, true);
			Bukkit.getPluginManager().callEvent(partialEvent);
			// Chunks get saved on unload by default
			mLoaded--;
			MMLog.finest(() -> "[CHUNK] Loaded " + mLoaded + "/" + worldChunks.size() + "/" + mLoadedChunks.size() + "chunk(s); world unload");
		}
	}

	public static boolean isChunkLoaded(Chunk chunk) {
		UUID worldId = chunk.getWorld().getUID();
		@Nullable Map<Long, EnumSet<ChunkType>> worldChunks = mLoadedChunks.get(worldId);
		if (worldChunks == null) {
			return false;
		}
		@Nullable EnumSet<ChunkType> loadedState = worldChunks.get(chunk.getChunkKey());
		return loadedState == null || loadedState.size() == ChunkType.values().length;
	}
}
