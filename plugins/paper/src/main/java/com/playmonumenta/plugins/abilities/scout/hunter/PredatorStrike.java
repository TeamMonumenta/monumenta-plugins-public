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
import com.playmonumenta.plugins.abilities.scout.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PredatorStrikeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;


public class PredatorStrike extends Ability implements AbilityWithDuration {
	private static final int COOLDOWN_1 = TICKS_PER_SECOND * 18;
	private static final int COOLDOWN_2 = TICKS_PER_SECOND * 14;
	private static final double DAMAGE_MULTIPLIER = 2.0;
	private static final double DISTANCE_SCALE_1 = 0.1;
	private static final double DISTANCE_SCALE_2 = 0.15;
	private static final int MAX_RANGE = 30;
	private static final int MAX_DAMAGE_RANGE = 12;
	private static final double EXPLODE_RADIUS = 1.25;
	private static final double EXPLODE_KNOCKBACK = 0.25;
	private static final int PIERCING = 0;
	private static final int R2_CAP = 400;
	private static final int R3_CAP = 750;
	private static final int CAP_LEVEL_TWO_MULTIPLIER = 2;
	private static final int DURATION = TICKS_PER_SECOND * 5;

	public static final String CHARM_COOLDOWN = "Predator Strike Cooldown";
	public static final String CHARM_DAMAGE = "Predator Strike Damage";
	public static final String CHARM_RADIUS = "Predator Strike Radius";
	public static final String CHARM_RANGE = "Predator Strike Range";
	public static final String CHARM_KNOCKBACK = "Predator Strike Knockback";
	public static final String CHARM_PIERCING = "Predator Strike Enemies Pierced";
	public static final String CHARM_DAMAGE_RANGE = "Predator Strike Damage Scaling Range";
	public static final String CHARM_DISTANCE_SCALE = "Predator Strike Damage Per Block";
	public static final String CHARM_BASE_DAMAGE = "Predator Strike Base Damage Multiplier";

	public static final AbilityInfo<PredatorStrike> INFO =
		new AbilityInfo<>(PredatorStrike.class, "Predator Strike", PredatorStrike::new)
			.linkedSpell(ClassAbility.PREDATOR_STRIKE)
			.scoreboardId("PredatorStrike")
			.shorthandName("PrS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Upon activation, your next shot will travel instantly and deal more damage the further it travels.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			// Put this trigger first so that they can be made the same for convenience
			.addTrigger(new AbilityTriggerInfo<>("unprime", "unprime", PredatorStrike::unprime,
				new AbilityTrigger(AbilityTrigger.Key.DROP).enabled(false).sneaking(true),
				new AbilityTriggerInfo.TriggerRestriction("Predator Strike is primed", player -> {
					PredatorStrike predatorStrike = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, PredatorStrike.class);
					return predatorStrike != null && predatorStrike.mDeactivationRunnable != null;
				})))
			.addTrigger(new AbilityTriggerInfo<>("cast", "prime", PredatorStrike::prime,
				new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.SPECTRAL_ARROW);

	private @Nullable BukkitRunnable mDeactivationRunnable = null;
	private final double mRange;
	private final double mDistanceScale;
	private final double mDamageRange;
	private final double mBaseDamage;
	private final double mExplodeRadius;
	private @Nullable SwiftCuts mSwiftCuts;
	private int mCurrDuration = -1;
	private int mLastPrimeTick = 0;

	private final PredatorStrikeCS mCosmetic;

	public PredatorStrike(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, MAX_RANGE);
		mDistanceScale = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DISTANCE_SCALE, isLevelOne() ? DISTANCE_SCALE_1 : DISTANCE_SCALE_2);
		mExplodeRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, EXPLODE_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new PredatorStrikeCS());
		mDamageRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE_RANGE, MAX_DAMAGE_RANGE);
		mBaseDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_BASE_DAMAGE, DAMAGE_MULTIPLIER);

		Bukkit.getScheduler().runTask(plugin, () ->
			mSwiftCuts = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, SwiftCuts.class));
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
		if (mDeactivationRunnable == null || (!EntityUtils.isAbilityTriggeringProjectile(projectile, true) && !projectile.getScoreboardTags().contains(Quickdraw.SOURCE_QUICKDRAW_TAG))) {
			return true;
		}
		if (Grappling.playerHoldingHook(mPlayer)) {
			return true;
		}
		mDeactivationRunnable.cancel();

		int cooldown = getModifiedCooldown();
		if (mSwiftCuts != null && mSwiftCuts.isEnhancementActive()) {
			cooldown = (int) (cooldown * (1 - mSwiftCuts.getPredatorStrikeCDR()));
		}
		putOnCooldown(cooldown);

		projectile.remove();
		mPlugin.mProjectileEffectTimers.removeEntity(projectile);

		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();
		World world = loc.getWorld();
		mCosmetic.strikeLaunch(world, mPlayer);

		int piercing = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PIERCING, PIERCING);
		List<LivingEntity> piercedMobs = new ArrayList<>();

		RayTraceResult result = world.rayTrace(loc, direction, mRange, FluidCollisionMode.NEVER, true, 0.425,
			e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid());

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
				e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid() && !piercedMobs.contains((LivingEntity) e));
		}

		Location endLoc;
		if (result == null) {
			endLoc = loc.clone().add(direction.multiply(mRange));
		} else {
			endLoc = result.getHitPosition().toLocation(world);
		}

		Bukkit.getScheduler().runTask(mPlugin, () -> {
			mCosmetic.strikeImpact(() -> mCosmetic.strikeExplode(world, mPlayer, endLoc, mExplodeRadius), endLoc, mPlayer);
			explode(endLoc, piercedMobs, projectile);
			mCosmetic.strikeParticleLine(mPlayer, loc, endLoc);
		});
		return true;
	}

	private void explode(Location loc, List<LivingEntity> piercedMobs, Projectile projectile) {
		ItemStack item = mPlayer.getInventory().getItemInMainHand();
		final ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(projectile);

		// go through pierced mobs and use their locations
		for (LivingEntity piercedMob : piercedMobs) {
			if (playerItemStats != null) {
				final ItemStatManager.PlayerItemStats.ItemStatsMap map = playerItemStats.getItemStats();
				final ItemStat projDamageAdd = Objects.requireNonNull(AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat());
				double damage = map.get(projDamageAdd);
				damage += PointBlank.apply(mPlayer, piercedMob.getLocation(), map.get(Objects.requireNonNull(EnchantmentType.POINT_BLANK.getItemStat())));
				damage += Sniper.apply(mPlayer, piercedMob.getLocation(), map.get(Objects.requireNonNull(EnchantmentType.SNIPER.getItemStat())));
				damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
				damage *= mBaseDamage + mDistanceScale * Math.min(mPlayer.getLocation().distance(piercedMob.getLocation()), mDamageRange);

				float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, EXPLODE_KNOCKBACK);

				MovementUtils.knockAway(mPlayer.getLocation(), piercedMob, knockback, knockback, true);
				DamageUtils.damage(mPlayer, piercedMob, DamageType.PROJECTILE_SKILL, damage, mInfo.getLinkedSpell(), true);
			}
		}

		// go through exploded mobs and use the explosion's location
		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mExplodeRadius);
		List<LivingEntity> mobs = hitbox.getHitMobs();
		// prevents stacked damage instances
		mobs.removeIf(piercedMobs::contains);

		if (!mobs.isEmpty() && playerItemStats != null) {
			final ItemStatManager.PlayerItemStats.ItemStatsMap map = playerItemStats.getItemStats();
			final ItemStat projDamageAdd = Objects.requireNonNull(AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat());
			double damage = map.get(projDamageAdd);
			damage += PointBlank.apply(mPlayer, loc, map.get(Objects.requireNonNull(EnchantmentType.POINT_BLANK.getItemStat())));
			damage += Sniper.apply(mPlayer, loc, map.get(Objects.requireNonNull(EnchantmentType.SNIPER.getItemStat())));
			damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
			damage *= mBaseDamage + mDistanceScale * Math.min(mPlayer.getLocation().distance(loc), mDamageRange);

			float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, EXPLODE_KNOCKBACK);

			for (LivingEntity mob : mobs) {
				MovementUtils.knockAway(loc, mob, knockback, knockback, true);
				DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, damage, mInfo.getLinkedSpell(), true);
			}
		}

		//Get enchant levels on weapon
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

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		int regionCap = ServerProperties.getAbilityEnhancementsEnabled(mPlayer) ? R3_CAP : R2_CAP;
		int damageCap = isLevelOne() ? regionCap : regionCap * CAP_LEVEL_TWO_MULTIPLIER;

		if (event.getAbility() == ClassAbility.PREDATOR_STRIKE) {
			event.setDamageCap((double) damageCap);
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

	// Annoying to do the damage correctly here because of sniper/pb
	private static Description<PredatorStrike> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger(1)
			.addDashedLine()
			.addLine("Prime a Predator Strike for %t.")
				.statValues(stat(DURATION))
			.addLine()
			.addLine("While primed, the next projectile you fire")
			.addLine("will travel instantly and explode on impact,")
			.addLine("dealing increased damage, plus bonus damage")
			.addLine("per block traveled.")
			.addLine()
			.addStat("Damage: %p (p) (of weapon damage),")
				.statValues(stat(a -> a.mBaseDamage, DAMAGE_MULTIPLIER))
			.tab().addLine("+%p1 per block (max %d blocks)")
				.statValues(stat(a -> a.mDistanceScale, DISTANCE_SCALE_1), stat(a -> a.mDamageRange, MAX_DAMAGE_RANGE))
			.tab().addLine("(capped at %d damage)")
				.statValues(perRegion(R2_CAP, R3_CAP))
			.addStat("Explosion Radius: %r")
				.statValues(stat(a -> a.mExplodeRadius, EXPLODE_RADIUS))
			.addStat("Max Range: %r")
				.statValues(stat(MAX_RANGE))
			.addStat("Cooldown: %t1")
				.statValues(cooldown(COOLDOWN_1))
			.addDashedLine();
	}

	private static Description<PredatorStrike> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Predator Strike*'s bonus damage").styles(UNDERLINED)
			.addLine("scaling and reduce its cooldown.")
			.addLine()
			.addStatComparison("Bonus Damage: +%p1 -> +%p2 per block")
				.statValues(stat(DISTANCE_SCALE_1), stat(a -> a.mDistanceScale, DISTANCE_SCALE_2), stat(a -> a.mDamageRange, MAX_DAMAGE_RANGE))
			.tab().addLine("(capped at %d damage)")
			.statValues(perRegion(R2_CAP * CAP_LEVEL_TWO_MULTIPLIER, R3_CAP * CAP_LEVEL_TWO_MULTIPLIER))
			.addStatComparison("Cooldown: %t1 -> %t2")
				.statValues(cooldown(COOLDOWN_1), cooldown(COOLDOWN_2))
			.addDashedLine();
	}
}
