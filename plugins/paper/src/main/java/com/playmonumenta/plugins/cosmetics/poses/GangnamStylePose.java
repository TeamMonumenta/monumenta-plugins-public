package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class GangnamStylePose implements GravePose {

	public static final String NAME = "Gangnam Style";

	@Override
	public Material getDisplayItem() {
		return Material.DIAMOND_HORSE_ARMOR;
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(12), Math.toRadians(333), Math.toRadians(0));
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(12), Math.toRadians(331), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(24), Math.toRadians(16), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(283), Math.toRadians(6), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(321), Math.toRadians(46), Math.toRadians(360));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(335), Math.toRadians(323), Math.toRadians(335));
	}

	@Override
	public void playAnimation(ArmorStand grave, Player player) {
		Pose startKeyframe = new Pose(
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(347), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(237), Math.toRadians(28), Math.toRadians(360)),
			new EulerAngle(Math.toRadians(271), Math.toRadians(303), Math.toRadians(327)),
			new EulerAngle(Math.toRadians(50), Math.toRadians(18), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(345), Math.toRadians(26), Math.toRadians(0))
		);

		Pose endKeyframe = new Pose(
			getHeadAngle(false),
			getBodyAngle(false),
			getLeftArmAngle(false),
			getRightArmAngle(false),
			getLeftLegAngle(false),
			getRightLegAngle(false)
		);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0, 10, 20:
						posToPos(endKeyframe, startKeyframe, grave, 5);
						grave.teleport(grave.getLocation().add(0, 0.25, 0));
						break;
					case 5, 15, 25:
						posToPos(startKeyframe, endKeyframe, grave, 5);
						grave.teleport(grave.getLocation().add(0, -0.25, 0));
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
