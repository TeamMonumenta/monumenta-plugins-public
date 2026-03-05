package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DisplayBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_display";

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
		@BossParam(help = "Overrides material and itemname, takes the item from the mob")
		public boolean ITEM_FROM_SLOT = false;
		@BossParam(help = "equipment slot to get the item for the display")
		public EquipmentSlot SLOT = EquipmentSlot.HAND;
		@BossParam(help = "Whether itemfromslot removes the item as well")
		public boolean REMOVE_FROM_SLOT = false;
		@BossParam(help = "Whether the display glows")
		public boolean GLOWING = false;
		@BossParam(help = "Display glow color (glow_color_override nbt tag)")
		public String GLOW_COLOR = "ffffff";

		@BossParam(help = "Translation is affected by rotation")
		public float TRANSLATION_X = 0;
		@BossParam(help = "Translation is affected by rotation")
		public float TRANSLATION_Y = 0;
		@BossParam(help = "Translation is affected by rotation")
		public float TRANSLATION_Z = 0;

		@BossParam(help = "Rotation about the X axis (degrees)")
		public float ROTATION_X = 0;
		@BossParam(help = "Rotation about the Y axis (degrees)")
		public float ROTATION_Y = 0;
		@BossParam(help = "Rotation about the Z axis (degrees)")
		public float ROTATION_Z = 0;

		@BossParam(help = "Should display rotate with the entity its on")
		public boolean FOLLOW_ENTITY_ROTATION = false;
		@BossParam(help = "Should display rotate without y")
		public boolean IGNORE_Y_ROTATION = false;

		@BossParam(help = "Scale of the display")
		public float SCALE_X = 1;
		@BossParam(help = "Scale of the display")
		public float SCALE_Y = 1;
		@BossParam(help = "Scale of the display")
		public float SCALE_Z = 1;
	}

	private final Display mDisplay;

	public DisplayBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mDisplay = boss.getWorld().spawn(boss.getLocation(), p.TYPE.entityClass(), d -> {
			d.setTransformation(new Transformation(
				new Vector3f(p.TRANSLATION_X, p.TRANSLATION_Y, p.TRANSLATION_Z),
				new Quaternionf().rotateYXZ((float) Math.toRadians(p.ROTATION_Y), (float) Math.toRadians(p.ROTATION_X), (float) Math.toRadians(p.ROTATION_Z)),
				new Vector3f(p.SCALE_X, p.SCALE_Y, p.SCALE_Z),
				new Quaternionf()
			));
			@Nullable
			EntityEquipment equipment = boss.getEquipment();
			if (equipment != null) {
				setDisplayContent(d, p, equipment);
			}
			d.setBillboard(p.BILLBOARD);
			d.setTeleportDuration(Math.clamp(0, p.ANIMATION_TICKS, 59));
			if (p.GLOWING) {
				d.setGlowing(true);
			}
			d.setGlowColorOverride(Color.fromRGB(Integer.parseInt(p.GLOW_COLOR)));

			EntityUtils.setRemoveEntityOnUnload(d);
		});
		boss.addPassenger(mDisplay);

		super.constructBoss(SpellManager.EMPTY, p.FOLLOW_ENTITY_ROTATION ? List.of(new Spell() {
			@Override
			public void run() {
				mDisplay.setInterpolationDelay(0);
				mDisplay.setRotation(boss.getYaw(), p.IGNORE_Y_ROTATION ? 0 : boss.getPitch());
			}

			@Override
			public int cooldownTicks() {
				return 0;
			}
		}) : List.of(), p.DETECTION, null, 0, 1);
	}

	private static void setDisplayContent(Display d, Parameters p, EntityEquipment equipment) {
		if (d instanceof BlockDisplay blockDisplay) {
			if (p.ITEM_FROM_SLOT) {
				blockDisplay.setBlock(equipment.getItem(p.SLOT).getType().createBlockData());
				if (p.REMOVE_FROM_SLOT) {
					equipment.setItem(p.SLOT, ItemStack.empty());
				}
			} else {
				blockDisplay.setBlock(p.MATERIAL.createBlockData());
			}
		}
		if (d instanceof ItemDisplay itemDisplay) {
			if (p.ITEM_FROM_SLOT) {
				itemDisplay.setItemStack(equipment.getItem(p.SLOT));
				if (p.REMOVE_FROM_SLOT) {
					equipment.setItem(p.SLOT, ItemStack.empty());
				}
			} else {
				itemDisplay.setItemStack(DisplayEntityUtils.generateRPItem(p.MATERIAL, p.ITEM_NAME));
			}
			itemDisplay.setItemDisplayTransform(p.ITEM_DISPLAY_TYPE);
		}
	}

	@Override
	public void unload() {
		super.unload();
		mDisplay.remove();
	}
}
