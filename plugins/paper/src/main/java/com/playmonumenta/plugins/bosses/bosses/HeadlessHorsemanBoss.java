package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellRemoveLevitation;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellBeeBombs;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellBurningVengence;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellHallowsEnd;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellHellzoneGrenade;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellPhantomOfTheOpera;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellReaperOfLife;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

/*
 * Barrier of Flames - (Hard mode only) When the boss enters phase 2 he gains a shield of
 * flames around him that prevents him from taking damage. This barrier can be broken if it
 * is either blown up with TnT or blown up with the Horseman’s Explosive attack. (A Warning
 * is given to players the first time they try to deal damage while the boss has the barrier up)

Phase 1
Hellzone Grenade - The horseman fires a pumpkin (Fireball with pumpkin block maybe) that
explodes on contact with the ground or a player, dealing NM20/HM35 damage in a 3 block radius.
Enemies hit are ignited for 3 seconds. The area affected leaves behind a lingering AoE if it hits
the ground, this lingering fire is also 3 blocks in radius and deals 5% max health damage every 0.5
seconds to players and ignites them for 3 seconds. (Does not leave the lingering thing behind if
it hits a player or the horseman)

Bat Bomb - The Horseman summons 20* bats around himself to distract his enemies. After 3 seconds
the bats explode dealing 18/25 damage to anyone with 4 blocks of them, killing the bat in the process.
The bats have 20hp.

Sinister Reach - The horseman pauses momentarily for 0.8 seconds, afterwards the swing forward
targeting the player who has his aggro using the shadows to extend his reach. Each player in a
60 degree cone in front of them 8 blocks in length takes 20/32 damage, given slowness 3 and rooted
 for 5 seconds.

Burning Vengeance - The horseman after 0.5 seconds summons a ring of flames at the edge of a 16 block
radius circle that travels inwards towards the boss, this ring travels over 1 block tall blocks.
Players hit by the ring take 10/20 damage, ignited for 5 seconds, and knocked towards the boss. Players
can be hit multiple times. After the ring reaches the horseman the fire erupts, dealing 20/30 damage
in a 5 block radius and knocking them away from the boss.


Phase 2 (Skills ADDED in phase 2. All the others are kept)
Phantom of the Opera (Idfk) - Fires out black projectiles at anyone within 12 blocks of himself after
a 0.8 seconds delay. Players hit by these projectiles are blinded for 5 seconds, dealt 16/28 damage
and 2 phantoms are summoned above the player with their aggro set to them.

On the Hunt - The Horseman begins marking the player that currently has aggro to be the target of
his hunt (They are warned he is doing so). After 1.5s, For the next 5 seconds the horseman has his
aggro permanently set to that enemy, gaining a burst of speed to run down his mark. If the player is
 dealt damage by The Horseman they are skewered through the chest, taking 28/42 damage and being given
 antiheal 5 for the next 7 seconds and wither 2 for 5 seconds. (Normal mode) In hard mode the speed is
 higher and the charge-up is reduced to 1 second.

(Ultimate) Hallow’s End - A pillar of smoke and flames appears on the horseman, after 1 second the area
nearby explodes, dealing 20/35 damage to players in a 4 block radius, igniting them for 8 seconds and
launching them upwards greatly. Afterwards pillars appear underneath ⅓ of the players within 32 blocks of
the horseman, after 1 second they also explode dealing the same thing. This continues to repeat as long as a
player is dealt damage by the pillars explosion to a max of 8 casts of the skill. (In hard mode players
are also blinded for 5 seconds.)

 */

public class HeadlessHorsemanBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_horseman";
	public static final int detectionRange = 22;
	public static final int arenaSize = 45;
	public int mCooldownTicks = 11 * 20;

	private final Location mOriginalSpawnLoc;
	private final Location mSpawnLoc;
	private final Location mEndLoc;

	private List<Player> mPlayers = new ArrayList<>();
	private boolean mCooldown = false;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new HeadlessHorsemanBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mOriginalSpawnLoc, mEndLoc);
	}

	public HeadlessHorsemanBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mOriginalSpawnLoc = spawnLoc;
		mSpawnLoc = spawnLoc.clone().subtract(0, 4, 0);
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);

		new BukkitRunnable() {
			@Nullable Creature mHorse = null;

			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}
				if (mHorse == null) {
					List<Entity> findHorse = mBoss.getNearbyEntities(5, 5, 5);
					for (Entity entity : findHorse) {
						if (entity.getType() == EntityType.SKELETON_HORSE) {
							mHorse = (Creature) entity;
						}
					}
				} else {
					if (!mPlayers.isEmpty()) {
						mHorse.setTarget(FastUtils.getRandomElement(mPlayers));
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 15 * 20);

		SpellManager phase1Spells = new SpellManager(Arrays.asList(
			new SpellHellzoneGrenade(plugin, boss, mSpawnLoc, detectionRange, mCooldownTicks, 4),
			new SpellBeeBombs(plugin, boss, mCooldownTicks, mSpawnLoc, 20, detectionRange),
			new SpellBurningVengence(plugin, boss, mCooldownTicks, mSpawnLoc, detectionRange, 0.5)
		));

		List<Spell> passives = Arrays.asList(
			// Teleport the boss to spawnLoc if he gets too far away from where he spawned
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> spawnLoc.distance(b.getLocation()) > 80),
			// Teleport the boss to spawnLoc if he is stuck in bedrock
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
				                                                   b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
				                                                   b.getLocation().getBlock().getType() == Material.LAVA),
			new SpellPhantomOfTheOpera(plugin, boss, mSpawnLoc, detectionRange, 20 * 60),
			new SpellShieldStun(30 * 20),
			new SpellRemoveLevitation(mBoss)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss -> {
			mPlayers.forEach(p -> p.sendMessage(Component.text("[The Horseman] ", NamedTextColor.DARK_RED).append(Component.text("You? You're who they sent? Oh ho ho, ", NamedTextColor.GOLD)).append(Component.text("we're ", NamedTextColor.DARK_RED)).append(Component.text("going to have some fun.", NamedTextColor.GOLD))));
		});

		events.put(50, mBoss -> {
			SpellManager p2C9Spells = new SpellManager(Arrays.asList(
				new SpellHellzoneGrenade(plugin, boss, mSpawnLoc, detectionRange, 9 * 20, 4),
				new SpellBeeBombs(plugin, boss, 9 * 20, mSpawnLoc, 20, detectionRange),
				new SpellBurningVengence(plugin, boss, 9 * 20, mSpawnLoc, detectionRange, 0.5),
				new SpellHallowsEnd(plugin, boss, 9 * 20, this),
				new SpellReaperOfLife(plugin, boss, mSpawnLoc, detectionRange, 9 * 20)
			));
			changePhase(p2C9Spells, passives, null);
			mPlayers.forEach(p -> p.sendMessage(Component.text("[The Horseman] ", NamedTextColor.DARK_RED).append(Component.text("Ha ha ha! I haven't felt this alive for what feels like eternity! ", NamedTextColor.GOLD)).append(Component.text("We'll ", NamedTextColor.DARK_RED)).append(Component.text("have to speed this up!", NamedTextColor.GOLD))));
			forceCastSpell(SpellReaperOfLife.class);
		});

		events.put(30, mBoss -> {
			SpellManager p2C6Spells = new SpellManager(Arrays.asList(
				new SpellHellzoneGrenade(plugin, boss, mSpawnLoc, detectionRange, 6 * 20, 4),
				new SpellBeeBombs(plugin, boss, 6 * 20, mSpawnLoc, 20, detectionRange),
				new SpellBurningVengence(plugin, boss, 6 * 20, mSpawnLoc, detectionRange, 0.5),
				new SpellHallowsEnd(plugin, boss, 6 * 20, this),
				new SpellReaperOfLife(plugin, boss, mSpawnLoc, detectionRange, 6 * 20)
			));
			//to enforce the new cooldown
			changePhase(p2C6Spells, passives, null);
			mPlayers.forEach(p -> p.sendMessage(Component.text("[The Horseman] ", NamedTextColor.DARK_RED).append(Component.text("Let's speed this up just a bit more!", NamedTextColor.GOLD))));
		});

		events.put(10, mBoss -> {
			mPlayers.forEach(p -> p.sendMessage(Component.text("[The Horseman] ", NamedTextColor.DARK_RED).append(Component.text("Ready or not, here they come!", NamedTextColor.GOLD))));
			forceCastSpell(SpellHellzoneGrenade.class);
		});

		events.put(5, mBoss -> {
			forceCastSpell(SpellHellzoneGrenade.class);
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange * 2, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(phase1Spells, passives, detectionRange, bossBar);
	}

	public Location getSpawnLocation() {
		return mSpawnLoc;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		Location loc = mBoss.getLocation();
		if (event.getType() == DamageType.MELEE && damagee.getLocation().distance(loc) <= 2) {
			if (!mCooldown) {
				mCooldown = true;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCooldown = false, 20);
				for (Player player : mPlayers) {
					if (player != damagee && player.getLocation().distance(loc) <= 4) {
						BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, event.getDamage());
					}
				}
				World world = mBoss.getWorld();
				new PartialParticle(Particle.DAMAGE_INDICATOR, loc, 30, 2, 2, 2, 0.1).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 10, 2, 2, 2, 0.1).spawnAsEntityActive(mBoss);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1, 0);
			}
		}

		if (damagee.getLocation().distance(loc) < 2.5) {
			double dist = 100;
			Player target = null;
			for (Player p : mPlayers) {
				if (p != damagee && p.getLocation().distance(mBoss.getLocation()) < dist) {
					target = p;
					dist = p.getLocation().distance(mBoss.getLocation());
				}
			}

			if (target != null) {
				((Creature) mBoss).setTarget(target);
			}
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (source instanceof Player player && !mPlayers.contains(player)) {
			event.setCancelled(true);
		}
	}

	public LivingEntity getEntity() {
		return mBoss;
	}

	@Override
	public void init() {
		mPlayers = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
		// Players are given the HorsemanFight tag upon starting the fight
		mPlayers.removeIf(p -> !ScoreboardUtils.checkTag(p, "HorsemanFight"));
		int playerCount = mPlayers.size();
		int hpDelta = 2500;
		double finalHp = hpDelta * BossUtils.healthScalingCoef(playerCount, 0.6, 0.35);
		EntityUtils.setMaxHealthAndHealth(mBoss, finalHp);

		for (Player player : mPlayers) {
			MessagingUtils.sendTitle(player, Component.text("Headless Horseman", NamedTextColor.DARK_RED, TextDecoration.BOLD), Component.text("Scourge of the Isles", NamedTextColor.RED, TextDecoration.BOLD));
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (Player player : mPlayers) {
			player.sendMessage(Component.text("[The Horseman] No matter. I'll be seeing you all again soon.", NamedTextColor.DARK_RED));
			player.playSound(player.getLocation(), Sound.ENTITY_HORSE_DEATH, SoundCategory.HOSTILE, 1.0f, 0.1f);
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 4));
		}
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mSpawnLoc, arenaSize)) {
			if (mob instanceof Phantom) {
				mob.setHealth(0);
			}
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		mPlayers.remove(event.getPlayer());
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}
}
