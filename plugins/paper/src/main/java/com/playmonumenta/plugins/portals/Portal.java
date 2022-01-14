package com.playmonumenta.plugins.portals;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Portal {
	private static final double VERTICAL_BOOST = 0.18;

	public Location mLocation1;
	public Location mLocation2;

	public Location mBlock1;
	public Location mBlock2;

	public BlockFace mFacing;
	public Vector mPortalTopDirection;
	public Vector mPortalOutDirection;

	//The portal it links to
	public @Nullable Portal mPair;
	//The owner of the portal
	public @Nullable Player mOwner;

	public Portal(Location loc1, Location loc2, BlockFace face, Location b1, Location b2) {
		mLocation1 = loc1;
		mLocation2 = loc2;
		mFacing = face;
		mBlock1 = b1;
		mBlock2 = b2;
		mPortalTopDirection = b2.toVector().subtract(b1.toVector());
		mPortalOutDirection = face.getDirection();
	}

	public @Nullable Vector getShift() {
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

	public World getWorld() {
		return mLocation1.getWorld();
	}

	public BoundingBox getBoundingBox() {
		return BoundingBox.of(mLocation1, mLocation2).expand(0.1, 0.1, 0.1, 1.1, 1.1, 1.1);
	}

	private Vector portalLeftDirection() {
		return mPortalTopDirection.getCrossProduct(mPortalOutDirection);
	}

	private Location centerLocation() {
		return mLocation1.clone().toCenterLocation().add(mPortalTopDirection.clone().multiply(0.5));
	}

	private static boolean willBeInBlock(Entity entity, Location location) {
		World world = location.getWorld();
		double entityHalfWidth = entity.getWidth() / 2.0;

		int minX = (int) Math.floor(location.getX() - entityHalfWidth);
		int minY = (int) Math.floor(location.getY());
		int minZ = (int) Math.floor(location.getZ() - entityHalfWidth);
		int maxX = (int) Math.floor(location.getX() + entityHalfWidth);
		int maxY = (int) Math.floor(location.getY() + entity.getHeight());
		int maxZ = (int) Math.floor(location.getZ() + entityHalfWidth);

		for (int x = minX; x <= maxX; ++x) {
			for (int y = minY; y <= maxY; ++y) {
				for (int z = minZ; z <= maxZ; ++z) {
					if (!world.getBlockAt(x, y, z).isPassable()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void fixInsideWall(Entity entity, Location location) {
		double entityHalfWidth = entity.getWidth() / 2.0;
		switch (mFacing) {
		case UP:
			// y+
			location.setY(Math.max(location.getY(), mLocation1.getY()));
			break;
		case DOWN:
			// y-
			location.setY(Math.min(location.getY(), mLocation1.getY() + 1.0 - entity.getHeight()));
			break;
		case SOUTH:
			// z+
			location.setZ(Math.max(location.getZ(), location.getZ() + entityHalfWidth));
			break;
		case NORTH:
			// z-
			location.setZ(Math.min(location.getZ(), location.getZ() + 1.0 - entityHalfWidth));
			break;
		case EAST:
			// x+
			location.setX(Math.max(location.getX(), location.getX() + entityHalfWidth));
			break;
		case WEST:
		default:
			// x-
			location.setX(Math.min(location.getX(), location.getX() + 1.0 - entityHalfWidth));
			break;
		}
	}

	private Location defaultTeleportLocation(Entity entity) {
		Location centerLoc = centerLocation();
		double entityHalfWidth = entity.getWidth() / 2.0;
		Location location;
		switch (mFacing) {
		case UP:
			// y+
			location = centerLoc.clone();
			location.setY(mLocation1.getY());
			break;
		case DOWN:
			// y-
			location = centerLoc.clone();
			location.setY(mLocation1.getY() + 1.0 - entity.getHeight());
			break;
		case SOUTH:
			// z+
			location = mLocation1.clone();
			location.setX(location.getX() + 0.5);
			location.setZ(location.getZ() + entityHalfWidth);
			break;
		case NORTH:
			// z-
			location = mLocation1.clone();
			location.setX(location.getX() + 0.5);
			location.setZ(location.getZ() + 1.0 - entityHalfWidth);
			break;
		case EAST:
			// x+
			location = mLocation1.clone();
			location.setZ(location.getZ() + 0.5);
			location.setX(location.getX() + entityHalfWidth);
			break;
		case WEST:
		default:
			// x-
			location = mLocation1.clone();
			location.setZ(location.getZ() + 0.5);
			location.setX(location.getX() + 1.0 - entityHalfWidth);
			break;
		}
		return location;
	}

	private static double getVectorComponent(Vector input, Vector direction) {
		return input.getX() * direction.getX() + input.getY() * direction.getY() + input.getZ() * direction.getZ();
	}

	private Vector fromInterPortalComponents(Vector input) {
		Vector result = new Vector();
		result.add(portalLeftDirection().clone().multiply(input.getX()));
		result.add(mPortalTopDirection.clone().multiply(input.getY()));
		result.add(mPortalOutDirection.clone().multiply(input.getZ()));
		return result;
	}

	// Does not handle look; that is better handled separately
	private Location toInterPortalCoords(Location locIn) {
		Vector locCentered = locIn.clone().subtract(centerLocation()).toVector();
		double relativeLeft = getVectorComponent(locCentered, portalLeftDirection());
		double relativeUp = getVectorComponent(locCentered, mPortalTopDirection);
		double relativeForward = getVectorComponent(locCentered, mPortalOutDirection);
		return new Location(locIn.getWorld(), -relativeLeft, relativeUp, -relativeForward);
	}

	private Vector toInterPortalDirection(Vector directionIn) {
		double relativeLeft = getVectorComponent(directionIn, portalLeftDirection());
		double relativeUp = getVectorComponent(directionIn, mPortalTopDirection);
		double relativeForward = getVectorComponent(directionIn, mPortalOutDirection);
		return new Vector(-relativeLeft, relativeUp, -relativeForward);
	}

	// Does not handle look; that is better handled separately
	private Location fromInterPortalCoords(Location locIn) {
		return centerLocation().add(fromInterPortalComponents(locIn.toVector()));
	}

	private Vector fromInterPortalDirection(Vector directionIn) {
		return fromInterPortalComponents(directionIn);
	}

	// Travel from this portal to the other portal
	public void travel(Entity entity) {
		travel(entity, entity.getVelocity());
	}

	public void travel(Entity entity, Vector velocity) {
		if (mPair == null) {
			return;
		}

		Location location = entity.getLocation().clone();
		Vector direction = location.getDirection();
		if (mFacing == BlockFace.DOWN) {
			direction.setY(Math.abs(direction.getY()));
		}
		Vector fireballDirection = direction; // Dummy value to satisfy the compiler; not actually used.
		if (entity instanceof Fireball) {
			fireballDirection = ((Fireball)entity).getDirection();
		}

		location = mPair.fromInterPortalCoords(toInterPortalCoords(location));
		fixInsideWall(entity, location);
		if (willBeInBlock(entity, location)) {
			location = mPair.defaultTeleportLocation(entity);
		}
		direction = mPair.fromInterPortalDirection(toInterPortalDirection(direction));
		velocity = mPair.fromInterPortalDirection(toInterPortalDirection(velocity));
		velocity.add(mPair.mFacing.getDirection().multiply(VERTICAL_BOOST));
		if (entity instanceof Fireball) {
			fireballDirection = mPair.fromInterPortalDirection(toInterPortalDirection(fireballDirection));
		}

		location.setDirection(direction);
		entity.teleport(location);
		entity.setVelocity(velocity);
		if (entity instanceof Fireball) {
			((Fireball)entity).setDirection(fireballDirection);
		}
	}
}
