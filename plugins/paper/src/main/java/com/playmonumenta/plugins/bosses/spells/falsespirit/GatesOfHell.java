package com.playmonumenta.plugins.bosses.spells.falsespirit;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class GatesOfHell extends Spell {

	//All armor stands at gates which have the gates of hell tag
	//These armor stands also have an additional tag that corresponds with the direction it faces "N" "E" "S" or "W"
	private final List<LivingEntity> mPortals;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;

	private final List<LivingEntity> mOpenPortals = new ArrayList<>();

	//Which portal number in succession (1-6)
	int mNum;

	public GatesOfHell(Plugin plugin, LivingEntity boss, List<LivingEntity> portals, int num) {
		mPlugin = plugin;
		mBoss = boss;
		mPortals = portals;
		mNum = num;
	}

	@Override
	public void run() {
		//Opens 1 portal at random
		if (mPortals.size() > 0) {
			openGate(mPortals.remove(FastUtils.RANDOM.nextInt(mPortals.size())));
		}
	}

	private void openGate(LivingEntity portal) {
		if (portal == null) {
			return;
		}

		mOpenPortals.add(portal);
		Entity portalEntity = Objects.requireNonNull(LibraryOfSoulsIntegration.summon(portal.getLocation(), "PortalGate"));
		portalEntity.addScoreboardTag("PortalNum" + mNum);

		Component name = portalEntity.customName();
		List<String> names = Arrays.asList("Hallud", "Chason", "Midat", "Daath", "Keter");
		if (name == null || mNum > names.size()) {
			MMLog.warning("Failed to summon a portal in GatesOfHell (could not process name): mNum = " + mNum);
			return;
		}
		portalEntity.customName(name.append(Component.text(" - ").append(Component.text(names.get(mNum - 1)))));

		mNum++;

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid() || portalEntity.isDead() || !portalEntity.isValid()) {
					if (!portalEntity.isDead()) {
						portalEntity.remove();
					}
					mOpenPortals.remove(portal);
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, PercentDamageReceived.GENERIC_NAME);
					this.cancel();
				}

				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, PercentDamageReceived.GENERIC_NAME,
					new PercentDamageReceived(30, -1.0));
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 20);
		mActiveRunnables.add(runnable);
	}

	//Returns true if portal is open and boss is invulnerable
	public boolean checkPortals() {
		return !mOpenPortals.isEmpty();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
