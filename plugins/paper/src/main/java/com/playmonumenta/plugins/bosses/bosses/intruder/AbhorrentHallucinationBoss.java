package com.playmonumenta.plugins.bosses.bosses.intruder;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellThrowSummon;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellAbhorrentHallucinationTeleport;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class AbhorrentHallucinationBoss extends BossAbilityGroup {
	public static final String TAG = "AbhorrentHallucination";

	public static final int DAMAGE_PER_MOB = 30;
	public static final String SUMMON_TAG = "AbhorrentHallucinationSpawn";

	public AbhorrentHallucinationBoss(Plugin plugin, LivingEntity boss, Location spawnlocation, List<String> elitesList) {
		super(plugin, TAG, boss);
		SpellAbhorrentHallucinationTeleport teleportSpell = new SpellAbhorrentHallucinationTeleport(boss, spawnlocation, 10);

		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();
		events.put(75, lBoss -> {
			teleportSpell.doTeleport();
			Objects.requireNonNull(LibraryOfSoulsIntegration.summon(boss.getLocation(), elitesList.get(0))).addScoreboardTag(SUMMON_TAG);
		});
		events.put(50, lBoss -> {
			teleportSpell.doTeleport();
			Objects.requireNonNull(LibraryOfSoulsIntegration.summon(boss.getLocation(), elitesList.get(1))).addScoreboardTag(SUMMON_TAG);
		});
		events.put(25, lBoss -> {
			teleportSpell.doTeleport();
			Objects.requireNonNull(LibraryOfSoulsIntegration.summon(boss.getLocation(), elitesList.get(2))).addScoreboardTag(SUMMON_TAG);
		});

		SpellManager spellManager = new SpellManager(List.of(
			new SpellThrowSummon(
				plugin, boss, new EntityTargets(EntityTargets.TARGETS.PLAYER, 100, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.NOT_STEALTHED)),
				1, 8 * 20, "~FacelessArmada",
				true, 3, 0, 1.0f,
				25, 0, 0.8, 200,
				20, false, "", false,
				ParticlesList.builder()
					.add(new ParticlesList.CParticle(Particle.DUST_COLOR_TRANSITION, 25, 0.5, 5.0, 0.5, 1.0,
						new Particle.DustTransition(Color.BLACK, Color.fromRGB(0x6b0000), 5.0f)))
					.build(),
				SoundsList.EMPTY)
		));
		BossBarManager bossBar = new BossBarManager(boss, IntruderBoss.DETECTION_RANGE, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_12, events, false, true);

		constructBoss(spellManager, List.of(teleportSpell), IntruderBoss.DETECTION_RANGE, bossBar, 2 * 20);
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		if (event.getSpell() instanceof SpellThrowSummon) {
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 3.0f, 0.5f, 45);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 2.0f, 1.4f, 4);
		}
	}
}
