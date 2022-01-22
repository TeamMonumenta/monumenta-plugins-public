package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EsotericEnhancements extends PotionAbility {
	private static final int PARALYZE_DURATION = 8 * 20;

	public static final double BRUTAL_DOT_DAMAGE = 3;
	public static final int POTION_CAP_INCREASE_2 = 2;

	public EsotericEnhancements(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Esoteric Enhancements", 0, 0);
		mInfo.mLinkedSpell = ClassAbility.ESOTERIC_ENHANCEMENTS;
		mInfo.mScoreboardId = "Esoteric";
		mInfo.mShorthandName = "Es";
		mInfo.mDescriptions.add("Your Brutal Alchemy damage over time is increased to 3 damage. Your Gruesome Alchemy potions now afflict Paralyze (25% to be slowed by 100% for 1s every 1s) for 8s.");
		mInfo.mDescriptions.add("Your max potion charges is increased by 2.");
		mDisplayItem = new ItemStack(Material.BREWING_STAND, 1);
	}

	@Override
	public void apply(LivingEntity mob, ThrownPotion potion, boolean isGruesome) {
		// Brutal effect handled in BrutalAlchemy
		if (isGruesome) {
			EntityUtils.paralyze(mPlugin, PARALYZE_DURATION, mob);
		}
	}

}
