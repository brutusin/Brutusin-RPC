#org.brutusin:rpc [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0) [![Build Status](https://api.travis-ci.org/brutusin/Brutusin-RPC.svg?branch=master)](https://travis-ci.org/brutusin/rpc) [![Maven Central Latest Version](https://maven-badges.herokuapp.com/maven-central/org.brutusin/rpc-root/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.brutusin/rpc/)

`org.brutusin:rpc` is a JEE web framework for creating thin server architectures in [Single-page applications](https://en.wikipedia.org/wiki/Single-page_application), and offering:

1. JSON-RPC 2.0 over HTTP
2. JSON-RPC 2.0 over websockets
3. Messaging (publish/subscribe) over websockets

## Full Documentation

See the [Wiki](https://github.com/brutusin/rpc/wiki) for full documentation, examples, operational details and other information.

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
and automatically your browser will be openend with a functional testing client for the newly created service:

![Service testing client](doc/img/service-test.png)

## Status
This project is an evolution of https://github.com/brutusin/jsonsrv. Documentation in progress

## Live demo
http://demo.rpc.brutusin.org

## Support, bugs and requests
https://github.com/brutusin/rpc/issues

## Authors

- Ignacio del Valle Alles (<https://github.com/idelvall/>)

Contributions are always welcome and greatly appreciated!

##License
Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
