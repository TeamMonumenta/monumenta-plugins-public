package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;

public class NoAbilityDamageBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_no_ability_damage";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new NoAbilityDamageBoss(plugin, boss);
	}

	public NoAbilityDamageBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player && event.getCause() == DamageCause.CUSTOM) {
			event.setCancelled(true);

			Location loc = event.getEntity().getLocation().add(0, 1, 0);
			World world = loc.getWorld();
			world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.25f, 1.5f);
			world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.25f, 0.75f);
			world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0, 0, 0, 0.3);
		}
	}
}
