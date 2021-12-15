package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PotionUtils;


/**
 * @G3m1n1Boy
 * @deprecated use boss_onhit instead, like this:
 * <blockquote><pre>
 * /boss var Tags add boss_onhit
 * /boss var Tags add boss_onhit[EFFECTS=[(SLOW,80,1)]]
 * CARE this ability has some particle & sound, fix those too if you don't want the default values
 * </pre></blockquote>
 */
public final class IceAspectBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_iceaspect";

	public static class Parameters extends BossParameters {
		public int DETECTION = 50;
		public int SLOW_AMPLIFIER = 1;
		public int SLOW_DURATION = 80;
	}

	private final Parameters mParams;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new IceAspectBoss(plugin, boss);
	}

	public IceAspectBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(null, null, mParams.DETECTION, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player player) {
			if (BossUtils.bossDamageBlocked(player, event.getDamage(), event.getDamager().getLocation()) && event.getCause() != DamageCause.MAGIC) {
				return;
			}
		}
		PotionUtils.applyPotion(mBoss, (LivingEntity) event.getEntity(), new PotionEffect(PotionEffectType.SLOW, mParams.SLOW_DURATION, mParams.SLOW_AMPLIFIER, false, true));
	}
}
