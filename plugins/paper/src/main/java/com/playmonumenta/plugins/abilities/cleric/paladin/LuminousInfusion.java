package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.AbilityManager;

public class LuminousInfusion extends Ability {

	private static final String EXPIRATION_MESSAGE = "The light from your hands fades...";
	private static final double RADIUS = 4;
	private static final int DAMAGE = 10;
	private static final int UNDEAD_DAMAGE = 20;
	private static final double PASSIVE_UNDEAD_DAMAGE_2 = 0.2;
	private static final int FIRE_DURATION_2 = 20 * 3;
	private static final int COOLDOWN = 20 * 14;
	private static final float KNOCKBACK_SPEED = 0.7f;

	private boolean mActive = false;

	private Crusade mCrusade;
	private boolean mCountsHumanoids = false;

	public LuminousInfusion(Plugin plugin, Player player) {
		super(plugin, player, "Luminous Infusion");
		mInfo.mLinkedSpell = Spells.LUMINOUS_INFUSION;
		mInfo.mScoreboardId = "LuminousInfusion";
		mInfo.mShorthandName = "LI";
		mInfo.mDescriptions.add("Swap while shifted to charge your weapon with holy light. Your next attack (melee, ranged, magic, etc.) on an undead mob triggers an explosion with a 4 block radius, knocking enemies away. Undead take 20 damage and all other mobs take 10 damage. Cooldown: 14s.");
		mInfo.mDescriptions.add("Additionally, melee attacks against undead passively deal 20% final damage as bonus damage, and any attack lights undead on fire for 3 seconds.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mIgnoreTriggerCap = true;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;

		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mCrusade = AbilityManager.getManager().getPlayerAbility(mPlayer, Crusade.class);
				if (mCrusade != null) {
					mCountsHumanoids = mCrusade.getAbilityScore() == 2;
				}
			}
		});
	}

	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer.isSneaking()) {
			event.setCancelled(true);
			if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
				return;
			}
		} else {
			return;
		}

		// Cast conditions met
		mActive = true;
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Holy energy radiates from your hands...");
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
						MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, EXPIRATION_MESSAGE);
					}
					mActive = false;
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 1, 1);

		putOnCooldown();
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity mob = (LivingEntity) event.getEntity();

		if (getAbilityScore() > 1 && (EntityUtils.isUndead(mob) || (mCountsHumanoids && EntityUtils.isHumanoid(mob)))) {
			EntityUtils.applyFire(mPlugin, FIRE_DURATION_2, mob, mPlayer);
			double bonusDamage = 0.0;
			if (mCrusade != null) {
				if (mCrusade.getAbilityScore() > 0) {
					bonusDamage = (event.getDamage() * PASSIVE_UNDEAD_DAMAGE_2) * 0.33;
				}
			}
			if (event.getCause() == DamageCause.ENTITY_ATTACK) {
				event.setDamage((event.getDamage() * (1 + PASSIVE_UNDEAD_DAMAGE_2)) + bonusDamage);
			}
		}

		if (mActive && (EntityUtils.isUndead(mob) || (mCountsHumanoids && EntityUtils.isHumanoid(mob)))) {
			execute(mob, event);
		}

		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (getAbilityScore() > 1 && (EntityUtils.isUndead(damagee) || (mCountsHumanoids && EntityUtils.isHumanoid(damagee)))) {
			EntityUtils.applyFire(mPlugin, FIRE_DURATION_2, damagee, mPlayer);
		}

		if (mActive && (EntityUtils.isUndead(damagee) || (mCountsHumanoids && EntityUtils.isHumanoid(damagee)))) {
			execute(damagee, event);
		}

		return true;
	}

	public void execute(LivingEntity damagee, EntityDamageByEntityEvent event) {
		mActive = false;

		EntityUtils.damageEntity(mPlugin, damagee, UNDEAD_DAMAGE, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);

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

			if (EntityUtils.isUndead(e) || (mCountsHumanoids && EntityUtils.isHumanoid(e))) {
				EntityUtils.damageEntity(mPlugin, e, UNDEAD_DAMAGE, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);

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
				 */
				if (getAbilityScore() > 1) {
					EntityUtils.applyFire(mPlugin, FIRE_DURATION_2, e, mPlayer);
				}
			} else {
				EntityUtils.damageEntity(mPlugin, e, DAMAGE, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
			}

			MovementUtils.knockAway(loc, e, KNOCKBACK_SPEED, KNOCKBACK_SPEED / 2);
		}
	}

}
