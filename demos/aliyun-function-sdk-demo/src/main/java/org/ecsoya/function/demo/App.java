package org.ecsoya.function.demo;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;

import com.aliyuncs.fc.client.FunctionComputeClient;
import com.aliyuncs.fc.constants.Const;
import com.aliyuncs.fc.model.Code;
import com.aliyuncs.fc.model.FunctionMetadata;
import com.aliyuncs.fc.model.ServiceMetadata;
import com.aliyuncs.fc.request.CreateFunctionRequest;
import com.aliyuncs.fc.request.CreateServiceRequest;
import com.aliyuncs.fc.request.InvokeFunctionRequest;
import com.aliyuncs.fc.request.ListFunctionsRequest;
import com.aliyuncs.fc.request.ListServicesRequest;
import com.aliyuncs.fc.request.UpdateFunctionRequest;
import com.aliyuncs.fc.response.CreateFunctionResponse;
import com.aliyuncs.fc.response.CreateServiceResponse;
import com.aliyuncs.fc.response.InvokeFunctionResponse;
import com.aliyuncs.fc.response.ListFunctionsResponse;
import com.aliyuncs.fc.response.ListServicesResponse;

public class App {
	private static final String ACCESS_KEY = "LTAIAL4EHsgmAsy0";
	private static final String ACCESS_SECRET = "wlHWLUvnRNPlXYdNteuL4WWQC5Jr1z";
	private static final String ACCOUNT_ID = "1752332840987097";
	private static final String REGION = "cn-shanghai";
	private static final String SERVICE_NAME = "spring-cloud-function";
	private static final String FUNCTION_NAME = "demo";

	private static boolean changed = true;

	public static void main(final String[] args) throws IOException {

		// Initialize FC client
		FunctionComputeClient fcClient = new FunctionComputeClient(REGION, ACCOUNT_ID, ACCESS_KEY, ACCESS_SECRET);
		
		// Use the jar with 'fc' suffix.
		File codeJar = new File(
				"../spring-cloud-function-aliyun-demo/target/spring-cloud-function-aliyun-demo-1.0.0-SNAPSHOT-fc.jar");

		ListServicesRequest lsReq = new ListServicesRequest();
		lsReq.setStartKey(SERVICE_NAME);
		ListServicesResponse lsResp = fcClient.listServices(lsReq);
		ServiceMetadata[] services = lsResp.getServices();
		boolean existService = false;
		for (ServiceMetadata serviceMetadata : services) {
			if (SERVICE_NAME.equals(serviceMetadata.getServiceName())) {
				existService = true;
				break;
			}
		}
		if (!existService) {
			// Create a service
			CreateServiceRequest csReq = new CreateServiceRequest();
			csReq.setServiceName(SERVICE_NAME);
			csReq.setDescription("Spring boot");
			CreateServiceResponse csResp = fcClient.createService(csReq);
			System.out.println("Created service, request ID " + csResp.getRequestId());
		}
		// Set to a specific endpoint in case needed, endpoint sample:
		// http://123456.cn-hangzhou.fc.aliyuncs.com
		// fcClient.setEndpoint("http://{accountId}.{regionId}.fc.aliyuncs.com.");

		if (changed) {
			ListFunctionsRequest lfReq = new ListFunctionsRequest(SERVICE_NAME);
			ListFunctionsResponse lfResp = fcClient.listFunctions(lfReq);
			FunctionMetadata[] functions = lfResp.getFunctions();
			boolean exist = false;
			for (FunctionMetadata fc : functions) {
				if (FUNCTION_NAME.equals(fc.getFunctionName())) {
					exist = true;
					break;
				}
			}

			Code code = new Code().setZipFile(Files.readAllBytes(codeJar.toPath()));
			if (exist) {
				UpdateFunctionRequest upf = new UpdateFunctionRequest(SERVICE_NAME, FUNCTION_NAME);
				upf.setCode(code);
				upf.setHandler("org.ecsoya.function.demo.DemoFunction::handleRequest");
				fcClient.updateFunction(upf);
				System.out.println("Updated function, request ID " + FUNCTION_NAME);
			} else {
				// Create a function
				CreateFunctionRequest cfReq = new CreateFunctionRequest(SERVICE_NAME);
				cfReq.setFunctionName(FUNCTION_NAME);
				cfReq.setDescription("Spring Cloud Function Demo for Aliyun.");
				cfReq.setMemorySize(512);
				cfReq.setHandler("org.ecsoya.function.demo.DemoFunction::handleRequest");
				cfReq.setRuntime("java8");

				cfReq.setCode(code);
				cfReq.setTimeout(600);
				CreateFunctionResponse cfResp = fcClient.createFunction(cfReq);
				System.out.println("Created function, request ID " + cfResp.getRequestId());
			}
		}
		// Invoke the function with a string as function event parameter, Sync mode
		InvokeFunctionRequest invkReq = new InvokeFunctionRequest(SERVICE_NAME, FUNCTION_NAME);
		String payload = "{\"value\":\"ecsoya\"}";
		invkReq.setPayload(payload.getBytes());
		InvokeFunctionResponse invkResp = fcClient.invokeFunction(invkReq);
		System.out.println(new String(invkResp.getContent()));

		// Invoke the function, Async mode
		invkReq.setInvocationType(Const.INVOCATION_TYPE_ASYNC);
		invkResp = fcClient.invokeFunction(invkReq);
		if (HttpURLConnection.HTTP_ACCEPTED == invkResp.getStatus()) {
			System.out
					.println("Async invocation has been queued for execution, request ID: " + invkResp.getRequestId());
		} else {
			System.out.println("Async invocation was not accepted");
		}
	}
}
