package com.playmonumenta.plugins.abilities.warrior.berserker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;



public class Rampage extends Ability implements AbilityWithChargesOrStacks {

	private static final int RAMPAGE_STACK_DECAY_TIME = 20 * 5;
	private static final int RAMPAGE_1_DAMAGE_PER_STACK = 40;
	private static final int RAMPAGE_2_DAMAGE_PER_STACK = 25;
	private static final int RAMPAGE_1_STACK_LIMIT = 15;
	private static final int RAMPAGE_2_STACK_LIMIT = 20;
	private static final double RAMPAGE_DAMAGE_RESISTANCE_STACK_RATIO = 1.0;
	private static final double RAMPAGE_RADIUS = 4;
	private static final double HEAL_PERCENT = 0.05;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "RampagePercentDamageResistEffect";

	private final int mDamagePerStack;
	private final int mStackLimit;

	private int mStacks = 0;
	private int mRemainderDamage = 0;
	private int mTimeToStackDecay = 0;
	private int mTimer = 0;

	public Rampage(Plugin plugin, Player player) {
		super(plugin, player, "Rampage");
		mInfo.mLinkedSpell = ClassAbility.RAMPAGE;
		mInfo.mCooldown = 0;
		mInfo.mScoreboardId = "Rampage";
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mShorthandName = "Rmp";
		mInfo.mDescriptions.add("Gain a stack of rage for each 40 melee damage dealt. Stacks decay by 1 every 5 seconds of not dealing melee damage and cap at 15. Passively gain 1% damage resistance for each stack. When at 10 or more stacks, right click while looking down to consume all stacks and damage mobs in a 4 block radius by stacks consumed. For the next (stacks consumed / 2) seconds, heal 5% of max health per second and keep your passive damage reduction.");
		mInfo.mDescriptions.add("Gain a stack of rage for each 25 melee damage dealt, with stacks capping at 20.");
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
				EntityUtils.damageEntity(mPlugin, mob, mStacks, mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);
				world.spawnParticle(Particle.VILLAGER_ANGRY, mob.getLocation(), 5, 0, 0, 0, 0.1);
			}

			mTimer = mStacks / 2;
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(mTimer, mStacks / -100.0));
			world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 3, 0.2, 0.2, 0.2, 0);
			world.spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 50, 3, 1, 3, 0);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, mStacks * 0.4f, 0.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, mStacks * 0.4f, 1.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, mStacks * 0.4f, 2);

			mStacks = 0;
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Rage: " + mStacks);
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
				MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Rage: " + mStacks);
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}

		if (oneSecond) {
			if (mTimer > 0) {
				mTimer--;
				double maxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				PlayerUtils.healPlayer(mPlayer, HEAL_PERCENT * maxHealth);
				mPlayer.getWorld().spawnParticle(Particle.HEART, (mPlayer.getLocation()).add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001);
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
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}

		return true;
	}

	public void customRecklessSwingInteraction(double swingDamage) {
		mTimeToStackDecay = 0;

		mRemainderDamage += swingDamage;
		int newStacks = mRemainderDamage / mDamagePerStack;
		mRemainderDamage %= mDamagePerStack;

		if (newStacks > 0) {
			mStacks = Math.min(mStackLimit, mStacks + newStacks);
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Rage: " + mStacks);
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, 1 - mStacks * RAMPAGE_DAMAGE_RESISTANCE_STACK_RATIO / 100.0));
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, 1 - mStacks * RAMPAGE_DAMAGE_RESISTANCE_STACK_RATIO / 100.0));
		return true;
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
