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
import com.playmonumenta.plugins.effects.CrusadeEnhancementTag;
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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;


public class ChoirBells extends Ability {

	private static final int DURATION = 20 * 8;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.35;
	private static final double VULNERABILITY_EFFECT_1 = 0.2;
	private static final double VULNERABILITY_EFFECT_2 = 0.35;
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

	public static final AbilityInfo<ChoirBells> INFO =
		new AbilityInfo<>(ChoirBells.class, "Choir Bells", ChoirBells::new)
			.linkedSpell(ClassAbility.CHOIR_BELLS)
			.scoreboardId("ChoirBells")
			.shorthandName("CB")
			.descriptions(
				"While not sneaking, pressing the swap key afflicts all enemies in a 10-block radius with 10% slowness for 8s. " +
					"Undead enemies also switch targets over to you, are dealt " + DAMAGE + " magic damage, " +
					"and are afflicted with 20% vulnerability and 20% weakness for 8s. Cooldown: 16s.",
				"Slowness is increased from 10% to 20%. Vulnerability and weakness are increased from 20% to 35%.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChoirBells::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)))
			.displayItem(new ItemStack(Material.BELL, 1));

	private final double mSlownessAmount;
	private final double mWeakenEffect;
	private final double mVulnerabilityEffect;
	private final ChoirBellsCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public ChoirBells(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSlownessAmount = CharmManager.getLevelPercentDecimal(player, CHARM_SLOW) + (isLevelOne() ? SLOWNESS_AMPLIFIER_1 : SLOWNESS_AMPLIFIER_2);
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKEN) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
		mVulnerabilityEffect = CharmManager.getLevelPercentDecimal(player, CHARM_VULN) + (isLevelOne() ? VULNERABILITY_EFFECT_1 : VULNERABILITY_EFFECT_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ChoirBellsCS(), ChoirBellsCS.SKIN_LIST);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);
		});
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		mCosmetic.bellsCastEffect(mPlayer, CHOIR_BELLS_RANGE);

		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), CharmManager.getRadius(mPlayer, CHARM_RANGE, CHOIR_BELLS_RANGE));
		for (LivingEntity mob : hitbox.getHitMobs()) {
			mCosmetic.bellsApplyEffect(mPlayer, mob);
			EntityUtils.applySlow(mPlugin, DURATION, mSlownessAmount, mob);

			if (Crusade.enemyTriggersAbilities(mob, mCrusade)) {
				// Infusion
				EntityUtils.applyTaunt(mPlugin, mob, mPlayer);
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE), mInfo.getLinkedSpell(), true, true);
				EntityUtils.applyVulnerability(mPlugin, DURATION, mVulnerabilityEffect, mob);
				EntityUtils.applyWeaken(mPlugin, DURATION, mWeakenEffect, mob);
			}

			if (Crusade.applyCrusadeToSlayer(mob, mCrusade)) {
				mPlugin.mEffectManager.addEffect(mob, "CrusadeSlayerTag", new CrusadeEnhancementTag(Crusade.getEnhancementDuration()));
			}
		}
		putOnCooldown();
	}
}
