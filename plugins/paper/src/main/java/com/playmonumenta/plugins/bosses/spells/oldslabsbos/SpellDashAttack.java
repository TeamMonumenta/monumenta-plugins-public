package com.playmonumenta.plugins.bosses.spells.oldslabsbos;

import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellDashAttack extends SpellBaseCharge {

	public SpellDashAttack(Plugin plugin, LivingEntity boss, int range, float damage) {
		super(plugin, boss, range, 160, 30, false,
			// Warning sound/particles at boss location and slow boss
			(LivingEntity player) -> {
				new PartialParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0).spawnAsEntityActive(boss);
				boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.15f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_VINDICATOR_AMBIENT, 1f, 0.85f);
			},
			// Warning particles
			(Location loc) -> {
				new PartialParticle(Particle.CRIT, loc, 2, 0.65, 0.65, 0.65, 0).spawnAsEntityActive(boss);
			},
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				new PartialParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 125, 0.4, 0.4, 0.4, 0.25).spawnAsEntityActive(boss);
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 45, 0.15, 0.4, 0.15, 0.15).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.4f);
			},
			// Attack hit a player
			(LivingEntity player) -> {
				new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData()).spawnAsEntityActive(boss);
				new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 12, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData()).spawnAsEntityActive(boss);
				BossUtils.blockableDamage(boss, player, DamageType.MELEE, damage, "Charge", boss.getLocation());
			},
			// Attack particles
			(Location loc) -> {
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 4, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(boss);
				new PartialParticle(Particle.CRIT, loc, 8, 0.5, 0.5, 0.5, 0.75).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SMOKE_NORMAL, loc, 12, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(boss);
			},
			// Ending particles on boss
			() -> {
				new PartialParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 125, 0.4, 0.4, 0.4, 0.25).spawnAsEntityActive(boss);
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 45, 0.15, 0.4, 0.15, 0.15).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.1f, 1.5f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.4f);
			});
	}

}
