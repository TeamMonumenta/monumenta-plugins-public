package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSoulLink extends Spell {
	private static final int PERSONAL_COOLDOWN = 90 * 20;
	private static final int DURATION = 45 * 20;
	private static final int FORECAST_TICKS = 3 * 20 + 1;

	private static final double DISTANCE_THRESHOLD = 8;
	private static final double TOO_FAR_THRESHOLD_ASCENSION_15 = 5 * 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;
	private final double mTooFarThreshold;

	private static final List<Player> mWarnedPlayers = new ArrayList<>();

	private boolean mOnCooldown = false;

	public SpellSoulLink(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;

		mTooFarThreshold = TOO_FAR_THRESHOLD_ASCENSION_15;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mOnCooldown = false;
		}, cooldownTicks() + PERSONAL_COOLDOWN);

		mWarnedPlayers.clear();

		List<Player> playerLists = PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true);
		Collections.shuffle(playerLists);

		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VEX_AMBIENT, 3, 0.5f);

		for (int i = 0; i < playerLists.size(); i += 2) {
			Player player1 = playerLists.get(i);

			if (playerLists.size() > i + 1) {
				Player player2 = playerLists.get(i + 1);
				soulLink(player1, player2);
			} else {
				soulLinkWithBoss(player1);
			}
		}
	}

	public void soulLink(Player player1, Player player2) {
		player1.playSound(player1.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1, 1.45f);
		player2.playSound(player2.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1, 1.45f);
		MessagingUtils.sendTitle(player1, Component.text("Stay Close!", NamedTextColor.RED, TextDecoration.BOLD), Component.text("Soul Linked with ", NamedTextColor.YELLOW).append(Component.text(player2.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED)), 0, 5*20, 10);
		MessagingUtils.sendTitle(player2, Component.text("Stay Close!", NamedTextColor.RED, TextDecoration.BOLD), Component.text("Soul Linked with ", NamedTextColor.YELLOW).append(Component.text(player1.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED)), 0, 5*20, 10);

		BukkitRunnable soulLinkRunnable = new BukkitRunnable() {
			int mSoulLinkTicks = -FORECAST_TICKS;
			int mTooFarCombo = 0;

			@Override
			public void run() {
				if (mSoulLinkTicks > DURATION || player1.getLocation().distance(mBoss.getLocation()) > 100 || player2.getLocation().distance(mBoss.getLocation()) > 100 || player1.isDead() || player2.isDead()) {
					player1.playSound(player1.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
					player1.playSound(player1.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1, 1f);
					player2.playSound(player2.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
					player2.playSound(player2.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1, 1f);

					// Draw tether line from Player to Boss.
					Location playerLoc = LocationUtils.getEntityCenter(player1);
					Vector dir = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(player2), playerLoc).normalize().multiply(0.5);
					Location pLoc = playerLoc.clone();
					for (int i = 0; i < 80; i++) {
						pLoc.add(dir);
						new PartialParticle(Particle.SMOKE_LARGE, pLoc, 1, 0, 1, 0)
							.extra(0.2)
							.directionalMode(true)
							.spawnForPlayer(ParticleCategory.BOSS, player1);
						new PartialParticle(Particle.SMOKE_LARGE, pLoc, 1, 0, 1, 0)
							.extra(0.2)
							.directionalMode(true)
							.spawnForPlayer(ParticleCategory.BOSS, player2);

						if (pLoc.distance(LocationUtils.getEntityCenter(player2)) < 0.5) {
							break;
						}
					}
					this.cancel();
					return;
				}

				if (mSoulLinkTicks < 0) {
					if (mSoulLinkTicks % 10 == 0) {
						player1.getWorld().playSound(player1.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 3, 0.5f);
						player2.getWorld().playSound(player2.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 3, 0.5f);
					}

					if (mSoulLinkTicks % 5 == 0) {
						// Draw tether line from Player to Boss.
						Location playerLoc = LocationUtils.getEntityCenter(player1);
						Vector dir = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(player2), playerLoc).normalize().multiply(0.5);
						Location pLoc = playerLoc.clone();
						for (int i = 0; i < 80; i++) {
							pLoc.add(dir);
							new PartialParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0)
								.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f))
								.extra(0)
								.spawnForPlayer(ParticleCategory.BOSS, player1);
							new PartialParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0)
								.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f))
								.extra(0)
								.spawnForPlayer(ParticleCategory.BOSS, player2);

							if (pLoc.distance(LocationUtils.getEntityCenter(player2)) < 0.5) {
								break;
							}
						}
					}
				}

				if (mSoulLinkTicks > 0) {
					double distance = player1.getLocation().distance(player2.getLocation());

					if (distance > DISTANCE_THRESHOLD) {
						mPlugin.mEffectManager.addEffect(player1, "SoulLinkWeaken", new PercentDamageDealt(20, -0.5));
						mPlugin.mEffectManager.addEffect(player2, "SoulLinkWeaken", new PercentDamageDealt(20, -0.5));

						if (!mWarnedPlayers.contains(player1)) {
							player1.sendMessage(Component.text("You are getting too far from the soul link! You feel your soul being stretched thinly.", NamedTextColor.AQUA));
							mWarnedPlayers.add(player1);
						}
						if (!mWarnedPlayers.contains(player2)) {
							player2.sendMessage(Component.text("You are getting too far from the soul link! You feel your soul being stretched thinly.", NamedTextColor.AQUA));
							mWarnedPlayers.add(player2);
						}

						mTooFarCombo++;

						MessagingUtils.sendTitle(player1, Component.text(""), Component.text("Soul Linked with ", NamedTextColor.YELLOW).append(Component.text(player2.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED)).append(Component.text(" (" + new DecimalFormat("#.#").format(Math.max(0.0, (mTooFarThreshold - mTooFarCombo) / 20.0)) + ")", NamedTextColor.RED, TextDecoration.BOLD)), 0, 20, 10);
						MessagingUtils.sendTitle(player2, Component.text(""), Component.text("Soul Linked with ", NamedTextColor.YELLOW).append(Component.text(player1.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED)).append(Component.text(" (" + new DecimalFormat("#.#").format(Math.max(0.0, (mTooFarThreshold - mTooFarCombo) / 20.0)) + ")", NamedTextColor.RED, TextDecoration.BOLD)), 0, 20, 10);

						if (mTooFarCombo > mTooFarThreshold) {
							if (mTooFarCombo % 5 == 0) {
								damage(player1);
								damage(player2);
							}
						} else if (mTooFarCombo % 5 == 0) {
							player1.playSound(player1.getLocation(), Sound.UI_BUTTON_CLICK, 1, 2f);
							player2.playSound(player2.getLocation(), Sound.UI_BUTTON_CLICK, 1, 2f);
						}
					} else {
						mTooFarCombo = 0;
					}

					if (mSoulLinkTicks % 20 == 0 && mTooFarCombo == 0) {
						player1.playSound(player1.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 3, 1f);
						player2.playSound(player2.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 3, 1f);
					} else if (mSoulLinkTicks % 5 == 0 && mTooFarCombo > 0) {
						player1.playSound(player1.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 3, 1.5f);
						player2.playSound(player2.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 3, 1.5f);
					}

					// Draw tether line from Player to Boss.
					Location playerLoc = LocationUtils.getEntityCenter(player1);
					Vector dir = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(player2), playerLoc).normalize().multiply(0.5);
					Location pLoc = playerLoc.clone();
					for (int i = 0; i < 80; i++) {
						pLoc.add(dir);

						if (distance <= DISTANCE_THRESHOLD) {
							new PartialParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0)
								.extra(9999999)
								.spawnForPlayer(ParticleCategory.BOSS, player1);
							new PartialParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0)
								.extra(9999999)
								.spawnForPlayer(ParticleCategory.BOSS, player2);
						} else {
							if (mTooFarCombo < mTooFarThreshold) {
								new PartialParticle(Particle.FLAME, pLoc, 1, 0, 0, 0)
									.extra(9999999)
									.spawnForPlayer(ParticleCategory.BOSS, player1);
								new PartialParticle(Particle.FLAME, pLoc, 1, 0, 0, 0)
									.extra(9999999)
									.spawnForPlayer(ParticleCategory.BOSS, player2);
							} else {
								new PartialParticle(Particle.SOUL_FIRE_FLAME, pLoc, 1, 0, 0, 0)
									.extra(9999999)
									.spawnForPlayer(ParticleCategory.BOSS, player1);
								new PartialParticle(Particle.SOUL_FIRE_FLAME, pLoc, 1, 0, 0, 0)
									.extra(9999999)
									.spawnForPlayer(ParticleCategory.BOSS, player2);
							}

							if (FastUtils.randomIntInRange(0, 20) == 0) {
								new PartialParticle(Particle.SOUL, pLoc, 1, 0, 0.5, 0)
									.extra(0.2)
									.directionalMode(true)
									.spawnForPlayer(ParticleCategory.BOSS, player1);
								new PartialParticle(Particle.SOUL, pLoc, 1, 0, 0.5, 0)
									.extra(0.2)
									.directionalMode(true)
									.spawnForPlayer(ParticleCategory.BOSS, player2);
							}
						}

						if (pLoc.distance(LocationUtils.getEntityCenter(player2)) < 0.5) {
							break;
						}
					}
				}

				mSoulLinkTicks++;
			}
		};
		soulLinkRunnable.runTaskTimer(mPlugin, 0, 1);

		mActiveRunnables.add(soulLinkRunnable);
	}

	public void soulLinkWithBoss(Player player) {
		double tooFarThreshold = mTooFarThreshold;

		player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1, 1.45f);
		MessagingUtils.sendTitle(player, Component.text("Stay Close!", NamedTextColor.RED, TextDecoration.BOLD), Component.text("Soul Linked with ", NamedTextColor.YELLOW).append(Component.text("Vesperidys", NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED)), 0, 5*20, 10);

		BukkitRunnable soulLinkRunnable = new BukkitRunnable() {
			int mSoulLinkTicks = -FORECAST_TICKS;
			int mTooFarCombo = 0;

			@Override
			public void run() {
				if (mSoulLinkTicks > DURATION || player.getLocation().distance(mBoss.getLocation()) > 100 || player.isDead()) {
					player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
					player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1, 1f);

					// Draw tether line from Player to Boss.
					Location playerLoc = LocationUtils.getEntityCenter(player);
					Vector dir = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(mBoss), playerLoc).normalize().multiply(0.5);
					Location pLoc = playerLoc.clone();
					for (int i = 0; i < 80; i++) {
						pLoc.add(dir);
						new PartialParticle(Particle.SMOKE_LARGE, pLoc, 1, 0, 1, 0)
							.extra(0.2)
							.directionalMode(true)
							.spawnForPlayer(ParticleCategory.BOSS, player);

						if (pLoc.distance(LocationUtils.getEntityCenter(mBoss)) < 0.5) {
							break;
						}
					}
					this.cancel();
					return;
				}

				if (mSoulLinkTicks < 0) {
					if (mSoulLinkTicks % 10 == 0) {
						player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 3, 0.5f);
					}

					if (mSoulLinkTicks % 5 == 0) {
						// Draw tether line from Player to Boss.
						Location playerLoc = LocationUtils.getEntityCenter(player);
						Vector dir = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(mBoss), playerLoc).normalize().multiply(0.5);
						Location pLoc = playerLoc.clone();
						for (int i = 0; i < 80; i++) {
							pLoc.add(dir);
							new PartialParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0)
								.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f))
								.extra(0)
								.spawnForPlayer(ParticleCategory.BOSS, player);

							if (pLoc.distance(LocationUtils.getEntityCenter(mBoss)) < 0.5) {
								break;
							}
						}
					}
				}

				if (mSoulLinkTicks > 0) {
					double distance = player.getLocation().distance(mBoss.getLocation());

					if (distance > DISTANCE_THRESHOLD) {
						mPlugin.mEffectManager.addEffect(player, "SoulLinkWeaken", new PercentDamageDealt(20, -0.5));

						if (!mWarnedPlayers.contains(player)) {
							player.sendMessage(Component.text("You are getting too far from the soul link! You feel your soul being stretched thinly.", NamedTextColor.AQUA));
							mWarnedPlayers.add(player);
						}

						mTooFarCombo++;

						MessagingUtils.sendTitle(player, Component.text(""), Component.text("Soul Linked with ", NamedTextColor.YELLOW).append(Component.text("Vesperidys", NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED)).append(Component.text(" (" + new DecimalFormat("#.#").format(Math.max(0.0, (tooFarThreshold - mTooFarCombo) / 20.0)) + ")", NamedTextColor.RED, TextDecoration.BOLD)), 0, 20, 10);

						if (mTooFarCombo > tooFarThreshold) {
							if (mTooFarCombo % 5 == 0) {
								damage(player);
							}
						} else if (mTooFarCombo % 5 == 0) {
							player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 2f);
						}
					} else {
						mTooFarCombo = 0;
					}

					if (mSoulLinkTicks % 20 == 0 && mTooFarCombo == 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 3, 1f);
					} else if (mSoulLinkTicks % 5 == 0 && mTooFarCombo > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 3, 1.5f);
					}

					// Draw tether line from Player to Boss.
					Location playerLoc = LocationUtils.getEntityCenter(player);
					Vector dir = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(mBoss), playerLoc).normalize().multiply(0.5);
					Location pLoc = playerLoc.clone();
					for (int i = 0; i < 80; i++) {
						pLoc.add(dir);

						if (distance <= DISTANCE_THRESHOLD) {
							new PartialParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0)
								.extra(9999999)
								.spawnForPlayer(ParticleCategory.BOSS, player);
						} else {
							if (mTooFarCombo < tooFarThreshold) {
								new PartialParticle(Particle.FLAME, pLoc, 1, 0, 0, 0)
									.extra(9999999)
									.spawnForPlayer(ParticleCategory.BOSS, player);
							} else {
								new PartialParticle(Particle.SOUL_FIRE_FLAME, pLoc, 1, 0, 0, 0)
									.extra(9999999)
									.spawnForPlayer(ParticleCategory.BOSS, player);
							}

							if (FastUtils.randomIntInRange(0, 20) == 0) {
								new PartialParticle(Particle.SOUL, pLoc, 1, 0, 0.5, 0)
									.extra(0.2)
									.directionalMode(true)
									.spawnForPlayer(ParticleCategory.BOSS, player);
							}
						}

						if (pLoc.distance(LocationUtils.getEntityCenter(mBoss)) < 0.5) {
							break;
						}
					}
				}

				mSoulLinkTicks++;
			}
		};
		soulLinkRunnable.runTaskTimer(mPlugin, 0, 1);

		mActiveRunnables.add(soulLinkRunnable);
	}

	public void damage(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1, 1f);
		player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1, 1f);
		mVesperidys.dealPercentageAndCorruptionDamage(player, 0.1, "Soul Link");
		new PPExplosion(Particle.SOUL, player.getLocation())
			.count(10)
			.spawnAsBoss();
	}

	@Override public boolean canRun() {
		return !mOnCooldown && !mVesperidys.mTeleportSpell.mTeleporting;
	}

	@Override
	public int cooldownTicks() {
		return 8 * 20;
	}
}
