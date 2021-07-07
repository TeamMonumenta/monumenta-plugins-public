package com.playmonumenta.plugins.abilities.rogue;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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

	private static final double GROUPED_FLAT_DAMAGE = 1;
	private static final double GROUPED_FLAT_DAMAGE_2 = 2;
	private static final double GROUPED_PERCENT_DAMAGE_1 = 0.1;
	private static final double GROUPED_PERCENT_DAMAGE_2 = 0.15;
	private static final double SKIRMISHER_FRIENDLY_RADIUS = 2.5;
	private static final int MOB_COUNT_CUTOFF = 1;

	private final double mIsolatedPercentDamage;
	private final double mIsolatedFlatDamage;

	public Skirmisher(Plugin plugin, Player player) {
		super(plugin, player, "Skirmisher");
		mInfo.mScoreboardId = "Skirmisher";
		mInfo.mShorthandName = "Sk";
		mInfo.mDescriptions.add("When holding two swords, deal 1 + 10% final damage to mobs when other mobs are within 2.5 blocks.");
		mInfo.mDescriptions.add("The damage bonus now also applies to mobs not targeting you, and the damage bonus is increased to 2 + 15% final damage done");
		mIsolatedPercentDamage = getAbilityScore() == 1 ? GROUPED_PERCENT_DAMAGE_1 : GROUPED_PERCENT_DAMAGE_2;
		mIsolatedFlatDamage = getAbilityScore() == 1 ? GROUPED_FLAT_DAMAGE : GROUPED_FLAT_DAMAGE_2;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			LivingEntity mob = (LivingEntity) event.getEntity();
			Location loc = mob.getLocation();

			if (EntityUtils.getNearbyMobs(loc, SKIRMISHER_FRIENDLY_RADIUS, mob).size() >= MOB_COUNT_CUTOFF
					|| getAbilityScore() > 1 && mob instanceof Mob && !mPlayer.equals(((Mob) mob).getTarget())) {
				World world = mPlayer.getWorld();
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
				world.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1, 0.5f);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
				loc.add(0, 1, 0);
				world.spawnParticle(Particle.SMOKE_NORMAL, loc, 10, 0.35, 0.5, 0.35, 0.05);
				world.spawnParticle(Particle.SPELL_MOB, loc, 10, 0.35, 0.5, 0.35, 0.00001);
				world.spawnParticle(Particle.CRIT, loc, 10, 0.25, 0.5, 0.25, 0.55);

				event.setDamage((event.getDamage() + mIsolatedFlatDamage) * (1 + mIsolatedPercentDamage));
			}
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		return InventoryUtils.rogueTriggerCheck(mainHand, offHand);
	}
}

