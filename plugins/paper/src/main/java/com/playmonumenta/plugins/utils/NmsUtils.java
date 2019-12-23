package com.playmonumenta.plugins.utils;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.CraftServer;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;

import net.minecraft.server.v1_13_R2.ChatMessage;
import net.minecraft.server.v1_13_R2.CommandListenerWrapper;
import net.minecraft.server.v1_13_R2.DamageSource;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityCreature;
import net.minecraft.server.v1_13_R2.EntityDamageSource;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.EntityLiving;
import net.minecraft.server.v1_13_R2.IChatBaseComponent;
import net.minecraft.server.v1_13_R2.MinecraftServer;
import net.minecraft.server.v1_13_R2.PathfinderGoalSelector;

public class NmsUtils {
	public static class ParsedCommandWrapper {
		private ParseResults<CommandListenerWrapper> mParse;

		protected ParsedCommandWrapper(ParseResults<CommandListenerWrapper> parse) {
			mParse = parse;
		}

		protected ParseResults<CommandListenerWrapper> getParseResults() {
			return mParse;
		}
	}

	public static ParsedCommandWrapper parseCommand(String cmd) throws Exception {
		//Need to make sure command does not have the / at the start - this breaks the Parser
		if(cmd.charAt(0) == '/') {
			cmd = cmd.substring(1);
		}

		ParseResults<CommandListenerWrapper> pr = null;

		MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
		net.minecraft.server.v1_13_R2.CommandDispatcher dispatcher = server.getCommandDispatcher();
		CommandDispatcher<CommandListenerWrapper> brigadierDispatcher = dispatcher.a();

		pr = brigadierDispatcher.parse(cmd, server.getServerCommandListener());

		if (pr == null) {
			throw new Exception("ParseResults are null");
		}

		return new ParsedCommandWrapper(pr);
	}

	public static void runParsedCommand(ParsedCommandWrapper parsed) throws Exception {
		ParseResults<CommandListenerWrapper> pr = parsed.getParseResults();

		MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
		net.minecraft.server.v1_13_R2.CommandDispatcher dispatcher = server.getCommandDispatcher();
		CommandDispatcher<CommandListenerWrapper> brigadierDispatcher = dispatcher.a();

		brigadierDispatcher.execute(pr);
	}

	public static void runParsedCommand(ParsedCommandWrapper parsed, Player player) throws Exception {
		CommandListenerWrapper playerContext = ((CraftPlayer) player).getHandle().getCommandListener();

		ParseResults<CommandListenerWrapper> pr = parsed.getParseResults();

		MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
		net.minecraft.server.v1_13_R2.CommandDispatcher dispatcher = server.getCommandDispatcher();
		CommandDispatcher<CommandListenerWrapper> brigadierDispatcher = dispatcher.a();

		brigadierDispatcher.execute(new ParseResults<CommandListenerWrapper>(pr.getContext().withSource(playerContext), pr.getStartIndex(), pr.getReader(), pr.getExceptions()));
	}

	private static Class<?> itemClazz = null;

	public static void removeVexSpawnAIFromEvoker(LivingEntity boss) {
		try {
			if (itemClazz == null) {
				itemClazz = Class.forName("net.minecraft.server.v1_13_R2.PathfinderGoalSelector$PathfinderGoalSelectorItem");
			}

			if (((CraftEntity) boss).getHandle() instanceof EntityInsentient && ((CraftEntity) boss).getHandle() instanceof EntityCreature) {
				EntityInsentient ei = (EntityInsentient)((CraftEntity) boss).getHandle();
				Set<?> goalB = (Set<?>) getPrivateField("b", PathfinderGoalSelector.class, ei.goalSelector);
				Iterator<?> it = goalB.iterator();
				while (it.hasNext()) {
					Object selector = it.next();
					Object goal = getPrivateField("a", itemClazz, selector);
					if (goal.getClass().getName().equals("net.minecraft.server.v1_13_R2.EntityEvoker$c")) {
						it.remove();
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static class CustomDamageSource extends EntityDamageSource {
		String mKilledUsingMsg;
		Entity mDamager;

		public CustomDamageSource(Entity damager, @Nullable String killedUsingMsg) {
			super("custom", damager);

			mDamager = damager;
			if (killedUsingMsg == null) {
				mKilledUsingMsg = "magic";
			} else {
				mKilledUsingMsg = killedUsingMsg;
			}
		}

		@Override
		public IChatBaseComponent getLocalizedDeathMessage(EntityLiving entityliving) {
			// death.attack.indirectMagic.item=%1$s was killed by %2$s using %3$s
			String s = "death.attack.indirectMagic.item";
			return new ChatMessage(s, new Object[] { entityliving.getScoreboardDisplayName(), this.w.getScoreboardDisplayName(), mKilledUsingMsg});
		}
	}

	public static void customDamageEntity(org.bukkit.entity.LivingEntity entity, double amount, Player damager) {
		customDamageEntity(entity, amount, damager, null);
	}

	public static void customDamageEntity(org.bukkit.entity.LivingEntity entity, double amount, Player damager, String killedUsingMsg) {
        DamageSource reason = new CustomDamageSource(((CraftHumanEntity) damager).getHandle(), killedUsingMsg);

        ((CraftLivingEntity)entity).getHandle().damageEntity(reason, (float) amount);
	}

	private static Object getPrivateField(String fieldName, Class<?> clazz, Object object) throws NoSuchFieldException, IllegalAccessException {
		Field field;
		Object o = null;

		field = clazz.getDeclaredField(fieldName);

		field.setAccessible(true);

		o = field.get(object);

		return o;
	}
}
