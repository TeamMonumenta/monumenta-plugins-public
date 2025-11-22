package com.playmonumenta.plugins.minigames;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.MinigameManager;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public abstract class Minigame implements Listener {
	public record Arguments(Map<String, Double> map) {
		public static Arguments of(Map<String, Double> map) {
			return new Arguments(map);
		}

		public double get(String key, double defaultValue) {
			@Nullable
			Double v = map.get(key);
			if (v == null) {
				return defaultValue;
			}
			return v;
		}

		public double get(String key, Supplier<Double> defaultGetter) {
			@Nullable
			Double v = map.get(key);
			if (v == null) {
				return defaultGetter.get();
			}
			return v;
		}

		public int getInt(String key, int defaultValue) {
			@Nullable
			Double v = map.get(key);
			if (v == null) {
				return defaultValue;
			}
			return v.intValue();
		}

		public int getInt(String key, Supplier<Integer> defaultGetter) {
			@Nullable
			Double v = map.get(key);
			if (v == null) {
				return defaultGetter.get();
			}
			return v.intValue();
		}
	}

	private static final Plugin PLUGIN = Plugin.getInstance();

	private final String mIdentifier;
	protected final World mWorld;
	protected Hitbox mHitbox;
	private final BukkitRunnable mHeartBeat = new BukkitRunnable() {
		long mTicks = 0;

		@Override
		public void run() {
			tick(mTicks++);
		}
	};

	public Minigame(String identifier, Location center, double size) {
		this(identifier, new Hitbox.AABBHitbox(center.getWorld(),
			BoundingBox.of(center.clone().subtract(size, size, size), center.clone().add(size, size, size))));
	}

	public Minigame(String identifier, Location loc1, Location loc2) {
		this(identifier, new Hitbox.AABBHitbox(loc1.getWorld(), BoundingBox.of(loc1, loc2)));
		// :c
		Preconditions.checkArgument(loc1.getWorld() == loc2.getWorld(), "Locations must be in the same world!");
	}

	public Minigame(String identifier, Hitbox hitbox) {
		mIdentifier = identifier;
		mWorld = hitbox.getWorld();
		mHitbox = hitbox;
		mHeartBeat.runTaskTimer(PLUGIN, 0, 1);
	}

	public void stopMinigame() {
		minigameEnd();
		MinigameManager.getInstance().stopMinigame(mIdentifier);
	}

	public void minigameEnd() {
		onEndMinigame();
		mHeartBeat.cancel();
		MinigameManager.getInstance().getActiveMinigames().remove(getId());
	}

	public abstract void startMinigame(@Nullable Player player);

	abstract void tick(long tick);

	public abstract void onEndMinigame();

	// Util for checking if a location is within range, example for death events
	public final boolean isWithinRange(Location location) {
		return location.getWorld() == mWorld && mHitbox.contains(location.toVector());
	}

	public final boolean isWithinRange(Entity entity) {
		return entity.getWorld() == mWorld && mHitbox.intersects(entity.getBoundingBox());
	}

	public String getId() {
		return mIdentifier;
	}

	public void onEntityDeath(EntityDeathEvent event) {

	}

	public void onPlayerDeath(PlayerDeathEvent event) {
		if (mHitbox.contains(event.getEntity().getLocation())) {
			stopMinigame();
		}
	}
}
