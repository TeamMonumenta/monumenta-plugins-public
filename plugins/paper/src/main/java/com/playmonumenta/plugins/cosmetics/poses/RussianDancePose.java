package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class RussianDancePose implements GravePose {

	public static final String NAME = "Russian Dance";

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_LEGGINGS;
	}

	@Override
	public EulerAngle getHeadAngle(boolean small) {
		return new EulerAngle(Math.toRadians(16), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getBodyAngle(boolean small) {
		return new EulerAngle(Math.toRadians(16), Math.toRadians(0), Math.toRadians(0));
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(287), Math.toRadians(0), Math.toRadians(353));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(10), Math.toRadians(0), Math.toRadians(3));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(333), Math.toRadians(0), Math.toRadians(64));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(335), Math.toRadians(0), Math.toRadians(297));
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
			new EulerAngle(Math.toRadians(16), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(16), Math.toRadians(0), Math.toRadians(0)),
			new EulerAngle(Math.toRadians(333), Math.toRadians(0), Math.toRadians(228)),
			new EulerAngle(Math.toRadians(335), Math.toRadians(0), Math.toRadians(132)),
			new EulerAngle(Math.toRadians(12), Math.toRadians(0), Math.toRadians(353)),
			new EulerAngle(Math.toRadians(289), Math.toRadians(0), Math.toRadians(3))
		);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0, 20, 40, 60:
						grave.teleport(grave.getLocation().add(0, 0.2, 0));
						posToPos(keyframe1, keyframe2, grave, 5);
						break;
					case 10, 30, 50:
						grave.teleport(grave.getLocation().add(0, 0.2, 0));
						posToPos(keyframe2, keyframe1, grave, 5);
						break;
					case 5, 15, 25, 35, 45, 55, 65:
						grave.teleport(grave.getLocation().add(0, -0.2, 0));
						break;
					case 70:
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
