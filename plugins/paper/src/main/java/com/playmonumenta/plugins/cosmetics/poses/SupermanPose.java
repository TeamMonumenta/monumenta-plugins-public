package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.copyPos;
import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class SupermanPose implements GravePose {

	public static final String NAME = "Superman";

	@Override
	public Material getDisplayItem() {
		return Material.RED_BANNER;
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(345), Math.toRadians(24), Math.toRadians(0));
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(38), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(82), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(60), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(255), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(227), Math.toRadians(0), Math.toRadians(0));
	}


	@Override
	public void playAnimation(ArmorStand grave, Player player) {
		Pose keyframe1 = new Pose(
			getHeadAngle(false),
			getBodyAngle(false),
			getLeftArmAngle(false),
			getRightArmAngle(false),
			getLeftLegAngle(false),
			getRightLegAngle(false)
		);
		Pose keyframe2 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0))
		);
		Location graveLocation = grave.getLocation();
		float yaw = graveLocation.getYaw();
		double radians = Math.toRadians(yaw);

		double offsetX = -0.5 * Math.sin(radians);
		double offsetZ = 0.5 * Math.cos(radians);

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						copyPos(grave, keyframe2);
						break;
					case 5:
						posToPos(keyframe2, keyframe1, grave, 5);
						break;
					case 9, 13, 17, 21:
						grave.teleport(graveLocation.add(offsetX, 0.5, offsetZ));
						break;
					case 26:
						this.cancel();
						grave.teleport(graveLocation.add(-offsetX * 4, -2, -offsetZ * 4));
						break;
					default:
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
