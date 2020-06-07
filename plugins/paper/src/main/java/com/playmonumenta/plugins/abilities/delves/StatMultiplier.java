package com.playmonumenta.plugins.abilities.delves;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.BossUtils.BossAbilityDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

public class StatMultiplier extends Ability {

	private static final String MESSAGED_PLAYER_TICK_METAKEY = "StatMultiplierMessagedPlayerTickMetakey";
	private static final String SPEED_MODIFIER_NAME = "DelvesSpeedModifier";
	private static final int PROPERTIES_APPLICATION_RADIUS = 24;

	// Overall tracker for mobs that have had properties applied
	private static final Set<LivingEntity> PROPERTIED_MOBS = new HashSet<>();
	// Cleaner, essentially final but must be initialized after we know the plugin is started
	private static BukkitRunnable PROPERTIED_MOBS_CLEANER;

	private final double mDamageDealtMultiplier;
	private final double mDamageTakenMultiplier;
	private final double mAbilityDamageTakenMultiplier;

	private final double mMobSpeedMultiplier;

	private final String[] mMobAbilityPool;
	private final double mMobAbilityChance;

	public StatMultiplier(Plugin plugin, World world, Player player, String message,
			double damageDealtMultiplier, double damageTakenMultiplier, double abilityDamageTakenMultiplier) {
		this(plugin, world, player, message,
				damageDealtMultiplier, damageTakenMultiplier, abilityDamageTakenMultiplier,
				1, null, 0);
	}

	public StatMultiplier(Plugin plugin, World world, Player player, String message,
			double damageDealtMultiplier, double damageTakenMultiplier, double abilityDamageTakenMultiplier,
			double mobSpeedMultiplier) {
		this(plugin, world, player, message,
				damageDealtMultiplier, damageTakenMultiplier, abilityDamageTakenMultiplier, mobSpeedMultiplier,
				null, 0);
	}

	public StatMultiplier(Plugin plugin, World world, Player player, String message,
			double damageDealtMultiplier, double damageTakenMultiplier, double abilityDamageTakenMultiplier,
			String[] mobAbilitiesPool, double mobAbilitiesChance) {
		this(plugin, world, player, message,
				damageDealtMultiplier, damageTakenMultiplier, abilityDamageTakenMultiplier,
				1, mobAbilitiesPool, mobAbilitiesChance);
	}

	public StatMultiplier(Plugin plugin, World world, Player player, String message,
			double damageDealtMultiplier, double damageTakenMultiplier, double abilityDamageTakenMultiplier,
			double mobSpeedMultiplier, String[] mobAbilityPool, double mobAbilityChance) {
		super(plugin, world, player, null);
		mInfo.mIgnoreTriggerCap = true;

		mDamageDealtMultiplier = damageDealtMultiplier;
		mDamageTakenMultiplier = damageTakenMultiplier;
		// Regular mob damage event also intercepts ability damage, so don't double dip
		mAbilityDamageTakenMultiplier = abilityDamageTakenMultiplier / damageTakenMultiplier;

		mMobSpeedMultiplier = mobSpeedMultiplier;

		mMobAbilityPool = mobAbilityPool;
		mMobAbilityChance = mobAbilityChance;

		initializeRunnable();

		// Class may be refreshed in multiple places, only message once
		if (player != null && MetadataUtils.checkOnceThisTick(plugin, player, MESSAGED_PLAYER_TICK_METAKEY)) {
			MessagingUtils.sendRawMessage(player, message);
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Silverfish) {
			return true;
		}

		if (event.getCause() != DamageCause.CUSTOM) {
			event.setDamage(event.getDamage() * mDamageDealtMultiplier);
		}
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity le, EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Silverfish) {
			return true;
		}

		event.setDamage(event.getDamage() * mDamageDealtMultiplier);
		return true;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (event.getDamaged() instanceof Silverfish) {
			return;
		}

		event.setDamage(event.getDamage() * mDamageDealtMultiplier);
	}

	@Override
	public void playerDealtUnregisteredCustomDamageEvent(CustomDamageEvent event) {
		if (event.getDamaged() instanceof Silverfish) {
			return;
		}

		event.setDamage(event.getDamage() * mDamageDealtMultiplier);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, mDamageTakenMultiplier));
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, mDamageTakenMultiplier));
		return true;
	}

	@Override
	public void playerDamagedByBossEvent(BossAbilityDamageEvent event) {
		AttributeInstance armor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
		AttributeInstance toughness = mPlayer.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);
		double armorValue = armor != null ? armor.getValue() : 0;
		double toughnessValue = toughness != null ? toughness.getValue() : 0;
		event.setDamage(EntityUtils.getDamageApproximation(armorValue, toughnessValue, event.getDamage(), mAbilityDamageTakenMultiplier));
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && (mMobSpeedMultiplier != 1 || mMobAbilityChance != 0)) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), PROPERTIES_APPLICATION_RADIUS)) {
				if (!PROPERTIED_MOBS.contains(mob)) {
					PROPERTIED_MOBS.add(mob);

					// Additional check in case the plugin stopped and cleared the faster internal tracking
					boolean hasProperties = false;
					AttributeInstance speed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
					if (speed != null) {
						for (AttributeModifier mod : speed.getModifiers()) {
							if (mod != null && mod.getName().equals(SPEED_MODIFIER_NAME)) {
								hasProperties = true;
								break;
							}
						}

						if (!hasProperties) {
							// Speed
							AttributeModifier mod = new AttributeModifier(SPEED_MODIFIER_NAME,
								mMobSpeedMultiplier - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
							speed.addModifier(mod);

							// Abilities
							if (FastUtils.RANDOM.nextDouble() < mMobAbilityChance) {
								Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
									"bossfight " + mob.getUniqueId() + " boss_blastresist");
								Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
									"bossfight " + mob.getUniqueId() + mMobAbilityPool[FastUtils.RANDOM.nextInt(mMobAbilityPool.length)]);
							}
						}
					}
				}
			}
		}
	}

	private void initializeRunnable() {
		if (PROPERTIED_MOBS_CLEANER == null || PROPERTIED_MOBS_CLEANER.isCancelled()) {
			PROPERTIED_MOBS_CLEANER = new BukkitRunnable() {
				@Override
				public void run() {
					PROPERTIED_MOBS.removeIf(mob -> mob.isDead() || !mob.isValid());
				}
			};
			PROPERTIED_MOBS_CLEANER.runTaskTimer(mPlugin, 0, 20 * 10);
		}
	}

}
