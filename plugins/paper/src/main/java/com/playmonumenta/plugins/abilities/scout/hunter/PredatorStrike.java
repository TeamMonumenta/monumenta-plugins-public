package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PredatorStrikeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enchantments.Recoil;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;


public class PredatorStrike extends Ability implements AbilityWithDuration {
	private static final double COOLDOWN_REDUCTION = 0.25;
	private static final int COOLDOWN = TICKS_PER_SECOND * 16;
	private static final double DAMAGE = 24;
	private static final double DAMAGE_MULTIPLIER = 1.0;
	private static final double DAMAGE_SPLINTER = 26;
	private static final double DAMAGE_SPLINTER_MULTIPLIER = 1.2;
	private static final int MAX_RANGE = 30;
	private static final double EXPLODE_RADIUS = 3;
	private static final double EXPLODE_KNOCKBACK = 0.25;
	private static final int PIERCING = 0;
	private static final int DURATION = TICKS_PER_SECOND * 5;
	private static final double SPLINTER_CONE = 60;
	private static final double SPLINTER_RADIUS = 6;
	private static final double SPLINTER_REQUIREMENT = 4;

	public static final String CHARM_COOLDOWN = "Predator Strike Cooldown";
	public static final String CHARM_DAMAGE = "Predator Strike Damage";
	public static final String CHARM_RADIUS = "Predator Strike Radius";
	public static final String CHARM_RANGE = "Predator Strike Range";
	public static final String CHARM_KNOCKBACK = "Predator Strike Knockback";
	public static final String CHARM_PIERCING = "Predator Strike Enemies Pierced";
	public static final String CHARM_SPLINTER_RADIUS = "Predator Strike Splinter Radius";
	public static final String CHARM_SPLINTER_CONE = "Predator Strike Splinter Cone";
	public static final String CHARM_SPLINTER_REQUIREMENT = "Predator Strike Splinter Distance Requirement";
	public static final String CHARM_COOLDOWN_REDUCTION = "Predator Strike Splinter Cooldown Reduction Multiplier";

	public static final AbilityInfo<PredatorStrike> INFO =
		new AbilityInfo<>(PredatorStrike.class, "Predator Strike", PredatorStrike::new)
			.linkedSpell(ClassAbility.PREDATOR_STRIKE)
			.scoreboardId("PredatorStrike")
			.shorthandName("PrS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Upon activation, your next shot will travel instantly and cause a powerful explosion.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			// Put this trigger first so that they can be made the same for convenience
			.addTrigger(new AbilityTriggerInfo<>("unprime", "unprime", PredatorStrike::unprime,
				new AbilityTrigger(AbilityTrigger.Key.DROP).enabled(false).sneaking(true),
				new AbilityTriggerInfo.TriggerRestriction("Predator Strike is primed", player -> {
					PredatorStrike predatorStrike = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, PredatorStrike.class);
					return predatorStrike != null && predatorStrike.mDeactivationRunnable != null;
				})))
			.addTrigger(new AbilityTriggerInfo<>("cast", "prime", PredatorStrike::prime,
				new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.SPECTRAL_ARROW);

	protected @Nullable BukkitRunnable mDeactivationRunnable = null;
	private final double mRange;
	private final double mDamageMultiplier;
	private final double mDamage;
	private final double mExplodeRadius;
	private final float mKnockback;
	private final double mSplinterRadius;
	private final double mSplinterCone;
	private final double mSplinterRequirement;
	private final double mSplinterDamageMultiplier;
	private final double mSplinterDamage;
	private final double mCooldownReduction;
	private @Nullable Sharpshooter mSharpshooter;

	private int mCurrDuration = -1;
	private int mLastPrimeTick = 0;

	private final PredatorStrikeCS mCosmetic;

	public PredatorStrike(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, MAX_RANGE);
		mExplodeRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, EXPLODE_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new PredatorStrikeCS());
		mDamageMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_MULTIPLIER);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, EXPLODE_KNOCKBACK);
		mSplinterDamageMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_SPLINTER_MULTIPLIER);
		mSplinterDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_SPLINTER);
		mSplinterCone = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPLINTER_CONE, SPLINTER_CONE);
		mSplinterRadius = CharmManager.getRadius(mPlayer, CHARM_SPLINTER_RADIUS, SPLINTER_RADIUS);
		mSplinterRequirement = CharmManager.getRadius(mPlayer, CHARM_SPLINTER_REQUIREMENT, SPLINTER_REQUIREMENT);
		mCooldownReduction = COOLDOWN_REDUCTION + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_COOLDOWN_REDUCTION);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mSharpshooter = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Sharpshooter.class);
		});
	}

	public boolean prime() {
		if (isOnCooldown()) {
			return false;
		}

		if (mDeactivationRunnable == null) {
			if (Bukkit.getCurrentTick() - mLastPrimeTick > 10) {
				mCosmetic.strikeSoundReady(mPlayer.getWorld(), mPlayer);
			}
		} else {
			mDeactivationRunnable.cancel();
		}
		mCurrDuration = 0;
		mLastPrimeTick = Bukkit.getCurrentTick();

		mDeactivationRunnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				mCurrDuration++;
				mCosmetic.strikeTick(mPlayer, mTicks);

				if (mTicks >= DURATION) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mDeactivationRunnable = null;
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, PredatorStrike.this);
			}
		};
		cancelOnDeath(mDeactivationRunnable.runTaskTimer(mPlugin, 0, 1));
		ClientModHandler.updateAbility(mPlayer, this);
		return true;
	}

	public boolean unprime() {
		if (mDeactivationRunnable != null) {
			mDeactivationRunnable.cancel();
			mCosmetic.onUnprime(mPlayer, mPlayer.getLocation());
			return true;
		}
		return false;
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (mDeactivationRunnable == null || !EntityUtils.isAbilityTriggeringProjectile(projectile, true) || projectile.hasMetadata(QuiverStorm.ARROW_METADATA)) {
			return true;
		}
		if (Grappling.playerHoldingHook(mPlayer)) {
			return true;
		}

		if (mSharpshooter != null) {
			mSharpshooter.doNotTrack(projectile);
		}

		ItemStatManager.PlayerItemStats stats = Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		DamageListener.appendProjectileStats(stats, projectile);

		AbilityUtils.removeProjectile(projectile);
		mPlugin.mProjectileEffectTimers.removeEntity(projectile);

		mDeactivationRunnable.cancel();

		int cooldown = getModifiedCooldown();

		// Check if anything is within splinter range first
		if (isLevelTwo() && !mPlayer.isSneaking()) {
			Location pLoc = mPlayer.getEyeLocation();
			Vector actualDir = NmsUtils.getVersionAdapter().getActualDirection(mPlayer).normalize();
			pLoc.setDirection(actualDir);
			Hitbox hitbox = Hitbox
				.approximateCylinder(pLoc, pLoc.clone().add(actualDir.multiply(mSplinterRequirement)), 2.5, true)
				.accuracy(0.5);

			if (!hitbox.getHitMobs().isEmpty()) {
				if (mSharpshooter != null) {
					mSharpshooter.addStacks(Sharpshooter.checkSharpshooterType(projectile, mPlayer.getInventory().getItemInMainHand()));
				}
				putOnCooldown((int) (cooldown * (1 - mCooldownReduction)));
				projectile.addScoreboardTag("NoRecoil");
				predatorSplinter(stats);
				return true;
			}
		}

		putOnCooldown(cooldown);
		mCosmetic.strikeLaunch(mPlayer.getWorld(), mPlayer);
		predatorStrike(projectile, stats);

		return true;
	}

	private void predatorSplinter(ItemStatManager.PlayerItemStats stats) {
		Location pLoc = mPlayer.getEyeLocation();

		mCosmetic.strikeSplinter(mPlayer, pLoc.clone().add(pLoc.getDirection()), mSplinterCone, mSplinterRadius);

		Hitbox hitbox = Hitbox.approximateCone(pLoc, mSplinterRadius, Math.toRadians(mSplinterCone));

		for (LivingEntity e : hitbox.getHitMobs()) {
			double damage = AbilityUtils.projectileFinalDamage(stats, mPlayer, e, mSplinterDamage, mSplinterDamageMultiplier);

			MovementUtils.knockAway(pLoc, e, mKnockback * 2, mKnockback * 2, true);
			DamageUtils.damage(mPlayer, e,
				new DamageEvent.Metadata(DamageType.PROJECTILE_SKILL, ClassAbility.PREDATOR_STRIKE, stats), damage,
				true, true, false);
		}

		if (!ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			ItemStack item = mPlayer.getInventory().getItemInMainHand();
			double recoil = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.RECOIL);

			Vector velocity = Recoil.getRecoilVector(mPlayer, recoil + 5);
			mPlayer.setFallDistance(0);
			mPlayer.setVelocity(velocity);
		}

		playAspectSound(pLoc);
	}

	private void predatorStrike(Projectile proj, ItemStatManager.PlayerItemStats stats) {
		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();
		World world = loc.getWorld();

		int piercing = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PIERCING, PIERCING);
		List<LivingEntity> piercedMobs = new ArrayList<>();

		RayTraceResult result = world.rayTrace(loc, direction, mRange, FluidCollisionMode.NEVER, true, 0.425,
			e -> (WindBomb.isWindBomb(e)
				|| (EntityUtils.isHostileMob(e)
				&& !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG)))
				&& !e.isDead()
				&& e.isValid());

		while (piercing > 0) {
			// if we hit a block then just stop
			if (result == null || result.getHitBlock() != null) {
				break;
			}

			Entity hitEntity = result.getHitEntity();
			if (hitEntity instanceof LivingEntity) {
				piercedMobs.add((LivingEntity) hitEntity);
				piercing--;
			}

			result = world.rayTrace(loc, direction, mRange, FluidCollisionMode.NEVER, true, 0.425,
				e -> (WindBomb.isWindBomb(e)
					|| (EntityUtils.isHostileMob(e)
					&& !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG)))
					&& !e.isDead()
					&& e.isValid()
					&& !piercedMobs.contains((LivingEntity) e));
		}

		Location endLoc;
		if (result == null) {
			endLoc = loc.clone().add(direction.multiply(mRange));
		} else {
			endLoc = result.getHitPosition().toLocation(world);
		}

		Bukkit.getScheduler().runTask(mPlugin, () -> {
			mCosmetic.strikeImpact(() -> mCosmetic.strikeExplode(world, mPlayer, endLoc, mExplodeRadius), endLoc, mPlayer);
			explode(endLoc, piercedMobs, proj, stats);
			mCosmetic.strikeParticleLine(mPlayer, loc, endLoc);
		});
	}

	private void explode(Location loc, List<LivingEntity> piercedMobs, Projectile proj, ItemStatManager.PlayerItemStats stats) {
		// go through pierced mobs and use their locations
		for (LivingEntity piercedMob : piercedMobs) {
			if (WindBomb.attemptHit(piercedMob)) {
				continue;
			}

			double damage = AbilityUtils.projectileFinalDamage(stats, mPlayer, piercedMob, mSplinterDamage, mSplinterDamageMultiplier);

			MovementUtils.knockAway(mPlayer.getLocation(), piercedMob, mKnockback, mKnockback, true);
			DamageUtils.damage(mPlayer, piercedMob,
				new DamageEvent.Metadata(DamageType.PROJECTILE_SKILL, ClassAbility.PREDATOR_STRIKE, stats), damage,
				true, true, false);

		}

		// go through exploded mobs and use the explosion's location
		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mExplodeRadius);

		Predicate<LivingEntity> isWindBomb = WindBomb::isWindBomb;
		List<LivingEntity> mobs = hitbox.getHitMobsInclude(isWindBomb);
		// prevents stacked damage instances
		mobs.removeIf(piercedMobs::contains);

		if (!mobs.isEmpty()) {
			for (LivingEntity mob : mobs) {
				if (WindBomb.attemptHit(mob)) {
					continue;
				}
				double damage = AbilityUtils.projectileFinalDamage(stats, mPlayer, mob, mDamage, mDamageMultiplier);

				MovementUtils.knockAway(loc, mob, mKnockback, mKnockback, true);
				DamageUtils.damage(mPlayer, mob,
					new DamageEvent.Metadata(DamageType.PROJECTILE_SKILL, ClassAbility.PREDATOR_STRIKE, stats), damage,
					true, true, false);
			}
		}

		if (mSharpshooter != null) {
			if (!piercedMobs.isEmpty() || !mobs.isEmpty()) {
				mSharpshooter.addStacks(Sharpshooter.checkSharpshooterType(proj, mPlayer.getInventory().getItemInMainHand()));
			} else {
				mSharpshooter.miss();
			}
		}
		//Get enchant levels on weapon

		playAspectSound(loc);
	}

	private void playAspectSound(Location loc) {
		ItemStack item = mPlayer.getInventory().getItemInMainHand();

		ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FIRE_ASPECT);
		int fire = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FIRE_ASPECT);
		int ice = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ICE_ASPECT);
		int thunder = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.THUNDER_ASPECT);
		int decay = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.DECAY);
		int bleed = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.BLEEDING);
		int earth = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.EARTH_ASPECT);
		int wind = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.WIND_ASPECT);

		if (ice > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 1.3f);
			new PartialParticle(Particle.SNOW_SHOVEL, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius).spawnAsPlayerActive(mPlayer);
		}
		if (thunder > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.6f, 0.8f);
			new PartialParticle(Particle.REDSTONE, loc, 12, mExplodeRadius, mExplodeRadius, mExplodeRadius, new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f)).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, loc, 12, mExplodeRadius, mExplodeRadius, mExplodeRadius, new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f)).spawnAsPlayerActive(mPlayer);
		}
		if (decay > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.4f, 0.7f);
			new PartialParticle(Particle.SQUID_INK, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius).spawnAsPlayerActive(mPlayer);
		}
		if (bleed > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.7f, 0.7f);
			new PartialParticle(Particle.REDSTONE, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius, new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f)).spawnAsPlayerActive(mPlayer);
		}
		if (wind > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.30f);
			new PartialParticle(Particle.CLOUD, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius).spawnAsPlayerActive(mPlayer);
		}
		if (earth > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			new PartialParticle(Particle.FALLING_DUST, loc, 12, mExplodeRadius, mExplodeRadius, mExplodeRadius, Material.COARSE_DIRT.createBlockData()).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, loc, 12, mExplodeRadius, mExplodeRadius, mExplodeRadius, new Particle.DustOptions(Color.fromRGB(120, 148, 82), 0.75f)).spawnAsPlayerActive(mPlayer);
		}
		if (fire > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_POP, SoundCategory.PLAYERS, 0.6f, 0.9f);
			new PartialParticle(Particle.LAVA, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius).spawnAsPlayerActive(mPlayer);
		}
	}

	public static boolean hasPredatorStrikeReady(Player player) {
		PredatorStrike pstrike = Plugin.getInstance().mAbilityManager.getPlayerAbilityIgnoringSilence(player, PredatorStrike.class);
		if (pstrike != null) {
			return pstrike.mDeactivationRunnable != null;
		}
		return false;
	}

	@Override
	public @Nullable String getMode() {
		return mDeactivationRunnable != null ? "active" : null;
	}

	@Override
	public int getInitialAbilityDuration() {
		return DURATION;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	private static Description<PredatorStrike> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger(1)
			.addDashedLine()
			.addLine("Prime a Predator Strike for %t.")
			.statValues(stat(DURATION))
			.addLine()
			.addLine("While primed, the next projectile you fire")
			.addLine("will travel instantly and explode on impact,")
			.addLine("dealing increased damage.")
			.addLine()
			.addStat("Damage: %d + %p (p) (of weapon damage),")
			.statValues(stat(a -> a.mDamage, DAMAGE), stat(a -> a.mDamageMultiplier, DAMAGE_MULTIPLIER))
			.addStat("Explosion Radius: %r")
			.statValues(stat(a -> a.mExplodeRadius, EXPLODE_RADIUS))
			.addStat("Max Range: %r")
			.statValues(stat(MAX_RANGE))
			.addStat("Cooldown: %t")
			.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<PredatorStrike> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("If a mob is within %d blocks then *Predator Strike*").styles(UNDERLINED)
			.statValues(stat(a -> a.mSplinterRequirement, SPLINTER_REQUIREMENT))
			.addLine("splinters into a short-ranged blast, dealing")
			.addLine("damage, knockback, and self-recoil.")
			.addLine("Refund %p of the cooldown when it splinters.")
			.statValues(stat(a -> a.mCooldownReduction, COOLDOWN_REDUCTION))
			.addLine("(Sneak to cancel the splinter)")
			.addLine()
			.addStat("Damage: %d + %p (p) (of weapon damage),")
			.statValues(stat(a -> a.mSplinterDamage, DAMAGE_SPLINTER), stat(a -> a.mSplinterDamageMultiplier, DAMAGE_SPLINTER_MULTIPLIER))
			.addStat("Radius: %r (Cone-Shaped)")
			.statValues(stat(a -> a.mSplinterRadius, SPLINTER_RADIUS))
			.addDashedLine();
	}
}
