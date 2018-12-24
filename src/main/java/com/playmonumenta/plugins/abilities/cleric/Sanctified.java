package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class Sanctified extends Ability {

	private static final double SANCTIFIED_1_DAMAGE = 3;
	private static final double SANCTIFIED_2_DAMAGE = 5;
	private static final int SANCTIFIED_EFFECT_LEVEL = 0;
	private static final int SANCTIFIED_EFFECT_DURATION = 10 * 20;
	private static final float SANCTIFIED_KNOCKBACK_SPEED = 0.35f;

	public Sanctified(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 3;
		mInfo.specId = -1;
		mInfo.scoreboardId = "Sanctified";
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		LivingEntity damager = (LivingEntity) event.getDamager();
		if (EntityUtils.isUndead(damager)) {
			if (damager instanceof Skeleton) {
				Skeleton skelly = (Skeleton)damager;
				ItemStack mainHand = skelly.getEquipment().getItemInMainHand();
				if (mainHand != null && mainHand.getType() == Material.BOW) {
					return true;
				}
			}

			int sanctified = getAbilityScore();
			if (sanctified > 0) {
				double extraDamage = sanctified == 1 ? SANCTIFIED_1_DAMAGE : SANCTIFIED_2_DAMAGE;
				EntityUtils.damageEntity(mPlugin, damager, extraDamage, mPlayer);

				MovementUtils.KnockAway(mPlayer, damager, SANCTIFIED_KNOCKBACK_SPEED);

				Location loc = damager.getLocation();
				mPlayer.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 5, 0.35, 0.35, 0.35, 0.001);

				if (sanctified > 1) {
					damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SANCTIFIED_EFFECT_DURATION, SANCTIFIED_EFFECT_LEVEL, false, true));
				}
			}
		}
		return true;
	}

}
