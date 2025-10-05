package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Prisklass;
import com.example.api.ElpriserAPI.Elpris;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {

    // Record för att hålla resultat av min/max-beräkningarna
    // Innehåller start- och sluttid för både lägsta och högsta pris, samt deras respektive medelvärden
    // Används för både timpris och kvartstimme-data
    public record MinMaxResultatMedSnitt(
            Elpris lägstaStart,
            Elpris lägstaSlut,
            double lägstaSnitt,
            Elpris högstaStart,
            Elpris högstaSlut,
            double högstaSnitt
    ) {}

    public static void main(String[] args) {

        //Om inga argument skrivs in, visa hjälpmeny
        if (args.length == 0) {
            helpMenu();
            return;
        }
        //Sätter startvärden för flaggorna
        String zone = null;
        String date = null;
        String charging = null;
        boolean isSorted = false;

        //En for loop som går igenom alla argument som skickas in
        // och sätter variablerna baserat på flaggorna
        for (int i = 0; i < args.length; i++) {
            //switch case för att hantera olika flaggor
            switch (args[i]) {
                case "--zone":
                    //Om det finns ett argument efter --zone
                    if (i + 1 < args.length) {
                        //så sätter vi zone till det argumentet
                        zone = args[++i];
                        if (!zone.equals("SE1") && !zone.equals("SE2") && !zone.equals("SE3") && !zone.equals("SE4")) {
                            System.out.println("Ogiltig zon. Giltiga zoner: SE1, SE2, SE3, SE4");
                            return;
                        }
                    }
                    break;
                case "--date":
                    //Om det finns ett argument efter --date
                    if (i + 1 < args.length) {
                        //så ökar vi i med 1 och värdet sparas i dateInput
                        String dateInput = args[++i];
                        try {
                            //försöker tolka dateInput som ett datum i formatet YYYY-MM-DD
                            //om det lyckas, sätts date till det tolkade datumet
                            LocalDate parsed = LocalDate.parse(dateInput, DateTimeFormatter.ISO_LOCAL_DATE);
                            date = parsed.toString();
                        } catch (DateTimeParseException e) {
                            System.out.println("Ogiltigt datumformat. Giltigt format: YYYY-MM-DD.");
                            return;
                        }
                    }
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        charging = args[++i];
                        // Kontrollera att laddningstiden är giltig
                        if (!charging.equals("2h") && !charging.equals("4h") && !charging.equals("8h")) {
                            System.out.println("Ogiltig laddningstid. Giltiga laddningstider: 2h, 4h, 8h.");
                            return;
                        }
                    }
                    break;
                case "--sorted":
                    isSorted = true;
                    break;
                case "--help":
                    //om --help används, anropas hjälpmenyn som visar användarinstruktioner
                    helpMenu();
                    return;
                default:
                    System.out.println("Ogiltigt argument: " + args[i]);
                    helpMenu();
                    return;
            }
        }
        //Kontrollerar att --zone flaggan har angetts
        if (zone == null) {
            System.out.println("Fel: --zone argument krävs.");
            helpMenu();
            return;
        }
        /*Om --date inte angavs, sätts date till dagens datum med LocalDate.now()
        toString() omvandlar LocalDate-objektet till en sträng i formatet "YYYY-MM-DD"*/
        if (date == null) {
            date = LocalDate.now().toString();
        }
        //LocalDate()objektet lagras i variabeln 'idag'
        LocalDate idag = LocalDate.parse(date);

        //Konvertera zon-strängen till enum-värde
        Prisklass prisklass = Prisklass.valueOf(zone);

        /*Skapar ett objekt av ElpriserAPI för att hämta elpriser
        och för att anropa metoder i ElpriserAPI-klassen, t.ex getPriser*/
        ElpriserAPI elpriserApi = new ElpriserAPI();

        //Skapar en lista för att kunna lagra elpriser
        List<Elpris> priser = new ArrayList<>();
        //Hämtar elpriser för det angivna datumet och zonen
        List<Elpris> priserIdag = elpriserApi.getPriser(idag.toString(), prisklass);
        //Lägger till dagens priser i listan 'priser'
        priser.addAll(priserIdag);

        // Om klockan är efter 13:00, försök hämta morgondagens priser också
        LocalTime nu = LocalTime.now();
        if (nu.isAfter(LocalTime.of(13, 0))) {
            // försök att hämta för morgondagen också
            LocalDate imorgon = idag.plusDays(1);
            //Hämtar elpriser för morgondagen och lägger till dem i listan 'priser' om de finns
            List<Elpris> priserImorgon = elpriserApi.getPriser(imorgon.toString(), prisklass);
            if (!priserImorgon.isEmpty()) {
                priser.addAll(priserImorgon);
            } else {
                System.out.println("Morgondagens priser är inte tillgängliga. Visar endast dagens priser.");
            }
        }
        //Kontrollerar om listan 'priser' är tom
        if (priser.isEmpty()) {
            System.out.println("Inga priser tillgängliga för valt datum och zon.");
            return;
        }
        // Formaterare för att visa priser med två decimaler och svensk lokalisering
        NumberFormat svenskFormat = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        svenskFormat.setMinimumFractionDigits(2);
        svenskFormat.setMaximumFractionDigits(2);

        //Anropar metoden beräknaMedelPris för att få medelpriset
        double medelPris = beräknaMedelPris(priser);
        // Formatera medelpriset till två decimaler
        String formateratMedelPris = svenskFormat.format(medelPris);
        //Skriver ut medelpriset
        System.out.println("Medelpris: " + formateratMedelPris + " öre");

        // Välj rätt metod beroende på om pris är per kvart eller per timme
        // Kontrollera om data är kvartstimme-data
        boolean isPriserPerKvart = isKvartstimmeData(priser);
        // Beräkna min och max med rätt metod
        MinMaxResultatMedSnitt resultat;
        //Om det är kvartstimme-data, använd metoden för att beräkna lägsta och högsta timmedelpris
        //Annars använd metoden för att beräkna min och max för timpris
        if (isPriserPerKvart) {
            resultat = beräknaLägstaOchHögstaTimmedelPris(priser);
        } else {
            resultat = beräknaMinMaxFörTimpris(priser);
        }
        // Formatera och skriv ut resultatet
        String utskrift = formatMinMaxResultat(resultat, svenskFormat);
        System.out.println(utskrift);

        //om --charging flaggan användes, beräkna bästa laddningstid
        if (charging != null) {
            //omvandlar "2h", "4h", "8h" till ett heltal som representerar antal timmar att ladda
            int hoursToCharge = Integer.parseInt(charging.replace("h", ""));
            //Beräknar längden på laddningsfönstret
            int längdLaddningsFönster;
            if (isPriserPerKvart) {
                längdLaddningsFönster = hoursToCharge * 4;
            } else {
                längdLaddningsFönster = hoursToCharge;
            }
            //Hittar det billigaste fönstret för laddning
            Elpris startBilligasteFönster = null;
            double lägstaFönsterKostnad = Double.MAX_VALUE;
            //Går igenom alla möjliga startpunkter för laddningsfönstret
            // och beräknar kostnaden för varje fönster
            for (int i = 0; i <= priser.size() - längdLaddningsFönster; i++) {
                double sum = 0;
                for (int j = 0; j < längdLaddningsFönster; j++) {
                    sum += priser.get(i + j).sekPerKWh();
                }
                //Sparar den billigaste fönsterkostnaden och dess starttid
                if (sum < lägstaFönsterKostnad) {
                    lägstaFönsterKostnad = sum;
                    startBilligasteFönster = priser.get(i);
                }
            }
            //Skriver ut resultatet för det billigaste laddningsfönstret
            if (startBilligasteFönster != null) {
                //Hämtar starttiden för det billigaste fönstret
                LocalTime startTid = startBilligasteFönster.timeStart().toLocalTime();
                //Beräknar medelpriset för det billigaste fönstret i öre
                double medelFörFönster = (lägstaFönsterKostnad / längdLaddningsFönster) * (100.0);
                //Skriver ut laddningsförslaget
                skrivUtLaddningsFörslag(startTid, hoursToCharge, medelFörFönster);
            } else {
                System.out.println("Ingen lämplig laddningsperiod hittades.");
            }
        }

        // Om --sorted flaggan användes, anropas sorteringsmetoden och skriv ut sorterade priser-metoden
        if (isSorted) {
            sortPricesDescending(priser);
            printSortedPrices(priser, svenskFormat);
            return;
        }
        //Går igenom listan med priser och skriver ut varje pris med start- och sluttid
        for (int i = 0; i < priser.size(); i++) {
            Elpris pris = priser.get(i);
            String formateratPris = svenskFormat.format(prisIOre(pris));
            System.out.println("Tid: " + pris.timeStart().toLocalTime() + "-" + pris.timeEnd().toLocalTime()
                    + " Pris: " + formateratPris + " öre/kWh");
        }
    }
    // Metod för att kontrollera om listan 'priser' består av kvartstimme-data
    public static boolean isKvartstimmeData(List<Elpris> priser) {
        // Antar att alla priser har samma varaktighet, kolla bara första
        Elpris elpris = priser.get(0);
        //Räknar antal minuter mellan start- och sluttid för priset
        int varaktighetIMinuter = (int) Duration.between(elpris.timeStart().toLocalTime(), elpris.timeEnd().toLocalTime()).toMinutes();
        //returnerar true om varaktigheten är 15 minuter
        return varaktighetIMinuter == 15;
    }

    // Metod som tar emot en lista med Elpris-objekt och beräknar det lägsta och högsta timpriset
    public static MinMaxResultatMedSnitt beräknaMinMaxFörTimpris(List<Elpris> priser) {
        // Initialiserar variabler för att hålla reda på lägsta och högsta pris
        double lägsta = Double.MAX_VALUE, högsta = Double.MIN_VALUE;
        //lägstaPris och högstaPris håller koll på vilken timme dessa priser inträffar
        Elpris lägstaPris = null, högstaPris = null;
        // Loopar igenom alla priser i listan
        for (int i = 0; i < priser.size(); i++) {
            Elpris pris = priser.get(i);
            // Hämtar priset i sek/kWh
            double timPris = pris.sekPerKWh();
            // Uppdaterar lägsta och högsta pris samt deras respektive Elpris-objekt
            if (timPris < lägsta) {
                lägsta = timPris;
                lägstaPris = pris;
            }
            if (timPris > högsta) {
                högsta = timPris;
                högstaPris = pris;
            }
        }
        /*Returnerar ett MinMaxResultatMedSnitt record med resultaten
        lägstaPris används både som start- och slut-tidpunkt för det lägsta priset
        högstaPris används både som start- och slut-tidpunkt för det högsta priset
        'lägsta' är värdet på det lägsta priset och 'högsta' är värdet på det högsta priset*/
        return new MinMaxResultatMedSnitt(lägstaPris, lägstaPris, lägsta, högstaPris, högstaPris, högsta);
    }

    //Metod som tar en lista av Elpris-objekt (kvarts-timmedata) och beräknar den timme som har högst/lägst medelpris
    public static MinMaxResultatMedSnitt beräknaLägstaOchHögstaTimmedelPris(List<Elpris> priser) {
        //Kontrollerar att antal priser i listan är jämnt delbart med 4
        if (priser.size() % 4 != 0) {
            throw new IllegalArgumentException("Antalet priser är inte delbart med 4. Kan inte gruppera i timmar.");
        }
        //variabler för att hålla reda på lägsta och högsta snittpriset av timmen
        double lägstaSnitt = Double.MAX_VALUE;
        double högstaSnitt = Double.MIN_VALUE;
        Elpris lägstaStart = null, lägstaEnd = null;
        Elpris högstaStart = null, högstaEnd = null;

        //Yttre loopen går igenom listan med priser i steg om 4 (en timme består av 4 kvartstimmar)
        //inre loopen summerar priset för de 4 kvartstimmarna
        for (int i = 0; i < priser.size(); i += 4) {
            double sum = 0;
            for (int j = 0; j < 4; j++) {
                sum += priser.get(i + j).sekPerKWh();
            }
            //genomsnittspriset för timmen beräknas genom att dela summan med 4
            double snitt = sum / 4.0;

            //uppdaterar lägsta och högsta snittpris samt deras respektive start- och sluttider
            if (snitt < lägstaSnitt) {
                lägstaSnitt = snitt;
                lägstaStart = priser.get(i);
                lägstaEnd = priser.get(i + 3);
            }
            if (snitt > högstaSnitt) {
                högstaSnitt = snitt;
                högstaStart = priser.get(i);
                högstaEnd = priser.get(i + 3);
            }
        }
        /*Returnerar ett MinMaxResultatMedSnitt record som innehåller start- och sluttid för
        både billigaste och dyraste timmen, samt deras respektive genomsnittsvärden*/
        return new MinMaxResultatMedSnitt(lägstaStart, lägstaEnd, lägstaSnitt,
                högstaStart, högstaEnd, högstaSnitt);
    }

    //Metod som formaterar och returnerar en sträng med resultat från MinMaxResultatMedSnitt
    public static String formatMinMaxResultat(MinMaxResultatMedSnitt resultat, NumberFormat format) {
        String lägstaPris = format.format(resultat.lägstaSnitt() * 100);
        String högstaPris = format.format(resultat.högstaSnitt() * 100);
        //Formaterar start- och sluttid för den billigaste timmen till "HH" format (bara timmar)
        String lägstaTid = resultat.lägstaStart().timeStart().toLocalTime().format(DateTimeFormatter.ofPattern("HH")) + "-" +
                resultat.lägstaSlut().timeEnd().toLocalTime().format(DateTimeFormatter.ofPattern("HH"));
        //Formaterar start- och sluttid för den dyraste timmen till "HH" format (bara timmar)
        String högstaTid = resultat.högstaStart().timeStart().toLocalTime().format(DateTimeFormatter.ofPattern("HH")) + "-" +
                resultat.högstaSlut().timeEnd().toLocalTime().format(DateTimeFormatter.ofPattern("HH"));

        return "Lägsta pris: " + lägstaPris + " öre/kWh (" + lägstaTid + ")\n" +
                "Högsta pris: " + högstaPris + " öre/kWh (" + högstaTid + ")";
    }
    //Metod som beräknar medelpriset från en lista av Elpris-objekt
    private static double beräknaMedelPris(List<Elpris> priser) {
        double total = 0.0;
        for (int i = 0; i < priser.size(); i++) {
            Elpris timPris = priser.get(i);
            total += timPris.sekPerKWh();
        }
        return (total / priser.size()) * 100;
    }
    //Metod som sorterar en lista av Elpris-objekt i fallande ordning
    public static void sortPricesDescending(List<Elpris> priser) {
        /*Yttre loop som går igenom listan från första till näst sista elementet och "bubblar"
        fram det största priset vid varje iteration
        Inre loop som jämför intilliggande element och byter plats om de är i fel ordning*/
        for (int i = 0; i < priser.size() - 1; i++) {
            for (int j = 0; j < priser.size() - 1 - i; j++) {
                if (priser.get(j).sekPerKWh() < priser.get(j + 1).sekPerKWh()) {
                    Elpris temp = priser.get(j);
                    priser.set(j, priser.get(j + 1));
                    priser.set(j + 1, temp);
                }
            }
        }
    }
    //Metod som skriver ut en lista av Elpris-objekt i sorterad ordning
    public static void printSortedPrices(List<Elpris> priser, NumberFormat format) {
        for (int i = 0; i < priser.size(); i++) {
            Elpris pris = priser.get(i);
            String start = pris.timeStart().toLocalTime().format(DateTimeFormatter.ofPattern("HH"));
            String end = pris.timeEnd().toLocalTime().format(DateTimeFormatter.ofPattern("HH"));
            String formateratPris = format.format(prisIOre(pris));
            System.out.println(start + "-" + end + " " + formateratPris + " öre");
        }
    }
    //Metod som konverterar pris från sek/kWh till öre/kWh
    public static double prisIOre(Elpris pris) {
        return pris.sekPerKWh() * 100;
    }
    //Metod som formaterar ett pris i öre till en sträng med två decimaler och svensk lokalisering
    private static String formatOre(double sek) {
        NumberFormat priceFormatter = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        priceFormatter.setMinimumFractionDigits(2);
        priceFormatter.setMaximumFractionDigits(2);
        return priceFormatter.format(sek);
    }
    //Metod som skriver ut laddningsförslag med starttid, antal timmar och medelpris
    private static void skrivUtLaddningsFörslag(LocalTime startTid, int antalTimmar, double medelpris) {
        String startStr = startTid.format(DateTimeFormatter.ofPattern("HH:mm"));
        String medelStr = formatOre(medelpris);
        System.out.println("Påbörja laddning kl " + startStr);
        System.out.println("Medelpris för fönster: " + medelStr + " öre");
    }
    //Metod som skriver ut en hjälpmeny med instruktioner för hur programmet används
    public static void helpMenu() {
        System.out.println("""
                ===================== USAGE =====================
                
                Programmet visar elpriser för valt elområde och datum.
                
                Tillgängliga flaggor:
                
                --zone SE1|SE2|SE3|SE4
                    Obligatorisk. Anger vilket elprisområde du vill se.
                
                --date YYYY-MM-DD
                    Valfri. Anger vilket datum du vill se elpriser för.
                    Om inget datum anges, används dagens datum.
                
                --sorted
                    Valfri. Visar priserna i fallande ordning.
                
                --charging 2h|4h|8h
                    Valfri. Visar när det är mest fördelaktigt att ladda under vald period.
                
                --help
                    Visar denna hjälpmeny.
                
                Exempel på användning:
                java Main --zone SE3 --date 2025-09-04 --sorted
                
                ==================================================
                """);
    }
}