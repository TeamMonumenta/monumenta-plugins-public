package com.playmonumenta.plugins.bosses.spells.sirius.declaration;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlightConversion;
import com.playmonumenta.plugins.effects.CustomTimerEffect;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DeclarationPoints extends Spell {
	private Sirius mSirius;
	private Plugin mPlugin;
	private PassiveStarBlightConversion mConverter;
	private static final int DURATION = 7 * 20;
	private static final double RADIUS = 6;
	private static final int CLEANSEDURATION = 3 * 20;

	public DeclarationPoints(Sirius sirius, Plugin plugin, PassiveStarBlightConversion converter) {
		mSirius = sirius;
		mPlugin = plugin;
		mConverter = converter;
	}

	@Override
	public void run() {
		for (Player p : mSirius.getPlayersInArena(false)) {
			MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("This tomb will be yours. There is no escape from this blight.", NamedTextColor.AQUA, TextDecoration.BOLD));
		}
		mConverter.blightArena(List.of(mSirius.mAuroraLocation.getBlock().getLocation(), mSirius.mTuulenLocation.getBlock().getLocation()), RADIUS, 4 * 20, 2 * 20, mPlugin);
		mConverter.restoreFullCircle(mSirius.mTuulenLocation, (int) RADIUS);
		mConverter.restoreFullCircle(mSirius.mAuroraLocation, (int) RADIUS);
		new BukkitRunnable() {
			int mTicks = 0;
			final ChargeUpManager mManager = new ChargeUpManager(mSirius.mBoss, DURATION, Component.text("Encroaching Blight", NamedTextColor.AQUA), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 75);

			@Override
			public void run() {
				mManager.nextTick();
				if (mTicks % 10 == 0) {
					new PPCircle(Particle.END_ROD, mSirius.mTuulenLocation, RADIUS).ringMode(true).count(15).spawnAsBoss();
					new PPCircle(Particle.END_ROD, mSirius.mAuroraLocation, RADIUS).ringMode(true).count(15).spawnAsBoss();
				}
				if (mTicks >= DURATION) {
					mManager.remove();
					List<Player> mProtectedPlayers = PlayerUtils.playersInRange(mSirius.mTuulenLocation, RADIUS, true, true);
					mProtectedPlayers.addAll(PlayerUtils.playersInRange(mSirius.mAuroraLocation, RADIUS, true, true));
					List<Player> mAttemptPlayers = PlayerUtils.playersInRange(mSirius.mTuulenLocation, RADIUS + 3, true, true);
					mAttemptPlayers.addAll(PlayerUtils.playersInRange(mSirius.mAuroraLocation, RADIUS + 3, true, true));
					for (Player p : mAttemptPlayers) {
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, Sirius.PARTICIPATION_TAG, new CustomTimerEffect(2 * 60 * 20, 1, "").displays(false).deleteOnLogout(true));
					}
					if (mProtectedPlayers.size() >= (int) (mSirius.getValidDeclarationPlayersInArena(false).size() / 2.0 + 0.5)) {
						mSirius.changeHp(true, 1);
						for (Player p : mSirius.getPlayersInArena(false)) {
							MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("This place should have been theirs! How have you stopped this?", NamedTextColor.AQUA));
						}
					} else {
						mSirius.changeHp(true, -5);
						for (Player p : mSirius.getPlayersInArena(false)) {
							MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("Yes, this tomb will become a portal to the Beyond...", NamedTextColor.AQUA));
						}
					}
					cleanse();
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void cleanse() {
		new BukkitRunnable() {
			int mTicks = 0;
			int mLastradius = 0;

			// by the end of cleanse duration radius needs to be ~= 75
			@Override
			public void run() {
				mTicks += 5;
				int radii = (int) (50 * (mTicks / (float) CLEANSEDURATION));
				mConverter.restoreFullCircle(mSirius.mTuulenLocation, radii);
				mConverter.restoreFullCircle(mSirius.mAuroraLocation, radii);
				mLastradius = radii;
				World world = mSirius.mBoss.getWorld();
				world.playSound(mSirius.mAuroraLocation, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 0.2f, 1.8f);
				world.playSound(mSirius.mAuroraLocation, Sound.ITEM_TRIDENT_RETURN, SoundCategory.NEUTRAL, 0.4f, 0.8f);
				world.playSound(mSirius.mAuroraLocation, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 0.2f, 1.8f);
				world.playSound(mSirius.mTuulenLocation, Sound.ITEM_TRIDENT_RETURN, 0.4f, 0.8f);
				if (mTicks >= CLEANSEDURATION) {
					mConverter.convertBehind();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}
}
