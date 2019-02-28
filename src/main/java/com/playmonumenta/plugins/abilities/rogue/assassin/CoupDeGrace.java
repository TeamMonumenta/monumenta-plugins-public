package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Coup De Grâce: If you melee attack a non-boss enemy when
 * they are under 25% health, they die instantly. At level 2,
 * the threshold increases to 30% health and enemies within 8
 * blocks of you are intimidated, taking Weakness I & Slowness
 * I for 8 seconds
 */
public class CoupDeGrace extends Ability {

	public CoupDeGrace(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "CoupDeGrace";
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity le = (LivingEntity) event.getEntity();
		if (!EntityUtils.isBoss(le)) {
			int coupDeGrace = getAbilityScore();
			double threshhold = coupDeGrace == 1 ? 0.25 : 0.3;
			if (le.getHealth() < le.getMaxHealth() * threshhold) {
				//Setting health does not count for a kill. Deal damage beyond god-level tiers
				EntityUtils.damageEntity(mPlugin, le, 9001, mPlayer);
				mWorld.spawnParticle(Particle.CRIT, le.getLocation().add(0, le.getHeight() / 2, 0), 35, 0, 0, 0, 1);
				if (coupDeGrace > 1) {
					mWorld.spawnParticle(Particle.CRIT_MAGIC, le.getLocation().add(0, le.getHeight() / 2, 0), 25, 0, 0, 0, 1);
					for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), 8, mPlayer)) {
						mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 8, 0, false, true));
						mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 8, 0, false, true));
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return InventoryUtils.isSwordItem(mHand) && InventoryUtils.isSwordItem(oHand);
	}
}
