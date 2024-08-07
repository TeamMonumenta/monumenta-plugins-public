package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.copyPos;
import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class TPose implements GravePose {

	public static final String NAME = "T-Pose";

	@Override
	public Material getDisplayItem() {
		return Material.SKELETON_SKULL;
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(270));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(90));
	}

	@Override
	public void playAnimation(ArmorStand grave, Player player) {
		Pose startKeyframe = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0))
		);

		Pose endKeyframe = new Pose(
			getHeadAngle(false),
			getBodyAngle(false),
			getLeftArmAngle(false),
			getRightArmAngle(false),
			getLeftLegAngle(false),
			getRightLegAngle(false)
		);
		Vector v = player.getLocation().toVector().subtract(grave.getLocation().toVector());
		float f = (float) -Math.toDegrees(Math.atan2(v.getX(), v.getZ()));

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						grave.setRotation(f, 0);
						copyPos(grave, startKeyframe);
						break;
					case 5:
						posToPos(startKeyframe, endKeyframe, grave, 15);
						break;
					case 20:
						this.cancel();
						break;
					default:
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
