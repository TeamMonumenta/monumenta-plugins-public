package com.playmonumenta.plugins.abilities.other;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.EvasionEvent;

public class EvasionEnchant extends Ability {

	public double chance = 0;

	public EvasionEnchant(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(evade(mPlayer, event.getDamage()));
		return true;
	}

	public boolean PlayerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(evade(mPlayer, event.getDamage()));
		return true;
	}

	private double evade(Player player, double damage) {
		double changedDamage = damage;
		if (chance >= 200) {
			chance -= 200;
			changedDamage *= 0.25;
			Location loc = player.getLocation();
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 80, 0.25, 0.45, 0.25, 0.1);
			mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1.5f);
			mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 2f);
			EvasionEvent event = new EvasionEvent(player);
			Bukkit.getPluginManager().callEvent(event);
		} else if (chance >= 100) {
			chance -= 100;
			changedDamage *= 0.5;
			Location loc = player.getLocation();
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 80, 0.25, 0.45, 0.25, 0.1);
			mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1.5f);
			EvasionEvent event = new EvasionEvent(player);
			Bukkit.getPluginManager().callEvent(event);
		}
		return changedDamage;
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

}
