package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;

public class KamikazeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_kamikaze";
	public static final int detectionRange = 30;

	LivingEntity mBoss;
	Plugin mPlugin;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new KamikazeBoss(plugin, boss);
	}

	public KamikazeBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		mPlugin = plugin;
		List<Spell> passiveSpells = Arrays.asList(
				new SpellRunAction(() -> mBoss.getLocation().getWorld().spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().clone().add(new Location(mBoss.getWorld(),0,1,0)), 2, 0.5, 1, 0.5, 0))
			);
		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player) {
			Entity damager = event.getDamager();
			if (damager instanceof Damageable) {
				((Damageable) damager).setHealth(0);
				World world = damager.getWorld();
				world.playSound(event.getDamager().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.7f);
				world.spawnParticle(Particle.EXPLOSION_LARGE, event.getDamager().getLocation(), 10, 0.5, 1, 0.5, 0.05);
			}
		}
	}

	// This exists because "bossDamagedEntity()" is bugged. Doesn't work with projectiles.
	@Override
	public void bossProjectileHit(ProjectileHitEvent event) {
		if (event.getHitEntity() instanceof Player) {
			if (event.getEntity().getShooter() instanceof Damageable) {
				Damageable entity = (Damageable) event.getEntity().getShooter();
				entity.setHealth(0);
				World world = event.getEntity().getWorld();
				world.playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.7f);
				world.spawnParticle(Particle.EXPLOSION_LARGE, entity.getLocation(), 10, 0.5, 1, 0.5, 0.05);
			}
		}
	}
}
