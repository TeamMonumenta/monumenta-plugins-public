package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ArcanePotionsCS extends GruesomeAlchemyCS {

	public static final double ENCHANT_PARTICLE_PER_METER = 3.5;
	public static final double SYMBOL_PARTICLES_PER_METER = 5;

	public static final String NAME = "Arcane Potions";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Drawing sigils to sap and twist the life force",
			"of living and undead creatures is well-known.",
			"Bottling them into potions however is a",
			"skill few alchemists have ever mastered.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void effectsOnSwap(Player mPlayer, boolean isGruesomeBeforeSwap) {
		// TODO custom? it's just sounds, and having the same is probably be beneficial for recognition
		super.effectsOnSwap(mPlayer, isGruesomeBeforeSwap);
	}

	private Symbol mLastSymbol = ArcanePotionsCS.PHLOGISTON;

	@Override
	public void effectsOnSplash(Player mPlayer, Location loc, boolean isGruesome, double radius, boolean isSpecialPot) {
		if (isSpecialPot) {
			return; // having both normal + special pot particles is too much for this
		}

		// sound
		loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 0.5f);

		// particle circle
		Location location = loc.clone().add(0, 0.125, 0);
		location.setDirection(loc.toVector().subtract(mPlayer.getLocation().toVector()));
		List<Symbol> symbols = new ArrayList<>(LARGE_SYMBOLS);
		symbols.remove(mLastSymbol);
		mLastSymbol = FastUtils.getRandomElement(symbols);
		drawAlchemyCircle(mPlayer, location, radius, isGruesome, mLastSymbol, false);
	}

	@Override
	public Color splashColor(boolean isGruesome) {
		// TODO disable these particles if possible
		return Color.fromRGB(255, 255, 255);
	}

	@Override
	public void damageOverTimeEffects(LivingEntity target) {
		new PartialParticle(Particle.ENCHANTMENT_TABLE, LocationUtils.getHalfHeightLocation(target), 16)
			.delta(target.getBoundingBox().getWidthX() / 3, target.getBoundingBox().getHeight() / 3, target.getBoundingBox().getWidthZ() / 3)
			.spawnAsEnemy();
	}

	public static void drawAlchemyCircle(Player player, Location loc, double radius, boolean gruesome, Symbol centerSymbol, boolean withSideSymbols) {
		Particle symbolParticle = gruesome ? Particle.SCRAPE : Particle.WAX_ON;
		float rotation = loc.getYaw();
		double startAngle = rotation + (gruesome ? -90 : 90);

		// center symbol
		double centerSymbolSize = radius * 0.35;
		centerSymbol.draw(new Transform(loc.clone(), centerSymbolSize, rotation + 180), symbolParticle, player);

		List<Symbol> symbols = null;
		if (withSideSymbols) {
			symbols = new ArrayList<>(SMALL_SYMBOLS);
			symbols.remove(centerSymbol);
			Collections.shuffle(symbols, FastUtils.RANDOM);
		}

		double smallRadius = 0.2 * radius;
		double arcCut = Math.toDegrees(2 * Math.asin(smallRadius / radius / 2));
		double enchantParticleDelta = 0.25;

		for (int i = 0; i < 3; i++) {
			double currentAngle = startAngle + i * 120;
			Vector dir = VectorUtils.rotateYAxis(new Vector(1, 0, 0), currentAngle);
			Location lineStartLoc = loc.clone().add(dir.clone().multiply(radius));

			// big circle
			new PPCircle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, enchantParticleDelta, 0), radius)
				.ringMode(true)
				.arcDegree(currentAngle + arcCut, currentAngle + 120 - arcCut)
				.countPerMeter(ENCHANT_PARTICLE_PER_METER)
				.directionalMode(true).delta(0, -enchantParticleDelta, 0).extra(1)
				.spawnAsPlayerActive(player);

			// triangle
			Location nextLineStart = loc.clone().add(VectorUtils.rotateYAxis(new Vector(radius, 0, 0), currentAngle + 120));
			Vector dirToNext = nextLineStart.clone().toVector().subtract(lineStartLoc.toVector()).normalize();
			new PPLine(Particle.ENCHANTMENT_TABLE,
				lineStartLoc.clone().add(0, enchantParticleDelta, 0).add(dirToNext.clone().multiply(smallRadius)),
				nextLineStart.clone().add(0, enchantParticleDelta, 0).add(dirToNext.clone().multiply(-smallRadius)))
				.countPerMeter(ENCHANT_PARTICLE_PER_METER)
				.directionalMode(true).delta(0, -enchantParticleDelta, 0).extra(1)
				.spawnAsPlayerActive(player);

			// smaller circles
			new PPCircle(Particle.ENCHANTMENT_TABLE, lineStartLoc.clone().add(0, enchantParticleDelta, 0), smallRadius)
				.ringMode(true).countPerMeter(ENCHANT_PARTICLE_PER_METER)
				.directionalMode(true).delta(0, -enchantParticleDelta, 0).extra(1)
				.spawnAsPlayerActive(player);

			// accent symbol in small circle
			(gruesome ? WATER : FIRE).draw(new Transform(lineStartLoc, smallRadius * 0.8, rotation + 180), symbolParticle, player);

			// side symbols
			if (symbols != null) {
				Location symbolLoc = loc.clone().add(dir.clone().multiply(-0.75 * radius));
				double sideSymbolSize = radius * 0.15;
				symbols.get(i).draw(new Transform(symbolLoc, sideSymbolSize, rotation + 180).skipBelowMultiplier(0.325), symbolParticle, player);
			}
		}
	}

	// region Symbols

	private static void drawParticle(AbstractPartialParticle<?> abstractPartialParticle, Transform transform, Player player) {
		abstractPartialParticle
			// Slight upward motion to prevent z-fighting, and also reduces flashing when a new bunch of particles spawn in (when looked at from above)
			.directionalMode(true).delta(0, 0.1, 0).extra(1)
			.skipBelowMultiplier(transform.mSkipBelowMultiplier)
			.spawnAsPlayerActive(player);
	}

	private static void drawDot(Particle particle, Player player, Transform transform, Location loc) {
		drawParticle(new PartialParticle(particle, loc), transform, player);
	}

	private static void drawLine(Particle particle, Player player, Transform transform, Location l1, Location l2, boolean includeStart, boolean includeEnd) {
		drawParticle(new PPLine(particle, l1, l2).countPerMeter(SYMBOL_PARTICLES_PER_METER).minParticlesPerMeter(SYMBOL_PARTICLES_PER_METER / 3)
			             .groupingDistance(0).includeStart(includeStart).includeEnd(includeEnd), transform, player);
	}

	private static void drawLine(Particle particle, Player player, Transform transform, Location l1, Location l2) {
		drawLine(particle, player, transform, l1, l2, true, true);
	}

	// line without start location
	private static void drawRay(Particle particle, Player player, Transform transform, Location l1, Location l2) {
		drawLine(particle, player, transform, l1, l2, false, true);
	}

	// line without start nor end locations
	private static void drawConnection(Particle particle, Player player, Transform transform, Location l1, Location l2) {
		drawLine(particle, player, transform, l1, l2, false, false);
	}

	private static void drawTriangle(Particle particle, Player player, Transform transform, Location l1, Location l2, Location l3) {
		drawRay(particle, player, transform, l1, l2);
		drawRay(particle, player, transform, l2, l3);
		drawRay(particle, player, transform, l3, l1);
	}

	private static void drawCircle(Particle particle, Player player, Transform transform, double x, double y, double radius) {
		drawArc(particle, player, transform, x, y, radius, 0, -360, false);
	}

	private static void drawArc(Particle particle, Player player, Transform transform, double x, double y, double radius, double arcStartDeg, double arcEndDeg, boolean includeStart) {
		drawParticle(new PPCircle(particle, transform.apply(x, y), radius * transform.mScale)
			             .ringMode(true)
			             .arcDegree(transform.mRotation - arcStartDeg, transform.mRotation - arcEndDeg)
			             .countPerMeter(SYMBOL_PARTICLES_PER_METER)
			             .directionalMode(true)
			             .includeStart(includeStart)
			             .includeEnd(true), transform, player);
	}

	private static final double SQRT_2 = Math.sqrt(2);
	private static final double SQRT_3 = Math.sqrt(3);
	private static final double COS_30 = Math.cos(Math.toRadians(30));

	public interface Symbol {
		void draw(Transform transform, Particle particle, Player player);
	}

	public static class Transform {
		private final Location mOrigin;
		private final double mScale;
		private final double mRotation;
		private final Vector mXAxis;
		private final Vector mYAxis;
		private double mSkipBelowMultiplier = 0;

		public Transform(Location origin, double scale, double rotation) {
			this(origin, scale, rotation, new Vector(1, 0, 0), new Vector(0, 0, -1));
		}

		public Transform(Location origin, double scale, double rotation, Vector xAxis, Vector yAxis) {
			mOrigin = origin;
			mScale = scale;
			mRotation = rotation;
			mXAxis = xAxis;
			mYAxis = yAxis;
		}

		public Transform skipBelowMultiplier(double skipBelowMultiplier) {
			mSkipBelowMultiplier = skipBelowMultiplier;
			return this;
		}

		public Location apply(double x, double y) {
			double cos = FastUtils.cosDeg(mRotation);
			double sin = FastUtils.sinDeg(mRotation);
			return mOrigin.clone().add(mXAxis.clone().multiply(mScale * (x * cos + y * sin))).add(mYAxis.clone().multiply(mScale * (y * cos - x * sin)));
		}
	}

	// Sulphur 🜍
	public static final Symbol SULPHUR = (transform, particle, player) -> {
		// triangle
		drawTriangle(particle, player, transform,
			transform.apply(-1 / SQRT_3, 0), transform.apply(1 / SQRT_3, 0), transform.apply(0, 1));

		// cross
		drawRay(particle, player, transform, transform.apply(0, 0), transform.apply(0, -1));
		drawLine(particle, player, transform, transform.apply(-1.0 / 3, -0.5), transform.apply(1.0 / 3, -0.5));
	};

	// Philosopher's Sulphur 🜎
	public static final Symbol PHILOSOPHERS_SULPHUR = (transform, particle, player) -> {
		// triangle
		drawTriangle(particle, player, transform,
			transform.apply(-1 / SQRT_3, 0), transform.apply(1 / SQRT_3, 0), transform.apply(0, 1));

		// "tails"
		drawRay(particle, player, transform, transform.apply(0, 0), transform.apply(0, -1));
		drawRay(particle, player, transform, transform.apply(0, 0), transform.apply(-1 / SQRT_3, -1));
		drawRay(particle, player, transform, transform.apply(0, 0), transform.apply(1 / SQRT_3, -1));
	};

	// Aqua Vitae 🜉
	public static final Symbol AQUA_VITAE = (transform, particle, player) -> {
		double s = 1.0 / 3; // circle radius
		drawCircle(particle, player, transform, 0, -2 * s, s);
		double x = 2 * s * COS_30; // x/y of the top two circles
		double y = 2 * s / 2;
		drawCircle(particle, player, transform, -x, y, s);
		drawCircle(particle, player, transform, x, y, s);
		drawConnection(particle, player, transform, transform.apply(s / 2, (-2 + COS_30) * s), transform.apply(x - s / 2, y - COS_30 * s));
		drawConnection(particle, player, transform, transform.apply(-s / 2, (-2 + COS_30) * s), transform.apply(-(x - s / 2), y - COS_30 * s));
	};

	// Salt of Antimony 🜭
	public static final Symbol SALT_OF_ANTIMONY = (transform, particle, player) -> {
		// circle
		drawCircle(particle, player, transform, 0, -0.5, 0.5);
		drawConnection(particle, player, transform, transform.apply(-0.5, -0.5), transform.apply(0.5, -0.5));

		// cross
		drawRay(particle, player, transform, transform.apply(0, 0), transform.apply(0, 1));
		drawLine(particle, player, transform, transform.apply(-0.5, 0.5), transform.apply(0.5, 0.5));
	};

	// Crocus of Iron 🜞
	public static final Symbol CROCUS_OF_IRON = (transform, particle, player) -> {
		// triangle
		drawTriangle(particle, player, transform,
			transform.apply(-1 / SQRT_3, 0), transform.apply(1 / SQRT_3, 0), transform.apply(0, -1));

		// cross
		drawRay(particle, player, transform, transform.apply(0, 0), transform.apply(0, 1));
		drawLine(particle, player, transform, transform.apply(-1.0 / 3, 1.0 / 3), transform.apply(1.0 / 3, 1.0 / 3));

		// arrow
		drawRay(particle, player, transform, transform.apply(0, 1), transform.apply(-1.0 / 3, 2.0 / 3));
		drawRay(particle, player, transform, transform.apply(0, 1), transform.apply(1.0 / 3, 2.0 / 3));
	};

	// Fire 🜂 - not used as random symbol
	public static final Symbol FIRE = (transform, particle, player) -> {
		double x = 1.5 / SQRT_3;
		double z = 0.5;
		drawTriangle(particle, player, transform,
			transform.apply(-x, -z), transform.apply(x, -z), transform.apply(0, 1));
	};

	// Water 🜄 - not used as random symbol
	public static final Symbol WATER = (transform, particle, player) -> {
		double x = 1.5 / SQRT_3;
		double z = 0.5;
		drawTriangle(particle, player, transform,
			transform.apply(-x, z), transform.apply(x, z), transform.apply(0, -1));
	};

	// Air 🜁
	public static final Symbol AIR = (transform, particle, player) -> {
		double x = 1.5 / SQRT_3;
		double z = 0.5;
		drawTriangle(particle, player, transform,
			transform.apply(-x, -z), transform.apply(x, -z), transform.apply(0, 1));
		drawLine(particle, player, transform, transform.apply(-x, 0.25), transform.apply(x, 0.25));
	};

	// Earth 🜃
	public static final Symbol EARTH = (transform, particle, player) -> {
		double x = 1.5 / SQRT_3;
		double z = 0.5;
		drawTriangle(particle, player, transform,
			transform.apply(-x, z), transform.apply(x, z), transform.apply(0, -1));
		drawLine(particle, player, transform, transform.apply(-x, -0.25), transform.apply(x, -0.25));
	};

	// Vinegar 🜊
	public static final Symbol VINEGAR = (transform, particle, player) -> {
		// cross
		drawConnection(particle, player, transform, transform.apply(-1, 0), transform.apply(1, 0));
		drawConnection(particle, player, transform, transform.apply(0, -1), transform.apply(0, 1));

		// serifs
		double s = 0.25;
		drawLine(particle, player, transform, transform.apply(-1, -s), transform.apply(-1, s));
		drawLine(particle, player, transform, transform.apply(1, -s), transform.apply(1, s));
		drawLine(particle, player, transform, transform.apply(-s, -1), transform.apply(s, -1));
		drawLine(particle, player, transform, transform.apply(-s, 1), transform.apply(s, 1));
	};

	// Vinegar (alternative) 🜋
	public static final Symbol VINEGAR_ALT = (transform, particle, player) -> {
		VINEGAR.draw(transform, particle, player);

		drawDot(particle, player, transform, transform.apply(0.5, 0.5));
		drawDot(particle, player, transform, transform.apply(0.5, -0.5));
		drawDot(particle, player, transform, transform.apply(-0.5, 0.5));
		drawDot(particle, player, transform, transform.apply(-0.5, -0.5));
	};

	// Gold 🜚
	public static final Symbol GOLD_COMET = (transform, particle, player) -> {
		// circle
		drawCircle(particle, player, transform, -0.5, -0.5, 0.5);

		// tail
		drawRay(particle, player, transform, transform.apply(0.9, 0.9), transform.apply(-0.5, 0));
		drawConnection(particle, player, transform, transform.apply(0.9, 0.9), transform.apply(0, -0.5));
	};

	// Alternative Gold ☼
	public static final Symbol GOLD_SUN = (transform, particle, player) -> {
		drawCircle(particle, player, transform, 0, 0, 0.5);

		for (int i = 0; i < 8; i++) {
			double rotation = i * 45;
			drawArc(particle, player, transform, 0.75 * FastUtils.cosDeg(rotation), 0.75 * FastUtils.sinDeg(rotation), 0.25, rotation + 180, rotation, false);
		}
	};

	// Tin ♃
	public static final Symbol TIN = (transform, particle, player) -> {
		drawArc(particle, player, transform, -0.75 - 1 / SQRT_2, -0.5 + 1 / SQRT_2, 1, -45, 45, false);

		drawLine(particle, player, transform, transform.apply(-0.75, -0.5), transform.apply(0.75, -0.5));
		drawLine(particle, player, transform, transform.apply(0.25, 0), transform.apply(0.25, -1));
	};

	// Lead ♄
	public static final Symbol LEAD = (transform, particle, player) -> {
		double d = 0.5; // third of the width
		// cross
		drawLine(particle, player, transform, transform.apply(-d / 2, -1), transform.apply(-d / 2, 1));
		drawLine(particle, player, transform, transform.apply(-d * 3 / 2, 1 - d * 2 / 3), transform.apply(d / 2, 1 - d * 2 / 3));

		// curve
		drawArc(particle, player, transform, d / 2, -d / 2, d, 180, -30, false);
		drawArc(particle, player, transform, d / 2 + 3 * d * COS_30, -1, 2 * d, 180 - 30, 180, false);
	};

	// Phlogiston - reserved for Scorched Earth
	public static final Symbol PHLOGISTON = (transform, particle, player) -> {
		double s = 1.0 / 4; // circle radius
		double y = 1 - s; // y of the top circle
		double x = y / SQRT_3; // x of the bottom two circles

		// circles
		drawCircle(particle, player, transform, 0, y, s);
		drawCircle(particle, player, transform, -x, 0, s);
		drawCircle(particle, player, transform, x, 0, s);

		// lines between circles
		drawConnection(particle, player, transform, transform.apply(s / 2, y - COS_30 * s), transform.apply(x - s / 2, COS_30 * s));
		drawConnection(particle, player, transform, transform.apply(-s / 2, y - COS_30 * s), transform.apply(-x + s / 2, COS_30 * s));
		drawConnection(particle, player, transform, transform.apply(-x + s, 0), transform.apply(x - s, 0));

		// cross
		drawRay(particle, player, transform, transform.apply(0, 0), transform.apply(0, -0.5 - x));
		drawLine(particle, player, transform, transform.apply(-x, -0.5), transform.apply(x, -0.5));
	};

	// Antimony (alternative) - reserved for Transmutation Ring
	public static final Symbol ANTIMONY = (transform, particle, player) -> {
		double s = 1.0 / 3;

		// circle
		double r = s * 0.75;
		drawCircle(particle, player, transform, 0, 1 - r, r);

		// cross
		drawRay(particle, player, transform, transform.apply(0, 1 - 2 * r), transform.apply(0, -1));
		drawLine(particle, player, transform, transform.apply(-0.5, -0.5), transform.apply(0.5, -0.5));

		// "arms"
		drawArc(particle, player, transform, 0, s, 0.5, 360, 180, true);
		drawArc(particle, player, transform, 1, s, 0.5, 180, 90, false);
		drawArc(particle, player, transform, -1, s, 0.5, 90, 0, false);
	};

	// Bismuth Ore 🜾 - reserved for Warding Remedy
	public static final Symbol BISMUTH_ORE = (transform, particle, player) -> {
		// circle
		drawCircle(particle, player, transform, 0, 0.25, 0.25);

		// "arms"
		drawArc(particle, player, transform, -1 - 1 / SQRT_2, -0.5 + 1 / SQRT_2, 1, 45, -45, false);
		drawArc(particle, player, transform, 1 + 1 / SQRT_2, -0.5 + 1 / SQRT_2, 1, 180 - 45, 180 + 45, false);

		// cross
		drawLine(particle, player, transform, transform.apply(-1, -0.5), transform.apply(1, -0.5));
		drawRay(particle, player, transform, transform.apply(0, 0), transform.apply(0, -1));
	};

	public static ImmutableList<Symbol> LARGE_SYMBOLS = ImmutableList.of(SULPHUR, PHILOSOPHERS_SULPHUR, AQUA_VITAE, SALT_OF_ANTIMONY, CROCUS_OF_IRON, AIR, EARTH, VINEGAR_ALT, GOLD_SUN, TIN, LEAD);
	public static ImmutableList<Symbol> SMALL_SYMBOLS = ImmutableList.of(SULPHUR, PHILOSOPHERS_SULPHUR, AQUA_VITAE, SALT_OF_ANTIMONY, AIR, EARTH, VINEGAR, GOLD_COMET, TIN, LEAD);

	// endregion

}
