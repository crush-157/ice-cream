package com.example.fn;

import com.oracle.emeatechnology.icecream.*;

public class KioskOrder {

    public Double handleRequest(OrderMessage input) {
      Shop s = new Kiosk();
      return s.order(input.flavour, input.quantity);
    }
}
