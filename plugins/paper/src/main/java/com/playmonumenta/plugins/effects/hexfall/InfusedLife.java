package com.playmonumenta.plugins.effects.hexfall;

import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class InfusedLife extends Effect {

	public static final String GENERIC_NAME = "Infusion of Life";
	public static final String effectId = "infusedLife";
	public static final double MAX_ENERGY = 50.0;
	public static final int ENERGY_PER_SECOND = 4;
	public static final int ENERGY_PER_KILL = 50;
	private final BossBar mBossBar;

	private double mCurrentEnergy;
	private boolean mIsFull;
	private @Nullable TextDisplay mDisplay;

	public InfusedLife(int duration) {
		super(duration, effectId);
		mBossBar = BossBar.bossBar(Component.text("Infusion of Life", NamedTextColor.GREEN), 0, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
		mIsFull = false;
		mCurrentEnergy = 0;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && entity instanceof Player player) {
			mCurrentEnergy += ENERGY_PER_SECOND;
			if (mCurrentEnergy > MAX_ENERGY) {
				mCurrentEnergy = MAX_ENERGY;
			}
			if (mCurrentEnergy == MAX_ENERGY) {
				if (!mIsFull) {
					entity.setGlowing(true);
					ScoreboardUtils.addEntityToTeam(player, "green");
				}
				mIsFull = true;
			}
			double progress = mCurrentEnergy / MAX_ENERGY;
			if (progress < 0) {
				progress = 0;
			}
			if (progress > 1) {
				progress = 1.0;
			}

			mBossBar.progress((float) progress);
			player.showBossBar(mBossBar);

			TextComponent text = Component.text("Infused Life\n ", Style.style(NamedTextColor.GREEN, TextDecoration.BOLD));
			text = text.append(Component.text("[", Style.style(NamedTextColor.WHITE)));

			for (int i = 0; i < mCurrentEnergy; i++) {
				text = text.append(Component.text("|", Style.style(NamedTextColor.GREEN)));
			}

			for (int i = 0; i < MAX_ENERGY - mCurrentEnergy; i++) {
				text = text.append(Component.text("|", Style.style(NamedTextColor.RED)));
			}
			text = text.append(Component.text("]", Style.style(NamedTextColor.WHITE)));

			if (mDisplay != null) {
				mDisplay.text(text);
				mDisplay.setTransformation(new Transformation(new Vector3f(0, 2.5f, 0), new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));
				mDisplay.setInterpolationDelay(-1);
				mDisplay.setInterpolationDuration(-1);
			}
		}
	}

	@Override
	public void onKill(EntityDeathEvent event, Player player) {
		mCurrentEnergy += ENERGY_PER_KILL;
		if (mCurrentEnergy > MAX_ENERGY) {
			mCurrentEnergy = MAX_ENERGY;
		}
		if (mCurrentEnergy == MAX_ENERGY) {
			mIsFull = false;
		}
		double progress = mCurrentEnergy / MAX_ENERGY;
		if (progress < 0) {
			progress = 0;
		}
		if (progress > 1) {
			progress = 1.0;
		}
		mBossBar.progress((float) progress);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1, 1);
			player.showBossBar(mBossBar);
			mDisplay = player.getWorld().spawn(player.getLocation().add(0, 3, 0), TextDisplay.class);
			mDisplay.setBillboard(Display.Billboard.CENTER);
			mDisplay.addScoreboardTag("HexfallDisplay");
			player.addPassenger(mDisplay);
		}
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text("Infusion of Life", NamedTextColor.GREEN);
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (event.isCancelled()) {
			return;
		}
		event.getEntity().getWorld().hideBossBar(mBossBar);
		if (mDisplay != null) {
			mDisplay.remove();
		}
		if (event.getEntity() instanceof Player player) {
			Team team = ScoreboardUtils.getEntityTeam(player);
			if (team != null) {
				team.removePlayer(player);
			}
			player.setGlowing(false);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		entity.getWorld().hideBossBar(mBossBar);
		if (mDisplay != null) {
			mDisplay.remove();
		}
		if (entity instanceof Player player) {
			Team team = ScoreboardUtils.getEntityTeam(player);
			if (team != null) {
				team.removePlayer(player);
			}
			player.setGlowing(false);
		}
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public boolean shouldDeleteOnLogout() {
		return true;
	}

	@Override
	public double getMagnitude() {
		return mCurrentEnergy;
	}

	public void setCurrentEnergy(double energy) {
		mCurrentEnergy = energy;
	}

	@Override
	public String toString() {
		return String.format(GENERIC_NAME + ":%d", this.getDuration());
	}
}
