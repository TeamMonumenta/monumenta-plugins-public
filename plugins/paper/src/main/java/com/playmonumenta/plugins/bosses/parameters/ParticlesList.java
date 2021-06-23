package com.playmonumenta.plugins.bosses.parameters;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

public class ParticlesList {
	private static final EnumSet<Particle> PARTICLE_MATERIALS = EnumSet.of(Particle.ITEM_CRACK, Particle.BLOCK_CRACK, Particle.BLOCK_DUST, Particle.FALLING_DUST);

	public static class CParticle<T, D> {
		Particle mParticle;
		int mCount;
		double mDx;
		double mDy;
		double mDz;
		T mExtra1;
		D mExtra2; //used when we have a particle that is inside PARTICLE_MATERIALS or Particle.REDSTONE

		public CParticle(Particle particle, int count) {
			this(particle, count, 0, 0, 0);
		}

		public CParticle(Particle particle, int count, double dx, double dy, double dz) {
			this(particle, count, dx, dy, dz, null);
		}

		public CParticle(Particle particle, int count, double dx, double dy, double dz, T extra1) {
			this(particle, count, dx, dy, dz, extra1, null);
		}

		public CParticle(Particle particle, int count, double dx, double dy, double dz, T extra1, D extra2) {
			mParticle = particle;
			mCount = count;
			mDx = dx;
			mDy = dy;
			mDz = dz;
			mExtra1 = extra1;
			mExtra2 = extra2;
		}

		@Override
		public String toString() {
			if (mExtra1 instanceof DustOptions) {
				String color = "#" + Integer.toHexString(((DustOptions) mExtra1).getColor().asRGB());
				String size = Float.toString(((DustOptions) mExtra1).getSize());
				return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + color + "," + size + ")";
			} else if (mExtra2 instanceof BlockData) {
				return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mExtra1 + "," + ((BlockData) mExtra2).getMaterial().name() + ")";
			} else if (mExtra2 instanceof ItemStack) {
				return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mExtra1 + "," + ((ItemStack) mExtra2).getType().name() + ")";
			}
			return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mExtra1 + "," + mExtra2 + ")";
		}

		public static CParticle fromString(String value) throws Exception {
			if (value.startsWith("(")) {
				value = value.substring(1);
			}
			if (value.endsWith(")")) {
				value = value.substring(0, value.length() - 1);
			}
			String[] split = value.split(",");

			Particle particle = Particle.valueOf(split[0].toUpperCase());

			if (particle == null) {
				throw new ParticleNotFoundException(split[0]);
			}
			if (split.length == 7) {
				if (particle.equals(Particle.REDSTONE)) {
					return new CParticle<>(particle, Integer.parseInt(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Double.parseDouble(split[4]),
						new DustOptions(BossUtils.colorFromString(split[5]), Float.parseFloat(split[6])));
				} else if (PARTICLE_MATERIALS.contains(particle)) {
					if (particle.equals(Particle.ITEM_CRACK)) {
						return new CParticle<>(particle, Integer.parseInt(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Double.parseDouble(split[4]),
						Double.parseDouble(split[5]), new ItemStack(Material.getMaterial(split[6].toUpperCase())));
					} else {
						return new CParticle<>(particle, Integer.parseInt(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Double.parseDouble(split[4]),
						Double.parseDouble(split[5]), Material.getMaterial(split[6].toUpperCase()).createBlockData());
					}
				} else {
					//we are loading one created from toString()
					if (split[6].equalsIgnoreCase("null")) {
						if (split[5].equalsIgnoreCase("null")) {
							return new CParticle<>(particle, Integer.parseInt(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Double.parseDouble(split[4]));
						} else {
							return new CParticle<>(particle, Integer.parseInt(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Double.parseDouble(split[4]),
									Double.parseDouble(split[5]));
						}
					} else {
						return new CParticle<>(particle, Integer.parseInt(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Double.parseDouble(split[4]));
					}

				}
			} else if (split.length == 6) {
				return new CParticle<>(particle, Integer.parseInt(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Double.parseDouble(split[4]),
									Double.parseDouble(split[5]));
			} else if (split.length == 5) {
				return new CParticle<>(particle, Integer.parseInt(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Double.parseDouble(split[4]));
			} else if (split.length == 2) {
				return new CParticle<>(particle, Integer.parseInt(split[1]));
			} else if (split.length == 1) {
				return new CParticle<>(particle, 0);
			} else {
				throw new IllegalFormatException("Fail loading custom particle. Object of size " + split.length);
			}
		}

		public void spawn(Location loc) {
			spawn(loc, 0, 0, 0, null);
		}

		public void spawn(Location loc, double dx, double dy, double dz, T extra1) {
			double fdx = mDx != 0 ? mDx : dx;
			double fdy = mDy != 0 ? mDy : dy;
			double fdz = mDz != 0 ? mDz : dz;
			T fExtra1 = mExtra1 != null ? mExtra1 : extra1;
			spawnNow(loc, fdx, fdy, fdz, fExtra1, mExtra2);

		}

		private void spawnNow(Location loc, double dx, double dy, double dz, T extra1, D extra2) {
			try {
				if (mParticle.equals(Particle.REDSTONE)) {
					new PartialParticle(mParticle, loc, mCount, dx, dy, dz, 0.0, extra1).spawnAsBoss();
				} else if (PARTICLE_MATERIALS.contains(mParticle)) {
					new PartialParticle(mParticle, loc, mCount, dx, dy, dz, ((Double) extra1).doubleValue(), extra2).spawnAsBoss();
				} else if (extra1 != null) {
					new PartialParticle(mParticle, loc, mCount, dx, dy, dz, ((Double) extra1).doubleValue()).spawnAsBoss();
				} else {
					new PartialParticle(mParticle, loc, mCount, dx, dy, dz, 0.0).spawnAsBoss();
				}
			} catch (Exception e) {
				Plugin.getInstance().getLogger().warning("Failed to spawn a particle at loc. Reason: " + e.getMessage());
			}

		}
	}

	private List<CParticle> mParticleList;

	public ParticlesList(String values) throws RuntimeException {
		mParticleList = new ArrayList<>();
		List<String> split = BossUtils.splitByCommasUsingBrackets(values);

		for (String stringSplitted : split) {
			try {
				mParticleList.add(CParticle.fromString(stringSplitted));
			} catch (Exception e) {
				Plugin.getInstance().getLogger().warning("Failed to parse '" + stringSplitted + "': " + e.getMessage());
			}
		}

		if (mParticleList.isEmpty()) {
			throw new ListEmptyException("Fail parsing string to list. List empty");
		}
	}

	public void spawn(Location loc) {
		spawn(loc, 0, 0, 0);
	}

	public void spawn(Location loc, double dx, double dy, double dz) {
		spawn(loc, dx, dy, dz, null);
	}

	public <F> void spawn(Location loc, double dx, double dy, double dz, F extra1) {
		for (CParticle particle : mParticleList) {
			particle.spawn(loc, dx, dy, dz, extra1);
		}
	}

	@Override
	public String toString() {
		String msg = "[";
		for (CParticle particle : mParticleList) {
			msg = msg + particle.toString() + ",";
		}
		//remove last comma
		if (msg.endsWith(",")) {
			msg = msg.substring(0, msg.length() - 1);
		}
		return msg + "]";
	}

	public static ParticlesList fromString(String string) throws RuntimeException {
		return new ParticlesList(string.replace(" ", ""));
	}


	private static class ParticleNotFoundException extends RuntimeException {
		ParticleNotFoundException(String value) {
			super("Particle don't found from values: " + value);
		}
	}

	private static class IllegalFormatException extends RuntimeException {
		public IllegalFormatException(String value) {
			super(value);
		}
	}

	private class ListEmptyException extends RuntimeException {
		public ListEmptyException(String value) {
			super(value);
		}
	}
}
