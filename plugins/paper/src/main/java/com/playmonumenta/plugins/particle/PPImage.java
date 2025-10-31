package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.colors.BlendMode;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Spawns a particle image by parsing a MCFunction file generated with the ParticleConverter program.
 */
public class PPImage extends AbstractPartialParticle<PPImage> {

	public static final String IMAGE_FOLDER_NAME = "images";

	private String mImagePath;
	private final ArrayList<ImageParticle> mImageParticles = new ArrayList<>();
	private boolean mShouldResize = false;
	private int mWidth = 32;
	private int mHeight = 32;
	private boolean mCentered = false;
	private Vector mAxis1 = new Vector(1, 0, 0);
	private Vector mAxis2 = new Vector(0, 1, 0);
	private @Nullable BlendInfo mBlendInfo = null;

	// Draw 1 pixel every mResolution pixels
	private int mResolution = 1;
	private boolean mShouldRegenerateParticles = true;

	public PPImage(Location loc, String imagePath) {
		super(Particle.REDSTONE, loc);
		mImagePath = imagePath;
	}

	@Override
	public PPImage copy() {
		return copy(new PPImage(mLocation.clone(), mImagePath));
	}

	@Override
	public PPImage copy(PPImage copy) {
		super.copy(copy);
		copy.mImagePath = mImagePath;
		copy.mImageParticles.addAll(mImageParticles);
		copy.mShouldResize = mShouldResize;
		copy.mWidth = mWidth;
		copy.mHeight = mHeight;
		copy.mCentered = mCentered;
		copy.mAxis1 = mAxis1.clone();
		copy.mAxis2 = mAxis2.clone();
		copy.mBlendInfo = mBlendInfo;
		copy.mResolution = mResolution;
		copy.mShouldRegenerateParticles = mShouldRegenerateParticles;
		return copy;
	}

	public PPImage imagePath(String imagePath) {
		mImagePath = imagePath;
		mShouldRegenerateParticles = true;
		return this;
	}

	public PPImage dimensions(int width, int height) {
		mWidth = width;
		mHeight = height;
		mShouldResize = true;
		mShouldRegenerateParticles = true;
		return this;
	}

	public PPImage centered(boolean centered) {
		mCentered = centered;
		mShouldRegenerateParticles = true;
		return this;
	}

	public PPImage axes(Vector axis1, Vector axis2) {
		mAxis1 = axis1.normalize();
		mAxis2 = axis2.normalize();
		mShouldRegenerateParticles = true;
		return this;
	}

	public PPImage normal(Vector normal) {
		Vector normalized = normal.normalize();
		double[] rotation = VectorUtils.vectorToRotation(normalized);
		mAxis2 = VectorUtils.rotationToVector(rotation[0], rotation[1] - 90).normalize();
		mAxis1 = mAxis2.clone().crossProduct(normalized.clone());
		mShouldRegenerateParticles = true;
		return this;
	}

	public PPImage blendColor(BlendMode blendMode, Color foregroundColor) {
		mBlendInfo = new BlendInfo(blendMode, foregroundColor);
		mShouldRegenerateParticles = true;
		return this;
	}

	private void populateDustOptions() {
		Path finalPath = Path.of(Plugin.getInstance().getDataFolder().getPath(), IMAGE_FOLDER_NAME, mImagePath);

		FileUtils.SimpleImage image;
		try {
			image = new FileUtils.SimpleImage(finalPath);
		} catch (Exception e) {
			MMLog.warning("Tried creating a PPImage with path " + finalPath + " but file was not found.");
			return;
		}

		if (mShouldResize) {
			image.resize(mWidth, mHeight);
		}

		mImageParticles.clear();
		int[][] matrix = image.getARGBMatrix();
		double halfBlockWidthX = image.getWidth() * 0.125 * 0.5;
		double halfBlockWidthY = image.getHeight() * 0.125 * 0.5;
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				double xCoord = mCentered ? halfBlockWidthX + x * -0.125 : x * -0.125;
				double yCoord = mCentered ? y * 0.125 - halfBlockWidthY : y * 0.125;
				double zCoord = 0;

				mImageParticles.add(new ImageParticle(matrix[image.getWidth() - 1 - x][image.getHeight() - 1 - y], 1, xCoord, yCoord, zCoord, mAxis1, mAxis2, mBlendInfo));
			}
		}
		mImageParticles.removeIf(imageParticle -> imageParticle.getAlpha() < 128);
	}

	@Override
	protected int getPartialCount(double multiplier, Player player, ParticleCategory source) {
		mResolution = multiplier <= 0.25 ? 4 : multiplier <= 0.5 ? 2 : 1;
		return 1;
	}

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		if (mShouldRegenerateParticles) {
			populateDustOptions();
			mShouldRegenerateParticles = false;
		}

		Location loc = mLocation.clone();
		packagedValues.count(1);
		int mParticleCounter = 1;
		for (ImageParticle particle : mImageParticles) {
			if (mParticleCounter == mResolution) {
				packagedValues.location(particle.getRelativeLocation(loc));
				packagedValues.data(particle.getDustOptions());
				spawnUsingSettings(packagedValues);
				mParticleCounter = 1;
			} else {
				mParticleCounter++;
			}
		}
	}

	private record BlendInfo(BlendMode mBlendMode, Color mColor) {

		public BlendMode getBlendMode() {
			return mBlendMode;
		}

		public Color getColor() {
			return mColor;
		}
	}

	private static class ImageParticle {
		private final Particle.DustOptions mDustOptions;
		private final Vector mRelativePosition;
		private final Vector mAxis1;
		private final Vector mAxis2;

		public ImageParticle(int argb, float size, double rX, double rY, double rZ, Vector axis1, Vector axis2, @Nullable BlendInfo blendInfo) {
			Particle.DustOptions options = new Particle.DustOptions(Color.fromARGB(argb), size);
			if (blendInfo != null && options.getColor().getAlpha() > 127) {
				mDustOptions = new Particle.DustOptions(blendInfo.getBlendMode().apply(Color.fromARGB(argb), blendInfo.getColor()), 1);
			} else {
				mDustOptions = options;
			}
			mRelativePosition = new Vector(rX, rY, rZ);
			mAxis1 = axis1;
			mAxis2 = axis2;
		}

		public Location getRelativeLocation(Location from) {
			Vector normal = mAxis1.clone().crossProduct(mAxis2.clone());
			Vector rotatedRelativePosition = new Vector(
				mRelativePosition.getX() * mAxis1.getX() + mRelativePosition.getY() * mAxis2.getX() + mRelativePosition.getZ() * normal.getX(),
				mRelativePosition.getX() * mAxis1.getY() + mRelativePosition.getY() * mAxis2.getY() + mRelativePosition.getZ() * normal.getY(),
				mRelativePosition.getX() * mAxis1.getZ() + mRelativePosition.getY() * mAxis2.getZ() + mRelativePosition.getZ() * normal.getZ()
			);
			return from.clone().add(rotatedRelativePosition);
		}

		public Particle.DustOptions getDustOptions() {
			return mDustOptions;
		}

		public int getAlpha() {
			return mDustOptions.getColor().getAlpha();
		}
	}

}
