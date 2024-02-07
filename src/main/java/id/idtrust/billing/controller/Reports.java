package id.idtrust.billing.controller;

import com.opencsv.exceptions.CsvValidationException;
import id.idtrust.billing.model.Account;
import id.idtrust.billing.model.Invoice;
import id.idtrust.billing.model.Product;
import id.idtrust.billing.repository.AccountRepository;
import id.idtrust.billing.repository.ProductRepository;
import id.idtrust.billing.repository.ReportRepository;
import id.idtrust.billing.util.DateFormatter;
import id.idtrust.billing.util.Description;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

@RestController
@CrossOrigin
@RequestMapping(value = "/billing")
@Controller
public class Reports extends Description {

    @Autowired
    private DataSource dataSource;

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    ProductRepository productRepository;

    private static final String[] HEADERS = {"invoice_id", "transaction_date", "amount", "total", "Description"};
    DateFormatter dateFormatter = new DateFormatter();
    private static final Logger logger = LogManager.getLogger();

    @PostMapping(value = "/report", produces = {"text/csv"})
    public void report(HttpServletResponse response, HttpServletRequest r) throws JSONException, IOException, ParseException, CsvValidationException, SQLException {

        String jdbcURL = "jdbc:postgresql://"+DATABASE_URL+":"+DATABASE_PORT+"/billing?useSSL=true";
        String username = DATABASE_USER;
        String password = DATABASE_PASS;

        long startTime = System.nanoTime();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        NumberFormat formatter = new DecimalFormat("#0.00000");

        String data = IOUtils.toString(r.getReader());

        logger.info("[" + VERSION + "]-[BILLTRUST/REQUEST] : " + data);
        JSONObject json = new JSONObject(data);

        JSONObject jo = new JSONObject();

        if (!json.has("type_file")) {
            jo.put("result_code", "B28");
            jo.put("message", "Missing type_file");
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

            response.setStatus(200);
            response.setContentType("application/json");
            response.getWriter().write(jo.toString());

            logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
            return;
        }
        try {
            if (!json.has("xkey")) {
                jo.put("result_code", "B28");
                jo.put("message", "Missing xkey");
                jo.put("log", uuid);
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                response.setContentType("application/json");
                response.setStatus(200);
                response.getWriter().write(jo.toString());
                logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                return;
            }

            if (json.has("start_date") && json.has("end_date")) {
                DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date start = dFormat.parse(json.getString("start_date"));
                Date endDate = dFormat.parse(json.getString("end_date"));
                long time_difference = endDate.getTime() - start.getTime();

                if (start.compareTo(endDate) > 0) {
                    jo.put("result_code", "B10");
                    jo.put("message", "End date must be greater than start date");
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                    response.setContentType("application/json");
                    response.setStatus(200);
                    response.getWriter().write(jo.toString());
                    logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                    return;
                }

                long diff = (time_difference / (1000 * 60 * 60 * 24)) % 365;
                ;

                logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : Range " + diff + " hari");

                if (diff > 31) {
                    jo.put("result_code", "B11");
                    jo.put("message", "Maximum range is 31 days");
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                    response.setContentType("application/json");
                    response.setStatus(200);
                    response.getWriter().write(jo.toString());
                    logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                    return;
                }
            }

            if (!json.getString("type_file").equalsIgnoreCase("csv") && !json.getString("type_file").equalsIgnoreCase("json")) {

                jo.put("result_code", "B12");
                jo.put("message", "Not supported type file");
                jo.put("log", uuid);
                jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                response.setContentType("application/json");
                response.setStatus(200);
                response.getWriter().write(jo.toString());
                logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                return;
            }

            if (json.getString("type_file").equalsIgnoreCase("csv")) {

//                FileWriter out = new FileWriter("te.csv");
//                CSVPrinter csvPrinter = new CSVPrinter(response.getWriter(), CSVFormat.DEFAULT.withHeader(HEADERS));
                String query = null;

                if (json.has("period"))
                {
                    Product product = productRepository.findByPkey(json.getString("product"));

                    Account account = accountRepository.findByExternalKey(json.getString("xkey"), product.getId());

                    if (json.getString("period").equalsIgnoreCase("monthly"))
                    {
                        //query weekly
                        query = "SELECT date_trunc('month', created_date) AS txn_month,account_id, abs(SUM(amount)) as total from invoices where trx=2 and account_id = '"+account.getId()+"' group by account_id, txn_month";
                    }
                    if (json.getString("period").equalsIgnoreCase("yearly"))
                    {
                        //query monthly
                        query = "SELECT date_trunc('year', created_date) AS txn_yearly,account_id, abs(SUM(amount)) as total from invoices where trx=2 and account_id = '"+account.getId()+"' group by account_id, txn_month";
                    }
                    if (json.getString("period").equalsIgnoreCase("quarterly"))
                    {
                        //query quarterly
                        query = "SELECT date_trunc('quarter', created_date) AS txn_quarterly,account_id, abs(SUM(amount)) as total from invoices where trx=2 and account_id = '"+account.getId()+"' group by account_id, txn_month";
                    }
                    if (json.getString("period").equalsIgnoreCase("weekly"))
                    {
                        //query monthly
                        query = "SELECT date_trunc('week', created_date) AS txn_weekly,account_id, abs(SUM(amount)) as total from invoices where trx=2 and account_id = '"+account.getId()+"' group by date_trunc('week', created_date), account_id";
                    }
                } else {
                    if (json.has("start_date") && json.has("end_date"))
                    {
                        query = "select i.invoice_id, i.created_date, i.current_balance as current_balance, i.description, i.amount from products p, accounts a, invoices i where a.product = p.id and i.account_id = a.id and a.xkey='" + json.getString("xkey") + "' and p.pkey='" + json.getString("product") + "' and (DATE(i.created_date) >= '" + json.getString("start_date") + "' and DATE(i.created_date) <= '" + json.getString("end_date") + "') order by i.created_date DESC";
                    } else {
                        query = "select i.invoice_id, i.created_date, i.current_balance as current_balance, i.description, i.amount from products p, accounts a, invoices i where a.product = p.id and i.account_id = a.id and a.xkey='" + json.getString("xkey") + "' and p.pkey='" + json.getString("product") + "'  order by i.created_date DESC";
                    }
                }

                //generate csv in database
//            jdbcTemplate.execute("COPY (select i.invoice_id, i.created_date, i.current_balance as total, i.description, i.amount from products p, accounts a, invoices i where a.product = p.id and i.account_id = a.id and a.xkey='"+json.getString("xkey")+"' and p.pkey='"+json.getString("product")+"' and (DATE(i.created_date) >= cast('"+json.getString("start_date")+"' as date) and DATE(i.created_date) <= cast('"+json.getString("end_date")+"' as date)) order by i.created_date DESC) To '/root/aja.csv' With CSV DELIMITER ',' HEADER");
//
//            String userHome = System.getProperty("user.home");
//            CSVReader reader = new CSVReader(new FileReader(userHome+"/aja.csv"));
//            String [] nextLine;
//            while ((nextLine = reader.readNext()) != null) {
//                // nextLine[] is an array of values from the line
//                System.out.println(Arrays.toString(nextLine));
//            }


                CopyManager copy = null;
                try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {

                    copy = new CopyManager(connection.unwrap(BaseConnection.class));

                    copy.copyOut("COPY (" + query + ") To STDOUT With CSV DELIMITER ',' HEADER", response.getWriter());

//                    Statement statement = connection.createStatement();
//
//                    ResultSet result = statement.executeQuery(query);
//
//                    while (result.next()) {
//
//                        csvPrinter.printRecord(result.getString("invoice_id"),
//                                result.getString("created_date"),
//                                result.getString("amount"),
//                                result.getString("total"),
//                                result.getString("description"));
//
//                    }
//                    csvPrinter.flush();

//                    statement.close();

                } catch (SQLException e) {
                    System.out.println("Database error:");
                    e.printStackTrace();

                    jo.put("result_code", "B13");
                    jo.put("message", "Timeout connection");
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                    response.setContentType("application/json");
                    response.setStatus(200);
                    response.getWriter().write(jo.toString());
                    logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                    return;
                }

                response.setContentType("text/csv");
                if (json.has("product")) {
                    response.addHeader("Content-Disposition", "attachment; filename=\"" + json.getString("product") + ".csv\"");
                } else {
                    response.addHeader("Content-Disposition", "attachment; filename=\"file.csv\"");
                }


//            jdbcTemplate.query(query, new RowCallbackHandler() {
//
//                public void processRow(ResultSet resultSet) throws SQLException {
//
//                   do {
//                        try {
//                            csvPrinter.printRecord(resultSet.getString("invoice_id"),
//                                    resultSet.getString("created_date"),
//                                    resultSet.getString("amount"),
//                                    resultSet.getString("total"),
//                                    resultSet.getString("description"));
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    } while (resultSet.next());
//                }
//            });

//            csvPrinter.flush();

//
//            // Create the zip output stream
//            ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
//
//            // Create an array of files to be compressed
//            File[] filesToCompress = new File[1];
//            filesToCompress[0] = new File("te.csv");
//
//            // Compress each file and add it to the zip output stream
//            for (File fileToCompress : filesToCompress) {
//                ZipEntry zipEntry = new ZipEntry(fileToCompress.getName());
//                zipOutputStream.putNextEntry(zipEntry);
//
//                FileInputStream fileInputStream = new FileInputStream(fileToCompress);
//                byte[] buffer = new byte[1024];
//                int len;
//                while ((len = fileInputStream.read(buffer)) > 0) {
//                    zipOutputStream.write(buffer, 0, len);
//                }
//                fileInputStream.close();
//                zipOutputStream.closeEntry();
//            }
//
//            // Close the zip output stream
//            zipOutputStream.close();
//
//
//            response.setContentType("application/zip");
//            response.setHeader("Content-Disposition", "attachment; filename=\"files.zip\"");

                logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + response.getHeader("Content-Disposition") + ", " + response.getContentType());
                return;
            } else {
                JSONArray l = new JSONArray();

                if(!json.has("size"))
                {
                    jo.put("result_code", "B14");
                    jo.put("message", "'size' required type_file 'json'");
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                    response.setContentType("application/json");
                    response.setStatus(200);
                    response.getWriter().write(jo.toString());
                    logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                    return;
                }

                if(!json.has("page"))
                {
                    jo.put("result_code", "B15");
                    jo.put("message", "'page' required type_file 'json'");
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                    response.setContentType("application/json");
                    response.setStatus(200);
                    response.getWriter().write(jo.toString());
                    logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                    return;
                }

                int size = json.getInt("size");
                int page = json.getInt("page") - 1;

                if(size > 50)
                {
                    jo.put("result_code", "B16");
                    jo.put("message", "Maximum size on one page is 50");
                    jo.put("log", uuid);
                    jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                    response.setContentType("application/json");
                    response.setStatus(200);
                    response.getWriter().write(jo.toString());
                    logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                    return;
                }

                Pageable pageable = PageRequest.of(page, size);

                Page<Invoice> pageInvoice = null;

                List<Object[]> period = new ArrayList<Object[]>();

                JSONObject ms = new JSONObject();

                if (json.has("period"))
                {
                    Product product = productRepository.findByPkey(json.getString("product"));

                    Account account = accountRepository.findByExternalKey(json.getString("xkey"), product.getId());

                    if (json.getString("period").equalsIgnoreCase("weekly"))
                    {
                        //query weekly
                        period = reportRepository.findWeekly(account.getId());
                    }
                    if (json.getString("period").equalsIgnoreCase("monthly"))
                    {
                        //query monthly
                        period = reportRepository.findMonthly(account.getId());
                    }
                    if (json.getString("period").equalsIgnoreCase("quarterly"))
                    {
                        //query quarterly
                        period = reportRepository.findQuarterly(account.getId());
                    }
                    if (json.getString("period").equalsIgnoreCase("yearly"))
                    {
                        //query yearly
                        period = reportRepository.findYearly(account.getId());
                    }

                    for(int i = 0 ; i < period.size() ; i++)
                    {
                        JSONObject print = new JSONObject();

                        if (json.getString("period").equalsIgnoreCase("weekly"))
                        {
                            //query weekly
                            print.put("period", "week "+period.get(i)[0].toString().replace(".0", ""));
                        }
                        if (json.getString("period").equalsIgnoreCase("monthly"))
                        {
                            //query monthly
                            print.put("period", "month "+period.get(i)[0].toString().replace(".0", ""));
                        }
                        if (json.getString("period").equalsIgnoreCase("quarterly"))
                        {
                            //query quarterly
                            print.put("period", "Q"+period.get(i)[0].toString().replace(".0", ""));
                        }
                        if (json.getString("period").equalsIgnoreCase("yearly"))
                        {
                            //query yearly
                            print.put("period", period.get(i)[0]);
                        }

                        print.put("total", period.get(i)[1]);

                        if(!json.getString("period").equalsIgnoreCase("yearly"))
                        {
                            print.put("year", period.get(i)[2]);
                        }

                        if(json.getString("period").equalsIgnoreCase("weekly"))
                        {
                            print.put("month", period.get(i)[3].toString().replace(".0", ""));
                        }

                        l.put(print);
                    }
                }
                else {
                    if (json.has("start_date") && json.has("end_date")) {
                        pageInvoice = reportRepository.findWithRangeDate(json.getString("xkey"), json.getString("product"), json.getString("start_date"), json.getString("end_date"), pageable);
                    } else {
                        pageInvoice = reportRepository.findDefault(json.getString("xkey"), json.getString("product"), pageable);
                    }

                    List<Invoice> listInvoice = pageInvoice.getContent();

                    if(pageInvoice.getTotalElements() == 0)
                    {
                        jo.put("result_code", "B17");
                        jo.put("message", "Empty Data");
                        jo.put("log", uuid);
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                        response.setContentType("application/json");
                        response.setStatus(200);
                        response.getWriter().write(jo.toString());
                        logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                        return;
                    }

                    if(page + 1 > pageInvoice.getTotalPages())
                    {
                        jo.put("result_code", "B18");
                        jo.put("message", "Limit page is " + pageInvoice.getTotalPages());
                        jo.put("log", uuid);
                        jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

                        response.setContentType("application/json");
                        response.setStatus(200);
                        response.getWriter().write(jo.toString());
                        logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
                        return;
                    }

                    for(int i = 0 ; i < listInvoice.size() ; i++)
                    {
                        JSONObject print = new JSONObject();

                        print.put("invoice_id", listInvoice.get(i).getInvoiceId());
                        print.put("created_date", listInvoice.get(i).getCreatedDate());
                        print.put("amount", listInvoice.get(i).getAmount());
                        print.put("current_balance", listInvoice.get(i).getCurrentBalance());

                        print.put("description", listInvoice.get(i).getDescription());

                        l.put(print);
                    }

                    ms.put("totalElements", pageInvoice.getTotalElements());
                    ms.put("page", page+1);
                    ms.put("size", size);
                    ms.put("totalPages", pageInvoice.getTotalPages());
                }

                ms.put("data", l);
                ms.put("result_code", "B00");
                ms.put("message", "Success");

                response.setContentType("application/json");

                logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + response.getHeader("Content-Disposition") + ", " + response.getContentType());

                response.getWriter().write(ms.toString());
                response.getWriter().flush();
                return;
            }


        } catch (Exception e) {
            e.printStackTrace();
            jo.put("result_code", "B06");
            jo.put("message", e.toString());
            jo.put("log", uuid);
            jo.put("ptime", formatter.format((System.nanoTime() - startTime) / 1000000000d));

            response.setContentType("application/json");
            response.setStatus(200);
            response.getWriter().write(jo.toString());
            logger.info("[" + VERSION + "]-[BILLTRUST/RESPONSE] : " + jo);
            return;
        }
    }
}

