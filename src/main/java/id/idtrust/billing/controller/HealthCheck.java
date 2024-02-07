package id.idtrust.billing.controller;

import id.idtrust.billing.util.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping(value = "/billing")
@Service
public class HealthCheck extends Description {


    private static final Logger logger = LogManager.getLogger();
    static Description ds = new Description();

    @GetMapping(value="/healthcheck", produces = {"application/json"})
    public ResponseEntity<?> get() throws Exception {

            JSONObject jo = new JSONObject();

            jo.put("version", "v1.0.0");

            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +jo.toString());
            return ResponseEntity
                    .status(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jo.toString());
        }
}