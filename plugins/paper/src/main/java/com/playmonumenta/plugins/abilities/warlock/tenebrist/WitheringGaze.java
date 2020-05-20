package com.playmonumenta.plugins.abilities.warlock.tenebrist;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

/*
 * Withering Gaze: Sprinting and left clicking unleashes a cone of
 * magic in the direction the player faces that stuns all enemies
 * in its path for 3 / 4 seconds (elites and bosses are given slowness 3) and gives wither III
 * for 6/8 seconds. 30/20 second cooldown.
 */

public class WitheringGaze extends Ability {

	private static final int WITHERING_GAZE_1_DURATION = 3;
	private static final int WITHERING_GAZE_2_DURATION = 4;
	private static final int WITHERING_GAZE_1_COOLDOWN = 20 * 30;
	private static final int WITHERING_GAZE_2_COOLDOWN = 20 * 20;

	public WitheringGaze(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Withering Gaze");
		mInfo.scoreboardId = "WitheringGaze";
		mInfo.mShorthandName = "WG";
		mInfo.mDescriptions.add("Sprint left-clicking unleashes a 9 block long cone in the direction the player is facing. Enemies in its path are stunned for 3 seconds (elites and bosses are given Slowness 3 instead) and given Wither 3 for 6 seconds. Cooldown: 30 seconds.");
		mInfo.mDescriptions.add("Stun lasts for 4 seconds and Wither lasts for 8 seconds. Cooldown: 20 seconds.");
		mInfo.linkedSpell = Spells.WITHERING_GAZE;
		mInfo.cooldown = getAbilityScore() == 1 ? WITHERING_GAZE_1_COOLDOWN : WITHERING_GAZE_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast(Action action) {
		Player player = mPlayer;
		Location loc = player.getLocation().add(0, 0.65, 0); // the Y height is higher so that the skill doesn't get stomped by halfslabs
		Vector direction = loc.getDirection().setY(0).normalize();
		int duration = getAbilityScore() == 1 ? 20 * WITHERING_GAZE_1_DURATION : 20 * WITHERING_GAZE_2_DURATION;
		player.getLocation().getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1f, 0.4f);
		player.getLocation().getWorld().playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 1f, 1f);
		new BukkitRunnable() {
			double t = 0;
			double damagerange = 1.15;
			double r = 1;
			@Override
			public void run() {

				t += 1;
				Vector vec;
				for (double degree = 0; degree < 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
					vec = VectorUtils.rotateXAxis(vec, 0);
					vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

					Location l = loc.clone().add(vec);
					mWorld.spawnParticle(Particle.SPELL_WITCH, l, 3, 0.15, 0.15, 0.15, 0.15);
					mWorld.spawnParticle(Particle.SPELL_MOB, l, 3, 0.15, 0.15, 0.15, 0);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, l, 2, 0.15, 0.15, 0.15, 0.05);
				}
				r += 0.55;

				for (Entity e : player.getNearbyEntities(damagerange, damagerange * 2, damagerange)) {
					if (EntityUtils.isHostileMob(e)) {
						Vector eVec = e.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
						if (direction.dot(eVec) > 0.4) {
							LivingEntity le = (LivingEntity) e;
							if (EntityUtils.isElite(le) || EntityUtils.isBoss(le) || ((e instanceof Player) && AbilityManager.getManager().isPvPEnabled((Player)e))) {
								PotionUtils.applyPotion(player, le, new PotionEffect(PotionEffectType.SLOW, duration, 2));
							} else {
								EntityUtils.applyStun(mPlugin, duration, le);
							}
							PotionUtils.applyPotion(player, le, new PotionEffect(PotionEffectType.WITHER, duration * 2, 2));
							CustomDamageEvent event = new CustomDamageEvent(player, le, 0, null);
							Bukkit.getPluginManager().callEvent(event);
						}
					}
				}

				damagerange += 1;
				loc.add(direction.clone().multiply(0.75));
				if (loc.getBlock().getType().isSolid()) {
					this.cancel();
				}

				if (t >= 9) {
					this.cancel();

				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && InventoryUtils.isScytheItem(mHand);
	}

}
