Brutusin-RPC es un nuevo microframework Java orientado a la creación APIs JSON-RPC para ser consumidas en [aplicaciones single-page](https://es.wikipedia.org/wiki/Single-page_application) via AJAX o Websockets.
El framework ha sido diseñado con el objetivos principal de ofrecer una alta mantentenibilidad de los servicios, y como característica diferenciadora, proporciona al desarrollador (cliente de la API) un repositorio de los servicios disponibles, que muestra sus características y descripciones, y permite oejecutarlos directamente.

En este artículo, a modo de tutorial, desarrollaremos una pequeña chat de ejemplo, en el que se hará uso de los distintos elementos que ofrece el framework: servicios sobre HTTP, servicios sobre Websocket y Topics sobre Websockets.

## Descripción general
El chat, será construido como una aplicación de página única. Una jsp (`index.jsp`) recibirá la petición inicial, y devolverá al navegador el código HTML, CSS y Javascript que definirá la presentación. Posteriormente esté codigo cliente desencadenará una sucesión de peticiones AJAX y Websocket a los servicios implementados por con Brutusin-RPC, para obtener datos.
La aplicación asociará un identificador (entero autoincremental) a cada sesión de usuario, y permitirá el envío de mensajes públicos (visibles por todos los usuarios) como privados (visible sólo por emisor y receptor), así como la subida/bajada de ficheros.

La API a crear será la siguiente:

### Servicios HTTP
Estos servicios serán los consumidos mediante AJAX desde el navegador.

 - `sendFile(to, files)`: Subirá los ficheros al servidor y comunicará a los destinatarios (emisor/receptor o todos) su identificador para descargarlos.
 - `download(id)`: Permita descargar un fichero, conocido su identificador.
 
### Servicios Websocket
Websocket es un protocolo de bajo nivel iniciado desde HTTP pero que posteriormente no incluye ninguna caraterística adicional a TCP/IP. Por ello su uso es recomendable para escenarios que requieran bidireccionalidad (HTTP no lo permite), o una alta frecuencia de intercambio de (pequeños) mensajes, en los que la trama HTTP añadiría un considerable overhead al tamaño de estos.

 - `getUserInfo()`: Devuelve la información (id de usuario), del propio usuario
 - `getUsers()`: Devuelve el listado de usuarios activos
 - `sendMessage(to, message)`: Envía un mensaje a los destinatarios (emisor/receptor o todos) 
 
### Topics
Los "topics" son entidades lógicas que, conceptualmente y de manera genérica, representan "puntos de interés" para sus clientes. Definidos originalmente en el patrón de diseño ["publish/subscribe"](https://en.wikipedia.org/wiki/Publish%E2%80%93subscribe_pattern), en el caso particular de Brutusin-RPC representan canales de comunicación de servidor a cliente.

Utilizando la [API Javascript](https://github.com/brutusin/Brutusin-RPC/wiki/Javascript-API) proporcionada por el framework, el código cliente puede realizar la suscripción al topic y especificar una función callback que será invocada por el framework cada vez que llege un mensaje del servidor. 

Desde el punto de vista del servidor, los topics:
 - Implementan un método de filtrado, que define, dado un filtro, qué subscriptores son destinatarios del mensaje
 - Ofrecen un método para publicar mensajes (utilizado desde los servicios)
 
Esta aplicación de define un único "topic":
 - `messages`: Topic al que se suscriben todos los usuarios y que permite su interacción, via publicación y notificación de mensajes. La aplicación publicará en este topic 3 tipos diferentes de mensajes: 
   - Mensajes de texto
   - Mensajes de subidas de ficheros
   - Login/logout de usuarios
 
## Implementación

### Requisitos
>- JDK 1.7 o posterior
- Maven 3.0+

### Creación del proyecto
El primer paso consiste en crear la estructura del proyecto utilizando el siguiente arquetipo maven:
 [`rpc-tomcat-war`](https://github.com/brutusin/Brutusin-RPC/tree/master/rpc-archetypes/rpc-tomcat-war)

Para ello, en el directorio de tu elección, ejecuta el siguiente comando maven:
```properties
mvn archetype:generate -B -DarchetypeGroupId=org.brutusin -DarchetypeArtifactId=rpc-tomcat-war -DarchetypeVersion=${version} -DgroupId=test -DartifactId=brutusin-rpc-chat -Dversion=1.0.0-SNAPSHOT
```

Siendo:
`${version}` la última versión del arquetipo disponible en Maven Central [![Maven Central Latest Version](https://maven-badges.herokuapp.com/maven-central/org.brutusin/rpc-tomcat-war/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.brutusin/rpc-tomcat-war/)

y entonces, la siguiente estructura del proyecto será creada:
```
.
|-- brutusin-rpc-chat
|   |-- src/main
|   |   |-- java/test
|   |   |-- webapp
|   |   |   |-- index.jsp
|   |   |   |-- WEB-INF
|   |   |   |   |-- web.xml
|   |   |-- resources
|   |   |   |-- brutusin-rpc.xml
|   |-- pom.xml
```
finalmente, establece la carpeta raiz del proyecto recien creado como directorio de trabajo:

```sh
cd brutusin-rpc-chat
```

### Identificación del usuario
 
Como se ha comentado, el usuario se identificará mediante un entero asociado a su sesión. 
Crearemos la clase `org.brutusin.chat.User` para representar a un usuario y dejar lugar a futura funcionalidad (nickname, IP, ...)
 
[**`src/main/java/org/brutusin/chat/User.java`**](https://raw.githubusercontent.com/brutusin/Brutusin-RPC/master/rpc-demos/rpc-chat/src/main/java/org/brutusin/chat/User.java):
 
 ```java
 public final class User {

    private static final AtomicInteger counter = new AtomicInteger();
    private final Integer id;

    private User() {
        this.id = counter.incrementAndGet();
    }

    public Integer getId() {
        return id;
    }

    public static User from(HttpSession httpSession) {
        synchronized (httpSession) {
            User user = (User) httpSession.getAttribute(User.class.getName());
            if (user == null) {
                user = new User();
                httpSession.setAttribute(User.class.getName(), user);
            }
            return user;
        }
    }
}
 ```

### Topic

El topic a implementar trabajará con mensajes de tipo `org.brutusin.chat.topics.Message` y permitirá filtrado por id de usuario (para permitir mensajes privados que sólo lleguen a ese usuario).

#### Mensaje:

Como hemos comentado anteriormente, estos mensajes representarán tres tipos de eventos (mensajes de texto, envio de fichero y lofin/logout de usuarios). Podría haberse optado por utilizar topics independientes para estos casos, pero por mantener el ejemplo más simple se ha optado por esto.

[**`src/main/java/org/brutusin/chat/topics/Message.java`**](https://raw.githubusercontent.com/brutusin/Brutusin-RPC/master/rpc-demos/rpc-chat/src/main/java/org/brutusin/chat/topics/Message.java):`

```java
public class Message {

    private long time;
    private Integer from;
    private Integer to;
    private String message;
    private Attachment[] attachments;
    private Boolean logged;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public Attachment[] getAttachments() {
        return attachments;
    }

    public void setAttachments(Attachment[] attachments) {
        this.attachments = attachments;
    }

    public Boolean getLogged() {
        return logged;
    }

    public void setLogged(Boolean logged) {
        this.logged = logged;
    }
}
```
#### Implementación:

Para crear un topic se extiende de una clase base del framework `Topic<F,M>` siendo `F` la clase que representa al filtro, y `M` la de sus mensajes. Por lo tanto en este caso crearemos la clase `MessageTopic`  extiendiendo de `Topic<Integer, Message>`. 

[**`src/main/java/org/brutusin/chat/topics/MessageTopic.java`**](https://raw.githubusercontent.com/brutusin/Brutusin-RPC/master/rpc-demos/rpc-chat/src/main/java/org/brutusin/chat/topics/MessageTopic.java):

```java
public class MessageTopic extends Topic<Integer, Message> {

    private final Map<Integer, WritableSession> sessionMap = Collections.synchronizedMap(new HashMap());

    @Override
    protected void beforeSubscribe(WritableSession session) {
        User user = getUser();
        session.getUserProperties().put("user", user);
        sessionMap.put(user.getId(), session);
        Message message = new Message();
        message.setFrom(user.getId());
        message.setTime(System.currentTimeMillis());
        message.setLogged(true);
        fire(null, message);
    }

    @Override
    protected void afterUnsubscribe(WritableSession session) {
        User user = getUser();
        sessionMap.remove(getUser().getId());
        Message message = new Message();
        message.setFrom(user.getId());
        message.setTime(System.currentTimeMillis());
        message.setLogged(false);
        fire(null, message);
    }

    @Override
    public Set<WritableSession> getSubscribers(Integer filter) {
        if (filter == null) {
            return super.getSubscribers();
        }
        WritableSession toSession = sessionMap.get(filter);
        if (toSession == null) {
            return null;
        }
        HashSet<WritableSession> ret = new HashSet<WritableSession>();
        ret.add(toSession);
        ret.add(sessionMap.get(getUser().getId()));
        return ret;
    }

    private User getUser() {
        return User.from(RpcActionSupport.getInstance().getHttpSession());
    }
}

```
Resaltar que hemos utilizado una estructura (`sessionMap`) orientada a obtener las sesiones de usuario dado su id y que en la creación y cancelación de suscripciones estamos creando mensajes (llamadas a `fire(null, message)`) para notificar a todos los suscriptores actuales del evento de login/logout al que hacíamos referencia.


getUserInfo

JSON Schema es una especificación (actualmente en estado draft) que define una sintaxis JSON para representar la estructura y restricciones de otros documentos JSON. Es el hómologo a XSD en el mundo JSON.
Los esquemas JSON le permiten describir de manera automática la estructura de los mensajes de los servicios a través de unos meta-servicios proporcionados por el propio framework, y más aún, ofrecer una interfaz al desarrollador para listar todos los servicios disponibles, ver sus características y descriptción, y ejecutarlos directamente.



- Asegurar un correcto uso de HTTP:




El segundo punto
 para de manera automática, describir la estructura de los mensajes lo permite al usuario implementar servicios con complejos mensajes de entrada/salida, sin comprometer la usabilidad.
Así mismo sus capacidades de descripción de los servicios le permiten ofrecer de serie, una aplicación de repositorio de servicios, donde se pueden listar todos los servicios disponibles, ver sus características y descriptción, y ejecutarlos directamente. Esto repercute en una alta mantenibilidad de los servicios, 
donde

Especialmente orientado a la mantenibilidad de los servicios, creación aplicaciones SPA de alta complejidad, en términos del número de servicios, de su naturaleza y de la estructura de sus mensajes.

Está orientado a la mantenibilidad de los servicios.

 

Las características diferenciadoras frente a otras alternativas son las siguientes: 
1 RPC, no REST. Existe mucha controversia en cuanto a qué modelo es mejor, si el REST, orientado a recursos (entidades), con un numero limitado de operaciones por entidad (una por cada método HTTP), o RPC orientado a operaciones. Los principales argumentos a favor de REST son la interoperabilidad, dada su predictibilidad debida a una semantica conocida (verbos, plurales, ...) y su popularidad (es el estandar de-facto). y que REST realiza un uso correcto de HTTP (en realidad está vinculado a él)
2 Basado en JSON-Schema
3 

>mvn archetype:generate -B -DarchetypeGroupId=org
>cd chat
