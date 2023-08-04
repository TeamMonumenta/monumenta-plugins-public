package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.Nullable;

public enum Operation {
	ADD(AttributeModifier.Operation.ADD_NUMBER, "add"),
	MULTIPLY(AttributeModifier.Operation.ADD_SCALAR, "multiply");

	public static final String KEY = "Operation";

	final AttributeModifier.Operation mAttributeOperation;
	final String mName;

	Operation(AttributeModifier.Operation attributeOperation, String name) {
		mAttributeOperation = attributeOperation;
		mName = name;
	}

	public AttributeModifier.Operation getAttributeOperation() {
		return mAttributeOperation;
	}

	public String getName() {
		return mName;
	}

	public static @Nullable Operation getOperation(String name) {
		for (Operation operation : Operation.values()) {
			if (operation.getName().replace(" ", "").equals(name.replace(" ", ""))) {
				return operation;
			}
		}

		return null;
	}
}
