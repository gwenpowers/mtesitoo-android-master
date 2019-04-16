package com.mtesitoo.helper;

import java.text.ParseException;

/**
 * Created by gwenp on 4/16/2019.
 *
 * Needed to wrap Parsing of Quantity into a try catch block otherwise app would crash if no quantity was entered.
 */

public class IntegerHelper {
    public static int parseInt(String quantity) {
        try {
            return Integer.parseInt(quantity);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
