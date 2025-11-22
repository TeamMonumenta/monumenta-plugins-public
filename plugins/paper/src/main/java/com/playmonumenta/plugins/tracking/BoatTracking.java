package com.playmonumenta.plugins.tracking;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;


public class BoatTracking implements EntityTracking {
	private final Set<Boat> mEntities = Collections.newSetFromMap(new WeakHashMap<>());
	private int mTicks = 0;

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Boat) entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(int ticks) {
		Iterator<Boat> boatIter = mEntities.iterator();
		while (boatIter.hasNext()) {
			Boat boat = boatIter.next();
			if (boat != null && boat.isValid() && boat.getLocation().isChunkLoaded()) {
				if (!LocationUtils.isValidBoatLocation(boat.getLocation())) {
					Material woodType = boat.getBoatMaterial();
					World world = boat.getWorld();
					switch (woodType) {
						case ACACIA_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.ACACIA_BOAT));
							break;
						case ACACIA_CHEST_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.ACACIA_CHEST_BOAT));
							break;
						case BIRCH_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.BIRCH_BOAT));
							break;
						case BIRCH_CHEST_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.BIRCH_CHEST_BOAT));
							break;
						case DARK_OAK_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.DARK_OAK_BOAT));
							break;
						case DARK_OAK_CHEST_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.DARK_OAK_CHEST_BOAT));
							break;
						case OAK_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.OAK_BOAT));
							break;
						case OAK_CHEST_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.OAK_CHEST_BOAT));
							break;
						case JUNGLE_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.JUNGLE_BOAT));
							break;
						case JUNGLE_CHEST_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.JUNGLE_CHEST_BOAT));
							break;
						case SPRUCE_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.SPRUCE_BOAT));
							break;
						case SPRUCE_CHEST_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.SPRUCE_CHEST_BOAT));
							break;
						case CHERRY_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.CHERRY_BOAT));
							break;
						case CHERRY_CHEST_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.CHERRY_CHEST_BOAT));
							break;
						case MANGROVE_CHEST_BOAT:
							world.dropItem(boat.getLocation(), new ItemStack(Material.MANGROVE_CHEST_BOAT));
							break;
						case MANGROVE_BOAT:
						default:
							world.dropItem(boat.getLocation(), new ItemStack(Material.MANGROVE_BOAT));
							break;
					}

					boatIter.remove();
					boat.remove();
				} else {
					// Very infrequently check if the boat is still actually there
					mTicks++;
					if (mTicks > 306) {
						mTicks = 0;
						if (!EntityUtils.isStillLoaded(boat)) {
							boatIter.remove();
						}
					}
				}
			} else {
				boatIter.remove();
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		mEntities.clear();
	}
}
