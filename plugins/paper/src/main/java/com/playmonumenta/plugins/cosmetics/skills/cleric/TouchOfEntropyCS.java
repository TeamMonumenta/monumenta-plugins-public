package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TouchOfEntropyCS extends HandOfLightCS {
	//Twisted theme

	public static final String NAME = "Touch of Entropy";

	private static final Color ENTRO_COLOR = Color.fromRGB(127, 0, 0);
	private static final Color DRAIN_COLOR = Color.fromRGB(224, 139, 158);
	private static final Color DRAIN_COLOR_LIGHT = Color.fromRGB(235, 209, 215);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"Cleanse your allies with harmony.",
			"Drain your enemies with dissonance.");

	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HAND_OF_LIGHT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STRUCTURE_VOID;
	}

	@Override
	public void lightHealEffect(Player mPlayer, Location loc, Player mTarget) {
		Location l = loc.clone().add(0, 1, 0);
		new PartialParticle(Particle.HEART, l, 7, 0.7, 0.7, 0.7, 0).spawnAsPlayerActive(mPlayer);
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.2f);

		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.5),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), mPlayer.getLocation().add(0, 1, 0), mPlayer, mTarget, null, false);
	}

	@Override
	public void lightHealCastEffect(World world, Location userLoc, Plugin mPlugin, Player mPlayer, float radius, double angle) {
		final float HEALING_RADIUS = radius;
		world.playSound(userLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.5f, 1.25f);
		cone(mPlayer, HEALING_RADIUS, false);
	}

	@Override
	public void lightDamageEffect(Player mPlayer, Location loc, LivingEntity target) {
		loc = loc.clone().add(0, 1, 0);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 50, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 1.5f, 0f);
		createOrb(new Vector(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-0.5, 0.5),
			FastUtils.randomDoubleInRange(-1, 1)), LocationUtils.getHalfHeightLocation(target),
			mPlayer, target, target.getLocation().add(
				FastUtils.randomDoubleInRange(-2, 2), FastUtils.randomDoubleInRange(5, 7),
				FastUtils.randomDoubleInRange(-2, 2)
			), true);
	}

	@Override
	public void lightDamageCastEffect(World world, Location userLoc, Plugin mPlugin, Player mPlayer, float radius, double angle) {
		final float DAMAGE_RADIUS = radius;
		world.playSound(userLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.5f, 0.6f);
		world.playSound(userLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.5f, 0.85f);
		cone(mPlayer, DAMAGE_RADIUS, true);
	}

	private void cone(Player mPlayer, double range, boolean damage) {
		final Location mLoc = mPlayer.getLocation();
		mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
		new BukkitRunnable() {

			double mRadius = damage ? range : 0;
			@Override
			public void run() {

				Vector vec;
				for (int i = 0; i < 4; i++) {
					mRadius += damage ? -0.5 : 0.5;
					for (double degree = 30; degree <= 150; degree += 5) {
						double radian1 = Math.toRadians(degree);
						double percent = Math.abs(degree - 90) / 60;
						double y = 1 * FastMath.sin(Math.PI * ((mRadius + 0.5) / range));
						vec = new Vector(FastUtils.cos(radian1) * mRadius, y, FastUtils.sin(radian1) * mRadius);
						vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

						Location l = mLoc.clone().add(0, 0.125, 0).add(vec);
						new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
							new Particle.DustOptions(damage ? ENTRO_COLOR : DRAIN_COLOR, 0.75f + (float) (percent * 0.5f))).spawnAsPlayerActive(mPlayer);
						if (damage) {
							new PartialParticle(Particle.SMOKE_NORMAL, l, 1, 0.125f, 0.125f, 0.125f, 0.075).spawnAsPlayerActive(mPlayer);
						} else {
							new PartialParticle(Particle.PORTAL, l, 3, 0, 0, 0, 0.15).spawnAsPlayerActive(mPlayer);
							Color c = FastUtils.RANDOM.nextBoolean() ? DRAIN_COLOR : DRAIN_COLOR_LIGHT;
							double red = c.getRed() / 255D;
							double green = c.getGreen() / 255D;
							double blue = c.getBlue() / 255D;
							new PartialParticle(Particle.SPELL_MOB,
								l.clone().add(FastUtils.randomDoubleInRange(-0.1, 0.1),
									FastUtils.randomDoubleInRange(-0.1, 0.1),
									FastUtils.randomDoubleInRange(-0.1, 0.1)),
								1, red, green, blue, 1)
								.directionalMode(true).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
						}
					}
					if (mRadius <= 0 || mRadius >= range) {
						this.cancel();
						return;
					}
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void createOrb(Vector dir, Location loc, Player mPlayer, LivingEntity target, Location optLoc, boolean damage) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = loc.clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = optLoc != null ? optLoc : LocationUtils.getHalfHeightLocation(target);

				for (int i = 0; i < (damage ? 3 : 4); i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.085;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.2) {
						mD.normalize().multiply(0.2);
					}

					mL.add(mD);

					if (damage) {
						new PartialParticle(Particle.REDSTONE, mL, 2, 0.075, 0.075, 0.075, 0,
							new Particle.DustOptions(ENTRO_COLOR, 1.25f))
							.spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SMOKE_NORMAL, mL, 2, 0.1, 0.1, 0.1, 0.035).spawnAsPlayerActive(mPlayer);
					} else {

						for (int j = 0; j < 2; j++) {
							Color c = FastUtils.RANDOM.nextBoolean() ? DRAIN_COLOR : DRAIN_COLOR_LIGHT;
							double red = c.getRed() / 255D;
							double green = c.getGreen() / 255D;
							double blue = c.getBlue() / 255D;
							new PartialParticle(Particle.SPELL_MOB,
								mL.clone().add(FastUtils.randomDoubleInRange(-0.05, 0.05),
									FastUtils.randomDoubleInRange(-0.05, 0.05),
									FastUtils.randomDoubleInRange(-0.05, 0.05)),
								1, red, green, blue, 1)
								.directionalMode(true).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
						}
						Color c = FastUtils.RANDOM.nextBoolean() ? DRAIN_COLOR : DRAIN_COLOR_LIGHT;
						new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0,
							new Particle.DustOptions(c, 1.4f))
							.spawnAsPlayerActive(mPlayer);
					}

					if (mT > 5 && mL.distance(to) < 0.35) {
						if (damage) {
							world.playSound(mL, Sound.ENTITY_GHAST_DEATH, SoundCategory.PLAYERS, 0.85f, 0.65f);
							world.playSound(mL, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.85f, 0.5f);
							new PartialParticle(Particle.SMOKE_NORMAL, mL, 35, 0, 0, 0, 0.1F).spawnAsPlayerActive(mPlayer);
						} else {
							world.playSound(mL, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 1f, 1.75f);
							world.playSound(mL, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1f, 0.65f);
							new PartialParticle(Particle.SPELL, target.getLocation().add(0, 1, 0), 20, 0.4f, 0.4f, 0.4f, 0.6F)
								.spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 15, 0, 0, 0, 0.15f).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.CRIT_MAGIC, mL, 45, 0, 0, 0, 0.75F).spawnAsPlayerActive(mPlayer);
						}
						new PartialParticle(Particle.CRIT_MAGIC, mL, 25, 0, 0, 0, 0.6F).spawnAsPlayerActive(mPlayer);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
