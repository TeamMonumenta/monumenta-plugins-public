package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.utils.BossUtils;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

public class OnHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_onhit";

	public static class Parameters {
		public int DETECTION = 20;
		public boolean CAN_BLOCK = true;

		public EffectsList EFFECTS = EffectsList.EMPTY;


		//Particle & Sounds!
		/** Particle summoned when the player got hit by the boss */
		public ParticlesList PARTICLE = ParticlesList.fromString("[(REDSTONE,20,0,0,0,#ffffff,2)]");
		/** Sound played when the player got hit by the boss */
		public SoundsList SOUND = SoundsList.fromString("[(BLOCK_PORTAL_TRIGGER,0.25,2)]");

	}

	private Parameters mParams;

	public OnHitBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		//this boss has no ability
		mParams = BossUtils.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(null, null, mParams.DETECTION, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (mParams.CAN_BLOCK) {
			if (event.getFinalDamage() == 0) {
				// Attack was blocked
				return;
			}
		}

		LivingEntity target = (LivingEntity) event.getEntity();
		if (target instanceof Player) {
			Location loc = target.getLocation().add(0, 1, 0);
			Player player = (Player) target;

			mParams.EFFECTS.apply(player, mBoss);

			//Particle & Sound
			mParams.SOUND.play(loc);
			mParams.PARTICLE.spawn(loc, 0d, 0d, 0d);
		}


	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new OnHitBoss(plugin, boss);
	}

}
