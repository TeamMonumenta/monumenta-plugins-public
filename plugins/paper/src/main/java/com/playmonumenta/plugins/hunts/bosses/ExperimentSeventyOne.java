package com.playmonumenta.plugins.hunts.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.Muddied;
import com.playmonumenta.plugins.effects.Parasites;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.HuntsManager;
import com.playmonumenta.plugins.hunts.bosses.spells.BanishDigMud;
import com.playmonumenta.plugins.hunts.bosses.spells.MudGeysers;
import com.playmonumenta.plugins.hunts.bosses.spells.MuddyLeap;
import com.playmonumenta.plugins.hunts.bosses.spells.MuddyShriek;
import com.playmonumenta.plugins.hunts.bosses.spells.Mudquake;
import com.playmonumenta.plugins.hunts.bosses.spells.PassiveExperimentBlockBreak;
import com.playmonumenta.plugins.hunts.bosses.spells.PassiveMudTrail;
import com.playmonumenta.plugins.hunts.bosses.spells.SludgeScatterShot;
import com.playmonumenta.plugins.hunts.bosses.spells.ToxicRepulsion;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ExperimentSeventyOne extends Quarry {
	public static final String identityTag = "boss_experimentseventyone";

	public static final int INNER_RADIUS = 30;
	public static final int OUTER_RADIUS = 65;
	private static final int MAX_HEALTH = 8000;
	private static final double MOVEMENT_SPEED = 0.29;
	private static final double KNOCKBACK_RESIST = 0.7;
	private static final int MELEE_ATTACK_DAMAGE = 55;

	private static final int INITIAL_WORM_SPAWNER_COOLDOWN = 20 * 5;
	private static final int PERMANENT_MUD_DURATION = 20 * 60 * 15;
	private static final String WORM_LOS = "ParasiticWorm";

	public static final int PARASITES_DURATION = 60;

	public static final int HIGH_PLAYER_CUTOFF = 5;

	private static final double DAMAGE_REDUCTION_PER_WORM = 0.05;
	private static final int DAMAGE_REDUCTION_TELEGRAPH_COOLDOWN = 20 * 3;

	private static final double DAMAGE_REDUCTION_DISTANCE = 8;

	private static final int SHIELD_STUN_TIME = 6 * 20;

	private static final double LEAP_DISTANCE = 10;
	private static final int LEAP_COOLDOWN = 12 * 20;

	public static final TextColor TEXT_COLOR = TextColor.color(179, 118, 48);

	public static final Material MUD_TRAIL = Material.MUD;
	public static final Material WORM_SPAWNER = Material.MANGROVE_ROOTS;
	public static final List<Material> MUD_TRAIL_BLOCKS = List.of(MUD_TRAIL, WORM_SPAWNER);
	public static final List<Material> INVALID_MUD_BLOCKS = List.of(Material.SPRUCE_WOOD, Material.DARK_OAK_LEAVES, Material.JUNGLE_LEAVES, Material.OAK_LEAVES);

	private static final int MUD_EFFECT_DURATION = 2 * 20;
	private static final String MUD_SLOWNESS_TAG = "ExperimentMudSlowness";
	private static final double MUD_SLOWNESS_AMOUNT = 0.2;
	private static final String MUD_HEALING_TAG = "ExperimentMudHealingReduction";
	private static final double MUD_HEALING_REDUCTION = 0.5;
	private static final String PARASITES_TAG = "ExperimentParasites";

	private @Nullable Spell mLastCastedSpell = null;

	private final List<Block> mMudReplacements = new ArrayList<>();
	private final Map<Block, Integer> mWormSpawners = new HashMap<>();

	private final List<LivingEntity> mWorms = new ArrayList<>();

	private @Nullable Entity mCurrentTarget;

	private final com.playmonumenta.plugins.Plugin mMonumentaPlugin;
	private final World mWorld;

	public ExperimentSeventyOne(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc, INNER_RADIUS, OUTER_RADIUS, HuntsManager.QuarryType.EXPERIMENT_SEVENTY_ONE);

		mMonumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();
		mWorld = boss.getWorld();

		// Initialize base information
		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, MOVEMENT_SPEED);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, MELEE_ATTACK_DAMAGE);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_RESIST);
		EntityUtils.setMaxHealthAndHealth(mBoss, MAX_HEALTH);
		mBoss.setPersistent(true);

		// Initialize spells
		MuddyLeap leap = new MuddyLeap(mPlugin, mBoss, this);
		SpellManager phase1Actives = new SpellManager(List.of(
			new MuddyShriek(mPlugin, mBoss, this, false, 0),
			new Mudquake(mPlugin, mBoss, this),
			new SludgeScatterShot(mPlugin, mBoss, this, false, 0),
			new ToxicRepulsion(mPlugin, mBoss, this, 0)
		));
		SpellManager phase2Actives = new SpellManager(List.of(
			new MuddyShriek(mPlugin, mBoss, this, false, -15),
			new Mudquake(mPlugin, mBoss, this),
			new SludgeScatterShot(mPlugin, mBoss, this, false, -15),
			new ToxicRepulsion(mPlugin, mBoss, this, -15),
			new MudGeysers(mPlugin, mBoss, this)
		));
		SpellManager phase3Actives = new SpellManager(List.of(
			new MuddyShriek(mPlugin, mBoss, this, true, -25),
			new Mudquake(mPlugin, mBoss, this),
			new SludgeScatterShot(mPlugin, mBoss, this, true, -25),
			new ToxicRepulsion(mPlugin, mBoss, this, -25),
			new MudGeysers(mPlugin, mBoss, this)
		));
		List<Spell> passives = List.of(
			new PassiveMudTrail(mBoss, this),
			new PassiveExperimentBlockBreak(mBoss, this),
			new SpellShieldStun(SHIELD_STUN_TIME)
		);
		mBanishSpell = new BanishDigMud(plugin, boss, this);

		Map<Integer, BossBarManager.BossHealthAction> events = getBaseHealthEvents();
		events.put(60, bossEntity -> changePhase(phase2Actives, passives, null));
		events.put(30, bossEntity -> changePhase(phase3Actives, passives, null));

		// Initialize boss bar
		BossBarManager bossBar = new BossBarManager(mBoss, OUTER_RADIUS, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, events, true, true, mSpawnLoc);

		// Finish boss
		constructBoss(phase1Actives, passives, OUTER_RADIUS, bossBar, 100, 1);

		// boss runnable
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			int mLeapCooldown = LEAP_COOLDOWN;

			@Override
			public void run() {
				if (mTicks % 5 == 0) {
					manageMudEffects();
				}

				manageMudTimings();

				if (mTicks % DAMAGE_REDUCTION_TELEGRAPH_COOLDOWN == 0) {
					mWorms.forEach(e -> ParticleUtils.launchOrb(new Vector(0, 1, 0), e.getLocation(), e, mBoss, 200, null, new Particle.DustOptions(Color.fromRGB(110, 63, 19), 1.2f), en -> {
					}));
				}

				mLeapCooldown--;
				if (mLeapCooldown <= 0 && !mMonumentaPlugin.mEffectManager.hasEffect(mBoss, "SelfRoot") && getCurrentTarget() != null && mBoss.getLocation().distance(getCurrentTarget().getLocation()) > LEAP_DISTANCE) {
					leap.run();
					mLeapCooldown = LEAP_COOLDOWN;
				}

				mTicks++;
				if (mBoss.isDead()) {
					this.cancel();

					mWorms.forEach(worm -> worm.damage(1000));
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private void manageMudTimings() {
		// remove invalid mud
		mMudReplacements.removeIf(block -> {
			if (block.getType() != MUD_TRAIL) {
				mBreakableBlocks.remove(block);
				return true;
			}
			return false;
		});
		// remove invalid worm spawners
		mWormSpawners.keySet().removeIf(block -> {
			if (block.getType() != WORM_SPAWNER) {
				mBreakableBlocks.remove(block);
				return true;
			}
			return false;
		});

		// manage worm spawners
		for (Map.Entry<Block, Integer> entry : mWormSpawners.entrySet()) {
			Block block = entry.getKey();
			int time = entry.getValue();
			time--;
			if (time < getWormSpawnDelay() / 5 && time % 5 == 0) {
				new PartialParticle(Particle.BLOCK_CRACK, block.getLocation().clone().add(0, 1, 0).toCenterLocation())
					.data(Material.MUDDY_MANGROVE_ROOTS.createBlockData())
					.count(5)
					.delta(0.25, 0, 0.25)
					.spawnAsBoss();
			}

			// if under worm cap, spawn worms, otherwise do nothing so worms will spawn as soon as a space is freed
			if (time == 0 && mWorms.size() < getMaxWorms()) {
				time = getWormSpawnDelay();

				// spawn worms
				Location location = block.getLocation().clone();
				boolean success = false;
				for (int blocksAbove = 1; blocksAbove < 10; blocksAbove++) {
					if (!location.clone().add(0, blocksAbove, 0).getBlock().getType().isSolid()) {
						location.add(0, blocksAbove, 0);
						success = true;
						break;
					}
				}
				if (success) {
					Entity spawnedWorm = LibraryOfSoulsIntegration.summon(location.toCenterLocation(), WORM_LOS);
					if (spawnedWorm instanceof LivingEntity worm) {
						EntityUtils.setRemoveEntityOnUnload(worm);
						mWorms.add(worm);

						new PartialParticle(Particle.REDSTONE, spawnedWorm.getLocation().clone().add(0, 0.5, 0))
							.data(new Particle.DustOptions(Color.fromRGB(51, 51, 17), 0.8f))
							.count(25)
							.delta(0.4, 0.4, 0.4)
							.extra(0.02)
							.spawnAsBoss();
					}
				}
			}
			entry.setValue(time);
		}
	}

	private int getMaxWorms() {
		return Math.min((int) (2 * Math.pow(mPlayers.size(), 0.617)), 8);
	}

	private int getWormSpawnDelay() {
		return Math.max((int) (20 * (15 - 2.25 * Math.sqrt(mPlayers.size()))), 4 * 20);
	}

	private void manageMudEffects() {
		EffectManager effectManager = mMonumentaPlugin.mEffectManager;
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), OUTER_RADIUS, true)) {
			if (MUD_TRAIL_BLOCKS.contains(player.getLocation().clone().add(0, -0.7, 0).getBlock().getType())) {
				mMonumentaPlugin.mEffectManager.addEffect(player, MUD_SLOWNESS_TAG, new PercentSpeed(MUD_EFFECT_DURATION, -MUD_SLOWNESS_AMOUNT, MUD_SLOWNESS_TAG));
				mMonumentaPlugin.mEffectManager.addEffect(player, MUD_HEALING_TAG, new PercentHeal(MUD_EFFECT_DURATION, -MUD_HEALING_REDUCTION));

				Parasites parasites = effectManager.getActiveEffect(player, Parasites.class);
				double amount = parasites == null ? 0 : parasites.getAmount();
				if (parasites != null && amount == 1) {
					parasites.setDuration(PARASITES_DURATION);
					return;
				}
				// function runs 4 times per second, aiming for 4 seconds before parasites amount goes from 0 to 1
				effectManager.clearEffects(player, PARASITES_TAG);
				effectManager.addEffect(player, PARASITES_TAG, new Parasites(Math.min(1, amount + 0.0625)));
			}
		}
	}

	private void manageBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		if (isMudTrail(block)) {
			event.setCancelled(true);

			mWorld.playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 0.15f, 0.93f);
			new PartialParticle(Particle.BLOCK_CRACK, player.getLocation()).data(MUD_TRAIL.createBlockData()).delta(0.3, 0.1, 0.3).count(15).spawnAsBoss();

			breakMud(block);
			if (mMudReplacements.size() >= 40) {
				for (BlockFace face : BlockUtils.CARTESIAN_BLOCK_FACES) {
					Block adjacent = block.getRelative(face);
					if (isMudTrail(adjacent)) {
						breakMud(adjacent);
					}
				}
			}

			countDownMudBlocksRemaining(player);
		} else if (isWormSpawner(block)) {
			event.setCancelled(true);

			mWorld.playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 0.35f, 0.8f);
			mWorld.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 0.8f, 0.75f);
			new PartialParticle(Particle.BLOCK_CRACK, player.getLocation()).data(WORM_SPAWNER.createBlockData()).delta(0.3, 0.1, 0.3).count(15).spawnAsBoss();

			TemporaryBlockChangeManager.INSTANCE.revertChangedBlock(block, WORM_SPAWNER);
			mWormSpawners.remove(block);
			mBreakableBlocks.remove(block);
			countDownMudBlocksRemaining(player);
		}
	}

	private void breakMud(Block block) {
		TemporaryBlockChangeManager.INSTANCE.revertChangedBlock(block, MUD_TRAIL);
		mMudReplacements.remove(block);
		mBreakableBlocks.remove(block);
	}

	private boolean isHiddenSpot(Block block) {
		Location loc = block.getLocation().clone().add(0, 1, 0);
		if (LocationUtils.hasLineOfSight(loc, mBoss.getLocation())) {
			return false;
		}
		return LocationUtils.rayTraceToBlock(loc, new Vector(0, 1, 0), 10, null).add(0, 0.1, 0).getBlock().isSolid();
	}

	public void placeWormSpawner(Block block) {
		// never place a spawner on top of another worm spawner or in an invalid location or not in spots easily visible
		if (block.getType().getHardness() == -1
			|| ZoneUtils.hasZoneProperty(block.getLocation(), ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)
			|| isWormSpawner(block)
			|| INVALID_MUD_BLOCKS.contains(block.getType())
			|| isHiddenSpot(block)) {
			return;
		}

		// overwrite mud trails
		if (isMudTrail(block)) {
			TemporaryBlockChangeManager.INSTANCE.revertChangedBlock(block, MUD_TRAIL);
			mMudReplacements.remove(block);
		}

		new PartialParticle(Particle.BLOCK_CRACK, block.getLocation().clone().add(0, 1, 0).toCenterLocation())
			.data(WORM_SPAWNER.createBlockData())
			.count(10)
			.delta(0.25, 0, 0.25)
			.spawnAsBoss();

		// override any decaying blocks with a spawner that turns into air
		if (mDecayingBlocks.containsKey(block)) {
			mDecayingBlocks.remove(block);
			block.setType(Material.AIR);
		}

		TemporaryBlockChangeManager.INSTANCE.changeBlock(block, WORM_SPAWNER, PERMANENT_MUD_DURATION);
		mWormSpawners.put(block, INITIAL_WORM_SPAWNER_COOLDOWN);
		mBreakableBlocks.add(block);
	}

	public void placeMudBlock(Block block) {
		placeMudBlock(block, PERMANENT_MUD_DURATION);
	}

	public void placeMudBlock(Block block, int time) {
		// never place mud on top of a worm spawner or in an invalid location
		if (block.getType().getHardness() == -1
			|| ZoneUtils.hasZoneProperty(block.getLocation(), ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)
			|| isWormSpawner(block)
			|| INVALID_MUD_BLOCKS.contains(block.getType())
			|| block.getRelative(BlockFace.UP).isSolid()) {
			return;
		}

		if (isMudTrail(block)) {
			TemporaryBlockChangeManager.INSTANCE.increaseDuration(block, time);
		} else {
			// override any decaying blocks with mud that turns into air
			if (mDecayingBlocks.containsKey(block)) {
				mDecayingBlocks.remove(block);
				block.setType(Material.AIR);
			}

			TemporaryBlockChangeManager.INSTANCE.changeBlock(block, MUD_TRAIL, time);
			mMudReplacements.add(block);
			mBreakableBlocks.add(block);

			new PartialParticle(Particle.BLOCK_CRACK, block.getLocation().clone().add(0, 1, 0).toCenterLocation())
				.data(MUD_TRAIL.createBlockData())
				.count(5)
				.delta(0.25, 0, 0.25)
				.spawnAsBoss();
		}
	}

	public void resetAllMud() {
		mMudReplacements.forEach(mBreakableBlocks::remove);
		mMudReplacements.forEach((block) -> TemporaryBlockChangeManager.INSTANCE.revertChangedBlock(block, MUD_TRAIL));
		mMudReplacements.clear();
	}

	public void resetAllSpawners() {
		mBreakableBlocks.removeAll(mWormSpawners.keySet());
		mWormSpawners.keySet().forEach((block) -> TemporaryBlockChangeManager.INSTANCE.revertChangedBlock(block, WORM_SPAWNER));
		mWormSpawners.clear();
	}

	public boolean isMudTrail(Block block) {
		return mMudReplacements.contains(block);
	}

	public boolean isWormSpawner(Block block) {
		return mWormSpawners.containsKey(block);
	}

	public boolean isPlacedMud(Block block) {
		return isMudTrail(block) || isWormSpawner(block);
	}

	public List<Block> getMudBlocks() {
		List<Block> list = new ArrayList<>();
		list.addAll(mMudReplacements);
		list.addAll(mWormSpawners.keySet());
		return list;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		super.onDamage(event, damagee);

		// Cleave AOE
		if (event.getType() == DamageEvent.DamageType.MELEE && event.getBossSpellName() == null) {
			UUID uuid = damagee.getUniqueId();
			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4, true)) {
				if (!player.getUniqueId().equals(uuid)) {
					BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, event.getDamage(), "Muddy Cleave", null);
				}
			}

			mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1, 0.5f);
			new PartialParticle(Particle.SWEEP_ATTACK, mBoss.getLocation())
				.count(10)
				.delta(2, 0, 2)
				.extra(0.1)
				.spawnAsBoss();
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);

		double damage = event.getFlatDamage() * (1 - DAMAGE_REDUCTION_PER_WORM * Math.min(mWorms.size(), 20));
		if (!mWorms.isEmpty()) {
			new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().clone().add(0, 0.5, 0))
				.data(Material.SOUL_SAND.createBlockData())
				.count(15 + 5 * mWorms.size())
				.delta(0.5, 0.5, 0.5)
				.spawnAsBoss();
			mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_SOUL_SAND_HIT, SoundCategory.HOSTILE, 0.8f, 0.65f);
		}

		if (event.getDamager() instanceof Player player && !AbilityUtils.isIndirectDamage(event)) {
			Effect effect = mMonumentaPlugin.mEffectManager.getActiveEffect(player, PARASITES_TAG);
			if (effect instanceof Parasites parasites && parasites.getAmount() == 1 && !mSpoiledPlayers.contains(player.getUniqueId())) {
				if (spoil(player)) {
					player.sendMessage(Component.text("Your attack infects Experiment Seventy-One with parasites, spoiling your loot.", TEXT_COLOR));
				}
				new PartialParticle(Particle.FALLING_DUST, mBoss.getEyeLocation())
					.data(Material.MUD_BRICKS.createBlockData())
					.count(30)
					.delta(1, 1, 1)
					.extra(0.02)
					.spawnAsBoss();
				player.playSound(mBoss.getLocation(), Sound.ENTITY_HOGLIN_DEATH, SoundCategory.HOSTILE, 1f, 0.85f);
			}
		}

		if (event.getDamager() != null && event.getDamager().getLocation().distance(mBoss.getLocation()) > DAMAGE_REDUCTION_DISTANCE) {
			damage *= 0.5;
		}

		event.setFlatDamage(damage);
	}

	@Override
	public void nearbyBlockBreak(BlockBreakEvent event) {
		super.nearbyBlockBreak(event);

		manageBlockBreak(event);
	}

	private void countDownMudBlocksRemaining(Player player) {
		Effect effect = EffectManager.getInstance().getActiveEffect(player, BanishDigMud.MUDDIED_EFFECT_SOURCE);
		if (effect instanceof Muddied muddied) {
			muddied.mudBlockBroken();
			if (muddied.getMudBlocksRemaining() == 0) {
				EffectManager.getInstance().clearEffects(player, BanishDigMud.MUDDIED_EFFECT_SOURCE);
			}
		}
	}

	@Override
	public double nearbyEntityDeathMaxRange() {
		return OUTER_RADIUS;
	}

	@Override
	public void nearbyEntityDeath(EntityDeathEvent event) {
		super.nearbyEntityDeath(event);

		mWorms.remove(event.getEntity());
	}

	public boolean canRunSpell(Spell spell) {
		return (mLastCastedSpell == null || !mLastCastedSpell.equals(spell));
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		super.bossCastAbility(event);

		mLastCastedSpell = event.getSpell();
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		super.bossChangedTarget(event);

		mCurrentTarget = event.getTarget();
	}

	public @Nullable Entity getCurrentTarget() {
		return mCurrentTarget;
	}

	@Override
	public boolean hasNearbyBlockBreakTrigger() {
		return true;
	}

	@Override
	public boolean hasNearbyEntityDeathTrigger() {
		return true;
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		super.death(event);

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_HORSE_DEATH, SoundCategory.HOSTILE, 5f, 0.65f);

		// death animation
		new BukkitRunnable() {
			int mTicks = 0;
			final Location mLocation = mBoss.getLocation().clone();

			@Override
			public void run() {
				if (mTicks % 2 == 0) {
					new PartialParticle(Particle.BLOCK_CRACK, mLocation.clone().add(0, 0.4 + 0.075 * mTicks, 0))
						.data(Material.MUD.createBlockData())
						.count(20)
						.delta(0.2)
						.extra(0.05)
						.spawnAsBoss();
					mWorld.playSound(mLocation, Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 3f, 0.8f);
					mWorld.playSound(mLocation, Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 3f, 0.8f);
				}

				mTicks++;
				if (mTicks >= 20) {
					new PartialParticle(Particle.BLOCK_CRACK, mLocation.clone().add(0, 2, 0))
						.data(Material.MUD.createBlockData())
						.count(100)
						.delta(0.5)
						.extra(0.05)
						.spawnAsBoss();
					mWorld.playSound(mLocation, Sound.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.HOSTILE, 5f, 0.5f);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		Bukkit.getScheduler().runTask(mPlugin, () -> {
			resetAllMud();
			resetAllSpawners();
		});
	}

	@Override
	public void onDespawn() {
		resetAllMud();
		resetAllSpawners();
	}

	@Override
	public String getUnspoiledLootTable() {
		return "epic:r3/hunts/loot/experiment_seventy_one_unspoiled";
	}

	@Override
	public String getSpoiledLootTable() {
		return "epic:r3/hunts/loot/experiment_seventy_one_spoiled";
	}

	@Override
	public String getAdvancement() {
		return "monumenta:challenges/r3/hunts/experiment_seventy_one";
	}

	@Override
	public String getQuestTag() {
		return "HuntHoglin";
	}
}
