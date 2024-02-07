package id.idtrust.billing;

import id.idtrust.billing.PDF.Invoices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class GenerateDocumentTests {
    @Test
    void generateBisnis() throws IOException {
        Invoices invoice = new Invoices();
        invoice.generatePersonall();
    }
}
