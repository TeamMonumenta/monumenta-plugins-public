package com.playmonumenta.plugins.events;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.scriptedquests.Plugin;
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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class DamageEvent extends Event implements Cancellable {

	public enum DamageType {
		MELEE(false, true),
		MELEE_SKILL(false, true),
		MELEE_ENCH(false, true),
		PROJECTILE(false, true),
		PROJECTILE_SKILL(false, true),
		MAGIC(false, true),
		THORNS(false, true),
		BLAST(true, true),
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
	}

	private final LivingEntity mDamagee;
	private final @Nullable Entity mDamager;
	private final @Nullable LivingEntity mSource;
	private final DamageType mType;
	private final @Nullable ClassAbility mAbility;
	private final @Nullable EntityDamageEvent mEvent;

	// Additional non-generic metadata about the DamageEvent
	private final Map<String, Boolean> mBooleans = new HashMap<>();
	private final Map<String, Integer> mIntegers = new HashMap<>();
	private final Map<String, Double> mDoubles = new HashMap<>();
	private final Map<String, String> mStrings = new HashMap<>();

	private double mDamage;
	private boolean mCancelled = false;
	private boolean mIsDelayed = false;

	private FixedMetadataValue mPlayerItemStat;

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee) {
		this(event, damagee, DamageType.getType(event.getCause()));
	}

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee, DamageType type) {
		this(event, damagee, type, null);
	}

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee, DamageType type, @Nullable ClassAbility ability) {
		mDamagee = damagee;
		mDamager = event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent ? entityDamageByEntityEvent.getDamager() : null;
		mAbility = ability;
		mDamage = event.getDamage();
		mEvent = event;

		if (type == null) {
			mType = DamageType.OTHER;
			Plugin.getInstance().getLogger().log(Level.WARNING, "Attempted to construct DamageEvent with null DamageType");
		} else {
			mType = type;
		}

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

	public DamageEvent(@Nullable EntityDamageEvent event, LivingEntity damagee, @Nullable Entity damager, @Nullable LivingEntity source, DamageType type, @Nullable ClassAbility ability, double damage) {
		mDamagee = damagee;
		mDamager = damager;
		mSource = source;
		mType = type;
		mAbility = ability;
		mDamage = damage;
		mEvent = event;
	}

	public DamageEvent(LivingEntity damagee, Entity damager, @Nullable LivingEntity source, DamageType type, @Nullable ClassAbility ability, double damage) {
		mDamagee = damagee;
		mDamager = damager;
		mSource = source;
		mType = type;
		mAbility = ability;
		mDamage = damage;
		mEvent = null;
	}

	public double getDamage() {
		return mDamage;
	}

	public void setDamage(double damage) {
		// Never set damage above 1000000 (arbitrary high amount) so that it doesn't go over the limit of what can actually be dealt
		damage = Math.min(damage, 1000000);

		if (mEvent != null) {
			if (mType == DamageType.POISON && mDamagee instanceof Player && mDamagee.getHealth() - damage <= 0) {
				mEvent.setDamage(mDamagee.getHealth() - 1);
				mDamage = mDamagee.getHealth() - 1;
				return;
			}
			mEvent.setDamage(damage);
			mDamage = damage;
		} else {
			if (mType == DamageType.POISON && mDamagee instanceof Player && mDamagee.getHealth() - damage <= 0) {
				mDamage = mDamagee.getHealth() - 1;
				return;
			}
			mDamage = damage;
		}
	}

	public DamageType getType() {
		return mType;
	}

	public @Nullable ClassAbility getAbility() {
		return mAbility;
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

	public Boolean getBoolean(String key) {
		return mBooleans.get(key);
	}

	public Integer getInteger(String key) {
		return mIntegers.get(key);
	}

	public Double getDouble(String key) {
		return mDoubles.get(key);
	}

	public @Nullable String getString(String key) {
		return mStrings.get(key);
	}

	public Boolean removeBoolean(String key) {
		return mBooleans.remove(key);
	}

	public Integer removeInteger(String key) {
		return mIntegers.remove(key);
	}

	public Double removeDouble(String key) {
		return mDoubles.remove(key);
	}

	public @Nullable String removeString(String key) {
		return mStrings.remove(key);
	}

	public void setBoolean(String key, boolean value) {
		mBooleans.put(key, value);
	}


	public void setInteger(String key, int value) {
		mIntegers.put(key, value);
	}

	public void setDouble(String key, double value) {
		mDoubles.put(key, value);
	}

	public void setString(String key, String value) {
		mStrings.put(key, value);
	}

	@Override
	public boolean isCancelled() {
		return mCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		mCancelled = cancelled;
		if (mEvent != null) {
			mEvent.setCancelled(cancelled);
		}
	}

	public void setDelayed(boolean delayed) {
		mIsDelayed = delayed;
	}

	public boolean isDelayed() {
		return mIsDelayed;
	}

	public void setPlayerItemStat(FixedMetadataValue playerItemStat) {
		mPlayerItemStat = playerItemStat;
	}

	public FixedMetadataValue getPlayerItemStat() {
		return mPlayerItemStat;
	}

	public @Nullable EntityDamageEvent getEvent() {
		return mEvent;
	}

	// Use getType() in most scenarios - this is just if differentiation is needed within the type
	public @Nullable DamageCause getCause() {
		if (mEvent == null) {
			return null;
		}
		return mEvent.getCause();
	}

	public boolean isBlocked() {
		return (mEvent != null && mEvent.getFinalDamage() <= 0) || mDamage <= 0;
	}

	// Mandatory Event Methods (If you remove these, I'm 99% sure the event will break)
	private static final HandlerList HANDLERS = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

}
