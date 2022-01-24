package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import javax.annotation.Nullable;


public class WarlockPassive extends Ability {

	private static final int PASSIVE_DURATION = 6 * 20;

	public WarlockPassive(Plugin plugin, @Nullable Player player) {
		super(plugin, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 7;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (EntityUtils.isHostileMob(event.getEntity())
				&& ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
					new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, PASSIVE_DURATION, 0, false, true));
		}
	}
}
