package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class WarlockPassive extends Ability {

	private static final int PASSIVE_DURATION = 6 * 20;

	public WarlockPassive(Plugin plugin, World world, Player player) {
		super(plugin, world, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 7;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (EntityUtils.isHostileMob(event.getEntity())
				&& InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
					new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, PASSIVE_DURATION, 0, false, true));
		}
	}

}
