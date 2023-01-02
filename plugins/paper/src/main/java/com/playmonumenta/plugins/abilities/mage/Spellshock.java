package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.SpellShockExplosion;
import com.playmonumenta.plugins.effects.SpellShockStatic;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class Spellshock extends Ability {

	public static final String NAME = "Spellshock";
	public static final ClassAbility ABILITY = ClassAbility.SPELLSHOCK;
	public static final String DAMAGED_THIS_TICK_METAKEY = "SpellShockDamagedThisTick";
	public static final String SPELL_SHOCK_STATIC_EFFECT_NAME = "SpellShockStaticEffect";
	public static final String PERCENT_SPEED_EFFECT_NAME = "SpellShockPercentSpeedEffect";
	public static final String PERCENT_SLOW_EFFECT_NAME = "SpellShockPercentSlowEffect";
	public static final String ENHANCEMENT_EFFECT_NAME = "SpellShockEnhancementEffect";

	public static final float DAMAGE_1 = 0.20f;
	public static final float DAMAGE_2 = 0.30f;
	public static final float MELEE_BONUS_1 = 0.10f;
	public static final float MELEE_BONUS_2 = 0.15f;
	public static final int SIZE = 3;
	public static final double SPEED_MULTIPLIER = 0.2;
	public static final int DURATION_TICKS = 6 * 20;
	public static final int SLOW_DURATION = 10;
	public static final double SLOW_MULTIPLIER = -0.3;
	public static final int ENHANCEMENT_DAMAGE = 6;
	public static final double ENHANCEMENT_RADIUS = 2.5;

	public static final String CHARM_SPELL = "Spellshock Spell Amplifier";
	public static final String CHARM_MELEE = "Spellshock Melee Amplifier";
	public static final String CHARM_SPEED = "Spellshock Speed Amplifier";
	public static final String CHARM_SLOW = "Spellshock Slowness Amplifier";
	public static final String CHARM_DETONATION_DAMAGE = "Spellshock Detonation Damage";
	public static final String CHARM_DETONATION_RADIUS = "Spellshock Detonation Radius";

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
				String.format("When an enemy that had ever had static applied dies, they explode, dealing %s damage to enemies in a %s block radius.",
					ENHANCEMENT_DAMAGE,
					ENHANCEMENT_RADIUS))
			.displayItem(new ItemStack(Material.GLOWSTONE_DUST, 1));

	private final float mLevelDamage;
	private final float mMeleeBonus;

	public Spellshock(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = (isLevelOne() ? DAMAGE_1 : DAMAGE_2) + (float) CharmManager.getLevelPercentDecimal(player, CHARM_SPELL);
		mMeleeBonus = (isLevelOne() ? MELEE_BONUS_1 : MELEE_BONUS_2) + (float) CharmManager.getLevelPercentDecimal(player, CHARM_MELEE);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ClassAbility eventAbility = event.getAbility();
		if (event.getType() == DamageType.MELEE
				&& mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(ItemStatUtils.EnchantmentType.MAGIC_WAND) > 0) {
			SpellShockStatic existingStatic = mPlugin.mEffectManager.getActiveEffect(enemy, SpellShockStatic.class);
			if (existingStatic != null) {
				event.setDamage(event.getDamage() * (1 + mMeleeBonus));
				mPlugin.mEffectManager.addEffect(enemy, PERCENT_SLOW_EFFECT_NAME, new PercentSpeed(SLOW_DURATION, SLOW_MULTIPLIER - CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW), PERCENT_SLOW_EFFECT_NAME));
				existingStatic.trigger();
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

				Location loc = LocationUtils.getHalfHeightLocation(enemy);
				World world = mPlayer.getWorld();
				new PartialParticle(Particle.SPELL_WITCH, loc, 60, 1, 1, 1, 0.001).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CRIT_MAGIC, loc, 45, 1, 1, 1, 0.25).spawnAsPlayerActive(mPlayer);
				world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 2.5f);
				world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 2.0f);
				world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 1.5f);

				// spellshock triggering other spellshocks propagates the damage at 100%
				double spellShockDamage = eventAbility == ClassAbility.SPELLSHOCK ? event.getDamage() : event.getDamage() * mLevelDamage;

				Hitbox hitbox = new Hitbox.SphereHitbox(loc, SIZE);
				for (LivingEntity hitMob : hitbox.getHitMobs()) {
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

				mPlugin.mEffectManager.addEffect(enemy, SPELL_SHOCK_STATIC_EFFECT_NAME, new SpellShockStatic(DURATION_TICKS));
				if (isEnhanced() && !mPlugin.mEffectManager.hasEffect(enemy, ENHANCEMENT_EFFECT_NAME)) {
					mPlugin.mEffectManager.addEffect(enemy, ENHANCEMENT_EFFECT_NAME,
							new SpellShockExplosion(mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer), SpellPower.getSpellDamage(mPlugin, mPlayer, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DETONATION_DAMAGE, ENHANCEMENT_DAMAGE)), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DETONATION_RADIUS, ENHANCEMENT_RADIUS), mPlayer.getUniqueId()));
				}
			}
		}
		return false; // Needs to apply to all damaged mobs. Uses an internal check to prevent recursion on dealing damage.
	}
}
