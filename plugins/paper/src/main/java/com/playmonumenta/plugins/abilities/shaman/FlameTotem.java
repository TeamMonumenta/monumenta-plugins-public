package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.FlameTotemCS;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FlameTotem extends TotemAbility {
	private static final int INTERVAL = 30;
	private static final int COOLDOWN = 22 * 20;
	private static final int DURATION = 15 * 20;
	private static final int AOE_RANGE_1 = 6;
	private static final int AOE_RANGE_2 = 7;
	private static final double DAMAGE_1 = 3;
	private static final double DAMAGE_2 = 4;
	private static final double STACKING_DAMAGE = 0.05;
	private static final double ABILITY_FLAT_DMG_ADDITION = 0.5;
	private static final int ABILITY_LIMIT = 4;
	private static final int ABILITY_LIMIT_ENHANCE = 6;
	private static final int IGNITION_DURATION = 4 * 20;
	private static final double ENHANCE_DAMAGE_BOOST = 0.2;
	private static final List<ClassAbility> TOTEM_ABILITY_LIST = List.of(
		ClassAbility.FLAME_TOTEM,
		ClassAbility.LIGHTNING_TOTEM,
		ClassAbility.CLEANSING_TOTEM,
		ClassAbility.WHIRLWIND_TOTEM,
		ClassAbility.DECAYED_TOTEM,
		ClassAbility.TOTEMIC_PROJECTION
	);

	public static final String CHARM_DURATION = "Flame Totem Duration";
	public static final String CHARM_RADIUS = "Flame Totem Radius";
	public static final String CHARM_COOLDOWN = "Flame Totem Cooldown";
	public static final String CHARM_ENHANCE_DAMAGE_BOOST = "Flame Totem Enhancement Damage Amplifier";
	public static final String CHARM_PULSE_DELAY = "Flame Totem Pulse Delay";
	public static final String CHARM_ABILITY_FLAT_BOOST = "Flame Totem Used Ability Bonus Damage";
	public static final String CHARM_ABILITY_LIMIT = "Flame Totem Used Ability Limit";
	public static final String CHARM_STACKING_DAMAGE = "Flame Totem Stacking Damage Amplifier";
	public static final String CHARM_DAMAGE = "Flame Totem Damage";

	public static final AbilityInfo<FlameTotem> INFO =
		new AbilityInfo<>(FlameTotem.class, "Flame Totem", FlameTotem::new)
			.linkedSpell(ClassAbility.FLAME_TOTEM)
			.scoreboardId("FlameTotem")
			.shorthandName("FT")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Summon a totem that incinerates mobs in an area.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FlameTotem::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.addAltPresetTrigger(new AbilityTriggerInfo<>("cast", "cast", FlameTotem::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.MAGMA_BLOCK);

	private final int mInterval;
	private final double mEnhanceDamageAmplifier;
	private final double mDamage;
	private final double mStackingDamageAmplifier;
	private final double mBonusDamageFlat;
	private final int mAbilityLimit;
	private final FlameTotemCS mCosmetic;

	public double mDecayedTotemBuff = 0;
	private double mCurrentBonusFlatDamage = 0;
	private WeakHashMap<LivingEntity, Double> mConsecutiveDamageMap = new WeakHashMap<>();

	public FlameTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Flame Totem Projectile", "FlameTotem", "Flame Totem");
		mDuration = CharmManager.getDuration(player, CHARM_DURATION, DURATION);
		setRadius(CharmManager.getRadius(player, CHARM_RADIUS, isLevelTwo() ? AOE_RANGE_2 : AOE_RANGE_1));
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mEnhanceDamageAmplifier = CharmManager.calculateFlatAndPercentValue(player, CHARM_ENHANCE_DAMAGE_BOOST, ENHANCE_DAMAGE_BOOST);
		mBonusDamageFlat = CharmManager.calculateFlatAndPercentValue(player, CHARM_ABILITY_FLAT_BOOST, ABILITY_FLAT_DMG_ADDITION);
		mAbilityLimit = (isEnhanced() ? ABILITY_LIMIT_ENHANCE : ABILITY_LIMIT) + (int) CharmManager.getLevel(player, CHARM_ABILITY_LIMIT);
		mStackingDamageAmplifier = STACKING_DAMAGE + CharmManager.getLevelPercentDecimal(player, CHARM_STACKING_DAMAGE);
		mInterval = CharmManager.getDuration(player, CHARM_PULSE_DELAY, INTERVAL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new FlameTotemCS());
	}

	@Override
	public void placeTotem(Location standLocation, Player player, ArmorStand stand) {
		mConsecutiveDamageMap = new WeakHashMap<>();
		mCurrentBonusFlatDamage = 0;
		mCosmetic.flameTotemSpawn(standLocation, player, stand, getTotemRadius());
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (EntityUtils.playerCantSeeEntity(mPlayer, stand)) {
			// player cannot see the flame totem, don't tick
			mConsecutiveDamageMap.clear();
			return;
		}

		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}

		if (isEnhanced()) {
			mCosmetic.flameTotemTickEnhanced(mPlayer, standLocation, getTotemRadius());
		} else {
			mCosmetic.flameTotemTick(mPlayer, standLocation, getTotemRadius());
		}
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean chainLightning) {
		mCosmetic.flameTotemPulse(mPlayer, standLocation, getTotemRadius());

		double chainLightningMultiplier = chainLightning ? (ChainLightning.ENHANCE_OFFENSIVE_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, ChainLightning.CHARM_OFFENSIVE_TOTEM_EFFICIENCY)) : 1;
		double damage = (mDamage + mDecayedTotemBuff + mCurrentBonusFlatDamage) * mSpiritualismMultiplier * chainLightningMultiplier;

		List<LivingEntity> affectedMobs = new Hitbox.SphereHitbox(standLocation, getTotemRadius()).getHitMobs();
		affectedMobs.removeIf(e -> EntityUtils.playerCantSeeEntity(mPlayer, e));

		boolean isFinalPulse = !chainLightning && getRemainingAbilityDuration() < mInterval;

		for (LivingEntity mob : affectedMobs) {
			double mobDamage = damage;
			if (mConsecutiveDamageMap.containsKey(mob) && !chainLightning) {
				mobDamage *= 1 + mConsecutiveDamageMap.get(mob);
			}
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC,
				mInfo.getLinkedSpell(), stats), mobDamage, true, false, false);
			if (isFinalPulse && isEnhanced()) {
				EntityUtils.applyFire(mPlugin, IGNITION_DURATION, mob, mPlayer, true);
			}
			mCosmetic.flameTotemBomb(mPlayer, mob, standLocation, mPlugin, 0.3);
		}

		if (isLevelOne() || chainLightning) {
			return;
		}

		mConsecutiveDamageMap.keySet().removeIf(mob -> !affectedMobs.contains(mob));
		affectedMobs.forEach(mob -> mConsecutiveDamageMap.merge(mob, mStackingDamageAmplifier, Double::sum));
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		mDecayedTotemBuff = 0;
		mCosmetic.flameTotemExpire(world, mPlayer, standLocation);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		// Handle enhancement damage boost
		if (isEnhanced() && mTotem != null && event.getType() == DamageEvent.DamageType.MAGIC && !TOTEM_ABILITY_LIST.contains(event.getAbility())) {
			if (new Hitbox.SphereHitbox(mTotem.getLocation(), getTotemRadius()).getHitMobs().contains(enemy)) {
				event.updateDamageWithMultiplier(1 + mEnhanceDamageAmplifier);
			}
		}

		return false;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (mTotem != null && mTotem.getLocation().distance(mPlayer.getLocation()) <= getTotemRadius()) {
			if (mCurrentBonusFlatDamage < mBonusDamageFlat * mAbilityLimit) {
				mCurrentBonusFlatDamage += mBonusDamageFlat;
			}
		}
		return true;
	}

	private static Description<FlameTotem> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to fire a projectile that summons a Flame Totem with a ")
			.add(TotemAbility::getTotemRadius, AOE_RANGE_1, false, Ability::isLevelOne)
			.add(" block radius. Every ")
			.addDuration(a -> a.mInterval, INTERVAL, true)
			.add(" seconds, so long as it is within your line of sight, the totem pulses with flames that do ")
			.add(a -> a.mDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage to mobs also within your line of sight. Non-totem spells cast within the radius increase your Flame Totem damage by ")
			.add(a -> a.mBonusDamageFlat, ABILITY_FLAT_DMG_ADDITION)
			.add(" until it expires, limited up to ")
			.add(a -> a.mAbilityLimit, ABILITY_LIMIT, false, Predicate.not(Ability::isEnhanced))
			.add(" times. Duration: ")
			.addDuration(a -> a.mDuration, DURATION, false, Ability::isLevelOne)
			.add("s.")
			.addCooldown(COOLDOWN);
	}

	private static Description<FlameTotem> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Radius increased to ")
			.add(TotemAbility::getTotemRadius, AOE_RANGE_2, false, Ability::isLevelTwo)
			.add(" blocks, and the damage is increased to ")
			.add(a -> a.mDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(". Mobs take ")
			.addPercent(a -> a.mStackingDamageAmplifier, STACKING_DAMAGE)
			.add(" extra damage from your Flame Totem with each consecutive pulse of damage taken.");
	}

	private static Description<FlameTotem> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The bonus damage from non-totem abilities can now be gained up to ")
			.add(a -> a.mAbilityLimit, ABILITY_LIMIT_ENHANCE, false, Ability::isEnhanced)
			.add(" times. Mobs within the Flame Totem's radius take ")
			.addPercent(a -> a.mEnhanceDamageAmplifier, ENHANCE_DAMAGE_BOOST)
			.add(" more magic damage from your non-totem spells. The final pulse of your Flame Totem now also ignites mobs for ")
			.addDuration(IGNITION_DURATION)
			.add(" seconds.");
	}
}
