package id.idtrust.billing.controller;

import id.idtrust.billing.model.Channel;
import id.idtrust.billing.model.Plan;
import id.idtrust.billing.repository.ChannelRepository;
import id.idtrust.billing.repository.PlanRepository;
import id.idtrust.billing.util.DateFormatter;
import id.idtrust.billing.util.Description;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping(value = "/billing")
public class Channels extends Description {

    @Autowired
    PlanRepository planRepository;

    @Autowired
    ChannelRepository channelRepository;

    DateFormatter dateFormatter = new DateFormatter();
    private static final Logger logger = LogManager.getLogger();
    @GetMapping(value="/channel", produces = {"application/json"})
    public ResponseEntity<?> get(@RequestParam String plan) throws Exception {
        long startTime = System.nanoTime();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        String notif="Account already exist";

        String uuid = UUID.randomUUID().toString().replace("-", "");

        logger.info("["+VERSION+"]-[BILLTRUST/REQUEST] : " +"Plan " + plan);
        
        JSONObject jo=new JSONObject();

        Plan dataPlan = planRepository.findByIds(Long.valueOf(plan));

        if (dataPlan == null)
        {
            notif="Plan not exist";
            jo.put("result", "B07");
            jo.put("notif", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }

        try {

            List<Channel> channel = channelRepository.getAll();

            JSONObject allChannel = new JSONObject();

            List<String> type = new ArrayList<String>();
            for(int b=0 ; b < channel.size() ; b++)
            {
                type.add(channel.get(b).getType());
            }

            JSONArray listData = new JSONArray();

            for (int i=0 ; i < type.size() ; i ++)
            {
                JSONArray lType = new JSONArray();
                for(int j=0 ; j < channel.size() ; j++)
                {
                    if(type.get(i).equals(channel.get(j).getType()))
                    {
                        JSONObject dataInType = new JSONObject();
                        int total = dataPlan.getTotal();
                        dataInType.put("channel", channel.get(j).getChannelCode());
                        dataInType.put("total", total);
                        dataInType.put("plan_price", dataPlan.getBundlePrice());

                        File inputFile = new File("bank/"+channel.get(j).getImage());

                        String ext = FilenameUtils.getExtension(inputFile.getName());

                        String mime = "";

                        if(ext.equalsIgnoreCase("svg"))
                        {
                            mime="data:image/svg+xml;base64,";
                        }

                        if(ext.equalsIgnoreCase("png"))
                        {
                            mime="data:image/png;base64,";
                        }

                        byte[] fileContent = FileUtils.readFileToByteArray(inputFile);
                        String imagetobase64 = Base64.getEncoder().encodeToString(fileContent);

                        dataInType.put("image", mime+imagetobase64);
                        dataInType.put("pixel", channel.get(j).getPixel());

                        lType.put(dataInType);
                    }
                }
                allChannel.put(type.get(i), lType);
            }

            Iterator keys = allChannel.keys();

            while(keys.hasNext()) {
                String key = keys.next().toString();

                JSONObject list = new JSONObject();

                list.put("type", key);
                list.put("list", allChannel.getJSONArray(key));

                listData.put(list);
            }

            jo.put("data", listData);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));
            jo.put("result_code", "B00");
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());

        }catch(RuntimeException e)
        {
            e.printStackTrace();
            logger.error("["+VERSION+"]-[BILLTRUST/ERROR] : " +e);
            notif="Failed get method channel";
            jo.put("result_code", "B06");
            jo.put("message", notif);
            jo.put("log", uuid);
            jo.put("timestamp", dateFormatter.getTimestamp());
            jo.put("error", e.toString());
            jo.put("ptime", formatter.format((System.nanoTime() - startTime)/1000000000d));

            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
    }
}