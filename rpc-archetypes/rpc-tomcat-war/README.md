#`rpc-tomcat-war` [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0) [![Maven Central Latest Version](https://maven-badges.herokuapp.com/maven-central/org.brutusin/rpc-tomcat-war/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.brutusin/rpc-tomcat-war/)

Maven archetype for creating a Brutusin-RCP war to be deployed in an external into a `Servlet 3.0/Websocket 1.1` compliant web container.

Projects generated with this archetype can be run in development environments via maven:

```sh
mvn exec:java -Dexec.mainClass=org.brutusin.rpc.Main
```

## Usage
```properties
mvn archetype:generate -B -DarchetypeGroupId=org.brutusin -DarchetypeArtifactId=rpc-tomcat-war -DarchetypeVersion=${version} -DgroupId=${your.groupId} -DartifactId=${your.artifactId} -Dversion=${your.version}
```

## Support, bugs and requests
https://github.com/brutusin/Brutusin-RPC/issues

## Authors

- Ignacio del Valle Alles (<https://github.com/idelvall/>)

Contributions are always welcome and greatly appreciated!

##License
Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0

