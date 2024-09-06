package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.hexfall.VoodooTotemBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.hexfall.VoodooBindings;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellCommandingIncantation extends Spell {

	private static final String ABILITY_NAME = "Commanding Incantation";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mCooldown;
	private final int mStackRad;
	private final int mTotemDelay;
	private final Location mSpawnLoc;
	private final boolean mSendMessage;
	private final ChargeUpManager mChargeUp;

	public SpellCommandingIncantation(Plugin plugin, LivingEntity boss, int range, int castTime, int cooldown, int stackRad, int delay, Location spawnLoc, boolean sendMessage) {
		mPlugin = plugin;
		mBoss = boss;
		mCooldown = cooldown;
		mStackRad = stackRad;
		mTotemDelay = delay;
		mSpawnLoc = spawnLoc;
		mSendMessage = sendMessage;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.reset();

		if (mSendMessage) {
			for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
				VoodooBindings voodooBindings = mPlugin.mEffectManager.getActiveEffect(p, VoodooBindings.class);
				if (voodooBindings != null) {
					VoodooBindings.VoodooBinding activeBinding = voodooBindings.getCurrentBinding();
					if (activeBinding != null) {
						Component component = activeBinding.toDirective();
						p.sendMessage(component);
						p.sendActionBar(component);
					}
				}
			}
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.getTime() % 20 == 0) {
					new PPSpiral(Particle.SPELL_WITCH, mBoss.getLocation(), 2)
						.count(10)
						.spawnAsBoss();

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						player.playSound(player, Sound.BLOCK_COPPER_HIT, SoundCategory.HOSTILE, 1f, 1f);
					}
				} else if (mChargeUp.getTime() % 20 == 10) {
					new PPSpiral(Particle.CRIT_MAGIC, mBoss.getLocation(), 2)
						.count(10)
						.spawnAsBoss();
				}

				if (mChargeUp.getTime() % 2 == 0) {
					for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						VoodooBindings voodooBindings = mPlugin.mEffectManager.getActiveEffect(p, VoodooBindings.class);
						if (voodooBindings != null) {
							VoodooBindings.VoodooBinding activeBinding = voodooBindings.getCurrentBinding();
							if (activeBinding != null) {
								Location pGround = LocationUtils.fallToGround(p.getLocation(), mSpawnLoc.getY());
								new PPCircle(Particle.SPELL_WITCH, pGround, mStackRad).ringMode(true).count(30).spawnAsBoss();

								boolean correct = switch (activeBinding) {
									case GREEN_CIRCLE, GREEN_DONUT, RED_CIRCLE, RED_DONUT -> HexfallUtils.playersInBossInXZRange(pGround, mStackRad, true).size() == activeBinding.playerCount();
									case YELLOW_CIRCLE, YELLOW_DONUT -> HexfallUtils.playersInBossInXZRange(pGround, mStackRad, true).stream().filter(player -> {
										VoodooBindings otherBindings = mPlugin.mEffectManager.getActiveEffect(player, VoodooBindings.class);
										return player.equals(p) || (otherBindings != null && (otherBindings.getCurrentBinding() == VoodooBindings.VoodooBinding.WHITE_CIRCLE || otherBindings.getCurrentBinding() == VoodooBindings.VoodooBinding.WHITE_DONUT));
									}).toList().size() == activeBinding.playerCount();
									default -> false;
								};

								if (activeBinding.playerCount() > 0) {
									if (correct) {
										new PPPillar(Particle.REDSTONE, pGround, 5).data(new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.25f)).count(15).spawnAsBoss();
									} else {
										new PPPillar(Particle.REDSTONE, pGround, 5).data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.25f)).count(5).spawnAsBoss();
									}
								}
							}
						}
					}
				}

				if (mChargeUp.nextTick()) {
					mChargeUp.remove();
					this.cancel();

					Set<Player> whiteConformed = new HashSet<>();
					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.5f);
						VoodooBindings voodooBindings = mPlugin.mEffectManager.getActiveEffect(player, VoodooBindings.class);
						if (voodooBindings != null) {
							VoodooBindings.VoodooBinding activeBinding = voodooBindings.getCurrentBinding();
							if (activeBinding != null) {
								Location pGround = LocationUtils.fallToGround(player.getLocation(), mSpawnLoc.getY());

								boolean circleOrDonut = switch (activeBinding) {
									case WHITE_CIRCLE, GREEN_CIRCLE, YELLOW_CIRCLE, RED_CIRCLE -> true;
									case WHITE_DONUT, GREEN_DONUT, YELLOW_DONUT, RED_DONUT -> false;
								};

								Entity circle = LibraryOfSoulsIntegration.summon(pGround, "VoodooBindings");
								String actionTag = "[actiondelay=" + mTotemDelay + ",actiontype=" + circleOrDonut + "]";

								if (circle instanceof LivingEntity circleLiving) {
									circleLiving.setInvisible(true);
									circleLiving.addScoreboardTag("boss_voodooTotem" + actionTag);
									mPlugin.mBossManager.manuallyRegisterBoss(circleLiving, new VoodooTotemBoss(mPlugin, circleLiving));
								}

								boolean correct = switch (activeBinding) {
									case GREEN_CIRCLE, GREEN_DONUT, RED_CIRCLE, RED_DONUT -> HexfallUtils.playersInBossInXZRange(pGround, mStackRad, true).size() == activeBinding.playerCount();
									case YELLOW_CIRCLE, YELLOW_DONUT -> HexfallUtils.playersInBossInXZRange(pGround, mStackRad, true).stream().filter(p -> {
										VoodooBindings otherBindings = mPlugin.mEffectManager.getActiveEffect(p, VoodooBindings.class);
										return player.equals(p) || (otherBindings != null && (otherBindings.getCurrentBinding() == VoodooBindings.VoodooBinding.WHITE_CIRCLE || otherBindings.getCurrentBinding() == VoodooBindings.VoodooBinding.WHITE_DONUT));
									}).toList().size() == activeBinding.playerCount();
									case WHITE_CIRCLE, WHITE_DONUT -> true;
								};

								if (correct) {
									if (activeBinding != VoodooBindings.VoodooBinding.WHITE_CIRCLE && activeBinding != VoodooBindings.VoodooBinding.WHITE_DONUT) {
										whiteConformed.addAll(HexfallUtils.playersInBossInXZRange(player.getLocation(), mStackRad, true));
									}
								} else {
									PlayerUtils.killPlayer(player, mBoss, ABILITY_NAME);
								}
							}
						}
					}

					for (Player stacker : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						VoodooBindings otherBindings = mPlugin.mEffectManager.getActiveEffect(stacker, VoodooBindings.class);
						if (!whiteConformed.contains(stacker) && otherBindings != null && (otherBindings.getCurrentBinding() == VoodooBindings.VoodooBinding.WHITE_CIRCLE || otherBindings.getCurrentBinding() == VoodooBindings.VoodooBinding.WHITE_DONUT)) {
							PlayerUtils.killPlayer(stacker, mBoss, ABILITY_NAME);
						}
					}

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						VoodooBindings voodooBindings = mPlugin.mEffectManager.getActiveEffect(player, VoodooBindings.class);
						if (voodooBindings != null) {
							voodooBindings.popCurrentBinding();
						}
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
