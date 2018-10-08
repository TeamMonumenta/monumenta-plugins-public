package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;

public class Riposte extends Ability {

	private static final int RIPOSTE_COOLDOWN = 10 * 20;
	private static final int RIPOSTE_SWORD_EFFECT_LEVEL = 1;
	private static final int RIPOSTE_SWORD_DURATION = 5 * 20;
	private static final int RIPOSTE_AXE_DURATION = 3 * 20;
	private static final int RIPOSTE_AXE_EFFECT_LEVEL = 6;
	private static final double RIPOSTE_SQRADIUS = 6.25;    //radius = 2.5, this is it squared
	private static final float RIPOSTE_KNOCKBACK_SPEED = 0.15f;

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) {
		LivingEntity damager = (LivingEntity) event.getEntity();
		if ((player.getLocation()).distanceSquared(damager.getLocation()) < RIPOSTE_SQRADIUS) {
			int riposte = getAbilityScore(player);
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			MovementUtils.KnockAway(player, damager, RIPOSTE_KNOCKBACK_SPEED);

			if (InventoryUtils.isAxeItem(mainHand) || InventoryUtils.isSwordItem(mainHand)) {
				if (riposte > 1) {
					if (InventoryUtils.isSwordItem(mainHand)) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, RIPOSTE_SWORD_DURATION, RIPOSTE_SWORD_EFFECT_LEVEL, true, true));
					} else if (InventoryUtils.isAxeItem(mainHand)) {
						damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, RIPOSTE_AXE_DURATION, RIPOSTE_AXE_EFFECT_LEVEL, true, false));
					}
				}

				mWorld.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
				mWorld.spawnParticle(Particle.SWEEP_ATTACK, (player.getLocation()).add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.001);
				mWorld.spawnParticle(Particle.CRIT_MAGIC, (player.getLocation()).add(0, 1, 0), 20, 0.75, 0.5, 0.75, 0.001);
				putOnCooldown(player);
				return false;
			}
		}
		return true;
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 2;
		info.specId = -1;
		info.linkedSpell = Spells.RIPOSTE;
		info.scoreboardId = "Obliteration";
		info.cooldown = RIPOSTE_COOLDOWN;
		return info;
	}

}
