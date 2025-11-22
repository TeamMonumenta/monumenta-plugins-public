package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.CholericFlamesCS;
import com.playmonumenta.plugins.effects.CholericFlamesAntiHeal;
import com.playmonumenta.plugins.effects.SpreadEffectOnDeath;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class CholericFlames extends Ability {

	private static final int RANGE = 9;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 5;
	public static final String ANTIHEAL_EFFECT = "CholericFlamesAntiHeal";
	private static final int DURATION = 7 * 20;
	private static final int COOLDOWN = 10 * 20;
	private static final int MAX_DEBUFFS = 2;
	private static final String SPREAD_EFFECT_ON_DEATH_EFFECT = "CholericFlamesSpreadEffectOnDeath";
	private static final int SPREAD_EFFECT_DURATION = 30 * 20;
	private static final int SPREAD_EFFECT_DURATION_APPLIED = 5 * 20;
	private static final double SPREAD_EFFECT_RADIUS = 3;
	public static final float KNOCKBACK = 0.5f;

	public static final String CHARM_DAMAGE = "Choleric Flames Damage";
	public static final String CHARM_RANGE = "Choleric Flames Range";
	public static final String CHARM_COOLDOWN = "Choleric Flames Cooldown";
	public static final String CHARM_DURATION = "Choleric Flames Duration";
	public static final String CHARM_KNOCKBACK = "Choleric Flames Knockback";
	public static final String CHARM_INFERNO_CAP = "Choleric Flames Inferno Cap";
	public static final String CHARM_ENHANCEMENT_RADIUS = "Choleric Flames Enhancement Radius";

	public static final AbilityInfo<CholericFlames> INFO =
		new AbilityInfo<>(CholericFlames.class, "Choleric Flames", CholericFlames::new)
			.linkedSpell(ClassAbility.CHOLERIC_FLAMES)
			.scoreboardId("CholericFlames")
			.shorthandName("CF")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Deal damage and ignite nearby mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CholericFlames::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.FIRE_CHARGE);

	private final double mDamage;
	private final double mRange;
	private final int mDuration;
	private final int mMaxDebuffs;
	private final double mSpreadRadius;

	private final CholericFlamesCS mCosmetic;

	public CholericFlames(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mMaxDebuffs = MAX_DEBUFFS + (int) CharmManager.getLevel(mPlayer, CHARM_INFERNO_CAP);
		mSpreadRadius = CharmManager.getRadius(mPlayer, CHARM_ENHANCEMENT_RADIUS, SPREAD_EFFECT_RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CholericFlamesCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		mCosmetic.flameEffects(mPlayer, mPlayer.getWorld(), mPlayer.getLocation(), mRange);

		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mRange);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
			MovementUtils.knockAway(mPlayer, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK));

			// Gets a copy so modifying the inferno level does not have effect elsewhere
			ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
			if (isEnhanced()) {
				int debuffs = Math.min(AbilityUtils.getDebuffCount(mPlugin, mob) / 2, mMaxDebuffs);
				if (debuffs > 0) {
					playerItemStats.getItemStats().add(Objects.requireNonNull(EnchantmentType.INFERNO.getItemStat()), debuffs);
				}
				mPlugin.mEffectManager.addEffect(mob, SPREAD_EFFECT_ON_DEATH_EFFECT, new SpreadEffectOnDeath(SPREAD_EFFECT_DURATION, Inferno.INFERNO_EFFECT_NAME, mSpreadRadius, SPREAD_EFFECT_DURATION_APPLIED, false));
			}

			EntityUtils.applyFire(mPlugin, mDuration, mob, mPlayer, playerItemStats);
			if (isLevelTwo()) {
				mPlugin.mEffectManager.addEffect(mob, ANTIHEAL_EFFECT, new CholericFlamesAntiHeal(mDuration));
			}
		}

		putOnCooldown();
		return true;
	}

	private static Description<CholericFlames> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to deal ")
			.add(a -> a.mDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage to mobs within ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks, knocking them back and igniting them for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

	private static Description<CholericFlames> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased to ")
			.add(a -> a.mDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(", and mobs are also afflicted with -100% healing for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" (Counts as a level 1 debuff).");
	}

	private static Description<CholericFlames> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Mobs ignited by this ability are inflicted with an additional level of Inferno for every two debuffs they have (rounded down) prior to this ability, up to ")
			.add(a -> a.mMaxDebuffs, MAX_DEBUFFS)
			.add(". Additionally, when these mobs die, they explode, applying all Inferno they have at the time of death to all mobs within ")
			.add(a -> a.mSpreadRadius, SPREAD_EFFECT_RADIUS)
			.add(" blocks for ")
			.addDuration(SPREAD_EFFECT_DURATION_APPLIED)
			.add(" seconds.");
	}
}
