package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class SlidinPose implements GravePose {

	public static final String NAME = "Slidin";

	@Override
	public Material getDisplayItem() {
		return Material.ICE;
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(358), Math.toRadians(350), Math.toRadians(7));
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(11), Math.toRadians(348), Math.toRadians(7));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(17));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(355), Math.toRadians(0), Math.toRadians(308));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(205), Math.toRadians(0), Math.toRadians(27));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(195), Math.toRadians(0), Math.toRadians(331));
	}

	@Override
	public void playAnimation(ArmorStand grave) {
		Pose keyframe1 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(14), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(358), Math.toRadians(350), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(345), Math.toRadians(325), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(330), Math.toRadians(34), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(9), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(328), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe2 = new Pose(
			new EulerAngle(Math.toRadians(24), Math.toRadians(17), Math.toRadians(343)),
			new EulerAngle(Math.toRadians(358), Math.toRadians(350), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(303), Math.toRadians(298), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(295), Math.toRadians(42), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(358), Math.toRadians(345), Math.toRadians(2)),
			new EulerAngle(Math.toRadians(69), Math.toRadians(335), Math.toRadians(0))
		);

		Pose keyframe3 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(14), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(358), Math.toRadians(350), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(345), Math.toRadians(350), Math.toRadians(14)),
			new EulerAngle(Math.toRadians(330), Math.toRadians(12), Math.toRadians(338)),
			new EulerAngle(Math.toRadians(325), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(11), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe4 = new Pose(
			getHeadAngle(false),
			getBodyAngle(false),
			getLeftArmAngle(false),
			getRightArmAngle(false),
			getLeftLegAngle(false),
			getRightLegAngle(false)
		);

		Pose keyframe5 = new Pose(
			new EulerAngle(Math.toRadians(11), Math.toRadians(325), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(343), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(313), Math.toRadians(7), Math.toRadians(14)),
			new EulerAngle(Math.toRadians(308), Math.toRadians(360), Math.toRadians(338)),
			new EulerAngle(Math.toRadians(325), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(14), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe6 = new Pose(
			new EulerAngle(Math.toRadians(21), Math.toRadians(325), Math.toRadians(9)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(343), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(348)),
			new EulerAngle(Math.toRadians(328), Math.toRadians(333), Math.toRadians(338)),
			new EulerAngle(Math.toRadians(67), Math.toRadians(22), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(14), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe7 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(14), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(4), Math.toRadians(9), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(328), Math.toRadians(42), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(325), Math.toRadians(305), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(9), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(328), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe8 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(14), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(4), Math.toRadians(9), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(328), Math.toRadians(42), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(325), Math.toRadians(305), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(346)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(0), Math.toRadians(39))
		);

		Location graveLocation = grave.getLocation();
		float yaw = graveLocation.getYaw();
		double radians = Math.toRadians(yaw);

		double offsetX = 0.5 * Math.cos(radians);
		double offsetZ = 0.5 * Math.sin(radians);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						posToPos(keyframe1, keyframe2, grave, 6);
						break;
					case 7:
						posToPos(keyframe2, keyframe3, grave, 6);
						break;
					case 14:
						posToPos(keyframe3, keyframe4, grave, 6);
						break;
					case 18:
						grave.teleport(graveLocation.add(offsetX, 0, offsetZ));
						break;
					case 21:
						posToPos(keyframe4, keyframe5, grave, 6);
						break;
					case 28:
						posToPos(keyframe5, keyframe6, grave, 6);
						break;
					case 35:
						posToPos(keyframe6, keyframe7, grave, 6);
						break;
					case 42:
						posToPos(keyframe7, keyframe8, grave, 6);
						break;
					case 46:
						grave.teleport(graveLocation.add(-offsetX, 0, -offsetZ));
						break;
					default:
						if (mTicks > 49) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
