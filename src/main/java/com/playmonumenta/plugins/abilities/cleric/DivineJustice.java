package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import java.util.Random;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

public class DivineJustice extends Ability {

	private static final int DIVINE_JUSTICE_DAMAGE = 5;
	private static final int DIVINE_JUSTICE_HEAL = 4;
	private static final int DIVINE_JUSTICE_CRIT_HEAL = 1;

	public DivineJustice(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 3;
		mInfo.specId = -1;
		mInfo.scoreboardId = "DivineJustice";
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity damagee = (LivingEntity) event.getEntity();
		if (damagee.getLastDamageCause() != null
				&& damagee.getLastDamageCause().getCause() != DamageCause.PROJECTILE
				&& EntityUtils.isUndead(damagee)) {
			EntityUtils.damageEntity(mPlugin, damagee, DIVINE_JUSTICE_DAMAGE, mPlayer);

			PlayerUtils.healPlayer(mPlayer, DIVINE_JUSTICE_CRIT_HEAL);

			World world = mPlayer.getWorld();
			Location loc = damagee.getLocation();
			world.spawnParticle(Particle.CRIT_MAGIC, loc.add(0, 1, 0), 20, 0.25, 0.5, 0.5, 0.001);
			world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.15f, 1.5f);
		}
		return true;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity killedEntity = event.getEntity();
		if (getAbilityScore() > 1
				&& killedEntity.getLastDamageCause() != null
				&& killedEntity.getLastDamageCause().getCause() != DamageCause.PROJECTILE
				&& EntityUtils.isUndead(killedEntity)) {
			PlayerUtils.healPlayer(mPlayer, DIVINE_JUSTICE_HEAL);

			World world = mPlayer.getWorld();
			Location loc = killedEntity.getLocation();
			world.spawnParticle(Particle.CRIT_MAGIC, loc.add(0, 1, 0), 20, 0.25, 0.25, 0.25, 0.001);
			mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 1.5f);
		}
	}

	@Override
	public boolean runCheck() {
		return PlayerUtils.isCritical(mPlayer);
	}

}
