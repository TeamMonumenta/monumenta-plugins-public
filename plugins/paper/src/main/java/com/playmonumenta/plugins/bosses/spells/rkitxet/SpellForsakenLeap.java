package com.playmonumenta.plugins.bosses.spells.rkitxet;

import com.playmonumenta.plugins.bosses.bosses.RKitxet;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import java.util.AbstractMap;
import java.util.Arrays;
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

public class SpellForsakenLeap extends SpellBaseSlam {

	public static final int DETECTION = 32;
	public static final int DELAY = 20 * 5;
	public static final int MIN_RANGE = 0;
	public static final int RUN_DISTANCE = 0;
	public static final double VELOCITY_MULTIPLIER = 0.5;
	public static final double DAMAGE_RADIUS = 3;
	public static final double DAMAGE = 25;
	public static final int JUMP_HEIGHT = 1;

	public Plugin mPlugin;
	private RKitxet mRKitxet;

	public SpellForsakenLeap(Plugin plugin, LivingEntity launcher, int cooldown, RKitxet rKitxet) {
		super(plugin, launcher, JUMP_HEIGHT, DETECTION, MIN_RANGE, RUN_DISTANCE, cooldown, VELOCITY_MULTIPLIER,
				(World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.FLAME, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.FLAME, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.spawnParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(0, 190, 0), 1.0f));
				}, (World world, Player player, Location loc, Vector dir) -> {
					rKitxet.useSpell("Forsaken Leap");

					ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
							Arrays.asList(
									new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
										world.spawnParticle(Particle.CRIMSON_SPORE, location, 1, 0.1, 0.1, 0.1, 0.1);
										world.spawnParticle(Particle.CLOUD, location, 1, 0.1, 0.1, 0.1, 0.1);
									})
							));

					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 1.25F);
					world.spawnParticle(Particle.CRIMSON_SPORE, loc, 60, 0F, 0F, 0F, 0.2F);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0F, 0F, 0F, 0.3F);
					world.spawnParticle(Particle.FLAME, loc, 3 * (int)(DAMAGE_RADIUS * DAMAGE_RADIUS), DAMAGE_RADIUS, 0.25f, DAMAGE_RADIUS, 0);
					if (player != null) {
						BossUtils.blockableDamage(launcher, player, DamageType.BLAST, DAMAGE, "Forsaken Leap", loc);
					}
					});

		mPlugin = plugin;
		mRKitxet = rKitxet;
	}

	@Override
	public boolean canRun() {
		return mRKitxet.canUseSpell("Forsaken Leap");
	}

}
