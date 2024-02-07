package id.idtrust.billing.PDF;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.pdf.PdfContentByte;
import id.idtrust.billing.model.TopupProduct;
import id.idtrust.billing.util.Description;

import java.io.*;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class Invoices extends Description {

    private String invoicePath;
    private String documentBase64;

    public String getInvoicePath() {
        return invoicePath;
    }

    public void setInvoicePath(String invoicePath) {
        this.invoicePath = invoicePath;
    }

    public String getDocumentBase64() {
        return documentBase64;
    }

    public void setDocumentBase64(String documentBase64) {
        this.documentBase64 = documentBase64;
    }

    public String generatePersonal(List<TopupProduct> topupProductList, String name, String date) throws IOException, URISyntaxException, BadElementException {
        // Creating a PdfWriter

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(Invoices.class.getClassLoader().getResourceAsStream("kop.pdf"));
        PdfWriter writer = new PdfWriter(byteArrayOutputStream);

        // Creating a PdfDocument
        PdfDocument pdfDoc = new PdfDocument(reader, writer);

        // Creating a Document
        Document document = new Document(pdfDoc);

//        //Create idtrust logo at tright top
//        String imFile = "idtrust.png";
//        ImageData data = ImageDataFactory.create(imFile);
//        Image image = new Image(data);
//
//        image.setFixedPosition(380,720);
//        image.setWidth(135);
//        image.setHeight(35);
//        //End idtrust logo

        PdfFont font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);

        //Title
        Paragraph title = new Paragraph("INVOICE");
        title.setFontSize(25f);
        title.setFixedPosition(400, 700, 1000);
        title.setFont(font);
        document.add(title);
        //End title

        //no
        Paragraph no = new Paragraph("Nomor 2023/INV/1000");
        no.setFontSize(11f);
        no.setFixedPosition(60, 710, 1000);
        no.setFont(font);
        document.add(no);
        //End no

        //Tanggal
        Paragraph tanggal = new Paragraph("Tanggal " + date);
        tanggal.setFontSize(11f);
        tanggal.setFixedPosition(60, 697, 1000);
        tanggal.setFont(font);
        document.add(tanggal);
        //End tanggal

        //To
        Paragraph to = new Paragraph("Kepada,\n" + name);
        to.setFontSize(11f);
        to.setFixedPosition(60, 630, 1000);
        to.setFont(font);
        document.add(to);
        //End To

        //Paragraph 1
        Paragraph paragraph = new Paragraph("Berikut informasi paket yang anda beli");
        paragraph.setFontSize(11f);
        paragraph.setFixedPosition(60, 550, 1000);
        paragraph.setFont(font);
        document.add(paragraph);
        //End To

        //Table
        Table table = new Table(new float[] { 0, 3, 3, 3, 3});
        table.setFixedPosition(60, 440, 460);
        table.setFont(font);
        table.setFontSize(11f);
        // Adding cells to the table
        //Header
        Cell num = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("No")).setBold();
        Cell produk = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Produk")).setBold();
        Cell harga = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Harga")).setBold();
        Cell qty = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Qty")).setBold();
        Cell totalHarga = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Total")).setBold();

        table.addHeaderCell(num);
        table.addHeaderCell(produk);
        table.addHeaderCell(qty);
        table.addHeaderCell(harga);
        table.addHeaderCell(totalHarga);

        table.setTextAlignment(TextAlignment.CENTER);
        //End Header

        //Data
//        table.addCell("1");
//        table.addCell("SMS");
//        table.addCell("10");
//        table.addCell("Rp. 10.000");
//        table.addCell("Rp. 100.000");
//
//        table.addCell("2");
//        table.addCell("Verifikasi");
//        table.addCell("10");
//        table.addCell("Rp. 10.000");
//        table.addCell("Rp. 100.000");

        DecimalFormat kursIndonesia = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        DecimalFormatSymbols formatRp = new DecimalFormatSymbols();

        formatRp.setCurrencySymbol("Rp. ");
        formatRp.setGroupingSeparator('.');
        formatRp.setMonetaryDecimalSeparator(',');

        kursIndonesia.setDecimalFormatSymbols(formatRp);
        kursIndonesia.setMaximumFractionDigits(0);

        int PPn = 0;
        int invNo = 1;
        for(int i = 0 ; i < topupProductList.size() ; i++)
        {
            table.addCell(String.valueOf(invNo));
            table.addCell(topupProductList.get(i).getProduct().getNameProduct());
            table.addCell(topupProductList.get(i).getQty().toString());
            table.addCell(kursIndonesia.format(topupProductList.get(i).getUnit_price()));
            table.addCell(kursIndonesia.format(topupProductList.get(i).getPrice()));

            PPn += topupProductList.get(i).getPrice();

            invNo += 1;
        }

        Cell subtotal = new Cell(1, 4).add(new Paragraph("Subtotal")).setTextAlignment(TextAlignment.RIGHT).setFont(font);
        table.addCell(subtotal);
        table.addCell(kursIndonesia.format(PPn));

        Cell ppn = new Cell(1, 4).add(new Paragraph("PPn 11%")).setTextAlignment(TextAlignment.RIGHT).setFont(font);
        table.addCell(ppn);
        table.addCell(kursIndonesia.format(PPn*11/100));

        Cell total = new Cell(1, 4).add(new Paragraph("Total")).setTextAlignment(TextAlignment.RIGHT).setFont(font);
        table.addCell(total);
        table.addCell(kursIndonesia.format(PPn + (PPn*11/100)));
        //End data

        document.add(table);
        //End Table

        //Info Pembayaran
        Paragraph pembayaran = new Paragraph("Segera lakukan pembayaran melalui rekening dibawah ini sejumlah invoice");
        pembayaran.setFontSize(11f);
        pembayaran.setFixedPosition(60, 390, 1000);
        pembayaran.setFont(font);
        document.add(pembayaran);

        //logo bank
        ImageData dataBank = ImageDataFactory.create("bca.png");
        Image imageBank = new Image(dataBank);

        imageBank.setFixedPosition(60,320);
        imageBank.setWidth(135);
        imageBank.setHeight(90);
        document.add(imageBank);

        Paragraph rekening = new Paragraph("2302405001 A/n Muhammad Iqbal Pratama");
        rekening.setFontSize(11f);
        rekening.setFixedPosition(200, 360, 1000);
        rekening.setFont(font);
        document.add(rekening);
        //End logo

        //end info pembayaran

        //Info marketing
        Paragraph Note = new Paragraph("*Note:\n- Pembayaran maksimal dilakukan 2 hari setelah invoice dibuat\n- Verifikasi pembayaran dilakukan setelah melakukan pembayaran");
        Note.setFontSize(11f);
        Note.setFixedPosition(60, 280, 1000);
        Note.setFont(font);
        document.add(Note);
        //End note

        //Info marketing
        Paragraph marketing = new Paragraph("Hermawan Putranto");
        marketing.setFontSize(11f);
        marketing.setFixedPosition(430, 230, 1000);
        marketing.setFont(font);
        document.add(marketing);

        Paragraph headMarketing = new Paragraph("Head of Marketing");
        headMarketing.setFontSize(11f);
        headMarketing.setFixedPosition(433, 150, 1000);
        headMarketing.setFont(font);
        document.add(headMarketing);

//        // Adding image to the document
//        document.add(image);


//        Paragraph watermark = createWatermarkParagraph("UNPAID");
//        document.add(watermark);

//
//        PdfCanvas canvass = new PdfCanvas(pdfPage);
//        new Canvas(canvass, pdfPage.getPageSize(), true)
//                .showTextAligned(createWatermarkParagraph("UNPAID"), 300, 450, 0, TextAlignment.CENTER, VerticalAlignment.MIDDLE, 45);

        // Closing the document
        document.close();
        byte[] encodedBytes = Base64.getEncoder().encode(byteArrayOutputStream.toByteArray());

        String pathInvoice = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        FileOutputStream fos = new FileOutputStream(new File(INVOICE_FILE_PERSONAL + pathInvoice));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.writeTo(fos);

        this.setInvoicePath(pathInvoice);
        this.setDocumentBase64(new String(encodedBytes));

        return getDocumentBase64();
//        return byteArrayOutputStream.toByteArray();
    }

    public String generateBisnis(List<TopupProduct> topupProductList, String name, String date) throws IOException, URISyntaxException, BadElementException {
        // Creating a PdfWriter

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(Invoices.class.getClassLoader().getResourceAsStream("kop.pdf"));
        PdfWriter writer = new PdfWriter(byteArrayOutputStream);

        // Creating a PdfDocument
        PdfDocument pdfDoc = new PdfDocument(reader, writer);

        // Creating a Document
        Document document = new Document(pdfDoc);

//        //Create idtrust logo at tright top
//        String imFile = "idtrust.png";
//        ImageData data = ImageDataFactory.create(imFile);
//        Image image = new Image(data);
//
//        image.setFixedPosition(380,720);
//        image.setWidth(135);
//        image.setHeight(35);
//        //End idtrust logo

        PdfFont font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);

        //Title
        Paragraph title = new Paragraph("INVOICE");
        title.setFontSize(25f);
        title.setFixedPosition(400, 700, 1000);
        title.setFont(font);
        document.add(title);
        //End title

        //no
        Paragraph no = new Paragraph("Nomor 2023/INV/1000");
        no.setFontSize(11f);
        no.setFixedPosition(60, 710, 1000);
        no.setFont(font);
        document.add(no);
        //End no

        //Tanggal
        Paragraph tanggal = new Paragraph("Tanggal " + date);
        tanggal.setFontSize(11f);
        tanggal.setFixedPosition(60, 697, 1000);
        tanggal.setFont(font);
        document.add(tanggal);
        //End tanggal

        //To
        Paragraph to = new Paragraph("Kepada,\n" + name);
        to.setFontSize(11f);
        to.setFixedPosition(60, 630, 1000);
        to.setFont(font);
        document.add(to);
        //End To

        //Paragraph 1
        Paragraph paragraph = new Paragraph("Berikut informasi produk yang anda beli");
        paragraph.setFontSize(11f);
        paragraph.setFixedPosition(60, 550, 1000);
        paragraph.setFont(font);
        document.add(paragraph);
        //End To

        //Table
        Table table = new Table(new float[] { 0, 3, 3, 3, 3});

        table.setFixedPosition(60, 440, 460);
        table.setFont(font);
        table.setFontSize(11f);
        // Adding cells to the table
        //Header
        Cell num = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("No")).setBold();
        Cell produk = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Produk")).setBold();
        Cell harga = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Harga")).setBold();
        Cell qty = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Qty")).setBold();
        Cell totalHarga = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Total")).setBold();

        table.addHeaderCell(num);
        table.addHeaderCell(produk);
        table.addHeaderCell(qty);
        table.addHeaderCell(harga);
        table.addHeaderCell(totalHarga);

        table.setTextAlignment(TextAlignment.CENTER);
        //End Header

        //Data
//        table.addCell("1");
//        table.addCell("SMS");
//        table.addCell("10");
//        table.addCell("Rp. 10.000");
//        table.addCell("Rp. 100.000");
//
//        table.addCell("2");
//        table.addCell("Verifikasi");
//        table.addCell("10");
//        table.addCell("Rp. 10.000");
//        table.addCell("Rp. 100.000");

        DecimalFormat kursIndonesia = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        DecimalFormatSymbols formatRp = new DecimalFormatSymbols();

        formatRp.setCurrencySymbol("Rp. ");
        formatRp.setGroupingSeparator('.');
        formatRp.setMonetaryDecimalSeparator(',');

        kursIndonesia.setDecimalFormatSymbols(formatRp);
        kursIndonesia.setMaximumFractionDigits(0);

        int PPn = 0;
        int invNo = 1;
        for(int i = 0 ; i < topupProductList.size() ; i++)
        {
            table.addCell(String.valueOf(invNo));
            table.addCell(topupProductList.get(i).getProduct().getNameProduct());
            table.addCell(topupProductList.get(i).getQty().toString());
            table.addCell(kursIndonesia.format(topupProductList.get(i).getUnit_price()));
            table.addCell(kursIndonesia.format(topupProductList.get(i).getPrice()));

            PPn += topupProductList.get(i).getPrice();

            invNo += 1;
        }

        Cell subtotal = new Cell(1, 4).add(new Paragraph("Subtotal")).setTextAlignment(TextAlignment.RIGHT).setFont(font);
        table.addCell(subtotal);
        table.addCell(kursIndonesia.format(PPn));

        Cell ppn = new Cell(1, 4).add(new Paragraph("PPn 11%")).setTextAlignment(TextAlignment.RIGHT).setFont(font);
        table.addCell(ppn);
        table.addCell(kursIndonesia.format(PPn*11/100));

        Cell total = new Cell(1, 4).add(new Paragraph("Total")).setTextAlignment(TextAlignment.RIGHT).setFont(font);
        table.addCell(total);
        table.addCell(kursIndonesia.format(PPn + (PPn*11/100)));
        //End data

        document.add(table);
        //End Table

        //Info Pembayaran
        Paragraph pembayaran = new Paragraph("Segera lakukan pembayaran melalui rekening dibawah ini sejumlah invoice");
        pembayaran.setFontSize(11f);
        pembayaran.setFixedPosition(60, 390, 1000);
        pembayaran.setFont(font);
        document.add(pembayaran);

        //logo bank
        ImageData dataBank = ImageDataFactory.create("bca.png");
        Image imageBank = new Image(dataBank);

        imageBank.setFixedPosition(60,320);
        imageBank.setWidth(135);
        imageBank.setHeight(90);
        document.add(imageBank);

        Paragraph rekening = new Paragraph("2302405001 A/n Muhammad Iqbal Pratama");
        rekening.setFontSize(11f);
        rekening.setFixedPosition(200, 360, 1000);
        rekening.setFont(font);
        document.add(rekening);
        //End logo

        //end info pembayaran

        //Info marketing
        Paragraph Note = new Paragraph("*Note:\n- Pembayaran maksimal dilakukan 2 hari setelah invoice dibuat\n- Verifikasi pembayaran dilakukan setelah melakukan pembayaran");
        Note.setFontSize(11f);
        Note.setFixedPosition(60, 280, 1000);
        Note.setFont(font);
        document.add(Note);
        //End note

        //Info marketing
        Paragraph marketing = new Paragraph("Hermawan Putranto");
        marketing.setFontSize(11f);
        marketing.setFixedPosition(430, 230, 1000);
        marketing.setFont(font);
        document.add(marketing);

        Paragraph headMarketing = new Paragraph("Head of Marketing");
        headMarketing.setFontSize(11f);
        headMarketing.setFixedPosition(433, 150, 1000);
        headMarketing.setFont(font);
        document.add(headMarketing);

        // Closing the document
        document.close();
        byte[] encodedBytes = Base64.getEncoder().encode(byteArrayOutputStream.toByteArray());

        String pathInvoice = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        FileOutputStream fos = new FileOutputStream(new File(INVOICE_FILE_BISNIS + pathInvoice));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.writeTo(fos);

        this.setInvoicePath(pathInvoice);
        this.setDocumentBase64(new String(encodedBytes));

        return getDocumentBase64();
//        return byteArrayOutputStream.toByteArray();
    }

    public String generatePersonall() throws IOException {
        // Creating a PdfWriter

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(Invoices.class.getClassLoader().getResourceAsStream("kop.pdf"));
        PdfWriter writer = new PdfWriter(byteArrayOutputStream);

        // Creating a PdfDocument
        PdfDocument pdfDoc = new PdfDocument(reader, writer);

        // Creating a Document
        Document document = new Document(pdfDoc);

//        //Create idtrust logo at tright top
//        String imFile = "idtrust.png";
//        ImageData data = ImageDataFactory.create(imFile);
//        Image image = new Image(data);
//
//        image.setFixedPosition(380,720);
//        image.setWidth(135);
//        image.setHeight(35);
//        //End idtrust logo

        PdfFont font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);

        //Title
        Paragraph title = new Paragraph("INVOICE");
        title.setFontSize(25f);
        title.setFixedPosition(400, 700, 1000);
        title.setFont(font);
        document.add(title);
        //End title

        //no
        Paragraph no = new Paragraph("Nomor 2023/INV/1000");
        no.setFontSize(11f);
        no.setFixedPosition(60, 710, 1000);
        no.setFont(font);
        document.add(no);
        //End no

        //Tanggal
        Paragraph tanggal = new Paragraph("Tanggal " + "date");
        tanggal.setFontSize(11f);
        tanggal.setFixedPosition(60, 697, 1000);
        tanggal.setFont(font);
        document.add(tanggal);
        //End tanggal

        //To
        Paragraph to = new Paragraph("Kepada,\n" + "name");
        to.setFontSize(11f);
        to.setFixedPosition(60, 630, 1000);
        to.setFont(font);
        document.add(to);
        //End To

        //Paragraph 1
        Paragraph paragraph = new Paragraph("Berikut informasi paket yang anda beli");
        paragraph.setFontSize(11f);
        paragraph.setFixedPosition(60, 550, 1000);
        paragraph.setFont(font);
        document.add(paragraph);
        //End To

        //Table
        Table table = new Table(new float[] { 3, 1, 1});
        table.setBorder(new SolidBorder(0));
        table.setBorderBottom(new SolidBorder(0));
        table.setFixedPosition(120, 440, 350);
        table.setFont(font);
        table.setFontSize(11f);
        // Adding cells to the table
        //Header
        Cell namapkt = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Nama Paket")).setBold();
        Cell detail = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Produk")).setBold();
        Cell qty = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Qty")).setBold();

        table.addHeaderCell(namapkt);
        table.addHeaderCell(detail);
        table.addHeaderCell(qty);

        table.setTextAlignment(TextAlignment.CENTER);
        //End Header

//        //Data
        Cell c1 = new Cell();
        c1.setTextAlignment(TextAlignment.CENTER);
        c1.add(new Paragraph("Nama Paket"));
        table.addCell(c1);

        Cell c2 = new Cell();
        Cell c3 = new Cell();
        c2.setTextAlignment(TextAlignment.LEFT);
        c3.setTextAlignment(TextAlignment.CENTER);

        c2.add(new Paragraph("Tandatangan"));
        c2.add(new Paragraph("SMS"));

        c3.add(new Paragraph("10"));
        c3.add(new Paragraph("20"));

        table.addCell(c2);
        table.addCell(c3);

        DecimalFormat kursIndonesia = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        DecimalFormatSymbols formatRp = new DecimalFormatSymbols();

        formatRp.setCurrencySymbol("Rp. ");
        formatRp.setGroupingSeparator('.');
        formatRp.setMonetaryDecimalSeparator(',');

        kursIndonesia.setDecimalFormatSymbols(formatRp);
        kursIndonesia.setMaximumFractionDigits(0);

        //End data

        document.add(table);
        //End Table

        //Info Pembayaran
        Paragraph pembayaran = new Paragraph("Segera lakukan pembayaran melalui rekening dibawah ini sejumlah invoice");
        pembayaran.setFontSize(11f);
        pembayaran.setFixedPosition(60, 390, 1000);
        pembayaran.setFont(font);
        document.add(pembayaran);

        //Info marketing
        Paragraph Note = new Paragraph("*Note:\n- Pembayaran maksimal dilakukan 2 hari setelah invoice dibuat\n- Verifikasi pembayaran dilakukan setelah melakukan pembayaran");
        Note.setFontSize(11f);
        Note.setFixedPosition(60, 280, 1000);
        Note.setFont(font);
        document.add(Note);
        //End note

        //Info marketing
        Paragraph marketing = new Paragraph("Hermawan Putranto");
        marketing.setFontSize(11f);
        marketing.setFixedPosition(430, 230, 1000);
        marketing.setFont(font);
        document.add(marketing);

        Paragraph headMarketing = new Paragraph("Head of Marketing");
        headMarketing.setFontSize(11f);
        headMarketing.setFixedPosition(433, 150, 1000);
        headMarketing.setFont(font);
        document.add(headMarketing);

        // Closing the document
        document.close();
        byte[] encodedBytes = Base64.getEncoder().encode(byteArrayOutputStream.toByteArray());

        String pathInvoice = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        FileOutputStream fos = new FileOutputStream("invoices.pdf");

        byteArrayOutputStream.writeTo(fos);

        this.setInvoicePath(pathInvoice);
        this.setDocumentBase64(new String(encodedBytes));

        return getDocumentBase64();
    }
}