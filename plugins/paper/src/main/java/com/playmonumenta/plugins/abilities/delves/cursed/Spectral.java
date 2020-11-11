package com.playmonumenta.plugins.abilities.delves.cursed;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * SPECTRAL: You take x1.6 damage.
 * Mobs turn into Spectres on death 15% of the time.
 */

public class Spectral extends StatMultiplier {

	public static final String MESSAGE = ChatColor.GRAY + "The air reeks of death, heralding a " + ChatColor.RED + ChatColor.BOLD + "SPECTRAL" + ChatColor.GRAY + " fate for the fallen.";
	public static final int SCORE = 14;

	public static final String SPECTRAL_SPECTRE_TAG = "boss_spectreparticle";

	public static final String SPECTRAL_1_SUMMON_COMMAND_DATA = "NarsenSpectre";
	public static final String SPECTRAL_2_SUMMON_COMMAND_DATA = "CelsianSpectre";

	private static final int SPECTRAL_SPAWN_COUNTER_SPAWN = 20;
	private static final double SPECTRAL_DAMAGE_TAKEN_MULTIPLIER = 1.6;

	private final String mSpectralSummonCommandData;

	private int mSpawnCounter = 0;

	public Spectral(Plugin plugin, Player player) {
		super(plugin, player, SPECTRAL_DAMAGE_TAKEN_MULTIPLIER, SPECTRAL_DAMAGE_TAKEN_MULTIPLIER, 1);
		mSpectralSummonCommandData = ServerProperties.getClassSpecializationsEnabled() ? SPECTRAL_2_SUMMON_COMMAND_DATA : SPECTRAL_1_SUMMON_COMMAND_DATA;
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, SCORE);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity mob = event.getEntity();

		if (!mob.getScoreboardTags().contains(SPECTRAL_SPECTRE_TAG)) {
			mSpawnCounter += (2 + FastUtils.RANDOM.nextInt(3));

			if (mSpawnCounter >= SPECTRAL_SPAWN_COUNTER_SPAWN) {
				mSpawnCounter -= SPECTRAL_SPAWN_COUNTER_SPAWN;

				Location loc = mob.getLocation();
				LibraryOfSoulsIntegration.summon(loc, mSpectralSummonCommandData);

				loc.add(0, 1, 0);
				World world = mPlayer.getWorld();
				world.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0, 0, 0, 0.5);
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0);
			}
		}
	}

}
