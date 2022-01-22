package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.player.PartialParticle;
import dev.jorel.commandapi.Tooltip;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ParticlesList {
	private static final EnumSet<Particle> PARTICLE_MATERIALS = EnumSet.of(Particle.ITEM_CRACK, Particle.BLOCK_CRACK, Particle.BLOCK_DUST, Particle.FALLING_DUST);
	private static final EnumSet<Particle> PARTICLES_WITH_PARAMETERS = EnumSet.of(Particle.REDSTONE, Particle.ITEM_CRACK, Particle.BLOCK_CRACK, Particle.BLOCK_DUST, Particle.FALLING_DUST);

	public static class CParticle {
		Particle mParticle;
		int mCount;
		double mDx;
		double mDy;
		double mDz;
		double mVelocity;
		Object mExtra2; //used when we have a particle that is inside PARTICLE_MATERIALS or Particle.REDSTONE

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

		public CParticle(Particle particle, int count, double dx, double dy, double dz, double extra1, Object extra2) {
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

		public void spawn(Location loc) {
			spawn(loc, 0, 0, 0, 0.0d);
		}

		public void spawn(Location loc, double dx, double dy, double dz) {
			spawn(loc, dx, dy, dz, 0.0d);
		}

		public void spawn(Location loc, double dx, double dy, double dz, double extra1) {
			double fdx = mDx != 0 ? mDx : dx;
			double fdy = mDy != 0 ? mDy : dy;
			double fdz = mDz != 0 ? mDz : dz;
			double fVelocity = mVelocity != 0.0d ? mVelocity : extra1;
			spawnNow(loc, fdx, fdy, fdz, fVelocity, mExtra2);

		}

		private void spawnNow(Location loc, double dx, double dy, double dz, double extra1, Object extra2) {
			try {
				if (mParticle.equals(Particle.REDSTONE)) {
					new PartialParticle(mParticle, loc, mCount, dx, dy, dz, extra1, extra2).spawnAsBoss();
				} else if (PARTICLE_MATERIALS.contains(mParticle)) {
					new PartialParticle(mParticle, loc, mCount, dx, dy, dz, extra1, extra2).spawnAsBoss();
				} else {
					new PartialParticle(mParticle, loc, mCount, dx, dy, dz, extra1).spawnAsBoss();
				}
			} catch (Exception e) {
				Plugin.getInstance().getLogger().warning("Failed to spawn a particle at loc. Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private final List<CParticle> mParticleList;

	public ParticlesList(List<CParticle> particles) {
		mParticleList = particles;
	}

	public void spawn(Location loc) {
		spawn(loc, 0, 0, 0);
	}

	public void spawn(Location loc, double dx, double dy, double dz) {
		spawn(loc, dx, dy, dz, null);
	}

	public <F> void spawn(Location loc, double dx, double dy, double dz, F extra1) {
		for (CParticle particle : mParticleList) {
			if (extra1 instanceof Double) {
				particle.spawn(loc, dx, dy, dz, (Double) extra1);
			} else if (extra1 instanceof Float) {
				particle.spawn(loc, dx, dy, dz, ((Float) extra1).doubleValue());
			} else if (extra1 instanceof Integer) {
				particle.spawn(loc, dx, dy, dz, ((Integer) extra1).doubleValue());
			} else if (extra1 == null) {
				particle.spawn(loc, dx, dy, dz);
			} else {
				Plugin.getInstance().getLogger().warning("[Particle List] Error during spawn for param. extra1 is not a number. Value: " + extra1);
			}
		}
	}

	public String toString() {
		String msg = "[";
		boolean first = true;
		for (CParticle cParticle : mParticleList) {
			msg = msg + (first ? "" : ",") + cParticle.toString();
			first = false;
		}
		return msg + "]";
	}

	public static ParticlesList fromString(String string) {
		ParseResult<ParticlesList> result = fromReader(new StringReader(string), "");
		if (result.getResult() == null) {
			Plugin.getInstance().getLogger().warning("Failed to parse '" + string + "' as ParticlesList");
			Thread.dumpStack();
			return new ParticlesList(new ArrayList<>(0));
		}

		return result.getResult();
	}

	/*
	 * Parses a ParticlesList at the next position in the StringReader.
	 * If this item parses successfully:
	 *   The returned ParseResult will contain a non-null getResult() and a null getTooltip()
	 *   The reader will be advanced to the next character past this ParticlesList value.
	 * Else:
	 *   The returned ParseResult will contain a null getResult() and a non-null getTooltip()
	 *   The reader will not be advanced
	 */
	public static ParseResult<ParticlesList> fromReader(StringReader reader, String hoverDescription) {
		if (!reader.advance("[")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "[", hoverDescription)));
		}

		List<CParticle> particlesList = new ArrayList<>(2);

		boolean atLeastOneParticleIter = false;
		while (true) {
			// Start trying to parse the next individual particle entry in the list

			if (reader.advance("]")) {
				// Got closing bracket and parsed rest successfully - complete particle list, break this loop
				break;
			}

			if (atLeastOneParticleIter) {
				if (!reader.advance(",")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", hoverDescription),
						Tooltip.of(reader.readSoFar() + "]", hoverDescription)
					));
				}
				if (!reader.advance("(")) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "(", hoverDescription)));
				}
			} else {
				if (!reader.advance("(")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + "(", hoverDescription),
						Tooltip.of(reader.readSoFar() + "]", hoverDescription)
					));
				}
			}

			atLeastOneParticleIter = true;
			Particle particle = reader.readParticle();
			if (particle == null) {
				// Entry not valid, offer all entries as completions
				List<Tooltip<String>> suggArgs = new ArrayList<>(Particle.values().length);
				String soFar = reader.readSoFar();
				for (Particle valid : Particle.values()) {
					suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
				}
				return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
			}

			/* Valid CParticles:
			 * (REDSTONE,count=1,dx=0,dy=0,dz=0,velocity=1,#color=none,size=1)
			 * (BLOCK_CRACK|BLOCK_DUST|FALLING_DUST,count=1,dx=0,dy=0,dz=0,velocity=1,Material=none)
			 * (ITEM_CRACK,count=1,dx=0,dy=0,dz=0,velocity=1,ItemStack=none)
			 * (<any other>,count=1,dx=0,dy=0,dz=0,velocity=1)
			 */
			if (!reader.advance(",")) {
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", "Specify count, variance, velocity, and particle-specific parameters"),
						Tooltip.of(reader.readSoFar() + ")", "Use default 1 particle, zero variance, 1 velocity")
					));
				}
				// End of this particle, loop to next
				particlesList.add(new CParticle(particle));
				continue;
			}

			Long count = reader.readLong();
			if (count == null || count <= 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "1", "Particle count > 0")));
			}

			if (!reader.advance(",")) {
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", "Specify variance, velocity and particle-specific parameters"),
						Tooltip.of(reader.readSoFar() + ")", "Use default zero variance, 1 velocity")
					));
				}
				// End of this particle, loop to next
				particlesList.add(new CParticle(particle, count.intValue()));
				continue;
			}

			Double dx = reader.readDouble();
			if (dx == null || dx < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "0.0", "X Variance >= 0")));
			}
			if (!reader.advance(",")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ",", hoverDescription)));
			}
			Double dy = reader.readDouble();
			if (dy == null || dy < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "0.0", "Y Variance >= 0")));
			}
			if (!reader.advance(",")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ",", hoverDescription)));
			}
			Double dz = reader.readDouble();
			if (dz == null || dz < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "0.0", "Z Variance >= 0")));
			}

			if (!reader.advance(",")) {
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", "Specify velocity and particle-specific parameters"),
						Tooltip.of(reader.readSoFar() + ")", "Use default 1 velocity")
					));
				}
				// End of this particle, loop to next
				particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz));
				continue;
			}

			Double velocity = reader.readDouble();
			if (velocity == null || velocity < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "1.0", "Velocity >= 0")));
			}

			if (!PARTICLES_WITH_PARAMETERS.contains(particle)) {
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", hoverDescription)));
				}

				// End of this particle, loop to next
				particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity));
				continue;
			} else {
				if (!reader.advance(",")) {
					if (!reader.advance(")")) {
						return ParseResult.of(Tooltip.arrayOf(
							Tooltip.of(reader.readSoFar() + ",", "Specify particle-specific parameters"),
							Tooltip.of(reader.readSoFar() + ")", "Use default particle parameters")
						));
					}
					// End of this particle, loop to next
					particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity));
					continue;
				}

				if (particle.equals(Particle.REDSTONE)) {
					// Redstone takes a color, and an optional size
					Color color = reader.readColor();
					if (color == null) {
						// Color not valid - need to offer all colors as a completion option, plus #FFFFFF
						List<Tooltip<String>> suggArgs = new ArrayList<>(1 + StringReader.COLOR_MAP.size());
						String soFar = reader.readSoFar();
						for (String valid : StringReader.COLOR_MAP.keySet()) {
							suggArgs.add(Tooltip.of(soFar + valid, "Particle color"));
						}
						suggArgs.add(Tooltip.of("#FFFFFF", "Particle color"));
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}

					if (!reader.advance(",")) {
						if (!reader.advance(")")) {
							return ParseResult.of(Tooltip.arrayOf(
								Tooltip.of(reader.readSoFar() + ",", "Specify redstone particle size"),
								Tooltip.of(reader.readSoFar() + ")", "Use default 1 redstone size")
							));
						}
						// End of this particle, loop to next
						particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity, new DustOptions(color, 1.0f)));
						continue;
					}

					Double size = reader.readDouble();
					if (size == null || size <= 0) {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "1.0", "Size > 0")));
					}

					if (!reader.advance(")")) {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", hoverDescription)));
					}

					// End of this particle, loop to next
					particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity, new DustOptions(color, size.floatValue())));
					continue;
				} else {
					// All other supported parameter particles take a material
					Material mat = reader.readMaterial();
					if (mat == null) {
						// Entry not valid, offer all entries as completions
						List<Tooltip<String>> suggArgs = new ArrayList<>(Material.values().length);
						String soFar = reader.readSoFar();
						for (Material valid : Material.values()) {
							suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
						}
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}

					if (!reader.advance(")")) {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", hoverDescription)));
					}

					// End of this particle, loop to next
					if (particle.equals(Particle.ITEM_CRACK)) {
						// ITEM_CRACK particle requires an itemstack, not the underlying material
						particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity, new ItemStack(mat)));
					} else {
						particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity, mat.createBlockData()));
					}
					continue;
				}
			}
		}

		return ParseResult.of(new ParticlesList(particlesList));
	}
}
