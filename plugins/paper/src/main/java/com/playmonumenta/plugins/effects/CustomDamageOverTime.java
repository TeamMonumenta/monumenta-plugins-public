package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CustomDamageOverTime extends Effect {

	private final double mDamage;
	private final Plugin mPlugin;
	private final Player mPlayer;
	private final Spells mSpell;
	private final MagicType mMagic;
	private final Particle mParticle;

	public CustomDamageOverTime(int duration, double damage, Player player, MagicType magic, Spells spell, Particle particle, Plugin plugin) {
		super(duration);
		mDamage = damage;
		mPlayer = player;
		mSpell = spell;
		mMagic = magic;
		mParticle = particle;
		mPlugin = plugin;
	}

	@Override
	public double getMagnitude() {
		return mDamage;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity) {
			LivingEntity le = (LivingEntity) entity;
			if (oneHertz) {
				EntityUtils.damageEntity(mPlugin, le, mDamage, mPlayer, mMagic, true, mSpell, false, false, true, true);
				mPlayer.getWorld().spawnParticle(mParticle, entity.getLocation().add(0, 1.6, 0), 8, 0.4, 0.4, 0.4, 0.1);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("CustomDoT duration:%d modifier:%s damage:%f", this.getDuration(), "CustomDamageOverTime", mDamage);
	}
}
