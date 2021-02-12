package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.EntityUtils;

public class FocusFireBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_focusfire";
	public static final int detectionRange = 15;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FocusFireBoss(plugin, boss);
	}

	public FocusFireBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
        //If we hit a player
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            //Set all nearby mobs to target them
            for (LivingEntity mob : EntityUtils.getNearbyMobs(mBoss.getLocation(), detectionRange)) {
                if (mob instanceof Mob) {
                    ((Mob) mob).setTarget(player);
                }
            }
            //Let the players know something happened
            player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3f, 0.9f);
            player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 25, 1.5, 1.5, 1.5);
        }
    }

}
