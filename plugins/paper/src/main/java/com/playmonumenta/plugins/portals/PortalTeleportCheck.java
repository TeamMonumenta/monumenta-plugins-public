package com.playmonumenta.plugins.portals;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

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
		EntityType.GLOW_ITEM_FRAME,
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

	private final Map<UUID, Integer> mCooldowns = new HashMap<>();

	private final List<Map<UUID, Vector>> mPastPlayerLocs = new ArrayList<>();

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

		// slightly expanded bounding boxes to better catch entering from the sides
		BoundingBox b1 = p1.getBoundingBox().expand(0.1);
		BoundingBox b2 = p2.getBoundingBox().expand(0.1);

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
		// Don't let players go through the portal if it's not theirs, unless they're in r3
		if (entity instanceof Player && !entityUuid.equals(mPlayer.getUniqueId()) && !ServerProperties.getAbilityEnhancementsEnabled(mPlayer)) {
			return;
		}
		if (mCooldowns.containsKey(entityUuid)) {
			return;
		}
		if (entity instanceof Player player) {
			if (!portalEnterCheck(player, from)) {
				return;
			}
			from.travel(entity, getRecentVelocity(player));
		} else {
			from.travel(entity);
		}
		switch (to.mFacing) {
			case UP:
				if (ServerProperties.getAbilityEnhancementsEnabled(mPlayer)) {
					mCooldowns.put(entityUuid, 25);
					break;
				}
				mCooldowns.put(entityUuid, 8);
				break;
			case DOWN:
				if (ServerProperties.getAbilityEnhancementsEnabled(mPlayer)) {
					mCooldowns.put(entityUuid, 25);
					break;
				}
				mCooldowns.put(entityUuid, 12);
				break;
			default:
				mCooldowns.put(entityUuid, entity instanceof Player ? 10 : 25);
		}
	}

	/**
	 * Checks if the player should be teleported by the given portal.
	 */
	private boolean portalEnterCheck(Player player, Portal portal) {
		// Check for intersection with the real bounding box first - we don't want to be able to enter portals from behind
		if (!player.getBoundingBox().overlaps(portal.getBoundingBox())) {
			return false;
		}
		// Ensure the player has a valid velocity to modify
		Vector recentVelocity = getRecentVelocity(player);
		if (portal.mFacing == BlockFace.DOWN) {
			// Ceiling portals must be entered going upwards, not level or downwards
			return recentVelocity.getY() > 0.01;
		} else if (portal.mFacing == BlockFace.UP) {
			// Floor portals must be entered going downwards or level to the ground, not upwards
			return recentVelocity.getY() < 0.01;
		} else {
			// Wall portals must be entered by going in their direction
			// To test this, check if the player would hit the portal if they kept going in the current direction
			// Also test for movement speed to teleport later if moving more slowly (or equivalently, teleport earlier when fast)

			Vector playerCenter = LocationUtils.getHalfHeightLocation(player).toVector();
			Vector portalCenter = portal.getBoundingBox().getCenter().add(portal.mFacing.getDirection().multiply(-0.5));
			double distanceToPortal = Math.abs(portalCenter.clone().subtract(playerCenter).dot(portal.mFacing.getDirection())) - player.getBoundingBox().getWidthX() / 2;

			if (Math.abs(distanceToPortal) < 0.01) {
				// Always teleport if touching the portal (e.g. moving while against the wall)
				return true;
			}
			double speed = recentVelocity.length();
			if (speed * 4 < distanceToPortal) {
				// Moving too slowly towards the portal (or not moving at all) - don't teleport (yet), will probably teleport later unless the player stops moving or changes direction
				return false;
			}
			Vector portalPlaneIntersection = VectorUtils.rayPlaneIntersection(playerCenter, recentVelocity.clone().normalize(), portalCenter, portal.mFacing.getDirection());
			if (portalPlaneIntersection == null) {
				// No intersection, i.e. moving away from or parallel to the portal (and we checked the special case of touching it already)
				return false;
			}
			double dy = (portal.getBoundingBox().getHeight() + player.getBoundingBox().getHeight()) / 2;
			double dxz = (portal.getBoundingBox().getWidthX() + player.getBoundingBox().getWidthX()) / 2;
			Vector delta = portalCenter.clone().subtract(portalPlaneIntersection);
			if (Math.abs(delta.getY()) > dy || Math.abs(delta.getX() + delta.getZ()) > dxz) { // (NB: either x or z is 0, so can add them)
				// Player will miss the portal if they keep their current direction
				return false;
			}
			return true;
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

		if (locNow != null && locPrevious1 != null) {
			return locNow.clone().subtract(locPrevious1);
		}
		if (locPrevious1 != null && locPrevious2 != null) {
			return locPrevious1.clone().subtract(locPrevious2);
		}
		return player.getVelocity();
	}
}
