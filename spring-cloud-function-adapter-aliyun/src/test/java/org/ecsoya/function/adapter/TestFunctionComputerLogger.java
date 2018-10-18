package org.ecsoya.function.adapter;

import com.aliyun.fc.runtime.FunctionComputeLogger;

public class TestFunctionComputerLogger implements FunctionComputeLogger {

	@Override
	public void trace(String string) {
		System.out.println("trace: " + string);
	}

	@Override
	public void debug(String string) {
		System.out.println("debug: " + string);
	}

	@Override
	public void info(String string) {
		System.out.println("info: " + string);
	}

	@Override
	public void warn(String string) {
		System.out.println("warn: " + string);
	}

	@Override
	public void error(String string) {
		System.err.println("err: " + string);
	}

}
