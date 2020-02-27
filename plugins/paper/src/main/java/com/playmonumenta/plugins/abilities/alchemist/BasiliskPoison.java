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
import com.playmonumenta.plugins.utils.PotionUtils;

public class BasiliskPoison extends Ability {
	private static final int BASILISK_POISON_1_EFFECT_LVL = 1;
	private static final int BASILISK_POISON_2_EFFECT_LVL = 2;
	private static final int BASILISK_POISON_1_DURATION = 7 * 20;
	private static final int BASILISK_POISON_2_DURATION = 6 * 20;

	public BasiliskPoison(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Basilisk Poison");
		mInfo.scoreboardId = "BasiliskPoison";
		mInfo.mShorthandName = "BP";
		mInfo.mDescriptions.add("Equips your arrows with a noxious mixture that afflicts targets with 7s of Wither II.");
		mInfo.mDescriptions.add("The debuff is improved to 6s of Wither III.");
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		apply(damagee);
		mWorld.spawnParticle(Particle.TOTEM, damagee.getLocation().add(0, 1.6, 0), 12, 0.4, 0.4, 0.4, 0.1);
		return true;
	}

	/* This is used by other abilities to apply this player's basilisk poison to a mob if needed */
	public void apply(LivingEntity entity) {
		int basiliskPoison = getAbilityScore();
		int effectLvl = basiliskPoison == 1 ? BASILISK_POISON_1_EFFECT_LVL : BASILISK_POISON_2_EFFECT_LVL;
		int duration = basiliskPoison == 1 ? BASILISK_POISON_1_DURATION : BASILISK_POISON_2_DURATION;
		PotionUtils.applyPotion(mPlayer, entity, new PotionEffect(PotionEffectType.WITHER, duration, effectLvl, false, true));
	}

	@Override
	public boolean playerShotArrowEvent(Arrow arrow) {
		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.TOTEM);
		return true;
	}
}
