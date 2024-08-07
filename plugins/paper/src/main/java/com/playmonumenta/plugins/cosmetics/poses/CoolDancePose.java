package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.copyPos;
import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class CoolDancePose implements GravePose {

	public static final String NAME = "Cool Dance";

	@Override
	public Material getDisplayItem() {
		return Material.LOOM;
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(6), Math.toRadians(353), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(6), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(291), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(38), Math.toRadians(118), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(38), Math.toRadians(247), Math.toRadians(0));
	}

	@Override
	public void playAnimation(ArmorStand grave, Player player) {
		Pose keyframe1 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe2 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(353), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(221), Math.toRadians(56), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(223), Math.toRadians(299), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe3 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(353), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(261), Math.toRadians(76), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(283), Math.toRadians(277), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(289), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe4 = new Pose(
			new EulerAngle(Math.toRadians(20), Math.toRadians(4), Math.toRadians(351)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(353), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(38), Math.toRadians(118), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(38), Math.toRadians(247), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(291), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe5 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(341), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(6), Math.toRadians(353), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(118), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(255), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(290), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(0), Math.toRadians(0))
		);

		Pose keyframe6 = new Pose(
			new EulerAngle(Math.toRadians(26), Math.toRadians(0), Math.toRadians(3)),
			new EulerAngle(Math.toRadians(8), Math.toRadians(353), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(38), Math.toRadians(108), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(36), Math.toRadians(237), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(54), Math.toRadians(11)),
			new EulerAngle(Math.toRadians(18), Math.toRadians(46), Math.toRadians(0))
		);

		Pose keyframe7 = new Pose(
			new EulerAngle(Math.toRadians(26), Math.toRadians(0), Math.toRadians(3)),
			new EulerAngle(Math.toRadians(349), Math.toRadians(353), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(86), Math.toRadians(108), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(92), Math.toRadians(237), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(22), Math.toRadians(86), Math.toRadians(349)),
			new EulerAngle(Math.toRadians(325), Math.toRadians(56), Math.toRadians(0))
		);

		Pose keyframe8 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(26), Math.toRadians(9)),
			new EulerAngle(Math.toRadians(4), Math.toRadians(353), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(88), Math.toRadians(206), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(86), Math.toRadians(142), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(323), Math.toRadians(52), Math.toRadians(349)),
			new EulerAngle(Math.toRadians(22), Math.toRadians(56), Math.toRadians(0))
		);

		Pose keyframe9 = new Pose(
			new EulerAngle(Math.toRadians(12), Math.toRadians(0), Math.toRadians(357)),
			new EulerAngle(Math.toRadians(4), Math.toRadians(349), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(169), Math.toRadians(206), Math.toRadians(1)),
			new EulerAngle(Math.toRadians(173), Math.toRadians(142), Math.toRadians(349)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(339), Math.toRadians(355)),
			new EulerAngle(Math.toRadians(309), Math.toRadians(0), Math.toRadians(5))
		);

		Pose keyframe10 = new Pose(
			new EulerAngle(Math.toRadians(12), Math.toRadians(0), Math.toRadians(357)),
			new EulerAngle(Math.toRadians(4), Math.toRadians(349), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(206), Math.toRadians(1)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(34), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(353), Math.toRadians(52), Math.toRadians(360)),
			new EulerAngle(Math.toRadians(10), Math.toRadians(66), Math.toRadians(0))
		);

		Pose keyframe11 = new Pose(
			new EulerAngle(Math.toRadians(8), Math.toRadians(323), Math.toRadians(360)),
			new EulerAngle(Math.toRadians(12), Math.toRadians(349), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(86), Math.toRadians(194), Math.toRadians(1)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(34), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(12), Math.toRadians(357)),
			new EulerAngle(Math.toRadians(297), Math.toRadians(10), Math.toRadians(0))
		);

		Pose keyframe12 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(347), Math.toRadians(360)),
			new EulerAngle(Math.toRadians(10), Math.toRadians(360), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(80), Math.toRadians(194), Math.toRadians(1)),
			new EulerAngle(Math.toRadians(317), Math.toRadians(34), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(291), Math.toRadians(12), Math.toRadians(357)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(10), Math.toRadians(0))
		);

		Pose keyframe13 = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(347), Math.toRadians(360)),
			new EulerAngle(Math.toRadians(10), Math.toRadians(360), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(80), Math.toRadians(239), Math.toRadians(1)),
			new EulerAngle(Math.toRadians(263), Math.toRadians(295), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(331), Math.toRadians(12), Math.toRadians(21)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(0), Math.toRadians(339))
		);

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						copyPos(grave, keyframe1);
						break;
					case 1:
						posToPos(keyframe1, keyframe2, grave, 6);
						break;
					case 7:
						posToPos(keyframe2, keyframe3, grave, 6);
						break;
					case 14:
						posToPos(keyframe3, keyframe4, grave, 6);
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
					case 49:
						posToPos(keyframe8, keyframe9, grave, 6);
						break;
					case 56:
						posToPos(keyframe9, keyframe10, grave, 6);
						break;
					case 63:
						posToPos(keyframe10, keyframe11, grave, 6);
						break;
					case 70:
						posToPos(keyframe11, keyframe12, grave, 6);
						break;
					case 77:
						posToPos(keyframe12, keyframe13, grave, 6);
						break;
					default:
						if (mTicks > 84) {
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

}
