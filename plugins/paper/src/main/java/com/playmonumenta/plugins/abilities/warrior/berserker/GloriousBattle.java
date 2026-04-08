package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.GloriousBattleCS;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;


public class GloriousBattle extends Ability {
	private static final int BLOODLUST_COST = 2;
	private static final int PIERCE_DAMAGE_1 = 5;
	private static final int PIERCE_DAMAGE_2 = 8;
	private static final double AOE_DAMAGE_1 = 0.5;
	private static final double AOE_DAMAGE_2 = 0.6;
	private static final double CRITICAL_DAMAGE_1 = 0.75;
	private static final double CRITICAL_DAMAGE_2 = 0.9;
	private static final double VELOCITY = 1.5;
	private static final double VERTICAL_SPEED_CAP = 0.3;
	private static final double RADIUS = 2.5;
	private static final int DURATION = 30;
	private static final float KNOCK_AWAY_SPEED = 0.4f;
	private static final String KBR_EFFECT = "GloriousBattleKnockbackResistanceEffect";

	public static final String CHARM_DAMAGE = "Glorious Battle Damage";
	public static final String CHARM_PIERCE_DAMAGE = "Glorious Battle Pierce Damage";
	public static final String CHARM_RADIUS = "Glorious Battle Attack Radius";
	public static final String CHARM_VELOCITY = "Glorious Battle Velocity";
	public static final String CHARM_KNOCKBACK = "Glorious Battle Knockback";
	public static final String CHARM_BLOODLUST_COST = "Glorious Battle Bloodlust Cost";
	public static final String CHARM_DURATION = "Glorious Battle Duration";
	public static final String CHARM_CRITICAL_DAMAGE = "Glorious Battle Critical Damage Multiplier";

	public static final AbilityInfo<GloriousBattle> INFO =
		new AbilityInfo<>(GloriousBattle.class, "Glorious Battle", GloriousBattle::new)
			.linkedSpell(ClassAbility.GLORIOUS_BATTLE)
			.scoreboardId("GloriousBattle")
			.shorthandName("GB")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Lunge forwards, dealing damage and empowering your next critical attack.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GloriousBattle::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)))
			.displayItem(Material.IRON_SWORD);

	private final List<LivingEntity> mCharged = new ArrayList<>();
	private final int mBloodlustCost;
	private final int mDuration;
	private final double mPierceDamage;
	private final double mAoeDamage;
	private final double mVelocity;
	private final double mRadius;
	private final double mKnockback;
	private final double mCriticalDamage;
	private final double mCriticalMultiplier;

	private boolean mCanCritAttack = false;
	private @Nullable Bloodlust mBloodlust;
	private @Nullable BukkitRunnable mRunnable;
	private final GloriousBattleCS mCosmetic;

	public GloriousBattle(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mAoeDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? AOE_DAMAGE_1 : AOE_DAMAGE_2);
		mCriticalDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? CRITICAL_DAMAGE_1 : CRITICAL_DAMAGE_2);
		mCriticalMultiplier = CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CRITICAL_DAMAGE);
		mPierceDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PIERCE_DAMAGE, isLevelOne() ? PIERCE_DAMAGE_1 : PIERCE_DAMAGE_2);
		mVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, VELOCITY);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mBloodlustCost = BLOODLUST_COST + (int) CharmManager.getLevel(mPlayer, CHARM_BLOODLUST_COST);
		mKnockback = CharmManager.getExtraPercent(mPlayer, CHARM_KNOCKBACK, KNOCK_AWAY_SPEED);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GloriousBattleCS());

		Bukkit.getScheduler().runTask(mPlugin, () ->
			mBloodlust = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Bloodlust.class));
	}

	public boolean cast() {
		if (ZoneUtils.hasZoneProperty(mPlayer.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)
			|| isInapplicableMainhand()) {
			return false;
		}

		if (mBloodlust == null || !mBloodlust.useStacks(mBloodlustCost)) {
			return false;
		}

		Vector dir = getLungeVector();
		mPlayer.setVelocity(dir);
		mPlugin.mEffectManager.addEffect(mPlayer, KBR_EFFECT,
			new PercentKnockbackResist(TICKS_PER_SECOND * 10, 1, KBR_EFFECT)
				.displaysTime(false).deleteOnAbilityUpdate(true));
		mCanCritAttack = true;

		Location location = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		int mSpellDelay = 10;
		mCosmetic.gloryStart(world, mPlayer, location, mSpellDelay);
		mCharged.clear();

		if (mRunnable != null) {
			mRunnable.cancel();
		}

		mRunnable = new BukkitRunnable() {
			int mT = 0;
			boolean mHasLanded = false;

			@Override
			public void run() {
				mT++;

				if (!mHasLanded) {

					mCosmetic.gloryTick(mPlayer, mT);
					if (PlayerUtils.isOnGroundOrMountIsOnGround(mPlayer) && mT >= mSpellDelay) {
						mPlugin.mEffectManager.clearEffects(mPlayer, KBR_EFFECT);
						mHasLanded = true;
						mT = 0;
					}

					// piercing
					List<LivingEntity> mobs = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), 4).getHitMobs();
					BoundingBox mBox = BoundingBox.of(mPlayer.getLocation().add(0, 1, 0), 2, 2, 2);
					mobs.removeAll(mCharged);
					mobs.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(mPlayer.getLocation())));
					for (LivingEntity mob : mobs) {
						if (mob.getBoundingBox().overlaps(mBox)) {
							mCharged.add(mob);
							DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mPierceDamage, ClassAbility.GLORIOUS_BATTLE, true);
							mCosmetic.gloryOnDamage(world, mPlayer, mob);
						}
					}

				} else if (mT > mDuration) {
					mCosmetic.onGloryDurationExpire(world, mPlayer.getLocation());
					mCanCritAttack = false;
					this.cancel();
				}

				//Logged off or something probably
				if (mT > 200) {
					this.cancel();
				}
			}
		};
		cancelOnDeath(mRunnable.runTaskTimer(mPlugin, 0, 1));
		return true;
	}

	@NotNull
	private Vector getLungeVector() {
		Vector dir = mPlayer.getLocation().getDirection();
		dir.multiply(mVelocity);
		// vertical speed cap for level 1
		if (isLevelOne()) {
			if (dir.getY() > VERTICAL_SPEED_CAP) {
				dir.setY(VERTICAL_SPEED_CAP + 0.2);
			} else {
				dir.setY(dir.getY() * 0.5);
			}
		} else {
			if (Math.signum(dir.getY()) > 0) {
				// +0.22 to allow for horizontal movement
				dir.setY(dir.getY() * 0.4 + 0.22);
			} else {
				// do not -0.3, otherwise will affect planned change for meteor slam (blocks fallen -> velocity)
				dir.setY(dir.getY() * 0.4);
			}
		}
		return dir;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!(event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer))) {
			return false;
		}
		if (!mCanCritAttack) {
			return true;
		}
		mCanCritAttack = false;

		final boolean weaponHasCumbersome = ItemStatUtils.hasEnchantment(mPlayer.getInventory().getItemInMainHand(), EnchantmentType.CUMBERSOME);
		double baseDamage = event.getFlatDamage() / (weaponHasCumbersome ? 1.5 : 1);

		mCosmetic.gloryOnLand(mPlayer.getWorld(), mPlayer, enemy, mRadius);

		List<LivingEntity> targets = EntityUtils.getNearbyMobs(enemy.getLocation(), mRadius);

		hitMob(enemy, baseDamage * (mCriticalDamage + mCriticalMultiplier));
		targets.remove(enemy);

		targets.forEach(e -> hitMob(e, baseDamage * mAoeDamage));

		return true;
	}

	private void hitMob(LivingEntity entity, double damage) {
		DamageUtils.damage(mPlayer, entity, DamageType.MELEE_SKILL, damage, ClassAbility.GLORIOUS_BATTLE, true);
		MovementUtils.knockAway(mPlayer, entity, (float) mKnockback, true);
		mCosmetic.gloryOnDamage(mPlayer.getWorld(), mPlayer, entity);
	}

	private boolean isInapplicableMainhand() {
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		// Not an applicable weapon (not a sword/axe, and it doesn't have any dmg stats)
		return (ItemStatUtils.getAttributeAmount(mainhand, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) <= 0
			|| ItemStatUtils.getAttributeAmount(mainhand, AttributeType.ATTACK_SPEED, Operation.ADD, Slot.MAINHAND) == 0)
			&& !(ItemUtils.isAxe(mainhand) || ItemUtils.isSword(mainhand));
	}

	private static Description<GloriousBattle> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Spend %d stacks of *Bloodlust* to lunge horizontally,").styles(Bloodlust.BLOODLUST_COLOR)
				.statValues(stat(a -> a.mBloodlustCost, BLOODLUST_COST))
			.addLine("gain knockback immunity, and deal damage to mobs")
			.addLine("while airborne.")
			.addLine()
			.addStat("Collision Damage: %d1 (m)")
				.statValues(stat(a -> a.mPierceDamage, PIERCE_DAMAGE_1))
			.addLine()
			.addLine("Your next critical attack while airborne or for %t")
				.statValues(stat(a -> a.mDuration, DURATION))
			.addLine("after landing causes a large swing that deals")
			.addLine("bonus damage to the target and nearby mobs.")
			.addLine()
			.addStat("Direct Damage: %p1 (m) (of the attack's damage)")
				.statValues(stat(a -> a.mCriticalDamage, CRITICAL_DAMAGE_1))
			.addStat("Area Damage: %p1 (m) (of the attack's damage)")
				.statValues(stat(a -> a.mAoeDamage, AOE_DAMAGE_1))
			.addStat("Area Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addDashedLine();
	}

	private static DescriptionBuilder<GloriousBattle> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Glorious Battle*'s damage, and").styles(UNDERLINED)
			.addLine("you can now lunge in any direction.")
			.addLine()
			.addStatComparison("Collision Damage: %d1 -> %d2 (m)")
				.statValues(stat(PIERCE_DAMAGE_1), stat(a -> a.mPierceDamage, PIERCE_DAMAGE_2))
			.addStatComparison("Direct Damage: %p1 -> %p2 (m)")
				.statValues(stat(CRITICAL_DAMAGE_1), stat(a -> a.mCriticalDamage, CRITICAL_DAMAGE_2))
			.addStatComparison("Area Damage: %p1 -> %p2 (m) ")
			.statValues(stat(AOE_DAMAGE_1), stat(a -> a.mAoeDamage, AOE_DAMAGE_2))
			.addDashedLine();
	}

}
