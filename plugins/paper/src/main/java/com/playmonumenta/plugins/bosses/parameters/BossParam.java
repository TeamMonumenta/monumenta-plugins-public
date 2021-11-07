package com.playmonumenta.plugins.bosses.parameters;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

public @interface BossParam {
	String help() default "not written";
}
