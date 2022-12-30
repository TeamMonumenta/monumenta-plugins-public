package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.BlackflameBurst;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.BlackflameCharge;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.BlackflameGolemNecromancy;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.BlackflameOrb;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.PassiveVoidRift;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class BeastOfTheBlackFlame extends BossAbilityGroup {

	public static final String identityTag = "boss_beast_blackflame";
	public static final int detectionRange = 75;
	public static final String losName = "BeastOfTheBlackFlame";

	private static final int BASE_HEALTH = 3333;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	//Lower number = faster cast speed
	//0.5 is double casting speed
	public double mCastSpeed = 1;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new BeastOfTheBlackFlame(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public BeastOfTheBlackFlame(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);

		mSpawnLoc = spawnLoc.add(0, 3, 0);
		mEndLoc = endLoc;

		SpellManager normalSpells = new SpellManager(Arrays.asList(
			new BlackflameCharge(plugin, boss, this),
			new BlackflameBurst(boss, plugin, this),
			new BlackflameOrb(boss, plugin, this),
			new BlackflameGolemNecromancy(mPlugin, mBoss, 10, detectionRange, 90, 20 * 6, mSpawnLoc.getY(), mSpawnLoc, this)
		));


		List<Spell> passiveNormalSpells = Arrays.asList(
			new SpellBlockBreak(boss, 2, 3, 2),
			new SpellShieldStun(6 * 20)
		);

		//Under 50%, adds passive
		List<Spell> lowHealthPassives = Arrays.asList(
			new SpellBlockBreak(boss, 2, 3, 2),
			new PassiveVoidRift(boss, plugin, 20 * 9),
			new SpellShieldStun(6 * 20)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();

		events.put(90, mBoss -> {
			forceCastSpell(BlackflameGolemNecromancy.class);
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"RISE, REMNANTS OF THE BLACKFLAME.\",\"color\":\"dark_red\"}]");
		});

		events.put(50, mBoss -> {
			changePhase(normalSpells, lowHealthPassives, null);
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE STRENGTH OF THE VOID WILL DRAG YOU DOWN!\",\"color\":\"dark_red\"}]");
		});

		events.put(40, mBoss -> {
			forceCastSpell(BlackflameGolemNecromancy.class);
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"RISE ONCE MORE, REMNANTS.\",\"color\":\"dark_red\"}]");
		});


		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				//If below 3 blocks, teleport
				if (mSpawnLoc.getY() - mBoss.getLocation().getY() >= 3) {
					teleport(mSpawnLoc);
				}

				//If player too far from arena center or below 4 blocks or too high and is on a block, damage them
				for (Player p : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
					if ((mSpawnLoc.distance(p.getLocation()) > 22
						     || mSpawnLoc.getY() - p.getLocation().getY() >= 3
						     || (mSpawnLoc.getY() - p.getLocation().getY() <= -2 && p.isOnGround()))
						    && p.getGameMode() != GameMode.CREATIVE) {
						Vector vel = p.getVelocity();
						BossUtils.bossDamagePercent(mBoss, p, 0.1);
						p.setVelocity(vel);

						p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1, 0f);
						new PartialParticle(Particle.FLAME, p.getLocation(), 10, 0.5, 0.25, 0.5, 0.2).spawnAsEntityActive(boss);
					}
				}
			}
		}.runTaskTimer(mPlugin, 20 * 7, 10);

		World world = mBoss.getWorld();

		mBoss.setGravity(false);
		mBoss.setInvulnerable(true);

		world.playSound(mSpawnLoc, Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 6, 0);

		new BukkitRunnable() {
			@Override
			public void run() {
				PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"SPEAKERS OF REMORSE, I ANSWER THE CALL.\",\"color\":\"dark_red\"}]");
			}
		}.runTaskLater(mPlugin, 20);

		new BukkitRunnable() {
			int mCount = 1;
			int mTicks = 0;
			double mYInc = 3 / 40.0;

			@Override
			public void run() {
				Creature c = (Creature) mBoss;
				c.setTarget(null);

				if (mTicks >= 20 * 6) {
					this.cancel();

					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);

					BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.PURPLE, BarStyle.SEGMENTED_10, events);
					constructBoss(normalSpells, passiveNormalSpells, detectionRange, bossBar, 20 * 10);

					for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
						MessagingUtils.sendBoldTitle(player, ChatColor.DARK_RED + "???", ChatColor.RED + "Beast of the Blackflame");
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 0.75f);
					}
				} else if (mTicks >= 20 * 2 && mBoss.getLocation().getY() < mSpawnLoc.getY()) {
					mBoss.teleport(mBoss.getLocation().add(0, mYInc, 0));
				}

				new PartialParticle(Particle.SMOKE_LARGE, mSpawnLoc, mCount, 0, 0, 0, 0.1).spawnAsEntityActive(boss);
				if (mTicks % 2 == 0) {
					if (mCount < 40) {
						mCount++;
					}

					if (mTicks <= 20 * 4) {
						world.playSound(mSpawnLoc, Sound.BLOCK_ANCIENT_DEBRIS_PLACE, SoundCategory.HOSTILE, 3f, 0);
					}
				}

				if (mTicks == 20 * 2) {
					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"FROM BEYOND I SERVE MY ETERNAL PURPOSE.\",\"color\":\"dark_red\"}]");
				} else if (mTicks == 20 * 4) {
					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE SEAL SHALL REMAIN.\",\"color\":\"dark_red\"}]");
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 20 * 3, 1);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mCastSpeed > .5) {
					mCastSpeed -= .1;
				} else {
					this.cancel();
					return;
				}

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"THE VOID DRAWS CLOSER BY THE MINUTE. SO TOO, DOES MY POWER.\",\"color\":\"dark_red\"}]");

				World world = mBoss.getWorld();
				Location loc = mBoss.getLocation();
				world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 5f, 0.6f);
				world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 5f, 1.5f);
				new PartialParticle(Particle.SPELL_WITCH, loc.add(0, mBoss.getHeight() / 2, 0), 30, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(boss);
				new PartialParticle(Particle.VILLAGER_ANGRY, loc.add(0, mBoss.getHeight() / 2, 0), 10, 0.35, 0.5, 0.35, 0).spawnAsEntityActive(boss);
			}
		}.runTaskTimer(mPlugin, 20 * 60, 20 * 60);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {

		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		mBoss.setHealth(100);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.setPersistent(true);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 1000, 10));
		teleport(mSpawnLoc);
		World world = mBoss.getWorld();

		if (event != null) {
			event.setCancelled(true);
			event.setReviveHealth(100);
		}

		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"FORGIVE ME SILVER ONES... I HAVE FAILED YOU...\",\"color\":\"dark_red\"}]");

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= 20 * 5) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 10, 0);

					this.cancel();
					mBoss.remove();

					new BukkitRunnable() {
						@Override
						public void run() {
							for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
								MessagingUtils.sendBoldTitle(player, ChatColor.GRAY + "VICTORY", ChatColor.DARK_GRAY + "Ghalkor, Svalgot, and The Beast");
								player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 100, 0.8f);
							}
						}
					}.runTaskLater(mPlugin, 20 * 3);
				}

				if (mTicks % 10 == 0) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);
				}

				new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 1, 1, 1, 1).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.CLOUD, mSpawnLoc, 80, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public void init() {
		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).size();
		double bossTargetHp = BASE_HEALTH * BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);

		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setHealth(bossTargetHp);

		mBoss.setPersistent(true);

	}

	//Teleport with special effects
	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
	}
}
