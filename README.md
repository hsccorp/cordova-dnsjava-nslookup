# cordova-dnsjava-nslookup

This plugin utilizes dnsjava to do DNS lookups. 

> - It support -type lookup(eg. "ANY", 'AAAA', 'MX' etc).<br>
> - It supports reverse DNS lookup for both ipv6 and ipv4 address types.<br>
> - It supports change in default timeout interval by using the 'timeout' parameter (in seconds).<br>
> - By default, it will query the same DNS the system is configured to use for all network operations. You can specify a custom DNS to query using the "dns" parameter in query.<br><br>
For reverse DNS lookup pass the "reverse" parameter as  'ipv6' or 'ipv4' in request.<br>
Query is passed as array of json objects.

## Installation

> cordova plugin add cordova-dnsjava-nslookup-master  

## Usage
```js

          var query =  [{query: "google.com"},
          {query: "yahoo.com",type: "ANY" },
          {query: 'x.x.x.x',reverse: "ipv4"}, 
          {query: 'xxxx:xxxx:x:xxxx::x',reverse: 'ipv6',dns: "x.x.x.x",timeout: '40'},
          {query: 'google.com',type: 'AAAA',dns: "192.x.x.x",timeout: '15' },
          {query: 'www.tiste.org',type: 'CNAME'}, 
          {query: 'google.com',type: 'MX' }, 
          {query: 'google.com',type: 'NS'}, 
          {query: '192.x.x.x.in-addr.arpa',type: 'PTR'}, 
          {query: 'google.com',type: 'SOA'}, 
          {query: '_xmpp-server._tcp.gmail.com',type: 'SRV'},
          {query: 'google.com',type: 'TXT'}, 
          {query: 'google.com',type: 'AAAA',dns: "192.x.x.x",timeout: '15'}];
         
          function success(results) {
               console.log(JSON.stringify(results));
          };
          function err(e) {
                  console.log(JSON.stringify(e));
          };
           var n = new NsLookup();
          n.nslookup(query, success, err);
```
## Response

Response is an array of json objects containing both request and response parameters.

```json

[{
	"request": {
		"query": "www.google.com",
		"dns": "",
		"protocol": "ipv4",
		"type": "A",
		"timeout": ""
	},
	"response": {
		"status": "success",
		"result": [{
			"TTL": 0,
			"type": "A",
			"address": "x.x.x.x",
			"lookupTime": 5
		}]
	}
}, {
	"request": {
		"query": "www.somesite.com",
		"dns": "",
		"protocol": "ipv4",
		"type": "AAAA",
		"timeout": ""
	},
	"response": {
		"status": "success",
		"result": [{
			"TTL": 0,
			"type": "AAAA",
			"address": "xxxx:xxx:xxx:xxx:xxx:xxxx:xxxx:xxxx",
			"lookupTime": 8
		}]
	}
}]

```

The following records are currently supported:

* A
* ANY
* AAAA
* CNAME
* MX
* NS
* PTR
* SOA
* SPF
* SRV
* TXT

### Supported Platforms

- Android
- iOS
