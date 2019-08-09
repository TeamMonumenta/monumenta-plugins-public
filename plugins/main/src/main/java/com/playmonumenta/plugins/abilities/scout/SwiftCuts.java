package com.playmonumenta.plugins.abilities.scout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * [Swift Cuts Level 1] : On melee hit the target is marked.
 * When the target is hit again they take an additional 2 damage,
 * remove the mark, and get 10% Vulnerability for 2 seconds.
 * Targets can only be marked each again after 3s
 *
 * [Swift Cuts Level 2] : The damage is increased to 4 and the
 * Vulnerability is increased to 20%
 */
public class SwiftCuts extends Ability {

	private static final double SWIFT_CUTS_1_DAMAGE = 2;
	private static final double SWIFT_CUTS_2_DAMAGE = 4;

	public SwiftCuts(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "SwiftCuts";
	}

	private List<Entity> marked = new ArrayList<Entity>();
	private List<Entity> cooldown = new ArrayList<Entity>();
	private boolean activated = false;

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK
		    && event.getEntityType().isAlive()
		    && !MetadataUtils.happenedThisTick(mPlugin, mPlayer, EntityUtils.PLAYER_DEALT_CUSTOM_DAMAGE_METAKEY, 0)) {
			LivingEntity ent = (LivingEntity) event.getEntity();
			if (!cooldown.contains(ent)) {
				World world = ent.getWorld();
				int amp = getAbilityScore() == 1 ? 1 : 3;
				if (!marked.contains(ent)) {
					marked.add(ent);
					new BukkitRunnable() {
						int t = 0;
						@Override
						public void run() {
							t++;
							world.spawnParticle(Particle.FALLING_DUST, ent.getLocation().add(0, 1, 0), 1, 0.35, 0.45, 0.35, Material.GRAVEL.createBlockData());
							if (!marked.contains(ent) || t >= 20 * 3 || ent.isDead() || !ent.isValid()) {
								this.cancel();
								marked.remove(ent);
							}
						}

					}.runTaskTimer(mPlugin, 0, 2);
				} else {
					marked.remove(ent);
					cooldown.add(ent);
					new BukkitRunnable() {

						@Override
						public void run() {
							cooldown.remove(ent);
						}

					}.runTaskLater(mPlugin, 20 * 3);
					ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1.5f);
					world.spawnParticle(Particle.SWEEP_ATTACK, ent.getLocation().add(0, 1, 0), 3, 0.35, 0.45, 0.35, 0.001);
					double damage = getAbilityScore() == 1 ? SWIFT_CUTS_1_DAMAGE : SWIFT_CUTS_2_DAMAGE;
					event.setDamage(event.getDamage() + damage);
					if (getAbilityScore() > 1 && !activated) {
						activated = true;
						mPlayer.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(mPlayer.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getBaseValue() + 0.1);
						mPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() + 0.01);
						new BukkitRunnable() {

							@Override
							public void run() {
								activated = false;
								mPlayer.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(mPlayer.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getBaseValue() - 0.1);
								mPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() - 0.01);
							}

						}.runTaskLater(mPlugin, 20 * 3);
					}

					PotionUtils.applyPotion(mPlayer, ent, new PotionEffect(PotionEffectType.UNLUCK, 20 * 2, amp));
				}
			}
		}
		return true;
	}

}
