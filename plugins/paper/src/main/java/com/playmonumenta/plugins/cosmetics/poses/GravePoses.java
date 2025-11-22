package com.playmonumenta.plugins.cosmetics.poses;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GravePoses {
	private static final ImmutableMap<String, GravePose> POSES =
		ImmutableMap.<String, GravePose>builder()
			.put(BirdPose.NAME, new BirdPose())
			.put(CleaningPose.NAME, new CleaningPose())
			.put(CoolDancePose.NAME, new CoolDancePose())
			.put(DabPose.NAME, new DabPose())
			.put(FlossPose.NAME, new FlossPose())
			.put(GangnamStylePose.NAME, new GangnamStylePose())
			.put(GriddyPose.NAME, new GriddyPose())
			.put(JumpingJacksPose.NAME, new JumpingJacksPose())
			.put(NoMovementPose.NAME, new NoMovementPose())
			.put(RussianDancePose.NAME, new RussianDancePose())
			.put(SalutePose.NAME, new SalutePose())
			.put(SlidinPose.NAME, new SlidinPose())
			.put(SupermanPose.NAME, new SupermanPose())
			.put(SwingPose.NAME, new SwingPose())
			.put(SwordSpinPose.NAME, new SwordSpinPose())
			.put(TakeTheLPose.NAME, new TakeTheLPose())
			.put(TPose.NAME, new TPose())
			.put(WavePose.NAME, new WavePose())
			.build();

	public static GravePose getGravePose(@Nullable String name) {
		if (name != null) {
			GravePose pose = POSES.get(name);
			if (pose != null) {
				return pose;
			}
		}
		return new DefaultGravePose();
	}

	public static GravePose getGravePose(@Nullable Cosmetic cosmetic) {
		return getGravePose(cosmetic == null ? null : cosmetic.getName());
	}

	public static GravePose getEquippedGravePose(Player player) {
		return getGravePose(CosmeticsManager.getInstance().getActiveCosmetic(player, CosmeticType.GRAVE_POSE));
	}

	public static Material getDisplayItem(String name) {
		GravePose pose = getGravePose(name);
		return pose.getDisplayItem();
	}

	public static String[] getNames() {
		return POSES.keySet().toArray(String[]::new);
	}

	public static Set<String> getNameSet() {
		return Set.copyOf(POSES.keySet());
	}

	public static boolean canAccess(Player player) {
		return (ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_1) || player.hasPermission("group.mod");
	}

	public static void handleLogin(Player player) {
		if (canAccess(player)) {
			for (String name : getNameSet()) {
				CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.GRAVE_POSE, name);
			}
		} else {
			CosmeticsManager.getInstance().clearCosmetics(player, CosmeticType.GRAVE_POSE);
		}
	}

	public static void posToPos(GravePose.Pose startPose, GravePose.Pose endPose, ArmorStand actualStand, int ticks) {
		EulerAngle startHead = startPose.head();
		EulerAngle startBody = startPose.body();
		EulerAngle startLeftArm = startPose.leftArm();
		EulerAngle startRightArm = startPose.rightArm();
		EulerAngle startLeftLeg = startPose.leftLeg();
		EulerAngle startRightLeg = startPose.rightLeg();

		EulerAngle endHead = endPose.head();
		EulerAngle endBody = endPose.body();
		EulerAngle endLeftArm = endPose.leftArm();
		EulerAngle endRightArm = endPose.rightArm();
		EulerAngle endLeftLeg = endPose.leftLeg();
		EulerAngle endRightLeg = endPose.rightLeg();

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= ticks + 1) {
					this.cancel();
					return;
				}

				double progress = (double) mTicks / ticks;

				actualStand.setHeadPose(interpolateAngles(startHead, endHead, progress));
				actualStand.setBodyPose(interpolateAngles(startBody, endBody, progress));
				actualStand.setLeftArmPose(interpolateAngles(startLeftArm, endLeftArm, progress));
				actualStand.setRightArmPose(interpolateAngles(startRightArm, endRightArm, progress));
				actualStand.setLeftLegPose(interpolateAngles(startLeftLeg, endLeftLeg, progress));
				actualStand.setRightLegPose(interpolateAngles(startRightLeg, endRightLeg, progress));

				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private static EulerAngle interpolateAngles(EulerAngle start, EulerAngle end, double progress) {
		return new EulerAngle(
			interpolateAngle(start.getX(), end.getX(), progress),
			interpolateAngle(start.getY(), end.getY(), progress),
			interpolateAngle(start.getZ(), end.getZ(), progress)
		);
	}

	private static double interpolateAngle(double start, double end, double progress) {
		double delta = end - start;

		if (Math.abs(delta) > Math.PI) {
			if (delta > 0) {
				start += 2 * Math.PI;
			} else {
				start -= 2 * Math.PI;
			}
			delta = end - start;
		}

		return start + delta * progress;
	}

	public static void copyPos(ArmorStand grave, GravePose.Pose pose) {
		grave.setHeadPose(pose.head());
		grave.setBodyPose(pose.body());
		grave.setLeftArmPose(pose.leftArm());
		grave.setRightArmPose(pose.rightArm());
		grave.setLeftLegPose(pose.leftLeg());
		grave.setRightLegPose(pose.rightLeg());
	}


}
