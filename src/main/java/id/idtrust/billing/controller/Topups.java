package id.idtrust.billing.controller;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.xml.bind.DatatypeConverter;
import java.io.*;

@RestController
@CrossOrigin
@RequestMapping(value = "/billing")
@EnableAutoConfiguration
public class Topups extends Description {

    @Autowired
    BusinessPriceRepository businessPriceRepository;

    @Autowired
    BatchRepository batchRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    TopupRepository topupRepository;
    @Autowired
    TopupProductRepository topupProductRepository;
    @Autowired
    AccountRepository AccountsRepo;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    PlanRepository planRepository;

    @Autowired
    PlanDetailRepository planDetailRepository;

    @Autowired
    TopupService topupService;

    @Autowired
    KafkaTemplate<String, id.idtrust.billing.broker.kafka.Payment> kafkaPayment;

    @Autowired
    RabbitTemplate rabbitTemplate;

    DateFormatter dateFormatter = new DateFormatter();
    private static final Logger logger = LogManager.getLogger("id.idtrust");

    @PostMapping(value="/topup/get", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> get(@RequestBody String data) throws Exception {
        long startTime = System.nanoTime();

        InetAddress inetAddress = null;

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);
        JSONObject jo = new JSONObject();
        String notif="Success";

        try {
            Page<Topup> pageSuccess = null;
            Page<Topup> pageWaiting = null;
            Page<Topup> pageFailed = null;

            List<Account> account = new ArrayList<Account>();
            List<Topup> topupSuccess = new ArrayList<Topup>();
            List<Topup> topupFailed = new ArrayList<Topup>();
            List<Topup> topupWaiting = new ArrayList<Topup>();

            account = AccountsRepo.findSingleAccount(rjson.getString("xkey"));

            if(account.size() < 1)
            {
                notif="Account not exist";
                jo.put("result_code", "B04");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
                return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
            }

            JSONArray listTopup = new JSONArray();
            JSONArray listTopupSuccess = new JSONArray();
            JSONArray listVoidTopupSuccess = new JSONArray();

            List<TopupProduct> tp = new ArrayList<TopupProduct>();

            Topup topupAccount;

            int successPage=0;
            int successSize=10;

            int failedPage=0;
            int failedSize=10;

            int waitingPage=0;
            int waitingSize=10;

            if(rjson.has("paging"))
            {
                if(rjson.getJSONObject("paging").has("success"))
                {
                    JSONObject pagingSuccess = rjson.getJSONObject("paging").getJSONObject("success");
                    successPage = pagingSuccess.getInt("page") - 1;
                    successSize = pagingSuccess.getInt("size");
                }

                if(rjson.getJSONObject("paging").has("failed"))
                {
                    JSONObject pagingFailed = rjson.getJSONObject("paging").getJSONObject("failed");
                    failedPage = pagingFailed.getInt("page") - 1;
                    failedSize = pagingFailed.getInt("size");
                }

                if(rjson.getJSONObject("paging").has("waiting"))
                {
                    JSONObject pagingWaiting = rjson.getJSONObject("paging").getJSONObject("waiting");
                    waitingPage = pagingWaiting.getInt("page") - 1;
                    waitingSize = pagingWaiting.getInt("size");
                }
            }

//            for(int i=0 ; i < account.size() ; i++)
//            {
                if(rjson.has("type") && rjson.getString("type").equals("emeterai"))
                {
                    if(successSize > 0) {
                        pageSuccess = topupRepository.findByAccountEmeteraiPaging(rjson.getString("xkey"), "TOPUP_SUCCESS", PageRequest.of(successPage, successSize));
                        if (pageSuccess.getTotalElements() != 0) {
                            topupSuccess = pageSuccess.getContent();
                        }
                    }

                    if(waitingSize > 0) {
                        pageWaiting = topupRepository.findByAccountEmeteraiPaging(rjson.getString("xkey"), "WAITING_PAYMENT", PageRequest.of(waitingPage, waitingSize));
                        if (pageWaiting.getTotalElements() != 0) {
                            topupWaiting = pageWaiting.getContent();
                        }
                    }

                    if(failedSize > 0) {
                        pageFailed = topupRepository.findByAccountEmeteraiPaging(rjson.getString("xkey"), "VOID", PageRequest.of(failedPage, failedSize));
                        if (pageFailed.getTotalElements() != 0) {
                            topupFailed = pageFailed.getContent();
                        }
                    }
                }
                else
                {
                    if(successSize > 0) {
                        pageSuccess = topupRepository.findByAccountPaging(rjson.getString("xkey"), "TOPUP_SUCCESS", PageRequest.of(successPage, successSize));
                        if (pageSuccess.getTotalElements() != 0) {
                            topupSuccess = pageSuccess.getContent();
                        }
                    }

                    if(waitingSize > 0) {
                        pageWaiting = topupRepository.findByAccountPaging(rjson.getString("xkey"), "WAITING_PAYMENT", PageRequest.of(waitingPage, waitingSize));
                        if (pageWaiting.getTotalElements() != 0) {
                            topupWaiting = pageWaiting.getContent();
                        }
                    }

                    if(failedSize > 0) {
                        pageFailed = topupRepository.findByAccountPaging(rjson.getString("xkey"), "VOID", PageRequest.of(failedPage, failedSize));
                        if (pageFailed.getTotalElements() != 0) {
                            topupFailed = pageFailed.getContent();
                        }
                    }
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                sdf.getCalendar().add(Calendar.HOUR, 7);

                if(topupSuccess.size() > 0)
                {
                    for(int j = 0 ; j < topupSuccess.size() ; j++)
                    {
//                        //request jika topup starter tidak dimunculkan
//                        if(!topup.get(j).getPlan().getType().equalsIgnoreCase("starter"))
//                        {

                            topupAccount = topupRepository.findByIds(topupSuccess.get(j).getId());

                            JSONObject objectTopup = new JSONObject();
                            objectTopup.put("id", topupAccount.getId());
                            objectTopup.put("plan_name", topupAccount.getPlan().getName());
                            objectTopup.put("topup_status", topupAccount.getStatus());
                            objectTopup.put("expired_at", sdf.format(topupAccount.getExpiredDate()));
                            objectTopup.put("price", topupAccount.getPrice());
                            objectTopup.put("payment_method", topupAccount.getPayment().getChannel().getChannelCode());
                            objectTopup.put("payment_code", topupAccount.getPayment().getPaymentCode());
                            objectTopup.put("payment_status", topupAccount.getPayment().getStatus());

                            tp = topupProductRepository.findByTopup(topupAccount.getId());

                            JSONArray listProduct = new JSONArray();

                            for (int k = 0; k < tp.size(); k++)
                            {
                                if(!tp.get(k).getProduct().getPkey().equalsIgnoreCase("sms"))
                                {
                                    JSONObject topupProduct = new JSONObject();

                                    topupProduct.put("name", tp.get(k).getProduct().getNameProduct());
                                    topupProduct.put("quantity", tp.get(k).getQty());

                                    topupProduct.put("title_id", tp.get(k).getProduct().getNameProduct());
                                    topupProduct.put("title_en", tp.get(k).getProduct().getNameProductEn());
                                    topupProduct.put("detail_id", tp.get(k).getProduct().getDetailId());
                                    topupProduct.put("detail_en", tp.get(k).getProduct().getDetailEn());

                                    listProduct.put(topupProduct);

                                    objectTopup.put("product_detail", listProduct);
                                }
                            }

                            objectTopup.put("payment_date", sdf.format(topupAccount.getPayment().getPaymentDate()));
                            objectTopup.remove("payment_code");
                            listTopupSuccess.put(objectTopup);
//                        }
                    }
                }

                if(topupWaiting.size() > 0)
                {
                    for(int j = 0 ; j < topupWaiting.size() ; j++)
                    {

                        topupAccount = topupRepository.findByIds(topupWaiting.get(j).getId());
                        JSONObject objectTopup = new JSONObject();
                        objectTopup.put("id", topupAccount.getId());
                        objectTopup.put("plan_name", topupAccount.getPlan().getName());
                        objectTopup.put("topup_status", topupAccount.getStatus());
                        objectTopup.put("expired_at", sdf.format(topupAccount.getExpiredDate()));
                        objectTopup.put("price", topupAccount.getPrice());
                        objectTopup.put("payment_method", topupAccount.getPayment().getChannel().getChannelCode());
                        objectTopup.put("payment_code", topupAccount.getPayment().getPaymentCode());
                        objectTopup.put("payment_status", topupAccount.getPayment().getStatus());

                        tp = topupProductRepository.findByTopup(topupAccount.getId());

                        JSONArray listProduct = new JSONArray();

                        for (int k = 0; k < tp.size(); k++)
                        {
                            if(!tp.get(k).getProduct().getPkey().equalsIgnoreCase("sms")) {
                                JSONObject topupProduct = new JSONObject();

                                topupProduct.put("name", tp.get(k).getProduct().getNameProduct());
                                topupProduct.put("quantity", tp.get(k).getQty());

                                topupProduct.put("title_id", tp.get(k).getProduct().getNameProduct());
                                topupProduct.put("title_en", tp.get(k).getProduct().getNameProductEn());
                                topupProduct.put("detail_id", tp.get(k).getProduct().getDetailId());
                                topupProduct.put("detail_en", tp.get(k).getProduct().getDetailEn());

                                listProduct.put(topupProduct);

                                objectTopup.put("product_detail", listProduct);
                            }
                        }

                        listTopup.put(objectTopup);

                    }
                }

                if(topupFailed.size() > 0)
                {
                    for(int j = 0 ; j < topupFailed.size() ; j++)
                    {
                        topupAccount = topupRepository.findByIds(topupFailed.get(j).getId());

                        JSONObject objectTopup = new JSONObject();
                        objectTopup.put("id", topupAccount.getId());
                        objectTopup.put("plan_name", topupAccount.getPlan().getName());
                        objectTopup.put("topup_status", topupAccount.getStatus());
                        objectTopup.put("expired_at", sdf.format(topupAccount.getExpiredDate()));
                        objectTopup.put("price", topupAccount.getPrice());
                        objectTopup.put("payment_method", topupAccount.getPayment().getChannel().getChannelCode());
                        objectTopup.put("payment_code", topupAccount.getPayment().getPaymentCode());
                        objectTopup.put("payment_status", topupAccount.getPayment().getStatus());

                        tp = topupProductRepository.findByTopup(topupAccount.getId());

                        JSONArray listProduct = new JSONArray();

                        for (int k = 0; k < tp.size(); k++)
                        {
                            if(!tp.get(k).getProduct().getPkey().equalsIgnoreCase("sms"))
                            {
                                JSONObject topupProduct = new JSONObject();

                                topupProduct.put("name", tp.get(k).getProduct().getNameProduct());
                                topupProduct.put("quantity", tp.get(k).getQty());

                                topupProduct.put("title_id", tp.get(k).getProduct().getNameProduct());
                                topupProduct.put("title_en", tp.get(k).getProduct().getNameProductEn());
                                topupProduct.put("detail_id", tp.get(k).getProduct().getDetailId());
                                topupProduct.put("detail_en", tp.get(k).getProduct().getDetailEn());

                                listProduct.put(topupProduct);

                                objectTopup.put("product_detail", listProduct);
                            }
                        }

                        objectTopup.remove("payment_code");
                        listVoidTopupSuccess.put(objectTopup);

                    }
                }
//            }

            JSONObject payment = new JSONObject();

            JSONObject dataPayment = new JSONObject();
            dataPayment.put("data", listTopup);
            if(pageWaiting != null) {
                dataPayment.put("totalPages", pageWaiting.getTotalPages());
                dataPayment.put("totalElements", pageWaiting.getTotalElements());
            }


            JSONObject dataSuccess = new JSONObject();
            dataSuccess.put("data", listTopupSuccess);
            if(pageSuccess != null) {
                dataSuccess.put("totalPages", pageSuccess.getTotalPages());
                dataSuccess.put("totalElements", pageSuccess.getTotalElements());
            }

            JSONObject dataFailed = new JSONObject();
            dataFailed.put("data", listVoidTopupSuccess);
            if(pageFailed != null) {
                dataFailed.put("totalPages", pageFailed.getTotalPages());
                dataFailed.put("totalElements", pageFailed.getTotalElements());
            }

            payment.put("need_payment", dataPayment);
            payment.put("success", dataSuccess);
            payment.put("failed", dataFailed);

            jo.put("data", payment);
            jo.put("result_code", "B00");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());

        }catch(Exception e)
        {
            e.printStackTrace();
            notif="Error Get List Topup";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error topup : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +e);
            }

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/topup/add", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> add(@RequestBody String data, @RequestHeader("User-Agent") String user_agent) throws Exception {

        String invoiceId= UUID.randomUUID().toString();
        long startTime = System.nanoTime();
        Date now = new Date();

        InetAddress inetAddress = null;

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);
        JSONObject jo=new JSONObject();

        String notif="Error topup";

        try {
            List<Product> dataTenant = productRepository.findAll();
            long hour = 3600*1000;

            ArrayList<String> listtenant = new ArrayList<String>();

            if(!rjson.has("data"))
            {
                notif="Missing parameter JSONArray data";
                jo.put("result_code", "B28");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }
            else
            {
                JSONArray arraytopup = null;
                JSONArray arraydatasuccess = new JSONArray();

                try
                {
                    arraytopup=(JSONArray) rjson.getJSONArray("data");
                }catch(Exception e)
                {
                    notif="data not JSONArray";
                    jo.put("result_code", "B28");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
//
//                    if(rjson.getBoolean("generate_payment"))
//                    {
//                        String name="";
//                        String pnumber="";
//                        String email="";
//
//                        int totalPrice = 0;
//                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"LENGTH "+arraytopup.length());
//                        String code = "";
//                        Channel channelData = null;
//                        Topup topup = new Topup();
//
//                        JSONArray resume = new JSONArray();
//
//                        Calendar c = Calendar.getInstance();
//                        c.setTime(now);
//                        c.add(Calendar.HOUR, 1);
//
//                        String Payer ="";
//                        JSONObject obj=null;
//                        List<DetailDataProduct> dataProducer = new ArrayList<DetailDataProduct>();
//
//                        for(int i=0; i<arraytopup.length(); i++)
//                        {
//                            long startTimeBatch = System.nanoTime();
//
//                            obj=(JSONObject) arraytopup.get(i);
//
//                            if(!obj.has("xkey"))
//                            {
//                                notif="Missing parameter xkey on JSONArray";
//                                jo.put("result_code", "B28");
//                                jo.put("message", notif);
//                                jo.put("log", uuid);
//                                jo.put("timestamp", dateFormatter.getTimestamp());
//                                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                return ResponseEntity
//                                    .status(200)
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .body(jo.toString());
//                            }
//                            if(!obj.has("product"))
//                            {
//                                notif="Missing parameter product on JSONArray";
//                                jo.put("result_code", "B28");
//                                jo.put("message", notif);
//                                jo.put("log", uuid);
//                                jo.put("timestamp", dateFormatter.getTimestamp());
//                                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                return ResponseEntity
//                                    .status(200)
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .body(jo.toString());
//                            }
//                            if(!obj.has("amount"))
//                            {
//                                notif="Missing parameter amount on JSONArray";
//                                jo.put("result_code", "B28");
//                                jo.put("message", notif);
//                                jo.put("log", uuid);
//                                jo.put("timestamp", dateFormatter.getTimestamp());
//                                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                return ResponseEntity
//                                        .status(200)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .body(jo.toString());
//                            }
//                            if(!obj.has("price"))
//                            {
//                                notif="Missing price on JSONArray";
//                                jo.put("result_code", "B28");
//                                jo.put("message", notif);
//                                jo.put("log", uuid);
//                                jo.put("timestamp", dateFormatter.getTimestamp());
//                                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                return ResponseEntity
//                                        .status(200)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .body(jo.toString());
//                            }
//
//                            Product tenantApiKey = productRepository.findByPkey(obj.getString("product"));
//
//                            if(tenantApiKey == null)
//                            {
//                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Product not exist");
//                                notif="Product available : " + listtenant;
//                                jo.put("result_code", "B01");
//                                jo.put("message", notif);
//                                jo.put("log", uuid);
//                                jo.put("timestamp", dateFormatter.getTimestamp());
//                                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                return ResponseEntity
//                                    .status(200)
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .body(jo.toString());
//                            }
//
//                            Account account = AccountsRepo.findByExternalKey(obj.getString("xkey"), tenantApiKey.getId());
//                            if(account == null)
//                            {
//                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Account not exist");
//                                notif="Account not exist";
//                                jo.put("result_code", "B01");
//                                jo.put("message", notif);
//                                jo.put("log", uuid);
//                                jo.put("timestamp", dateFormatter.getTimestamp());
//                                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                return ResponseEntity
//                                    .status(200)
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .body(jo.toString());
//                            }
//                            else
//                            {
//
//                                try {
//                                    JSONObject payment = new JSONObject(rjson.getString("payment"));
//
//                                    name=payment.getString("name");
//                                    pnumber=payment.getString("mobile_number");
//                                    email=payment.getString("email");
//
//                                    String channel = payment.getString("channel");
//                                    Payer = payment.getString("name");
//
//                                    channelData = new Channel();
//
//                                    try {
//                                        channelData = channelRepository.findChannel(channel);
//                                    }catch(Exception e)
//                                    {
//                                        notif="Failed access database";
//                                        jo.put("result_code", "06");
//                                        jo.put("message", notif);
//                                        jo.put("error", e.toString());
//                                        jo.put("log", uuid);
//                                        jo.put("timestamp", dateFormatter.getTimestamp());
//                                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                        logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                        return ResponseEntity
//                                            .status(200)
//                                            .contentType(MediaType.APPLICATION_JSON)
//                                            .body(jo.toString());
//                                    }
//
//                                    if (channelData == null)
//                                    {
//                                        notif="Metode Pembayaran tidak ditemukan";
//                                        jo.put("result_code", "B14");
//                                        jo.put("message", notif);
//                                        jo.put("log", uuid);
//                                        jo.put("timestamp", dateFormatter.getTimestamp());
//                                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                        return ResponseEntity
//                                            .status(200)
//                                            .contentType(MediaType.APPLICATION_JSON)
//                                            .body(jo.toString());
//                                    }
//
//                                    totalPrice += obj.getInt("price");
////                                    topup.setPrice(totalPayment);
//                                    topup.setCreatedDate(now.toInstant());
//                                    topup.setStatus("WAITING_PAYMENT");
//                                    topup.setAccount(account);
//
//                                    topup.setExpiredDate(c.getTime().toInstant());
//
//                                    topupRepository.save(topup);
//
//
//                                    TopupProduct pt = new TopupProduct();
//
//                                    Product product = productRepository.findByPkey(obj.getString("product"));
//
//                                    if (product == null)
//                                    {
//                                        notif="Product not exist";
//                                        jo.put("result_code", "B01");
//                                        jo.put("message", notif);
//                                        jo.put("log", uuid);
//                                        jo.put("timestamp", dateFormatter.getTimestamp());
//                                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                        return ResponseEntity
//                                            .status(200)
//                                            .contentType(MediaType.APPLICATION_JSON)
//                                            .body(jo.toString());
//                                    }
//
//                                    pt.setProduct(product);
//                                    pt.setQty(obj.getInt("amount"));
//                                    pt.setTopup(topup);
//                                    pt.setPrice(obj.getInt("price"));
//                                    pt.setAccount(account);
//
//                                    JSONObject datasuccess = new JSONObject();
//                                    datasuccess.put("name", pt.getProduct().getNameProduct());
//                                    datasuccess.put("qty", obj.getInt("amount"));
//                                    datasuccess.put("price", obj.getInt("price"));
//
//                                    resume.put(datasuccess);
//
//                                    DetailDataProduct ddp = new DetailDataProduct();
//                                    ddp.setBalance(pt.getQty());
//                                    ddp.setType_balance(pt.getProduct().getNameProduct());
//                                    ddp.setType_balance_en(pt.getProduct().getNameProductEn());
//
//                                    dataProducer.add(ddp);
//
//                                    topupProductRepository.save(pt);
//
//
//                                }
//                                catch(Exception e)
//                                {
//                                    e.printStackTrace();
//                                    logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
//
//                                    notif="Failed topup balance";
//                                    jo.put("result_code", "B06");
//                                    jo.put("message", notif);
//                                    jo.put("error", e.toString());
//                                    jo.put("log", uuid);
//                                    jo.put("timestamp", dateFormatter.getTimestamp());
//                                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                                    jo.put("product", obj.getString("product"));
//                                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                                    return ResponseEntity
//                                        .status(200)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .body(jo.toString());
//                                }
//                            }
//                        }
//
//
//                        Payment pay = new Payment();
//                        pay.setPaymentCode(code);
//                        pay.setChannel(channelData);
//                        pay.setStatus("WAITING_PAYMENT");
//
//                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Resume topup : " + arraytopup.toString() + " with total price " + totalPrice);
//
//                        //Request to xendit
//
//                        JSONObject respXendit = new JSONObject();
//                        String id = "";
//
//                        JSONArray methods = new JSONArray();
//
//                        methods.put(channelData.getChannelCode());
//
//                        double totalPayment=0;
//
//                        totalPayment = totalPrice;
//
//                        String description = "Topup idtrust";
//                        id = "IDTRUST-"+topup.getId();
//                        respXendit = sendXendit.paymentURL(methods, id, Integer.parseInt(new DecimalFormat("#").format(totalPayment)), c.getTime(), description, name, email, pnumber);
//                        if(respXendit.getInt("code") != 200)
//                        {
//                            notif="Failed topup";
//                            jo.put("result_code", "B06");
//                            jo.put("message", notif);
//                            jo.put("log", uuid);
//                            jo.put("timestamp", dateFormatter.getTimestamp());
//                            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                            jo.put("error", respXendit);
//                            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                            return ResponseEntity
//                                    .status(200)
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .body(jo.toString());
//                        }
//                        code = respXendit.getString("invoice_url");
//
//                        pay.setPaymentCode(code);
//
//                        topup.setPayment(pay);
//                        topup.setPrice(totalPrice);
//                        topup.setTotal(Integer.parseInt(new DecimalFormat("#").format(totalPayment)));
//
//
//                        paymentRepository.save(pay);
//                        topupRepository.save(topup);
//
//                        DataProduct dp = new DataProduct();
//
//                        dp.setPackage_id("0");
//                        dp.setPackage_name("Idtrust Basic");
////			  	  		dp.setPayment_method(topup.getPayment().getChannel().getChannel_code());
//                        dp.setPrice(topup.getTotal());
//                        dp.setTransaction_date(topup.getCreatedDate().plus(Duration.ofHours(7)).truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", "+07:00"));
//                        dp.setPackage_detail(dataProducer);
//                        dp.setExpired_date(topup.getExpiredDate().plus(Duration.ofHours(7)).truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", "+07:00"));
//                        dp.setLink_xendit(code);
//                        dp.setTransaction_ip(InetAddress.getLocalHost().getHostAddress());
//                        dp.setTransaction_user_agent(user_agent);
//
//                        JSONObject paymentInfo = new JSONObject();
//
//                        paymentInfo.put("id", id);
//                        paymentInfo.put("type", channelData.getType());
//                        paymentInfo.put("data", code);
//                        paymentInfo.put("payment_expired", dateFormat.format(Date.from(topup.getExpiredDate())));
//                        paymentInfo.put("name", name);
//                        paymentInfo.put("price", Integer.parseInt(new DecimalFormat("#").format(totalPrice)));
//
//                        notif="Success. Waiting for payment";
//                        jo.put("result_code", "B00");
//                        jo.put("message", notif);
//                        jo.put("log", uuid);
//                        jo.put("timestamp", dateFormatter.getTimestamp());
//                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                        jo.put("product", resume);
//                        paymentInfo.put("total_price", Integer.parseInt(new DecimalFormat("#").format(totalPayment)));
//                        jo.put("payment_info", paymentInfo);
//                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//
//                        try {
//                            //send to message broker
//                            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Send to kafka");
//
//                            id.idtrust.billing.broker.kafka.Payment paymentProducer = new id.idtrust.billing.broker.kafka.Payment();
//
//                            paymentProducer.setData(dp);
//                            paymentProducer.setEmail(email);
//                            paymentProducer.setName(name);
//                            paymentProducer.setTRACE_ID(uuid);
//                            paymentProducer.setTimestamp(new Date().toInstant().plus(Duration.ofHours(7)).truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", "+07:00"));
//                            paymentProducer.setUser_id(topup.getAccount().getXkey().replaceAll("[^0-9]", ""));
//
//                            kafkaPayment.send("pending_payment", paymentProducer);
//
////                            if(!status)
////                            {
////                                try
////                                {
////                                    TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
////                                    bot.execute(new SendMessage(213382980, "Failed send topic pending_payment : " + uuid));
////                                }catch(Exception t)
////                                {
////                                    logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
////                                    logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
////                                }
////                            }
//
//                        }catch(Exception e)
//                        {
//                            jo.put("error", e.toString());
//                            e.printStackTrace();
//                            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +"Failed send to message broker");
//                        }
//
//                        try {
////                            Rabbitmq rabbit = new Rabbitmq();
//                            JSONObject message = new JSONObject();
//                            message.put("id", id);
//
////                            Boolean statusRabbit = rabbit.send(message.toString(), uuid, dateFormat.format(Date.from(topup.getCreatedDate())),dateFormat.format(Date.from(topup.getExpiredDate())));
//
//                            rabbitTemplate.convertAndSend("billtrust-key", message.toString(), m -> {
//                                m.getMessageProperties().getHeaders().put("x-delay", 3600000);
//                                m.getMessageProperties().setPriority(1);
//
//                                LogSystem.info("RabbitMQ [x] Sent '" + message + " at " + dateFormat.format(Date.from(topup.getCreatedDate())) + " will execute at " + dateFormat.format(Date.from(topup.getExpiredDate())) +"'");
//                                return m;
//                            });
//
////                            if(!statusRabbit)
////                            {
////                                try
////                                {
////                                    TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
////                                    bot.execute(new SendMessage(213382980, "Failed process RabbitMQ : " + uuid));
////                                }catch(Exception t)
////                                {
////                                    logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
////                                    logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
////                                }
////                            }
//
//                        }catch(Exception e)
//                        {
//                            e.printStackTrace();
//                            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +"Failed send to rabbitmq");
//                        }
//
//                        return ResponseEntity
//                            .status(200)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .body(jo.toString());
//                    }
//

                List<Invoice> invoiceList = new ArrayList<Invoice>();
                List<Batch> batchList = new ArrayList<Batch>();
                //Process batch topup without payment
                for(int i=0; i<arraytopup.length(); i++)
                {
                    long startTimeBatch = System.nanoTime();
                    JSONObject datasuccess = new JSONObject();
                    JSONObject obj=(JSONObject) arraytopup.get(i);

                    if(!obj.has("xkey"))
                    {
                        notif="Missing parameter xkey on JSONArray";
                        jo.put("result_code", "B28");
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                    if(!obj.has("product"))
                    {
                        notif="Missing parameter product on JSONArray";
                        jo.put("result_code", "B28");
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                    if(!obj.has("amount"))
                    {
                        notif="Missing parameter amount on JSONArray";
                        jo.put("result_code", "B28");
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                    if(!obj.has("price"))
                    {
                        notif="Missing parameter price on JSONArray";
                        jo.put("result_code", "B28");
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }

                    Product tenantApiKey = productRepository.findByPkey(obj.getString("product"));

                    if(tenantApiKey == null)
                    {
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Product not available");
                        notif="Product not available";
                        jo.put("result_code", "B01");
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }

                    Account account = AccountsRepo.findByExternalKey(obj.getString("xkey"), tenantApiKey.getId());
                    if(account == null)
                    {
                        notif="Account not exist";
                        jo.put("result_code", "B04");
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                    else
                    {
                        Invoice invTopup = new Invoice();
                        try
                        {
                            Batch batch = new Batch();
                            batch.setAccount(account);
                            batch.setOpenDate(now);
                            batch.setNameBatch("PRE_" + obj.getString("xkey") + "_" + tenantApiKey.getPkey());
                            batch.setSettled(false);
                            batch.setQuota(obj.getInt("amount"));
                            if(obj.has("price"))
                            {
                                batch.setPrice(obj.getInt("price"));
                            }
                            else
                            {
                                batch.setPrice(0);
                            }

                            batch.setRemainingBalance(obj.getInt("amount"));
                            batch.setUsage(0);
                            Calendar c = Calendar.getInstance();
                            c.setTime(new Date());
                            c.add(Calendar.YEAR, 1);
                            batch.setExpired(c.getTime());

//                                session.save(batch);
                            batchList.add(batch);

                            invTopup.setAccount(account);
                            invTopup.setCreatedDate(now);
                            invTopup.setInvoiceId(invoiceId);
//                            invTopup.setType("CREDIT_ADJ");
                            invTopup.setAmount(obj.getInt("amount"));
                            invTopup.setDescription("Topup Balance Custom");
                            invTopup.setTrx(1);
                            invTopup.setBatch(batch);

//                                session.save(invTopup);
                            invoiceList.add(invTopup);

                            datasuccess.put("xkey", account.getXkey());
                            datasuccess.put("product", account.getProduct().getPkey());
                            datasuccess.put("status", true);
                            datasuccess.put("info", "Success");
                            datasuccess.put("current_balance", invTopup.getCurrentBalance());
                            datasuccess.put("invoiceid", invTopup.getInvoiceId());
                            datasuccess.put("datetime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(invTopup.getCreatedDate()));
                            arraydatasuccess.put(datasuccess);
                            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +datasuccess);
                        }catch(Exception e) {
                            e.printStackTrace();
                            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e);

                            notif="Failed topup";
                            jo.put("result_code", "B06");
                            jo.put("message", notif);
                            jo.put("error", e.toString());
                            jo.put("log", uuid);
                            jo.put("timestamp", dateFormatter.getTimestamp());
                            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);

                            return ResponseEntity
                                    .status(200)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jo.toString());
                        }
                    }
                }

                try {
                    long commitinsert = System.nanoTime();
                    topupService.setBatch(batchList);
                    topupService.setInvoice(invoiceList);

                    int current_balance = topupService.topup();

                    jo.put("current_balance", current_balance);

                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"PROCESS INSERT COMMIT : " + formatter.format((System.nanoTime() - commitinsert)/1000000000d));
                }catch(Exception e)
                {
                    e.printStackTrace();
                    logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());

                    notif="Failed topup";
                    jo.put("result_code", "B06");
                    jo.put("message", notif);
                    jo.put("error", e.toString());
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

                    try
                    {
                        TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                        bot.execute(new SendMessage(213382980, "Failed create topup log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
                    }catch(Exception t)
                    {
                        logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                        logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
                    }
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                notif="Success";
                jo.put("data", arraydatasuccess);
                jo.put("result_code", "B00");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            notif="Error Topup";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Failed topup : " + uuid + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            }

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/topup/bisnis", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> topupBisnis(@RequestBody String data, @RequestHeader("User-Agent") String user_agent) throws Exception {
        long startTime = System.nanoTime();
        String invoiceId= UUID.randomUUID().toString();
        String uuid= UUID.randomUUID().toString();

        NumberFormat formatter = new DecimalFormat("#0.00000");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);

        JSONObject jo=new JSONObject();
        String transactionid = UUID.randomUUID().toString().replace("-", "");

        try {
            Topup topup;
            String topupId;
            String source;
            String proofOfPayment;

            topupId = rjson.getString("request_topup_id");
            proofOfPayment = rjson.getString("proof_of_payment");

            JSONObject response = new JSONObject();

            long id = Long.parseLong(topupId);

            topup = topupRepository.findByIds(id);

            if (topup == null) {

                logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "No data topup");

                jo.put("result_code", "B23");
                jo.put("message", "Data not found");
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());

            } else
            {
                //Save proof of payments
                String[] strings = proofOfPayment.split(",");
                String extension;
                switch (strings[0]) {//check image's extension
                    case "data:image/jpeg;base64":
                        extension = "jpeg";
                        break;
                    case "data:image/png;base64":
                        extension = "png";
                        break;
                    default://should write cases for more images types
                        extension = "jpg";
                        break;
                }

                if(topup.getStatus().equalsIgnoreCase("TOPUP_SUCCESS"))
                {
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Topup already success");

                    jo.put("result_code", "B24");
                    jo.put("message", "Topup already success");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                if(topup.getStatus().equalsIgnoreCase("VOID"))
                {
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Topup has void");

                    jo.put("result_code", "B25");
                    jo.put("message", "Topup has void");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                //convert base64 string to binary data
                byte[] bytes = DatatypeConverter.parseBase64Binary(strings[1]);
                String fileProof = UUID.randomUUID().toString().replace("-", "");
                String path = PAYMENT_FILE + fileProof+"."+extension;

                File file = new File(path);
                try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Failed to read proof of payment image file");

                    jo.put("result_code", "B26");
                    jo.put("message", "Topup already success");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
            }

            List<TopupProduct> tp = topupProductRepository.findByTopup(topup.getId());

            Account account = AccountsRepo.findByIds(topup.getAccount().getId());
            Date now = new Date();

            Payment payment = paymentRepository.ById(topup.getPayment().getId());

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
//                invoice.setType("CREDIT_ADJ");
                invoice.setAmount(tp.get(i).getQty());
                invoice.setDescription("Topup Balance with Payment");
                invoice.setTrx(1);
                invoice.setBatch(batch);

                if(topup.getPlan() != null)
                {
                    invoice.setPlan(topup.getPlan());
                }

                invoiceList.add(invoice);
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"PRODUKNYA " + tp.get(i).getProduct().getPkey());
                if(!tp.get(i).getProduct().getPkey().equalsIgnoreCase("sms")) {
                    DetailDataProduct ddp = new DetailDataProduct();
                    ddp.setBalance(tp.get(i).getQty());
                    ddp.setType_balance(tp.get(i).getProduct().getNameProduct());
                    ddp.setType_balance_en(tp.get(i).getProduct().getNameProductEn());
                    dataProducer.add(ddp);
                }
            }

            topup.setStatus("TOPUP_SUCCESS");
            payment.setPaymentDate(now);
            payment.setStatus("PAID");
            payment.setTransactionId("");
            payment.setSource(null);

            topupService.setBatch(batchList);
            topupService.setInvoice(invoiceList);
            topupService.setPayment(payment);
            topupService.setTopup(topup);

            Date date = new Date();
            DateFormatter dateIndo = new DateFormatter();
            String invoiceDate = dateIndo.tampilkanTanggalDanWaktu(date, "dd MMMM yyyy", null);

            String to = topup.getAccount().getName();

            //Creating pdf invoice
            Invoices invoices = new Invoices();
//            String docInvoice = invoices.generateBisnis(tp, to, invoiceDate);

            InvoiceFiles invoiceFiles = new InvoiceFiles();
            invoiceFiles.setPath(invoices.getInvoicePath());
            invoiceFiles.setTopup(topup);

            topupService.setInvoiceFiles(invoiceFiles);

            topupService.topupCallback();

            DataProduct dp = new DataProduct();
            if(topup.getPlan() != null) {
                dp.setPackage_id(topup.getPlan().getId().toString());
                dp.setPackage_name(topup.getPlan().getName());
            }

            dp.setPayment_method(null);
            dp.setPrice(topup.getTotal());

            dp.setTransaction_date(payment.getPaymentDate().toString());
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"date " + dp.getTransaction_date());
            dp.setPackage_detail(dataProducer);
            dp.setTransaction_id(transactionid);
            dp.setTransaction_user_agent(user_agent);
            dp.setTransaction_ip(InetAddress.getLocalHost().getHostAddress());

            try {
                //send to message broker
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Send to kafka");

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
                logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +"Failed send to message broker");
            }

            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"All processes have been completed " + formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +response);

            jo.put("result_code", "B00");
            jo.put("message", "Topup success");
            jo.put("log", uuid);
            jo.put("invoice", "JVBERi0xLjcNMSAwIG9iag08PC9UeXBlIC9YT2JqZWN0IC9TdWJ0");
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());

        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +e);
            jo.put("status", false);

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Failed process callback payment : " + uuid));
            }catch(Exception t)
            {
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +e);
            }

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/topup/bisnis/get", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> getBisnis(@RequestBody String data) throws Exception {
        long startTime = System.nanoTime();

        InetAddress inetAddress = null;

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);
        JSONObject jo = new JSONObject();
        String notif="Success";

        try {
            Page<Topup> pageSuccess = null;
            Page<Topup> pageWaiting = null;
            Page<Topup> pageFailed = null;

            List<Account> account = new ArrayList<Account>();
            List<Topup> topupSuccess = new ArrayList<Topup>();
            List<Topup> topupFailed = new ArrayList<Topup>();
            List<Topup> topupWaiting = new ArrayList<Topup>();

            account = AccountsRepo.findSingleAccount(rjson.getString("xkey"));

            if(account.size() < 1)
            {
                notif="Account not exist";
                jo.put("result_code", "B04");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            JSONArray listTopup = new JSONArray();
            JSONArray listTopupSuccess = new JSONArray();
            JSONArray listVoidTopupSuccess = new JSONArray();

            List<TopupProduct> tp = new ArrayList<TopupProduct>();

            Topup topupAccount;

            int successPage=0;
            int successSize=10;

            int failedPage=0;
            int failedSize=10;

            int waitingPage=0;
            int waitingSize=10;

            if(rjson.has("paging"))
            {
                if(rjson.getJSONObject("paging").has("success"))
                {
                    JSONObject pagingSuccess = rjson.getJSONObject("paging").getJSONObject("success");
                    successPage = pagingSuccess.getInt("page") - 1;
                    successSize = pagingSuccess.getInt("size");
                }

                if(rjson.getJSONObject("paging").has("failed"))
                {
                    JSONObject pagingFailed = rjson.getJSONObject("paging").getJSONObject("failed");
                    failedPage = pagingFailed.getInt("page") - 1;
                    failedSize = pagingFailed.getInt("size");
                }

                if(rjson.getJSONObject("paging").has("waiting"))
                {
                    JSONObject pagingWaiting = rjson.getJSONObject("paging").getJSONObject("waiting");
                    waitingPage = pagingWaiting.getInt("page") - 1;
                    waitingSize = pagingWaiting.getInt("size");
                }
            }

            if(rjson.has("type") && rjson.getString("type").equals("emeterai"))
            {
                if(successSize > 0) {
                    pageSuccess = topupRepository.findByBisnisEmeteraiPaging(rjson.getString("xkey"), "TOPUP_SUCCESS", PageRequest.of(successPage, successSize));
                    if (pageSuccess.getTotalElements() != 0) {
                        topupSuccess = pageSuccess.getContent();
                    }
                }

                if(waitingSize > 0) {
                    pageWaiting = topupRepository.findByBisnisEmeteraiPaging(rjson.getString("xkey"), "WAITING_PAYMENT", PageRequest.of(waitingPage, waitingSize));
                    if (pageWaiting.getTotalElements() != 0) {
                        topupWaiting = pageWaiting.getContent();
                    }
                }

                if(failedSize > 0) {
                    pageFailed = topupRepository.findByBisnisEmeteraiPaging(rjson.getString("xkey"), "VOID", PageRequest.of(failedPage, failedSize));
                    if (pageFailed.getTotalElements() != 0) {
                        topupFailed = pageFailed.getContent();
                    }
                }

            }
            else
            {
                if(successSize > 0) {
                    pageSuccess = topupRepository.findByBisnisPaging(rjson.getString("xkey"), "TOPUP_SUCCESS", PageRequest.of(successPage, successSize));
                    if (pageSuccess.getTotalElements() != 0) {
                        topupSuccess = pageSuccess.getContent();
                    }
                }

                if(waitingSize > 0) {
                    pageWaiting = topupRepository.findByBisnisPaging(rjson.getString("xkey"), "WAITING_PAYMENT", PageRequest.of(waitingPage, waitingSize));
                    if (pageWaiting.getTotalElements() != 0) {
                        topupWaiting = pageWaiting.getContent();
                    }
                }

                if(failedSize > 0) {
                    pageFailed = topupRepository.findByBisnisPaging(rjson.getString("xkey"), "VOID", PageRequest.of(failedPage, failedSize));
                    if (pageFailed.getTotalElements() != 0) {
                        topupFailed = pageFailed.getContent();
                    }
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

            if(topupSuccess.size() > 0)
            {
                for(int j = 0 ; j < topupSuccess.size() ; j++)
                {
                    topupAccount = topupRepository.findByIds(topupSuccess.get(j).getId());

                    JSONObject objectTopup = new JSONObject();
                    objectTopup.put("id", topupAccount.getId());
                    objectTopup.put("topup_status", topupAccount.getStatus());
                    objectTopup.put("price", topupAccount.getPrice());
                    objectTopup.put("total", topupAccount.getTotal());
                    objectTopup.put("ppn", topupAccount.getPrice() * 11/100);

                    objectTopup.put("payment_status", topupAccount.getPayment().getStatus());

                    tp = topupProductRepository.findByTopup(topupAccount.getId());

                    JSONArray listProduct = new JSONArray();

                    for (int k = 0; k < tp.size(); k++)
                    {
                        JSONObject topupProduct = new JSONObject();

                        topupProduct.put("name", tp.get(k).getProduct().getNameProduct());
                        topupProduct.put("quantity", tp.get(k).getQty());
                        topupProduct.put("unit_price", tp.get(k).getQty());

                        listProduct.put(topupProduct);

                        objectTopup.put("product_detail", listProduct);
                    }

                    objectTopup.put("payment_date", sdf.format(topupAccount.getPayment().getPaymentDate()));
                    objectTopup.remove("payment_code");
                    listTopupSuccess.put(objectTopup);
                }
            }

            if(topupWaiting.size() > 0)
            {
                for(int j = 0 ; j < topupWaiting.size() ; j++)
                {

                    topupAccount = topupRepository.findByIds(topupWaiting.get(j).getId());
                    JSONObject objectTopup = new JSONObject();
                    objectTopup.put("id", topupAccount.getId());
                    objectTopup.put("topup_status", topupAccount.getStatus());
                    objectTopup.put("expired_at", sdf.format(topupAccount.getExpiredDate()));
                    objectTopup.put("price", topupAccount.getPrice());
                    objectTopup.put("total", topupAccount.getTotal());
                    objectTopup.put("ppn", topupAccount.getPrice() * 11/100);

                    objectTopup.put("payment_status", topupAccount.getPayment().getStatus());

                    tp = topupProductRepository.findByTopup(topupAccount.getId());

                    JSONArray listProduct = new JSONArray();

                    for (int k = 0; k < tp.size(); k++)
                    {
                        JSONObject topupProduct = new JSONObject();

                        topupProduct.put("name", tp.get(k).getProduct().getNameProduct());
                        topupProduct.put("quantity", tp.get(k).getQty());
                        topupProduct.put("unit_price", tp.get(k).getQty());

                        listProduct.put(topupProduct);

                        objectTopup.put("product_detail", listProduct);
                    }

                    listTopup.put(objectTopup);

                }
            }

            if(topupFailed.size() > 0)
            {
                for(int j = 0 ; j < topupFailed.size() ; j++)
                {
                    topupAccount = topupRepository.findByIds(topupFailed.get(j).getId());

                    JSONObject objectTopup = new JSONObject();
                    objectTopup.put("id", topupAccount.getId());
                    objectTopup.put("topup_status", topupAccount.getStatus());
                    objectTopup.put("price", topupAccount.getPrice());
                    objectTopup.put("total", topupAccount.getTotal());
                    objectTopup.put("ppn", topupAccount.getPrice() * 11/100);
                    objectTopup.put("payment_status", topupAccount.getPayment().getStatus());

                    tp = topupProductRepository.findByTopup(topupAccount.getId());

                    JSONArray listProduct = new JSONArray();

                    for (int k = 0; k < tp.size(); k++)
                    {
                        JSONObject topupProduct = new JSONObject();

                        topupProduct.put("name", tp.get(k).getProduct().getNameProduct());
                        topupProduct.put("quantity", tp.get(k).getQty());
                        topupProduct.put("unit_price", tp.get(k).getQty());

                        listProduct.put(topupProduct);

                        objectTopup.put("product_detail", listProduct);
                    }

                    objectTopup.remove("payment_code");
                    listVoidTopupSuccess.put(objectTopup);
                }
            }

            JSONObject payment = new JSONObject();

            JSONObject dataPayment = new JSONObject();
            dataPayment.put("data", listTopup);
            if(pageWaiting != null) {
                dataPayment.put("totalPages", pageWaiting.getTotalPages());
                dataPayment.put("totalElements", pageWaiting.getTotalElements());
            }


            JSONObject dataSuccess = new JSONObject();
            dataSuccess.put("data", listTopupSuccess);
            if(pageSuccess != null) {
                dataSuccess.put("totalPages", pageSuccess.getTotalPages());
                dataSuccess.put("totalElements", pageSuccess.getTotalElements());
            }

            JSONObject dataFailed = new JSONObject();
            dataFailed.put("data", listVoidTopupSuccess);
            if(pageFailed != null) {
                dataFailed.put("totalPages", pageFailed.getTotalPages());
                dataFailed.put("totalElements", pageFailed.getTotalElements());
            }
            payment.put("need_payment", dataPayment);
            payment.put("success", dataSuccess);
            payment.put("failed", dataFailed);

            jo.put("data", payment);
            jo.put("result_code", "B00");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());

        }catch(Exception e)
        {
            e.printStackTrace();
            notif="Error Get List Topup";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error topup : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +e);
            }

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/topup/bisnis/request", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> requestBisnis(@RequestBody String data, @RequestHeader("User-Agent") String user_agent) throws Exception {

        long startTime = System.nanoTime();
        Date now = new Date();

        Boolean batchtopup = true;
        NumberFormat formatter = new DecimalFormat("#0.00000");

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("[" + VERSION + "]-[BILLTRUST/REQUEST] : " + rjson);
        JSONObject jo = new JSONObject();

        String notif = "Error topup";

        try {

            if (!rjson.has("xkey")) {
                notif = "Missing parameter xkey";
                jo.put("result_code", "B28");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            if (!rjson.has("data")) {
                notif = "Missing parameter amount";
                jo.put("result_code", "B28");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            JSONArray dataArray = rjson.getJSONArray("data");

            Topup topup = new Topup();
            Payment payment = new Payment();
            List<TopupProduct> topupProductList = new ArrayList<TopupProduct>();

            int total = 0;

            Account acc = null;

            Boolean def = true;

            List<BusinessPrice> businessPriceList = businessPriceRepository.bisnisFindByXkey(rjson.getString("xkey"));

            if(businessPriceList.size() > 0)
            {
                def = false;
            }
            String to=null;

            for(int i = 0 ; i < dataArray.length() ; i++)
            {

                JSONObject dataObject = dataArray.getJSONObject(i);

                if(!dataObject.has("product"))
                {
                    notif = "Missing product in data";
                    jo.put("result_code", "B28");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo.toString());
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                if(!dataObject.has("amount")) {
                    notif = "Missing amount in data";
                    jo.put("result_code", "B28");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo.toString());
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                Product product = productRepository.findByPkey(dataObject.getString("product"));
                Account account = AccountsRepo.findAccountByProduct(rjson.getString("xkey"), product.getId());

                if(to == null)
                {
                    to = account.getName();
                }

                if(product == null)
                {
                    jo.put("result_code", "B01");
                    jo.put("message", "Product not exist " + dataObject.getString("product"));
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo.toString());
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                acc = account;

                if(account == null)
                {
                    jo.put("result_code", "B04");
                    jo.put("message", "Account not exist "+ dataObject.getString("product"));
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                int amount = dataObject.getInt("amount");

                //Query harga bisnis sendiri atau default
                int unitPrice = 0;

                if(def)
                {
                    unitPrice = product.getBase_price();
                }
                else
                {
                    BusinessPrice businessPrice = businessPriceRepository.bisnisFindByAccountId(account.getId());
                    if(businessPrice == null)
                    {
                        jo.put("result_code", "B01");
                        jo.put("message", rjson.getString("xkey") + " Price for " + account.getProduct().getPkey() + " not exist");
                        jo.put("log", uuid);
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                        jo.put("timestamp", dateFormatter.getTimestamp());

                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                    unitPrice = businessPrice.getPrice();
                }

                TopupProduct topupProduct = new TopupProduct();
                topupProduct.setUnit_price(unitPrice);
                int price = amount * unitPrice;

                total += price;

                topupProduct.setPrice(price);
                topupProduct.setProduct(product);
                topupProduct.setAccount(account);
                topupProduct.setQty(amount);
                topupProduct.setTopup(topup);

                topupProductList.add(topupProduct);
            }

            Channel channel = channelRepository.findChannel("IDTRUST");

            payment.setPaymentDate(null);
            payment.setPaymentCode("2302405001");
            payment.setStatus("WAITING_PAYMENT");
            payment.setChannel(channel);
            payment.setSource(null);
            payment.setTransactionId(UUID.randomUUID().toString().replace("-", ""));

            topupService.setTopupProducts(topupProductList);
            topupService.setPayment(payment);

            topup.setTotal(total + (total * 11/100));

            topup.setPayment(payment);
            topup.setStatus("WAITING_PAYMENT");

            Calendar c = Calendar.getInstance();
            c.setTime(now);
            c.add(Calendar.HOUR, 1);

            topup.setCreatedDate(now);
            topup.setExpiredDate(c.getTime());
            topup.setPlan(null);
            topup.setAccount(acc);
            topup.setPrice(total);

            topupService.setTopup(topup);

            topupService.requestTopupBisnis();

            jo.put("id", topup.getId());
            jo.put("result_code", "B00");
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            jo.put("timestamp", dateFormatter.getTimestamp());

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e);
            notif="Error Topup";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Failed topup : " + uuid + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            }

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/topup/plan", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> Plan(@RequestBody String data, @RequestHeader("User-Agent") String user_agent) throws RuntimeException, Exception {

        String invoiceId= UUID.randomUUID().toString();
        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);
        JSONObject jo = new JSONObject();

        try {
            Plan plan = planRepository.findByIds(rjson.getLong("plan"));

            JSONObject response = new JSONObject();

            if(plan == null)
            {
                response.put("result_code", "B07");
                response.put("message", "Plan not exist");
                response.put("log", uuid);
                response.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +response);

                return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.toString());
            }

            if(!plan.getType().equalsIgnoreCase("emeterai"))
            {
                int sizeStarter = topupRepository.userHasTopupStarter(rjson.getString("xkey")).size();
                logger.debug("[" + VERSION + "]-[BILLTRUST/INFO] : Starter " + sizeStarter);
                logger.debug("[" + VERSION + "]-[BILLTRUST/INFO] : Plan type " + plan.getType());

                //cek usernya udah pernah beli paket perdana/starter trus beli lagi ditolak
                if (sizeStarter > 0 && plan.getType().equals("starter")) {
                    response.put("result_code", "B21");
                    response.put("message", "User has purchased a starter pack");
                    response.put("log", uuid);
                    response.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + response);

                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response.toString());
                }

                //cek usernya belum pernah beli paket perdana/starter tapi ga pilih paket perdana/starter
                if(sizeStarter < 1 && !plan.getType().equals("starter"))
                {
                    response.put("result_code", "B22");
                    response.put("message", "User must purchased a starter pack");
                    response.put("log", uuid);
                    response.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +response);

                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response.toString());
                }

                //cek jika masih ada proses beli saldo perdana tapi dia beli saldo bukan perdana
                int starterNeedPayment = topupRepository.userHasTopupStarterWaitingPayment(rjson.getString("xkey")).size();

                if(starterNeedPayment > 0 && !plan.getType().equalsIgnoreCase("starter"))
                {
                    response.put("result_code", "B29");
                    response.put("message", "the user has an unpaid starter pack bill");
                    response.put("log", uuid);
                    response.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +response);

                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response.toString());
                }

            }

            List<PlanDetail> pd = planDetailRepository.findById(plan.getId());

            Account account = new Account();
            Product product = new Product();

            if(rjson.getBoolean("generate_payment") && !plan.isFree())
            {
                String code = "";
                JSONArray resume = new JSONArray();
                int totalPrice = 0;
                Topup topup = new Topup();
                Date now = new Date();

                JSONObject payment = new JSONObject(rjson.getString("payment"));
                String name = "";
                if(payment.has("name")) {
                    name = payment.getString("name");
                }
                String pnumber="";

                String channel = payment.getString("channel");
                Channel channelData = new Channel();

                try {
                    channelData = channelRepository.findChannel(channel);
                }catch(Exception e)
                {
                    jo.put("result_code", "06");
                    jo.put("message", "Failed access database");
                    jo.put("error", e.toString());
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                    return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
                }

                if (channelData == null)
                {
                    jo.put("result_code", "B14");
                    jo.put("message", "Metode Pembayaran tidak ditemukan");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                    return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
                }

                totalPrice = plan.getTotal();
                Calendar c = Calendar.getInstance();
                c.setTime(now);
                c.add(Calendar.HOUR, 1);
                String email = "";
                ArrayList<DetailDataProduct> dataProducer = new ArrayList<DetailDataProduct>();

                List<TopupProduct> listTopupProduct = new ArrayList<TopupProduct>();

                for(int i = 0 ; i < pd.size() ; i++)
                {
                    Product prd = productRepository.findByPkey(pd.get(i).getProduct().getPkey());
                    account = AccountsRepo.findByExternalKey(rjson.getString("xkey"), prd.getId());

                    if(account == null)
                    {
                        jo.put("result_code", "B04");
                        jo.put("message", "Account not exist");
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
                        return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                    }

                    email=account.getEmail();
                    name=account.getName();
                    topup.setPrice(totalPrice);
                    topup.setCreatedDate(now);
                    topup.setStatus("WAITING_PAYMENT");
                    topup.setAccount(account);
                    topup.setPlan(plan);

                    topup.setExpiredDate(c.getTime());
//                    topupRepository.save(topup);

                    TopupProduct td = new TopupProduct();

                    td.setPrice(pd.get(i).getPrice());
                    td.setProduct(prd);
                    td.setQty(pd.get(i).getQty());
                    td.setTopup(topup);
                    td.setAccount(account);

                    listTopupProduct.add(td);
//                    topupProductRepository.save(td);

                    JSONObject datasuccess = new JSONObject();
                    datasuccess.put("name", td.getProduct().getNameProduct());
                    datasuccess.put("qty", td.getQty());
                    datasuccess.put("price", td.getPrice());

                    resume.put(datasuccess);

                    DetailDataProduct dp = new DetailDataProduct();
                    dp.setBalance(pd.get(i).getQty());
                    dp.setType_balance(pd.get(i).getProduct().getNameProduct());
                    dp.setType_balance_en(pd.get(i).getProduct().getNameProductEn());

                    if(!prd.getPkey().equalsIgnoreCase("sms"))
                    {
                        dataProducer.add(dp);
                    }
                }

                Payment pay = new Payment();
                pay.setPaymentCode(code);
                pay.setChannel(channelData);
                pay.setStatus("WAITING_PAYMENT");

                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Resume topup : " + resume.toString() + " with total price " + totalPrice);

                JSONArray methods = new JSONArray();

                methods.put(channelData.getChannelCode());

                double totalPayment=0;

                totalPayment = plan.getTotal();

                String description = plan.getName();

                DataProduct dp = new DataProduct();

                dp.setPackage_id(topup.getPlan().getId().toString());
                dp.setPackage_name(topup.getPlan().getName());
                dp.setPrice(Integer.parseInt(new DecimalFormat("#").format(totalPayment)));
                dp.setTransaction_date(dateFormatter.getTimestamp(topup.getCreatedDate()));
                dp.setPackage_detail(dataProducer);
                dp.setExpired_date(dateFormatter.getTimestamp(topup.getExpiredDate()));
                dp.setLink_xendit(code);
                dp.setTransaction_ip(InetAddress.getLocalHost().getHostAddress());
                dp.setTransaction_user_agent(user_agent);

                topupService.setPayment(pay);
                topupService.setTopup(topup);
                topupService.setTopupProducts(listTopupProduct);

                JSONObject service = topupService.requestTopup(c, description, name, email, pnumber, totalPayment, methods);

                if(!service.getBoolean("status"))
                {
                    topupRepository.delete(topup);
                    jo.put("result_code", "B06");
                    jo.put("message", "Failed topup");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    jo.put("error", service.getJSONObject("xendit"));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                String id = service.getString("id");
                code = service.getJSONObject("xendit").getString("invoice_url");

                JSONObject paymentInfo = new JSONObject();
                paymentInfo.put("type", channelData.getType());
                paymentInfo.put("data", code);
                paymentInfo.put("payment_expired", dateFormatter.getTimestamp(topup.getExpiredDate()));
                paymentInfo.put("id", id);
                paymentInfo.put("name", name);
                paymentInfo.put("price", Integer.parseInt(new DecimalFormat("#").format(totalPrice)));
                
                jo.put("result_code", "B00");
                jo.put("message", "Success. Waiting for payment");
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                jo.put("product", resume);
                paymentInfo.put("total_price", Integer.parseInt(new DecimalFormat("#").format(totalPayment)));
                jo.put("payment_info", paymentInfo);

                try {
                    //send to message broker
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Send to kafka");

                    id.idtrust.billing.broker.kafka.Payment paymentProducer = new id.idtrust.billing.broker.kafka.Payment();
                    paymentProducer.setEmail(email);
                    paymentProducer.setName(name);
                    paymentProducer.setTRACE_ID(uuid);

                    paymentProducer.setData(dp);
                    paymentProducer.setTimestamp(new Date().toInstant().plus(Duration.ofHours(7)).truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", "+07:00"));
                    paymentProducer.setUser_id(topup.getAccount().getXkey().replaceAll("[^0-9]", ""));

                    kafkaPayment.send("pending_payment", paymentProducer);

                }catch(Exception e)
                {
                    e.printStackTrace();
                    logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +"Failed send to kafka");
                    response.put("error", e.toString());
                }

//				//Send to RabbitMQ
                try {
                    JSONObject msg = new JSONObject();
                    msg.put("id", id);

//                    rabbitTemplate.convertAndSend("billtrust-key", message.toString(), m -> {
//                        m.getMessageProperties().getHeaders().put("x-delay", RABBITMQ_DELLAYED_TIME);
//                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +m.toString());
//                        LogSystem.info("RabbitMQ [x] Sent '" + message + " at " + dateFormatter.getTimestamp(topup.getCreatedDate()) + " will execute at " + dateFormatter.getTimestamp(topup.getExpiredDate()) +"'");
//                        return m;
//                    });

                    rabbitTemplate.convertAndSend("payment_void","billtrust-key", msg.toString(),
                            message -> {
                                message.getMessageProperties().setHeader("x-delay", RABBITMQ_DELLAYED_TIME);
                                LogSystem.info("RabbitMQ [x] Sent '" + message + " at " + dateFormatter.getTimestamp(topup.getCreatedDate()) + " will execute at " + dateFormatter.getTimestamp(topup.getExpiredDate()) +"'");
                                return message;
                            });

                }catch(Exception e)
                {
                    e.printStackTrace();
                    logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +"Failed send to rabbitmq");
                }
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
                return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
            }
            else
            {
                List<Batch> batchList = new ArrayList<Batch>();
                List<Invoice> invoiceList = new ArrayList<Invoice>();
                boolean exist = false;
                for (int i = 0 ; i < pd.size() ; i++)
                {
                    product = productRepository.findByPkey(pd.get(i).getProduct().getPkey());
                    account = AccountsRepo.findByExternalKeyActive(rjson.getString("xkey"), product.getId());

                    if(account == null)
                    {
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : Account " + rjson.getString("xkey") + " " +product.getPkey() + " not exist, skip topup");

                    }
                    else
                    {
                        exist = true;
                        Date now = new Date();

                        Batch batch = new Batch();
                        batch.setAccount(account);
                        batch.setOpenDate(now);
                        batch.setNameBatch("PRE_" + account.getXkey() + "_" + account.getProduct().getPkey());
                        batch.setSettled(false);
                        batch.setQuota(pd.get(i).getQty());
                        batch.setPrice(pd.get(i).getPrice());

                        if(plan.isFree())
                        {
                            batch.setFree(true);
                        }

                        batch.setRemainingBalance(pd.get(i).getQty());
                        batch.setUsage(0);

                        batchList.add(batch);

                        Invoice invoice = new Invoice();

                        invoice.setAccount(account);
                        invoice.setCreatedDate(now);
                        invoice.setInvoiceId(invoiceId);
//                        invoice.setType("CREDIT_ADJ");
                        invoice.setAmount(pd.get(i).getQty());
                        if(plan.isFree())
                        {
                            invoice.setDescription("(Promo) Topup Free Balance with plan " + plan.getName());
                        }
                        else
                        {
                            invoice.setDescription("Topup Balance with plan " + plan.getName());
                        }

                        invoice.setTrx(1);
                        invoice.setBatch(batch);
                        invoice.setPlan(plan);

                        invoiceList.add(invoice);
                    }
                }

                if(!exist)
                {
                    jo.put("result_code", "B04");
                    jo.put("message", "Account not exist");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);

                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                topupService.setBatch(batchList);
                topupService.setInvoice(invoiceList);

                int current_balance = topupService.topup();

                jo.put("current_balance", current_balance);
            }

            jo.put("result_code", "B00");
            jo.put("message", "Success");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            jo.put("timestamp", dateFormatter.getTimestamp());
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());

        }catch(RuntimeException e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +e);

            jo.put("result_code", "B06");
            jo.put("message", "Failed topup");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("error", e.toString());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Failed process topup : " + uuid));
            }catch(Exception t)
            {
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +t);
            }

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }
}