package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class TakeTheLPose implements GravePose {

	public static final String NAME = "Take The L";

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(351), Math.toRadians(14), Math.toRadians(0));
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(12), Math.toRadians(345), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(337), Math.toRadians(0), Math.toRadians(277));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(357), Math.toRadians(0), Math.toRadians(15));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(225), Math.toRadians(45), Math.toRadians(0));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(337), Math.toRadians(359), Math.toRadians(321));
	}

	@Override
	public void playAnimation(ArmorStand grave, Player player) {
		Pose startKeyframe = new Pose(
			new EulerAngle(Math.toRadians(351), Math.toRadians(14), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(225), Math.toRadians(45), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(337), Math.toRadians(360), Math.toRadians(321)),
			new EulerAngle(Math.toRadians(360), Math.toRadians(0), Math.toRadians(351)),
			new EulerAngle(Math.toRadians(347), Math.toRadians(0), Math.toRadians(76))
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
					case 0, 20:
						posToPos(startKeyframe, endKeyframe, grave, 5);
						grave.teleport(grave.getLocation().add(0, 0.25, 0));
						break;
					case 5, 15:
						grave.teleport(grave.getLocation().add(0, -0.25, 0));
						break;
					case 10:
						posToPos(endKeyframe, startKeyframe, grave, 5);
						grave.teleport(grave.getLocation().add(0, 0.25, 0));
						break;
					case 25:
						grave.teleport(grave.getLocation().add(0, -0.25, 0));
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
