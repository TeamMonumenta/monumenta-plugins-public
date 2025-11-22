package com.playmonumenta.plugins.events;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
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
import org.jetbrains.annotations.Nullable;

public class DamageEvent extends Event implements Cancellable {

	public enum DamageType {
		MELEE(false, true, "Melee"),
		MELEE_SKILL(false, true, "Melee Skill"),
		MELEE_ENCH(false, true, "Melee Enchantment"),
		PROJECTILE(false, true, "Projectile"),
		PROJECTILE_SKILL(false, true, "Projectile Skill"),
		PROJECTILE_ENCH(false, true, "Projectile Enchantment"),
		MAGIC(false, true, "Magic"),
		THORNS(false, true, "Thorns"),
		BLAST(false, true, "Blast"),
		FIRE(true, true, "Fire"),
		FALL(true, true, "Fall"),
		AILMENT(false, false, "Ailment"),
		POISON(false, false, "Poison"),
		TRUE(false, false, "True"),
		OTHER(false, false, "Other");

		public static DamageType getType(DamageCause cause) {
			// List every cause for completeness
			return switch (cause) {
				case WORLD_BORDER, CONTACT, MELTING, DROWNING, STARVATION, LIGHTNING, FALLING_BLOCK, CUSTOM, DRYOUT,
				     FREEZE, CRAMMING, SONIC_BOOM, SUFFOCATION -> OTHER;
				case ENTITY_ATTACK -> MELEE;
				case ENTITY_SWEEP_ATTACK -> MELEE_ENCH;
				case PROJECTILE -> PROJECTILE;
				case DRAGON_BREATH, MAGIC -> MAGIC;
				case THORNS -> THORNS;
				case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> BLAST;
				case FIRE, FIRE_TICK, HOT_FLOOR, LAVA -> FIRE;
				case FALL, FLY_INTO_WALL -> FALL;
				case POISON -> POISON;
				case WITHER -> AILMENT;
				case VOID, KILL, SUICIDE -> TRUE;
				// we should log an error on default, this makes porting easier since any new damage types added will
				// automatically lead to a stacktrace
				default -> {
					MMLog.warning("Unknown/new damage type: " + cause);
					yield OTHER;
				}
			};
		}

		public static boolean is(DamageCause cause, DamageType type) {
			return getType(cause) == type;
		}

		private final boolean mIsEnvironmental;
		private final boolean mIsDefendable;
		private final String mDisplay;

		DamageType(boolean isEnvironmental, boolean isDefendable, String display) {
			mIsEnvironmental = isEnvironmental;
			mIsDefendable = isDefendable;
			mDisplay = display;
		}

		public boolean isEnvironmental() {
			return mIsEnvironmental;
		}

		public boolean isDefendable() {
			return mIsDefendable;
		}

		public String getDisplay() {
			return mDisplay;
		}

		public static EnumSet<DamageType> getEnumSet() {
			return EnumSet.copyOf(List.of(values()));
		}

		public static EnumSet<DamageType> getScalableDamageType() {
			EnumSet<DamageType> enumSet = getEnumSet();
			enumSet.removeAll(getUnscalableDamageType());
			return enumSet;
		}

		public static EnumSet<DamageType> getUnscalableDamageType() {
			return EnumSet.of(AILMENT, POISON, FALL, OTHER, TRUE);
		}

		public static EnumSet<DamageType> getAllMeleeTypes() {
			return EnumSet.of(MELEE, MELEE_ENCH, MELEE_SKILL);
		}

		public static EnumSet<DamageType> getAllProjectileTypes() {
			return EnumSet.of(PROJECTILE, PROJECTILE_SKILL, PROJECTILE_ENCH);
		}

		public static EnumSet<DamageType> getAllMagicTypes() {
			// Might create MAGIC_ENCH to handle Trivium properly in the future, not this PR though
			return EnumSet.of(MAGIC);
		}

		public static EnumSet<DamageType> getAllMeleeAndProjectileTypes() {
			EnumSet<DamageType> enumSet = getAllMeleeTypes();
			enumSet.addAll(getAllProjectileTypes());
			return enumSet;
		}

		public static EnumSet<DamageType> getAllProjectileAndMagicTypes() {
			EnumSet<DamageType> enumSet = getAllProjectileTypes();
			enumSet.addAll(getAllMagicTypes());
			return enumSet;
		}

		public static EnumSet<DamageType> getAllMeleeProjectileAndMagicTypes() {
			EnumSet<DamageType> enumSet = getAllMeleeAndProjectileTypes();
			enumSet.addAll(getAllMagicTypes());
			return enumSet;
		}
	}

	public static class Metadata {

		private DamageType mType;
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

		public DamageType getType() {
			return mType;
		}

		public void setType(DamageType type) {
			mType = type;
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
	private boolean mLifelineCancel;

	private final double mOriginalDamage;
	private double mDamageMultiplier = 1;
	private double mGearDamageMultiplier = 1;
	private double mDamageReductionMultiplier = 1;
	private double mFlatDamage;
	private double mUnmodifiableDamage = 0;
	private @Nullable Double mDamageCap = null;
	private boolean mIsCrit = false;
	private List<EffectsList.Effect> mEffects;

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
		mFlatDamage = event.getDamage();
		mEvent = event;
		mLifelineCancel = false;
		mEffects = new ArrayList<>();

		if (mDamager instanceof Projectile proj) {
			ProjectileSource source = proj.getShooter();
			if (source instanceof LivingEntity le) {
				mSource = le;
			} else {
				mSource = null;
			}
		} else if (mDamager instanceof EvokerFangs fangs) {
			mSource = fangs.getOwner();
			mMetadata.setType(DamageType.MAGIC);
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
	// Bukkit deprecates EntityDamageEvent.DamageModifier
	@SuppressWarnings("deprecation")
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

	public void setFlatDamage(double damage) {
		if (damage < 0) {
			Plugin.getInstance().getLogger().log(Level.FINE, "Negative damage dealt: " + damage, new Exception());
		}
		if (!Double.isFinite(damage)) {
			Plugin.getInstance().getLogger().log(Level.WARNING, "Non-finite damage dealt: " + damage, new Exception());
			damage = 0;
		}

		// Update flat damage, since we're setting a new base
		// In case something goes horribly wrong, log the stack trace when set to finest
		mFlatDamage = damage;
		MMLog.finest(() -> Arrays.toString(Thread.currentThread().getStackTrace()));

		recalculateDamage();
	}

	public void updateDamageWithMultiplier(double damageMultiplier) {
		if (damageMultiplier < 0) {
			Plugin.getInstance().getLogger().log(Level.FINE, "Negative damage multiplier: " + damageMultiplier, new Exception());
		}
		if (damageMultiplier > 1) {
			// Accumulate damage multiplier (Additively)
			mDamageMultiplier += (damageMultiplier - 1);
		} else {
			// Accumulate weakness / reduction multiplier (Multiplicatively)
			mDamageReductionMultiplier *= Math.max(0, damageMultiplier);
		}
		recalculateDamage();
	}

	public void updateGearDamageWithMultiplier(double damageGearMultiplier) {
		if (damageGearMultiplier < 0) {
			Plugin.getInstance().getLogger().log(Level.FINE, "Negative damage multiplier: " + damageGearMultiplier, new Exception());
		}
		// Accumulate damage multiplier
		mGearDamageMultiplier += (damageGearMultiplier - 1);
		recalculateDamage();
	}

	static final int DAMAGE_CAP = 1000000;
	static final int DAMAGE_WARN = 10000;
	private boolean mHasBeenWarned = false;
	private final UUID mEventIdentifier = UUID.randomUUID();

	private void recalculateDamage() {
		// Never set damage above 1000000 (arbitrary high amount) so that it doesn't go over the limit of what can actually be dealt
		double damage = Math.max(Math.min(mFlatDamage * mGearDamageMultiplier * mDamageMultiplier * mDamageReductionMultiplier * critModifier() + mUnmodifiableDamage, DAMAGE_CAP), 0);
		if (mDamageCap != null) {
			damage = Math.min(damage, mDamageCap);
		}
		// Log big warning because damage is too high
		if (damage >= DAMAGE_WARN) {
			damageCapWarn(damage);
		}
		if (mMetadata.mType == DamageType.POISON && mDamagee instanceof Player && mDamagee.getHealth() - damage <= 0) {
			mEvent.setDamage(Math.max(mDamagee.getHealth() - 1, 0));
			return;
		}
		mEvent.setDamage(damage);
	}

	private void damageCapWarn(double damage) {
		if (!(mSource instanceof Player player)) {
			return;
		}
		if (!mHasBeenWarned) {
			final var inventory = player.getInventory();
			// grab current charms
			final var charms = CharmManager.getInstance().getCharms(player, CharmManager.getInstance().mEnabledCharmType);
			final var charmNames = new ArrayList<>();
			if (charms != null && !charms.isEmpty()) {
				for (final var charm : charms) {
					charmNames.add(ItemUtils.getPlainName(charm));
				}
			}
			final var charmString = charmNames.isEmpty() ? "" : String.join(",", charmNames.toArray(new String[0]));
			String equipment;
			try {
				equipment = String.join(",", "mainhand=" + ItemUtils.getPlainName(inventory.getItemInMainHand()), "offhand=" + ItemUtils.getPlainName(inventory.getItemInOffHand()), "helmet=" + ItemUtils.getPlainName(inventory.getHelmet()), "chestplate=" + ItemUtils.getPlainName(inventory.getChestplate()), "leggings=" + ItemUtils.getPlainName(inventory.getLeggings()), "boots=" + ItemUtils.getPlainName(inventory.getBoots()), "charms=[" + charmString + "]");
			} catch (Exception ex) {
				equipment = "error";
			}
			final String string = String.join(" ", "Player dealt damage higher than " + DAMAGE_WARN, "[" + String.join(",", "player=" + player.getName(), "damage=" + damage, "originalDamage=" + mOriginalDamage, "flatDamage=" + mFlatDamage, "damageEventId=" + mEventIdentifier, "equipment=[" + equipment + "]") + "]");
			// now craft the stacktrace
			MMLog.severe(() -> string + parseStackTracesFromMonumentaPlugin());
			AuditListener.logPlayer(string);
			return;
		}
		MMLog.severe(() -> "Player: " + player.getName() + " excceded damage cap! [damageEventId=" + mEventIdentifier + ",damage=" + damage + "]");
	}

	private static final Set<String> IGNORED_CLAZZ = Set.of(
		"DamageEvent",
		"DamageListener",
		"DamageUtils"
	);

	private static String parseStackTracesFromMonumentaPlugin() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		var s = "\n";
		for (StackTraceElement stackTraceElement : stackTrace) {
			String className = stackTraceElement.getClassName();
			if (!className.contains("com.playmonumenta")) {
				continue;
			}

			var founcIgnored = false;
			for (var ignoredClass : IGNORED_CLAZZ) {
				if (className.contains(ignoredClass)) {
					founcIgnored = true;
					break;
				}
			}
			if (founcIgnored) {
				continue;
			}

			s = s + stackTraceElement.toString() + "\n";
		}
		return s;
	}

	public double getFlatDamage() {
		return mFlatDamage;
	}

	public double getDamageMultiplier() {
		return mDamageMultiplier;
	}

	public double getGearDamageMultiplier() {
		return mGearDamageMultiplier;
	}

	public double getWeaknessMultiplier() {
		return mDamageReductionMultiplier;
	}

	public void setIsCrit(boolean crit) {
		mIsCrit = crit;
		recalculateDamage();
	}

	private double critModifier() {
		return mIsCrit ? 1.5 : 1;
	}

	// Will override an existing cap!
	public void setDamageCap(@Nullable Double cap) {
		mDamageCap = cap;
		recalculateDamage();
	}

	public @Nullable Double getDamageCap() {
		return mDamageCap;
	}

	public void addUnmodifiableDamage(double damage) {
		mUnmodifiableDamage += damage;
	}

	public DamageType getType() {
		return mMetadata.mType;
	}

	public void setType(DamageType newType) {
		mMetadata.mType = newType;
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

	public void setLifelineCancel(boolean lifelineCancel) {
		mLifelineCancel = lifelineCancel;
	}

	public boolean isLifelineCancel() {
		return mLifelineCancel;
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
	// Bukkit deprecates EntityDamageEvent.DamageModifier
	@SuppressWarnings("deprecation")
	public boolean isBlockedByShield() {
		return mEvent.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
	}

	// Stores the potion effects that were applied by a bosstag/spell
	public void setEffects(List<EffectsList.Effect> effectList) {
		mEffects = effectList;
	}

	public List<EffectsList.Effect> getEffects() {
		return mEffects;
	}

	/**
	 * Returns true if the damage cause is {@link DamageCause#VOID} or {@link DamageCause#KILL}, false if not
	 */
	public boolean isUnblockable() {
		return getCause() == DamageCause.VOID || getCause() == DamageCause.KILL;
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
