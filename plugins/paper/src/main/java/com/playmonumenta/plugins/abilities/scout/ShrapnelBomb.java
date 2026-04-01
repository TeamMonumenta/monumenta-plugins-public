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
import com.playmonumenta.plugins.cosmetics.skills.scout.ShrapnelBombCS;
import com.playmonumenta.plugins.effects.ShrapnelBombMark;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
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
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class ShrapnelBomb extends Ability {
	private static final WeakHashMap<Projectile, ItemStatManager.PlayerItemStats> PROJECTILE_MAP = new WeakHashMap<>();

	private static final double VELOCITY = 1;
	private static final double BOMB_DAMAGE_L1_R1 = 8;
	private static final double BOMB_DAMAGE_L1_R2 = 13;
	private static final double BOMB_DAMAGE_L1_R3 = 18;
	private static final double BOMB_DAMAGE_L2_R1 = 12;
	private static final double BOMB_DAMAGE_L2_R2 = 18;
	private static final double BOMB_DAMAGE_L2_R3 = 24;
	private static final double SHRAP_DAMAGE_L1_R1 = 5;
	private static final double SHRAP_DAMAGE_L1_R2 = 8;
	private static final double SHRAP_DAMAGE_L1_R3 = 13;
	private static final double SHRAP_DAMAGE_L2_R1 = 8;
	private static final double SHRAP_DAMAGE_L2_R2 = 12;
	private static final double SHRAP_DAMAGE_L2_R3 = 18;
	private static final double BOMB_DAMAGE_ENHANCEMENT = 0.8;
	private static final double SHRAPNEL_SPREAD = 15;
	private static final double BOMB_RADIUS = 4;
	private static final double BOMB_RADIUS_ENHANCEMENT = 3;
	private static final int SHRAP_COUNT = 3;
	private static final double SHRAP_DISTANCE = 8;
	private static final int STAGGER_DURATION = Constants.TICKS_PER_SECOND;
	private static final double RANGE = 8;
	private static final double DAMAGE_BOOST = 0.3;
	private static final int DAMAGE_BOOST_DURATION = 5 * Constants.TICKS_PER_SECOND;
	private static final int DAMAGE_BOOST_HITS = 1;
	private static final float KNOCKBACK = 0.75f;
	private static final int COOLDOWN = 12 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_BOMB_DAMAGE = "Shrapnel Bomb Damage";
	public static final String CHARM_BOMB_ENHANCEMENT_DAMAGE = "Shrapnel Bomb Enhancement Damage";
	public static final String CHARM_SHRAPNEL_DAMAGE = "Shrapnel Bomb Shrapnel Damage";
	public static final String CHARM_BOMB_RADIUS = "Shrapnel Bomb Radius";
	public static final String CHARM_BOMB_ENHANCEMENT_RADIUS = "Shrapnel Bomb Enhancement Radius";
	public static final String CHARM_BOMB_VELOCITY = "Shrapnel Bomb Velocity";
	public static final String CHARM_SHRAPNEL_COUNT = "Shrapnel Bomb Shrapnel Count";
	public static final String CHARM_SHRAPNEL_DISTANCE = "Shrapnel Bomb Shrapnel Distance";
	public static final String CHARM_SHRAPNEL_SPREAD = "Shrapnel Bomb Shrapnel Spread";
	public static final String CHARM_STAGGER_DURATION = "Shrapnel Bomb Stagger Duration";
	public static final String CHARM_RANGE = "Shrapnel Bomb Range";
	public static final String CHARM_DAMAGE_BOOST = "Shrapnel Bomb Damage Boost";
	public static final String CHARM_DAMAGE_BOOST_DURATION = "Shrapnel Bomb Damage Boost Duration";
	public static final String CHARM_DAMAGE_BOOST_HITS = "Shrapnel Bomb Boosted Hits";
	public static final String CHARM_KNOCKBACK = "Shrapnel Bomb Knockback";
	public static final String CHARM_COOLDOWN = "Shrapnel Bomb Cooldown";

	private final double mBombDamage;
	private final double mBombEnhancementDamage;
	private final double mShrapnelDamage;
	private final double mBombRadius;
	private final double mBombEnhancementRadius;
	private final double mVelocity;
	private final int mShrapnelCount;
	private final double mShrapnelDistance;
	private final double mShrapnelSpread;
	private final int mStaggerDuration;
	private final double mRange;
	private final double mDamageBoost;
	private final int mDamageBoostDuration;
	private final int mDamageBoostHits;
	private final float mKnockback;

	public static final AbilityInfo<ShrapnelBomb> INFO =
		new AbilityInfo<>(ShrapnelBomb.class, "Shrapnel Bomb", ShrapnelBomb::new)
			.linkedSpell(ClassAbility.SHRAPNEL_BOMB)
			.scoreboardId("ShrapnelBomb")
			.shorthandName("SB")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Launch a bomb that explodes, releasing shrapnel if it hits a mob.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ShrapnelBomb::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.quest216Message("-------t-------e-------")
			.displayItem(Material.FLINT);

	private final ShrapnelBombCS mCosmetic;

	public ShrapnelBomb(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		double bombDmg = isLevelOne() ? AbilityUtils.regionalScale(player, BOMB_DAMAGE_L1_R1, BOMB_DAMAGE_L1_R2, BOMB_DAMAGE_L1_R3)
			: AbilityUtils.regionalScale(player, BOMB_DAMAGE_L2_R1, BOMB_DAMAGE_L2_R2, BOMB_DAMAGE_L2_R3);

		double shrapnelDmg = isLevelOne() ? AbilityUtils.regionalScale(player, SHRAP_DAMAGE_L1_R1, SHRAP_DAMAGE_L1_R2, SHRAP_DAMAGE_L1_R3)
			: AbilityUtils.regionalScale(player, SHRAP_DAMAGE_L2_R1, SHRAP_DAMAGE_L2_R2, SHRAP_DAMAGE_L2_R3);

		mBombDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_BOMB_DAMAGE, bombDmg);
		mBombEnhancementDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_BOMB_ENHANCEMENT_DAMAGE, BOMB_DAMAGE_ENHANCEMENT);
		mVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_BOMB_VELOCITY, VELOCITY);
		mShrapnelDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SHRAPNEL_DAMAGE, shrapnelDmg);
		mBombRadius = CharmManager.getRadius(mPlayer, CHARM_BOMB_RADIUS, BOMB_RADIUS);
		mBombEnhancementRadius = CharmManager.getRadius(mPlayer, CHARM_BOMB_ENHANCEMENT_DAMAGE, BOMB_RADIUS_ENHANCEMENT);
		mShrapnelCount = SHRAP_COUNT + (int) CharmManager.getLevel(mPlayer, CHARM_SHRAPNEL_COUNT);
		mShrapnelDistance = CharmManager.getRadius(mPlayer, CHARM_SHRAPNEL_DISTANCE, SHRAP_DISTANCE);
		mStaggerDuration = CharmManager.getDuration(mPlayer, CHARM_STAGGER_DURATION, STAGGER_DURATION);
		mShrapnelSpread = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SHRAPNEL_SPREAD, SHRAPNEL_SPREAD);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mDamageBoost = DAMAGE_BOOST + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_BOOST);
		mDamageBoostDuration = CharmManager.getDuration(mPlayer, CHARM_DAMAGE_BOOST_DURATION, DAMAGE_BOOST_DURATION);
		mDamageBoostHits = DAMAGE_BOOST_HITS + (int) CharmManager.getLevel(mPlayer, CHARM_DAMAGE_BOOST_HITS);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ShrapnelBombCS());
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

		final ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		Projectile bomb = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, mPlayer.getWorld(), mVelocity, mCosmetic.getName(), mCosmetic.getParticle(), LocationUtils.isLocationInWater(mPlayer.getLocation()));

		PROJECTILE_MAP.put(bomb, playerItemStats);

		mCosmetic.modify(bomb, mPlayer, mPlugin);
		mCosmetic.bombShoot(mPlayer.getWorld(), mPlayer);
		mCosmetic.bombTick(mPlayer.getWorld(), mPlayer, bomb);

		putOnCooldown();

		return true;
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, final Projectile proj) {
		boolean isBomb = PROJECTILE_MAP.keySet().stream().anyMatch(proj::equals);

		if (!isBomb) {
			return;
		}

		event.setCancelled(true);

		// You can. hit. players ?
		if (event.getHitEntity() instanceof Player) {
			return;
		}

		ItemStatManager.PlayerItemStats stats = PROJECTILE_MAP.remove(proj);
		proj.remove();

		if (stats == null) {
			return;
		}

		explode(proj, stats, event.getHitEntity());
	}

	public void explode(Projectile bomb, ItemStatManager.PlayerItemStats stats, @Nullable Entity struckMob) {
		Location loc = LocationUtils.getHalfHeightLocation(bomb);

		mCosmetic.bombExplode(mPlayer.getWorld(), mPlayer, loc, mBombRadius);

		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, mBombRadius)) {

			DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.PROJECTILE_SKILL, mBombDamage, mInfo.getLinkedSpell(), true);
			MovementUtils.knockAway(loc, e, mKnockback, mKnockback / 2, true);

			boolean enhancementMark = isEnhanced() && e.equals(struckMob);

			if (isLevelTwo() || enhancementMark) {
				mPlugin.mEffectManager.addEffect(e, "ShrapnelBomb",
					new ShrapnelBombMark(mDamageBoost, mDamageBoostHits, mDamageBoostDuration, mPlayer, mCosmetic,
						this, stats, enhancementMark));
			}
		}

		if (struckMob instanceof LivingEntity e) {
			Vector dir = bomb.getVelocity().normalize();
			dir.setY(0);
			shrapnel(loc, dir, e);

			if (WindBomb.attemptHit(e)) {
				return;
			}

			EntityUtils.applyStagger(mPlugin, mStaggerDuration, e);
			HuntingCompanion.staggerApplied(mPlayer, e);
		}
	}

	public void explodeEnhancement(Location loc, ItemStatManager.PlayerItemStats stats) {
		mCosmetic.bombEnhancementExplode(mPlayer.getWorld(), mPlayer, loc, mBombRadius);

		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, mBombEnhancementRadius)) {
			double dmg = AbilityUtils.projectileFinalDamage(stats, mPlayer, e, 0, mBombEnhancementDamage);

			DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.PROJECTILE_SKILL, dmg, mInfo.getLinkedSpell(), true);
			MovementUtils.knockAway(loc, e, mKnockback / 2, mKnockback / 4, true);
		}
	}

	public void shrapnel(Location loc, Vector dir, LivingEntity struckMob) {
		mCosmetic.shrapnelExplode(mPlayer.getWorld(), mPlayer, loc, dir);

		double center = (mShrapnelCount - 1) / 2.0;

		for (int i = 0; i < mShrapnelCount; i++) {
			Vector newDir = VectorUtils.rotateTargetDirection(dir, mShrapnelSpread * (i - center), 0);
			shrapnelProjectile(loc.clone().setDirection(newDir), struckMob);
		}
	}

	public void shrapnelProjectile(Location loc, LivingEntity struckMob) {
		Item shrap = AbilityUtils.spawnAbilityItem(mPlayer.getWorld(), loc, mCosmetic.getShrapnelItem(), "Shrapnel", false, 0.8f, false, true);
		shrap.setGravity(false);

		final int MAX_DISTANCE_TIME = (int) (mShrapnelDistance / 0.8f);

		new BukkitRunnable() {
			private final Item mPhysicsItem = shrap;
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;

				if (!mPlayer.isOnline()) {
					mPhysicsItem.remove();
					this.cancel();
					return;
				}

				mCosmetic.shrapnelTick(mPhysicsItem.getWorld(), mPlayer, mPhysicsItem.getLocation().clone().add(0, 0.1, 0));

				List<LivingEntity> enemyCollisionList = getEnemyCollision(mPhysicsItem);
				enemyCollisionList.remove(struckMob);
				boolean hasCollidedWithEnemy = !enemyCollisionList.isEmpty();

				if (mPhysicsItem.isOnGround()
					|| mTicks > MAX_DISTANCE_TIME
					|| mPhysicsItem.isInLava()
					|| hasCollidedWithEnemy
					|| LocationUtils.collidesWithBlocks(BoundingBox.of(mPhysicsItem.getLocation(), 0.25, 0.25, 0.25), mPhysicsItem.getWorld())) {
					if (hasCollidedWithEnemy) {
						Collections.shuffle(enemyCollisionList);
						LivingEntity e = enemyCollisionList.getFirst();

						mCosmetic.shrapnelHit(mPhysicsItem.getWorld(), mPlayer, mPhysicsItem.getLocation());
						MovementUtils.knockAway(loc, e, mKnockback / 3, mKnockback / 6, true);
						DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.PROJECTILE_SKILL, mShrapnelDamage, mInfo.getLinkedSpell(), true);
					}

					mPhysicsItem.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private List<LivingEntity> getEnemyCollision(Item physicsItem) {
		Hitbox hitbox = new Hitbox.AABBHitbox(physicsItem.getWorld(), BoundingBox.of(physicsItem.getLocation(), 1.025, 1.025, 1.025));
		return hitbox.getHitMobs();
	}

	private static Description<ShrapnelBomb> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Throw a *Shrapnel Bomb* that explodes").styles(UNDERLINED)
			.addLine("on contact, dealing damage.")
			.addLine()
			.addStat("Bomb Damage: %d1 (p)")
			.statValues(perRegion(a -> a.mBombDamage, BOMB_DAMAGE_L1_R1, BOMB_DAMAGE_L1_R2, BOMB_DAMAGE_L1_R3))
			.addStat("Bomb Radius: %r")
			.statValues(stat(a -> a.mBombRadius, BOMB_RADIUS))
			.addStat("Cooldown: %t")
			.statValues(cooldown(COOLDOWN))
			.addLine()
			.addLine("Direct hits stagger the mob and").styles(UNDERLINED)
			.addLine("release shrapnel behind it.")
			.addLine("(Shrapnel cannot damage the same mob)")
			.addLine()
			.addStat("Shrapnel Damage: %d1 (p)")
			.statValues(perRegion(a -> a.mShrapnelDamage, SHRAP_DAMAGE_L1_R1, SHRAP_DAMAGE_L1_R2, SHRAP_DAMAGE_L1_R3))
			.addStat("Count: %d")
			.statValues(stat(a -> a.mShrapnelCount, SHRAP_COUNT))
			.addStat("Effect: Stagger for %t")
			.statValues(stat(a -> a.mStaggerDuration, STAGGER_DURATION))
			.addStat("Range: %r")
			.statValues(stat(a -> a.mRange, RANGE))
			.addDashedLine();
	}

	private static Description<ShrapnelBomb> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Shrapnel Bomb*'s damage.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Bomb Damage: %d1 -> %d2 (p)")
			.statValues(perRegion(BOMB_DAMAGE_L1_R1, BOMB_DAMAGE_L1_R2, BOMB_DAMAGE_L1_R3),
				perRegion(a -> a.mBombDamage, BOMB_DAMAGE_L2_R1, BOMB_DAMAGE_L2_R2, BOMB_DAMAGE_L2_R3))
			.addStatComparison("Shrapnel Damage: %d1 -> %d2 (p)")
			.statValues(perRegion(SHRAP_DAMAGE_L1_R1, SHRAP_DAMAGE_L1_R2, SHRAP_DAMAGE_L1_R3),
				perRegion(a -> a.mShrapnelDamage, SHRAP_DAMAGE_L2_R1, SHRAP_DAMAGE_L2_R2, SHRAP_DAMAGE_L2_R3))
			.addLine()
			.addLine("*Shrapnel Bomb* now boosts your next").styles(UNDERLINED)
			.addLine("instance of damage against struck targets.")
			.addLine()
			.addStat("Damage Boost: %p for %t")
			.statValues(stat(a -> a.mDamageBoost, DAMAGE_BOOST), stat(a -> a.mDamageBoostDuration, DAMAGE_BOOST_DURATION))
			.addIf((a, p) -> a != null && a.mDamageBoostHits != 1, desc -> desc
				.addStat("Boosted Hits: %d")
				.statValues(stat(a -> a.mDamageBoostHits, DAMAGE_BOOST_HITS)))
			.addDashedLine();
	}

	private static Description<ShrapnelBomb> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Mobs staggered by *Shrapnel Bomb* boosts your").styles(UNDERLINED)
			.addLine("next instance of damage as an explosion.")
			.addLine()
			.addStat("Damage: %p (p) (of weapon damage)")
			.statValues(stat(a -> a.mBombEnhancementDamage, BOMB_DAMAGE_ENHANCEMENT))
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mBombEnhancementRadius, BOMB_RADIUS_ENHANCEMENT))
			.addDashedLine();
	}
}
