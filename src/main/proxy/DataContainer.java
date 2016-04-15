package main.proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;

/**
 * Created by phijo967 on 2016-04-15.
 */
public class DataContainer {
    //If redirect is true, server should ignore data and redirect
    public boolean redirect = false;
    //The data of the response
    public ByteArrayOutputStream responseData = null;
}
