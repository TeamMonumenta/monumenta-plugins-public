package com.playmonumenta.plugins.abilities.delves;

import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.ChargerBoss;
import com.playmonumenta.plugins.bosses.bosses.ProjectileBoss;
import com.playmonumenta.plugins.bosses.bosses.RejuvenationBoss;
import com.playmonumenta.plugins.bosses.bosses.TpBehindBoss;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;

public class Arcanic extends DelveModifier {

	private static final double[] ABILITY_CHANCE = {
		0.06,
		0.12,
		0.18,
		0.24,
		0.3,
		0.36,
		0.42,
		};

	private static final List<List<String>> ABILITY_POOL_R1;
	private static final List<List<String>> ABILITY_POOL_R2;

	private static final String TRACKING_SPELL_NAME = "Arcanic Missile";
	private static final String MAGIC_ARROW_SPELL_NAME = "Arcanic Arrow";
	private static final String CHARGE_SPELL_NAME = "Arcanic Charge";
	public static final ImmutableSet<String> SPELL_NAMES = ImmutableSet.of(TRACKING_SPELL_NAME, MAGIC_ARROW_SPELL_NAME, CHARGE_SPELL_NAME);

	static {
		ABILITY_POOL_R1 = new ArrayList<>();
		ABILITY_POOL_R2 = new ArrayList<>();

		//RejuvenationBoss
		List<String> rejuvenation = new ArrayList<>();
		rejuvenation.add(RejuvenationBoss.identityTag);
		ABILITY_POOL_R1.add(rejuvenation);
		ABILITY_POOL_R2.add(rejuvenation);

		//ProjectileBoss - tracking
		List<String> trackingProjectile = new ArrayList<>();
		trackingProjectile.add(ProjectileBoss.identityTag);
		trackingProjectile.add(ProjectileBoss.identityTag + "[damage=30,speed=0.2,delay=20,cooldown=320,turnradius=3.141,spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[soundstart=[(BLOCK_BEACON_POWER_SELECT,1,0.5)],soundlaunch=[(ENTITY_EVOKER_CAST_SPELL,1,0.5)],soundprojectile=[(BLOCK_BEACON_POWER_SELECT,0.4,0.5)],soundhit=[(BLOCK_BEACON_DEACTIVATE,1,0.5)]]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[particlelaunch=[(SPELL_WITCH,40,0,0,0,0.3)],particleprojectile=[(SPELL_WITCH,6,0,0,0,0.3),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],particlehit=[(SPELL_WITCH,50,0,0,0,0.3)]]");
		ABILITY_POOL_R2.add(trackingProjectile);
		trackingProjectile = new ArrayList<>();
		trackingProjectile.add(ProjectileBoss.identityTag);
		trackingProjectile.add(ProjectileBoss.identityTag + "[damage=15,speed=0.2,delay=20,cooldown=320,turnradius=3.141],spellname=\"" + TRACKING_SPELL_NAME + "\"");
		trackingProjectile.add(ProjectileBoss.identityTag + "[soundstart=[(BLOCK_BEACON_POWER_SELECT,1,0.5)],soundlaunch=[(ENTITY_EVOKER_CAST_SPELL,1,0.5)],soundprojectile=[(BLOCK_BEACON_POWER_SELECT,0.4,0.5)],soundhit=[(BLOCK_BEACON_DEACTIVATE,1,0.5)]]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[particlelaunch=[(SPELL_WITCH,40,0,0,0,0.3)],particleprojectile=[(SPELL_WITCH,6,0,0,0,0.3),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],particlehit=[(SPELL_WITCH,50,0,0,0,0.3)]]");
		ABILITY_POOL_R1.add(trackingProjectile);

		//TpBehindTargetedBoss
		List<String> tpBehind = new ArrayList<>();
		tpBehind.add(TpBehindBoss.identityTag);
		tpBehind.add(TpBehindBoss.identityTag + "[range=50,random=false]");
		ABILITY_POOL_R1.add(tpBehind);
		ABILITY_POOL_R2.add(tpBehind);

		//ChargerStrongBoss
		List<String> charger = new ArrayList<>();
		charger.add(ChargerBoss.identityTag);
		charger.add(ChargerBoss.identityTag + "[damage=30,spellname=\"" + CHARGE_SPELL_NAME + "\"]");
		ABILITY_POOL_R2.add(charger);
		charger = new ArrayList<>();
		charger.add(ChargerBoss.identityTag);
		charger.add(ChargerBoss.identityTag + "[damage=15,spellname=\"" + CHARGE_SPELL_NAME + "\"]");
		ABILITY_POOL_R1.add(charger);

		//ProjectileBoss - magic arrow
		List<String> magicArrow = new ArrayList<>();
		magicArrow.add(ProjectileBoss.identityTag);
		magicArrow.add(ProjectileBoss.identityTag + "[damage=30,distance=32,speed=0.8,delay=20,cooldown=160,turnradius=0,spellname=\"" + MAGIC_ARROW_SPELL_NAME + "\"]");
		magicArrow.add(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1)],soundlaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.5)]]");
		magicArrow.add(ProjectileBoss.identityTag + "[particlelaunch=[],particleprojectile=[(FIREWORKS_SPARK,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.25)]]");
		ABILITY_POOL_R2.add(magicArrow);
		magicArrow = new ArrayList<>();
		magicArrow.add(ProjectileBoss.identityTag);
		magicArrow.add(ProjectileBoss.identityTag + "[damage=15,distance=32,speed=0.8,delay=20,cooldown=160,turnradius=0,spellname=\"" + MAGIC_ARROW_SPELL_NAME + "\"]");
		magicArrow.add(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1)],soundlaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.5)]]");
		magicArrow.add(ProjectileBoss.identityTag + "[particlelaunch=[],particleprojectile=[(FIREWORKS_SPARK,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.25)]]");
		ABILITY_POOL_R1.add(magicArrow);
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
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[5] * 100) + "% chance to be Arcanic."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[6] * 100) + "% chance to be Arcanic."
			}
	};

	private final double mAbilityChance;

	public Arcanic(Plugin plugin, @Nullable Player player) {
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
			List<List<String>> abilityPool = ServerProperties.getClassSpecializationsEnabled() ? ABILITY_POOL_R2 : ABILITY_POOL_R1;
			List<String> ability = abilityPool.get(FastUtils.RANDOM.nextInt(abilityPool.size()));
			for (String abilityTag : ability) {
				mob.addScoreboardTag(abilityTag);
			}
		}
	}

}
