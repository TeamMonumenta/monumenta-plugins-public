package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class GriddyPose implements GravePose {

	public static final String NAME = "Griddy";

	@Override
	public Material getDisplayItem() {
		return Material.NETHERITE_BOOTS;
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(32), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(14), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(311), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(16), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(70), Math.toRadians(56), Math.toRadians(1));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(66), Math.toRadians(301), Math.toRadians(0));
	}

	@Override
	public void playAnimation(ArmorStand grave) {
		Pose keyframe1 = new Pose(
			new EulerAngle(Math.toRadians(26), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(12), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(305), Math.toRadians(26), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(311), Math.toRadians(339), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(307), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(8), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe2 = new Pose(
			new EulerAngle(Math.toRadians(30), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(12), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(329), Math.toRadians(0), Math.toRadians(1)),
			new EulerAngle(Math.toRadians(329), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(2), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe3 = new Pose(
			new EulerAngle(Math.toRadians(30), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(14), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(10), Math.toRadians(0), Math.toRadians(345)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(9)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(307), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe4 = new Pose(
			new EulerAngle(Math.toRadians(22), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(10), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(46), Math.toRadians(36), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(42), Math.toRadians(331), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(14), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(353), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe5 = new Pose(
			getHeadAngle(false),
			getBodyAngle(false),
			getLeftArmAngle(false),
			getRightArmAngle(false),
			getLeftLegAngle(false),
			getRightLegAngle(false)
		);

		Location graveLocation = grave.getLocation();
		float yaw = graveLocation.getYaw();
		double radians = Math.toRadians(yaw);

		double offsetX = -0.1 * Math.sin(radians);
		double offsetZ = 0.1 * Math.cos(radians);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						posToPos(keyframe1, keyframe2, grave, 4);
						grave.teleport(graveLocation.add(offsetX, 0.1, offsetZ));
						break;
					case 4:
						posToPos(keyframe2, keyframe3, grave, 4);
						grave.teleport(graveLocation.add(offsetX, -0.1, offsetZ));
						break;
					case 8:
						posToPos(keyframe3, keyframe4, grave, 4);
						grave.teleport(graveLocation.add(offsetX, 0.1, offsetZ));
						break;
					case 12:
						posToPos(keyframe4, keyframe5, grave, 4);
						grave.teleport(graveLocation.add(offsetX, -0.1, offsetZ));
						break;
					case 16:
						posToPos(keyframe5, keyframe4, grave, 4);
						grave.teleport(graveLocation.add(offsetX, 0.1, offsetZ));
						break;
					case 20:
						posToPos(keyframe4, keyframe3, grave, 4);
						grave.teleport(graveLocation.add(offsetX, -0.1, offsetZ));
						break;
					case 24:
						posToPos(keyframe3, keyframe2, grave, 4);
						grave.teleport(graveLocation.add(-offsetX, 0.1, -offsetZ));
						break;
					case 28:
						posToPos(keyframe2, keyframe1, grave, 4);
						grave.teleport(graveLocation.add(-offsetX, -0.1, -offsetZ));
						break;
					case 32:
						posToPos(keyframe1, keyframe2, grave, 4);
						grave.teleport(graveLocation.add(-offsetX, 0.1, -offsetZ));
						break;
					case 36:
						posToPos(keyframe2, keyframe3, grave, 4);
						grave.teleport(graveLocation.add(-offsetX, -0.1, -offsetZ));
						break;
					case 40:
						posToPos(keyframe3, keyframe4, grave, 4);
						grave.teleport(graveLocation.add(-offsetX, 0.1, -offsetZ));
						break;
					case 44:
						posToPos(keyframe4, keyframe5, grave, 4);
						grave.teleport(graveLocation.add(-offsetX, -0.1, -offsetZ));
						break;
					case 48:
						posToPos(keyframe5, keyframe4, grave, 4);
						break;
					default:
						if (mTicks > 52) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
