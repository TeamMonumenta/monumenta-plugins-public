package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class SinkholeFinisher implements EliteFinisher {
	public static final String NAME = "Sinkhole";
	private static final int DURATION = 3 * 20;
	private static final List<Material> MATERIALS = List.of(Material.MUD, Material.SOUL_SAND, Material.MYCELIUM, Material.DRIPSTONE_BLOCK, Material.MUDDY_MANGROVE_ROOTS, Material.PACKED_MUD, Material.ROOTED_DIRT, Material.COARSE_DIRT, Material.GRANITE);
	private static final List<Material> MATERIALS_WITH_FLORA = List.of(Material.MUD, Material.MYCELIUM, Material.ROOTED_DIRT, Material.SOUL_SAND, Material.DRIPSTONE_BLOCK, Material.PACKED_MUD);

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		if (!(killedMob instanceof LivingEntity le)) {
			return;
		}

		final Location mLoc = killedMob.getLocation().clone();
		Location antiDivByZero = mLoc.clone();
		antiDivByZero.setPitch(0);
		final Vector mDir = antiDivByZero.getDirection();
		final double mWidth = killedMob.getWidth();
		final double mHeight = killedMob.getHeight();

		new BukkitRunnable() {
			int mTicks = 0;
			@Nullable LivingEntity mClonedKilledMob;

			final List<ItemDisplay> mMudDisplays = new ArrayList<>();
			final List<@Nullable BlockDisplay> mFloraDisplays = new ArrayList<>();
			final ItemDisplay mHole = createMudDisplay(Material.BLACK_CONCRETE, mLoc);

			@Override
			public void run() {
				double sineWaveOffset = Math.sin(Math.toRadians(mTicks * 4.5));
				double sineWaveOffsetPlus10t = Math.sin(Math.toRadians((10 + mTicks) * 4.5));
				double sineWaveOffsetPlus15t = Math.sin(Math.toRadians((15 + mTicks) * 4.5));
				if (mTicks == 0) {
					killedMob.remove();
					mClonedKilledMob = EliteFinishers.createClonedMob(le, p, NamedTextColor.DARK_GREEN, false, false, true);

					new PartialParticle(Particle.SMOKE_NORMAL, mLoc).count(25).delta(0.2).extra(0.15).spawnAsPlayerActive(p);
					mLoc.getWorld().playSound(mLoc, Sound.BLOCK_SCULK_BREAK, SoundCategory.PLAYERS, 1.6f, 0.5f);
					mLoc.getWorld().playSound(mLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.3f, 0.6f);
					mLoc.getWorld().playSound(mLoc, Sound.ENTITY_HUSK_AMBIENT, SoundCategory.PLAYERS, 1.2f, 0.8f);
					mLoc.getWorld().playSound(mLoc, Sound.ENTITY_WARDEN_DIG, SoundCategory.PLAYERS, 1f, 1.25f);

					for (int i = 0; i < 10 * (0.8 + Math.min(0.6, mWidth)); i++) {
						final ItemDisplay mudDisplay = createMudDisplay(MATERIALS.get(FastUtils.randomIntInRange(0, MATERIALS.size() - 1)), mLoc);
						// Random tilt
						mudDisplay.setInterpolationDelay(FastUtils.randomIntInRange(0, 5));
						// Random offset
						mudDisplay.setInterpolationDuration(FastUtils.randomIntInRange(0, 5));
						// Random size
						mudDisplay.setFallDistance(FastUtils.randomIntInRange(0, 3));

						float size = 3f + 0.25f * mudDisplay.getFallDistance();
						mudDisplay.setTransformation(new Transformation(
							new Vector3f(0, 0, 0),
							new AxisAngle4f(0, 0, 0, 0),
							new Vector3f(size, size, size),
							new AxisAngle4f(0, 0, 0, 0)));

						mMudDisplays.add(mudDisplay);

						// Spawn flowers/mushrooms on a selection of blocks, with large scale (about 1 in 4)
						if (mudDisplay.getItemStack() != null && MATERIALS_WITH_FLORA.contains(mudDisplay.getItemStack().getType()) && mudDisplay.getFallDistance() >= 3) {
							mFloraDisplays.add(createFloraDisplay(mudDisplay.getItemStack().getType(), mLoc, size));
						} else {
							mFloraDisplays.add(null);
						}
					}
				}

				new PPCircle(Particle.BLOCK_CRACK, mLoc, 1.2 + Math.min(0.6, mWidth) * sineWaveOffset).data(Material.PACKED_MUD.createBlockData()).countPerMeter(0.45).spawnAsPlayerActive(p);

				if (mTicks >= 10 && mTicks < DURATION - 30 && mClonedKilledMob != null) {
					mClonedKilledMob.teleport(mLoc.clone().add(0, (1 + mHeight) * sineWaveOffsetPlus10t - (1 + mHeight), 0));
				}

				float holeSize = (float) ((0.8 + Math.min(0.6, mWidth) * sineWaveOffset) * 8);
				mHole.teleport(mLoc.clone().add(0, 0.05, 0).setDirection(mDir));
				mHole.setTransformation(new Transformation(
					new Vector3f(0, 0, 0),
					new AxisAngle4f(0, 0, 0, 0),
					new Vector3f(holeSize, 0, holeSize),
					new AxisAngle4f(0, 0, 0, 0)));

				for (int i = 0; i < mMudDisplays.size(); i++) {
					ItemDisplay display = mMudDisplays.get(i);
					double yawIncrement = (double) 360 / mMudDisplays.size();
					Vector vec = VectorUtils.rotateTargetDirection(mDir, i * yawIncrement, 0).clone().multiply(0.8 + Math.min(0.6, mWidth) * sineWaveOffset + 0.03 * display.getInterpolationDuration());
					Location displayLoc = mLoc.clone().subtract(0, 0.7, 0).add(vec);
					if (mTicks > DURATION - 15) {
						displayLoc.subtract(0, 1 + sineWaveOffsetPlus15t, 0);
					}
					vec.setY(-0.15 - 0.15 * sineWaveOffset - 0.02 * display.getInterpolationDelay());
					display.teleport(displayLoc.clone().setDirection(vec));

					@Nullable BlockDisplay floraDisplay = mFloraDisplays.get(i);
					if (floraDisplay != null) {
						floraDisplay.teleport(display.getLocation());
					}
				}

				if (mTicks == DURATION - 30) {
					if (mClonedKilledMob != null) {
						mClonedKilledMob.remove();
					}
				}
				if (mTicks >= DURATION) {
					mHole.remove();
					for (ItemDisplay display : mMudDisplays) {
						display.remove();
					}
					for (BlockDisplay display : mFloraDisplays) {
						if (display != null) {
							display.remove();
						}
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.MUD;
	}

	private ItemDisplay createMudDisplay(Material material, Location loc) {
		ItemDisplay display = loc.getWorld().spawn(loc.clone().add(0, 0.25, 0), ItemDisplay.class);
		display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
		display.setItemStack(new ItemStack(material));
		EntityUtils.setRemoveEntityOnUnload(display);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), display::remove, DURATION);
		display.setBrightness(new Display.Brightness(12, 12));
		return display;
	}

	private BlockDisplay createFloraDisplay(Material mudMaterial, Location loc, float mudSize) {
		BlockDisplay display = loc.getWorld().spawn(loc.clone().add(0, 0.25, 0), BlockDisplay.class);
		display.setTransformation(new Transformation(
			new Vector3f(-0.125f * mudSize, 0.3125f * mudSize, -0.125f * mudSize),
			new AxisAngle4f(0, 0, 0, 0),
			new Vector3f(0.25f * mudSize, 0.25f * mudSize, 0.25f * mudSize),
			new AxisAngle4f(0, 0, 0, 0)));
		switch (mudMaterial) {
			case MUD, SOUL_SAND -> display.setBlock(Material.WITHER_ROSE.createBlockData());
			case MYCELIUM, DRIPSTONE_BLOCK -> display.setBlock(Material.BROWN_MUSHROOM.createBlockData());
			case ROOTED_DIRT, PACKED_MUD -> display.setBlock(Material.RED_MUSHROOM.createBlockData());
			default -> {
			}
		}
		EntityUtils.setRemoveEntityOnUnload(display);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), display::remove, DURATION);
		display.setBrightness(new Display.Brightness(12, 12));
		return display;
	}
}
