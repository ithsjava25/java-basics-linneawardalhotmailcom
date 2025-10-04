package com.example;

import com.example.api.ElpriserAPI;
//Prisklass är en enum inuti ElpriserAPI klassen
import com.example.api.ElpriserAPI.Prisklass;
//Elpris är en record inuti ElpriserAPI klassen
import com.example.api.ElpriserAPI.Elpris;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {

        //todo: gör utskriften av hjälpmenyn mer användarvänlig
        //todo: Then, process the data: calculate charging windows if requested.

        //Om inga argument anges, skrivs hjälpmenyn ut och programmet avslutas
        if (args.length == 0) {
            helpMenu();
            return;
        }
        //Sätter startvärden för variablerna (flaggorna)
        String zone = null;
        String date = null;
        String charging = null;
        boolean isSorted = false;
        boolean isHelp = false;
        boolean isDateGiven = false;

        //En for-loop som går igenom varje element i args[]
        //Loopen läser in vad användaren skriver in i terminalen
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone":
                    //Om det finns ett argument efter flaggan --zone
                    if (i + 1 < args.length) {
                        //så ökas i (flyttar till nästa argument) och värdet tilldelas variabeln zone.
                        zone = args[++i];
                        if (!zone.equals("SE1") && !zone.equals("SE2") && !zone.equals("SE3") && !zone.equals("SE4")) {
                            System.out.println("Ogiltig zon. Giltiga zoner: SE1, SE2, SE3, SE4");
                            return;
                        }
                    }
                    break;
                case "--date":
                    //Om det finns ett argument efter flaggan --date
                    if (i + 1 < args.length) {
                        //så ökar i (flyttar till nästa argument) och värdet tilldelas variabeln dateInput.
                        String dateInput = args[++i];
                        try {
                            //försöker tolka dateInput som ett datum i formatet ÅÅÅÅ-MM-DD.
                            LocalDate parsedDate = LocalDate.parse(dateInput, DateTimeFormatter.ISO_LOCAL_DATE);
                            //Om tolkningen lyckas, tilldelas det formaterade datumet variabeln date och isDateGiven sätts till true
                            date = parsedDate.toString();
                            isDateGiven = true;
                            //Om indatan har fel format, fångas undantaget och ett felmeddelande skrivs ut
                        } catch (DateTimeParseException e) {
                            System.out.println("Ogiltigt datumformat. Giltigt format: YYYY-MM-DD.");
                            return;
                        }
                    }
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        charging = args[++i];
                        //Kontrollerar att indatan (laddningstiden) är en av de tillåtna värdena
                        if (!charging.equals("2h") && !charging.equals("4h") && !charging.equals("8h")) {
                            System.out.println("Ogiltig laddningstid. Giltiga laddningstider: 2h, 4h, 8h.");
                            return;
                        }
                    }
                    break;
                case "--sorted":
                    //todo: Här ska anrop till sorteringsmetoden göras
                    isSorted = true;
                    break;
                case "--help":
                    helpMenu();
                    isHelp = true;
                    return;

                default:
                    //Om argumentet inte matchar någon av de kända flaggorna, skrivs ett felmeddelande ut och hjälpmenyn visas
                    System.out.println("Ogiltigt argument: " + args[i]);
                    helpMenu();
                    return;
            }
        }
        //Om zone är null, skrivs ett felmeddelande ut och hjälpmenyn visas
        if (zone == null) {
            System.out.println("Fel: --zone argument krävs.");
            helpMenu();
            return;
        }
        //Om användaren inte angivit ett datum, sätts date till dagens datum genom LocalDate.now()
        //.toString() omvandlar LocalDate-objektet till en sträng i formatet YYYY-MM-DD
        if (date == null) {
            date = LocalDate.now().toString();
        }
        //Det nya LocalDate-objektet lagras i variabeln idag, som senare används för att hämta dagenns elpriser
        LocalDate idag = LocalDate.parse(date);

        //Prisklass är en enum (en uppsättning fasta värden; SE1, SE2, SE3, SE4)
        //zone är en sträng som användaren anger via terminalen (t.ex "SE3")
        //Prisklass.valueOf(zone) omvandlar strängen till motsvarande enum-värde
        //Om användaren t.ex skrev "SE3", blir prisklass = Prisklass.SE3
        //
        Prisklass prisklass = Prisklass.valueOf(zone);

        //Skapar ett objekt av klassen ElpriserAPI och kan nu därmed anropa APIns metoder, t.ex getPriser(...)
        ElpriserAPI elpriser = new ElpriserAPI();

        //List<Elpris> är en lista som kan innehålla objekt av typen Elpris
        //Listan kommer att fyllas med priser för det valda datumet (och eventuellt nästa dag)
        List<Elpris> priser = new ArrayList<>();

        //Anropar metoden getPriser(...) från objektet elpriser
        //Metoden returnerar en lista med Elpris-objekt för det angivna datumet och zonen (prisklassen)
        //Det returnerade värdet sparas i listan priserIdag
        //toString konverterar datumobjektet 'idag' till en sträng i formatet "YYYY-MM-DD".
        List<Elpris> priserIdag = elpriser.getPriser(idag.toString(), prisklass);

        //For-loop som går igenom varje Elpris-objekt i listan priserIdag
        //Hämtar elpris-objektet vid index i från listan priserIdag och lagrar det i variabeln pris
        //Lägger till objektet pris (ett Elpris-objekt) i listan priser (huvudlistan som byggs upp).
        for (int i = 0; i < priserIdag.size(); i++) {
            Elpris timPris = priserIdag.get(i);
            priser.add(timPris);
        }



        // Om användaren har valt --sorted eller --charging, hämtas även priser för imorgon
        if (isSorted || charging != null) {
            LocalDate imorgon = idag.plusDays(1);
            List<Elpris> priserImorgon = elpriser.getPriser(imorgon.toString(), prisklass);

            //Lägg till varje pris från imorgon till huvudlistan 'priser'
            for (int i = 0; i < priserImorgon.size(); i++) {
                Elpris timPris = priserImorgon.get(i);
                priser.add(timPris);
            }
        }

        if (priser.isEmpty()) {
            System.out.println("Inga priser tillgängliga för valt datum och zon.");
            return;
        }

        //Skapar ett objekt som kan formatera tal och anger att vi vill använda svensk formatering
        NumberFormat svenskFormat = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        //Minst/max 2 decimaler skrivs ut
        svenskFormat.setMinimumFractionDigits(2);
        svenskFormat.setMaximumFractionDigits(2);

        //Variabel för att hålla reda på summan av alla elpriser
        double totalPris = 0.0;

        //Loop som går igenom alla Elpris-objekt i listan priser
        for (int i = 0; i < priser.size(); i++) {
            Elpris timPris = priser.get(i); // Hämta elprisobjektet på index i
            totalPris += timPris.sekPerKWh(); // Lägg till priset till totalPris
        }

        // Räkna och skriv ut medelpriset
        double medelPris = (totalPris / priser.size()) * 100; // Omvandla till öre/kWh
        String formateratMedelPris = svenskFormat.format(medelPris);
        System.out.println("Medelpris för perioden är: " + formateratMedelPris + " öre/kWh");

        Elpris lägstaPris = null;
        Elpris högstaPris = null;

        for (Elpris pris : priser) {
            if (lägstaPris == null || pris.sekPerKWh() < lägstaPris.sekPerKWh()) {
                lägstaPris = pris;
            }
            if (högstaPris == null || pris.sekPerKWh() > högstaPris.sekPerKWh()) {
                högstaPris = pris;
            }
        }

        String formateratLägsta = svenskFormat.format(prisIOre(lägstaPris));
        String formateratHögsta = svenskFormat.format(prisIOre(högstaPris));

        String lägstaTid = lägstaPris.timeStart().toLocalTime().format(DateTimeFormatter.ofPattern("HH")) + "-" +
                lägstaPris.timeEnd().toLocalTime().format(DateTimeFormatter.ofPattern("HH"));

        String högstaTid = högstaPris.timeStart().toLocalTime().format(DateTimeFormatter.ofPattern("HH")) + "-" +
                högstaPris.timeEnd().toLocalTime().format(DateTimeFormatter.ofPattern("HH"));

        System.out.println("Lägsta pris: " + formateratLägsta + " öre/kWh (" + lägstaTid + ")");
        System.out.println("Högsta pris: " + formateratHögsta + " öre/kWh (" + högstaTid + ")");


        //Sliding window algoritm för att hitta bästa laddningstid
        if (charging != null) {
            int hoursToCharge = Integer.parseInt(charging.replace("h", ""));

            ElpriserAPI.Elpris startHour = null;
            double lowestTotal = Double.MAX_VALUE;

            for (int i = 0; i <= priser.size() - hoursToCharge; i++) {
                double windowSum = 0.0;
                for (int j = 0; j < hoursToCharge; j++) {
                    windowSum += priser.get(i + j).sekPerKWh();
                }
                //Pick this window if it's cheaper than the current lowest
                if (windowSum < lowestTotal) {
                    lowestTotal = windowSum;
                    startHour = priser.get(i);
                }
            }
            //If a valid window was found, print the result
            if (startHour != null) {
                LocalTime startTime = startHour.timeStart().toLocalTime();
                LocalTime endTime = startTime.plusHours(hoursToCharge);
                double averageOre = (lowestTotal / hoursToCharge) * 100;
                String formattedAverage = svenskFormat.format(averageOre);

                skrivUtLaddningsFörslag(startTime, hoursToCharge, averageOre);

            } else {
                System.out.println("Ingen lämplig laddningsperiod hittades.");
            }
        }

        if (isSorted) {
            sortPricesDescending(priser);
            printSortedPrices(priser, svenskFormat); // Skriv ut i testformat
            return; // Hoppa över resten av utskriften om sorterad lista visades
        }

        //Skriver ut priserna
        for (ElpriserAPI.Elpris pris : priser) {
            String formateratPris = svenskFormat.format(prisIOre(pris));
            System.out.println("Tid: " + pris.timeStart().toLocalTime() + "-" + pris.timeEnd().toLocalTime()
                    + " Pris: " + formateratPris + " öre/kWh");
        }

    }

    //Metod som sorterar priser i fallande ordning
    public static void sortPricesDescending(List<ElpriserAPI.Elpris> prices) {
        for (int i = 0; i < prices.size() - 1; i++) {
            for (int j = 0; j < prices.size() - 1 - i; j++) {
                double currentPrice = prices.get(j).sekPerKWh();
                double nextPrice = prices.get(j + 1).sekPerKWh();

                //Om nuvarande pris är mindre än nästa pris, byt plats på dem
                if (currentPrice < nextPrice) {
                    ElpriserAPI.Elpris temporary = prices.get(j);
                    prices.set(j, prices.get(j + 1));
                    prices.set(j + 1, temporary);
                }
            }
        }
    }
    //Metod som omvandlar pris i SEK/kWh till öre/kWh
    public static double prisIOre(ElpriserAPI.Elpris pris) {
        return pris.sekPerKWh() * 100;
    }
    //Metod som skriver ut en sorterad lista av elpriser
    public static void printSortedPrices(List<ElpriserAPI.Elpris> priser, NumberFormat format) {
        for (int i = 0; i < priser.size(); i++) {
            ElpriserAPI.Elpris pris = priser.get(i);

            String start = pris.timeStart().toLocalTime().format(DateTimeFormatter.ofPattern("HH"));
            String end = pris.timeEnd().toLocalTime().format(DateTimeFormatter.ofPattern("HH"));

            double prisIore = prisIOre(pris);  // Använder den befintliga metoden
            String formateratPris = format.format(prisIore);

            System.out.println(start + "-" + end + " " + formateratPris + " öre");
        }
    }
    //Metod som formaterar ett double-värde till svensk valuta (öre) med två decimaler
    private static String formatOre(double sek) {
        NumberFormat svenskFormat = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        svenskFormat.setMinimumFractionDigits(2);
        svenskFormat.setMaximumFractionDigits(2);
        return svenskFormat.format(sek);
    }

    private static void skrivUtLaddningsFörslag(LocalTime startTid, int antalTimmar, double medelpris) {
        String startStr = startTid.format(DateTimeFormatter.ofPattern("HH:mm"));
        String slutStr = startTid.plusHours(antalTimmar).format(DateTimeFormatter.ofPattern("HH:mm"));
        String medelprisStr = formatOre(medelpris);

        if (antalTimmar == 2) {
            System.out.println("För att ladda elbilen billigast under " + antalTimmar + "h. " + " Påbörja laddning kl " + startStr + " till " + slutStr + ".");
        } else if (antalTimmar == 4 || antalTimmar == 8) {
            System.out.println("Påbörja laddning kl " + startStr + " och " + slutStr + ".");
        } else {
            throw new IllegalArgumentException("Endast laddningstider på 2h, 4h och 8h stöds. Angivet värde: " + antalTimmar + "h");
        }

        System.out.println("Medelpris för fönster: " + medelprisStr + " öre");
    }


    //Metod som skriver ut hjälpmenyn
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
