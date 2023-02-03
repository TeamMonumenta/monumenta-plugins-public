package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;

public abstract class PotionAbility extends Ability {

	public static final AbilityTriggerInfo.TriggerRestriction HOLDING_ALCHEMIST_BAG_RESTRICTION =
		new AbilityTriggerInfo.TriggerRestriction("holding an Alchemist's Bag", player -> ItemUtils.isAlchemistItem(player.getInventory().getItemInMainHand()));

	public PotionAbility(Plugin plugin, Player player,
	                     AbilityInfo<? extends PotionAbility> info) {
		super(plugin, player, info);
	}

	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {

	}

	public void applyToPlayer(Player player, ThrownPotion potion, boolean isGruesome) {

	}

	public void createAura(Location loc, ThrownPotion potion, ItemStatManager.PlayerItemStats playerItemStats) {

	}
}
