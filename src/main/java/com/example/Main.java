package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        //todo: gör en metod som sorterar dagens priser i fallande ordning

        //todo:jämföra data med  --zone SE1|SE2|SE3|SE4 (required)
        //todo jämföra data med  --date YYYY-MM-DD (optional, defaults to current date)
        //todo jämföra data med  --sorted (optional, to display prices in descending order)
        //todo jämföra data med  --charging 2h|4h|8h (optional, to find optimal charging windows)
        //todo jämföra data med  --help (optional, to display usage information)

        System.out.println("Välkommen till Elpriskollen");
        helpMenu();

        //Sätter startvärden för variablerna
        String zone = null;
        String date = null;
        boolean isSorted = false;
        String charging = null;
        boolean isHelp = false;


        //En for-loop som går igenom varje element i args[]. args[] är en lista av argument från kommandoraden.
        //Loopen läser in vad användaren skriver in i terminalen
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone":
                    //om 'i' plus det som kommer efter är mindre än args längd
                    //så tilldelas zonOf värdet som kommer efter "--zone"
                    if (i + 1 < args.length) {
                        zone = args[++i];
                    }
                    System.out.println("Zone of: " + zone);
                    break;
                case "--date":
                    if (i + 1 < args.length) {
                        date = args[++i];
                    }
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        charging = args[++i];
                    }
                    break;
                case "--sorted":
                    sorting();
                    isSorted = true;
                    break;
                case "--help":
                    helpMenu();
                    isHelp = true;
                    break;

            }

        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        ElpriserAPI elpriser = new ElpriserAPI();
        //Hämtar elpriser för idag i prisområde SE3 och sparar i listan priserIdag
        //todo: ändra så att SE3 är dynamiskt beroende på vad användaren matar in
        List<ElpriserAPI.Elpris> priserIdag = elpriser.getPriser(today, ElpriserAPI.Prisklass.SE3);

// Always fetch today's prices
        System.out.println("Elpriser för idag i SE3:");
        //'pris' är variabelnamnet som vi använder för att referera till det aktuella objektet i loopen.
        //se det som ett "tillfälligt namn" för varje elpris.
        //För varje Elpris-objekt pris i listan priserIdag
        for (ElpriserAPI.Elpris pris : priserIdag) {
            System.out.printf("Tid: %s - %.2f SEK/kWh%n",
                    pris.timeStart().toLocalTime(),
                    pris.sekPerKWh()
            );
        }

// If after 13:00, also fetch tomorrow's prices
        if (now.isAfter(LocalTime.of(13, 0)) || now.equals(LocalTime.of(13, 0))) {
            LocalDate tomorrow = today.plusDays(1);
            List<ElpriserAPI.Elpris> priserImorgon = elpriser.getPriser(tomorrow, ElpriserAPI.Prisklass.SE3);
            System.out.println("Elpriser för imorgon i SE3:");
            //Detta är det som skickas tillbaka från getPriser metoden
            //System.out.println(priserImorgon.get(0));
            for (ElpriserAPI.Elpris pris : priserImorgon) {
                /*System.out.printf("Tid: %s - %.2f SEK/kWh%n",
                        pris.timeStart().toLocalTime(),
                        pris.sekPerKWh()
                );*/
            }
        }
        double sekPerKWh = 0;
        double sum = 0;
        double average = 0;
        //Går igenom listan priserIdag med en for-loop
        for (int i = 0; i < priserIdag.size(); i++) {
                sekPerKWh = priserIdag.get(i).sekPerKWh();
                sum = sum + sekPerKWh;
                average = sum / priserIdag.size();
        }
        System.out.println("Medelpris för idag är: " + String.format("%.2f", average) + " SEK/kWh");






    }

    public static void sorting(){
        //Här vill jag sortera dagens priser i fallande ordning


    }

    public static void helpMenu() {
        System.out.println("""
        ===================== HJÄLPMENY =====================
        
        Mata in --zone för att ange elprisområde

        Mata in --date för att se dagens prisvisning

        Mata in --sorted för att se priserna i fallande ordning

        Mata in --charging för att hitta optimala laddningstider för vald period 2h|4h|8h
        
        Mata in --help för att visa denna hjälpmeny
        =====================================================
        """);


        /*ElpriserAPI elpriserAPI = new ElpriserAPI();
        var prices = elpriserAPI.getPriser(LocalDate.now(), ElpriserAPI.Prisklass.SE3);
        System.out.println(prices.get(0).timeStart());*/
        /*System.out.println("Tid: " + elpriser.timeStart().toLocalTime() +"-"+ elpriser.timeEnd().toLocalTime()
                + " Pris: " + formateratPris + " öre/KWh");*/






    }
}
