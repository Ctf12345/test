#! /bin/bash
shc -e 31/12/2017 -m "Please contact yuanyuefeng@cootf.com" -r -f ./server.sh
sleep 3
rm ./server.sh.x.c
rm ./server.sh
mv ./server.sh.x ./server
chmod a+x ./server
