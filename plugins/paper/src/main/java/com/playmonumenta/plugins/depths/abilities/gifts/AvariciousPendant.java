package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class AvariciousPendant extends DepthsAbility {
	public static final String ABILITY_NAME = "Avaricious Pendant";

	public static final DepthsAbilityInfo<AvariciousPendant> INFO =
		new DepthsAbilityInfo<>(AvariciousPendant.class, ABILITY_NAME, AvariciousPendant::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.GOLD_NUGGET)
			.floors(floor -> floor == 1)
			.descriptions(AvariciousPendant::getDescription);

	public AvariciousPendant(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void increaseTreasure(DepthsPlayer dp) {
		dp.mBonusTreasureScore++;
		Player p = dp.getPlayer();
		if (p != null) {
			p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0f, 1.0f);
			p.sendActionBar(Component.text("1 treasure score added to personal score!", NamedTextColor.GOLD));
		}
	}

	private static Description<AvariciousPendant> getDescription() {
		return new DescriptionBuilder<AvariciousPendant>().add("Every time you conquer an elite room or boss, gain one treasure score.");
	}
}
