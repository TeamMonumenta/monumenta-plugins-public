package com.playmonumenta.plugins.bosses.bosses;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.portals.Portal;
import com.playmonumenta.plugins.portals.PortalManager;

public class PortalBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_portal";

	PortalEntityCheck mRunnable;
	Location mPastPosition1;
	Location mPastPosition2;
	int mCooldown = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PortalBoss(plugin, boss);
	}

	public PortalBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, 20, null);
		mRunnable = new PortalEntityCheck();
		mRunnable.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void unload() {
		if (mRunnable != null) {
			mRunnable.cancel();
		}
		mRunnable = null;
	}

	public class PortalEntityCheck extends BukkitRunnable {
		@Override
		public void run() {

			//Velocity updating and storage
			if (mPastPosition1 != null) {
				mPastPosition2 = mPastPosition1;
			}
			mPastPosition1 = mBoss.getLocation().clone();
			//Get portal list from portal manager
			if (PortalManager.mPlayerPortal1 == null || PortalManager.mPlayerPortal2 == null) {
				return;
			}
			ArrayList<Portal> activePortals = new ArrayList<Portal>();
			activePortals.addAll(PortalManager.mPlayerPortal1.values());
			activePortals.addAll(PortalManager.mPlayerPortal2.values());

			//Get collision box for portal boss
			BoundingBox box = mBoss.getBoundingBox().shift(new Vector(0, 1.25, 0));
			box.expand(BlockFace.UP, .5);
			box.expand(BlockFace.DOWN, .5);
			box.expand(0.2, 0, 0.2);

			//Loop through and check for collision
			if (mCooldown > 0) {
				mCooldown--;
			} else {
				for (Portal mP1 : activePortals) {

					World mWorld = mBoss.getWorld();
					BoundingBox b1 = mWorld.getBlockAt(mP1.mBlock1).getBoundingBox().shift(mP1.getShift()).expand(.1);
					BoundingBox b2 = mWorld.getBlockAt(mP1.mBlock2).getBoundingBox().shift(mP1.getShift()).expand(.1);
					if (b1.overlaps(box)) {
						mBoss.teleport(mP1.mPair.mLocation1.toCenterLocation());
						calculateMomentum(mP1.mFacing, mP1.mPair.mFacing, mBoss);
					}
					if (b2.overlaps(box)) {
						mBoss.teleport(mP1.mPair.mLocation2.toCenterLocation());
						calculateMomentum(mP1.mFacing, mP1.mPair.mFacing, mBoss);
					}
				}
			}
		}
	}

	//Set the object velocity to what it should be after entering portal

	public void calculateMomentum(BlockFace b1, BlockFace b2, LivingEntity p) {

		if (mPastPosition2 == null) {
			return;
		}

		Vector v = getRecentVelocity(mPastPosition2, mPastPosition1);

		//Set score of cube to move correctly
		int scoreToSet = facingToScore(b2);
		Location l = mBoss.getLocation();
		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "execute positioned " + l.getX() + " " + l.getY() + " " + l.getZ() + " as @e[tag=boss_portal,distance=..1] run scoreboard players set @s temp " + scoreToSet);

		//Tp logic

		if (b1 == BlockFace.UP) {

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

		//Account for armor stand height when tping it down
		if (b2 == BlockFace.DOWN) {
			mBoss.teleport(mBoss.getLocation().add(0, -1, 0));
		}
		//Tp the cube further out if it's stuck
		if (mBoss.getVelocity().length() < 0.1) {
			if (b2 == BlockFace.EAST) {
				mBoss.teleport(mBoss.getLocation().add(new Vector(1, 0, 0)));
			}
			if (b2 == BlockFace.WEST) {
				mBoss.teleport(mBoss.getLocation().add(new Vector(-1, 0, 0)));
			}
			if (b2 == BlockFace.NORTH) {
				mBoss.teleport(mBoss.getLocation().add(new Vector(0, 0, -1)));
			}
			if (b2 == BlockFace.SOUTH) {
				mBoss.teleport(mBoss.getLocation().add(new Vector(0, 0, 1)));
			}
		}

		//Set cooldown before it can tp again
		mCooldown = 12;
	}

	//Workaround for calculating near-instant velocity
	public Vector getRecentVelocity(Location x, Location y) {
		Vector difference = new Vector();
		difference.setX(y.getX() - x.getX());
		difference.setY(y.getY() - x.getY());
		difference.setZ(y.getZ() - x.getZ());
		return difference;
	}

	//Exit portal direction -> score to set for cube

	private int facingToScore(BlockFace facing) {
		switch (facing) {
			case EAST:
				return 1;
			case WEST:
				return 2;
			case SOUTH:
				return 3;
			case NORTH:
				return 4;
			default:
				return 0;
		}
	}

}
