package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.SpellshockCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.SpellShockStatic;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import java.util.NavigableSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


public class Spellshock extends Ability {

	public static final String NAME = "Spellshock";
	public static final ClassAbility ABILITY = ClassAbility.SPELLSHOCK;
	public static final String DAMAGED_THIS_TICK_METAKEY = "SpellShockDamagedThisTick";
	public static final String SPELL_SHOCK_STATIC_EFFECT_NAME = "SpellShockStaticEffect";
	public static final String PERCENT_SPEED_EFFECT_NAME = "SpellShockPercentSpeedEffect";
	public static final String PERCENT_SLOW_EFFECT_NAME = "SpellShockPercentSlowEffect";
	public static final String PERCENT_SLOW_ENHANCEMENT_EFFECT_NAME = "SpellShockEnhancementPercentSlowEffect";
	public static final String PERCENT_WEAK_ENHANCEMENT_EFFECT_NAME = "SpellShockEnhancementPercentWeakEffect";

	public static final float DAMAGE_1 = 0.20f;
	public static final float DAMAGE_2 = 0.30f;
	public static final float MELEE_BONUS_1 = 0.10f;
	public static final float MELEE_BONUS_2 = 0.15f;
	public static final int SIZE = 3;
	public static final double SPEED_MULTIPLIER = 0.2;
	public static final int DURATION_TICKS = 6 * 20;
	public static final int SLOW_DURATION = 10;
	public static final double SLOW_MULTIPLIER = -0.3;
	public static final double ENHANCEMENT_SPEED_MULTIPLIER = 0.03;
	public static final int ENHANCEMENT_SPEED_MAX_STACKS = 5;
	public static final String ENHANCEMENT_SPEED_METAKEY = "SpellshockUTick";
	public static final String PERCENT_SPEED_ENHANCE_EFFECT_NAME = "SpellShockEnhancePercentSpeedEffect";
	public static final double ENHANCEMENT_DAMAGE_MULTIPLIER = 0.2;
	public static final double ENHANCEMENT_SLOW_MULTIPLIER = -0.15;
	public static final double ENHANCEMENT_WEAK_MULTIPLIER = -0.2;
	public static final int ENHANCEMENT_EFFECT_DURATION = 6 * 20;
	public static final EnumSet<DamageType> ENHANCEMENT_WEAKEN_AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.PROJECTILE
	);

	public static final String CHARM_SPELL = "Spellshock Spell Amplifier";
	public static final String CHARM_MELEE = "Spellshock Melee Amplifier";
	public static final String CHARM_SPEED = "Spellshock Speed Amplifier";
	public static final String CHARM_SLOW = "Spellshock Slowness Amplifier";
	public static final String CHARM_ENHANCE_KNOCKBACK = "Spellshock Enhancement Knockback Amplifier";
	public static final String CHARM_ENHANCE_DAMAGE = "Spellshock Enhancement Damage Amplifier";
	public static final String CHARM_ENHANCE_SLOW = "Spellshock Enhancement Slowness Amplifier";
	public static final String CHARM_ENHANCE_WEAK = "Spellshock Enhancement Weakness Amplifier";

	private final SpellshockCS mCosmetic;

	public static final AbilityInfo<Spellshock> INFO =
		new AbilityInfo<>(Spellshock.class, NAME, Spellshock::new)
			.linkedSpell(ABILITY)
			.scoreboardId("SpellShock")
			.shorthandName("SS")
			.descriptions(
				String.format("Hitting an enemy with a spell inflicts static for %s seconds." +
					              " If an enemy with static is hit by another spell, a spellshock centered on the enemy deals %s%% of the triggering spell's damage to all mobs in a %s block radius." +
					              " Spellshock can cause a chain reaction on enemies with static. An enemy can only be hit by a spellshock once per tick." +
					              " If a static mob is struck by a melee attack, it takes %s%% more damage on the hit and is slowed by %s%% for %s seconds, clearing the static.",
					DURATION_TICKS / 20,
					(int) (DAMAGE_1 * 100),
					SIZE,
					(int) (MELEE_BONUS_1 * 100),
					(int) (-SLOW_MULTIPLIER * 100),
					SLOW_DURATION / 20.0),
				String.format("Damage is increased to %s%% for spells and %s%% for melee. Additionally, gain +%s%% speed for 2 seconds whenever a spellshock is triggered.",
					(int) (DAMAGE_2 * 100),
					(int) (MELEE_BONUS_2 * 100),
					(int) (SPEED_MULTIPLIER * 100)),
				String.format("Attacking enemies with Spellshock with an Arcane spell will grant you +%s%% speed for %s seconds, stacking up to %s times. Enemies that have Spellshock will take %s%% more damage from your Fire spells, be slowed by %s%% from your Ice spells for %s seconds, and will be weakened by %s%% from your Lightning spells for %s seconds.",
					StringUtils.multiplierToPercentage(ENHANCEMENT_SPEED_MULTIPLIER),
					ENHANCEMENT_EFFECT_DURATION / 20.0,
					ENHANCEMENT_SPEED_MAX_STACKS,
					(int) (ENHANCEMENT_DAMAGE_MULTIPLIER * 100),
					(int) (ENHANCEMENT_SLOW_MULTIPLIER * -100),
					ENHANCEMENT_EFFECT_DURATION / 20.0,
					(int) (ENHANCEMENT_WEAK_MULTIPLIER * -100),
					ENHANCEMENT_EFFECT_DURATION / 20.0))
			.simpleDescription("Deal extra damage to nearby mobs when damaging a mob recently damaged by a spell.")
			.displayItem(Material.GLOWSTONE_DUST);

	private final float mLevelDamage;
	private final float mMeleeBonus;

	public Spellshock(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = (isLevelOne() ? DAMAGE_1 : DAMAGE_2) + (float) CharmManager.getLevelPercentDecimal(player, CHARM_SPELL);
		mMeleeBonus = (isLevelOne() ? MELEE_BONUS_1 : MELEE_BONUS_2) + (float) CharmManager.getLevelPercentDecimal(player, CHARM_MELEE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SpellshockCS());

		// Wait a tick so that it can iterate through the ability manager properly, handles thunder arrows for the enhancement.
		new BukkitRunnable() {
			@Override
			public void run() {
				if (isEnhanced()) {
					for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
						if (abil instanceof ElementalArrows elementalArrows && elementalArrows.isEnhanced()) {
							elementalArrows.mSpellshockEnhanced = true;
						}
					}
				}
			}
		}.runTaskLater(mPlugin, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ClassAbility eventAbility = event.getAbility();

		if (isEnhanced()) {
			SpellShockStatic existingStatic = mPlugin.mEffectManager.getActiveEffect(enemy, SpellShockStatic.class);
			if (existingStatic != null) {
				if (eventAbility == ClassAbility.MAGMA_SHIELD ||
					eventAbility == ClassAbility.ELEMENTAL_ARROWS_FIRE ||
					eventAbility == ClassAbility.ELEMENTAL_SPIRIT_FIRE ||
					eventAbility == ClassAbility.STARFALL) {
					double damageMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_DAMAGE, ENHANCEMENT_DAMAGE_MULTIPLIER) + 1;
					event.setDamage(event.getDamage() * damageMultiplier);
				} else if (eventAbility == ClassAbility.FROST_NOVA ||
					eventAbility == ClassAbility.ELEMENTAL_ARROWS_ICE ||
					eventAbility == ClassAbility.ELEMENTAL_SPIRIT_ICE ||
					eventAbility == ClassAbility.BLIZZARD) {
					double slownessMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_SLOW, ENHANCEMENT_SLOW_MULTIPLIER);
					mPlugin.mEffectManager.addEffect(enemy, PERCENT_SLOW_ENHANCEMENT_EFFECT_NAME, new PercentSpeed(ENHANCEMENT_EFFECT_DURATION, slownessMultiplier, PERCENT_SLOW_ENHANCEMENT_EFFECT_NAME));
				} else if (eventAbility == ClassAbility.THUNDER_STEP) {
					double weaknessMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_WEAK, ENHANCEMENT_WEAK_MULTIPLIER);
					mPlugin.mEffectManager.addEffect(enemy, PERCENT_WEAK_ENHANCEMENT_EFFECT_NAME, new PercentDamageDealt(ENHANCEMENT_EFFECT_DURATION, weaknessMultiplier, ENHANCEMENT_WEAKEN_AFFECTED_DAMAGE_TYPES));
					// Also should work on Thunder Elemental Arrows, but can't be done the same way, see Elemental Arrow Code File
				} else if (eventAbility == ClassAbility.COSMIC_MOONBLADE ||
					eventAbility == ClassAbility.ARCANE_STRIKE ||
					eventAbility == ClassAbility.MANA_LANCE) {
					// No point checking Arcane Strike (u) since this can only happen once per tick. Also don't want to check Omen or it will trigger every single spell.
					if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, ENHANCEMENT_SPEED_METAKEY)) {
						NavigableSet<Effect> oldEffects = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_SPEED_ENHANCE_EFFECT_NAME);
						if (oldEffects != null && !oldEffects.isEmpty()) {
							Effect oldEffect = oldEffects.last();
							int oldStacks = (int) Math.round(oldEffect.getMagnitude() / ENHANCEMENT_SPEED_MULTIPLIER);
							if (oldStacks >= ENHANCEMENT_SPEED_MAX_STACKS) {
								oldEffect.setDuration(ENHANCEMENT_EFFECT_DURATION);
							} else {
								oldEffect.clearEffect();
								mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_ENHANCE_EFFECT_NAME, new PercentSpeed(ENHANCEMENT_EFFECT_DURATION, ENHANCEMENT_SPEED_MULTIPLIER * (oldStacks + 1), PERCENT_SPEED_ENHANCE_EFFECT_NAME));
							}
						} else {
							mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_ENHANCE_EFFECT_NAME, new PercentSpeed(ENHANCEMENT_EFFECT_DURATION, ENHANCEMENT_SPEED_MULTIPLIER, PERCENT_SPEED_ENHANCE_EFFECT_NAME));
						}
					}
				}
			}
		}

		if (event.getType() == DamageType.MELEE
				&& mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(EnchantmentType.MAGIC_WAND) > 0) {
			SpellShockStatic existingStatic = mPlugin.mEffectManager.getActiveEffect(enemy, SpellShockStatic.class);
			if (existingStatic != null) {
				event.setDamage(event.getDamage() * (1 + mMeleeBonus));
				mPlugin.mEffectManager.addEffect(enemy, PERCENT_SLOW_EFFECT_NAME, new PercentSpeed(SLOW_DURATION, SLOW_MULTIPLIER - CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW), PERCENT_SLOW_EFFECT_NAME));
				existingStatic.trigger();
				mCosmetic.meleeClearStatic(mPlayer, enemy);
			}
		} else if (eventAbility != null
				&& !eventAbility.isFake()
				&& eventAbility != ClassAbility.BLIZZARD
				&& eventAbility != ClassAbility.ASTRAL_OMEN) {
			// Check if the mob has static, and trigger it if possible; otherwise, apply/refresh it
			// We don't directly remove the effect, because we don't want mobs with static to immediately be re-applied with it, so set flags instead
			SpellShockStatic existingStatic = mPlugin.mEffectManager.getActiveEffect(enemy, SpellShockStatic.class);
			if (existingStatic != null && !existingStatic.isTriggered()) {
				// static on the mob, trigger it

				// Arcane Strike cannot trigger static (but can apply it)
				if (eventAbility == ClassAbility.ARCANE_STRIKE
						|| eventAbility == ClassAbility.ARCANE_STRIKE_ENHANCED) {
					return false;
				}

				existingStatic.trigger();

				if (isLevelTwo()) {
					mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION_TICKS, SPEED_MULTIPLIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED), PERCENT_SPEED_EFFECT_NAME));
				}

				mCosmetic.spellshockEffect(mPlayer, enemy);

				// spellshock triggering other spellshocks propagates the damage at 100%
				double spellShockDamage = eventAbility == ClassAbility.SPELLSHOCK ? event.getDamage() : event.getDamage() * mLevelDamage;

				Location loc = LocationUtils.getHalfHeightLocation(enemy);
				Hitbox hitbox = new Hitbox.SphereHitbox(loc, SIZE);
				for (LivingEntity hitMob : hitbox.getHitMobs()) {
					if (hitMob.isDead()) {
						continue;
					}
					// Only damage a mob once per tick
					if (MetadataUtils.checkOnceThisTick(mPlugin, hitMob, DAMAGED_THIS_TICK_METAKEY)) {
						DamageUtils.damage(mPlayer, hitMob, DamageType.OTHER, spellShockDamage, ClassAbility.SPELLSHOCK, true);
					}
				}
			} else {
				// no static on the mob, apply new static

				// Spellshock and Elemental Arrows cannot apply static (but can trigger it)
				if (eventAbility == ClassAbility.SPELLSHOCK
						|| eventAbility == ClassAbility.ELEMENTAL_ARROWS_FIRE
						|| eventAbility == ClassAbility.ELEMENTAL_ARROWS_ICE
						|| eventAbility == ClassAbility.ELEMENTAL_ARROWS) {
					return false;
				}

				mPlugin.mEffectManager.addEffect(enemy, SPELL_SHOCK_STATIC_EFFECT_NAME, new SpellShockStatic(DURATION_TICKS, mCosmetic));
			}
		}
		return false; // Needs to apply to all damaged mobs. Uses an internal check to prevent recursion on dealing damage.
	}
}
