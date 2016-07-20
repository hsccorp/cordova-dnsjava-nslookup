# cordova-dnsjava-nslookup

This plugin utilizes dnsjava to do DNS lookups. 

1)It support -type lookup(eg. "ANY", 'AAAA', 'MX' etc).
2)It supports reverse DNS lookup for both ipv6 and ipv4 address types.
3)It supports change in default timeout interval by using the 'timeout' parameter (in seconds).
4)By default, it will query the same DNS the system is configured to use for all network operations. You can specify a custom DNS to query using the "dns" parameter in query.


For reverse DNS lookup pass the "reverse" parameter as  'ipv6' or 'ipv4' in request.
Query is passed as array of json objects.

## Installation

> cordova plugin add cordova-plugin-nslookup-master  

## Usage

          var query =  [{query: "GOOGLE.COM"},
          {query: "yahoo.com",	type: "ANY" },
          {query: '192.168.121.51',reverse: "ipv4"}, 
          {query: '2001:41d0:8:e8ad::1',reverse: 'ipv6',dns: "192.168.0.37",timeout: '40'},
          {query: 'google.com',type: 'AAAA',dns: "192.168.0.37",	timeout: '15' },
          {query: 'www.tiste.org',	type: 'CNAME'}, 
          {query: 'google.com',type: 'MX' }, 
          {query: 'google.com',	type: 'NS'}, 
          {query: '192.174.198.91.in-addr.arpa',type: 'PTR'}, 
          {query: 'google.com',type: 'SOA'}, 
          {query: '_xmpp-server._tcp.gmail.com',type: 'SRV'},
          {query: 'google.com',type: 'TXT'}, 
          {query: 'google.com',type: 'AAAA',dns: "192.168.0.37",timeout: '15'}];
         
          function success(results) {
               console.log(JSON.stringify(results));
            };
           function err(e) {
                  console.log(JSON.stringify(results));
            };
            n = new NsLookup();
            n.nslookup(query, success, err);

