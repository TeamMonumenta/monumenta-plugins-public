package com.playmonumenta.plugins.abilities.rogue;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Skirmisher extends Ability {

	private static final double PASSIVE_DAMAGE_ELITE_MODIFIER = 2.0;
	private static final double PASSIVE_DAMAGE_BOSS_MODIFIER = 1.25;
	private static final int SKIRMISHER_1_DAMAGE = 3;
	private static final int SKIRMISHER_2_DAMAGE = 5;
	private static final int SKIRMISHER_ISOLATION_RADIUS = 4;

	public Skirmisher(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Skirmisher");
		mInfo.scoreboardId = "Skirmisher";
		mInfo.mShorthandName = "Sk";
		mInfo.mDescriptions.add("When holding two swords, deal an additional 3 melee damage to mobs with no other mobs within 4 blocks.");
		mInfo.mDescriptions.add("Deal an additional 5 melee damage instead.");
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && event.getEntity() instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) event.getEntity();
			Location loc = mob.getLocation();

			if (EntityUtils.getNearbyMobs(loc, SKIRMISHER_ISOLATION_RADIUS, mob).size() == 0) {
				mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
				mWorld.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1, 0.5f);
				mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 10, 0.35, 0.5, 0.35, 0.05);
				mWorld.spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 10, 0.35, 0.5, 0.35, 0.00001);
				mWorld.spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 10, 0.25, 0.5, 0.25, 0.55);

				// Not a CustomDamageEvent (similar to By My Blade)
				int damage = getAbilityScore() == 1 ? SKIRMISHER_1_DAMAGE : SKIRMISHER_2_DAMAGE;
				if (EntityUtils.isElite(mob)) {
					damage *= PASSIVE_DAMAGE_ELITE_MODIFIER;
				} else if (EntityUtils.isBoss(mob)) {
					damage *= PASSIVE_DAMAGE_BOSS_MODIFIER;
				}
				event.setDamage(event.getDamage() + damage);
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

