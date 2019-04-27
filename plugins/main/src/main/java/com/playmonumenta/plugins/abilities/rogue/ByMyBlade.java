package com.playmonumenta.plugins.abilities.rogue;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.rogue.assassin.Preparation;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class ByMyBlade extends Ability {

	private static final int BY_MY_BLADE_HASTE_1_LVL = 1;
	private static final int BY_MY_BLADE_HASTE_2_LVL = 3;
	private static final int BY_MY_BLADE_HASTE_DURATION = 4 * 20;
	private static final double BY_MY_BLADE_DAMAGE_1 = 12;
	private static final double BY_MY_BLADE_DAMAGE_2 = 24;
	private static final int BY_MY_BLADE_COOLDOWN = 10 * 20;

	public ByMyBlade(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.BY_MY_BLADE;
		mInfo.scoreboardId = "ByMyBlade";
		mInfo.cooldown = BY_MY_BLADE_COOLDOWN;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() != DamageCause.PROJECTILE) {
			int byMyBlade = getAbilityScore();
			LivingEntity damagee = (LivingEntity) event.getEntity();
			int effectLevel = (byMyBlade == 1) ? BY_MY_BLADE_HASTE_1_LVL : BY_MY_BLADE_HASTE_2_LVL;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.FAST_DIGGING,
			                                                  BY_MY_BLADE_HASTE_DURATION,
			                                                  effectLevel, false, true));


			double extraDamage = (byMyBlade == 1) ? BY_MY_BLADE_DAMAGE_1 : BY_MY_BLADE_DAMAGE_2;
			if (damagee instanceof Player) {
				extraDamage = BY_MY_BLADE_DAMAGE_1;
			}
			Preparation pp = (Preparation) AbilityManager.getManager().getPlayerAbility(mPlayer, Preparation.class);
			if (pp != null) {
				extraDamage += pp.getBonus(mInfo.linkedSpell);
			}
			EntityUtils.damageEntity(mPlugin, damagee, extraDamage, mPlayer);

			Location loc = damagee.getLocation();
			loc.add(0, 1, 0);
			int count = 15;
			if (byMyBlade > 1) {
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 45, 0.2, 0.65, 0.2, 1.0);
				count = 30;
				if (damagee instanceof Player) {
					MovementUtils.KnockAway(mPlayer, damagee, 0.3f);
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
					                                 new PotionEffect(PotionEffectType.SPEED,
					                                                  BY_MY_BLADE_HASTE_DURATION,
					                                                  0, false, true));
				}
			}
			mWorld.spawnParticle(Particle.SPELL_MOB, loc, count, 0.25, 0.5, 0.5, 0.001);
			mWorld.spawnParticle(Particle.CRIT, loc, 30, 0.25, 0.5, 0.5, 0.001);
			mWorld.playSound(loc, Sound.ITEM_SHIELD_BREAK, 2.0f, 0.5f);
			putOnCooldown();
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		if (PlayerUtils.isCritical(mPlayer)) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
				return true;
			}
		}
		return false;
	}

}
