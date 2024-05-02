package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.falsespirit.*;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.worlds.common.MMLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class FalseSpirit extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_falsespirit";
	public static final int detectionRange = 75;
	public static final int meleeRange = 10;
	// WARNING: Ceiling Portal should only have PORTAL_CEILING_TAG as a tag
	public static final String PORTAL_CEILING_TAG = "CeilingPoH";

	private static final int HEALTH_HEALED = 100;
	private static final String PORTAL_TAG = "PortalOfHell";
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);
	// The material that the ground that should damage you is made out of
	private static final EnumSet<Material> mGroundMats = EnumSet.of(
		Material.BLACK_CONCRETE_POWDER,
		Material.GRAY_CONCRETE_POWDER,
		Material.BLACKSTONE,
		Material.BASALT
	);
	private static final Component bhairaviBackseating = Component.text("[Bhairavi]", NamedTextColor.GOLD)
		.append(Component.text(" Quickly! Kill those creatures! They will charge the Spear with power and let you claim it!", NamedTextColor.WHITE));
	private static final Component[] fightDialogue = new Component[] {
		Component.text("I am deeper than the power of Malkus... I shall take you into the nothingness from which you came.", NamedTextColor.DARK_RED),
		Component.text("The Gates open! Come forth, nithlings!", NamedTextColor.DARK_RED),
		Component.text("The Gates open once more! Come forth, nithlings!", NamedTextColor.DARK_RED),
		Component.text("I am more than the nithlings from my gates. The magic of Hallud flows through me. Battle me and perish.", NamedTextColor.DARK_RED),
		Component.text("Despite your efforts, nothingness is what you will return to. The magic of Midat is with me.", NamedTextColor.DARK_RED),
		Component.text("The Gates are opened! Come forth, nithlings!", NamedTextColor.DARK_RED),
		Component.text("The Final Gate opens. Meet your demise.", NamedTextColor.DARK_RED),
		Component.text("The Tree of Life calls. I only wish to answer...", NamedTextColor.DARK_RED)
	};

	private final LapseOfReality mMania;
	private final GatesOfHell mHell;

	private GatesOfHell mCeilingHell;
	private double mScalingCoefficient;

	public FalseSpirit(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		// If delves is enabled in instance, turn on delves mode
		boolean isDelve = false;
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 100, true);
		for (Player player : players) {
			if (DelvesUtils.getPlayerTotalDelvePoint(null, player, ServerProperties.getShardName()) > 0) {
				isDelve = true;
				break;
			}
		}

		mMania = new LapseOfReality(boss, plugin);

		List<LivingEntity> portals = new ArrayList<>(24);
		LivingEntity ceilingPortal = null;
		for (LivingEntity e : EntityUtils.getNearbyMobs(spawnLoc, 75, EnumSet.of(EntityType.ARMOR_STAND))) {
			Set<String> tags = e.getScoreboardTags();
			for (String tag : tags) {
				switch (tag) {
					case PORTAL_TAG -> portals.add(e);
					case PORTAL_CEILING_TAG -> ceilingPortal = e;
				}
			}
		}

		// Theoretically ceilingPortal should never be null after the above block runs because the armor stands are
		// spawned via mechs, but I'm adding a log here in case it does happen
		mHell = new GatesOfHell(plugin, boss, portals, 1);
		try {
			mCeilingHell = new GatesOfHell(plugin, boss, new ArrayList<>(List.of(ceilingPortal)), 5);
		} catch (NullPointerException exception) {
			exception.printStackTrace();
			MMLog.warning("[False Spirit] mCeilingHell in FalseSpirit.java failed to initialize!");
		}

		int multiEarthshakeDuration = 50;
		int passiveCooldown = 20 * 8;
		double passiveSpeed = .25;

		if (isDelve) {
			multiEarthshakeDuration = 30;
			passiveCooldown = 20 * 6;
			passiveSpeed = .3;
		}

		SpellManager phase1Spells = new SpellManager(Arrays.asList(
			new SpellForceTwo(plugin, boss, 5, 20 * 2),
			new TriplicateSlash(plugin, boss)
		));

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellForceTwo(plugin, boss, 5, 20 * 2),
			new TriplicateSlash(plugin, boss),
			new SpellMultiEarthshake(plugin, boss, 1, multiEarthshakeDuration, isDelve, mSpawnLoc),
			new SpellFlamethrower(plugin, boss, isDelve)
		));


		List<Spell> passiveSpells = Arrays.asList(
			new SpellPurgeNegatives(boss, 20 * 5),
			new DamageBlocker(plugin, boss, mHell, mCeilingHell),
			new SpellBlockBreak(boss, 2, 3, 2),
			new NothingnessSeeker(plugin, boss, passiveCooldown, passiveSpeed, isDelve)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();

		events.put(85, mBoss -> {
			mHell.run();
			sendDialogue(1, true);
		});

		events.put(70, mBoss -> {
			mHell.run();
			sendDialogue(2, true);
		});

		events.put(66, mBoss -> {
			mMania.setSpeed(4);
			mMania.run();

			//Prevents other moves from being used during bullet world
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
			new BukkitRunnable() {
				@Override
				public void run() {
					changePhase(activeSpells, passiveSpells, null);
				}
			}.runTaskLater(mPlugin, mMania.cooldownTicks());

			sendDialogue(3, false);
		});

		events.put(33, mBoss -> {
			mMania.setSpeed(2);
			mMania.run();

			//Prevents other moves from being used during bullet world
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
			new BukkitRunnable() {
				@Override
				public void run() {
					changePhase(activeSpells, passiveSpells, null);
				}
			}.runTaskLater(mPlugin, mMania.cooldownTicks());

			sendDialogue(4, false);
		});

		events.put(30, mBoss -> {
			mHell.run();
			sendDialogue(5, true);
		});

		//Last one is the ceiling one
		events.put(10, mBoss -> {
			mCeilingHell.run();
			mMania.run();

			//Prevents other moves from being used during bullet world
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
			new BukkitRunnable() {
				@Override
				public void run() {
					changePhase(activeSpells, passiveSpells, null);
				}
			}.runTaskLater(mPlugin, mMania.cooldownTicks());

			sendDialogue(6, true);
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.WHITE, BarStyle.SEGMENTED_10, events);
		constructBoss(phase1Spells, passiveSpells, detectionRange, bossBar, 20 * 10);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				if (mSpawnLoc.distance(mBoss.getLocation()) > meleeRange) {
					mTicks += 10;

					if (mTicks >= 20 * 5) {
						teleport(mSpawnLoc);
						mTicks = 0;
					}
				} else {
					mTicks = 0;
				}
			}
		}.runTaskTimer(mPlugin, 0, 10);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				//Damage players below the arena
				List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
				for (Player p : players) {
					if (p.getLocation().getY() <= 3 && mGroundMats.contains(p.getLocation().add(0, -1, 0).getBlock().getType())) {
						Vector vel = p.getVelocity();
						BossUtils.bossDamagePercent(mBoss, p, 0.1);
						p.setVelocity(vel);

						p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1, 0.5f);
						new PartialParticle(Particle.FLAME, p.getLocation(), 10, 0.5, 0.25, 0.5, 0.2).spawnAsEntityActive(boss);
					}
				}
				int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
				mScalingCoefficient = BossUtils.healthScalingCoef(playerCount, 0.5, 0.6);
			}
		}.runTaskTimer(mPlugin, 0, 20);

		sendDialogue(0, false);
		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, Component.text("False Spirit", NamedTextColor.RED), Component.text("Remnant of Olive", NamedTextColor.DARK_RED));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.75f);
		}
	}

	@Override
	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {
		if (event.getEntity() instanceof Fireball ball) {
			ball.setIsIncendiary(false);
			ball.setYield(0f);
			ball.setFireTicks(0);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 1);
		}
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		// Do not take damage to the gate closer trident
		if (damager instanceof Trident trident) {
			ItemStack item = trident.getItemStack();

			if (ItemUtils.getPlainName(item).contains("Gate Closer")) {
				event.setCancelled(true);
			}
		}

	}

	// Reduce damage taken for each player by a percent
	@Override
	public void onHurt(DamageEvent event) {
		event.setDamage(event.getDamage() / mScalingCoefficient);
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();

		//Heal code
		double hp = mBoss.getHealth() + HEALTH_HEALED;
		double max = EntityUtils.getMaxHealth(mBoss);
		mBoss.setHealth(Math.min(hp, max));
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1, 1.25f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 1, 2f);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 1, 0), 5, 0.15, 0.15, 0.15, RED_COLOR).spawnAsEntityActive(mBoss);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	// Teleport with special effects
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

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);

		BossUtils.endBossFightEffects(mBoss, players, 20 * 10, true, false);
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		teleport(mSpawnLoc);
		sendDialogue(7, false);

		if (event != null) {
			event.setCancelled(true);
			event.setReviveHealth(100);
		}

		World world = mBoss.getWorld();
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= 20 * 5) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 10, 0);

					this.cancel();
					mBoss.remove();

					new BukkitRunnable() {
						@Override
						public void run() {
							for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
								MessagingUtils.sendBoldTitle(player, Component.text("VICTORY", NamedTextColor.RED), Component.text("False Spirit, Remnant of Olive", NamedTextColor.DARK_RED));
								player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 100, 0.8f);
							}

							mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
						}
					}.runTaskLater(mPlugin, 20 * 3);
				}

				if (mTicks % 10 == 0) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);
				}

				new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 1, 1, 1, 1).minimumCount(1).spawnAsEntityActive(mBoss);

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public void init() {
		int hpDel = 3000;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		mScalingCoefficient = BossUtils.healthScalingCoef(playerCount, 0.5, 0.6);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, hpDel);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(hpDel);

		mBoss.setPersistent(true);
	}

	private void sendDialogue(int dialogueIndex, boolean bhairaviTalkingPrivileges) {
		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
			player.sendMessage(fightDialogue[dialogueIndex]);
			if (bhairaviTalkingPrivileges) {
				player.sendMessage(bhairaviBackseating);
			}
		}
	}
}
