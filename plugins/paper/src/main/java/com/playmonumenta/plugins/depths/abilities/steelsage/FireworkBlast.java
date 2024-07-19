package com.playmonumenta.plugins.depths.abilities.steelsage;

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
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class FireworkBlast extends DepthsAbility {
	public static final String ABILITY_NAME = "Firework Blast";
	private static final int COOLDOWN = 12 * 20;
	private static final int[] DAMAGE = {12, 16, 20, 24, 28, 36};
	private static final int[] DAMAGE_CAP = {24, 32, 40, 48, 56, 72};
	private static final double DAMAGE_INCREASE_PER_BLOCK = 0.05;
	private static final double RADIUS = 4;

	public static final String CHARM_COOLDOWN = "Firework Blast Cooldown";

	public static final DepthsAbilityInfo<FireworkBlast> INFO =
		new DepthsAbilityInfo<>(FireworkBlast.class, ABILITY_NAME, FireworkBlast::new, DepthsTree.STEELSAGE, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.FIREWORK_BLAST)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FireworkBlast::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.FIREWORK_ROCKET)
			.descriptions(FireworkBlast::getDescription);

	private final double mBaseDamage;
	private final double mDamagePerBlock;
	private final double mDamageCap;
	private final double mRadius;

	public FireworkBlast(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mBaseDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FIREWORK_BLAST_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mDamagePerBlock = DAMAGE_INCREASE_PER_BLOCK + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.FIREWORK_BLAST_DAMAGE_PER_BLOCK.mEffectName);
		mDamageCap = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FIREWORK_BLAST_DAMAGE_CAP.mEffectName, DAMAGE_CAP[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.FIREWORK_BLAST_RADIUS.mEffectName, RADIUS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation().add(mPlayer.getEyeLocation().getDirection());
		Vector dir = loc.getDirection();

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1.25f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20).extra(0.12).spawnAsPlayerActive(mPlayer);

		Vector up = VectorUtils.rotateTargetDirection(dir, 0, 90);
		Vector right = dir.getCrossProduct(up);
		new PPCircle(Particle.DUST_COLOR_TRANSITION, loc, 0.7).data(new DustTransition(Color.WHITE, Color.BLACK, 1.0f))
			.axes(up, right).countPerMeter(8).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.DUST_COLOR_TRANSITION, loc.clone().add(dir), 0.5).data(new DustTransition(Color.WHITE, Color.BLACK, 1.0f))
			.axes(up, right).countPerMeter(8).spawnAsPlayerActive(mPlayer);

		new BukkitRunnable() {
			int mTicks = 0;
			final Location mLoc = loc.clone();
			final Location mStartLoc = loc.clone();
			final Vector mDir = dir.clone();
			Vector mSpiral1 = VectorUtils.rotateTargetDirection(mDir, 0, -90).multiply(0.5);
			Vector mSpiral2 = VectorUtils.rotateTargetDirection(mDir, 0, -90).multiply(0.8);
			@Override
			public void run() {
				for (int i = 0; i < 4; i++) {
					new PPLine(Particle.DUST_COLOR_TRANSITION, mLoc, mDir, 0.5).data(new DustTransition(Color.WHITE, Color.BLACK, 1.0f))
						.countPerMeter(4).delta(0.1).spawnAsPlayerActive(mPlayer);
					new PPPeriodic(Particle.DUST_COLOR_TRANSITION, mLoc).count(3).delta(0.05).data(new DustTransition(Color.WHITE, Color.BLACK, 1.2f)).spawnAsPlayerActive(mPlayer);
					if (i % 2 == 0) {
						Vector sparkDir = VectorUtils.randomUnitVector().add(mDir);
						new PPPeriodic(Particle.FIREWORKS_SPARK, mLoc).count(1).extra(0.2)
							.directionalMode(true).delta(sparkDir.getX(), sparkDir.getY(), sparkDir.getZ()).spawnAsPlayerActive(mPlayer);
					}

					int extraFireworks = 0;
					if (mTicks > 3) {
						new PPPeriodic(Particle.CRIT, mLoc).count(1).delta(0.1).spawnAsPlayerActive(mPlayer);
						new PPPeriodic(Particle.CRIT, mLoc.clone().add(mSpiral1)).count(1).spawnAsPlayerActive(mPlayer);
						extraFireworks = 2;
					}
					if (mTicks > 8) {
						new PPPeriodic(Particle.SPELL_INSTANT, mLoc).count(1).delta(0.15).spawnAsPlayerActive(mPlayer);
						new PPPeriodic(Particle.SPELL_INSTANT, mLoc.clone().add(mSpiral2)).count(1).spawnAsPlayerActive(mPlayer);
						extraFireworks = 3;
					}
					mSpiral1 = mSpiral1.rotateAroundAxis(mDir, Math.PI / 16);
					mSpiral2 = mSpiral2.rotateAroundAxis(mDir, -Math.PI / 16);

					mLoc.add(mDir.clone().multiply(0.45));

					Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, 0.5);
					if (!hitbox.getHitMobs().isEmpty() || LocationUtils.collidesWithBlocks(BoundingBox.of(mLoc.clone().add(0.25, 0.25, 0.25), mLoc.clone().add(-0.5, -0.25, -0.25)), mLoc.getWorld(), false)) {
						explode(mLoc, extraFireworks);

						this.cancel();
						break;
					}
				}

				if (mTicks > 100) {
					this.cancel();
				}
				mTicks++;
			}

			private void explode(Location loc, int extraFireworks) {
				double dist = Math.max(0, mStartLoc.distance(loc) - 6);
				double mult = 1 + dist * mDamagePerBlock;
				double damage = Math.min(mBaseDamage * mult, mDamageCap);

				for (LivingEntity mob : new Hitbox.SphereHitbox(loc, mRadius).getHitMobs()) {
					DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, damage, ClassAbility.FIREWORK_BLAST, true, true);
					MovementUtils.knockAway(loc, mob, 0.4f);
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, LocationUtils.getEntityCenter(mob), 20).delta(0.5).data(new DustTransition(Color.WHITE, Color.BLACK, 1.2f)).spawnAsPlayerActive(mPlayer);
				}

				World world = loc.getWorld();
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 2.0f, 1.2f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 0.5f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 0.75f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 2.0f, 0.7f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 2.0f, 1.0f);

				Color randomColor = List.of(Color.WHITE, Color.GRAY, Color.fromRGB(0, 0, 0)).get(FastUtils.randomIntInRange(0, 2));
				Firework rocket = (Firework) mPlayer.getWorld().spawnEntity(loc, EntityType.FIREWORK);
				FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(randomColor).build();
				FireworkMeta meta = rocket.getFireworkMeta();
				meta.addEffect(effect);
				rocket.setFireworkMeta(meta);
				rocket.detonate();

				if (extraFireworks > 0) {
					new BukkitRunnable() {
						int mExplosions = 0;
						final Location mLoc = loc.clone();

						@Override
						public void run() {
							Location loc = LocationUtils.varyInUniform(mLoc, 2.5);
							int i = 0;
							while (LocationUtils.collidesWithSolid(loc)) {
								loc = LocationUtils.varyInUniform(mLoc, 2.5);
								i++;
								if (i > 5) {
									break;
								}
							}

							Color randomColor = List.of(Color.WHITE, Color.GRAY, Color.fromRGB(0, 0, 0)).get(FastUtils.randomIntInRange(0, 2));
							Firework rocket = (Firework) mPlayer.getWorld().spawnEntity(loc, EntityType.FIREWORK);
							FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(randomColor).build();
							FireworkMeta meta = rocket.getFireworkMeta();
							meta.addEffect(effect);
							rocket.setFireworkMeta(meta);
							rocket.detonate();

							new PartialParticle(Particle.ELECTRIC_SPARK, loc, 30, 0, 0, 0, 2).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.FLASH, loc, 1).spawnAsPlayerActive(mPlayer);
							world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 1.2f);

							mExplosions++;
							if (mExplosions >= extraFireworks) {
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 2, 2);
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	private static Description<FireworkBlast> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<FireworkBlast>(color)
			.add("Right click while sneaking to shoot a firework that deals ")
			.addDepthsDamage(a -> a.mBaseDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage to enemies within ")
			.add(RADIUS)
			.add(" blocks of its explosion. The damage is increased by ")
			.addPercent(a -> a.mDamagePerBlock, DAMAGE_INCREASE_PER_BLOCK)
			.add(" for every block the firework travels past 6 blocks, up to ")
			.addDepthsDamage(a -> a.mDamageCap, DAMAGE_CAP[rarity - 1], true)
			.add(" damage.")
			.addCooldown(COOLDOWN);
	}
}
