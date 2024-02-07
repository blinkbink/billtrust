package id.idtrust.billing.util;

public class Description {
    public final String VERSION="V1.0.0B1";

    //RABBITMQ
    public static final String RABBITMQ_URL=System.getenv("RABBITMQ_URL");
    public static final String RABBITMQ_USER=System.getenv("RABBITMQ_USER");
    public static final String RABBITMQ_PASSWORD=System.getenv("RABBITMQ_PASSWORD");
    public static final String RABBITMQ_PORT=System.getenv("RABBITMQ_PORT");

    //KAFKA
    public static final String KAFKA_URL=System.getenv("KAFKA_URL");
    public static final String KAFKA_SCHEMA_URL=System.getenv("KAFKA_SCHEMA_URL");

    //XENDIT
    public static final String X_CALLBACK_TOKEN = System.getenv("XENDIT_TOKEN");
    public static final String USER_SERVICE = System.getenv("USERSERVICE");
    public static final String XENDIT_URL = "https://api.xendit.co/";
    public static final String XENDIT_CREDENTIAL =System.getenv("XENDIT_CREDENTIAL");

    public static final String DATABASE_URL =System.getenv("DATABASE_URL");
    public static final String DATABASE_PORT =System.getenv("DATABASE_PORT");
    public static final String DATABASE_USER =System.getenv("DATABASE_USER");
    public static final String DATABASE_PASS =System.getenv("DATABASE_PASS");

    public static final Integer RABBITMQ_DELLAYED_TIME = Integer.valueOf(System.getenv("RABBITMQ_DELAYED_TIME"));

    public static final String PAYMENT_FILE = "file/payments/";
    public static final String INVOICE_FILE_PERSONAL = "file/invoice/personal/";
    public static final String INVOICE_FILE_BISNIS = "./file/invoice/bisnis/";

}