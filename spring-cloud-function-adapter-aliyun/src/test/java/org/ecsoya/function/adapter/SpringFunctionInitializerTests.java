package org.ecsoya.function.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.function.Function;

import org.ecsoya.function.adapter.SpringFunctionInitializer;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;

import reactor.core.publisher.Flux;

/**
 * @author Ecsoya
 *
 */
public class SpringFunctionInitializerTests {

	private SpringFunctionInitializer handler = null;

	<I, O> SpringFunctionInitializer handler(Class<?> config) {
		SpringFunctionInitializer handler = new SpringFunctionInitializer(config);
		this.handler = handler;
		return handler;
	}

	@After
	public void close() throws IOException {
		if (handler != null)
			handler.close();
	}

	@Test
	public void bareConfig() {
		SpringFunctionInitializer handler = handler(BareConfig.class);
		handler.initialize(new TestContext("uppercase"));
		Bar bar = (Bar) Flux.from(handler.getFunction().apply(Flux.just(new Foo("bar")))).blockFirst();
		assertThat(bar.getValue()).isEqualTo("BAR");
	}

	@Test
	public void initializer() {
		SpringFunctionInitializer handler = handler(InitializerConfig.class);
		handler.initialize(new TestContext("uppercase"));
		Bar bar = (Bar) Flux.from(handler.getFunction().apply(Flux.just(new Foo("bar")))).blockFirst();
		assertThat(bar.getValue()).isEqualTo("BAR");
	}

	@Test
	public void function() {
		SpringFunctionInitializer handler = handler(FunctionConfig.class);
		handler.initialize(new TestContext("uppercase"));
		Bar bar = (Bar) Flux.from(handler.getFunction().apply(Flux.just(new Foo("bar")))).blockFirst();
		assertThat(bar.getValue()).isEqualTo("BAR");
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	protected static class BareConfig {
		@Bean
		public Function<Foo, Bar> function() {
			return foo -> new Bar(foo.getValue().toUpperCase());
		}
	}

	@SpringBootConfiguration
	protected static class FunctionConfig implements Function<Foo, Bar> {
		@Override
		public Bar apply(Foo foo) {
			return new Bar(foo.getValue().toUpperCase());
		}
	}

	@SpringBootConfiguration
	protected static class InitializerConfig implements ApplicationContextInitializer<GenericApplicationContext> {

		public Function<Foo, Bar> function() {
			return foo -> new Bar(foo.getValue().toUpperCase());
		}

		@Override
		public void initialize(GenericApplicationContext context) {
			context.registerBean(FunctionRegistration.class,
					() -> new FunctionRegistration<Function<Foo, Bar>>(function(), "uppercase")
							.type(FunctionType.from(Foo.class).to(Bar.class)));
		}
	}
}
