package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public final class DebuffHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_debuffhit";
	public static final int detectionRange = 50;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DebuffHitBoss(plugin, boss);
	}

	public DebuffHitBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof LivingEntity) {
			LivingEntity target = (LivingEntity) event.getEntity();
			if (target instanceof Player) {
				Player player = (Player)target;
				if (BossUtils.bossDamageBlocked(player, event.getDamage(), event.getDamager().getLocation()) && event.getCause() != DamageCause.MAGIC) {
					return;
				}
			}
			int rand = FastUtils.RANDOM.nextInt(4);
			if (rand == 0) {
				target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0, false, true));
			} else if (rand == 1) {
				target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true));
			} else if (rand == 2) {
				target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, true));
			} else if (rand == 3) {
				target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0, false, true));
			}
		}
	}
}

