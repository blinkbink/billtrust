package id.idtrust.billing.broker.kafka;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.model.Account;
import id.idtrust.billing.model.Invoice;
import id.idtrust.billing.model.Product;
import id.idtrust.billing.repository.AccountRepository;
import id.idtrust.billing.repository.BatchRepository;
import id.idtrust.billing.repository.InvoiceRepository;
import id.idtrust.billing.repository.ProductRepository;
import id.idtrust.billing.service.TransactionService;
import id.idtrust.billing.util.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;


@EnableKafka
@Service
public class Listener {

    @Autowired
    ProductRepository productRepository;
    @Autowired
    BatchRepository batchRepository;
    @Autowired
    AccountRepository accountsRepo;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    TransactionService transactionService;

    static Description ds = new Description();
    private static final Logger logger = LogManager.getLogger();

    @KafkaListener(topics = "charge_billing")
    public void listenerTransaction(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,  @Header(KafkaHeaders.OFFSET) int offset) throws JSONException {

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

        JSONObject rjson=null;
        try {
            rjson = new JSONObject(message);
            logger.info("["+ds.VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + "OFFSET " + offset + ", PARTITION" + partition);
        }
        catch(Exception e)
        {
            logger.error(e.toString());
            return;
        }

        JSONObject jo = new JSONObject();

        if (!rjson.has("xkey")) {
            notif = "Missing parameter xkey";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return;
        }

        if (!rjson.has("product")) {
            notif = "Missing parameter product";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return;
        }
        if (!rjson.has("amount")) {
            notif = "Missing parameter amount";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return;
        }

        if (rjson.getString("amount").equals("0")) {
            notif = "Invalid request amount can't 0";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return;
        }

        Account acc = null;
        Product pKey = productRepository.findByPkey(rjson.getString("product"));
        try {


            if (pKey == null) {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Product not exist");
                notif = "Product not exist";
                jo.put("result_code", "B01");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return;
            }

            acc = accountsRepo.findByExternalKey(rjson.getString("xkey"), pKey.getId());

            if (acc == null) {
                notif = "Account not exist";
                jo.put("result_code", "B04");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return;
            } else {
                if (acc.getSubscription().equals("prepaid")) {
                    if (rjson.has("item")) {
                        if (rjson.getString("product").equals("document")) {
                            //cek dokumen id
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Document ID : " + rjson.getLong("item"));
                            List<Invoice> existingTransaction;
                            List<Invoice> existingReversal;
                            List<Invoice> existingTransactioncheck;

                            existingTransactioncheck = invoiceRepository.getItemcheck(rjson.getLong("item"));
                            if (existingTransactioncheck.size() != 0) {
                                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"TRX 1");
                                existingTransaction = invoiceRepository.getItem(rjson.getLong("item"), existingTransactioncheck.get(0).getInvoiceId());

                                existingReversal = invoiceRepository.getItemReversal(rjson.getLong("item"), existingTransaction.get(0).getInvoiceId());
                                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"REVERSAL size :" + existingReversal.size());
                            } else {
                                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"TRX 0 REVERSAL 0");
                                existingTransaction = existingTransactioncheck;
                                existingReversal = invoiceRepository.getItemReversalcheck(rjson.getLong("item"));
                            }

                            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"existingTransaction size : " + existingTransaction.size() +" existingReversal size : " + existingReversal.size());

                            if (existingTransaction.size() >= 1 && existingReversal.size() <= 0) {
                                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Current balance existing : " + existingTransaction.get(0).getCurrentBalance());
                                notif = "Success create transaction";
                                jo.put("result_code", "B00");
                                jo.put("message", notif);
                                jo.put("current_balance", existingTransaction.get(0).getCurrentBalance());
                                jo.put("invoiceid", existingTransaction.get(0).getInvoiceId());
                                jo.put("log", uuid);
                                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                                jo.put("datetime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(existingTransaction.get(0).getCreatedDate()));
                                jo.put("product", rjson.getString("product"));
                                jo.put("xkey", rjson.getString("xkey"));
                                jo.put("name", existingTransaction.get(0).getAccount().getName());

                                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                                return;
                            }
                        }
                    }

                    try {
                        //process logic transaction
                        jo = transactionService.prepaidTransaction(rjson, acc, startTime, uuid, inetAddress, invoiceId);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                        logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +e);

                        notif = "Failed create transaction";
                        jo.put("result_code", "B06");
                        jo.put("error", e.toString());
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                        try {
                            TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                        } catch (Exception t) {
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                        }

                        return;

                    }
                    return;

                }//end if akun adalah prepaid
                else {
                    try {
                        //process logic transaction
                        jo = transactionService.postpaidTransaction(rjson, acc, startTime, uuid, inetAddress, invoiceId, pKey);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                        logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +e);

                        notif = "Failed create transaction";
                        jo.put("result_code", "B06");
                        jo.put("error", e.toString());
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                        try {
                            TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                        } catch (Exception t) {
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                        }

                        return;

                    }
                    return;

                }//end else akun adalah postpaid

            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +e);

            notif = "Failed create transaction";
            jo.put("result_code", "B06");
            jo.put("error", e.toString());
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            try {
                TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
            } catch (Exception t) {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
            }

            return;
        }

    }

    @KafkaListener(topics = "reversal_billing")
    public void listenerReversal(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,  @Header(KafkaHeaders.OFFSET) int offset) throws JSONException {

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

        JSONObject rjson=null;
        try {
            rjson = new JSONObject(message);
            logger.info("["+ds.VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + "OFFSET " + offset + ", PARTITION" + partition);
        }
        catch(Exception e)
        {
            logger.error(e.toString());
            return;
        }


        JSONObject jo = new JSONObject();

        if(!rjson.has("invoiceid"))
        {
            notif="Missing parameter invoiceid";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return;
        }

        try {

            List<Invoice> datainv = invoiceRepository.findByInvoiceId(rjson.getString("invoiceid"));

            if(datainv.size() < 1)
            {
                notif="invoiceid not exist";
                jo.put("result_code", "B09");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return;
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
                        logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
                        notif="Error revers transaction";
                        jo.put("result_code", "B06");
                        jo.put("message", notif);
                        jo.put("error", e.toString());
                        jo.put("log", uuid);
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

                        try
                        {
                            TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
                        }catch(Exception t)
                        {
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
                        }

                        return;
                    }
                }//end if akun adalah prepaid
                else
                {
                    try {
                        jo = transactionService.postpaidReversal(rjson, startTime, uuid, datainv, inetAddress);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                        logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
                        notif="Error revers transaction";
                        jo.put("result_code", "B06");
                        jo.put("message", notif);
                        jo.put("error", e.toString());
                        jo.put("log", uuid);
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

                        try
                        {
                            TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
                        }catch(Exception t)
                        {
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
                        }

                        return;
                    }
                }//end else akun adalah prepaid

            }

        }catch(RuntimeException e)
        {

            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            notif="Error revers transaction";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
            }

            return;
        }

        return;

    }
}
