package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAura;
import com.playmonumenta.plugins.bosses.spells.SpellBaseBolt;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.bosses.spells.SpellDelayedAction;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public final class TCalin extends BossAbilityGroup {
	public static final String identityTag = "boss_tcalin";
	public static final int detectionRange = 60;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> new TCalin(plugin, boss, spawnLoc, endLoc));
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public TCalin(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);
		World world = mSpawnLoc.getWorld();
		mBoss.addScoreboardTag("Boss");
		SpellBaseCharge charge = new SpellBaseCharge(plugin, mBoss, 20, 25, 160, true,
			(LivingEntity player) -> {
				new PartialParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0).spawnAsEntityActive(boss);
				boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1f, 1.5f);
			},
			// Warning particles
			(Location loc) -> new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 1, 1, 1, 0).spawnAsEntityActive(boss),
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				new PartialParticle(Particle.SMOKE_LARGE, boss.getLocation(), 100, 2, 2, 2, 0).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1f, 0.5f);
			},
			// Attack hit a player
			(LivingEntity player) -> {
				new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation(), 80, 1, 1, 1, 0).spawnAsEntityActive(boss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 20, 1, 1, 1, 0.15).spawnAsEntityActive(boss);
				boss.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1f, 0.85f);
				BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, 14);
				MovementUtils.knockAway(mBoss.getLocation(), player, 0.25f, 0.4f);
			},
			// Attack particles
			(Location loc) -> new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss),
			// Ending particles on boss
			() -> new PartialParticle(Particle.SMOKE_LARGE, boss.getLocation(), 150, 2, 2, 2, 0).spawnAsEntityActive(boss)
		);

		SpellBaseBolt bolt = new SpellBaseBolt(plugin, mBoss, (int) (20 * 2.25), 20 * 5, 1.15, detectionRange, 0.5, true, true, 2, 10,
			(Entity entity, int tick) -> {
				float t = tick / 15.0f;
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 1, 0.35, 0, 0.35, 0.05).spawnAsEntityActive(boss);
				new PartialParticle(Particle.BLOCK_CRACK, mBoss.getLocation().add(0, 1, 0), 5, 0.25, 0.45, 0.25,
					0.5, Material.OAK_LEAVES.createBlockData()).spawnAsEntityActive(boss);
				world.playSound(mBoss.getLocation(), Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10, t);
				mBoss.removePotionEffect(PotionEffectType.SLOW);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 2, 1));
			},

			(Entity entity) -> {
				world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 5, 1f);
				new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 80, 0.2, 0.45, 0.2, 0.2).spawnAsEntityActive(boss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 30, 0.2, 0.45, 0.2,
					0.1).spawnAsEntityActive(boss);
			},

			(Location loc) -> {
				new PartialParticle(Particle.BLOCK_DUST, loc, 10, 0.35, 0.35, 0.35, 0.25,
					Material.OAK_LEAVES.createBlockData()).spawnAsEntityActive(boss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.2, 0.2, 0.2, 0.125).spawnAsEntityActive(boss);
			},

			(@Nullable Player player, Location loc, boolean blocked, @Nullable Location prevLoc) -> {
				if (!blocked && player != null) {
					BossUtils.blockableDamage(mBoss, player, DamageType.MAGIC, 12, prevLoc);
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 7, 1));
				}
				new PartialParticle(Particle.BLOCK_CRACK, loc, 125, 0.35, 0.35, 0.35, 1,
					Material.OAK_LEAVES.createBlockData()).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1.25f);
			},
			null
		);

		SpellDelayedAction aoe = new SpellDelayedAction(plugin, mBoss.getLocation(), 20 * 3,
			// Start
			(Location loc) -> {
				mBoss.removePotionEffect(PotionEffectType.SLOW);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 1));
				new BukkitRunnable() {
					float mJ = 0;
					double mRotation = 0;
					double mRadius = 5;

					@Override
					public void run() {
						Location loc = mBoss.getLocation();
						mJ++;
						new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 2, 0.25, 0.45, 0.25).spawnAsEntityActive(boss);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3, 0.5f + (mJ / 20));
						for (int i = 0; i < 5; i++) {
							double radian1 = Math.toRadians(mRotation + (72 * i));
							loc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
							new PartialParticle(Particle.SPELL_WITCH, loc, 6, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(boss);
							new PartialParticle(Particle.SPELL_MOB, loc, 4, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(boss);
							loc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
						}
						mRotation += 10;
						mRadius -= 0.1;
						if (mRadius <= 0 || mBoss.isDead()) {
							this.cancel();
						}
					}

				}.runTaskTimer(plugin, 0, 1);
			},
			// Warning
			(Location loc) -> {

			},
			// End
			(Location loc) -> {
				new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 200, 0, 0, 0, 0.175).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation().add(0, 1, 0), 75, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
				knockback(plugin, 5);
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 5, true)) {
					BossUtils.blockableDamage(mBoss, player, DamageType.MAGIC, 16);
				}
			}
		);

		SpellManager activeSpells = new SpellManager(Arrays.asList(bolt, charge));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(bolt, charge, aoe));

		List<Spell> passiveSpells = List.of(
			new SpellBaseAura(mBoss, 8, 5, 8, 10, Particle.FALLING_DUST,
				Material.ANVIL.createBlockData(), (Player player) ->
				player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, true, true))
			)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss ->
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[T'Calin] \",\"color\":\"gold\"},{\"text\":\"Thank you for foolishly opening the way into this town, hero.\",\"color\":\"white\"}]"));

		events.put(50, mBoss -> {
			world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 1f);
			new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(boss);
			new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(boss);
			new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(boss);
			mBoss.setAI(false);
			mBoss.setInvulnerable(true);
			mBoss.teleport(mSpawnLoc);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1, 0f);
			new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(boss);
			new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(boss);
			new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(boss);
			super.changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
			new BukkitRunnable() {
				float mJ = 0;
				double mRotation = 0;
				double mRadius = 10;
				Location mLoc = mBoss.getLocation();

				@Override
				public void run() {
					mJ++;
					new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 2, 0.25, 0.45, 0.25, 0).spawnAsEntityActive(boss);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3, 0.5f + (mJ / 20));
					for (int i = 0; i < 5; i++) {
						double radian1 = Math.toRadians(mRotation + (72 * i));
						mLoc.add(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
						new PartialParticle(Particle.SPELL_WITCH, mLoc, 6, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(boss);
						new PartialParticle(Particle.SPELL_MOB, mLoc, 4, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(boss);
						mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
					}
					mRotation += 10;
					mRadius -= 0.25;
					if (mRadius <= 0) {
						this.cancel();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1, 1.5f);
						new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45,
							0.25, 0.15).spawnAsEntityActive(boss);
						new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1,
							0.10).spawnAsEntityActive(boss);
						mBoss.setAI(true);
						mBoss.setInvulnerable(false);
						changePhase(SpellManager.EMPTY, passiveSpells, null);
						new BukkitRunnable() {

							@Override
							public void run() {
								changePhase(phase2Spells, passiveSpells, null);
							}

						}.runTaskLater(plugin, 20 * 10);
					}
				}

			}.runTaskTimer(plugin, 30, 1);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[T'Calin] \",\"color\":\"gold\"},{\"text\":\"Know the true power of the Sons of the Forest!\",\"color\":\"white\"}]");
		});

		events.put(50, mBoss -> PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[T'Calin] \",\"color\":\"gold\"},{\"text\":\"This cannot be!\",\"color\":\"white\"}]"));

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.GREEN, BarStyle.SEGMENTED_10, events);

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, bossBar);
	}

	private void knockback(Plugin plugin, double r) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 0.5f);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), r, true)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.4f, false);
		}
		new BukkitRunnable() {
			double mRotation = 0;
			final Location mLoc = mBoss.getLocation();
			double mRadius = 0;
			double mY = 2.5;
			double mYMinus = 0.35;

			@Override
			public void run() {

				mRadius += 1;
				for (int i = 0; i < 15; i += 1) {
					mRotation += 24;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.SPELL_WITCH, mLoc, 2, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_LARGE, mLoc, 2, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);

				}
				mY -= mY * mYMinus;
				mYMinus += 0.02;
				if (mYMinus >= 1) {
					mYMinus = 1;
				}
				if (mRadius >= r) {
					this.cancel();
				}

			}

		}.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 100.0f, 0.8f);
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 512;
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		mBoss.setHealth(bossTargetHp);

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, ChatColor.GREEN + "T'Calin", ChatColor.DARK_GREEN + "Forest Battlemage");
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 1.25f);
		}
	}
}
