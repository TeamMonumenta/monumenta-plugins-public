package com.playmonumenta.plugins.abilities.warrior.berserker;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Gain a stack of rage for each 25 / 15 melee damage you deal,
 * capped at 15 / 20. Each stack grants passive 1% damage reduction.
 * Whenever at 10 or more stacks, RClick while looking down to
 * consume all stacks, dealing stacks consumed damage to enemies in
 * a 4 block radius and healing by ⅓ of stacks consumed. Stacks fall
 * off one at a time after 5 seconds of not dealing melee damage.
 */

public class Rampage extends Ability {

	private static final int RAMPAGE_STACK_DECAY_TIME = 20 * 5;
	private static final int RAMPAGE_1_DAMAGE_PER_STACK = 25;
	private static final int RAMPAGE_2_DAMAGE_PER_STACK = 15;
	private static final int RAMPAGE_1_STACK_LIMIT = 15;
	private static final int RAMPAGE_2_STACK_LIMIT = 20;
	private static final double RAMPAGE_HEAL_STACK_RATIO = 1.0 / 3;
	private static final double RAMPAGE_RADIUS = 4;

	private final int mDamagePerStack;
	private final int mStackLimit;

	private int mStacks = 0;
	private int mRemainderDamage = 0;
	private int mTimeToStackDecay = 0;

	public Rampage(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Rampage");
		mInfo.linkedSpell = Spells.RAMPAGE;
		mInfo.cooldown = 0;
		mInfo.scoreboardId = "Rampage";
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mShorthandName = "Rmp";
		mInfo.mDescriptions.add("Gain a stack of rage for each 25 melee damage dealt. Stacks decay by 1 every 5 seconds of not dealing melee damage and cap at 15. Passively gain 1% damage reduction (before armor calculations) for each stack. When at 10 or more stacks, right click while looking down to consume all stacks and damage mobs in a 4 block radius by stacks consumed and heal self by 1/3 stacks consumed.");
		mInfo.mDescriptions.add("Gain a stack of rage for each 15 melee damage dealt, with stacks capping at 20.");
		mDamagePerStack = getAbilityScore() == 1 ? RAMPAGE_1_DAMAGE_PER_STACK : RAMPAGE_2_DAMAGE_PER_STACK;
		mStackLimit = getAbilityScore() == 1 ? RAMPAGE_1_STACK_LIMIT : RAMPAGE_2_STACK_LIMIT;
	}

	@Override
	public void cast(Action action) {
		Location loc = mPlayer.getLocation();

		if (mStacks >= 10 && loc.getPitch() > 70) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RAMPAGE_RADIUS)) {
				EntityUtils.damageEntity(mPlugin, mob, mStacks, mPlayer);
				mWorld.spawnParticle(Particle.VILLAGER_ANGRY, mob.getLocation(), 5, 0, 0, 0, 0.1);
			}

			PlayerUtils.healPlayer(mPlayer, mStacks * RAMPAGE_HEAL_STACK_RATIO);
			mWorld.spawnParticle(Particle.EXPLOSION_HUGE, loc, 3, 0.2, 0.2, 0.2, 0);
			mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 50, 3, 1, 3, 0);
			mWorld.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, mStacks * 0.4f, 0.5f);
			mWorld.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, mStacks * 0.4f, 1.5f);
			mWorld.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, mStacks * 0.4f, 2);

			mStacks = 0;
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Rage: " + mStacks);
		}
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0) {
			mTimeToStackDecay += 5;

			if (mTimeToStackDecay >= RAMPAGE_STACK_DECAY_TIME) {
				mTimeToStackDecay = 0;
				mStacks--;
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Rage: " + mStacks);
			}
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			mTimeToStackDecay = 0;

			mRemainderDamage += event.getFinalDamage();
			int newStacks = mRemainderDamage / mDamagePerStack;
			mRemainderDamage %= mDamagePerStack;

			if (newStacks > 0) {
				mStacks = Math.min(mStackLimit, mStacks + newStacks);
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Rage: " + mStacks);
			}
		}

		return true;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() * (1 - mStacks / 100.0));
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(event.getDamage() * (1 - mStacks / 100.0));
		return true;
	}

}

