package com.playmonumenta.plugins.abilities.warlock;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.CursedWoundCS;
import com.playmonumenta.plugins.effects.Bleed;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

public class CursedWound extends Ability {

	private static final int CURSED_WOUND_DOT_DAMAGE = 1;
	private static final int CURSED_WOUND_DOT_PERIOD = 20;
	private static final int CURSED_WOUND_DURATION = 6 * 20;
	private static final int CURSED_WOUND_RADIUS = 3;
	private static final double CURSED_WOUND_DAMAGE_1 = 0.05;
	private static final double CURSED_WOUND_DAMAGE_2 = 0.1;
	private static final int CURSED_WOUND_CAP = 3;
	private static final String DOT_EFFECT_NAME = "CursedWoundDamageOverTimeEffect";
	private static final double DAMAGE_PER_EFFECT_RATIO = 0.03;

	public static final String CHARM_DAMAGE = "Cursed Wound Damage Modifier";
	public static final String CHARM_RADIUS = "Cursed Wound Radius";
	public static final String CHARM_CAP = "Cursed Wound Ability Cap";
	public static final String CHARM_DOT = "Cursed Wound DoT";

	public static final AbilityInfo<CursedWound> INFO =
		new AbilityInfo<>(CursedWound.class, "Cursed Wound", CursedWound::new)
			.linkedSpell(ClassAbility.CURSED_WOUND)
			.scoreboardId("CursedWound")
			.shorthandName("CW")
			.actionBarColor(TextColor.color(217, 217, 217))
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Your attacks deal more damage for each ability on cooldown and apply damage over time.")
			.displayItem(Material.GOLDEN_SWORD);

	private final double mCursedWoundDamage;
	private final int mAbilityCap;
	private final double mDOTDamage;
	private final double mRadius;

	private @Nullable Collection<PotionEffect> mStoredPotionEffects;
	private @Nullable HashMap<String, Effect> mStoredCustomEffects;
	private int mStoredFireTicks;

	private final CursedWoundCS mCosmetic;

	public CursedWound(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCursedWoundDamage = (isLevelOne() ? CURSED_WOUND_DAMAGE_1 : CURSED_WOUND_DAMAGE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mAbilityCap = CURSED_WOUND_CAP + (int) CharmManager.getLevel(mPlayer, CHARM_CAP);
		mDOTDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT, CURSED_WOUND_DOT_DAMAGE);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, CURSED_WOUND_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CursedWoundCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			return false;
		}

		DamageType type = event.getType();
		if (type == DamageType.MELEE || type == DamageType.MELEE_ENCH) {
			World world = mPlayer.getWorld();

			if (isEnhanced() && type == DamageType.MELEE && mStoredPotionEffects != null && mStoredCustomEffects != null) {
				int debuffCount = mStoredPotionEffects.size() + mStoredCustomEffects.size();

				// do not double-count fire ticks and inferno
				if (mStoredFireTicks > 0 && !mStoredCustomEffects.containsKey(Inferno.INFERNO_EFFECT_NAME)) {
					debuffCount++;
				}

				double damage = event.getDamage() * DAMAGE_PER_EFFECT_RATIO * debuffCount;
				if (damage > 0) {
					Map<String, JsonObject> serializedEffects = new HashMap<>();
					mStoredCustomEffects.forEach((source, effect) -> serializedEffects.put(source, effect.serialize()));
					for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), mRadius)) {
						mStoredPotionEffects.forEach(mob::addPotionEffect);
						EntityUtils.applyFire(mPlugin, mStoredFireTicks, mob, mPlayer, null);
						serializedEffects.forEach((source, jsonEffect) -> {
							try {
								Effect deserializedEffect = EffectManager.getEffectFromJson(jsonEffect, mPlugin);
								if (deserializedEffect != null) {
									mPlugin.mEffectManager.addEffect(mob, source, deserializedEffect);
								}
							} catch (Exception e) {
								MMLog.warning("Caught exception deserializing effect in Cursed Wound:");
								e.printStackTrace();
							}
						});
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, true);
					}

					mStoredPotionEffects = null;
					mStoredFireTicks = 0;
					mStoredCustomEffects = null;


					mCosmetic.onReleaseStoredEffects(mPlayer, enemy, mRadius);
				}
			}

			mCosmetic.onAttack(mPlayer, enemy);

			int cooldowns = 0;
			for (Integer ability : mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId()).values()) {
				if (ability > 0) {
					cooldowns++;
				}
			}

			event.updateDamageWithMultiplier(1 + (Math.min(cooldowns, mAbilityCap) * mCursedWoundDamage));

			if (type == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer)) {
				mCosmetic.onCriticalAttack(world, mPlayer, enemy, cooldowns);
				ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
				for (LivingEntity mob : new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(enemy), CURSED_WOUND_RADIUS).getHitMobs()) {
					mCosmetic.onEffectApplication(mPlayer, mob);
					mPlugin.mEffectManager.addEffect(mob, DOT_EFFECT_NAME,
						new CustomDamageOverTime(CURSED_WOUND_DURATION, mDOTDamage,
							CURSED_WOUND_DOT_PERIOD, mPlayer, playerItemStats, mInfo.getLinkedSpell(), DamageType.MAGIC));
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (!ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			return;
		}
		World world = event.getEntity().getWorld();
		Location loc = mPlayer.getLocation();
		LivingEntity entity = event.getEntity();
		EntityDamageEvent entityDamageEvent = entity.getLastDamageCause();
		if (isEnhanced() && entityDamageEvent != null && entityDamageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {

			mStoredPotionEffects = entity.getActivePotionEffects();
			mStoredPotionEffects.removeIf(effect -> effect.getType().isInstant() || PotionUtils.hasPositiveEffects(effect.getType()));
			mStoredPotionEffects.removeIf(PotionUtils::isInfinite);

			mStoredFireTicks = entity.getFireTicks();

			mStoredCustomEffects = new HashMap<>();
			Map<String, Effect> customEffects = mPlugin.mEffectManager.getPriorityEffects(entity);
			for (Map.Entry<String, Effect> entry : customEffects.entrySet()) {
				String source = entry.getKey();
				Effect effect = entry.getValue();
				if (effect.isDebuff()) {
					if (effect instanceof Bleed bleedEffect && bleedEffect.hasHemorrhaged()) {
						// Prevent it from transferring a Bleed effect that has already hemorrhaged, as that is used to mark the mob and prevent it from receiving bleed stacks
						continue;
					}
					mStoredCustomEffects.put(source, effect);
				}
			}

			if (!mStoredPotionEffects.isEmpty() || mStoredFireTicks > 0 || !mStoredCustomEffects.isEmpty()) {
				mCosmetic.onStoreEffects(mPlayer, world, loc, entity);
			}
		}
	}

	private static Description<CursedWound> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Attacking a mob with a critical scythe attack passively afflicts it and all mobs within ")
			.add(a -> a.mRadius, CURSED_WOUND_RADIUS)
			.add(" blocks around it with ")
			.add(a -> a.mDOTDamage, CURSED_WOUND_DOT_DAMAGE)
			.add(" magic damage every second for ")
			.addDuration(CURSED_WOUND_DURATION)
			.add(" seconds. Your melee scythe attacks passively deal ")
			.addPercent(a -> a.mCursedWoundDamage, CURSED_WOUND_DAMAGE_1, false, Ability::isLevelOne)
			.add(" more damage per ability on cooldown, capped at ")
			.add(a -> a.mAbilityCap, CURSED_WOUND_CAP)
			.add(" abilities.");
	}

	private static Description<CursedWound> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your melee scythe attacks now passively deal ")
			.addPercent(a -> a.mCursedWoundDamage, CURSED_WOUND_DAMAGE_2, false, Ability::isLevelTwo)
			.add(" more damage per ability on cooldown instead.");
	}

	private static Description<CursedWound> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When you kill a mob with a melee scythe attack, all debuffs (excluding stuns and silences) on the mob get stored in your scythe. Then, on your next melee scythe attack, all mobs within ")
			.add(a -> a.mRadius, CURSED_WOUND_RADIUS)
			.add(" blocks of the target are inflicted with the effects stored in your scythe, as well as ")
			.addPercent(DAMAGE_PER_EFFECT_RATIO)
			.add(" of your melee attack's damage as magic damage per effect.");
	}
}
