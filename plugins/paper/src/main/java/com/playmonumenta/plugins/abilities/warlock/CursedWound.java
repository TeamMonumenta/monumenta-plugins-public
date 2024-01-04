package com.playmonumenta.plugins.abilities.warlock;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.CursedWoundCS;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
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
			.descriptions(
				"Attacking an enemy with a critical scythe attack passively afflicts it and all enemies in a 3 block radius around it with 1 damage every second for 6s. " +
					"Your melee attacks passively deal 5% more damage per ability on cooldown, capped at 3 abilities.",
				"Your melee attacks passively deal 10% more damage per ability on cooldown instead.",
				"When you kill a mob with a melee scythe attack, all debuffs on the mob get stored in your scythe. " +
					"Then, on your next melee scythe attack, all mobs within 3 blocks of the target are inflicted with the effects stored in your scythe, " +
					"as well as 3% of your melee attack's damage as magic damage per effect."
				)
			.simpleDescription("Your attacks deal more damage for each ability on cooldown and apply damage over time.")
			.displayItem(Material.GOLDEN_SWORD);

	private final double mCursedWoundDamage;
	private @Nullable Collection<PotionEffect> mStoredPotionEffects;
	private @Nullable HashMap<String, Effect> mStoredCustomEffects;

	private final CursedWoundCS mCosmetic;

	public CursedWound(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCursedWoundDamage = CharmManager.getLevel(player, CHARM_CAP) + (isLevelOne() ? CURSED_WOUND_DAMAGE_1 : CURSED_WOUND_DAMAGE_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CursedWoundCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			World world = mPlayer.getWorld();

			if (isEnhanced() && mStoredPotionEffects != null && mStoredCustomEffects != null) {
				double damage = event.getDamage() * DAMAGE_PER_EFFECT_RATIO * (mStoredPotionEffects.size() + mStoredCustomEffects.size());
				double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, CURSED_WOUND_RADIUS);
				if (damage > 0) {
					Map<String, JsonObject> serializedEffects = new HashMap<>();
					mStoredCustomEffects.forEach((source, effect) -> serializedEffects.put(source, effect.serialize()));
					for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), radius)) {
						mStoredPotionEffects.forEach(mob::addPotionEffect);
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
					mStoredCustomEffects = null;

					mCosmetic.onReleaseStoredEffects(mPlayer, world, mPlayer.getLocation(), enemy, radius);
				}
			}

			mCosmetic.onAttack(mPlayer, enemy);

			int cooldowns = 0;
			for (Integer ability : mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId())) {
				if (ability > 0) {
					cooldowns++;
				}
			}

			event.setDamage(event.getDamage() * (1 + (Math.min(cooldowns, CURSED_WOUND_CAP + CharmManager.getLevel(mPlayer, CHARM_CAP)) * (mCursedWoundDamage + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE)))));

			if (PlayerUtils.isFallingAttack(mPlayer)) {
				mCosmetic.onCriticalAttack(world, mPlayer.getLocation());
				ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
				for (LivingEntity mob : new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(enemy), CURSED_WOUND_RADIUS).getHitMobs()) {
					mCosmetic.onEffectApplication(mPlayer, mob);
					mPlugin.mEffectManager.addEffect(mob, DOT_EFFECT_NAME,
						new CustomDamageOverTime(CURSED_WOUND_DURATION, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT, CURSED_WOUND_DOT_DAMAGE),
							CURSED_WOUND_DOT_PERIOD, mPlayer, playerItemStats, mInfo.getLinkedSpell(), DamageType.AILMENT));
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

			mStoredCustomEffects = new HashMap<>();
			HashMap<String, Effect> customEffects = mPlugin.mEffectManager.getPriorityEffects(entity);
			for (Map.Entry<String, Effect> e : customEffects.entrySet()) {
				String source = e.getKey();
				Effect effect = e.getValue();
				if (effect.isDebuff()) {
					mStoredCustomEffects.put(source, effect);
				}
			}

			if (!mStoredPotionEffects.isEmpty() || !mStoredCustomEffects.isEmpty()) {
				mCosmetic.onStoreEffects(mPlayer, world, loc, entity);
			}
		}
	}

}
