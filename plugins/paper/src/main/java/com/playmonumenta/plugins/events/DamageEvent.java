package com.playmonumenta.plugins.events;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;

public class DamageEvent extends Event implements Cancellable {

	public enum DamageType {
		MELEE(false, true),
		MELEE_SKILL(false, true),
		MELEE_ENCH(false, true),
		PROJECTILE(false, true),
		PROJECTILE_SKILL(false, true),
		MAGIC(false, true),
		THORNS(false, true),
		BLAST(false, true),
		FIRE(true, true),
		FALL(true, true),
		AILMENT(false, false),
		POISON(false, false),
		OTHER(false, false);

		public static DamageType getType(DamageCause cause) {
			// List every cause for completeness
			switch (cause) {
			case CONTACT:
			case ENTITY_ATTACK:
			case FALLING_BLOCK:
				return DamageType.MELEE;
			case ENTITY_SWEEP_ATTACK:
				return DamageType.MELEE_ENCH;
			case PROJECTILE:
				return DamageType.PROJECTILE;
			case DRAGON_BREATH:
			case MAGIC:
				return DamageType.MAGIC;
			case THORNS:
				return DamageType.THORNS;
			case BLOCK_EXPLOSION:
			case ENTITY_EXPLOSION:
				return DamageType.BLAST;
			case FIRE:
			case FIRE_TICK:
			case HOT_FLOOR:
			case LAVA:
				return DamageType.FIRE;
			case FALL:
			case FLY_INTO_WALL:
				return DamageType.FALL;
			case POISON:
				return DamageType.POISON;
			case WITHER:
				return DamageType.AILMENT;
			case CRAMMING:
			case CUSTOM:
			case DROWNING:
			case DRYOUT:
			case LIGHTNING:
			case MELTING:
			case STARVATION:
			case SUFFOCATION:
			case SUICIDE:
			case VOID:
				return DamageType.OTHER;
			default:
				return DamageType.OTHER;
			}
		}

		private final boolean mIsEnvironmental;
		private final boolean mIsDefendable;

		DamageType(boolean isEnvironmental, boolean isDefendable) {
			mIsEnvironmental = isEnvironmental;
			mIsDefendable = isDefendable;
		}

		public boolean isEnvironmental() {
			return mIsEnvironmental;
		}

		public boolean isDefendable() {
			return mIsDefendable;
		}

		public static ParseResult<DamageType> fromReader(StringReader reader, String hoverDescription) {
			DamageType type = reader.readEnum(DamageType.values());
			if (type == null) {
				List<Tooltip<String>> suggArgs = new ArrayList<>(DamageType.values().length);
				String soFar = reader.readSoFar();
				for (DamageType valid : DamageType.values()) {
					suggArgs.add(Tooltip.of(soFar + valid.name(), hoverDescription));
				}
				return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
			}

			return ParseResult.of(type);
		}
	}

	public static class Metadata {

		private final DamageType mType;
		private final @Nullable ClassAbility mAbility;
		private final @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;
		private final @Nullable String mBossSpellName;

		public Metadata(DamageType type, @Nullable ClassAbility ability) {
			this(type, ability, null);
		}

		public Metadata(DamageType type, @Nullable ClassAbility ability, @Nullable ItemStatManager.PlayerItemStats playerItemStats) {
			this(type, ability, playerItemStats, null);
		}

		public Metadata(DamageType type, @Nullable ClassAbility ability, @Nullable ItemStatManager.PlayerItemStats playerItemStats, @Nullable String bossSpellName) {
			if (type == null) {
				mType = DamageType.OTHER;
				Plugin.getInstance().getLogger().log(Level.WARNING, "Attempted to construct DamageEvent with null DamageType");
			} else {
				mType = type;
			}
			mAbility = ability;
			mPlayerItemStats = playerItemStats;
			mBossSpellName = bossSpellName;
		}

		public @Nullable String getBossSpellName() {
			return mBossSpellName;
		}
	}

	private final LivingEntity mDamagee;
	private final @Nullable Entity mDamager;
	private final @Nullable LivingEntity mSource;
	private final EntityDamageEvent mEvent;

	private final Metadata mMetadata;

	private final double mOriginalDamage;

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee) {
		this(event, damagee, DamageType.getType(event.getCause()));
	}

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee, DamageType type) {
		this(event, damagee, type, null);
	}

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee, DamageType type, @Nullable ClassAbility ability) {
		this(event, damagee, new Metadata(type, ability));
	}

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee, Metadata metadata) {
		mDamagee = damagee;
		mDamager = event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent ? entityDamageByEntityEvent.getDamager() : null;
		mMetadata = metadata;
		mOriginalDamage = event.getDamage();
		mEvent = event;

		if (mDamager instanceof Projectile proj) {
			ProjectileSource source = proj.getShooter();
			if (source instanceof LivingEntity le) {
				mSource = le;
			} else {
				mSource = null;
			}
		} else if (mDamager instanceof EvokerFangs fangs) {
			mSource = fangs.getOwner();
		} else if (mDamager instanceof LivingEntity le) {
			mSource = le;
		} else {
			mSource = null;
		}
	}

	public double getDamage() {
		return mEvent.getDamage();
	}

	/**
	 * Gets the final damage that will be dealt by this damage event, if the damage were to happen right now.
	 * To make sure to get the actual final damage, set the priority the ability or attribute using this sufficiently high.
	 * This also means that this method should not be used by low-priority handlers.
	 *
	 * @param includeAbsorption Whether to deduct existing absorption from the result (same behaviour as {@link EntityDamageEvent#getFinalDamage()}),
	 *                          useful to check if an attack would be lethal
	 * @return The final damage that will be dealt
	 */
	public double getFinalDamage(boolean includeAbsorption) {
		if (includeAbsorption) {
			return Math.max(0, mEvent.getFinalDamage());
		} else {
			return Math.max(0, mEvent.getFinalDamage() - mEvent.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION));
		}
	}

	public double getOriginalDamage() {
		return mOriginalDamage;
	}

	public void setDamage(double damage) {
		if (damage < 0) {
			Plugin.getInstance().getLogger().log(Level.INFO, "Negative damage dealt: " + damage, new Exception());
		}

		// Never set damage above 1000000 (arbitrary high amount) so that it doesn't go over the limit of what can actually be dealt
		damage = Math.max(Math.min(damage, 1000000), 0);

		if (mMetadata.mType == DamageType.POISON && mDamagee instanceof Player && mDamagee.getHealth() - damage <= 0) {
			mEvent.setDamage(Math.max(mDamagee.getHealth() - 1, 0));
			return;
		}
		mEvent.setDamage(damage);
	}

	public DamageType getType() {
		return mMetadata.mType;
	}

	public @Nullable ClassAbility getAbility() {
		return mMetadata.mAbility;
	}

	public @Nullable String getBossSpellName() {
		return mMetadata.mBossSpellName;
	}

	public LivingEntity getDamagee() {
		return mDamagee;
	}

	public @Nullable Entity getDamager() {
		return mDamager;
	}

	public @Nullable LivingEntity getSource() {
		return mSource;
	}

	@Override
	public boolean isCancelled() {
		return mEvent.isCancelled();
	}

	@Override
	public void setCancelled(boolean cancelled) {
		mEvent.setCancelled(cancelled);
	}

	public @Nullable ItemStatManager.PlayerItemStats getPlayerItemStats() {
		return mMetadata.mPlayerItemStats;
	}

	public EntityDamageEvent getEvent() {
		return mEvent;
	}

	/**
	 * Use getType() in most scenarios - this is just if differentiation is needed within the type.
	 * This will return {@link DamageCause#CUSTOM} for any custom damage dealt (e.g. by abilities).
	 *
	 * @return The original damage cause of the {@link EntityDamageEvent}
	 */
	public DamageCause getCause() {
		return mEvent.getCause();
	}

	/**
	 * Returns whether the event deals 0 damage (e.g. blocked by a shield, iframes, resistance, etc.)
	 */
	public boolean isBlocked() {
		return isBlockedByShield() || getFinalDamage(false) <= 0;
	}

	/**
	 * Returns whether the damage is blocked by a shield
	 */
	public boolean isBlockedByShield() {
		return mEvent.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
	}

	// Mandatory Event Methods
	private static final HandlerList HANDLERS = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

}
