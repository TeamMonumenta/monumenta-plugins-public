package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/*
 * Deadly Ronde: After using a skill, your next sword
 * attack deals 4 / 6 extra damage, also adding half of
 * that bonus to sweeping attacks. At lvl 2, the sweep
 * attack takes the full bonus and all attacks also
 * staggers the single mob you melee hit, afflicting
 * it with Slowness II for 4 s.
 */

public class DeadlyRonde extends Ability implements AbilityWithChargesOrStacks {

	private static final int RONDE_1_DAMAGE = 3;
	private static final int RONDE_2_DAMAGE = 5;
	private static final int RONDE_1_MAX_STACKS = 2;
	private static final int RONDE_2_MAX_STACKS = 3;
	private static final double RONDE_SPEED_BONUS = 0.2;
	private static final int RONDE_DECAY_TIMER = 5 * 20;
	private static final double RONDE_RADIUS = 4.5;
	private static final double RONDE_DOT_COSINE = 0.33;
	private static final float RONDE_KNOCKBACK_SPEED = 0.14f;

	public static final String CHARM_DAMAGE = "Deadly Ronde Damage";
	public static final String CHARM_RADIUS = "Deadly Ronde Radius";

	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	private @Nullable BukkitRunnable mActiveRunnable = null;
	private int mRondeStacks = 0;

	public DeadlyRonde(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Deadly Ronde");
		mInfo.mLinkedSpell = ClassAbility.DEADLY_RONDE;
		mInfo.mScoreboardId = "DeadlyRonde";
		mInfo.mShorthandName = "DR";
		mInfo.mDescriptions.add(
			String.format("After casting a skill, gain a stack of Deadly Ronde for %s seconds, stacking up to %s times. While Deadly Ronde is active, you gain %s%% Speed, and your next melee attack consumes a stack to fire a flurry of blades, that fire in a cone with a radius of %s blocks and deal %s melee damage to all enemies they hit.",
				RONDE_DECAY_TIMER / 20,
				RONDE_1_MAX_STACKS,
				(int)(RONDE_SPEED_BONUS * 100),
				RONDE_RADIUS,
				RONDE_1_DAMAGE
				));
		mInfo.mDescriptions.add(
			String.format("Damage increased to %s, and you can now store up to %s charges.",
				RONDE_2_DAMAGE,
				RONDE_2_MAX_STACKS));
		mDisplayItem = new ItemStack(Material.BLAZE_ROD, 1);
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (mPlayer == null) {
			return true;
		}
		/* Re-up the duration every time an ability is cast */
		if (mActiveRunnable != null) {
			mActiveRunnable.cancel();
		} else {
			new BukkitRunnable() {

				@Override
				public void run() {
					new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 3, 0.25, 0.45, 0.25, SWORDSAGE_COLOR).spawnAsPlayerBuff(mPlayer);
					mPlugin.mEffectManager.addEffect(mPlayer, "DeadlyRonde", new PercentSpeed(5, RONDE_SPEED_BONUS, "DeadlyRondeMod"));
					if (mActiveRunnable == null) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
		mActiveRunnable = new BukkitRunnable() {

			@Override
			public void run() {
				mActiveRunnable = null;
				mRondeStacks = 0;
				ClientModHandler.updateAbility(mPlayer, DeadlyRonde.this);
			}

		};
		mActiveRunnable.runTaskLater(mPlugin, RONDE_DECAY_TIMER);

		int maxStacks = getMaxCharges();
		if (mRondeStacks < maxStacks) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_OUT, 1, 1f);
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SNOW_GOLEM_DEATH, 0.7f, 1.5f);
			mRondeStacks++;
			ClientModHandler.updateAbility(mPlayer, this);
		}

		MessagingUtils.sendActionBarMessage(mPlayer, "Deadly Ronde stacks: " + mRondeStacks);

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mActiveRunnable != null
			    && event.getType() == DamageType.MELEE
			    && InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			Vector playerDirVector = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, RONDE_RADIUS))) {
				Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
				if (playerDirVector.dot(toMobVector) > RONDE_DOT_COSINE) {
					double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? RONDE_1_DAMAGE : RONDE_2_DAMAGE);
					DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage, mInfo.mLinkedSpell, true);
					MovementUtils.knockAway(mPlayer, mob, RONDE_KNOCKBACK_SPEED, true);
				}
			}

			Location particleLoc = mPlayer.getEyeLocation().add(mPlayer.getEyeLocation().getDirection().multiply(3));
			World world = mPlayer.getWorld();
			new PartialParticle(Particle.SWEEP_ATTACK, particleLoc, 10, 1.5, 0.5, 1.5).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT, particleLoc, 50, 1.5, 0.5, 1.5, 0.2).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CLOUD, particleLoc, 20, 1.5, 0.5, 1.5, 0.3).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, particleLoc, 45, 1.5, 0.5, 1.5, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);

			world.playSound(particleLoc, Sound.ITEM_TRIDENT_THROW, 1, 1.25f);
			world.playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 0.75f);
			world.playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.75f);
			world.playSound(particleLoc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);

			mActiveRunnable.cancel();
			mActiveRunnable = null;

			mRondeStacks--;
			MessagingUtils.sendActionBarMessage(mPlayer, "Deadly Ronde stacks: " + mRondeStacks);
			ClientModHandler.updateAbility(mPlayer, this);
			if (mRondeStacks > 0) {
				mActiveRunnable = new BukkitRunnable() {

					@Override
					public void run() {
						mActiveRunnable = null;
						mRondeStacks = 0;
						MessagingUtils.sendActionBarMessage(mPlayer, "Deadly Ronde stacks: " + mRondeStacks);
						ClientModHandler.updateAbility(mPlayer, DeadlyRonde.this);
					}

				};
				mActiveRunnable.runTaskLater(mPlugin, RONDE_DECAY_TIMER);
			}
			return true; // only trigger once per attack
		}
		return false;
	}

	@Override
	public int getCharges() {
		return mRondeStacks;
	}

	@Override
	public int getMaxCharges() {
		return isLevelOne() ? RONDE_1_MAX_STACKS : RONDE_2_MAX_STACKS;
	}

}
