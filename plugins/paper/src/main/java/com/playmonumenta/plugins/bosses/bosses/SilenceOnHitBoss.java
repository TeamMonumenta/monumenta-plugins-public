package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;


/**
 * @deprecated use boss_onhit instead, like this:
 *<blockquote><pre>
 * /boss var Tags add boss_onhit
 * /boss var Tags add boss_onhit[effects=[(silence,100)]]
 * CARE this ability has some particle & sound, fix those too if you don't want the default values
 * </pre></blockquote>
 * @G3m1n1Boy
 *
*/
@Deprecated
public class SilenceOnHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_silencehit";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) {
		return new SilenceOnHitBoss(plugin, boss);
	}

	public SilenceOnHitBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player) {
			if (BossUtils.bossDamageBlocked(player, mBoss.getLocation()) && event.getType() != DamageType.MAGIC) {
				return;
			}
			AbilityUtils.silencePlayer(player, 5 * 20);
		}

	}
}
