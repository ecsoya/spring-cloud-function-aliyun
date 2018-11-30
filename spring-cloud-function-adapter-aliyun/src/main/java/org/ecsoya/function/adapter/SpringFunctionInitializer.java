package org.ecsoya.function.adapter;

import java.io.Closeable;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.jar.Manifest;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.FunctionalSpringApplication;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

import com.aliyun.fc.runtime.Context;

import reactor.core.publisher.Flux;

public class SpringFunctionInitializer implements Closeable {

	private final Class<?> configurationClass;

	private Function<Publisher<?>, Publisher<?>> function;

	private AtomicBoolean initialized = new AtomicBoolean();

	@Autowired(required = false)
	private FunctionInspector inspector;

	@Autowired(required = false)
	private FunctionCatalog catalog;

	private volatile static ConfigurableApplicationContext context;

	public SpringFunctionInitializer(Class<?> configurationClass) {
		this.configurationClass = configurationClass;
	}

	public SpringFunctionInitializer() {
		this(getStartClass());
	}

	@Override
	public void close() {
		if (SpringFunctionInitializer.context != null) {
			SpringFunctionInitializer.context.close();
			SpringFunctionInitializer.context = null;
		}
	}

	@SuppressWarnings("unchecked")
	protected void initialize(Context ctxt) {
		ConfigurableApplicationContext context = SpringFunctionInitializer.context;

		if (!this.initialized.compareAndSet(false, true)) {
			return;
		}
		if (ctxt != null) {
			ctxt.getLogger().info("Initializing: " + configurationClass);
		}

		if (context == null) {
			synchronized (SpringFunctionInitializer.class) {
				if (context == null) {
					ClassUtils.overrideThreadContextClassLoader(SpringFunctionInitializer.class.getClassLoader());
					context = springApplication().run();
					SpringFunctionInitializer.context = context;
				}
			}
		}

		context.getAutowireCapableBeanFactory().autowireBean(this);
		if (ctxt != null) {
			ctxt.getLogger().info("Initialized context: catalog=" + this.catalog);
		}
		String name = context.getEnvironment().getProperty("function.name");
		if (name == null) {
			name = "function";
		}
		if (this.catalog == null) {
			if (context.containsBean(name)) {
				if (ctxt != null) {
					ctxt.getLogger().info("No catalog. Looking for Function bean name=" + name);
				}
				this.function = context.getBean(name, Function.class);
			}
		} else {
			Set<String> functionNames = this.catalog.getNames(Function.class);
			if (functionNames.size() == 1) {
				this.function = this.catalog.lookup(Function.class, functionNames.iterator().next());
			} else {
				this.function = this.catalog.lookup(Function.class, name);
			}
		}
		if (ctxt != null) {
			ctxt.getLogger().info("Function=" + this.function);
		}
	}

	private SpringApplication springApplication() {
		Class<?> sourceClass = configurationClass;
		SpringApplication application = new FunctionalSpringApplication(sourceClass);
		application.setWebApplicationType(WebApplicationType.NONE);
		return application;
	}

	public Function<Publisher<?>, Publisher<?>> getFunction() {
		return function;
	}

	public FunctionCatalog getCatalog() {
		return catalog;
	}

	public FunctionInspector getInspector() {
		return inspector;
	}

	protected boolean isSingleInput(Function<?, ?> function, Object input) {
		if (!(input instanceof Collection)) {
			return true;
		}
		if (this.inspector != null) {
			return Collection.class.isAssignableFrom(inspector.getInputType(function));
		}
		return ((Collection<?>) input).size() <= 1;
	}

	protected boolean isSingleOutput(Function<?, ?> function, Object output) {
		if (!(output instanceof Collection)) {
			return true;
		}
		if (this.inspector != null) {
			return Collection.class.isAssignableFrom(inspector.getOutputType(function));
		}
		return ((Collection<?>) output).size() <= 1;
	}

	protected Publisher<?> apply(Publisher<?> input) {
		if (this.function != null) {
			return Flux.from(function.apply(input));
		}
		throw new IllegalStateException("No function defined");
	}

	protected Function<Publisher<?>, Publisher<?>> lookup(String name) {
		Function<Publisher<?>, Publisher<?>> function = this.function;
		if (name != null && this.catalog != null) {
			Function<Publisher<?>, Publisher<?>> preferred = this.catalog.lookup(Function.class, name);
			if (preferred != null) {
				function = preferred;
			}
		}
		if (function != null) {
			return function;
		}
		throw new IllegalStateException("No function defined with name=" + name);
	}

	private static Class<?> getStartClass() {
		ClassLoader classLoader = SpringFunctionInitializer.class.getClassLoader();
		String property = System.getProperty("start-class");
		if (property != null) {
			return ClassUtils.resolveClassName(property, classLoader);
		}
		try {
			Class<?> result = getStartClass(Collections.list(classLoader.getResources("META-INF/MANIFEST.MF")));
			if (result == null) {
				result = getStartClass(Collections.list(classLoader.getResources("meta-inf/manifest.mf")));
			}
			return result;
		} catch (Exception ex) {
			return null;
		}
	}

	private static Class<?> getStartClass(List<URL> list) {
		for (URL url : list) {
			try {
				InputStream inputStream = url.openStream();
				try {
					Manifest manifest = new Manifest(inputStream);
					String startClass = manifest.getMainAttributes().getValue("Start-Class");
					if (startClass != null) {
						return ClassUtils.forName(startClass, SpringFunctionInitializer.class.getClassLoader());
					}
				} finally {
					inputStream.close();
				}
			} catch (Exception ex) {
			}
		}
		return null;
	}
}
