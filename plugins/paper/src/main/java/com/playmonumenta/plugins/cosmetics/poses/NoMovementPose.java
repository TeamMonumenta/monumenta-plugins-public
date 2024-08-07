package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class NoMovementPose implements GravePose {

	public static final String NAME = "No Movement";

	@Override
	public Material getDisplayItem() {
		return Material.BARRIER;
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(355), Math.toRadians(349), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(4), Math.toRadians(12), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(225), Math.toRadians(0), Math.toRadians(0));
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
			new EulerAngle(Math.toRadians(20), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(265), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(263), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(350), Math.toRadians(349), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(339), Math.toRadians(12), Math.toRadians(0))
		);

		new BukkitRunnable() {
			int mTicks = 0;
			boolean mCorrect = true;

			@Override
			public void run() {
				switch (mTicks) {
					case 0, 10, 20, 30, 40:
						posToPos(keyframe1, keyframe2, grave, 3);
						break;
					case 5, 25, 45:
						posToPos(keyframe2, keyframe1, grave, 5);
						break;
					case 15:
						posToPos(keyframe2, keyframe1, grave, 3);
						if (grave.getBodyYaw() < 180) {
							grave.setRotation(grave.getBodyYaw() + 40, 0);
						} else {
							grave.setRotation(grave.getBodyYaw() - 40, 0);
							mCorrect = false;
						}
						break;
					case 35:
						posToPos(keyframe2, keyframe1, grave, 5);
						if (mCorrect) {
							grave.setRotation(grave.getBodyYaw() - 40, 0);
						} else {
							grave.setRotation(grave.getBodyYaw() + 40, 0);
						}
						break;
					case 50:
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
