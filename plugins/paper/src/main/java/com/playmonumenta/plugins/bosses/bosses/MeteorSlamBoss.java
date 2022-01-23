package com.playmonumenta.plugins.bosses.bosses;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;

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

public class MeteorSlamBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_meteor_slam";


	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DETECTION = 32;
		@BossParam(help = "not written")
		public int DELAY = 20 * 5;
		@BossParam(help = "not written")
		public int COOLDOWN = 20 * 8;
		@BossParam(help = "not written")
		public int MIN_RANGE = 0;
		@BossParam(help = "not written")
		public int RUN_DISTANCE = 0;
		@BossParam(help = "not written")
		public double VELOCITY_MULTIPLIER = 0.5;
		@BossParam(help = "not written")
		public double DAMAGE_RADIUS = 3;
		@BossParam(help = "Damage dealt (blast)")
		public double DAMAGE = 20;
		@BossParam(help = "Percent health damage dealt")
		public double DAMAGE_PERCENT = 0;
		@BossParam(help = "not written")
		public int JUMP_HEIGHT = 1;
		//notes: this ability will probably become deprecated in the future!
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new MeteorSlamBoss(plugin, boss);
	}

	public MeteorSlamBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		SpellManager manager = new SpellManager(Arrays.asList(new SpellBaseSlam(plugin, boss, p.JUMP_HEIGHT, p.DETECTION, p.MIN_RANGE, p.RUN_DISTANCE, p.COOLDOWN, p.VELOCITY_MULTIPLIER,
				(World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.spawnParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f));
				}, (World world, Player player, Location loc, Vector dir) -> {
					ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
							Arrays.asList(
									new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
										world.spawnParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.1);
										world.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.1);
									})
							));

					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 1.25F);
					world.spawnParticle(Particle.FLAME, loc, 60, 0F, 0F, 0F, 0.2F);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0F, 0F, 0F, 0.3F);
					world.spawnParticle(Particle.LAVA, loc, 3 * (int) (p.DAMAGE_RADIUS * p.DAMAGE_RADIUS), p.DAMAGE_RADIUS, 0.25f, p.DAMAGE_RADIUS, 0);
					if (player != null) {
						BossUtils.blockableDamage(boss, player, DamageType.BLAST, p.DAMAGE);
						BossUtils.bossDamagePercent(boss, player, p.DAMAGE_PERCENT);
						return;
					}
					for (Player players : PlayerUtils.playersInRange(loc, p.DAMAGE_RADIUS, true)) {
						BossUtils.blockableDamage(boss, players, DamageType.BLAST, p.DAMAGE);
						BossUtils.bossDamagePercent(boss, players, p.DAMAGE_PERCENT);
					}
					})));
		super.constructBoss(manager, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
