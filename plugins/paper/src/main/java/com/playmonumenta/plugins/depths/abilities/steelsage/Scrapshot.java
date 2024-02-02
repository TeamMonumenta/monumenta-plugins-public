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
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Scrapshot extends DepthsAbility {
	public static final String ABILITY_NAME = "Scrapshot";
	private static final int COOLDOWN = 10 * 20;
	private static final int[] DAMAGE = {30, 37, 45, 52, 60, 75};
	private static final double RECOIL_VELOCITY = 1;
	private static final int RANGE = 8;
	private static final double SHRAPNEL_DAMAGE_PERCENT = 0.33;
	private static final double SHRAPNEL_RANGE = 4;
	private static final double SHRAPNEL_CONE_ANGLE = 50;

	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(140, 140, 140), 1.0f);
	private static final Particle.DustOptions DARK_COLOR = new Particle.DustOptions(Color.fromRGB(100, 100, 100), 1.0f);

	public static final String CHARM_COOLDOWN = "Scrapshot Cooldown";

	public static final DepthsAbilityInfo<Scrapshot> INFO =
		new DepthsAbilityInfo<>(Scrapshot.class, ABILITY_NAME, Scrapshot::new, DepthsTree.STEELSAGE, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.SCRAPSHOT)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.actionBarColor(TextColor.color(130, 130, 130))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Scrapshot::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.NETHERITE_SCRAP)
			.descriptions(Scrapshot::getDescription);

	private final double mDamage;
	private final double mRange;
	private final double mShrapnelDamagePercent;
	private final double mShrapnelRange;
	private final double mShrapnelConeAngle;

	public Scrapshot(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SCRAPSHOT_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.SCRAPSHOT_RANGE.mEffectName, RANGE);
		mShrapnelDamagePercent = SHRAPNEL_DAMAGE_PERCENT;
		mShrapnelRange = SHRAPNEL_RANGE;
		mShrapnelConeAngle = Math.min(180, CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SCRAPSHOT_SHRAPNEL_CONE_ANGLE.mEffectName, SHRAPNEL_CONE_ANGLE));
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		World world = mPlayer.getWorld();

		RayTraceResult result = world.rayTrace(loc, dir, mRange, FluidCollisionMode.NEVER, true, 0.5,
			e -> e instanceof LivingEntity && EntityUtils.isHostileMob(e) && !e.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		Location endLoc;
		LivingEntity target = null;
		if (result == null) {
			endLoc = loc.clone().add(dir.clone().multiply(mRange));
		} else {
			endLoc = result.getHitPosition().toLocation(world);
			if (result.getHitEntity() instanceof LivingEntity le) {
				target = le;
			}
		}

		double mult = 0;
		if (target != null) {
			double dist = endLoc.distance(loc);
			mult = Math.min(1, (mRange * 1.25 - dist) / mRange);
			double damage = mult * mDamage;
			DamageUtils.damage(mPlayer, target, DamageType.PROJECTILE_SKILL, damage, mInfo.getLinkedSpell(), true, true);

			new PartialParticle(Particle.SQUID_INK, target.getLocation(), (int) ((mRange - dist) * 2), 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
			world.playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 0);

			new PartialParticle(Particle.EXPLOSION_LARGE, endLoc, 1).spawnAsPlayerActive(mPlayer);

			// shrapnel cone
			Location shrapnelOrigin = LocationUtils.getHalfHeightLocation(target).add(0, -mShrapnelRange, 0).setDirection(dir);
			Hitbox hitbox = Hitbox.approximateCylinderSegment(shrapnelOrigin, 2 * mShrapnelRange, mShrapnelRange, Math.toRadians(mShrapnelConeAngle));
			for (LivingEntity mob : hitbox.getHitMobs()) {
				if (mob == target) { // don't hit the target again
					continue;
				}
				DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, damage * mShrapnelDamagePercent, mInfo.getLinkedSpell(), true, true);
			}

			Location targetLocation = LocationUtils.getHalfHeightLocation(target);
			double degree = 90 - mShrapnelConeAngle;
			int degreeSteps = ((int) (2 * mShrapnelConeAngle)) / 12;
			double degreeStep = 2 * mShrapnelConeAngle / degreeSteps;
			for (int step = 0; step < degreeSteps + 1; step++, degree += degreeStep) {
				double radian1 = Math.toRadians(degree);
				Vector vec = new Vector(FastUtils.cos(radian1) * mShrapnelRange, 0, FastUtils.sin(radian1) * mShrapnelRange);
				vec = VectorUtils.rotateYAxis(vec, loc.getYaw()); // rotate to match the shot direction
				Location shrapnelEnd = targetLocation.clone().add(vec);

				new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(target), shrapnelEnd).countPerMeter(2).delta(0.1).data(LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
				new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(target), shrapnelEnd).countPerMeter(2).delta(0.1).data(DARK_COLOR).spawnAsPlayerActive(mPlayer);
				new PPLine(Particle.SMOKE_NORMAL, LocationUtils.getHalfHeightLocation(target), shrapnelEnd).countPerMeter(4).delta(0.15).spawnAsPlayerActive(mPlayer);
			}
		}

		Vector velocity = mPlayer.getLocation().getDirection().multiply(-CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SCRAPSHOT_VELOCITY.mEffectName, RECOIL_VELOCITY));
		mPlayer.setVelocity(velocity.setY(Math.max(0.1, velocity.getY())));

		Location startLoc = loc.clone().add(dir);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, startLoc, 0, 0.3, 0, 90, 30, 0.5f, true, 0, 1.5, Particle.SMOKE_NORMAL);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, startLoc, 0, 0.3, 0, 90, 40, 0.75f, true, 0, 2, Particle.SMOKE_NORMAL);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, startLoc, 0, 0.3, 0, 90, 50, 1.0f, true, 0, 2.5, Particle.SMOKE_NORMAL);
		new PPLine(Particle.SMOKE_NORMAL, startLoc, endLoc).countPerMeter(7).delta(0.1).extra(0.05).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).countPerMeter(4).delta(0.25).data(LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).countPerMeter(4).delta(0.25).data(DARK_COLOR).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1.4f);

		if (mult > 0.75) { // extra sounds for close-range high damage hits
			world.playSound(loc, Sound.BLOCK_IRON_DOOR_OPEN, SoundCategory.PLAYERS, 1f, 1.2f);
			world.playSound(loc, Sound.BLOCK_IRON_DOOR_CLOSE, SoundCategory.PLAYERS, 1f, 0.65f);
			world.playSound(loc, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1f, 1.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1f, 1.2f);
		}

		return true;
	}

	private static Description<Scrapshot> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Scrapshot>(color)
			.add("Left click while sneaking to fire a blunderbuss shot that goes up to ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks away, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage to the first mob and knocking you backwards. Damage is decreased based on distance if the distance is greater than 25% of the max range. ")
			.add("The shot splits into shrapnel after hitting a mob and deals ")
			.addPercent(a -> a.mShrapnelDamagePercent, SHRAPNEL_DAMAGE_PERCENT)
			.add(" of the initial damage dealt to all mobs in a ")
			.add(a -> a.mShrapnelRange, SHRAPNEL_RANGE)
			.add(" block cone behind the target.")
			.addCooldown(COOLDOWN);
	}
}
