package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class DisplayBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_display";
	private final Display mDisplay;

	public DisplayBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mDisplay = boss.getWorld().spawn(boss.getLocation(), p.TYPE.entityClass(), d -> {
			d.setTransformation(new Transformation(
				new Vector3f(p.TRANSLATION_X, p.TRANSLATION_Y, p.TRANSLATION_Z),
				new AxisAngle4f((float) Math.toRadians(p.ROTATION_DEGREES), p.ROTATION_X, p.ROTATION_Y, p.ROTATION_Z),
				new Vector3f(p.SCALE_X, p.SCALE_Y, p.SCALE_Z),
				new AxisAngle4f()
			));
			if (d instanceof BlockDisplay blockDisplay) {
				blockDisplay.setBlock(p.MATERIAL.createBlockData());
			}
			if (d instanceof ItemDisplay itemDisplay) {
				itemDisplay.setItemDisplayTransform(p.ITEM_DISPLAY_TYPE);
				itemDisplay.setItemStack(DisplayEntityUtils.generateRPItem(p.MATERIAL, p.ITEM_NAME));
			}
			d.setBillboard(p.BILLBOARD);
			d.setTeleportDuration(Math.clamp(0, p.ANIMATION_TICKS, 59));
			EntityUtils.setRemoveEntityOnUnload(d);
		});
		boss.addPassenger(mDisplay);

		super.constructBoss(SpellManager.EMPTY, List.of(new Spell() {
			@Override
			public void run() {
				if (p.FOLLOW_ENTITY_ROTATION) {
					mDisplay.setInterpolationDelay(0);
					mDisplay.setRotation(boss.getYaw(), boss.getPitch());
				}
				if (!boss.isValid()) {
					mDisplay.remove();
				}
			}

			@Override
			public int cooldownTicks() {
				return 0;
			}
		}), p.DETECTION, null, 0, 1);
	}

	@Override
	public void unload() {
		mDisplay.remove();
	}

	public static class Parameters extends BossParameters {
		public enum DisplayType {
			ITEM(ItemDisplay.class),
			BLOCK(BlockDisplay.class);

			private final Class<? extends Display> mClazz;

			DisplayType(Class<? extends Display> clazz) {
				mClazz = clazz;
			}

			public Class<? extends Display> entityClass() {
				return mClazz;
			}
		}

		@BossParam(help = "Minimum distance to a player for the display to update")
		public int DETECTION = 50;
		@BossParam(help = "Interpolation duration")
		public int ANIMATION_TICKS = 1;

		@BossParam(help = "Material of the display")
		public Material MATERIAL = Material.GLASS;
		@BossParam(help = "Display billboard mode")
		public Display.Billboard BILLBOARD = Display.Billboard.FIXED;
		@BossParam(help = "Item displays are centered, block displays are not")
		public DisplayType TYPE = DisplayType.ITEM;
		@BossParam(help = "For item displays, the display mode of the item")
		public ItemDisplay.ItemDisplayTransform ITEM_DISPLAY_TYPE = ItemDisplay.ItemDisplayTransform.NONE;
		@BossParam(help = "For item displays, the name of the item")
		public String ITEM_NAME = "";

		@BossParam(help = "Translation is affected by rotation")
		public float TRANSLATION_X = 0;
		@BossParam(help = "Translation is affected by rotation")
		public float TRANSLATION_Y = 0;
		@BossParam(help = "Translation is affected by rotation")
		public float TRANSLATION_Z = 0;

		@BossParam(help = "Rotation is in axis-angle (x,y,z,rotation)")
		public float ROTATION_X = 0;
		@BossParam(help = "Rotation is in axis-angle (x,y,z,rotation)")
		public float ROTATION_Y = 0;
		@BossParam(help = "Rotation is in axis-angle (x,y,z,rotation)")
		public float ROTATION_Z = 0;
		@BossParam(help = "Rotation is in axis-angle (x,y,z,rotation)")
		public double ROTATION_DEGREES = 0;

		@BossParam(help = "Should display rotate with the entity its on")
		public boolean FOLLOW_ENTITY_ROTATION = false;

		@BossParam(help = "Scale of the display")
		public float SCALE_X = 1;
		@BossParam(help = "Scale of the display")
		public float SCALE_Y = 1;
		@BossParam(help = "Scale of the display")
		public float SCALE_Z = 1;
	}
}
