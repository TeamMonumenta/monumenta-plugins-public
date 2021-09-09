package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.BlackflameBurst;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.BlackflameCharge;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.BlackflameGolemNecromancy;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.BlackflameOrb;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.PassiveVoidRift;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public class BeastOfTheBlackFlame extends BossAbilityGroup {

	public static final String identityTag = "boss_beast_blackflame";
	public static final int detectionRange = 75;
	public static final String losName = "BeastOfTheBlackFlame";

	private static final int BASE_HEALTH = 2524;

	private final Plugin mPlugin;
	public final LivingEntity mBoss;

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

		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc.add(0, 3, 0);
		mEndLoc = endLoc;

		SpellManager normalSpells = new SpellManager(Arrays.asList(
				new BlackflameCharge(plugin, boss, this),
				new BlackflameBurst(boss, plugin, this),
				new BlackflameOrb(boss, plugin, this),
				new BlackflameGolemNecromancy(mPlugin, mBoss, 10, detectionRange, 90, 20 * 6, mSpawnLoc.getY(), mSpawnLoc, this)
				));



		List<Spell> passiveNormalSpells = Arrays.asList(
				new SpellPurgeNegatives(boss, 20 * 3),
				new SpellBlockBreak(boss, 2, 3, 2)
			);

		//Under 50%, adds passive
		List<Spell> lowHealthPassives = Arrays.asList(
				new SpellPurgeNegatives(boss, 20 * 3),
				new SpellBlockBreak(boss, 2, 3, 2),
				new PassiveVoidRift(boss, plugin, 20 * 9)
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

				//If player too far from arena center or below 4 blocks or too high and either moving very slowly or is on a block, damage them
				for (Player p : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
					if ((mSpawnLoc.distance(p.getLocation()) > 22
							|| mSpawnLoc.getY() - p.getLocation().getY() >= 3
							|| (mSpawnLoc.getY() - p.getLocation().getY() <= -3 && (p.getVelocity().getY() <= 0.1 || p.isOnGround())))
							&& p.getGameMode() != GameMode.CREATIVE) {
						Vector vel = p.getVelocity();
						BossUtils.bossDamagePercent(mBoss, p, 0.1);
						p.setVelocity(vel);

						p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1, 0f);
						p.spawnParticle(Particle.FLAME, p.getLocation(), 10, 0.5, 0.25, 0.5, 0.2);
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

					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "title @s title [\"\",{\"text\":\"???\",\"color\":\"dark_red\",\"bold\":true}]");
					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "title @s subtitle [\"\",{\"text\":\"Beast of the Blackflame\",\"color\":\"red\",\"bold\":true}]");
					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.75");
				} else if (mTicks >= 20 * 2 && mBoss.getLocation().getY() < mSpawnLoc.getY()) {
					mBoss.teleport(mBoss.getLocation().add(0, mYInc, 0));
				}

				world.spawnParticle(Particle.SMOKE_LARGE, mSpawnLoc, mCount, 0, 0, 0, 0.1);
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
				world.spawnParticle(Particle.SPELL_WITCH, loc.add(0, mBoss.getHeight() / 2, 0), 30, 0.25, 0.45, 0.25, 1);
				world.spawnParticle(Particle.VILLAGER_ANGRY, loc.add(0, mBoss.getHeight() / 2, 0), 10, 0.35, 0.5, 0.35, 0);
			}
		}.runTaskTimer(mPlugin, 20 * 60, 20 * 60);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player && event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			Player player = (Player) event.getEntity();
			if (player.isBlocking()) {
				player.setCooldown(Material.SHIELD, 20 * 6);
			}
		}
	}


	@Override
	public void death(EntityDeathEvent event) {

		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		changePhase(null, null, null);
		mBoss.setHealth(100);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.setPersistent(true);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 1000, 10));
		teleport(mSpawnLoc);
		World world = mBoss.getWorld();

		event.setCancelled(true);
		event.setReviveHealth(100);

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
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 100 0.8");
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"VICTORY\",\"color\":\"gray\",\"bold\":true}]");
							PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Ghalkor, Svalgot, and The Beast\",\"color\":\"dark_gray\",\"bold\":true}]");
						}
					}.runTaskLater(mPlugin, 20 * 3);
				}

				if (mTicks % 10 == 0) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);
				}

				world.spawnParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 1, 1, 1, 1);
				world.spawnParticle(Particle.CLOUD, mSpawnLoc, 80, 0, 0, 0, 0.1);

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).size();

		/*
		 * New boss mechanic: The more players there are,
		 * the less invulnerability frames/no damage ticks it has.
		 * Note: A normal mob's maximum NoDamageTicks is 20, with 10 being when it can be damaged.
		 * It's really weird, but regardless, remember that its base will always be 20.
		 */
		int noDamageTicksTake = playerCount / 3;
		if (noDamageTicksTake > 5) {
			noDamageTicksTake = 5;
		}
		mBoss.setMaximumNoDamageTicks(mBoss.getMaximumNoDamageTicks() - noDamageTicksTake);
		bossTargetHp = (int) (BASE_HEALTH * (1 + (1 - 1/Math.E) * Math.log(playerCount)));
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
		world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		world.spawnParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1);
	}
}
