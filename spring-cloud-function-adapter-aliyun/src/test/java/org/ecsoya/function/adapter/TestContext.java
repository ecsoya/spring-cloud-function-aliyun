/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ecsoya.function.adapter;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.Credentials;
import com.aliyun.fc.runtime.FunctionComputeLogger;
import com.aliyun.fc.runtime.FunctionParam;

public class TestContext implements Context {

	private final FunctionComputeLogger logger = new TestFunctionComputerLogger();
	private final FunctionParam functionParam;

	public TestContext(String name) {
		functionParam = new TestFunctionParam(name);
	}

	@Override
	public String getRequestId() {
		return null;
	}

	@Override
	public Credentials getExecutionCredentials() {
		return null;
	}

	@Override
	public FunctionParam getFunctionParam() {
		return functionParam;
	}

	@Override
	public FunctionComputeLogger getLogger() {
		return logger;
	}
}