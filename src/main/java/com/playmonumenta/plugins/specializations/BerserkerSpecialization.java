package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Random;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.World;

public class BerserkerSpecialization extends BaseSpecialization {
	World mWorld;

	public BerserkerSpecialization(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		if (player.getFallDistance() >= 1) {
			int fall = Math.round(player.getFallDistance());
			int meteorSlam = ScoreboardUtils.getScoreboardValue(player, "MeteorSlam");
			/*
			 * Meteor Slam: Hitting an enemy while falling removes fall damage
			 * and does +2/2.5 for block fallen extra damage to all mobs within
			 * 3/5 blocks
			 */
			if (meteorSlam > 0) {
				player.setFallDistance(0);
				LivingEntity entity = (LivingEntity) event.getEntity();
				Location loc = entity.getLocation();
				float radius = meteorSlam == 1 ? 3 : 5;
				double dmgMult = meteorSlam == 1 ? 2 : 2.5;

				loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 0);
				loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 1.25F);
				mWorld.spawnParticle(Particle.FLAME, loc, 175, 0F, 0F, 0F, 0.175F);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0F, 0F, 0F, 0.3F);
				mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0F, 0F, 0F, 0.3F);
				mWorld.spawnParticle(Particle.LAVA, loc, 100, radius, 0.25f, radius, 0);
				for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
					if (EntityUtils.isHostileMob(e)) {
						LivingEntity le = (LivingEntity) e;
						EntityUtils.damageEntity(mPlugin, le, fall * dmgMult, player);
					}
				}
			}
		}
		return true;
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				if (InventoryUtils.isAxeItem(player.getInventory().getItemInMainHand())) {
					int warCry = ScoreboardUtils.getScoreboardValue(player, "WarCry");
					/*
					 * Warcry: Shift+Right Click with an axe in the main hand
					 * lets out a cry that spooks all enemies in a 6/8 block
					 * radius for 10/15 seconds. Gives enemies Weakness 1 and
					 * Slowness 1/2 while also giving the warrior Strength 1/2.
					 * 50s cooldown.
					 */
					if (warCry > 0) {
						double radius = warCry == 1 ? 6 : 8;

					}
				}
			}
		}
	}

	@Override
	public void PeriodicTrigger(Player player, boolean twoHertz, boolean oneSecond, boolean twoSeconds,
	                            boolean fourtySeconds, boolean sixtySeconds, int originalTime) {
		int psychosis = ScoreboardUtils.getScoreboardValue(player, "Psychosis");
		if (psychosis > 0) {
			if (oneSecond) {
				if (PotionUtils.hasNegativeEffects(player.getActivePotionEffects())) {
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 0, true, true));
					player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0.3);
					player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(2);
				} else {
					player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0);
					player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(1);
				}
			}
		}
	}

}
