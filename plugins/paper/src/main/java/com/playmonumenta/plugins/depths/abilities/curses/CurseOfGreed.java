package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsRarity;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class CurseOfGreed extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Greed";
	public static final String MODIFIER_NAME = "CurseOfGreedPercentHealthModifier";
	public static final double PERCENT_MAX_HEALTH = -0.05;

	public static final DepthsAbilityInfo<CurseOfGreed> INFO =
		new DepthsAbilityInfo<>(CurseOfGreed.class, ABILITY_NAME, CurseOfGreed::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.remove(player -> EntityUtils.removeAttribute(player, Attribute.GENERIC_MAX_HEALTH, MODIFIER_NAME))
			.displayItem(Material.BEETROOT)
			.descriptions(CurseOfGreed::getDescription)
			.singleCharm(false);

	private final double mPercentMaxHealth;

	public CurseOfGreed(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			mPercentMaxHealth = 0;
			return;
		}
		int abilities = 0;
		for (String ability : dp.mAbilities.keySet()) {
			if (dp.getLevelInAbility(ability) >= 5) {
				DepthsAbilityInfo<?> info = DepthsManager.getInstance().getAbility(ability);
				if (info != null && info.getHasLevels()) {
					abilities++;
				}
			}
		}
		mPercentMaxHealth = Math.max(PERCENT_MAX_HEALTH * abilities, -0.99);
		EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
			new AttributeModifier(MODIFIER_NAME, mPercentMaxHealth, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	private static Description<CurseOfGreed> getDescription() {
		return new DescriptionBuilder<CurseOfGreed>()
			.add("Lose ")
			.addPercent(-PERCENT_MAX_HEALTH)
			.add(" max health for each ability you have at ")
			.add(DepthsRarity.LEGENDARY.getDisplay())
			.add(" rarity or higher.")
			.add((a, p) -> a != null ? Component.text("\nCurrent: " + StringUtils.multiplierToPercentageWithSign(a.mPercentMaxHealth) + " health") : Component.empty());
	}
}
