package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.TemporalFlux;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellLingeringParadox extends Spell {

	private final LivingEntity mBoss;
	private final Location mStartLoc;
	private final int mRange;
	private final List<Entity> mSpawnedMobs = new ArrayList<>();

	public SpellLingeringParadox(LivingEntity boss, Location startLoc, int range) {
		mStartLoc = startLoc;
		mRange = range;
		mBoss = boss;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mStartLoc, mRange, true);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 100, 1.5f);
		Collections.shuffle(players);
		for (Player player : players) {
			if (!Plugin.getInstance().mEffectManager.hasEffect(player, TemporalFlux.class)) {
				players.forEach(p -> p.sendMessage(Component.text("[Imperial Construct]", NamedTextColor.GOLD).append(Component.text(" TEMPORAL SHIFT PROTOCOL INITIATED - PARADOX REDIRECTED TO TARGET:", NamedTextColor.WHITE))));
				Plugin.getInstance().mEffectManager.addEffect(player, TemporalFlux.GENERIC_NAME, new TemporalFlux(20 * 30));
				return;
			}
		}
	}

	public void spawnExchanger(Location loc) {
		mSpawnedMobs.add(LibraryOfSoulsIntegration.summon(loc, "TemporalExchanger"));
	}

	public void deleteExchangers() {
		for (Entity e : mSpawnedMobs) {
			e.remove();
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
