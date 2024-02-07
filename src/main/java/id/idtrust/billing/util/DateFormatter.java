package id.idtrust.billing.util;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateFormatter {

    public String getTimestamp(Date date)
    {
        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS");
        return DateFor.format(date);
    }

    public String getTimestamp()
    {
        Date date = new Date();
        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS");
        return DateFor.format(date);
    }

    public String tampilkanTanggalDanWaktu(Date tanggalDanWaktu, String pola, Locale lokal) {
        String tanggalStr = null;
        SimpleDateFormat formatter = null;
        if (lokal == null) {
            formatter = new SimpleDateFormat(pola);
        } else {
            formatter = new SimpleDateFormat(pola, lokal);
        }

        tanggalStr = formatter.format(tanggalDanWaktu);
        return tanggalStr;
    }

}