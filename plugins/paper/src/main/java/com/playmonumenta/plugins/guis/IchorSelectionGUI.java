package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.infusions.IchorDawnbringer;
import com.playmonumenta.plugins.itemstats.infusions.IchorEarthbound;
import com.playmonumenta.plugins.itemstats.infusions.IchorFlamecaller;
import com.playmonumenta.plugins.itemstats.infusions.IchorFrostborn;
import com.playmonumenta.plugins.itemstats.infusions.IchorPrismatic;
import com.playmonumenta.plugins.itemstats.infusions.IchorShadowdancer;
import com.playmonumenta.plugins.itemstats.infusions.IchorSteelsage;
import com.playmonumenta.plugins.itemstats.infusions.IchorWindwalker;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class IchorSelectionGUI extends Gui {

	public IchorSelectionGUI(Player player) {
		super(player, 4 * 9, Component.text("Ichor Imbuement Selection"));
	}

	@Override
	protected void setup() {
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		setItem(1, 1, DepthsTree.DAWNBRINGER.createItemWithDescription(IchorDawnbringer.DESCRIPTION)).onClick((clickEvent) -> {
			infuseItem(mainhand, IchorListener.ICHOR_INFUSIONS.get(0));
			close();
		});
		setItem(1, 2, DepthsTree.EARTHBOUND.createItemWithDescription(IchorEarthbound.DESCRIPTION)).onClick((clickEvent) -> {
			infuseItem(mainhand, IchorListener.ICHOR_INFUSIONS.get(1));
			close();
		});
		setItem(1, 3, DepthsTree.FLAMECALLER.createItemWithDescription(IchorFlamecaller.DESCRIPTION)).onClick((clickEvent) -> {
			infuseItem(mainhand, IchorListener.ICHOR_INFUSIONS.get(2));
			close();
		});
		setItem(1, 4, DepthsTree.FROSTBORN.createItemWithDescription(IchorFrostborn.DESCRIPTION)).onClick((clickEvent) -> {
			infuseItem(mainhand, IchorListener.ICHOR_INFUSIONS.get(3));
			close();
		});
		setItem(2, 4, DepthsTree.PRISMATIC.createItemWithDescription(IchorPrismatic.DESCRIPTION)).onClick((clickEvent) -> {
			infuseItem(mainhand, IchorListener.ICHOR_INFUSIONS.get(4));
			close();
		});
		setItem(1, 5, DepthsTree.SHADOWDANCER.createItemWithDescription(IchorShadowdancer.DESCRIPTION)).onClick((clickEvent) -> {
			infuseItem(mainhand, IchorListener.ICHOR_INFUSIONS.get(5));
			close();
		});
		setItem(1, 6, DepthsTree.STEELSAGE.createItemWithDescription(IchorSteelsage.DESCRIPTION)).onClick((clickEvent) -> {
			infuseItem(mainhand, IchorListener.ICHOR_INFUSIONS.get(6));
			close();
		});
		setItem(1, 7, DepthsTree.WINDWALKER.createItemWithDescription(IchorWindwalker.DESCRIPTION)).onClick((clickEvent) -> {
			infuseItem(mainhand, IchorListener.ICHOR_INFUSIONS.get(7));
			close();
		});
	}

	private void infuseItem(ItemStack mainhand, InfusionType infusionType) {
		mPlayer.playSound(mPlayer, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1f, 2f);
		mPlayer.sendMessage(Component.text("Your " + ItemUtils.getPlainNameIfExists(mainhand) + " has been imbued with " + infusionType.getName() + "!").color(NamedTextColor.GRAY));
		clearIchorInfusions(mainhand);
		ItemStatUtils.addInfusion(mainhand, infusionType, 1, mPlayer.getUniqueId());
	}

	private void clearIchorInfusions(ItemStack item) {
		for (InfusionType type : IchorListener.ICHOR_INFUSIONS) {
			if (ItemStatUtils.hasInfusion(item, type)) {
				ItemStatUtils.removeInfusion(item, type);
			}
		}
	}
}
