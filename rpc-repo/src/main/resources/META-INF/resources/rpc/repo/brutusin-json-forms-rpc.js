/*
 * Copyright 2015 brutusin.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
if ("undefined" === typeof brutusin || "undefined" === typeof brutusin["json-forms"]) {
    throw new Error("brutusin-json-forms-rpc.js requires brutusin-json-forms.js");
}
if ("undefined" === typeof brutusin["rpc"]) {
    throw new Error("brutusin-json-forms-rpc.js requires brutusin-rpc.js");
}
(function () {
    var BrutusinForms = brutusin["json-forms"];
    var rpc = brutusin["rpc"];

    BrutusinForms.createHttpResolver = function (serviceId) {
        return function (names, data, callback) {
            var input = new Object();
            input.id = serviceId;
            input.fieldNames = names;
            input.input = data;
            rpc.getHttpEndpoint().ajax({
                service: "rpc.http.schema-provider",
                input: input,
                load: function (response) {
                    if (response.error) {
                        callback();
                    } else {
                        callback(response.result);
                    }
                }
            });
        };
    };
    
    BrutusinForms.createWebsocketResolver = function (serviceId) {
        return function (names, data, callback) {
            var input = new Object();
            input.id = serviceId;
            input.fieldNames = names;
            input.input = data;
            rpc.getWebsocketEndpoint().exec({
                service: "rpc.wskt.schema-provider",
                input: input,
                load: function (response) {
                    if (response.error) {
                        callback();
                    } else {
                        callback(response.result);
                    }
                }
            });
        };
    };

    BrutusinForms.bootstrap.addFormatDecorator("inputstream", "file");

    BrutusinForms.addDecorator(
            function (element, schema) {
                if (element.tagName) {
                    var tagName = element.tagName.toLowerCase();
                    if (tagName === "input" && element.type === "file") {
                        element.getValue = function () {
                            if (element.value) {
                                return element.id
                            } else {
                                return null;
                            }
                        };
                    }
                }
            }
    );

    function populateFileMap(map, element) {
        if (element.tagName) {
            var tagName = element.tagName.toLowerCase();
            if (tagName === "input" && element.type === "file") {
                if (element.files !== null && element.files.length > 0) {
                    map[element.id] = element.files;
                }
            }
        }
        if (element.childNodes) {
            for (var i = 0; i < element.childNodes.length; i++) {
                populateFileMap(map, element.childNodes[i]);
            }
        }
    }

    function getFileMap(bf) {
        var container = bf.getRenderingContainer();
        if (container) {
            var map = new Object();
            populateFileMap(map, container);
            if (Object.keys(map).length > 0) {
                return map;
            }
        }
        return null;
    }

    var previousRender = BrutusinForms.postRender;
    BrutusinForms.postRender = function (bf) {
        if (previousRender) {
            previousRender(bf);
        }
        if (!bf.getFileMap) {
            bf.getFileMap = function () {
                return getFileMap(bf);
            };
        }
    };

    for (var i = 0; i < BrutusinForms.instances.length; i++) {
        var bf = BrutusinForms.instances[i];
        bf.getFileMap = function () {
            return getFileMap(bf);
        };
    }

}());