package com.fewstreet.remotefakegps;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by peter on 7/21/16.
 */
public class GPSHTTPD extends NanoHTTPD {
    public static final String NEW_LOCATION_CHOSEN_ACTION = "new_location_chosen";
    private Context ctx;
    public GPSHTTPD(int port, Context ctx) {
        super(port);
        this.ctx = ctx;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String html = null;
        InputStream is = null;
        Map<String, String> parms = session.getParms();
        if(parms.get("lat") != null) {
            float lat = new Float(parms.get("lat"));
            float lng = new Float(parms.get("lng"));

            Intent intent = new Intent(NEW_LOCATION_CHOSEN_ACTION);
            intent.putExtra("lat", lat);
            intent.putExtra("lng", lng);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);

            return newFixedLengthResponse("ok");
        }
        try {
            is = ctx.getAssets().open("index.html");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        byte[] b;
        try {
            b = new byte[is.available()];
            is.read(b);
            html = new String(b);
        } catch (IOException e) { // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return newFixedLengthResponse(html);
    }
}