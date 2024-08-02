package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class SwordSpinPose implements GravePose {

	public static final String NAME = "Sword Spin";

	@Override
	public Material getDisplayItem() {
		return Material.IRON_SWORD;
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(1), Math.toRadians(350), Math.toRadians(0));
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(358), Math.toRadians(323), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(6), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(343), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(16), Math.toRadians(325), Math.toRadians(14));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(134), Math.toRadians(115), Math.toRadians(285));
	}

	@Override
	public void playAnimation(ArmorStand grave) {
		Pose keyframe1 = new Pose(
			new EulerAngle(Math.toRadians(358), Math.toRadians(323), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(1), Math.toRadians(350), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(318), Math.toRadians(14)),
			new EulerAngle(Math.toRadians(325), Math.toRadians(0), Math.toRadians(7)),
			new EulerAngle(Math.toRadians(343), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe2 = new Pose(
			new EulerAngle(Math.toRadians(358), Math.toRadians(323), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(1), Math.toRadians(350), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(325), Math.toRadians(14)),
			new EulerAngle(Math.toRadians(87), Math.toRadians(172), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(343), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe3 = new Pose(
			new EulerAngle(Math.toRadians(358), Math.toRadians(323), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(1), Math.toRadians(350), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(325), Math.toRadians(14)),
			new EulerAngle(Math.toRadians(87), Math.toRadians(172), Math.toRadians(105)),
			new EulerAngle(Math.toRadians(343), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe4 = new Pose(
			new EulerAngle(Math.toRadians(358), Math.toRadians(323), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(1), Math.toRadians(350), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(325), Math.toRadians(14)),
			new EulerAngle(Math.toRadians(87), Math.toRadians(172), Math.toRadians(270)),
			new EulerAngle(Math.toRadians(343), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe5 = new Pose(
			getHeadAngle(false),
			getBodyAngle(false),
			getLeftArmAngle(false),
			getRightArmAngle(false),
			getLeftLegAngle(false),
			getRightLegAngle(false)
		);

		Pose keyframe6 = new Pose(
			new EulerAngle(Math.toRadians(358), Math.toRadians(323), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(1), Math.toRadians(350), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(325), Math.toRadians(14)),
			new EulerAngle(Math.toRadians(112), Math.toRadians(212), Math.toRadians(185)),
			new EulerAngle(Math.toRadians(343), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(0), Math.toRadians(0))
		);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						posToPos(keyframe1, keyframe2, grave, 6);
						break;
					case 6, 12, 18, 24:
						posToPos(keyframe2, keyframe3, grave, 2);
						break;
					case 8, 14, 20, 26:
						posToPos(keyframe3, keyframe4, grave, 2);
						break;
					case 10, 16, 22, 28:
						posToPos(keyframe4, keyframe2, grave, 2);
						break;
					case 31:
						posToPos(keyframe2, keyframe5, grave, 5);
						break;
					case 38:
						posToPos(keyframe5, keyframe6, grave, 4);
						break;
					default:
						if (mTicks > 43) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
