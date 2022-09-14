package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ToughBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tough";
	public static final int detectionRange = 50;

	public static class Parameters extends BossParameters {
		public double HEALTH_INCREASE = 1;
	}

	final Parameters mParam;
	private ArmorStand mBannerHolder;


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ToughBoss(plugin, boss);
	}

	public ToughBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new ToughBoss.Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
		EntityUtils.scaleMaxHealth(mBoss, mParam.HEALTH_INCREASE, "vengeance_modifier");
		ItemStack banner = new ItemStack(Material.RED_BANNER);
		mBannerHolder = mBoss.getWorld().spawn(mBoss.getLocation(), ArmorStand.class);
		mBannerHolder.setVisible(false);
		mBannerHolder.setMarker(true);
		mBannerHolder.getEquipment().setHelmet(banner);
		mBoss.addPassenger(mBannerHolder);
	}

	@Override
	public void death(EntityDeathEvent event) {
		mBannerHolder.remove();
	}
}


