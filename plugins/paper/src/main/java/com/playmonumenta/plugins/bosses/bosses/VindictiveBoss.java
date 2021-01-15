package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellVindictiveParticle;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.EntityUtils;

public class VindictiveBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_vindictive";
	public static final int detectionRange = 40;

	public static final String PERCENT_SPEED_EFFECT_NAME = "VindictivePercentSpeedEffect";
	public static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "VindictivePercentDamageDealtEffect";
	public static final int DURATION = 20 * 8;
	public static final double PERCENT_SPEED_EFFECT = 0.3;
	public static final double PERCENT_DAMAGE_DEALT_EFFECT = 0.8;
	public static final int HEAL = 100;

	public static final int RANGE = 12;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new VindictiveBoss(plugin, boss);
	}

	public VindictiveBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		List<Spell> passiveSpells = Arrays.asList(
			new SpellVindictiveParticle(mBoss)
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		com.playmonumenta.plugins.Plugin plugin = com.playmonumenta.plugins.Plugin.getInstance();
		World world = mBoss.getWorld();

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mBoss.getLocation(), RANGE)) {
			Location loc = mob.getEyeLocation();
			world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.5f);
			world.spawnParticle(Particle.FLAME, loc, 20, 0, 0, 0, 0.1);

			plugin.mEffectManager.addEffect(mob, PERCENT_SPEED_EFFECT_NAME,
					new PercentSpeed(DURATION, PERCENT_SPEED_EFFECT, PERCENT_SPEED_EFFECT_NAME));
			plugin.mEffectManager.addEffect(mob, PERCENT_DAMAGE_DEALT_EFFECT_NAME,
					new PercentDamageDealt(DURATION, PERCENT_DAMAGE_DEALT_EFFECT));

			mob.setHealth(Math.min(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), mob.getHealth() + HEAL));
		}
	}
}
