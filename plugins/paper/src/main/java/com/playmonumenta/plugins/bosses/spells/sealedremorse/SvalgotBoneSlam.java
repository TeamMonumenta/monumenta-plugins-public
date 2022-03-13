package com.playmonumenta.plugins.bosses.spells.sealedremorse;

import com.playmonumenta.plugins.bosses.bosses.Ghalkor;
import com.playmonumenta.plugins.bosses.bosses.Svalgot;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.AbstractMap;
import java.util.Arrays;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class SvalgotBoneSlam extends SpellBaseSlam {

	public static final int COOLDOWN = 20 * 8;
	private static final int MIN_RANGE = 0;
	private static final int RUN_DISTANCE = 0;
	private static final double VELOCITY_MULTIPLIER = 0.375; //25% slower than 0.5
	private static final double DAMAGE_RADIUS = 3;
	private static final double DAMAGE = 36;
	private static final int JUMP_HEIGHT = 1;

	public static final String ATTACK_MODIFIER_NAME = "AttackRemoveDamageModifier";

	private Svalgot mBossClass;

	public SvalgotBoneSlam(Plugin plugin, LivingEntity boss, Svalgot bossClass) {
		super(plugin, boss, JUMP_HEIGHT, Ghalkor.detectionRange, MIN_RANGE, RUN_DISTANCE, COOLDOWN, VELOCITY_MULTIPLIER,
		(World world, Location loc) -> {
			world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 0);
			world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 15, 1, 0f, 1, 0);

			EntityUtils.addAttribute(boss, Attribute.GENERIC_ATTACK_DAMAGE,
					new AttributeModifier(ATTACK_MODIFIER_NAME, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}, (World world, Location loc) -> {
			world.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
			world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 15, 1, 0f, 1, 0);
		}, (World world, Location loc) -> {
			world.spawnParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f));
		}, (World world, Player player, Location loc, Vector dir) -> {
			ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
					Arrays.asList(
							new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
								world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.1, 0.1, 0.1, 0.1);
								world.spawnParticle(Particle.SMOKE_LARGE, loc, 1, 0.1, 0.1, 0.1, 0.1);
							})
					));

			EntityUtils.removeAttribute(boss, Attribute.GENERIC_ATTACK_DAMAGE, ATTACK_MODIFIER_NAME);

			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 1.25F);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 2, 0);
			world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 60, 0F, 0F, 0F, 0.2F);
			world.spawnParticle(Particle.SMOKE_LARGE, loc, 20, 0F, 0F, 0F, 0.3F);

			world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 3 * (int)(DAMAGE_RADIUS * DAMAGE_RADIUS), DAMAGE_RADIUS, 0.25f, DAMAGE_RADIUS, 0);
			if (player != null) {
				BossUtils.blockableDamage(boss, player, DamageType.MAGIC, DAMAGE, "Bone Slam", boss.getLocation());
				return;
			}
			for (Player players : PlayerUtils.playersInRange(loc, DAMAGE_RADIUS, true)) {
				BossUtils.blockableDamage(boss, players, DamageType.MAGIC, DAMAGE, "Bone Slam", boss.getLocation());
			}
			});

		mBossClass = bossClass;
	}

	@Override
	public int cooldownTicks() {
		return (int) (7 * 20 * mBossClass.mCastSpeed);
	}
}
