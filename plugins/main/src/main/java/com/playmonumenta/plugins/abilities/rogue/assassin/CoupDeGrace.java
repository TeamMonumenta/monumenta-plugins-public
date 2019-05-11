package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NetworkUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Coup De Grâce: If you melee attack a non-boss enemy and that attack
 * brings it under 20% health, they die instantly. At level 2,
 * the threshold increases to 25% health and enemies within 8
 * blocks of you are intimidated, taking Weakness I & Slowness
 * I for 8 seconds
 */
public class CoupDeGrace extends Ability {

	private static final double COUP_1_THRESHOLD = 0.2;
	private static final double COUP_2_THRESHOLD = 0.25;
	private static final int COUP_2_INTIMIDATION_DURATION = 20 * 8;

	public CoupDeGrace(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "CoupDeGrace";
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity le = (LivingEntity) event.getEntity();
		double maxHealth = le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		if (!EntityUtils.isBoss(le)) {
			double threshold = getAbilityScore() == 1 ? COUP_1_THRESHOLD : COUP_2_THRESHOLD;
			if (le.getHealth() - event.getDamage() < maxHealth * threshold) {
				event.setDamage(event.getDamage() + 9001);
				mWorld.spawnParticle(Particle.CRIT, le.getLocation().add(0, le.getHeight() / 2, 0), 35, 0, 0, 0, 1);
				if (getAbilityScore() > 1) {
					mWorld.spawnParticle(Particle.CRIT_MAGIC, le.getLocation().add(0, le.getHeight() / 2, 0), 25, 0, 0, 0, 1);
					for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), 8, mPlayer)) {
						PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, COUP_2_INTIMIDATION_DURATION, 0, false, true));
						PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, COUP_2_INTIMIDATION_DURATION, 0, false, true));
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
