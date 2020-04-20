package com.playmonumenta.plugins.abilities.delves.twisted;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.BossUtils.BossAbilityDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * MERCILESS: You take x3 more damage and deal x3 less damage.
 */

public class Merciless extends Ability {

	private static final int MERCILESS_CHALLENGE_SCORE = 21;
	private static final double MERCILESS_DAMAGE_TAKEN_MULTIPLIER = 3;
	private static final double MERCILESS_DAMAGE_DEALT_MULTIPLIER = 0.333;

	public Merciless(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
		mInfo.mIgnoreTriggerCap = true;
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, MERCILESS_CHALLENGE_SCORE);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, MERCILESS_DAMAGE_TAKEN_MULTIPLIER));
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, MERCILESS_DAMAGE_TAKEN_MULTIPLIER));
		return true;
	}

	@Override
	public void playerDamagedByBossEvent(BossAbilityDamageEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(mPlayer.getAttribute(Attribute.GENERIC_ARMOR).getValue(),
				mPlayer.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue(), event.getDamage(), MERCILESS_DAMAGE_TAKEN_MULTIPLIER));
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() != DamageCause.CUSTOM) {
			event.setDamage(event.getDamage() * MERCILESS_DAMAGE_DEALT_MULTIPLIER);
		}
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity le, EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() * MERCILESS_DAMAGE_DEALT_MULTIPLIER);
		return true;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		event.setDamage(event.getDamage() * MERCILESS_DAMAGE_DEALT_MULTIPLIER);
	}

	@Override
	public void playerDealtUnregisteredCustomDamageEvent(CustomDamageEvent event) {
		event.setDamage(event.getDamage() * MERCILESS_DAMAGE_DEALT_MULTIPLIER);
	}



}
