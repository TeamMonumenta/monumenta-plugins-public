package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.HandOfLightCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HandOfLight extends Ability {
	public static final int RANGE = 12;
	public static final int NEARBY_SPHERE_RANGE = 2;
	private static final double HEALING_ANGLE = 70; // (half) angle of the healing cone
	private static final double HEALING_DOT_ANGLE = Math.cos(Math.toRadians(HEALING_ANGLE));
	private static final int HEALING_1_COOLDOWN = 14 * 20;
	private static final int HEALING_2_COOLDOWN = 10 * 20;
	private static final int FLAT_1 = 4;
	private static final int FLAT_2 = 6;
	private static final double PERCENT_1 = 0.1;
	private static final double PERCENT_2 = 0.15;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 4;
	private static final int DAMAGE_PER_1 = 2;
	private static final int DAMAGE_PER_2 = 3;
	private static final int MAX_MOBS_1 = 5;
	private static final int MAX_MOBS_2 = 4;
	private static final int REGEN_LEVEL = 1; // Actually 2 because of how effects work
	private static final int REGEN_DURATION = 4 * 20;
	private static final double ENHANCEMENT_COOLDOWN_REDUCTION_PER_4_HP_HEALED = 0.025;
	private static final double ENHANCEMENT_COOLDOWN_REDUCTION_MAX = 0.5;
	private static final int ENHANCEMENT_HERETIC_STUN_DURATION = 10;

	public static final String CHARM_DAMAGE = "Hand of Light Damage";
	public static final String CHARM_MAX_MOBS = "Hand of Light Max Mobs";
	public static final String CHARM_COOLDOWN = "Hand of Light Cooldown";
	public static final String CHARM_RANGE = "Hand of Light Range";
	public static final String CHARM_HEALING = "Hand of Light Healing";
	public static final String CHARM_REGEN = "Hand of Light Regeneration Amplifier";

	public static final AbilityInfo<HandOfLight> INFO =
		new AbilityInfo<>(HandOfLight.class, "Hand of Light", HandOfLight::new)
			.linkedSpell(ClassAbility.HAND_OF_LIGHT)
			.scoreboardId("Healing")
			.shorthandName("HoL")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Heal all players in front of the Cleric, and damage all mobs based on the number of Heretics in the area.")
			.cooldown(HEALING_1_COOLDOWN, HEALING_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HandOfLight::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.SNEAK_WITH_SHIELD).keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS_EXCEPT_SHIELD)))
			.displayItem(Material.PINK_DYE);

	private final double mRange;
	private final double mFlat;
	private final double mPercent;
	private final double mDamage;
	private final double mDamagePer;
	private final int mMaxMobs;
	private final int mRegenerationLevel;

	private final HandOfLightCS mCosmetic;

	public HandOfLight(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mFlat = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, isLevelOne() ? FLAT_1 : FLAT_2);
		mPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, isLevelOne() ? PERCENT_1 : PERCENT_2);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamagePer = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_PER_1 : DAMAGE_PER_2);
		mMaxMobs = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MAX_MOBS, isLevelOne() ? MAX_MOBS_1 : MAX_MOBS_2);
		mRegenerationLevel = REGEN_LEVEL + (int) CharmManager.getLevel(player, CHARM_REGEN);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HandOfLightCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		World world = mPlayer.getWorld();
		Location userLoc = mPlayer.getLocation();

		Hitbox hitbox;
		if (!isEnhanced()) {
			hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), mRange, Math.toRadians(HEALING_ANGLE))
				.union(new Hitbox.SphereHitbox(mPlayer.getLocation(), NEARBY_SPHERE_RANGE));
		} else {
			hitbox = new Hitbox.SphereHitbox(mPlayer.getEyeLocation(), mRange);
		}
		List<LivingEntity> nearbyMobs = hitbox.getHitMobs();
		nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		boolean doCooldown = false;
		List<LivingEntity> heretics = new ArrayList<>(nearbyMobs);
		heretics.removeIf(mob -> !Crusade.enemyTriggersAbilities(mob));
		if (isEnhanced()) {
			heretics.forEach(mob -> EntityUtils.applyStun(mPlugin, ENHANCEMENT_HERETIC_STUN_DURATION, mob));
			if (!heretics.isEmpty()) {
				doCooldown = true;
			}
		}
		double damage = mDamage + mDamagePer * Math.min(heretics.size(), mMaxMobs);
		double cooldown = getModifiedCooldown();

		if (!heretics.isEmpty()) {
			doCooldown = true;
			for (LivingEntity mob : nearbyMobs) {
				Location loc = mob.getLocation();
				DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, true);
				mCosmetic.lightDamageEffect(mPlayer, loc, mob, heretics);
			}
			mCosmetic.lightDamageCastEffect(world, userLoc, mPlugin, mPlayer, (float) mRange, !isEnhanced() ? HEALING_DOT_ANGLE : -1, nearbyMobs);
		}

		List<Player> nearbyPlayers = hitbox.getHitPlayers(mPlayer, true);
		nearbyPlayers.removeIf(p -> p.getScoreboardTags().contains("disable_class"));

		if (!nearbyPlayers.isEmpty()) {
			doCooldown = true;
			double healthHealed = 0;
			for (Player p : nearbyPlayers) {
				double maxHealth = EntityUtils.getMaxHealth(p);
				double healthBeforeHeal = p.getHealth();
				PlayerUtils.healPlayer(mPlugin, p, mFlat + mPercent * maxHealth, mPlayer);
				healthHealed += p.getHealth() - healthBeforeHeal;

				Location loc = p.getLocation();
				mPlugin.mPotionManager.addPotion(p, PotionManager.PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.REGENERATION, REGEN_DURATION, mRegenerationLevel, true, true));
				mCosmetic.lightHealEffect(mPlayer, loc, p);
			}

			mCosmetic.lightHealCastEffect(world, userLoc, mPlugin, mPlayer, (float) mRange, !isEnhanced() ? HEALING_DOT_ANGLE : -1, nearbyPlayers);

			if (isEnhanced()) {
				cooldown *= 1 - Math.min((healthHealed / 4) * ENHANCEMENT_COOLDOWN_REDUCTION_PER_4_HP_HEALED, ENHANCEMENT_COOLDOWN_REDUCTION_MAX);
			}
		}

		if (!doCooldown) {
			return false;
		}

		putOnCooldown((int) cooldown);
		return true;
	}

	private static Description<HandOfLight> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to heal all other players in a ")
			.add(a -> a.mRange, RANGE)
			.add(" block cone in front of you or within ")
			.add(a -> NEARBY_SPHERE_RANGE, NEARBY_SPHERE_RANGE)
			.add(" blocks of you for ")
			.add(a -> a.mFlat, FLAT_1, false, Ability::isLevelOne)
			.add(" health + ")
			.addPercent(a -> a.mPercent, PERCENT_1, false, Ability::isLevelOne)
			.add(" of their max health, and give them Regeneration ")
			.addPotionAmplifier(a -> a.mRegenerationLevel, REGEN_LEVEL)
			.add(" for ")
			.addDuration(REGEN_DURATION)
			.add(" seconds. Additionally, if there is at least one Heretic in the area, damage all mobs in the area with magic damage equal to ")
			.add(a -> a.mDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" + ")
			.add(a -> a.mDamagePer, DAMAGE_PER_1, false, Ability::isLevelOne)
			.add(" * the number of Heretics in the range, up to ")
			.add(a -> a.mMaxMobs, MAX_MOBS_1, false, Ability::isLevelOne)
			.add(" mobs.")
			.addCooldown(HEALING_1_COOLDOWN, Ability::isLevelOne);
	}

	private static Description<HandOfLight> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The healing is improved to ")
			.add(a -> a.mFlat, FLAT_2, false, Ability::isLevelTwo)
			.add(" health + ")
			.addPercent(a -> a.mPercent, PERCENT_2, false, Ability::isLevelTwo)
			.add(" of their max health. Damage is increased to ")
			.add(a -> a.mDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" + ")
			.add(a -> a.mDamagePer, DAMAGE_PER_2, false, Ability::isLevelOne)
			.add(" * the number of Heretics in the range, up to ")
			.add(a -> a.mMaxMobs, MAX_MOBS_2, false, Ability::isLevelOne)
			.add(" mobs.")
			.addCooldown(HEALING_2_COOLDOWN, Ability::isLevelTwo);
	}

	private static Description<HandOfLight> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The cone is changed to a sphere of equal range, centered on the Cleric. The cooldown is reduced by ")
			.addPercent(ENHANCEMENT_COOLDOWN_REDUCTION_PER_4_HP_HEALED)
			.add(" for each 4 health healed, capped at ")
			.addPercent(ENHANCEMENT_COOLDOWN_REDUCTION_MAX)
			.add(" cooldown. All Heretics caught in the radius are stunned for ")
			.addDuration(ENHANCEMENT_HERETIC_STUN_DURATION)
			.add(" seconds.");
	}
}
