#Brutusin-RPC [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0) [![Build Status](https://api.travis-ci.org/brutusin/Brutusin-RPC.svg?branch=master)](https://travis-ci.org/brutusin/Brutusin-RPC) [![Maven Central Latest Version](https://maven-badges.herokuapp.com/maven-central/org.brutusin/rpc-root/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.brutusin/rpc-root/)

JEE web microframework for creating JSON-RPC APIs for [Single-page applications](https://en.wikipedia.org/wiki/Single-page_application) offering:

- JSON-RPC 2.0 over HTTP
- JSON-RPC 2.0 over websockets
- Messaging (publish/subscribe) over websockets
- Javascript client API
- Functional testing module

## Full Documentation

See the [Wiki](https://github.com/brutusin/Brutusin-RPC/wiki) for full documentation, examples, operational details and other information.

## Quick start. Code and test!

Run this java application:
```java
import org.brutusin.rpc.Server;
import org.brutusin.rpc.websocket.WebsocketAction;

public class HelloAction extends WebsocketAction<String, String> {

    @Override
    public String execute(String input) throws Exception {
        return "Hello " + input + "!";
    }

    public static void main(String[] args) throws Exception {
        Server.test(new HelloAction());
    }
}
```
and automatically your browser will be open with a functional testing client for the newly created service:

![Service testing client](https://github.com/brutusin/Brutusin-RPC/wiki/img/hello-action-test.png)

## Live demo
http://demo.rpc.brutusin.org

## Support, bugs and requests
https://github.com/brutusin/Brutusin-RPC/issues

## Authors

- Ignacio del Valle Alles (@idelvall)

Contributions are always welcome and greatly appreciated! Just fork, and send me a pull request.

##License
Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
