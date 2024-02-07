package id.idtrust.billing.controller;

import brave.Tracer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.PDF.Invoices;
import id.idtrust.billing.broker.kafka.DataProduct;
import id.idtrust.billing.broker.kafka.DetailDataProduct;
import id.idtrust.billing.model.*;
import id.idtrust.billing.repository.*;
import id.idtrust.billing.service.TopupService;
import id.idtrust.billing.util.DateFormatter;
import id.idtrust.billing.util.Description;
import id.idtrust.billing.util.LogSystem;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping(value = "/api")
public class Callback extends Description {

    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    AccountRepository AccountsRepo;

    @Autowired
    TopupRepository topupRepository;

    @Autowired
    TopupProductRepository topupProductRepository;

    @Autowired
    TopupService topupService;

    @Autowired
    private Tracer tracer;

    @Autowired
    KafkaTemplate<String, id.idtrust.billing.broker.kafka.Payment> kafkaPayment;

    @Autowired
    BillRepository billRepository;

    @Autowired
    HttpClient httpClient;

    @Autowired
    BatchRepository batchRepository;

    static Description ds = new Description();
    private static final Logger logger = LogManager.getLogger();
    @PostMapping(value="/callback", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> callback(@RequestBody String data, @RequestHeader("User-Agent") String user_agent, @RequestHeader(name="x-callback-token", required = false) String token) throws Exception {

        if(token != null)
        {
            if(!token.equals(X_CALLBACK_TOKEN))
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Access forbidden due invalid token");
                JSONObject response = new JSONObject();

                response.put("message", "Access forbidden due invalid token");
                return ResponseEntity
                        .status(403)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response.toString());
            }
        }

        long startTime = System.nanoTime();
        String invoiceId= UUID.randomUUID().toString();
        String uuid= UUID.randomUUID().toString();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif="Account already exist";

        JSONObject rjson = new JSONObject(data);

        logger.info("["+ds.VERSION+"]-[BILLTRUST/REQUEST] : " +rjson.toString());

        JSONObject jo=new JSONObject();
        JSONObject callback = new JSONObject();
        int amount = 0;

        try {
            Topup topup = new Topup();
            String rawId;
            String source = "";
            Date paid_at;
            
            Date date= null;
            if(rjson.has("paid_at"))
            {
                date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(rjson.getString("paid_at"));
            }
            else
            {
                date = new Date();
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.HOUR_OF_DAY, 7);

            if (rjson.has("data"))
            {
                amount = rjson.getJSONObject("data").getInt("amount");
                rawId = rjson.getJSONObject("data").getString("reference_id");
            }
            else
            {
                if(rjson.has("bank_code"))
                {
                    callback.put("bank_code", rjson.getString("bank_code"));
                    source = rjson.getString("bank_code") + " - " + rjson.getString("payment_destination");
                }
                else
                {
                    if(rjson.has("payment_channel"))
                    {
                        source = rjson.getString("payment_channel");
                    }

                    if(rjson.has("payment_details"))
                    {
                        source = rjson.getString("payment_channel") + " - " + rjson.getJSONObject("payment_details").getString("source");
                    }
                }
                amount = rjson.getInt("amount");
                rawId = rjson.getString("external_id");
            }

            callback.put("id", rawId);
            callback.put("amount", amount);
            callback.put("payment_status", "PAID");

            String transactionid = "";

            if(rjson.has("data"))
            {
                if(rjson.getJSONObject("data").has("payment_detail"))
                {
                    transactionid = rjson.getJSONObject("data").getJSONObject("payment_detail").getString("receipt_id");
                    source = rjson.getJSONObject("data").getJSONObject("payment_detail").getString("source");
                }
            }
            else
            {
                if(rjson.has("id"))
                {
                    transactionid = rjson.getString("id");
                }
            }

            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"ID Transaction " + rawId);

            callback.put("transaction_id", transactionid);

            JSONObject response = new JSONObject();

            if(rawId.split("-")[1].equalsIgnoreCase("postpaid"))
            {
                //process postpaid user payment to bill
                int id = Integer.parseInt(rawId.split("-")[2]);

                Bill bill = billRepository.findById(id);

                if(bill == null)
                {
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : No Bill Available");
                    response.put("status", false);

                    return ResponseEntity
                            .status(404)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response.toString());
                }

                bill.setStatus("PAID");

                Invoice invoice = new Invoice();

                invoice.setAccount(bill.getAccount());
                invoice.setDescription("Postpaid payment success");
                invoice.setBatch(bill.getBatch());
//                invoice.setType("PAYMENT");
                invoice.setCreatedDate(new Date());
                invoice.setTrx(4);
                invoice.setAmount(bill.getTotal());
                invoice.setInvoiceId(UUID.randomUUID().toString());

                topupService.setInv(invoice);
                topupService.setBill(bill);
                int status=200;
                if(topupService.postpaidPayment())
                {
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : Success process payment");
                    response.put("status", true);
                }
                else
                {
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : Failed process payment");
                    response.put("status", false);
                    status=500;
                }

                return ResponseEntity
                        .status(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response.toString());
            }
            else
            {
                long id = Long.parseLong(rawId.split("-")[1]);

                topup = topupRepository.findByIds(id);

                if(topup == null)
                {
                    response.put("status", false);
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"No data topup");
                    return ResponseEntity
                            .status(404)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response.toString());
                }
                else
                {
                    response.put("status", true);

                    if(topup.getStatus().equalsIgnoreCase("TOPUP_SUCCESS"))
                    {
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Transaction was success at " + topup.getPayment().getPaymentDate());
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(response.toString());
                    }
                }

                if(topup.getPlan() != null)
                {
                    if(topup.getPlan().getType().equalsIgnoreCase("starter"))
                    {
                        //hit to FE first payment
                        JSONObject payload = new JSONObject();

                        String xkey_without_letter = topup.getAccount().getXkey();

                        payload.put("userid", xkey_without_letter);
                        payload.put("transaction_date", new Date().toInstant());
                        payload.put("transaction_id", transactionid);

                        String json = payload.toString();

                        LogSystem.info("First Payment Payload " + json);

                        HttpPost httpPost = new HttpPost(USER_SERVICE);
                        httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
                        httpPost.setHeader("X-B3-TraceId", tracer.currentSpan().toString());

                        JSONObject resp;

                        HttpResponse respo = null;
                        try {
                            StringEntity stringEntity = new StringEntity(json);
                            httpPost.setEntity(stringEntity);

                            respo = httpClient.execute(httpPost);

                            HttpEntity entity = respo.getEntity();
                            InputStream inputStream = entity.getContent();
                            ObjectMapper mapper = new ObjectMapper();
                            Map<String, String> jsonMap = mapper.readValue(inputStream, Map.class);
                            String jsonString = new ObjectMapper().writeValueAsString(jsonMap);

                            resp = new JSONObject(jsonString);
                            LogSystem.info("First Payment Response : " + resp);
                            resp.put("code", respo.getStatusLine().getStatusCode());

                        } catch (JSONException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                List<TopupProduct> tp = topupProductRepository.findByTopup(topup.getId());

                Account account = AccountsRepo.findByIds(topup.getAccount().getId());
                Date now = new Date();

                Payment payment = paymentRepository.ById(topup.getPayment().getId());

                ArrayList<JSONObject> dataProduct = new ArrayList<JSONObject>();

                List<DetailDataProduct> dataProducer = new ArrayList<DetailDataProduct>();

                List<Batch> batchList = new ArrayList<Batch>();
                List<Invoice> invoiceList = new ArrayList<Invoice>();

                for (int i = 0 ; i < tp.size() ; i++)
                {
                    Batch batch = new Batch();
                    batch.setAccount(tp.get(i).getAccount());
                    batch.setOpenDate(now);
                    batch.setNameBatch("PRE_" + tp.get(i).getAccount().getXkey() + "_" + tp.get(i).getProduct().getPkey());
                    batch.setSettled(false);
                    batch.setQuota(tp.get(i).getQty());
                    batch.setPrice(tp.get(i).getPrice());

                    //Menambahkan satu tahun untuk masa kadaluarsa saldo
                    Calendar c = Calendar.getInstance();
                    c.setTime(new Date());
                    c.add(Calendar.YEAR, 1);
                    batch.setExpired(c.getTime());

                    batch.setRemainingBalance(tp.get(i).getQty());
                    batch.setUsage(0);

                    batchList.add(batch);

                    //query batch jika yang sudah ada masih terbuka expired diupdate ke waktu terbaru
                    List<Batch> batchExisting = batchRepository.findBatchByListAccount(tp.get(i).getAccount().getId());

                    if(batchExisting.size() > 0)
                    {
                        for (int j = 0; j < batchExisting.size(); j++)
                        {
                            batchExisting.get(j).setExpired(c.getTime());
                            batchList.add(batchExisting.get(j));
                        }
                    }

                    Invoice invoice = new Invoice();

                    invoice.setAccount(tp.get(i).getAccount());
                    invoice.setCreatedDate(now);
                    invoice.setInvoiceId(invoiceId);
//                    invoice.setType("CREDIT_ADJ");
                    invoice.setAmount(tp.get(i).getQty());
                    invoice.setDescription("Topup Balance with Payment");
                    invoice.setTrx(1);
                    invoice.setBatch(batch);
                    invoice.setTopup(topup);

                    if(topup.getPlan() != null)
                    {
                        invoice.setPlan(topup.getPlan());
                    }

                    invoiceList.add(invoice);

                    JSONObject objectProduct = new JSONObject();

                    objectProduct.put("product", tp.get(i).getProduct().getPkey());
                    objectProduct.put("qty", tp.get(i).getQty());

                    dataProduct.add(objectProduct);

                    DetailDataProduct ddp = new DetailDataProduct();
                    ddp.setBalance(tp.get(i).getQty());
                    ddp.setType_balance(tp.get(i).getProduct().getNameProduct());
                    ddp.setType_balance_en(tp.get(i).getProduct().getNameProductEn());
                    if(!tp.get(i).getProduct().getPkey().equalsIgnoreCase("sms"))
                    {
                        dataProducer.add(ddp);
                    }
                }

                topup.setStatus("TOPUP_SUCCESS");
                payment.setPaymentDate(calendar.getTime());
                payment.setStatus("PAID");
                payment.setTransactionId(transactionid);
                payment.setSource(source);

                topupService.setBatch(batchList);
                topupService.setInvoice(invoiceList);
                topupService.setPayment(payment);

                topupService.setTopup(topup);



//                //Creating pdf invoice
//                Invoices invoices = new Invoices();
//                String docInvoice = invoices.generateBisnis(tp, to, invoiceDate);
//
//                InvoiceFiles invoiceFiles = new InvoiceFiles();
//                invoiceFiles.setPath(invoices.getInvoicePath());
//                invoiceFiles.setTopup(topup);
//
//                topupService.setInvoiceFiles(invoiceFiles);

                topupService.topupCallback();

                DataProduct dp = new DataProduct();
                if(topup.getPlan() != null) {
                    dp.setPackage_id(topup.getPlan().getId().toString());
                    dp.setPackage_name(topup.getPlan().getName());
                }

                dp.setPayment_method(source);
                dp.setPrice(topup.getTotal());

                DateFormatter dateFormatter = new DateFormatter();

                dp.setTransaction_date(dateFormatter.getTimestamp(payment.getPaymentDate()));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"date " + dp.getTransaction_date());
                dp.setPackage_detail(dataProducer);
                dp.setTransaction_id(transactionid);
                dp.setTransaction_user_agent(user_agent);
                dp.setTransaction_ip(InetAddress.getLocalHost().getHostAddress());

                callback.put("topup_status", topup.getStatus());

                try {
                    //send to message broker
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Send to kafka");

                    id.idtrust.billing.broker.kafka.Payment Producer = new id.idtrust.billing.broker.kafka.Payment();
                    Producer.setData(dp);
                    Producer.setEmail(account.getEmail());
                    Producer.setName(account.getName());
                    Producer.setTRACE_ID(uuid);
                    Producer.setTimestamp(new Date().toInstant().plus(Duration.ofHours(7)).truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", "+07:00"));
                    Producer.setUser_id(topup.getAccount().getXkey().replaceAll("[^0-9]", ""));

                    kafkaPayment.send("success_payment", Producer);

                }catch(Exception e)
                {
                    e.printStackTrace();
                    logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Failed send to message broker");
                }

                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"All processes have been completed " + formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +response);

                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response.toString());
            }
        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +e);
            jo.put("status", false);

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Failed process callback payment : " + uuid));
            }catch(Exception t)
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            }

            return ResponseEntity
                    .status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }
}