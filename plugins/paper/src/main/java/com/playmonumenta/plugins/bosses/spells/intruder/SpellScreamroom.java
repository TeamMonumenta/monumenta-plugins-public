package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.LucidRendBoss;
import com.playmonumenta.plugins.bosses.bosses.intruder.AbhorrentHallucinationBoss;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.delves.mobabilities.TwistedMiniBoss;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.SingleArgumentEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
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
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellScreamroom extends Spell {
	public static final String DEBUFF_ID = "ScreamroomDistortion";
	private final IntruderBoss.Dialogue mDialogue;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mYLevel;
	private final Consumer<Player> mOnDistort;
	private final IntruderBoss.Narration mNarration;
	private final List<PlayerScreamroom> mScreamrooms = new ArrayList<>();
	private final List<LivingEntity> mLucidRends = new ArrayList<>();
	private final World mWorld;
	private final ChargeUpManager mChargeUpManager;

	private final SpellCooldownManager mSpellCooldownManager;

	private static final String SPELL_NAME = "Screamroom (☠)";
	private static final int SIZE = 4;
	private static final int CHARGE_TIME = 4 * 20;
	private static final int DURATION = 10 * 20;

	public SpellScreamroom(Plugin plugin, LivingEntity boss, int yLevel, Consumer<Player> onDistort, IntruderBoss.Dialogue dialogue, IntruderBoss.Narration narration) {
		mPlugin = plugin;
		mBoss = boss;
		mYLevel = yLevel;
		mOnDistort = onDistort;
		mNarration = narration;
		mWorld = mBoss.getWorld();
		mDialogue = dialogue;
		mChargeUpManager = new ChargeUpManager(mBoss, CHARGE_TIME, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)), BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS, 75);
		mSpellCooldownManager = new SpellCooldownManager(60 * 20, 0, boss::isValid, boss::hasAI);
	}

	public class PlayerScreamroom {
		private final List<BlockDisplay> mWalls = new ArrayList<>();
		private final Slime mScreamroomCore;
		private final LivingEntity mScreamroomWall;
		private final ItemDisplay mDisplay;
		private final Player mPlayer;

		List<Block> mChangedBlocks = new ArrayList<>();

		public boolean mKilled = false;

		private final BukkitTask mSoulCaptureTask;

		public PlayerScreamroom(Player player) {
			Location blockLocation = player.getLocation();
			blockLocation.setY(mYLevel);
			player.teleport(blockLocation);

			mScreamroomCore = Objects.requireNonNull((Slime) LibraryOfSoulsIntegration.summon(blockLocation.clone().add(new Vector(0, 0.25, 0)), "ScreamroomCore"));
			mScreamroomWall = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(blockLocation, "ScreamroomWall"));

			BossManager.getInstance().createBossInternal(mScreamroomCore, new BossAbilityGroup(mPlugin, "ScreamroomCore", mScreamroomCore) {
				@Override
				public void onHurt(DamageEvent event) {
					if (event.getSource() != null && event.getSource() != mPlayer) {
						event.setCancelled(true);
					}
				}
			});

			BossManager.getInstance().createBossInternal(mScreamroomWall, new BossAbilityGroup(mPlugin, "ScreamroomWall", mScreamroomWall) {
				@Override
				public void onHurt(DamageEvent event) {
					if (event.getSource() == mPlayer) {
						event.setCancelled(true);
					}
				}
			});

			player.hideEntity(mPlugin, mScreamroomWall);
			mDisplay = player.getWorld().spawn(blockLocation, ItemDisplay.class);
			mDisplay.setItemStack(mScreamroomCore.getEquipment().getHelmet());
			mDisplay.setTransformation(new Transformation(
				new Vector3f(0, 1.5f, 0),
				new Quaternionf(),
				new Vector3f(2, 2, 2),
				new Quaternionf()
			));
			mPlayer = player;
			final float SIZE_WALL = SIZE - 0.1f;

			new PartialParticle(Particle.FLASH, blockLocation).minimumCount(1).spawnAsBoss();
			spawnDisplayEntity(Bukkit.createBlockData(Material.BLACK_CONCRETE),
				new Transformation(new Vector3f(0, 0, 0), new Quaternionf(),
					new Vector3f(0.2f, SIZE_WALL * 2, SIZE_WALL * 2),
					new Quaternionf()), blockLocation.clone().add(new Vector(-SIZE_WALL, 0, -SIZE_WALL))
			);

			spawnDisplayEntity(Bukkit.createBlockData(Material.BLACK_CONCRETE),
				new Transformation(new Vector3f(0, 0, 0), new Quaternionf(),
					new Vector3f(SIZE_WALL * 2, 0.2f, SIZE_WALL * 2),
					new Quaternionf()), blockLocation.clone().add(new Vector(-SIZE_WALL, 0, -SIZE_WALL))
			);

			spawnDisplayEntity(Bukkit.createBlockData(Material.BLACK_CONCRETE),
				new Transformation(new Vector3f(0, 0, 0), new Quaternionf(),
					new Vector3f(SIZE_WALL * 2, SIZE_WALL * 2, 0.2f),
					new Quaternionf()), blockLocation.clone().add(new Vector(-SIZE_WALL, 0, -SIZE_WALL))
			);

			spawnDisplayEntity(Bukkit.createBlockData(Material.BLACK_CONCRETE),
				new Transformation(new Vector3f(0, 0, 0), new Quaternionf(),
					new Vector3f(SIZE_WALL * 2, SIZE_WALL * 2, 0.2f),
					new Quaternionf()), blockLocation.clone().add(new Vector(-SIZE_WALL, 0, SIZE_WALL))
			);

			spawnDisplayEntity(Bukkit.createBlockData(Material.BLACK_CONCRETE),
				new Transformation(new Vector3f(0, 0, 0), new Quaternionf(),
					new Vector3f(0.2f, SIZE_WALL * 2, SIZE_WALL * 2),
					new Quaternionf()), blockLocation.clone().add(new Vector(SIZE_WALL, 0, -SIZE_WALL))
			);

			spawnDisplayEntity(Bukkit.createBlockData(Material.BLACK_CONCRETE),
				new Transformation(new Vector3f(0, 0, 0), new Quaternionf(),
					new Vector3f(SIZE_WALL * 2, 0.2f, SIZE_WALL * 2),
					new Quaternionf()), blockLocation.clone().add(new Vector(-SIZE_WALL, SIZE_WALL * 2, -SIZE_WALL))
			);

			spawnDisplayEntity(Bukkit.createBlockData(Material.BLACK_CONCRETE),
				new Transformation(new Vector3f(0, 0, 0), new Quaternionf(),
					new Vector3f(0.2f, SIZE_WALL * 2, SIZE_WALL * 2),
					new Quaternionf()), blockLocation.clone().add(new Vector(-SIZE_WALL, 0, -SIZE_WALL))
			);

			for (int x = -SIZE; x <= SIZE; x++) {
				for (int z = -SIZE; z <= SIZE; z++) {
					Location location = blockLocation.clone().add(x, -1, z);
					TemporaryBlockChangeManager.INSTANCE.changeBlock(location.getBlock(), Material.BLACK_CONCRETE, 12 * 20);
					mChangedBlocks.add(location.getBlock());
				}
			}

			// running the actual stuff:
			mNarration.narration("That pulsating mass at the center must be trapping you here...");

			// """Shield Wall"""
			mSoulCaptureTask = new BukkitRunnable() {
				int mSelfDamageCooldown = 0;
				int mOthersDamageCooldown = 0;


				int mSafety = 0;

				@Override
				public void run() {
					mSafety++;
					if (mSafety > DURATION) {
						removeScreamroom();
						this.cancel();
						return;
					}
					// Pulsate the mass
					mDisplay.setRotation((float) FastUtils.sin(mSafety * 0.66) * 10, (float) FastUtils.cos(mSafety * 1.33) * 5);

					if (mSelfDamageCooldown > 0) {
						mSelfDamageCooldown--;
					}
					if (mOthersDamageCooldown > 0) {
						mOthersDamageCooldown--;
					}
					Vector screamroomPlayerVec = mPlayer.getLocation().subtract(mScreamroomCore.getLocation()).toVector();
					if (mSelfDamageCooldown <= 0 && (Math.abs(screamroomPlayerVec.getX()) > SIZE || Math.abs(screamroomPlayerVec.getZ()) > SIZE || screamroomPlayerVec.getY() > SIZE * 2 || screamroomPlayerVec.getY() < -1)) {
						mSelfDamageCooldown = 10;
						BossUtils.bossDamagePercent(mScreamroomCore, mPlayer, 0.75, "Soul Capture");
						MovementUtils.pullTowardsNormalized(mScreamroomCore.getLocation().add(new Vector(0, 1, 0)), mPlayer, 0.7f, false);
						mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1f, 0.1f);
						mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 1f, 1.4f);
						mDialogue.dialogue("THERE IS NO WAY. TO LEAVE.");
						mPlayer.sendMessage(Component.text("Shouldn't wander too far into the darkness.", NamedTextColor.GRAY, TextDecoration.ITALIC));
					}
					IntruderBoss.playersInRange(mBoss.getLocation()).stream().filter(player1 -> {
							Vector otherPlayerVec = player1.getLocation().subtract(mScreamroomCore.getLocation()).toVector();
							return player1 != mPlayer && mOthersDamageCooldown <= 0 && Math.abs(otherPlayerVec.getX()) <= SIZE && Math.abs(otherPlayerVec.getZ()) <= SIZE && screamroomPlayerVec.getY() <= SIZE * 2 && screamroomPlayerVec.getY() > -1;
						})
						.forEach(player1 -> {
							mOthersDamageCooldown = 10;
							BossUtils.bossDamagePercent(mScreamroomCore, player1, 0.5, "Soul Isolation");
							MovementUtils.knockAway(mScreamroomCore.getLocation(), player1, 1);
							player1.playSound(player1.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1f, 0.1f);
							player1.playSound(mPlayer.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 1f, 1.4f);
						});
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		private void spawnDisplayEntity(BlockData data, Transformation transformation, Location location) {
			location.setPitch(0);
			location.setYaw(0);
			BlockDisplay display = mBoss.getWorld().spawn(location, BlockDisplay.class);
			display.setBlock(data);
			display.setInterpolationDelay(-1);
			display.setTransformation(transformation);
			display.setBrightness(new Display.Brightness(1, 1));
			mWalls.add(display);
		}

		public void onDeath() {
			new PPCircle(Particle.SQUID_INK, mScreamroomCore.getLocation().add(new Vector(0, 0.2, 0)), 2)
				.countPerMeter(10)
				.directionalMode(true)
				.rotateDelta(true)
				.delta(1, 0, 0.2)
				.extra(1)
				.spawnAsBoss();
			removeScreamroom();
			mPlayer.stopSound(Sound.ENTITY_ELDER_GUARDIAN_DEATH);
		}

		private void removeScreamroom() {
			if (!mKilled) {
				mKilled = true;
				mWalls.forEach(Entity::remove);
				mWalls.clear();
				mScreamroomCore.remove();
				mScreamroomWall.remove();
				mDisplay.remove();

				TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, Material.BLACK_CONCRETE);

				mPlayer.sendMessage(Component.text("The walls of the illusion fade.", NamedTextColor.GRAY, TextDecoration.ITALIC));
				mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 0.7f, 0.1f);
				if (!mSoulCaptureTask.isCancelled()) {
					mSoulCaptureTask.cancel();
				}
			}
		}
	}

	@SuppressWarnings("ReturnValueIgnored")
	@Override
	public void run() {
		List<Player> players = IntruderBoss.playersInRange(mBoss.getLocation());
		mSpellCooldownManager.setOnCooldown();
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);

		mLucidRends.clear();
		mLucidRends.addAll(EntityUtils.getNearbyMobs(mBoss.getLocation(), IntruderBoss.DETECTION_RANGE, IntruderBoss.DETECTION_RANGE, IntruderBoss.DETECTION_RANGE,
			entity -> entity.getScoreboardTags().contains("LucidRend")));
		mLucidRends.forEach(entity -> {
			entity.addScoreboardTag(LucidRendBoss.DISABLE_TAG);
			EffectManager.getInstance().addEffect(entity, "Screamroom", new PercentSpeed(CHARGE_TIME + DURATION, -1.0, "Screamroom"));
		});

		mDialogue.dialogue(2 * 20, List.of("YOU WILL KNOW. ISOLATION.", "YOUR REGRETS. WILL HAUNT YOU."));
		players.forEach(player -> {
			player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 3f, 1f, 20);
			player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, CHARGE_TIME, 1));
		});
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 5.0f, 0.7f);
		// To play at double loudness
		mWorld.playSound(mBoss.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, SoundCategory.HOSTILE, 5.0f, 2.0f, 7);
		mWorld.playSound(mBoss.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, SoundCategory.HOSTILE, 5.0f, 2.0f, 7);

		mChargeUpManager.setTime(0);
		mChargeUpManager.setChargeTime(CHARGE_TIME);
		mChargeUpManager.setColor(BossBar.Color.PURPLE);
		mChargeUpManager.setTitle(Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
		mActiveTasks.add(new BukkitRunnable() {
			boolean mStarted = false;

			@Override
			public void run() {
				if (!mStarted) {
					if (mChargeUpManager.nextTick()) {
						mChargeUpManager.setTime(DURATION);
						mChargeUpManager.setChargeTime(DURATION);
						mChargeUpManager.setColor(BossBar.Color.RED);
						mChargeUpManager.setTitle(Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
						mChargeUpManager.update();
						players.removeIf(player -> player.getScoreboardTags().contains(IntruderBoss.DEAD_TAG));

						players.forEach(player -> player.setVelocity(new Vector()));
						mStarted = true;

						mScreamrooms.clear();
						players.forEach(player -> {
							mScreamrooms.add(new PlayerScreamroom(player));
						});
					} else {
						mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 0.3f, 0.5f + 1.5f * mChargeUpManager.getChargeTime() / CHARGE_TIME, 7);
						if (mChargeUpManager.getTime() == CHARGE_TIME - 20) {
							players.removeIf(player -> player.getScoreboardTags().contains(IntruderBoss.DEAD_TAG));
							players.forEach(player -> EffectManager.getInstance().addEffect(player, "ScreamroomRoot", new PercentSpeed(20, -1.0, "ScreamroomRoot")));
						} else if (mChargeUpManager.getTime() == CHARGE_TIME - 10) {
							players.removeIf(player -> player.getScoreboardTags().contains(IntruderBoss.DEAD_TAG));
							players.stream().reduce((player1, player2) -> {
								if (isOverlapping(player1, player2)) {
									MovementUtils.knockAway(player2.getLocation(), player1, 2, 0.08f, false);
									MovementUtils.knockAway(player1.getLocation(), player2, 2, 0.08f, false);
								}
								return player2;
							});
						}
						double progress = (double) mChargeUpManager.getTime() / CHARGE_TIME;
						if (mChargeUpManager.getTime() % 10 == 0) {
							players.removeIf(player -> player.getScoreboardTags().contains(IntruderBoss.DEAD_TAG));

							players.forEach(player -> {
								double size = SIZE + 1 - progress;
								boolean overlapped = players.stream().anyMatch(otherPlayer -> otherPlayer != player && isOverlapping(otherPlayer, player));
								ParticleUtils.drawRectangleTelegraph(player.getLocation().subtract(size / 2, 0, size / 2), size, size, 8, 1, 1, SIZE * 0.04 * progress,
									overlapped ? Particle.FLAME : Particle.SOUL_FIRE_FLAME,
									mPlugin, mBoss);
								if (overlapped) {
									player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.0f, 0.5f);
									player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.3f, 0.1f);
								}
							});
						}
					}
				} else {
					if (mChargeUpManager.previousTick()) {
						endScreamroom();
						this.cancel();
					} else {
						if (mChargeUpManager.getTime() % 20 == 0) {
							players.forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.HOSTILE, 2.2f, 1.5f * (mChargeUpManager.getTime() + 1) / mChargeUpManager.getChargeTime()));
						}
						players.forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.HOSTILE, 0.6f, 1.5f * (mChargeUpManager.getTime() + 1) / mChargeUpManager.getChargeTime()));
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private static boolean isOverlapping(Player player1, Player player2) {
		return BoundingBox.of(player1.getLocation(), SIZE, SIZE, SIZE).overlaps(BoundingBox.of(player2.getLocation(), SIZE, SIZE, SIZE));
	}

	private void endScreamroom() {
		mScreamrooms.forEach(playerScreamroom -> {
			if (!playerScreamroom.mKilled) {
				Player player = playerScreamroom.mPlayer;
				player.sendMessage(Component.text("The effects of the barrier distort your soul...", NamedTextColor.GRAY, TextDecoration.ITALIC));
				double previousPower = 0;
				if (EffectManager.getInstance().hasEffect(player, DEBUFF_ID)) {
					previousPower = Objects.requireNonNull(EffectManager.getInstance().getEffects(player, DEBUFF_ID)).stream()
						.filter(effect -> effect instanceof SingleArgumentEffect)
						.reduce(0.0, (d, effect) -> d + effect.getMagnitude(), Double::sum);
				}
				EffectManager.getInstance().clearEffects(player, DEBUFF_ID);
				EffectManager.getInstance().addEffect(player, DEBUFF_ID, new PercentHealthBoost(60 * 60 * 20, -.10 - previousPower, DEBUFF_ID));
				mOnDistort.accept(player);
				playerScreamroom.removeScreamroom();
			}
		});
		finishSpell();
	}

	public void bossEntityDeathEvent(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		mScreamrooms.forEach(playerScreamroom -> {
			if (entity == playerScreamroom.mScreamroomCore) {
				playerScreamroom.onDeath();
			}
		});
		if (!mScreamrooms.isEmpty() && mScreamrooms.stream().allMatch(playerScreamroom -> playerScreamroom.mKilled)) {
			cancel();
		}
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getPlayer();
		mScreamrooms.forEach(playerScreamroom -> {
			if (player == playerScreamroom.mPlayer) {
				playerScreamroom.removeScreamroom();
			}
		});
	}

	@Override
	public void cancel() {
		if (isRunning()) {
			finishSpell();
		}
		super.cancel();
	}

	private void finishSpell() {
		mLucidRends.forEach(entity -> {
			entity.removeScoreboardTag(LucidRendBoss.DISABLE_TAG);
			EffectManager.getInstance().clearEffects(entity, "Screamroom");
		});
		mScreamrooms.clear();
		mChargeUpManager.remove();
		mBoss.setInvulnerable(false);
		mBoss.setAI(true);
	}

	// Jank to stop the edge case where this spell cancells because of phase change

	@Override
	public int cooldownTicks() {
		return 3 * 20 + DURATION;
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown() && !screamroomCannotCast();
	}

	private boolean screamroomCannotCast() {
		return mBoss.getWorld().getNearbyEntities(mBoss.getLocation(), IntruderBoss.DETECTION_RANGE, IntruderBoss.DETECTION_RANGE, IntruderBoss.DETECTION_RANGE).stream()
			.anyMatch(entity -> {
				Set<String> tags = entity.getScoreboardTags();
				return tags.contains(TwistedMiniBoss.identityTag)
					|| tags.contains(AbhorrentHallucinationBoss.TAG)
					|| tags.contains(SpellPsychicMiasma.TAG)
					|| tags.contains(SpellCerebralOnslaught.SPAWN_TAG);
			});
	}
}
