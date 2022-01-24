# cpu-memory
控制服务器cpu和内存占用
1、将压缩包内的所有文件放到/usr/local/src/cpu-test目录，如果想要放到其他目录，需要修改start.sh脚本，将AGENT_PATH修改为自定义的路径
2、修改application.yml，将定时任务时间，以及程序运行时间修改为自己想要的。
3、执行./start.sh脚本启动程序
4、不想要运行程序的时候，执行./stop.sh脚本停止程序
5、程序端口为19999
