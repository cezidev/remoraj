package com.jkoolcloud.remora.advices;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import com.jkoolcloud.remora.RemoraConfig;
import com.jkoolcloud.remora.core.EntryDefinition;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class SocketAdvice extends BaseTransformers implements RemoraAdvice {

	public static final String ADVICE_NAME = "SocketAdvice";
	public static String[] INTERCEPTING_CLASS = { "java.net.Socket" };
	public static String INTERCEPTING_METHOD = "connect";

	public static boolean logging = false;

	/**
	 * Method matcher intended to match intercepted class method/s to instrument. See (@ElementMatcher) for available
	 * method matches.
	 */

	private static ElementMatcher<? super MethodDescription> methodMatcher() {
		return named(INTERCEPTING_METHOD).and(takesArguments(2));
	}

	/**
	 * Type matcher should find the class intended for instrumentation See (@ElementMatcher) for available matches.
	 */

	@Override
	public ElementMatcher<TypeDescription> getTypeMatcher() {
		return hasSuperType(named(INTERCEPTING_CLASS[0]));
	}

	@Override
	public AgentBuilder.Transformer getAdvice() {
		return advice;
	}

	static AgentBuilder.Transformer.ForAdvice advice = new AgentBuilder.Transformer.ForAdvice()
			.include(SocketAdvice.class.getClassLoader()).include(RemoraConfig.INSTANCE.classLoader)//
			.advice(methodMatcher(), SocketAdvice.class.getName());

	/**
	 * Advices before method is called before instrumented method code
	 *
	 * @param thiz
	 *            reference to method object
	 * @param socketAddress
	 * @param timeout
	 *            arguments provided for method
	 * @param method
	 *            instrumented method description
	 * @param ed
	 *            {@link EntryDefinition} for collecting ant passing values to
	 *            {@link com.jkoolcloud.remora.core.output.OutputManager}
	 * @param startTime
	 *            method startTime
	 *
	 */

	@Advice.OnMethodEnter
	public static void before(@Advice.This Socket thiz, //
			@Advice.Argument(0) SocketAddress socketAddress, //
			@Advice.Argument(1) int timeout, //
			@Advice.Origin Method method, //
			@Advice.Local("ed") EntryDefinition ed, @Advice.Local("context") InterceptionContext ctx, //
			@Advice.Local("startTime") long startTime) {
		try {
			ctx = prepareIntercept(SocketAdvice.class, thiz, method, socketAddress, timeout);
			if (!ctx.intercept) {
				return;
			}
			TaggedLogger logger = ctx.interceptorInstance.getLogger();

			ed = getEntryDefinition(ed, SocketAdvice.class, ctx);
			logger.info("Entering: {} {}", SocketAdvice.class.getName(), ctx.interceptorInstance, "before");
			startTime = fillDefaultValuesBefore(ed, stackThreadLocal, thiz, method, ctx);

			ed.addPropertyIfExist("resource",
					thiz.getInetAddress() == null ? null : thiz.getInetAddress().getHostName());
			ed.addPropertyIfExist("localAddress",
					thiz.getLocalAddress() == null ? null : thiz.getLocalAddress().getHostName());
			ed.addPropertyIfExist("localPort", thiz.getLocalPort());
			ed.addPropertyIfExist("port", thiz.getPort());
			if (socketAddress instanceof InetSocketAddress) {
				ed.addPropertyIfExist("port", ((InetSocketAddress) socketAddress).getPort());
				ed.addPropertyIfExist("hostName", ((InetSocketAddress) socketAddress).getHostName());
				ed.addPropertyIfExist("hostString", ((InetSocketAddress) socketAddress).getHostString());

			}

		} catch (Throwable t) {
			handleAdviceException(t, ctx);
		}
	}

	/**
	 * Method called on instrumented method finished.
	 *
	 * @param obj
	 *            reference to method object
	 * @param method
	 *            instrumented method description
	 * @param socketAddress
	 * @param timeout
	 *            arguments provided for method
	 * @param exception
	 *            exception thrown in method exit (not caught)
	 * @param ed
	 *            {@link EntryDefinition} passed along the method (from before method)
	 * @param startTime
	 *            startTime passed along the method
	 */

	@Advice.OnMethodExit(onThrowable = Throwable.class)
	public static void after(@Advice.This Object thiz, //
			@Advice.Origin Method method, //
			@Advice.Argument(0) SocketAddress socketAddress, //
			@Advice.Argument(1) int timeout, //
			@Advice.Thrown Throwable exception, @Advice.Local("ed") EntryDefinition ed,
			@Advice.Local("context") InterceptionContext ctx, //
			@Advice.Local("startTime") long startTime) {
		boolean doFinally = true;
		try {
			ctx = prepareIntercept(SocketAdvice.class, thiz, method, socketAddress, timeout);
			if (!ctx.intercept) {
				return;
			}
			TaggedLogger logger = ctx.interceptorInstance.getLogger();

			if (ed == null) { // ed expected to be null if not created by entry, that's for duplicates
				logger.info(
						"EntryDefinition not exist, ctx.interceptorInstance, entry might be filtered out as duplicate or ran on test");
				doFinally = false;
				return;
			}
			logger.info("Exiting: {} {}", SocketAdvice.class.getName(), ctx.interceptorInstance, "after");
			fillDefaultValuesAfter(ed, startTime, exception, ctx);
		} catch (Throwable t) {
			handleAdviceException(t, ctx);
		} finally {
			if (doFinally) {
				doFinally(ctx, thiz.getClass());
			}
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
