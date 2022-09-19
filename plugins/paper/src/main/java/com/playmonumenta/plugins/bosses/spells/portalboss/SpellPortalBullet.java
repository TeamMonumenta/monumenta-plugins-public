package com.playmonumenta.plugins.bosses.spells.portalboss;

import com.playmonumenta.plugins.bosses.bosses.PortalBoss;
import com.playmonumenta.plugins.bosses.spells.SpellBullet;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class SpellPortalBullet extends SpellBullet {

	private PortalBoss mPortalBoss;

	public SpellPortalBullet(Plugin plugin, LivingEntity boss, int cooldownTicks, PortalBoss portalBoss) {

		super(plugin, boss, new Vector(0, -3.0, 0), 150, 20, 3, 0.3, 30, 0.5, cooldownTicks, 120, "BORDER", 0, 0, 0, false, 0, (Entity entity, int tick) -> {
				float t = tick/10;
				if (tick % 5 == 0) {
					boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 2, t);
				}
			},
			(Entity entity) -> {
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 0);
			},
			Material.GOLD_BLOCK,
			(Player player, Location loc, boolean blocked) -> {
				if (!blocked) {
					BossUtils.blockableDamage(boss, player, DamageEvent.DamageType.PROJECTILE, 20, false, false, "Thunderstorm", null, 0, 0);
					boss.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 0);
				}
				boss.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0, 0, 0, 0.175);

			});

		mPortalBoss = portalBoss;
	}

	@Override
	public boolean canRun() {
		return !mPortalBoss.mIsHidden;
	}

}
