package com.playmonumenta.plugins.bosses.bosses;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class JumpBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_jump";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DELAY = 100;
		@BossParam(help = "not written")
		public int MIN_RANGE = 0;
		@BossParam(help = "not written")
		public int DETECTION = 32;
		@BossParam(help = "not written")
		public int JUMP_HEIGHT = 1;
		@BossParam(help = "not written")
		public int RUN_DISTANCE = 0;
		@BossParam(help = "not written")
		public int COOLDOWN = 20 * 8;
		@BossParam(help = "not written")
		public double VELOCITY_MULTIPLIER = 0.45;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new JumpBoss(plugin, boss);
	}

	public JumpBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager manager = new SpellManager(Arrays.asList(new SpellBaseSlam(plugin, boss, p.JUMP_HEIGHT, p.DETECTION, p.MIN_RANGE, p.RUN_DISTANCE, p.COOLDOWN, p.VELOCITY_MULTIPLIER,
				(World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_PILLAGER_CELEBRATE, SoundCategory.PLAYERS, 1f, 1.1f);
					world.spawnParticle(Particle.CLOUD, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.CLOUD, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.spawnParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f));
				}, (World world, Player player, Location loc, Vector dir) -> {
					ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
							Arrays.asList(
									new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
										world.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.1);
									})
							));

					world.playSound(loc, Sound.ENTITY_HORSE_GALLOP, SoundCategory.PLAYERS, 1.3F, 0);
					world.playSound(loc, Sound.ENTITY_HORSE_GALLOP, SoundCategory.PLAYERS, 2, 1.25F);
					world.spawnParticle(Particle.CLOUD, loc, 60, 0F, 0F, 0F, 0.2F);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0F, 0F, 0F, 0.3F);
					})));
		super.constructBoss(manager, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
