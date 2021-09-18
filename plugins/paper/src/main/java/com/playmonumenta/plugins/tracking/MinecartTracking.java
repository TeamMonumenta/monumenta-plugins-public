package com.playmonumenta.plugins.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

public class MinecartTracking implements EntityTracking {
	private Set<Minecart> mEntities = new HashSet<Minecart>();
	private int mTicks = 0;

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Minecart)entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(int ticks) {
		Iterator<Minecart> minecartIter = mEntities.iterator();
		while (minecartIter.hasNext()) {
			Minecart minecart = minecartIter.next();
			if (minecart != null && minecart.isValid() && minecart.getLocation().isChunkLoaded()) {
				if (!LocationUtils.isValidMinecartLocation(minecart.getLocation())) {
					if (minecart instanceof ExplosiveMinecart) {
						minecart.getWorld().dropItem(minecart.getLocation(), new ItemStack(Material.TNT_MINECART));
					} else if (minecart instanceof HopperMinecart) {
						minecart.getWorld().dropItem(minecart.getLocation(), new ItemStack(Material.HOPPER_MINECART));
					} else if (minecart instanceof StorageMinecart) {
						minecart.getWorld().dropItem(minecart.getLocation(), new ItemStack(Material.CHEST_MINECART));
					} else if (minecart instanceof PoweredMinecart) {
						minecart.getWorld().dropItem(minecart.getLocation(), new ItemStack(Material.FURNACE_MINECART));
					} else {
						minecart.getWorld().dropItem(minecart.getLocation(), new ItemStack(Material.MINECART));
					}
					minecartIter.remove();
					minecart.remove();
				} else {
					// Very infrequently check if the minecart is still actually there
					mTicks++;
					if (mTicks > 306) {
						mTicks = 0;
						if (!EntityUtils.isStillLoaded(minecart)) {
							minecartIter.remove();
						}
					}
				}
			} else {
				minecartIter.remove();
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		mEntities.clear();
	}
}
