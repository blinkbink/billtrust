package id.idtrust.billing.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Postpaid {

        @Scheduled(cron = "0 0 0 25 * ?")
        public void generateBilling() {
            //Cek ke redis apa sudah ada server node lain yang eksekusi
        }
    }

