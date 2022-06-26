package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;



public class Rampage extends Ability implements AbilityWithChargesOrStacks {

	private static final int RAMPAGE_STACK_DECAY_TIME = 20 * 5;
	private static final int RAMPAGE_1_DAMAGE_PER_STACK = 50;
	private static final int RAMPAGE_2_DAMAGE_PER_STACK = 35;
	private static final int RAMPAGE_1_STACK_LIMIT = 15;
	private static final int RAMPAGE_2_STACK_LIMIT = 20;
	private static final double RAMPAGE_DAMAGE_RESISTANCE_STACK_RATIO = 1.0;
	private static final double RAMPAGE_RADIUS = 4;
	private static final double HEAL_PERCENT = 0.025;
	private static final double RAMPAGE_STACK_PERCENTAGE = 1.5;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "RampagePercentDamageResistEffect";

	private final int mDamagePerStack;
	private final int mStackLimit;

	private int mStacks = 0;
	private double mRemainderDamage = 0;
	private int mTimeToStackDecay = 0;
	private int mTimer = 0;

	public Rampage(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Rampage");
		mInfo.mLinkedSpell = ClassAbility.RAMPAGE;
		mInfo.mCooldown = 0;
		mInfo.mScoreboardId = "Rampage";
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mShorthandName = "Rmp";
		mInfo.mDescriptions.add("Gain a stack of rage for each 50 melee damage dealt. Stacks decay by 1 every 5 seconds of not dealing melee damage and cap at 15. Passively gain 1% damage resistance for each stack. When at 10 or more stacks, right click while looking down to consume all stacks and damage mobs in a 4 block radius by 1.5 times the number of stacks consumed. For the next (stacks consumed / 2) seconds, heal 2.5% of max health per second and keep your passive damage reduction.");
		mInfo.mDescriptions.add("Gain a stack of rage for each 35 melee damage dealt, with stacks capping at 20.");
		mDisplayItem = new ItemStack(Material.BLAZE_POWDER, 1);
		mDamagePerStack = getAbilityScore() == 1 ? RAMPAGE_1_DAMAGE_PER_STACK : RAMPAGE_2_DAMAGE_PER_STACK;
		mStackLimit = getAbilityScore() == 1 ? RAMPAGE_1_STACK_LIMIT : RAMPAGE_2_STACK_LIMIT;
	}

	@Override
	public void cast(Action action) {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (ItemUtils.isSomeBow(inMainHand) || ItemUtils.isSomePotion(inMainHand) || inMainHand.getType().isBlock()
				|| inMainHand.getType().isEdible() || inMainHand.getType() == Material.TRIDENT) {
			return;
		}

		Location loc = mPlayer.getLocation();

		if (mStacks >= 10 && loc.getPitch() > 70) {
			World world = mPlayer.getWorld();
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RAMPAGE_RADIUS)) {
				DamageUtils.damage(mPlayer, mob, DamageType.WARRIOR_AOE, mStacks * RAMPAGE_STACK_PERCENTAGE, mInfo.mLinkedSpell);
				new PartialParticle(Particle.VILLAGER_ANGRY, mob.getLocation(), 5, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);
			}

			mTimer = mStacks / 2;
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(mStacks / 10, mStacks / -100.0));
			new PartialParticle(Particle.EXPLOSION_HUGE, loc, 3, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 50, 3, 1, 3, 0).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, mStacks * 0.4f, 0.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, mStacks * 0.4f, 1.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, mStacks * 0.4f, 2);

			mStacks = 0;
			MessagingUtils.sendActionBarMessage(mPlayer, "Rage: " + mStacks);
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0) {
			mTimeToStackDecay += 5;

			if (mTimeToStackDecay >= RAMPAGE_STACK_DECAY_TIME) {
				mTimeToStackDecay = 0;
				mStacks--;
				MessagingUtils.sendActionBarMessage(mPlayer, "Rage: " + mStacks);
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}

		if (oneSecond) {
			if (mTimer > 0) {
				mTimer--;
				double maxHealth = EntityUtils.getMaxHealth(mPlayer);
				PlayerUtils.healPlayer(mPlugin, mPlayer, HEAL_PERCENT * maxHealth);
				new PartialParticle(Particle.HEART, mPlayer.getLocation().add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001).spawnAsPlayerActive(mPlayer);
			}
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if ((event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH)
			    && event.getAbility() != ClassAbility.RAMPAGE) {
			damageDealt(event.getFinalDamage(false));
		}
		return false; // does not deal damage, just tallies the damage dealt
	}

	private void damageDealt(double damage) {
		mTimeToStackDecay = 0;

		mRemainderDamage += damage;
		int newStacks = (int)(mRemainderDamage / mDamagePerStack);
		mRemainderDamage -= (newStacks * mDamagePerStack);

		if (newStacks > 0) {
			int previousStacks = mStacks;
			mStacks = Math.min(mStackLimit, mStacks + newStacks);
			MessagingUtils.sendActionBarMessage(mPlayer, "Rage: " + mStacks);
			if (mStacks != previousStacks) {
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		event.setDamage(event.getDamage() * (1 - mStacks * RAMPAGE_DAMAGE_RESISTANCE_STACK_RATIO / 100.0));
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mStackLimit;
	}

}
