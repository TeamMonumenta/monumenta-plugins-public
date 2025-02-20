package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.MagmaShieldCS;
import com.playmonumenta.plugins.effects.PercentAbilityDamageReceived;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class MagmaShield extends Ability {

	public static final String NAME = "Magma Shield";

	public static final int DAMAGE_1 = 6;
	public static final int DAMAGE_2 = 12;
	private static final int HEIGHT = 6;
	private static final int RADIUS = 7;
	public static final int FIRE_TICKS = 6 * 20;
	public static final float KNOCKBACK = 0.5f;
	// 70° on each side of look direction for XZ-plane (flattened Y),
	// so 140° angle of effect
	private static final int ANGLE = 70;
	public static final int COOLDOWN_TICKS = 12 * 20;
	public static final float ENHANCEMENT_FIRE_DAMAGE_BONUS = 0.5f;
	public static final float ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS = 0.35f;
	public static final String ENHANCEMENT_FIRE_DAMAGE_BONUS_EFFECT_NAME = "MagmaShieldFireDamageBonus";
	public static final String ENHANCEMENT_INFERNO_DAMAGE_BONUS_EFFECT_NAME = "MagmaShieldInfernoDamageBonus";
	public static final String ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS_EFFECT_NAME = "MagmaShieldFireAbilityDamageBonus";
	public static final int ENHANCEMENT_BONUS_DURATION = 6 * 20;

	public static final String CHARM_DAMAGE = "Magma Shield Damage";
	public static final String CHARM_RANGE = "Magma Shield Range";
	public static final String CHARM_COOLDOWN = "Magma Shield Cooldown";
	public static final String CHARM_DURATION = "Magma Shield Fire Duration";
	public static final String CHARM_KNOCKBACK = "Magma Shield Knockback";
	public static final String CHARM_CONE = "Magma Shield Cone";
	public static final String CHARM_FIRE_BONUS = "Magma Shield Fire Damage Bonus";
	public static final String CHARM_ABILITY_BONUS = "Magma Shield Fire Ability Damage Bonus";
	public static final String CHARM_DAMAGE_BONUS_DURATION = "Magma Shield Damage Bonus Duration";

	public static final AbilityInfo<MagmaShield> INFO =
		new AbilityInfo<>(MagmaShield.class, NAME, MagmaShield::new)
			.linkedSpell(ClassAbility.MAGMA_SHIELD)
			.scoreboardId("Magma")
			.shorthandName("MS")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Damage and ignite mobs in a cone.")
			.quest216Message("-------m-------r-------")
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", MagmaShield::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.MAGMA_CREAM);

	private final double mLevelDamage;
	private final double mRadius;
	private final int mFireDuration;
	private final double mFireBonusDamage;
	private final double mFireAbilityBonusDamage;
	private final int mEnhancementDuration;

	private final MagmaShieldCS mCosmetic;

	public MagmaShield(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RADIUS);
		mFireDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, FIRE_TICKS);
		mFireBonusDamage = ENHANCEMENT_FIRE_DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_FIRE_BONUS);
		mFireAbilityBonusDamage = ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ABILITY_BONUS);
		mEnhancementDuration = CharmManager.getDuration(mPlayer, CHARM_DAMAGE_BONUS_DURATION, ENHANCEMENT_BONUS_DURATION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new MagmaShieldCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mLevelDamage);
		double angle = Math.min(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE, ANGLE), 180);
		Hitbox hitbox = Hitbox.approximateCylinderSegment(
			LocationUtils.getHalfHeightLocation(mPlayer).add(0, -HEIGHT, 0), 2 * HEIGHT, mRadius, Math.toRadians(angle));

		for (LivingEntity target : hitbox.getHitMobs()) {
			EntityUtils.applyFire(mPlugin, mFireDuration, target, mPlayer);
			DamageUtils.damage(mPlayer, target, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, false);
			MovementUtils.knockAway(mPlayer, target, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK), true);
			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(target, ENHANCEMENT_FIRE_DAMAGE_BONUS_EFFECT_NAME,
					new PercentDamageReceived(mEnhancementDuration, mFireBonusDamage, EnumSet.of(DamageType.FIRE)));
				mPlugin.mEffectManager.addEffect(target, ENHANCEMENT_INFERNO_DAMAGE_BONUS_EFFECT_NAME,
					new PercentAbilityDamageReceived(mEnhancementDuration, mFireBonusDamage, EnumSet.of(ClassAbility.INFERNO)));
				mPlugin.mEffectManager.addEffect(target, ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS_EFFECT_NAME,
					new PercentAbilityDamageReceived(mEnhancementDuration, mFireAbilityBonusDamage,
						EnumSet.of(ClassAbility.MAGMA_SHIELD, ClassAbility.ELEMENTAL_ARROWS_FIRE, ClassAbility.ELEMENTAL_SPIRIT_FIRE,
							ClassAbility.STARFALL, ClassAbility.CHOLERIC_FLAMES, ClassAbility.FLAME_TOTEM)));
			}
		}

		mCosmetic.magmaEffects(mPlayer.getWorld(), mPlayer, mRadius, angle);

		return true;
	}

	private static Description<MagmaShield> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to summon a torrent of flames, dealing ")
			.add(a -> a.mLevelDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" fire magic damage to all enemies in front of you within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks, setting them on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds, and knocking them away.")
			.addCooldown(COOLDOWN_TICKS);
	}

	private static Description<MagmaShield> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increasd to ")
			.add(a -> a.mLevelDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<MagmaShield> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Enemies hit by this ability take ")
			.addPercent(a -> a.mFireBonusDamage, ENHANCEMENT_FIRE_DAMAGE_BONUS)
			.add(" extra damage from fire and ")
			.addPercent(a -> a.mFireAbilityBonusDamage, ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS)
			.add(" from fire based abilities for ")
			.addDuration(a -> a.mEnhancementDuration, ENHANCEMENT_BONUS_DURATION)
			.add(".");
	}

}
