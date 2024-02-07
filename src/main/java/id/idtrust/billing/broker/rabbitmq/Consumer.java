package id.idtrust.billing.broker.rabbitmq;

import id.idtrust.billing.broker.kafka.DataProduct;
import id.idtrust.billing.broker.kafka.DetailDataProduct;
import id.idtrust.billing.model.Payment;
import id.idtrust.billing.model.Topup;
import id.idtrust.billing.model.TopupProduct;
import id.idtrust.billing.repository.PaymentRepository;
import id.idtrust.billing.repository.TopupProductRepository;
import id.idtrust.billing.repository.TopupRepository;
import id.idtrust.billing.util.DateFormatter;
import id.idtrust.billing.util.LogSystem;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServlet;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@EnableAutoConfiguration
@Component
@Service
public class Consumer extends HttpServlet {
	@Autowired
	TopupRepository topupRepo;

	@Autowired
	KafkaTemplate<String, id.idtrust.billing.broker.kafka.Payment> kafkaPayment;

	@Autowired
	PaymentRepository paymentRepo;

	@Autowired
	TopupProductRepository topupProductRepo;

	@RabbitListener(queues = "${billing.rabbitmq.queue}")
	public void recievedMessage(String message) throws JSONException {
		JSONObject json = null;
		LogSystem.info("ISI JSON " + message);
		try {
			json = new JSONObject(message);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String uuid = UUID.randomUUID().toString();

		LogSystem.info("[RABBITMQ-CONSUMER] [x] Received " + message);

		//Send data
		String[] id = json.getString("id").split("-");

		try {
			Topup topup = topupRepo.findByIds(Long.valueOf(id[1]));

			List<DetailDataProduct> dataProducer = new ArrayList<DetailDataProduct>();
			List<TopupProduct> tp = new ArrayList<TopupProduct>();

			if (topup.getStatus().equalsIgnoreCase("WAITING_PAYMENT")) {
				Payment payments = paymentRepo.ById(topup.getPayment().getId());
				topup.setStatus("VOID");
				payments.setStatus("NO_PAYMENT");

				topupRepo.save(topup);
				paymentRepo.save(payments);

				tp = topupProductRepo.findByTopup(topup.getId());

				for (int i = 0; i < tp.size(); i++) {
					if(!tp.get(i).getProduct().getPkey().equalsIgnoreCase("sms"))
					{
						DetailDataProduct ddp = new DetailDataProduct();
						ddp.setBalance(tp.get(i).getQty());
						ddp.setType_balance(tp.get(i).getProduct().getNameProduct());
						ddp.setType_balance_en(tp.get(i).getProduct().getNameProductEn());
						dataProducer.add(ddp);
					}
				}

				DataProduct dp = new DataProduct();
				if (topup.getPlan() == null) {
					dp.setPackage_id("0");
					dp.setPackage_name("Idtrust Basic");
				} else {
					dp.setPackage_id(topup.getPlan().getId().toString());
					dp.setPackage_name(topup.getPlan().getName());
				}

				DateFormatter dateFormatter = new DateFormatter();

				dp.setPrice(topup.getTotal());
				dp.setTransaction_date(dateFormatter.getTimestamp(topup.getCreatedDate()));
				dp.setPackage_detail(dataProducer);
				dp.setExpired_date(topup.getExpiredDate().toString());

				id.idtrust.billing.broker.kafka.Payment payment = new id.idtrust.billing.broker.kafka.Payment();
				payment.setData(dp);
				payment.setEmail(topup.getAccount().getEmail());
				payment.setName(topup.getAccount().getName());
				payment.setTRACE_ID(uuid);
				payment.setTimestamp(new Date().toInstant().plus(Duration.ofHours(7)).truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", "+07:00"));
				payment.setUser_id(topup.getAccount().getXkey().replaceAll("[^0-9]", ""));

				kafkaPayment.send("cancelled_payment", payment);

//				if (!status) {
//					try {
//						TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
//						bot.execute(new SendMessage(213382980, "Failed send topic pending_cancel: " + uuid));
//					} catch (Exception t) {
//						LogSystem.error(t.toString());
//					}
//				}

				LogSystem.info("[RABBITMQ-CONSUMER] Process done void topup with id " + id[1]);
			}
			else
			{
				LogSystem.info("No data process to void");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}