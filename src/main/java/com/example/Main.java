package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();
        var prices = elpriserAPI.getPriser(LocalDate.now(), ElpriserAPI.Prisklass.SE3);
        System.out.println(prices.get(0).timeStart());

        /*System.out.println("Tid: " + elpriser.timeStart().toLocalTime() +"-"+ elpriser.timeEnd().toLocalTime()
                + " Pris: " + formateratPris + " öre/KWh");*/


    }
}
