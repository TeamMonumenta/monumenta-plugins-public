package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.FrostNovaCS;
import com.playmonumenta.plugins.effects.CholericFlamesAntiHeal;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FrostNova extends Ability {

	public static final String NAME = "Frost Nova";
	public static final ClassAbility ABILITY = ClassAbility.FROST_NOVA;

	public static final int DAMAGE_1 = 5;
	public static final int DAMAGE_2 = 10;
	public static final int SIZE = 7;
	public static final double SLOW_MULTIPLIER_1 = 0.2;
	public static final double SLOW_MULTIPLIER_2 = 0.4;
	public static final double ELITE_SLOW_MULTIPLIER_REDUCTION = 0.1;
	public static final double ENHANCED_DAMAGE_MODIFIER = 1.15;
	public static final int DURATION_TICKS = 4 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_FROZEN_DURATION = 2 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS_1 = 18 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS_2 = 16 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_COOLDOWN_TICKS = Constants.TICKS_PER_SECOND;
	public static final String ANTIHEAL_EFFECT = "FrostNovaAntiHeal";

	public static final String CHARM_DAMAGE = "Frost Nova Damage";
	public static final String CHARM_COOLDOWN = "Frost Nova Cooldown";
	public static final String CHARM_RANGE = "Frost Nova Range";
	public static final String CHARM_SLOW = "Frost Nova Slowness Amplifier";
	public static final String CHARM_DURATION = "Frost Nova Slowness Duration";
	public static final String CHARM_FROZEN = "Frost Nova Frozen Duration";

	public static final AbilityInfo<FrostNova> INFO =
		new AbilityInfo<>(FrostNova.class, NAME, FrostNova::new)
			.linkedSpell(ABILITY)
			.scoreboardId("FrostNova")
			.shorthandName("FN")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Damage and slow nearby mobs.")
			.cooldown(COOLDOWN_TICKS_1, COOLDOWN_TICKS_2, COOLDOWN_TICKS_1 - ENHANCED_COOLDOWN_TICKS, COOLDOWN_TICKS_2 - ENHANCED_COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FrostNova::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.ICE);

	private final double mBaseDamage;
	private final double mLevelDamage;
	private final double mLevelSlowMultiplier;
	private final int mDuration;
	private final double mRadius;
	private final int mFrozenDuration;

	private final FrostNovaCS mCosmetic;

	public FrostNova(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mBaseDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mLevelDamage = mBaseDamage * (isEnhanced() ? ENHANCED_DAMAGE_MODIFIER : 1);
		mLevelSlowMultiplier = (isLevelOne() ? SLOW_MULTIPLIER_1 : SLOW_MULTIPLIER_2) + CharmManager.getLevelPercentDecimal(player, CHARM_SLOW);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION_TICKS);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, SIZE);
		mFrozenDuration = CharmManager.getDuration(mPlayer, CHARM_FROZEN, ENHANCED_FROZEN_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new FrostNovaCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();
		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mLevelDamage);
		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mRadius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				EntityUtils.applySlow(mPlugin, mDuration, mLevelSlowMultiplier - ELITE_SLOW_MULTIPLIER_REDUCTION, mob);
			} else {
				EntityUtils.applySlow(mPlugin, mDuration, mLevelSlowMultiplier, mob);
				if (isEnhanced()) {
					EntityUtils.applyFreeze(mPlugin, mFrozenDuration, mob);
					// Choleric Flame already uses an anti heal effect, no need to create a new one as long as name is different.
					mPlugin.mEffectManager.addEffect(mob, ANTIHEAL_EFFECT, new CholericFlamesAntiHeal(mFrozenDuration));
				}
			}
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, false);
			mCosmetic.enemyEffect(mPlugin, mPlayer, mob);
			if (mob.getFireTicks() > 1) {
				mob.setFireTicks(1);
			}
		}

		// Extinguish fire on all nearby players
		for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}

		World world = mPlayer.getWorld();
		mCosmetic.onCast(mPlugin, mPlayer, world, mRadius);

		return true;
	}

	private static Description<FrostNova> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to unleash a frost nova, dealing ")
			.add(a -> a.mBaseDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" ice magic damage to enemies within ")
			.add(a -> a.mRadius, SIZE)
			.add(" blocks around you, afflicting them with ")
			.addPercent(a -> a.mLevelSlowMultiplier, SLOW_MULTIPLIER_1, false, Ability::isLevelOne)
			.add(" slowness for ")
			.addDuration(a -> a.mDuration, DURATION_TICKS)
			.add(" seconds, and extinguishing them if they're on fire. Slowness is reduced by ")
			.addPercent(ELITE_SLOW_MULTIPLIER_REDUCTION)
			.add(" on elites and bosses, and all players in the nova are also extinguished.")
			.addCooldown(COOLDOWN_TICKS_1, a -> !a.isEnhanced());
	}

	private static Description<FrostNova> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mBaseDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" and base slowness is increased to ")
			.addPercent(a -> a.mLevelSlowMultiplier, SLOW_MULTIPLIER_2, false, Ability::isLevelTwo)
			.add(".")
			.addCooldown(COOLDOWN_TICKS_2, a -> !a.isEnhanced());
	}

	private static Description<FrostNova> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased by ")
			.addPercent(ENHANCED_DAMAGE_MODIFIER - 1)
			.add(". Non elites and bosses are frozen for ")
			.addDuration(a -> a.mFrozenDuration, ENHANCED_FROZEN_DURATION)
			.add(" seconds, having their AI and gravity removed and gain 100% Anti-Heal. Cooldown is further reduced by ")
			.addDuration(ENHANCED_COOLDOWN_TICKS)
			.add(" second.");
	}

}
