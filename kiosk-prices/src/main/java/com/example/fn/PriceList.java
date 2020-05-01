package com.example.fn;

import com.oracle.emeatechnology.icecream.*;

public class PriceList {

    public String handleRequest(String input) {
        Shop s = new Kiosk();
        return s.getPriceListAsString();
    }
}
