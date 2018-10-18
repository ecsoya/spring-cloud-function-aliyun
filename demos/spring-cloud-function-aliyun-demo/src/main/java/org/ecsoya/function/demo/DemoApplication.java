package org.ecsoya.function.demo;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties(Properties.class)
public class DemoApplication implements ApplicationContextInitializer<GenericApplicationContext> {

	private Properties props;

	public DemoApplication() {
	}

	public DemoApplication(Properties props) {
		this.props = props;
	}

	@Bean
	public Function<Foo, Bar> function() {
		return value -> new Bar(value.uppercase() + (props.getFoo() != null ? "-" + props.getFoo() : ""));
	}

	@Override
	public void initialize(GenericApplicationContext context) {
		Properties properties = new Properties();
		this.props = properties;
		context.registerBean(Properties.class, () -> properties);
		context.registerBean("function", FunctionRegistration.class,
				() -> new FunctionRegistration<Function<Foo, Bar>>(function())
						.type(FunctionType.from(Foo.class).to(Bar.class).getType()));
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
