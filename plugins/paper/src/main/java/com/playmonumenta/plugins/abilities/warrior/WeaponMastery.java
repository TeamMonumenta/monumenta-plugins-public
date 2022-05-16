package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
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
	private static final double SWORD_ENHANCED_DAMAGE = 0.1;
	private static final double WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE = 0.1;
	private static final double SWORD_WEAKEN = 0.1;
	private static final int SWORD_WEAKEN_DURATION = 4 * 20;
	private static final double AXE_ATTACK_SPEED = 0.15;
	private static final String ATTACK_SPEED_EFFECT = "WeaponMasteryAttackSpeedEffect";

	private final double mDamageBonusAxeFlat;
	private final double mDamageBonusSwordFlat;
	private final double mDamageBonusAxe;
	private final double mDamageBonusSword;

	public WeaponMastery(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Weapon Mastery");
		mInfo.mScoreboardId = "WeaponMastery";
		mInfo.mShorthandName = "WM";
		mInfo.mDescriptions.add("You gain 10% damage resistance while holding a sword. Additionally, your axe damage is increased by +2 plus 5% of final damage done.");
		mInfo.mDescriptions.add("Increase axe damage by +4 plus 10% of final damage done and increase sword damage by +1 plus 10% of final damage done.");
		mInfo.mDescriptions.add("Gain +15% attack when using an axe. Deal +10% final damage and apply 10% weaken for 4s when using a sword.");
		mDisplayItem = new ItemStack(Material.STONE_SWORD, 1);
		mDamageBonusAxeFlat = isLevelOne() ? AXE_1_DAMAGE_FLAT : AXE_2_DAMAGE_FLAT;
		mDamageBonusSwordFlat = isLevelOne() ? 0 : SWORD_2_DAMAGE_FLAT;
		mDamageBonusAxe = isLevelOne() ? AXE_1_DAMAGE : AXE_2_DAMAGE;
		mDamageBonusSword = (isLevelOne() ? 0 : SWORD_2_DAMAGE) + (isEnhanced() ? SWORD_ENHANCED_DAMAGE : 0);
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
					EntityUtils.applyWeaken(mPlugin, SWORD_WEAKEN_DURATION, SWORD_WEAKEN, enemy);
				}
			}
		}
		return false; // only changes event damage
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (isEnhanced() && mPlayer != null && ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand())) {
			mPlugin.mEffectManager.addEffect(mPlayer, ATTACK_SPEED_EFFECT, new PercentAttackSpeed(6, AXE_ATTACK_SPEED, ATTACK_SPEED_EFFECT));
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (mPlayer != null && ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() * (1 - WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE));
		}
	}
}
