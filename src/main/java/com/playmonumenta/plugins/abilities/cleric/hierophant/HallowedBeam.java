package com.playmonumenta.plugins.abilities.cleric.hierophant;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

/*
 * Hallowed Beam: Level 1 – Firing a fully-drawn bow while sneaking, 
 * if pointed directly at a non-boss undead, will instantly deal 40 damage 
 * to the undead instead of consuming the arrow. Cooldown: 20s. Level 2 - 
 * The targeted undead explodes, dealing 20 damage to undead within a 
 * 5-block radius, and giving slowness 4 to all enemies.
 */
public class HallowedBeam extends Ability {

	private static final Particle.DustOptions HALLOWED_BEAM_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 150), 1.0f);
	
	public HallowedBeam(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "HallowedBeam";
		mInfo.linkedSpell = Spells.HALLOWED_BEAM;
		mInfo.cooldown = 20 * 20;
	}
	
	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (arrow.isCritical()) {
			Player player = mPlayer;
			LivingEntity e = EntityUtils.getCrosshairTarget(player, 30, false, true, true, false);
			if (e != null && EntityUtils.isUndead(e)) {
				player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.5f);
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 0.9f);
				Location loc = player.getEyeLocation();
				Vector dir = LocationUtils.getDirectionTo(e.getEyeLocation(), loc);
				for (int i = 0; i < 30; i++) {
					loc.add(dir);
					mWorld.spawnParticle(Particle.REDSTONE, loc, 22, 0.35, 0.35, 0.35, HALLOWED_BEAM_COLOR);
					mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f);
					if (loc.distance(e.getEyeLocation()) < 1.25) {
						loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.35f);
						loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
						break;
					}
				}
				EntityUtils.damageEntity(mPlugin, e, 40, player);
				Location eLoc = e.getLocation().add(0, e.getHeight() / 2, 0);
				mWorld.spawnParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f);
				mWorld.spawnParticle(Particle.FIREWORKS_SPARK, eLoc, 75, 0, 0, 0, 0.3f);
				if (getAbilityScore() > 1) {
					// TODO: Revamp explosion effects
					mWorld.spawnParticle(Particle.SPELL_INSTANT, e.getLocation(), 500, 5, 0.15f, 5, 1);
					for (Entity ne : e.getNearbyEntities(5, 5, 5)) {
						if (ne instanceof LivingEntity) {
							LivingEntity le = (LivingEntity) ne;
							if (EntityUtils.isUndead(le)) {
								EntityUtils.damageEntity(mPlugin, le, 20, player);
							}
							le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 3, false, true));
						}
					}
				}
				putOnCooldown();
				return true;
			}
		}
		return true;
	}
	
	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking();
	}

}
