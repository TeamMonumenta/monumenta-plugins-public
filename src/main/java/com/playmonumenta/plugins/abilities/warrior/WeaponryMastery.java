package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class WeaponryMastery extends Ability {

	private static final int WEAPON_MASTERY_AXE_1_DAMAGE = 2;
	private static final int WEAPON_MASTERY_AXE_2_DAMAGE = 4;
	private static final int WEAPON_MASTERY_SWORD_2_DAMAGE = 1;

	public WeaponryMastery(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "WeaponMastery";
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		int weaponMastery = getAbilityScore();
		// The extra damage that will be applied to the hit damagee at the end of this function
		double extraDamage = 0;
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();

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
	public void setupClassPotionEffects() {
		PlayerItemHeldEvent(mPlayer.getInventory().getItemInMainHand(), null);
	}

	@Override
	public void PlayerItemHeldEvent(ItemStack mainHand, ItemStack offHand) {
		//  Player has an sword in their mainHand.
		if (mainHand != null && InventoryUtils.isSwordItem(mainHand)) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, 0, true, false));
		} else {
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.DAMAGE_RESISTANCE);
		}
	}
}
