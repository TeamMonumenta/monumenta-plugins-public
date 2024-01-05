package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellBlightedBolts;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SiriusContagion extends Effect {
	public static final String effectID = "SiriusContagion";


	public SiriusContagion(int duration, String effectID) {
		super(duration, effectID);
	}


	@Override
	public String toString() {
		return String.format("Contagion %d", this.getDuration());
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);

		return object;
	}

	@SuppressWarnings("deprecation")
	@Override
	public @Nullable String getSpecificDisplay() {
		return ChatColor.RED + "Contagion";
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		Plugin.getInstance().mEffectManager.clearEffects(entity, SpellBlightedBolts.BLIGHTEDBOLTTAG);
		Plugin.getInstance().mEffectManager.addEffect(entity, "BlightProtection", new CustomTimerEffect(SpellBlightedBolts.SAFEDURATION, "Contagion Protected"));
		new PPExplosion(Particle.REDSTONE, entity.getLocation()).count(25).data(new Particle.DustOptions(Color.fromRGB(0, 128, 128), 1.25f)).spawnAsBoss();
		for (Player p : PlayerUtils.playersInRange(entity.getLocation(), SpellBlightedBolts.RADIUS, false, false)) {
			if (Plugin.getInstance().mEffectManager.getActiveEffect(p, SpellBlightedBolts.BLIGHTEDBOLTTAG) == null && Plugin.getInstance().mEffectManager.getActiveEffect(p, "BlightProtection") == null) {
				Plugin.getInstance().mEffectManager.addEffect(p, SpellBlightedBolts.BLIGHTEDBOLTTAG, new SiriusContagion(SpellBlightedBolts.BLIGHTEDBOLTCONTAGIONDURATION, SpellBlightedBolts.BLIGHTEDBOLTTAG));
				DamageUtils.damage(null, p, DamageEvent.DamageType.MAGIC, SpellBlightedBolts.CONATGIONDAMAGE);
			}
		}

	}


	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			new PPCircle(Particle.REDSTONE, entity.getLocation(), SpellBlightedBolts.RADIUS).ringMode(true).data(new Particle.DustOptions(Color.fromRGB(0, 128, 128), 5f)).count(10).spawnAsBoss();

		}
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player) {
			Player p = (Player) entity;
			p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.6f, 0.6f);
			p.playSound(p, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 0.7f, 0.8f);
			p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 2f, 2f);
			p.playSound(p, Sound.ENTITY_ZOMBIE_HORSE_DEATH, SoundCategory.HOSTILE, 0.5f, 0.6f);
			p.playSound(p, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.HOSTILE, 0.9f, 0.6f);
			p.playSound(p, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.6f, 0.6f);


		}
	}
}
