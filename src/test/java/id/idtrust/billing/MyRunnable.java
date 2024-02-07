package id.idtrust.billing;

import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class MyRunnable implements Runnable{

	@Test
	void threadTest()
	{
		int id;
		int numberOfTasks = 10;
		ExecutorService executor= Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		try{
			for ( int i=0; i < numberOfTasks; i++){
				executor.execute(new MyRunnable(i));
			}
		}catch(Exception err){
			err.printStackTrace();
		}
		executor.shutdown(); // once you are done with ExecutorService
	}

	int id;
	public MyRunnable(int i){
		this.id = i;
	}
	public void run(){
		try{

			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
						}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
						}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};

			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();
			builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
			builder.hostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});

			OkHttpClient client = builder.build();

			MediaType mediaType = MediaType.parse("application/json");
			RequestBody body = RequestBody.create(mediaType, "{\n\t\"xkey\": \"ID10\",\n\t\"product\": \"sign\",\n\t\"amount\": \"1\",\n\t\"description\": \"Transaksi tandatangan\"\n}");
			Request request = new Request.Builder()
					.url("https://idtrust-1020848017.ap-southeast-3.elb.amazonaws.com/apigw/public/billing/transaction")
					.method("POST", body)
					.addHeader("X-Amzn-Trace-Id", "1-63d21121-18959e163c80fe616fb0f704,Root=1-63d21121-18959e163c80fe616fb0f704;Parent=1-63d103a0-606d21ab31c6d4a66de5b3d1")
					.addHeader("X-Forwarded-For", "0.0.0.0")
					.addHeader("Authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ7XCJ1c2VybmFtZVwiOlwieDF0ZXN0XCIsXCJuYW1lXCI6XCJOdXIgQXNpYWhcIixcInVzZXJfaWRcIjpcIjYzYzY3MjM5YTQyZjc0MTU3NGY2Zjg0Y1wiLFwiZW1haWxcIjpcIngxdGVzdEBpZHRydXN0LmlkXCJ9IiwiZXhwIjoxNjc1ODYwMjAwLCJpYXQiOjE2NzU4NDIyMDB9.OawgQEn3Ugog1aqQKHKcHdDMi41l3YvEsTDSuKJlxLdTXRhkq5rQXRogZBqDzqinhF4nd_sEd3e24krSiKG8rg")
					.addHeader("Content-Type", "application/json")
					.build();
			Response response = client.newCall(request).execute();

			System.out.println(response.message());
		}catch(Exception err){
			err.printStackTrace();
		}
	}
}