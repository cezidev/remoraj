/*
 * Copyright 2019-2020 NASTEL TECHNOLOGIES, INC.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.jkoolcloud.remora.advices;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import com.jkoolcloud.remora.RemoraConfig;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class OutputStreamWriteAdvice extends BaseTransformers implements RemoraAdvice {

	public static final String ADVICE_NAME = "OutputStreamWriteAdvice";
	public static String[] INTERCEPTING_CLASS = { "java.io.OutputStream" };
	public static String INTERCEPTING_METHOD = "write";

	/**
	 * Method matcher intended to match intercepted class method/s to instrument. See (@ElementMatcher) for available
	 * method matches.
	 */

	private static ElementMatcher<? super MethodDescription> methodMatcher() {
		return named(INTERCEPTING_METHOD).and(takesArguments(1).and(takesArgument(0, byte.class)));
	}

	/**
	 * Type matcher should find the class intended for instrumentation See (@ElementMatcher) for available matches.
	 */

	@Override
	public ElementMatcher<TypeDescription> getTypeMatcher() {
		return hasGenericSuperType(named(INTERCEPTING_CLASS[0]));
	}

	@Override
	public AgentBuilder.Transformer getAdvice() {
		return advice;
	}

	static AgentBuilder.Transformer.ForAdvice advice = new AgentBuilder.Transformer.ForAdvice()
			.include(OutputStreamWriteAdvice.class.getClassLoader()).include(RemoraConfig.INSTANCE.classLoader)//
			.advice(methodMatcher(), OutputStreamWriteAdvice.class.getName());

	/**
	 * Advices before method is called before instrumented method code
	 *
	 * @param thiz
	 *            reference to method object
	 * @param arguments
	 *            arguments provided for method
	 * @param method
	 *            instrumented method description
	 *
	 */

	@Advice.OnMethodEnter
	public static void before(@Advice.This OutputStream thiz, //
			@Advice.AllArguments Object[] arguments, //
			@Advice.Local("context") InterceptionContext ctx, @Advice.Origin Method method//
	) {
		try {
			ctx = prepareIntercept(OutputStreamWriteAdvice.class, thiz, method, arguments);
			if (!ctx.intercept) {
				return;
			}
			TaggedLogger logger = ctx.interceptorInstance.getLogger();
			StreamsManager.INSTANCE.get(thiz, ctx, method).advanceCount();
		} catch (Throwable t) {
			handleAdviceException(t, ctx);
		}

	}

	@Override
	protected AgentBuilder.Listener getListener() {
		return new TransformationLoggingListener(logger);
	}

	@Override
	public void install(Instrumentation instrumentation) {
		logger = Logger.tag(ADVICE_NAME);
		if (load) {
			getTransform().with(getListener()).installOn(instrumentation);
		} else {
			logger.info("Advice {} not enabled", this, getName());
		}
	}

	@Override
	public String getName() {
		return ADVICE_NAME;
	}

}
