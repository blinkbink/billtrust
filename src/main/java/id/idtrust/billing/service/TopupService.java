package id.idtrust.billing.service;

import id.idtrust.billing.api.Xendit;
import id.idtrust.billing.model.*;
import id.idtrust.billing.repository.*;
import id.idtrust.billing.util.Description;
import lombok.Getter;
import lombok.Setter;
import java.util.Calendar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

@Setter
@Getter
@Service
@Transactional(rollbackFor = Exception.class, timeout=10)
public class TopupService {
    NumberFormat formatter = new DecimalFormat("#0.00000");
    @Autowired
    TopupRepository topupRepository;

    @Autowired
    TopupProductRepository topupProductRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    BatchRepository batchRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    InvoiceFilesRepository invoiceFilesRepository;

    private List<Invoice> invoice;
    private List<Batch> batch;
    private List<TopupProduct> topupProducts;

    private Topup topup;
    private List<Topup> topupClaim;
    private Payment payment;

    private Bill bill;

    private Invoice inv;

    private InvoiceFiles invoiceFiles;

    static Description ds = new Description();
    private static final Logger logger = LogManager.getLogger();
    @Autowired
    private BillRepository billRepository;

    @Autowired
    Xendit sendXendit;

    @Transactional(rollbackFor = Exception.class, timeout=10)
    public void requestTopupBisnis() throws Exception {
        JSONObject ret = new JSONObject();
        try{

            paymentRepository.save(payment);
            topupRepository.save(topup);

//            invoiceFilesRepository.save(invoiceFiles);

            topupProductRepository.saveAll(topupProducts);
            ret.put("status", true);

        } catch (Exception e) {
            ret.put("status", false);
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            throw new Exception(e);
        }
    }

    @Transactional(rollbackFor = Exception.class, timeout=10)
    public JSONObject requestTopup(Calendar c, String description, String name, String email, String pnumber, double totalPayment, JSONArray methods) throws Exception {
        JSONObject ret = new JSONObject();
        try{
            topupRepository.save(topup);
            JSONObject respXendit = new JSONObject();
            String id = "IDTRUST-"+topup.getId();

            respXendit = sendXendit.paymentURL(methods, id, Integer.parseInt(new DecimalFormat("#").format(totalPayment)), c.getTime(), description, name, email, pnumber);
            if(respXendit.getInt("code") != 200)
            {
                ret.put("status", false);
            }
            else
            {
                ret.put("status", true);
                ret.put("xendit", respXendit);
                ret.put("id", id);
            }

            String code = respXendit.getString("invoice_url");

            payment.setPaymentCode(code);

            topup.setTotal(Integer.parseInt(new DecimalFormat("#").format(totalPayment)));

            paymentRepository.save(payment);

            topup.setPayment(payment);
            topupRepository.save(topup);

            topupProductRepository.saveAll(topupProducts);
            ret.put("status", true);
        } catch (Exception e) {
            ret.put("status", false);
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            throw new Exception(e);
        }
        return ret;
    }

    @Transactional(rollbackFor = Exception.class, timeout=10)
    public boolean postpaidPayment() throws Exception {
        try{
            invoiceRepository.save(inv);
            billRepository.save(bill);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            throw new Exception(e);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class, timeout=10)
    public int topupCallback() throws Exception {
        int spTopup = 0;
        try {
            for(int b=0 ; b < invoice.size() ; b++)
            {
                long gettoptupbalance = System.nanoTime();
                spTopup = topupRepository.SPTopup(invoice.get(b).getAccount().getId(), invoice.get(b).getAmount());

                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Time Get SP Balance : " + formatter.format((System.nanoTime() - gettoptupbalance) / 1000000000d));

                invoice.get(b).setCurrentBalance(spTopup);
            }

            if(this.getInvoiceFiles() != null)
            {
                invoiceFilesRepository.save(getInvoiceFiles());
            }

            invoiceRepository.saveAll(invoice);
            batchRepository.saveAll(batch);
            topupRepository.save(topup);
            paymentRepository.save(payment);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            throw new Exception(e);
        }
        return spTopup;
    }

    @Transactional(rollbackFor = Exception.class, timeout=10)
    public int topupClaim() throws Exception {
        int spTopup = 0;
        try {
            for(int b=0 ; b < invoice.size() ; b++)
            {
                long gettoptupbalance = System.nanoTime();
                spTopup = topupRepository.SPTopup(invoice.get(b).getAccount().getId(), invoice.get(b).getAmount());

                logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + "Processing topup : " + invoice.get(b).getAccount().getProduct().getPkey() + " Current Balance: " + spTopup + " time: " + formatter.format((System.nanoTime() - gettoptupbalance) / 1000000000d));

                invoice.get(b).setCurrentBalance(spTopup);
            }

            if(this.getInvoiceFiles() != null)
            {
                invoiceFilesRepository.save(getInvoiceFiles());
            }

            invoiceRepository.saveAll(invoice);
            batchRepository.saveAll(batch);
            topupRepository.saveAll(topupClaim);
            topupProductRepository.saveAll(topupProducts);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            throw new Exception(e);
        }
        return spTopup;
    }

    @Transactional(rollbackFor = Exception.class, timeout=10)
    public int topup() throws Exception {
        int spTopup = 0;
        try {
            for(int b=0 ; b < invoice.size() ; b++) {

                    long gettoptupbalance = System.nanoTime();
                    spTopup = topupRepository.SPTopup(invoice.get(b).getAccount().getId(), invoice.get(b).getAmount());

                    logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " +"Time Get SP Balance : " + formatter.format((System.nanoTime() - gettoptupbalance) / 1000000000d));

                invoice.get(b).setCurrentBalance(spTopup);
            }

            invoiceRepository.saveAll(invoice);
            batchRepository.saveAll(batch);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " +e);
            throw new Exception(e);
        }
        return spTopup;
    }

}
