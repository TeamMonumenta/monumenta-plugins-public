package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.paladin.ChoirBellsCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class ChoirBells extends Ability {

	private static final int DURATION = 20 * 8;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.35;
	private static final double VULNERABILITY_EFFECT_1 = 0.35;
	private static final double VULNERABILITY_EFFECT_2 = 0.5;
	private static final double SLOWNESS_AMPLIFIER_1 = 0.1;
	private static final double SLOWNESS_AMPLIFIER_2 = 0.2;
	private static final int COOLDOWN = 16 * 20;
	private static final int CHOIR_BELLS_RANGE = 10;
	private static final int DAMAGE = 6;

	public static final String CHARM_DAMAGE = "Choir Bells Damage";
	public static final String CHARM_COOLDOWN = "Choir Bells Cooldown";
	public static final String CHARM_SLOW = "Choir Bells Slowness Amplifier";
	public static final String CHARM_VULN = "Choir Bells Vulnerability Amplifier";
	public static final String CHARM_WEAKEN = "Choir Bells Weakness Amplifier";
	public static final String CHARM_RANGE = "Choir Bells Range";
	public static final String CHARM_DURATION = "Choir Bells Debuff Duration";

	public static final AbilityInfo<ChoirBells> INFO =
		new AbilityInfo<>(ChoirBells.class, "Choir Bells", ChoirBells::new)
			.linkedSpell(ClassAbility.CHOIR_BELLS)
			.scoreboardId("ChoirBells")
			.shorthandName("CB")
			.hotbarName("\uD83D\uDD14")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Taunt, slow, and apply vulnerability to nearby mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChoirBells::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)))
			.displayItem(Material.BELL);

	private final double mSlownessAmount;
	private final double mWeakenEffect;
	private final double mVulnerabilityEffect;
	private final int mDuration;
	private final double mRange;
	private final double mDamage;
	private final ChoirBellsCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public ChoirBells(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSlownessAmount = CharmManager.getLevelPercentDecimal(player, CHARM_SLOW) + (isLevelOne() ? SLOWNESS_AMPLIFIER_1 : SLOWNESS_AMPLIFIER_2);
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKEN) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
		mVulnerabilityEffect = CharmManager.getLevelPercentDecimal(player, CHARM_VULN) + (isLevelOne() ? VULNERABILITY_EFFECT_1 : VULNERABILITY_EFFECT_2);
		mDuration = CharmManager.getDuration(player, CHARM_DURATION, DURATION);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, CHOIR_BELLS_RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ChoirBellsCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);
		});
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		mCosmetic.bellsCastEffect(mPlayer, mRange);

		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mRange);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			mCosmetic.bellsApplyEffect(mPlayer, mob);
			EntityUtils.applySlow(mPlugin, mDuration, mSlownessAmount, mob);

			if (Crusade.enemyTriggersAbilities(mob, mCrusade)) {
				// Infusion
				EntityUtils.applyTaunt(mob, mPlayer);
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, true);
				EntityUtils.applyVulnerability(mPlugin, mDuration, mVulnerabilityEffect, mob);
				EntityUtils.applyWeaken(mPlugin, mDuration, mWeakenEffect, mob);
			}
			Crusade.addCrusadeTag(mob, mCrusade);
		}
		putOnCooldown();
		return true;
	}

	private static Description<ChoirBells> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to afflict all enemies within ")
			.add(a -> a.mRange, CHOIR_BELLS_RANGE)
			.add(" blocks with ")
			.addPercent(a -> a.mSlownessAmount, SLOWNESS_AMPLIFIER_1, false, Ability::isLevelOne)
			.add(" slowness for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds. Undead enemies also switch targets over to you, are dealt ")
			.add(a -> a.mDamage, DAMAGE)
			.add(" magic damage, and are afflicted with ")
			.addPercent(a -> a.mVulnerabilityEffect, VULNERABILITY_EFFECT_1, false, Ability::isLevelOne)
			.add(" vulnerability and ")
			.addPercent(a -> a.mWeakenEffect, WEAKEN_EFFECT_1, false, Ability::isLevelOne)
			.add(" weakness for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

	private static Description<ChoirBells> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Slowness is increased to ")
			.addPercent(a -> a.mSlownessAmount, SLOWNESS_AMPLIFIER_2, false, Ability::isLevelTwo)
			.add(". Vulnerability is increased to ")
			.addPercent(a -> a.mVulnerabilityEffect, VULNERABILITY_EFFECT_2, false, Ability::isLevelTwo)
			.add(". Weakness is increased to ")
			.addPercent(a -> a.mWeakenEffect, WEAKEN_EFFECT_2, false, Ability::isLevelTwo)
			.add(".");
	}
}
