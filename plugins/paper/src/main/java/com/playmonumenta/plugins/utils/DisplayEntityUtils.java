package com.playmonumenta.plugins.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.playmonumenta.plugins.Plugin;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DisplayEntityUtils {

	public static BukkitTask groundBlockQuake(Location center, double radius, List<Material> possibleMaterials, @Nullable Display.Brightness brightness) {
		return groundBlockQuake(center, radius, possibleMaterials, brightness, 0.06);
	}

	public static BukkitTask groundBlockQuake(Location center, double radius, List<Material> possibleMaterials, @Nullable Display.Brightness brightness, double blockDensity) {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Map<Integer, ArrayList<Location>> mLocationDelays = new HashMap<>();
			final List<BlockDisplay> mAllDisplays = new ArrayList<>();

			@Override
			public void run() {
				for (int currRadius = 1; currRadius <= radius; currRadius++) {
					for (int blockCounter = 0; blockCounter < currRadius * 2 * Math.PI * blockDensity; blockCounter++) {
						int appearDelay = FastUtils.randomIntInRange(0, 3) * 2 + currRadius;
						double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);
						double finalRadius = currRadius + FastUtils.randomDoubleInRange(-0.5, 0.5);
						Location finalBlockLocation = center.clone().add(FastUtils.cos(theta) * finalRadius, 0, FastUtils.sin(theta) * finalRadius).toCenterLocation().add(0, -1.4, 0);
						ArrayList<Location> blocksThisTick = mLocationDelays.computeIfAbsent(appearDelay, key -> new ArrayList<>());
						if (!blocksThisTick.contains(finalBlockLocation)) {
							blocksThisTick.add(finalBlockLocation);
						}
					}
				}

				if (mLocationDelays.containsKey(mTicks)) {
					mLocationDelays.get(mTicks).forEach(l -> {
						BlockDisplay blockDisplay = center.getWorld().spawn(l.clone().add(-0.5, -0.3, -0.5), BlockDisplay.class);
						blockDisplay.setBlock(possibleMaterials.get(FastUtils.randomIntInRange(0, possibleMaterials.size() - 1)).createBlockData());
						if (brightness != null) {
							blockDisplay.setBrightness(new Display.Brightness(15, 15));
						}
						blockDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf()));
						blockDisplay.setInterpolationDuration(2);
						mAllDisplays.add(blockDisplay);

						BukkitRunnable runnable = new BukkitRunnable() {
							int mTicks = 0;
							final double mMaxHeight = FastUtils.randomDoubleInRange(0.5, 0.8);

							@Override
							public void run() {
								double currentHeight = mMaxHeight * (-0.04 * ((mTicks - 5) * (mTicks - 5)) + 1);
								blockDisplay.setTransformation(new Transformation(new Vector3f(0, (float) currentHeight, 0), blockDisplay.getTransformation().getLeftRotation(), blockDisplay.getTransformation().getScale(), blockDisplay.getTransformation().getRightRotation()));
								blockDisplay.setInterpolationDelay(-1);

								mTicks++;
								if (mTicks > 10) {
									this.cancel();
								}
							}

							@Override
							public synchronized void cancel() throws IllegalStateException {
								super.cancel();

								blockDisplay.remove();
							}
						};
						runnable.runTaskTimer(Plugin.getInstance(), 0, 1);
					});
				}

				mTicks++;
				if (mTicks > (radius + 1) * 2 + 8) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				mAllDisplays.forEach(Entity::remove);
			}
		};
		return runnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public static ArrayList<BlockDisplay> turnBlockCuboidIntoBlockDisplays(Location corner1, Location corner2) {
		return turnBlockCuboidIntoBlockDisplays(corner1, corner2, false);
	}

	public static ArrayList<BlockDisplay> turnBlockCuboidIntoBlockDisplays(Location corner1, Location corner2, boolean ignoreBarriersBedrock) {
		ArrayList<BlockDisplay> blockDisplays = new ArrayList<>();

		int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
		int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
		int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
		int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
		int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
		int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Location blockLoc = corner1.clone().set(x, y, z);

					if (ignoreBarriersBedrock && (blockLoc.getBlock().getType() == Material.BARRIER || blockLoc.getBlock().getType() == Material.BEDROCK)) {
						continue;
					}

					BlockDisplay display = turnBlockIntoBlockDisplay(blockLoc);
					if (display != null) {
						blockDisplays.add(display);
					}
				}
			}
		}

		return blockDisplays;
	}

	public static void turnBlockDisplayCuboidIntoBlocks(Location corner1, Location corner2) {
		int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
		int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
		int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
		int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
		int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
		int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

		new Hitbox.AABBHitbox(corner1.getWorld(), BoundingBox.of(corner1.getBlock(), corner2.getBlock()).expand(2))
			.getHitEntitiesByClass(BlockDisplay.class)
			.stream().filter(display -> {
				Location loc = display.getLocation();
				double x = loc.getBlockX();
				double y = loc.getBlockY();
				double z = loc.getBlockZ();
				return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
			})
			.forEach(DisplayEntityUtils::turnBlockDisplayIntoBlock);
	}

	public static @Nullable BlockDisplay turnBlockIntoBlockDisplay(Location loc) {
		Location blockLoc = loc.clone().set(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		Entity entity = blockLoc.getWorld().spawnEntity(blockLoc, EntityType.BLOCK_DISPLAY);

		if (entity instanceof BlockDisplay display) {
			display.setBlock(blockLoc.getBlock().getBlockData());
			blockLoc.getBlock().setBlockData(Material.AIR.createBlockData());
			return display;
		}
		return null;
	}

	public static void turnBlockDisplaysIntoBlocks(List<BlockDisplay> displays) {
		for (BlockDisplay blockDisplay : displays) {
			turnBlockDisplayIntoBlock(blockDisplay);
		}
	}

	public static void turnBlockDisplayIntoBlock(BlockDisplay blockDisplay) {
		blockDisplay.getLocation().getBlock().setBlockData(blockDisplay.getBlock());
		blockDisplay.remove();
	}

	public static void translate(BlockDisplay blockDisplay, float x, float y, float z, int duration) {
		blockDisplay.setInterpolationDelay(-1);
		blockDisplay.setInterpolationDuration(duration);
		blockDisplay.setTransformation(new Transformation(new Vector3f(x, y, z), new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf()));
	}

	public static void translate(List<BlockDisplay> blockDisplays, float x, float y, float z, int duration) {
		blockDisplays.forEach(display -> translate(display, x, y, z, duration));
	}

	public static @Nullable Display spawnCopyOfDisplay(Display display, Location loc) {
		if (display instanceof BlockDisplay blockDisplay) {
			return spawnCopyOfBlockDisplay(blockDisplay, loc);
		}
		if (display instanceof TextDisplay textDisplay) {
			return spawnCopyOfTextDisplay(textDisplay, loc);
		}
		if (display instanceof ItemDisplay itemDisplay) {
			return spawnCopyOfItemDisplay(itemDisplay, loc);
		}
		return null;
	}

	public static @Nullable BlockDisplay spawnCopyOfBlockDisplay(BlockDisplay display, Location loc) {
		Entity e = display.getWorld().spawnEntity(loc, EntityType.BLOCK_DISPLAY);
		if (e instanceof BlockDisplay blockDisplayCopy) {
			blockDisplayCopy.setBlock(display.getBlock());
			blockDisplayCopy.setTransformation(new Transformation(new Vector3f(), display.getTransformation().getLeftRotation(), display.getTransformation().getScale(), display.getTransformation().getRightRotation()));
			blockDisplayCopy.setBrightness(display.getBrightness());
			blockDisplayCopy.setBillboard(display.getBillboard());
			blockDisplayCopy.setDisplayHeight(display.getDisplayHeight());
			blockDisplayCopy.setDisplayWidth(display.getDisplayWidth());
			return blockDisplayCopy;
		}
		return null;
	}

	public static @Nullable TextDisplay spawnCopyOfTextDisplay(TextDisplay display, Location loc) {
		Entity e = display.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
		if (e instanceof TextDisplay textDisplayCopy) {
			textDisplayCopy.text(display.text());
			textDisplayCopy.setTransformation(new Transformation(new Vector3f(), display.getTransformation().getLeftRotation(), display.getTransformation().getScale(), display.getTransformation().getRightRotation()));
			textDisplayCopy.setAlignment(display.getAlignment());
			textDisplayCopy.setTextOpacity(display.getTextOpacity());
			textDisplayCopy.setDisplayHeight(display.getDisplayHeight());
			textDisplayCopy.setDisplayWidth(display.getDisplayWidth());
			textDisplayCopy.setDefaultBackground(display.isDefaultBackground());
			textDisplayCopy.setLineWidth(display.getLineWidth());
			textDisplayCopy.setSeeThrough(display.isSeeThrough());
			textDisplayCopy.setShadowed(display.isShadowed());
			textDisplayCopy.setBrightness(display.getBrightness());
			textDisplayCopy.setBillboard(display.getBillboard());
			return textDisplayCopy;
		}
		return null;
	}

	public static @Nullable ItemDisplay spawnCopyOfItemDisplay(ItemDisplay display, Location loc) {
		Entity e = display.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
		if (e instanceof ItemDisplay itemDisplayCopy) {
			itemDisplayCopy.setItemStack(display.getItemStack());
			itemDisplayCopy.setTransformation(new Transformation(new Vector3f(), display.getTransformation().getLeftRotation(), display.getTransformation().getScale(), display.getTransformation().getRightRotation()));
			itemDisplayCopy.setDisplayHeight(display.getDisplayHeight());
			itemDisplayCopy.setDisplayWidth(display.getDisplayWidth());
			itemDisplayCopy.setBrightness(display.getBrightness());
			itemDisplayCopy.setBillboard(display.getBillboard());
			return itemDisplayCopy;
		}
		return null;
	}

	public static Transformation getTranslation(float x, float y, float z) {
		return new Transformation(new Vector3f(x, y, z), new Quaternionf(), new Vector3f(1), new Quaternionf());
	}

	public static Transformation getScale(float x, float y, float z) {
		return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(x, y, z), new Quaternionf());
	}

	public static Transformation getScale(float d) {
		return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(d), new Quaternionf());
	}

	public static Matrix4f quaternionToMatrix4(Quaternionf quaternion) {
		float x = quaternion.x;
		float y = quaternion.y;
		float z = quaternion.z;
		float w = quaternion.w;
		float x2 = x + x;
		float y2 = y + y;
		float z2 = z + z;

		return new Matrix4f(
			1 - y * y2 - z * z2, y * x2 + w * z2, z * x2 - w * y2, 0,
			y * x2 - w * z2, 1 - x * x2 - z * z2, z * y2 + w * x2, 0,
			z * x2 + w * y2, z * y2 - w * x2, 1 - x * x2 - y * y2, 0,
			0, 0, 0, 1
		);
	}

	public static Vector getTranslationVector(Matrix4f matrix) {
		return new Vector(matrix.m30(), matrix.m31(), matrix.m32());
	}

	public static Matrix4f transformationToMatrix4(Transformation transformation) {
		return new Matrix4f()
			.translate(transformation.getTranslation())
			.mul(quaternionToMatrix4(transformation.getLeftRotation()))
			.scale(transformation.getScale())
			.mul(quaternionToMatrix4(transformation.getRightRotation()));
	}

	public static @Nullable ItemDisplay spawnItemDisplayWithBase64Head(Location loc, String base64) {
		Entity e = loc.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
		if (e instanceof ItemDisplay itemDisplay) {
			SkullMeta meta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD);
			PlayerProfile headProfile = Bukkit.createProfile(UUID.fromString("4caebc47-f172-4e32-af6a-01bd00b61b5f"));
			headProfile.setProperty(new ProfileProperty("textures", base64));
			meta.setPlayerProfile(headProfile);
			ItemStack item = new ItemStack(Material.PLAYER_HEAD);
			item.setItemMeta(meta);
			itemDisplay.setItemStack(item);
			return itemDisplay;
		}
		return null;
	}

	public static class DisplayAnimation {
		private final ArrayList<Matrix4f> mFrames = new ArrayList<>();
		private final ArrayList<Integer> mDurations = new ArrayList<>();
		private ArrayList<Display> mDisplays = new ArrayList<>();
		private Matrix4f mCancelFrame;
		private Integer mCancelDuration;
		private Integer mCancelDelay;
		private boolean mRemoveDisplaysAfterwards = false;
		private boolean mCancel = false;


		public DisplayAnimation(Display display) {
			mDisplays.add(display);
		}

		public DisplayAnimation(List<Display> displays) {
			mDisplays.addAll(displays);
		}

		public DisplayAnimation addKeyframe(Transformation frame, int duration) {
			mFrames.add(transformationToMatrix4(frame));
			mDurations.add(duration);
			return this;
		}

		public DisplayAnimation addKeyframe(Matrix4f frame, int duration) {
			mFrames.add(frame);
			mDurations.add(duration);
			return this;
		}

		public DisplayAnimation addDelay(int amount) {
			mFrames.add(null);
			mDurations.add(amount);
			return this;
		}

		public DisplayAnimation addCancelFrame(Transformation frame, int duration) {
			mCancelFrame = transformationToMatrix4(frame);
			mCancelDuration = duration;
			return this;
		}

		public DisplayAnimation addCancelFrame(Matrix4f frame, int duration) {
			mCancelFrame = frame;
			mCancelDuration = duration;
			return this;
		}

		public DisplayAnimation addCancelDelay(int amount) {
			mCancelDelay = amount;
			return this;
		}

		public DisplayAnimation removeDisplaysAfterwards() {
			mRemoveDisplaysAfterwards = true;
			return this;
		}

		public DisplayAnimation cancel() {
			mCancel = true;
			return this;
		}

		public void play() {
			playInternal(0);
		}

		private void playInternal(int currentFrame) {
			if (currentFrame != 0) {
				Matrix4f previousFrame = mFrames.get(currentFrame - 1);
				if (previousFrame != null) {
					ArrayList<Display> teleportedDisplays = new ArrayList<>();
					mDisplays.forEach(display -> {
						teleportedDisplays.add(spawnCopyOfDisplay(display, display.getLocation().add(getTranslationVector(previousFrame))));
						display.remove();

					});
					mDisplays = teleportedDisplays;
				}
			}

			if (mCancel) {
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					if (mCancelFrame != null) {
						mDisplays.forEach(display -> {
							display.setInterpolationDelay(-1);
							display.setInterpolationDuration(mCancelDuration);
							display.setTransformationMatrix(mCancelFrame);
						});
					}
					mCancel = false;
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> playInternal(mFrames.size()), mCancelDuration + mCancelDelay);
				}, 1);
				return;
			}

			if (currentFrame >= mFrames.size()) {
				if (mRemoveDisplaysAfterwards) {
					mDisplays.forEach(Entity::remove);
				}
				return;
			}

			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				Matrix4f frame = mFrames.get(currentFrame);
				if (frame != null) {
					mDisplays.forEach(display -> {
						display.setInterpolationDelay(-1);
						display.setInterpolationDuration(mDurations.get(currentFrame));
						display.setTransformationMatrix(frame);
					});
				}
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> playInternal(currentFrame + 1), mDurations.get(currentFrame));
			}, 1);
		}
	}

	/**
	 * Generates an item stack for itemdisplays that will show the rp texture
	 *
	 * @param baseItem base item in the rp
	 * @param itemName item name in the rp
	 * @return Item stack with correctly edited meta
	 */
	public static ItemStack generateRPItem(Material baseItem, String itemName) {
		ItemStack stack = new ItemStack(baseItem);
		ItemMeta meta = stack.getItemMeta();
		meta.displayName(Component.text(itemName));
		stack.setItemMeta(meta);
		ItemUtils.setPlainTag(stack);
		return stack;
	}

	public static void rotateAroundDisplayLocation(Display display, double radius, int ticksPerRotation) {
		rotateAroundLocation(display, radius, ticksPerRotation, new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), ticksPerRotation);
	}

	public static void rotateAroundLocation(Display display, double radius, int ticksPerRotation, Vector3f leftAxis, Vector3f rightAxis, int duration) {
		display.setInterpolationDuration(2);

		new BukkitRunnable() {
			int mTicks = 0;
			float mAngle = 0;
			final float mIncriment = (float) ((Math.PI * 2) / ticksPerRotation);

			@Override
			public void run() {
				rotateAround(display, radius, mAngle, leftAxis, rightAxis);
				if (mTicks >= duration) {
					this.cancel();
				}
				mAngle += mIncriment;
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public static void rotateAroundEntityLocation(Entity entity, Display display, double radius, int ticksPerRotation) {
		rotateAroundEntityLocation(entity, display, radius, ticksPerRotation, new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), ticksPerRotation);
	}

	public static void rotateAroundEntityLocation(Entity entity, Display display, double radius, int ticksPerRotation, Vector3f leftAxis, Vector3f rightAxis, int duration) {
		display.setInterpolationDuration(2);

		new BukkitRunnable() {
			int mTicks = 0;
			float mAngle = 0;
			final float mIncriment = (float) ((Math.PI * 2) / ticksPerRotation);

			@Override
			public void run() {
				display.teleport(entity.getLocation().setDirection(new Vector(1, 0, 0)));
				rotateAround(display, radius, mAngle, leftAxis, rightAxis);
				if (mTicks >= duration) {
					this.cancel();
				}
				mAngle += mIncriment;
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private static void rotateAround(Display display, double radius, float angle, Vector3f leftAxis, Vector3f rightAxis) {
		display.setTransformation(new Transformation(
			new Vector3f((float) (radius * Math.cos(angle)), 0, (float) (radius * Math.sin(angle))),
			new AxisAngle4f(angle, leftAxis.x, leftAxis.y, leftAxis.z),
			display.getTransformation().getScale(),
			new AxisAngle4f(angle, rightAxis.x, rightAxis.y, rightAxis.z)));
		display.setInterpolationDelay(-1);
	}

	/**
	 * Only works for cubes and will make item displays vertical and point towards it. By default, rotates it for weapons
	 *
	 * @param display  display to be rotated
	 * @param vec      vector direction the display should be rotated.
	 *                 For looking at a loc get the vector between the two locs
	 * @param duration duration of the interpolation
	 */
	public static void rotateToPointAtLoc(Display display, Vector vec, int duration) {
		rotateToPointAtLoc(display, vec, duration, -Math.PI / 4.0f);
	}

	/**
	 * Only works for cubes and will make item displays vertical and point towards it.
	 *
	 * @param display     display to be rotated
	 * @param vec         vector direction the display should be rotated.
	 *                    For looking at a loc get the vector between the two locs
	 * @param duration    duration of the interpolation
	 * @param angleOffset the angle in radians to rotate it in additions. Cross bows would be +π/4, armor would be 0,
	 *                    and weapons would be -π/4
	 */
	public static void rotateToPointAtLoc(Display display, Vector vec, int duration, double angleOffset) {
		vec.normalize();
		display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f((float) -Math.atan2(vec.getZ(), vec.getX()), 0, 1, 0), new Vector3f(2f), new AxisAngle4f((float) (Math.asin(vec.getY()) + angleOffset), 0, 0, 1)));
		display.setInterpolationDuration(duration);
		display.setInterpolationDelay(-1);
	}

	// Ignore deprecation; API subject to change, but no replacement is provided at this time
	// This is the only way I could silence IntelliJ and the reviewdog about Bukkit's decisions
	@SuppressWarnings("deprecation")
	public static void setTextDisplayBackgroundColor(TextDisplay textDisplay, Color backgroundColor) {
		textDisplay.setBackgroundColor(backgroundColor);
	}

}
