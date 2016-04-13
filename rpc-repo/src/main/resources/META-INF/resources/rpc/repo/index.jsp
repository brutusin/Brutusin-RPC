<%@page session="false" contentType="text/html" pageEncoding="UTF-8"%><%
String hash = request.getParameter("hash");
if(hash != null){
    response.sendRedirect(request.getRequestURI()+"#"+hash);
} else {
%><!DOCTYPE html>
<html>
    <head>
        <meta name=viewport content='width=650'>
        <meta name="_csrf" content="${_csrf.token}"/>
        <meta name="_csrf_header" content="${_csrf.headerName}"/>
        <title>brutusin:rpc</title>
        <link rel="stylesheet" href='https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css'/>
        <link rel="stylesheet" href='lib/bootstrap/css/bootstrap-callouts.css'/>
        <link rel="stylesheet" href='https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.9.4/css/bootstrap-select.min.css'/>
        <link rel="stylesheet" href='https://cdn.jsdelivr.net/brutusin.json-forms/1.2.2/css/brutusin-json-forms.min.css' />
        <link rel="stylesheet" href='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.11.0/codemirror.min.css' />
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/octicons/3.4.1/octicons.min.css">
        <style>
            html * {
                outline: 0 !important;
            }
            .class hidden{
                position: absolute;
                z-index: -1;
                visibility: hidden;
            }
            .bs-callout {
                padding: 20px;
                margin: 20px 0;
                border: 1px solid #ddd;
                border-left-color:  #777;
                border-left-width: 5px;
            }
            .tab-content-padded{
                padding: 16px;
                border-bottom: 1px solid #ddd;
                border-left: 1px solid #ddd;
                border-right : 1px solid #ddd;
                border-bottom-left-radius: 4px;
                border-bottom-left-radius: 4px;
            }
            .CodeMirror {
                height: auto;
                width:100%;
            }
            td.right, th.right{
                text-align: right;
            }
            td.left, th.left{
                text-align: left;
            }
            th, td{
                text-align: center;
            }
            #github{
                color:white;
                text-decoration: none;
            }
            #github:hover{
                color:black;
            }

        </style>
        <script src='https://cdnjs.cloudflare.com/ajax/libs/jquery/1.12.0/jquery.min.js'></script>
        <script src='https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js'></script>
        <script src='https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.9.4/js/bootstrap-select.min.js'></script>
        <script src='https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.9.4/js/i18n/defaults-en_US.min.js'></script>
        <script src='lib/markdown/js/markdown.min.js'></script>
        <script src='https://cdn.jsdelivr.net/brutusin.json-forms/1.2.2/js/brutusin-json-forms.min.js'></script>
        <script src='https://cdn.jsdelivr.net/brutusin.json-forms/1.2.2/js/brutusin-json-forms-bootstrap.min.js'></script>
        <script src='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.11.0/codemirror.min.js'></script>
        <script src='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.11.0/mode/javascript/javascript.min.js'></script>
        <script src="../brutusin-rpc.js"></script>
        <script src='brutusin-json-forms-rpc.js'></script>  

        <script language='javascript'>
            var maxMessagesPerTopic = 50;
            var BrutusinForms = brutusin["json-forms"];
            var http = brutusin["rpc"].initHttpEndpoint("<%=request.getContextPath() + org.brutusin.rpc.RpcConfig.getInstance().getPath()+"/http"%>");
            var wskt = brutusin["rpc"].initWebsocketEndpoint("<%=request.getContextPath() + org.brutusin.rpc.RpcConfig.getInstance().getPath()+"/wskt"%>");
            var codeMirrors = new Object();
            var httpServices, wsServices, topics;
            var isServiceHttp;
            var service, topic;

            http.ajax({load: function (response) {
                    if (response === null) {
                        location.reload();
                        return;
                    }
                    httpServices = response.result;
                    renderHTTPServices();
                    route();
                }, service: "rpc.http.services"});

            http.ajax({load: function (response) {
                    if (response === null) {
                        location.reload();
                        return;
                    }
                    if (response.result && response.result.principal) {
                        var userLabel = document.getElementById("labelUser");
                        userLabel.innerHTML = '<span class="octicon octicon-person"></span> ' + response.result.principal;
                    }
                }, service: "rpc.http.user"});

            http.ajax({load: function (response) {
                    if (response === null) {
                        location.reload();
                        return;
                    }
                    if (response.result) {
                        var versionLabel = document.getElementById("versionLabel");
                        versionLabel.innerHTML = response.result;
                    }
                }, service: "rpc.http.version"});

            http.ajax({load: function (response) {
                    if (response === null) {
                        location.reload();
                        return;
                    }
                    if (response.result) {
                        var descDiv = document.getElementById("descDiv");
                        descDiv.className = "bs-callout bs-callout-default";
                        descDiv.innerHTML = markdown.toHTML(response.result);
                    }
                }, service: "rpc.http.description"});

            wskt.exec({load: function (response) {
                    if (response === null) {
                        location.reload();
                        return;
                    }
                    wsServices = response.result;
                    renderWskServices();
                    route();
                }, service: "rpc.wskt.services"});

            wskt.exec({load: function (response) {
                    if (response === null) {
                        location.reload();
                        return;
                    }
                    topics = response.result;
                    renderTopics();
                    route();
                }, service: "rpc.topics"});

            function route() {
                if (!httpServices || !wsServices || !topics) {
                    return;
                }
                if (!window.onhashchange) {
                    window.onhashchange = route;
                }
                var hash = document.location.hash;
                if (hash) {
                    while (hash.endsWith("/")) {
                        hash = hash.substring(0, hash.length - 1);
                    }
                    var tokens = hash.split("/");
                    if (tokens[0] === "#http-services") {
                        if (tokens.length === 1) {
                            showList();
                            $('a[href="#http-services"]').tab('show');
                            return;
                        }
                        var serviceId = tokens[1];
                        for (var i = 0; i < httpServices.length; i++) {
                            if (httpServices[i].id === serviceId) {
                                selectService(i, true);
                                return;
                            }
                        }
                        document.location.hash = tokens[0];
                        return;

                    } else if (tokens[0] === "#wskt-services") {
                        if (tokens.length === 1) {
                            showList();
                            $('a[href="#wskt-services"]').tab('show');
                            return;
                        }
                        var serviceId = tokens[1];
                        for (var i = 0; i < wsServices.length; i++) {
                            if (wsServices[i].id === serviceId) {
                                selectService(i, false);
                                return;
                            }
                        }
                        document.location.hash = tokens[0];
                        return;
                    } else if (tokens[0] === "#topics") {
                        if (tokens.length === 1) {
                            showList();
                            $('a[href="#topics"]').tab('show');
                            return;
                        }
                        var topicId = hash.substring(8);
                        for (var i = 0; i < topics.length; i++) {
                            if (topics[i].id === topicId) {
                                selectTopic(i);
                                return;
                            }
                        }
                        document.location.hash = tokens[0];
                        return;
                    }
                }
                document.location.hash = "http-services";
            }


            function showList() {
                $('a[href="#list"]').tab('show');
            }
            function showService() {
                $('a[href="#service"]').tab('show');
            }
            function showTopic() {
                $('a[href="#topic"]').tab('show');
            }
            function initCodeMirror(id) {
                if (!codeMirrors[id]) {
                    codeMirrors[id] = CodeMirror(document.getElementById(id), {
                        mode: "javascript",
                        lineNumbers: true,
                        viewportMargin: Infinity,
                        readOnly: true
                    });
                }
            }
            function setCodeMirrorValue(id, schema) {
                if (!codeMirrors[id]) {
                    return;
                }
                if (schema) {
                    value = schema;
                } else {
                    value = "";
                }
                codeMirrors[id].setValue(JSON.stringify(value, null, 2));
            }
            function createElement(type, child, className, style) {
                var td = document.createElement(type);
                if (child != null && typeof child !== "undefined") {
                    if (typeof child !== "object") {
                        var txt = document.createTextNode(child);
                        td.appendChild(txt);
                    } else {
                        td.appendChild(child);
                    }
                }
                if (className) {
                    td.className = className;
                }
                if (style) {
                    td.setAttribute("style", style);
                }
                return td;
            }

            function createHelpTooltips() {
                $("[title]").each(function () {
                    if (this.hasAttribute("data-toggle")) {
                        return;
                    }
                    this.setAttribute("data-toggle", "popover");
                    this.setAttribute("data-trigger", "hover");
                    if ("undefined" === typeof markdown) {
                        this.setAttribute("data-content", this.title);
                    } else {
                        this.setAttribute("data-content", markdown.toHTML(this.title));
                    }
                    if (this.help) {
                        this.title = this.help;
                    } else {
                        this.title = "Help";
                    }
                    $(this).popover({
                        placement: 'top',
                        container: 'body',
                        html: !("undefined" === typeof markdown)
                    });
                });
            }
        </script> 
    </head>    
    <body>
        <div style="position: absolute;z-index: -1;visibility: hidden">
            <a href="#list" aria-controls="list" role="tab" data-toggle="tab"></a>
            <a href="#service" aria-controls="service" role="tab" data-toggle="tab"></a>
            <a href="#topic" aria-controls="topic" role="tab" data-toggle="tab"></a>
        </div>
        <div class="container" style="margin-top:10px">
            <img style="position: absolute; z-index: 2;cursor:pointer" src="img/brutusin-logo_small.png" onclick="document.location.hash=''" />
            <div style="padding-left: 80px; padding-top: 7px; margin-top:8px; background-color: #ccc; border-radius: 4px; height: 34px">
                <table style="width: 100%">
                    <tr>
                        <td style="text-align: left"><label id="versionLabel" class="label label-default" style="font-size:14px;cursor:pointer" onclick="document.location.hash = '';">brutusin:rpc</label></td>
                        <td style="text-align: right; padding-right: 12px"><label class="label" id="labelUser" style="font-size:12px; margin-right: 10px"></label><a id="github" href="https://github.com/brutusin/Brutusin-RPC"><span class="octicon octicon-mark-github"></span></a></td>
                    </tr>
                </table>
            </div>
        </div>

        <div class="container" style="margin-bottom: 20px" >
            <div class="tab-content" style="margin-top: 20px;">
                <div role="tabpanel" class="tab-pane active" id="list">
                    <%@include file="list.jspf" %>  
                </div>
                <div role="tabpanel" class="tab-pane" id="service">
                    <%@include file="service.jspf" %>  
                </div>
                <div role="tabpanel" class="tab-pane" id="topic">
                    <%@include file="topic.jspf" %>  
                </div>
            </div>
        </div>
        <div class="modal fade" id="exampleModal" tabindex="-1" role="dialog" aria-labelledby="modalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                        <h4>Service <code class="modal-title" id="modalLabel"></code> description</h4>
                    </div>
                    <div class="modal-body"></div>
                </div>
            </div>
        </div>
        <script>
            createHelpTooltips();
        </script>
    </body>
</html><%}%>

