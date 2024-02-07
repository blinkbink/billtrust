package id.idtrust.billing.controller;

import id.idtrust.billing.model.Account;
import id.idtrust.billing.model.BusinessPrice;
import id.idtrust.billing.model.Product;
import id.idtrust.billing.repository.AccountRepository;
import id.idtrust.billing.repository.ProductRepository;
import id.idtrust.billing.repository.BusinessPriceRepository;
import id.idtrust.billing.util.DateFormatter;
import id.idtrust.billing.util.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping(value = "/billing")
@Service
public class Products extends Description {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    BusinessPriceRepository businessPriceRepository;

    DateFormatter dateFormatter = new DateFormatter();
    private static final Logger logger = LogManager.getLogger();

    @GetMapping(value="/product/pricelist/personal", produces = {"application/json"})
    public ResponseEntity<?> pricelistPersonal(@RequestParam(required = false) String xkey, @RequestParam(required = false) String category) throws Exception {

        long startTime = System.nanoTime();

        InetAddress inetAddress = null;

        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {

            e1.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif = "Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject jo = new JSONObject();

        try {

            List<Product> product = new ArrayList<Product>();

            if(xkey == null)
            {
                product = productRepository.findAll();
            }
            else
            {
                if(category != null)
                {
                    product = productRepository.findByAccountWCategory(xkey, category);
                }
                else
                {
                    product = productRepository.findByAccount(xkey);
                }

                if(product.size() < 1)
                {
                    jo.put("result_code", "B04");
                    jo.put("message", "Account not exist");
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
            }

            JSONArray listproduct = new JSONArray();

            List<String> listProduct = new ArrayList<String>();

            for (int i = 0; i < product.size(); i++)
            {
                JSONObject objectProduct = new JSONObject();

                if(!listProduct.contains(product.get(i).getCategory()))
                {
                    objectProduct.put("category", product.get(i).getCategory());
                    listProduct.add(product.get(i).getCategory());

                    JSONArray pr = new JSONArray();
                    for (int j = 0; j < product.size(); j++)
                    {
                        if (product.get(j).getCategory().equals(product.get(i).getCategory()))
                        {
                            if(!product.get(j).getPkey().equalsIgnoreCase("selfie") && !product.get(j).getPkey().equalsIgnoreCase("text") && !product.get(j).getPkey().equalsIgnoreCase("signer-2") && !product.get(j).getPkey().equalsIgnoreCase("signer-u"))
                            {
                                JSONObject lproduct = new JSONObject();

                                lproduct.put("pkey", product.get(j).getPkey());
                                if (product.get(j).getBase_price() == 0) {
                                    lproduct.put("base_price", "free");
                                }

                                lproduct.put("title_id", product.get(j).getNameProduct());
                                lproduct.put("title_en", product.get(j).getNameProductEn());
                                lproduct.put("description_id", product.get(j).getDetailId());
                                lproduct.put("description_en", product.get(j).getDetailEn());

                                pr.put(lproduct);
                            }
                        }
                    }
                    objectProduct.put("product", pr);
                    listproduct.put(objectProduct);
                }
            }

            JSONArray tnc_id = new JSONArray();
            JSONArray tnc_en = new JSONArray();

            tnc_id.put("Terkait dengan harga EKYC Rp. 10.000 dengan rincian pembagian sebagai berikut;");
            tnc_id.put("EKYC Demografi/text Rp. 3000");
            tnc_id.put("EKYC Biometrik + Certificate Issued Rp. 7000");

            tnc_en.put("Related to the EKYC price of Rp. 10,000 with details of the distribution as follows;");
            tnc_en.put("EKYC Demographics/text Rp. 3000");
            tnc_en.put("EKYC Biometric + Certificate Issued Rp. 7000");

            jo.put("result_code", "B00");
            jo.put("message", "Success");

            JSONObject tnc = new JSONObject();

            tnc.put("id", tnc_id);
            tnc.put("en", tnc_en);

            jo.put("term_and_condition", tnc);

            jo.put("log", uuid);
            jo.put("data", listproduct);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo);

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e);
            jo.put("result_code", "B06");
            jo.put("message", "Failed get list product");
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }

    @GetMapping(value="/product/pricelist/bisnis", produces = {"application/json"})
    public ResponseEntity<?> pricelistBisnis(@RequestParam(required = false) String xkey, @RequestParam(required = false) String category) throws Exception {

        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject jo = new JSONObject();

        try {
            List<BusinessPrice> businessPrices = new ArrayList<BusinessPrice>();
            List<Product> product;
            JSONArray listproduct = new JSONArray();
            if(xkey != null) {
                if (category != null) {
                    businessPrices = businessPriceRepository.bisnisFindByCategory(xkey, category);
                } else {
                    businessPrices = businessPriceRepository.bisnisFindByXkey(xkey);
                }

                if (businessPrices.size() > 0) {
                    for (int i = 0; i < businessPrices.size(); i++) {
                        listproduct = new JSONArray();

                        List<String> listProduct = new ArrayList<String>();

                        JSONObject objectProduct = new JSONObject();
                        listProduct.add("1");

                        if (!listProduct.contains(businessPrices.get(i).getAccount().getProduct().getCategory())) {
                            objectProduct.put("category", businessPrices.get(i).getAccount().getProduct().getCategory());
                            listProduct.add(businessPrices.get(i).getAccount().getProduct().getCategory());

                            JSONArray pr = new JSONArray();
                            for (int j = 0; j < businessPrices.size(); j++) {
                                if (businessPrices.get(j).getAccount().getProduct().getCategory().equals(businessPrices.get(i).getAccount().getProduct().getCategory())) {
                                    JSONObject lproduct = new JSONObject();

                                    lproduct.put("id", businessPrices.get(j).getId());
                                    lproduct.put("pkey", businessPrices.get(j).getAccount().getProduct().getPkey());
                                    if (businessPrices.get(j).getPrice() == 0) {
                                        lproduct.put("unit_price", "free");
                                    } else {
                                        lproduct.put("unit_price", businessPrices.get(j).getPrice());
                                    }

                                    lproduct.put("title_id", businessPrices.get(j).getAccount().getProduct().getNameProduct());
                                    lproduct.put("title_en", businessPrices.get(j).getAccount().getProduct().getNameProductEn());
                                    lproduct.put("description_id", businessPrices.get(j).getAccount().getProduct().getDetailId());
                                    lproduct.put("description_en", businessPrices.get(j).getAccount().getProduct().getDetailEn());

                                    pr.put(lproduct);
                                }
                            }
                            objectProduct.put("product", pr);
                            listproduct.put(objectProduct);
                        }
                    }
                }
                else
                {
                    product = productRepository.fingAllAccountMitraProduct();

                    if(product.size() < 1)
                    {
                        jo.put("result_code", "B04");
                        jo.put("message", "Account not exist");
                        jo.put("log", uuid);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }

                    List<String> listProduct = new ArrayList<String>();

                    for (int i = 0; i < product.size(); i++)
                    {
                        JSONObject objectProduct = new JSONObject();

                        if(!listProduct.contains(product.get(i).getCategory()))
                        {
                            objectProduct.put("category", product.get(i).getCategory());
                            listProduct.add(product.get(i).getCategory());

                            JSONArray pr = new JSONArray();
                            for (int j = 0; j < product.size(); j++)
                            {
                                if (product.get(j).getCategory().equals(product.get(i).getCategory()))
                                {
                                    JSONObject lproduct = new JSONObject();

                                    lproduct.put("id", product.get(j).getId());
                                    lproduct.put("pkey", product.get(j).getPkey());
                                    if (product.get(j).getBase_price() == 0) {
                                        lproduct.put("unit_price", "free");
                                    } else {
                                        lproduct.put("unit_price", product.get(j).getBase_price());
                                    }
                                    lproduct.put("title_id", product.get(j).getNameProduct());
                                    lproduct.put("title_en", product.get(j).getNameProductEn());
                                    lproduct.put("detail_id", product.get(j).getDetailId());
                                    lproduct.put("detail_en", product.get(j).getDetailEn());

                                    pr.put(lproduct);
                                }
                            }
                            objectProduct.put("product", pr);
                            listproduct.put(objectProduct);
                        }
                    }
                }
            }
            else
            {
                product = productRepository.fingAllAccountMitraProduct();

                if(product.size() < 1)
                {
                    jo.put("result_code", "B04");
                    jo.put("message", "Account not exist");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }

                List<String> listProduct = new ArrayList<String>();

                for (int i = 0; i < product.size(); i++)
                {
                    JSONObject objectProduct = new JSONObject();

                    if(!listProduct.contains(product.get(i).getCategory()))
                    {
                        objectProduct.put("category", product.get(i).getCategory());
                        listProduct.add(product.get(i).getCategory());

                        JSONArray pr = new JSONArray();
                        for (int j = 0; j < product.size(); j++)
                        {
                            if (product.get(j).getCategory().equals(product.get(i).getCategory()))
                            {
                                JSONObject lproduct = new JSONObject();

                                lproduct.put("id", product.get(j).getId());
                                lproduct.put("pkey", product.get(j).getPkey());
                                if (product.get(j).getBase_price() == 0) {
                                    lproduct.put("unit_price", "free");
                                } else {
                                    lproduct.put("unit_price", product.get(j).getBase_price());
                                }
                                lproduct.put("title_id", product.get(j).getNameProduct());
                                lproduct.put("title_en", product.get(j).getNameProductEn());
                                lproduct.put("detail_id", product.get(j).getDetailId());
                                lproduct.put("detail_en", product.get(j).getDetailEn());

                                pr.put(lproduct);
                            }
                        }
                        objectProduct.put("product", pr);
                        listproduct.put(objectProduct);
                    }
                }
            }

            JSONArray tnc_id = new JSONArray();
            JSONArray tnc_en = new JSONArray();

            tnc_id.put("Harga belum termasuk pajak");
            tnc_id.put("Pembayaran dilakukan di muka (Pre-paid/ Up - front based)");
            tnc_id.put("Minimum pembelian / Top Up layanan Document Sign dan /atau Electronic Sign sebanyak Qty 1.000");
            tnc_id.put("Harga SMS OTP dapat berubah sewaktu-waktu");

            tnc_en.put("Prices do not include tax");
            tnc_en.put("Payment is made in advance(Pre-paid/ Up - front based)");
            tnc_en.put("Minimum purchase/Top Up of Document Sign and/or Electronic Sign services as many as Qty 1.000");
            tnc_en.put("OTP SMS prices can change at any time");

            jo.put("result_code", "B00");
            jo.put("message", "Success");

            JSONObject tnc = new JSONObject();

            tnc.put("id", tnc_id);
            tnc.put("en", tnc_en);

            jo.put("term_and_condition", tnc);

            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("data", listproduct);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("[" + VERSION + "]-[BILLTRUST/INFO] : " + jo);

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e);
            jo.put("result_code", "B06");
            jo.put("message", "Failed get list product");
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

    }

    @GetMapping(value="/product/get/mitra", produces = {"application/json"})
    public ResponseEntity<?> getMitra(@RequestParam(required = false) String xkey, @RequestParam(required = false) String category) throws Exception {

        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif = "Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject jo = new JSONObject();

        try {
            List<Product> product = new ArrayList<Product>();

            if(xkey == null)
            {
                product = productRepository.fingAllAccountMitraProduct();
            }
            else
            {
                product = productRepository.findByAccountMitraProduct(xkey);

                if(product.size() < 1)
                {
                    jo.put("result_code", "B04");
                    jo.put("message", "Account not exist");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
            }

            JSONArray listproduct = new JSONArray();

            List<String> listProduct = new ArrayList<String>();

            for (int i = 0; i < product.size(); i++)
            {
                JSONObject objectProduct = new JSONObject();

                if(!listProduct.contains(product.get(i).getCategory()))
                {
                    objectProduct.put("category", product.get(i).getCategory());
                    listProduct.add(product.get(i).getCategory());

                    JSONArray pr = new JSONArray();
                    for (int j = 0; j < product.size(); j++)
                    {
                        if (product.get(j).getCategory().equals(product.get(i).getCategory()))
                        {
                            JSONObject lproduct = new JSONObject();

                            lproduct.put("pkey", product.get(j).getPkey());
                            lproduct.put("title_id", product.get(j).getNameProduct());
                            lproduct.put("title_en", product.get(j).getNameProductEn());
                            lproduct.put("detail_id", product.get(j).getDetailId());
                            lproduct.put("detail_en", product.get(j).getDetailEn());

                            pr.put(lproduct);
                        }
                    }
                    objectProduct.put("product", pr);
                    listproduct.put(objectProduct);
                }
            }


            jo.put("result_code", "B00");
            jo.put("message", "Success");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("data", listproduct);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            jo.put("result_code", "B06");
            jo.put("message", "Failed get list product");
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

    @GetMapping(value="/product/get", produces = {"application/json"})
    public ResponseEntity<?> get(@RequestParam(required = false) String xkey, @RequestParam(required = false) String category) throws Exception {

        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif = "Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject jo = new JSONObject();

        try {
            List<Product> product = new ArrayList<Product>();

            if(xkey == null)
            {
                product = productRepository.findAll();
            }
            else
            {
                if(category != null)
                {
                    product = productRepository.findByAccountWCategory(xkey, category);
                }
                else
                {
                    product = productRepository.findByAccount(xkey);
                }
                
                if(product.size() < 1)
                {
                    jo.put("result_code", "B04");
                    jo.put("message", "Account not exist");
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
                    logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

                    return ResponseEntity
                            .status(200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jo.toString());
                }
            }

            JSONArray listproduct = new JSONArray();

            List<String> listProduct = new ArrayList<String>();

            for (int i = 0; i < product.size(); i++)
            {
                JSONObject objectProduct = new JSONObject();

                if(!listProduct.contains(product.get(i).getCategory()))
                {
                    objectProduct.put("category", product.get(i).getCategory());
                    listProduct.add(product.get(i).getCategory());

                    JSONArray pr = new JSONArray();
                    for (int j = 0; j < product.size(); j++)
                    {
                        if (product.get(j).getCategory().equals(product.get(i).getCategory()))
                        {
                            Account acc = accountRepository.findByExternalKey(xkey, product.get(j).getId());
                            if(acc != null && acc.getType() == 1)
                            {
                                if(!acc.getProduct().getPkey().equalsIgnoreCase("signer-2") && !acc.getProduct().getPkey().equalsIgnoreCase("signer-u"))
                                {
                                    JSONObject lproduct = new JSONObject();

                                    lproduct.put("pkey", product.get(j).getPkey());
                                    lproduct.put("title_id", product.get(j).getNameProduct());
                                    lproduct.put("title_en", product.get(j).getNameProductEn());
                                    lproduct.put("detail_id", product.get(j).getDetailId());
                                    lproduct.put("detail_en", product.get(j).getDetailEn());

                                    pr.put(lproduct);
                                }
                            }
                            else
                            {
                                JSONObject lproduct = new JSONObject();

                                lproduct.put("pkey", product.get(j).getPkey());
                                lproduct.put("title_id", product.get(j).getNameProduct());
                                lproduct.put("title_en", product.get(j).getNameProductEn());
                                lproduct.put("detail_id", product.get(j).getDetailId());
                                lproduct.put("detail_en", product.get(j).getDetailEn());

                                pr.put(lproduct);
                            }
                        }
                    }
                    objectProduct.put("product", pr);
                    listproduct.put(objectProduct);
                }
            }


            jo.put("result_code", "B00");
            jo.put("message", "Success");
            jo.put("log", uuid);
            jo.put("data", listproduct);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            jo.put("result_code", "B06");
            jo.put("message", "Failed get list product");
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

    @PostMapping(value="/product/add", produces = {"application/json"}, consumes = {"application/json"})
    public ResponseEntity<?> add(@RequestBody String data) throws Exception {
        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif = "Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        JSONObject rjson = new JSONObject(data);

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +rjson.toString());
        
        JSONObject jo = new JSONObject();

        if(!rjson.has("product"))
        {
            notif="Missing parameter product";
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
        if(!rjson.has("name_en"))
        {
            notif="Missing parameter name_en";
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
            Product product = new Product();
       
            if(productRepository.findByPkey(rjson.getString("product")) != null)
            {
                notif="product already exist";
                jo.put("result_code", "B02");
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
                product.setCreatedDate(new Date());
                product.setPkey(rjson.getString("product"));
                product.setNameProduct(rjson.getString("name"));
                product.setNameProductEn(rjson.getString("name_en"));
                product.setDetailId(rjson.getString("detail_id"));
                product.setDetailEn(rjson.getString("detail_en"));

                productRepository.save(product);

                notif="Success create product";
                jo.put("result_code", "00");
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


        }catch(RuntimeException e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e.toString());
            notif="Failed create product";
            jo.put("result_code", "06");
            jo.put("message", notif);
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }
}