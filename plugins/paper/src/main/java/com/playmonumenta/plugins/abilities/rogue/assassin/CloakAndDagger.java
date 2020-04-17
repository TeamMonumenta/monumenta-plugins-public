package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

public class CloakAndDagger extends Ability implements KillTriggeredAbility {

	private static final String CLOAK_METADATA = "CloakAndDaggerPlayerIsInvisible";
	private static final double CLOAK_1_DAMAGE_MULTIPLIER = 1.5;
	private static final double CLOAK_2_DAMAGE_MULTIPLIER = 2.5;
	private static final int CLOAK_1_MAX_STACKS = 8;
	private static final int CLOAK_2_MAX_STACKS = 12;
	private static final int CLOAK_MIN_STACKS = 5;
	private static final int CLOAK_PENALTY_DURATION = 20 * 5;
	private static final int CLOAK_STACKS_ON_ELITE_KILL = 5;

	private final KillTriggeredAbilityTracker mTracker;

	private double mDamageMultiplier;
	private int mMaxStacks;
	private boolean active = false;
	private int mTickAttacked = 0;
	private int cloak = 0;
	private int cloakOnActivation = 0;

	public CloakAndDagger(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Cloak and Dagger");
		mInfo.scoreboardId = "CloakAndDagger";
		mInfo.mShorthandName = "CnD";
		mInfo.mDescriptions.add("When you kill an enemy you gain a stack of cloak. Elite kills and Boss \"kills\" give you five stacks. Stacks are capped at 8. When you shift right click while looking up with dual wielded swords, you lose your cloak stacks and gain X seconds of invisibility (Mobs won't target you) and (1.5)(X) extra damage on your next attack while invisible where X is the number of stacks you had at activation. You must have at least 5 stacks to activate this. Attacking with a sword or switching to any weapon that is not a sword cancels invisibility. If invisibility expires without attacking, you suffer from Mining Fatigue 2 for 5 seconds.");
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
		if (!active && cloak >= CLOAK_MIN_STACKS && mPlayer.isSneaking() && mPlayer.getLocation().getPitch() < -50) {
			cloakOnActivation = cloak;
			cloak = 0;
			mTickAttacked = mPlayer.getTicksLived();
			active = true;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.INVISIBILITY, 20 * cloakOnActivation, 0, false, true));
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
			mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 0.1);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), 64)) {
				if (mob instanceof Mob) {
					Mob m = (Mob) mob;
					if (m.getTarget() != null && m.getTarget().getUniqueId().equals(mPlayer.getUniqueId())) {
						m.setTarget(null);
					}
				}
			}
			mPlayer.setMetadata(CLOAK_METADATA, new FixedMetadataValue(mPlugin, null));
			new BukkitRunnable() {
				int t = 0;
				@Override
				public void run() {
					ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
					if (t >= 20 * cloakOnActivation || !active || !InventoryUtils.isSwordItem(mHand)) {
						if (active) {
							if (mPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)
								&& mPlayer.getPotionEffect(PotionEffectType.INVISIBILITY).getDuration() <= 400) {
								mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.INVISIBILITY);
							}
							mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
							                                 new PotionEffect(PotionEffectType.SLOW_DIGGING, CLOAK_PENALTY_DURATION, 1, false, true));
						}
						mPlayer.removeMetadata(CLOAK_METADATA, mPlugin);
						this.cancel();
						active = false;
						mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
						mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
						mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
						mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 0.1);
					}
					t++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return InventoryUtils.isSwordItem(mHand) && InventoryUtils.isSwordItem(oHand);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (active && mTickAttacked != mPlayer.getTicksLived() && event.getCause() == DamageCause.ENTITY_ATTACK) {
			active = false;
			if (mPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)
				&& mPlayer.getPotionEffect(PotionEffectType.INVISIBILITY).getDuration() <= 400) {
				mPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
			}
			event.setDamage(event.getDamage() + cloakOnActivation * mDamageMultiplier);
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
	public void triggerOnKill(Entity mob) {
		if (cloak < mMaxStacks) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				cloak = Math.min(mMaxStacks, cloak + CLOAK_STACKS_ON_ELITE_KILL);
			} else {
				cloak++;
			}
		}

		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Cloak stacks: " + cloak);
	}

	@Override
	public void entityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {
		if (active) {
			event.setCancelled(true);
			event.setTarget(null);
		}
	}

	public static boolean isInvisible(Player player) {
		return player.hasMetadata(CLOAK_METADATA);
	}
}
