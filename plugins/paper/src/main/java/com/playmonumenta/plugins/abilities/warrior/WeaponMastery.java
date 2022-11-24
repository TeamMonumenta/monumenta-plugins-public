package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class WeaponMastery extends Ability {

	private static final double AXE_1_DAMAGE_FLAT = 2;
	private static final double AXE_2_DAMAGE_FLAT = 4;
	private static final double SWORD_2_DAMAGE_FLAT = 1;
	private static final double AXE_1_DAMAGE = 0.05;
	private static final double AXE_2_DAMAGE = 0.1;
	private static final double SWORD_2_DAMAGE = 0.1;
	private static final double WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE = 0.1;
	private static final double SWORD_WEAKEN = 0.1;
	private static final double ENHANCED_DAMAGE = 0.1;
	private static final int SWORD_WEAKEN_DURATION = 4 * 20;
	private static final double AXE_SPEED = 0.15;
	private static final String SPEED_EFFECT = "WeaponMasterySpeedEffect";

	public static final String CHARM_REDUCTION = "Weapon Mastery Damage Reduction";
	public static final String CHARM_WEAKEN = "Weapon Mastery Weaken";
	public static final String CHARM_DURATION = "Weapon Mastery Duration";
	public static final String CHARM_SPEED = "Weapon Mastery Speed";

	public static final AbilityInfo<WeaponMastery> INFO =
		new AbilityInfo<>(WeaponMastery.class, "Weapon Mastery", WeaponMastery::new)
			.scoreboardId("WeaponMastery")
			.shorthandName("WM")
			.descriptions(
				"You gain 10% damage resistance while holding a sword. Additionally, your axe damage is increased by +2 plus 5% of final damage done.",
				"Increase axe damage by +4 plus 10% of final damage done and increase sword damage by +1 plus 10% of final damage done.",
				"Deal +10% final damage when using either an axe or a sword. Gain +15% speed when using an axe. Apply 10% weaken for 4s when using a sword.")
			.displayItem(new ItemStack(Material.STONE_SWORD, 1));

	private final double mDamageBonusAxeFlat;
	private final double mDamageBonusSwordFlat;
	private final double mDamageBonusAxe;
	private final double mDamageBonusSword;

	public WeaponMastery(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageBonusAxeFlat = isLevelOne() ? AXE_1_DAMAGE_FLAT : AXE_2_DAMAGE_FLAT;
		mDamageBonusSwordFlat = isLevelOne() ? 0 : SWORD_2_DAMAGE_FLAT;
		double enhancementDamage = (isEnhanced() ? ENHANCED_DAMAGE : 0);
		mDamageBonusAxe = (isLevelOne() ? AXE_1_DAMAGE : AXE_2_DAMAGE) + enhancementDamage;
		mDamageBonusSword = (isLevelOne() ? 0 : SWORD_2_DAMAGE) + enhancementDamage;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isAxe(mainHand)) {
				event.setDamage((event.getDamage() + mDamageBonusAxeFlat) * (1 + mDamageBonusAxe));
			} else if (ItemUtils.isSword(mainHand)) {
				event.setDamage((event.getDamage() + mDamageBonusSwordFlat) * (1 + mDamageBonusSword));
				if (isEnhanced()) {
					EntityUtils.applyWeaken(mPlugin, SWORD_WEAKEN_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION), SWORD_WEAKEN + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN), enemy);
				}
			}
		}
		return false; // only changes event damage
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (isEnhanced() && ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand())) {
			mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT, new PercentSpeed(6, AXE_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED), SPEED_EFFECT).displaysTime(false));
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() * (1 - (WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION))));
		}
	}
}
