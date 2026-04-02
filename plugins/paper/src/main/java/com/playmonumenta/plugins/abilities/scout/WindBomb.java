package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.WindBombCS;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class WindBomb extends Ability {
	private static final String BOMB_NAME = "WindBomb";

	private static final int DURATION = Constants.TICKS_PER_SECOND * 4;
	private static final int SLOW_FALL_POTENCY = 0;
	private static final int COOLDOWN_1 = Constants.TICKS_PER_SECOND * 16;
	private static final int COOLDOWN_2 = Constants.TICKS_PER_SECOND * 14;
	private static final int SLOW_FALL_DURATION = Constants.TICKS_PER_SECOND;
	private static final int SIZE = 1;
	private static final int RADIUS = 5;

	private static final double DAMAGE_FLAT_L1 = 10;
	private static final double DAMAGE_FLAT_L2 = 14;
	private static final double DAMAGE_PERCENT_L1 = 0.6;
	private static final double DAMAGE_PERCENT_L2 = 0.8;
	private static final double PULL_VELOCITY = 0.2;

	private static final double VORTEX_HEIGHT = 3;
	private static final int PULL_INTERVAL = 4;
	private static final double PULL_RADIUS = 10;
	private static final int PULL_DURATION = (Constants.TICKS_PER_SECOND * 3);
	private static final double PULL_RATIO = 0.05;
	private static final float TRANSFER_COEFFICIENT = 0.25f;

	public static final String CHARM_DURATION = "Wind Bomb Duration";
	public static final String CHARM_COOLDOWN = "Wind Bomb Cooldown";
	public static final String CHARM_DAMAGE = "Wind Bomb Damage";
	public static final String CHARM_RADIUS = "Wind Bomb Radius";
	public static final String CHARM_PULL = "Wind Bomb Pull";
	public static final String CHARM_SLOW_FALL_DURATION = "Wind Bomb Slow Fall Duration";
	public static final String CHARM_SIZE = "Wind Bomb Size";

	public static final String CHARM_VORTEX_DURATION = "Wind Bomb Vortex Duration";
	public static final String CHARM_VORTEX_RADIUS = "Wind Bomb Vortex Radius";
	public static final String CHARM_VORTEX_HEIGHT = "Wind Bomb Vortex Height";

	public static final AbilityInfo<WindBomb> INFO =
		new AbilityInfo<>(WindBomb.class, "Wind Bomb", WindBomb::new)
			.linkedSpell(ClassAbility.WIND_BOMB)
			.scoreboardId("WindBomb")
			.shorthandName("WB")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Throw a bomb that upon being damaged explodes.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WindBomb::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.TNT);

	private final double mRadius;
	private final int mSlowfallDuration;
	private final float mPull;
	private final int mBombDuration;
	private final double mBombDamageFlat;
	private final double mBombDamagePercent;
	private final int mSize;

	private final double mEnhancePullRadius;
	private final int mEnhancePullDuration;
	private final double mEnhanceVortexHeight;

	private final WindBombCS mCosmetic;
	private @Nullable LivingEntity mBomb;
	private @Nullable BukkitTask mRunnable;

	public WindBomb(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);

		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mSlowfallDuration = CharmManager.getDuration(mPlayer, CHARM_SLOW_FALL_DURATION, SLOW_FALL_DURATION);
		mBombDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mPull = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL, PULL_VELOCITY);
		mBombDamageFlat = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_FLAT_L1 : DAMAGE_FLAT_L2);
		mBombDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_PERCENT_L1 : DAMAGE_PERCENT_L2);
		mSize = (int) Math.max(1, SIZE + CharmManager.getLevel(mPlayer, CHARM_SIZE));

		mEnhancePullRadius = CharmManager.getRadius(mPlayer, CHARM_VORTEX_RADIUS, PULL_RADIUS);
		mEnhancePullDuration = CharmManager.getDuration(mPlayer, CHARM_VORTEX_DURATION, PULL_DURATION);
		mEnhanceVortexHeight = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VORTEX_HEIGHT, VORTEX_HEIGHT);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new WindBombCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		final ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		final ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if ((ItemStatUtils.hasEnchantment(inMainHand, EnchantmentType.TWO_HANDED)
			&& !(ItemUtils.isNullOrAir(inOffHand) || ItemStatUtils.hasEnchantment(inOffHand, EnchantmentType.WEIGHTLESS)))
			|| ItemUtils.isShootableItem(inOffHand)) {
			return false;
		}

		if (mBomb != null) {
			if (mRunnable != null) {
				mRunnable.cancel();
			}
			doExplosion(mBomb.getLocation());
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		mBomb = launchBomb(NmsUtils.getVersionAdapter().getActualDirection(mPlayer).multiply(0.75));

		if (mBomb == null) {
			return false;
		}
		EntityUtils.setSize(mBomb, mSize);

		mCosmetic.onThrow(mPlugin, world, loc);
		mCosmetic.modify(mBomb, mPlugin, mSize);

		return true;
	}

	private @Nullable LivingEntity launchBomb(Vector direction) {
		Vector dir = mPlayer.getEyeLocation().getDirection();

		Location summonLoc = LocationUtils.getHalfHeightLocation(mPlayer).add(dir);
		if (LocationUtils.collidesWithSolid(summonLoc)) {
			summonLoc = mPlayer.getEyeLocation().subtract(dir.multiply(0.5));
		}
		LivingEntity bomb = (LivingEntity) LibraryOfSoulsIntegration.summon(summonLoc, BOMB_NAME);
		if (bomb == null) {
			return null;
		}
		bomb.setAI(true);
		bomb.setVelocity(direction);
		EntityUtils.selfRoot(bomb, 9999 * 20);

		EnumSet<DamageEvent.DamageType> allButProj = DamageEvent.DamageType.getEnumSet();
		allButProj.remove(DamageEvent.DamageType.PROJECTILE);
		mPlugin.mEffectManager.addEffect(bomb, "WindBombOnlyProjectile", new PercentDamageReceived(9999 * 20, -1, allButProj));
		// 0.5s invulnerability so it doesn't get triggered immediately
		mPlugin.mEffectManager.addEffect(bomb, "WindBombImmunity", new PercentDamageReceived(10, -1));

		mRunnable = new BukkitRunnable() {
			final LivingEntity mRunnableBomb = bomb;
			int mTicks = 0;

			@Override
			public void run() {
				if (!mPlayer.isOnline()) {
					removeWindBomb();
					this.cancel();
					return;
				}

				Vector dir = mRunnableBomb.getVelocity();

				mRunnableBomb.setVelocity(dir.setY(dir.getY() * 0.95));

				if (mTicks % 5 == 0) {
					mCosmetic.aerial(mPlayer, mRunnableBomb, mTicks, mBombDuration);
				}

				if (mRunnableBomb.isDead() || mTicks >= mBombDuration) {
					doExplosion(mRunnableBomb.getLocation());
					this.cancel();
					return;
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return bomb;
	}

	private void doExplosion(Location loc) {
		if (mBomb == null) {
			return;
		}

		projectileHitAudio(mBomb);
		removeWindBomb();
		World world = loc.getWorld();
		mCosmetic.onExplode(mPlayer, world, loc, mRadius);

		List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, mRadius).getHitMobs();
		mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));

		for (LivingEntity mob : mobs) {
			double dmg = AbilityUtils.projectileFinalDamage(mPlayer, mob, mBombDamageFlat, mBombDamagePercent);

			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.PROJECTILE_SKILL, dmg, mInfo.getLinkedSpell(), true);
			mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, mSlowfallDuration, SLOW_FALL_POTENCY, false, false, false));
			MovementUtils.pullTowards(loc, mob, mPull);
		}

		if (isEnhanced()) {
			enhancementVortex(loc);
		} else {
			new BukkitRunnable() { // continue pulling for a bit
				int mTicks = 0;
				final Location mLoc = loc.clone();

				@Override
				public void run() {
					if (mTicks > 5) {
						List<LivingEntity> mobs = new Hitbox.SphereHitbox(mLoc, mRadius).getHitMobs();
						mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));

						for (LivingEntity mob : mobs) {
							MovementUtils.pullTowardsNormalized(mLoc, mob, mPull);
						}
					}

					if (mTicks > 8) {
						this.cancel();
					}
					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void enhancementVortex(Location loc) {
		final Location vortexLoc;
		World world = loc.getWorld();
		RayTraceResult result = world.rayTraceBlocks(loc, new Vector(0, -1, 0), mEnhanceVortexHeight, FluidCollisionMode.NEVER, true);
		if (result == null) {
			vortexLoc = loc;
		} else {
			vortexLoc = result.getHitPosition().toLocation(world).add(new Vector(0, mEnhanceVortexHeight, 0));
		}

		mCosmetic.onVortexSpawn(mPlayer, world, vortexLoc, mEnhancePullDuration);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				mCosmetic.onVortexTick(mPlayer, vortexLoc, mEnhancePullRadius, mTicks);

				if (mTicks >= mEnhancePullDuration) {
					this.cancel();
					return;
				}

				if (mTicks % PULL_INTERVAL != 0) {
					return;
				}

				Hitbox.SphereHitbox vortexBox = new Hitbox.SphereHitbox(vortexLoc, mEnhancePullRadius);
				Hitbox.SphereHitbox vortexEyeBox = new Hitbox.SphereHitbox(vortexLoc, mEnhancePullRadius / 10);

				for (final LivingEntity mob : vortexBox.getHitMobs()) {
					mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, mSlowfallDuration, SLOW_FALL_POTENCY, false, false, false));
					if (!EntityUtils.isCCImmuneMob(mob) || ZoneUtils.hasZoneProperty(mob.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
						if (vortexEyeBox.getHitMobs().contains(mob)) {
							final Vector velocity = new Vector(0, 0.1, 0);
							mob.setVelocity(velocity);
						} else {
							final Vector vector = mob.getLocation().toVector().subtract(vortexLoc.toVector());
							final double ratio = PULL_RATIO + vector.length() / mEnhancePullRadius;
							final Vector velocity = vector.normalize().multiply(mPull)
								.multiply(-ratio);
							if (!(mob instanceof Flying)) {
								velocity.add(new Vector(0, 0.03 + 0.1 * ratio, 0));
							}
							MovementUtils.knockAwayDirection(velocity, mob, TRANSFER_COEFFICIENT);
						}
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public static boolean isWindBomb(Entity e) {
		return e instanceof LivingEntity le && ScoreboardUtils.checkTag(le, BOMB_NAME);
	}

	public static boolean attemptHit(Entity e) {
		if (isWindBomb(e)) {
			DamageUtils.damage(null, (LivingEntity) e, DamageEvent.DamageType.TRUE, 50000);
			return true;
		}
		return false;
	}

	// Killing wind bomb doesn't trigger projectile audio, here for feedback response
	private static void projectileHitAudio(LivingEntity bomb) {
		if (bomb.getLastDamageCause() != null) {
			Entity proj = bomb.getLastDamageCause().getEntity();
			World world = bomb.getWorld();
			Location loc = bomb.getLocation();

			if (proj instanceof Trident) {
				world.playSound(loc, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
			} else if (proj instanceof AbstractArrow) {
				world.playSound(loc, Sound.ENTITY_ARROW_HIT, 1f, 1f);
			}

		}
	}

	private void removeWindBomb() {
		if (mBomb != null) {
			mBomb.getPassengers().forEach(Entity::remove);
			mBomb.remove();
		}
		mBomb = null;
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		removeWindBomb();
	}

	@Override
	public void playerTeleportEvent(PlayerTeleportEvent event) {
		if (event.getFrom().getWorld() != event.getTo().getWorld()) {
			removeWindBomb();
		}
	}

	private static Description<WindBomb> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Throw a *Wind Bomb* that floats in the air").styles(UNDERLINED)
			.addLine("for %t. Shooting it causes it to explode,")
			.statValues(stat(a -> a.mBombDuration, DURATION))
			.addLine("pulling mobs to the center.")
			.addLine()
			.addStat("Damage: %d1 + %p1 (p) (of weapon damage)")
			.statValues(stat(a -> a.mBombDamageFlat, DAMAGE_FLAT_L1), stat(a -> a.mBombDamagePercent, DAMAGE_PERCENT_L1))
			.addStat("Effect: Slow Falling for %t")
			.statValues(stat(a -> a.mSlowfallDuration, SLOW_FALL_DURATION))
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mRadius, RADIUS))
			.addStat("Cooldown: %t1")
			.statValues(cooldown(COOLDOWN_1))
			.addDashedLine();
	}

	private static Description<WindBomb> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Wind Bomb*'s damage").styles(UNDERLINED)
			.addLine("and reduce its cooldown.")
			.addLine()
			.addStatComparison("Damage: %d1 + %p1 -> %d2 + %p2 (p)")
			.statValues(stat(DAMAGE_FLAT_L1), stat(DAMAGE_PERCENT_L1), stat(a -> a.mBombDamageFlat, DAMAGE_FLAT_L2), stat(a -> a.mBombDamagePercent, DAMAGE_PERCENT_L2))
			.addStatComparison("Cooldown: %t1 -> %t2")
			.statValues(cooldown(COOLDOWN_1), cooldown(COOLDOWN_2))
			.addDashedLine();
	}

	private static Description<WindBomb> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Wind Bomb* creates a vortex that").styles(UNDERLINED)
			.addLine("pulls mobs towards its center and")
			.addLine("applies its effects.")
			.addLine()
			.addStat("Vortex Radius: %r")
			.statValues(stat(a -> a.mEnhancePullRadius, PULL_RADIUS))
			.addStat("Vortex Duration: %t")
			.statValues(stat(a -> a.mEnhancePullDuration, PULL_DURATION))
			.addDashedLine();
	}
}
