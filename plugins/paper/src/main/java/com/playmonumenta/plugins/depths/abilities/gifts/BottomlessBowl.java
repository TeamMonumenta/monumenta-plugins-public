package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.Nullable;

public class BottomlessBowl extends DepthsAbility {
	public static final String ABILITY_NAME = "Bottomless Bowl";

	public static final DepthsAbilityInfo<BottomlessBowl> INFO =
		new DepthsAbilityInfo<>(BottomlessBowl.class, ABILITY_NAME, BottomlessBowl::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.BOWL)
			.floors(floor -> floor == 2)
			.descriptions(BottomlessBowl::getDescription);

	private final @Nullable DepthsPlayer mDepthsPlayer;

	public BottomlessBowl(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDepthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
	}

	private static Description<BottomlessBowl> getDescription() {
		return new DescriptionBuilder<BottomlessBowl>().add("You now have the choice to skip room rewards; doing so will grant you +5% healing and damage dealt.")
			.add((a, p) -> a != null && a.mDepthsPlayer != null
				? Component.text("\nCurrent bonus: " + StringUtils.multiplierToPercentageWithSign(a.mDepthsPlayer.mRewardSkips * 0.05))
				: Component.empty());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(mPlayer);
		if (dp != null) {
			double damageMultiplier = 1 + (dp.mRewardSkips * 0.05);
			event.updateDamageWithMultiplier(damageMultiplier);
		}
		return false;
	}

	@Override
	public void playerRegainHealthEvent(EntityRegainHealthEvent event) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(mPlayer);
		if (dp != null) {
			double healMultiplier = 1 + (dp.mRewardSkips * 0.05);
			event.setAmount(event.getAmount() * healMultiplier);
		}
	}
}
