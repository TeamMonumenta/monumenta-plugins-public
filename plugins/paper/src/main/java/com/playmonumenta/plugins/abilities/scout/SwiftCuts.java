package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.Location;
import org.bukkit.Material;
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

public class SwiftCuts extends Ability {

	private static final double CONSECUTIVE_PERCENT_DAMAGE_1 = 0.2;
	private static final double CONSECUTIVE_PERCENT_DAMAGE_2 = 0.35;

	private final double mConsecutivePercentDamage;

	private LivingEntity mLastTarget = null;

	public SwiftCuts(Plugin plugin, Player player) {
		super(plugin, player, "Swift Cuts");
		mInfo.mScoreboardId = "SwiftCuts";
		mInfo.mShorthandName = "SC";
		mInfo.mDescriptions.add("If you perform a melee attack on the same mob 2 or more times in a row, each hit after the first does +20% damage.");
		mInfo.mDescriptions.add("Bonus damage increased to +35%.");
		mDisplayItem = new ItemStack(Material.STONE_SWORD, 1);
		mConsecutivePercentDamage = getAbilityScore() == 1 ? CONSECUTIVE_PERCENT_DAMAGE_1 : CONSECUTIVE_PERCENT_DAMAGE_2;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			LivingEntity mob = (LivingEntity) event.getEntity();

			if (mob.equals(mLastTarget)) {
				Location loc = mob.getLocation();
				World world = mPlayer.getWorld();
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
				world.spawnParticle(Particle.SWEEP_ATTACK, loc, 2, 0.25, 0.35, 0.25, 0.001);

				event.setDamage(event.getDamage() * (1 + mConsecutivePercentDamage));
			} else {
				mLastTarget = mob;
			}
		}

		return true;
	}

}
