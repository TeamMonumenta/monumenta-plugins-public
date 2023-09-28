package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;



public class EntombFinisher implements EliteFinisher {
	public static final String NAME = "Entomb";
	private static final int DURATION = 2 * 20;
	private static final int LAYERMAX = 6;

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		new BukkitRunnable() {
			Location mLoc = killedMob.getLocation().clone();
			int mLayer = 0;
			final double mLayerHeight = killedMob.getHeight() / LAYERMAX;
			int mTicks = 0;
			@Nullable LivingEntity mClonedKilledMob;

			List<BlockDisplay> mDisplays = new ArrayList<>();
			//(float) -(killedMob.getWidth() / 2), (float) 0, (float) -(killedMob.getWidth() / 2)
			BlockDisplay mMain = createBlockDisplay(Bukkit.createBlockData(Material.SAND), new Transformation(
					new Vector3f(0, 0, 0),
					new AxisAngle4f(0, 0, 0, 0),
					new Vector3f((float) killedMob.getWidth() * 2, 0, (float) killedMob.getWidth() * 2),
					new AxisAngle4f(0, 0, 0, 0)),
				killedMob,
				new Location(killedMob.getWorld(), mLoc.getX() - killedMob.getWidth(), mLoc.getY(), mLoc.getZ() - killedMob.getWidth()));

			@Override
			public void run() {
				if (mTicks == 0) {
					// Let's let the mob freeze
					killedMob.remove();
					mClonedKilledMob = EntityUtils.copyMob((LivingEntity) killedMob);
					mClonedKilledMob.setHealth(1);
					mClonedKilledMob.setInvulnerable(true);
					mClonedKilledMob.setGravity(false);
					mClonedKilledMob.setCollidable(false);
					mClonedKilledMob.setAI(false);
					mClonedKilledMob.addScoreboardTag("SkillImmune");
				}
				if (mLayer < LAYERMAX) {
					mLayer++;
					//(float) -killedMob.getWidth(), 0, (float) -killedMob.getWidth() / 2
					BlockDisplay b = createBlockDisplay(Bukkit.createBlockData(Material.SAND),
						new Transformation(
							new Vector3f(),
							new AxisAngle4f(0, 0, 0, 0),
							new Vector3f((float) killedMob.getWidth() * 2, (float) mLayerHeight, (float) killedMob.getWidth() * 2),
							new AxisAngle4f(0, 0, 0, 0)),
						killedMob,
						new Location(killedMob.getWorld(), mLoc.getX() - killedMob.getWidth(), mLoc.getY() + 5 + killedMob.getHeight(), mLoc.getZ() - killedMob.getWidth()));
					mDisplays.add(b);
					new BukkitRunnable() {
						int mT = 0;
						BlockDisplay mCurrentDisplay = b;
						final double mDecreaseHeight = ((5 + killedMob.getHeight()) - mLayer * mLayerHeight) / 10;

						@Override
						public void run() {
							mCurrentDisplay.teleport(mCurrentDisplay.getLocation().clone().subtract(0, mDecreaseHeight, 0));
							mCurrentDisplay.setInterpolationDelay(-1);
							if (mT > 10) {
								mCurrentDisplay.getWorld().playSound(mCurrentDisplay.getLocation(), Sound.BLOCK_SAND_FALL, 1.0f, 1.0f);
								mCurrentDisplay.remove();
								mDisplays.remove(mCurrentDisplay);
								this.cancel();
							}
							mT++;
						}
					}.runTaskTimer(Plugin.getInstance(), 0, 1);

				}
				if (mTicks != 0 && !mDisplays.isEmpty()) {
					mMain.setTransformation(new Transformation(
						new Vector3f(0, 0, 0),
						new AxisAngle4f(0, 0, 0, 0),
						new Vector3f((float) killedMob.getWidth() * 2, (float) mLayerHeight * mLayer, (float) killedMob.getWidth() * 2),
						new AxisAngle4f(0, 0, 0, 0)));
				}
				if (mTicks == DURATION + 20) {
					if (mClonedKilledMob != null) {
						mClonedKilledMob.remove();
					}
				}
				if (mTicks > DURATION + 20) {
					mMain.remove();
					//temp
					for (BlockDisplay state : mDisplays) {
						state.remove();
					}
					this.cancel();
				}
				mTicks += 10;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 10);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SAND;
	}

	private BlockDisplay createBlockDisplay(BlockData data, Transformation transform, Entity e, Location l) {
		BlockDisplay display = (BlockDisplay) e.getWorld().spawnEntity(l, EntityType.BLOCK_DISPLAY);
		display.setBlock(data);
		display.setTransformation(transform);
		return display;
	}

}
