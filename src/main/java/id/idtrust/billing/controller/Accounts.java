package id.idtrust.billing.controller;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.model.Account;
import id.idtrust.billing.model.Balance;
import id.idtrust.billing.model.Product;
import id.idtrust.billing.repository.AccountRepository;
import id.idtrust.billing.repository.BalanceRepository;
import id.idtrust.billing.repository.InvoiceRepository;
import id.idtrust.billing.repository.ProductRepository;
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping(value = "/billing")
public class Accounts extends Description{
    @Autowired
    ProductRepository productRepository;
    @Autowired
    BalanceRepository BalancesRepo;
    @Autowired
    AccountRepository AccountsRepo;

    @Autowired
    InvoiceRepository invRepository;

    private static final Logger logger = LogManager.getLogger();
    
    DateFormatter dateFormatter = new DateFormatter();
    
    @PostMapping(
            value="/account/verification",
            produces = {"application/json"},
            consumes = "application/json"
    )
    public ResponseEntity<?> countVerification(@RequestBody String json) throws Exception {
        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif="Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(json);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);

        JSONObject jo=new JSONObject();

        if(!rjson.has("xkey"))
        {
            notif="Missing parameter xkey";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/RESPONSE] : " +jo);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        try {
            Product product = productRepository.findByPkey(rjson.getString("product"));

            if(product == null)
            {
                notif = "Product not exist";
                jo.put("result_code", "B01");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            int invoice = invRepository.Verification(rjson.getString("xkey"), product.getId());

            notif = "Success";
            jo.put("result_code", "B00");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

//            JSONObject data = new JSONObject();
//            data.put("total_transaction", invoice);
            jo.put("total_transaction", invoice);

            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }catch(Exception e)
        {
            e.printStackTrace();

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/account/get", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> get(@RequestBody String json) throws Exception {
        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif="Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(json);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);

        JSONObject jo=new JSONObject();

        if(!rjson.has("xkey"))
        {
            notif="Missing parameter xkey";
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

        try {
            Product product;
            List<Account> account;
            if(rjson.has("product"))
            {
                product = productRepository.findByPkey(rjson.getString("product"));
                account =  AccountsRepo.findAccountByProductList(rjson.getString("xkey"), product.getId());
            }
            else
            {
                account =  AccountsRepo.findAccount(rjson.getString("xkey"));
            }

            if(account.size() < 1)
            {
                notif = "Account not exist";
                jo.put("result_code", "B01");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            JSONObject objectData = new JSONObject();
            JSONArray arrayProduct = new JSONArray();

            String internalPIC = "";
            String externalPIC = "";

            JSONArray arrayInternalPIC = new JSONArray();
            JSONArray arrayExternalPIC = new JSONArray();
            if(account.get(0).getInternal_email() != null)
            {
                internalPIC = account.get(0).getInternal_email();
                String[] listInternalPIC = internalPIC.split(",");

                for(String data : listInternalPIC)
                {
                    arrayInternalPIC.put(data);
                }
            }

            if(account.get(0).getEmail() != null)
            {
                externalPIC = account.get(0).getEmail();
                String[] listexternalPIC = externalPIC.split(",");

                for(String data : listexternalPIC)
                {
                    arrayExternalPIC.put(data);
                }
            }

            objectData.put("name", account.get(0).getName());
            objectData.put("external_pic", arrayExternalPIC);
            objectData.put("internal_pic", arrayInternalPIC);
            objectData.put("type", account.get(0).getSubscription());
            List<String> listProduct = new ArrayList<String>();
            JSONObject response = new JSONObject();

            for(int i = 0 ; i < account.size() ; i++)
            {
                JSONObject objectProduct = new JSONObject();

                if(!listProduct.contains(account.get(i).getProduct().getCategory()))
                {
                    if(rjson.has("group"))
                    {
                        if(rjson.getString("group").equalsIgnoreCase("category"))
                        {
                            objectProduct.put("category", account.get(i).getProduct().getCategory());
                            listProduct.add(account.get(i).getProduct().getCategory());

                            JSONArray pr = new JSONArray();
                            for (int j = 0; j < account.size(); j++)
                            {
                                if (account.get(j).getProduct().getCategory().equals(account.get(i).getProduct().getCategory()))
                                {
                                    JSONObject lproduct = new JSONObject();

                                    lproduct.put("pkey", account.get(j).getProduct().getPkey());
                                    lproduct.put("active", account.get(j).getActive());
                                    lproduct.put("title_id", account.get(j).getProduct().getNameProduct());
                                    lproduct.put("title_en", account.get(j).getProduct().getNameProductEn());
                                    lproduct.put("detail_id", account.get(j).getProduct().getDetailId());
                                    lproduct.put("detail_en", account.get(j).getProduct().getDetailEn());

                                    pr.put(lproduct);
                                }
                            }
                            objectProduct.put("product", pr);
                            arrayProduct.put(objectProduct);
                        }
                    }
                    else
                    {
                        objectProduct.put("pkey", account.get(i).getProduct().getPkey());
                        objectProduct.put("active", account.get(i).getActive());
                        objectProduct.put("title_en", account.get(i).getProduct().getNameProductEn());
                        objectProduct.put("title_id", account.get(i).getProduct().getNameProduct());

                        arrayProduct.put(objectProduct);
                    }
                }
            }

            if(rjson.has("group"))
            {
                if(rjson.getString("group").equalsIgnoreCase("category"))
                {
                    response.put("data", arrayProduct);
                }
                else
                {
                    objectData.put("product", arrayProduct);
                    response.put("data", objectData);
                }
            }
            else
            {
                objectData.put("product", arrayProduct);
                response.put("data", objectData);
            }


            response.put("result_code", "B00");
            response.put("message", "Success");
            response.put("log", uuid);

            response.put("timestamp", dateFormatter.getTimestamp());
            response.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.toString());
        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            notif="Error get billing account";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error create billing account : " + uuid + "\n Externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
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

    @PatchMapping(value="/account/update/data", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> patchData(@RequestBody String json) throws Exception
    {
        JSONObject jo=new JSONObject();
        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(json);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);

        if(!rjson.has("action"))
        {
            jo.put("result_code", "B28");
            jo.put("message", "Missing parameter action");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        try {
            if (rjson.has("xkey")) {
                List<Account> account = AccountsRepo.findAccountByList(rjson.getString("xkey"));

                if (account.size() < 1) {
                    jo.put("result_code", "B04");
                    jo.put("message", "Account not exist");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo.toString());
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                boolean email = false;
                boolean internal_email = false;

                JSONArray listExistingInternalPIC = new JSONArray();
                JSONArray listExistingExternalPIC = new JSONArray();

                List<String> existingInternalPIC = Arrays.asList(account.get(0).getInternal_email().split(","));
                List<String> existingExternalPIC = Arrays.asList(account.get(0).getEmail().split(","));

                for (int i = 0; i < existingInternalPIC.size(); i++) {
                    listExistingInternalPIC.put(existingInternalPIC.get(i));
                }

                for (int i = 0; i < existingExternalPIC.size(); i++) {
                    listExistingExternalPIC.put(existingExternalPIC.get(i));
                }

                //menambahkan pic internal dan external
                if (rjson.getString("action").equalsIgnoreCase("add")) {
                    if (rjson.has("external_pic") && rjson.getJSONArray("external_pic").length() > 0) {
                        JSONArray listAddExternalPIC = rjson.getJSONArray("external_pic");
                        for (int i = 0; i < listAddExternalPIC.length(); i++) {
                            for (int j = 0; j < existingExternalPIC.size(); j++) {
                                if (listAddExternalPIC.get(i).toString().equalsIgnoreCase(existingExternalPIC.get(j))) {
                                    //datanya udah ada kasih response
                                    jo.put("result_code", "B27");
                                    jo.put("message", "Existing data cannot be added");
                                    jo.put("log", uuid);
                                    jo.put("timestamp", dateFormatter.getTimestamp());
                                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                                    return ResponseEntity
                                            .status(200)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(jo.toString());
                                }
                            }
                            //datanya belom ada tambahin ke list
                            listExistingExternalPIC.put(listAddExternalPIC.getString(i));
                        }

                        String updateExternalPIC = listExistingExternalPIC.join(",").replace("\"", "");

                        for (int i = 0; i < account.size(); i++) {
                            account.get(i).setEmail(updateExternalPIC);
                        }
                        email = true;
                    }

                    if (rjson.has("internal_pic") && rjson.getJSONArray("internal_pic").length() > 0) {
                        JSONArray listAddInternalPIC = rjson.getJSONArray("internal_pic");
                        for (int i = 0; i < listAddInternalPIC.length(); i++) {
                            for (int j = 0; j < existingInternalPIC.size(); j++) {
                                if (listAddInternalPIC.get(i).toString().equalsIgnoreCase(existingInternalPIC.get(j))) {
                                    //datanya udah ada kasih response
                                    jo.put("result_code", "B27");
                                    jo.put("message", "Existing data cannot be added");
                                    jo.put("log", uuid);
                                    jo.put("timestamp", dateFormatter.getTimestamp());
                                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                                    return ResponseEntity
                                            .status(200)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(jo.toString());
                                }
                            }
                            //datanya belom ada tambahin ke list
                            listExistingInternalPIC.put(listAddInternalPIC.getString(i));
                        }

                        String updateInternalPIC = listExistingInternalPIC.join(",").replace("\"", "");

                        for (int i = 0; i < account.size(); i++) {
                            account.get(i).setInternal_email(updateInternalPIC);
                        }
                        internal_email = true;
                    }
                }

                boolean remove = false;
                //Menghapus pic internal dan external
                if (rjson.getString("action").equalsIgnoreCase("remove")) {
                    if (rjson.has("external_pic") && rjson.getJSONArray("external_pic").length() > 0) {
                        JSONArray listAddExternalPIC = rjson.getJSONArray("external_pic");
                        for (int i = 0; i < listAddExternalPIC.length(); i++) {
                            for (int j = 0; j < existingExternalPIC.size(); j++) {
                                //datanya ada yang mau dihapus
                                if (listAddExternalPIC.get(i).toString().equalsIgnoreCase(existingExternalPIC.get(j))) {
                                    listExistingExternalPIC.remove(j);
                                    remove = true;
                                }
                            }
                        }

                        if (!remove) {
                            jo.put("result_code", "B28");
                            jo.put("message", "No data can be deleted");
                            jo.put("log", uuid);
                            jo.put("timestamp", dateFormatter.getTimestamp());
                            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                            return ResponseEntity
                                    .status(200)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jo.toString());
                        }

                        String updateExternalPIC = listExistingExternalPIC.join(",").replace("\"", "");

                        for (int i = 0; i < account.size(); i++) {
                            account.get(i).setEmail(updateExternalPIC);
                        }
                        email = true;
                    }

                    if (rjson.has("internal_pic") && rjson.getJSONArray("internal_pic").length() > 0) {

                        JSONArray listAddInternalPIC = rjson.getJSONArray("internal_pic");
                        for (int i = 0; i < listAddInternalPIC.length(); i++) {
                            for (int j = 0; j < existingInternalPIC.size(); j++) {
                                //datanya ada yang mau dihapus
                                if (listAddInternalPIC.get(i).toString().equalsIgnoreCase(existingInternalPIC.get(j))) {
                                    listExistingExternalPIC.remove(j);
                                    remove = true;
                                }
                            }
                        }

                        if (!remove) {
                            jo.put("result_code", "B28");
                            jo.put("message", "No data can be deleted");
                            jo.put("log", uuid);
                            jo.put("timestamp", dateFormatter.getTimestamp());
                            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                            return ResponseEntity
                                    .status(200)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jo.toString());
                        }

                        String updateInternalPIC = listExistingInternalPIC.join(",").replace("\"", "");

                        for (int i = 0; i < account.size(); i++) {
                            account.get(i).setInternal_email(updateInternalPIC);
                        }
                        internal_email = true;
                    }
                }

                if (!email && !internal_email) {
                    jo.put("result_code", "B00");
                    jo.put("message", "No Process update");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                try {
                    AccountsRepo.saveAll(account);
                } catch (Exception e) {
                    jo.put("result_code", "B06");
                    jo.put("message", "Failed patch data");
                    jo.put("log", uuid);
                    jo.put("error", e.toString());
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
            }

            jo.put("result_code", "B00");
            jo.put("message", "Success patch data");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo.toString());
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
//        else
//        {
//            //ga ada xkey, update semua internal pic email
//            List<Account> account = AccountsRepo.findAccountByList(rjson.getString("xkey"));
//
//            if(account.size() < 1)
//            {
//                jo.put("result_code", "B04");
//                jo.put("message", "Account not exist");
//                jo.put("log", uuid);
//                jo.put("timestamp", dateFormatter.getTimestamp());
//                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                return ResponseEntity
//                        .status(200)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .body(jo.toString());
//            }
//
//            boolean email=false;
//            boolean internal_email=false;
//            if(rjson.has("action") && rjson.getString("action").equalsIgnoreCase(""))
//                if(rjson.has("external_pic") && rjson.getJSONArray("external_pic").length() > 0)
//                {
//                    for (int i = 0; i < account.size(); i++)
//                    {
//                        account.get(i).setEmail(rjson.getString("external_pic"));
//                    }
//                    email=true;
//                }
//
//            if(rjson.has("internal_pic") && !rjson.getString("internal_pic").equalsIgnoreCase("") && !rjson.getString("internal_pic").equalsIgnoreCase(" "))
//            {
//                for (int i = 0; i < account.size(); i++)
//                {
//                    account.get(i).setInternal_email(rjson.getString("internal_pic"));
//                }
//                internal_email=true;
//            }
//
//            if(!email && !internal_email)
//            {
//                jo.put("result_code", "B00");
//                jo.put("message", "No Process update");
//                jo.put("log", uuid);
//                jo.put("timestamp", dateFormatter.getTimestamp());
//                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                return ResponseEntity
//                        .status(200)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .body(jo.toString());
//            }
//
//            try{
//                AccountsRepo.saveAll(account);
//            }catch(Exception e)
//            {
//                jo.put("result_code", "B06");
//                jo.put("message", "Failed patch data");
//                jo.put("log", uuid);
//                jo.put("error", e.toString());
//                jo.put("timestamp", dateFormatter.getTimestamp());
//                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
//                return ResponseEntity
//                        .status(200)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .body(jo.toString());
//            }
//
//            jo.put("result_code", "B00");
//            jo.put("message", "Success patch data");
//            jo.put("log", uuid);
//            jo.put("timestamp", dateFormatter.getTimestamp());
//            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
//            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
//            return ResponseEntity
//                    .status(200)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .body(jo.toString());
//        }
        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());

            jo.put("result_code", "B06");
            jo.put("message", "Error patch billing account");
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error create billing account : " + uuid + "\n Externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
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

    @PatchMapping(value="/account/update/product", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> updateBatch(@RequestBody String data) throws Exception {
        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif="Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);

        JSONObject jo=new JSONObject();

        if(!rjson.has("xkey"))
        {
            notif="Missing parameter xkey";
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

        if(!rjson.has("activate"))
        {
            notif="Missing parameter activate";
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

        if(!rjson.has("deactivate"))
        {
            notif="Missing parameter deactivate";
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

        if(!rjson.has("type"))
        {
            notif="Missing parameter type";
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

        try {
            JSONArray activate = rjson.getJSONArray("activate");
            JSONArray deactivate = rjson.getJSONArray("deactivate");

            if(activate.length() > 0 && deactivate.length() > 0)
            {
                for(int a = 0 ; a < activate.length() ; a++)
                {
                    for(int b = 0 ; b < deactivate.length() ; b++)
                    {
                        if(activate.getString(a).equalsIgnoreCase(deactivate.getString(b)))
                        {
                            jo.put("result_code", "B01");
                            jo.put("message", "Array activate contain same data on array deactivate");
                            jo.put("log", uuid);
                            jo.put("timestamp", dateFormatter.getTimestamp());
                            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                            return ResponseEntity
                                    .status(200)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jo.toString());
                        }
                    }
                }
            }

            List<Account> account = new ArrayList<Account>();

            if(rjson.getString("type").equalsIgnoreCase("product"))
            {
                if(activate.length() > 0)
                {
                    for(int i = 0 ; i < activate.length() ; i++)
                    {
                        Product product = productRepository.findByPkey(activate.getString(i));

                        if (product == null) {
                            notif = "Product not exist '" + activate.get(i) + "'";
                            jo.put("result_code", "B01");
                            jo.put("message", notif);
                            jo.put("log", uuid);
                            jo.put("timestamp", dateFormatter.getTimestamp());
                            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                            return ResponseEntity
                                    .status(200)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jo.toString());
                        }

                        Account acc = AccountsRepo.findByExternalKey(rjson.getString("xkey"), product.getId());

                        if (acc == null)
                        {
                            notif = "Account not exist ";
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
                        }
                        else
                        {
                            acc.setActive(true);
                        }

                        account.add(acc);
                    }
                }

                if(deactivate.length() > 0)
                {
                    for(int i = 0 ; i < deactivate.length() ; i++)
                    {
                        Product product = productRepository.findByPkey(deactivate.getString(i));

                        if (product == null) {
                            notif = "Product not exist '" + deactivate.get(i) + "'";
                            jo.put("result_code", "B01");
                            jo.put("message", notif);
                            jo.put("log", uuid);
                            jo.put("timestamp", dateFormatter.getTimestamp());
                            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                            return ResponseEntity
                                    .status(200)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jo.toString());
                        }

                        Account acc = AccountsRepo.findByExternalKey(rjson.getString("xkey"), product.getId());

                        if (acc == null) {
                            notif = "Account not exist ";
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
                        }
                        else
                        {
                            acc.setActive(false);
                        }

                        account.add(acc);
                    }
                }
            }
            else
            {
                if(activate.length() > 0)
                {
                    List<Account> acc = null;
                    for(int i = 0 ; i < activate.length() ; i++)
                    {
                        List<Product> product = productRepository.findByCategory(activate.getString(i));

                        if (product.size() < 1) {
                            notif = "Product not exist '" + activate.get(i) + "'";
                            jo.put("result_code", "B01");
                            jo.put("message", notif);
                            jo.put("log", uuid);
                            jo.put("timestamp", dateFormatter.getTimestamp());
                            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                            return ResponseEntity
                                    .status(200)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jo.toString());
                        }

                        acc = AccountsRepo.findAccountByCategoryList(rjson.getString("xkey"), activate.getString(i));

                        if (acc == null)
                        {
                            notif = "Account not exist ";
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
                        }
                        else
                        {
                            for(int b = 0 ; b < acc.size() ; b++)
                            {
                                acc.get(b).setActive(true);
                            }
                        }
                    }
                    account.addAll(acc);
                }

                if(deactivate.length() > 0)
                {
                    List<Account> acc = null;
                    for(int i = 0 ; i < deactivate.length() ; i++)
                    {
                        List<Product> product = productRepository.findByCategory(deactivate.getString(i));

                        if (product.size() < 1) {
                            notif = "Product not exist '" + deactivate.get(i) + "'";
                            jo.put("result_code", "B01");
                            jo.put("message", notif);
                            jo.put("log", uuid);
                            jo.put("timestamp", dateFormatter.getTimestamp());
                            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                            return ResponseEntity
                                    .status(200)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jo.toString());
                        }

                        acc = AccountsRepo.findAccountByCategoryList(rjson.getString("xkey"), deactivate.getString(i));

                        if (acc == null)
                        {
                            notif = "Account not exist ";
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
                        }
                        else
                        {
                            for(int b = 0 ; b < acc.size() ; b++)
                            {
                                acc.get(b).setActive(false);
                            }
                        }
                    }
                    account.addAll(acc);
                }
            }

            if(activate.length() > 0 || deactivate.length() > 0)
            {
                AccountsRepo.saveAll(account);
            }
            else
            {
                notif = "Nothing patch, same data";
                jo.put("result_code", "B20");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }


            JSONObject response = new JSONObject();

            response.put("result_code", "B00");
            response.put("message", "Success");
            response.put("log", uuid);
            response.put("timestamp", new Date().toInstant());
            response.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + response);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.toString());
        }catch(Exception e)
        {
            e.printStackTrace();

            jo.put("result_code", "B06");
            jo.put("message", "Failed");
            jo.put("error", e.toString());

            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PatchMapping(value="/account/update", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> update(@RequestBody String data) throws Exception {
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

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson);

        JSONObject jo=new JSONObject();

        if(!rjson.has("xkey"))
        {
            notif="Missing parameter xkey";
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

        try {
            List<Account> account = null;

            if(rjson.has("product"))
            {
                JSONArray pr = rjson.getJSONArray("product");
                List<Long> itProduct = new ArrayList<Long>();

                for (int i = 0; i < pr.length(); i++) {
                    Product product = productRepository.findByPkey(pr.getString(i));

                    if (product == null) {
                        notif = "Product not exist '" + pr.get(i) + "'";
                        jo.put("result_code", "B01");
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                        logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                    itProduct.add(product.getId());
                }

                account = AccountsRepo.findAccountByProductInList(rjson.getString("xkey"), itProduct);
                itProduct.clear();
            }

            if(rjson.has("category"))
            {
                JSONArray pr = rjson.getJSONArray("category");
                List<String> caProduct = new ArrayList<String>();

                for(int i = 0 ; i < pr.length() ; i++)
                {
                    List<Product> product = productRepository.findByCategory(pr.getString(i));

                    if (product.size() < 1) {
                        notif = "Category not exist '" + pr.get(i) + "'";
                        jo.put("result_code", "B01");
                        jo.put("message", notif);
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                        logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                    caProduct.add(product.get(i).getCategory());
                }

                account = AccountsRepo.findAccountByCategoryInList(rjson.getString("xkey"), caProduct);
                caProduct.clear();
            }

            if(!rjson.has("product") && !rjson.has("category"))
            {
                account = AccountsRepo.findAccountByList(rjson.getString("xkey"));
            }

            if(account != null && account.size() < 1)
            {
                notif = "Account not exist";
                jo.put("result_code", "B04");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            JSONObject response = new JSONObject();

            if(account.size() == 1)
            {
                if (rjson.getBoolean("active") != account.get(0).getActive()) {
                    account.get(0).setActive(rjson.getBoolean("active"));
                    AccountsRepo.save(account.get(0));
                    response.put("result_code", "B00");
                    response.put("message", "Success patch");
                } else {
                    response.put("result_code", "B20");
                    response.put("message", "Nothing patch, same data");
                }
            }
            else
            {
                boolean save = false;
                for(int i=0 ; i < account.size() ; i++)
                {
                    if (rjson.getBoolean("active") == account.get(i).getActive())
                    {
                        response.put("result_code", "B20");
                        response.put("message", "Nothing patch, same data");
                    } else {
                        account.get(i).setActive(rjson.getBoolean("active"));
                        save = true;
                        response.put("result_code", "B00");
                        response.put("message", "Success patch");
                    }
                }
                if(save)
                {
                    AccountsRepo.saveAll(account);
                }
            }

            response.put("log", uuid);
            response.put("timestamp", new Date().toInstant());
            response.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +response);
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.toString());
        }catch(Exception e)
        {
            e.printStackTrace();

            jo.put("result_code", "B06");
            jo.put("message", "Failed");
            jo.put("error", e.toString());

            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

        return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @PostMapping(value="/account/create", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> add(@RequestBody String data) throws Exception {
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

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson.toString());
        List<String> productaccount = new ArrayList<String>();
        JSONObject jo=new JSONObject();

        if(!rjson.has("xkey"))
        {
            notif="Missing parameter xkey";
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

        if(!rjson.has("type"))
        {
            notif="Missing parameter type";
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

        if(!rjson.has("company"))
        {
            notif="Missing parameter company";
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
        if(!rjson.has("name"))
        {
            notif="Missing parameter name";
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
        if(!rjson.has("external_pic"))
        {
            notif="Missing parameter external_pic";
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
        if(!rjson.has("address"))
        {
            notif="Missing parameter address";
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
        if(!rjson.has("internal_pic"))
        {
            notif="Missing parameter internal_pic";
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

        try {
            ArrayList<String> listtenant = new ArrayList<String>();
            ArrayList<Long> idlisttenant = new ArrayList<Long>();

            List<Product> dataTenant = productRepository.findAll();

            for (int l = 0 ; l < dataTenant.size() ; l++)
            {
                listtenant.add(dataTenant.get(l).getPkey());
                idlisttenant.add(dataTenant.get(l).getId());
            }

            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"List products : " + listtenant);

            String subscription = null;

            if(rjson.has("subscription"))
            {
                subscription = rjson.getString("subscription");
            }
            else
            {
                subscription = "prepaid";
            }

            if(rjson.has("product"))
            {
                JSONArray productList = rjson.getJSONArray("product");

                for(int c=0 ; c < productList.length() ; c++)
                {
                    Product product = productRepository.findByPkey(productList.getString(c));
                    if(product == null)
                    {
                        logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Product not available");
                        notif = "Product '" + productList.getString(c) + "' not exist";
                        jo.put("result_code", "B01");
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

//                    if (AccountsRepo.findByExternalKey(rjson.getString("xkey"), product.getId()) != null)
//                    {
//                        logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Account already exist " + rjson.getString("xkey") + " " + productList.getString(c));
//                        productList.remove(c);
//                        c = 0;
//                        notif = "Account '" + productList.getString(c) + "' already exist";
//                        jo.put("result_code", "B04");
//                        jo.put("message", notif);
//                        jo.put("log", uuid);
//                        jo.put("timestamp", dateFormatter.getTimestamp());
//                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
//                        logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo.toString());
//                        return ResponseEntity
//                                .status(200)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .body(jo.toString());
//                    }
                }

                JSONArray successProduct = new JSONArray();

                for(int b=0 ; b < productList.length() ; b++)
                {
                    Product tenantData = productRepository.findByPkey(productList.getString(b));

                    if (AccountsRepo.findByExternalKey(rjson.getString("xkey"), tenantData.getId()) == null) {
                        logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Create account " + tenantData.getPkey());
                        Account account = new Account();
//                    Product tenantData = productRepository.findByPkey(rjson.getString("product"));

                        Balance mb = new Balance();

//                    if (AccountsRepo.findByExternalKey(rjson.getString("xkey"), tenantData.getId()) == null) {
                        account.setXkey(rjson.getString("xkey"));
                        account.setName(rjson.getString("name"));
                        account.setAddress(rjson.getString("address"));
                        account.setCreatedDate(new Date());
                        account.setUpdatedDate(new Date());
                        account.setCompany(rjson.getString("company"));
                        account.setProduct(tenantData);

                        StringBuilder externalPIC= new StringBuilder();
                        StringBuilder internalPIC= new StringBuilder();

                        JSONArray listExternalPIC = rjson.getJSONArray("external_pic");
                        JSONArray listInternalPIC = rjson.getJSONArray("internal_pic");

                        for(int i = 0 ; i < listExternalPIC.length() ; i++)
                        {
                            externalPIC.append(listExternalPIC.get(i));
                            if(listExternalPIC.length() - i != 1)
                            {
                                externalPIC.append(",");
                            }
                        }

                        for(int i = 0 ; i < listInternalPIC.length() ; i++)
                        {
                            internalPIC.append(listInternalPIC.get(i));
                            if(listInternalPIC.length() - i != 1)
                            {
                                internalPIC.append(",");
                            }
                        }

                        account.setEmail(externalPIC.toString());
                        account.setInternal_email(internalPIC.toString());
                        if(tenantData.getCategory().equalsIgnoreCase("signer") || tenantData.getCategory().equalsIgnoreCase("document"))
                        {
                            account.setActive(false);
                        }
                        else
                        {
                            account.setActive(true);
                        }

                        //default type personal adalah 1
                        int type = 1;

                        if (rjson.getString("type").equalsIgnoreCase("karyawan")) {
                            type = 2;
                        }

                        if (rjson.getString("type").equalsIgnoreCase("mitra")) {
                            type = 3;
                        }

                        account.setType(type);

                        if (rjson.has("notifminimum")) {
                            account.setThreshold(rjson.getInt("notifminimum"));
                        } else {
                            account.setThreshold(200);
                        }

                        account.setAlertBalance(false);
                        account.setIsAlertBalance(false);

                        if (rjson.has("bill_period")) {
                            account.setBillPeriod(rjson.getInt("bill_period"));
                        } else {
                            account.setBillPeriod(25);
                        }

                        if (subscription.equals("postpaid")) {
                            account.setSubscription(subscription);
                        } else {
                            account.setSubscription(subscription);
                            mb.setBalance(0L);
                            mb.setAccount(account);
                        }

                        if (rjson.has("data")) {
                            if (rjson.getJSONObject("data").has("child")) {
                                if (rjson.getJSONObject("data").getBoolean("child")) {
                                    if (rjson.getJSONObject("data").has("parent_to")) {
                                        Account acc = AccountsRepo.findAccountByProduct(rjson.getJSONObject("data").getString("xkey"), tenantData.getId());

                                        if (acc == null) {
                                            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Account parent not exist");
                                            notif = "Account parent not exist";
                                            jo.put("result_code", "B01");
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
                                            account.setParent(acc.getId());
                                            AccountsRepo.save(account);
                                            mb = null;
                                        }
                                    } else {
                                        logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Missing parameter child on JSONObJECT data");
                                        notif = "Missing parameter parent on JSONObject data";
                                        jo.put("result_code", "B28");
                                        jo.put("message", notif);
                                        jo.put("log", uuid);
                                        jo.put("timestamp", dateFormatter.getTimestamp());
                                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                                        logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
                                        return ResponseEntity
                                                .status(200)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .body(jo.toString());
                                    }
                                }
                            } else {
                                logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + "Missing parameter child on JSONObJECT data");
                                notif = "Missing parameter child on JSONObject data";
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
                        } else {
                            AccountsRepo.save(account);
                            BalancesRepo.save(mb);
                        }
                    }

                    successProduct.put(productList.getString(b));

//                    else {
//                        notif = "Billing product already exist";
//                        jo.put("result_code", "B02");
//                        jo.put("message", notif);
//                        jo.put("log", uuid);
//                        jo.put("timestamp", dateFormatter.getTimestamp());
//                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
//                        logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);
//                        return ResponseEntity
//                                .status(200)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .body(jo.toString());
//                    }
                }
                notif = "Success";
                jo.put("result_code", "B00");
                jo.put("message", notif);
                jo.put("product", successProduct);
                jo.put("log", uuid);
                jo.put("timestamp", dateFormatter.getTimestamp());
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }
            else
            {
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Create default accounts");

                for(int b = 0 ; b < idlisttenant.size() ; b++)
                {
                    Account account = new Account();
                    List<Product> tenantData = productRepository.findAll();
                    Balance mb = new Balance();

                    if(AccountsRepo.findByExternalKey(rjson.getString("xkey"), Long.parseLong(idlisttenant.get(b).toString())) == null)
                    {
                        account.setXkey(rjson.getString("xkey"));
                        account.setName(rjson.getString("name"));
                        account.setAddress(rjson.getString("address"));
                        account.setCreatedDate(new Date());
                        account.setUpdatedDate(new Date());
                        account.setCompany(rjson.getString("company"));
                        account.setProduct(tenantData.get(b));
                        account.setThreshold(200);
                        account.setAlertBalance(false);
                        account.setIsAlertBalance(false);

                        StringBuilder externalPIC= new StringBuilder();
                        StringBuilder internalPIC= new StringBuilder();

                        JSONArray listExternalPIC = rjson.getJSONArray("external_pic");
                        JSONArray listInternalPIC = rjson.getJSONArray("internal_pic");

                        for(int i = 0 ; i < listExternalPIC.length() ; i++)
                        {
                            externalPIC.append(listExternalPIC.get(i));
                            if(listExternalPIC.length() - i != 1)
                            {
                                externalPIC.append(",");
                            }
                        }

                        for(int i = 0 ; i < listInternalPIC.length() ; i++)
                        {
                            internalPIC.append(listInternalPIC.get(i));
                            if(listInternalPIC.length() - i != 1)
                            {
                                internalPIC.append(",");
                            }
                        }

                        account.setEmail(externalPIC.toString());
                        account.setInternal_email(internalPIC.toString());

                        if(tenantData.get(b).getCategory().equalsIgnoreCase("signer") || tenantData.get(b).getCategory().equalsIgnoreCase("document"))
                        {
                            account.setActive(false);
                        }
                        else
                        {
                            account.setActive(true);
                        }

                        //default personal adalah 1
                        int type = 1;

                        if(rjson.getString("type").equalsIgnoreCase("karyawan"))
                        {
                            type = 2;
                        }

                        if(rjson.getString("type").equalsIgnoreCase("mitra"))
                        {
                            type = 3;
                        }

                        account.setType(type);

                        if(rjson.has("bill_period"))
                        {
                            account.setBillPeriod(rjson.getInt("bill_period"));
                        }
                        else
                        {
                            account.setBillPeriod(25);
                        }

                        productaccount.add(tenantData.get(b).getPkey());

                        if(subscription.equals("postpaid"))
                        {
                            account.setSubscription(subscription);
                        }
                        else
                        {
                            account.setSubscription(subscription);
                            mb.setBalance(0L);
                            mb.setAccount(account);
                        }

                        if(rjson.has("data"))
                        {
                            if(rjson.getJSONObject("data").has("child"))
                            {
                                if(rjson.getJSONObject("data").getBoolean("child"))
                                {
                                    if(rjson.getJSONObject("data").has("parent_to"))
                                    {
                                        //cari akun parent
                                        Account acc = AccountsRepo.findAccountByProduct(rjson.getJSONObject("data").getString("xkey"), Long.parseLong(idlisttenant.get(b).toString()));

                                        if(acc == null)
                                        {
                                            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Account parent not exist");
                                            notif="Account parent not exist";
                                            jo.put("result_code", "B01");
                                            jo.put("message", notif);
                                            jo.put("log", uuid);
                                            jo.put("timestamp", dateFormatter.getTimestamp());
                                            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                                            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " + jo);
                                            return ResponseEntity
                                                    .status(200)
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .body(jo.toString());
                                        }
                                        else
                                        {
                                            account.setParent(acc.getId());
                                            AccountsRepo.save(account);
                                            mb = null;
                                        }
                                    }
                                    else
                                    {
                                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Missing parameter child on JSONObJECT data");
                                        notif="Missing parameter parent on JSONObject data";
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
                                }
                            }
                            else
                            {
                                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Missing parameter child on JSONObJECT data");
                                notif="Missing parameter child on JSONObject data";
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
                        }
                        else
                        {
                            AccountsRepo.save(account);
                            BalancesRepo.save(mb);
                        }
                    }
                }

                if (productaccount.size() > 0)
                {
                    notif="Success";
                    jo.put("result_code", "B00");
                    jo.put("message", notif);
                    jo.put("product", productaccount);
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
                    notif="All account billing already exist";
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
            }

        }catch(Exception e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            notif="Error create billing account";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error create billing account : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
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

}