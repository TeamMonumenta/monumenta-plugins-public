package com.playmonumenta.plugins.tracking;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public class BoatTracking implements EntityTracking {
	private Set<Boat> mEntities = new HashSet<Boat>();
	private int mTicks = 0;

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Boat)entity);
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
					TreeSpecies woodType = boat.getWoodType();
					World world = boat.getWorld();
					switch (woodType) {
					case ACACIA:
						world.dropItem(boat.getLocation(), new ItemStack(Material.ACACIA_BOAT));
						break;
					case BIRCH:
						world.dropItem(boat.getLocation(), new ItemStack(Material.BIRCH_BOAT));
						break;
					case DARK_OAK:
						world.dropItem(boat.getLocation(), new ItemStack(Material.DARK_OAK_BOAT));
						break;
					case GENERIC:
						world.dropItem(boat.getLocation(), new ItemStack(Material.OAK_BOAT));
						break;
					case JUNGLE:
						world.dropItem(boat.getLocation(), new ItemStack(Material.JUNGLE_BOAT));
						break;
					case REDWOOD:
					default:
						world.dropItem(boat.getLocation(), new ItemStack(Material.SPRUCE_BOAT));
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
