package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.paladin.HolyJavelinCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;


public class HolyJavelin extends Ability implements AbilityWithDuration {
	private static final int RANGE = 12;
	private static final double SIZE = 0.95;
	private static final double[] DAMAGE_1 = {10, 13};
	private static final double[] DAMAGE_2 = {12, 16};
	private static final double[] HERETIC_DAMAGE_1 = {20, 26};
	private static final double[] HERETIC_DAMAGE_2 = {24, 32};
	private static final int FIRE_DURATION = 5 * 20;
	private static final float VELOCITY_MULTIPLIER = 1.15f;
	private static final double DIVINE_JUSTICE_BONUS_DAMAGE = 0.6;
	private static final int DIVINE_JUSTICE_BONUS_PRIME_DURATION = 3 * 20;
	private static final int COOLDOWN_1 = 9 * 20;
	private static final int COOLDOWN_2 = 8 * 20;

	public static final String CHARM_DAMAGE = "Holy Javelin Damage";
	public static final String CHARM_COOLDOWN = "Holy Javelin Cooldown";
	public static final String CHARM_RANGE = "Holy Javelin Range";
	public static final String CHARM_SIZE = "Holy Javelin Size";
	public static final String CHARM_DJ_DAMAGE = "Holy Javelin Divine Justice Damage Bonus";
	public static final String CHARM_DJ_PRIME = "Holy Javelin Divine Justice Prime Duration";
	public static final String CHARM_VELOCITY = "Holy Javelin Velocity";

	public static final AbilityInfo<HolyJavelin> INFO =
		new AbilityInfo<>(HolyJavelin.class, "Holy Javelin", HolyJavelin::new)
			.linkedSpell(ClassAbility.HOLY_JAVELIN)
			.scoreboardId("HolyJavelin")
			.shorthandName("HJ")
			.actionBarColor(TextColor.color(255, 255, 50))
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Throw a piercing spear of light that ignites and damages mobs.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HolyJavelin::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false).sprinting(true)
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.GOLDEN_SWORD);

	private final double mDamage;
	private final double mHereticDamage;
	private final double mRange;
	private final double mSize;
	private final float mVelocity;
	private final double mDJDamageBonus;
	private final int mInitialPrimeDuration;

	private int mRemainingPrimeDuration = 0;
	private @Nullable BukkitRunnable mPrimeRunnable = null;
	private @Nullable DivineJustice mDivineJustice;
	private boolean mDisableMobility = false;

	private final HolyJavelinCS mCosmetic;

	public HolyJavelin(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, AbilityUtils.getRegionScaled(player, isLevelOne() ? DAMAGE_1 : DAMAGE_2));
		mHereticDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, AbilityUtils.getRegionScaled(player, isLevelOne() ? HERETIC_DAMAGE_1 : HERETIC_DAMAGE_2));
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mSize = CharmManager.getRadius(mPlayer, CHARM_SIZE, SIZE);
		mVelocity = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_VELOCITY, VELOCITY_MULTIPLIER);
		mDJDamageBonus = DIVINE_JUSTICE_BONUS_DAMAGE + CharmManager.getLevelPercentDecimal(player, CHARM_DJ_DAMAGE);
		mInitialPrimeDuration = CharmManager.getDuration(player, CHARM_DJ_PRIME, DIVINE_JUSTICE_BONUS_PRIME_DURATION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HolyJavelinCS());
		Bukkit.getScheduler().runTask(plugin, () ->
			mDivineJustice = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, DivineJustice.class));
	}

	public boolean cast() {
		return execute(0, null);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (getRemainingAbilityDuration() > 0 && event.getAbility() != null && event.getAbility() == ClassAbility.DIVINE_JUSTICE) {
			event.updateDamageWithMultiplier(1 + mDJDamageBonus);
			mRemainingPrimeDuration = 0;
			ClientModHandler.updateAbility(mPlayer, ClassAbility.HOLY_JAVELIN);
			return false;
		}

		if (event.getType() == DamageType.MELEE
			&& mCustomTriggers.getFirst().check(mPlayer, AbilityTrigger.Key.LEFT_CLICK)) {
			double sharedPassiveDamage = 0;
			if (mDivineJustice != null && Crusade.enemyTriggersAbilities(enemy)) {
				sharedPassiveDamage += mDivineJustice.calculateDamage(event, true);
			}
			execute(sharedPassiveDamage, enemy);
		}
		return false;
	}

	public boolean execute(double bonusDamage, @Nullable LivingEntity triggeringEnemy) {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		World world = mPlayer.getWorld();
		mCosmetic.javelinSound(world, mPlayer.getLocation());
		Location startLoc = mPlayer.getEyeLocation();

		Location endLoc = LocationUtils.rayTraceToBlock(mPlayer, mRange, loc -> mCosmetic.javelinHitBlock(mPlayer, loc, world));

		mCosmetic.javelinParticle(mPlayer, startLoc, endLoc, mSize);

		List<LivingEntity> mobs = Hitbox.approximateCylinder(startLoc, endLoc, mSize, true).accuracy(0.5).getHitMobs();

		if (mobs.isEmpty() && isLevelTwo() && !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES) && triggeringEnemy == null && !mDisableMobility) {
			Vector dir = mPlayer.getLocation().getDirection();
			dir.setY(dir.getY() + 0.4 * (1 - dir.getY()));
			dir.multiply(mVelocity);
			mPlayer.setVelocity(mPlayer.getVelocity().multiply(0.25).add(dir));

			mDisableMobility = true;
			new BukkitRunnable() {
				@Override
				public void run() {
					if (((Entity) mPlayer).isOnGround() || mPlayer.isInWater() || mPlayer.isDead() || !mPlayer.isValid() || !mPlayer.isOnline()) {
						mDisableMobility = false;
						this.cancel();
					}
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);

			mRemainingPrimeDuration = mInitialPrimeDuration;
			if (mPrimeRunnable != null) {
				ClientModHandler.updateAbility(mPlayer, ClassAbility.HOLY_JAVELIN);
				mPrimeRunnable.cancel();
			}
			mPrimeRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					mRemainingPrimeDuration--;
					if (mRemainingPrimeDuration <= 0) {
						ClientModHandler.updateAbility(mPlayer, ClassAbility.HOLY_JAVELIN);
						this.cancel();
					}
				}
			};
			mPrimeRunnable.runTaskTimer(mPlugin, 0, 1);
			ClientModHandler.updateAbility(mPlayer, ClassAbility.HOLY_JAVELIN);
			return true;
		}
		for (LivingEntity enemy : mobs) {
			double damage = Crusade.enemyTriggersAbilities(enemy) ? mHereticDamage : mDamage;
			if (enemy != triggeringEnemy || !PlayerUtils.isFallingAttack(mPlayer)) {
				// Triggering enemy would've already received the magic damage from Divine Justice unless the attack wasn't a crit
				damage += bonusDamage;
			}
			EntityUtils.applyFire(mPlugin, FIRE_DURATION, enemy, mPlayer);
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true);
		}
		return true;
	}

	private static Description<HolyJavelin> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Throw a spear of light that deals damage")
			.addLine("and ignites mobs in a line in front of you.")
			.addLine("*Heretics* take increased damage.").styles(Cleric.HERETIC_COLOR)
			.addLine()
			.addLine("Attacking a mob while throwing the spear")
			.addLine("will also deal *Divine Justice*'s damage").styles(UNDERLINED)
			.addLine("to other mobs hit by the spear.")
			.addLine()
			.addStat("Damage: %d1R (s) (to non-Heretics)")
				.statValues(perRegion(a -> a.mDamage, DAMAGE_1[0], DAMAGE_1[1]))
			.addStat("Damage: %d1R (s) (to Heretics)")
				.statValues(perRegion(a -> a.mHereticDamage, HERETIC_DAMAGE_1[0], HERETIC_DAMAGE_1[1]))
			.addStat("Effect: Fire for %t")
				.statValues(stat(FIRE_DURATION))
			.addStat("Range: %r")
				.statValues(stat(a -> a.mRange, RANGE))
			.addStat("Cooldown: %t1")
				.statValues(cooldown(COOLDOWN_1))
			.addDashedLine();
	}

	private static Description<HolyJavelin> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Holy Javelin*'s damage and").styles(UNDERLINED)
			.addLine("reduce its cooldown.")
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2R (s) (to non-Heretics)")
				.statValues(perRegion(DAMAGE_1[0], DAMAGE_1[1]),
					perRegion(a -> a.mDamage, DAMAGE_2[0], DAMAGE_2[1]))
			.addStatComparison("Damage: %d1 -> %d2R (s) (to Heretics)")
				.statValues(perRegion(HERETIC_DAMAGE_1[0], HERETIC_DAMAGE_1[1]),
					perRegion(a -> a.mHereticDamage, HERETIC_DAMAGE_2[0], HERETIC_DAMAGE_2[1]))
			.addStatComparison("Cooldown: %t1 -> %t2")
			.statValues(
				cooldown(COOLDOWN_1),
				cooldown(COOLDOWN_2))
			.addLine()
			.addLine("If no mobs are hit by the spear, launch")
			.addLine("forwards with the spear instead, and prime")
			.addLine("a buff to your next *Divine Justice*").styles(UNDERLINED)
			.addLine("trigger within %t.")
			.statValues(stat(a -> a.mInitialPrimeDuration, DIVINE_JUSTICE_BONUS_PRIME_DURATION))
			.addLine("(Can only launch once while midair)")
			.addLine()
			.addStat("Damage Bonus: +%p (s)")
				.statValues(stat(a -> a.mDJDamageBonus, DIVINE_JUSTICE_BONUS_DAMAGE))
			.addDashedLine();
	}

	@Override
	public int getInitialAbilityDuration() {
		return mInitialPrimeDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mRemainingPrimeDuration;
	}
}
