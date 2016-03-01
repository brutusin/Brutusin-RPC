<!DOCTYPE html>
<html>
    <head>
        <meta name="_csrf" content="${_csrf.token}"/>
        <meta name="_csrf.parameterName" content="${_csrf.parameterName}"/>
        <meta name="_csrf_header" content="${_csrf.headerName}"/>
    </head>
    <frameset cols="50%,50%">
        <frame src="../repo/#wskt-services/publish-service"></frame>    
        <frame src="../repo/#topics/<%=request.getParameter("id")%>">  </frame>
    </frameset>
</html>