# DAQ_UI

> 有界面的DAQ系统 

## 一、实现功能

### 1、init

> 1.初始化ringbuffer
>
> 2.初始化各个功能模块（config、readout、builder、store)
>
> 3.状态机转入initialized状态

### 2、config

> 1.向电子学端发送配置信息
>
> 2.状态机转入configed状态

### 3、start

> 1.向电子学端发送start命令
>
> 2.将start操作及其时间记录在log文件
>
> 3.接收来自电子学的数据
>
> 4.状态机转入running状态

### 4、stop

> 1.向电子学端发送stop命令
>
> 2.将start操作及其时间记录在log文件
>
> 3.状态机转入configed状态

### 5、unconfig

> 状态机进入initialized状态，此时可重新对电子学进行

### 6、uninit

> 状态机进入waiting状态，此时可重新对DAQ程序进行初始化设置