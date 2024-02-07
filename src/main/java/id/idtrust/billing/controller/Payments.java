package id.idtrust.billing.controller;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.api.Xendit;
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping(value = "/billing")

public class Payments extends Description {

    @Autowired
    AccountRepository AccountsRepo;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    BillRepository billRepository;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    PaymentRepository paymentRepository;


    DateFormatter dateFormatter = new DateFormatter();
    private static final Logger logger = LogManager.getLogger();

    @PostMapping(value="/payment", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> generatePayment(@RequestBody String data) throws Exception {
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

            Product product = productRepository.findByPkey(rjson.getString("product"));

            if(product == null)
            {
                notif="Account not exist";
                jo.put("result_code", "B01");
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

            Account account = AccountsRepo.findAccountByProduct(rjson.getString("xkey"), product.getId());

            if(account == null)
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

            Bill bill = billRepository.findByAccountId(account.getId());

            if(bill ==  null)
            {
                jo.put("result_code", "B09");
                jo.put("message", "Tidak ada tagihan yang tersedia");
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " + jo);
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            JSONObject payment = new JSONObject(rjson.getString("payment"));

            String name=payment.getString("name");
            String pnumber="";

            String channel = payment.getString("channel");
            String Payer = payment.getString("name");

            Channel channelData = null;

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

            //Request to xendit
            Xendit sendXendit = new Xendit();

            JSONObject respXendit = new JSONObject();

            String id = "";

            JSONArray methods = new JSONArray();

            methods.put(channelData.getChannelCode());

            double fee=0;
            double ppn=0;
            double totalPayment=0;

            //Hitung pajak
            if(channelData.getType().equals("va"))
            {
                totalPayment = bill.getTotal();
            }

            if(channelData.getType().equals("otc"))
            {
                totalPayment = bill.getTotal();
            }

            String description = "Topup idtrust";
            id = "IDTRUST-POSTPAID-"+bill.getId();

            Calendar c = Calendar.getInstance();
            c.setTime(new Date());
            c.add(Calendar.HOUR, 1);

            String email = null;

            respXendit = sendXendit.paymentURL(methods, id, Integer.parseInt(new DecimalFormat("#").format(totalPayment)), c.getTime(), description, name, email, pnumber);
            if(respXendit.getInt("code") != 200)
            {

                jo.put("result_code", "B06");
                jo.put("message", "Failed topup");
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                jo.put("error", respXendit);
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            String code = respXendit.getString("invoice_url");

            Payment pay = new Payment();
            pay.setChannel(channelData);
            pay.setStatus("WAITING_PAYMENT");
            pay.setPaymentCode(code);

            bill.setPayment(pay);

            paymentRepository.save(pay);
            billRepository.save(bill);

            DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

            JSONObject paymentInfo = new JSONObject();

            paymentInfo.put("type", channelData.getType());
            paymentInfo.put("data", code);
            paymentInfo.put("payment_expired", dateFormat.format(Date.from(c.getTime().toInstant())));
            paymentInfo.put("id", id);
            paymentInfo.put("name", name);
            paymentInfo.put("fee", fee);
            paymentInfo.put("ppn11", Integer.parseInt(new DecimalFormat("#").format(ppn)));
            paymentInfo.put("price", Integer.parseInt(new DecimalFormat("#").format(bill.getTotal())));
            paymentInfo.put("total_price", Integer.parseInt(new DecimalFormat("#").format(totalPayment)));
            jo.put("payment_info", paymentInfo);

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

        }catch(RuntimeException e)
        {
            e.printStackTrace();
            notif="Error Get List Topup";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error topup : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
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

}