package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;

public class ByMyBlade extends Ability {

	private static final int BY_MY_BLADE_HASTE_1_LVL = 1;
	private static final int BY_MY_BLADE_HASTE_2_LVL = 3;
	private static final int BY_MY_BLADE_HASTE_DURATION = 4 * 20;
	private static final double BY_MY_BLADE_DAMAGE_1 = 12;
	private static final double BY_MY_BLADE_DAMAGE_2 = 24;
	private static final int BY_MY_BLADE_COOLDOWN = 10 * 20;

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		if (event.getCause() != DamageCause.PROJECTILE) {
			int byMyBlade = getAbilityScore(player);
			LivingEntity damagee = (LivingEntity) event.getEntity();
			int effectLevel = (byMyBlade == 1) ? BY_MY_BLADE_HASTE_1_LVL : BY_MY_BLADE_HASTE_2_LVL;
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.FAST_DIGGING,
			                                                  BY_MY_BLADE_HASTE_DURATION,
			                                                  effectLevel, false, true));


			double extraDamage = (byMyBlade == 1) ? BY_MY_BLADE_DAMAGE_1 : BY_MY_BLADE_DAMAGE_2;
			AbilityUtils.rogueDamageMob(mPlugin, player, damagee, extraDamage);

			Location loc = damagee.getLocation();
			loc.add(0, 1, 0);
			int count = 15;
			if (byMyBlade > 1) {
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 45, 0.2, 0.65, 0.2, 1.0);
				count = 30;
			}
			mWorld.spawnParticle(Particle.SPELL_MOB, loc, count, 0.25, 0.5, 0.5, 0.001);
			mWorld.spawnParticle(Particle.CRIT, loc, 30, 0.25, 0.5, 0.5, 0.001);
			mWorld.playSound(loc, Sound.ITEM_SHIELD_BREAK, 2.0f, 0.5f);
			putOnCooldown(player);
		}
		return true;
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 4;
		info.specId = -1;
		info.linkedSpell = Spells.BY_MY_BLADE;
		info.scoreboardId = "ByMyBlade";
		info.cooldown = BY_MY_BLADE_COOLDOWN;
		return info;
	}

	@Override
	public boolean runCheck(Player player) {
		if (PlayerUtils.isCritical(player)) {
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();
			if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
				return true;
			}
		}
		return false;
	}

}
