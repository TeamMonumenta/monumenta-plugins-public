package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.SpellshockCS;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.SpellShockStatic;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Spellshock extends Ability {
	public static final String NAME = "Spellshock";
	public static final ClassAbility ABILITY = ClassAbility.SPELLSHOCK;
	public static final ClassAbility ENHANCE_ARCANE = ClassAbility.SPELLSHOCK_ARCANE;
	public static final ClassAbility ENHANCE_THUNDER = ClassAbility.SPELLSHOCK_THUNDER;

	/* These are also used by Elemental Arrows */
	public static final int ENHANCEMENT_EFFECT_DURATION = Constants.TICKS_PER_SECOND * 3;
	public static final int ENHANCE_LIGHTNING_DAMAGE = 3;
	public static final int ENHANCE_LIGHTNING_RANGE = 7;

	private static final String DAMAGED_THIS_TICK_METAKEY = "SpellshockDamagedThisTick";
	private static final String SPELLSHOCK_STATIC_SRC = "SpellShockStaticEffect";
	private static final String SPEED_SRC = "SpellShockPercentSpeedEffect";
	private static final String ENHANCE_DOT_EFFECT_NAME = "SpellShockEnhanceDoTEffect";
	private static final double ENHANCE_DOT_DAMAGE = 1.5;
	private static final int ENHANCE_DOT_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final double DAMAGE_1 = 0.2;
	private static final double DAMAGE_2 = 0.3;
	private static final double MELEE_BONUS_1 = 0.1;
	private static final double MELEE_BONUS_2 = 0.15;
	private static final int SPELLSHOCK_RADIUS = 3;
	private static final int STATIC_DURATION = Constants.TICKS_PER_SECOND * 6;
	private static final double SPEED_POTENCY = 0.2;
	private static final int SPEED_DURATION = Constants.TICKS_PER_SECOND * 6;
	private static final int SLOW_DURATION = 30;
	private static final double SLOW_POTENCY = 0.3;
	private static final double ENHANCE_DAMAGE_MULT = 0.2;
	private static final double ENHANCE_SLOW_POTENCY = 0.15;
	private static final double ENHANCE_VULN_POTENCY = 0.1;

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
	public static final String CHARM_ENHANCE_VULN = "Spellshock Enhancement Vulnerability Amplifier";
	public static final String CHARM_ENHANCE_WEAK = "Spellshock Enhancement Weakness Amplifier";
	public static final String CHARM_ENHANCE_DOT_DAMAGE = "Spellshock Enhancement DoT Damage";
	public static final String CHARM_ENHANCE_DOT_DURATION = "Spellshock Enhancement DoT Duration";
	public static final String CHARM_ENHANCE_LIGHTNING_DAMAGE = "Spellshock Enhancement Lightning Damage";
	public static final String CHARM_ENHANCE_LIGHTNING_RANGE = "Spellshock Enhancement Lightning Range";
	public static final String CHARM_RADIUS = "Spellshock Radius";

	public static final Style STATIC_COLOR = Style.style(TextColor.color(0xC883E3));

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
	private final double mEnhanceDamageMult;
	private final double mEnhanceSlowPotency;
	private final double mEnhanceVulnPotency;
	private final double mEnhanceDoTDamage;
	private final int mEnhanceDoTDuration;
	private final double mEnhanceLightningDamage;
	private final double mEnhanceLightningRange;
	private final double mRadius;

	public Spellshock(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mSpellDamageMult = (isLevelOne() ? DAMAGE_1 : DAMAGE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPELL);
		mMeleeBonusMult = (isLevelOne() ? MELEE_BONUS_1 : MELEE_BONUS_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_MELEE);
		mSpeedPotency = SPEED_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mSlowPotency = SLOW_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW);
		mEnhanceDamageMult = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_DAMAGE, ENHANCE_DAMAGE_MULT);
		mEnhanceSlowPotency = ENHANCE_SLOW_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_SLOW);
		mEnhanceVulnPotency = ENHANCE_VULN_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_VULN);
		mEnhanceDoTDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_DOT_DAMAGE, ENHANCE_DOT_DAMAGE);
		mEnhanceDoTDuration = (int) Math.floor(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_DOT_DURATION, ENHANCE_DOT_DURATION));
		mEnhanceLightningDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_LIGHTNING_DAMAGE, ENHANCE_LIGHTNING_DAMAGE);
		mEnhanceLightningRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_LIGHTNING_RANGE, ENHANCE_LIGHTNING_RANGE);
		mRadius = CharmManager.getDuration(player, CHARM_RADIUS, SPELLSHOCK_RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new SpellshockCS());

		// Wait a tick so that it can iterate through the ability manager properly, handles thunder arrows for the enhancement.
		if (isEnhanced()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					ElementalArrows elementalArrows = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, ElementalArrows.class);
					if (elementalArrows != null && elementalArrows.isEnhanced()) {
						elementalArrows.mSpellshockEnhanced = true;
					}
				}
			}.runTaskLater(mPlugin, 1);
		}
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		DamageType type = event.getType();
		if (type == DamageType.TRUE) {
			// Fixes a bug involving elemental arrows triggering this twice, once magic and once true
			return false;
		}

		final ClassAbility eventAbility = event.getAbility();
		final SpellShockStatic existingStatic = mPlugin.mEffectManager.getActiveEffect(enemy, SpellShockStatic.class);

		if (isEnhanced() && existingStatic != null) {
			if (FIRE_ABILITIES.contains(eventAbility)) {
				event.updateDamageWithMultiplier(1 + mEnhanceDamageMult);
			} else if (ICE_ABILITIES.contains(eventAbility)) {
				EntityUtils.applySlow(mPlugin, ENHANCEMENT_EFFECT_DURATION, mEnhanceSlowPotency, enemy);
				EntityUtils.applyVulnerability(mPlugin, ENHANCEMENT_EFFECT_DURATION, mEnhanceVulnPotency, enemy);
			} else if (eventAbility == ClassAbility.THUNDER_STEP) {
				// RNG 5 mob damage in 5 blocks, prioritizing elites and bosses
				float dmg = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mEnhanceLightningDamage);
				spellShockThunder(mPlayer, enemy, mEnhanceLightningRange, dmg, ENHANCE_THUNDER, mCosmetic);
			} else if (ARCANE_ABILITIES.contains(eventAbility)) {
				// 3 magic dot per second for 3s
				float dotDmg = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mEnhanceDoTDamage);
				CustomDamageOverTime dot = new CustomDamageOverTime(mEnhanceDoTDuration, dotDmg, Constants.TICKS_PER_SECOND, mPlayer, ENHANCE_ARCANE, DamageEvent.DamageType.MAGIC);
				dot.setVisuals(this.mCosmetic::damageOverTimeEffects);
				mPlugin.mEffectManager.addEffect(enemy, ENHANCE_DOT_EFFECT_NAME, dot);
			}
		}

		if (type == DamageType.MELEE
			&& mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(EnchantmentType.MAGIC_WAND) > 0
			&& existingStatic != null) {
			event.updateDamageWithMultiplier(1 + mMeleeBonusMult);
			EntityUtils.applySlow(mPlugin, SLOW_DURATION, mSlowPotency, enemy);
			existingStatic.trigger();
			mCosmetic.meleeClearStatic(mPlayer, enemy);
		} else if (eventAbility != null && !eventAbility.isFake() && eventAbility != ClassAbility.BLIZZARD
			&& eventAbility != ClassAbility.ASTRAL_OMEN && eventAbility != ClassAbility.SPELLSHOCK_ARCANE
			&& eventAbility != ClassAbility.SPELLSHOCK_THUNDER) {
			// Check if the mob has static, and trigger it if possible; otherwise, apply/refresh it
			// We don't directly remove the effect, because we don't want mobs with static to immediately be re-applied
			// with it, so set flags instead
			if (existingStatic != null && !existingStatic.isTriggered()) {
				// Arcane Strike cannot trigger static (but can apply it)
				if (eventAbility == ClassAbility.ARCANE_STRIKE || eventAbility == ClassAbility.ARCANE_STRIKE_ENHANCED) {
					// In fact the melee hit will eat the static that Arcane strike should create since it runs after, but we add the static back after to simulate an order reversal.
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
						SpellShockStatic newStatic = new SpellShockStatic(STATIC_DURATION, mCosmetic);
						mPlugin.mEffectManager.clearEffects(enemy, SPELLSHOCK_STATIC_SRC); // Need to clear the old static else the new one will inherit its mTriggered status
						mPlugin.mEffectManager.addEffect(enemy, SPELLSHOCK_STATIC_SRC, newStatic);
					});
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
				} else if (eventAbility == ClassAbility.ARCANE_STRIKE || eventAbility == ClassAbility.ARCANE_STRIKE_ENHANCED) { // Arcane strike should apply Static later to stop the melee attack eating the static
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> mPlugin.mEffectManager.addEffect(enemy, SPELLSHOCK_STATIC_SRC, new SpellShockStatic(STATIC_DURATION, mCosmetic)));
					return false;
				}

				mPlugin.mEffectManager.addEffect(enemy, SPELLSHOCK_STATIC_SRC, new SpellShockStatic(STATIC_DURATION, mCosmetic));
			}
		}
		return false; // Needs to apply to all damaged mobs. Uses an internal check to prevent recursion on dealing damage.
	}

	public static void spellShockThunder(Player player, LivingEntity from, double range, double damage, ClassAbility ability, SpellshockCS mCosmetic) {
		//get targets
		Location loc = from.getLocation();
		List<LivingEntity> targets = EntityUtils.getNearbyMobs(loc, range);
		List<LivingEntity> elites = new ArrayList<>();
		List<LivingEntity> mobs = new ArrayList<>();
		targets.removeIf(e -> e == from);
		for (LivingEntity mob : targets) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				elites.add(mob);
			} else {
				mobs.add(mob);
			}
		}

		//choose target
		LivingEntity target = null;
		if (!elites.isEmpty()) {
			Collections.shuffle(elites);
			target = elites.getFirst();
		} else if (!mobs.isEmpty()) {
			Collections.shuffle(mobs);
			target = mobs.getFirst();
		}
		if (target != null) {
			// attack target
			DamageUtils.damage(player, target, DamageType.MAGIC, damage, ability, true, false);
			mCosmetic.enhancedLightning(from, target);
		}
	}

	private static Description<Spellshock> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Hitting a mob with a non-Spellshock ability inflicts")
			.addLine("them with *Static* for %t.").styles(STATIC_COLOR)
				.statValues(stat(STATIC_DURATION))
			.addLine()
			.addLine("When one of your abilities hits a mob with *Static*,").styles(STATIC_COLOR)
			.addLine("trigger a *Spellshock* that deals a portion of that").styles(STATIC_COLOR)
			.addLine("ability's damage to that mob and nearby mobs, which")
			.addLine("can trigger other mobs' *Static* in a chain reaction.").styles(STATIC_COLOR)
			.addLine()
			.addStat("Damage: %p1 (s) (of the ability's damage)")
				.statValues(stat(a -> a.mSpellDamageMult, DAMAGE_1))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, SPELLSHOCK_RADIUS))
			.addLine()
			.addLine("Attacks against mobs with *Static* clear it").styles(STATIC_COLOR)
			.addLine("to deal increased damage and inflict slowness.")
			.addLine()
			.addStat("Damage Boost: +%p1 (m)")
				.statValues(stat(a -> a.mMeleeBonusMult, MELEE_BONUS_1))
			.addStat("Effect: %p Slowness for %t")
				.statValues(stat(a -> a.mSlowPotency, SLOW_POTENCY), stat(SLOW_DURATION))
			.addDashedLine();
	}

	private static Description<Spellshock> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Spellshock*'s damage.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Damage: %p1 -> %p2 (s)")
				.statValues(stat(DAMAGE_1), stat(a -> a.mSpellDamageMult, DAMAGE_2))
			.addStatComparison("Damage Boost: +%p1 -> +%p2 (m)")
				.statValues(stat(MELEE_BONUS_1), stat(a -> a.mMeleeBonusMult, MELEE_BONUS_2))
			.addLine()
			.addLine("Triggering a *Spellshock* grants you speed.").styles(STATIC_COLOR)
			.addLine()
			.addStat("Effect: +%p Speed for %t")
				.statValues(stat(a -> a.mSpeedPotency, SPEED_POTENCY), stat(SPEED_DURATION))
			.addDashedLine();
	}

	private static Description<Spellshock> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Triggering a *Spellshock* performs additional").styles(STATIC_COLOR)
			.addLine("effects based on the ability's element.")
			.addLine()
			.addLine("*Arcane* abilities inflict damage over time.").styles(Mage.ARCANE_COLOR)
			.addStat("Effect: %d (s) every %t for %t")
				.statValues(stat(a -> a.mEnhanceDoTDamage, ENHANCE_DOT_DAMAGE), stat(20), stat(a -> a.mEnhanceDoTDuration, ENHANCE_DOT_DURATION))
			.addLine()
			.addLine("*Fire* abilities deal increased damage.").styles(Mage.FIRE_COLOR)
			.addStat("Damage Boost: +%p (s)")
				.statValues(stat(a -> a.mEnhanceDamageMult, ENHANCE_DAMAGE_MULT))
			.addLine()
			.addLine("*Ice* abilities inflict slowness and vulnerability.").styles(Mage.ICE_COLOR)
			.addStat("Effect: %p Slowness for %t")
				.statValues(stat(a -> a.mEnhanceSlowPotency, ENHANCE_SLOW_POTENCY), stat(ENHANCEMENT_EFFECT_DURATION))
			.addStat("Effect: %p Vulnerability for %t")
				.statValues(stat(a -> a.mEnhanceVulnPotency, ENHANCE_VULN_POTENCY), stat(ENHANCEMENT_EFFECT_DURATION))
			.addLine()
			.addLine("*Thunder* abilities deal extra damage to a").styles(Mage.THUNDER_COLOR)
			.addLine("random nearby mob.")
			.addLine("(Prioritizes Elites and Bosses)")
			.addStat("Damage: %d (s)")
				.statValues(stat(a -> a.mEnhanceLightningDamage, ENHANCE_LIGHTNING_DAMAGE))
			.addStat("Range: %r")
				.statValues(stat(a -> a.mEnhanceLightningRange, ENHANCE_LIGHTNING_RANGE))
			.addDashedLine();
	}
}
