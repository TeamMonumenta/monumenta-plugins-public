package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class UnlockEnchantmentListener implements Listener {

	private static final Advancement ENCHANTMENT_ROOT = Bukkit.getAdvancement(NamespacedKeyUtils.fromString("monumenta:handbook/enchantments/root"));

	@EventHandler(ignoreCancelled = false)
	public void onAdvancementUnlock(PlayerAdvancementDoneEvent evt) {
		Advancement adv = evt.getAdvancement();
		Player p = evt.getPlayer();
		if (isEnchantmentAdvancement(adv) && adv.getDisplay() != null) {
			p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
			// The advancement's displayName component includes the enchantment description as a hover-over
			p.sendMessage(Component.text("You've discovered a new enchantment: ", NamedTextColor.AQUA)
				.append(adv.displayName().replaceText(TextReplacementConfig.builder().match("\s{2,}").replacement("").build()))
			);
			p.sendMessage(Component.text("(Tip: Hover over the enchantment for more information)", NamedTextColor.GRAY));
		}
	}

	private boolean isEnchantmentAdvancement(Advancement a) {
		Advancement parent = a.getParent();
		if (ENCHANTMENT_ROOT == null) {
			MMLog.warning("UnlockEnchantmentListener could not find the root enchantment advancement!");
			return false;
		}
		while (parent != null) {
			if (parent.displayName().equals(ENCHANTMENT_ROOT.displayName())) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}
}
