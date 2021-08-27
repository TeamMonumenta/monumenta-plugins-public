package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.utils.BossUtils;

/**
 * @deprecated use boss_onhit instead, like this:
 *<blockquote><pre>
 * /boss var Tags add boss_onhit
 * /boss var Tags add boss_onhit[effects=[(WITHER,80,1)]]
 * CARE this ability has some particle & sound, fix those too if you don't want the default values
 * </pre></blockquote>
 * @G3m1n1Boy
 *
*/
public class WitherHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_witherhit";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WitherHitBoss(plugin, boss);
	}

	public WitherHitBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		LivingEntity target = (LivingEntity) event.getEntity();
		if (target instanceof Player) {
			Player player = (Player)target;
			if (BossUtils.bossDamageBlocked(player, event.getDamage(), event.getDamager().getLocation()) && event.getCause() != DamageCause.MAGIC) {
				return;
			}
		}
		target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, true));
	}
}

