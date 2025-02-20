package com.playmonumenta.plugins.bosses.bosses.hexfall;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellDestroyTotemPlatform;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellTotemPlatformLaunchAds;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellTotemPlatformSummonAds;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;

public class TotemPlatformBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_totemplatform";
	public static final int detectionRange = HyceneaRageOfTheWolf.detectionRange * 2;

	// Life totem if true, death totem if false
	public static class Parameters extends BossParameters {
		public boolean LIFE_OR_DEATH = true;
	}

	private final Plugin mMonumentaPlugin;
	private final @Nullable ArmorStand mArmorStand;
	private final @Nullable ArmorStand mCenterStand;
	private final Parameters mParameters;

	public TotemPlatformBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mMonumentaPlugin = plugin;
		EntityUtils.selfRoot(mBoss, 1000000);
		int mHealth = 600;
		EntityUtils.setMaxHealthAndHealth(mBoss, mHealth);
		mBoss.setAI(false);

		mParameters = BossParameters.getParameters(boss, identityTag, new TotemPlatformBoss.Parameters());

		mArmorStand = mBoss.getLocation().getNearbyEntitiesByType(ArmorStand.class, 10)
			.stream().filter(stand -> stand.getScoreboardTags().contains("Hycenea_Island"))
			.min((stand, stand2) -> (int) (stand.getLocation().distanceSquared(mBoss.getLocation()) - stand2.getLocation().distanceSquared(mBoss.getLocation())))
			.orElse(null);

		mCenterStand = mBoss.getLocation().getNearbyEntitiesByType(ArmorStand.class, detectionRange)
			.stream().filter(stand -> stand.getScoreboardTags().contains("Hycenea_Center"))
			.min((stand, stand2) -> (int) (stand.getLocation().distanceSquared(mBoss.getLocation()) - stand2.getLocation().distanceSquared(mBoss.getLocation())))
			.orElse(null);

		if (mArmorStand == null || mCenterStand == null) {
			MMLog.warning("[Hexfall] Failed to find an armor stand for this TotemPlatformBoss.");
			mBoss.remove();
			return;
		}

		HexfallUtils.clearPlatformAndAbove(mArmorStand.getLocation().clone());

		GrowableAPI.grow(mParameters.LIFE_OR_DEATH ? "islandLife" : "islandDeath", mArmorStand.getLocation().add(new Vector(0, -1, 0)), 1, 200, true);

		SpellManager activeSpells = new SpellManager(List.of(
			new SpellTotemPlatformSummonAds(mBoss.getLocation(), 3, 20 * 20, 2, mCenterStand)
		));

		List<Spell> passiveSpells = List.of(
			new SpellTotemPlatformLaunchAds(mBoss, 9, mCenterStand, 0, mParameters.LIFE_OR_DEATH)
		);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, null, 2 * 20, 2 * 20);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event == null || mArmorStand == null || mCenterStand == null) {
			return;
		}

		if (mArmorStand.getScoreboardTags().contains("Hycenea_StranglingRupture_Target")) {
			mArmorStand.addScoreboardTag("Hycenea_StranglingRupture_KillzoneActive");
		} else if (mArmorStand.getScoreboardTags().contains("Hycenea_TotemicDestruction_Target")) {
			mArmorStand.addScoreboardTag("Hycenea_TotemicDestruction_ShieldActive");
		} else {
			new SpellDestroyTotemPlatform(mMonumentaPlugin, mParameters.LIFE_OR_DEATH, mArmorStand, mCenterStand.getLocation()).run();
		}
	}
}
