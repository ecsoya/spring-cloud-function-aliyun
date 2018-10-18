package org.ecsoya.function.adapter;

import com.aliyun.fc.runtime.FunctionParam;

public class TestFunctionParam implements FunctionParam {

	private String name;

	public TestFunctionParam(String name) {
		this.name = name;
	}

	@Override
	public String getFunctionName() {
		return name;
	}

	@Override
	public String getFunctionHandler() {
		return null;
	}

	@Override
	public int getExecTimeLimitInMillis() {
		return 0;
	}

	@Override
	public int getMemoryLimitInMB() {
		return 0;
	}

}
