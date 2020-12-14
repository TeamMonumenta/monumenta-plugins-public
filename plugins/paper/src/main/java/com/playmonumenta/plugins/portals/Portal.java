package com.playmonumenta.plugins.portals;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Portal {

	Location mLocation1;
	Location mLocation2;

	Location mBlock1;
	Location mBlock2;

	BlockFace mFacing;

	Entity mItemFrame1;
	Entity mItemFrame2;

	//The portal it links to
	Portal mPair;
	//The owner of the portal
	Player mOwner;

	public Portal(Location loc1, Location loc2, BlockFace face, Location b1, Location b2) {
		mLocation1 = loc1;
		mLocation2 = loc2;
		mFacing = face;
		mBlock1 = b1;
		mBlock2 = b2;
	}

	public Vector getShift() {
		if (mFacing == BlockFace.UP) {
			return new Vector(0, 1, 0);
		} else if (mFacing == BlockFace.DOWN) {
			return new Vector(0, -1, 0);
		} else if (mFacing == BlockFace.WEST) {
			return new Vector(-1, 0, 0);
		} else if (mFacing == BlockFace.EAST) {
			return new Vector(1, 0, 0);
		} else if (mFacing == BlockFace.NORTH) {
			return new Vector(0, 0, -1);
		} else if (mFacing == BlockFace.SOUTH) {
			return new Vector(0, 0, 1);
		}
		return null;
	}

	public Vector getOffset(int loc, BlockFace from) {

		if (mFacing == BlockFace.DOWN) {
			return new Vector(0, -2, 0);
		} else if (mFacing == BlockFace.UP) {
			return new Vector(0, 1, 0);
			//Ignore which side of the portal was entered if the entry was facing up or down
		} else if (from == BlockFace.UP || from == BlockFace.DOWN) {
			//For sideways portals, loc 1 will be the lower block and loc 2 will be the upper block. We want players to come out in the middle
			//This provides more consistency in the puzzles -> less frustration
			if (loc == 1) {
				return new Vector(0, .5, 0);
			}
			if (loc == 2) {
				return new Vector(0, -0.5, 0);
			}

		}

		return new Vector(0, 0, 0);
	}

	public Location getPortalMidpoint() {
		Location l = new Location(mLocation1.getWorld(), (mLocation1.getX() + mLocation2.getX()) / 2, (mLocation1.getY() + mLocation2.getY()) / 2, (mLocation1.getZ() + mLocation2.getZ()) / 2);
		l.setYaw(mFacing.getDirection().toLocation(mLocation1.getWorld()).getYaw());
		l.setPitch(mFacing.getDirection().toLocation(mLocation1.getWorld()).getPitch());
		return l;
	}

	public Location getYaw(Location loc, Entity p) {
		Location l = loc.clone();
		if (mFacing == BlockFace.SOUTH) {
			l.setYaw(0);
		} else if (mFacing == BlockFace.NORTH) {
			l.setYaw(180);
		} else if (mFacing == BlockFace.WEST) {
			l.setYaw(90);
		} else if (mFacing == BlockFace.EAST) {
			l.setYaw(-90);
		} else {
			l.setYaw(p.getLocation().getYaw());

		}
		l.setPitch(p.getLocation().getPitch());
		return l;
	}

}