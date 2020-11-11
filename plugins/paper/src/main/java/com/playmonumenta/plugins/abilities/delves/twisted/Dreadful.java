package com.playmonumenta.plugins.abilities.delves.twisted;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.abilities.delves.cursed.Spectral;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * DREADFUL: You take x2 damage.
 * Mobs turn into Spectres on death 25% of the time.
 * Elites turn into Dreadnaughts on death 50% of the time.
 */

public class Dreadful extends StatMultiplier {

	public static final String MESSAGE = ChatColor.GRAY + "The air reeks of death, heralding a " + ChatColor.DARK_RED + ChatColor.BOLD + "DREADFUL" + ChatColor.GRAY + " fate for the fallen.";
	public static final int SCORE = 24;

	public static final String DREADFUL_DREADNAUGHT_TAG = "boss_dreadnaughtparticle";
	public static final String DREADFUL_DREADLING_TAG = "boss_dreadling";

	private static final String DREADFUL_1_SUMMON_COMMAND_DATA = "NarsenDreadnaught";
	private static final String DREADFUL_2_SUMMON_COMMAND_DATA = "CelsianDreadnaught";

	private static final int DREADFUL_SPAWN_COUNTER_SPAWN = 10;
	private static final double DREADFUL_DAMAGE_TAKEN_MULTIPLIER = 2;

	private final String mSpectralSummonCommandData;
	private final String mDreadfulSummonCommandData;

	private int mSpawnCounter = 0;
	private int mEliteSpawnCounter = 0;

	public Dreadful(Plugin plugin, Player player) {
		super(plugin, player, DREADFUL_DAMAGE_TAKEN_MULTIPLIER, DREADFUL_DAMAGE_TAKEN_MULTIPLIER, 1);
		mSpectralSummonCommandData = ServerProperties.getClassSpecializationsEnabled() ? Spectral.SPECTRAL_2_SUMMON_COMMAND_DATA : Spectral.SPECTRAL_1_SUMMON_COMMAND_DATA;
		mDreadfulSummonCommandData = ServerProperties.getClassSpecializationsEnabled() ? DREADFUL_2_SUMMON_COMMAND_DATA : DREADFUL_1_SUMMON_COMMAND_DATA;
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, SCORE);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity mob = event.getEntity();

		if (!mob.getScoreboardTags().contains(DREADFUL_DREADNAUGHT_TAG)
				&& !mob.getScoreboardTags().contains(Spectral.SPECTRAL_SPECTRE_TAG)
				&& !mob.getScoreboardTags().contains(DREADFUL_DREADLING_TAG)) {
			World world = mPlayer.getWorld();
			if (EntityUtils.isElite(mob)) {
				mEliteSpawnCounter += (4 + FastUtils.RANDOM.nextInt(3));

				if (mEliteSpawnCounter >= DREADFUL_SPAWN_COUNTER_SPAWN) {
					mEliteSpawnCounter -= DREADFUL_SPAWN_COUNTER_SPAWN;
					Location loc = mob.getLocation();
					LibraryOfSoulsIntegration.summon(loc, mDreadfulSummonCommandData);

					loc.add(0, 1, 0);
					world.spawnParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.1);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0);
				}
			} else {
				mSpawnCounter += (2 + FastUtils.RANDOM.nextInt(2));

				if (mSpawnCounter >= DREADFUL_SPAWN_COUNTER_SPAWN) {
					mSpawnCounter -= DREADFUL_SPAWN_COUNTER_SPAWN;
					Location loc = mob.getLocation();
					LibraryOfSoulsIntegration.summon(loc, mSpectralSummonCommandData);

					loc.add(0, 1, 0);
					world.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0, 0, 0, 0.5);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0);
				}
			}
		}
	}

}
