package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
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
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
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

	/* These are also used by Elemental Arrows */
	public static final String ENHANCE_WEAK_SRC = "SpellshockEnhanceWeakness";
	public static final double ENHANCE_WEAK_POTENCY = 0.2;
	public static final int ENHANCEMENT_EFFECT_DURATION = Constants.TICKS_PER_SECOND * 6;
	public static final EnumSet<DamageType> ENHANCE_WEAK_AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.PROJECTILE
	);

	private static final String DAMAGED_THIS_TICK_METAKEY = "SpellshockDamagedThisTick";
	private static final String SPELLSHOCK_STATIC_SRC = "SpellShockStaticEffect";
	private static final String SPEED_SRC = "SpellShockPercentSpeedEffect";
	private static final String ENHANCE_SPEED_METAKEY = "SpellshockUTick";
	private static final String ENHANCE_SPEED_SRC = "SpellShockEnhancePercentSpeedEffect";
	private static final double DAMAGE_1 = 0.2;
	private static final double DAMAGE_2 = 0.3;
	private static final double MELEE_BONUS_1 = 0.1;
	private static final double MELEE_BONUS_2 = 0.15;
	private static final int SPELLSHOCK_RADIUS = 3;
	private static final int STATIC_DURATION = Constants.TICKS_PER_SECOND * 6;
	private static final double SPEED_POTENCY = 0.2;
	private static final int SPEED_DURATION = Constants.TICKS_PER_SECOND * 6;
	private static final int SLOW_DURATION = Constants.HALF_TICKS_PER_SECOND;
	private static final double SLOW_POTENCY = 0.3;
	private static final double ENHANCE_SPEED_POTENCY = 0.03;
	private static final int ENHANCE_SPEED_MAX_STACKS = 5;
	private static final double ENHANCE_DAMAGE_MULT = 0.2;
	private static final double ENHANCE_SLOW_POTENCY = 0.15;
	private static final EnumSet<ClassAbility> FIRE_ABILITIES = EnumSet.of(
		ClassAbility.MAGMA_SHIELD,
		ClassAbility.ELEMENTAL_ARROWS_FIRE,
		ClassAbility.ELEMENTAL_SPIRIT_FIRE,
		ClassAbility.STARFALL
	);
	private static final EnumSet<ClassAbility> ICE_ABILITIES = EnumSet.of(
		ClassAbility.FROST_NOVA,
		ClassAbility.ELEMENTAL_ARROWS_ICE,
		ClassAbility.ELEMENTAL_SPIRIT_ICE,
		ClassAbility.BLIZZARD
	);
	private static final EnumSet<ClassAbility> ARCANE_ABILITIES = EnumSet.of(
		ClassAbility.COSMIC_MOONBLADE,
		ClassAbility.ARCANE_STRIKE,
		ClassAbility.MANA_LANCE
	);

	public static final String CHARM_SPELL = "Spellshock Spell Amplifier";
	public static final String CHARM_MELEE = "Spellshock Melee Amplifier";
	public static final String CHARM_SPEED = "Spellshock Speed Amplifier";
	public static final String CHARM_SLOW = "Spellshock Slowness Amplifier";
	public static final String CHARM_ENHANCE_DAMAGE = "Spellshock Enhancement Damage Amplifier";
	public static final String CHARM_ENHANCE_SLOW = "Spellshock Enhancement Slowness Amplifier";
	public static final String CHARM_ENHANCE_WEAK = "Spellshock Enhancement Weakness Amplifier";
	public static final String CHARM_RADIUS = "Spellshock Radius";

	public static final AbilityInfo<Spellshock> INFO =
		new AbilityInfo<>(Spellshock.class, NAME, Spellshock::new)
			.linkedSpell(ABILITY)
			.scoreboardId("SpellShock")
			.shorthandName("SS")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Deal extra damage to nearby enemies when damaging an enemy recently damaged by a spell.")
			.displayItem(Material.GLOWSTONE_DUST);

	private final SpellshockCS mCosmetic;
	private final double mSpellDamageMult;
	private final double mMeleeBonusMult;
	private final double mSpeedPotency;
	private final double mSlowPotency;
	private final double mEnhanceSpeedPotency;
	private final double mEnhanceDamageMult;
	private final double mEnhanceSlowPotency;
	private final double mEnhanceWeakPotency;
	private final double mRadius;

	public Spellshock(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mSpellDamageMult = (isLevelOne() ? DAMAGE_1 : DAMAGE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPELL);
		mMeleeBonusMult = (isLevelOne() ? MELEE_BONUS_1 : MELEE_BONUS_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_MELEE);
		mSpeedPotency = SPEED_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mSlowPotency = SLOW_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW);
		mEnhanceSpeedPotency = ENHANCE_SPEED_POTENCY;
		mEnhanceDamageMult = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_DAMAGE, ENHANCE_DAMAGE_MULT);
		mEnhanceSlowPotency = ENHANCE_SLOW_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_SLOW);
		mEnhanceWeakPotency = ENHANCE_WEAK_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_WEAK);
		mRadius = CharmManager.getDuration(player, CHARM_RADIUS, SPELLSHOCK_RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new SpellshockCS());

		// Wait a tick so that it can iterate through the ability manager properly, handles thunder arrows for the enhancement.
		new BukkitRunnable() {
			@Override
			public void run() {
				if (isEnhanced()) {
					for (final Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
						if (abil instanceof final ElementalArrows elementalArrows && elementalArrows.isEnhanced()) {
							elementalArrows.mSpellshockEnhanced = true;
						}
					}
				}
			}
		}.runTaskLater(mPlugin, 1);
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		final ClassAbility eventAbility = event.getAbility();
		final SpellShockStatic existingStatic = mPlugin.mEffectManager.getActiveEffect(enemy, SpellShockStatic.class);

		if (isEnhanced() && existingStatic != null) {
			if (FIRE_ABILITIES.contains(eventAbility)) {
				event.updateDamageWithMultiplier(1 + mEnhanceDamageMult);
			} else if (ICE_ABILITIES.contains(eventAbility)) {
				EntityUtils.applySlow(mPlugin, ENHANCEMENT_EFFECT_DURATION, mEnhanceSlowPotency, enemy);
			} else if (eventAbility == ClassAbility.THUNDER_STEP) {
				/* This will also happen for Thunder Elemental Arrows, but it is handled in ElementalArrows.java */
				mPlugin.mEffectManager.addEffect(enemy, ENHANCE_WEAK_SRC,
					new PercentDamageDealt(ENHANCEMENT_EFFECT_DURATION, -mEnhanceWeakPotency)
						.damageTypes(ENHANCE_WEAK_AFFECTED_DAMAGE_TYPES));
			} else if (ARCANE_ABILITIES.contains(eventAbility)) {
				/* Does not check for Arcane Strike (U) since this can only happen once per tick, does not check for
				 * Astral Omen to prevent triggering every spell */
				if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, ENHANCE_SPEED_METAKEY)) {
					final NavigableSet<Effect> oldEffects = mPlugin.mEffectManager.getEffects(mPlayer, ENHANCE_SPEED_SRC);
					if (oldEffects != null && !oldEffects.isEmpty()) {
						final Effect oldEffect = oldEffects.last();
						final int oldStacks = (int) Math.round(oldEffect.getMagnitude() / ENHANCE_SPEED_POTENCY);
						if (oldStacks >= ENHANCE_SPEED_MAX_STACKS) {
							oldEffect.setDuration(ENHANCEMENT_EFFECT_DURATION);
						} else {
							oldEffect.clearEffect();
							mPlugin.mEffectManager.addEffect(mPlayer, ENHANCE_SPEED_SRC,
								new PercentSpeed(ENHANCEMENT_EFFECT_DURATION, mEnhanceSpeedPotency * (oldStacks + 1), ENHANCE_SPEED_SRC)
									.deleteOnAbilityUpdate(true));
						}
					} else {
						mPlugin.mEffectManager.addEffect(mPlayer, ENHANCE_SPEED_SRC,
							new PercentSpeed(ENHANCEMENT_EFFECT_DURATION, mEnhanceSpeedPotency, ENHANCE_SPEED_SRC)
								.deleteOnAbilityUpdate(true));
					}
				}
			}
		}

		if (event.getType() == DamageType.MELEE
			&& mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(EnchantmentType.MAGIC_WAND) > 0
			&& existingStatic != null) {
			event.updateDamageWithMultiplier(1 + mMeleeBonusMult);
			EntityUtils.applySlow(mPlugin, SLOW_DURATION, mSlowPotency, enemy);
			existingStatic.trigger();
			mCosmetic.meleeClearStatic(mPlayer, enemy);
		} else if (eventAbility != null && !eventAbility.isFake() && eventAbility != ClassAbility.BLIZZARD
			&& eventAbility != ClassAbility.ASTRAL_OMEN) {
			// Check if the mob has static, and trigger it if possible; otherwise, apply/refresh it
			// We don't directly remove the effect, because we don't want mobs with static to immediately be re-applied
			// with it, so set flags instead
			if (existingStatic != null && !existingStatic.isTriggered()) {
				// Arcane Strike cannot trigger static (but can apply it)
				if (eventAbility == ClassAbility.ARCANE_STRIKE || eventAbility == ClassAbility.ARCANE_STRIKE_ENHANCED) {
					return false;
				}

				/* Finally the part where Static gets triggered */
				existingStatic.trigger();
				mCosmetic.spellshockEffect(mPlayer, enemy);

				if (isLevelTwo()) {
					mPlugin.mEffectManager.addEffect(mPlayer, SPEED_SRC,
						new PercentSpeed(SPEED_DURATION, mSpeedPotency, SPEED_SRC).deleteOnAbilityUpdate(true));
				}

				// spellshock triggering other spellshocks propagates the damage at 100%
				final double spellShockDamage = eventAbility == ClassAbility.SPELLSHOCK ? event.getDamage() : event.getDamage() * mSpellDamageMult;
				final Location loc = LocationUtils.getHalfHeightLocation(enemy);
				final Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
				for (final LivingEntity hitMob : hitbox.getHitMobs()) {
					if (hitMob.isDead()) {
						continue;
					}
					// Only damage a mob once per tick
					if (MetadataUtils.checkOnceThisTick(mPlugin, hitMob, DAMAGED_THIS_TICK_METAKEY)) {
						DamageUtils.damage(mPlayer, hitMob, DamageType.OTHER, spellShockDamage, ClassAbility.SPELLSHOCK, true);
					}
				}
			} else { // no static on the mob, apply new static

				// Spellshock and Elemental Arrows cannot apply static (but can trigger it)
				if (eventAbility == ClassAbility.SPELLSHOCK || eventAbility == ClassAbility.ELEMENTAL_ARROWS_FIRE
					|| eventAbility == ClassAbility.ELEMENTAL_ARROWS_ICE || eventAbility == ClassAbility.ELEMENTAL_ARROWS) {
					return false;
				}

				mPlugin.mEffectManager.addEffect(enemy, SPELLSHOCK_STATIC_SRC, new SpellShockStatic(STATIC_DURATION, mCosmetic));
			}
		}
		return false; // Needs to apply to all damaged mobs. Uses an internal check to prevent recursion on dealing damage.
	}

	private static Description<Spellshock> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Hitting an enemy with a spell inflicts Static for ")
			.addDuration(STATIC_DURATION)
			.add(" seconds. Hitting an enemy inflicted with Static triggers a spellshock centered on the enemy that deals ")
			.addPercent(a -> a.mSpellDamageMult, DAMAGE_1, false, Ability::isLevelOne)
			.add(" of the triggering spell's damage to all enemies within ")
			.add(a -> a.mRadius, SPELLSHOCK_RADIUS)
			.add(" blocks. Spellshocks can cause chain reactions on enemies with Static, but an enemy can only be hit by a spellshock once per tick. If a Static enemy is hit by a Melee attack, the hit gains ")
			.addPercent(a -> a.mMeleeBonusMult, MELEE_BONUS_1, false, Ability::isLevelOne)
			.add(" melee damage, the enemy receives ")
			.addPercent(a -> a.mSlowPotency, SLOW_POTENCY)
			.add(" slowness for ")
			.addDuration(SLOW_DURATION)
			.add(" seconds, and the Static dissipates.");
	}

	private static Description<Spellshock> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Spellshocks deal ")
			.addPercent(a -> a.mSpellDamageMult, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" of the triggering spell's damage and melee attacks gain ")
			.addPercent(a -> a.mMeleeBonusMult, MELEE_BONUS_2, false, Ability::isLevelTwo)
			.add(" melee damage. Additionally, gain ")
			.addPercent(a -> a.mSpeedPotency, SPEED_POTENCY)
			.add(" speed for ")
			.addDuration(SPEED_DURATION)
			.add(" seconds whenever a spellshock is triggered.");
	}

	private static Description<Spellshock> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Hitting an enemy inflicted with Static with certain spell types grants additional effects or applies additional debuffs. Arcane spells grant ")
			.addPercent(a -> a.mEnhanceSpeedPotency, ENHANCE_SPEED_POTENCY)
			.add(" speed for ")
			.addDuration(ENHANCEMENT_EFFECT_DURATION)
			.add(" seconds that stacks up to ")
			.add(a -> ENHANCE_SPEED_MAX_STACKS, ENHANCE_SPEED_MAX_STACKS)
			.add(" times, Fire spells gain ")
			.addPercent(a -> a.mEnhanceDamageMult, ENHANCE_DAMAGE_MULT)
			.add(" magic damage, enemies hit by Ice spells receive ")
			.addPercent(a -> a.mEnhanceSlowPotency, ENHANCE_SLOW_POTENCY)
			.add(" slowness for ")
			.addDuration(ENHANCEMENT_EFFECT_DURATION)
			.add(" seconds, and enemies hit by Lightning spells receive ")
			.addPercent(a -> a.mEnhanceWeakPotency, ENHANCE_WEAK_POTENCY)
			.add(" weakness for ")
			.addDuration(ENHANCEMENT_EFFECT_DURATION)
			.add(" seconds.");
	}
}
