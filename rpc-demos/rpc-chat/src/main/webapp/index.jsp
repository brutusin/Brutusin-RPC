<!DOCTYPE html>
<html>
    <head>
        <meta name="_csrf" content="${_csrf.token}"/>
        <meta name="_csrf_header" content="${_csrf.headerName}"/>
        <meta charset="utf-8">
        <title>Brutusin-RPC chat</title>
        <script src="rpc/brutusin-rpc.js"></script>
        <script language='javascript'>
            var http = brutusin["rpc"].initHttpEndpoint("<%=request.getContextPath() + org.brutusin.rpc.RpcConfig.getInstance().getPath() + "/http"%>");
            var wsk = brutusin["rpc"].initWebsocketEndpoint("<%=request.getContextPath() + org.brutusin.rpc.RpcConfig.getInstance().getPath() + "/wskt"%>");
            var totalConnections;
            var userId;
            var autoscroll;
            var inputTo;
            var inputText;
            wsk.subscribe("topic.messages", onMessage);
            wsk.exec({
                service: "svr.getUserInfo",
                load: function (response) {
                    userId = response.result.id;
                    document.getElementById("userId").innerHTML = userId;
                }});
            wsk.exec({
                service: "svr.getUsers",
                load: function (response) {
                    setTotalUsers(response.result.length);
                }});
            function setTotalUsers(n) {
                totalConnections = n;
                document.getElementById("total").innerHTML = totalConnections;
            }
            function onMessage(message) {
                if (message.hasOwnProperty("logged")) {
                    if (message.logged) {
                        setTotalUsers(totalConnections + 1);
                    } else {
                        setTotalUsers(totalConnections - 1);
                    }
                } else {
                    if (message.message) {
                        renderMessage(message);
                    } else if (message.attachments) {
                        renderFile(message);
                    }
                    scroll();
                }
            }

            function scroll() {
                if (autoscroll.checked) {
                    var main = document.getElementById("main");
                    main.scrollTop = main.scrollHeight;
                }
            }

            function getDate(milis) {
                var date;
                if (milis) {
                    date = new Date(milis);
                } else {
                    date = new Date();
                }
                function pad(num, size) {
                    var s = "00" + num;
                    return s.substr(s.length - size);
                }
                return pad(date.getHours(), 2) + ":" + pad(date.getMinutes(), 2) + ":" + pad(date.getSeconds(), 2);
            }


            function renderFile(message) {
                var msg = "[" + message.from + ">" + (message.to ? message.to : "*") + "] [" + getDate(message.time) + "]";
                var ul = document.getElementById("messages");
                var li = document.createElement("li");
                li.className += " file";
                if (message.from === userId) {
                    li.className += " from";
                } else
                if (message.to === userId) {
                    li.className += " to";
                }
                ul.appendChild(li);
                var span = document.createElement("span");
                var txtNode = document.createTextNode(msg);
                span.appendChild(txtNode);
                li.appendChild(span);

                for (var i = 0; i < message.attachments.length; i++) {
                    var a = document.createElement("a");
                    a.href = "#";
                    (function () {
                        var id = message.attachments[i].id;
                        a.onclick = function () {
                            downloadFile(id);
                        }
                    }());
                    var txtNode = document.createTextNode(message.attachments[i].name);
                    a.appendChild(txtNode);
                    li.appendChild(a);
                }
            }

            function renderMessage(message) {
                var msg = "[" + message.from + ">" + (message.to ? message.to : "*") + "] [" + getDate(message.time) + "] " + (message.message ? message.message : "");
                var ul = document.getElementById("messages");
                var li = document.createElement("li");
                if (message.from === userId) {
                    li.className += " from";
                } else
                if (message.to === userId) {
                    li.className += " to";
                }
                li.onclick = function () {
                    inputTo.value = message.from;
                    inputText.focus();
                };
                li.style.cursor = "pointer";
                var txtNode = document.createTextNode(msg);
                li.appendChild(txtNode);
                ul.appendChild(li);
            }

            function renderError(to, error) {
                var ul = document.getElementById("messages");
                var li = document.createElement("li");
                li.className += " error";
                var txtNode = document.createTextNode("[" + userId + ">" + (to ? to : "*") + "] [" + getDate() + "] [" + error + "]");
                li.appendChild(txtNode);
                ul.appendChild(li);
            }

            function renderProgress(to) {
                var ul = document.getElementById("messages");
                var li = document.createElement("li");
                li.className += "from";
                li.appendChild(document.createTextNode("[" + userId + ">" + (to ? to : "*") + "] [" + getDate() + "] "));
                var txtNode = document.createTextNode("");
                li.appendChild(txtNode);
                ul.appendChild(li);
                scroll();
                return txtNode;
            }

            function sendFiles(files) {
                if (files.length > 0) {
                    var input = getInput(files);
                    if (inputTo.value) {
                        input.data.to = parseInt(inputTo.value);
                    }
                    var progressNode = renderProgress(input.data.to);
                    var current = 0;
                    http.ajax({service: "svr.sendFile",
                        files: input.fileMap,
                        input: input.data,
                        load: function (response, status, message) {
                            if (response) {
                                if (response.error) {
                                    progressNode.nodeValue = " [" + response.error.meaning + (response.error.data ? (". " + response.error.data) : "") + "]";
                                    progressNode.parentNode.className += " error";
                                    scroll();
                                } else if (!response.result) {
                                    progressNode.nodeValue = " [Files could not be delivered]";
                                    progressNode.parentNode.className += " error";
                                    scroll();
                                } else {
                                    progressNode.parentNode.parentNode.removeChild(progressNode.parentNode);
                                }
                            } else {
                                progressNode.nodeValue = " [HTTP " + status + " (" + message + ")]";
                                progressNode.parentNode.className += " error";
                                scroll();
                            }
                        },
                        progress: function (evt) {
                            var num = Math.round(50 * evt.loaded / evt.total);
                            if (num > current) {
                                var str = "";
                                for (var i = 1; i <= num; i++) {
                                    str += "*";
                                }
                                for (var i = num + 1; i <= 50; i++) {
                                    str += ".";
                                }
                                current = num;
                                progressNode.nodeValue = str;
                            }
                        }});
                }
            }

            function sendMessage() {

                if (inputText.value) {
                    var input = {};
                    if (inputTo.value) {
                        input.to = parseInt(inputTo.value);
                    }
                    input.message = inputText.value;
                    wsk.exec({
                        service: "svr.sendMessage",
                        input: input,
                        load: function (response) {
                            if (response.error) {
                                renderError(input.to, response.error.meaning + (response.error.data ? (". " + response.error.data) : ""));
                                scroll();
                            } else if (!response.result) {
                                renderError(input.to, "Message could not be delivered '" + input.message + "'");
                                scroll();
                            }
                        }});
                    inputText.value = "";
                }
            }

            function allowDrop(ev) {
                ev.preventDefault();
            }


            function getInput(files) {
                var input = {};
                input.data = {};
                input.data.files = [];
                input.fileMap = {};
                for (var i = 0; i < files.length; i++) {
                    var key = i + "";
                    input.data.files[i] = key;
                    input.fileMap[key] = files[i];
                }
                return input;
            }

            function drop(ev) {
                ev.preventDefault();
                sendFiles(ev.dataTransfer.files);

            }

            function downloadFile(id) {
                http.ajax({service: "svr.download",
                    input: id});
            }
        </script>
        <style>
            body, input, button{font-family: monospace}
            #messages {list-style-type: none; margin: 0; padding: 0}
            #messages li { padding: 5px 10px;}
            #messages li.from { color: lightgray;} 
            #messages li.to { background-color: lightgreen} 
            #messages li.error { color: red} 
            #messages li a {margin-right: 7px; margin-left: 7px} 
            .wrapperVertical {
                height: 100vh;
                display: -webkit-box;
                display: -webkit-flex;
                display: -ms-flexbox;
                display: flex;
                -webkit-box-orient: vertical;
                -webkit-box-direction: normal;
                -webkit-flex-direction: column;
                -ms-flex-direction: column;
                flex-direction: column;
            }
            .flex {
                -webkit-box-flex: 1;
                -webkit-flex: 1;
                -ms-flex: 1;
                flex: 1;
                overflow-y: scroll;
            }
            main {
                border: lightgray solid 2px;
                margin: 4px;
            }
            header {
                height: 40px;
                margin: 8px;
            }
            footer {
                height: 70px;
            }
            body {
                margin: 0;
            }

        </style></head><body>
        <div class="wrapperVertical">
            <header>
                <table style="width:100%">
                    <tr>
                        <td style="text-align: left; width: 100%; font-size: 200%; font-weight: bold">Brutusin-RPC chat</td>
                        <td style="text-align: right">
                            <table>
                                <tr>
                                    <td style="text-align: right; white-space: nowrap">User id: <span id="userId"></span></td>
                                </tr>
                                <tr>    
                                    <td style="text-align: right; white-space: nowrap">Total connections:  <span id="total"></span></td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </header>
            <main id="main" class="flex" ondrop="drop(event)" ondragover="allowDrop(event)">
                <ul id="messages"></ul>
            </main>
            <footer>
                <table style="width:100%">
                    <tr>
                        <td>
                            <table >
                                <tr>
                                    <td style="text-align: right"> Autoscroll:</td>
                                    <td><input id="autoscroll" type="checkbox" onclick="scroll()"/></td>
                                </tr>
                                <tr>
                                    <td  style="text-align: right">Send to:</td>
                                    <td><input type="number" id="inputTo" placeholder="All" style="width: 40px"/></td>
                                </tr>
                            </table>
                        </td>
                        <td style="width:100%">
                            <input id="message" style="height:40px;width:100%" type="text" onkeydown = "if (event.keyCode == 13)
                                        sendMessage()"/>
                        </td>
                        <td style="width:50px">
                            <table >
                                <tr>
                                    <td><button onclick="sendMessage()">Send</button></td>
                                </tr>
                                <tr>
                                    <td><button onclick="document.getElementById('inputFile').click()">Attach</button><input id="inputFile" multiple="true" type="file" style="position: fixed; top: -100em" onchange="sendFiles(this.files)"></td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </footer>
        </div>
    </body>
    <script>
        autoscroll = document.getElementById("autoscroll");
        inputTo = document.getElementById("inputTo");
        inputText = document.getElementById("message");
        inputText.focus();
    </script>
</html>