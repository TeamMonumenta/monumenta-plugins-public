package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SizeChangerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_size_changer";

	public static class Parameters extends BossParameters {

		@BossParam(help = "if the size should increase or decrease")
		public boolean INCREASE = false;

		@BossParam(help = "max size of this slime/magma cube")
		public int MAX_SIZE = 20;

		@BossParam(help = "min size of this slime/magma cube")
		public int MIN_SIZE = 1;

		@BossParam(help = "how much is the increase/decrease")
		public int CHANGER = 1;

		@BossParam(help = "How often (%hp) the size change")
		public double SPEED = 0.1;

		public int DETECTION = 20;

		@BossParam(help = "Effects applyed to the mob when the size change")
		public EffectsList EFFECTS = EffectsList.fromString("[(GLOWING,20,0)]");

		@BossParam(help = "Particles summon in circle around the mob, leave empty for no particles")
		public ParticlesList PARTICLES = ParticlesList.fromString("[(FLAME,1,0,0,0,0.05)]");

		public SoundsList SOUNDS = SoundsList.EMPTY;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SizeChangerBoss(plugin, boss);
	}

	private final int mStartSize;
	private final Parameters mParams;

	public SizeChangerBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		mStartSize = EntityUtils.getSize(boss);

		mParams = BossParameters.getParameters(mBoss, identityTag, new Parameters());

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.DETECTION, null);

	}

	@Override
	public void onHurt(DamageEvent event) {
		int currentSize = EntityUtils.getSize(mBoss);
		if (mParams.INCREASE ? currentSize >= mParams.MAX_SIZE : currentSize <= mParams.MIN_SIZE) {
			return;
		}

		double missingHealthFactor = Math.max(0, Math.min(1 - (mBoss.getHealth() - event.getFinalDamage(true)) / EntityUtils.getMaxHealth(mBoss), 1));

		int changePerStep = mParams.INCREASE ? mParams.CHANGER : -mParams.CHANGER;
		int steps = (int) Math.floor(missingHealthFactor / mParams.SPEED);
		int newSize = Math.min(mParams.MAX_SIZE, Math.max(mParams.MIN_SIZE, mStartSize + steps * changePerStep));

		if (newSize != currentSize) {
			EntityUtils.setSize(mBoss, newSize);

			mParams.EFFECTS.apply(mBoss, mBoss);
			mParams.SOUNDS.play(mBoss.getLocation());

			double height = mBoss.getHeight();

			if (!mParams.PARTICLES.isEmpty()) {
				new BukkitRunnable() {
					final Location mLoc = mBoss.getLocation().clone().add(0, height / 2, 0);

					@Override
					public void run() {
						for (int i = 0; i < 360; i = (int) (i + 20 * (2f / currentSize))) {
							double rad = Math.toRadians(i);
							double cos = FastUtils.cos(rad) * height;
							double sin = FastUtils.sin(rad) * height;
							mParams.PARTICLES.spawn(mBoss, mLoc.clone().add(cos, sin, sin));
							mParams.PARTICLES.spawn(mBoss, mLoc.clone().add(sin, sin, cos));
						}
					}
				}.runTaskLater(mPlugin, 1);
			}

		}

	}
}
