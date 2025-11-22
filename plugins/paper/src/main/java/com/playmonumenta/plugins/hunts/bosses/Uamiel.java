package com.playmonumenta.plugins.hunts.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.HuntsManager;
import com.playmonumenta.plugins.hunts.bosses.spells.BanishParalyzingMusk;
import com.playmonumenta.plugins.hunts.bosses.spells.CragSplit;
import com.playmonumenta.plugins.hunts.bosses.spells.GaianRoots;
import com.playmonumenta.plugins.hunts.bosses.spells.PetrifyingRoar;
import com.playmonumenta.plugins.hunts.bosses.spells.RockyCharge;
import com.playmonumenta.plugins.hunts.bosses.spells.TectonicSlam;
import com.playmonumenta.plugins.hunts.bosses.spells.TerraBlast;
import com.playmonumenta.plugins.hunts.bosses.spells.TerrestrialShield;
import com.playmonumenta.plugins.hunts.bosses.spells.Tremor;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Uamiel extends Quarry {
	public static final String identityTag = "boss_uamiel";

	public static final int INNER_RADIUS = 35;
	public static final int OUTER_RADIUS = 100;

	// maximum health of the boss
	public static final int MAX_HEALTH = 8500;

	public static final TextColor TEXT_COLOR = TextColor.color(63, 117, 71);

	// how long a players shield will get stunned for if blocking while hit by an attack
	public static final int SHIELD_STUN_TIME = 20 * 8;

	// the movement speed of the boss, in whatever mojang's units are
	public static final double MOVEMENT_SPEED = 0.18;

	// the melee attack damage of the boss
	private static final int ATTACK_DAMAGE = 50;

	// how long since the boss has not hit its target and how far from the target it has to be for it to become more aggressive
	private static final int TIME_WITHOUT_HIT_AGGRO = 100;
	private static final double DISTANCE_WITHOUT_HIT_AGGRO = 8;

	// the possibilities for the blocks of the slam to be composed of
	public static final List<Material> DISPLAY_BLOCK_OPTIONS = new ArrayList<>(Arrays.asList(Material.TUFF, Material.MOSSY_COBBLESTONE, Material.COBBLESTONE, Material.DEAD_HORN_CORAL_BLOCK));

	private @Nullable Spell mLastCastedSpell = null;

	private int mTimeSinceLastTargetHit = 0;
	private final RockyCharge mCatchUpCharge;

	public boolean mIsBanishing = false;

	private boolean mLastPhase = false;

	private final World mWorld;

	// 3 blocks above mSpawnLocation (which is in the ground)
	public final Location mCenterLocation;

	public Uamiel(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc, INNER_RADIUS, OUTER_RADIUS, HuntsManager.QuarryType.UAMIEL);

		mCenterLocation = spawnLoc.clone().add(0, 3, 0);

		mWorld = boss.getWorld();

		// Initialize base information
		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, MOVEMENT_SPEED);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, ATTACK_DAMAGE);
		EntityUtils.setMaxHealthAndHealth(mBoss, MAX_HEALTH);
		mBoss.setInvulnerable(true);
		mBoss.setPersistent(true);
		mBoss.setAI(false);

		mCatchUpCharge = new RockyCharge(mPlugin, mBoss, this, 0);
		TerrestrialShield terrestrialShield = new TerrestrialShield(mPlugin, mBoss, this);

		// Initialize spells
		SpellManager phase1Actives = new SpellManager(List.of(
			new TectonicSlam(mPlugin, mBoss, this, 20),
			new PetrifyingRoar(mPlugin, mBoss, this, 20),
			new RockyCharge(mPlugin, mBoss, this, 20),
			terrestrialShield
		));
		SpellManager phase2Actives = new SpellManager(List.of(
			new TectonicSlam(mPlugin, mBoss, this, 0),
			new PetrifyingRoar(mPlugin, mBoss, this, 0),
			new RockyCharge(mPlugin, mBoss, this, 0),
			terrestrialShield
		));
		SpellManager phase3Actives = new SpellManager(List.of(
			new TectonicSlam(mPlugin, mBoss, this, 0),
			new PetrifyingRoar(mPlugin, mBoss, this, 0),
			new RockyCharge(mPlugin, mBoss, this, 0),
			new GaianRoots(mPlugin, mBoss, this, 0),
			terrestrialShield
		));
		SpellManager phase4Actives = new SpellManager(List.of(
			new TectonicSlam(mPlugin, mBoss, this, -20),
			new PetrifyingRoar(mPlugin, mBoss, this, -20),
			new RockyCharge(mPlugin, mBoss, this, -20),
			new GaianRoots(mPlugin, mBoss, this, -20),
			terrestrialShield
		));

		List<Spell> part1Passives = List.of(
			new TerraBlast(mPlugin, mBoss, this),
			new SpellBlockBreak(mBoss, 4, 3, 4),
			new SpellShieldStun(SHIELD_STUN_TIME),
			new Tremor(mBoss, this)
		);
		List<Spell> part2Passives = List.of(
			new TerraBlast(mPlugin, mBoss, this),
			new SpellBlockBreak(mBoss, 4, 3, 4),
			new SpellShieldStun(SHIELD_STUN_TIME),
			new Tremor(mBoss, this),
			new CragSplit(mPlugin, mBoss, this)
		);

		// Initialize health manager
		Map<Integer, BossBarManager.BossHealthAction> events = getBaseHealthEvents();
		events.put(85, mBoss -> changePhase(phase2Actives, part2Passives, null)); // add crag split passive & speed up
		events.put(60, mBoss -> {
			changePhase(phase3Actives, part2Passives, null);
			forceCastSpell(TerrestrialShield.class);
		}); // add stone throw
		events.put(20, mBoss -> {
			mLastPhase = true;

			mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, 5f, 0.57f);
			mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_CELEBRATE, SoundCategory.HOSTILE, 3.5f, 0.76f);

			changePhase(phase4Actives, part2Passives, null);
		}); // speed up

		mBanishSpell = new BanishParalyzingMusk(mPlugin, mBoss, this);

		// Initialize boss bar
		BossBarManager bossBar = new BossBarManager(mBoss, OUTER_RADIUS, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10, events, true, true, mSpawnLoc);

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 8f, 0.56f);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 8f, 0.66f);
			mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 8f, 0.8f);

			new PartialParticle(Particle.BLOCK_CRACK, mBoss.getEyeLocation().clone().add(mBoss.getEyeLocation().clone().getDirection()))
				.data(Material.TUFF.createBlockData())
				.count(200)
				.extra(0.2)
				.delta(0.5)
				.spawnAsBoss();
		}, 11 * 20);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks < 40) {
					mBoss.teleport(mBoss.getLocation().clone().add(0, 3.1 / 40, 0));
					new PartialParticle(Particle.BLOCK_CRACK, mCenterLocation.clone().add(0, 0.5, 0)).data(Material.ANDESITE.createBlockData())
						.count(10)
						.delta(2, 0, 2)
						.spawnAsBoss();
				}
				if (mTicks == 40) {
					mBoss.setAI(true);
					mBoss.setInvulnerable(false);

					// Finish boss
					constructBoss(phase1Actives, part1Passives, OUTER_RADIUS, bossBar, 300, 1);
				}

				if (mLastPhase && mTicks % 4 == 0 && mBoss.isOnGround()) {
					double r = FastUtils.randomDoubleInRange(0, Math.PI * 2);
					Location location = mBoss.getLocation().clone().add(FastUtils.cos(r) * FastUtils.randomDoubleInRange(2.5, 4.5), -0.95, FastUtils.sin(r) * FastUtils.randomDoubleInRange(2.5, 4.5));
					FallingBlock block = mWorld.spawn(location, FallingBlock.class, fb -> fb.setBlockData(DISPLAY_BLOCK_OPTIONS.get(FastUtils.randomIntInRange(0, DISPLAY_BLOCK_OPTIONS.size() - 1)).createBlockData()));
					EntityUtils.disableBlockPlacement(block);
					block.setDropItem(false);
					Vector velocity = new Vector(0, FastUtils.randomDoubleInRange(0.1, 0.25), 0);
					block.setVelocity(velocity);

					Bukkit.getScheduler().runTaskLater(mPlugin, block::remove, 10);
				}

				if (!mIsBanishing) {
					mTimeSinceLastTargetHit++;
					if (mTimeSinceLastTargetHit > TIME_WITHOUT_HIT_AGGRO && !hasRunningSpell() && mBoss instanceof Mob mob) {
						LivingEntity target = mob.getTarget();
						if (target instanceof Player player && target.getLocation().distance(mBoss.getLocation()) > DISTANCE_WITHOUT_HIT_AGGRO) {
							Location playerLocation = player.getLocation();
							Vector toTarget = playerLocation.clone().subtract(mBoss.getLocation()).toVector().normalize();
							Location targetLocation = playerLocation.clone().add(toTarget.clone().multiply(3));
							targetLocation.add(new Vector(0, 0.25, 0));

							mCatchUpCharge.charge(targetLocation);
							mTimeSinceLastTargetHit = 0;
						}
					}
				}

				mTicks++;
				if (mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				bossBar.remove();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	public boolean canRunSpell(Spell spell) {
		return (mLastCastedSpell == null || !mLastCastedSpell.equals(spell)) && !mIsBanishing;
	}

	public void ranBanished() {
		if (mBanishSpell != null) {
			mIsBanishing = true;
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> mIsBanishing = false, mBanishSpell.cooldownTicks());
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);

		// Prevent fall damage
		if (event.getCause() == EntityDamageEvent.DamageCause.FALL || event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
			event.setCancelled(true);
			return;
		}

		mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_DEEPSLATE_BRICKS_BREAK, SoundCategory.HOSTILE, 3.5f, 0.55f);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		super.bossChangedTarget(event);

		mTimeSinceLastTargetHit = 0;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		super.onDamage(event, damagee);

		if (mBoss instanceof Mob mob && event.getDamagee().equals(mob.getTarget())) {
			mTimeSinceLastTargetHit = 0;
		}

		// Cleave AOE
		if (event.getType() == DamageEvent.DamageType.MELEE && event.getBossSpellName() == null) {
			UUID uuid = damagee.getUniqueId();
			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4, true)) {
				if (!player.getUniqueId().equals(uuid)) {
					BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, event.getDamage(), "Rocky Cleave", null);
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
	public void death(@Nullable EntityDeathEvent event) {
		super.death(event);

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, SoundCategory.HOSTILE, 5f, 0.68f);
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		super.bossCastAbility(event);

		mLastCastedSpell = event.getSpell();
	}

	@Override
	public String getUnspoiledLootTable() {
		return "epic:r3/hunts/loot/uamiel_unspoiled";
	}

	@Override
	public String getSpoiledLootTable() {
		return "epic:r3/hunts/loot/uamiel_spoiled";
	}

	@Override
	public String getAdvancement() {
		return "monumenta:challenges/r3/hunts/uamiel";
	}

	@Override
	public String getQuestTag() {
		return "HuntRavager";
	}
}
