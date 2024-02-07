package id.idtrust.billing.controller;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.model.Account;
import id.idtrust.billing.model.Plan;
import id.idtrust.billing.model.PlanDetail;
import id.idtrust.billing.model.Product;
import id.idtrust.billing.repository.*;
import id.idtrust.billing.util.DateFormatter;
import id.idtrust.billing.util.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
public class Plans extends Description {
    @Autowired
    ProductRepository productRepository;
    @Autowired
    AccountRepository AccountsRepo;
    @Autowired
    PlanRepository planRepository;
    @Autowired
    PlanDetailRepository planDetailRepository;
    @Autowired
    private SessionFactory sessionFactory;
    DateFormatter dateFormatter = new DateFormatter();
    private static final Logger logger = LogManager.getLogger();

    @PostMapping(value="/plan/get", produces = {"application/json"}, consumes = "application/json")
    public ResponseEntity<?> get(@RequestBody String data) throws Exception {

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

        JSONObject jo=new JSONObject();

        //Cek jika ambil plan hanya emeterai
        if(rjson.has("plan"))
        {
            if(rjson.getString("plan").equals("emeterai"))
            {
                Product product = productRepository.findByPkey("emeterai");
                List<PlanDetail> planDetail = planDetailRepository.findByProduct(product.getId());

                JSONArray arrayPlan = new JSONArray();

                List<Long> plan = new ArrayList<Long>();

                for(int c = 0 ; c < planDetail.size() ; c++)
                {
                    plan.add(planDetail.get(c).getPlan().getId());
                }

                boolean isFree = false;
                int ppn = 0;
                int total=0;
                int admin_fee=0;

                for(int d = 0 ; d < plan.size() ; d++)
                {
                    int bundle_price=0;
                    long id = 0;
                    String name="";
                    String detail_id="";
                    String detail_en="";
                    boolean recommended=false;
                    JSONObject dataPlan = new JSONObject();

                    JSONArray arrayDetail = new JSONArray();

                    List<PlanDetail> pDetail = planDetailRepository.findByIds(plan.get(d));
                    for (int b = 0; b < pDetail.size(); b++)
                    {
                        JSONObject dataPlanDetail = new JSONObject();

                        dataPlanDetail.put("pkey", pDetail.get(b).getProduct().getPkey());
                        dataPlanDetail.put("price", pDetail.get(b).getPrice());
                        dataPlanDetail.put("qty", pDetail.get(b).getQty());
                        dataPlanDetail.put("unit_price", pDetail.get(b).getUnitPrice());

                        dataPlanDetail.put("title_id", pDetail.get(b).getProduct().getNameProduct());
                        dataPlanDetail.put("title_en", pDetail.get(b).getProduct().getNameProductEn());
                        dataPlanDetail.put("detail_id", pDetail.get(b).getProduct().getDetailId());
                        dataPlanDetail.put("detail_en", pDetail.get(b).getProduct().getDetailEn());

                        name = pDetail.get(b).getPlan().getName();
                        detail_id= pDetail.get(b).getPlan().getTitle_id();
                        detail_en= pDetail.get(b).getPlan().getTitle_en();
                        bundle_price = pDetail.get(b).getPlan().getBundlePrice();
                        id = pDetail.get(b).getPlan().getId();
                        recommended = pDetail.get(b).getPlan().getRecommended();
                        arrayDetail.put(dataPlanDetail);
                        isFree = pDetail.get(b).getPlan().isFree();
                        ppn = pDetail.get(b).getPlan().getPpn();
                        total = pDetail.get(b).getPlan().getTotal();
                        admin_fee = pDetail.get(b).getPlan().getAdmin_fee();
                    }

                    dataPlan.put("id", id);
                    dataPlan.put("bundle_price", bundle_price);
                    dataPlan.put("name", name);
                    dataPlan.put("detail_plan_id", detail_id);
                    dataPlan.put("detail_plan_en", detail_en);
                    dataPlan.put("detail", arrayDetail);
                    dataPlan.put("recommended", recommended);
                    dataPlan.put("free", isFree);
                    dataPlan.put("ppn", ppn);
                    dataPlan.put("admin_fee", admin_fee);
                    dataPlan.put("total", total);

                    arrayPlan.put(dataPlan);
                }

                jo.put("result_code", "B00");
                jo.put("message", "Success");
                jo.put("data", arrayPlan);
                jo.put("log", uuid);
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());

                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }
        }
        
        if(!rjson.has("xkey"))
        {
            notif="Missing parameter xkey";
            jo.put("result_code", "B28");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        try {
            List<Plan> plan;

            String xkey = rjson.getString("xkey");

            List<Account> accountBase = AccountsRepo.findSingleAccount(xkey);

            if(accountBase.size() < 1)
            {
                notif="Account not exist";
                jo.put("result_code", "B01");
                jo.put("message", notif);
                jo.put("log", uuid);
                jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jo.toString());
            }

            if(rjson.has("plan"))
            {
                plan = planRepository.findByIdList(rjson.getLong("plan"));
            }
            else
            {
                plan = planRepository.findByXkey(xkey);
            }

            String type="";

            if (plan.size() < 1)
            {
                if(rjson.has("type") && rjson.getString("type").equalsIgnoreCase("emeterai"))
                {
                    type = "emeterai";
                }
                else
                {
                    if(accountBase.get(0).getType() == 1)
                    {
                        type = "personal";
                        if(rjson.has("expired") && rjson.getBoolean("expired"))
                        {
                            type = "expired";
                        }
                        else
                        {
                            List<Account> account = AccountsRepo.findAccountStarter(xkey);

                            if (account.size() < 1)
                            {
                                type = "starter";
                                if(rjson.has("with_verification") && rjson.getBoolean("with_verification"))
                                {
                                    type = "starter_offline";
                                }
                            }
                        }
                    }
                    else
                    {
                        type = "merchant";
                    }
                }

                plan = planRepository.findByType(type);
            }

            JSONArray arrayPlan = new JSONArray();

            for (int i = 0 ; i < plan.size() ; i++)
            {
                JSONObject dataPlan = new JSONObject();

                List<PlanDetail> pDetail = new ArrayList<PlanDetail>();

                pDetail = planDetailRepository.findById(plan.get(i).getId());
                JSONArray arrayDetail = new JSONArray();

                for(int j = 0 ; j < pDetail.size() ; j++)
                {
//                    Account account = AccountsRepo.findAccountActiveByProduct(xkey, pDetail.get(j).getProduct().getId());

//                    if(account != null && !account.getProduct().getPkey().equalsIgnoreCase("sms"))

//                    if(account != null && !pDetail.get(j).getProduct().getPkey().equalsIgnoreCase("sms"))
                    if(!pDetail.get(j).getProduct().getPkey().equalsIgnoreCase("sms"))
//                    if(account != null)
                    {
//                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +account.getProduct().getPkey() + " " + account.getActive());
                        JSONObject dataPlanDetail = new JSONObject();

                        dataPlanDetail.put("pkey", pDetail.get(j).getProduct().getPkey());
                        dataPlanDetail.put("price", pDetail.get(j).getPrice());
                        dataPlanDetail.put("qty", pDetail.get(j).getQty());
                        dataPlanDetail.put("unit_price", pDetail.get(j).getUnitPrice());

                        dataPlanDetail.put("title_id", pDetail.get(j).getProduct().getNameProduct());
                        dataPlanDetail.put("title_en", pDetail.get(j).getProduct().getNameProductEn());
                        dataPlanDetail.put("detail_id", pDetail.get(j).getProduct().getDetailId());
                        dataPlanDetail.put("detail_en", pDetail.get(j).getProduct().getDetailEn());

                        arrayDetail.put(dataPlanDetail);
                    }
                }

                dataPlan.put("id", plan.get(i).getId());
                dataPlan.put("bundle_price", plan.get(i).getBundlePrice());
                dataPlan.put("name", plan.get(i).getName());
                dataPlan.put("detail", arrayDetail);
                dataPlan.put("recommended", plan.get(i).getRecommended());
                dataPlan.put("detail_plan_id", plan.get(i).getTitle_id());
                dataPlan.put("detail_plan_en", plan.get(i).getTitle_en());
                dataPlan.put("free", plan.get(i).isFree());
                dataPlan.put("ppn", plan.get(i).getPpn());
                dataPlan.put("total", plan.get(i).getTotal());
                dataPlan.put("admin_fee", plan.get(i).getAdmin_fee());

                arrayPlan.put(dataPlan);
            }

            jo.put("result_code", "B00");
            jo.put("message", "Success");
            jo.put("data", arrayPlan);
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
            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +e.toString());
            notif="Error get list plan";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("error", e.toString());
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            jo.put("timestamp", dateFormatter.getTimestamp());
            try
            {
                TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                bot.execute(new SendMessage(213382980, "Error get plan : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Externalkey : " + "\n externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
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

    @PostMapping(value="/plan/add", produces = {"application/json"}, consumes = "application/json")
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

        JSONObject jo=new JSONObject();

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

        if(!rjson.has("plan"))
        {
            notif="Missing parameter plan";
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

            if(rjson.get("plan")!=null)
            {
                JSONArray sendList = null;
                try
                {
                    sendList=(JSONArray) rjson.getJSONArray("plan");
                }catch(Exception e)
                {
                    notif="Incompatible type plan must be list of jsonobject";
                    jo.put("result_code", "B28");
                    jo .put("error", e.toString());
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

                List<PlanDetail> planDetail = new ArrayList<PlanDetail>();
                Plan plan = new Plan();

                Session session = sessionFactory.openSession();
                session.getTransaction().setTimeout(10);
                session.getTransaction().begin();

                for(int i=0; i<sendList.length(); i++)
                {
                    JSONObject obj=(JSONObject) sendList.get(i);

                    if(!obj.has("qty"))
                    {
                        notif="Missing parameter qty at array" + i;
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
                    if(!obj.has("unit_price"))
                    {
                        notif="Missing parameter unit_price at array" + i;
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
                        notif="Missing parameter product at array" + i;
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


                    Product product = new Product();
                    product = productRepository.findByPkey(obj.getString("product"));

                    Account account = null;

                    if(rjson.has("xkey"))
                    {
                        account = AccountsRepo.findByExternalKey(rjson.getString("xkey"), product.getId());

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
                    }

                    if(productRepository.findByPkey(obj.getString("product")) == null)
                    {
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +"Product not available");
                        notif="Product not exist";
                        jo.put("result_code", "B01");
                        jo.put("message", notif);
                        jo.put("timestamp", dateFormatter.getTimestamp());
                        jo.put("log", uuid);
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                        logger.info("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
                        return ResponseEntity
                                .status(200)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(jo.toString());
                    }
                    else
                    {

                        if(i<1)
                        {
                            if(account != null)
                            {
                                plan.setXkey(rjson.getString("xkey"));
                            }

                            plan.setType(rjson.getString("type"));
                            plan.setName(rjson.getString("name"));
                            plan.setBundlePrice(rjson.getInt("bundle_price"));
                            session.save(plan);
                        }

                        PlanDetail planD = new PlanDetail();
                        planD.setPlan(plan);
                        planD.setPrice(obj.getInt("unit_price"));
                        planD.setProduct(product);
                        planD.setQty(obj.getInt("qty"));
                        planD.setUnitPrice(obj.getInt("unit_price"));
                        planDetail.add(planD);
                        session.save(planD);
                    }
                }

                try {
                    session.getTransaction().commit();
                }catch (Exception e)
                {
                    e.printStackTrace();

                    notif="Failed process database";
                    jo.put("result_code", "06");
                    jo.put("message", notif);
                    jo.put("error", e.toString());
                    jo.put("log", uuid);
                    jo.put("timestamp", dateFormatter.getTimestamp());
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
                    logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());


                    try
                    {
                        TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                        bot.execute(new SendMessage(213382980, "Error add plan : " + uuid + "\n Address : " + inetAddress.getHostAddress() + "@" + inetAddress.getHostName() + "\n Externalkey : " + "\n externalkey : " + rjson.getString("xkey") + "\n Message : " + e));
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
                finally {
                    {
                        session.clear();
                        session.close();
                    }
                }

            }

            notif="Success create new plan";
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

        }catch(RuntimeException e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/INFO] : " +e.toString());
            notif="Failed add plan";
            jo.put("result_code", "06");
            jo.put("error", e.toString());
            jo.put("message", notif);
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