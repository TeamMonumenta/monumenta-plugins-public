package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.TrainingDummyBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Aeroblast extends DepthsAbility {

	public static final String ABILITY_NAME = "Aeroblast";
	public static final double[] DAMAGE = {10, 12, 14, 16, 18, 22};
	public static final int COOLDOWN = 6 * 20;
	private static final int DURATION = 3 * 20;
	private static final double SPEED_AMPLIFIER = 0.2;
	private static final String PERCENT_SPEED_EFFECT_NAME = "AeroblastSpeedEffect";
	private static final int SIZE = 4;
	private static final float KNOCKBACK_SPEED = 2f;

	public static final String CHARM_COOLDOWN = "Aeroblast Cooldown";

	public static final DepthsAbilityInfo<Aeroblast> INFO =
		new DepthsAbilityInfo<>(Aeroblast.class, ABILITY_NAME, Aeroblast::new, DepthsTree.WINDWALKER, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.AEROBLAST)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Aeroblast::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.PHANTOM_MEMBRANE)
			.descriptions(Aeroblast::getDescription);

	private final float mKnockbackSpeed;
	private final int mDuration;
	private final double mSpeed;
	private final double mDamage;
	private final double mSize;

	public Aeroblast(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mKnockbackSpeed = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.AEROBLAST_KNOCKBACK.mEffectName, KNOCKBACK_SPEED);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.AEROBLAST_SPEED_DURATION.mEffectName, DURATION);
		mSpeed = SPEED_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.AEROBLAST_SPEED_AMPLIFIER.mEffectName);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.AEROBLAST_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mSize = CharmManager.getRadius(mPlayer, CharmEffects.AEROBLAST_SIZE.mEffectName, SIZE);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();
		new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.075f).spawnAsPlayerActive(mPlayer);
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(mDuration, mSpeed, PERCENT_SPEED_EFFECT_NAME));

		Location startLoc = mPlayer.getEyeLocation();
		Location slightOffsetLoc = startLoc.clone().add(startLoc.clone().getDirection().multiply(0.25));
		Location endLoc = LocationUtils.rayTraceToBlock(mPlayer, mSize);
		World world = startLoc.getWorld();
		world.playSound(slightOffsetLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.25f, 1.5f);
		world.playSound(slightOffsetLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.25f, 1.5f);
		world.playSound(slightOffsetLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.25f, 0.5f);
		world.playSound(slightOffsetLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.25f, 0.7f);

		for (LivingEntity mob : Hitbox.approximateCylinder(startLoc, endLoc, mSize / 2, false).accuracy(0.5).getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
			if (!EntityUtils.isBoss(mob) && !ScoreboardUtils.checkTag(mob, "callicarpa_flower") && !ScoreboardUtils.checkTag(mob, "hoglin_menace")
				&& mob.hasAI() && !ScoreboardUtils.checkTag(mob, TrainingDummyBoss.identityTag)) {
				Vector vel = mPlayer.getEyeLocation().getDirection().setY(0.15).multiply(mKnockbackSpeed);
				mob.setVelocity(vel);

				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						if (mTicks > 15) {
							this.cancel();
						}

						BoundingBox offsetBox = mob.getBoundingBox().clone();
						offsetBox.shift(mPlayer.getEyeLocation().getDirection().getX(), 0.15, mPlayer.getEyeLocation().getDirection().getZ());
						offsetBox.expand(-0.05, -0.4, -0.05);

						if (LocationUtils.collidesWithBlocks(offsetBox, mob.getWorld())) {
							// handle collision
							explode(mob, world);
							this.cancel();
							return;
						}
						mTicks++;
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}

		new BukkitRunnable() {
			final Location mLoc = mPlayer.getEyeLocation();
			final Vector mIncrement = mLoc.getDirection().multiply(1.5);

			@Override
			public void run() {
				for (int i = 0; i < 4; i++) {
					mLoc.add(mIncrement.clone().multiply(0.25));

					if (LocationUtils.collidesWithBlocks(BoundingBox.of(mLoc.clone().add(0.4, 0.4, 0.4), mLoc.clone().add(-0.4, -0.4, -0.4)), mLoc.getWorld(), false) || mLoc.distance(startLoc) > mSize) {
						new PartialParticle(Particle.EXPLOSION_NORMAL, mLoc, 20, 0.05, 0.05, 0.05, 0.2).spawnAsPlayerActive(mPlayer);
						this.cancel();
						break;
					}
				}
				new PartialParticle(Particle.CLOUD, mLoc.clone(), 1, 0.1, 0.1, 0.1, 0.1).spawnAsPlayerActive(mPlayer);
				ParticleUtils.drawParticleCircleExplosion(mPlayer, mLoc.clone().setDirection(mIncrement), 0, mSize, 0, 90, 30, 0.275f,
					true, 0, 0, Particle.EXPLOSION_NORMAL);

				for (int i = 0; i < 3; i++) {
					Vector dir = VectorUtils.rotateTargetDirection(mLoc.getDirection(), 90, FastUtils.randomDoubleInRange(-225, 45));
					Location l = mLoc.clone().add(dir.multiply(FastUtils.randomDoubleInRange(0.9, mSize / 2)));
					ParticleUtils.drawParticleLineSlash(l, l.getDirection(), 0, 1.5, 0.1, 5,
						(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.ELECTRIC_SPARK, lineLoc, 1, 0, 0, 0, 0.05)
							.spawnAsPlayerActive(mPlayer));
				}

			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	private void explode(LivingEntity mob, World world) {
		DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
		new PartialParticle(Particle.CRIT, mob.getLocation().add(0, 1, 0), 100, 0.1, 0.1, 0.1, 1.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_LARGE, mob.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
		world.playSound(mob.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 2f, 1.5f);
		world.playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2f, 0.5f);
		world.playSound(mob.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 2.25f, 0.5f);
		world.playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2.25f, 0.5f);
	}

	private static Description<Aeroblast> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Aeroblast>(color)
			.add("Right click while sneaking to blow a front-facing gust of wind, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to all mobs up to ")
			.add(a -> a.mSize, SIZE)
			.add(" blocks away, and knocking back all non-boss enemies. Deal an additional ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage if enemies hit by this ability collide with a wall within 0.75 seconds. " +
				"Additionally, casting this ability grants you ")
			.addPercent(a -> a.mSpeed, SPEED_AMPLIFIER)
			.add(" speed for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

}

