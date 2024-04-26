package com.playmonumenta.plugins.market.filters;

import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ComponentConfig {

	public static class ComponentConfigObject {
		@Nullable String mDisplayName;
		@Nullable ItemStack mDisplayItemStack;
		int mOrder;

		@Nullable Pair<String, String>[] mBlacklistConditions;

		ComponentConfigObject(@Nullable String displayName, @Nullable ItemStack displayItemStack, int order, @Nullable Pair<String, String>[] blacklistConditions) {
			this.mDisplayName = displayName;
			this.mDisplayItemStack = displayItemStack;
			this.mOrder = order;
			this.mBlacklistConditions = blacklistConditions;
		}

		public @Nullable String getDisplayName() {
			return mDisplayName;
		}

		public @Nullable ItemStack getDisplayItemStack() {
			return mDisplayItemStack;
		}

		public int getOrder() {
			if (mOrder == 0) {
				return 999999;
			}
			return mOrder;
		}

		public @Nullable Pair<String, String>[] getBlacklistConditions() {
			return mBlacklistConditions;
		}
	}

	public static Map<String, ComponentConfigObject> REGION_CONFIG = Map.of(
		Region.VALLEY.toString(), new ComponentConfigObject(
			"King's Valley",
			ItemUtils.createBanner(Material.CYAN_BANNER, new Pattern(DyeColor.LIGHT_BLUE, PatternType.CROSS), new Pattern(DyeColor.BLUE, PatternType.CIRCLE_MIDDLE), new Pattern(DyeColor.BLACK, PatternType.FLOWER), new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_TOP), new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_BOTTOM)),
			1,
			null
		),
		Region.ISLES.toString(), new ComponentConfigObject(
			"Celsian Isles",
			ItemUtils.createBanner(Material.GREEN_BANNER, new Pattern(DyeColor.LIME, PatternType.GRADIENT_UP), new Pattern(DyeColor.GREEN, PatternType.BORDER), new Pattern(DyeColor.GREEN, PatternType.RHOMBUS_MIDDLE), new Pattern(DyeColor.LIME, PatternType.CIRCLE_MIDDLE)),
			2,
			null
		),
		Region.RING.toString(), new ComponentConfigObject(
			"Architect's Ring",
			ItemUtils.createBanner(Material.WHITE_BANNER, new Pattern(DyeColor.BROWN, PatternType.STRIPE_SMALL), new Pattern(DyeColor.GREEN, PatternType.TRIANGLES_BOTTOM), new Pattern(DyeColor.GREEN, PatternType.TRIANGLES_TOP), new Pattern(DyeColor.LIGHT_GRAY, PatternType.GRADIENT), new Pattern(DyeColor.GREEN, PatternType.STRIPE_MIDDLE), new Pattern(DyeColor.GRAY, PatternType.GRADIENT_UP), new Pattern(DyeColor.BLACK, PatternType.FLOWER), new Pattern(DyeColor.WHITE, PatternType.CIRCLE_MIDDLE)),
			3,
			null
		)
	);

}
