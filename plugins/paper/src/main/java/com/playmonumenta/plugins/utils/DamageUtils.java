package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enchantments.Retaliation;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.listeners.StasisListener;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class DamageUtils {

	/**
	 * To be used by {@link com.playmonumenta.plugins.listeners.DamageListener} to fill this info into the created {@link DamageEvent} on custom damage.
	 */
	public static @Nullable DamageEvent.Metadata nextEventMetadata = null;

	public static double getDamageMultiplier(double armor, double agility, double epf, boolean environmental) {
		double ar = Math.max(0, armor);
		double ag = Math.max(0, agility);
		double defense = ar + ag == 0 ? 0 : ar + ag - 0.5 * ar * ag / (ar + ag);
		return environmental ? Math.pow(0.96, (defense / 2) + epf) : Math.pow(0.96, defense + epf);
	}

	/**
	 * Checks if an entity is immune to all damage. Current causes of damage immunity are:
	 * <ul>
	 *     <li>Being invulnerable according to {@link LivingEntity#isInvulnerable()}
	 *     <li>Having resistance 5+
	 *     <li>Having a custom damage resistance effect of 100% (or more) to the given type of damage
	 *     <li>Being in stasis
	 *     <li>Being in creative or spectator mode
	 * </ul>
	 *
	 * @param entity     The entity to check
	 * @param damageType The damage type to check for, or null to check for any damage type (excluding true damage which bypasses some forms of invulnerability)
	 */
	public static boolean isImmuneToDamage(LivingEntity entity, @Nullable DamageType damageType) {
		if (entity.isInvulnerable()) {
			return true;
		}
		PotionEffect resistance = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
		if (resistance != null && resistance.getAmplifier() >= 4) {
			return true;
		}
		if (entity instanceof Player player && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)) {
			return true;
		}
		if (damageType != DamageType.TRUE) {
			List<Effect> effects = Plugin.getInstance().mEffectManager.getEffects(entity);
			if (effects != null && effects.stream().anyMatch(effect -> effect instanceof PercentDamageReceived dre
				&& dre.getMagnitude() <= -1 && (dre.getAffectedDamageTypes() == null || dre.getAffectedDamageTypes().contains(damageType)))) {
				return true;
			}
		}
		return StasisListener.isInStasis(entity);
	}

	/**
	 * Deal True damage to a LivingEntity based on its maximum health as determined by
	 * <code>EntityUtils#getMaxHealth</code>. True damage cannot be negated by damage resistances or blocked by
	 * player invulnerability ticks (iframes) but this method does allow it to be blocked by a shield using the vanilla
	 * checks for backwards compatibility with <code>BossUtils#bossDamagePercent</code>. Refer to the
	 * <code>VersionAdapter</code> for more information
	 *
	 * @param damager        LivingEntity dealing damage, pass null if not applicable
	 * @param damagee        LivingEntity receiving damage
	 * @param percentHealth  How much of the entity's health should be damaged
	 * @param causeKnockback Whether the damage should cause knockback
	 * @param blockable      Whether the damage can be blocked with a shield
	 * @param cause          Used for player death messages
	 * @param isBossAbility  Whether this damage originated from a Boss. Only used by the Retaliation enchantment
	 * @param effects        Statuses applied to the player with this attack. Only used by the Retaliation enchantment
	 */
	public static void damagePercentHealth(@Nullable final LivingEntity damager, final LivingEntity damagee,
	                                       final double percentHealth, final boolean causeKnockback,
	                                       final boolean blockable, final String cause, final boolean isBossAbility,
	                                       final List<EffectsList.Effect> effects) {
		final double damageToTake = percentHealth * EntityUtils.getMaxHealth(damagee);

		// TODO: Retaliation should not be in this part of the damage pipeline. DamageUtils shouldn't need to know about
		//  effects as this reduces the modularity/separation of concerns for the two systems. Move to DamageListener
		if (damagee instanceof final Player player &&
			ItemStatUtils.hasEnchantment(player.getInventory().getItemInOffHand(), EnchantmentType.RETALIATION)) {
			new Retaliation().startEffect(player, effects, damager, isBossAbility);
		}

		damage(damager, damagee, new DamageEvent.Metadata(DamageType.TRUE, null, null, cause),
			damageToTake, true, causeKnockback, blockable);
	}

	/**
	 * Deals damage to a LivingEntity.
	 *
	 * @param damager LivingEntity dealing damage, pass null if not applicable
	 * @param damagee LivingEntity receiving damage
	 * @param type    DamageType the damage counts as
	 * @param amount  amount of damage to be dealt
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double amount) {
		damage(damager, damagee, type, amount, null, false);
	}

	/**
	 * Deals damage to a LivingEntity.
	 *
	 * @param damager       LivingEntity dealing damage, pass null if not applicable
	 * @param damagee       LivingEntity receiving damage
	 * @param type          DamageType the damage counts as
	 * @param amount        amount of damage to be dealt
	 * @param ability       ClassAbility causing the damage, pass null if not applicable
	 * @param bypassIFrames whether the damage should bypass IFrames
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double amount,
	                          @Nullable ClassAbility ability, boolean bypassIFrames) {
		damage(damager, damagee, type, amount, ability, bypassIFrames, false);
	}

	/**
	 * Deals damage to a LivingEntity.
	 *
	 * @param damager        LivingEntity dealing damage, pass null if not applicable
	 * @param damagee        LivingEntity receiving damage
	 * @param type           DamageType the damage counts as
	 * @param amount         amount of damage to be dealt
	 * @param ability        ClassAbility causing the damage, pass null if not applicable
	 * @param bypassIFrames  whether the damage should bypass IFrames
	 * @param causeKnockback whether the damage should cause knockback
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double amount,
	                          @Nullable ClassAbility ability, boolean bypassIFrames, boolean causeKnockback) {
		damage(damager, damagee, type, amount, ability, bypassIFrames, causeKnockback, null);
	}

	/**
	 * Deals damage to a LivingEntity.
	 *
	 * @param damager        LivingEntity dealing damage, pass null if not applicable
	 * @param damagee        LivingEntity receiving damage
	 * @param type           DamageType the damage counts as
	 * @param amount         amount of damage to be dealt
	 * @param ability        ClassAbility causing the damage, pass null if not applicable
	 * @param bypassIFrames  whether the damage should bypass IFrames
	 * @param causeKnockback whether the damage should cause knockback
	 * @param bossCause      string to pass for boss death messages
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double amount,
	                          @Nullable ClassAbility ability, boolean bypassIFrames, boolean causeKnockback,
	                          @Nullable String bossCause) {
		damage(damager, damagee, new DamageEvent.Metadata(type, ability, null, bossCause), amount,
			bypassIFrames, causeKnockback, false);
	}

	/**
	 * Deals damage to a LivingEntity.
	 * <br>
	 * Use this method when additional metadata needs to be added to the DamageEvent before calling it.
	 *
	 * @param damager        LivingEntity dealing damage, pass null if not applicable
	 * @param damagee        LivingEntity receiving damage
	 * @param metadata       Metadata for the event
	 * @param amount         amount of damage to be dealt
	 * @param bypassIFrames  whether the damage should bypass IFrames
	 * @param causeKnockback whether the damage should cause knockback
	 * @param blockable      Whether the damage can be blocked with a shield
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageEvent.Metadata metadata,
	                          double amount, boolean bypassIFrames, boolean causeKnockback, boolean blockable) {
		if (!damagee.isValid() || isImmuneToDamage(damagee, metadata.getType())) {
			return;
		}

		if (amount <= 0.0) {
			MMLog.severe(() -> "[DamageUtils] Something tried to apply negative damage! No damage will be applied." +
				"damagee: " + damagee.getName() + " metadata: " + metadata);
			return;
		}

		int originalAttackCooldown = 0;
		if (damager != null) {
			originalAttackCooldown = NmsUtils.getVersionAdapter().getAttackCooldown(damager);
		}

		int originalIFrames = damagee.getNoDamageTicks();
		double originalLastDamage = damagee.getLastDamage();
		Vector originalVelocity = damagee.getVelocity();
		if (bypassIFrames) {
			damagee.setNoDamageTicks(0);
		}

		Plugin plugin = Plugin.getInstance();
		double amountFinal = amount;
		if (damager instanceof Player) {
			amountFinal = amount * (1 - 0.25 * plugin.mItemStatManager.getEnchantmentLevel((Player) damager, EnchantmentType.WORLDLY_PROTECTION));
		}

		nextEventMetadata = metadata;
		try {
			NmsUtils.getVersionAdapter().customDamageEntity(damager, damagee, amountFinal, blockable, metadata.getBossSpellName());
		} finally {
			nextEventMetadata = null;

			if (bypassIFrames) {
				damagee.setNoDamageTicks(originalIFrames);
				damagee.setLastDamage(originalLastDamage);
			}

			if (!causeKnockback) {
				damagee.setVelocity(originalVelocity);
			}

			if (damager != null) {
				NmsUtils.getVersionAdapter().setAttackCooldown(damager, originalAttackCooldown);
			}
		}
	}

	public static @Nullable ItemStatManager.PlayerItemStats getDamagingPlayerItemStats(DamageEvent event) {
		if (event.getDamager() instanceof Projectile projectile) {
			ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(projectile);
			if (playerItemStats != null) {
				return playerItemStats;
			}
		}
		if (event.getSource() instanceof Player player) {
			return Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
		}
		return null;
	}
}
