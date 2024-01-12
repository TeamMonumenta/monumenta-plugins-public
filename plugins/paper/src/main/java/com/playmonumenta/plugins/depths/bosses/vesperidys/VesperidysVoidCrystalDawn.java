package com.playmonumenta.plugins.depths.bosses.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellMobHealAoE;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVoidCrystalTeleportPassive;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VesperidysVoidCrystalDawn extends BossAbilityGroup {
	public static final String identityTag = "boss_vesperidysvoidcrystaldawn";

	private final Vesperidys mVesperidys;
	private final Plugin mMonuPlugin;

	private static final int HEAL = 200;
	private static final double MAX_HEALTH_PERCENTAGE = 0.5;
	private static final int DELAY = 2 * 20;
	private static final int COOLDOWN = 20 * 20;
	private static final int DURATION = 4 * 20;
	private static final int PARTICLE_RADIUS = 10;
	private static final boolean CAN_MOVE = false;
	private static final boolean OVERHEAL = true;
	private final EntityTargets TARGETS = EntityTargets.GENERIC_MOB_TARGET.clone().setOptional(false);
	private static final ParticlesList PARTICLE_CHARGE_AIR = ParticlesList.fromString("[(SPELL_INSTANT,3)]");
	private static final ParticlesList PARTICLE_CHARGE_CIRCLE = ParticlesList.fromString("[(SPELL_INSTANT,3)]");
	private static final SoundsList SOUND_CHARGE = SoundsList.fromString("[(ITEM_TRIDENT_RETURN,0.8)]");
	private static final SoundsList SOUND_OUTBURST_CIRCLE = SoundsList.fromString("[(ENTITY_ILLUSIONER_CAST_SPELL,3,1.25),(ENTITY_ZOMBIE_VILLAGER_CONVERTED,3,2)]");
	private static final ParticlesList PARTICLE_OUTBURST_AIR = ParticlesList.fromString("[(FIREWORKS_SPARK,3),(VILLAGER_HAPPY,3,3.5,3.5,3.5,0.5)]");
	private static final ParticlesList PARTICLE_OUTBURST_CIRCLE = ParticlesList.fromString("[(CRIT_MAGIC,3,0.25,0.25,0.25,0.35),(FIREWORKS_SPARK,2,0.25,0.25,0.25,0.15)]");
	private static final ParticlesList PARTICLE_HEAL = ParticlesList.fromString("[(FIREWORKS_SPARK,3,0.25,0.5,0.25,0.3),(HEART,3,0.4,0.5,0.4)]");


	public static final Vector[] MODEL_OFFSETS = {
		new Vector(2, 0, 0),
		new Vector(0, 0, 2),
		new Vector(-2, 0, 0),
		new Vector(0, 0, -2),
		new Vector(0, -4, 0)
	};
	public static final Color START_COLOR = Color.WHITE;
	public static final Color END_COLOR = Color.ORANGE;
	public static final int FALL_TIME_TICKS = 24;
	public double mSpawnHeight;
	public double mFallDistance;
	public static final double DAMAGE = 120;
	public static final double DAMAGE_RADIUS = 4;
	public static final List<Material> GROUND_QUAKE_BLOCKS = List.of(Material.DARK_PRISMARINE, Material.PRISMARINE_BRICKS, Material.PEARLESCENT_FROGLIGHT, Material.GLOWSTONE);
	public static final Particle.DustOptions END_DUST_OPTIONS = new Particle.DustOptions(END_COLOR, 2f);
	public static final int CAST_DELAY = 50;
	public static final int TELEGRAPH_MODULO = 10;
	public static final float TELEGRAPH_SOUND_PITCH = 0.5f;
	public static final float TELEGRAPH_SOUND_PITCH_INCREASE = 1.5f * TELEGRAPH_MODULO / CAST_DELAY;

	public static @Nullable VesperidysVoidCrystalDawn deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable VesperidysVoidCrystalDawn construct(Plugin plugin, LivingEntity boss) {
		// Get nearest entity called Vesperidys.
		Vesperidys vesperidys = null;
		List<LivingEntity> witherSkeletons = EntityUtils.getNearbyMobs(boss.getLocation(), 100, EnumSet.of(EntityType.WITHER_SKELETON));
		for (LivingEntity mob : witherSkeletons) {
			if (mob.getScoreboardTags().contains(Vesperidys.identityTag)) {
				vesperidys = BossUtils.getBossOfClass(mob, Vesperidys.class);
				break;
			}
		}
		if (vesperidys == null) {
			MMLog.warning("VesperidysBlockPlacerBoss: Vesperidys wasn't found! (This is a bug)");
			return null;
		}
		return new VesperidysVoidCrystalDawn(plugin, boss, vesperidys);
	}

	public VesperidysVoidCrystalDawn(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		super(plugin, identityTag, boss);
		mMonuPlugin = plugin;
		mVesperidys = vesperidys;

		mSpawnHeight = vesperidys.mSpawnLoc.getY() + 10;
		mFallDistance = mSpawnHeight - vesperidys.mSpawnLoc.getY();

		Spell spell = new SpellMobHealAoE(
			plugin,
			boss,
			COOLDOWN,
			DURATION,
			PARTICLE_RADIUS,
			CAN_MOVE,
			() -> {
				return TARGETS.getTargetsList(mBoss);
			},
			(Location loc, int ticks) -> {
				PARTICLE_CHARGE_AIR.spawn(boss, loc, 3.5, 3.5, 3.5, 0.25);
				if (ticks <= (DURATION - 5) && ticks % 2 == 0) {
					SOUND_CHARGE.play(mBoss.getLocation(), 0.8f, 0.25f + ((float) ticks / (float) 100));
				}

				open();
			},
			(Location loc, int ticks) -> {
				PARTICLE_CHARGE_CIRCLE.spawn(boss, loc, 0.25, 0.25, 0.25);
			},
			(Location loc, int ticks) -> {
				PARTICLE_OUTBURST_AIR.spawn(boss, loc, 3.5, 3.5, 3.5, 0.25);
				SOUND_OUTBURST_CIRCLE.play(loc);
			},
			(Location loc, int ticks) -> {
				PARTICLE_OUTBURST_CIRCLE.spawn(boss, loc);
				close();
			},
			(LivingEntity target) -> {
				PARTICLE_HEAL.spawn(boss, target.getEyeLocation());
				if (target == mVesperidys.mBossTwo) {
					AbsorptionUtils.addAbsorption(target, HEAL * 2, EntityUtils.getMaxHealth(target) * MAX_HEALTH_PERCENTAGE, -1);
				} else {
					double max = EntityUtils.getMaxHealth(target);
					double hp = target.getHealth() + HEAL;
					if (hp >= max) {
						target.setHealth(max);
						if (OVERHEAL) {
							int missing = (int) (hp - max);
							AbsorptionUtils.addAbsorption(target, missing, EntityUtils.getMaxHealth(target) * MAX_HEALTH_PERCENTAGE, -1);
						}
					} else {
						target.setHealth(hp);
					}
				}

				// Buffs
				mMonuPlugin.mEffectManager.addEffect(target, "DawnCrystalSpeedBuff", new PercentSpeed(15 * 20, 0.2, "DawnCrystalSpeedBuff"));
				mMonuPlugin.mEffectManager.addEffect(target, "DawnCrystalDamageBuff", new PercentDamageDealt(15 * 20, 0.2));
				mMonuPlugin.mEffectManager.addEffect(target, "DawnCrystalResistanceBuff", new PercentDamageReceived(15 * 20, -0.2));
			}
		);

		SpellManager activeSpells = new SpellManager(List.of(spell));

		List<Spell> passiveSpells = List.of(
			new SpellVoidCrystalTeleportPassive(mVesperidys, boss)
		);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, Vesperidys.detectionRange);
		super.constructBoss(activeSpells, passiveSpells, Vesperidys.detectionRange, null, DELAY);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 50, true);
		for (int i = 0; i < 3; i++) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 8f, 1f);
				Trident trident = mBoss.getWorld().spawn(mBoss.getLocation(), Trident.class);
				trident.setVelocity(new Vector(0, 5, 0));
				trident.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
				trident.setHasDealtDamage(true);
			}, i * 10L);
		}

		// Telegraph the fang, damocles sword
		new BukkitRunnable() {
			int mTicks = 0;
			float mCurrentPitch = TELEGRAPH_SOUND_PITCH;

			@Override
			public void run() {
				if (mVesperidys.mDefeated) {
					this.cancel();
					return;
				}

				players.removeIf(player -> player.isDead() || player.getLocation().distance(mVesperidys.mSpawnLoc) > Vesperidys.detectionRange);

				if (mTicks % TELEGRAPH_MODULO == 0) {
					List<Location> centers = players.stream().map(Player::getLocation).toList();
					for (Location center : centers) {
						// Circle on the ground
						Location groundCenter = center.clone();
						groundCenter.setY(mVesperidys.mSpawnLoc.getY());
						new PPCircle(Particle.REDSTONE, groundCenter, DAMAGE_RADIUS).data(END_DUST_OPTIONS).count(15).spawnAsBoss();
						// Fang
						Location fangCenter = center.clone();
						fangCenter.setY(mSpawnHeight);
						drawFang(fangCenter);
					}
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.HOSTILE, 8f, mCurrentPitch);
					mCurrentPitch += TELEGRAPH_SOUND_PITCH_INCREASE;
				}
				if (mTicks >= CAST_DELAY) {
					// Drop it like it's hot
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 8f, 0.5f);
					dropFang();
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 30, 1);
	}

	public void open() {
		if (mBoss instanceof Shulker shulker && shulker.getPeek() < 0.5f) {
			shulker.setPeek(1.0f);
		}
	}

	public void close() {
		if (mBoss instanceof Shulker shulker && shulker.getPeek() > 0.5f) {
			shulker.setPeek(0.0f);
		}
	}

	private void dropFang() {
		List<Location> centers = mBoss.getLocation().getNearbyPlayers(60).stream().filter(p -> !p.getGameMode().equals(GameMode.SPECTATOR)).map(Player::getLocation).toList();
		for (Location center : centers) {
			Location startCenter = center.clone();
			startCenter.setY(mSpawnHeight);
			new BukkitRunnable() {
				int mTicks = 0;
				final Location mCenter = startCenter;
				final double mFallAmount = (double) mFallDistance / (double) FALL_TIME_TICKS;

				@Override
				public void run() {
					drawFang(mCenter);
					mCenter.add(0, -mFallAmount, 0);

					mTicks++;
					if (mTicks >= FALL_TIME_TICKS) {
						// Do damage
						mBoss.getWorld().playSound(mCenter, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 2f, 2f);
						mBoss.getWorld().playSound(mCenter, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 1f);
						new PartialParticle(Particle.CLOUD, mCenter, 50).delta(0.5).extra(0.5).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.BLOCK_CRACK, mCenter, 50).delta((DAMAGE_RADIUS - 1) / 2, 0, (DAMAGE_RADIUS - 1) / 2)
							.data(Material.OCHRE_FROGLIGHT.createBlockData()).extra(0.5).spawnAsEntityActive(mBoss);
						// Circle on the ground again
						new PPCircle(Particle.REDSTONE, mCenter, DAMAGE_RADIUS).data(END_DUST_OPTIONS).count(15).spawnAsBoss();
						// Block quake, making sure it spawns at the correct height of the floor
						mCenter.setY(mVesperidys.mSpawnLoc.getY());
						mCenter.setY(mVesperidys.mSpawnLoc.getY());
						DisplayEntityUtils.groundBlockQuake(mCenter, DAMAGE_RADIUS, GROUND_QUAKE_BLOCKS, new Display.Brightness(8, 8), 0.02);
						Hitbox hitbox = new Hitbox.UprightCylinderHitbox(mCenter, mFallDistance, DAMAGE_RADIUS);
						List<Player> hitPlayers = hitbox.getHitPlayers(true);
						for (Player hitPlayer : hitPlayers) {
							BossUtils.blockableDamage(mBoss, hitPlayer, DamageEvent.DamageType.PROJECTILE_SKILL, DAMAGE, "Divine Smite", mCenter);
						}
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void drawFang(Location center) {
		int units = 10;
		for (int i = 0; i < MODEL_OFFSETS.length - 1; i++) {
			ParticleUtils.drawLine(center.clone().add(MODEL_OFFSETS[i]), center.clone().add(MODEL_OFFSETS[4]), units,
				(l, t) -> {
					new PartialParticle(Particle.REDSTONE, l, 1).extra(0).data(new Particle.DustOptions(ParticleUtils.getTransition(START_COLOR, END_COLOR, t / (double) units), 2)).spawnAsBoss();
				}
			);
		}
	}
}
