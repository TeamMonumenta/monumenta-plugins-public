package com.playmonumenta.plugins.effects;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;

public class AbilitySilence extends Effect {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);

	public AbilitySilence(int duration) {
		super(duration);
	}

	@Override
	public double getMagnitude() {
		// This is useful so that the "active" effect is always the correct duration
		return getDuration();
	}

	@Override
	public boolean entityCastSpellEvent(SpellCastEvent event) {
		event.setCancelled(true);
		return true;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player) {
			entity.sendActionBar(Component.text("You are silenced! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
			AbilityManager.getManager().getPlayerAbilities((Player) entity).silence();
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player) {
			AbilityManager.getManager().getPlayerAbilities((Player) entity).unsilence();
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			if (entity instanceof Player) {
				entity.sendActionBar(Component.text("You are silenced! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
			}
		}

		if (fourHertz) {
			entity.getWorld().spawnParticle(Particle.REDSTONE, entity.getLocation().add(0, 0.5, 0), 4, 0.2, 0.4, 0.2, 0, COLOR);
		}
	}

	@Override
	public String toString() {
		return String.format("AbilitySilence duration:%d", this.getDuration());
	}
}
