package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class BirdPose implements GravePose {

	public static final String NAME = "Bird";

	@Override
	public Material getDisplayItem() {
		return Material.FEATHER;
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(30), Math.toRadians(349), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(345), Math.toRadians(0), Math.toRadians(351));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(333), Math.toRadians(0), Math.toRadians(3));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(205), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public void playAnimation(ArmorStand grave) {
		Pose keyframe1 = new Pose(
			getHeadAngle(false),
			getBodyAngle(false),
			getLeftArmAngle(false),
			getRightArmAngle(false),
			getLeftLegAngle(false),
			getRightLegAngle(false)
		);

		Pose keyframe2 = new Pose(
			new EulerAngle(0, 0, 0),
			new EulerAngle(Math.toRadians(18), Math.toRadians(10), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(205), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(0, 0, 0),
			new EulerAngle(Math.toRadians(345), Math.toRadians(0), Math.toRadians(351)),
			new EulerAngle(Math.toRadians(333), Math.toRadians(0), Math.toRadians(7))
		);

		Pose keyframe3 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(30), Math.toRadians(11)),
			new EulerAngle(Math.toRadians(14), Math.toRadians(26), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(337), Math.toRadians(42), Math.toRadians(21)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(0), Math.toRadians(31)),
			new EulerAngle(Math.toRadians(333), Math.toRadians(331), Math.toRadians(351)),
			new EulerAngle(Math.toRadians(337), Math.toRadians(305), Math.toRadians(0))
		);

		Pose keyframe4 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(333), Math.toRadians(347)),
			new EulerAngle(Math.toRadians(14), Math.toRadians(329), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(353), Math.toRadians(18), Math.toRadians(323)),
			new EulerAngle(Math.toRadians(337), Math.toRadians(301), Math.toRadians(355)),
			new EulerAngle(Math.toRadians(349), Math.toRadians(38), Math.toRadians(9)),
			new EulerAngle(Math.toRadians(349), Math.toRadians(36), Math.toRadians(19))
		);

		Location graveLocation = grave.getLocation();
		float yaw = graveLocation.getYaw();
		double radians = Math.toRadians(yaw);

		double offsetX = 0.06 * Math.cos(radians);
		double offsetZ = 0.06 * Math.sin(radians);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0, 14:
						posToPos(keyframe2, keyframe1, grave, 5);
						grave.teleport(grave.getLocation().add(0, 0.08, 0));
						break;
					case 7, 21:
						posToPos(keyframe1, keyframe2, grave, 5);
						grave.teleport(grave.getLocation().add(0, -0.08, 0));
						break;
					case 28:
						posToPos(keyframe2, keyframe3, grave, 5);
						break;
					case 35, 49:
						posToPos(keyframe3, keyframe4, grave, 5);
						grave.teleport(graveLocation.add(offsetX, 0, offsetZ));
						break;
					case 42, 56:
						posToPos(keyframe4, keyframe3, grave, 5);
						grave.teleport(graveLocation.add(-offsetX, 0, -offsetZ));
						break;
					default:
						if (mTicks > 63) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
