package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class CleaningPose implements GravePose {

	public static final String NAME = "Cleaning";

	@Override
	public Material getDisplayItem() {
		return Material.SPONGE;
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(4), Math.toRadians(12), Math.toRadians(17));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(6), Math.toRadians(42), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(2), Math.toRadians(22), Math.toRadians(13));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(349), Math.toRadians(40), Math.toRadians(7));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(203), Math.toRadians(0), Math.toRadians(27));
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
			new EulerAngle(Math.toRadians(12), Math.toRadians(14), Math.toRadians(5)),
			new EulerAngle(Math.toRadians(351), Math.toRadians(64), Math.toRadians(13)),
			new EulerAngle(Math.toRadians(203), Math.toRadians(0), Math.toRadians(250)),
			new EulerAngle(Math.toRadians(357), Math.toRadians(343), Math.toRadians(3)),
			new EulerAngle(Math.toRadians(345), Math.toRadians(26), Math.toRadians(0))
		);

		Pose keyframe3 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(8), Math.toRadians(347), Math.toRadians(351)),
			new EulerAngle(Math.toRadians(351), Math.toRadians(52), Math.toRadians(9)),
			new EulerAngle(Math.toRadians(207), Math.toRadians(0), Math.toRadians(122)),
			new EulerAngle(Math.toRadians(14), Math.toRadians(28), Math.toRadians(357)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(18), Math.toRadians(7))
		);

		Pose keyframe4 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(8), Math.toRadians(345), Math.toRadians(345)),
			new EulerAngle(Math.toRadians(351), Math.toRadians(40), Math.toRadians(7)),
			new EulerAngle(Math.toRadians(207), Math.toRadians(24), Math.toRadians(360)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(337), Math.toRadians(339)),
			new EulerAngle(Math.toRadians(12), Math.toRadians(48), Math.toRadians(0))
		);


		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0, 42:
						posToPos(keyframe1, keyframe2, grave, 7);
						grave.teleport(grave.getLocation().add(0, -0.06, 0));
						break;
					case 7, 49:
						grave.teleport(grave.getLocation().add(0, -0.06, 0));
						posToPos(keyframe2, keyframe3, grave, 7);
						grave.teleport(grave.getLocation().add(0, 0.06, 0));
						break;
					case 14, 56:
						posToPos(keyframe3, keyframe4, grave, 7);
						grave.teleport(grave.getLocation().add(0, 0.06, 0));
						break;
					case 21:
						posToPos(keyframe4, keyframe3, grave, 7);
						grave.teleport(grave.getLocation().add(0, -0.06, 0));
						break;
					case 28:
						grave.teleport(grave.getLocation().add(0, -0.06, 0));
						posToPos(keyframe3, keyframe2, grave, 7);
						grave.teleport(grave.getLocation().add(0, 0.06, 0));
						break;
					case 35:
						posToPos(keyframe2, keyframe1, grave, 7);
						grave.teleport(grave.getLocation().add(0, 0.06, 0));
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
