package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.copyPos;
import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class SalutePose implements GravePose {

	public static final String NAME = "Salute";

	@Override
	public Material getDisplayItem() {
		return Material.GLOBE_BANNER_PATTERN;
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(12), Math.toRadians(353), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(345), Math.toRadians(327), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(16), Math.toRadians(0), Math.toRadians(7));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(6), Math.toRadians(204), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(231), Math.toRadians(309), Math.toRadians(0));
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
			new EulerAngle(Math.toRadians(12), Math.toRadians(353), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(204), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(231), Math.toRadians(360), Math.toRadians(3)),
			new EulerAngle(Math.toRadians(345), Math.toRadians(327), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(0), Math.toRadians(7))
		);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						copyPos(grave, startKeyframe);
						break;
					case 7:
						posToPos(startKeyframe, endKeyframe, grave, 7);
						break;
					case 16:
						posToPos(endKeyframe, middleKeyframe, grave, 5);
						break;
					default:
						if (mTicks > 23) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
