package id.idtrust.billing.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import id.idtrust.billing.util.Description;
import id.idtrust.billing.util.LogSystem;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;



@Service
public class Xendit extends Description {

	@Autowired
	HttpClient httpClient;
	private static final Logger logger = LogManager.getLogger();
	
	public JSONObject paymentURL(JSONArray methods, String id, int amount, Date payment_expired, String description, String name, String email, String pnumber) throws Exception
	{

		Description ds = new Description();
		try {

			 String query = XENDIT_URL+"v2/invoices";

			 JSONObject payload = new JSONObject();
			 JSONObject customer = new JSONObject();
			 
			 payload.put("external_id", id);
			 payload.put("amount", amount);
			 payload.put("description", description);
			 payload.put("payment_methods", methods);
			 payload.put("invoice_duration", 3600);
//			 payload.put("success_redirect_url", "https://idtrust.id");
//			 payload.put("items", items);
			 
			 customer.put("given_names", name);
			 customer.put("email", email);
			 customer.put("mobile_number", pnumber);
			 
			 String json = payload.toString();
			logger.info("["+ds.VERSION+"]-[BILLTRUST/REQUEST] : Basic " + XENDIT_CREDENTIAL + " Xendit payload " + json);


			HttpPost httpPost = new HttpPost(query);
			httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
			httpPost.setHeader("Authorization", "Basic "+XENDIT_CREDENTIAL);
			httpPost.setHeader("api-version", "2022-07-31");

			StringEntity stringEntity = new StringEntity(json);
			httpPost.setEntity(stringEntity);
			final RequestConfig params = RequestConfig.custom().setConnectTimeout(40000).setSocketTimeout(40000).setConnectionRequestTimeout(40000).build();

			httpPost.setConfig(params);
			HttpResponse response = httpClient.execute(httpPost);
			JSONObject resp;

			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			ObjectMapper mapper = new ObjectMapper();
			Map jsonMap = mapper.readValue(inputStream, Map.class);
			String jsonString = new ObjectMapper().writeValueAsString(jsonMap);

			resp = new JSONObject(jsonString);
			LogSystem.info("Xendit Response : " + resp);
			logger.info("["+ds.VERSION+"]-[BILLTRUST/REQUEST] : Xendit Response : " + resp);
			resp.put("code", response.getStatusLine().getStatusCode());

			EntityUtils.consume(entity);

			return resp;
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new Exception(e.toString());
		}
	}

}