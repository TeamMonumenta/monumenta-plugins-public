package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

public class BasiliskPoison extends Ability {
	private static final int BASILISK_POISON_1_EFFECT_LVL = 1;
	private static final int BASILISK_POISON_2_EFFECT_LVL = 2;
	private static final int BASILISK_POISON_1_DURATION = 7 * 20;
	private static final int BASILISK_POISON_2_DURATION = 6 * 20;

	public BasiliskPoison(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 5;
		mInfo.specId = -1;
		mInfo.scoreboardId = "BasiliskPoison";
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		int basiliskPoison = getAbilityScore();
		int effectLvl = basiliskPoison == 1 ? BASILISK_POISON_1_EFFECT_LVL : BASILISK_POISON_2_EFFECT_LVL;
		int duration = basiliskPoison == 1 ? BASILISK_POISON_1_DURATION : BASILISK_POISON_2_DURATION;
		damagee.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, effectLvl, false, true));
		mWorld.spawnParticle(Particle.TOTEM, damagee.getLocation().add(0, 1.6, 0), 12, 0.4, 0.4, 0.4, 0.1);
		return true;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.TOTEM);
		return true;
	}
}
