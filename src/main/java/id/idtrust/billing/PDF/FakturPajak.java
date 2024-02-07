//package id.idtrust.billing.PDF;//package id.idtrust.billing.PDF;
//
//import com.itextpdf.io.font.constants.StandardFonts;
//import com.itextpdf.io.image.ImageData;
//import com.itextpdf.io.image.ImageDataFactory;
//import com.itextpdf.kernel.colors.DeviceRgb;
//import com.itextpdf.kernel.font.PdfFont;
//import com.itextpdf.kernel.font.PdfFontFactory;
//import com.itextpdf.kernel.pdf.PdfDocument;
//import com.itextpdf.kernel.pdf.PdfPage;
//import com.itextpdf.kernel.pdf.PdfWriter;
//import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
//import com.itextpdf.layout.Canvas;
//import com.itextpdf.layout.Document;
//import com.itextpdf.layout.element.*;
//import com.itextpdf.layout.properties.TextAlignment;
//import com.itextpdf.layout.properties.VerticalAlignment;
//
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.util.UUID;
//
//public class Invoices {
//
//    public void generate() throws IOException, URISyntaxException {
//        // Creating a PdfWriter
//        String uuid = UUID.randomUUID().toString();
//        String dest = "invoices.pdf";
//        PdfWriter writer = new PdfWriter(dest);
//
//        // Creating a PdfDocument
//        PdfDocument pdfDoc = new PdfDocument(writer);
//
//        // Creating a Document
//        Document document = new Document(pdfDoc);
//
//        //Create idtrust logo at tright top
//        String imFile = "idtrust.png";
//        ImageData data = ImageDataFactory.create(imFile);
//        Image image = new Image(data);
//
//        image.setFixedPosition(380,720);
//        image.setWidth(135);
//        image.setHeight(35);
//        //End idtrust logo
//
//        PdfFont font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
//
//        //Title
//        Paragraph title = new Paragraph("INVOICE");
//        title.setFontSize(25f);
//        title.setFixedPosition(60, 700, 1000);
//        title.setFont(font);
//        document.add(title);
//        //End title
//
//        //no
//        Paragraph no = new Paragraph("Nomor 2023/INV/1000");
//        no.setFontSize(11f);
//        no.setFixedPosition(60, 660, 1000);
//        no.setFont(font);
//        document.add(no);
//        //End no
//
//        //Tanggal
//        Paragraph tanggal = new Paragraph("Tanggal 1 Januari 2023");
//        tanggal.setFontSize(11f);
//        tanggal.setFixedPosition(60, 645, 1000);
//        tanggal.setFont(font);
//        document.add(tanggal);
//        //End tanggal
//
//        //To
//        Paragraph to = new Paragraph("Kepada,\nPT Grahadi Digital Technology");
//        to.setFontSize(11f);
//        to.setFixedPosition(60, 600, 1000);
//        to.setFont(font);
//        document.add(to);
//        //End To
//
//        //Paragraph 1
//        Paragraph paragraph = new Paragraph("Berikut informasi produk yang anda beli");
//        paragraph.setFontSize(11f);
//        paragraph.setFixedPosition(60, 510, 1000);
//        paragraph.setFont(font);
//        document.add(paragraph);
//        //End To
//
//        //Table
//        Table table = new Table(new float[] { 0, 3, 3});
//        table.setFixedPosition(60, 400, 460);
//        table.setFont(font);
//        table.setFontSize(11f);
//        // Adding cells to the table
//        //Header
//        Cell num = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("No")).setBold();
//        Cell produk = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Produk")).setBold();
//        Cell harga = new Cell(1, 1).setBackgroundColor(new DeviceRgb(245, 141, 66)).add(new Paragraph("Harga")).setBold();
//
//        table.addHeaderCell(num);
//        table.addHeaderCell(produk);
//        table.addHeaderCell(harga);
//        table.setTextAlignment(TextAlignment.CENTER);
//        //End Header
//
//        //Data
//        table.addCell("1");
//        table.addCell("SMS");
//        table.addCell("Rp. 10.000");
//
//        table.addCell("2");
//        table.addCell("Verifikasi");
//        table.addCell("Rp. 10.000");
//
//        Cell ppn = new Cell(1, 2).add(new Paragraph("PPn"));
//        table.addCell(ppn);
//        table.addCell("Rp. 2.000");
//
//        Cell total = new Cell(1, 2).add(new Paragraph("Total"));
//        table.addCell(total);
//        table.addCell("Rp. 20.000");
//        //End data
//
//        document.add(table);
//        //End Table
//
//        //Info Pembayaran
//        Paragraph pembayaran = new Paragraph("Segera lakukan pembayaran melalui rekening dibawah ini sejumlah invoice");
//        pembayaran.setFontSize(11f);
//        pembayaran.setFixedPosition(60, 360, 1000);
//        pembayaran.setFont(font);
//        document.add(pembayaran);
//        //logo bank
//        String pathBank = "bca.png";
//        ImageData dataBank = ImageDataFactory.create(pathBank);
//        Image imageBank = new Image(dataBank);
//
//        imageBank.setFixedPosition(60,290);
//        imageBank.setWidth(135);
//        imageBank.setHeight(90);
//        document.add(imageBank);
//
//        Paragraph rekening = new Paragraph("2302405001 A/n Muhammad Iqbal Pratama");
//        rekening.setFontSize(11f);
//        rekening.setFixedPosition(200, 330, 1000);
//        rekening.setFont(font);
//        document.add(rekening);
//        //End logo
//
//        //end info pembayaran
//
//        //Info marketing
//        Paragraph Note = new Paragraph("*Note:\n- Pembayaran maksimal dilakukan 2 hari setelah invoice dibuat\n- Verifikasi pembayaran dilakukan");
//        Note.setFontSize(11f);
//        Note.setFixedPosition(60, 200, 1000);
//        Note.setFont(font);
//        document.add(Note);
//        //End note
//
//        //Info marketing
//        Paragraph marketing = new Paragraph("Hermawan Putranto");
//        marketing.setFontSize(11f);
//        marketing.setFixedPosition(400, 160, 1000);
//        marketing.setFont(font);
//        document.add(marketing);
//
//        PdfPage pdfPage = pdfDoc.getFirstPage();
//        PdfCanvas line = new PdfCanvas(pdfPage);
//
//        line.moveTo(60, 580);
//        line.lineTo(520, 580);
//        line.setLineWidth(0.5f);
//        line.setStrokeColor(new DeviceRgb(245, 141, 66));
//        line.closePathStroke();
//        //End To
//
//        PdfCanvas canvas = new PdfCanvas(pdfPage);
//
//        canvas.moveTo(60, 75);
//        canvas.lineTo(520, 75);
//        canvas.setLineWidth(0.5f);
//        canvas.setStrokeColor(new DeviceRgb(245, 141, 66));
//        canvas.closePathStroke();
//
//        // Adding image to the document
//        document.add(image);
//
//
////        Paragraph watermark = createWatermarkParagraph("UNPAID");
////        document.add(watermark);
//
//
//        PdfCanvas canvass = new PdfCanvas(pdfPage);
//        new Canvas(canvass, pdfPage.getPageSize(), true)
//                .showTextAligned(createWatermarkParagraph("UNPAID"), 300, 450, 0, TextAlignment.CENTER, VerticalAlignment.MIDDLE, 45);
//
//
//        // Closing the document
//        document.close();
//    }
//
//    public Paragraph createWatermarkParagraph(String watermark) throws IOException {
//
//        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
//        Text text = new Text(watermark);
//        text.setFont(font);
//        text.setFontSize(70);
//        text.setFontColor(new DeviceRgb(217, 46, 46));
//        text.setOpacity(0.1f);
//        return new Paragraph(text);
//    }
//
//}