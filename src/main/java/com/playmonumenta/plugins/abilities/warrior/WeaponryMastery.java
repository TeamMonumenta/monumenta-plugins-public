package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WeaponryMastery extends Ability {

	private static final int WEAPON_MASTERY_AXE_1_DAMAGE = 2;
	private static final int WEAPON_MASTERY_AXE_2_DAMAGE = 4;
	private static final int WEAPON_MASTERY_SWORD_2_DAMAGE = 1;

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		int weaponMastery = getAbilityScore(player);
		// The extra damage that will be applied to the hit damagee at the end of this function
		double extraDamage = 0;
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		LivingEntity damagee = (LivingEntity) event.getEntity();

		if (InventoryUtils.isAxeItem(mainHand) && weaponMastery >= 1) {
			extraDamage += (weaponMastery == 1) ? WEAPON_MASTERY_AXE_1_DAMAGE : WEAPON_MASTERY_AXE_2_DAMAGE;
		} else if (InventoryUtils.isSwordItem(mainHand) && weaponMastery >= 2) {
			extraDamage += WEAPON_MASTERY_SWORD_2_DAMAGE;
		}

		if (extraDamage > 0) {
			event.setDamage(event.getDamage() + extraDamage);
		}
		return true;
	}

	@Override
	public void setupClassPotionEffects(Player player) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.DAMAGE_RESISTANCE);

		//  Player has an sword in their mainHand.
		if (InventoryUtils.isSwordItem(mainHand)) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, 0, true, false));
		}
	}

	@Override
	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) {
		mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.DAMAGE_RESISTANCE);

		//  Player has an sword in their mainHand.
		if (InventoryUtils.isSwordItem(mainHand)) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, 0, true, false));
		}
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 2;
		info.specId = -1;
		info.scoreboardId = "WeaponMastery";
		return info;
	}

}
