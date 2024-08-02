package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.copyPos;
import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class WavePose implements GravePose {

	public static final String NAME = "Wave";

	@Override
	public Material getDisplayItem() {
		return Material.WATER_BUCKET;
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(2), Math.toRadians(355), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(10), Math.toRadians(347), Math.toRadians(351));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(353), Math.toRadians(26), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(12), Math.toRadians(110), Math.toRadians(7));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(349), Math.toRadians(0), Math.toRadians(146));
	}

	@Override
	public void playAnimation(ArmorStand grave) {
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

		Pose middleKeyframe = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(2), Math.toRadians(355), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(12), Math.toRadians(110), Math.toRadians(7)),
			new EulerAngle(Math.toRadians(349), Math.toRadians(0), Math.toRadians(192)),
			new EulerAngle(Math.toRadians(10), Math.toRadians(347), Math.toRadians(351)),
			new EulerAngle(Math.toRadians(353), Math.toRadians(26), Math.toRadians(0))
		);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						copyPos(grave, startKeyframe);
						break;
					case 5:
						posToPos(startKeyframe, endKeyframe, grave, 5);
						break;
					case 10, 20:
						posToPos(endKeyframe, middleKeyframe, grave, 5);
						break;
					case 15, 25:
						posToPos(middleKeyframe, endKeyframe, grave, 5);
						break;
					default:
						if (mTicks > 30) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
