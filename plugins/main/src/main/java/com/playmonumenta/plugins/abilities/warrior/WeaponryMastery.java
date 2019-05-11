package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NetworkUtils;

public class WeaponryMastery extends Ability {

	private static final double WEAPON_MASTERY_AXE_1_DAMAGE = 2;
	private static final double WEAPON_MASTERY_AXE_2_DAMAGE = 4;
	private static final double WEAPON_MASTERY_SWORD_2_DAMAGE = 1.5;

	public WeaponryMastery(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "WeaponMastery";
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		int weaponMastery = getAbilityScore();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();

		if (InventoryUtils.isAxeItem(mainHand)) {
			double damage = weaponMastery == 1 ? WEAPON_MASTERY_AXE_1_DAMAGE : WEAPON_MASTERY_AXE_2_DAMAGE;
			event.setDamage(event.getDamage() + damage);
		} else if (InventoryUtils.isSwordItem(mainHand) && weaponMastery > 1) {
			event.setDamage(event.getDamage() + WEAPON_MASTERY_SWORD_2_DAMAGE);
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
