package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.hunts.bosses.AlocAcoc;
import com.playmonumenta.plugins.hunts.bosses.Quarry;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PassivePolarAura extends Spell {

	//doubled for outer ring
	public static final float INNER_RADIUS = 9f;
	public static final float OUTER_RADIUS = 15f;
	private static final int SWAP_PERIOD = 10 * 20;
	private static final int SWAP_DURATION = 30;

	private static final String SLOWNESS_TAG = "AlocAcocAuraSlowness";
	private static final double MAX_SLOWNESS = 0.4;

	private static final Component NORMAL_BOSSBAR = Component.text("Frostbite", TextColor.color(87, 165, 201));
	private static final Component FROZEN_BOSSBAR = Component.text(Quarry.BANISH_CHARACTER, NamedTextColor.WHITE)
		.append(Component.text(" FREEZING ", TextColor.color(87, 165, 201)))
		.append(Component.text(Quarry.BANISH_CHARACTER, NamedTextColor.WHITE));
	private static final Component HOT_BOSSBAR = Component.text("!", NamedTextColor.WHITE)
		.append(Component.text(" BURNING ", TextColor.color(87, 165, 201)))
		.append(Component.text("!", NamedTextColor.WHITE));

	public static final Particle.DustOptions LIGHT_BLUE = new Particle.DustOptions(Color.fromRGB(137, 207, 240), 1.5f);

	public boolean mInnerAura;

	private final Plugin mMonumentaPlugin;
	private final LivingEntity mBoss;
	private final AlocAcoc mAlocAcoc;

	private final Map<Player, Float> mFrostbiteValues = new HashMap<>();
	private final Map<Player, BossBar> mFrostbiteBossBars = new HashMap<>();

	private Collection<Player> mNearbyPlayers = Collections.emptyList();

	public double mCurrentRadius;

	private int mTicks = 0;

	public PassivePolarAura(LivingEntity boss, AlocAcoc alocAcoc) {
		mMonumentaPlugin = Plugin.getInstance();
		mBoss = boss;
		mAlocAcoc = alocAcoc;
		mInnerAura = false;
		mCurrentRadius = OUTER_RADIUS;

		Bukkit.getScheduler().runTaskLater(mMonumentaPlugin, () -> PlayerUtils.playersInRange(mBoss.getLocation(), AlocAcoc.OUTER_RADIUS, true).forEach(this::createFrostbiteBar), 0);
	}

	@Override
	public void run() {
		if (mTicks % 5 == 0) {
			spawnIndicatorRing(mBoss.getLocation(), mCurrentRadius, LIGHT_BLUE);
			if (mTicks % 4 == 0) {
				spawnRegionRing(mBoss.getLocation(), mCurrentRadius);
			}
		}

		if (mTicks % SWAP_PERIOD == 0 && mTicks > 0) {
			swap();
		}

		if (mTicks % 20 == 0) {
			mNearbyPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), AlocAcoc.OUTER_RADIUS, true);
		}

		updateFrostbiteBars(mNearbyPlayers);
		doAuraEffect(mNearbyPlayers);

		mTicks++;
	}

	private void swap() {
		double start;
		double end;
		if (mInnerAura) {
			start = INNER_RADIUS;
			end = OUTER_RADIUS;
		} else {
			start = OUTER_RADIUS;
			end = INNER_RADIUS;
		}

		new BukkitRunnable() {
			int mSwapTicks = 0;

			@Override
			public void run() {
				double progress = (double) mSwapTicks / SWAP_DURATION;
				mCurrentRadius = start + (end - start) * progress; // lerp between start and end

				mSwapTicks++;
				if (mSwapTicks > SWAP_DURATION || mBoss.isDead()) {
					mInnerAura = !mInnerAura;
					this.cancel();
				}
			}
		}.runTaskTimer(mMonumentaPlugin, 0, 1);
	}

	public int timeUntilFinishedNextSwap() {
		return SWAP_PERIOD + SWAP_DURATION - (mTicks % (SWAP_PERIOD + SWAP_DURATION));
	}

	// manage applying frostbite
	private void doAuraEffect(Collection<Player> players) {
		Collection<Player> playersInAura = getPlayersInAura();
		for (Player player : playersInAura) {
			addFrostbite(player, mAlocAcoc.isWarmingItem(player.getInventory().getItemInMainHand()) ? 0.004f : 0.008f);
		}

		Collection<Player> outsidePlayers = new ArrayList<>(players);
		outsidePlayers.removeAll(playersInAura);
		int spirits = EntityUtils.getNearbyMobs(mBoss.getLocation(), AlocAcoc.OUTER_RADIUS, EnumSet.of(EntityType.STRAY)).size();
		for (Player player : outsidePlayers) {
			addFrostbite(player, mAlocAcoc.isWarmingItem(player.getInventory().getItemInMainHand()) ? -0.01f : 0.001f * (2 - 1f / (1 + spirits)));
		}

		Set<Player> playersToForget = new HashSet<>(mFrostbiteBossBars.keySet());
		playersToForget.removeAll(players);
		for (Player player : playersToForget) {
			clearPlayerFrostbiteBar(player);
			mFrostbiteValues.remove(player);
			mFrostbiteBossBars.remove(player);
		}
	}

	public void addFrostbite(Player player, float amount) {
		if (!mFrostbiteValues.containsKey(player) || !mFrostbiteBossBars.containsKey(player)) {
			return;
		}

		float newAmount = Math.max(-1, Math.min(1, mFrostbiteValues.get(player) + amount));
		mFrostbiteValues.put(player, newAmount);

		player.setFreezeTicks((int) (player.getMaxFreezeTicks() * Math.max(newAmount, 0) * 0.5));
		BossBar bar = mFrostbiteBossBars.get(player);
		bar.progress(0.5f * (1 + newAmount)); // -1 -> 0, 0 -> 1/2, 1 -> 1
		bar.name(newAmount <= -0.75 ? HOT_BOSSBAR : (newAmount >= 0.75 ? FROZEN_BOSSBAR : NORMAL_BOSSBAR));

		if (newAmount == 1) {
			// Freeze the player
			freeze(player);
		} else if (newAmount == -1) {
			if (mAlocAcoc.spoil(player)) {
				player.sendMessage(Component.text("Your heat got too high, singing Aloc Acoc's fur and spoiling your loot.", NamedTextColor.RED));
				player.playSound(player, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1, 1);
			}
		} else if (newAmount > 0) {
			mMonumentaPlugin.mEffectManager.addEffect(player, SLOWNESS_TAG, new PercentSpeed(20, -(float) (MAX_SLOWNESS * newAmount), SLOWNESS_TAG));
		}
	}

	// creates or removes bossbars if needed depending on whether the player is new or has left
	private void updateFrostbiteBars(Collection<Player> players) {
		for (Player player : players) {
			createFrostbiteBar(player);
		}
		for (Player player : new HashSet<>(mFrostbiteBossBars.keySet())) {
			if (!players.contains(player)) {
				clearPlayerFrostbiteBar(player);
			}
		}
	}

	private void createFrostbiteBar(Player player) {
		if (mFrostbiteBossBars.containsKey(player)) {
			return;
		}

		mFrostbiteValues.put(player, 0f);

		BossBar bar = BossBar.bossBar(NORMAL_BOSSBAR, 0, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
		player.showBossBar(bar);
		mFrostbiteBossBars.put(player, bar);
	}

	// remove all players' bossbars
	public void clearFrostbiteBars() {
		new HashSet<>(mFrostbiteBossBars.keySet()).forEach(this::clearPlayerFrostbiteBar);
	}

	// remove a specific player's bossbar
	public void clearPlayerFrostbiteBar(Player player) {
		player.hideBossBar(mFrostbiteBossBars.get(player));
		mFrostbiteBossBars.remove(player);
		mMonumentaPlugin.mEffectManager.clearEffects(player, SLOWNESS_TAG);
	}

	private void freeze(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_SNOW_STEP, SoundCategory.HOSTILE, 1f, 0.8f);
		player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1f, 0.65f);
		player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 1f, 0.5f);
		player.playSound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1, 0.5f);
		player.stopSound(Sound.ITEM_ELYTRA_FLYING, SoundCategory.HOSTILE);

		new PPCircle(Particle.SNOWFLAKE, LocationUtils.getHalfHeightLocation(player), 1)
			.directionalMode(true)
			.rotateDelta(true)
			.delta(1, 0, 0)
			.extra(0.15)
			.countPerMeter(8)
			.spawnAsBoss();

		mAlocAcoc.banish(player);
		clearPlayerFrostbiteBar(player);
		player.sendMessage(Component.text("Before you are encased in ice, you retreat to the lodge.", AlocAcoc.COLOR));
	}

	// returns all players outside the ring of the aura
	public Collection<Player> getPlayersInAura() {
		Collection<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), AlocAcoc.OUTER_RADIUS, true);
		players.removeIf(player -> player.getLocation().distanceSquared(mBoss.getLocation()) < mCurrentRadius * mCurrentRadius);
		return players;
	}

	// ring of stationary dust particles
	public void spawnIndicatorRing(Location center, double radius, Particle.DustOptions data) {
		new PPParametric(Particle.REDSTONE, center,
			(t, builder) -> {
				Location finalLocation = LocationUtils.fallToGround(LocationUtils.mapToGround(center.clone().add(radius * FastUtils.cosDeg(t * 360), 0, radius * FastUtils.sinDeg(t * 360)), 5), center.getY() - 5);
				builder.location(finalLocation);
			})
			.count((int) (radius * (float) Math.PI * 3))
			.data(data)
			.spawnAsBoss();
	}

	// ring of cloud particles which moves outwards
	public void spawnRegionRing(Location center, double radius) {
		new PPParametric(Particle.CLOUD, center,
			(t, builder) -> {
				Location finalLocation = LocationUtils.fallToGround(LocationUtils.mapToGround(center.clone().add(radius * FastUtils.cosDeg(t * 360), 0, radius * FastUtils.sinDeg(t * 360)), 5), center.getY() - 5);
				builder.location(finalLocation.clone().add(0, 0.15, 0));
				builder.offset(FastUtils.cosDeg(t * 360), 0, FastUtils.sinDeg(t * 360));
			})
			.count((int) (radius * (float) Math.PI * 3))
			.directionalMode(true)
			.extra(0.3)
			.spawnAsBoss();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
