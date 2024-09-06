package com.playmonumenta.plugins.effects.hexfall;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.SingleArgumentEffect;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class CreepingDeath extends SingleArgumentEffect {

	public static final String GENERIC_NAME = "Creeping Death";
	public static final String effectId = "creepingDeath";
	public static final Integer MAX_STACKS = 10;
	public static final Integer STACK_FALLOFF_INTERVAL = 2;

	private int mTicks = 0;
	private int mStacks;
	private final Plugin mPlugin;
	private @Nullable TextDisplay mDisplay;

	public CreepingDeath(int duration, Plugin plugin) {
		super(duration, 1, effectId);
		mStacks = 1;
		mPlugin = plugin;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text(mStacks + " x Creeping Death", NamedTextColor.RED);
	}

	@Override
	public double getMagnitude() {
		return mStacks;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		super.entityTickEffect(entity, fourHertz, twoHertz, oneHertz);

		if (!(entity instanceof Player player) || !HexfallUtils.playerInBoss(player)) {
			return;
		}

		if (oneHertz) {
			if (mStacks >= MAX_STACKS) {
				PlayerUtils.killPlayer(player, null, GENERIC_NAME);
			}

			if (mTicks > STACK_FALLOFF_INTERVAL) {
				if (mStacks > 0) {
					mStacks -= 1;
					mTicks = 0;
				}
			}

			if (mStacks == 0) {
				mPlugin.mEffectManager.clearEffects(entity, GENERIC_NAME);
			}

			mTicks++;
		}

		TextComponent text = Component.text("Creeping Death\n ", Style.style(NamedTextColor.DARK_RED, TextDecoration.BOLD));

		for (int i = 0; i < MAX_STACKS - mStacks; i++) {
			text = text.append(Component.text("■ ", Style.style(NamedTextColor.GREEN)));
		}

		for (int i = 0; i < mStacks; i++) {
			text = text.append(Component.text("■ ", Style.style(NamedTextColor.RED)));
		}

		if (mDisplay != null) {
			mDisplay.text(text);
			mDisplay.setTransformation(new Transformation(new Vector3f(0, 1.5f, 0), new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));
			mDisplay.setInterpolationDelay(-1);
			mDisplay.setInterpolationDuration(-1);
		}

		if (fourHertz) {
			if (mStacks > 0) {
				if (mStacks > 5) {
					new PPCircle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 0.05 * mStacks)
						.count(7)
						.spawnAsBoss();
				} else {
					new PPCircle(Particle.SMOKE_LARGE, entity.getLocation(), 0.05 * mStacks)
						.count(7)
						.spawnAsBoss();
				}

			}
		}

		if (twoHertz) {
			TextComponent actionBarText = Component.text("Creeping Death: ", Style.style(NamedTextColor.DARK_RED, TextDecoration.BOLD));

			for (int i = 0; i < MAX_STACKS - mStacks; i++) {
				actionBarText = actionBarText.append(Component.text("■ ", Style.style(NamedTextColor.GREEN)));
			}

			for (int i = 0; i < mStacks; i++) {
				actionBarText = actionBarText.append(Component.text("■ ", Style.style(NamedTextColor.RED)));
			}

			player.sendActionBar(actionBarText);
		}
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			mDisplay = player.getWorld().spawn(player.getLocation().add(0, 3, 0), TextDisplay.class);
			mDisplay.setBillboard(Display.Billboard.CENTER);
			mDisplay.addScoreboardTag("HexfallDisplay");
			player.addPassenger(mDisplay);
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		mStacks = 0;
		if (event.isCancelled()) {
			return;
		}
		if (mDisplay != null) {
			mDisplay.remove();
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (mDisplay != null) {
			mDisplay.remove();
		}
	}

	@Override
	public boolean shouldDeleteOnLogout() {
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"%s | duration:%s magnitude:%s",
			this.getClass().getName(),
			getDuration(),
			getMagnitude()
		);
	}

	public void setStacks(int stacks) {
		mStacks = stacks;
		if (mStacks > MAX_STACKS) {
			mStacks = MAX_STACKS;
		}
	}
}
