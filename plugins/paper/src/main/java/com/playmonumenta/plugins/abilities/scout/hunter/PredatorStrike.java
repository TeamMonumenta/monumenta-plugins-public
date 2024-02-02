package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PredatorStrikeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
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
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
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


public class PredatorStrike extends Ability implements AbilityWithDuration {

	private static final int COOLDOWN_1 = 20 * 18;
	private static final int COOLDOWN_2 = 20 * 14;
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

	private static final int DURATION = 20 * 5;

	public static final String CHARM_COOLDOWN = "Predator Strike Cooldown";
	public static final String CHARM_DAMAGE = "Predator Strike Damage";
	public static final String CHARM_RADIUS = "Predator Strike Radius";
	public static final String CHARM_RANGE = "Predator Strike Range";
	public static final String CHARM_KNOCKBACK = "Predator Strike Knockback";
	public static final String CHARM_PIERCING = "Predator Strike Enemies Pierced";


	public static final AbilityInfo<PredatorStrike> INFO =
			new AbilityInfo<>(PredatorStrike.class, "Predator Strike", PredatorStrike::new)
					.linkedSpell(ClassAbility.PREDATOR_STRIKE)
					.scoreboardId("PredatorStrike")
					.shorthandName("PrS")
					.descriptions(
							String.format("Left-clicking with a projectile weapon while not sneaking will prime a Predator Strike that unprimes after 5s. " +
											"When you fire a critical projectile, it will instantaneously travel in a straight line " +
											"for up to %d blocks or until it hits an enemy or block and damages enemies in a %s block radius. " +
											"This ability deals %s%% of your projectile damage increased by %s%% for every block of distance from you and the target " +
											"(up to %d blocks, or %s%% total). Final damage cannot go above %d in Region 2 and %d in Region 3. Cooldown: %ds.",
									MAX_RANGE, EXPLODE_RADIUS, StringUtils.multiplierToPercentage(DAMAGE_MULTIPLIER), StringUtils.multiplierToPercentage(DISTANCE_SCALE_1), MAX_DAMAGE_RANGE,
									StringUtils.multiplierToPercentage(MAX_DAMAGE_RANGE * DISTANCE_SCALE_1 + DAMAGE_MULTIPLIER), R2_CAP, R3_CAP, COOLDOWN_1 / 20),
							String.format("Damage now increases by %s%% for each block of distance (up to %s%% in total). " +
								              "Final damage cap is increased to %d in Region 2 and %d in Region 3. Cooldown: %ds.",
									StringUtils.multiplierToPercentage(DISTANCE_SCALE_2), StringUtils.multiplierToPercentage(MAX_DAMAGE_RANGE * DISTANCE_SCALE_2 + DAMAGE_MULTIPLIER),
									R2_CAP * CAP_LEVEL_TWO_MULTIPLIER, R3_CAP * CAP_LEVEL_TWO_MULTIPLIER, COOLDOWN_2 / 20))
					.simpleDescription("Upon activation, your next shot will travel instantly and deal more damage the further it travels.")
					.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
					// Put this trigger first so that they can be made the same for convenience
					.addTrigger(new AbilityTriggerInfo<>("unprime", "unprime", PredatorStrike::unprime, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).enabled(false).sneaking(false),
							new AbilityTriggerInfo.TriggerRestriction("Predator Strike is primed", player -> {
								PredatorStrike predatorStrike = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, PredatorStrike.class);
								return predatorStrike != null && predatorStrike.mDeactivationRunnable != null;
							})))
					.addTrigger(new AbilityTriggerInfo<>("cast", "prime", PredatorStrike::prime, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false),
							AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
					.displayItem(Material.SPECTRAL_ARROW);

	private @Nullable BukkitRunnable mDeactivationRunnable = null;
	private final double mDistanceScale;
	private final double mExplodeRadius;
	private @Nullable SwiftCuts mSwiftCuts;
	private int mCurrDuration = 0;

	private final PredatorStrikeCS mCosmetic;

	public PredatorStrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDistanceScale = isLevelOne() ? DISTANCE_SCALE_1 : DISTANCE_SCALE_2;
		mExplodeRadius = CharmManager.getRadius(player, CHARM_RADIUS, EXPLODE_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PredatorStrikeCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mSwiftCuts = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, SwiftCuts.class);
		});
	}

	public boolean prime() {
		if (isOnCooldown()) {
			return false;
		}

		if (mDeactivationRunnable == null) {
			mCosmetic.strikeSoundReady(mPlayer.getWorld(), mPlayer);
		} else {
			mDeactivationRunnable.cancel();
		}
		mCurrDuration = 0;

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
		if (mDeactivationRunnable == null || !EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
			return true;
		}
		mDeactivationRunnable.cancel();
		putOnCooldown();

		if (mSwiftCuts != null && mSwiftCuts.isEnhancementActive()) {
			for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
				ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
				if (abil == this && linkedSpell != null) {
					int totalCD = abil.getModifiedCooldown();
					int reducedCD = (int) Math.floor(totalCD * (SwiftCuts.PREDATOR_STRIKE_CDR + CharmManager.getLevelPercentDecimal(mPlayer, SwiftCuts.CHARM_ENHANCE)));
					mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, reducedCD);
				}
			}
		}

		projectile.remove();
		mPlugin.mProjectileEffectTimers.removeEntity(projectile);

		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();
		World world = loc.getWorld();
		mCosmetic.strikeLaunch(world, mPlayer);

		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, MAX_RANGE);
		int piercing = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PIERCING, PIERCING);
		List<LivingEntity> piercedMobs = new ArrayList<>();

		RayTraceResult result = world.rayTrace(loc, direction, range, FluidCollisionMode.NEVER, true, 0.425,
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

			result = world.rayTrace(loc, direction, range, FluidCollisionMode.NEVER, true, 0.425,
				e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid() && !piercedMobs.contains((LivingEntity) e));
		}

		Location endLoc;
		if (result == null) {
			endLoc = loc.clone().add(direction.multiply(range));
		} else {
			endLoc = result.getHitPosition().toLocation(world);
		}

		mCosmetic.strikeImpact(() -> mCosmetic.strikeExplode(world, mPlayer, endLoc, mExplodeRadius), endLoc, mPlayer);
		explode(endLoc, piercedMobs);
		mCosmetic.strikeParticleLine(mPlayer, loc, endLoc);

		return true;
	}

	private void explode(Location loc, List<LivingEntity> piercedMobs) {
		ItemStack item = mPlayer.getInventory().getItemInMainHand();

		// go through pierced mobs and use their locations
		for (LivingEntity piercedMob : piercedMobs) {
			double damage = ItemStatUtils.getAttributeAmount(item, AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND);
			damage += PointBlank.apply(mPlayer, piercedMob.getLocation(), ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.POINT_BLANK));
			damage += Sniper.apply(mPlayer, piercedMob.getLocation(), ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.SNIPER));
			damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
			damage *= DAMAGE_MULTIPLIER + mDistanceScale * Math.min(mPlayer.getLocation().distance(piercedMob.getLocation()), MAX_DAMAGE_RANGE);

			float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, EXPLODE_KNOCKBACK);

			MovementUtils.knockAway(mPlayer.getLocation(), piercedMob, knockback, knockback, true);
			DamageUtils.damage(mPlayer, piercedMob, DamageType.PROJECTILE_SKILL, damage, mInfo.getLinkedSpell(), true);
		}

		// go through exploded mobs and use the explosion's location
		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mExplodeRadius);
		List<LivingEntity> mobs = hitbox.getHitMobs();
		if (!mobs.isEmpty()) {
			double damage = ItemStatUtils.getAttributeAmount(item, AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND);
			damage += PointBlank.apply(mPlayer, loc, ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.POINT_BLANK));
			damage += Sniper.apply(mPlayer, loc, ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.SNIPER));
			damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
			damage *= DAMAGE_MULTIPLIER + mDistanceScale * Math.min(mPlayer.getLocation().distance(loc), MAX_DAMAGE_RANGE);

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

		if (event.getAbility() == ClassAbility.PREDATOR_STRIKE && event.getFinalDamage(true) > damageCap) {
			event.setDamage(damageCap);
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
}
