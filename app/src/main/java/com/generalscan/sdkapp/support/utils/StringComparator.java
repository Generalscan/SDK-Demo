package com.generalscan.sdkapp.support.utils;

import java.util.Comparator;

public class StringComparator implements   Comparator {

    public StringComparator() {
    }


    public int compare(Object o1, Object o2) {

        String catalog0 = "";
        String catalog1 = "";

        if(o1!=null)
            catalog0=o1.toString().toLowerCase();
        if(o2!=null)
            catalog1=o2.toString().toLowerCase();
        return catalog0.compareTo(catalog1);
    }
}