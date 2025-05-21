package com.playmonumenta.plugins.effects.hexfall;

import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectPriority;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.EntityEffect;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class Reincarnation extends Effect {

	public static final String GENERIC_NAME = "Reincarnation";
	public static final String effectId = "reincarnation";

	private double mStacks;

	public Reincarnation(int duration, double stacks) {
		super(duration, effectId);
		this.mStacks = stacks;
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (event.getEntity() instanceof Player player) {
			if (mStacks > 0) {
				mStacks--;

				if (mStacks <= 0) {
					this.clearEffect();
				}

				player.playEffect(EntityEffect.TOTEM_RESURRECT);
				player.playSound(player, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1f, 1f);

				for (Player p : PlayerUtils.playersInRange(player.getLocation(), 80, true)) {
					p.sendMessage(Component.text(player.getName(), NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(Component.text(" has Reincarnated!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				}

				player.setHealth(EntityUtils.getMaxHealth(player));
				event.setCancelled(true);
				player.setNoDamageTicks(20);
			}
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
	public @Nullable String getDisplayedName() {
		return "Reincarnation";
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text(String.format("%d", (long) mStacks) + " " + GENERIC_NAME + (mStacks > 1 ? "s" : ""), NamedTextColor.GREEN);
	}

	@Override
	public String toString() {
		return String.format(GENERIC_NAME + ":%f", this.mStacks);
	}

	@Override
	public double getMagnitude() {
		return mStacks;
	}

	public void setMagnitude(double stacks) {
		mStacks = stacks;
	}

	@Override
	public EffectPriority getPriority() {
		return EffectPriority.EARLY;
	}
}
