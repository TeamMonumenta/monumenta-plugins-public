package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Math;
import org.joml.Matrix4f;

public class GrindsMyGearsFinisher implements EliteFinisher {
	public static final String NAME = "Grinds My Gears";

	private static final int EXTEND_PARTS = 2;
	private static final int EXTEND_PARTS_PER_TICK = 8;
	private static final int EXTEND_PART_SFX_DELAY_TICKS = 4;

	private static final int CONTRACT_PARTS_PER_GEAR = 2;
	private static final int CONTRACT_PARTS_TICKS = 4;
	private static final int CONTRACT_PART_SFX_DELAY_TICKS = 2;

	private static final float HIDDEN_ROTATION = 1.5f * (float) Math.PI;
	private static final float HIDDEN_HEIGHT_MULTIPLIER = -1.75f;

	private static final Matrix4f GEAR_BASE_TRANSFORM = new Matrix4f()
		.translate(-0.5f, -0.125f, -0.1875f)
		.scale(1.0f, 0.25f, 0.1875f);
	private static final Matrix4f GEAR_TOOTH_TRANSFORM = new Matrix4f()
		.translate(-0.25f, -0.09375f, -0.0625f)
		.scale(0.5f, 0.1875f, 0.5f);

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		if (!(killedMob instanceof LivingEntity le)) {
			return;
		}

		LivingEntity clonedEntity = EliteFinishers.createClonedMob(le, p, NamedTextColor.GOLD, false, false, true);
		clonedEntity.setInvulnerable(true);
		final UUID cloneId = clonedEntity.getUniqueId();
		killedMob.remove();

		Location gearSectionLoc = loc.clone();
		gearSectionLoc.setPitch(0.0f);
		gearSectionLoc.setYaw(0.0f);

		final float width = (float) killedMob.getWidth();
		final float height = (float) killedMob.getHeight();

		final float hiddenHeight = HIDDEN_HEIGHT_MULTIPLIER * height;

		final List<Gear> gears = new ArrayList<>();
		for (int gearNum = 0; gearNum < 3; gearNum++) {
			boolean isEven = (gearNum & 1) == 0;
			Matrix4f hiddenTransform = new Matrix4f()
				.scale(0.0f)
				.rotate(isEven ? HIDDEN_ROTATION : -HIDDEN_ROTATION, 0.0f, 1.0f, 0.0f)
				.translate(0.0f, hiddenHeight, 0.0f);

			Gear gear = new Gear(gearSectionLoc, 12, 0.33f * (gearNum + 1) * width);
			gear.setTransformation(hiddenTransform);
			gears.add(gear);
			gearSectionLoc.add(0.0f, 0.25 * height, 0.0f);
		}

		final int contractTotalTicks = CONTRACT_PARTS_PER_GEAR * CONTRACT_PARTS_TICKS * gears.size();
		final float contractDistancePerTick = hiddenHeight / contractTotalTicks;

		new BukkitRunnable() {
			int mPart = 0;
			int mTicks = 0;

			@Override
			public void run() {
				if (!loc.isWorldLoaded() || !loc.isChunkLoaded()) {
					cancel();
					return;
				}
				if (
					!(
						Bukkit.getEntity(cloneId) instanceof LivingEntity clonedEntity
							&& clonedEntity.isValid()
					)
				) {
					cancel();
					return;
				}

				if (mPart < gears.size()) {
					// Gears rise in turn
					Gear gear = gears.get(mPart);
					boolean isEven = (mPart & 1) == 0;

					if (mTicks % EXTEND_PARTS_PER_TICK == EXTEND_PART_SFX_DELAY_TICKS) {
						loc.getWorld().playSound(loc, Sound.BLOCK_PISTON_EXTEND, SoundCategory.PLAYERS, 0.5f, 1.0f);
					}

					int extendPartsTicks = EXTEND_PARTS * EXTEND_PARTS_PER_TICK;
					float hiddenPercent = ((float) extendPartsTicks - mTicks) / extendPartsTicks;
					Matrix4f transform = new Matrix4f()
						.scale(1.0f - hiddenPercent)
						.rotate(hiddenPercent * (isEven ? HIDDEN_ROTATION : -HIDDEN_ROTATION), 0.0f, 1.0f, 0.0f)
						.translate(0.0f, hiddenPercent * hiddenHeight, 0.0f);
					gear.setInterpolate();
					gear.setTransformation(transform);

					if (mTicks >= extendPartsTicks) {
						mPart++;
						mTicks = 0;
						return;
					}
				} else if (mPart == gears.size()) {
					// Delay
					if (mTicks >= 10) {
						mPart++;
						mTicks = 0;
						return;
					}
				} else {
					// And down they go, all at once
					float hiddenPercent = (float) mTicks / (float) contractTotalTicks;

					Location updatedLocation = clonedEntity.getLocation();
					updatedLocation.add(0.0f, contractDistancePerTick, 0.0f);
					clonedEntity.teleport(updatedLocation);

					boolean isEven = true;
					for (Gear gear : gears) {
						isEven = !isEven;
						Matrix4f transform = new Matrix4f()
							.rotate(hiddenPercent * (isEven ? HIDDEN_ROTATION : -HIDDEN_ROTATION), 0.0f, 1.0f, 0.0f)
							.translate(0.0f, hiddenPercent * hiddenHeight, 0.0f);
						gear.setInterpolate();
						gear.setTransformation(transform);
					}

					if (mTicks >= contractTotalTicks) {
						cancel();
					} else if (mTicks % CONTRACT_PARTS_TICKS == CONTRACT_PART_SFX_DELAY_TICKS) {
						updatedLocation.getWorld().playSound(updatedLocation, Sound.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 0.5f, 1.0f);
					}
				}
				mTicks++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				for (Gear gear : gears) {
					gear.remove();
				}
				gears.clear();

				if (
					Bukkit.getEntity(cloneId) instanceof LivingEntity clonedEntity
						&& clonedEntity.isValid()
				) {
					clonedEntity.remove();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0L, 1L);
	}

	@Override
	public Material getDisplayItem() {
		return Material.CUT_COPPER_STAIRS;
	}

	private static class Gear {
		private final List<GearSection> mSections = new ArrayList<>();

		public Gear(Location loc, int teeth, float scale) {
			float tau = 2.0f * (float) Math.PI;
			float rotationIncrement = tau / teeth;

			float sectionScale = 2.0f * (float) Math.PI / teeth;

			boolean isEven = true;
			float angle = 0;
			for (int toothNum = 0; toothNum < teeth; toothNum++) {
				Matrix4f sectionTransform = new Matrix4f()
					.rotate(angle, 0.0f, 1.0f, 0.0f)
					.scale(scale)
					.translate(0.0f, 0.0f, 1.0f)
					.scale((isEven ? 1.0f : 1.001f) * sectionScale);
				mSections.add(new GearSection(loc, sectionTransform));

				angle += rotationIncrement;
				isEven = !isEven;
			}
		}

		public void setInterpolate() {
			for (GearSection section : mSections) {
				section.setInterpolate();
			}
		}

		public void setTransformation(Matrix4f transform) {
			for (GearSection section : mSections) {
				section.setTransform(transform);
			}
		}

		public void remove() {
			for (GearSection section : mSections) {
				section.remove();
			}
			mSections.clear();
		}
	}

	private static class GearSection {
		private final Matrix4f mBaseTransform;
		private final UUID mBaseId;
		private final UUID mToothId;

		public GearSection(Location loc, Matrix4f transform) {
			mBaseTransform = transform;

			World world = loc.getWorld();

			BlockDisplay base = world.spawn(loc, BlockDisplay.class);
			base.setBlock(Material.COPPER_BLOCK.createBlockData());
			EntityUtils.setRemoveEntityOnUnload(base);
			mBaseId = base.getUniqueId();

			BlockDisplay tooth = world.spawn(loc, BlockDisplay.class);
			tooth.setBlock(Material.COPPER_BLOCK.createBlockData());
			EntityUtils.setRemoveEntityOnUnload(tooth);
			mToothId = tooth.getUniqueId();

			setTransform(new Matrix4f());
		}

		public void setInterpolate() {
			if (Bukkit.getEntity(mBaseId) instanceof BlockDisplay base) {
				base.setInterpolationDelay(-1);
				base.setInterpolationDuration(1);
			}

			if (Bukkit.getEntity(mToothId) instanceof BlockDisplay tooth) {
				tooth.setInterpolationDelay(-1);
				tooth.setInterpolationDuration(1);
			}
		}

		public void setTransform(Matrix4f transform) {
			if (Bukkit.getEntity(mBaseId) instanceof BlockDisplay base) {
				Matrix4f baseTransform = new Matrix4f(transform)
					.mul(mBaseTransform)
					.mul(GEAR_BASE_TRANSFORM);
				base.setTransformationMatrix(baseTransform);
			}

			if (Bukkit.getEntity(mToothId) instanceof BlockDisplay tooth) {
				Matrix4f toothTransform = new Matrix4f(transform)
					.mul(mBaseTransform)
					.mul(GEAR_TOOTH_TRANSFORM);
				tooth.setTransformationMatrix(toothTransform);
			}
		}

		public void remove() {
			if (Bukkit.getEntity(mBaseId) instanceof BlockDisplay base) {
				base.remove();
			}

			if (Bukkit.getEntity(mToothId) instanceof BlockDisplay tooth) {
				tooth.remove();
			}
		}
	}
}
