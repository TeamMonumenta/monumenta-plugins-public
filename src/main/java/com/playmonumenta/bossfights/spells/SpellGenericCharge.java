package com.playmonumenta.bossfights.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;

public class SpellGenericCharge extends BaseCharge
{
	public SpellGenericCharge(Plugin plugin, LivingEntity boss, int range, float damage)
	{
		super(plugin, boss, range, 25,
		      // Warning sound/particles at boss location and slow boss
		      (Player player) ->
			    {
		            boss.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0);
					boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 4), true);
		            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);
				},
		      // Warning particles
		      (Location loc) ->
			  	{
		            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 1, 1, 1, 0);
				},
		      // Charge attack sound/particles at boss location
		      (Player player) ->
			    {
		            boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 100, 2, 2, 2, 0);
		            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1f, 1.5f);
				},
		      // Attack hit a player
		      (Player player) ->
			    {
		            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation(), 80, 1, 1, 1, 0);
					player.damage(damage, boss);
				},
		      // Attack particles
		      (Location loc) ->
			  	{
		            loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0.02, 0.02, 0.02, 0);
				},
		      // Ending particles on boss
		      () ->
			  	{
		            boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 200, 2, 2, 2, 0);
		        });
	}
}
