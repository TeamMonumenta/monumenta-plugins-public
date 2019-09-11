package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Deadly Ronde: After using a skill, your next sword
 * attack deals 4 / 6 extra damage, also adding half of
 * that bonus to sweeping attacks. At lvl 2, the sweep
 * attack takes the full bonus and all attacks also
 * staggers the single mob you melee hit, afflicting
 * it with Slowness II for 4 s.
 */

public class DeadlyRonde extends Ability {

	private static final int RONDE_1_DAMAGE_BONUS = 4;
	private static final int RONDE_2_DAMAGE_BONUS = 6;
	private static final int RONDE_2_SLOWNESS_AMPLIFIER = 1;
	private static final int RONDE_2_SLOWNESS_DURATION = 4 * 20;
	private static final Particle.DustOptions RONDE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);
	private static final Particle.DustOptions RONDE_ATTACK_COLOR = new Particle.DustOptions(Color.fromRGB(232, 232, 232), 0.8f);

	private int mDamageBonus;
	boolean cancelled = false;
	BukkitRunnable activeRunnable = null;

	public DeadlyRonde(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "DeadlyRonde";
		mDamageBonus = getAbilityScore() == 1 ? RONDE_1_DAMAGE_BONUS : RONDE_2_DAMAGE_BONUS;
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		/* Re-up the duration every time an ability is cast */
		if (activeRunnable != null) {
			activeRunnable.cancel();
		} else {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 2f);
			new BukkitRunnable() {

				@Override
				public void run() {
					mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 3, 0.25, 0.45, 0.25, RONDE_COLOR);
					if (activeRunnable == null) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Deadly Ronde activated!");
		}

		activeRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				activeRunnable = null;
			}
		};
		activeRunnable.runTaskLater(mPlugin, 20 * 5);
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (activeRunnable != null && (event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK)) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInMainHand();
			if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
				Entity ent = event.getEntity();
				mWorld.spawnParticle(Particle.REDSTONE, ent.getLocation().add(0, 1, 0), 40, 0.45, 0.65, 0.45, RONDE_ATTACK_COLOR);
				if (event.getCause() == DamageCause.ENTITY_ATTACK) {
					event.setDamage(event.getDamage() + mDamageBonus);
					mWorld.spawnParticle(Particle.BLOCK_CRACK, ent.getLocation().add(0, 1, 0), 35, 0.25, 0.45, 0.25, 0.25, Material.REDSTONE_WIRE.createBlockData());
					mWorld.spawnParticle(Particle.SWEEP_ATTACK, ent.getLocation().add(0, 1, 0), 6, 0.45, 0.65, 0.45);
					mPlayer.getWorld().playSound(ent.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.25f);
					mPlayer.getWorld().playSound(ent.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 0.75f);
					mPlayer.getWorld().playSound(ent.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.75f);
					if (getAbilityScore() > 1 && event.getEntity() instanceof LivingEntity) {
						LivingEntity le = (LivingEntity)event.getEntity();
						PotionUtils.applyPotion(mPlayer, le, new PotionEffect(PotionEffectType.SLOW, RONDE_2_SLOWNESS_DURATION, RONDE_2_SLOWNESS_AMPLIFIER, true, false));
						mWorld.spawnParticle(Particle.FALLING_DUST, ent.getLocation().add(0, 1, 0), 10, 0.25, 0.45, 0.25, 0.25, Material.GRAVEL.createBlockData());
					}
				} else {
					int toAdd = getAbilityScore() == 1 ? (mDamageBonus / 2) : mDamageBonus;
					event.setDamage(event.getDamage() + toAdd);
					mWorld.spawnParticle(Particle.BLOCK_CRACK, ent.getLocation().add(0, 1, 0), 15, 0.25, 0.45, 0.25, 0.25, Material.REDSTONE_WIRE.createBlockData());
				}
			}

			//The cancelled variable is so we don't spout multiple BukkitRunnables
			if (!cancelled) {
				cancelled = true;
				//Deactivates 1 tick after the strike so that way the sweep bonus is applied.
				//If we had it cancelled on damage, then entities affected by sweeps wouldn't get damaged.
				new BukkitRunnable() {
					@Override
					public void run() {
						activeRunnable.cancel();
						activeRunnable = null;
						cancelled = false;

					}
				}.runTaskLater(mPlugin, 1);
			}
		}
		return true;
	}

}
