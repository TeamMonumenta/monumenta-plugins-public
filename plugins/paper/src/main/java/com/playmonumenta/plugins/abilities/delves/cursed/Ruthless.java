package com.playmonumenta.plugins.abilities.delves.cursed;

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
 * RUTHLESS: You take x2 more damage and deal x2 less damage.
 */

public class Ruthless extends Ability {

	private static final int RUTHLESS_CHALLENGE_SCORE = 11;
	private static final double RUTHLESS_DAMAGE_TAKEN_MULTIPLIER = 2;
	private static final double RUTHLESS_DAMAGE_DEALT_MULTIPLIER = 0.5;

	public Ruthless(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
		mInfo.mIgnoreTriggerCap = true;
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, RUTHLESS_CHALLENGE_SCORE);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, RUTHLESS_DAMAGE_TAKEN_MULTIPLIER));
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, RUTHLESS_DAMAGE_TAKEN_MULTIPLIER));
		return true;
	}

	@Override
	public void playerDamagedByBossEvent(BossAbilityDamageEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(mPlayer.getAttribute(Attribute.GENERIC_ARMOR).getValue(),
				mPlayer.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue(), event.getDamage(), RUTHLESS_DAMAGE_TAKEN_MULTIPLIER));
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity le, EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() * RUTHLESS_DAMAGE_DEALT_MULTIPLIER);
		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() != DamageCause.CUSTOM) {
			event.setDamage(event.getDamage() * RUTHLESS_DAMAGE_DEALT_MULTIPLIER);
		}
		return true;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		event.setDamage(event.getDamage() * RUTHLESS_DAMAGE_DEALT_MULTIPLIER);
	}

	@Override
	public void playerDealtUnregisteredCustomDamageEvent(CustomDamageEvent event) {
		event.setDamage(event.getDamage() * RUTHLESS_DAMAGE_DEALT_MULTIPLIER);
	}

}
