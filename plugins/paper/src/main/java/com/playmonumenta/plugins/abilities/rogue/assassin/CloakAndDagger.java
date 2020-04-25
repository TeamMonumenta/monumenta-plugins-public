package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

public class CloakAndDagger extends Ability implements KillTriggeredAbility {

	private static final double CLOAK_1_DAMAGE_MULTIPLIER = 1.5;
	private static final double CLOAK_2_DAMAGE_MULTIPLIER = 2.5;
	private static final int CLOAK_1_MAX_STACKS = 8;
	private static final int CLOAK_2_MAX_STACKS = 12;
	private static final int CLOAK_MIN_STACKS = 5;
	private static final int CLOAK_STACKS_ON_ELITE_KILL = 5;

	private final KillTriggeredAbilityTracker mTracker;

	private double mDamageMultiplier;
	private int mMaxStacks;
	private int cloak = 0;
	private int cloakOnActivation = 0;
	private boolean active = false;

	public CloakAndDagger(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Cloak and Dagger");
		mInfo.scoreboardId = "CloakAndDagger";
		mInfo.mShorthandName = "CnD";
		mInfo.mDescriptions.add("When you kill an enemy you gain a stack of cloak. Elite kills and Boss \"kills\" give you five stacks. Stacks are capped at 8. When you shift right click while looking up with dual wielded swords, you lose your cloak stacks and gain X seconds of Stealth and (1.5)(X) extra damage on your next stealth attack, where X is the number of stacks you had at activation. You must have at least 5 stacks to activate this.");
		mInfo.mDescriptions.add("Cloak stacks are now capped at 12 and bonus damage is increased to (2.5)(X) where X is the number of stacks you have upon activating this skill.");
		mInfo.linkedSpell = Spells.CLOAK_AND_DAGGER;
		mInfo.cooldown = 0;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mDamageMultiplier = getAbilityScore() == 1 ? CLOAK_1_DAMAGE_MULTIPLIER : CLOAK_2_DAMAGE_MULTIPLIER;
		mMaxStacks = getAbilityScore() == 1 ? CLOAK_1_MAX_STACKS : CLOAK_2_MAX_STACKS;
		mTracker = new KillTriggeredAbilityTracker(this);
	}

	@Override
	public void cast(Action action) {
		if (!AbilityUtils.isStealthed(mPlayer) && cloak >= CLOAK_MIN_STACKS && mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50) {
			cloakOnActivation = cloak;
			cloak = 0;
			active = true;
			AbilityUtils.applyStealth(mPlugin, mPlayer, 20 * cloakOnActivation);
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
			mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 0.1);
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return InventoryUtils.isSwordItem(mHand) && InventoryUtils.isSwordItem(oHand);
	}

	@Override
	public boolean onStealthAttack(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && active) {
			AbilityUtils.removeStealth(mPlugin, mPlayer, false);
			event.setDamage(event.getDamage() + cloakOnActivation * mDamageMultiplier);

			Location loc = event.getEntity().getLocation();

			mWorld.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, 1f, 2f);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 25, 0.25, 0.5, 0.25, 0.2f);

			active = false;
		}

		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		triggerOnKill(event.getEntity());
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (active) {
			if (!AbilityUtils.isStealthed(mPlayer)) {
				active = false;
			}
		}
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		if (cloak < mMaxStacks) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				cloak = Math.min(mMaxStacks, cloak + CLOAK_STACKS_ON_ELITE_KILL);
			} else {
				cloak++;
			}
		}

		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Cloak stacks: " + cloak);
	}
}
