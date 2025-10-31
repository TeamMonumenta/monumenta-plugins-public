package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

public interface GravePose {

	record Pose(EulerAngle head, EulerAngle body, EulerAngle leftArm, EulerAngle rightArm, EulerAngle leftLeg,
	            EulerAngle rightLeg) {
	}

	// In general, override the individual angle methods instead of this method, unless there is randomization and the angles are not independent
	default Pose getPose(boolean small) {
		return new Pose(getHeadAngle(small), getBodyAngle(small), getLeftArmAngle(small), getRightArmAngle(small), getLeftLegAngle(small), getRightLegAngle(small));
	}

	default EulerAngle getHeadAngle(boolean small) {
		return EulerAngle.ZERO;
	}

	default EulerAngle getBodyAngle(boolean small) {
		return EulerAngle.ZERO;
	}

	default EulerAngle getLeftArmAngle(boolean small) {
		return small ? new EulerAngle(Math.toRadians(260), Math.toRadians(45), 0) : EulerAngle.ZERO;
	}

	default EulerAngle getRightArmAngle(boolean small) {
		return small ? new EulerAngle(Math.toRadians(270), Math.toRadians(330), 0) : EulerAngle.ZERO;
	}

	default EulerAngle getLeftLegAngle(boolean small) {
		return EulerAngle.ZERO;
	}

	default EulerAngle getRightLegAngle(boolean small) {
		return EulerAngle.ZERO;
	}

	default boolean switchHeldItemHand() {
		return false;
	}

	default boolean basePlate(boolean small) {
		return true;
	}

	// This is run once a second
	default void passiveParticles(Player player, ArmorStand grave, boolean phylactery) {
		if (!phylactery) {
			return;
		}
		Location loc = grave.getLocation();
		new BukkitRunnable() {
			int mTicks = 0;
			double mY = 0;
			double mTheta = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.TOTEM, loc.clone().add(FastUtils.cos(mTheta), mY, FastUtils.sin(mTheta)), 1, 0, 0, 0, 0).spawnAsPlayerPassive(player);
				new PartialParticle(Particle.TOTEM, loc.clone().add(-FastUtils.cos(mTheta), mY, -FastUtils.sin(mTheta)), 1, 0, 0, 0, 0).spawnAsPlayerPassive(player);

				mTicks += 2;
				mY += 0.2;
				mTheta += Math.PI / 5;

				if (mTicks >= 20) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 2);
	}

	void playAnimation(ArmorStand grave, Player player);

	Material getDisplayItem();
}
