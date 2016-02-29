<!DOCTYPE html>
<html>
    <head>
        <meta name="_csrf" content="${_csrf.token}"/>
        <meta name="_csrf_header" content="${_csrf.headerName}"/>
        <script src="rpc/brutusin-rpc.js"></script>
        <script language='javascript'>
            var http = brutusin["rpc"].initHttpEndpoint("<%=request.getContextPath() + org.brutusin.rpc.RpcConfig.getInstance().getPath()+"/http"%>");
            var wskt = brutusin["rpc"].initWebsocketEndpoint("<%=request.getContextPath() + org.brutusin.rpc.RpcConfig.getInstance().getPath()+"/wskt"%>");
        </script>
        <script language='javascript'>
            http.ajax({
                service: "rpc.http.version",
                load: function (response) {
                    alert(JSON.stringify(response));
                }
            });
        </script>
    </head>
    <body>
        <h1>Congratulations!</h1> You have successfully created a Brutusin-RPC war application</h1>
    <div>The functional testing module is accessible <a href="rpc/repo">here</a></div>
</body>
</html>
