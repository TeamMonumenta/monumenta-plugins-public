package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;



public class LuminousInfusion extends Ability {
	public static final int DAMAGE_1 = 10;
	public static final int DAMAGE_UNDEAD_1 = 20;
	public static final double DAMAGE_MULTIPLIER_2 = 0.2;

	private final boolean mDoMultiplierAndFire;

	// Passive damage to share with Holy Javelin
	public double mLastPassiveMeleeDamage = 0;
	public double mLastPassiveDJDamage = 0;

	private static final String EXPIRATION_MESSAGE = "The light from your hands fades...";
	private static final double RADIUS = 4;
	private static final int FIRE_DURATION_2 = 20 * 3;
	private static final int COOLDOWN = 20 * 14;
	private static final float KNOCKBACK_SPEED = 0.7f;

	private boolean mActive = false;

	private Crusade mCrusade;

	public LuminousInfusion(Plugin plugin, Player player) {
		super(plugin, player, "Luminous Infusion");
		mInfo.mLinkedSpell = ClassAbility.LUMINOUS_INFUSION;
		mInfo.mScoreboardId = "LuminousInfusion";
		mInfo.mShorthandName = "LI";
		mInfo.mDescriptions.add("While sneaking, pressing the swap key charges your hands with holy light. The next time you damage an undead enemy, your attack is infused with explosive power, dealing 20 holy damage to it and all enemies in a 4-block cube around it, or 10 against non-undead, and knocking other enemies away from it. Swapping hands no longer does its vanilla function. Cooldown: 14s.");
		mInfo.mDescriptions.add("Your melee attacks now passively deal 20% holy damage to undead enemies, and Divine Justice now passively deals 20% more total damage. Damaging an undead enemy now passively sets it on fire for 3s. That holy damage ignores iframes.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mIgnoreTriggerCap = true;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.BLAZE_POWDER, 1);

		mDoMultiplierAndFire = getAbilityScore() == 2;

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbility(mPlayer, Crusade.class);
			});
		}
	}

	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (
			!isTimerActive()
			&& mPlayer.isSneaking()
		) {
			putOnCooldown();

			mActive = true;
			MessagingUtils.sendActionBarMessage(Plugin.getInstance(), mPlayer, "Holy energy radiates from your hands...");

			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1.65f);
			world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.75f, 0.25f, 0.75f, 1);
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					mT++;
					Location rightHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.45).subtract(0, .8, 0);
					Location leftHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.45).subtract(0, .8, 0);
					world.spawnParticle(Particle.SPELL_INSTANT, leftHand, 1, 0.05f, 0.05f, 0.05f, 0);
					world.spawnParticle(Particle.SPELL_INSTANT, rightHand, 1, 0.05f, 0.05f, 0.05f, 0);
					if (mT >= COOLDOWN || !mActive) {
						if (mT >= COOLDOWN) {
							MessagingUtils.sendActionBarMessage(Plugin.getInstance(), mPlayer, EXPIRATION_MESSAGE);
						}
						mActive = false;
						this.cancel();
					}
				}
			}.runTaskTimer(Plugin.getInstance(), 1, 1);
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		//TODO pass in casted entities for events like these
		LivingEntity enemy = (LivingEntity)event.getEntity();
		boolean enemyTriggersAbilities = Crusade.enemyTriggersAbilities(enemy, mCrusade);

		// Do explosion first, then bypass iframes for passive
		if (mActive && enemyTriggersAbilities) {
			execute(enemy, event);
		}

		if (
			mDoMultiplierAndFire
			&& event.getCause() == DamageCause.ENTITY_ATTACK
			&& enemyTriggersAbilities
		) {
			EntityUtils.applyFire(Plugin.getInstance(), FIRE_DURATION_2, enemy, mPlayer);

			double originalDamage = event.getDamage();
			// Store the raw pre-event damage.
			// When it is used by Holy Javelin later,
			// the custom damage event will fire including this raw damage,
			// then event processing runs for it from there
			mLastPassiveMeleeDamage = originalDamage * DAMAGE_MULTIPLIER_2;
			EntityUtils.damageEntity(
				Plugin.getInstance(),
				enemy,
				mLastPassiveMeleeDamage,
				mPlayer,
				MagicType.HOLY,
				true,
				mInfo.mLinkedSpell,
				true,
				true,
				true
			);
		}

		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		boolean enemyTriggersAbilities = Crusade.enemyTriggersAbilities(damagee, mCrusade);
		if (mDoMultiplierAndFire && enemyTriggersAbilities) {
			EntityUtils.applyFire(Plugin.getInstance(), FIRE_DURATION_2, damagee, mPlayer);
		}

		if (mActive && enemyTriggersAbilities) {
			execute(damagee, event);
		}

		return true;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent customDamageEvent) {
		if (
			mDoMultiplierAndFire
			&& customDamageEvent.getSpell() == ClassAbility.DIVINE_JUSTICE
		) {
			double originalDamage = customDamageEvent.getDamage();
			mLastPassiveDJDamage = originalDamage * DAMAGE_MULTIPLIER_2;
			customDamageEvent.setDamage(originalDamage + mLastPassiveDJDamage);
		}
	}

	public void execute(LivingEntity damagee, EntityDamageByEntityEvent event) {
		mActive = false;

		EntityUtils.damageEntity(Plugin.getInstance(), damagee, DAMAGE_UNDEAD_1, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);

		Location loc = damagee.getLocation();
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 100, 0.05f, 0.05f, 0.05f, 0.3);
		world.spawnParticle(Particle.FLAME, loc, 75, 0.05f, 0.05f, 0.05f, 0.3);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, 0.8f, 1.1f);

		// Exclude the damagee so that the knockaway is valid
		List<LivingEntity> affected = EntityUtils.getNearbyMobs(loc, RADIUS, damagee);
		for (LivingEntity e : affected) {
			// Reduce overall volume of noise the more mobs there are, but still make it louder for more mobs
			double volume = 0.6 / Math.sqrt(affected.size());
			world.playSound(loc, Sound.ITEM_TOTEM_USE, (float) volume, 1.1f);
			world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 10, 0.05f, 0.05f, 0.05f, 0.1);
			world.spawnParticle(Particle.FLAME, loc, 7, 0.05f, 0.05f, 0.05f, 0.1);

			if (Crusade.enemyTriggersAbilities(e, mCrusade)) {
				/*
				 * Annoying thing to fix eventually: there's some stuff with how the AbilityManager
				 * currently works (to infinite loop safe against certain abilities like Brute Force)
				 * where only one damage event per tick is counted. This means that there's not really
				 * a self-contained way for Luminous Infusion level 2 to make AoE abilities light all
				 * enemies on fire (instead of just the first hit) without some restructuring, which
				 * is planned (but I have no time to do that right now). Luckily, the only multi-hit
				 * abilities Paladin has are Holy Javelin (already lights things on fire) and this,
				 * and so the fire, though it should be generically applied to all abilities, is
				 * hard coded for Luminous Infusion level 2 and has the same effect, so this workaround
				 * will be in place until the AbilityManager gets restructured.
				 * - Rubiks
				 */
				if (mDoMultiplierAndFire) {
					EntityUtils.applyFire(Plugin.getInstance(), FIRE_DURATION_2, e, mPlayer);
				}
				EntityUtils.damageEntity(Plugin.getInstance(), e, DAMAGE_UNDEAD_1, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
			} else {
				EntityUtils.damageEntity(Plugin.getInstance(), e, DAMAGE_1, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
			}
			MovementUtils.knockAway(loc, e, KNOCKBACK_SPEED, KNOCKBACK_SPEED / 2);
		}
	}
}