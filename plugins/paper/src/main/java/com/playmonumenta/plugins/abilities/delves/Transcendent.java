package com.playmonumenta.plugins.abilities.delves;

import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CommanderBoss;
import com.playmonumenta.plugins.bosses.bosses.ProjectileBoss;
import com.playmonumenta.plugins.bosses.bosses.WrathBoss;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class Transcendent extends DelveModifier {

	private static final double[] ABILITY_CHANCE = {
		0.15,
		0.3,
		0.45,
		0.6,
		0.75,
		0.9
	};

	private static final List<List<String>> ABILITY_POOL_MELEE_R1;
	private static final List<List<String>> ABILITY_POOL_MELEE_R2;

	private static final List<List<String>> ABILITY_POOL_R1;
	private static final List<List<String>> ABILITY_POOL_R2;

	private static final String TRACKING_SPELL_NAME = "Transcendental Missile";
	private static final String WRATH_SPELL_NAME = "Transcendental Wrath";
	public static final ImmutableSet<String> SPELL_NAMES = ImmutableSet.of(TRACKING_SPELL_NAME, WRATH_SPELL_NAME);

	static {
		ABILITY_POOL_MELEE_R1 = new ArrayList<>();
		ABILITY_POOL_MELEE_R2 = new ArrayList<>();

		ABILITY_POOL_R1 = new ArrayList<>();
		ABILITY_POOL_R2 = new ArrayList<>();

		List<String> seekingProjectileBoss = new ArrayList<>();
		seekingProjectileBoss.add(ProjectileBoss.identityTag);
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[damage=8,distance=32,speed=0.5,delay=10,cooldown=25,turnradius=0.04,effects=[(pushforce,0.5)],spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[soundstart=[],soundlaunch=[(ENTITY_ILLUSIONER_CAST_SPELL,1,0.5)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,0.5)]]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[particlelaunch=[(EXPLOSION_LARGE,1)],particleprojectile=[(FIREWORKS_SPARK,3,0,0,0,0.1),(SPELL_WITCH,10,0.2,0.2,0.2,0),(END_ROD,2,0.2,0.2,0.2,0)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.3)]]");
		ABILITY_POOL_MELEE_R1.add(seekingProjectileBoss);
		ABILITY_POOL_R1.add(seekingProjectileBoss);
		seekingProjectileBoss = new ArrayList<>();
		seekingProjectileBoss.add(ProjectileBoss.identityTag);
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[damage=16,distance=32,speed=0.7,delay=10,cooldown=25,turnradius=0.06,effects=[(pushforce,0.5)],spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[soundstart=[],soundlaunch=[(ENTITY_ILLUSIONER_CAST_SPELL,1,0.5)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,0.5)]]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[particlelaunch=[(EXPLOSION_LARGE,1)],particleprojectile=[(FIREWORKS_SPARK,3,0,0,0,0.1),(SPELL_WITCH,10,0.2,0.2,0.2,0),(END_ROD,2,0.2,0.2,0.2,0)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.3)]]");
		ABILITY_POOL_MELEE_R2.add(seekingProjectileBoss);
		ABILITY_POOL_R2.add(seekingProjectileBoss);

		List<String> commanderBoss = new ArrayList<>();
		commanderBoss.add(CommanderBoss.identityTag);
		ABILITY_POOL_MELEE_R1.add(commanderBoss);
		ABILITY_POOL_R1.add(commanderBoss);
		ABILITY_POOL_MELEE_R2.add(commanderBoss);
		ABILITY_POOL_R2.add(commanderBoss);

		List<String> wrathBoss = new ArrayList<>();
		wrathBoss.add(WrathBoss.identityTag);
		wrathBoss.add(WrathBoss.identityTag + "[damage=9,spellname=\"" + WRATH_SPELL_NAME + "\"]");
		ABILITY_POOL_MELEE_R1.add(wrathBoss);
		wrathBoss = new ArrayList<>();
		wrathBoss.add(WrathBoss.identityTag);
		wrathBoss.add(WrathBoss.identityTag + "[damage=18,spellname=\"" + WRATH_SPELL_NAME + "\"]");
		ABILITY_POOL_MELEE_R2.add(commanderBoss);

	}

	public static final String DESCRIPTION = "Elites become greatly empowered.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Elites have a " + Math.round(ABILITY_CHANCE[0] * 100) + "% chance to be Transcendent."
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[1] * 100) + "% chance to be Transcendent."
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[2] * 100) + "% chance to be Transcendent."
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[3] * 100) + "% chance to be Transcendent."
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[4] * 100) + "% chance to be Transcendent."
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[5] * 100) + "% chance to be Transcendent."
			}
	};

	private final double mAbilityChance;

	public Transcendent(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.TRANSCENDENT);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.TRANSCENDENT);
			mAbilityChance = ABILITY_CHANCE[rank - 1];
		} else {
			mAbilityChance = 0;
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (EntityUtils.isElite(mob) && !DelvesUtils.isDelveMob(mob) && FastUtils.RANDOM.nextDouble() < mAbilityChance) {
			EntityEquipment equipment = mob.getEquipment();
			ItemStack mainhand = equipment == null ? null : equipment.getItemInMainHand();
			Material material = mainhand == null ? null : mainhand.getType();
			if (material == Material.BOW || material == Material.CROSSBOW || material == Material.TRIDENT
				    || mob instanceof Evoker) {
				List<List<String>> abilityPool = ServerProperties.getClassSpecializationsEnabled() ? ABILITY_POOL_R2 : ABILITY_POOL_R1;
				List<String> ability = abilityPool.get(FastUtils.RANDOM.nextInt(abilityPool.size()));
				for (String abilityTag : ability) {
					mob.addScoreboardTag(abilityTag);
				}
			} else {
				List<List<String>> abilityPool = ServerProperties.getClassSpecializationsEnabled() ? ABILITY_POOL_MELEE_R2 : ABILITY_POOL_MELEE_R1;
				List<String> ability = abilityPool.get(FastUtils.RANDOM.nextInt(abilityPool.size()));
				for (String abilityTag : ability) {
					mob.addScoreboardTag(abilityTag);
				}
			}
		}
	}

}
