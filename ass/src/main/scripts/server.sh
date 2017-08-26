#! /bin/bash
####################################################################################################
# Author    : yuanyuefeng                                                                          #
# E-mail    : yuanyuefeng@cootf.com                                                                #
# Version   : v1.01                                                                                #
# ModifyLog : v1.01 2017/04/18                                                                     #
#                   1.mv ./monitor.log to ./logs                                                   #
#                   2.clean ./cronfile after use immediately                                       #
#           : v1.0  2017/04/17                                                                     #
#                   First Edition                                                                  #
# Comment   : 1.Need bash support                                                                  #
#             2.Should encrypt with shc before deploy to server                                    #
#                   $sudo apt install shc                                                          #
#                   $shc -e 31/12/2017 -m "Please contact yuanyuefeng@cootf.com" -r -f ./server.sh #
#                   $rm ./server.sh.x.c                                                            #
#                   $mv ./server.sh.x ./server                                                     #
#                   $chmod a+x ./server                                                            #
####################################################################################################
function usage {
  echo "Usage : `script_filename` <operation>"
  echo "where <operation>s include:"
  echo "    help,h             : show this usage message"
  echo "    start,s            : start the server"
  echo "    stop,t             : stop the server"
  echo "    restart,r          : restart the server"
  echo "    status,?           : show the status of server & monitor"
  echo "    start_monitor,S    : start server monitor."
  echo "                         The monitor will check whether the server is running and start the server if stopped regularly."
  echo "    stop_monitor,T     : stop server monitor."
  echo "    view_log,v         : view the server log."
  echo "    view_monitor_log,V : view the monitor log"
  echo "    do_monitor,d       : do the monitor job (check whether the server is running and start the server if stopped)"
}

function script_dir {
  echo $( dirname "$0" )
}

function script_filename {
  echo $( basename "$0" )
}

function start_server {
  # check server status
  pid=`server_pid`
  if [ "$pid" != "" ]; then
    echo "Server has already started!!!"
    return
  fi

  echo "Starting server..."

  base_dir=`script_dir`
  # mkdir "logs" if not exist
  if [ ! -d "$base_dir/logs" ]; then
    mkdir "$base_dir/logs"
  fi
  # get db ip based on localhost ip
  # !!!NOTE: ifconfig called in crontab will return empty, '/sbin/' prefix is *Necessary*!!!
  # '/usr/bin/' prefix is NOT necessary for awk is because of crontab is in '/usr/bin' too
  IP=`/sbin/ifconfig | awk -F : '{if($2~/Bcast/)print $2}' | awk '{print $1}'`
  if [ $IP = "192.168.8.73" ]; then
    DB_IP="192.168.8.74:37018"
  else
    DB_IP="$IP:37018"
  fi

  # start server
  $JRE_HOME/bin/java -jar `ls $base_dir/ass*.jar` -Ddb_ip=$DB_IP -Ddb_id="allCode" -Ddb_psd="allCode@cootf.com" 1>/dev/null 2>>$base_dir/logs/logs_default.log &

  # wait 5 seconds for the server init
  sleep 5

  # check operation result
  pid=`server_pid`
  if [ "$pid" != "" ]; then
    echo "[Succeed]"
  else
    echo "[Failed!!!]"
  fi
}

function stop_server {
  # check server status
  pid=`server_pid`
  if [ "$pid" = "" ]; then
    echo "Server has already stopped!!!"
    return
  fi

  echo "Stopping server..."

  # stop server
  kill $pid

  sleep 2

  # check operation result
  pid=`server_pid`
  if [ "$pid" = "" ]; then
    echo "[Succeed]"
  else
    echo "[Failed!!!]"
  fi
}

function server_pid {
  netstat -apn 2>/dev/null | grep 18080 | awk '{print $NF}' | awk -F "/" '{print $1}'
}

function start_monitor {
  # check monitor status
  if [ "`monitor_status`" != "" ]; then
    echo "Monitor has already started!!!"
    return
  fi

  echo "Starting monitor..."

  # start monitor
  base_dir=`script_dir`
  echo "* * * * * `pwd`/`script_filename` d">$base_dir/cronfile
  crontab $base_dir/cronfile
  rm $base_dir/cronfile

  # check operation result
  if [ "`monitor_status`" != "" ]; then
    echo "[Succeed]"
  else
    echo "[Failed!!!]"
  fi
}

function stop_monitor {
  # check monitor status
  if [ "`monitor_status`" = "" ]; then
    echo "Monitor has already stopped!!!"
    return
  fi

  echo "Stopping monitor..."

  # stop monitor
  base_dir=`script_dir`
  rm -f $base_dir/logs/monitor.log
  crontab -r

  # check operation result
  if [ "`monitor_status`" = "" ]; then
    echo "[Succeed]"
  else
    echo "[Failed!!!]"
  fi
}

function monitor_status {
  crontab -l 2>/dev/null | grep `script_filename`
}

function curr_time {
  date '+%Y/%m/%d %T'
}

function do_monitor {
  # check whether server is running
  pid=`server_pid`
  if [ "$pid" = "" ]; then
    base_dir=`script_dir`
    echo "[`curr_time`] Server down!" >> $base_dir/logs/monitor.log

    # setup environment
    export JRE_HOME="/opt/jre1.8.0_121"
    export CLASSPATH=$JRE_HOME/lib/rt.jar:.

    # start server
    echo "[`curr_time`] try to start the server..." >> $base_dir/logs/monitor.log
    start_server

    # check operation result
    pid=`server_pid`
    if [ "$pid" != "" ]; then
      echo "[Succeed]" >> $base_dir/logs/monitor.log
    else
      echo "[Failed!!!]" >> $base_dir/logs/monitor.log
    fi
  fi
}


function output_status {
  # server status
  pid=`server_pid`
  if [ "$pid" = "" ]; then
    echo "Server has Stopped!!!"
  else
    echo "Server is Running..."
  fi
  # monitor status
  if [ "`monitor_status`" = "" ]; then
    echo "Monitor has stopped!!!"
  else
    echo "Monitor is Running..."
  fi
}

function view_log {
  # view log
  base_dir=`script_dir`
  view $base_dir/logs/logs_default.log
}

function view_monitor_log {
  # view monitor log
  base_dir=`script_dir`
  view $base_dir/logs/monitor.log
}

# --------------------------
#  main program starts here
# --------------------------
if [ $# -lt 1 ]
then
  usage
  exit 1
fi

if [ $1 == 'help' -o $1 == 'h' ]; then
  usage
elif [ $1 == 'start' -o $1 == 's' ]; then
  start_server
elif [ $1 == 'stop' -o $1 == 't' ]; then
  stop_server
elif [ $1 == 'restart' -o $1 == 'r' ]; then
  stop_server
  start_server
elif [ $1 == 'status' -o $1 == '?' ]; then
  output_status
elif [ $1 == 'start_monitor' -o $1 == 'S' ]; then
  start_monitor
elif [ $1 == 'stop_monitor' -o $1 == 'T' ]; then
  stop_monitor
elif [ $1 == 'view_log' -o $1 == 'v' ]; then
  view_log
elif [ $1 == 'view_monitor_log' -o $1 == 'V' ]; then
  view_monitor_log
elif [ $1 == 'do_monitor' -o $1 == 'd' ]; then
  do_monitor
else
  echo -e "Unknown <operation>!\n"
  usage
  exit 1
fi
