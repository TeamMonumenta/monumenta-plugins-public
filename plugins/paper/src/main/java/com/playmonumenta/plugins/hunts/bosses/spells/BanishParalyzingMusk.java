package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.UamielPetrification;
import com.playmonumenta.plugins.hunts.bosses.Quarry;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BanishParalyzingMusk extends Spell {

	private static final int CAST_DURATION = 5 * 20;
	private static final int DISPEL_DURATION = 4 * 20;

	private static final int RADIUS = 35;

	private static final String SOURCE = "ParalyzingMuskEffect";
	private static final String SLOW_ATTR_TAG = "UamielBanishSlow";

	private final Plugin mPlugin;
	private final com.playmonumenta.plugins.Plugin mMonumentaPlugin;
	private final LivingEntity mBoss;
	private final Uamiel mUamiel;
	private final World mWorld;

	public BanishParalyzingMusk(Plugin plugin, LivingEntity boss, Uamiel uamiel) {
		mPlugin = plugin;
		mBoss = boss;
		mUamiel = uamiel;
		mWorld = boss.getWorld();

		mMonumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();
	}

	@Override
	public void run() {
		mUamiel.ranBanished();

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, Uamiel.MOVEMENT_SPEED / 6);

		List<Player> players = new ArrayList<>(mUamiel.getPlayers());
		for (Player player : players) {
			player.sendMessage(Component.text("Hazardous spores begin to settle on your skin, slowing you.", Uamiel.TEXT_COLOR));
			mMonumentaPlugin.mEffectManager.addEffect(player, SOURCE, new UamielPetrification(CAST_DURATION + DISPEL_DURATION + 1));
		}

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.HOSTILE, 5f, 0.7f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 4f, 0.55f);

		ChargeUpManager chargeUp = new ChargeUpManager(mBoss, CAST_DURATION + DISPEL_DURATION, Component.text("Releasing ", TextColor.color(30, 87, 29)).append(Component.text(String.format("Paralyzing Musk (%s)", Quarry.BANISH_CHARACTER), TextColor.color(77, 153, 77))), BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, RADIUS);

		new PPExplosion(Particle.SPIT, mBoss.getBoundingBox().getCenter().toLocation(mWorld))
			.count(80)
			.extra(2)
			.spawnAsBoss();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Map<Player, Location> mLockPlayerLocations = new HashMap<>();

			@Override
			public void run() {
				if (mTicks % 5 == 0) {
					for (Player player : players) {
						mMonumentaPlugin.mEffectManager.addEffect(player, SLOW_ATTR_TAG, new PercentSpeed(CAST_DURATION - mTicks + DISPEL_DURATION, -((double) mTicks / CAST_DURATION), SLOW_ATTR_TAG));
					}
				}

				// check if players have CurePetrify
				List<Player> toRemovePlayers = new ArrayList<>();
				for (Player player : players) {
					if (!mMonumentaPlugin.mEffectManager.hasEffect(player, SOURCE)) {
						player.sendMessage(Component.text("Feeling freshly invigorated, the spores melt away, curing you.", Uamiel.TEXT_COLOR));
						mWorld.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 0.65f, 0.8f);
						toRemovePlayers.add(player);

						mMonumentaPlugin.mEffectManager.clearEffects(player, SLOW_ATTR_TAG);
						player.removePotionEffect(PotionEffectType.BLINDNESS);
						AbilityUtils.unsilencePlayer(player);
						chargeUp.excludePlayer(player);
					}
				}
				players.removeAll(toRemovePlayers);

				new PartialParticle(Particle.SPORE_BLOSSOM_AIR, mBoss.getLocation().clone().add(0, 3, 0))
					.count(50)
					.delta(10, 1.5, 10)
					.spawnAsBoss();

				if (mTicks == CAST_DURATION) {
					for (Player player : players) {
						player.sendMessage(Component.text("The spores solidify on your skin, paralyzing you.", Uamiel.TEXT_COLOR));
						AbilityUtils.silencePlayer(player, DISPEL_DURATION);
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, DISPEL_DURATION + 20, 1));

						mLockPlayerLocations.put(player, player.getLocation());

						mWorld.playSound(player.getLocation(), Sound.BLOCK_BASALT_BREAK, SoundCategory.HOSTILE, 1.5f, 0.65f);
					}

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 3f, 0.7f);
				}
				if (mTicks > CAST_DURATION) {
					for (Player player : players) {
						player.teleport(mLockPlayerLocations.get(player));
					}
				}

				if ((mTicks < CAST_DURATION && mTicks % 16 == 0) || (mTicks > CAST_DURATION && mTicks % 8 == 0)) {
					for (Player player : players) {
						mWorld.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.0f, 1f);
					}
				}

				if (mTicks == CAST_DURATION + DISPEL_DURATION) {
					for (Player player : players) {
						player.sendMessage(Component.text("The spores, going uncured, completely paralyzed you and you have been banished.", Uamiel.TEXT_COLOR));
						mUamiel.banish(player);
					}
				}

				chargeUp.nextTick();

				mTicks++;
				if (mTicks > CAST_DURATION + DISPEL_DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				for (Player player : players) {
					mMonumentaPlugin.mEffectManager.clearEffects(player, SLOW_ATTR_TAG);
					player.removePotionEffect(PotionEffectType.BLINDNESS);
					AbilityUtils.unsilencePlayer(player);
				}

				EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, Uamiel.MOVEMENT_SPEED);
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return CAST_DURATION + DISPEL_DURATION + 20;
	}

	@Override
	public boolean persistOnPhaseChange() {
		return true;
	}
}
