#### 概述

基于netty对websocket的支持，编写了一个简单的聊天室的后台。

前端页面基于[zfowed](https://github.com/zfowed)编写的[聊天室模板](https://github.com/zfowed/charooms-html)，添加了一些页面控制以及与后台交互的逻辑（就是里面写的很挫的js代码）。

主要实现功能：

1、聊天室人数获取

2、聊天室用户列表

3、聊天信息的发送和接受

#### 实现细节

整个聊天室共三个页面

##### 登陆页面

![登录页](https://github.com/zfowed/charooms-html/raw/master/screenshots/login.jpg)

在该页面，用户输入用户名，并选择头像id。没有与后台交互，单纯把用户名以及头像id存到本地cookie中，方便以后获取，然后跳转到聊天室大厅。

后台HttpRequestHandler的handleStaticFile方法响应静态文件获取请求。

##### 聊天室大厅

![首页](https://github.com/zfowed/charooms-html/raw/master/screenshots/index.jpg)

前端功能：

进入界面后，从cookie中获取用户名和头像，显示在右上角。

主题更换功能沿用原前端代码，实现细节不详。

ajax从后台获取各个聊天室人数。

点击聊天室后，页面跳转到对应聊天室页面。在请求链接中添加参数，告知后台进入的房间号。

后台功能：

后台HttpRequestHandlerr的handleStaticFile方法响应静态文件获取请求

HttpRequestHandlerr的handleJsonQuery方法，处理人数查询请求。用channel代表用户链接，用channelgroup代表聊天室，channelgroup的size方法可以返回聊天室人数。

##### 聊天室界面

![聊天室页](https://github.com/zfowed/charooms-html/raw/master/screenshots/room.jpg)

前端功能：

1、进入页面后，与后台建立websocket连接，连接成功后，返回成功信息，并在主窗体显示欢迎信息。

2、聊天信息发送。聊天信息显示在前端页面，并用json封装，以websocket的text形式，发送给后台，便于后台传输给其他用户。

3、聊天信息接收。收到信息后，json解码，不是本人发送的消息，显示在主窗体。

4、用户列表查询。用json封装请求，以websocket的text形式，发给后台。获取用户列表后，解码并添加到页面。

用户查询和信息发送方式完全相同，仅仅用不同数字区别。

后台功能：

1、使用netty自带的WebSocketServerProtocolHandler处理websocket的连接和断开请求。

2、用户连接成功后，将用户信息和channel绑定。通过channel可以查询用户名以及头像id。

3、收到前端传来的信息后，广播到聊天室每个用户。通过向channelgroup发送信息实现。

4、处理用户列表请求。将channelgroup的channel封装为json列表，传给前端。

#### 存在问题

前端的websocket原生api有限，只有：打开、关闭、收到信息和出错四个。想实现心跳，需要自己定义。

netty对websocket的支持，包含了心跳，ping/pong frame，可以用来与netty客户端心跳，想和网页客户端交互心跳，同样需要自己定义。




