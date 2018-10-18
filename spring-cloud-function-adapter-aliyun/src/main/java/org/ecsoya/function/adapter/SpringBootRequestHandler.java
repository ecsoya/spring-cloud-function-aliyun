package org.ecsoya.function.adapter;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.FunctionParam;
import com.aliyun.fc.runtime.PojoRequestHandler;

import reactor.core.publisher.Flux;

public class SpringBootRequestHandler<I, O> extends SpringFunctionInitializer implements PojoRequestHandler<I, O> {

	public SpringBootRequestHandler(Class<?> configurationClass) {
		super(configurationClass);
	}

	public SpringBootRequestHandler() {
		super();
	}

	public O handleRequest(I input, Context context) {
		String name = null;
		try {
			if (context != null) {
				FunctionParam functionParam = context.getFunctionParam();
				if (functionParam != null) {
					name = functionParam.getFunctionName();
				}
				context.getLogger().info("Handler processing a request for: " + name);
			}
			initialize(context);

			Function<Publisher<?>, Publisher<?>> function = lookup(name);
			Publisher<?> events = extract(function, convertEvent(input));
			Publisher<?> output = function.apply(events);
			return result(function, input, output);
		} catch (Throwable ex) {
			if (context != null) {
				context.getLogger().error(getClass().getName() + " failed: " + ex.getLocalizedMessage());
			}
			if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			}
			if (ex instanceof Error) {
				throw (Error) ex;
			}
			throw new UndeclaredThrowableException(ex);
		} finally {
			if (context != null) {
				context.getLogger().info("Handler processed a request for: " + name);
			}
		}
	}

	protected Object convertEvent(I input) {
		return input;
	}

	private Flux<?> extract(Function<?, ?> function, Object input) {
		if (!isSingleInput(function, input)) {
			if (input instanceof Collection) {
				return Flux.fromIterable((Iterable<?>) input);
			}
		}
		return Flux.just(input);
	}

	private O result(Function<?, ?> function, Object input, Publisher<?> output) {
		List<Object> result = new ArrayList<>();
		for (Object value : Flux.from(output).toIterable()) {
			result.add(convertOutput(value));
		}
		if (isSingleInput(function, input) && result.size() == 1) {
			@SuppressWarnings("unchecked")
			O value = (O) result.get(0);
			return value;
		}
		if (isSingleOutput(function, input) && result.size() == 1) {
			@SuppressWarnings("unchecked")
			O value = (O) result.get(0);
			return value;
		}
		@SuppressWarnings("unchecked")
		O value = (O) result;
		return value;
	}

	@SuppressWarnings("unchecked")
	protected O convertOutput(Object output) {
		return (O) output;
	}
}
