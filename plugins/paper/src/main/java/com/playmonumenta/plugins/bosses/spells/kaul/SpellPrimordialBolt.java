package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.bosses.PrimordialElementalKaulBoss;
import com.playmonumenta.plugins.bosses.spells.SpellBaseBolt;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SpellPrimordialBolt extends SpellBaseBolt {
	private static final String SLOWNESS_SRC = "PrimordialBoltSlowness";
	private static final String WEAKNESS_SRC = "PrimordialBoltWeakness";
	private static final int DIRECT_HIT_DEBUFF_DURATION = 20 * 15;
	private static final int AOE_HIT_DEBUFF_DURATION = 20 * 10;
	private static final double SLOWNESS_POTENCY = -0.3;
	private static final double WEAKNESS_POTENCY = -0.1;
	private static final List<Material> ARENA_MATS = List.of(
		Material.SMOOTH_SANDSTONE,
		Material.SMOOTH_RED_SANDSTONE,
		Material.NETHERRACK,
		Material.MAGMA_BLOCK
	);

	public SpellPrimordialBolt(Plugin plugin, LivingEntity boss) {
		super(plugin, boss, 20 * 2, 20 * 5, 1.1, PrimordialElementalKaulBoss.detectionRange, 0.5, false, true, 1, 1,
			(Entity entity, int tick) -> {
				if (entity.getLocation().getY() > 60) {
					return;
				}
				float t = tick / 15f;
				World world = boss.getWorld();
				if (tick == 1) {
					GlowingManager.startGlowing(boss, NamedTextColor.RED, 20 * 2, GlowingManager.BOSS_SPELL_PRIORITY);
					world.playSound(boss.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 5f, 0.5f);
					world.playSound(boss.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_AMBIENT, SoundCategory.HOSTILE, 5f, 0.5f);
				}
				new PartialParticle(Particle.LAVA, boss.getLocation(), 1, 0.35, 0, 0.35, 0.005).spawnAsEntityActive(boss);
				new PartialParticle(Particle.BLOCK_CRACK, boss.getLocation(), 3, 0, 0, 0, 0.5,
					Material.STONE.createBlockData()).spawnAsEntityActive(boss);
				world.playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 10, t);
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME);
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
					new BaseMovementSpeedModifyEffect(40, -0.3));
			},

			(Entity entity) -> {
				World world = boss.getWorld();
				world.playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 5, 0.5f);
				new PartialParticle(Particle.FLAME, boss.getLocation().add(0, 1, 0), 80, 0.2, 0.45,
					0.2, 0.2).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SMOKE_LARGE, boss.getLocation().add(0, 1, 0), 30, 0.2,
					0.45, 0.2, 0.1).spawnAsEntityActive(boss);
			},

			(Location loc) -> {
				if (loc.getY() > 60) {
					return;
				}
				new PartialParticle(Particle.BLOCK_DUST, loc, 6, 0.45, 0.45, 0.45, 0.25,
					Material.STONE.createBlockData()).spawnAsEntityActive(boss);
				new PartialParticle(Particle.EXPLOSION_LARGE, loc, 2, 0.2, 0.2, 0.2, 0.25).minimumCount(1).spawnAsEntityActive(boss);
				for (Block block : LocationUtils.getNearbyBlocks(loc.getBlock(), 1)) {
					if (block.getType().isSolid()) {
						Material material = block.getType();
						if (ARENA_MATS.contains(material)) {
							block.setType(Material.AIR);
						}
					}
				}
			},

			(@Nullable Player player, Location loc, boolean blocked, @Nullable Location prevLoc) -> {
				if (player == null || player.getLocation().getY() > 60 || (loc != null && loc.getY() > 60)) {
					return;
				}
				if (!blocked) {
					BossUtils.blockableDamage(boss, player, DamageType.BLAST, 37, "Primordial Bolt", prevLoc);
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
						new PercentSpeed(DIRECT_HIT_DEBUFF_DURATION, SLOWNESS_POTENCY, SLOWNESS_SRC));
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, WEAKNESS_SRC,
						new PercentDamageDealt(DIRECT_HIT_DEBUFF_DURATION, WEAKNESS_POTENCY, DamageType.getScalableDamageType()));
				} else {
					for (Player p : PlayerUtils.playersInRange(loc, 2.5, true)) {
						if (p.getLocation().getY() <= 60) {
							BossUtils.blockableDamage(boss, p, DamageType.BLAST, 16, "Primordial Bolt", prevLoc);
							MovementUtils.knockAway(loc, p, 0.3f, false);
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
								new PercentSpeed(AOE_HIT_DEBUFF_DURATION, SLOWNESS_POTENCY, SLOWNESS_SRC));
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, WEAKNESS_SRC,
								new PercentDamageDealt(AOE_HIT_DEBUFF_DURATION, WEAKNESS_POTENCY, DamageType.getScalableDamageType()));
						}
					}
				}
				World world = boss.getWorld();
				new PartialParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.175).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.9f);
			},

			// Only allow targeting of players below y=60
			(Player player) -> player != null && player.getLocation().getY() < 60
		);
	}
}
