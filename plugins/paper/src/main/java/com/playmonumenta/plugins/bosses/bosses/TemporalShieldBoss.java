package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.TemporalFlux;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class TemporalShieldBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_temporalshield";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TemporalShieldBoss(plugin, boss);
	}

	public TemporalShieldBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		EffectManager manager = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager;
		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				List<LivingEntity> mobs = EntityUtils.getNearbyMobs(boss.getLocation(), detectionRange, detectionRange, detectionRange);
				for (Player p : PlayerUtils.playersInRange(boss.getLocation(), 40, true)) {
					if (manager.hasEffect(p, TemporalFlux.class)) {
						p.spawnParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 10, 1, 1, 1, 0.01);
					}
				}
				for (LivingEntity e : mobs) {
					if (e.getScoreboardTags().contains("shieldReceiver")) {
						new PPLine(Particle.TOTEM, boss.getEyeLocation(), e.getEyeLocation().add(0, -1, 0)).spawnAsBoss();
					}
				}
			})
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (!(damager instanceof Player)) {
			return;
		}
		EffectManager manager = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager;
		if (!manager.hasEffect(damager, TemporalFlux.class)) {
			event.setCancelled(true);
		}
	}

	@Override
	public void death(EntityDeathEvent event) {
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), detectionRange, detectionRange, detectionRange);
		for (LivingEntity e : mobs) {
			if (e.getScoreboardTags().contains("shieldReceiver")) {
				e.setInvulnerable(false);
			}
		}
	}
}
