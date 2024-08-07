package com.playmonumenta.plugins.cosmetics.poses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import static com.playmonumenta.plugins.cosmetics.poses.GravePoses.posToPos;

public class JumpingJacksPose implements GravePose {

	public static final String NAME = "Jumping Jacks";

	@Override
	public Material getDisplayItem() {
		return Material.CHAINMAIL_CHESTPLATE;
	}

	@Override
	public EulerAngle getLeftLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(315));
	}

	@Override
	public EulerAngle getRightLegAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(43));
	}

	@Override
	public EulerAngle getLeftArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(220));
	}

	@Override
	public EulerAngle getRightArmAngle(boolean small) {
		return new EulerAngle(Math.toRadians(0), Math.toRadians(0), Math.toRadians(138));
	}

	@Override
	public void playAnimation(ArmorStand grave, Player player) {
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

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
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
					case 20:
						posToPos(startKeyframe, endKeyframe, grave, 5);
						grave.teleport(grave.getLocation().add(0, 0.3, 0));
						break;
					default:
						if (mTicks > 25) {
							grave.teleport(grave.getLocation().add(0, -0.3, 0));
							this.cancel();
						}
						break;
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
