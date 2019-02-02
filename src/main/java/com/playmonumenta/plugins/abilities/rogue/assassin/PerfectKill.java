package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Perfect Kill: While sprinting, instantly 
 * kill a non-boss/elite mob with a melee attack. (Cooldown: 30 / 20 seconds)
 */
public class PerfectKill extends Ability {

	public PerfectKill(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "PerfectKill";
		mInfo.linkedSpell = Spells.PERFECT_KILL;
		mInfo.cooldown = getAbilityScore() == 1 ? 20 * 30 : 20 * 20;
	}
	
	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity le = (LivingEntity) event.getEntity();
		
		if (!EntityUtils.isBoss(le) && !EntityUtils.isElite(le)) {
			le.setHealth(0);
			le.getWorld().playSound(le.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1, 1.75f);
			mWorld.spawnParticle(Particle.SPELL_WITCH, le.getLocation().add(0, 1.15, 0), 50, 0.3, 0.35, 0.3, 1);
			mWorld.spawnParticle(Particle.SPELL_MOB, le.getLocation().add(0, 1.15, 0), 50, 0.2, 0.35, 0.2, 0);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, le.getLocation().add(0, 1.15, 0), 5, 0.3, 0.35, 0.3, 0);
			
			putOnCooldown();
		}
		return true;
	}
	
	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return mPlayer.isSneaking() && InventoryUtils.isSwordItem(mHand) && InventoryUtils.isSwordItem(oHand);
	}

}
