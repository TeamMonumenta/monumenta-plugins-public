package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Sanctified extends Ability {

	private static final int SANCTIFIED_1_DAMAGE = 5;
	private static final int SANCTIFIED_2_DAMAGE = 7;
	private static final int SANCTIFIED_EFFECT_LEVEL = 0;
	private static final int SANCTIFIED_EFFECT_DURATION = 10 * 20;
	private static final float SANCTIFIED_KNOCKBACK_SPEED = 0.35f;

	private int mDamage;

	public Sanctified(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Sanctified";
		mDamage = getAbilityScore() == 1 ? SANCTIFIED_1_DAMAGE : SANCTIFIED_2_DAMAGE;
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		LivingEntity damager = (LivingEntity) event.getDamager();
		if (EntityUtils.isUndead(damager) && event.getCause() == DamageCause.ENTITY_ATTACK) {
			EntityUtils.damageEntity(mPlugin, damager, mDamage, mPlayer);

			MovementUtils.knockAway(mPlayer, damager, SANCTIFIED_KNOCKBACK_SPEED);

			Location loc = damager.getLocation();
			mPlayer.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc.add(0, damager.getHeight() / 2, 0), 7, 0.35, 0.35, 0.35, 0.125);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.7f, 1.2f);

			if (getAbilityScore() > 1) {
				PotionUtils.applyPotion(mPlayer, damager, new PotionEffect(PotionEffectType.SLOW, SANCTIFIED_EFFECT_DURATION, SANCTIFIED_EFFECT_LEVEL, false, true));
			}
		}
		return true;
	}

}
