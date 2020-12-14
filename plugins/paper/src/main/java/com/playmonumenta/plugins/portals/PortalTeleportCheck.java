package com.playmonumenta.plugins.portals;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class PortalTeleportCheck extends BukkitRunnable {

	Player mPlayer;
	int mCooldown = 0;
	Vector mVelocity;
	Location mPastPosition1;
	Location mPastPosition2;
	double mMaxY = 0;
	int mMaxYcooldown = 0;
	//Whether the player most recently went through portal 1 or portal 2
	int mLastPortal;

	@Override
	public void run() {

		if (mMaxYcooldown == 0) {
			mMaxY = 0;
		}

		Portal mP1 = PortalManager.mPlayerPortal1.get(mPlayer);
		Portal mP2 = PortalManager.mPlayerPortal2.get(mPlayer);

		World mWorld = mPlayer.getWorld();

		//Velocity loading - avoid getting stuck on tp by using velocity 2 ticks before for calculations
		mVelocity = mPlayer.getVelocity().clone();

		if (mPastPosition1 != null) {
			mPastPosition2 = mPastPosition1;
		}

		mPastPosition1 = mPlayer.getLocation().clone();

		//Detect collision between player and their portals
		//There are four possibilities (2 portals of 2 blocks each)

		BoundingBox b1 = mWorld.getBlockAt(mP1.mBlock1).getBoundingBox().shift(mP1.getShift()).expand(.1);
		BoundingBox b2 = mWorld.getBlockAt(mP1.mBlock2).getBoundingBox().shift(mP1.getShift()).expand(.1);
		BoundingBox b3 = mWorld.getBlockAt(mP2.mBlock1).getBoundingBox().shift(mP2.getShift()).expand(.1);
		BoundingBox b4 = mWorld.getBlockAt(mP2.mBlock2).getBoundingBox().shift(mP2.getShift()).expand(.1);
		BoundingBox pb = mPlayer.getBoundingBox();

		if (pb.overlaps(b1) && (mCooldown == 0 || mLastPortal == 1)) {
			mPlayer.teleport(mP2.getYaw(mP2.mLocation1.toCenterLocation().add(mP2.getOffset(1, mP1.mFacing)), mPlayer));

			//Allow for smooth up-up portals while not trapping player in wall portals
			if (mP2.mFacing == BlockFace.UP) {
				mCooldown = 12;
			} else {
				mCooldown = 25;
			}

			mLastPortal = 1;
			calculateMomentum(mP1.mFacing, mP2.mFacing, mPlayer);
		} else if (pb.overlaps(b2) && (mCooldown == 0 || mLastPortal == 1)) {
			mPlayer.teleport(mP2.getYaw(mP2.mLocation2.toCenterLocation().add(mP2.getOffset(2, mP1.mFacing)), mPlayer));
			if (mP2.mFacing == BlockFace.UP) {
				mCooldown = 12;
			} else {
				mCooldown = 25;
			}
			mLastPortal = 1;
			calculateMomentum(mP1.mFacing, mP2.mFacing, mPlayer);
		} else if (pb.overlaps(b3) && (mCooldown == 0 || mLastPortal == 2)) {
			mPlayer.teleport(mP1.getYaw(mP1.mLocation1.toCenterLocation().add(mP1.getOffset(1, mP2.mFacing)), mPlayer));

			if (mP1.mFacing == BlockFace.UP) {
				mCooldown = 12;
			} else {
				mCooldown = 25;
			}
			mLastPortal = 2;
			calculateMomentum(mP2.mFacing, mP1.mFacing, mPlayer);
		} else if (pb.overlaps(b4) && (mCooldown == 0 || mLastPortal == 2)) {
			mPlayer.teleport(mP1.getYaw(mP1.mLocation2.toCenterLocation().add(mP1.getOffset(2, mP2.mFacing)), mPlayer));
			if (mP1.mFacing == BlockFace.UP) {
				mCooldown = 12;
			} else {
				mCooldown = 25;
			}
			mLastPortal = 2;
			calculateMomentum(mP2.mFacing, mP1.mFacing, mPlayer);
		}

		if (mCooldown > 0) {
			mCooldown -= 1;
		}

		if (mMaxYcooldown > 0) {
			mMaxY -= 1;
		}
	}

	//Set the player velocity to what it should be after entering portal

	public void calculateMomentum(BlockFace b1, BlockFace b2, Player p) {

		if (mPastPosition2 == null) {
			return;
		}

		Vector v = getRecentVelocity(mPastPosition2, mPastPosition1);

		if (b1 == BlockFace.UP) {

			//If you go from up portal to side portal, set tertiary axis to zero for more intuitive travel

			if (b2 == BlockFace.UP) {
				p.setVelocity(new Vector(v.getX(), v.getY() * -1, v.getZ()));
			} else if (b2 == BlockFace.DOWN) {
				p.setVelocity(v.clone());
			} else if (b2 == BlockFace.WEST) {
				p.setVelocity(new Vector(v.getY(), v.getX(), 0.0));
			} else if (b2 == BlockFace.EAST) {
				p.setVelocity(new Vector(v.getY() * -1, v.getX() * -1, 0.0));
			} else if (b2 == BlockFace.NORTH) {
				p.setVelocity(new Vector(0.0, v.getZ(), v.getY()));
			} else if (b2 == BlockFace.SOUTH) {
				p.setVelocity(new Vector(0.0, v.getZ() * -1, v.getY() * -1));
			}
		} else if (b1 == BlockFace.DOWN) {
			if (b2 == BlockFace.DOWN) {
				p.setVelocity(new Vector(v.getX(), v.getY() * -1, v.getZ()));
			} else if (b2 == BlockFace.UP) {
				p.setVelocity(v.clone());
			} else if (b2 == BlockFace.EAST) {
				p.setVelocity(new Vector(v.getY(), v.getX(), v.getZ()));
			} else if (b2 == BlockFace.WEST) {
				p.setVelocity(new Vector(v.getY() * -1, v.getX() * -1, v.getZ()));
			} else if (b2 == BlockFace.SOUTH) {
				p.setVelocity(new Vector(v.getX(), v.getZ(), v.getY()));
			} else if (b2 == BlockFace.NORTH) {
				p.setVelocity(new Vector(v.getX(), v.getZ() * -1, v.getY() * -1));
			}
		} else if (b1 == BlockFace.EAST) {
			if (b2 == BlockFace.EAST) {
				p.setVelocity(new Vector(v.getX() * -1, v.getY(), v.getZ()));
			} else if (b2 == BlockFace.WEST) {
				p.setVelocity(v.clone());
			} else if (b2 == BlockFace.DOWN) {
				p.setVelocity(new Vector(v.getY(), v.getX(), v.getZ()));
			} else if (b2 == BlockFace.UP) {
				p.setVelocity(new Vector(v.getY() * -1, v.getX() * -1, v.getZ()));
			} else if (b2 == BlockFace.NORTH) {
				p.setVelocity(new Vector(v.getZ(), v.getY(), v.getX()));
			} else if (b2 == BlockFace.SOUTH) {
				p.setVelocity(new Vector(v.getZ() * -1, v.getY(), v.getX() * -1));
			}
		} else if (b1 == BlockFace.WEST) {
			if (b2 == BlockFace.WEST) {
				p.setVelocity(new Vector(v.getX() * -1, v.getY(), v.getZ()));
			} else if (b2 == BlockFace.EAST) {
				p.setVelocity(v.clone());
			} else if (b2 == BlockFace.UP) {
				p.setVelocity(new Vector(v.getY(), v.getX(), v.getZ()));
			} else if (b2 == BlockFace.DOWN) {
				p.setVelocity(new Vector(v.getY() * -1, v.getX() * -1, v.getZ()));
			} else if (b2 == BlockFace.SOUTH) {
				p.setVelocity(new Vector(v.getZ(), v.getY(), v.getX()));
			} else if (b2 == BlockFace.NORTH) {
				p.setVelocity(new Vector(v.getZ() * -1, v.getY(), v.getX() * -1));
			}
		} else if (b1 == BlockFace.NORTH) {
			if (b2 == BlockFace.NORTH) {
				p.setVelocity(new Vector(v.getX(), v.getY(), v.getZ() * -1));
			} else if (b2 == BlockFace.SOUTH) {
				p.setVelocity(v.clone());
			} else if (b2 == BlockFace.DOWN) {
				p.setVelocity(new Vector(v.getX(), v.getZ() * -1, v.getY() * -1));
			} else if (b2 == BlockFace.UP) {
				p.setVelocity(new Vector(v.getX(), v.getZ(), v.getY()));
			} else if (b2 == BlockFace.EAST) {
				p.setVelocity(new Vector(v.getZ(), v.getY(), v.getX()));
			} else if (b2 == BlockFace.WEST) {
				p.setVelocity(new Vector(v.getZ() * -1, v.getY(), v.getX() * -1));
			}
		} else if (b1 == BlockFace.SOUTH) {
			if (b2 == BlockFace.SOUTH) {
				p.setVelocity(new Vector(v.getX(), v.getY(), v.getZ() * -1));
			} else if (b2 == BlockFace.NORTH) {
				p.setVelocity(v.clone());
			} else if (b2 == BlockFace.UP) {
				p.setVelocity(new Vector(v.getX(), v.getZ() * -1, v.getY() * -1));
			} else if (b2 == BlockFace.DOWN) {
				p.setVelocity(new Vector(v.getX(), v.getZ(), v.getY()));
			} else if (b2 == BlockFace.WEST) {
				p.setVelocity(new Vector(v.getZ(), v.getY(), v.getX()));
			} else if (b2 == BlockFace.EAST) {
				p.setVelocity(new Vector(v.getZ() * -1, v.getY(), v.getX() * -1));
			}
		}

		if (b2 == BlockFace.DOWN) {
			mCooldown = 5;
			//Extra short cooldown for people that want to do infinite loops if they are spit out facing up
		}

		if (v.getY() > mMaxY) {
			mMaxY = v.getY();
			mMaxYcooldown = 10;
		}
	}

	//Workaround for calculating near-instant velocity
	public Vector getRecentVelocity(Location x, Location y) {
		Vector difference = new Vector();
		difference.setX(y.getX() - x.getX());
		difference.setY(y.getY() - x.getY());
		difference.setZ(y.getZ() - x.getZ());
		return difference;
	}

}
