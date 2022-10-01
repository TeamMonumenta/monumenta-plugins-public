package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CharmListener implements Listener {
	Plugin mPlugin;

	public CharmListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoin(PlayerJoinEvent event) {
		//Load charm data from plugin data
		CharmManager.getInstance().onJoin(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void playerQuit(PlayerQuitEvent event) {
		//Discard local data a few ticks later
		CharmManager.getInstance().onQuit(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {
		//Save local data to charm plugin data
		CharmManager.getInstance().onSave(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onCustomDamage(DamageEvent event) {
		//Only runs on r3 shards
		if (!ServerProperties.getAbilityEnhancementsEnabled() || !(event.getDamager() instanceof Player)) {
			return;
		}
		Player caster = (Player) event.getDamager();
		if (CharmManager.getInstance().mPlayerAbilityEffectMap.get(caster.getUniqueId()) != null) {
			for (Effect effectToCopy : CharmManager.getInstance().mPlayerAbilityEffectMap.get(caster.getUniqueId()).get(event.getAbility())) {
				if (effectToCopy instanceof PercentSpeed) {
					caster.sendMessage(effectToCopy.toString());
					double amplifier = effectToCopy.getMagnitude();
					if (((PercentSpeed) effectToCopy).isSlow()) {
						amplifier *= -1;
					}
					EntityUtils.applySlow(Plugin.getInstance(), effectToCopy.getDuration(), amplifier, event.getDamagee());
				}
				// TODO add more effects

			}
		}
	}
}
