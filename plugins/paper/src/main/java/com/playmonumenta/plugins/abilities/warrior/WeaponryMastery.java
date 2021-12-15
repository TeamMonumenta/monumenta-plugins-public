package com.playmonumenta.plugins.abilities.warrior;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;


public class WeaponryMastery extends Ability {

	private static final double AXE_1_DAMAGE_FLAT = 2;
	private static final double AXE_2_DAMAGE_FLAT = 4;
	private static final double SWORD_2_DAMAGE_FLAT = 1;
	private static final double AXE_1_DAMAGE = 0.05;
	private static final double AXE_2_DAMAGE = 0.1;
	private static final double SWORD_2_DAMAGE = 0.1;
	private static final double WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE = 0.1;

	private final double mDamageBonusAxeFlat;
	private final double mDamageBonusSwordFlat;
	private final double mDamageBonusAxe;
	private final double mDamageBonusSword;

	public WeaponryMastery(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Weapon Mastery");
		mInfo.mScoreboardId = "WeaponMastery";
		mInfo.mShorthandName = "WM";
		mInfo.mDescriptions.add("You gain 10% damage resistance while holding a sword. Additionally, your axe damage is increased by +2 plus 5% of final damage done.");
		mInfo.mDescriptions.add("Increase axe damage by +4 plus 10% of final damage done and increase sword damage by +1 plus 10% of final damage done.");
		mDisplayItem = new ItemStack(Material.STONE_SWORD, 1);
		mDamageBonusAxeFlat = getAbilityScore() == 1 ? AXE_1_DAMAGE_FLAT : AXE_2_DAMAGE_FLAT;
		mDamageBonusSwordFlat = getAbilityScore() == 1 ? 0 : SWORD_2_DAMAGE_FLAT;
		mDamageBonusAxe = getAbilityScore() == 1 ? AXE_1_DAMAGE : AXE_2_DAMAGE;
		mDamageBonusSword = getAbilityScore() == 1 ? 0 : SWORD_2_DAMAGE;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mPlayer != null && event.getCause() == DamageCause.ENTITY_ATTACK) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isAxe(mainHand)) {
				event.setDamage((event.getDamage() + mDamageBonusAxeFlat) * (1 + mDamageBonusAxe));
			} else if (ItemUtils.isSword(mainHand)) {
				event.setDamage((event.getDamage() + mDamageBonusSwordFlat) * (1 + mDamageBonusSword));
			}
		}

		return true;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (mPlayer != null && ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(EntityUtils.getDamageApproximation(event, 1 - WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE));
		}

		return true;
	}
}
