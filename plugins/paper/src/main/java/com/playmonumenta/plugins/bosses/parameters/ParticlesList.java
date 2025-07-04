package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ParticlesList {
	public static class CParticle {
		public Particle mParticle;
		public int mCount;
		public double mDx;
		public double mDy;
		public double mDz;
		public double mVelocity;
		public @Nullable Object mExtra2; //used when we have a particle that is inside PARTICLE_MATERIALS or Particle.REDSTONE

		public CParticle(Particle particle) {
			this(particle, 1, 0, 0, 0);
		}

		public CParticle(Particle particle, int count) {
			this(particle, count, 0, 0, 0);
		}

		public CParticle(Particle particle, int count, double dx, double dy, double dz) {
			this(particle, count, dx, dy, dz, 0.0d);
		}

		public CParticle(Particle particle, int count, double dx, double dy, double dz, double extra1) {
			this(particle, count, dx, dy, dz, extra1, null);
		}

		public CParticle(Particle particle, int count, double dx, double dy, double dz, double extra1, @Nullable Object extra2) {
			mParticle = particle;
			mCount = count;
			mDx = dx;
			mDy = dy;
			mDz = dz;
			mVelocity = extra1;
			mExtra2 = extra2;
		}

		@Override
		public String toString() {
			if (mExtra2 != null && mExtra2 instanceof DustOptions) {
				String color = "#" + Integer.toHexString(((DustOptions) mExtra2).getColor().asRGB());
				String size = Float.toString(((DustOptions) mExtra2).getSize());
				return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + "," + color + "," + size + ")";
			} else if (mExtra2 != null && mExtra2 instanceof BlockData) {
				return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + "," + ((BlockData) mExtra2).getMaterial().name() + ")";
			} else if (mExtra2 != null && mExtra2 instanceof ItemStack) {
				return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + "," + ((ItemStack) mExtra2).getType().name() + ")";
			} else if (mExtra2 != null) {
				String ret = "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + "," + mExtra2 + ")";
				Plugin.getInstance().getLogger().warning("Got strange particle serialization to string of unknown type, likely plugin bug: " + ret);
				return ret;
			}
			return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + ")";
		}

		public void spawn(LivingEntity boss, Location loc) {
			spawn(boss, loc, 0, 0, 0, 0.0d);
		}

		public void spawn(LivingEntity boss, Location loc, double dx, double dy, double dz) {
			spawn(boss, loc, dx, dy, dz, 0.0d);
		}

		public void spawn(LivingEntity boss, Location loc, double dx, double dy, double dz, double extra1) {
			double fdx = mDx != 0 ? mDx : dx;
			double fdy = mDy != 0 ? mDy : dy;
			double fdz = mDz != 0 ? mDz : dz;
			double fVelocity = mVelocity != 0.0d ? mVelocity : extra1;
			spawnNow(boss, loc, fdx, fdy, fdz, fVelocity, mExtra2);
		}

		public <T extends AbstractPartialParticle<T>> T toPartialParticle(T particle) {
			particle.count(mCount);
			if (mDx != 0) {
				particle.mDeltaX = mDx;
			}
			if (mDy != 0) {
				particle.mDeltaX = mDy;
			}
			if (mDz != 0) {
				particle.mDeltaX = mDz;
			}
			if (mVelocity != 0) {
				particle.extra(mVelocity);
			}
			particle.data(mExtra2);
			return particle;
		}

		private void spawnNow(LivingEntity boss, Location loc, double dx, double dy, double dz, double extra1, @Nullable Object extra2) {
			try {
				new PartialParticle(mParticle, loc, mCount, dx, dy, dz, extra1, extra2).spawnAsEntityActive(boss);
			} catch (Exception e) {
				MMLog.warning(() -> "Failed to spawn a particle at loc. Reason: " + e.getMessage());
			}
		}
	}

	public static final ParticlesList EMPTY = new ParticlesList(List.of());

	private final List<CParticle> mParticleList;

	public ParticlesList(List<CParticle> particles) {
		mParticleList = particles;
	}

	public boolean isEmpty() {
		return mParticleList.isEmpty();
	}

	public List<CParticle> getParticleList() {
		return mParticleList;
	}

	public void spawn(LivingEntity boss, Location loc) {
		spawn(boss, loc, 0, 0, 0);
	}

	public void spawn(LivingEntity boss, Location loc, double dx, double dy, double dz) {
		for (CParticle particle : mParticleList) {
			particle.spawn(boss, loc, dx, dy, dz);
		}
	}

	public void spawn(LivingEntity boss, Location loc, double dx, double dy, double dz, double extra1) {
		for (CParticle particle : mParticleList) {
			particle.spawn(boss, loc, dx, dy, dz, extra1);
		}
	}

	public <T extends AbstractPartialParticle<T>> void spawn(LivingEntity boss, Function<Particle, T> particleSupplier) {
		for (CParticle particle : mParticleList) {
			particle.toPartialParticle(particleSupplier.apply(particle.mParticle)).spawnAsEntityActive(boss);
		}
	}

	@Override
	public String toString() {
		StringBuilder msg = new StringBuilder("[");
		boolean first = true;
		for (CParticle cParticle : mParticleList) {
			msg.append(first ? "" : ",").append(cParticle.toString());
			first = false;
		}
		return msg + "]";
	}

	public static ParticlesList fromString(String string) {
		return Parser.parseOrDefault(Parser.getParserMethod(ParticlesList.class), string, EMPTY);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		List<CParticle> mParticles = new ArrayList<>();

		public Builder add(CParticle particle) {
			mParticles.add(particle);
			return this;
		}

		public ParticlesList build() {
			return new ParticlesList(mParticles);
		}
	}
}