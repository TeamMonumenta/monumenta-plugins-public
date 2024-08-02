package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class SwingPose implements GravePose {

	public static final String NAME = "Swing";

	@Override
	public Material getDisplayItem() {
		return Material.LEAD;
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(11), Math.toRadians(29), Math.toRadians(353));
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(11), Math.toRadians(333), Math.toRadians(360));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(360), Math.toRadians(22), Math.toRadians(14));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(37), Math.toRadians(17));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(242), Math.toRadians(57), Math.toRadians(360));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(197), Math.toRadians(0), Math.toRadians(333));
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
			new EulerAngle(Math.toRadians(0), Math.toRadians(2), Math.toRadians(12)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(328), Math.toRadians(0), Math.toRadians(343)),
			new EulerAngle(Math.toRadians(335), Math.toRadians(0), Math.toRadians(333)),
			new EulerAngle(Math.toRadians(345), Math.toRadians(280), Math.toRadians(360)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe3 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(2), Math.toRadians(12)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(343), Math.toRadians(348)),
			new EulerAngle(Math.toRadians(182), Math.toRadians(308), Math.toRadians(24)),
			new EulerAngle(Math.toRadians(147), Math.toRadians(172), Math.toRadians(59)),
			new EulerAngle(Math.toRadians(328), Math.toRadians(280), Math.toRadians(360)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(47), Math.toRadians(341))
		);

		Location graveLocation = grave.getLocation();
		float yaw = graveLocation.getYaw();
		double radians = Math.toRadians(yaw);

		double offsetX = 0.15 * Math.cos(radians);
		double offsetZ = 0.15 * Math.sin(radians);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0, 28, 56:
						posToPos(keyframe1, keyframe2, grave, 6);
						grave.teleport(graveLocation.add(offsetX, 0, offsetZ));
						break;
					case 6, 34, 62:
						posToPos(keyframe2, keyframe3, grave, 6);
						break;
					case 14, 42, 70:
						posToPos(keyframe3, keyframe2, grave, 6);
						grave.teleport(graveLocation.add(-offsetX, 0, -offsetZ));
						break;
					case 20, 48, 76:
						posToPos(keyframe2, keyframe1, grave, 6);
						break;
					default:
						if (mTicks > 83) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
