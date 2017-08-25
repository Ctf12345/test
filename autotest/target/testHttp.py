#! /usr/bin/python3
#ref-to https://www.zhaokeli.com/Article/6330.html
import urllib.request
import urllib.parse
import collections
import sys,os
import json
from optparse import OptionParser
#./testHttp.py test_func -Dusername=\"test123\" -Dpassword=123
#<exec cmd='>testHttp.py user_logout_sub -DuserId="test00001"' expect_retcode="0"/>

headers = {"Content-Type" : "application/x-www-form-urlencoded; charset=UTF-8",
           "Accept" : "*/*"}
url = "http://192.168.8.133:18088/"

dic = collections.OrderedDict()

def posturl(sub_url, data={}):
    try:
        params = urllib.parse.urlencode(data).encode(encoding='UTF8')
        req = urllib.request.Request(sub_url, params, headers)
        rsp = urllib.request.urlopen(req, timeout=5).read()
        return(rsp.decode('utf-8'))
    except urllib.error.HTTPError as e:
        print(e.code)
        print(e.read().decode("utf8"))

def user_logout_sub(userId):
    return posturl(url+"user_logout", {'userId' : userId})

def imsi_revuim_sub(imsi):
    return posturl(url+"user_revuim", {'imsi' : imsi})

def user_revuim_sub(userId):
    return posturl(url+"user_revuim", {'userId' : userId})

def user_binduim_sub(userId,imsi):
    dic["imsi"] = imsi
    dic["userId"] = userId
    return posturl(url+"user_binduim", dic)

def user_unbinduim_sub(imsi):
    return posturl(url+"user_unbinduim", {'imsi' : imsi})

def sim_disable_sub(imsi,disable):
    dic["imsi"] = imsi
    dic["disable"] = disable
    return posturl(url+"sim_disable", dic)

def user_trace_off_sub(userId):
    return posturl(url+"user_trace", {'userId' : userId})

def user_trace_on_sub(userId,level):
    dic["userId"] = userId
    dic["level"] = level
    return posturl(url+"user_trace", dic)

def user_disable_sub( userId, disable):
    dic["userId"] = userId
    dic["disable"] = disable
    return posturl(url+"user_disable", dic)

def user_assignuim_sub(userId,imsi):
    dic["userId"] = userId
    dic["imsi"] = imsi
    return posturl(url+"user_assignuim", dic)

def user_logupload_sub(userId,startTime,endTime,level,logSizeLimit):
    dic["userId"] = userId
    dic["startTime"] = startTime
    dic["endTime"] = endTime
    dic["level"] = level
    dic["logSizeLimit"] = logSizeLimit
    return posturl(url+"user_logupload", dic)

def sp_getLogs_sub(macAddress):
    return posturl(url+"simpool_getLogs", {'macAddress' : macAddress})

def sp_disable_sub(macAddress,disable):
    dic["macAddress"] = macAddress
    dic["disable"] = disable
    return posturl(url+"simpool_disable", dic)

def sp_logupload_sub(macAddress,dayTime):
    dic["macAddress"] = macAddress
    dic["dayTime"] = dayTime
    return posturl(url+"simpool_logupload", dic)

def sp_upgrade_sub(macAddress,sp_url):
    dic["macAddress"] = macAddress
    dic["sp_url"] = sp_url
    return posturl(url+"simpool_upgrade", dic)

def sp_trace_sub(macAddress,on_off):
    dic["macAddress"] = macAddress
    dic["on_off"] = on_off
    return posturl(url+"simpool_trace", dic)

def sp_reboot_sub(macAddress,cause):
    dic["macAddress"] = macAddress
    dic["cause"] = cause
    return posturl(url+"simpool_reboot", dic)

def sp_clearLogs_sub(macAddress,fileName):
    dic["macAddress"] = macAddress  
    dic["fileName"] = fileName
    return posturl(url+"simpool_clearLogs", dic)

if __name__ == '__main__':
    func_name = sys.argv[1]
    params = sys.argv[2:]
    parser = OptionParser()
    parser.add_option("-D", "--option", action="append", dest="params")

    (options, args) = parser.parse_args(sys.argv)
    params = options.params
    funcname = args[1]
    script = "%s(%s)" % (funcname, ", ".join(params))
    #print(script)
    retstr = eval(script)
    sys.stderr.write(retstr)
    json_obj = json.loads(retstr)
    code = json_obj['code']
    print(code)
 
