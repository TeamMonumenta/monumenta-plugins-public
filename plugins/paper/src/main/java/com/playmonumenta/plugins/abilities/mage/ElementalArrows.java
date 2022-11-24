package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Stray;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;


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

	public static final String CHARM_DAMAGE = "Elemental Arrows Damage";
	public static final String CHARM_AREA_DAMAGE = "Elemental Arrows Area Damage";
	public static final String CHARM_DURATION = "Elemental Arrows Duration";
	public static final String CHARM_STUN_DURATION = "Elemental Arrows Stun Duration";
	public static final String CHARM_SLOWNESS = "Elemental Arrows Slowness Amplifier";
	public static final String CHARM_RANGE = "Elemental Arrows Range";
	public static final String CHARM_THUNDER_COOLDOWN = "Elemental Arrows Thunder Arrow Cooldown";

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
				String.format("Your next elemental arrow every %ss stuns non elite enemies hit for %ss and deals an extra %s%% bow damage to affected enemies.",
					ENHANCED_ARROW_COOLDOWN / 20,
					ENHANCED_ARROW_STUN_DURATION / 20,
					(int) (ENHANCED_DAMAGE_MULTIPLIER * 100)
				))
			.cooldown(0, 0, ENHANCED_ARROW_COOLDOWN, CHARM_THUNDER_COOLDOWN)
			.displayItem(new ItemStack(Material.SPECTRAL_ARROW, 1));

	private double mLastDamage = 0;

	public ElementalArrows(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!(event.getDamager() instanceof Projectile proj) || !EntityUtils.isAbilityTriggeringProjectile(proj, true)) {
			return false;
		}
		ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(proj);

		int duration = ELEMENTAL_ARROWS_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);

		if (proj.hasMetadata("ElementalArrowsFireArrow")) {
			applyArrowEffects(event, enemy, 1, ABILITY_FIRE, playerItemStats, Stray.class, (entity) -> {
				EntityUtils.applyFire(mPlugin, duration, entity, mPlayer, playerItemStats);
			});
		} else if (proj.hasMetadata("ElementalArrowsIceArrow")) {
			double slowAmplifier = SLOW_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
			applyArrowEffects(event, enemy, 1, ABILITY_ICE, playerItemStats, Blaze.class, (entity) -> {
				EntityUtils.applySlow(mPlugin, duration, slowAmplifier, entity);
			});
		} else if (proj.hasMetadata("ElementalArrowsThunderArrow")) {
			putOnCooldown();
			int stunDuration = ENHANCED_ARROW_STUN_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_STUN_DURATION);
			applyArrowEffects(event, enemy, 1 + ENHANCED_DAMAGE_MULTIPLIER, ABILITY, playerItemStats, null, (entity) -> {
				EntityUtils.applyStun(mPlugin, stunDuration, entity);
			});
		}
		return true; // creates new damage instances
	}

	private void applyArrowEffects(DamageEvent event, LivingEntity enemy, double multiplier, ClassAbility ability, ItemStatManager.PlayerItemStats playerItemStats, @Nullable Class<? extends Entity> bonusEntity, Consumer<LivingEntity> effectAction) {
		double damage = playerItemStats.getMainhandAddStats().get(ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat());
		mLastDamage = damage;

		double targetDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage) * multiplier;
		double areaDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_AREA_DAMAGE, damage * AOE_DAMAGE_MULTIPLIER) * multiplier;
		double radius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, ELEMENTAL_ARROWS_RADIUS);

		if (isLevelTwo()) {
			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(enemy), radius);
			for (LivingEntity mob : hitbox.getHitMobs(enemy)) {
				effectAction.accept(mob);
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, ability, playerItemStats), areaDamage, true, true, false);
			}
		}

		if (enemy.getClass() == bonusEntity) {
			targetDamage += ELEMENTAL_ARROWS_BONUS_DAMAGE;
		}

		effectAction.accept(enemy);
		event.setDamage(0);
		DamageUtils.damage(mPlayer, enemy, new DamageEvent.Metadata(DamageType.MAGIC, ability, playerItemStats), targetDamage, true, true, false);
	}

	public double getLastDamage() {
		return mLastDamage;
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
			if (isEnhanced() && !isOnCooldown()) {
				projectile.setMetadata("ElementalArrowsThunderArrow", new FixedMetadataValue(mPlugin, 0));
				mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.END_ROD);
			} else if (mPlayer.isSneaking()) {
				projectile.setMetadata("ElementalArrowsIceArrow", new FixedMetadataValue(mPlugin, 0));
				projectile.setFireTicks(0);
				mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.SNOW_SHOVEL);
			} else {
				projectile.setMetadata("ElementalArrowsFireArrow", new FixedMetadataValue(mPlugin, 0));
				projectile.setFireTicks(ELEMENTAL_ARROWS_DURATION);
				mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.FLAME);
			}
		}
		return true;
	}
}
