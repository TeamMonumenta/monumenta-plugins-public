package com.playmonumenta.plugins.abilities.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class CloakAndDagger extends Ability implements KillTriggeredAbility, AbilityWithChargesOrStacks {

	private static final double CLOAK_1_DAMAGE_MULTIPLIER = 2;
	private static final double CLOAK_2_DAMAGE_MULTIPLIER = 3;
	private static final int CLOAK_1_MAX_STACKS = 8;
	private static final int CLOAK_2_MAX_STACKS = 12;
	private static final int CLOAK_MIN_STACKS = 5;
	private static final int CLOAK_STACKS_ON_ELITE_KILL = 5;
	private static final int STEALTH_DURATION = (int)(2.5 * 20);
	private static final int BOSS_DAMAGE_THRESHOLD = 300;

	public static final String CHARM_DAMAGE = "Cloak And Dagger Damage";
	public static final String CHARM_STACKS = "Cloak And Dagger Max Stacks";
	public static final String CHARM_STEALTH = "Cloak And Dagger Stealth Duration";

	private final KillTriggeredAbilityTracker mTracker;

	private final double mDamageMultiplier;
	private final int mMaxStacks;
	private int mCloak = 0;
	private int mCloakOnActivation = 0;
	private boolean mActive = false;

	public CloakAndDagger(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Cloak and Dagger");
		mInfo.mScoreboardId = "CloakAndDagger";
		mInfo.mShorthandName = "CnD";
		mInfo.mDescriptions.add(
			String.format("When you kill an enemy you gain a stack of cloak. Elite kills and Boss \"kills\" give you %s stacks (every %s damage to them). Stacks are capped at %s. When you sneak left click while looking up with dual wielded swords, you lose your cloak stacks and gain %s seconds of Stealth and (%s * X) extra damage on your next stealth attack, where X is the number of stacks you had at activation. You must have at least %s stacks to activate this.",
				CLOAK_STACKS_ON_ELITE_KILL,
				BOSS_DAMAGE_THRESHOLD,
				CLOAK_1_MAX_STACKS,
				STEALTH_DURATION / 20.0,
				(int)CLOAK_1_DAMAGE_MULTIPLIER,
				CLOAK_MIN_STACKS));
		mInfo.mDescriptions.add(
			String.format("Cloak stacks are now capped at %s and bonus damage is increased to (%s * X) where X is the number of stacks you have upon activating this skill.",
				CLOAK_2_MAX_STACKS,
				(int)CLOAK_2_DAMAGE_MULTIPLIER));
		mInfo.mLinkedSpell = ClassAbility.CLOAK_AND_DAGGER;
		mInfo.mCooldown = 0;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.IRON_SWORD, 1);
		mDamageMultiplier = (isLevelOne() ? CLOAK_1_DAMAGE_MULTIPLIER : CLOAK_2_DAMAGE_MULTIPLIER) + CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE);
		mMaxStacks = (isLevelOne() ? CLOAK_1_MAX_STACKS : CLOAK_2_MAX_STACKS) + (int) CharmManager.getLevel(player, CHARM_STACKS);
		mTracker = new KillTriggeredAbilityTracker(this, BOSS_DAMAGE_THRESHOLD);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer != null
				&& !AbilityUtils.isStealthed(mPlayer) && mCloak >= CLOAK_MIN_STACKS
				&& mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50
				&& InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			mCloakOnActivation = mCloak;
			mCloak = 0;
			mActive = true;
			AbilityUtils.applyStealth(mPlugin, mPlayer, STEALTH_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_STEALTH));
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
			new PartialParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsPlayerActive(mPlayer);

			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		mTracker.updateDamageDealtToBosses(event);
		if (AbilityUtils.isStealthed(mPlayer) && (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH) && mActive) {
			AbilityUtils.removeStealth(mPlugin, mPlayer, false);
			if (InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
				DamageUtils.damage(mPlayer, enemy, DamageType.MELEE_SKILL, mCloakOnActivation * mDamageMultiplier, mInfo.mLinkedSpell, true);

				Location loc = enemy.getLocation();
				World world = mPlayer.getWorld();
				world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, 1f, 2f);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
				new PartialParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 25, 0.25, 0.5, 0.25, 0.2f).spawnAsPlayerActive(mPlayer);
			}

			mActive = false;
		}
		return false; // only tallies damage done
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		triggerOnKill(event.getEntity());
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayer != null && mActive) {
			if (!AbilityUtils.isStealthed(mPlayer)) {
				mActive = false;
			}
		}
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		if (mPlayer == null) {
			return;
		}
		if (mCloak < mMaxStacks) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				mCloak = Math.min(mMaxStacks, mCloak + CLOAK_STACKS_ON_ELITE_KILL);
			} else {
				mCloak++;
			}
			ClientModHandler.updateAbility(mPlayer, this);
		}

		MessagingUtils.sendActionBarMessage(mPlayer, "Cloak stacks: " + mCloak);
	}

	@Override
	public int getCharges() {
		return mCloak;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}

}
