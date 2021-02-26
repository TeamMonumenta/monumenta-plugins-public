package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SpellFloat extends Spell {
	private static final double FLOAT_VELOCITY = 0.25;
	private static final double HORIZONTAL_JUMP_VELOCITY = 0.25;
	private static final double VERTICAL_JUMP_IN_VELOCITY = 0.5;
	private static final double VERTICAL_JUMP_OUT_VELOCITY = 0.4;
	private static final double HORIZONTAL_MOVEMENT_ALLOWANCE = 0.25;
	private static final double VERTICAL_MOVEMENT_ALLOWANCE = 1.5;
	private static final int JUMP_CHARGE_TIME = 1 + 4 * 2;
	private static final int JUMP_COOLDOWN = 4 * 3;

	private LivingEntity mFloater;
	private Location mPrevLoc;
	private int mTicksInSamePos = 0;
	private int mTicks = 0;
	private int mJumpCooldown = 0;

	public SpellFloat(LivingEntity floater) {
		mFloater = floater;
		mPrevLoc = floater.getLocation();
	}

	@Override
	public void run() {
		mTicks++;
		if (mJumpCooldown > 0) {
			mJumpCooldown--;
		}
		Location entityFeet = mFloater.getLocation().add(new Vector(0, -0.25, 0));
		Location entityEyes = mFloater.getEyeLocation();
		Location[] potentialWaterEntrances = {
			mFloater.getLocation().add(new Vector(-1, -1, 0)),
			mFloater.getLocation().add(new Vector(1, -1, 0)),
			mFloater.getLocation().add(new Vector(0, -1, -1)),
			mFloater.getLocation().add(new Vector(0, -1, 1)),
		};
		Location[] potentialWaterExits = {
			mFloater.getLocation().add(new Vector(-1, 0, 0)),
			mFloater.getLocation().add(new Vector(1, 0, 0)),
			mFloater.getLocation().add(new Vector(0, 0, -1)),
			mFloater.getLocation().add(new Vector(0, 0, 1)),
		};
		Location blockAbove = entityEyes.add(new Vector(0, 1, 0));

		// This is the actual floating code
		if (mTicks >= 4) {
			mTicks = 0;
			if ((entityFeet.getBlock().isLiquid() || entityEyes.getBlock().isLiquid()) && !blockAbove.getBlock().getType().isSolid()) {
				mFloater.setVelocity(mFloater.getVelocity().add(new Vector(0, FLOAT_VELOCITY, 0)));
			}
		}

		// Keep track of how long the entity has been in approximately the same spot
		if (Math.abs(entityFeet.getX() - mPrevLoc.getX()) > HORIZONTAL_MOVEMENT_ALLOWANCE ||
			Math.abs(entityFeet.getZ() - mPrevLoc.getZ()) > HORIZONTAL_MOVEMENT_ALLOWANCE ||
			Math.abs(entityFeet.getY() - mPrevLoc.getY()) > VERTICAL_MOVEMENT_ALLOWANCE) {
			mTicksInSamePos = 0;
		} else {
			mTicksInSamePos++;
		}
		mPrevLoc = entityFeet;

		// Try jumping only if the mob has not moved more than the allowance for 2 seconds and jump is off cooldown
		if (mJumpCooldown == 0 && mTicksInSamePos >= JUMP_CHARGE_TIME) {
			// Check if a block exists to jump onto or if water exists to jump into
			if (entityFeet.getBlock().isLiquid() && !entityEyes.getBlock().isLiquid()) {
				for (int i = 0; i < 4; i++) {
					if (potentialWaterExits[i].getBlock().getType().isSolid() &&
					    potentialWaterExits[i].add(new Vector(0, 1, 0)).getBlock().isPassable() &&
					    potentialWaterExits[i].add(new Vector(0, 2, 0)).getBlock().isPassable()) {
						this.jump(true, i);
						mJumpCooldown = JUMP_COOLDOWN;
						break;
					}
				}
			} else if (mFloater.getLocation().getBlock().isPassable() && !entityEyes.getBlock().isLiquid()) {
				for (int i = 0; i < 4; i++) {
					if (potentialWaterEntrances[i].getBlock().isLiquid() &&
					    potentialWaterEntrances[i].add(new Vector(0, 1, 0)).getBlock().isPassable() &&
					    potentialWaterEntrances[i].add(new Vector(0, 2, 0)).getBlock().isPassable()) {
						this.jump(false, i);
						mJumpCooldown = JUMP_COOLDOWN;
						break;
					}
				}
			}
		}
	}

	/*
	 * out = true means jump out of water
	 * out = false means jump into water
	 */
	private void jump(boolean out, int i) {
		if (out) {
			mFloater.setVelocity(mFloater.getVelocity().add(new Vector(0, VERTICAL_JUMP_OUT_VELOCITY, 0)));
		} else {
			mFloater.setVelocity(mFloater.getVelocity().add(new Vector(0, VERTICAL_JUMP_IN_VELOCITY, 0)));
		}

		if (i == 0) {
			mFloater.setVelocity(mFloater.getVelocity().add(new Vector(-HORIZONTAL_JUMP_VELOCITY, 0, 0)));
		} else if (i == 1) {
			mFloater.setVelocity(mFloater.getVelocity().add(new Vector(HORIZONTAL_JUMP_VELOCITY, 0, 0)));
		} else if (i == 2) {
			mFloater.setVelocity(mFloater.getVelocity().add(new Vector(0, 0, -HORIZONTAL_JUMP_VELOCITY)));
		} else {
			mFloater.setVelocity(mFloater.getVelocity().add(new Vector(0, 0, HORIZONTAL_JUMP_VELOCITY)));
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
