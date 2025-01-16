package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.ElementalArrowsCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.SpellShockStatic;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Stray;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;


public class ElementalArrows extends Ability {
	public static final String NAME = "Elemental Arrows";
	public static final ClassAbility ABILITY = ClassAbility.ELEMENTAL_ARROWS;
	public static final ClassAbility ABILITY_FIRE = ClassAbility.ELEMENTAL_ARROWS_FIRE;
	public static final ClassAbility ABILITY_ICE = ClassAbility.ELEMENTAL_ARROWS_ICE;

	public static final double AOE_DAMAGE_MULTIPLIER = 0.2;
	public static final double ENHANCED_DAMAGE_MULTIPLIER = 0.5;
	public static final int ELEMENTAL_ARROWS_BONUS_DAMAGE = 8;
	public static final int ELEMENTAL_ARROWS_DURATION = 20 * 6;
	public static final double ELEMENTAL_ARROWS_RADIUS = 3.0;
	public static final double SLOW_AMPLIFIER = 0.2;
	public static final int ENHANCED_ARROW_COOLDOWN = 8 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_ARROW_STUN_DURATION = 1 * Constants.TICKS_PER_SECOND;
	public static final String FIRE_ARROW_METAKEY = "ElementalArrowsFireArrow";
	public static final String ICE_ARROW_METAKEY = "ElementalArrowsIceArrow";
	public static final String THUNDER_ARROW_METAKEY = "ElementalArrowsThunderArrow";

	public static final String CHARM_DAMAGE = "Elemental Arrows Damage";
	public static final String CHARM_AREA_DAMAGE = "Elemental Arrows Area Damage";
	public static final String CHARM_DURATION = "Elemental Arrows Duration";
	public static final String CHARM_STUN_DURATION = "Elemental Arrows Stun Duration";
	public static final String CHARM_SLOWNESS = "Elemental Arrows Slowness Amplifier";
	public static final String CHARM_RANGE = "Elemental Arrows Range";
	public static final String CHARM_THUNDER_COOLDOWN = "Elemental Arrows Thunder Arrow Cooldown";

	private final ElementalArrowsCS mCosmetic;

	public static final AbilityInfo<ElementalArrows> INFO =
		new AbilityInfo<>(ElementalArrows.class, NAME, ElementalArrows::new)
			.linkedSpell(ABILITY)
			.scoreboardId("Elemental")
			.shorthandName("EA")
			.descriptions(
				String.format("Your fully drawn projectiles are set on fire. If sneaking, shoot an ice arrow instead, afflicting the target with %s%% Slowness for %s seconds. " +
					              "Projectiles shot this way are magically infused, scaling off of magic damage instead of projectile damage. Ice arrows deal %s extra damage to Blazes. " +
					              "Fire arrows deal %s extra damage to strays. This skill can not apply Spellshock.",
					(int) (SLOW_AMPLIFIER * 100),
					ELEMENTAL_ARROWS_DURATION / 20,
					ELEMENTAL_ARROWS_BONUS_DAMAGE,
					ELEMENTAL_ARROWS_BONUS_DAMAGE
				),
				String.format("Your fire arrows also set nearby enemies within a radius of %s blocks on fire when they hit a target. " +
					              "Your ice arrows also slow nearby enemies within a radius of %s blocks when they hit a target. " +
					              "Both area of effect effects do %s%% bow damage to all targets affected.",
					(int) ELEMENTAL_ARROWS_RADIUS,
					(int) ELEMENTAL_ARROWS_RADIUS,
					(int) (AOE_DAMAGE_MULTIPLIER * 100)
				),
				String.format("Your next elemental arrow every %ss also stuns enemies hit for %ss and deals %s%% more damage.",
					ENHANCED_ARROW_COOLDOWN / 20,
					ENHANCED_ARROW_STUN_DURATION / 20,
					(int) (ENHANCED_DAMAGE_MULTIPLIER * 100)
				))
			.simpleDescription("Shoot fire and ice projectiles that deal magic damage.")
			.cooldown(0, 0, ENHANCED_ARROW_COOLDOWN, CHARM_THUNDER_COOLDOWN)
			.displayItem(Material.SPECTRAL_ARROW);

	private double mLastDamage = 0;
	public boolean mSpellshockEnhanced = false;

	public ElementalArrows(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ElementalArrowsCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!(event.getDamager() instanceof Projectile proj)
			    || !EntityUtils.isAbilityTriggeringProjectile(proj, true)
			    || event.getType() != DamageType.PROJECTILE) {
			return false;
		}
		ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(proj);
		if (playerItemStats == null) {
			return false;
		}

		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, ELEMENTAL_ARROWS_DURATION);

		boolean thunder = proj.hasMetadata(THUNDER_ARROW_METAKEY);
		if (proj.hasMetadata(FIRE_ARROW_METAKEY)) {
			applyArrowEffects(event, proj, enemy, thunder, ABILITY_FIRE, playerItemStats, Stray.class, (entity) -> {
				EntityUtils.applyFire(mPlugin, duration, entity, mPlayer, playerItemStats);
				mCosmetic.fireEffect(mPlayer, enemy);
			});
		} else if (proj.hasMetadata(ICE_ARROW_METAKEY)) {
			double slowAmplifier = SLOW_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
			applyArrowEffects(event, proj, enemy, thunder, ABILITY_ICE, playerItemStats, Blaze.class, (entity) -> {
				EntityUtils.applySlow(mPlugin, duration, slowAmplifier, entity);
				mCosmetic.iceEffect(mPlayer, enemy);
			});
		}
		return false; // creates new damage instances, but of a type it doesn't handle again
	}

	private void applyArrowEffects(DamageEvent event, Projectile proj, LivingEntity enemy, boolean thunder, ClassAbility ability, ItemStatManager.PlayerItemStats playerItemStats, Class<? extends Entity> bonusEntity, Consumer<LivingEntity> effectAction) {
		double baseDamage = playerItemStats.getMainhandAddStats().get(AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat());
		@Nullable
		ItemStack arrowItem = proj instanceof AbstractArrow arrow ? arrow.getItemStack() : null;
		if (arrowItem != null) {
			baseDamage *= 1 + ItemStatUtils.getAttributeAmount(arrowItem, AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, Slot.PROJECTILE);
		}

		double targetDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, baseDamage);
		if (bonusEntity.isInstance(enemy)) {
			targetDamage += ELEMENTAL_ARROWS_BONUS_DAMAGE;
		}
		targetDamage += PointBlank.apply(mPlayer, enemy, playerItemStats.getItemStats().get(EnchantmentType.POINT_BLANK));
		targetDamage += Sniper.apply(mPlayer, enemy, playerItemStats.getItemStats().get(EnchantmentType.SNIPER));
		if (thunder) {
			targetDamage *= 1 + ENHANCED_DAMAGE_MULTIPLIER;
		}

		mLastDamage = targetDamage;

		if (thunder) {
			int stunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, ENHANCED_ARROW_STUN_DURATION);
			effectAction = effectAction.andThen(entity -> EntityUtils.applyStun(mPlugin, stunDuration, entity));
			mCosmetic.thunderEffect(mPlayer, enemy);
			if (mSpellshockEnhanced) {
				SpellShockStatic existingStatic = mPlugin.mEffectManager.getActiveEffect(enemy, SpellShockStatic.class);
				if (existingStatic != null && existingStatic.isTriggered()) {
					double weaknessMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, Spellshock.CHARM_ENHANCE_WEAK, Spellshock.ENHANCEMENT_WEAK_MULTIPLIER);
					mPlugin.mEffectManager.addEffect(enemy, Spellshock.PERCENT_WEAK_ENHANCEMENT_EFFECT_NAME, new PercentDamageDealt(Spellshock.ENHANCEMENT_EFFECT_DURATION, weaknessMultiplier, Spellshock.ENHANCEMENT_WEAKEN_AFFECTED_DAMAGE_TYPES));
				}
			}
		}
		effectAction.accept(enemy);
		//Jank fix - run the effect twice, before and after the damage
		//For some reason, dealing the damage makes the fire clear later in the tick
		//Should be ran beforehand as well so the effects can trigger stuff like Choler
		Consumer<LivingEntity> finalEffectAction = effectAction;
		Bukkit.getScheduler().runTask(mPlugin, () -> finalEffectAction.accept(enemy));

		event.setDamage(0);
		DamageUtils.damage(mPlayer, enemy, new DamageEvent.Metadata(DamageType.MAGIC, ability, playerItemStats, NAME), targetDamage, false, true, false);

		double areaDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_AREA_DAMAGE, baseDamage * AOE_DAMAGE_MULTIPLIER);
		if (thunder) {
			areaDamage *= 1 + ENHANCED_DAMAGE_MULTIPLIER;
		}
		double radius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, ELEMENTAL_ARROWS_RADIUS);

		if (isLevelTwo()) {
			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(enemy), radius);
			for (LivingEntity mob : hitbox.getHitMobs(enemy)) {
				effectAction.accept(mob);
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, ability, playerItemStats), areaDamage, true, true, false);
			}
		}
	}

	public double getLastDamage() {
		return mLastDamage;
	}

	public static boolean isElementalArrowDamage(DamageEvent event) {
		// Use the "boss spell name" as marker if it's the main projectile damage or AoE damage
		return (event.getAbility() == ClassAbility.ELEMENTAL_ARROWS_FIRE || event.getAbility() == ClassAbility.ELEMENTAL_ARROWS_ICE)
			       && event.getBossSpellName() != null;
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
			boolean thunderApplied = false;
			if (isEnhanced() && !isOnCooldown()) {
				projectile.setMetadata(THUNDER_ARROW_METAKEY, new FixedMetadataValue(mPlugin, 0));
				mCosmetic.thunderProjectile(mPlayer, projectile, mPlugin);
				thunderApplied = true;
				putOnCooldown();
			}

			if (mPlayer.isSneaking()) {
				projectile.setMetadata(ICE_ARROW_METAKEY, new FixedMetadataValue(mPlugin, 0));
				projectile.setFireTicks(0);
				if (!thunderApplied) {
					mCosmetic.iceProjectile(mPlayer, projectile, mPlugin);
				}
			} else {
				projectile.setMetadata(FIRE_ARROW_METAKEY, new FixedMetadataValue(mPlugin, 0));
				projectile.setFireTicks(ELEMENTAL_ARROWS_DURATION);
				if (!thunderApplied) {
					mCosmetic.fireProjectile(mPlayer, projectile, mPlugin);
				}
			}
		}
		return true;
	}
}
