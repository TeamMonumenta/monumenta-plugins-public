package com.playmonumenta.plugins.abilities.warlock;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
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
import com.playmonumenta.plugins.utils.MetadataUtils;
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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

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
	private static final String ATTACKED_THIS_TICK = "CursedWoundAttackedThisTick";

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

			if (isEnhanced()) {
				MetadataUtils.markThisTick(mPlugin, enemy, ATTACKED_THIS_TICK);
			}

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
							CURSED_WOUND_DOT_PERIOD, mPlayer, playerItemStats, ClassAbility.CURSED_WOUND_DOT, DamageType.MAGIC));
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
		if (isEnhanced() && MetadataUtils.happenedThisTick(entity, ATTACKED_THIS_TICK)) {

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
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Deal increased melee damage for each")
			.addLine("ability you have on cooldown.")
			.addLine()
			.addStat("Damage Boost: +%p1 (m) per ability")
			.statValues(stat(a -> a.mCursedWoundDamage, CURSED_WOUND_DAMAGE_1), stat(a -> a.mAbilityCap, CURSED_WOUND_CAP))
			.addStat("Max Abilities: %d")
			.statValues(stat(a -> a.mAbilityCap, CURSED_WOUND_CAP))
			.addLine()
			.addLine("Critical scythe attacks inflict the target")
			.addLine("and nearby mobs with damage over time.")
			.addLine()
			.addStat("Damage: %d (s) every %t for %t")
			.statValues(stat(a -> a.mDOTDamage, CURSED_WOUND_DOT_DAMAGE), stat(CURSED_WOUND_DOT_PERIOD), stat(CURSED_WOUND_DURATION))
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mRadius, CURSED_WOUND_RADIUS))
			.addDashedLine();
	}

	private static Description<CursedWound> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Cursed Wound*'s damage boost.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Damage Boost: +%p1 -> +%p2 (m)")
			.statValues(stat(CURSED_WOUND_DAMAGE_1), stat(a -> a.mCursedWoundDamage, CURSED_WOUND_DAMAGE_2))
			.addDashedLine();
	}

	private static Description<CursedWound> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Killing a mob with a scythe attack will store all")
			.addLine("debuffs that were on that mob.")
			.addLine("(Excluding stun, silence, and stagger)")
			.addLine()
			.addLine("Your next attack will inflict the stored debuffs")
			.addLine("onto the target and nearby mobs, and deal bonus")
			.addLine("magic damage (s) for each debuff stored.")
			.addLine()
			.addStat("Bonus Damage: %p (s) per debuff")
			.statValues(stat(DAMAGE_PER_EFFECT_RATIO))
			.tab().addLine("(of the attack's damage)")
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mRadius, CURSED_WOUND_RADIUS))
			.addDashedLine();
	}
}
