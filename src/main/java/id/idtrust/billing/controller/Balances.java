package id.idtrust.billing.controller;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.model.*;
import id.idtrust.billing.repository.*;
import id.idtrust.billing.util.DateFormatter;
import id.idtrust.billing.util.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping(value = "/billing")
public class Balances extends Description {

    private static final Logger logger = LogManager.getLogger();
    static Description ds = new Description();
    @Autowired
    ProductRepository productRepository;
    @Autowired
    BalanceRepository BalancesRepo;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    BatchRepository batchRepository;
    @Autowired
    BillRepository billRepository;

    DateFormatter dateFormatter = new DateFormatter();

    @PostMapping(value="/balance/renew", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> renewExpired(@RequestBody String data) throws Exception {
        long startTime = System.nanoTime();

        InetAddress inetAddress = null;

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif="Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+ds.VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);

        JSONObject jo=new JSONObject();

        if(!rjson.has("xkey"))
        {
            jo.put("result_code", "B28");
            jo.put("message", "Missing parameter xkey");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + jo);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        try {
            List<Account> account = accountRepository.findAccount(rjson.getString("xkey"));

            if(account.size() < 1)
            {
                jo.put("result_code", "B04");
                jo.put("message", "Account not exist");
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + jo);
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            for(int i=0 ; i < account.size() ; i++)
            {
                Batch batch = batchRepository.findBatchByAccount(account.get(i).getId());

                if(batch != null)
                {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new Date());
                    calendar.add(Calendar.YEAR, 1);

                    batch.setExpired(calendar.getTime());

                    batchRepository.save(batch);
                }
            }

            jo.put("result_code", "B00");
            jo.put("message", "Success renew active balances");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + jo);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            notif="Failed renew active balances";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error get balance : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Externalkey : " + "\n externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            }
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/usage", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> usage(@RequestBody String data) throws Exception {
        long startTime = System.nanoTime();

        InetAddress inetAddress = null;

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif="Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+ds.VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);

        JSONObject jo=new JSONObject();

        if(!rjson.has("xkey"))
        {
            jo.put("result_code", "B28");
            jo.put("message", "Missing parameter xkey");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + jo);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        try {
            if(!rjson.has("product"))
            {
                List<Account> accountexternalkey = accountRepository.findAccount(rjson.getString("xkey"));

                JSONArray allbalance = new JSONArray();

                if(accountexternalkey.size() > 0)
                {
                    for(int i = 0; i < accountexternalkey.size() ; i++)
                    {
                        Batch batch = batchRepository.findPostpaidByAccountRecord(accountexternalkey.get(i).getId());
                        if(batch != null)
                        {
                            JSONObject jsonObject = new JSONObject();

                            jsonObject.put("product", accountexternalkey.get(i).getProduct().getPkey());
                            jsonObject.put("subscription", accountexternalkey.get(i).getSubscription());
                            jsonObject.put("usage", batch.getUsage());
                            jsonObject.put("closing_date", batch.getClosingDate());
                            jsonObject.put("title_id", accountexternalkey.get(i).getProduct().getNameProduct());
                            jsonObject.put("title_en", accountexternalkey.get(i).getProduct().getNameProductEn());
                            jsonObject.put("detail_id", accountexternalkey.get(i).getProduct().getDetailId());
                            jsonObject.put("detail_en", accountexternalkey.get(i).getProduct().getDetailEn());

                            Bill bill = billRepository.findByAccountId(accountexternalkey.get(i).getId());

                            if(bill != null)
                            {
                                JSONObject dataUnpaid = new JSONObject();

                                dataUnpaid.put("total_usage",bill.getTotal());
                                dataUnpaid.put("due_date", bill.getDueDate());
                                dataUnpaid.put("status",bill.getStatus());
                                dataUnpaid.put("product", bill.getBatch().getAccount().getProduct().getPkey());
                                dataUnpaid.put("period", bill.getBatch().getOpenDate() + "-" + bill.getBatch().getClosingDate());

                                jsonObject.put("unpaid", dataUnpaid);
                            }

                            allbalance.put(jsonObject);
                        }
                        else
                        {
                            JSONObject jsonObject = new JSONObject();

                            jsonObject.put("product", accountexternalkey.get(i).getProduct().getPkey());
                            jsonObject.put("subscription", accountexternalkey.get(i).getSubscription());
                            jsonObject.put("usage", 0);
                            jsonObject.put("title_id", accountexternalkey.get(i).getProduct().getNameProduct());
                            jsonObject.put("title_en", accountexternalkey.get(i).getProduct().getNameProductEn());
                            jsonObject.put("detail_id", accountexternalkey.get(i).getProduct().getDetailId());
                            jsonObject.put("detail_en", accountexternalkey.get(i).getProduct().getDetailEn());

                            allbalance.put(jsonObject);
                        }
                    }
                }
                else
                {
                    notif="Account not exist";
                    jo.put("result_code", "B04");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                if(allbalance.length() > 0)
                {
                    jo.put("result_code", "B00");
                    jo.put("message", "Success");
                    jo.put("xkey", rjson.getString("xkey"));

                    jo.put("balance", allbalance);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
                else
                {
                    notif="Account not exist";
                    jo.put("result_code", "B04");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
            }
            else
            {
                Product productData = productRepository.findByPkey(rjson.getString("product"));
                if(productData == null)
                {
                    notif="Product not exist";
                    jo.put("result_code", "B01");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                Account account = accountRepository.findByExternalKey(rjson.getString("xkey"), productData.getId());

                if(account != null)
                {
                    JSONObject obalance = new JSONObject();

                    Batch batch = batchRepository.findPostpaidByAccountRecord(account.getId());
                    if(batch != null)
                    {
                        obalance.put("product", account.getProduct().getPkey());
                        obalance.put("subscription", account.getSubscription());
                        obalance.put("usage", batch.getUsage());
                        obalance.put("closing_date", batch.getClosingDate());
                        obalance.put("title_id", account.getProduct().getNameProduct());
                        obalance.put("title_en", account.getProduct().getNameProductEn());
                        obalance.put("detail_id", account.getProduct().getDetailId());
                        obalance.put("detail_en", account.getProduct().getDetailEn());

                        Bill bill = billRepository.findByAccountId(account.getId());

                        if(bill != null)
                        {
                            JSONObject dataUnpaid = new JSONObject();

                            dataUnpaid.put("total_usage",bill.getTotal());
                            dataUnpaid.put("due_date", bill.getDueDate());
                            dataUnpaid.put("status",bill.getStatus());
                            dataUnpaid.put("product", bill.getBatch().getAccount().getProduct().getPkey());
                            dataUnpaid.put("period", bill.getBatch().getOpenDate() + "-" + bill.getBatch().getClosingDate());

                            obalance.put("unpaid", dataUnpaid);
                        }
                    }
                    else
                    {
                        obalance.put("product", account.getProduct().getPkey());
                        obalance.put("subscription", account.getSubscription());
                        obalance.put("usage", 0);
                        obalance.put("title_id", account.getProduct().getNameProduct());
                        obalance.put("title_en", account.getProduct().getNameProductEn());
                        obalance.put("detail_id", account.getProduct().getDetailId());
                        obalance.put("detail_en", account.getProduct().getDetailEn());
                    }


                    jo.put("result_code", "B00");
                    jo.put("message", "Success");
                    jo.put("xkey", rjson.getString("xkey"));
                    jo.put("balance", obalance);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
                else
                {
                    notif="Account not exist";
                    jo.put("result_code", "B04");
                    jo.put("message", notif);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            notif="Failed get balance";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error get balance : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Externalkey : " + "\n externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            }
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/balance", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> balances(@RequestBody String data) throws Exception {
        long startTime = System.nanoTime();

        InetAddress inetAddress = null;

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif="Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+ds.VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);

        JSONObject jo=new JSONObject();

        if(!rjson.has("xkey"))
        {
            jo.put("result_code", "B28");
            jo.put("message", "Missing parameter xkey");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + jo);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        try {
            if(!rjson.has("product"))
            {
                List<Account> accountexternalkey = accountRepository.findAccount(rjson.getString("xkey"));

                JSONArray allbalance = new JSONArray();

                if(accountexternalkey.size() > 0)
                {
                    for(int i = 0; i < accountexternalkey.size() ; i++)
                    {
                        if(accountexternalkey.get(i).getSubscription().equals("prepaid"))
                        {
                            if (!accountexternalkey.get(i).getProduct().getPkey().equals("sms"))
                            {
                                if (rjson.has("type"))
                                {
                                    if (rjson.getString("type").equalsIgnoreCase("signing"))
                                    {
                                        if (accountexternalkey.get(i).getProduct().getCategory().equals("signer"))
                                        {
                                            Balance balances = BalancesRepo.saldo(accountexternalkey.get(i).getId());
                                            if(balances != null)
                                            {
                                                JSONObject jsonObject = new JSONObject();
                                                jsonObject.put("product", accountexternalkey.get(i).getProduct().getPkey());
                                                jsonObject.put("subscription", accountexternalkey.get(i).getSubscription());
                                                jsonObject.put("amount", balances.getBalance());
                                                jsonObject.put("title_id", accountexternalkey.get(i).getProduct().getNameProduct());
                                                jsonObject.put("title_en", accountexternalkey.get(i).getProduct().getNameProductEn());
                                                jsonObject.put("detail_id", accountexternalkey.get(i).getProduct().getDetailId());
                                                jsonObject.put("detail_en", accountexternalkey.get(i).getProduct().getDetailEn());
                                                if (batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()) != null) {
                                                    jsonObject.put("expired", dateFormatter.getTimestamp(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()).getExpired()));
                                                }
                                                allbalance.put(jsonObject);
                                            }
                                            else {
                                                JSONObject jsonObject = new JSONObject();
                                                jsonObject.put("product", accountexternalkey.get(i).getProduct().getPkey());
                                                jsonObject.put("subscription", accountexternalkey.get(i).getSubscription());
                                                jsonObject.put("amount", new BigDecimal(0));
                                                jsonObject.put("title_id", accountexternalkey.get(i).getProduct().getNameProduct());
                                                jsonObject.put("title_en", accountexternalkey.get(i).getProduct().getNameProductEn());
                                                jsonObject.put("detail_id", accountexternalkey.get(i).getProduct().getDetailId());
                                                jsonObject.put("detail_en", accountexternalkey.get(i).getProduct().getDetailEn());
                                                if (batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()) != null) {
                                                    jsonObject.put("expired", dateFormatter.getTimestamp(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()).getExpired()));
                                                }
                                                allbalance.put(jsonObject);
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    Balance balances = BalancesRepo.saldo(accountexternalkey.get(i).getId());
                                    if (balances != null)
                                    {
                                        JSONObject jsonObject = new JSONObject();

                                        jsonObject.put("product", accountexternalkey.get(i).getProduct().getPkey());
                                        jsonObject.put("subscription", accountexternalkey.get(i).getSubscription());
                                        jsonObject.put("amount", balances.getBalance());
                                        jsonObject.put("title_id", accountexternalkey.get(i).getProduct().getNameProduct());
                                        jsonObject.put("title_en", accountexternalkey.get(i).getProduct().getNameProductEn());
                                        jsonObject.put("detail_id", accountexternalkey.get(i).getProduct().getDetailId());
                                        jsonObject.put("detail_en", accountexternalkey.get(i).getProduct().getDetailEn());
                                        if(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()) != null)
                                        {
                                            jsonObject.put("expired", dateFormatter.getTimestamp(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()).getExpired()));
                                        }
                                        allbalance.put(jsonObject);
                                    }
                                    else
                                    {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("product", accountexternalkey.get(i).getProduct().getPkey());
                                        jsonObject.put("subscription", accountexternalkey.get(i).getSubscription());
                                        jsonObject.put("amount", new BigDecimal(0));
                                        jsonObject.put("title_id", accountexternalkey.get(i).getProduct().getNameProduct());
                                        jsonObject.put("title_en", accountexternalkey.get(i).getProduct().getNameProductEn());
                                        jsonObject.put("detail_id", accountexternalkey.get(i).getProduct().getDetailId());
                                        jsonObject.put("detail_en", accountexternalkey.get(i).getProduct().getDetailEn());

                                        if(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()) != null)
                                        {
                                            jsonObject.put("expired", dateFormatter.getTimestamp(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()).getExpired()));
                                        }
                                        allbalance.put(jsonObject);
                                    }
                                }
                            }
                        }
                        else
                        {
                            Batch batch = batchRepository.findPostpaidByAccountRecord(accountexternalkey.get(i).getId());
                            if(batch != null)
                            {
                                JSONObject jsonObject = new JSONObject();

                                jsonObject.put("product", accountexternalkey.get(i).getProduct().getPkey());
                                jsonObject.put("subscription", accountexternalkey.get(i).getSubscription());
                                jsonObject.put("usage", batch.getUsage());
                                jsonObject.put("closing_date", batch.getClosingDate());
                                jsonObject.put("title_id", accountexternalkey.get(i).getProduct().getNameProduct());
                                jsonObject.put("title_en", accountexternalkey.get(i).getProduct().getNameProductEn());
                                jsonObject.put("detail_id", accountexternalkey.get(i).getProduct().getDetailId());
                                jsonObject.put("detail_en", accountexternalkey.get(i).getProduct().getDetailEn());
                                if(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()) != null)
                                {
                                    jsonObject.put("expired", dateFormatter.getTimestamp(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()).getExpired()));
                                }
                                allbalance.put(jsonObject);
                            }
                            else
                            {
                                JSONObject jsonObject = new JSONObject();

                                jsonObject.put("product", accountexternalkey.get(i).getProduct().getPkey());
                                jsonObject.put("subscription", accountexternalkey.get(i).getSubscription());
                                jsonObject.put("usage", 0);
                                jsonObject.put("title_id", accountexternalkey.get(i).getProduct().getNameProduct());
                                jsonObject.put("title_en", accountexternalkey.get(i).getProduct().getNameProductEn());
                                jsonObject.put("detail_id", accountexternalkey.get(i).getProduct().getDetailId());
                                jsonObject.put("detail_en", accountexternalkey.get(i).getProduct().getDetailEn());
                                if(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()) != null)
                                {
                                    jsonObject.put("expired", dateFormatter.getTimestamp(batchRepository.findBatchByAccount(accountexternalkey.get(i).getId()).getExpired()));
                                }
                                allbalance.put(jsonObject);
                            }
                        }
                    }
                }
                else
                {
                    notif="Account not exist";
                    jo.put("result_code", "B04");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                if(allbalance.length() > 0)
                {
                    jo.put("result_code", "B00");
                    jo.put("message", "Success");
                    jo.put("xkey", rjson.getString("xkey"));
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("balance", allbalance);
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
                else
                {
                    notif="Account not exist";
                    jo.put("result_code", "B04");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
            }
            else
            {
                Product productData = productRepository.findByPkey(rjson.getString("product"));
                if(productData == null)
                {
                    notif="Product not exist";
                    jo.put("result_code", "B01");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                Account account = accountRepository.findByExternalKey(rjson.getString("xkey"), productData.getId());

                if(account != null)
                {
                    JSONObject obalance = new JSONObject();
                    if(account.getSubscription().equals("prepaid")) {

                        Balance mb = BalancesRepo.saldo(account.getId());
                        if (mb == null) {
                            notif = "Account not exist";
                            jo.put("result_code", "B04");
                            jo.put("message", notif);
                            jo.put("timestamp", dateFormatter.getTimestamp());
                            jo.put("log", uuid);
                            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                            logger.info("[" + ds.VERSION + "]-[BILLTRUST/INFO] : " + jo);
                            return ResponseEntity
                                    .status(200)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jo.toString());
                        }
                        logger.info("[" + ds.VERSION + "]-[BILLTRUST/INFO] : " + "Value balance : " + mb.getBalance());

                        obalance.put("product", productData.getPkey());
                        obalance.put("amount", mb.getBalance());

                        obalance.put("product", account.getProduct().getPkey());
                        obalance.put("subscription", account.getSubscription());
                        obalance.put("title_id", account.getProduct().getNameProduct());
                        obalance.put("title_en", account.getProduct().getNameProductEn());
                        obalance.put("detail_id", account.getProduct().getDetailId());
                        obalance.put("detail_en", account.getProduct().getDetailEn());
                        if(batchRepository.findBatchByAccount(account.getId()) != null)
                        {
                            obalance.put("expired", dateFormatter.getTimestamp(batchRepository.findBatchByAccount(account.getId()).getExpired()));
                        }
                    }
                    else
                    {
                        Batch batch = batchRepository.findPostpaidByAccountRecord(account.getId());
                        if(batch != null)
                        {
                            obalance.put("product", account.getProduct().getPkey());
                            obalance.put("subscription", account.getSubscription());
                            obalance.put("usage", batch.getUsage());
                            obalance.put("closing_date", batch.getClosingDate());
                            obalance.put("title_id", account.getProduct().getNameProduct());
                            obalance.put("title_en", account.getProduct().getNameProductEn());
                            obalance.put("detail_id", account.getProduct().getDetailId());
                            obalance.put("detail_en", account.getProduct().getDetailEn());
                            if(batchRepository.findBatchByAccount(account.getId()) != null) {
                                obalance.put("expired", dateFormatter.getTimestamp(batchRepository.findBatchByAccount(account.getId()).getExpired()));
                            }
                        }
                        else
                        {
                            obalance.put("product", account.getProduct().getPkey());
                            obalance.put("subscription", account.getSubscription());
                            obalance.put("usage", 0);
                            obalance.put("title_id", account.getProduct().getNameProduct());
                            obalance.put("title_en", account.getProduct().getNameProductEn());
                            obalance.put("detail_id", account.getProduct().getDetailId());
                            obalance.put("detail_en", account.getProduct().getDetailEn());
                            if(batchRepository.findBatchByAccount(account.getId()) != null) {
                                obalance.put("expired", dateFormatter.getTimestamp(batchRepository.findBatchByAccount(account.getId()).getExpired()));
                            }
                        }
                    }

                    jo.put("result_code", "B00");
                    jo.put("message", "Success");
                    jo.put("xkey", rjson.getString("xkey"));
                    jo.put("balance", obalance);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
                else
                {
                    notif="Account not exist";
                    jo.put("result_code", "B04");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
            }

        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            notif="Failed get balance";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error get balance : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Externalkey : " + "\n externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            }
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }
}