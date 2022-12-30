package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
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
import org.jetbrains.annotations.Nullable;

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
		public double DAMAGE = 30;
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
				mBoss.addScoreboardTag(BlockPlacerBoss.STOP_PLACING_BLOCK);
				world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
				new PartialParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0).spawnAsEntityActive(boss);
			}, (World world, Location loc) -> {
			world.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
			new PartialParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0).spawnAsEntityActive(boss);
		}, (World world, Location loc) -> {
			new PartialParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f)).spawnAsEntityActive(boss);
		}, (World world, @Nullable Player player, Location loc, Vector dir) -> {
			mBoss.removeScoreboardTag(BlockPlacerBoss.STOP_PLACING_BLOCK);
			ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
				Arrays.asList(
					new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
						new PartialParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(boss);
						new PartialParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(boss);
					})
				));

			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 0);
			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 1.25F);
			new PartialParticle(Particle.FLAME, loc, 60, 0F, 0F, 0F, 0.2F).spawnAsEntityActive(boss);
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0F, 0F, 0F, 0.3F).spawnAsEntityActive(boss);
			new PartialParticle(Particle.LAVA, loc, 3 * (int) (p.DAMAGE_RADIUS * p.DAMAGE_RADIUS), p.DAMAGE_RADIUS, 0.25f, p.DAMAGE_RADIUS, 0).spawnAsEntityActive(boss);
			if (player != null) {
				BossUtils.blockableDamage(boss, player, DamageType.BLAST, p.DAMAGE);
				BossUtils.bossDamagePercent(boss, player, p.DAMAGE_PERCENT, boss.getLocation());
				return;
			}
			for (Player players : PlayerUtils.playersInRange(loc, p.DAMAGE_RADIUS, true)) {
				BossUtils.blockableDamage(boss, players, DamageType.BLAST, p.DAMAGE);
				BossUtils.bossDamagePercent(boss, players, p.DAMAGE_PERCENT, boss.getLocation());
			}
		})));
		super.constructBoss(manager, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
