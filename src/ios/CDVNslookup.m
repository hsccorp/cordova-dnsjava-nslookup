
/*
 * Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
 * Copyright (c) 1997,1999 by Internet Software Consortium.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
 * OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#import "CDVNslookup.h"
#import <Foundation/Foundation.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <arpa/nameser.h>
#include <resolv.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>

@implementation CDVNslookup
NSMutableDictionary *completeResult;
NSMutableDictionary *responseObj;
NSMutableDictionary *requestObj;
NSMutableDictionary *resultObj;
NSMutableArray *tempResultArr;
NSMutableArray *completeResultArr;

#define MY_GET16(s, cp) do { \
register const u_char *t_cp = (const u_char *)(cp); \
(s) = ((u_int16_t)t_cp[0] << 8) \
| ((u_int16_t)t_cp[1]) \
; \
(cp) += NS_INT16SZ; \
} while (0)

#define MY_GET32(l, cp) do { \
register const u_char *t_cp = (const u_char *)(cp); \
(l) = ((u_int32_t)t_cp[0] << 24) \
| ((u_int32_t)t_cp[1] << 16) \
| ((u_int32_t)t_cp[2] << 8) \
| ((u_int32_t)t_cp[3]) \
; \
(cp) += NS_INT32SZ; \
} while (0)



- (void) getNsLookupInfo:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSLog(@"---------------Get NS Lookup------------------");
        completeResultArr = [[NSMutableArray alloc] init];

        NSDictionary *typeObj = @{
                                  @"A" : @"ns_t_a",
                                  @"AAAA" : @"ns_t_aaaa"
                                  };

        int arraySize = [command.arguments count];
        NSLog(@"\nArray Size:%d", arraySize);
        NSLog(@"\n\nArray :%@", command.arguments);

        u_char answer[1024] = "";

        /* Take a res_state variable and initialize it. Post-initialization,
         * the DNS servers read from system's resolv.conf would be set in it.
         * In order to use a custom DNS server, we will change this struct and
         * call the res_nquery function which takes the res_state object. */
        struct __res_state res;
        res_ninit(&res);

        int i;
        for(i = 0; i < arraySize; i++) {
            requestObj = [[NSMutableDictionary alloc] init];

            NSString *query = command.arguments[i][@"query"];
            NSString *type =  command.arguments[i][@"type"];
            NSString *dns_server = command.arguments[i][@"dns"];

            const char *currQuery = [query UTF8String];
            const char *currType = [type UTF8String];
            int tempType;
            int compareResult = strncmp("AAAA", currType, 4);
            if(compareResult == 0) {
                tempType = ns_t_aaaa;
            }
            else {
                tempType = ns_t_a;
            }

            /* Custom DNS server specified? */
            if (dns_server != nil && [dns_server length] > 0) {
                const char *currServer = [dns_server UTF8String];

                struct in_addr addr;
                inet_aton(currServer, &addr);

                res.nsaddr_list[0].sin_addr = addr;
                res.nsaddr_list[0].sin_family = AF_INET;
                res.nsaddr_list[0].sin_port = htons(NS_DEFAULTPORT);
                res.nscount = 1;
            }

            /* Note the time just before the query */
            struct timeval tp;
            gettimeofday(&tp, NULL);
            long int startTime = tp.tv_sec * 1000 + tp.tv_usec / 1000;
            NSLog(@"-----------MS--------%ld", startTime	);

            int rv = res_nquery(&res, currQuery, ns_c_in,
                                tempType, answer, sizeof(answer));

            /* Note the time just after the response */
            gettimeofday(&tp, NULL);
            long int endTime = tp.tv_sec * 1000 + tp.tv_usec / 1000;
            NSLog(@"\n----------timeAfter:%ld",endTime);

            dump_dns(answer, rv, stdout, "\n");

            long timeDiff = endTime - startTime;
            NSNumber *lookupTime = [NSNumber numberWithInt:timeDiff];

            if(rv>-1) {
                responseObj[@"status"] = @"success";
            }
            else {
                responseObj[@"status"] = @"error";

            }
            responseObj[@"lookupTime"] = lookupTime;
            requestObj[@"query"] = query;
            requestObj[@"type"] = type;
            requestObj[@"timeout"] = @"";
            if (dns_server != nil) {
                requestObj[@"dns"] = dns_server;
            }
            completeResult[@"request"] = requestObj;
            completeResult[@"response"] = responseObj;
            [completeResultArr addObject:completeResult];

            NSLog(@"\n\nTemp Arr:%@",tempResultArr);

        }
        NSLog(@"\n\nComplte:%@\n\n\n",completeResultArr);
        NSLog(@"\nAll Lookup Complete");

        CDVPluginResult* pluginResult = nil;

        pluginResult =    [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:completeResultArr];

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

void dump_dns(const u_char *payload, size_t paylen, FILE *trace, const char *endline) {
    NSLog(@"\n\nDump DNS Start");
    completeResult = [[NSMutableDictionary alloc] init];
    responseObj = [[NSMutableDictionary alloc] init];

    tempResultArr = [[NSMutableArray alloc] init];
    u_int opcode, rcode, id;
    const char *sep;
    ns_msg msg;

    //fprintf(trace, " %sdns ", endline);
    if (ns_initparse(payload, paylen, &msg) < 0) {
        fputs(strerror(errno), trace);
        return;
    }
    opcode = ns_msg_getflag(msg, ns_f_opcode);
    rcode = ns_msg_getflag(msg, ns_f_rcode);
    id = ns_msg_id(msg);
    //fprintf(trace, "%u,%s,%u",opcode, p_rcode(rcode), id);
    /*fprintf(trace, "%s,%s,%u", _res_opcodes[opcode], p_rcode(rcode), id);*/

    NSLog(@"\n\nDump DNS Complete");
    dump_dns_sect(&msg, ns_s_an, trace, endline);
}

static void dump_dns_sect(ns_msg *msg, ns_sect sect, FILE *trace, const char *endline) {
    NSLog(@"\n\ndump_dns_sect start\n");
    int rrnum, rrmax;
    const char *sep;
    ns_rr rr;

    rrmax = ns_msg_count(*msg, sect);
    if (rrmax == 0) {
        fputs(" 0", trace);
        return;
    }
    fprintf(trace, " %s%d", endline, rrmax);
    sep = "";
    for (rrnum = 0; rrnum < rrmax; rrnum++) {
        if (ns_parserr(msg, sect, rrnum, &rr)) {
            fputs(strerror(errno), trace);
            return;
        }
        fprintf(trace, " %s", sep);
        dump_dns_rr(msg, &rr, sect, trace);
        sep = endline;
    }
    NSLog(@"\n\ndump_dns_sect Complete\n");
}


static void dump_dns_rr(ns_msg *msg, ns_rr *rr, ns_sect sect, FILE *trace) {
    NSLog(@"\n\n dump_dns_rr start\n");
    char buf[NS_MAXDNAME];
    u_int class, type;
    const u_char *rd;
    u_int32_t soa[5];
    u_int16_t mx;
    int n;

    class = ns_rr_class(*rr);
    type = ns_rr_type(*rr);
    fprintf(trace, "%s,%s,%s",
            ns_rr_name(*rr),
            p_class(class),
            p_type(type));
    if (sect == ns_s_qd)
        return;
    fprintf(trace, ",%lu", (u_long)ns_rr_ttl(*rr));

    NSNumber *ttlVal = [NSNumber numberWithInt : ns_rr_ttl(*rr)];
    //NSLog(@"\nTTL VAL:%@",ttlVal);
    rd = ns_rr_rdata(*rr);
    switch (type) {
        case ns_t_soa:
            n = ns_name_uncompress(ns_msg_base(*msg), ns_msg_end(*msg),
                                   rd, buf, sizeof buf);
            if (n < 0)
                goto error;
            putc(',', trace);
            fputs(buf, trace);
            rd += n;
            n = ns_name_uncompress(ns_msg_base(*msg), ns_msg_end(*msg),
                                   rd, buf, sizeof buf);
            if (n < 0)
                goto error;
            putc(',', trace);
            fputs(buf, trace);
            rd += n;
            if (ns_msg_end(*msg) - rd < 5*NS_INT32SZ)
                goto error;
            for (n = 0; n < 5; n++)
                MY_GET32(soa[n], rd);
            sprintf(buf, "%u,%u,%u,%u,%u",
                    soa[0], soa[1], soa[2], soa[3], soa[4]);
            break;
        case ns_t_a:
            inet_ntop(AF_INET, rd, buf, sizeof buf);
            break;
        case ns_t_aaaa:
            inet_ntop(AF_INET6, rd, buf, sizeof buf);
            break;
        case ns_t_mx:
            MY_GET16(mx, rd);
            fprintf(trace, ",%u", mx);
            /* FALLTHROUGH */
        case ns_t_ns:
        case ns_t_ptr:
        case ns_t_cname:
            n = ns_name_uncompress(ns_msg_base(*msg), ns_msg_end(*msg),
                                   rd, buf, sizeof buf);
            if (n < 0)
                goto error;
            break;
        case ns_t_txt:
            snprintf(buf, (size_t)rd[0]+1, "%s", rd+1);
            break;
        default:
        error:
            sprintf(buf, "[%u]", ns_rr_rdlen(*rr));
    }
    if (buf[0] != '\0') {
        putc(',', trace);
        fputs(buf, trace);
    }
    //NSLog(@"\n\nComplte Temp:%@\n\n\n",completeResult);
    //NSLog(@"\nBUF:%s",buf);
    NSString *address = [NSString stringWithFormat:@"%s" , buf];
    //NSLog(@"\nBUF2:%@",address);

    //tempResultArr = [[NSMutableArray alloc] init];


    NSDictionary *result = @{
                             @"TTL":ttlVal,
                             @"address":address
                             };
    //NSLog(@"\n\nResultObj:%@",result);
    [tempResultArr addObject:result ];
    //NSLog(@"\n\nTemp Arr:%@",tempResultArr);
    //completeResult[@"result"] = tempResultArr;
    responseObj[@"result"] = tempResultArr;


    //NSLog(@"\n\nComplte:%@\n\n\n",completeResult);
    NSLog(@"\n\n dump_dns_rr complete\n");
}

@end