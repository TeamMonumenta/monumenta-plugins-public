package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

// From Hekawt; take double damage from all sources and -50% healing
public class LichCurseEffect extends Effect {
	public static final String effectId = "LichCurseEffect";

	public static double DAMAGE_INCREASE = 2.0;
	public static double HEAL_REDUCTION = 0.5;

	public LichCurseEffect(int duration) {
		super(duration, effectId);
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		event.setFlatDamage(event.getDamage() * DAMAGE_INCREASE);
	}

	@Override
	public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
		event.setAmount(event.getAmount() * HEAL_REDUCTION);
		return HEAL_REDUCTION > -1;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			new PartialParticle(Particle.SOUL, entity.getLocation().add(0, 0.75, 0), 6, 0.3, 0.3, 0.3, 0.01).spawnAsBoss();
			if (entity instanceof Player p) {
				p.sendActionBar(Component.text("Cursed for " + this.getDuration() / 20 + " seconds.", NamedTextColor.DARK_RED));
			}
		}
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public String toString() {
		return String.format("LichCurseEffect duration:%d", this.getDuration());
	}
}
