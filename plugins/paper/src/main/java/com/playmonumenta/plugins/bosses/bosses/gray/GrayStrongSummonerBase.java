package com.playmonumenta.plugins.bosses.bosses.gray;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSummon;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class GrayStrongSummonerBase extends BossAbilityGroup {
	private static final int SUMMON_TIME = 200;
	private static final int TIME_BETWEEN_CASTS = 700;
	private static final int PLAYER_RADIUS = 7;
	private static final int SPAWNS_PER_PLAYER = 3;
	private static final int PLAYER_RANGE = 32;
	private static final int MAX_NEARBY_SUMMONS = 15;

	GrayStrongSummonerBase(Plugin plugin, LivingEntity boss, String identityTag, int detectionRange, String mobName) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob)) {
			throw new Exception("gray boss tags only work on mobs!");
		}

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSummon(plugin, boss, TIME_BETWEEN_CASTS, SUMMON_TIME, PLAYER_RADIUS, 1.8f, false, true, false,
				() -> {
					List<Entity> nearbyEntities = boss.getNearbyEntities(PLAYER_RANGE, PLAYER_RANGE, PLAYER_RANGE);

					if (nearbyEntities.stream().filter(
							e -> e.getScoreboardTags().contains(GraySummoned.identityTag)
						).count() > MAX_NEARBY_SUMMONS) {
						return 0;
					}

					for (Player player : PlayerUtils.playersInRange(boss.getLocation(), PLAYER_RANGE, true)) {
						if (LocationUtils.hasLineOfSight(boss, player)) {
							return SPAWNS_PER_PLAYER;
						}
					}

					return 0;
				}, () -> {
					// Run on some number of nearby players. Scale a bit below linear to avoid insane spam
					List<Player> targets = PlayerUtils.playersInRange(boss.getLocation(), PLAYER_RANGE, true);
					List<Location> targetLoc = new ArrayList<>();
					Collections.shuffle(targets);
					switch (targets.size()) {
					case 0:
					case 1:
					case 2:
						for (Player player : targets) {
							targetLoc.add(player.getLocation());
						}
						return targetLoc;
					case 3:
					case 4:
						targets.remove(0);
						for (Player player : targets) {
							targetLoc.add(player.getLocation());
						}
						return targetLoc;
					case 5:
					case 6:
						targets.remove(0);
						targets.remove(0);
						for (Player player : targets) {
							targetLoc.add(player.getLocation());
						}
						return targetLoc;
					case 7:
					case 8:
						targets.remove(0);
						targets.remove(0);
						targets.remove(0);
						for (Player player : targets) {
							targetLoc.add(player.getLocation());
						}
						return targetLoc;
					default:
						for (Player player : targets.subList(0, 5)) {
							targetLoc.add(player.getLocation());
						}
						return targetLoc;
					}
				}, (summonLoc, times) -> {
					summonLoc.getWorld().playSound(summonLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
					Entity entity = LibraryOfSoulsIntegration.summon(summonLoc, mobName);
					return entity;
				}, (LivingEntity bos, Location loc, int ticks) -> {
					if (ticks == 0) {
						boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.HOSTILE, 1.5f, 1.0f);
					}
					loc.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0);

				}, (LivingEntity bos, Location loc, int ticks) -> {
					if (ticks == 0) {
						loc.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0);
					}
				})));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}
