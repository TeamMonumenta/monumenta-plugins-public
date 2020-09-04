package com.playmonumenta.plugins.abilities.rogue;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Skirmisher extends Ability {

	private static final double ISOLATED_PERCENT_DAMAGE_1 = 0.25;
	private static final double ISOLATED_PERCENT_DAMAGE_2 = 0.4;
	private static final double SKIRMISHER_ISOLATION_RADIUS = 2.5;

	private final double mIsolatedPercentDamage;

	public Skirmisher(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Skirmisher");
		mInfo.mScoreboardId = "Skirmisher";
		mInfo.mShorthandName = "Sk";
		mInfo.mDescriptions.add("When holding two swords, deal +25% melee damage to mobs with no other mobs within 2.5 blocks.");
		mInfo.mDescriptions.add("The damage bonus now also applies to mobs not targeting you, and the damage bonus is increased +40%.");
		mIsolatedPercentDamage = getAbilityScore() == 1 ? ISOLATED_PERCENT_DAMAGE_1 : ISOLATED_PERCENT_DAMAGE_2;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			LivingEntity mob = (LivingEntity) event.getEntity();
			Location loc = mob.getLocation();

			if (EntityUtils.getNearbyMobs(loc, SKIRMISHER_ISOLATION_RADIUS, mob).size() == 0
					|| getAbilityScore() > 1 && mob instanceof Mob && !mPlayer.equals(((Mob) mob).getTarget())) {
				mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
				mWorld.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1, 0.5f);
				mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
				loc.add(0, 1, 0);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 10, 0.35, 0.5, 0.35, 0.05);
				mWorld.spawnParticle(Particle.SPELL_MOB, loc, 10, 0.35, 0.5, 0.35, 0.00001);
				mWorld.spawnParticle(Particle.CRIT, loc, 10, 0.25, 0.5, 0.25, 0.55);

				event.setDamage(event.getDamage() * (1 + mIsolatedPercentDamage));
			}
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		return InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand);
	}
}

