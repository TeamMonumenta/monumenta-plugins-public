package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.bosses.spells.SpellChangeFloor;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class Virius extends BossAbilityGroup {
	public static final String identityTag = "boss_virius";
	public static final int detectionRange = 50;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Virius(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Virius(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);

		mBoss.addScoreboardTag("Boss");

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellChangeFloor(plugin, mBoss, mSpawnLoc, detectionRange, 3, Material.MAGMA_BLOCK, 800),
			new SpellBaseLaser(plugin, boss, detectionRange, 100, false, false, 160,
				// Tick action per player
				(LivingEntity player, int ticks, boolean blocked) -> {
					player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (ticks / 80f) * 1.5f);
					boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (ticks / 80f) * 1.5f);
					if (ticks == 0) {
						boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 110, 4));
					}
				},
				// Particles generated by the laser
				(Location loc) -> {
					new PartialParticle(Particle.CLOUD, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.WATER_SPLASH, loc, 1, 0.04, 0.04, 0.04, 1).spawnAsEntityActive(boss);
				},
				// Damage generated at the end of the attack
				(LivingEntity player, Location loc, boolean blocked) -> {
					loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 1.5f);
					new PartialParticle(Particle.WATER_WAKE, loc, 300, 0.8, 0.8, 0.8, 0).spawnAsEntityActive(boss);
					if (!blocked) {
						BossUtils.blockableDamage(boss, player, DamageType.MAGIC, 20);
					}
				})
		));

		BossBarManager bossBar = new BossBarManager(plugin, mBoss, detectionRange, BarColor.RED, BarStyle.SOLID, null);

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, bossBar);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 256;
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		mBoss.setHealth(bossTargetHp);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}
