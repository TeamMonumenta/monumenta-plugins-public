package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.TemporalFlux;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
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
import org.bukkit.entity.Shulker;

public class SpellLingeringParadox extends Spell {
	private static final int RANGE = 30;

	private final LivingEntity mBoss;
	private final ImperialConstruct mConstruct;
	private final Location mStartLoc;
	private final List<Entity> mSpawnedMobs = new ArrayList<>();

	public SpellLingeringParadox(LivingEntity boss, ImperialConstruct construct, Location startLoc) {
		mStartLoc = startLoc;
		mConstruct = construct;
		mBoss = boss;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mStartLoc, RANGE, true);
		players.removeIf(p -> !mConstruct.isInArena(p));
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 100, 1.5f);
		Collections.shuffle(players);
		for (Player player : players) {
			if (!Plugin.getInstance().mEffectManager.hasEffect(player, TemporalFlux.class)) {
				mConstruct.sendMessage(Component.text("[Imperial Construct]", NamedTextColor.GOLD).append(Component.text(" TEMPORAL SHIFT PROTOCOL INITIATED - PARADOX REDIRECTED TO TARGET:", NamedTextColor.WHITE)));
				Plugin.getInstance().mEffectManager.addEffect(player, TemporalFlux.GENERIC_NAME, new TemporalFlux(20 * 30));
				return;
			}
		}
	}

	public void spawnExchanger(Location loc) {
		Entity entity = LibraryOfSoulsIntegration.summon(loc, "TemporalExchanger");
		if (entity instanceof Shulker shulker) {
			// THIS DOESN'T WORK FOR PISTONS ;-;
			shulker.setCollidable(false);
			shulker.addScoreboardTag(AbilityUtils.IGNORE_TAG);
			shulker.addScoreboardTag("UNPUSHABLE");
			shulker.addScoreboardTag("SkillImmune");
			ScoreboardUtils.addEntityToTeam(shulker, TrackingManager.UNPUSHABLE_TEAM);
			mSpawnedMobs.add(shulker);
		}
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
