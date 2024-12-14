package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.bosses.spells.SpellChangeFloor;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.Collections;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class Virius extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_virius";
	public static final int detectionRange = 30;

	private static final int BASE_HEALTH = 300;
	private static final double SCALING_X = 0.4;
	private static final double SCALING_Y = 0.3;

	private final double mDefenseScaling;

	public Virius(final Plugin plugin, final LivingEntity boss, final Location spawnLoc, final Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		final int mPlayerCount = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).size();
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);

		final SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellChangeFloor(mPlugin, mBoss, mSpawnLoc, detectionRange, 3, Material.MAGMA_BLOCK,
				Constants.TICKS_PER_SECOND * 20),
			new SpellBaseSeekingProjectile(mPlugin, mBoss, false, Constants.TICKS_PER_SECOND * 6,
				(int) (Constants.TICKS_PER_SECOND * 1.5), 0.6, 0, Constants.TICKS_PER_SECOND * 3,
				0.5, true, true, 0, true,
				/* Targets */
				() -> PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, false),
				/* Init aesthetics */
				(final World world, final Location loc, final int ticks) -> {
					world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 1, 1);
					GlowingManager.startGlowing(mBoss, NamedTextColor.RED, (int) (Constants.TICKS_PER_SECOND * 1.5), GlowingManager.BOSS_SPELL_PRIORITY);
				},
				/* Launch aesthetic */
				(final World world, final Location loc, final int ticks) ->
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 1, 1.5f),
				/* Projectile aesthetics */
				(final World world, final Location loc, final int ticks) -> {
					new PartialParticle(Particle.SPELL_INSTANT, loc, 3, 0.05, 0.05, 0.05, 0.03).spawnAsEntityActive(mBoss);
					if (ticks % (Constants.TICKS_PER_SECOND * 2) == 0) {
						world.playSound(loc, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 0.5f, 0.5f);
					}
				},
				/* Hit action */
				(final World world, final @Nullable LivingEntity target, final Location loc, final @Nullable Location prevLoc) -> {
					if (target != null) {
						DamageUtils.damage(mBoss, target, new DamageEvent.Metadata(DamageType.MAGIC, null, null,
							"Hasty Bolt"), 7, false, true, true);
					}
					new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 20, 0, 0, 0, 0.2).spawnAsEntityActive(mBoss);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.5f);
				}),
			new SpellBaseLaser(mPlugin, mBoss, detectionRange, Constants.TICKS_PER_SECOND * 3, false,
				false, Constants.TICKS_PER_SECOND * 8,
				/* Tick action per player */
				(final LivingEntity player, final int ticks, final boolean blocked) -> {
					player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 80f) * 1.5f);
					mBoss.getLocation().getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (ticks / 80f) * 1.5f);
					if (ticks == 0) {
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
							new BaseMovementSpeedModifyEffect(110, -0.75));
					}
				},
				/* Laser particles */
				(final Location loc) -> {
					new PartialParticle(Particle.CLOUD, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.WATER_SPLASH, loc, 1, 0.04, 0.04, 0.04, 1).spawnAsEntityActive(mBoss);
				},
				/* Hit action */
				(final LivingEntity player, final Location loc, final boolean blocked) -> {
					loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 1f, 1.5f);
					new PartialParticle(Particle.WATER_WAKE, loc, 20, 0.4, 0.4, 0.4, 0.02).spawnAsEntityActive(mBoss);
					if (!blocked) {
						DamageUtils.damage(mBoss, player, new DamageEvent.Metadata(DamageType.MAGIC, null, null,
							"Memory Laser"), 16, false, true, true);
					}
				})
		));

		final BossBarManager bossBar = new BossBarManager(mBoss, detectionRange, BossBar.Color.RED, BossBar.Overlay.PROGRESS, null);
		constructBoss(activeSpells, Collections.emptyList(), detectionRange, bossBar);
	}

	@Override
	public void init() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0.25);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0.3);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, BASE_HEALTH);

		mBoss.addScoreboardTag("Boss");
		mBoss.setRemoveWhenFarAway(false);
		mBoss.setInvulnerable(false);
		mBoss.setAI(true);
		mBoss.setHealth(BASE_HEALTH);
	}

	@Override
	public void death(final @Nullable EntityDeathEvent event) {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void onHurt(final DamageEvent event) {
		event.setDamage(event.getFlatDamage() / mDefenseScaling);
	}

	@Override
	public void onDamage(final DamageEvent event, final LivingEntity damagee) {
		if (event.getType() == DamageType.MELEE) {
			MovementUtils.knockAway(mBoss.getLocation(), damagee, 1.0f, 0.5f, true);
			EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), Constants.TICKS_PER_SECOND * 4,
				damagee, mBoss);
		}
	}
}
