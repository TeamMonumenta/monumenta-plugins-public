package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

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

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class Riposte extends Ability {

	private static final int RIPOSTE_COOLDOWN = 10 * 20;
	private static final int RIPOSTE_SWORD_EFFECT_LEVEL = 1;
	private static final int RIPOSTE_SWORD_DURATION = 5 * 20;
	private static final int RIPOSTE_AXE_DURATION = 3 * 20;
	private static final int RIPOSTE_AXE_EFFECT_LEVEL = 6;
	private static final double RIPOSTE_MELEE_THRESHOLD = 2;
	private static final float RIPOSTE_KNOCKBACK_SPEED = 0.15f;

	public Riposte(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.RIPOSTE;
		mInfo.scoreboardId = "Obliteration";
		mInfo.cooldown = RIPOSTE_COOLDOWN;
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (event.getFinalDamage() > 0) { //don't activate if the player wouldn't take damage
			LivingEntity damager = (LivingEntity) event.getDamager();
			// First checks that the damage cause was either melee or custom (since entity.damage()
			// counts as ENTITY_ATTACK), then eliminates 99% of ability attack cases by checking
			// that the bounding box expanded by an arbitrary number intersects the player's location.
			if (event.getCause() == DamageCause.ENTITY_ATTACK &&
			    damager.getBoundingBox().expand(RIPOSTE_MELEE_THRESHOLD).contains(mPlayer.getLocation().toVector())) {
				ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
				MovementUtils.KnockAway(mPlayer, damager, RIPOSTE_KNOCKBACK_SPEED);

				if (InventoryUtils.isAxeItem(mainHand) || InventoryUtils.isSwordItem(mainHand)) {
					if (getAbilityScore() > 1) {
						if (InventoryUtils.isSwordItem(mainHand)) {
							mPlugin.mPotionManager.addPotion(mPlayer, PotionID.APPLIED_POTION,
							                                 new PotionEffect(PotionEffectType.INCREASE_DAMAGE, RIPOSTE_SWORD_DURATION,
							                                                  RIPOSTE_SWORD_EFFECT_LEVEL, true, true));
						} else if (InventoryUtils.isAxeItem(mainHand)) {
							if (!EntityUtils.isBoss(damager)) {
								// Potentially change this to stun for consistency? Ability description currently says "immobilize."
								// Changing this to stun would technically be a buff, so waiting on executive decision before changing.
								damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, RIPOSTE_AXE_DURATION, RIPOSTE_AXE_EFFECT_LEVEL, true, false));
							}
						}
					}

					mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
					mWorld.spawnParticle(Particle.SWEEP_ATTACK, (mPlayer.getLocation()).add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.001);
					mWorld.spawnParticle(Particle.CRIT_MAGIC, (mPlayer.getLocation()).add(0, 1, 0), 20, 0.75, 0.5, 0.75, 0.001);
					putOnCooldown();
					return false;
				}
			}
		}
		return true;
	}
}
