
package com.hsc.cordova.nslookup;


import org.xbill.DNS.*;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Nslookup extends CordovaPlugin {
    public static final String TAG = "DNSJavaNslookup";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, final JSONArray args,
            final CallbackContext callbackContext) throws JSONException {
        if ("getNsLookupInfo".equals(action)) {
            int pid = android.os.Process.myPid();
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    nsLookup(args, callbackContext);
                }
            });
            return true;

        }
        return false;
    }

    private void nsLookup(JSONArray args, CallbackContext callbackContext) {
        try {
            if (args != null && args.length() > 0) {
                JSONArray resultList = new JSONArray();
                int length = args.length();
                for (int index = 0; index < length; index++) {
                    String query = "";
                    String type = "";
                    String dns = "";
                    String reverse = "";
                    String timeout = "";
                    String protocol ="";
                    try {
                        JSONObject obj = args.optJSONObject(index);
                        query = obj.optString("query");
                        type = obj.optString("type");
                        dns = obj.optString("dns");
                        reverse = obj.optString("reverse");
                        if(reverse.toLowerCase().equals("ipv6")||reverse.toLowerCase().equals("ipv4")){
                            System.out.println("true"+query+"::"+reverse);
                            protocol = reverse;
                            reverse = "true";
                        }else{
                            protocol = reverse;
                            reverse = "false";
                        }
                        timeout = obj.optString("timeout");
                    } catch (Exception err) {
                        try {
                            query = args.getString(index);
                            type = "";
                        } catch (Exception e) {

                        }
                    }
                    JSONObject result = null;
                    if(reverse.equals("false")){
                        result = doNslookup(query, type, dns, reverse,
                                timeout);
                    }

                    else{
                        System.out.println("protocol"+protocol);
                        if(protocol.toLowerCase().equals("ipv6")){
                            result = reverseDnsIpv6(query,dns,timeout);
                        }else{
                            result = reverseDns(query,dns,timeout);
                        }
                    }
                    resultList.put(result);
                }
                callbackContext.success(resultList);
            } else {
                callbackContext.error("Error occurred");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }



    public  JSONObject reverseDns(String hostIp, String dns, String timeout) throws IOException {
        JSONObject r = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject responseJson = new JSONObject();
        try{
            request.put("query",hostIp);
            request.put("dns",dns);
            request.put("protocol","ipv4");
            request.put("type","ANY");
            // r.put("query",hostIp);
            JSONArray recordArray = new JSONArray();
            Record opt = null;
            Resolver res =null;
            if(dns!=""){
                String[] dnslist = new String[1];
                dnslist[0] = dns;
                res = new ExtendedResolver(dnslist);
            }
            else{
                res = new ExtendedResolver(); 
                System.out.println("???");
            }
            if(timeout!=""){
                request.put("timeout",Integer.parseInt(timeout));
                res.setTimeout(Integer.parseInt(timeout));
            }
            Name name = ReverseMap.fromAddress(hostIp);
            int type = Type.ANY;
            int dclass = DClass.IN;
            Record rec = Record.newRecord(name, type, dclass,0);
            Message query = Message.newQuery(rec); 
            long before = System.currentTimeMillis();
            Message response = res.send(query);
            Record[] answers = response.getSectionArray(Section.ANSWER);
            if (answers.length == 0) {
                responseJson.put("status", "failed");
                r.put("request",request);
                r.put("response",responseJson);
                return r;
            }
            else{
                long after = System.currentTimeMillis();
                for(int i =0; i< answers.length;i++){
                    JSONObject obj = new JSONObject();
                    Record s = answers[i];
                    obj.put("type",s.getType());
                    obj.put("TTL",s.getTTL());
                    obj.put("address", s.rdataToString());
                    obj.put("lookupTime", (after-before));
                    responseJson.put("status", "success");
                    recordArray.put(obj);
                }
                responseJson.put("result", recordArray);
                r.put("request",request);
                r.put("response",responseJson);
            } 
        }catch (Exception e) {
            // TODO: handle exception
        }
        return r;
    }


    public  JSONObject reverseDnsIpv6(String hostIp, String dns, String timeout) throws IOException {
        JSONObject r = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject responseJson = new JSONObject();
        try{
            request.put("query",hostIp);
            request.put("dns",dns);
            request.put("protocol","ipv6");
            request.put("type","ANY");
            JSONArray recordArray = new JSONArray();
            Record opt = null;
            Resolver res =null;
            if(dns!=""){
                String[] dnslist = new String[1];
                dnslist[0] = dns;
                res = new ExtendedResolver(dnslist);
            }
            else{
                res = new ExtendedResolver(); 
            }
            if(timeout!=""){
                res.setTimeout(Integer.parseInt(timeout));
                request.put("timeout",Integer.parseInt(timeout));
            }
            Name name = ReverseMap.fromAddress(Inet6Address.getByName(hostIp));
            int type = Type.ANY;
            int dclass = DClass.IN;
            Record rec = Record.newRecord(name, type, dclass,0);
            Message query = Message.newQuery(rec); 
            long before = System.currentTimeMillis();
            Message response = res.send(query);
            Record[] answers = response.getSectionArray(Section.ANSWER);
            if (answers.length == 0) {
                responseJson.put("status", "failed");
                r.put("request",request);
                r.put("response",responseJson);
                return r;
            }
            else{
                long after = System.currentTimeMillis();
                for(int i =0; i< answers.length;i++){
                    JSONObject obj = new JSONObject();
                    Record s = answers[i];
                    obj.put("TTL",s.getTTL());
                    obj.put("address", s.rdataToString());
                    obj.put("lookupTime", (after-before));
                    responseJson.put("status", "success");
                    recordArray.put(obj);
                }
                responseJson.put("result", recordArray);
            }
            r.put("request",request);
            r.put("response",responseJson);
        }catch(Exception e){

        }
        return r;
    }



    private JSONObject doNslookup(String query, String type, String dns,
            String reverse, String timeout) {
        Cache c = new Cache();
        c.setMaxCache(0);
        c.setMaxNCache(0);
        System.out.println("doNslookup \n");
        System.out.println(query + "\n");
        JSONObject r = new JSONObject();
        JSONArray recordArray = new JSONArray();
        JSONArray requestArray = new JSONArray();
        JSONObject request = new JSONObject();
        JSONObject responseJson = new JSONObject();
        try{
            request.put("query", query);
            request.put("dns",dns);
            request.put("protocol","ipv4");
            request.put("type",type);
            request.put("timeout",timeout);
        }catch(Exception e){

        }

        if (type == "" && false == Boolean.parseBoolean(reverse)) {
            try {
                Resolver resolver = null;
                Name name = new Name(query.trim());
                Lookup lookup = new Lookup(name);
                lookup.setCache(c);
                if (dns != "") {
                    String[] dnslist = new String[1];
                    dnslist[0] = dns;
                    resolver = new ExtendedResolver(dnslist);
                    lookup.setResolver(resolver);
                } else {
                    resolver = new ExtendedResolver();
                }
                if (timeout != "") {
                    resolver.setTimeout(Integer.parseInt(timeout));
                }
                long before = System.currentTimeMillis();
                lookup.run();

                if (lookup.getResult() == Lookup.SUCCESSFUL) {
                    long after = System.currentTimeMillis();
                    for (int i = 0; i < lookup.getAnswers().length; i++) {
                        JSONObject obj = new JSONObject();
                        Record v = lookup.getAnswers()[i];
                        obj.put("TTL", v.getTTL());
                        obj.put("type", lookup.getAnswers()[i].toString()
                                .replaceAll("\\s+", " ").split(" ")[3]);
                        obj.put("address", v.rdataToString());
                        obj.put("lookupTime", (after-before));
                        responseJson.put("status", "success");
                        recordArray.put(obj);
                    }
                } else {
                    responseJson.put("status", "failed");
                }
                responseJson.put("result", recordArray);
                r.put("request",request);
                r.put("response",responseJson);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(false == Boolean.parseBoolean(reverse)){
            try {
                Resolver resolver = null;
                Name name = new Name(query.trim());
                if (dns != "") {
                    String[] dnslist = new String[1];
                    dnslist[0] = dns;
                    resolver = new ExtendedResolver(dnslist);
                } else {
                    resolver = new ExtendedResolver();
                }
                if (timeout != "") {
                    resolver.setTimeout(Integer.parseInt(timeout));
                }

                if (type.equals("A")) {
                    Lookup lookup = new Lookup(name,Type.A);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            Record v = lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);

                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }
                    responseJson.put("result", recordArray);
                    r.put("request",request);
                    r.put("response",responseJson);

                } else if (type.equals("AAAA")) {
                    Lookup lookup = new Lookup(name,Type.AAAA);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            Record v = lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);
                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }
                } 

                else if (type.equals("ANY")) {
                    Lookup lookup = new Lookup(name,Type.ANY);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            Record v = lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);

                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }


                }
                else if (type.equals("CNAME")) {
                    Lookup lookup = new Lookup(name,Type.CNAME);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL){
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            CNAMERecord v = (CNAMERecord) lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("target", v.getTarget());
                            obj.put("alias", v.getAlias());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);

                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }



                } else if (type.equals("MX")) {
                    Lookup lookup = new Lookup(name,Type.MX);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            MXRecord v = (MXRecord)lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("target", v.getTarget());
                            obj.put("priority", v.getPriority());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);

                        }}
                    else{
                        responseJson.put("status", "failed");
                    }

                } else if (type.equals("NS")) {
                    Lookup lookup = new Lookup(name,Type.NS);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            NSRecord v = (NSRecord)lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("target", v.getTarget());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);
                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }

                } else if (type.equals("PTR")) {
                    Lookup lookup = new Lookup(name,Type.PTR);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            PTRRecord v =(PTRRecord) lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("target", v.getTarget());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);
                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }

                } else if (type.equals("SOA")) {
                    Lookup lookup = new Lookup(name,Type.SOA);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            SOARecord v =(SOARecord) lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("host", v.getHost());
                            obj.put("admin", v.getAdmin());
                            obj.put("serial", v.getSerial());
                            obj.put("refresh", v.getRefresh());
                            obj.put("retry", v.getRetry());
                            obj.put("expire", v.getExpire());
                            obj.put("minimum", v.getMinimum());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);
                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }

                } else if (type.equals("SPF")) {
                    Lookup lookup = new Lookup(name,Type.SPF);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            SPFRecord v =(SPFRecord) lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("strings", v.getStrings());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);
                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }

                } else if (type.equals("SRV")) {

                    Lookup lookup = new Lookup(name,Type.SRV);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            SRVRecord v =(SRVRecord) lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("target", v.getTarget());
                            obj.put("port", v.getPort());
                            obj.put("priority", v.getPriority());
                            obj.put("weight", v.getWeight());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);
                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }

                } else if (type.equals("TXT")) {

                    Lookup lookup = new Lookup(name,Type.TXT);
                    lookup.setCache(c);
                    lookup.setResolver(resolver);
                    long before = System.currentTimeMillis();
                    lookup.run();
                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        long after = System.currentTimeMillis();
                        for (int i = 0; i < lookup.getAnswers().length; i++) {
                            JSONObject obj = new JSONObject();
                            TXTRecord v =(TXTRecord) lookup.getAnswers()[i];
                            obj.put("TTL", v.getTTL());
                            obj.put("type", lookup.getAnswers()[i].toString()
                                    .replaceAll("\\s+", " ").split(" ")[3]);
                            obj.put("address", v.rdataToString());
                            obj.put("strings", v.getStrings());
                            obj.put("lookupTime", (after-before));
                            responseJson.put("status", "success");
                            recordArray.put(obj);
                        }
                    }
                    else{
                        responseJson.put("status", "failed");
                    }
                }
                responseJson.put("result", recordArray);
                r.put("request",request);
                r.put("response",responseJson);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return r;
    }
}

