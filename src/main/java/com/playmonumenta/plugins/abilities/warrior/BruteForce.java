package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import java.util.Random;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class BruteForce extends Ability {

	private static final float BRUTE_FORCE_RADIUS = 2.0f;
	private static final Integer BRUTE_FORCE_1_DAMAGE = 2;
	private static final Integer BRUTE_FORCE_2_DAMAGE = 4;
	private static final float BRUTE_FORCE_KNOCKBACK_SPEED = 0.5f;

	public BruteForce(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 2;
		mInfo.specId = -1;
		mInfo.scoreboardId = "BruteForce";
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		int bruteForce = getAbilityScore(player);
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		DamageCause cause = event.getCause();
		LivingEntity damagee = (LivingEntity) event.getEntity();

		if (bruteForce > 0 && PlayerUtils.isCritical(player) && cause != DamageCause.PROJECTILE &&
		    (InventoryUtils.isAxeItem(mainHand) || InventoryUtils.isSwordItem(mainHand) || InventoryUtils.isScytheItem(mainHand))) {

			double bruteForceDamage = bruteForce == 1 ? BRUTE_FORCE_1_DAMAGE : BRUTE_FORCE_2_DAMAGE;
			event.setDamage(event.getDamage() + bruteForceDamage);

			Location loc = damagee.getLocation().add(0, damagee.getHeight() / 2, 0);
			mWorld.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 1);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.135);

			// Damage those non-hit nearby entities and knock them away
			for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), BRUTE_FORCE_RADIUS)) {
				if (mob != damagee) {
					EntityUtils.damageEntity(mPlugin, mob, bruteForceDamage, player);
					MovementUtils.KnockAway(player, mob, BRUTE_FORCE_KNOCKBACK_SPEED);
				}
			}

			// Knock away just the hit entity
			MovementUtils.KnockAway(player, damagee, BRUTE_FORCE_KNOCKBACK_SPEED);
		}

		return true;
	}
}
