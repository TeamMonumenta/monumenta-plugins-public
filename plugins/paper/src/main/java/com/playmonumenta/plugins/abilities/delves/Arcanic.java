package com.playmonumenta.plugins.abilities.delves;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.ChargerBoss;
import com.playmonumenta.plugins.bosses.bosses.ProjectileBoss;
import com.playmonumenta.plugins.bosses.bosses.RejuvenationBoss;
import com.playmonumenta.plugins.bosses.bosses.TpBehindBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;

public class Arcanic extends DelveModifier {

	private static final double[] ABILITY_CHANCE = {
			0.06,
			0.12,
			0.18,
			0.24,
			0.3
	};

	private static final List<List<String>> ABILITY_POOL;


	static {
		ABILITY_POOL = new ArrayList<>();

		//RejuvenationBoss
		List<String> rejuvenation = new ArrayList<>();
		rejuvenation.add(RejuvenationBoss.identityTag);
		ABILITY_POOL.add(rejuvenation);

		//ProjectileBoss - tracking
		List<String> trackingProjectile = new ArrayList<>();
		trackingProjectile.add(ProjectileBoss.identityTag);
		trackingProjectile.add(ProjectileBoss.identityTag + "[damage=30,speed=0.2,delay=20,cooldown=320,turnRadius=3.141]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[SoundStart=[(BLOCK_BEACON_POWER_SELECT,1,0.5)],SoundLaunch=[(ENTITY_EVOKER_CAST_SPELL,1,0.5)],SoundProjectile=[(BLOCK_BEACON_POWER_SELECT,0.4,0.5)],SoundHit=[(BLOCK_BEACON_DEACTIVATE,1,0.5)]]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[ParticleLaunch=[(SPELL_WITCH,40,0,0,0,0.3)],ParticleProjectile=[(SPELL_WITCH,6,0,0,0,0.3),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],ParticleHit=[(SPELL_WITCH,50,0,0,0,0.3)]]");
		ABILITY_POOL.add(trackingProjectile);

		//TpBehindTargetedBoss
		List<String> tpBehind = new ArrayList<>();
		tpBehind.add(TpBehindBoss.identityTag);
		tpBehind.add(TpBehindBoss.identityTag + "[range=50,random=false]");
		ABILITY_POOL.add(tpBehind);

		//ChargerStrongBoss
		List<String> charger = new ArrayList<>();
		charger.add(ChargerBoss.identityTag);
		charger.add(ChargerBoss.identityTag + "[damage=37.5]");
		ABILITY_POOL.add(charger);

		//ProjectileBoss - magic arrow
		List<String> magicArrow = new ArrayList<>();
		magicArrow.add(ProjectileBoss.identityTag);
		magicArrow.add(ProjectileBoss.identityTag + "[damage=30,distance=32,speed=0.8,delay=20,cooldown=160,turnRadius=0]");
		magicArrow.add(ProjectileBoss.identityTag + "[SoundStart=[(ENTITY_FIREWORK_ROCKET_LAUNCH,0.01,1)],SoundLaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],SoundProjectile=[(ENTITY_BLAZE_BURN,0)],SoundHit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.5)]]");
		magicArrow.add(ProjectileBoss.identityTag + "[ParticleLaunch=[(FIREWORKS_SPARK,0)],ParticleProjectile=[(FIREWORKS_SPARK,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)],ParticleHit=[(FIREWORKS_SPARK,30,0,0,0,0.25)]]");
		ABILITY_POOL.add(magicArrow);
	}

	public static final String DESCRIPTION = "Enemies gain magical abilities.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(ABILITY_CHANCE[0] * 100) + "% chance to be Arcanic."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[1] * 100) + "% chance to be Arcanic."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[2] * 100) + "% chance to be Arcanic."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[3] * 100) + "% chance to be Arcanic."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[4] * 100) + "% chance to be Arcanic."
			}
	};

	private final double mAbilityChance;

	public Arcanic(Plugin plugin, Player player) {
		super(plugin, player, Modifier.ARCANIC);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.ARCANIC);
			mAbilityChance = ABILITY_CHANCE[rank - 1];
		} else {
			mAbilityChance = 0;
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mAbilityChance) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			List<String> ability = ABILITY_POOL.get(FastUtils.RANDOM.nextInt(ABILITY_POOL.size()));
			for (String abilityTag: ability) {
				mob.addScoreboardTag(abilityTag);
			}
		}
	}

}
