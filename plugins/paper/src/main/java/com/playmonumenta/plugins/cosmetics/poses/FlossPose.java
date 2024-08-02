package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class FlossPose implements GravePose {

	public static final String NAME = "Floss";

	@Override
	public Material getDisplayItem() {
		return Material.STRING;
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(10), Math.toRadians(0), Math.toRadians(15));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(6), Math.toRadians(118), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(24), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(313));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(32), Math.toRadians(0), Math.toRadians(315));
	}

	@Override
	public void playAnimation(ArmorStand grave) {
		Pose startKeyframe = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(18), Math.toRadians(16), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(0), Math.toRadians(51)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(60)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(15)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(23))
		);

		Pose endKeyframe = new Pose(
			getHeadAngle(false),
			getBodyAngle(false),
			getLeftArmAngle(false),
			getRightArmAngle(false),
			getLeftLegAngle(false),
			getRightLegAngle(false)
		);

		Pose mid1Keyframe = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(20), Math.toRadians(0), Math.toRadians(13)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(321)),
			new EulerAngle(Math.toRadians(343), Math.toRadians(0), Math.toRadians(319)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(118), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(24), Math.toRadians(0))
		);

		Pose mid2Keyframe = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(16), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(38), Math.toRadians(0), Math.toRadians(57)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(60)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(15)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(23))
		);


		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0, 30:
						posToPos(startKeyframe, endKeyframe, grave, 5);
						break;
					case 5, 35:
						posToPos(endKeyframe, startKeyframe, grave, 5);
						break;
					case 10, 40:
						posToPos(startKeyframe, mid1Keyframe, grave, 5);
						break;
					case 15:
						posToPos(mid1Keyframe, mid2Keyframe, grave, 5);
						break;
					case 20:
						posToPos(mid2Keyframe, mid1Keyframe, grave, 5);
						break;
					case 25:
						posToPos(mid1Keyframe, startKeyframe, grave, 5);
						break;
					default:
						if (mTicks > 45) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
