package com.playmonumenta.plugins.abilities.warrior;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class WeaponryMastery extends Ability {

	private static final double WEAPON_MASTERY_AXE_1_DAMAGE = 3;
	private static final double WEAPON_MASTERY_AXE_2_DAMAGE = 6;
	private static final double WEAPON_MASTERY_SWORD_2_DAMAGE = 2;
	private static final double WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE = 0.1;

	private final double mDamageBonusAxe;
	private final double mDamageBonusSword;

	public WeaponryMastery(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Weapon Mastery");
		mInfo.mScoreboardId = "WeaponMastery";
		mInfo.mShorthandName = "WM";
		mInfo.mDescriptions.add("You gain 10% damage resistance while holding a sword. Deal +3 damage while using an axe.");
		mInfo.mDescriptions.add("Instead deal +6 damage with an axe and gain an additional +2 damage while using a sword.");
		mDamageBonusAxe = getAbilityScore() == 1 ? WEAPON_MASTERY_AXE_1_DAMAGE : WEAPON_MASTERY_AXE_2_DAMAGE;
		mDamageBonusSword = getAbilityScore() == 1 ? 0 : WEAPON_MASTERY_SWORD_2_DAMAGE;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();

			if (InventoryUtils.isAxeItem(mainHand)) {
				event.setDamage(event.getDamage() + mDamageBonusAxe);
			} else if (InventoryUtils.isSwordItem(mainHand)) {
				event.setDamage(event.getDamage() + mDamageBonusSword);
			}
		}

		return true;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (InventoryUtils.isSwordItem(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(EntityUtils.getDamageApproximation(event, 1 - WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE));
		}

		return true;
	}

}
