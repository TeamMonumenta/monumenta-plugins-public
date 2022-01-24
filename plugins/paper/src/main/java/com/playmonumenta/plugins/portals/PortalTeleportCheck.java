package com.playmonumenta.plugins.portals;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Uncomment to debug
//import com.playmonumenta.plugins.utils.ParticleUtils;

public class PortalTeleportCheck extends BukkitRunnable {
	private static final Set<EntityType> IMMOVABLE_ENTITY_TYPES = Set.of(
		EntityType.AREA_EFFECT_CLOUD,
		EntityType.BOAT,
		EntityType.DONKEY,
		EntityType.ENDER_CRYSTAL,
		EntityType.EVOKER_FANGS,
		EntityType.FISHING_HOOK,
		EntityType.HORSE,
		EntityType.ITEM_FRAME,
		EntityType.LEASH_HITCH,
		EntityType.LIGHTNING,
		EntityType.LLAMA,
		EntityType.MINECART,
		EntityType.MINECART_CHEST,
		EntityType.MINECART_COMMAND,
		EntityType.MINECART_FURNACE,
		EntityType.MINECART_HOPPER,
		EntityType.MINECART_MOB_SPAWNER,
		EntityType.MINECART_TNT,
		EntityType.MULE,
		EntityType.PAINTING,
		EntityType.PIG,
		EntityType.SHULKER,
		EntityType.SKELETON_HORSE,
		EntityType.STRIDER,
		EntityType.TRADER_LLAMA,
		EntityType.VILLAGER,
		EntityType.WANDERING_TRADER,
		EntityType.ZOMBIE_HORSE
	);

	private static final Map<UUID, Integer> mCooldowns = new HashMap<>();

	private List<Map<UUID, Vector>> mPastPlayerLocs = new ArrayList<>();

	private final Player mPlayer;

	public PortalTeleportCheck(Player player) {
		this.mPlayer = player;
	}

	@Override
	public void run() {
		@Nullable Portal p1 = PortalManager.mPlayerPortal1.get(mPlayer);
		@Nullable Portal p2 = PortalManager.mPlayerPortal2.get(mPlayer);
		if (p1 == null || p2 == null) {
			return;
		}
		World w1 = p1.getWorld();
		World w2 = p2.getWorld();
		BoundingBox b1 = p1.getBoundingBox();
		BoundingBox b2 = p2.getBoundingBox();

		// Show bounding boxes for debugging
		//ParticleUtils.tickBoundingBoxEdge(w1, b1, Color.fromRGB(127, 127, 255), 50);
		//ParticleUtils.tickBoundingBoxEdge(w2, b2, Color.fromRGB(255, 127, 0), 50);

		Map<UUID, Vector> currentLocs = new HashMap<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			UUID playerUuid = player.getUniqueId();
			currentLocs.put(playerUuid, player.getLocation().toVector().clone());
		}
		mPastPlayerLocs.add(0, currentLocs);
		if (mPastPlayerLocs.size() > 3) {
			mPastPlayerLocs.remove(3);
		}

		Iterator<Map.Entry<UUID, Integer>> cooldownIt = mCooldowns.entrySet().iterator();
		while (cooldownIt.hasNext()) {
			Map.Entry<UUID, Integer> entry = cooldownIt.next();
			UUID entityUuid = entry.getKey();
			Entity entity = Bukkit.getEntity(entityUuid);
			if (entity == null) {
				// Stop tracking dead/unloaded entities
				cooldownIt.remove();
				continue;
			}
			BoundingBox be = entity.getBoundingBox();
			if (!(b1.overlaps(be) || b2.overlaps(be))) {
				// Clear cooldown immediately if not in either portal
				cooldownIt.remove();
				continue;
			}

			int cooldown = entry.getValue();
			cooldown--;

			if (cooldown > 1) {
				entry.setValue(cooldown);
			} else {
				cooldownIt.remove();
			}
		}

		for (Entity entity : w1.getNearbyEntities(b1)) {
			BoundingBox be = entity.getBoundingBox();
			if (b1.overlaps(be)) {
				attemptTeleport(entity, p1, p2);
			}
		}
		for (Entity entity : w2.getNearbyEntities(b2)) {
			BoundingBox be = entity.getBoundingBox();
			if (b2.overlaps(be)) {
				attemptTeleport(entity, p2, p1);
			}
		}
	}

	public void attemptTeleport(Entity entity, Portal from, Portal to) {
		if (entity.isDead()) {
			return;
		}
		if (IMMOVABLE_ENTITY_TYPES.contains(entity.getType())) {
			return;
		}
		UUID entityUuid = entity.getUniqueId();
		if (entity instanceof Player && !entityUuid.equals(mPlayer.getUniqueId())) {
			return;
		}
		if (mCooldowns.containsKey(entityUuid)) {
			return;
		}
		// Ensure the player has a valid velocity to modify
		if (entity instanceof Player) {
			from.travel(entity, getRecentVelocity((Player) entity));
		} else {
			from.travel(entity);
		}
		switch (to.mFacing) {
		case UP:
			mCooldowns.put(entityUuid, 8);
			break;
		case DOWN:
			mCooldowns.put(entityUuid, 12);
			break;
		default:
			mCooldowns.put(entityUuid, 25);
		}
	}

	// Workaround for calculating near-instant velocity
	public Vector getRecentVelocity(Player player) {
		UUID playerUuid = player.getUniqueId();

		@Nullable Vector locNow = null;
		if (mPastPlayerLocs.size() > 0) {
			locNow = mPastPlayerLocs.get(0).get(playerUuid);
		}
		@Nullable Vector locPrevious1 = null;
		if (mPastPlayerLocs.size() > 1) {
			locPrevious1 = mPastPlayerLocs.get(1).get(playerUuid);
		}
		@Nullable Vector locPrevious2 = null;
		if (mPastPlayerLocs.size() > 2) {
			locPrevious2 = mPastPlayerLocs.get(2).get(playerUuid);
		}

		@Nullable Vector velocity1 = null;
		if (locNow != null && locPrevious1 != null) {
			return locNow.clone().subtract(locPrevious1);
		}
		@Nullable Vector velocity2 = null;
		if (locPrevious1 != null && locPrevious2 != null) {
			return locPrevious1.clone().subtract(locPrevious2);
		}

		if (velocity1 == null) {
			return player.getVelocity();
		} else if (velocity2 == null) {
			return velocity1;
		} else if (velocity2.lengthSquared() > velocity1.lengthSquared()) {
			return velocity1.normalize().multiply(velocity2.length());
		} else {
			return velocity1;
		}
	}
}
