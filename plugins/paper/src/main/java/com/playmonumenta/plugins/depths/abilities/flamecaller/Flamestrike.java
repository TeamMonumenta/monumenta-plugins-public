package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Flamestrike extends DepthsAbility {
	public static final String ABILITY_NAME = "Flamestrike";
	public static final int COOLDOWN = 10 * 20;
	public static final double[] DAMAGE = {14, 17, 20, 23, 26, 32};
	public static final int HEIGHT = 6;
	public static final int RADIUS = 7;
	public static final int HALF_ANGLE = 70;
	public static final int FIRE_TICKS = 4 * 20;
	public static final float KNOCKBACK = 0.5f;

	public static final String CHARM_COOLDOWN = "Flamestrike Cooldown";

	public static final DepthsAbilityInfo<Flamestrike> INFO =
		new DepthsAbilityInfo<>(Flamestrike.class, ABILITY_NAME, Flamestrike::new, DepthsTree.FLAMECALLER, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.FLAMESTRIKE)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Flamestrike::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.FLINT_AND_STEEL)
			.descriptions(Flamestrike::getDescription);

	private final double mDamage;
	private final double mRadius;
	private final int mFireDuration;
	private final double mKnockback;
	private final double mHalfAngle;

	public Flamestrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FLAMESTRIKE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.FLAMESTRIKE_RANGE.mEffectName, RADIUS);
		mFireDuration = CharmManager.getDuration(mPlayer, CharmEffects.FLAMESTRIKE_FIRE_DURATION.mEffectName, FIRE_TICKS);
		mKnockback = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FLAMESTRIKE_KNOCKBACK.mEffectName, KNOCKBACK);
		mHalfAngle = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FLAMESTRIKE_CONE_ANGLE.mEffectName, HALF_ANGLE);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Hitbox hitbox = Hitbox.approximateCylinderSegment(
			LocationUtils.getHalfHeightLocation(mPlayer).add(0, -HEIGHT, 0),
			2 * HEIGHT, mRadius, Math.toRadians(mHalfAngle));

		for (LivingEntity mob : hitbox.getHitMobs()) {
			EntityUtils.applyFire(mPlugin, mFireDuration, mob, mPlayer);
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, true);
			MovementUtils.knockAway(mPlayer, mob, (float) mKnockback, true);
		}

		World world = mPlayer.getWorld();
		new PartialParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 15, 0.05, 0.05, 0.05, 0.1).spawnAsPlayerActive(mPlayer);
		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mTempRadius = 0;

			@Override
			public void run() {
				if (mTempRadius == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mTempRadius += 1.25;
				for (double degree = -mHalfAngle; degree <= mHalfAngle; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.sin(radian1) * mTempRadius, 0.125, FastUtils.cos(radian1) * mTempRadius);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);
					new PartialParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.15).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, l, 3, 0.15, 0.15, 0.15, 0.1).spawnAsPlayerActive(mPlayer);
				}

				if (mTempRadius >= mRadius + 1 || !mPlayer.isOnline()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.5f);
		return true;
	}

	private static Description<Flamestrike> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.addTrigger()
			.add(" to create a torrent of flames, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to all enemies in front of you within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks, setting them on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds and knocking them away.")
			.addCooldown(COOLDOWN);
	}

}
