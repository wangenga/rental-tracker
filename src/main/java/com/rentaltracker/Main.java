package com.rentaltracker;


import com.rentaltracker.infrastructure.Database;

public class Main {
    public static void main(String[] args) {

        String path = args.length > 0 ? args[0] : "data/local.db";

        try(Database db = new Database(path)){
            System.out.println("Hello, Rental Tracker!");
        }
    }
}
