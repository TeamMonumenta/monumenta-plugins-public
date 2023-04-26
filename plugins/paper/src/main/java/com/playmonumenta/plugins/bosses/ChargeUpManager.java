package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.utils.MessagingUtils;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@Deprecated
public class ChargeUpManager {

	private final AdventureChargeUpManager mChargeUp;

	public ChargeUpManager(LivingEntity boss, int chargeTime, String title, BarColor color, BarStyle style, int range) {
		this(boss.getLocation(), boss, chargeTime, title, color, style, range);
	}

	public ChargeUpManager(Location loc, @Nullable LivingEntity boss, int chargeTime, String title, BarColor color, BarStyle style, int range) {
		mChargeUp = new AdventureChargeUpManager(loc, boss, chargeTime, MessagingUtils.LEGACY_SERIALIZER.deserialize(title), net.kyori.adventure.bossbar.BossBar.Color.valueOf(color.name()), legacyBarStyleConversion(style), range);
	}

	public static net.kyori.adventure.bossbar.BossBar.Overlay legacyBarStyleConversion(BarStyle style) {
		switch (style) {
			case SEGMENTED_6 -> {
				return net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_6;
			}
			case SEGMENTED_10 -> {
				return net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_10;
			}
			case SEGMENTED_12 -> {
				return net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_12;
			}
			case SEGMENTED_20 -> {
				return net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_20;
			}
			default -> {
				return net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS;
			}
		}
	}


	public void setChargeTime(int chargeTime) {
		mChargeUp.setChargeTime(chargeTime);
	}

	public void reset() {
		mChargeUp.reset();
	}

	public int getTime() {
		return mChargeUp.getTime();
	}

	public void setTime(int time) {
		mChargeUp.setTime(time);
	}

	public boolean nextTick() {
		return mChargeUp.nextTick(1);
	}

	public boolean nextTick(int time) {
		return mChargeUp.nextTick(time);
	}

	public boolean previousTick() {
		return mChargeUp.previousTick(1);
	}

	public boolean previousTick(int time) {
		return mChargeUp.previousTick(time);
	}

	public void setTitle(String title) {
		mChargeUp.setTitle(MessagingUtils.LEGACY_SERIALIZER.deserialize(title));
	}

	public void setColor(BarColor color) {
		mChargeUp.setColor(net.kyori.adventure.bossbar.BossBar.Color.valueOf(color.name()));
	}

	public BarColor getColor() {
		return BarColor.valueOf(mChargeUp.getColor().name());
	}

	public void update() {
		mChargeUp.update();
	}

	public void setProgress(double progress) {
		mChargeUp.setProgress((float) progress);
	}

	public void remove() {
		mChargeUp.remove();
	}
}
