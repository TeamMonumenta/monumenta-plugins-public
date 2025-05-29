package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.SingleArgumentEffect;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Vibration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellCognitiveDistortion extends Spell {
	private static final String SPELL_NAME = "Cognitive Distortion";
	public static final String WEAKNESS_SOURCE = "DistortedDamage";
	public static final String ANTI_HEAL_SOURCE = "DistortedHeal";
	public static final String SLOWNESS_SOURCE = "DistortedSpeed";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private List<Player> mPlayers;
	private final Location mCenterLoc;
	private final Location mShiftedLoc;

	private final Map<Player, PotionEffect> mPreviousGlowingEffect = new HashMap<>();
	private final Map<Player, Location> mPlayerPreviousLocation = new HashMap<>();

	private final IntruderBoss.Dialogue mDialogue;
	private final IntruderBoss.Narration mNarration;
	private final ChargeUpManager mChargeUpManager;

	public static final int Y_OFFSET = 50;
	public static final double SPREAD_RANGE = 10;
	public static final int CAST_TIME = 10 * 20;
	public static final int DEBUFF_DURATION = 60 * 60 * 20;
	public static final double CIRCLE_RADIUS = 3;

	public SpellCognitiveDistortion(Plugin plugin, LivingEntity boss, List<Player> players, Location centerLocation, IntruderBoss.Dialogue dialogue, IntruderBoss.Narration narration) {
		mPlugin = plugin;
		mBoss = boss;
		mPlayers = List.copyOf(players);
		mCenterLoc = centerLocation;
		mShiftedLoc = mCenterLoc.clone().subtract(0, Y_OFFSET, 0);
		mDialogue = dialogue;
		mNarration = narration;
		mChargeUpManager = new ChargeUpManager(mBoss, 20, Component.text("Channeling ", NamedTextColor.GRAY).append(Component.text(SPELL_NAME, NamedTextColor.DARK_GRAY)), BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS, IntruderBoss.DETECTION_RANGE);
	}

	@Override
	public void run() {
		mPlayers = IntruderBoss.playersInRange(mBoss.getLocation());
		mChargeUpManager.setTime(0);
		mChargeUpManager.setChargeTime(20);
		mChargeUpManager.setColor(BossBar.Color.PURPLE);
		mChargeUpManager.setTitle(Component.text("Channeling ", NamedTextColor.GRAY).append(Component.text(SPELL_NAME, NamedTextColor.DARK_GRAY)));
		mDialogue.dialogue("ALONE. IN THE DARK. YOUR MIND WILL SUCCUMB.");

		mPlayers.forEach(player -> {
			if (player.hasPotionEffect(PotionEffectType.GLOWING)) {
				mPreviousGlowingEffect.put(player, player.getPotionEffect(PotionEffectType.GLOWING));
			}
			mPlayerPreviousLocation.put(player, player.getLocation());
			player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60 * 20, 2, true, false));
			player.removePotionEffect(PotionEffectType.GLOWING);

			player.playSound(player.getLocation(), Sound.AMBIENT_CRIMSON_FOREST_LOOP, SoundCategory.HOSTILE, 3f, 2.0f);
			player.playSound(player.getLocation(), Sound.AMBIENT_WARPED_FOREST_LOOP, SoundCategory.HOSTILE, 3f, 1.5f);
			player.playSound(player.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.HOSTILE, 3f, 1.5f);
		});
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		EntityUtils.selfRoot(mBoss, 20);

		boolean solo = mPlayers.size() == 1;

		mActiveTasks.add(new BukkitRunnable() {
			boolean mCasted = false;
			int mTogetherTicks = 0;

			@Override
			public void run() {
				if (!mCasted) {
					if (mChargeUpManager.nextTick()) {
						for (int d = 0; d < 360; d += 72) {
							for (int r = 1; r <= 3; r++) {
								LivingEntity summon = (LivingEntity) Objects.requireNonNull(LibraryOfSoulsIntegration.summon(mShiftedLoc.clone().add(VectorUtils.rotateYAxis(new Vector(r * 8, 0, 0), d)), "LiminalShadow"));
								if (!solo) {
									EffectManager.getInstance().addEffect(summon, "CognitiveDistortionAlone", new PercentSpeed(15 * 20, 0.2, "CognitiveDistortionAlone"));
								}
							}
						}

						mChargeUpManager.setTime(CAST_TIME);
						mChargeUpManager.setChargeTime(CAST_TIME);
						mChargeUpManager.setColor(BossBar.Color.WHITE);
						mChargeUpManager.setTitle(Component.text("Casting ", NamedTextColor.GRAY).append(Component.text(SPELL_NAME, NamedTextColor.DARK_GRAY)));

						spreadPlayers();
						mNarration.narration("Reality morphs around you... What is this place?");
						mBoss.teleport(mCenterLoc);
						mCasted = true;
					} else {
						float progress = (float) mChargeUpManager.getTime() / mChargeUpManager.getChargeTime();
						mPlayers.forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.4f, 0.1f + progress));
						mPlayers.forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 0.75f, 0.1f + progress));
					}
				} else {
					if (mChargeUpManager.previousTick()) {
						failMechanic();
						this.cancel();
					} else {
						if (mChargeUpManager.getTime() % 10 == 0) {
							boolean increased = solo ? tickSolo() : tick();
							if (increased) {
								mTogetherTicks++;
							}
							if (mTogetherTicks > 0) {
								if ((solo && mTogetherTicks > 6) ||
									(!solo && (mTogetherTicks > 5 || mChargeUpManager.getTime() == 1))) {
									finishSpell();
									this.cancel();
								} else if (increased) {
									mPlayers.forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.5f + mTogetherTicks / 3.0f, 2.0f));
								}
							}
						}
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private void failMechanic() {
		mDialogue.dialogue("NO ONE WILL BE THERE. TO SAVE YOU FROM THE DARK.");
		mNarration.narration("The cold and dark pierce your very being...");
		IntruderBoss.playersInRange(mBoss.getLocation()).forEach(player -> {
			double previousPower = 0;
			// Use weakness source as a basis for additional debuff
			if (EffectManager.getInstance().hasEffect(player, WEAKNESS_SOURCE)) {
				previousPower = Objects.requireNonNull(EffectManager.getInstance().getEffects(player, WEAKNESS_SOURCE)).stream()
					.filter(effect -> effect instanceof SingleArgumentEffect)
					.reduce(0.0, (d, effect) -> d + effect.getMagnitude(), Double::sum);
			}

			EffectManager.getInstance().clearEffects(player, WEAKNESS_SOURCE);
			EffectManager.getInstance().clearEffects(player, ANTI_HEAL_SOURCE);
			EffectManager.getInstance().clearEffects(player, SLOWNESS_SOURCE);

			EffectManager.getInstance().addEffect(player, WEAKNESS_SOURCE, new PercentDamageDealt(DEBUFF_DURATION, -0.1 - previousPower).deleteOnLogout(true));
			EffectManager.getInstance().addEffect(player, ANTI_HEAL_SOURCE, new PercentHeal(DEBUFF_DURATION, -0.1 - previousPower).deleteOnLogout(true));
			EffectManager.getInstance().addEffect(player, SLOWNESS_SOURCE, new PercentSpeed(DEBUFF_DURATION, -0.1 - previousPower, SLOWNESS_SOURCE).deleteOnLogout(true));
		});
		finishSpell();
	}

	private void spreadPlayers() {
		Vector offset = VectorUtils.randomHorizontalUnitVector().multiply(FastUtils.randomDoubleInRange(SPREAD_RANGE / 2, SPREAD_RANGE));
		for (Player player : mPlayers) {
			player.teleport(mShiftedLoc.clone().add(offset));
			offset.rotateAroundY(FastUtils.randomDoubleInRange(Math.PI / 2, 3 * Math.PI / 2));
			player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 0.6f, 1.1f);
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		if (isRunning()) {
			finishSpell();
		}
	}

	private boolean tickSolo() {
		Player player = mPlayers.get(0);
		boolean together = player.getLocation().distance(mShiftedLoc) <= CIRCLE_RADIUS;

		new PPCircle(Particle.SOUL_FIRE_FLAME, mShiftedLoc.clone().add(0, 0.15, 0), CIRCLE_RADIUS)
			.count(together ? 12 : 6)
			.delta(0.2)
			.spawnAsBoss();

		new PPPillar(Particle.SOUL_FIRE_FLAME, mShiftedLoc, 5)
			.count(40)
			.delta(0.1)
			.spawnAsBoss();

		Location loc = player.getLocation();
		new PPCircle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.15, 0), CIRCLE_RADIUS / 2)
			.count(together ? 12 : 5)
			.delta(CIRCLE_RADIUS / 2, 0, CIRCLE_RADIUS / 2)
			.spawnAsBoss();

		if (together) {
			player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 0.7f, 1.2f);
			player.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.8f, 1f);
			player.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, 0.7f, 1.2f);
			new PPCircle(Particle.TOTEM, loc.clone().add(0, 0.15, 0), 0.1)
				.count(40)
				.rotateDelta(true).directionalMode(true)
				.delta(0.4, 0, 0)
				.extraRange(0.1, CIRCLE_RADIUS)
				.spawnAsBoss();
			player.showTitle(Title.title(Component.empty(), Component.text("TOGETHER", NamedTextColor.WHITE), Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(100))));
		} else {
			for (int i = 0; i < 5; i++) {
				player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.3f, 0.1f);
			}
			if (FastUtils.RANDOM.nextBoolean()) {
				player.playSound(player.getLocation().add(VectorUtils.randomUnitVector().multiply(3)), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 2f, 0.1f);
			}

			Vector vecToCenter = LocationUtils.getDirectionTo(mShiftedLoc, player.getLocation());

			new PPLine(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 0.9, 0), vecToCenter, 3)
				.countPerMeter(2)
				.distanceFalloff(3)
				.delta(0.2)
				.spawnAsBoss();

			new PartialParticle(Particle.VIBRATION, player.getLocation())
				.data(new Vibration(new Vibration.Destination.BlockDestination(mShiftedLoc), 10))
				.spawnAsBoss();
			player.showTitle(Title.title(Component.empty(), Component.text("ALONE", NamedTextColor.GRAY), Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(100))));
		}
		return together;
	}

	private boolean tick() {
		boolean together = checkTogether();

		List.copyOf(mPlayers).forEach(player -> {
			Location loc = player.getLocation();
			new PPCircle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.15, 0), CIRCLE_RADIUS / 2)
				.count(together ? 12 : 5)
				.delta(CIRCLE_RADIUS / 2, 0, CIRCLE_RADIUS / 2)
				.spawnAsBoss();

			if (together) {
				player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 0.7f, 1.2f);
				player.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.8f, 1f);
				player.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, 0.7f, 1.2f);
				new PPCircle(Particle.TOTEM, loc.clone().add(0, 0.15, 0), 0.1)
					.count(40)
					.rotateDelta(true).directionalMode(true)
					.delta(0.4, 0, 0)
					.extraRange(0.1, CIRCLE_RADIUS)
					.spawnAsBoss();
				player.showTitle(Title.title(Component.empty(), Component.text("TOGETHER", NamedTextColor.WHITE), Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(100))));
			} else {
				for (int i = 0; i < 5; i++) {
					player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.3f, 0.1f);
				}
				if (FastUtils.RANDOM.nextBoolean()) {
					player.playSound(player.getLocation().add(VectorUtils.randomUnitVector().multiply(3)), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 2f, 0.1f);
				}
				player.showTitle(Title.title(Component.empty(), Component.text("ALONE", NamedTextColor.GRAY), Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(100))));
			}
			List.copyOf(mPlayers).stream().filter(playerOther -> playerOther != player).forEach(playerOther -> {
				player.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 3.0f, 1.0f);

				Vector vecToPlayer = LocationUtils.getDirectionTo(playerOther.getLocation(), player.getLocation());

				new PPLine(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 0.9, 0), vecToPlayer, 3)
					.countPerMeter(2)
					.distanceFalloff(3)
					.delta(0.2)
					.spawnForPlayer(ParticleCategory.BOSS, player);

				new PartialParticle(Particle.VIBRATION, player.getLocation())
					.data(new Vibration(new Vibration.Destination.EntityDestination(playerOther), 10))
					.spawnForPlayer(ParticleCategory.BOSS, player);
			});
		});
		return together;
	}

	private boolean checkTogether() {
		List<Player> players = List.copyOf(mPlayers);
		return players.size() > 1 && players.stream().allMatch(player -> player.getLocation().distance(mPlayers.get(0).getLocation()) <= 2 * CIRCLE_RADIUS);
	}

	private void finishSpell() {
		mBoss.setInvulnerable(false);
		List.copyOf(mPlayers).forEach(player -> {
			player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.7f, 0.1f);
			player.stopSound(Sound.AMBIENT_CRIMSON_FOREST_LOOP);
			player.stopSound(Sound.AMBIENT_WARPED_FOREST_LOOP);
			player.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
			player.teleport(mPlayerPreviousLocation.get(player));
			player.removePotionEffect(PotionEffectType.DARKNESS);
		});
		mChargeUpManager.remove();
		mPreviousGlowingEffect.forEach(LivingEntity::addPotionEffect);
		mPreviousGlowingEffect.clear();
		mPlayerPreviousLocation.clear();
		EntityUtils.getNearbyMobs(mCenterLoc.clone().subtract(0, Y_OFFSET, 0), 25, 20, 25).forEach(Entity::remove);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.setAI(true), 20);
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		LivingEntity player = event.getPlayer();
		if (isRunning() && mPlayers.contains(player)) {
			event.setCancelled(true);
			mPlayers.remove(player);
			player.setHealth(EntityUtils.getMaxHealth(player));
			player.teleport(mPlayerPreviousLocation.get(player));
			if (mPlayers.isEmpty()) {
				failMechanic();
				cancel();
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return CAST_TIME + 4 * 20;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
