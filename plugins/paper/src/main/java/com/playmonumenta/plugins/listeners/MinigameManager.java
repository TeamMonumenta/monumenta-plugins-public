package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.minigames.Minigame;
import com.playmonumenta.plugins.minigames.SpiritArcheryMinigame;
import com.playmonumenta.plugins.minigames.TestMinigame;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public class MinigameManager implements Listener {
	@MonotonicNonNull
	private static MinigameManager INSTANCE = null;

	public static MinigameManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new MinigameManager();
		}
		return INSTANCE;
	}

	@FunctionalInterface
	public interface MinigameConstructor {
		Minigame apply(Location location, Minigame.Arguments arguments);
	}

	public MinigameManager() {
		registerAll();
	}

	public void registerAll() {
		registerMinigame(TestMinigame.ID, TestMinigame::new);
		registerMinigame(SpiritArcheryMinigame.ID, SpiritArcheryMinigame::new);
	}

	public void registerMinigame(String id, MinigameConstructor minigameSupplier) {
		mMinigamesRegistered.put(id, minigameSupplier);
	}

	private final Map<String, MinigameConstructor> mMinigamesRegistered = new HashMap<>();
	private final Map<String, Minigame> mActiveMinigames = new HashMap<>();


	public void start(String id, String customName, Location location, Player player, Minigame.Arguments arguments) {
		MinigameConstructor minigameCreator = mMinigamesRegistered.get(id);
		if (minigameCreator == null) {
			MMLog.severe(String.format("Minigame with id %s does not exist!", id));
			Thread.dumpStack();
			return;
		}
		if (player == null) {
			// Might be the wrong error to throw
			MMLog.severe(String.format("Player with id %s does not exist!", id));
			Thread.dumpStack();
			return;
		}
		Minigame minigame = minigameCreator.apply(location, arguments);
		startMinigame(customName, minigame, player);
	}

	@Nullable
	public Minigame getMinigame(String id, Location location, Minigame.Arguments arguments) {
		MinigameConstructor minigameCreator = mMinigamesRegistered.get(id);
		if (minigameCreator == null) {
			MMLog.severe(String.format("Minigame with id %s does not exist!", id));
			Thread.dumpStack();
			return null;
		}
		return minigameCreator.apply(location, arguments);
	}

	public void startMinigame(Minigame minigame) {
		// ONLY USE if minigame does not target one specific player
		startMinigame(minigame, null);
	}

	public void startMinigame(Minigame minigame, @Nullable Player player) {
		startMinigame(minigame.getId(), minigame, player);
	}

	public void startMinigame(String customId, Minigame minigame, @Nullable Player player) {
		@Nullable
		Minigame lastMinigame = getActiveMinigames().get(customId);
		if (lastMinigame != null) {
			lastMinigame.minigameEnd();
		}
		getActiveMinigames().put(customId, minigame);
		minigame.startMinigame(player);
	}

	public void stopMinigame(String id) {
		@Nullable
		Minigame removed = getActiveMinigames().remove(id);
		if (removed != null) {
			removed.minigameEnd();
		}
	}

	public boolean checkActiveMinigame(String id) {
		return getActiveMinigames().get(id) != null;
	}

	public Map<String, MinigameConstructor> getMinigamesRegistered() {
		return mMinigamesRegistered;
	}

	public Map<String, Minigame> getActiveMinigames() {
		return mActiveMinigames;
	}

	public void stopActiveMinigames() {
		getActiveMinigames().values().forEach(Minigame::minigameEnd);
		getActiveMinigames().clear();
	}

	private @NotNull Stream<Minigame> getMinigamesInRange(Location location) {
		return getActiveMinigames().values().stream()
			.filter(minigame -> minigame.isWithinRange(location));
	}

	// add events accordingly
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDeath(EntityDeathEvent event) {
		getMinigamesInRange(event.getEntity().getLocation()).forEach(minigame ->
			minigame.onEntityDeath(event)
		);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDeath(PlayerDeathEvent event) {
		getActiveMinigames().values().forEach(minigame ->
			minigame.onPlayerDeath(event)
		);
	}
}
