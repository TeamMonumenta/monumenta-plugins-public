package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
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
import com.playmonumenta.plugins.utils.StringUtils;
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
			.descriptions(
				("While not sneaking, pressing the swap key afflicts all enemies in a %d-block radius with %s%% slowness for %ds. " +
					"Undead enemies also switch targets over to you, are dealt %d magic damage, " +
					"and are afflicted with %s%% vulnerability and %s%% weakness for %ds. Cooldown: %ds.")
					.formatted(
						CHOIR_BELLS_RANGE,
						StringUtils.multiplierToPercentage(SLOWNESS_AMPLIFIER_1),
						DURATION/20,
						DAMAGE,
						StringUtils.multiplierToPercentage(VULNERABILITY_EFFECT_1),
						StringUtils.multiplierToPercentage(WEAKEN_EFFECT_1),
						DURATION/20,
						COOLDOWN/20),
				"Slowness is increased from %s%% to %s%%. Vulnerability is increased from %s%% to %s%%. Weakness is increased from %s%% to %s%%."
					.formatted(
						StringUtils.multiplierToPercentage(SLOWNESS_AMPLIFIER_1),
						StringUtils.multiplierToPercentage(SLOWNESS_AMPLIFIER_2),
						StringUtils.multiplierToPercentage(VULNERABILITY_EFFECT_1),
						StringUtils.multiplierToPercentage(VULNERABILITY_EFFECT_2),
						StringUtils.multiplierToPercentage(WEAKEN_EFFECT_1),
						StringUtils.multiplierToPercentage(WEAKEN_EFFECT_2)
					))
			.simpleDescription("Taunt, slow, and apply vulnerability to nearby mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChoirBells::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)))
			.displayItem(Material.BELL);

	private final double mSlownessAmount;
	private final double mWeakenEffect;
	private final double mVulnerabilityEffect;
	private final int mDuration;
	private final ChoirBellsCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public ChoirBells(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSlownessAmount = CharmManager.getLevelPercentDecimal(player, CHARM_SLOW) + (isLevelOne() ? SLOWNESS_AMPLIFIER_1 : SLOWNESS_AMPLIFIER_2);
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKEN) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
		mVulnerabilityEffect = CharmManager.getLevelPercentDecimal(player, CHARM_VULN) + (isLevelOne() ? VULNERABILITY_EFFECT_1 : VULNERABILITY_EFFECT_2);
		mDuration = CharmManager.getDuration(player, CHARM_DURATION, DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ChoirBellsCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);
		});
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		mCosmetic.bellsCastEffect(mPlayer, CHOIR_BELLS_RANGE);

		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), CharmManager.getRadius(mPlayer, CHARM_RANGE, CHOIR_BELLS_RANGE));
		for (LivingEntity mob : hitbox.getHitMobs()) {
			mCosmetic.bellsApplyEffect(mPlayer, mob);
			EntityUtils.applySlow(mPlugin, mDuration, mSlownessAmount, mob);

			if (Crusade.enemyTriggersAbilities(mob, mCrusade)) {
				// Infusion
				EntityUtils.applyTaunt(mob, mPlayer);
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE), mInfo.getLinkedSpell(), true, true);
				EntityUtils.applyVulnerability(mPlugin, mDuration, mVulnerabilityEffect, mob);
				EntityUtils.applyWeaken(mPlugin, mDuration, mWeakenEffect, mob);
			}
			Crusade.addCrusadeTag(mob, mCrusade);
		}
		putOnCooldown();
		return true;
	}
}
