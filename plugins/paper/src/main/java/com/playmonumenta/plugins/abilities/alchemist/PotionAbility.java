package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.util.Vector;

public interface PotionAbility {

	AbilityTriggerInfo.TriggerRestriction HOLDING_ALCHEMIST_BAG_RESTRICTION =
		new AbilityTriggerInfo.TriggerRestriction("Holding an Alchemical Utensil", player -> ItemUtils.isAlchemistItem(player.getInventory().getItemInMainHand()));

	/**
	 * Called for every mob in the area when an Alchemist Potion splashes
	 *
	 * @param mob              The mob hit
	 * @param isGruesome       Whether this was a gruesome potion
	 * @param playerItemStats  Player item stats from when the potion was thrown
	 * @param level            The level of the effect that was applied. Can be used to force a certain level
	 * @param refreshBrutalDot Whether the brutal DoT effect should be refreshed, increasing its potency
	 */
	default void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats, int level, boolean refreshBrutalDot) {
	}

	/**
	 * Called for every player other than the Alchemist themselves in the area when an Alchemist Potion splashes
	 *
	 * @param player     The player hit
	 * @param potion     The thrown Alchemist Potion
	 * @param isGruesome Whether this was a gruesome potion
	 */
	default void applyToPlayer(Player player, ThrownPotion potion, boolean isGruesome) {
	}

	/**
	 * Called when an Alchemist Potion is thrown
	 *
	 * @param potion The thrown Alchemist Potion
	 */
	default void alchemistPotionThrown(ThrownPotion potion) {
	}

	/**
	 * Creates an effect where an Alchemist Potion lands.
	 *
	 * @param loc                    Real potion landing location, this is ahead of the potion's current location
	 * @param potion                 The Alchemist Potion that splashed
	 * @param originalPotionVelocity The velocity that the potion had when it splashed
	 * @param playerItemStats        Player item stats from when the potion was thrown
	 * @return Whether an aura was created. This is used to reduce splash effects of the potion for some cosmetics.
	 */
	default boolean createAura(Location loc, ThrownPotion potion, Vector originalPotionVelocity, ItemStatManager.PlayerItemStats playerItemStats) {
		return false;
	}

}
