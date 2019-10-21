package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

/*
 * Deadly Ronde: After using a skill, your next sword
 * attack deals 4 / 6 extra damage, also adding half of
 * that bonus to sweeping attacks. At lvl 2, the sweep
 * attack takes the full bonus and all attacks also
 * staggers the single mob you melee hit, afflicting
 * it with Slowness II for 4 s.
 */

public class DeadlyRonde extends Ability {

	private static final int RONDE_1_DAMAGE = 5;
	private static final int RONDE_2_DAMAGE = 8;
	private static final int RONDE_1_MAX_STACKS = 2;
	private static final int RONDE_2_MAX_STACKS = 3;
	private static final double RONDE_RADIUS = 4.5;
	private static final double RONDE_DOT_COSINE = 0.33;
	private static final float RONDE_KNOCKBACK_SPEED = 0.14f;

	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	boolean cancelled = false;
	BukkitRunnable activeRunnable = null;
	int rondeStacks = 0;

	public DeadlyRonde(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "DeadlyRonde";
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		/* Re-up the duration every time an ability is cast */
		if (activeRunnable != null) {
			activeRunnable.cancel();
		} else {
			new BukkitRunnable() {

				@Override
				public void run() {
					mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 3, 0.25, 0.45, 0.25, SWORDSAGE_COLOR);
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, 5, 0, true, false));
					if (activeRunnable == null) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
		activeRunnable = new BukkitRunnable() {

			@Override
			public void run() {
				activeRunnable = null;
				rondeStacks = 0;
			}

		};
		activeRunnable.runTaskLater(mPlugin, 20 * 5);

		int maxStacks = getAbilityScore() == 1 ? RONDE_1_MAX_STACKS : RONDE_2_MAX_STACKS;
		if (rondeStacks < maxStacks) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_OUT, 1, 1f);
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SNOW_GOLEM_DEATH, 0.7f, 1.5f);
		}

		rondeStacks = Math.min(rondeStacks + 1, maxStacks);
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Deadly Ronde stacks: " + rondeStacks);

		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (activeRunnable != null && event.getCause() == DamageCause.ENTITY_ATTACK) {

			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInMainHand();

			if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
				Vector playerDirVector = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RONDE_RADIUS)) {
					Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
					if (playerDirVector.dot(toMobVector) > RONDE_DOT_COSINE) {
						int damage = getAbilityScore() == 1 ? RONDE_1_DAMAGE : RONDE_2_DAMAGE;
						mob.setNoDamageTicks(0);
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
						MovementUtils.KnockAway(mPlayer, mob, RONDE_KNOCKBACK_SPEED);
					}
				}
			}

			Location particleLoc = mPlayer.getEyeLocation().add(mPlayer.getEyeLocation().getDirection().multiply(3));

			mWorld.spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 10, 1.5, 0.5, 1.5);
			mWorld.spawnParticle(Particle.CRIT, particleLoc, 50, 1.5, 0.5, 1.5, 0.2);
			mWorld.spawnParticle(Particle.CLOUD, particleLoc, 20, 1.5, 0.5, 1.5, 0.3);
			mWorld.spawnParticle(Particle.REDSTONE, particleLoc, 45, 1.5, 0.5, 1.5, SWORDSAGE_COLOR);

			mWorld.playSound(particleLoc, Sound.ITEM_TRIDENT_THROW, 1, 1.25f);
			mWorld.playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 0.75f);
			mWorld.playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.75f);
			mWorld.playSound(particleLoc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.75f);

			activeRunnable.cancel();
			activeRunnable = null;

			rondeStacks--;
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Deadly Ronde stacks: " + rondeStacks);
			if (rondeStacks > 0) {
				activeRunnable = new BukkitRunnable() {

					@Override
					public void run() {
						activeRunnable = null;
						rondeStacks = 0;
					}

				};
				activeRunnable.runTaskLater(mPlugin, 20 * 5);
			}
		}
		return true;
	}

}
