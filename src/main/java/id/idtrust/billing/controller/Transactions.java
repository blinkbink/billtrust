package id.idtrust.billing.controller;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.PDF.Invoices;
import id.idtrust.billing.model.*;
import id.idtrust.billing.repository.AccountRepository;
import id.idtrust.billing.repository.BatchRepository;
import id.idtrust.billing.repository.InvoiceRepository;
import id.idtrust.billing.repository.ProductRepository;
import id.idtrust.billing.service.TopupService;
import id.idtrust.billing.service.TransactionService;
import id.idtrust.billing.util.DateFormatter;
import id.idtrust.billing.util.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping(value = "/billing")
public class Transactions extends Description {

    @Autowired
    ProductRepository productRepository;
    @Autowired
    AccountRepository accountsRepo;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    TransactionService transactionService;

    @Autowired
    TopupService topupService;

    DateFormatter dateFormatter = new DateFormatter();
    private static final Logger logger = LogManager.getLogger();

    @PostMapping(value="/claim", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> claim(@RequestBody String json) throws Exception {
        long startTime = System.nanoTime();
        String invoiceId = UUID.randomUUID().toString();
        InetAddress inetAddress = null;

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif = "Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(json);

        logger.info("[" + VERSION + "]-[BILLTRUST/REQUEST] : " + rjson.toString());

        JSONObject jo = new JSONObject();

        if (!rjson.has("xkey")) {
            notif = "Missing parameter xkey";
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

        List<Account> acc = accountsRepo.findByCertificateVerification(rjson.getString("xkey"));

        List<String> listMissing = new ArrayList<>();
        listMissing.add("verification");
        listMissing.add("certificate");
        listMissing.add("sms");

        if (acc.size() < 3) {
            String missingAccount = "verification";

            for(int i = 0 ; i < acc.size() ; i++)
            {
                for (int j = 0; j < listMissing.size(); j++)
                {
                    if (listMissing.get(j).equalsIgnoreCase(acc.get(i).getProduct().getPkey()))
                    {
                        listMissing.remove(j);
                    }
                }
            }

            notif = "Account " + listMissing.toString() + "not exist";
            jo.put("result_code", "B04");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        } else {
            List<Batch> batchClaim = new ArrayList<>();
            List<Invoice> invoiceClaim = new ArrayList<>();
            List<Topup> topupClaim = new ArrayList<>();
            List<TopupProduct> topupProductsClaim = new ArrayList<>();

            for (int i = 0; i < acc.size(); i++)
            {
                if (acc.get(i).getSubscription().equals("prepaid"))
                {
                    try {
                        //process logic prepaid topup claim
                        Topup topup = new Topup();
                        TopupProduct topupProduct = new TopupProduct();
                        Batch batch = new Batch();
                        Invoice invoice = new Invoice();

                        topup.setPrice(acc.get(i).getProduct().getPersonal_base_price());
                        topup.setAccount(acc.get(i));
                        topup.setTotal(acc.get(i).getProduct().getPersonal_base_price());
                        topup.setStatus("TOPUP_SUCCESS");
                        topup.setCreatedDate(new Date());
                        topup.setExpiredDate(null);

                        topupProduct.setPrice(acc.get(i).getProduct().getPersonal_base_price());
                        topupProduct.setTopup(topup);
                        topupProduct.setQty(1);
                        topupProduct.setUnit_price(acc.get(i).getProduct().getPersonal_base_price());
                        topupProduct.setAccount(acc.get(i));
                        topupProduct.setProduct(acc.get(i).getProduct());

                        Calendar c = Calendar.getInstance();
                        c.setTime(new Date());
                        c.add(Calendar.YEAR, 1);
                        batch.setPrice(acc.get(i).getProduct().getPersonal_base_price());
                        batch.setFree(false);
                        batch.setExpired(c.getTime());
                        batch.setAccount(acc.get(i));
                        batch.setSettled(false);
                        batch.setOpenDate(new Date());
                        batch.setQuota(1);
                        batch.setRemainingBalance(1);
                        batch.setClosingDate(null);
                        batch.setUsage(0);

                        invoice.setTopup(topup);
                        invoice.setTrx(1);
                        invoice.setBatch(batch);
                        invoice.setInvoiceId(UUID.randomUUID().toString());
                        invoice.setDescription("Claim " + acc.get(i).getProduct().getPkey());
                        invoice.setCreatedDate(new Date());
                        invoice.setAccount(acc.get(i));
                        invoice.setAmount(1);

                        topupClaim.add(topup);
                        topupProductsClaim.add(topupProduct);
                        batchClaim.add(batch);
                        invoiceClaim.add(invoice);


                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("[" + VERSION + "]-[BILLTRUST/INFO] : " + e.toString());

                        notif = "Failed create transaction";
                        jo.put("result_code", "B06");
                        jo.put("error", e.toString());
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                        try {
                            TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                        } catch (Exception t) {
                            logger.info("[" + VERSION + "]-[BILLTRUST/ERROR] : " + "Failed send message telegram");
                            logger.info("[" + VERSION + "]-[BILLTRUST/ERROR] : " + t.toString());
                        }

                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                }
//                //end if akun adalah prepaid
//                else {
//                    try {
//                        //process logic claim postpaid
//                        jo = transactionService.postpaidTransaction(rjson, acc, startTime, uuid, inetAddress, invoiceId, pKey);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        logger.error("[" + VERSION + "]-[BILLTRUST/INFO] : " + e);
//
//                        notif = "Failed process claim";
//                        jo.put("result_code", "B06");
//                        jo.put("error", e.toString());
//                        jo.put("message", notif);
//                        jo.put("log", uuid);
//                        jo.put("timestamp", dateFormatter.getTimestamp());
//                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
//                        try {
//                            TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
//                            bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
//                        } catch (Exception t) {
//                            logger.info("[" + VERSION + "]-[BILLTRUST/ERROR] : " + "Failed send message telegram");
//                            logger.info("[" + VERSION + "]-[BILLTRUST/ERROR] : " + t.toString());
//                        }
//
//                        return ResponseEntity
//                                .status(200)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .body(jo.toString());
//
//                    }
//                }//end else akun adalah postpaid
            }

            try {
                topupService.setInvoice(invoiceClaim);
                topupService.setBatch(batchClaim);
                topupService.setTopupClaim(topupClaim);
                topupService.setTopupProducts(topupProductsClaim);

                topupService.topupClaim();

                logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Success topup");

                notif = "Success claim";
                jo.put("result_code", "B00");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }catch(Exception e)
            {
                e.printStackTrace();
                logger.error("[" + VERSION + "]-[BILLTRUST/INFO] : " + e);

                notif = "Failed process claim";
                jo.put("result_code", "B06");
                jo.put("error", e.toString());
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                try {
                    TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                    bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                } catch (Exception t) {
                    logger.info("[" + VERSION + "]-[BILLTRUST/ERROR] : " + "Failed send message telegram");
                    logger.info("[" + VERSION + "]-[BILLTRUST/ERROR] : " + t);
                }

                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }
        }
    }

    @PostMapping(value="/transaction", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> transaction(@RequestBody String json) throws Exception {
        long startTime = System.nanoTime();
        String invoiceId = UUID.randomUUID().toString();
        InetAddress inetAddress = null;

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif = "Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(json);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson.toString());

        JSONObject jo = new JSONObject();

        if (!rjson.has("xkey")) {
            notif = "Missing parameter xkey";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        if (!rjson.has("product")) {
            notif = "Missing parameter product";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
        if (!rjson.has("amount")) {
            notif = "Missing parameter amount";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        if (rjson.getString("amount").equals("0")) {
            notif = "Invalid request amount can't 0";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        Account acc = null;
        Product pKey = productRepository.findByPkey(rjson.getString("product"));
        try {

            if (pKey == null) {
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Product not exist");
                notif = "Product not exist";
                jo.put("result_code", "B01");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            acc = accountsRepo.findByExternalKey(rjson.getString("xkey"), pKey.getId());

            if (acc == null) {
                notif = "Account not exist";
                jo.put("result_code", "B04");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            } else {
                if(!acc.getActive())
                {
                    notif = "inactive account";
                    jo.put("result_code", "B19");
                    jo.put("message", notif);
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                if (acc.getSubscription().equals("prepaid")) {
                    if (rjson.has("item")) {
                        if (rjson.getString("product").equals("document")) {
                            //cek dokumen id
                            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Document ID : " + rjson.getLong("item"));
                            List<Invoice> existingTransaction;
                            List<Invoice> existingReversal;
                            List<Invoice> existingTransactioncheck;

                            existingTransactioncheck = invoiceRepository.getItemcheck(rjson.getLong("item"));
                            if (existingTransactioncheck.size() != 0) {
                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"TRX 1");
                                existingTransaction = invoiceRepository.getItem(rjson.getLong("item"), existingTransactioncheck.get(0).getInvoiceId());

                                existingReversal = invoiceRepository.getItemReversal(rjson.getLong("item"), existingTransaction.get(0).getInvoiceId());
                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"REVERSAL size :" + existingReversal.size());
                            } else {
                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"TRX 0");
                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"REVERSAL 0");
                                existingTransaction = existingTransactioncheck;
                                existingReversal = invoiceRepository.getItemReversalcheck(rjson.getLong("item"));
                            }

                            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"existingTransaction size : " + existingTransaction.size());
                            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"existingReversal size : " + existingReversal.size());

                            if (existingTransaction.size() >= 1 && existingReversal.size() <= 0) {
                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Current balance existing : " + existingTransaction.get(0).getCurrentBalance());
                                notif = "Success create transaction";
                                jo.put("result_code", "B00");
                                jo.put("message", notif);
                                jo.put("current_balance", existingTransaction.get(0).getCurrentBalance());
                                jo.put("invoiceid", existingTransaction.get(0).getInvoiceId());
                                jo.put("log", uuid);
                                jo.put("timestamp", dateFormatter.getTimestamp());
                                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                                jo.put("product", rjson.getString("product"));
                                jo.put("xkey", rjson.getString("xkey"));
                                jo.put("name", existingTransaction.get(0).getAccount().getName());

                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                                return ResponseEntity
                                        .status(200)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(jo.toString());
                            }
                        }
                    }

                    try {
                        //process logic transaction
                        jo = transactionService.prepaidTransaction(rjson, acc, startTime, uuid, inetAddress, invoiceId);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                        logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +e.toString());

                        notif = "Failed create transaction";
                        jo.put("result_code", "B06");
                        jo.put("error", e.toString());
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                        try {
                            TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                        } catch (Exception t) {
                            logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
                        }

                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());

                    }
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());

                }//end if akun adalah prepaid
                else {
                    try {
                        //process logic transaction
                        jo = transactionService.postpaidTransaction(rjson, acc, startTime, uuid, inetAddress, invoiceId, pKey);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                        logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +e.toString());

                        notif = "Failed create transaction";
                        jo.put("result_code", "B06");
                        jo.put("error", e.toString());
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                        try {
                            TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                        } catch (Exception t) {
                            logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
                        }

                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());

                    }
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());

                }//end else akun adalah postpaid

            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +e.toString());

            notif = "Failed create transaction";
            jo.put("result_code", "B06");
            jo.put("error", e.toString());
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            try {
                TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
            } catch (Exception t) {
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
            }

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/reversal", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> reversal(@RequestBody String data) throws Exception {
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

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson.toString());

        JSONObject jo = new JSONObject();
        String notif="Account already exist";

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        if(!rjson.has("invoiceid"))
        {
            notif="Missing parameter invoiceid";
            jo.put("result_code", "B28");
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

        try {

            List<Invoice> datainv = invoiceRepository.findByInvoiceId(rjson.getString("invoiceid"));

            if(datainv.size() < 1)
            {
                notif="invoiceid not exist";
                jo.put("result_code", "B09");
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
                if(!datainv.get(0).getAccount().getSubscription().equals("Postpaid"))
                {
                    try {
                        jo = transactionService.prepaidReversal(rjson, startTime, uuid, datainv, inetAddress);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                        logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
                        notif="Error revers transaction";
                        jo.put("result_code", "B06");
                        jo.put("message", notif);
                        jo.put("error", e.toString());
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

                        try
                        {
                            TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
                        }catch(Exception t)
                        {
                            logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
                        }

                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                }//end if akun adalah prepaid
                else
                {
                    try {
                        jo = transactionService.postpaidReversal(rjson, startTime, uuid, datainv, inetAddress);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                        logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
                        notif="Error revers transaction";
                        jo.put("result_code", "B06");
                        jo.put("message", notif);
                        jo.put("error", e.toString());
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

                        try
                        {
                            TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
                        }catch(Exception t)
                        {
                            logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
                        }

                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                }//end else akun adalah prepaid
            }

        }catch(RuntimeException e)
        {

            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            notif="Error revers transaction";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
            }

             return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
        }

        return ResponseEntity
                .status(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jo.toString());
    }
}