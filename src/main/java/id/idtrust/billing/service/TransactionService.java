package id.idtrust.billing.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.broker.kafka.DataNotification;
import id.idtrust.billing.broker.kafka.NotificationAlert;
import id.idtrust.billing.model.*;
import id.idtrust.billing.repository.*;
import id.idtrust.billing.util.DateFormatter;
import id.idtrust.billing.util.Description;
import id.idtrust.billing.util.LogSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(rollbackFor = Exception.class, timeout=25)
public class TransactionService {

    @Autowired
    InvoiceRepository invoiceRepository;
    
    @Autowired
    AccountRepository accountsRepo;
    
    @Autowired
    BalanceRepository balanceRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    BatchRepository batchRepository;

    @Autowired
    BillRepository billRepository;

    KafkaTemplate<String, NotificationAlert> kafkaNotification;

    DateFormatter dateFormatter = new DateFormatter();
    static Description ds = new Description();
    private static final Logger logger = LogManager.getLogger();



    @Transactional(rollbackFor = Exception.class, timeout=25)
    public JSONObject prepaidTransaction(JSONObject rjson, Account acc, long startTime, String uuid, InetAddress inetAddress, String invoiceId) throws Exception {
        Date date = new Date();
        String[] masterBalancemines;
        String[] masterBalanceminesnext = null;
        NumberFormat formatter = new DecimalFormat("#0.00000");
        
        JSONObject jo = new JSONObject();

        Invoice invtrx = new Invoice();
        Invoice invtrxnext = null;

        try {
            //Potong update saldo ke master balance
            long getspbalance = System.nanoTime();

            masterBalancemines = invoiceRepository.SPBalanceMines(acc.getId(), rjson.getInt("amount")).split(",");

            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Try create transaction charge amount : ");

            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Success charge amount : " + masterBalancemines[0]);
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Time Get SP Balance : " + formatter.format((System.nanoTime() - getspbalance)/1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Balance from SP : " + masterBalancemines[0]);

        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +e);
            
            jo.put("result_code", "B06");
            jo.put("error", e.toString());
            jo.put("message", "Failed create transaction");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\nMessage : " + e));
            }catch(Exception t)
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
            }

            throw new Exception(e.toString());
        }

        try
        {
            if (masterBalancemines[0] != null)
            {
                if (Integer.parseInt(masterBalancemines[0])  < 0)
                {
                    if(Integer.parseInt(masterBalancemines[0]) <= -2)
                    {
                        jo.put("result_code", "B06");
                        jo.put("message", "Failed create transaction");
                        jo.put("error", "Function balance error");
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        jo.put("product", rjson.getString("product"));
                        jo.put("xkey", rjson.getString("xkey"));
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

                        try
                        {
                            TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : SP -2"));
                        }catch(Exception t)
                        {
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                        }
                        return jo;
                    }
                    else
                    {
                        //Try send email notif balance
                        long queryemail = System.nanoTime();
                        try {
                            JSONArray saldo = new JSONArray();

                            if(!acc.getAlertBalance() && acc.getIsAlertBalance())
                            {
                                List<DataNotification> listDN = new ArrayList<DataNotification>();

                                List<Account> accountlist = accountsRepo.findAccount(rjson.getString("xkey"));
                                if(accountlist.size() > 0)
                                {
                                    for(int i = 0; i < accountlist.size() ; i++)
                                    {
                                        if(accountlist.get(i).getIsAlertBalance())
                                        {
                                            Balance mb = balanceRepository.saldo(accountlist.get(i).getId());

                                            JSONObject jsonObject = new JSONObject();

                                            jsonObject.put("product", accountlist.get(i).getProduct().getPkey());

                                            DataNotification data = new DataNotification();

                                            data.setType_balance(accountlist.get(i).getProduct().getNameProduct());
                                            data.setType_balance_en(accountlist.get(i).getProduct().getNameProductEn());

                                            if(mb != null)
                                            {
                                                jsonObject.put("amount", mb.getBalance());
                                                data.setBalance(mb.getBalance().intValue());
                                            }
                                            else
                                            {
                                                jsonObject.put("amount", 0);
                                                data.setBalance(0);
                                            }

                                            saldo.put(jsonObject);
                                            listDN.add(data);
                                        }

                                    }
                                }

                                acc.setAlertBalance(true);
                                acc.setUpdatedDate(new Date());

                                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"SALDO : " + saldo);

                                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Sending email warning minimum balance to : " + rjson.getString("xkey") + " " + acc.getEmail());

                                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"List saldo yang dikirim peringatan saldo : " +  saldo);
                                try {
//										  				action.kirim(acc.getName(), acc.getEmail(), saldo);
                                    JSONObject kirim = new JSONObject();

                                    kirim.put("name", acc.getName());
                                    String email = acc.getEmail();
                                    if(acc.getInternal_email() != null)
                                    {
                                        email = email + "," + acc.getInternal_email();
                                    }
                                    kirim.put("email", email);
                                    kirim.put("saldo", saldo);

                                    try {
                                        //send to message broker
                                        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Send to kafka");

                                        NotificationAlert notifProducer = new NotificationAlert();
                                        notifProducer.setName(acc.getName());
                                        notifProducer.setEmail(acc.getEmail());
                                        notifProducer.setData(listDN);
                                        notifProducer.setTRACE_ID(uuid);
                                        notifProducer.setTimestamp(new Date().toInstant().plus(Duration.ofHours(7)).truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", "+07:00"));
                                        notifProducer.setUser_id(acc.getXkey().replaceAll("[^0-9]", ""));

//                                        Boolean status = kafka.sendNotification(notifProducer, "alert_notification", uuid);

                                        kafkaNotification.send("alert_notification", notifProducer);
//                                        if(!status)
//                                        {
//                                            try
//                                            {
//                                                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
//                                                bot.execute(new SendMessage(213382980, "Failed process callback payment : " + uuid));
//                                            }catch(Exception t)
//                                            {
//                                                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
//                                                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
//                                            }
//                                        }

                                    }catch(Exception e)
                                    {
                                        e.printStackTrace();
                                        jo.put("error", e.toString());
                                        logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Failed send to message broker");
                                    }

                                    accountsRepo.save(acc);
//                                                new InvoicesDao(db).updateEmail(acc);
                                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Sukses kirim notifikasi saldo");
                                }catch (Exception e)
                                {
                                    logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Gagal kirim notifikasi saldo");
                                    e.printStackTrace();

                                    try
                                    {
                                        TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                                        bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed send email notification minimum balance log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                                    }catch(Exception t)
                                    {
                                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                                    }
                                }
                            }
//									  		}
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Running email checking : " + formatter.format((System.nanoTime() - queryemail)/1000000000d));
                        }catch(Exception e)
                        {
                            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);

                            try
                            {
                                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                                bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed send email notification minimum balance log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                            }catch(Exception t)
                            {
                                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                            }
                        }
                        
                        jo.put("result_code", "B03");
                        jo.put("message", "Insufficient balance");
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        jo.put("product", rjson.getString("product"));
                        jo.put("xkey", rjson.getString("xkey"));
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

                        return jo;
                    }
                }
                else
                {
                    try {
                        Batch batch = new Batch();
                        batch.setId(Integer.parseInt(masterBalancemines[1]));

                        invtrx.setAccount(acc);
                        invtrx.setAmount(rjson.getInt("amount") * -1);
                        invtrx.setCreatedDate(date);
                        invtrx.setDescription(rjson.getString("description"));
                        invtrx.setInvoiceId(invoiceId);
//                        invtrx.setType("USAGE");
                        invtrx.setCurrentBalance(Integer.parseInt(masterBalancemines[0]));
                        invtrx.setTrx(2);
                        invtrx.setBatch(batch);

                        if(rjson.has("referral"))
                        {
                            invtrx.setReferral(rjson.getString("referral"));
                        }

                        if(rjson.has("item"))
                        {
                            invtrx.setItemId(rjson.getString("item"));
                        }

                        invoiceRepository.save(invtrx);

                        boolean sisa = true;
                        while(sisa)
                        {
                            if(Integer.parseInt(masterBalancemines[2]) > 0)
                            {
                                invtrxnext =  new Invoice();
                                try {
                                    //Potong update saldo ke master balance
                                    long getspbalance = System.nanoTime();

                                    //masterBalancemines = ido.SPBalanceMines(rjson.getString("xkey"), acc.getProduct().getId(), BigDecimal.valueOf(rjson.getLong("amount")), acc.getId());
                                    int kurang = Integer.parseInt(masterBalancemines[2]);

                                    if(masterBalanceminesnext != null)
                                    {
                                        kurang = Integer.parseInt(masterBalanceminesnext[2]);
                                    }

//                                                masterBalanceminesnext = invoiceRepository.SPBalanceMines(acc.getId(), kurang, acc.getId().intValue());
                                    masterBalanceminesnext = invoiceRepository.SPBalanceMines(acc.getId(), kurang).split(",");

                                    if(Integer.parseInt(masterBalanceminesnext[2]) < 0)
                                    {
                                        jo.put("result_code", "B06");
                                        jo.put("error", "Function balance error");
                                        jo.put("message", "Failed create transaction");
                                        jo.put("log", uuid);
                                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                                        jo.put("product", rjson.getString("product"));
                                        jo.put("xkey", rjson.getString("xkey"));

                                        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                                        return jo;
                                    }
                                    if(Integer.parseInt(masterBalanceminesnext[2]) == 0)
                                    {
                                        sisa = false;
                                    }

                                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Try create transaction charge amount next : ");

                                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Success charge amount : " + masterBalanceminesnext[0]);
                                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Time Get SP Balance : " + formatter.format((System.nanoTime() - getspbalance)/1000000000d));
                                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Balance from SP : " + masterBalanceminesnext[0]);
                                }catch(Exception e)
                                {
                                    logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
                                    
                                    jo.put("result_code", "B06");
                                    jo.put("error", e.toString());
                                    jo.put("message", "Failed create transaction");
                                    jo.put("log", uuid);
                                    jo.put("timestamp", dateFormatter.getTimestamp());
                                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                                    logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

                                    try
                                    {
                                        TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                                        bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\nMessage : " + e));
                                    }catch(Exception t)
                                    {
                                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                                    }

                                    throw new Exception(e.toString());
                                }

                                Batch batchnext = new Batch();
                                batchnext.setId(Integer.valueOf(masterBalanceminesnext[1]));

                                invtrxnext.setAccount(acc);
                                invtrxnext.setAmount(Integer.parseInt(masterBalanceminesnext[3]) * -1);
                                invtrxnext.setCreatedDate(date);
                                invtrxnext.setDescription(rjson.getString("description"));
//                                invtrxnext.setType("USAGE");
                                invtrxnext.setCurrentBalance(Integer.parseInt(masterBalanceminesnext[0]));
                                invtrxnext.setTrx(2);
                                invtrxnext.setBatch(batchnext);
                                invtrxnext.setInvoiceId(invoiceId);

                                if(rjson.has("referral"))
                                {
                                    invtrxnext.setReferral(rjson.getString("referral"));
                                }

                                boolean first = false;
                                if (!first)
                                {
                                    int update = rjson.getInt("amount") - Integer.parseInt(masterBalancemines[2]);
                                    invtrx.setAmount(update * -1);
                                    first = true;
                                }

                                invoiceRepository.save(invtrx);
//                                            db.session().save(invtrx);

                                if(rjson.has("item"))
                                {
                                    invtrxnext.setItemId(rjson.getString("item"));
                                }
                                invoiceRepository.save(invtrxnext);
//                                            db.session().save(invtrxnext);
                            }
                            else
                            {
                                sisa = false;
                            }
                        }

                        long commitinsert = System.nanoTime();

//                                    tx.commit();

                        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"PROCESS INSERT COMMIT: " + formatter.format((System.nanoTime() - commitinsert)/1000000000d));
//							  			logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"LEBIH AMOUNT TRANSAKSI " + (int)masterBalancemines[2], uuid);

                    }catch(Exception e)
                    {

                        e.printStackTrace();
                        logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +e);

                        
                        jo.put("result_code", "B06");
                        jo.put("error", e.toString());
                        jo.put("message", "Failed create transaction");
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        jo.put("product", rjson.getString("product"));
                        jo.put("xkey", rjson.getString("xkey"));
                        try
                        {
                            TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\nAddress : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\nMessage : " + e));
                        }catch(Exception t)
                        {
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                        }

                        throw new Exception(e.toString());
                    }

                    //Try send email notif balance
                    long queryemail = System.nanoTime();
                    try {
                        JSONArray saldo = new JSONArray();

//								  		if(rjson.getString("xkey").startsWith("MT"))
//								  		{
                        if(acc.getThreshold() > invtrx.getCurrentBalance() && !acc.getAlertBalance() && acc.getIsAlertBalance())
                        {
                            List<DataNotification> listDN = new ArrayList<DataNotification>();

                            List<Account> accountlist = accountsRepo.findAccount(rjson.getString("xkey"));
                            if(accountlist.size() > 0)
                            {
                                for(int i = 0; i < accountlist.size() ; i++)
                                {
                                    if(accountlist.get(i).getIsAlertBalance())
                                    {
                                        Balance mb = balanceRepository.saldo(accountlist.get(i).getId());

                                        JSONObject jsonObject = new JSONObject();

                                        jsonObject.put("product", accountlist.get(i).getProduct().getPkey());

                                        DataNotification data = new DataNotification();

                                        data.setType_balance(accountlist.get(i).getProduct().getNameProduct());
                                        data.setType_balance_en(accountlist.get(i).getProduct().getNameProductEn());

                                        if(mb != null)
                                        {
                                            data.setBalance(mb.getBalance().intValue());
                                            jsonObject.put("amount", mb.getBalance());
                                        }
                                        else
                                        {
                                            data.setBalance(mb.getBalance().intValue());
                                            jsonObject.put("amount", 0);
                                        }
                                        saldo.put(jsonObject);
                                        listDN.add(data);
                                    }
                                }
                            }

                            acc.setAlertBalance(true);
                            acc.setUpdatedDate(date);

                            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"SALDO : " + saldo);

                            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Sending email warning minimum balance to : " + rjson.getString("xkey") + " " + acc.getEmail());

                            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"List saldo yang dikirim peringatan saldo : " +  saldo);
                            try {
//									  				action.kirim(acc.getName(), acc.getEmail(), saldo);
                                JSONObject kirim = new JSONObject();

                                kirim.put("name", acc.getName());

                                String email = acc.getEmail();
                                if(acc.getInternal_email() != null)
                                {
                                    email = email + "," + acc.getInternal_email();
                                }
                                kirim.put("email", email);
                                kirim.put("saldo", saldo);

                                try {
                                    //send to message broker
                                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Send to kafka");

                                    NotificationAlert notifProducer = new NotificationAlert();
                                    notifProducer.setName(acc.getName());
                                    notifProducer.setEmail(acc.getEmail());
                                    notifProducer.setData(listDN);
                                    notifProducer.setTRACE_ID(uuid);
                                    notifProducer.setTimestamp(new Date().toInstant().plus(Duration.ofHours(7)).truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", "+07:00"));
                                    notifProducer.setUser_id(acc.getXkey().replaceAll("[^0-9]", ""));

                                    kafkaNotification.send("alert_notification", notifProducer);

//                                    Boolean status = kafka.sendNotification(notifProducer, "alert_notification", uuid);

//                                    if(!status)
//                                    {
//                                        try
//                                        {
//                                            TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
//                                            bot.execute(new SendMessage(213382980, "Failed process callback payment : " + uuid));
//                                        }catch(Exception t)
//                                        {
//                                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
//                                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t.toString());
//                                        }
//                                    }

                                }catch(Exception e)
                                {
                                    e.printStackTrace();
                                    logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Failed send to message broker");
                                }

                                accountsRepo.save(acc);
//                                            new InvoicesDao(db).updateEmail(acc);
                                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Sukses kirim notifikasi saldo");
                            }catch (Exception e)
                            {
                                logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Gagal kirim notifikasi saldo");
                                e.printStackTrace();

                                try
                                {
                                    TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                                    bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed send email notification minimum balance log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                                }catch(Exception t)
                                {
                                    logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                                    logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                                }
                            }
                        }
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Running email checking : " + formatter.format((System.nanoTime() - queryemail)/1000000000d));
                    }catch(Exception e)
                    {
                        logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);

                        try
                        {
                            TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed send email notification minimum balance log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                        }catch(Exception t)
                        {
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                        }
                    }


                    jo.put("result_code", "B00");
                    jo.put("message", "Success");
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    if(invtrxnext != null)
                    {
                        jo.put("current_balance", invtrxnext.getCurrentBalance());
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"ISI INVTRXNEXT " + invtrxnext.getId());
                    }
                    else
                    {
                        jo.put("current_balance", invtrx.getCurrentBalance());
                    }

                    jo.put("datetime", dateFormatter.getTimestamp(invtrx.getCreatedDate())+"Z");

                    jo.put("invoiceid", invoiceId);
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    jo.put("product", rjson.getString("product"));
                    jo.put("xkey", rjson.getString("xkey"));
                    jo.put("name", invtrx.getAccount().getName());
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return jo;
                }
            }
            else
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"SP Balance null");

                jo.put("result_code", "B06");
                jo.put("error", "Function balance error");
                jo.put("message", "Failed create transaction");
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                jo.put("product", rjson.getString("product"));
                jo.put("xkey", rjson.getString("xkey"));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                try
                {
                    TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                    bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : SP Balance null "));
                }catch(Exception t)
                {
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                }

                return jo;
            }
        }catch(Exception e) {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +e);

            jo.put("result_code", "B06");
            jo.put("message", "Failed create transaction");
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            jo.put("product", rjson.getString("product"));
            jo.put("xkey", rjson.getString("xkey"));
            logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
            }catch(Exception t)
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
            }

            throw new Exception(e.toString());
        }
    }


    @Transactional(rollbackFor = Exception.class, timeout=25)
    public JSONObject prepaidReversal(JSONObject rjson, long startTime, String uuid, List<Invoice> datainv, InetAddress inetAddress ) throws Exception {
        Date date = new Date();
        JSONObject jo = new JSONObject();
        NumberFormat formatter = new DecimalFormat("#0.00000");

        List<Invoice> checkreversal =  invoiceRepository.findTransactionReversal(rjson.getString("invoiceid"));
        List<Invoice> usage = invoiceRepository.findByInvoiceId(rjson.getString("invoiceid"));
        int trx = 0;
        if(checkreversal.size() > 0)
        {
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"SIZE CHECK REVERSAL :" + checkreversal.size());

            for(int i = 0 ; i < checkreversal.size() ; i++)
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"TRX SEBELUM :" + trx);
                trx = trx + checkreversal.get(i).getAmount();
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"REVERSAL AMOUNT DB :" + checkreversal.get(i).getAmount());
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"DITAMBAH :" + trx);
            }

            if (trx == Math.abs(usage.get(0).getAmount()));
            {
                jo.put("result_code", "B08");
                jo.put("message", "invoiceid already returned for reversal of all amount");
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                return jo;
            }
        }
        else
        {
            if(rjson.has("item"))
            {
                if(!datainv.get(0).getItemId().equals(BigInteger.valueOf(rjson.getLong("item"))) && datainv.get(0).getItemId() != null)
                {
                    jo.put("result_code", "B08");
                    jo.put("message", "item for invoice id " + datainv.get(0).getInvoiceId() + " not match");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return jo;
                }
            }

            List<Invoice> itm = invoiceRepository.findByInvoiceId(rjson.getString("invoiceid"));

            Account account;
            Product tenant = null;
            Invoice invReversal=null;
            for(int i = 0 ; i < itm.size() ; i++)
            {
                invReversal = new Invoice();
                account = accountsRepo.findByIds(itm.get(i).getAccount().getId());
                tenant = productRepository.findByID(itm.get(i).getAccount().getProduct().getId());

                String[] masterBalanceReversal = null;
                try {
                    //Potong update saldo ke master balance
                    long getspbalance = System.nanoTime();

                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Try reversal");

                    masterBalanceReversal= invoiceRepository.SPBalancePlus(account.getId(), Math.abs(itm.get(i).getAmount()), itm.get(i).getBatch().getId().intValue()).split(",");

                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Success reversal add balances");
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Time Get SP Balance : " + formatter.format((System.nanoTime() - getspbalance)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Balance from SP : " + masterBalanceReversal[0]);

                }catch(Exception e)
                {
                    logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);

                    jo.put("result_code", "B06");
                    jo.put("error", e.toString());
                    jo.put("message", "Failed create reversal");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    jo.put("product", tenant.getPkey());
                    logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

                    try
                    {
                        TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                        bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
                    }catch(Exception t)
                    {
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                    }

                    throw new Exception(e.toString());
                }

                try
                {
                    if (masterBalanceReversal != null)
                    {
                        Batch batch = new Batch();
                        batch.setId(Integer.parseInt(masterBalanceReversal[1]));

                        invReversal.setAccount(account);

                        invReversal.setAmount(Math.abs(itm.get(i).getAmount()));
                        invReversal.setCreatedDate(date);
                        invReversal.setDescription("Failed transaction");
                        invReversal.setInvoiceId(rjson.getString("invoiceid"));
//                        invReversal.setType("ITEM_ADJ");
                        invReversal.setCurrentBalance(Integer.parseInt(masterBalanceReversal[0]));
                        invReversal.setTrx(3);
                        invReversal.setDescription(itm.get(i).getDescription());

                        if(rjson.has("item"))
                        {
                            invReversal.setItemId(rjson.getString("item"));
                        }
                        else
                        {
                            invReversal.setItemId(itm.get(i).getItemId());
                        }

                        invReversal.setBatch(batch);

                        invoiceRepository.save(invReversal);
                    }
                }catch(Exception e) {

                    logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);

                    jo.put("result_code", "B06");
                    jo.put("message", "Failed create reversal");
                    jo.put("error", e.toString());
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    jo.put("product", tenant.getPkey());
                    logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

                    try
                    {
                        TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                        bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
                    }catch(Exception t)
                    {
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                    }

                    throw new Exception(e.toString());
                }
            }

            jo.put("result_code", "B00");
            jo.put("message", "Success reversal");
            jo.put("invoiceid", invReversal.getInvoiceId());
            jo.put("current_balance", invReversal.getCurrentBalance());
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            jo.put("product", tenant.getPkey());
            jo.put("xkey", invReversal.getAccount().getXkey());
            jo.put("name", invReversal.getAccount().getName());
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

            return jo;
            }
        }

    @Transactional(rollbackFor = Exception.class, timeout=25)
    public JSONObject postpaidTransaction(JSONObject rjson, Account acc, long startTime, String uuid, InetAddress inetAddress, String invoiceId, Product pKey) throws Exception {
        Date date = new Date();
        JSONObject jo = new JSONObject();
        NumberFormat formatter = new DecimalFormat("#0.00000");
        Invoice postpaidItems = new Invoice();

        synchronized (this) {
            Batch batch_data = batchRepository.findPostpaidByAccountRecord(acc.getId());
            Date now = new Date();
            if (batch_data != null) {
                SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd");
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Cek hari ini apa sudah melewati tanggal closing");

                String convertNow = myFormat.format(now);

                LogSystem.info(""+batch_data.getClosingDate());

                String convertClosing = batch_data.getClosingDate().toString();

                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Tanggal hari ini " + convertNow);
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Tanggal closing " + convertClosing);

                Date parseNow = null;
                Date parseClosing = null;
                try {
                    parseNow = myFormat.parse(convertNow);
                    parseClosing = myFormat.parse(convertClosing);
                } catch (Exception e) {
                    logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Error parse date now");
                    jo.put("result_code", "B06");
                    jo.put("error", e.toString());
                    jo.put("message", "Failed create transaction");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    return jo;
                }

                long diff = parseNow.getTime() - parseClosing.getTime();
                long selisih = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Selisih " + selisih);

                if (selisih > 0) {
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Tutup dan buat batch baru");

                    //Masukan ke tabel tagihan
                    Bill bill = new Bill();

                    bill.setBatch(batch_data);
                    bill.setStatus("UNPAID");
                    bill.setTotal(batch_data.getUsage() * batch_data.getPrice());

                    //menambahkan 20 hari masa tenggang
                    Date dt = new Date();
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);
                    c.add(Calendar.DATE, 20);

                    bill.setDueDate(c.getTime());

                    batch_data.setSettled(true);

                    billRepository.save(bill);
                    batchRepository.save(batch_data);

                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Selesai tutup dan buat batch baru");

                    batch_data = null;
                }
            }

            //Set Closing date
            Calendar cal = Calendar.getInstance();
            if (cal.get(Calendar.DAY_OF_MONTH) > acc.getBillPeriod()) {
                cal.add(Calendar.MONTH, 1);
                cal.set(Calendar.DAY_OF_MONTH, acc.getBillPeriod());
            } else {
                cal.add(Calendar.MONTH, 0);
                cal.set(Calendar.DAY_OF_MONTH, acc.getBillPeriod());
            }

            if (batch_data == null) {
                //Create new batch
                Batch batch = new Batch();
                batch.setAccount(acc);
                batch.setOpenDate(now);
                batch.setClosingDate(cal.getTime());
                batch.setNameBatch("POST_" + rjson.getString("xkey") + "_" + pKey.getPkey());
                batch.setSettled(false);
                batch.setUsage(rjson.getInt("amount"));
                batch.setQuota(0);
                batch.setRemainingBalance(0);
                batch.setPrice(0);

                batchRepository.save(batch);
//                            db.session().save(batch);

                //Use existing batch
                postpaidItems.setAccount(acc);
                postpaidItems.setAmount(rjson.getInt("amount"));
                postpaidItems.setCreatedDate(now);
                postpaidItems.setDescription(rjson.getString("description"));
                postpaidItems.setInvoiceId(invoiceId);
//                postpaidItems.setType("USAGE");
                postpaidItems.setCurrentBalance(batch.getUsage());
                postpaidItems.setTrx(2);
                postpaidItems.setBatch(batch);

                invoiceRepository.save(postpaidItems);
//                            db.session().save(postpaidItems);

                if (rjson.has("item")) {
                    postpaidItems.setItemId(rjson.getString("item"));
                }

                try {
                    long commitinsert = System.nanoTime();

                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"PROCESS INSERT COMMIT : " + formatter.format((System.nanoTime() - commitinsert) / 1000000000d));
                } catch (Exception e) {
                    e.printStackTrace();
                    jo.put("result_code", "B06");
                    jo.put("error", e.toString());
                    jo.put("message", "Failed create transaction postpaid");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    try {
                        TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                        bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction postpaid log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                    } catch (Exception t) {
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                    }
                    return jo;
                }
            } else {
                batch_data.setUsage(batch_data.getUsage() + rjson.getInt("amount"));
//                            db.session().update(batch_data);
                batchRepository.save(batch_data);
                //Use existing batch
                postpaidItems.setAccount(acc);
                postpaidItems.setAmount(rjson.getInt("amount"));
                postpaidItems.setCreatedDate(now);
                postpaidItems.setDescription(rjson.getString("description"));
//                postpaidItems.setType("USAGE");
                postpaidItems.setCurrentBalance(batch_data.getUsage());
                postpaidItems.setTrx(2);
                postpaidItems.setBatch(batch_data);
                postpaidItems.setInvoiceId(invoiceId);


                invoiceRepository.save(postpaidItems);
//                            db.session().save(postpaidItems);

                if (rjson.has("item")) {
                    postpaidItems.setItemId(rjson.getString("item"));
                }

                try {
                    long commitinsert = System.nanoTime();

//                                txpostpaid.commit();
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"PROCESS INSERT COMMIT : " + formatter.format((System.nanoTime() - commitinsert) / 1000000000d));
                } catch (Exception e) {
                    e.printStackTrace();
                    jo.put("result_code", "B06");
                    jo.put("error", e.toString());
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("message", "Failed create transaction postpaid");
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    try {
                        TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                        bot.execute(new SendMessage(213382980, rjson.getString("xkey") + " " + rjson.getString("product") + "\nFailed create transaction postpaid log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Message : " + e));
                    } catch (Exception t) {
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                    }
                    throw new Exception(e.toString());
                }
            }
        }

        ZoneId zone = ZoneId.of("Asia/Jakarta");
        jo.put("result_code", "B00");
        jo.put("closing_date", postpaidItems.getBatch().getClosingDate());
        jo.put("message", "Success");
        jo.put("usage", postpaidItems.getCurrentBalance());
        jo.put("datetime", dateFormatter.getTimestamp(postpaidItems.getCreatedDate())+"Z");
        jo.put("invoiceid", invoiceId);
        jo.put("log", uuid);
        jo.put("timestamp", dateFormatter.getTimestamp());
        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

        return jo;
    }

    @Transactional(rollbackFor = Exception.class, timeout=25)
    public JSONObject postpaidReversal(JSONObject rjson, long startTime, String uuid, List<Invoice> datainv, InetAddress inetAddress ) throws Exception {
        Date date = new Date();
        JSONObject jo = new JSONObject();
        NumberFormat formatter = new DecimalFormat("#0.00000");
        List<Invoice> checkreversal =  invoiceRepository.findTransactionReversal(rjson.getString("invoiceid"));
        List<Invoice> usage = invoiceRepository.findByInvoiceId(rjson.getString("invoiceid"));
        int trx = 0;
        if(checkreversal.size() > 0)
        {
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"SIZE CHECK REVERSAL :" + checkreversal.size());

            for(int i = 0 ; i < checkreversal.size() ; i++)
            {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"TRX SEBELUM :" + trx);
                trx = trx + checkreversal.get(i).getAmount();
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"REVERSAL AMOUNT DB :" + checkreversal.get(i).getAmount());
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"DITAMBAH :" + trx);
            }

            if (trx == Math.abs(usage.get(0).getAmount()))
            {

                jo.put("result_code", "B08");
                jo.put("message", "invoiceid already returned for reversal of all amount");
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                return jo;
            }

            if(trx + rjson.getInt("amount") > (Math.abs(usage.get(0).getAmount())))
            {

                jo.put("result_code", "B08");
                jo.put("message", "existing reversal and amount cannot be greater than transaction");
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                return jo;
            }
            else
            {
                if(rjson.has("item"))
                {
                    if(!datainv.get(0).getItemId().equals(BigInteger.valueOf(rjson.getLong("item"))) && datainv.get(0).getItemId() != null)
                    {

                        jo.put("result_code", "B08");
                        jo.put("message", "item for invoice id " + datainv.get(0).getInvoiceId() + " not match");
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                        return jo;
                    }
                }

                Invoice invReversal = new Invoice();

                List<Invoice> itm = invoiceRepository.findByInvoiceId(rjson.getString("invoiceid"));

                synchronized(this)
                {
                    Batch batch_data = batchRepository.findPostpaidByBatchId(itm.get(0).getBatch().getId());

                    try
                    {
                        batch_data.setUsage(batch_data.getUsage()-itm.get(0).getAmount());
                        batchRepository.save(batch_data);

                        invReversal.setAccount(itm.get(0).getAccount());
                        invReversal.setAmount(Math.abs(itm.get(0).getAmount()));
                        invReversal.setCreatedDate(date);
                        invReversal.setDescription("Failed transaction");
                        invReversal.setInvoiceId(rjson.getString("invoiceid"));
//                        invReversal.setType("ITEM_ADJ");
                        invReversal.setCurrentBalance(batch_data.getUsage());
                        invReversal.setTrx(3);
                        invReversal.setBatch(batch_data);

                        if(rjson.has("item"))
                        {
                            invReversal.setItemId(rjson.getString("item"));
                        }
                        else
                        {
                            invReversal.setItemId(datainv.get(0).getItemId());
                        }

                        invoiceRepository.save(invReversal);

                    }catch(Exception e) {

                        logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);

                        jo.put("result_code", "B06");
                        jo.put("message", "Failed create reversal");
                        jo.put("error", e.toString());
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

                        try
                        {
                            TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                            bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
                        }catch(Exception t)
                        {
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                            logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                        }

                        throw new Exception(e.toString());
                    }
                }
                jo.put("result_code", "B00");
                jo.put("message", "Success reversal postpaid");
                jo.put("invoiceid", invReversal.getInvoiceId());
                jo.put("current_balance", invReversal.getCurrentBalance());
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

                return jo;
            }
        }
        else
        {
            if(rjson.has("item"))
            {
                if(!datainv.get(0).getItemId().equals(BigInteger.valueOf(rjson.getLong("item"))) && datainv.get(0).getItemId() != null)
                {
                    jo.put("result_code", "B08");
                    jo.put("message", "item for invoice id " + datainv.get(0).getInvoiceId() + " not match");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);
                    return jo;
                }
            }

            Invoice invReversal = new Invoice();

            List<Invoice> itm = invoiceRepository.findByInvoiceId(rjson.getString("invoiceid"));

            synchronized(this)
            {
                Batch batch_data = batchRepository.findPostpaidByBatchId(itm.get(0).getBatch().getId());

                try
                {
                    batch_data.setUsage(batch_data.getUsage()-itm.get(0).getAmount());
                    batchRepository.save(batch_data);

                    invReversal.setAccount(itm.get(0).getAccount());
                    invReversal.setAmount(Math.abs(invoiceRepository.findByInvoiceId(rjson.getString("invoiceid")).get(0).getAmount()));
                    invReversal.setCreatedDate(date);
                    invReversal.setDescription("Failed transaction");
                    invReversal.setInvoiceId(rjson.getString("invoiceid"));
//                    invReversal.setType("ITEM_ADJ");
                    invReversal.setCurrentBalance(batch_data.getUsage());
                    invReversal.setTrx(3);
                    invReversal.setBatch(batch_data);


                    if(rjson.has("item"))
                    {
                        invReversal.setItemId(rjson.getString("item"));
                    }
                    else
                    {
                        invReversal.setItemId(datainv.get(0).getItemId());
                    }

                    invoiceRepository.save(invReversal);


                }catch(Exception e) {

                    logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);

                    jo.put("result_code", "B06");
                    jo.put("error", e.toString());
                    jo.put("message", "Failed create reversal postpaid");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.error("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

                    try
                    {
                        TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                        bot.execute(new SendMessage(213382980, "Failed create reversal log id : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n invoiceid : " + rjson.getString("invoiceid") + "\n Message : " + e));
                    }catch(Exception t)
                    {
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +"Failed send message telegram");
                        logger.info("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +t);
                    }

                    throw new Exception(e.toString());
                }
            }
            jo.put("result_code", "B00");
            jo.put("message", "Success reversal");
            jo.put("invoiceid", invReversal.getInvoiceId());
            jo.put("current_balance", invReversal.getCurrentBalance());
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo);

            return jo;
            }
    }
}
