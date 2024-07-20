package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.events.EffectTypeApplyFromPotionEvent;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class CurseOfSobriety extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Sobriety";

	public static final DepthsAbilityInfo<CurseOfSobriety> INFO =
		new DepthsAbilityInfo<>(CurseOfSobriety.class, ABILITY_NAME, CurseOfSobriety::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.GLASS_BOTTLE)
			.descriptions(CurseOfSobriety::getDescription);

	public CurseOfSobriety(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		List<EffectManager.EffectPair> pairs = mPlugin.mEffectManager.getAllEffectPairs(mPlayer);
		if (pairs != null) {
			for (EffectManager.EffectPair pair : pairs) {
				// This theoretically might catch a few extra things but none are important enough to care
				if (EffectType.isEffectTypeAppliedEffect(pair.mSource)) {
					pair.mEffect.setDuration(0);
				}
			}
		}
		for (PotionEffectType type : PotionUtils.POSITIVE_EFFECTS) {
			if (Objects.equals(type, PotionEffectType.GLOWING)) {
				continue;
			}
			mPlugin.mPotionManager.clearPotionEffectType(mPlayer, type);
		}
	}

	@Override
	public void effectTypeApplyFromPotionEvent(EffectTypeApplyFromPotionEvent event) {
		if (event.getEffectType().isPositive()) {
			event.setCancelled(true);
		}
	}

	public static Description<CurseOfSobriety> getDescription() {
		return new DescriptionBuilder<CurseOfSobriety>()
			.add("All positive potion effects are nullified.");
	}
}
