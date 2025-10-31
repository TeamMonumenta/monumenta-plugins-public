package com.playmonumenta.plugins.delves.abilities;

import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.mobabilities.ArcanicBoss;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;

public class Arcanic {

	private static final double ABILITY_CHANCE_PER_LEVEL = 0.06;

	public static final String TRACKING_SPELL_NAME = "Arcanic Missile";
	public static final String CHARGE_SPELL_NAME = "Arcanic Charge";
	public static final String MAGIC_ARROW_SPELL_NAME = "Arcanic Arrow";

	public static final ImmutableSet<String> SPELL_NAMES = ImmutableSet.of(TRACKING_SPELL_NAME, MAGIC_ARROW_SPELL_NAME, CHARGE_SPELL_NAME);

	public static final String DESCRIPTION = "Enemies gain magical abilities.";
	public static final String AVOID_ARCANIC = "boss_arcanicimmune";

	public static Component[] rankDescription(int level) {
		return new Component[]{Component.text("Enemies have a " + Math.round(100 * ABILITY_CHANCE_PER_LEVEL * level) + "% chance to be Arcanic.")};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		Player nearestPlayer = EntityUtils.getNearestPlayer(mob.getLocation(), 64);
		if (FastUtils.RANDOM.nextDouble() < ABILITY_CHANCE_PER_LEVEL * level && !DelvesUtils.isDelveMob(mob) && !mob.getScoreboardTags().contains(AVOID_ARCANIC) && !mob.getScoreboardTags().contains("boss_immortalmount") && !mob.getScoreboardTags().contains("boss_delveimmune")) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			int region = (ServerProperties.getClassSpecializationsEnabled(nearestPlayer) ? (ServerProperties.getAbilityEnhancementsEnabled(nearestPlayer) ? 3 : 2) : 1);
			String ability = FastUtils.getRandomElement(Arrays.asList(ArcanicBoss.ArcanicSpell.values())).name();
			if (mob instanceof Shulker && ability.equals("CHARGE")) {
				return;
			}
			mob.addScoreboardTag(ArcanicBoss.identityTag);
			mob.addScoreboardTag(ArcanicBoss.identityTag + "[spell=" + ability + ",region=" + region + "]");

			World world = mob.getWorld();
			world.playSound(mob.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 0.85f, 2f);
			world.playSound(mob.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.HOSTILE, 0.85f, 2f);
			new PartialParticle(Particle.FIREWORKS_SPARK, mob.getEyeLocation().add(0, 0.9, 0), 10, 0.1, 0.1, 0.1, 0.15).spawnAsEnemy();
		}
	}
}
