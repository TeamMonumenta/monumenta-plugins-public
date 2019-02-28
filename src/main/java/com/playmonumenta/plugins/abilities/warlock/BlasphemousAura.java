package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class BlasphemousAura extends Ability {

	private static final int BLASPHEMY_RADIUS = 3;
	private static final float BLASPHEMY_KNOCKBACK_SPEED = 0.3f;
	private static final int BLASPHEMY_1_VULN_LEVEL = 3;
	private static final int BLASPHEMY_2_VULN_LEVEL = 5;
	private static final int BLASPHEMY_VULN_DURATION = 6 * 20;

	public BlasphemousAura(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "BlasphemousAura";
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		LivingEntity damager = (LivingEntity) event.getDamager();
		if (!(damager instanceof Player)) {
			// ABILITY: Blasphemous Aura
			if (damager instanceof Skeleton) {
				Skeleton skelly = (Skeleton) damager;
				ItemStack mainHand = skelly.getEquipment().getItemInMainHand();
				if (mainHand != null && mainHand.getType() == Material.BOW) {
					return true;
				}
			}

			Location loc = mPlayer.getLocation();
			World world = mPlayer.getWorld();

			world.spawnParticle(Particle.SMOKE_NORMAL, loc.add(0, 1, 0), 30, 1.5, 0.6, 1.5, 0.001);
			world.spawnParticle(Particle.SPELL, loc.add(0, 1, 0), 30, 1.5, 0.6, 1.5, 0.001);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.6f);

			MovementUtils.KnockAway(mPlayer, damager, BLASPHEMY_KNOCKBACK_SPEED);
			int vulnLevel = (getAbilityScore() == 1) ? BLASPHEMY_1_VULN_LEVEL : BLASPHEMY_2_VULN_LEVEL;

			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), BLASPHEMY_RADIUS, mPlayer)) {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, BLASPHEMY_VULN_DURATION, vulnLevel, false, true));
			}
		}
		return true;
	}

}
