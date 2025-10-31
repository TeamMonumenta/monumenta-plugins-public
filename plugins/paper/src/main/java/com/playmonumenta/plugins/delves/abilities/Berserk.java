package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class Berserk {

	public static final String DESCRIPTION = "Go berserk by building up a combo.";
	private static final int MAX_KILL_TIME = 7 * 20;
	private static final int MIN_KILL_TIME = 3 * 20;
	private static final double COMBO_MULTI = 0.03;
	private static final Map<Player, Integer> berserkCombo = new HashMap<>();
	private static final Map<Player, BukkitTask> activeBerserk = new HashMap<>();
	private static final String sourceName = "berserkEffect";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Kill enemies within " + MAX_KILL_TIME / 20 + " seconds to build combo (-0.1 seconds per combo)."),
			Component.text("Each combo adds " + COMBO_MULTI + "x damage dealt and taken."),
			Component.text("Losing the combo will remove the effect."),
		};
	}

	public static void applyModifiers(Player p, LivingEntity mob) {
		if (mob.getScoreboardTags().contains("GrayTess") || DelvesUtils.getModifierLevel(p, DelvesModifier.BERSERK) == 0) {
			return;
		}
		for (Player p2 : p.getLocation().getNearbyPlayers(15)) {
			berserkCombo.put(p2, berserkCombo.getOrDefault(p2, 0) + 1);
			if (activeBerserk.containsKey(p2)) {
				BukkitTask previousTask = activeBerserk.get(p2);
				if (previousTask != null && !previousTask.isCancelled()) {
					previousTask.cancel();
				}
				Plugin.getInstance().mEffectManager.clearEffects(p2, sourceName);
			}
			int duration = Math.max(MIN_KILL_TIME, MAX_KILL_TIME - (2 * berserkCombo.get(p2)));
			Plugin.getInstance().mEffectManager.addEffect(p2, sourceName, new PercentDamageReceived(duration, COMBO_MULTI * berserkCombo.get(p2)));
			Plugin.getInstance().mEffectManager.addEffect(p2, sourceName, new PercentDamageDealt(duration, COMBO_MULTI * berserkCombo.get(p2)));
			MessagingUtils.sendActionBarMessage(p2, "Combo Multi: " + Math.round((1.0 + (berserkCombo.get(p2) * COMBO_MULTI)) * 100.0) / 100.0 + "x", NamedTextColor.RED);
			if (berserkCombo.get(p2) % 10 == 0 || berserkCombo.get(p2) == 1) {
				p2.getWorld().playSound(p2.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 1f, 1f);
			}
			BukkitTask task = new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks > duration) {
						activeBerserk.remove(p2);
						berserkCombo.remove(p2);
						Plugin.getInstance().mEffectManager.clearEffects(p2, sourceName);
						MessagingUtils.sendActionBarMessage(p2, "Combo Broken!", NamedTextColor.RED);
						p2.getWorld().playSound(p2.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.75f, 1f);
						this.cancel();
					}
					mTicks++;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
			activeBerserk.put(p2, task);
		}
		Location eyeLocation = p.getEyeLocation();
		Vector direction = eyeLocation.getDirection().setY(0).normalize();
		Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		Location particleLoc = p.getLocation().add(0, 2, 0).add(direction.clone().multiply(2)).add(right.clone().multiply(2));
		// this is all math from drawing the digit so i can know what location to put the x
		double leftMost;
		if (Integer.toString(berserkCombo.get(p)).length() % 2 == 0) {
			leftMost = (0.5 + Math.floor(Integer.toString(berserkCombo.get(p)).length() / 2.0)) * 0.8;
		} else {
			leftMost = (Math.floor(Integer.toString(berserkCombo.get(p)).length() / 2.0) + 1) * 0.8;
		}
		Vector right2 = VectorUtils.crossProd(new Vector(0, 1, 0), LocationUtils.getDirectionTo(p.getLocation(), particleLoc.clone()).setY(0).normalize());
		Location xLoc = particleLoc.clone().subtract(right2.clone().multiply(leftMost)).add(0, -0.3, 0);
		xLoc.add(right2.clone().multiply(0.8).multiply(Integer.toString(berserkCombo.get(p)).length() + 1));
		Location xLoc2 = xLoc.clone().add(right2.clone().multiply(0.4)).add(new Vector(0, 0.4, 0));
		for (int i = 0; i < 4; i++) {
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				ParticleUtils.drawSevenSegmentNumber(
					berserkCombo.get(p), particleLoc.clone(),
					p, 0.4, 0.4, Particle.REDSTONE, new Particle.DustOptions(comboColour(berserkCombo.get(p)), 0.75f)
				);
				new PPLine(Particle.REDSTONE, xLoc, xLoc2).data(new Particle.DustOptions(comboColour(berserkCombo.get(p)), 0.75f)).countPerMeter(12).spawnAsPlayerActive(p);
				new PPLine(Particle.REDSTONE, xLoc.clone().add(0, 0.4, 0), xLoc2.clone().add(0, -0.4, 0)).data(new Particle.DustOptions(comboColour(berserkCombo.get(p)), 0.75f)).countPerMeter(12).spawnAsPlayerActive(p);
			}, i * 3L);
		}
	}

	private static Color comboColour(int comboMulti) {
		int combo2 = comboMulti / 10;
		// per 5 combo, change colour to wool dungeon colour
		return switch (combo2) {
			case 0 -> Color.WHITE;
			case 1 -> Color.ORANGE;
			case 2 -> Color.FUCHSIA;
			case 3 -> Color.AQUA;
			case 4 -> Color.YELLOW;
			case 5 -> Color.LIME;
			case 6 -> Color.fromRGB(254, 127, 156);
			case 7 -> Color.GRAY;
			case 8 -> Color.SILVER;
			case 9 -> Color.TEAL;
			case 10 -> Color.PURPLE;
			case 11 -> Color.BLUE;
			case 12 -> Color.MAROON;
			case 13 -> Color.GREEN;
			case 14 -> Color.RED;
			default -> Color.BLACK;
		};
	}
}
