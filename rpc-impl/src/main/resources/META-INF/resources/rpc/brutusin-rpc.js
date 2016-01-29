/*
 * Copyright 2015 brutusin.org
 *
 * Licensed under the Apache License, Version 2.0 (the "SuperLicense");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */

if (typeof brutusin === "undefined") {
    window.brutusin = new Object();
} else if (typeof brutusin !== "object") {
    throw "brutusin global variable already exists";
}
(function () {
    if (!String.prototype.startsWith) {
        String.prototype.startsWith = function (searchString, position) {
            position = position || 0;
            return this.indexOf(searchString, position) === position;
        };
    }
    if (!String.prototype.endsWith) {
        String.prototype.endsWith = function (searchString, position) {
            var subjectString = this.toString();
            if (position === undefined || position > subjectString.length) {
                position = subjectString.length;
            }
            position -= searchString.length;
            var lastIndex = subjectString.indexOf(searchString, position);
            return lastIndex !== -1 && lastIndex === position;
        };
    }
    if (!String.prototype.includes) {
        String.prototype.includes = function () {
            'use strict';
            return String.prototype.indexOf.apply(this, arguments) !== -1;
        };
    }
    if (!String.prototype.format) {
        String.prototype.format = function () {
            var formatted = this;
            for (var i = 0; i < arguments.length; i++) {
                var regexp = new RegExp('\\{' + i + '\\}', 'gi');
                formatted = formatted.replace(regexp, arguments[i]);
            }
            return formatted;
        };
    }

    var rpc = new Object();
    brutusin["rpc"] = rpc;
    function createRpcRequest(service, input, id) {
        var req = new Object();
        req.jsonrpc = "2.0";
        if (typeof id !== 'undefined') {
            req.id = id;
        }
        req.params = input;
        req.method = service;
        return req;
    }

    rpc.initHttpEndpoint = function (endpoint) {
        var proxy = new Object();
        rpc.getHttpEndpoint = function () {
            return proxy;
        };
        var queue = [];
        var queuedSubmit;
        var services;

        ajax(function (response, status) {
            if (status === 200) {
                services = new Object();
                for (var i = 0; i < response.result.length; i++) {
                    services[response.result[i].id] = response.result[i];
                }
                if (queuedSubmit) {
                    proxy.submit(queuedSubmit);
                    return;
                }
                for (var i = 0; i < queue.length; i++) {
                    var ajaxParam = queue[i];
                    proxy.ajax(ajaxParam);
                }
                queue = [];
            } else {
                throw response;
            }
        }, "rpc.http.services", null, null, "GET", "json");

        proxy.ajax = function (ajaxParam) {
            if (ajaxParam === null || typeof ajaxParam !== "object") {
                throw "ajax() parameter has to be an object";
            }
            if (services) {
                var httpMethod, responseType;
                if (ajaxParam.service) {
                    var service = services[ajaxParam.service];
                    if (!service) {
                        throw "Service not found: '" + ajaxParam.service + "'";
                    }
                    var httpMethod;
                    if (service.safe) {
                        httpMethod = "GET";
                    } else {
                        if (service.idempotent) {
                            httpMethod = "PUT";
                        } else {
                            httpMethod = "POST";
                        }
                    }
                    var responseType;
                    if (service.binaryResponse) {
                        responseType = "blob";
                    } else {
                        responseType = "json";
                    }
                } else {
                    httpMethod = "GET";
                    responseType = "json";
                }
                ajax(ajaxParam.load, ajaxParam.service, ajaxParam.input, ajaxParam.files, httpMethod, responseType, ajaxParam.progress);
            } else {
                queue[queue.length] = ajaxParam;
            }
        };

        var submitted = false;
        var queued = false;
        proxy.submit = function (submitParam) {
            if (submitParam === null || typeof submitParam !== "object") {
                throw "submit() parameter has to be an object";
            }
            if (submitted) {
                return;
            }
            if (services) {
                var httpMethod;
                if (submitParam.service) {
                    var service = services[submitParam.service];
                    if (!service) {
                        throw "Service not found: '" + submitParam.service + "'";
                    }
                    var httpMethod;
                    if (service.safe) {
                        httpMethod = "GET";
                    } else {
                        if (service.idempotent) {
                            httpMethod = "PUT";
                        } else {
                            httpMethod = "POST";
                        }
                    }
                } else {
                    httpMethod = "GET";
                }
                submitted = true;
                setTimeout(function () {
                    submitted = false;
                }, 500);
                submit(submitParam.service, submitParam.input, submitParam.files, httpMethod);

            } else {
                if (!queued) {
                    queuedSubmit = submitParam;
                    queued = true;
                }
            }

        };
        proxy.services = services;
        return proxy;

        /**
         * Returns the upload order for the files by ascending size. This way the last one, that can be 
         * streamed directly in the server without going to disk, is the larger one.
         * @param {type} files
         * @returns {undefined}
         */
        function getOrder(files) {
            var keys = Object.keys(files).slice(0);
            keys.sort(
                    function (a, b) {
                        if (files[a].length === 0) {
                            return 1;
                        } else if (files[b].length === 0) {
                            return -1;
                        }
                        return files[a][0].size - files[b][0].size;
                    }
            );
            return keys;
        }

        function submit(service, input, files, httpMethod) {
            var req = createRpcRequest(service, input);
            if (files) {
                httpMethod = "POST";
            }
            if (httpMethod === "PUT") { // HTML forms only supports GET and POST
                if (window.console) {
                    console.warn("Idempotent unsafe services (like '" + service + "' service) are recommended to be executed via ajax() httpMethod, since it supports the PUT HTTP method");
                }
                httpMethod = "POST";
            }
            var form = document.createElement("form");
            form.action = endpoint;

            form.httpMethod = httpMethod;
            addHidden("jsonrpc", JSON.stringify(req));
            if (files) {
                form.enctype = "multipart/form-data";
                var order = getOrder(files);
                for (var i = 0; i < order.length; i++) {
                    var p = order[i];
                    if (files[p].length > 0) {
                        addFile(p, files[p]);
                    }
                }
            }
            document.body.appendChild(form);
            form.submit();

            function addHidden(name, value) {
                var input = document.createElement("input");
                input.type = "hidden";
                input.name = name;
                input.value = value;
                form.appendChild(input);
            }

            function addFile(name, fileList) {
                var input = document.createElement("input");
                input.type = "file";
                input.name = name;
                input.files = fileList;
                form.appendChild(input);
            }
        }

        function getAttachmentFileName(contentDisposition) {
            var filename;
            if (contentDisposition && contentDisposition.indexOf('attachment') !== -1) {
                var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                var matches = filenameRegex.exec(contentDisposition);
                if (matches !== null && matches[1])
                    filename = matches[1].replace(/['"]/g, '');
            }
            return filename;
        }

        function ajax(load, service, input, files, httpMethod, responseType, progress) {
            var req = createRpcRequest(service, input);
            if (files) {
                httpMethod = "POST";
            }
            if (!responseType) {
                responseType = 'json';
            }
            var data;
            var xhr = new XMLHttpRequest();
            if (progress) {
                xhr.upload.addEventListener("progress", progress);
            }
            if (httpMethod === "GET") {
                var urlparam = "?jsonrpc=" + JSON.stringify(req);
                xhr.open(httpMethod, endpoint + urlparam, true);
            } else {
                data = new FormData();
                data.append("jsonrpc", JSON.stringify(req));
                if (files) {
                    var order = getOrder(files);
                    for (var i = 0; i < order.length; i++) {
                        var p = order[i];
                        if (files[p].length > 0) {
                            data.append(p, files[p][0]);
                        }
                    }
                }
                xhr.open(httpMethod, endpoint, true);
            }

            xhr.onload = function () {
                if (responseType === "blob" && this.status === 200) {
                    var type = xhr.getResponseHeader('Content-Type');
                    var blob = new Blob([this.response], {type: type});
                    if (load) {
                        load(blob, this.status, this.statusText);
                    } else {
                        var filename = getAttachmentFileName(xhr.getResponseHeader('Content-Disposition'));
                        var URL = window.URL || window.webkitURL;
                        var downloadUrl = URL.createObjectURL(blob);
                        if (filename) {
                            if (typeof window.navigator.msSaveBlob !== 'undefined') {
                                window.navigator.msSaveBlob(blob, filename);
                            } else {
                                // use HTML5 a[download] attribute to specify filename
                                var a = document.createElement("a");
                                // safari doesn't support this yet
                                if (typeof a.download === 'undefined') {
                                    window.location = downloadUrl;
                                } else {
                                    a.href = downloadUrl;
                                    a.download = filename;
                                    document.body.appendChild(a);
                                    a.click();
                                }
                            }
                        } else {
                            if (typeof window.navigator.msSaveOrOpenBlob !== 'undefined') {
                                window.navigator.msSaveOrOpenBlob(blob);
                            } else {
                                window.location = downloadUrl;
                            }
                        }

                        setTimeout(function () {
                            URL.revokeObjectURL(downloadUrl);
                        }, 100); // cleanup
                    }
                } else { // json
                    if (load) {
                        var response;
                        if (typeof this.response === "string") {
                            eval("response=" + this.response);
                        } else {
                            response = this.response;
                        }
                        load(response, this.status, this.statusText);
                    }
                }
            };
            xhr.onerror = function (e) {
                load({
                    "jsonrpc": "2.0",
                    "error": {
                        "code": -32003,
                        "message": "Connection error",
                        "meaning": "Cannot connect to server"
                    }
                });
            };
            xhr.responseType = responseType;
            xhr.send(data);
        }
    };
    rpc.initWebsocketEndpoint = function (endpoint) {
        var proxy = new Object();
        rpc.getWebsocketEndpoint = function () {
            return proxy;
        };
        var queue = new Array();
        var services;
        var url;
        if (endpoint.startsWith("wss:") || endpoint.startsWith("ws:")) {
            url = endpoint;
        } else {
            if (window.location.protocol === "https:") {
                url = "wss:";
            } else {
                url = "ws:";
            }
            if (endpoint.startsWith("/")) {
                url += "//" + window.location.host + endpoint;
            } else {
                url += "//" + window.location.host + window.location.pathname + endpoint;
            }
        }
        var ws = new WebSocket(url);
        var lastReqId = 0;
        var rpcCallbacks = new Object();
        var topicCallbacks = new Object();

        function exec(load, service, input) {
            var reqId;
            if (load) {
                reqId = ++lastReqId;
                rpcCallbacks[reqId] = load;
            }
            var req = createRpcRequest(service, input, reqId);

            function sendMessage(msg) {
                if (ws.readyState === 1) {
                    ws.send(msg);
                } else if (ws.readyState === 0) {
                    setTimeout(
                            function () {
                                sendMessage(msg);
                            },
                            100);
                } else {
                    load({
                        "jsonrpc": "2.0",
                        "id": reqId,
                        "error": {
                            "code": -32003,
                            "message": "Connection error",
                            "meaning": "Cannot connect to server",
                            "data": "Server connection lost. Try reloading the page"
                        }
                    });
                }
            }
            sendMessage(JSON.stringify(req));
        }

        ws.onmessage = function (event) {
            var response;
            eval("response=" + event.data);
            if (response.jsonrpc) {
                var callback = rpcCallbacks[response.id];
                delete rpcCallbacks[response.id];
                callback(response);
            } else {
                var callback = topicCallbacks[response.topic];
                callback(response.message);
            }
        };
        exec(function (response) {
            services = new Object();
            for (var i = 0; i < response.result.length; i++) {
                services[response.result[i].id] = response.result[i];
            }
            for (var i = 0; i < queue.length; i++) {
                var execParam = queue[i];
                proxy.exec(execParam);
            }
            queue = [];
        }, "rpc.wskt.services");
        proxy.subscribe = function (topic, callback) {
            topicCallbacks[topic] = callback;
            proxy.exec({
                service: "rpc.topics.subscribe",
                input: {id: topic}
            });
        };
        proxy.unsubscribe = function (topic, callback) {
            topicCallbacks[topic] = callback;
            proxy.exec({
                service: "rpc.topics.unsubscribe",
                input: {id: topic}
            });
        };
        proxy.exec = function (execParam) {
            if (execParam === null || typeof execParam !== "object") {
                throw "exec() parameter has to be an object";
            }
            if (!execParam.service) {
                throw "execParam.service is required";
            }
            if (services) {
                var service = services[execParam.service];
                if (!service) {
                    throw "Service not found: '" + execParam.service + "'";
                }
                exec(execParam.load, execParam.service, execParam.input);
            } else {
                queue[queue.length] = execParam;
            }
        };
        return proxy;
    };

}());
