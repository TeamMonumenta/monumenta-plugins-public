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
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GravityBomb extends DepthsAbility {
	public static final String ABILITY_NAME = "Gravity Bomb";
	public static final String BOMB_NAME = "GravityBomb";
	public static final int COOLDOWN = 22 * 20;
	public static final int DURATION = 8 * 20;
	public static final double[] DAMAGE = {20, 25, 30, 35, 40, 50};
	public static final int RADIUS = 7;

	public static final String CHARM_COOLDOWN = "Gravity Bomb Cooldown";

	public static final DepthsAbilityInfo<GravityBomb> INFO =
		new DepthsAbilityInfo<>(GravityBomb.class, ABILITY_NAME, GravityBomb::new, DepthsTree.STEELSAGE, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.GRAVITY_BOMB)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GravityBomb::cast, DepthsTrigger.SWAP))
			.displayItem(Material.GRAY_GLAZED_TERRACOTTA)
			.descriptions(GravityBomb::getDescription);

	private final double mDamage;
	private final double mRadius;

	public GravityBomb(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.GRAVITY_BOMB_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.GRAVITY_BOMB_RADIUS.mEffectName, RADIUS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();


		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.8f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.0f, 1.8f);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.0f, 1.8f), 2);

		launchBomb(VectorUtils.rotateYAxis(mPlayer.getEyeLocation().getDirection().normalize().multiply(0.87), 25.5));
		launchBomb(VectorUtils.rotateYAxis(mPlayer.getEyeLocation().getDirection().normalize().multiply(0.87), -25.5));

		return true;
	}

	private void launchBomb(Vector direction) {
		Location loc = mPlayer.getLocation();
		Vector dir = mPlayer.getEyeLocation().getDirection();

		Location summonLoc = loc.clone().add(0, 2.25, 0).add(dir);
		if (LocationUtils.collidesWithSolid(summonLoc)) {
			summonLoc = mPlayer.getEyeLocation().subtract(dir.multiply(0.5));
		}
		LivingEntity bomb = (LivingEntity) LibraryOfSoulsIntegration.summon(summonLoc, BOMB_NAME);
		if (bomb == null) {
			return;
		}
		EntityUtils.selfRoot(bomb, 9999 * 20);
		bomb.setVelocity(direction);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			bomb.setVelocity(new Vector(0, 0, 0));
		}, 20L);

		EnumSet<DamageType> allButProj = DamageType.getEnumSet();
		allButProj.remove(DamageType.PROJECTILE);
		mPlugin.mEffectManager.addEffect(bomb, "GravityBombOnlyProjectile", new PercentDamageReceived(9999 * 20, -1, allButProj));
		// 0.5s invulnerability so it doesn't get triggered immediately
		mPlugin.mEffectManager.addEffect(bomb, "GravityBombImmunity", new PercentDamageReceived(15, -1));

		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			final LivingEntity mBomb = bomb;

			@Override
			public void run() {
				if (mBomb == null) {
					this.cancel();
					return;
				}


				if (mTicks % 5 == 0) {
					new PPPeriodic(Particle.FALLING_DUST, LocationUtils.getEntityCenter(mBomb)).count(2).data(Material.GRAY_CONCRETE.createBlockData()).spawnAsPlayerActive(mPlayer);
					new PPPeriodic(Particle.PORTAL, LocationUtils.getEntityCenter(mBomb)).count(2).extra(0.5).spawnAsPlayerActive(mPlayer);
				}

				if (mTicks % 20 == 0) {
					float pitch = 1.625f + mTicks / 320f;
					mBomb.getWorld().playSound(loc, Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.PLAYERS, 0.4f, pitch);
				}

				List<LivingEntity> mobs = new Hitbox.SphereHitbox(mBomb.getLocation(), mRadius).getHitMobs();
				mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
				for (LivingEntity mob : mobs) {
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, true));
				}

				if (mBomb.isDead() || mTicks >= DURATION) {
					doExplosion(mBomb.getLocation());
					mBomb.remove();
					this.cancel();
					return;
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private void doExplosion(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.5f, 2.0f);
		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.5f, 2.0f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1.5f, 1.2f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.5f, 1.2f);
		new PartialParticle(Particle.PORTAL, loc, 150, 0, 0, 0, 7).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SQUID_INK, loc, 75).delta(mRadius / 2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FALLING_DUST, loc, 75).delta(mRadius / 2).data(Material.GRAY_CONCRETE.createBlockData()).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLASH, loc, 1).spawnAsPlayerActive(mPlayer);

		ParticleUtils.drawSphere(loc, 7, 4,
			(l, t) -> {
				Vector v = loc.clone().subtract(l).toVector().normalize();
				new PartialParticle(Particle.ELECTRIC_SPARK, LocationUtils.varyInUniform(l, 0.5), 1).directionalMode(true)
					.delta(v.getX(), v.getY(), v.getZ()).extra(5).spawnAsPlayerActive(mPlayer);
			});
		ParticleUtils.drawSphere(loc, 7, 7,
			(l, t) -> {
				l = LocationUtils.varyInUniform(l, 0.5);
				Vector v = loc.clone().subtract(l).toVector().normalize();
				new PartialParticle(Particle.WAX_OFF, l, 1).directionalMode(true)
					.delta(v.getX(), v.getY() * 0.5, v.getZ()).extra(70).spawnAsPlayerActive(mPlayer);
			});


		List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, mRadius).getHitMobs();
		mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
		for (LivingEntity mob : mobs) {
			DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
			MovementUtils.pullTowards(loc, mob, 0.4f);
		}

		for (int i = 0; i < 12; i++) {
			sparkParticle(loc, VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 2)), 0.8f);
		}

		new BukkitRunnable() { // continue pulling for a bit
			int mTicks = 0;
			final Location mLoc = loc.clone();

			@Override
			public void run() {
				if (mTicks == 3 || mTicks == 5) {
					for (int i = 0; i < 5; i++) {
						sparkParticle(loc, VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(0.6, 1.2)), 0.6f);
					}
				}

				if (mTicks > 5) {
					List<LivingEntity> mobs = new Hitbox.SphereHitbox(mLoc, mRadius).getHitMobs();
					mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));

					for (LivingEntity mob : mobs) {
						MovementUtils.pullTowardsNormalized(mLoc, mob, 0.6f);
					}
				}

				if (mTicks > 10) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void sparkParticle(Location loc, Vector dir, float size) {
		Location location = loc.clone();
		Vector direction = dir.clone();

		for (int i = 0; i < 4; i++) {
			Location oldLocation = location.clone();
			location.add(direction.multiply(0.9)).add(FastUtils.randomDoubleInRange(-0.5, 0.5), FastUtils.randomDoubleInRange(-0.5, 0.5), FastUtils.randomDoubleInRange(-0.5, 0.5));

			new PPLine(Particle.DUST_COLOR_TRANSITION, oldLocation, location).data(new DustTransition(Color.WHITE, Color.PURPLE, size))
				.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(mPlayer);
		}
	}

	private static Description<GravityBomb> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<GravityBomb>(color)
			.add("Swap hands to launch two flying bombs that arm after 0.75 seconds and explode after ")
			.addDuration(DURATION)
			.add(" seconds. Enemies near a bomb are afflicted with Slow Falling. Hitting a bomb with a projectile detonates it early, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage to all mobs in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius and pulling them to the center of the explosion.")
			.addCooldown(COOLDOWN);
	}
}
