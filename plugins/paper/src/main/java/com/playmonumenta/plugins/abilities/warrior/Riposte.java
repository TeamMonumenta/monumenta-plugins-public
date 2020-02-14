package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

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
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class Riposte extends Ability {

	private static final int RIPOSTE_COOLDOWN = 10 * 20;
	private static final int RIPOSTE_SWORD_EFFECT_LEVEL = 1;
	private static final int RIPOSTE_SWORD_DURATION = 5 * 20;
	private static final int RIPOSTE_AXE_DURATION = 3 * 20;
	private static final double RIPOSTE_MELEE_THRESHOLD = 2;
	private static final float RIPOSTE_KNOCKBACK_SPEED = 0.15f;

	public Riposte(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Riposte");
		mInfo.linkedSpell = Spells.RIPOSTE;
		mInfo.scoreboardId = "Obliteration";
		mInfo.cooldown = RIPOSTE_COOLDOWN;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (EntityUtils.getRealFinalDamage(event) > 0) { //don't activate if the player wouldn't take damage
			LivingEntity damager = (LivingEntity) event.getDamager();
			// First checks that the damage cause was either melee or custom (since entity.damage()
			// counts as ENTITY_ATTACK), then eliminates 99% of ability attack cases by checking
			// that the bounding box expanded by an arbitrary number intersects the player's location.
			if (event.getCause() == DamageCause.ENTITY_ATTACK &&
			    damager.getBoundingBox().expand(RIPOSTE_MELEE_THRESHOLD).contains(mPlayer.getLocation().toVector())) {
				ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
				MovementUtils.knockAway(mPlayer, damager, RIPOSTE_KNOCKBACK_SPEED);

				if (InventoryUtils.isAxeItem(mainHand) || InventoryUtils.isSwordItem(mainHand)) {
					if (getAbilityScore() > 1) {
						if (InventoryUtils.isSwordItem(mainHand)) {
							mPlugin.mPotionManager.addPotion(mPlayer, PotionID.APPLIED_POTION,
							                                 new PotionEffect(PotionEffectType.INCREASE_DAMAGE, RIPOSTE_SWORD_DURATION,
							                                                  RIPOSTE_SWORD_EFFECT_LEVEL, true, true));
						} else if (InventoryUtils.isAxeItem(mainHand)) {
							if (!EntityUtils.isBoss(damager)) {
								EntityUtils.applyStun(mPlugin, RIPOSTE_AXE_DURATION, damager);
							}
						}
					}

					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 1.8f);
					Vector dir = LocationUtils.getDirectionTo(mPlayer.getLocation().add(0, 1, 0), damager.getLocation().add(0, damager.getHeight() / 2, 0));
					Location loc = mPlayer.getLocation().add(0, 1, 0).subtract(dir);
					mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 8, 0.75, 0.5, 0.75, 0.001);
					mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.75, 0.5, 0.75, 0.1);
					mWorld.spawnParticle(Particle.CRIT, loc, 75, 0.1, 0.1, 0.1, 0.6);
					putOnCooldown();
					return false;
				}
			}
		}
		return true;
	}
}
